package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun NeonTopBar(
    title: String? = null,
    onBackClick: (() -> Unit)? = null,
    onSettingsClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back Button (Red Neon Circle)
        if (onBackClick != null) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .shadow(elevation = 12.dp, shape = CircleShape, spotColor = NeonPlayerRed)
                    .clip(CircleShape)
                    .background(Color(0x33FF3B30))
                    .border(2.5.dp, NeonPlayerRed, CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onBackClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.size(50.dp))
        }

        // Optional Title
        if (title != null) {
            Text(
                text = title,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = NeonPlayerOrange
            )
        }

        // Settings Button (Cyan Neon Circle)
        if (onSettingsClick != null) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .shadow(elevation = 12.dp, shape = CircleShape, spotColor = NeonPlayerCyan)
                    .clip(CircleShape)
                    .background(Color(0x3300F0FF))
                    .border(2.5.dp, NeonPlayerCyan, CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onSettingsClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = "Settings",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.size(50.dp))
        }
    }
}
