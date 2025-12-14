package org.sudoku.shared.model

/**
 * Represents a 9x9 Sudoku board with support for 2-player simultaneous play.
 */
class SudokuBoard {
    // 9x9 grid of cells
    private val cells: Array<Array<Cell>> = Array(9) { Array(9) { Cell() } }

    /**
     * Get the cell at the specified position
     */
    fun getCell(row: Int, col: Int): Cell {
        require(row in 0..8 && col in 0..8) { "Invalid position: ($row, $col)" }
        return cells[row][col]
    }

    /**
     * Set the cell at the specified position
     */
    fun setCell(row: Int, col: Int, cell: Cell) {
        require(row in 0..8 && col in 0..8) { "Invalid position: ($row, $col)" }
        cells[row][col] = cell
    }

    /**
     * Update the value of a cell if it's editable
     * @return true if update was successful, false if cell is not editable
     */
    fun updateCell(row: Int, col: Int, value: Int, playerId: Int = 0): Boolean {
        val cell = getCell(row, col)
        if (!cell.isEditable()) return false

        setCell(row, col, cell.withValue(value))
        return true
    }

    /**
     * Lock a cell by a player
     * @return true if lock was successful, false if cell is not editable
     */
    fun lockCell(row: Int, col: Int, playerId: Int): Boolean {
        val cell = getCell(row, col)
        if (!cell.isEditable() || cell.isEmpty()) return false

        setCell(row, col, cell.lockBy(playerId))
        return true
    }

    /**
     * Check if a value is valid for the given position according to Sudoku rules
     */
    fun isValidMove(row: Int, col: Int, value: Int): Boolean {
        if (value == 0) return true // Empty is always valid

        // Check row
        for (c in 0..8) {
            if (c != col && cells[row][c].value == value) return false
        }

        // Check column
        for (r in 0..8) {
            if (r != row && cells[r][col].value == value) return false
        }

        // Check 3x3 box
        val boxStartRow = (row / 3) * 3
        val boxStartCol = (col / 3) * 3
        for (r in boxStartRow until boxStartRow + 3) {
            for (c in boxStartCol until boxStartCol + 3) {
                if (r != row && c != col && cells[r][c].value == value) return false
            }
        }

        return true
    }

    /**
     * Check if the board is completely filled (not necessarily correct)
     */
    fun isFilled(): Boolean {
        for (row in 0..8) {
            for (col in 0..8) {
                if (cells[row][col].isEmpty()) return false
            }
        }
        return true
    }

    /**
     * Check if the board is solved correctly
     */
    fun isSolved(): Boolean {
        if (!isFilled()) return false

        for (row in 0..8) {
            for (col in 0..8) {
                val value = cells[row][col].value
                if (!isValidMove(row, col, value)) return false
            }
        }
        return true
    }

    /**
     * Get all cells as a 2D array (for iteration)
     */
    fun getAllCells(): Array<Array<Cell>> = cells.map { it.copyOf() }.toTypedArray()

    /**
     * Initialize the board with a puzzle (array of 81 values, row-by-row)
     */
    fun initializeWithPuzzle(puzzle: IntArray) {
        require(puzzle.size == 81) { "Puzzle must have exactly 81 values" }

        var index = 0
        for (row in 0..8) {
            for (col in 0..8) {
                val value = puzzle[index++]
                if (value != 0) {
                    cells[row][col] = Cell(value = value, isInitial = true)
                } else {
                    cells[row][col] = Cell()
                }
            }
        }
    }

    /**
     * Clear all non-initial cells
     */
    fun reset() {
        for (row in 0..8) {
            for (col in 0..8) {
                val cell = cells[row][col]
                if (!cell.isInitial) {
                    cells[row][col] = Cell()
                }
            }
        }
    }

    /**
     * Count how many cells are filled (excluding initial cells if countInitial is false)
     */
    fun getFilledCount(countInitial: Boolean = true): Int {
        var count = 0
        for (row in 0..8) {
            for (col in 0..8) {
                val cell = cells[row][col]
                if (!cell.isEmpty() && (countInitial || !cell.isInitial)) {
                    count++
                }
            }
        }
        return count
    }

    /**
     * Get count of cells locked by a specific player
     */
    fun getLockedCountByPlayer(playerId: Int): Int {
        var count = 0
        for (row in 0..8) {
            for (col in 0..8) {
                if (cells[row][col].lockedBy == playerId) {
                    count++
                }
            }
        }
        return count
    }
}
