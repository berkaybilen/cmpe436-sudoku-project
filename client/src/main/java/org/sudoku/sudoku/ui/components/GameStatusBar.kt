package org.sudoku.sudoku.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Status bar showing scores and system messages
 */
@Composable
fun GameStatusBar(
    player1Name: String,
    player2Name: String,
    player1Score: Int,
    player2Score: Int,
    lastMessage: String?,
    lastMessageIsError: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Player 1 Score
        PlayerScoreCard(
            playerName = player1Name,
            score = player1Score,
            color = Color(0xFFEF5350)  // Red
        )

        // Message Center
        if (lastMessage != null) {
            Text(
                text = lastMessage,
                color = if (lastMessageIsError) Color.Red else Color.Green,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp).wrapContentWidth(Alignment.CenterHorizontally)
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        // Player 2 Score
        PlayerScoreCard(
            playerName = player2Name,
            score = player2Score,
            color = Color(0xFF66BB6A)  // Green
        )
    }
}

/**
 * Individual player score card
 */
@Composable
private fun PlayerScoreCard(
    playerName: String,
    score: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = playerName,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = "$score",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
