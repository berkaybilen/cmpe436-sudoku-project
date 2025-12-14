package org.sudoku.sudoku

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SudokuAppTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testNewGameButton() {
        // Check if New Game button exists and is displayed
        composeTestRule.onNodeWithText("New Game").assertExists()
        
        // Perform a click
        composeTestRule.onNodeWithText("New Game").performClick()
    }
}
