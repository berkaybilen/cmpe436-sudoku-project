package org.sudoku.shared.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Sealed class hierarchy representing messages exchanged between client and server
 * for the 2-player Sudoku game.
 */
@Serializable
sealed class GameMessage {

    // ===== Matchmaking Messages =====

    /**
     * Client requests to create a new game
     */
    @Serializable
    @SerialName("CreateGameRequest")
    data class CreateGameRequest(
        val playerName: String,
        val difficulty: String = "EASY" // EASY, MEDIUM, HARD
    ) : GameMessage()

    /**
     * Server responds with game code
     */
    @Serializable
    @SerialName("CreateGameResponse")
    data class CreateGameResponse(val gameCode: String, val playerId: Int) : GameMessage()

    /**
     * Client requests to join an existing game
     */
    @Serializable
    @SerialName("JoinGameRequest")
    data class JoinGameRequest(val gameCode: String, val playerName: String) : GameMessage()

    /**
     * Server informs client they're waiting for opponent
     */
    @Serializable
    @SerialName("WaitingForPlayer")
    data class WaitingForPlayer(val gameCode: String) : GameMessage()

    /**
     * Server lists available games
     */
    @Serializable
    @SerialName("ListGamesRequest")
    data class ListGamesRequest(val dummy: String = "") : GameMessage()

    /**
     * Server responds with list of waiting games
     */
    @Serializable
    @SerialName("ListGamesResponse")
    data class ListGamesResponse(val games: List<GameInfo>) : GameMessage() {
        @Serializable
        data class GameInfo(val gameCode: String, val creator: String)
    }

    // ===== Game Lifecycle Messages =====

    /**
     * Game starts with a puzzle
     */
    @Serializable
    @SerialName("GameStart")
    data class GameStart(
        val puzzle: IntArray,
        val player1Name: String,
        val player2Name: String,
        val yourPlayerId: Int
    ) : GameMessage() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is GameStart) return false
            if (!puzzle.contentEquals(other.puzzle)) return false
            if (player1Name != other.player1Name) return false
            if (player2Name != other.player2Name) return false
            if (yourPlayerId != other.yourPlayerId) return false
            return true
        }

        override fun hashCode(): Int {
            var result = puzzle.contentHashCode()
            result = 31 * result + player1Name.hashCode()
            result = 31 * result + player2Name.hashCode()
            result = 31 * result + yourPlayerId
            return result
        }
    }

    // ===== Move Messages =====

    /**
     * Player requests to make a move
     */
    @Serializable
    @SerialName("MoveRequest")
    data class MoveRequest(
        val row: Int,
        val col: Int,
        val value: Int
    ) : GameMessage()

    /**
     * Server responds to a move attempt
     */
    @Serializable
    @SerialName("MoveResult")
    data class MoveResult(
        val playerId: Int,
        val row: Int,
        val col: Int,
        val value: Int,
        val isSuccess: Boolean,
        val isCorrect: Boolean = false,
        val reason: String? = null,
        val player1Score: Int = 0,
        val player2Score: Int = 0
    ) : GameMessage()

    // ===== Game End Messages =====

    /**
     * Game ended
     */
    @Serializable
    @SerialName("GameEnd")
    data class GameEnd(
        val winnerId: Int,
        val winnerName: String,
        val player1Score: Int,
        val player2Score: Int,
        val reason: String = "Game completed"
    ) : GameMessage()

    /**
     * Player disconnected
     */
    @Serializable
    @SerialName("PlayerDisconnect")
    data class PlayerDisconnect(val playerId: Int, val playerName: String) : GameMessage()

    /**
     * Player voluntarily exits game (returns to menu)
     */
    @Serializable
    @SerialName("ExitGameRequest")
    data class ExitGameRequest(val dummy: String = "") : GameMessage()

    // ===== Error Messages =====

    /**
     * Generic error message
     */
    @Serializable
    @SerialName("ErrorMessage")
    data class ErrorMessage(val message: String) : GameMessage()

    // ===== Keep-Alive Messages =====

    /**
     * Keep-alive ping
     */
    @Serializable
    @SerialName("Ping")
    object Ping : GameMessage()

    /**
     * Keep-alive pong
     */
    @Serializable
    @SerialName("Pong")
    object Pong : GameMessage()

    // Deprecated messages (kept for compatibility)
    @Deprecated("Use MoveRequest instead")
    @Serializable
    data class CellUpdate(
        val playerId: Int,
        val row: Int,
        val col: Int,
        val value: Int
    ) : GameMessage()

    @Deprecated("Locking handled server-side")
    @Serializable
    data class CellLock(
        val playerId: Int,
        val row: Int,
        val col: Int
    ) : GameMessage()

    @Deprecated("Use ErrorMessage instead")
    @Serializable
    data class ActionRejected(
        val playerId: Int,
        val reason: String
    ) : GameMessage()

    @Deprecated("Use GameStart instead")
    @Serializable
    data class PlayerJoin(val playerId: Int, val playerName: String) : GameMessage()
}
