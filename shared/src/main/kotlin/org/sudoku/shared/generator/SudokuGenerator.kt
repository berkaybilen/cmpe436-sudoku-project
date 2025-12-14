package org.sudoku.shared.generator

import kotlin.random.Random

/**
 * Generates valid Sudoku puzzles with different difficulty levels.
 */
object SudokuGenerator {

    /**
     * Data class to hold both puzzle and its solution
     */
    data class PuzzleWithSolution(
        val puzzle: IntArray,
        val solution: IntArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is PuzzleWithSolution) return false
            if (!puzzle.contentEquals(other.puzzle)) return false
            if (!solution.contentEquals(other.solution)) return false
            return true
        }

        override fun hashCode(): Int {
            var result = puzzle.contentHashCode()
            result = 31 * result + solution.contentHashCode()
            return result
        }
    }

    enum class Difficulty(val cluesCount: Int) {
        EASY(70),      // More clues = easier
        MEDIUM(40),    // Medium amount of clues
        HARD(25)       // Fewer clues = harder
    }

    /**
     * Generate a complete valid Sudoku solution
     */
    fun generateSolution(): IntArray {
        val board = Array(9) { IntArray(9) }
        fillBoard(board)
        return board.flatMap { it.toList() }.toIntArray()
    }

    /**
     * Generate a Sudoku puzzle with its solution
     * @return PuzzleWithSolution containing both the puzzle and its matching solution
     */
    fun generatePuzzle(difficulty: Difficulty = Difficulty.EASY): PuzzleWithSolution {
        val solution = generateSolution()
        val puzzle = solution.copyOf()

        // Calculate how many cells to remove
        val cellsToRemove = 81 - difficulty.cluesCount
        val positions = (0 until 81).shuffled(Random).take(cellsToRemove)

        positions.forEach { puzzle[it] = 0 }

        return PuzzleWithSolution(puzzle, solution)
    }

    /**
     * Fill the board using backtracking algorithm
     */
    private fun fillBoard(board: Array<IntArray>): Boolean {
        for (row in 0..8) {
            for (col in 0..8) {
                if (board[row][col] == 0) {
                    // Try numbers 1-9 in random order
                    val numbers = (1..9).shuffled(Random)
                    for (num in numbers) {
                        if (isValidPlacement(board, row, col, num)) {
                            board[row][col] = num
                            if (fillBoard(board)) {
                                return true
                            }
                            board[row][col] = 0
                        }
                    }
                    return false
                }
            }
        }
        return true
    }

    /**
     * Check if placing a number at a position is valid
     */
    private fun isValidPlacement(board: Array<IntArray>, row: Int, col: Int, num: Int): Boolean {
        // Check row
        for (c in 0..8) {
            if (board[row][c] == num) return false
        }

        // Check column
        for (r in 0..8) {
            if (board[r][col] == num) return false
        }

        // Check 3x3 box
        val boxStartRow = (row / 3) * 3
        val boxStartCol = (col / 3) * 3
        for (r in boxStartRow until boxStartRow + 3) {
            for (c in boxStartCol until boxStartCol + 3) {
                if (board[r][c] == num) return false
            }
        }

        return true
    }

    /**
     * Generate an easy puzzle (for testing)
     */
    fun generateEasyPuzzle(): PuzzleWithSolution = generatePuzzle(Difficulty.EASY)

    /**
     * Generate a sample puzzle (hardcoded for quick testing)
     */
    fun getSamplePuzzle(): IntArray {
        return intArrayOf(
            5, 3, 0, 0, 7, 0, 0, 0, 0,
            6, 0, 0, 1, 9, 5, 0, 0, 0,
            0, 9, 8, 0, 0, 0, 0, 6, 0,
            8, 0, 0, 0, 6, 0, 0, 0, 3,
            4, 0, 0, 8, 0, 3, 0, 0, 1,
            7, 0, 0, 0, 2, 0, 0, 0, 6,
            0, 6, 0, 0, 0, 0, 2, 8, 0,
            0, 0, 0, 4, 1, 9, 0, 0, 5,
            0, 0, 0, 0, 8, 0, 0, 7, 9
        )
    }
}
