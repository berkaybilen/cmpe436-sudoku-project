package org.sudoku.sudoku.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class GameSession(
    val id: String,
    val name: String,
    val hostName: String
)

@Composable
fun GameListScreen(
    availableGames: List<GameSession>,
    onGameSelected: (GameSession) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Available Games",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            
            Button(onClick = onBack) {
                Text("Back")
            }
        }

        if (availableGames.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No games available")
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(availableGames) { game ->
                    GameListItem(
                        game = game,
                        onClick = { onGameSelected(game) }
                    )
                }
            }
        }
    }
}

@Composable
fun GameListItem(
    game: GameSession,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            Text(
                text = game.name,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Text(
                text = "Host: ${game.hostName}",
                fontSize = 14.sp
            )
        }
    }
}
