package com.indianservers.aiexplorer

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import com.indianservers.aiexplorer.core.AnalyticGeometry3D
import com.indianservers.aiexplorer.core.Solid
import com.indianservers.aiexplorer.core.SolidType
import com.indianservers.aiexplorer.core.SolidMeshFactory
import com.indianservers.aiexplorer.core.Vec3
import kotlin.math.*

/** Fitted, opaque catalogue artwork. This never changes workspace geometry or hit testing. */
@Composable
internal fun GeometryShapeArtwork(solid: Solid, color: Color, modifier: Modifier = Modifier) {
    val mesh = remember(solid) { SolidMeshFactory.create(if (solid.type == SolidType.Cuboid) solid.copy(width = 1.0, height = 1.7, depth = .9) else solid, segments = 80) }
    val vertices = remember(mesh) {
        val yaw = Math.toRadians(-35.0)
        val pitch = Math.toRadians(22.0)
        mesh.vertices.map { p ->
            val x = p.x * cos(yaw) + p.z * sin(yaw)
            val z = -p.x * sin(yaw) + p.z * cos(yaw)
            Vec3(x, p.y * cos(pitch) - z * sin(pitch), p.y * sin(pitch) + z * cos(pitch))
        }
    }
    val faces = remember(mesh, vertices) { mesh.faces.filter { it.size >= 3 }.sortedBy { f -> f.map { vertices[it].z }.average() } }
    Canvas(modifier) {
        val radius = size.minDimension * .44f
        val bright = lerp(color, Color.White, .38f)
        val dark = lerp(color, Color(0xFF030720), .62f)
        if (solid.type == SolidType.Sphere) {
            drawCircle(Brush.radialGradient(listOf(bright, color, dark), center = center - Offset(radius * .35f, radius * .45f), radius = radius * 1.7f), radius, center)
            drawCircle(color.copy(alpha = .6f), radius, center, style = Stroke(.8f))
            return@Canvas
        }
        if (solid.type == SolidType.Hemisphere) {
            val left = center.x - radius * 1.25f; val right = center.x + radius * 1.25f
            val base = center.y + radius * .48f; val top = center.y - radius * .85f
            val dome = Path().apply {
                moveTo(left, base)
                cubicTo(left + radius * .15f, top - radius * .2f, right - radius * .15f, top - radius * .2f, right, base)
                cubicTo(right - radius * .4f, base + radius * .42f, left + radius * .4f, base + radius * .42f, left, base)
                close()
            }
            drawPath(dome, Brush.radialGradient(listOf(bright, color, dark), center = Offset(center.x - radius * .5f, top), radius = radius * 2.5f))
            return@Canvas
        }
        if (solid.type == SolidType.Cylinder) {
            val w = radius * 1.45f; val h = radius * 1.72f
            val x = center.x - w / 2; val y = center.y - h / 2
            val body = Path().apply {
                moveTo(x, y); lineTo(x + w, y); lineTo(x + w, y + h)
                cubicTo(x + w, y + h + radius * .36f, x, y + h + radius * .36f, x, y + h); close()
            }
            drawPath(body, Brush.horizontalGradient(listOf(dark, color, bright, color, dark), startX = x, endX = x + w))
            drawOval(Brush.linearGradient(listOf(bright, color)), Offset(x, y - radius * .24f), Size(w, radius * .48f))
            return@Canvas
        }
        if (solid.type == SolidType.Cone) {
            val left = center.x - radius * .8f; val right = center.x + radius * .8f
            val bottom = center.y + radius * .8f
            val cone = Path().apply {
                moveTo(center.x, center.y - radius); lineTo(right, bottom)
                cubicTo(right, bottom + radius * .4f, left, bottom + radius * .4f, left, bottom)
                close()
            }
            drawPath(cone, Brush.horizontalGradient(listOf(dark, color, bright, color, dark), startX = left, endX = right))
            return@Canvas
        }
        if (vertices.isEmpty()) return@Canvas
        val minX = vertices.minOf { it.x }; val maxX = vertices.maxOf { it.x }
        val minY = vertices.minOf { it.y }; val maxY = vertices.maxOf { it.y }
        val scale = min(size.width * .86 / (maxX - minX).coerceAtLeast(.01), size.height * .88 / (maxY - minY).coerceAtLeast(.01))
        fun project(p: Vec3) = Offset((center.x + (p.x - (minX + maxX) / 2) * scale).toFloat(), (center.y - (p.y - (minY + maxY) / 2) * scale).toFloat())
        val light = Vec3(-.45, .7, .8).normalized()
        faces.forEach { face ->
            val path = Path().apply {
                val first = project(vertices[face.first()]); moveTo(first.x, first.y)
                face.drop(1).forEach { val p = project(vertices[it]); lineTo(p.x, p.y) }; close()
            }
            val normal = AnalyticGeometry3D.cross(vertices[face[1]] - vertices[face[0]], vertices[face[2]] - vertices[face[0]]).normalized()
            val lighting = abs(normal.dot(light)).toFloat()
            val base = lerp(Color(0xFF060B24), color, .45f + lighting * .55f)
            val highlight = lerp(base, Color.White, lighting.pow(8) * .40f)
            drawPath(path, Brush.linearGradient(listOf(highlight, base), start = Offset.Zero, end = Offset(size.width, size.height)))
            // Seal triangle seams; only low-poly solids receive a visible edge highlight.
            drawPath(path, base, style = Stroke(.65f))
            if (mesh.faces.size <= 12) drawPath(path, lerp(base, Color.White, .25f), style = Stroke(.8f))
        }
    }
}
