package org.sudoku.sudoku.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.sudoku.shared.model.SudokuBoard
import org.sudoku.shared.protocol.GameMessage
import org.sudoku.sudoku.network.GameClient
import org.sudoku.sudoku.ui.GameSession

class GameViewModel : ViewModel() {
    private val client = GameClient()

    // UI States
    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _availableGames = MutableStateFlow<List<GameSession>>(emptyList())
    val availableGames: StateFlow<List<GameSession>> = _availableGames.asStateFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private var myPlayerId: Int = -1
    
    init {
        connect()
    }

    private fun connect() {
        viewModelScope.launch(Dispatchers.IO) {
            _connectionState.value = ConnectionState.Connecting
            
            // Start listening for messages
            launch {
                client.messages.collect { message ->
                    handleMessage(message)
                }
            }
            
            // Connect
            client.connect()
        }
    }

    private fun handleMessage(message: GameMessage) {
        Log.d("GameViewModel", "Handling message: $message")
        when (message) {
            is GameMessage.CreateGameResponse -> {
                myPlayerId = message.playerId
                _connectionState.value = ConnectionState.WaitingForOpponent(message.gameCode)
            }
            
            is GameMessage.ListGamesResponse -> {
                _availableGames.value = message.games.map { 
                    GameSession(it.gameCode, "Game ${it.gameCode}", it.creator)
                }
            }
            
            is GameMessage.WaitingForPlayer -> {
                _connectionState.value = ConnectionState.WaitingForOpponent(message.gameCode)
            }
            
            is GameMessage.GameStart -> {
                android.util.Log.d("GameViewModel", "===== GAME START RECEIVED =====")
                android.util.Log.d("GameViewModel", "Player ID: ${message.yourPlayerId}")
                android.util.Log.d("GameViewModel", "Current state BEFORE: ${_connectionState.value}")
                
                myPlayerId = message.yourPlayerId
                val board = SudokuBoard()
                board.initializeWithPuzzle(message.puzzle)
                
                _gameState.value = GameState(
                    board = board,
                    currentPlayer = 1,
                    isGameComplete = false,
                    player1Name = message.player1Name,
                    player2Name = message.player2Name,
                    myPlayerId = myPlayerId,
                    boardVersion = 0
                )
                
                // Force state change to trigger navigation
                val newState = ConnectionState.InGame()
                _connectionState.value = newState
                
                android.util.Log.d("GameViewModel", "Current state AFTER: ${_connectionState.value}")
                android.util.Log.d("GameViewModel", "State is InGame: ${_connectionState.value is ConnectionState.InGame}")
            }
            
            is GameMessage.MoveResult -> {
                val state = _gameState.value
                
                if (message.isSuccess) {
                    val isMyMove = message.playerId == myPlayerId
                    
                    if (message.isCorrect) {
                        // ALWAYS update the cell value first (even if there's already a wrong value there)
                        state.board.updateCell(message.row, message.col, message.value, message.playerId)
                        state.board.lockCell(message.row, message.col, message.playerId)
                        
                        // Show success message if it was me
                        if (isMyMove) {
                            _gameState.value = state.copy(
                                lastMessage = "Correct!",
                                lastMessageIsError = false,
                                player1Score = message.player1Score,
                                player2Score = message.player2Score,
                                boardVersion = state.boardVersion + 1
                            )
                        } else {
                            // Opponent's move - just update scores and board version
                            _gameState.value = state.copy(
                                player1Score = message.player1Score,
                                player2Score = message.player2Score,
                                boardVersion = state.boardVersion + 1
                            )
                        }
                    } else {
                        // Wrong answer
                        
                        // If it was MY move, I already have the number on my screen from inputNumber().
                        // Since it's wrong, I should clear it.
                        if (isMyMove) {
                            state.board.updateCell(message.row, message.col, 0, message.playerId)
                            _gameState.value = state.copy(
                                lastMessage = "Wrong number!",
                                lastMessageIsError = true,
                                player1Score = message.player1Score,
                                player2Score = message.player2Score,
                                boardVersion = state.boardVersion + 1
                            )
                        } else {
                            // If it was the OPPONENT'S move and it was wrong,
                            // usually we don't see their wrong guesses on our board anyway 
                            // (unless we want to show it briefly).
                            // But if we did receive a value update for them previously (which we don't currently),
                            // we would clear it here.
                            // Since we don't show opponent's pending guesses, we just update the version
                            // to ensure any side effects are processed.
                            _gameState.value = state.copy(
                                player1Score = message.player1Score,
                                player2Score = message.player2Score,
                                boardVersion = state.boardVersion + 1
                            )
                        }
                    }
                } else {
                    // Move failed (e.g. invalid move or rejected)
                    Log.w("GameViewModel", "Move rejected: ${message.reason}")
                    if (message.playerId == myPlayerId) {
                        _gameState.value = state.copy(
                            lastMessage = message.reason ?: "Invalid Move",
                            lastMessageIsError = true,
                            player1Score = message.player1Score,
                            player2Score = message.player2Score
                        )
                    } else {
                        // Update scores even for opponent's failed moves
                        _gameState.value = state.copy(
                            player1Score = message.player1Score,
                            player2Score = message.player2Score,
                            boardVersion = state.boardVersion + 1
                        )
                    }
                }
            }
            
            is GameMessage.GameEnd -> {
                _gameState.value = _gameState.value.copy(
                    isGameComplete = true,
                    winnerName = message.winnerName
                )
            }
            
            is GameMessage.ErrorMessage -> {
                Log.e("GameViewModel", "Server error: ${message.message}")
                _gameState.value = _gameState.value.copy(
                    lastMessage = message.message,
                    lastMessageIsError = true
                )
            }
            
            is GameMessage.Pong -> {
                Log.d("GameViewModel", "Received PONG from server - connection alive")
            }
            
            else -> { Log.d("GameViewModel", "Unhandled message type") }
        }
    }

