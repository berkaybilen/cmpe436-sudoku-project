package org.sudoku;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.sudoku.shared.protocol.GameMessage;

import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * WebSocket server for Sudoku multiplayer game.
 * Handles client connections and routes messages to appropriate game instances.
 */
public class SudokuWebSocketServer extends WebSocketServer {
    private final GameManager gameManager;
    
    // Maps WebSocket connection to (gameCode, playerId, playerName)
    private final ConcurrentHashMap<WebSocket, PlayerSession> playerSessions = new ConcurrentHashMap<>();
    
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");

    public SudokuWebSocketServer(int port) {
        super(new InetSocketAddress(port));
        this.gameManager = new GameManager();
    }
    
    private void logWithTimestamp(String message) {
        String timestamp = dateFormat.format(new Date());
        System.out.println("[" + timestamp + "] [Server] " + message);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        String address = conn.getRemoteSocketAddress().toString();
        logWithTimestamp("===== NEW CONNECTION OPENED =====");
        logWithTimestamp("Remote address: " + address);
        logWithTimestamp("Resource descriptor: " + handshake.getResourceDescriptor());
        logWithTimestamp("Total active connections: " + getConnections().size());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        String address = conn.getRemoteSocketAddress().toString();
        PlayerSession session = playerSessions.get(conn);
        String playerName = session != null ? session.playerName : "unknown";
        
        logWithTimestamp("===== CONNECTION CLOSED =====");
        logWithTimestamp("Player: " + playerName);
        logWithTimestamp("Remote address: " + address);
        logWithTimestamp("Close code: " + code);
        logWithTimestamp("Close reason: " + (reason != null && !reason.isEmpty() ? reason : "none"));
        logWithTimestamp("Closed by remote: " + remote);
        
        // Handle player disconnect
        playerSessions.remove(conn);
        if (session != null) {
            Game game = gameManager.getGame(session.gameCode);
            if (game != null) {
                game.handleDisconnect(session.playerId);
                logWithTimestamp("Player '" + session.playerName + "' (ID: " + session.playerId + ") disconnected from game " + session.gameCode);
            }
        }
        logWithTimestamp("Remaining active connections: " + getConnections().size());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        // Don't log Ping messages to avoid spam
        if (!message.contains("\"type\":\"Ping\"")) {
            PlayerSession session = playerSessions.get(conn);
            String playerName = session != null ? session.playerName : "unknown";
            logWithTimestamp("[" + playerName + "] Received message: " + message);
        }
        
        GameMessage gameMessage = MessageSerializer.deserialize(message);
        if (gameMessage == null) {
            logWithTimestamp("ERROR: Failed to deserialize message: " + message);
            sendError(conn, "Invalid message format");
            return;
        }
        
        handleMessage(conn, gameMessage);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        String address = conn != null ? conn.getRemoteSocketAddress().toString() : "unknown";
        PlayerSession session = conn != null ? playerSessions.get(conn) : null;
        String playerName = session != null ? session.playerName : "unknown";
        
        logWithTimestamp("===== ERROR ON CONNECTION =====");
        logWithTimestamp("Player: " + playerName);
        logWithTimestamp("Connection: " + address);
        logWithTimestamp("Exception type: " + ex.getClass().getSimpleName());
        logWithTimestamp("Error message: " + ex.getMessage());
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        logWithTimestamp("===== WEBSOCKET SERVER STARTED =====");
        logWithTimestamp("Listening on port: " + getPort());
        
        // Enable WebSocket-level ping frames to keep connections alive through Cloud Run
        // This sends protocol-level ping frames every 30 seconds
        // If no pong is received within 60 seconds, the connection is considered lost
        setConnectionLostTimeout(60);
        logWithTimestamp("Connection lost timeout: 60 seconds (WebSocket ping frames enabled)");
        logWithTimestamp("This will send WebSocket PING frames every 30s to keep Cloud Run connections alive");
    }

