package org.sudoku.sudoku.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.sudoku.sudoku.ui.components.GameStatusBar
import org.sudoku.sudoku.ui.components.NumberInputPanel
import org.sudoku.sudoku.ui.components.SudokuBoardView
import org.sudoku.sudoku.viewmodel.GameViewModel

/**
 * Main Sudoku game screen
 */
@Composable
fun SudokuScreen(
    modifier: Modifier = Modifier,
    viewModel: GameViewModel = viewModel(),
    onExitGame: () -> Unit
) {
    val gameState by viewModel.gameState.collectAsState()

    // Show game complete dialog
    if (gameState.isGameComplete) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Game Complete!") },
            text = {
                Column {
                    Text("Congratulations! The puzzle is solved!")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("${gameState.winnerName ?: "Someone"} won!")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${gameState.player1Name}: ${gameState.player1Score} points")
                    Text("${gameState.player2Name}: ${gameState.player2Score} points")
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.exitGame() // Notify server on exit
                    onExitGame()
                }) {
                    Text("Back to Menu")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with Title and Exit button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Sudoku",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            
            Button(onClick = {
                viewModel.exitGame() // Notify server on exit
                onExitGame()
            }) {
                Text("Exit")
            }
        }

        // Status bar
        GameStatusBar(
            player1Name = gameState.player1Name,
            player2Name = gameState.player2Name,
            player1Score = gameState.player1Score,
            player2Score = gameState.player2Score,
            lastMessage = gameState.lastMessage,
            lastMessageIsError = gameState.lastMessageIsError
        )

        // Sudoku Board
        key(gameState.boardVersion) {
            SudokuBoardView(
                board = gameState.board,
                selectedRow = gameState.selectedRow,
                selectedCol = gameState.selectedCol,
                currentPlayer = gameState.currentPlayer,
                onCellClick = { row, col ->
                    viewModel.selectCell(row, col)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        }

        // Number Input Panel
        NumberInputPanel(
            onNumberClick = { number ->
                viewModel.inputNumber(number)
            },
            onClearClick = {
                viewModel.clearSelectedCell()
            },
            onWriteClick = {
                viewModel.writeSelectedCell()
            },
            isWriteEnabled = gameState.selectedRow != null &&
                    gameState.selectedCol != null &&
                    gameState.board.getCell(
                        gameState.selectedRow!!,
                        gameState.selectedCol!!
                    ).value != 0,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}
