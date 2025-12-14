package org.sudoku;

import org.java_websocket.WebSocket;
import org.sudoku.shared.protocol.GameMessage;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages all active games and handles matchmaking.
 * Thread-safe using ConcurrentHashMap and synchronized methods.
 */
public class GameManager {
    private final ConcurrentHashMap<String, Game> games = new ConcurrentHashMap<>();
    private final Set<String> usedGameCodes = Collections.synchronizedSet(new HashSet<>());

    /**
     * Create a new game and return its game code
     */
    public String createGame(String playerName, WebSocket session, String difficulty) {
        String gameCode = generateUniqueGameCode();
        Game game = new Game(gameCode, difficulty);
        game.addPlayer(playerName, session);
        games.put(gameCode, game);
        // Note: usedGameCodes.add() is now handled atomically in generateUniqueGameCode()
        
        System.out.println("[GameManager] Created game " + gameCode + " by player: " + playerName + " (difficulty: " + difficulty + ")");
        return gameCode;
    }

    /**
     * Join an existing game by game code
     * @return the Game object if successful, null otherwise
     *
     * We could have this method is not synchronized but then they will just need to add check in game.isFull()
     * and addPlayer methods which does not make huge difference.
     */
    public Game joinGame(String gameCode, String playerName, WebSocket session) {
        Game game = games.get(gameCode);
        
        if (game == null) {
            System.out.println("[GameManager] Game " + gameCode + " not found");
            return null;
        }
        
        if (game.isFull()) {
            System.out.println("[GameManager] Game " + gameCode + " is already full");
            return null;
        }

        // add Player object to Game object as Player1 or Player2 field.
        int playerId = game.addPlayer(playerName, session);
        if (playerId == -1) {
            System.out.println("[GameManager] Failed to add player to game " + gameCode);
            return null;
        }
        
        System.out.println("[GameManager] Player " + playerName + " joined game " + gameCode + " as player " + playerId);
        return game;
    }

    /**
     * Get list of games waiting for players
     */
    public List<GameInfo> getWaitingGames() {
        List<GameInfo> waitingGames = new ArrayList<>();
        
        for (Map.Entry<String, Game> entry : games.entrySet()) {
            Game game = entry.getValue();
            if (game.isWaiting() && !game.isGameEnded()) {
                waitingGames.add(new GameInfo(entry.getKey(), game.getPlayer1().getName()));
            }
        }
        
        System.out.println("[GameManager] Listed " + waitingGames.size() + " waiting games");
        return waitingGames;
    }

    /**
     * Get a game by its code
     */
    public Game getGame(String gameCode) {
        return games.get(gameCode);
    }

    /**
     * Remove a game (cleanup after completion)
     */
    public synchronized void removeGame(String gameCode) {
        Game game = games.remove(gameCode);
        if (game != null) {
            usedGameCodes.remove(gameCode);
            System.out.println("[GameManager] Removed game " + gameCode);
        }
    }

    /**
     * Cleanup all ended games
     */
    public synchronized void cleanupEndedGames() {
        List<String> toRemove = new ArrayList<>();
        
        for (Map.Entry<String, Game> entry : games.entrySet()) {
            if (entry.getValue().isGameEnded()) {
                toRemove.add(entry.getKey());
            }
        }
        
        for (String gameCode : toRemove) {
            removeGame(gameCode);
        }
    }

    /**
     * Generate a unique 4-digit game code.
     * Thread-safe: Uses ThreadLocalRandom for lock-free random generation per thread,
     * and atomic check-and-add via Set.add() to prevent race conditions.
     *
     * Race condition prevention:
     * - Old approach: contains() then add() separately → race window
     * - New approach: add() returns false if exists → atomic operation
     */
    private String generateUniqueGameCode() {
        String code;
        do {
            int randomCode = ThreadLocalRandom.current().nextInt(1000, 10000);
            code = String.valueOf(randomCode);
            // Atomic operation: add() returns false if code already exists, true if added
            // This prevents race condition where two threads generate same code
        } while (!usedGameCodes.add(code));
        
        return code;
    }

    /**
     * Get total number of active games
     */
    public int getActiveGameCount() {
        return games.size();
    }

    /**
     * Game information for listing
     */
    public static class GameInfo {
        private final String gameCode;
        private final String creatorName;

        public GameInfo(String gameCode, String creatorName) {
            this.gameCode = gameCode;
            this.creatorName = creatorName;
        }

        public String getGameCode() {
            return gameCode;
        }

        public String getCreatorName() {
            return creatorName;
        }

        public GameMessage.ListGamesResponse.GameInfo toMessageGameInfo() {
            return new GameMessage.ListGamesResponse.GameInfo(gameCode, creatorName);
        }
    }
}
