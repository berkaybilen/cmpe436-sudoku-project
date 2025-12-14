package org.sudoku.sudoku.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MainScreen(
    onNewGame: (String) -> Unit, // Takes player name
    onJoinGame: (String) -> Unit, // Takes player name
    isConnecting: Boolean = false
) {
    var playerName by remember { mutableStateOf("Player") }
    var showNameInput by remember { mutableStateOf(false) }
    var isJoinMode by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Sudoku Battle",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 48.dp)
        )

        if (showNameInput) {
            Text("Enter your name:", modifier = Modifier.padding(bottom = 8.dp))
            OutlinedTextField(
                value = playerName,
                onValueChange = { playerName = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Difficulty selection for new games
            if (!isJoinMode) {
                var selectedDifficulty by remember { mutableStateOf("EASY") }
                
                Text("Select Difficulty:", modifier = Modifier.padding(bottom = 8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { selectedDifficulty = "EASY" },
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                    ) {
                        Text(if (selectedDifficulty == "EASY") "✓ Easy" else "Easy")
                    }
                    Button(
                        onClick = { selectedDifficulty = "MEDIUM" },
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                    ) {
                        Text(if (selectedDifficulty == "MEDIUM") "✓ Medium" else "Medium")
                    }
                    Button(
                        onClick = { selectedDifficulty = "HARD" },
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                    ) {
                        Text(if (selectedDifficulty == "HARD") "✓ Hard" else "Hard")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (isConnecting) {
                    CircularProgressIndicator()
                } else {
                    Button(
                        onClick = { onNewGame("$playerName:$selectedDifficulty") },
                        enabled = playerName.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = 32.dp)
                    ) {
                        Text("Create Game", fontSize = 18.sp)
                    }
                }
            } else {
                // Join game mode - no difficulty selection

                if (isConnecting) {
                    CircularProgressIndicator()
                } else {
                    Button(
                        onClick = { onJoinGame(playerName) },
                        enabled = playerName.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = 32.dp)
                    ) {
                        Text("Find Games", fontSize = 18.sp)
                    }
                }
            }
        } else {
            Button(
                onClick = { 
                    isJoinMode = false
                    showNameInput = true 
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 32.dp)
            ) {
                Text("New Game", fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { 
                    isJoinMode = true
                    showNameInput = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 32.dp)
            ) {
                Text("Join Game", fontSize = 18.sp)
            }
        }
    }
}
