package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*

@Composable
fun OnlineRoomDialog(
    initialGridSize: Int = 3,
    onHostRoom: (gridSize: Int, hostName: String, onCodeGenerated: (String) -> Unit) -> Unit,
    onJoinRoom: (roomCode: String, guestName: String, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedGridSize by remember { mutableIntStateOf(if (initialGridSize in 3..7) initialGridSize else 3) }
    var hostNameInput by remember { mutableStateOf("Player 1") }
    var guestNameInput by remember { mutableStateOf("Player 2") }
    var roomCodeInput by remember { mutableStateOf("") }
    var generatedCode by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isJoining by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = NeonBackgroundCard,
            border = androidx.compose.foundation.BorderStroke(2.dp, NeonBoardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Wifi,
                            contentDescription = null,
                            tint = NeonPlayerCyan,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ONLINE ROOM",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (generatedCode == null) {
                    // Board Option Selection
                    Text(
                        text = "SELECT BOARD OPTION",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = NeonTextMuted,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(3, 4, 5, 6, 7).forEach { size ->
                            val isSelected = selectedGridSize == size
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) NeonPlayerCyan.copy(alpha = 0.25f) else Color(0x33140D2E))
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) NeonPlayerCyan else NeonButtonBorder.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedGridSize = size }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${size}x${size}",
                                    color = if (isSelected) NeonPlayerCyan else Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Option 1: Create Room
                    OutlinedTextField(
                        value = hostNameInput,
                        onValueChange = { hostNameInput = it },
                        label = { Text("Your Name (Host)", color = NeonTextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonPlayerOrange,
                            unfocusedBorderColor = NeonButtonBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            onHostRoom(selectedGridSize, hostNameInput) { code ->
                                generatedCode = code
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPlayerOrange),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(Icons.Rounded.AddCircle, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CREATE ${selectedGridSize}x${selectedGridSize} ROOM",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "— OR —",
                        color = NeonTextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Option 2: Join Room with 3-digit code
                    OutlinedTextField(
                        value = guestNameInput,
                        onValueChange = { guestNameInput = it },
                        label = { Text("Your Name (Guest)", color = NeonTextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonPlayerCyan,
                            unfocusedBorderColor = NeonButtonBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = roomCodeInput,
                        onValueChange = { if (it.length <= 3) roomCodeInput = it },
                        label = { Text("Enter 3-Digit Room Code", color = NeonTextMuted) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonPlayerCyan,
                            unfocusedBorderColor = NeonButtonBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            if (roomCodeInput.length == 3) {
                                isJoining = true
                                errorMessage = null
                                onJoinRoom(
                                    roomCodeInput,
                                    guestNameInput,
                                    {
                                        isJoining = false
                                        onDismiss()
                                    },
                                    { err ->
                                        isJoining = false
                                        errorMessage = err
                                    }
                                )
                            } else {
                                errorMessage = "Please enter a valid 3-digit code"
                            }
                        },
                        enabled = roomCodeInput.length == 3 && !isJoining,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPlayerCyan),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        if (isJoining) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Rounded.Login, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "JOIN ROOM (Player B)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = errorMessage!!,
                            color = Color(0xFFFF5555),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    // Room Created - Displaying 3-digit code to share
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 12.dp)
                    ) {
                        Text(
                            text = "ROOM CREATED (${selectedGridSize}x${selectedGridSize})!",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = NeonPlayerCyan
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Share this 3-digit code with Player B:",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF1B0B3B))
                                .border(2.dp, NeonPlayerOrange, RoundedCornerShape(16.dp))
                                .padding(horizontal = 32.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = generatedCode!!,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Black,
                                color = NeonPlayerOrange,
                                letterSpacing = 8.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPlayerCyan),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "ENTER GAME BOARD",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }
            }
        }
    }
}
