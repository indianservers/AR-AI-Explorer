package com.indianservers.aiexplorer

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

internal val standardSplashQuotes = listOf(
    "The Universe Speaks Mathematics",
    "Decode the Patterns of the Universe",
    "Math is the Universal Language",
    "Numbers. Patterns. Possibilities.",
    "The Universe is Written in Mathematics",
    "Discover the Mathematics of the Universe",
    "Unlock the Patterns of the Universe",
    "Where Numbers Explain the Universe",
    "Mathematics Reveals the Universe",
    "The Language Behind the Universe",
    "Numbers Tell the Story of the Universe",
    "Discover the Patterns Behind Everything",
    "Decode the Mathematics of Everything",
    "Where Patterns Become Understanding",
    "Numbers. Logic. Infinity.",
    "Patterns. Logic. Discovery.",
    "Numbers. Patterns. Infinity.",
    "Think. Discover. Understand.",
    "Explore. Decode. Discover.",
    "Observe. Calculate. Discover.",
    "Discover the Logic of the Universe",
    "Explore the Patterns of Everything",
    "Mathematics Behind Everything",
    "Every Pattern Has a Story",
    "Every Number Reveals a Pattern",
    "Where Mathematics Meets Infinity",
    "From Numbers to the Universe",
    "From Patterns to Possibilities",
    "Infinity Begins with Mathematics",
    "See the Universe Through Mathematics",
)

internal fun weightedSplashQuotes(): List<String> = standardSplashQuotes

internal fun randomSplashQuote(random: Random = Random.Default): String {
    val weighted = weightedSplashQuotes()
    return weighted[random.nextInt(weighted.size)]
}

