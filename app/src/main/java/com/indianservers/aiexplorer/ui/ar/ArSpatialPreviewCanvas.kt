package com.indianservers.aiexplorer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.indianservers.aiexplorer.core.Solid
import com.indianservers.aiexplorer.spatial.SpatialRenderScene
import com.indianservers.aiexplorer.spatial.SpatialScenePlacement
import kotlin.math.abs

@Composable
internal fun SpatialPreviewCanvas(
    modifier: Modifier,
    solids: List<Solid>,
    spatialScene: SpatialRenderScene,
    placement: SpatialScenePlacement,
    onGestureStart: () -> Unit,
    onGesture: (Offset, Float, Float) -> Unit,
    onGestureEnd: () -> Unit,
) {
    val currentPlacement by rememberUpdatedState(placement)
    val objectCount = spatialScene.primitives.count { it.visible } + solids.size
    Canvas(
        modifier
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    onGestureStart()
                    var totalPan = Offset.Zero
                    var totalRotation = 0f
                    var totalScale = 1f
                    do {
                        val event = awaitPointerEvent()
                        totalPan += event.calculatePan()
                        totalRotation += event.calculateRotation()
                        totalScale *= event.calculateZoom()
                        onGesture(totalPan, totalScale, totalRotation)
                        event.changes.forEach { change -> if (change.pressed) change.consume() }
                    } while (event.changes.any { it.pressed })
                    if (totalPan.getDistance() > 1f || abs(totalScale - 1f) > .01f || abs(totalRotation) > .4f) {
                        onGestureEnd()
                    }
                }
            }
            .semantics {
                contentDescription = "AR spatial mathematics preview with direct move, rotate and scale gestures"
            },
    ) {
        drawRect(
            Brush.verticalGradient(
                listOf(
                    Color(0xFF08131B),
                    Color(0xFF18242A),
                    Color(0xFF101317),
                ),
            ),
        )
        val scene = currentPlacement
        val center = Offset(
            size.width * .56f + scene.pose.positionMeters.x.toFloat() * 220f,
            size.height * .53f + scene.pose.positionMeters.z.toFloat() * 34f,
        )
        drawPreviewGrid(center.copy(y = center.y + 110f))
        val accent = if (scene.isPlaced) Green else Cyan
        drawCircle(accent.copy(.16f), 86f * scene.pose.uniformScale.toFloat(), center)
        drawCircle(accent, 34f, center, style = Stroke(4f))
        drawLine(Color.White.copy(.72f), center - Offset(70f, 0f), center + Offset(70f, 0f), 3f)
        drawLine(Color.White.copy(.72f), center - Offset(0f, 70f), center + Offset(0f, 70f), 3f)
        drawLine(Violet.copy(.75f), center + Offset(-48f, 42f), center + Offset(52f, -38f), 3f)
        drawPreviewLabel(
            "${if (scene.isPlaced) "Anchored" else "Preview"} - $objectCount object(s) - ${scene.visibleScale}",
            center + Offset(28f, -130f),
            if (scene.isPlaced) Green else Amber,
        )
    }
}

private fun DrawScope.drawPreviewGrid(center: Offset) {
    val grid = Color.White.copy(.12f)
    for (i in -4..4) {
        val dx = i * 42f
        drawLine(grid, center + Offset(dx, -170f), center + Offset(dx * .36f, 130f), 1f)
        drawLine(grid, center + Offset(-190f, i * 26f), center + Offset(190f, i * 26f), 1f)
    }
    drawLine(Cyan.copy(.45f), center + Offset(-210f, 0f), center + Offset(210f, 0f), 2f)
    drawLine(Violet.copy(.35f), center + Offset(0f, -180f), center + Offset(0f, 140f), 2f)
}

private fun DrawScope.drawPreviewLabel(text: String, offset: Offset, color: Color) {
    val paint = android.graphics.Paint().apply {
        isAntiAlias = true
        textSize = 26f
        this.color = color.copy(.92f).toArgb()
    }
    drawContext.canvas.nativeCanvas.drawText(text, offset.x, offset.y, paint)
}
