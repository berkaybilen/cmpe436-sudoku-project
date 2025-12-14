package org.sudoku.sudoku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.sudoku.sudoku.ui.GameListScreen
import org.sudoku.sudoku.ui.GameSession
import org.sudoku.sudoku.ui.MainScreen
import org.sudoku.sudoku.ui.SudokuScreen
import org.sudoku.sudoku.ui.WaitingScreen
import org.sudoku.sudoku.ui.theme.SudokuGameTheme
import org.sudoku.sudoku.viewmodel.ConnectionState
import org.sudoku.sudoku.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SudokuGameTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val gameViewModel: GameViewModel = viewModel()
                    val connectionState by gameViewModel.connectionState.collectAsState()
                    
                    // Navigation side effects based on connection state
                    LaunchedEffect(connectionState) {
                        android.util.Log.d("MainActivity", "ConnectionState changed to: $connectionState")
                        when (connectionState) {
                            is ConnectionState.WaitingForOpponent -> {
                                android.util.Log.d("MainActivity", "Current route: ${navController.currentDestination?.route}")
                                if (navController.currentDestination?.route != "waiting") {
                                    navController.navigate("waiting") {
                                        popUpTo("main") { inclusive = false }
                                    }
                                }
                            }
                            is ConnectionState.InGame -> {
                                android.util.Log.d("MainActivity", "Navigating to game screen from: ${navController.currentDestination?.route}")
                                // Always navigate to game, clearing waiting screen if needed
                                navController.navigate("game") {
                                    popUpTo("main") { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                            else -> {
                                // Handle disconnects or other states if needed
                            }
                        }
                    }

                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = "main",
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable("main") {
                                MainScreen(
                                    onNewGame = { playerData ->
                                        // Format: "playerName:difficulty"
                                        val parts = playerData.split(":")
                                        val playerName = parts[0]
                                        val difficulty = parts.getOrElse(1) { "EASY" }
                                        gameViewModel.createGame(playerName, difficulty)
                                    },
                                    onJoinGame = { playerName ->
                                        gameViewModel.refreshGames()
                                        navController.navigate("join/${playerName}") 
                                    }
                                )
                            }
                            
                            composable("waiting") {
                                val currentState = connectionState
                                val message = if (currentState is ConnectionState.WaitingForOpponent) {
                                    "Waiting for opponent...\nGame Code: ${currentState.gameCode}"
                                } else {
                                    "Connecting..."
                                }
                                
                                WaitingScreen(
                                    message = message,
                                    onBack = { 
                                        gameViewModel.exitGame()
                                        navController.popBackStack("main", inclusive = false)
                                    }
                                )
                            }
                            
                            composable("join/{playerName}") { backStackEntry ->
                                val playerName = backStackEntry.arguments?.getString("playerName") ?: "Player"
                                val availableGames by gameViewModel.availableGames.collectAsState()
                                
                                GameListScreen(
                                    availableGames = availableGames,
                                    onGameSelected = { game ->
                                        gameViewModel.joinGame(game.id, playerName)
                                    },
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            
                            composable("game") {
                                SudokuScreen(
                                    viewModel = gameViewModel,
                                    onExitGame = {
                                        navController.popBackStack("main", inclusive = false)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
