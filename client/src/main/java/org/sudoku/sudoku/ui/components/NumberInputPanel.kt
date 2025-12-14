package org.sudoku.sudoku.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Composable for number input controls (1-9, Clear, Write)
 */
@Composable
fun NumberInputPanel(
    onNumberClick: (Int) -> Unit,
    onClearClick: () -> Unit,
    onWriteClick: () -> Unit,
    isWriteEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // First row: 1-5
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            for (num in 1..5) {
                NumberButton(
                    number = num,
                    onClick = { onNumberClick(num) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Second row: 6-9
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            for (num in 6..9) {
                NumberButton(
                    number = num,
                    onClick = { onNumberClick(num) },
                    modifier = Modifier.weight(1f)
                )
            }
            // Empty space to balance the row
            Spacer(modifier = Modifier.weight(1f))
        }

        // Third row: Clear and Write buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            Button(
                onClick = onClearClick,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF5252)
                )
            ) {
                Text("Clear", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onWriteClick,
                enabled = isWriteEnabled,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Text("Write", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Individual number button
 */
@Composable
private fun NumberButton(
    number: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primary)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number.toString(),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}