    /**
     * Route messages based on type // MAIN LOGIC IS HANDLED HERE
     */
    private void handleMessage(WebSocket conn, GameMessage message) {
        try {
            if (message instanceof GameMessage.CreateGameRequest) {
                handleCreateGame(conn, (GameMessage.CreateGameRequest) message);
            } else if (message instanceof GameMessage.JoinGameRequest) {
                handleJoinGame(conn, (GameMessage.JoinGameRequest) message);
            } else if (message instanceof GameMessage.ListGamesRequest) {
                handleListGames(conn);
            } else if (message instanceof GameMessage.MoveRequest) {
                handleMoveRequest(conn, (GameMessage.MoveRequest) message);
            } else if (message instanceof GameMessage.ExitGameRequest) {
                handleExitGameRequest(conn, (GameMessage.ExitGameRequest) message);
            } else if (message instanceof GameMessage.Ping) {
                PlayerSession session = playerSessions.get(conn);
                String playerName = session != null ? session.playerName : "unknown";
                logWithTimestamp("[" + playerName + "] Received PING, sending PONG");
                conn.send(MessageSerializer.serialize(GameMessage.Pong.INSTANCE));
            } else {
                sendError(conn, "Unknown message type: " + message.getClass().getSimpleName());
            }
        } catch (Exception e) {
            System.err.println("[Server] Error handling message: " + e.getMessage());
            e.printStackTrace();
            sendError(conn, "Server error: " + e.getMessage());
        }
    }
    
    /**
     * Handle exit game request - player voluntarily exits (different from disconnect)
     * When exiting, we clean up the game from memory and notify the other player
     */
    private void handleExitGameRequest(WebSocket conn, GameMessage.ExitGameRequest request) {
        PlayerSession session = playerSessions.remove(conn);
        if (session == null) {
            sendError(conn, "You are not in a game");
            return;
        }

        Game game = gameManager.getGame(session.gameCode);
        if (game == null) {
            sendError(conn, "Game not found");
            return;
        }

        logWithTimestamp("Player '" + session.playerName + "' (ID: " + session.playerId + ") exited game " + session.gameCode);
        
        // Mark player as disconnected and notify the other player
        game.handleDisconnect(session.playerId);
        
        // Clean up ended games from memory
        gameManager.cleanupEndedGames();
        
        logWithTimestamp("Cleaned up ended games. Active games: " + gameManager.getActiveGameCount());
    }

    /**
     * Handle create game request
     */
    private void handleCreateGame(WebSocket conn, GameMessage.CreateGameRequest request) {
        String playerName = request.getPlayerName();
        String gameCode = gameManager.createGame(playerName, conn, request.getDifficulty());
        
        // Store player session
        playerSessions.put(conn, new PlayerSession(gameCode, 1, playerName));
        
        // Send response
        GameMessage.CreateGameResponse response = new GameMessage.CreateGameResponse(gameCode, 1);
        conn.send(MessageSerializer.serialize(response));
        
        // Inform player they're waiting
        GameMessage.WaitingForPlayer waiting = new GameMessage.WaitingForPlayer(gameCode);
        conn.send(MessageSerializer.serialize(waiting));
        
        logWithTimestamp("Game created: " + gameCode + " by '" + playerName + "' (difficulty: " + request.getDifficulty() + ")");
    }

    /**
     * Handle join game request
     */
    private void handleJoinGame(WebSocket conn, GameMessage.JoinGameRequest request) {
        String gameCode = request.getGameCode();
        String playerName = request.getPlayerName();
        Game game = gameManager.joinGame(gameCode, playerName, conn);
        
        if (game == null) {
            sendError(conn, "Failed to join game " + gameCode + " (not found or full)");
            return;
        }
        
        // Store player session
        playerSessions.put(conn, new PlayerSession(gameCode, 2, playerName));
        
        // Start the game (send GameStart to both players)
        game.sendGameStart();
        
        logWithTimestamp("Game " + gameCode + " started with 2 players: '" + game.getPlayer1().getName() + "' vs '" + game.getPlayer2().getName() + "'");
    }

