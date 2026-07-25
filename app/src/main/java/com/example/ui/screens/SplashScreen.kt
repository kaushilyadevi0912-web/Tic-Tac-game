package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }
    val scale = animateFloatAsState(
        targetValue = if (startAnimation) 1.0f else 0.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "logoScale"
    )

    var dotCount by remember { mutableIntStateOf(1) }

    LaunchedEffect(Unit) {
        startAnimation = true
        val dotsJob = launch {
            while (true) {
                delay(400)
                dotCount = (dotCount % 3) + 1
            }
        }
        delay(2800)
        dotsJob.cancel()
        onSplashFinished()
    }

    val loadingDots = " .".repeat(dotCount)

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(32.dp)
                .scale(scale.value)
        ) {
            // App Logo in Center
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .shadow(
                        elevation = 24.dp,
                        shape = RoundedCornerShape(32.dp),
                        spotColor = NeonPlayerCyan,
                        ambientColor = NeonBoardBorder
                    )
                    .border(
                        width = 3.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(NeonPlayerCyan, NeonBoardBorder, NeonPlayerOrange)
                        ),
                        shape = RoundedCornerShape(32.dp)
                    )
                    .clip(RoundedCornerShape(32.dp))
                    .background(NeonBackgroundCard),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.neon_app_icon_1784890317789),
                    contentDescription = "App Logo",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(32.dp))
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // App Name
            Text(
                text = stringResource(id = R.string.app_name).uppercase(),
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = NeonPlayerCyan,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Created By MANISH RAUT
            Text(
                text = "Created By MANISH RAUT",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = NeonPlayerOrange,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(44.dp))

            // Loading indicator and text
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(36.dp),
                    color = NeonPlayerCyan,
                    strokeWidth = 3.5.dp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Loading$loadingDots",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NeonTextMuted,
                    letterSpacing = 1.5.sp
                )
            }
        }
    }
}
