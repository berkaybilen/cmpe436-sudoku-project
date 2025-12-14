package org.sudoku.sudoku.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sudoku.shared.model.Cell
import org.sudoku.shared.model.SudokuBoard

/**
 * Composable that renders the 9x9 Sudoku board
 */
@Composable
fun SudokuBoardView(
    board: SudokuBoard,
    selectedRow: Int?,
    selectedCol: Int?,
    currentPlayer: Int,
    onCellClick: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .aspectRatio(1f)
            .background(Color.Black) // Main border
            .padding(3.dp)
    ) {
        for (row in 0..8) {
            Row(
                modifier = Modifier.weight(1f)
            ) {
                for (col in 0..8) {
                    val cell = board.getCell(row, col)

                    // Calculate borders for 3x3 grid effect
                    val topBorder = if (row % 3 == 0 && row != 0) 2.dp else 0.5.dp
                    val leftBorder = if (col % 3 == 0 && col != 0) 2.dp else 0.5.dp
                    val bottomBorder = 0.5.dp
                    val rightBorder = 0.5.dp

                    CellView(
                        cell = cell,
                        isSelected = row == selectedRow && col == selectedCol,
                        onClick = { onCellClick(row, col) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(
                                top = topBorder,
                                start = leftBorder,
                                bottom = bottomBorder,
                                end = rightBorder
                            )
                    )
                }
            }
        }
    }
}

/**
 * Composable that renders a single cell
 */
@Composable
private fun CellView(
    cell: Cell,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isSelected -> Color(0xFFBBDEFB) // Light blue for selected
        cell.isLocked && cell.lockedBy == 1 -> Color(0xFFFFCDD2) // Light red for player 1
        cell.isLocked && cell.lockedBy == 2 -> Color(0xFFC8E6C9) // Light green for player 2
        else -> Color.White
    }

    val textColor = when {
        cell.isInitial -> Color.Black
        cell.isLocked -> Color(0xFF424242)
        else -> Color(0xFF1976D2)
    }

    Box(
        modifier = modifier
            .background(backgroundColor)
            .clickable(enabled = !cell.isInitial && !cell.isLocked) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (cell.value != 0) {
            Text(
                text = cell.value.toString(),
                fontSize = 24.sp,
                fontWeight = if (cell.isInitial) FontWeight.Bold else FontWeight.Normal,
                color = textColor,
                textAlign = TextAlign.Center
            )
        }
    }
}
