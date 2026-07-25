package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.ParticleVictoryOverlay
import com.example.ui.components.SynthwaveBackground
import com.example.ui.screens.GameScreen
import com.example.ui.screens.MenuScreen
import com.example.ui.screens.OnlineRoomDialog
import com.example.ui.screens.ResultDialog
import com.example.ui.screens.SettingsDialog
import com.example.ui.theme.NeonTicTacToeTheme
import com.example.viewmodel.GameViewModel

enum class Screen {
    MENU, GAME
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            NeonTicTacToeTheme {
                val viewModel: GameViewModel = viewModel()
                val gameState by viewModel.gameState.collectAsStateWithLifecycle()

                var currentScreen by remember { mutableStateOf(Screen.MENU) }
                var showSettingsDialog by remember { mutableStateOf(false) }
                var showOnlineRoomDialog by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    viewModel.soundManager.startMusic()
                }

                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        when (event) {
                            Lifecycle.Event.ON_PAUSE -> viewModel.soundManager.pauseMusic()
                            Lifecycle.Event.ON_RESUME -> viewModel.soundManager.resumeMusic()
                            else -> {}
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    SynthwaveBackground {
                        when (currentScreen) {
                            Screen.MENU -> {
                                MenuScreen(
                                    currentGridSize = gameState.gridSize,
                                    onSelectMode = { mode ->
                                        viewModel.setGameMode(mode)
                                        currentScreen = Screen.GAME
                                    },
                                    onSelectGridSize = { size ->
                                        viewModel.setGridSize(size)
                                    },
                                    onOpenSettings = {
                                        showSettingsDialog = true
                                    },
                                    onOpenOnlineRoom = {
                                        showOnlineRoomDialog = true
                                    }
                                )
                            }
                            Screen.GAME -> {
                                GameScreen(
                                    gameState = gameState,
                                    isSoundEnabled = viewModel.soundManager.isSoundEnabled,
                                    onToggleSound = {
                                        viewModel.toggleSound(!viewModel.soundManager.isSoundEnabled)
                                    },
                                    onToggleMic = {
                                        viewModel.toggleMicrophone()
                                    },
                                    onCellClick = { index ->
                                        viewModel.onUserCellClick(index)
                                    },
                                    onUndoClick = {
                                        viewModel.undoMove()
                                    },
                                    onHintClick = {
                                        viewModel.provideHint()
                                    },
                                    onRestartClick = {
                                        viewModel.restartRound()
                                    },
                                    onBackClick = {
                                        currentScreen = Screen.MENU
                                    },
                                    onOpenSettings = {
                                        showSettingsDialog = true
                                    }
                                )
                            }
                        }
                    }

                    // Online Multiplayer Room Creation/Join Modal Dialog
                    if (showOnlineRoomDialog) {
                        OnlineRoomDialog(
                            onHostRoom = { onCodeGenerated ->
                                viewModel.createOnlineRoom { code ->
                                    onCodeGenerated(code)
                                    currentScreen = Screen.GAME
                                }
                            },
                            onJoinRoom = { code, onSuccess, onError ->
                                viewModel.joinOnlineRoom(
                                    roomCode = code,
                                    onSuccess = {
                                        onSuccess()
                                        currentScreen = Screen.GAME
                                    },
                                    onError = onError
                                )
                            },
                            onDismiss = {
                                showOnlineRoomDialog = false
                            }
                        )
                    }

                    // Settings Modal Dialog
                    if (showSettingsDialog) {
                        SettingsDialog(
                            isSoundEnabled = viewModel.soundManager.isSoundEnabled,
                            isMusicEnabled = viewModel.soundManager.isMusicEnabled,
                            isHapticsEnabled = viewModel.soundManager.isHapticsEnabled,
                            currentDifficulty = gameState.aiDifficulty,
                            currentGridSize = gameState.gridSize,
                            onToggleSound = { enabled ->
                                viewModel.toggleSound(enabled)
                            },
                            onToggleMusic = { enabled ->
                                viewModel.toggleMusic(enabled)
                            },
                            onToggleHaptics = { enabled ->
                                viewModel.toggleHaptics(enabled)
                            },
                            onSelectDifficulty = { diff ->
                                viewModel.setDifficulty(diff)
                            },
                            onSelectGridSize = { size ->
                                viewModel.setGridSize(size)
                            },
                            onResetScores = {
                                viewModel.resetScores()
                            },
                            onDismiss = {
                                showSettingsDialog = false
                            }
                        )
                    }

                    // Game Over Result Modal Dialog & Particle Burst Overlay
                    if (gameState.isGameOver) {
                        ParticleVictoryOverlay(
                            isGameOver = gameState.isGameOver,
                            winner = gameState.winner,
                            isDraw = gameState.isDraw,
                            modifier = Modifier.fillMaxSize()
                        )

                        ResultDialog(
                            winner = gameState.winner,
                            isDraw = gameState.isDraw,
                            gameMode = gameState.gameMode,
                            playerOScore = gameState.playerOScore,
                            playerXScore = gameState.playerXScore,
                            onPlayAgain = {
                                viewModel.restartRound()
                            },
                            onMainMenu = {
                                viewModel.restartRound()
                                currentScreen = Screen.MENU
                            }
                        )
                    }
                }
            }
        }
    }
}
