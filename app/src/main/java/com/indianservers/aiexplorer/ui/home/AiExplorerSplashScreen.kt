package com.indianservers.aiexplorer

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun AiExplorerSplashScreen(modifier: Modifier = Modifier) {
    var animateIn by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animateIn = true }
    val imageScale by animateFloatAsState(
        targetValue = if (animateIn) 1f else .92f,
        animationSpec = tween(durationMillis = 720, easing = FastOutSlowInEasing),
        label = "splash image scale",
    )
    val imageAlpha by animateFloatAsState(
        targetValue = if (animateIn) 1f else .45f,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "splash image alpha",
    )
    val footerAlpha by animateFloatAsState(
        targetValue = if (animateIn) 1f else 0f,
        animationSpec = tween(durationMillis = 520, delayMillis = 180, easing = FastOutSlowInEasing),
        label = "splash footer alpha",
    )
    val footerOffset by animateFloatAsState(
        targetValue = if (animateIn) 0f else 18f,
        animationSpec = tween(durationMillis = 520, delayMillis = 180, easing = FastOutSlowInEasing),
        label = "splash footer offset",
    )
    Box(
        modifier
            .background(Color.Black)
            .semantics { contentDescription = "AI Explorer splash screen" },
    ) {
        Image(
            painter = painterResource(R.drawable.ai_explorer_splash),
            contentDescription = "AI Explorer STEM AR",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = imageAlpha
                    scaleX = imageScale
                    scaleY = imageScale
                },
        )
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = footerAlpha
                    translationY = footerOffset
                }
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .76f))))
                .padding(horizontal = 18.dp, vertical = 20.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Text(
                "Powered by www.IndianServers.com",
                color = Ink,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