    // Actions
    
    fun createGame(playerName: String, difficulty: String = "EASY") {
        viewModelScope.launch {
            client.send(GameMessage.CreateGameRequest(playerName, difficulty))
        }
    }
    
    fun refreshGames() {
        viewModelScope.launch {
            client.send(GameMessage.ListGamesRequest())
        }
    }
    
    fun joinGame(gameCode: String, playerName: String) {
        viewModelScope.launch {
            client.send(GameMessage.JoinGameRequest(gameCode, playerName))
        }
    }

    fun exitGame() {
        val state = _gameState.value
        
        // Reset connection state to prevent auto-navigation back to waiting screen
        _connectionState.value = ConnectionState.Connecting
        
        if (state.myPlayerId != -1) {
            viewModelScope.launch {
                try {
                    client.send(GameMessage.ExitGameRequest(state.myPlayerId.toString()))
                } catch (e: Exception) {
                    Log.e("GameViewModel", "Error sending exit game request", e)
                }
            }
            
            // Reset game state
            _gameState.value = GameState()
            myPlayerId = -1
        }
    }

    fun selectCell(row: Int, col: Int) {
        val state = _gameState.value
        // Only select if it's not locked
        val cell = state.board.getCell(row, col)
        if (!cell.isLocked) {
            _gameState.value = state.copy(
                selectedRow = row,
                selectedCol = col
            )
        }
    }

    fun inputNumber(number: Int) {
        val state = _gameState.value
        val row = state.selectedRow ?: return
        val col = state.selectedCol ?: return
        
        // Optimistically update UI: Show number on board but DO NOT SEND to server yet
        // We only update our local board view for now
        state.board.updateCell(row, col, number, state.myPlayerId)
        // Trigger recomposition by creating a new GameState with incremented version
        _gameState.value = state.copy(
            selectedRow = row,
            selectedCol = col,
            boardVersion = state.boardVersion + 1
        )
    }

    fun clearSelectedCell() {
        val state = _gameState.value
        val row = state.selectedRow ?: return
        val col = state.selectedCol ?: return
        
        // Clear local cell
        state.board.updateCell(row, col, 0, state.myPlayerId)
        _gameState.value = state.copy(
             boardVersion = state.boardVersion + 1
        )
    }

    fun writeSelectedCell() {
        val state = _gameState.value
        val row = state.selectedRow ?: return
        val col = state.selectedCol ?: return
        
        val cell = state.board.getCell(row, col)
        val value = cell.value
        
        if (value != 0) {
            viewModelScope.launch {
                client.send(GameMessage.MoveRequest(row, col, value))
            }
            // Deselect after writing
            _gameState.value = state.copy(
                selectedRow = null,
                selectedCol = null,
                lastMessage = null // Clear previous messages
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            client.close()
        }
    }
}

data class GameState(
    val board: SudokuBoard = SudokuBoard(),
    val selectedRow: Int? = null,
    val selectedCol: Int? = null,
    val currentPlayer: Int = 1, // Kept for compatibility, but might not be needed for real-time
    val isGameComplete: Boolean = false,
    val myPlayerId: Int = -1,
    val player1Name: String = "Player 1",
    val player2Name: String = "Player 2",
    val player1Score: Int = 0,
    val player2Score: Int = 0,
    val winnerName: String? = null,
    val lastMessage: String? = null,
    val lastMessageIsError: Boolean = false,
    val boardVersion: Int = 0 // Added to force recomposition
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GameState) return false
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }
}

sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    data class WaitingForOpponent(val gameCode: String) : ConnectionState()
    data class InGame(val timestamp: Long = System.currentTimeMillis()) : ConnectionState()
}
