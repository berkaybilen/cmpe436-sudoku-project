package org.sudoku.shared.model

/**
 * Represents a single cell in the Sudoku board.
 *
 * @property value The current value (1-9), or 0 if empty
 * @property isInitial True if this is a given number (puzzle clue), false if player-filled
 * @property isLocked True if this cell has been locked by a player
 * @property lockedBy Player ID who locked this cell (1 or 2), or 0 if not locked
 */
data class Cell(
    val value: Int = 0,
    val isInitial: Boolean = false,
    val isLocked: Boolean = false,
    val lockedBy: Int = 0
) {
    /**
     * Check if this cell is empty
     */
    fun isEmpty(): Boolean = value == 0

    /**
     * Check if this cell can be modified (not initial and not locked)
     */
    fun isEditable(): Boolean = !isInitial && !isLocked

    /**
     * Create a copy with a new value
     */
    fun withValue(newValue: Int): Cell = copy(value = newValue)

    /**
     * Create a copy that is locked by a player
     */
    fun lockBy(playerId: Int): Cell = copy(isLocked = true, lockedBy = playerId)
}
