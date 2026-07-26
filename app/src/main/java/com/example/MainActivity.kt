package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.logic.GameMode
import com.example.ui.components.ParticleVictoryOverlay
import com.example.ui.components.SynthwaveBackground
import com.example.ui.screens.GameScreen
import com.example.ui.screens.MenuScreen
import com.example.ui.screens.OnlineRoomDialog
import com.example.ui.screens.ResultDialog
import com.example.ui.screens.SettingsDialog
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.*
import com.example.viewmodel.GameViewModel

enum class Screen {
    SPLASH, MENU, GAME
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            NeonTicTacToeTheme {
                val context = LocalContext.current
                val viewModel: GameViewModel = viewModel()
                val gameState by viewModel.gameState.collectAsStateWithLifecycle()

                var currentScreen by remember { mutableStateOf(Screen.SPLASH) }
                var showSettingsDialog by remember { mutableStateOf(false) }
                var showOnlineRoomDialog by remember { mutableStateOf(false) }
                var showMicPermissionDialog by remember { mutableStateOf(false) }

                val micPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        viewModel.toggleMicrophone()
                    } else {
                        showMicPermissionDialog = true
                    }
                }

                val checkMicPermissionGranted = {
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                }

                val requestMicAndToggle = {
                    if (checkMicPermissionGranted()) {
                        viewModel.toggleMicrophone()
                    } else {
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }

                val openAppSettings = {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    )
                    context.startActivity(intent)
                }

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
                            Screen.SPLASH -> {
                                SplashScreen(
                                    onSplashFinished = {
                                        currentScreen = Screen.MENU
                                    }
                                )
                            }
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
                                        requestMicAndToggle()
                                    },
                                    onToggleChat = {
                                        viewModel.toggleChatWindow()
                                    },
                                    onSendChatMessage = { msg ->
                                        viewModel.sendChatMessage(msg)
                                    },
                                    onDismissChatToast = {
                                        viewModel.clearChatToast()
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
                                        if (gameState.gameMode == GameMode.ONLINE_MULTIPLAYER) {
                                            viewModel.leaveOnlineRoom()
                                        }
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
                            initialGridSize = gameState.gridSize,
                            onHostRoom = { selectedGridSize, hostName, onCodeGenerated ->
                                viewModel.createOnlineRoom(gridSize = selectedGridSize, hostName = hostName) { code ->
                                    onCodeGenerated(code)
                                    currentScreen = Screen.GAME
                                }
                            },
                            onJoinRoom = { code, guestName, onSuccess, onError ->
                                viewModel.joinOnlineRoom(
                                    roomCode = code,
                                    guestName = guestName,
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
                            playerOName = gameState.playerOName,
                            playerXName = gameState.playerXName,
                            myOnlineSymbol = gameState.myOnlineSymbol,
                            onPlayAgain = {
                                viewModel.restartRound()
                            },
                            onMainMenu = {
                                if (gameState.gameMode == GameMode.ONLINE_MULTIPLAYER) {
                                    viewModel.leaveOnlineRoom()
                                }
                                viewModel.restartRound()
                                currentScreen = Screen.MENU
                            }
                        )
                    }

                    // Microphone Permission Dialog
                    if (showMicPermissionDialog) {
                        Dialog(onDismissRequest = { showMicPermissionDialog = false }) {
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = NeonBackgroundCard,
                                border = androidx.compose.foundation.BorderStroke(2.dp, NeonBoardBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Rounded.Mic,
                                                contentDescription = null,
                                                tint = NeonPlayerCyan,
                                                modifier = Modifier.size(28.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "MICROPHONE PERMISSION",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color.White
                                            )
                                        }
                                        IconButton(onClick = { showMicPermissionDialog = false }) {
                                            Icon(
                                                imageVector = Icons.Rounded.Close,
                                                contentDescription = "Close",
                                                tint = Color.White.copy(alpha = 0.7f)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        text = "माइक्रोफोन अनुमति आवश्यक छ (Microphone Permission)",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonPlayerOrange,
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "अनलाइन गेममा साथीसँग बोल्न (Voice Chat) को लागि माइक्रोफोन अनुमति चाहिन्छ। पर्मिसन नभए 'ALLOW PERMISSION' वा 'OPEN APP SETTINGS' मा थिच्नुहोस्।",
                                        fontSize = 13.sp,
                                        color = Color.White.copy(alpha = 0.9f),
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(20.dp))

                                    Button(
                                        onClick = {
                                            showMicPermissionDialog = false
                                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonPlayerCyan),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                    ) {
                                        Icon(Icons.Rounded.Mic, contentDescription = null, tint = Color.Black)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "ALLOW PERMISSION",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedButton(
                                        onClick = {
                                            showMicPermissionDialog = false
                                            openAppSettings()
                                        },
                                        shape = RoundedCornerShape(16.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.5.dp, NeonPlayerOrange),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                    ) {
                                        Icon(Icons.Rounded.Settings, contentDescription = null, tint = NeonPlayerOrange)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "OPEN APP SETTINGS",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NeonPlayerOrange
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
