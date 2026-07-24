package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.logic.AiDifficulty
import com.example.ui.theme.*

@Composable
fun SettingsDialog(
    isSoundEnabled: Boolean,
    isMusicEnabled: Boolean,
    isHapticsEnabled: Boolean,
    currentDifficulty: AiDifficulty,
    currentGridSize: Int,
    onToggleSound: (Boolean) -> Unit,
    onToggleMusic: (Boolean) -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
    onSelectDifficulty: (AiDifficulty) -> Unit,
    onSelectGridSize: (Int) -> Unit,
    onResetScores: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(24.dp, RoundedCornerShape(24.dp), spotColor = NeonBoardBorder)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF140D2E))
                .border(2.5.dp, NeonBoardBorder, RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "SETTINGS",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = NeonPlayerOrange
                )

                HorizontalDivider(color = NeonButtonBorder.copy(alpha = 0.3f))

                // Background Music Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Background Music", color = Color.White, fontSize = 16.sp)
                    Switch(
                        checked = isMusicEnabled,
                        onCheckedChange = onToggleMusic,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = NeonPlayerOrange
                        )
                    )
                }

                // Sound Effects Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Sound Effects", color = Color.White, fontSize = 16.sp)
                    Switch(
                        checked = isSoundEnabled,
                        onCheckedChange = onToggleSound,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = NeonPlayerCyan
                        )
                    )
                }

                // Haptics Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Vibration / Haptics", color = Color.White, fontSize = 16.sp)
                    Switch(
                        checked = isHapticsEnabled,
                        onCheckedChange = onToggleHaptics,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = NeonPlayerCyan
                        )
                    )
                }

                HorizontalDivider(color = NeonButtonBorder.copy(alpha = 0.3f))

                // AI Difficulty Selector
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("AI DIFFICULTY", color = NeonTextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AiDifficulty.entries.forEach { diff ->
                            val isSelected = currentDifficulty == diff
                            Button(
                                onClick = { onSelectDifficulty(diff) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) NeonPlayerCyan else Color(0x33A200FF)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = diff.name,
                                    fontSize = 11.sp,
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Grid Size Selector
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("BOARD SIZE", color = NeonTextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(3, 4, 5, 6, 7).forEach { size ->
                            val isSelected = currentGridSize == size
                            Button(
                                onClick = { onSelectGridSize(size) },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(0.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) NeonPlayerOrange else Color(0x33A200FF)
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "${size}x${size}",
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = NeonButtonBorder.copy(alpha = 0.3f))

                // Reset Scores Button
                OutlinedButton(
                    onClick = onResetScores,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonPlayerRed),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(NeonPlayerRed))
                ) {
                    Text("RESET SCORES", fontWeight = FontWeight.Bold)
                }

                // Close Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPlayerCyan)
                ) {
                    Text("CLOSE", color = Color.Black, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}
