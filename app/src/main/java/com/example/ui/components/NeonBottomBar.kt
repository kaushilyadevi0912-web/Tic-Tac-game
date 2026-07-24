package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*

@Composable
fun NeonBottomBar(
    onUndoClick: () -> Unit,
    onHintClick: () -> Unit,
    onRestartClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Undo Button
        NeonControlButton(
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Undo,
                    contentDescription = "Undo",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            },
            onClick = onUndoClick
        )

        // Hint Button
        NeonControlButton(
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Lightbulb,
                    contentDescription = "Hint",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            },
            onClick = onHintClick
        )

        // Restart / Spin Button
        NeonControlButton(
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = "Restart",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            },
            onClick = onRestartClick
        )
    }
}

@Composable
fun NeonControlButton(
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(68.dp)
            .shadow(elevation = 12.dp, shape = RoundedCornerShape(20.dp), spotColor = NeonButtonBorder)
            .clip(RoundedCornerShape(20.dp))
            .background(NeonButtonBg)
            .border(2.5.dp, NeonButtonBorder, RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}