@Composable
internal fun AiExplorerSplashScreen(modifier: Modifier = Modifier) {
    val timeline = remember { Animatable(0f) }
    val quote = remember { randomSplashQuote() }

    LaunchedEffect(Unit) {
        launch { playSplashWhoosh() }
        timeline.animateTo(1f, tween(durationMillis = 2_000, easing = LinearEasing))
    }

    val progress = timeline.value
    val logoAlpha = phase(progress, .48f, .67f) * (1f - phase(progress, .96f, 1f) * .10f)
    val titleAlpha = phase(progress, .58f, .76f)
    val calculationAlpha = phase(progress, .72f, .82f)
    val legalAlpha = phase(progress, .88f, .96f)

    Box(
        modifier
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF071B3D), Color(0xFF020713), Color(0xFF050505)),
                    radius = 1_350f,
                ),
            )
            .semantics { contentDescription = "AI Explorer mathematics splash screen" },
    ) {
        MathematicalUniverse(progress, Modifier.fillMaxSize())

        Image(
            painter = painterResource(R.drawable.ai_explorer_icon_foreground),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.Center)
                .size(88.dp)
                .graphicsLayer {
                    alpha = logoAlpha
                    val scale = .42f + phase(progress, .43f, .66f) * .58f + phase(progress, .66f, .72f) * .06f
                    scaleX = scale
                    scaleY = scale
                    rotationZ = (1f - phase(progress, .42f, .66f)) * -8f
                    shadowElevation = 32f * logoAlpha
                },
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(top = 174.dp, start = 22.dp, end = 22.dp)
                .graphicsLayer { alpha = titleAlpha },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = quote,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                lineHeight = 28.sp,
                maxLines = 2,
            )
            Text(
                text = "Visual Proofs  •  Interactive Learning",
                color = Color(0xFF78E7FF),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = .5.sp,
                maxLines = 1,
            )
        }

        Text(
            text = "Calculating Infinity...",
            color = Color(0xFF5DF4FF),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 78.dp)
                .graphicsLayer { alpha = calculationAlpha },
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .graphicsLayer { alpha = legalAlpha },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Powered by Indian Servers Pvt Ltd  •  www.IndianServers.com",
                color = Color(0xFFB8D7FF),
                fontSize = 8.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Text(
                "© 2026 Indian Servers Pvt Ltd. All Rights Reserved.",
                color = Color.White.copy(alpha = .68f),
                fontSize = 7.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun MathematicalUniverse(progress: Float, modifier: Modifier = Modifier) {
    val symbols = remember { listOf("\u03C0", "\u2211", "\u222B", "\u221A", "\u221E", "\u03B8", "\u03BB") }
    val equations = remember {
        listOf(
            "E = mc\u00B2",
            "a\u00B2 + b\u00B2 = c\u00B2",
            "sin\u00B2\u03B8 + cos\u00B2\u03B8 = 1",
            "e^(i\u03C0) + 1 = 0",
        )
    }
    Canvas(modifier) {
        val center = Offset(size.width / 2f, size.height / 2f - size.minDimension * .04f)
        val gather = phase(progress, 0f, .42f)
        val network = phase(progress, .28f, .68f) * (1f - phase(progress, .76f, .96f))
        val collapse = phase(progress, .70f, .94f)
        val radius = size.minDimension * (.21f * (1f - collapse) + .055f * collapse)
        val points = symbols.indices.map { index ->
            val angle = (index.toFloat() / symbols.size) * (PI * 2.0) - PI / 2.0
            val startRadius = size.minDimension * (.48f + (index % 3) * .07f)
            val start = Offset(
                center.x + cos(angle).toFloat() * startRadius,
                center.y + sin(angle).toFloat() * startRadius * .78f,
            )
            val sphereAngle = angle + progress * .55f
            val target = Offset(
                center.x + cos(sphereAngle).toFloat() * radius,
                center.y + sin(sphereAngle).toFloat() * radius * .78f,
            )
            lerp(start, target, gather)
        }

        // A sparse star field gives depth without a bitmap or texture allocation.
        repeat(46) { index ->
            val x = ((index * 83) % 101) / 101f * size.width
            val y = ((index * 47 + 11) % 103) / 103f * size.height
            val warp = phase(progress, .05f, .45f) * (1f - phase(progress, .80f, 1f))
            val pulse = .18f + .35f * ((sin(progress * 22f + index) + 1f) / 2f)
            val drift = 26f * warp
            val star = Offset((x + drift * ((index % 5) - 2)).coerceIn(0f, size.width), (y + drift * ((index % 7) - 3)).coerceIn(0f, size.height))
            drawCircle(Color(0xFF2C9DFF).copy(alpha = pulse), 1f + index % 3, star)
        }

        val letterbox = phase(progress, 0f, .22f) * (1f - phase(progress, .88f, 1f))
        drawRect(Color.Black.copy(alpha = .46f * letterbox), topLeft = Offset.Zero, size = androidx.compose.ui.geometry.Size(size.width, size.height * .11f))
        drawRect(Color.Black.copy(alpha = .46f * letterbox), topLeft = Offset(0f, size.height * .89f), size = androidx.compose.ui.geometry.Size(size.width, size.height * .11f))

        // Equation particles stream inward before resolving into the neural sphere.
        repeat(34) { index ->
            val angle = index * 2.39996f + .35f
            val startRadius = size.minDimension * (.54f + (index % 5) * .035f)
            val destinationRadius = radius * (.68f + (index % 7) * .045f)
            val movingRadius = startRadius + (destinationRadius - startRadius) * gather
            val verticalScale = .70f + (index % 4) * .035f
            val point = Offset(
                center.x + cos(angle + progress * .9f) * movingRadius,
                center.y + sin(angle + progress * .9f) * movingRadius * verticalScale,
            )
            val tail = Offset(
                point.x + cos(angle) * (10f + (1f - gather) * 28f),
                point.y + sin(angle) * (10f + (1f - gather) * 28f),
            )
            val alpha = (.16f + (index % 5) * .055f) * (1f - collapse)
            drawLine(Color(0xFF168CFF).copy(alpha = alpha), tail, point, 1.2f, StrokeCap.Round)
            drawCircle(Color(0xFF72F4FF).copy(alpha = alpha + .18f), 1.5f + index % 3, point)
        }

        if (network > 0f) {
            points.forEachIndexed { index, point ->
                val next = points[(index + 1) % points.size]
                drawLine(Color(0xFF16DFFF).copy(alpha = network * .14f), point, next, 8f)
                drawLine(Color(0xFF72EEFF).copy(alpha = network * .82f), point, next, 1.4f)
                if (index % 2 == 0) {
                    drawLine(
                        Color(0xFF397BFF).copy(alpha = network * .42f),
                        point,
                        points[(index + 3) % points.size],
                        1f,
                    )
                }
            }
            // Latitude rings and rotating longitude arcs form a real spherical lattice.
            repeat(7) { latitudeIndex ->
                val normalizedY = (latitudeIndex - 3) / 3.5f
                val ringRadius = radius * sqrt((1f - normalizedY * normalizedY).coerceAtLeast(0f))
                val ringCenter = Offset(center.x, center.y + radius * normalizedY * .78f)
                drawOval(
                    color = Color(0xFF64EDFF).copy(alpha = network * (.20f + (3 - kotlin.math.abs(latitudeIndex - 3)) * .045f)),
                    topLeft = Offset(ringCenter.x - ringRadius, ringCenter.y - ringRadius * .18f),
                    size = androidx.compose.ui.geometry.Size(ringRadius * 2f, ringRadius * .36f),
                    style = Stroke(width = 1.1f),
                )
            }
            repeat(9) { longitudeIndex ->
                val longitude = longitudeIndex * PI.toFloat() / 9f + progress * 1.35f
                val path = Path()
                repeat(25) { sample ->
                    val t = -PI.toFloat() / 2f + sample * PI.toFloat() / 24f
                    val point = Offset(
                        center.x + cos(t) * cos(longitude) * radius,
                        center.y + sin(t) * radius * .82f,
                    )
                    if (sample == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
                }
                drawPath(
                    path = path,
                    color = Color(0xFF478CFF).copy(alpha = network * (.18f + .18f * ((sin(longitude) + 1f) / 2f))),
                    style = Stroke(width = 1.05f),
                )
            }
            repeat(3) { ring ->
                drawCircle(
                    color = Color(0xFF21CFFF).copy(alpha = network * (.20f - ring * .04f)),
                    radius = radius * (1f + ring * .18f),
                    center = center,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2f),
                )
            }
        }

        val symbolAlpha = (1f - phase(progress, .53f, .70f)).coerceIn(0f, 1f)
        drawIntoCanvas { canvas ->
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.rgb(91, 228, 255)
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = size.minDimension * .064f
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.BOLD)
                alpha = (symbolAlpha * 255).roundToInt()
                setShadowLayer(18f, 0f, 0f, android.graphics.Color.CYAN)
            }
            points.forEachIndexed { index, point ->
                canvas.nativeCanvas.drawText(symbols[index], point.x, point.y + paint.textSize * .34f, paint)
            }

            val equationAlpha = network * (1f - collapse)
            paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.NORMAL)
            paint.textSize = (size.minDimension * .026f).coerceAtLeast(18f)
            paint.alpha = (equationAlpha * 220).roundToInt()
            equations.forEachIndexed { index, equation ->
                val angle = index * PI.toFloat() / 2f + progress * .8f
                val orbit = radius * 1.58f
                val position = Offset(
                    center.x + cos(angle) * orbit,
                    center.y + sin(angle) * orbit * .72f,
                )
                canvas.nativeCanvas.drawText(equation, position.x, position.y, paint)
            }
        }

        val sweep = phase(progress, .57f, .86f)
        if (sweep in .001f..0.999f) {
            val x = center.x - size.minDimension * .28f + size.minDimension * .56f * sweep
            drawLine(
                brush = Brush.verticalGradient(listOf(Color.Transparent, Color.White, Color.Transparent)),
                start = Offset(x - 24f, center.y - size.minDimension * .17f),
                end = Offset(x + 24f, center.y + size.minDimension * .25f),
                strokeWidth = 10f,
                cap = StrokeCap.Round,
                alpha = .86f,
            )
        }

        val lock = phase(progress, .62f, .82f) * (1f - phase(progress, .94f, 1f))
        if (lock > 0f) {
            repeat(4) { index ->
                val r = radius * (1.28f + index * .20f + .05f * sin(progress * 18f + index))
                drawCircle(
                    color = Color(0xFF79F8FF).copy(alpha = lock * (.22f - index * .035f)),
                    radius = r,
                    center = center,
                    style = Stroke(width = 1.5f + index),
                )
            }
            val bar = radius * 1.95f
            drawLine(Color(0xFFBDFBFF).copy(alpha = lock * .80f), Offset(center.x - bar, center.y), Offset(center.x - bar * .56f, center.y), 2.2f, StrokeCap.Round)
            drawLine(Color(0xFFBDFBFF).copy(alpha = lock * .80f), Offset(center.x + bar * .56f, center.y), Offset(center.x + bar, center.y), 2.2f, StrokeCap.Round)
            drawLine(Color(0xFFBDFBFF).copy(alpha = lock * .80f), Offset(center.x, center.y - bar), Offset(center.x, center.y - bar * .56f), 2.2f, StrokeCap.Round)
            drawLine(Color(0xFFBDFBFF).copy(alpha = lock * .80f), Offset(center.x, center.y + bar * .56f), Offset(center.x, center.y + bar), 2.2f, StrokeCap.Round)
        }
    }
}