    /**
     * Handle list games request
     */
    private void handleListGames(WebSocket conn) {
        List<GameManager.GameInfo> waitingGames = gameManager.getWaitingGames();
        
        List<GameMessage.ListGamesResponse.GameInfo> messageGameInfos = waitingGames.stream()
                .map(GameManager.GameInfo::toMessageGameInfo)
                .collect(Collectors.toList());
        
        GameMessage.ListGamesResponse response = new GameMessage.ListGamesResponse(messageGameInfos);
        conn.send(MessageSerializer.serialize(response));
        
        System.out.println("[Server] Sent list of " + waitingGames.size() + " waiting games");
    }

    /**
     * Handle move request
     */
    private void handleMoveRequest(WebSocket conn, GameMessage.MoveRequest request) {
        PlayerSession session = playerSessions.get(conn);
        if (session == null) {
            sendError(conn, "You are not in a game");
            return;
        }
        
        Game game = gameManager.getGame(session.gameCode);
        if (game == null) {
            sendError(conn, "Game not found");
            return;
        }
        
        String playerName = session.playerName;
        
        // Process the move
        Game.MoveResult result = game.processMove(session.playerId, request.getRow(), 
                request.getCol(), request.getValue());
        
        // Create message response with updated scores
        GameMessage.MoveResult resultMessage = new GameMessage.MoveResult(
                result.playerId,
                result.row,
                result.col,
                result.value,
                result.success,
                result.success, // isCorrect (only true if successful)
                result.reason,
                game.getPlayer1Score(),
                game.getPlayer2Score()
        );
        
        if (result.success) {
            // Broadcast successful move to both players (shows cell reveal + scores)
            game.broadcast(resultMessage);
            logWithTimestamp("Player '" + playerName + "' (ID: " + session.playerId + ") made successful move: (" 
                    + result.row + "," + result.col + ") = " + result.value);
            
            // Check if game is complete
            if (game.isGameEnded()) {
                game.sendGameEnd(false);
                logWithTimestamp("Game " + session.gameCode + " completed by players '" + 
                        game.getPlayer1().getName() + "' and '" + game.getPlayer2().getName() + "'");
            }
        } else {
            // For failed moves:
            // 1. Send detailed error to the player who made the move
            conn.send(MessageSerializer.serialize(resultMessage));
            logWithTimestamp("Player '" + playerName + "' (ID: " + session.playerId + ") move rejected: " + result.reason);
            
            // 2. If score changed (penalty for wrong answer), broadcast score update to both players
            if (result.reason != null && (result.reason.contains("Wrong answer") || 
                                         result.reason.contains("Invalid move (Sudoku rules)"))) {
                // Create a score-only update message for the other player (don't reveal the failed move details)
                GameMessage.MoveResult scoreUpdateMessage = new GameMessage.MoveResult(
                        result.playerId,
                        -1, -1, 0, // Don't reveal position/value
                        false,
                        false,
                        "Opponent made an incorrect move", // Generic message
                        game.getPlayer1Score(),
                        game.getPlayer2Score()
                );
                
                // Send to the OTHER player to show opponent's score decreased
                Player otherPlayer = (session.playerId == 1) ? game.getPlayer2() : game.getPlayer1();
                if (otherPlayer != null && otherPlayer.isConnected()) {
                    otherPlayer.send(scoreUpdateMessage);
                }
            }
        }
    }

    /**
     * Send error message to client
     */
    private void sendError(WebSocket conn, String errorMessage) {
        PlayerSession session = playerSessions.get(conn);
        String playerName = session != null ? session.playerName : "unknown";
        GameMessage.ErrorMessage error = new GameMessage.ErrorMessage(errorMessage);
        conn.send(MessageSerializer.serialize(error));
        logWithTimestamp("Sent error to '" + playerName + "': " + errorMessage);
    }

    /**
     * Get the game manager
     */
    public GameManager getGameManager() {
        return gameManager;
    }

    /**
     * Helper class to store player session info
     */
    private static class PlayerSession {
        final String gameCode;
        final int playerId;
        final String playerName;

        PlayerSession(String gameCode, int playerId, String playerName) {
            this.gameCode = gameCode;
            this.playerId = playerId;
            this.playerName = playerName;
        }
    }
}
