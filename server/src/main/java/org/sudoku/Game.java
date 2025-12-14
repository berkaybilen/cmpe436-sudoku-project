package org.sudoku;

import org.java_websocket.WebSocket;
import org.sudoku.shared.protocol.GameMessage;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Represents a single Sudoku game between two players.
 * Handles game state, cell-level locking, and move validation.
 */
public class Game {
    private final String gameCode;
    private final int[] puzzle;  // Flat array for sending to client
    private final int[][] currentBoard;  // 9x9 current game state
    private final int[][] solutionBoard; // 9x9 solution
    private final boolean[][] isInitial; // 9x9 track initial clue cells
    private final Lock[][] cellLocks;

    private Player player1;
    private Player player2;
    private int player1Score = 0;
    private int player2Score = 0;
    private boolean gameStarted = false;
    private boolean gameEnded = false;

    /**
     * Create a new game with a premade puzzle
     */
    public Game(String gameCode, String difficulty) {
        this.gameCode = gameCode;
        
        // Parse difficulty string to enum
        PremadePuzzles.Difficulty diff;
        try {
            diff = PremadePuzzles.Difficulty.valueOf(difficulty.toUpperCase());
        } catch (IllegalArgumentException e) {
            System.err.println("[Game] Invalid difficulty: " + difficulty + ", defaulting to EASY");
            diff = PremadePuzzles.Difficulty.EASY;
        }
        
        // Get a random premade puzzle and solution
        PremadePuzzles.ParsedPuzzle puzzleData = PremadePuzzles.getRandomPuzzle(diff);
        this.puzzle = puzzleData.puzzle;
        
        // Initialize boards
        this.currentBoard = new int[9][9];
        this.solutionBoard = new int[9][9];
        this.isInitial = new boolean[9][9];
        
        // Convert flat arrays to 2D boards
        for (int i = 0; i < 81; i++) {
            int row = i / 9;
            int col = i % 9;
            int value = puzzleData.puzzle[i];
            int solutionValue = puzzleData.solution[i];
            
            currentBoard[row][col] = value;
            solutionBoard[row][col] = solutionValue;
            isInitial[row][col] = (value != 0);
        }

        // Initialize cell-level locks (9x9 grid)
        this.cellLocks = new Lock[9][9];
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                cellLocks[i][j] = new ReentrantLock();
                // we don't use recursive mechanism of reentrant lock but this is an class that implements try-lock mechanism, we just use the class instance without a recursive protection overhead.
            }
        }
    }

    /**
     * Add a player to the game
     * @return player ID (1 or 2), or -1 if game is full
     */
    public synchronized int addPlayer(String playerName, WebSocket session) {
        if (player1 == null) {
            player1 = new Player(1, playerName, session);
            return 1;
        } else if (player2 == null) {
            player2 = new Player(2, playerName, session);
            gameStarted = true;
            return 2;
        }
        return -1; // Game is full
    }

    /**
     * Check if game is full (both players joined)
     */
    public boolean isFull() {
        return player1 != null && player2 != null;
    }

    /**
     * Check if game is waiting for a second player
     */
    public boolean isWaiting() {
        return player1 != null && player2 == null;
    }

    /**
     * Process a move from a player
     */
    public MoveResult processMove(int playerId, int row, int col, int value) {
        // Validate player
        if (playerId != 1 && playerId != 2) {
            return new MoveResult(playerId, row, col, value, false, "Invalid player ID");
        }

        // Check if game has started
        if (!gameStarted) {
            return new MoveResult(playerId, row, col, value, false, "Game not started");
        }

        // Check if game has ended
        if (gameEnded) {
            return new MoveResult(playerId, row, col, value, false, "Game already ended");
        }

        // Validate coordinates
        if (row < 0 || row > 8 || col < 0 || col > 8) {
            return new MoveResult(playerId, row, col, value, false, "Invalid coordinates");
        }

        // Validate value
        if (value < 1 || value > 9) {
            return new MoveResult(playerId, row, col, value, false, "Invalid value");
        }
        // Check if cell is an initial clue (can't modify)
        if (isInitial[row][col]) {
            return new MoveResult(playerId, row, col, value, false, "Cannot modify initial clue");
        }

        // Check if cell is already filled
        if (currentBoard[row][col] != 0) {
            return new MoveResult(playerId, row, col, value, false, "Cell already filled");
        }


        // Try to acquire cell lock (non-blocking)
        System.out.println("Player " + playerId + " attempting move at [" + row + ", " + col + "] = " + value);
        Lock lock = cellLocks[row][col];
        if (!lock.tryLock()) {
            System.out.println("Cell [" + row + ", " + col + "] is busy");
            return new MoveResult(playerId, row, col, value, false, "Cell is busy");
        }

        try {

            // Check Sudoku rules
            if (!isValidSudokuMove(row, col, value)) {
                if (playerId == 1) {
                    player1Score -= 10;
                } else {
                    player2Score -= 10;
                }
                return new MoveResult(playerId, row, col, value, false, "Invalid move (Sudoku rules)");
            }

            // Check against solution
            boolean isCorrect = (solutionBoard[row][col] == value);

            if (!isCorrect) {
                // Wrong answer - penalize and return error (don't broadcast to other player)
                if (playerId == 1) {
                    player1Score -= 10;
                } else {
                    player2Score -= 10;
                }
                return new MoveResult(playerId, row, col, value, false, "Wrong answer");
            }

            // Correct answer - update board and score
            currentBoard[row][col] = value;
            
            if (playerId == 1) {
                player1Score += 10;
            } else {
                player2Score += 10;
            }

            // Check if game is complete
            if (isBoardFilled()) {
                gameEnded = true;
            }

            System.out.println("Player " + playerId + " correctly filled [" + row + ", " + col + "] = " + value);
            return new MoveResult(playerId, row, col, value, true, null);

        } finally {
            lock.unlock();
        }
    }

    /**
     * Check if a value is valid at the given position according to Sudoku rules
     */
    private boolean isValidSudokuMove(int row, int col, int value) {
        // Check row
        for (int c = 0; c < 9; c++) {
            if (c != col && currentBoard[row][c] == value) {
                return false;
            }
        }

        // Check column
        for (int r = 0; r < 9; r++) {
            if (r != row && currentBoard[r][col] == value) {
                return false;
            }
        }

        // Check 3x3 box
        int boxStartRow = (row / 3) * 3;
        int boxStartCol = (col / 3) * 3;
        for (int r = boxStartRow; r < boxStartRow + 3; r++) {
            for (int c = boxStartCol; c < boxStartCol + 3; c++) {
                if (r != row && c != col && currentBoard[r][c] == value) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Check if the board is completely filled
     */
    private boolean isBoardFilled() {
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                if (currentBoard[row][col] == 0) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Broadcast a message to both players
     */
    public void broadcast(GameMessage message) {
        if (player1 != null && player1.isConnected()) {
            player1.send(message);
        }
        if (player2 != null && player2.isConnected()) {
            player2.send(message);
        }
    }

    /**
     * Send game start message to both players
     */
    public void sendGameStart() {
        if (player1 != null && player2 != null) {
            GameMessage.GameStart startMsg1 = new GameMessage.GameStart(
                    puzzle, player1.getName(), player2.getName(), 1);
            player1.send(startMsg1);

            GameMessage.GameStart startMsg2 = new GameMessage.GameStart(
                    puzzle, player1.getName(), player2.getName(), 2);
            player2.send(startMsg2);
        }
    }

    /**
     * Handle player disconnect: it ends the game of the player.
     */
    public synchronized void handleDisconnect(int playerId) {
        if (!gameEnded) {
            gameEnded = true;
            String disconnectedName = playerId == 1 ? player1.getName() : player2.getName();
            broadcast(new GameMessage.PlayerDisconnect(playerId, disconnectedName));
            
            // Determine winner (the other player)
            if (playerId == 1 && player2 != null) {
                broadcast(new GameMessage.GameEnd(2, player2.getName(),
                        player1Score, player2Score, "Opponent disconnected"));
            } else if (playerId == 2 && player1 != null) {
                broadcast(new GameMessage.GameEnd(1, player1.getName(), 
                        player1Score, player2Score, "Opponent disconnected"));
            }
        }
    }

    /**
     * Send game end message
     */
    public void sendGameEnd(boolean isDisconnect) {
        if (gameEnded) {
            int winnerId = player1Score > player2Score ? 1 : 2;
            String winnerName = winnerId == 1 ? player1.getName() : player2.getName();
            broadcast(new GameMessage.GameEnd(winnerId, winnerName, 
                    player1Score, player2Score, "Game completed"));
        }
    }

    // Getters
    public String getGameCode() {
        return gameCode;
    }

    public int[] getPuzzle() {
        return puzzle;
    }

    public Player getPlayer1() {
        return player1;
    }

    public Player getPlayer2() {
        return player2;
    }

    public int getPlayer1Score() {
        return player1Score;
    }

    public int getPlayer2Score() {
        return player2Score;
    }

    public boolean isGameStarted() {
        return gameStarted;
    }

    public boolean isGameEnded() {
        return gameEnded;
    }

    /**
     * Inner class to represent move results
     */
    public static class MoveResult {
        public final int playerId;
        public final int row;
        public final int col;
        public final int value;
        public final boolean success;
        public final String reason;

        public MoveResult(int playerId, int row, int col, int value, boolean success, String reason) {
            this.playerId = playerId;
            this.row = row;
            this.col = col;
            this.value = value;
            this.success = success;
            this.reason = reason;
        }
    }
}