private fun phase(value: Float, start: Float, end: Float): Float =
    ((value - start) / (end - start)).coerceIn(0f, 1f)

private fun lerp(start: Offset, end: Offset, amount: Float): Offset =
    Offset(
        x = start.x + (end.x - start.x) * amount,
        y = start.y + (end.y - start.y) * amount,
    )

private suspend fun playSplashWhoosh() = withContext(Dispatchers.Default) {
    runCatching {
        val sampleRate = 24_000
        val durationSeconds = .45f
        val sampleCount = (sampleRate * durationSeconds).roundToInt()
        val samples = ShortArray(sampleCount)
        var filteredNoise = 0f
        val random = Random(System.nanoTime())
        for (index in samples.indices) {
            val t = index.toFloat() / sampleRate
            val normalized = t / durationSeconds
            val envelope = sin(PI.toFloat() * normalized).coerceAtLeast(0f)
            val frequency = 180f + 760f * normalized * normalized
            filteredNoise = filteredNoise * .82f + (random.nextFloat() * 2f - 1f) * .18f
            val tone = sin(2f * PI.toFloat() * frequency * t)
            samples[index] = ((tone * .34f + filteredNoise * .66f) * envelope * Short.MAX_VALUE * .10f).toInt().toShort()
        }
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(samples.size * 2)
            .build()
        track.write(samples, 0, samples.size)
        track.setVolume(.16f)
        track.play()
        delay(500)
        track.release()
    }
}
