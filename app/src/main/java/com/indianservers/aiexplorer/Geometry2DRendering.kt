package com.indianservers.aiexplorer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.indianservers.aiexplorer.core.Geometry2D
import com.indianservers.aiexplorer.core.Vec2
import com.indianservers.aiexplorer.workspace.Shape2D
import com.indianservers.aiexplorer.workspace.Shape2DType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val RenderCyan = WorkspaceVisualStyles.ReferenceCyan
private val RenderViolet = WorkspaceVisualStyles.ReferenceViolet
private val RenderBlue = WorkspaceVisualStyles.ReferenceBlue
private val RenderMagenta = WorkspaceVisualStyles.ReferenceMagenta
private val RenderCoral = WorkspaceVisualStyles.ReferenceCoral
private val RenderOrange = WorkspaceVisualStyles.ReferenceOrange
private val RenderYellow = WorkspaceVisualStyles.ReferenceYellow

/** Keep labels readable for visible anchors without pinning offscreen objects to the edge. */
private fun DrawScope.drawGeometryLabel(text:String,anchor:Offset,position:Offset,color:Color) {
    if(anchor.x !in 0f..size.width||anchor.y !in 0f..size.height)return
    val paint=android.graphics.Paint().apply{textSize=24f;isAntiAlias=true}
    val width=(paint.measureText(text)+24f).coerceAtLeast(190f).coerceAtMost((size.width-8f).coerceAtLeast(1f))
    val x=position.x.coerceIn(4f,(size.width-width-4f).coerceAtLeast(4f));val y=position.y.coerceIn(4f,(size.height-46f).coerceAtLeast(4f));val top=Offset(x,y)
    drawRoundRect(SurfaceA,topLeft=top,size=Size(width,42f),cornerRadius=androidx.compose.ui.geometry.CornerRadius(12f,12f))
    drawRoundRect(color.copy(.75f),topLeft=top,size=Size(width,42f),cornerRadius=androidx.compose.ui.geometry.CornerRadius(12f,12f),style=Stroke(1.4f))
    drawTrigText(text,x+12f,y+27f,color)
}

private fun DrawScope.drawMeasuredAngle(p:List<Vec2>,value:Double,tx:(Vec2)->Offset,oriented:Boolean=false,exterior:Boolean=false) {
        val center=tx(p[1]);val a=tx(p[0]);val b=tx(p[2]);val radius=minOf(48f,(a-center).getDistance()*.3f,(b-center).getDistance()*.3f)
        if(radius<2f)return
        val start=kotlin.math.atan2(a.y-center.y,a.x-center.x)
        var sweep=kotlin.math.atan2(b.y-center.y,b.x-center.x)-start
        while(sweep>PI)sweep-=(2*PI).toFloat();while(sweep< -PI)sweep+=(2*PI).toFloat()
        if(oriented)sweep=-Math.toRadians(value).toFloat()
        if(!oriented&&!exterior&&value>180)sweep=if(sweep>=0)sweep-(2*PI).toFloat()else sweep+(2*PI).toFloat()
        if(exterior)sweep=if(sweep>=0)sweep-PI.toFloat() else sweep+PI.toFloat()
        val path=Path();for(i in 0..48){val angle=start+sweep*i/48;val q=center+Offset(cos(angle)*radius,sin(angle)*radius);if(i==0)path.moveTo(q.x,q.y)else path.lineTo(q.x,q.y)}
        drawPath(path,RenderYellow,style=Stroke(3f))
        val degrees=value
        drawGeometryLabel("%.4g°".format(java.util.Locale.US,degrees),center,center+Offset(16f,20f),RenderYellow)
}
internal fun DrawScope.drawRuhiAngleMarks(state:com.indianservers.aiexplorer.workspace.WorkspaceState,tx:(Vec2)->Offset) {
    val language=com.indianservers.aiexplorer.assistant.workspace.RuhiAngleLanguage
    language.marks(state).forEach{mark->
        val value=runCatching{language.markDegrees(state,mark)}.getOrNull() ?: return@forEach
        drawMeasuredAngle(mark.indices.map{state.points[it]},value,tx,mark.oriented,mark.exterior)
    }
    val reference=com.indianservers.aiexplorer.assistant.workspace.RuhiReferenceAngleLanguage
    reference.marks(state).forEach{mark->
        val measured=runCatching{reference.measure(state,mark)}.getOrNull() ?: return@forEach
        drawMeasuredAngle(measured.arms,measured.degrees,tx)
    }
}

internal fun DrawScope.drawRuhiSlopeMarks(state:com.indianservers.aiexplorer.workspace.WorkspaceState,tx:(Vec2)->Offset) {
    com.indianservers.aiexplorer.assistant.workspace.RuhiSlopeLanguage.marks(state).forEach{mark->
        val a=state.points[mark.first];val b=state.points[mark.second]
        if(!listOf(a.x,a.y,b.x,b.y).all{it.isFinite()})return@forEach
        val corner=Vec2(b.x,a.y);val run=b.x-a.x;val rise=b.y-a.y
        val value=if(run==0.0)"undefined" else "%.4g".format(java.util.Locale.US,rise/run)
        if(mark.triangle) {
            val path=Path().apply{val p=tx(a);moveTo(p.x,p.y);val q=tx(corner);lineTo(q.x,q.y);val r=tx(b);lineTo(r.x,r.y);close()}
            drawPath(path,RenderCyan.copy(alpha=.10f));drawLine(RenderYellow,tx(a),tx(corner),3f);drawLine(RenderOrange,tx(corner),tx(b),3f)
            val runAnchor=tx((a+corner)*.5);val riseAnchor=tx((corner+b)*.5)
            drawGeometryLabel("run = %.4g".format(java.util.Locale.US,run),runAnchor,runAnchor+Offset(-60f,48f),RenderYellow)
            drawGeometryLabel("rise = %.4g".format(java.util.Locale.US,rise),riseAnchor,riseAnchor+Offset(12f,-16f),RenderOrange)
        }
        val anchor=tx((a+b)*.5);drawGeometryLabel("slope = $value",anchor,anchor+Offset(-85f,-70f),RenderCyan)
    }
}

fun DrawScope.drawStoredShapes(
    points: List<Vec2>,
    shapes: List<Shape2D>,
    selectedShapes: Set<Int>,
    selectedShape: Int,
    tx: (Vec2) -> Offset,
    pointLabels: Map<Int,String> = emptyMap(),
) {
    val objectPalette = listOf(
        RenderCyan, RenderBlue, RenderViolet, RenderMagenta, RenderCoral, RenderOrange, RenderYellow,
    )
    fun objectColor(shape: Shape2D): Color = when (shape.styleKey) {
        "cyan" -> RenderCyan
        "violet" -> RenderViolet
        "green" -> RenderBlue
        "accent" -> RenderYellow
        else -> if (Regex("#[0-9A-Fa-f]{6}").matches(shape.styleKey)) Color(android.graphics.Color.parseColor(shape.styleKey)) else objectPalette[shape.id.hashCode().ushr(1) % objectPalette.size]
    }
    shapes.forEachIndexed { index, shape ->
        if (!shape.visible) return@forEachIndexed
        val shapePoints = shape.pointIndices.mapNotNull { points.getOrNull(it) }
        val isSelected = index in selectedShapes
        val accent = objectColor(shape).copy(alpha = if (selectedShapes.isNotEmpty() && !isSelected) .28f else 1f)
        drawShape2D(shape.type, shapePoints, tx, accent, filled = true)
        if (isSelected) shapePoints.forEach { drawCircle(objectColor(shape).copy(alpha = .22f), 15f, tx(it)) }
    }
    val visibleJunctions = shapes.filter { it.visible }.flatMap { it.pointIndices }.distinct()
    val selectedJunctions = shapes.getOrNull(selectedShape)?.pointIndices.orEmpty().toSet()
    val selectedTop = selectedShapes.flatMap{shapes.getOrNull(it)?.pointIndices.orEmpty()}.mapNotNull(points::getOrNull).minOfOrNull{tx(it).y}
    visibleJunctions.forEach { pointIndex ->
        val point = points.getOrNull(pointIndex) ?: return@forEach
        val selected = pointIndex in selectedJunctions
        val owner = shapes.getOrNull(selectedShape)?.takeIf { it.visible && pointIndex in it.pointIndices }
            ?: shapes.firstOrNull { it.visible && pointIndex in it.pointIndices }
        val accent = owner?.let(::objectColor) ?: RenderCyan
        val dimmed = selectedShapes.isNotEmpty() && !selected
        drawCircle(accent.copy(alpha = if (dimmed) .07f else if (selected) .28f else .15f), if (selected) 22f else 16f, tx(point))
        drawCircle(accent.copy(alpha = if (dimmed) .28f else .95f), if (selected) 8f else 6f, tx(point))
        drawCircle(accent.copy(alpha = if (dimmed) .35f else 1f), if (selected) 5f else 3.5f, tx(point))
        pointLabels[pointIndex]?.let {
            val anchor=tx(point)
            // The selection measurement occupies the row above the uppermost selected controls.
            val below = selected && selectedTop?.let{top->kotlin.math.abs(anchor.y-top)<1f}==true
            drawGeometryLabel(it,anchor,anchor+Offset(12f,if(below||anchor.y<60f)12f else -56f),accent)
        }
    }
}

fun DrawScope.drawShape2D(
    type: Shape2DType,
    points: List<Vec2>,
    tx: (Vec2) -> Offset,
    accent: Color,
    filled: Boolean,
) {
    if (points.isEmpty()) return
    fun offset(index: Int) = tx(points[index])
    val stroke = Stroke(if (filled) 3.5f else 2.4f, cap = StrokeCap.Round)
    when (type) {
        Shape2DType.Line, Shape2DType.Ray, Shape2DType.Segment, Shape2DType.Vector -> {
            if (points.size < 2) return
            val a = offset(0); val b = offset(1); val direction = b - a
            val length = direction.getDistance().coerceAtLeast(1f)
            val unit = Offset(direction.x / length, direction.y / length)
            val start = if (type == Shape2DType.Line) a - unit * 2000f else a
            val end = if (type in setOf(Shape2DType.Line, Shape2DType.Ray)) a + unit * 2000f else b
            drawLine(accent, start, end, stroke.width, cap = StrokeCap.Round)
            if (type == Shape2DType.Vector) {
                val normal = Offset(-unit.y, unit.x); val head = 20f
                drawPath(Path().apply {
                    moveTo(end.x, end.y)
                    lineTo((end - unit * head + normal * head * .55f).x, (end - unit * head + normal * head * .55f).y)
                    lineTo((end - unit * head - normal * head * .55f).x, (end - unit * head - normal * head * .55f).y)
                    close()
                }, accent)
            }
        }
        Shape2DType.Parallel, Shape2DType.Perpendicular -> {
            if (points.size < 3) return
            val base = points[1] - points[0]
            val direction = if (type == Shape2DType.Parallel) base else Vec2(-base.y, base.x)
            val unit = direction * (1.0 / direction.distanceTo(Vec2(0.0, 0.0)).coerceAtLeast(1e-9))
            drawLine(accent, tx(points[2] - unit * 100.0), tx(points[2] + unit * 100.0), stroke.width, cap = StrokeCap.Round)
        }
        Shape2DType.AngleBisector -> {
            if (points.size < 3) return
            val u = points[0] - points[1]; val v = points[2] - points[1]
            val direction = u * (1.0 / u.distanceTo(Vec2(0.0, 0.0)).coerceAtLeast(1e-9)) +
                v * (1.0 / v.distanceTo(Vec2(0.0, 0.0)).coerceAtLeast(1e-9))
            drawLine(accent, tx(points[1]), tx(points[1] + direction * 100.0), stroke.width, cap = StrokeCap.Round)
        }
        Shape2DType.Circle -> {
            if (points.size < 2) return
            val center = offset(0); val radius = (offset(1) - center).getDistance()
            if (filled) drawCircle(Brush.radialGradient(listOf(accent.copy(.22f), Color.Transparent), center, radius), radius, center)
            drawCircle(accent, radius, center, style = stroke)
        }
        Shape2DType.CircleThreePoints -> {
            if (points.size < 3) return
            val center = tx(Geometry2D.circumcenter(points[0], points[1], points[2]) ?: return)
            val radius = (tx(points[0]) - center).getDistance()
            if (filled) drawCircle(Brush.radialGradient(listOf(accent.copy(.18f), Color.Transparent), center, radius), radius, center)
            drawCircle(accent, radius, center, style = stroke)
        }
        Shape2DType.Ellipse -> {
            if (points.size < 3) return
            val center = tx(points[0]); val rx = (tx(points[1]) - center).getDistance().coerceAtLeast(1f)
            val ry = (tx(points[2]) - center).getDistance().coerceAtLeast(1f)
            if (filled) drawOval(accent.copy(alpha = .14f), Offset(center.x - rx, center.y - ry), Size(rx * 2f, ry * 2f))
            drawOval(accent, Offset(center.x - rx, center.y - ry), Size(rx * 2f, ry * 2f), style = stroke)
        }
        Shape2DType.Rectangle, Shape2DType.Square -> {
            if (points.size < 2) return
            val a = offset(0); val b = offset(1); val width = b.x - a.x
            val height = if (type == Shape2DType.Square) width else b.y - a.y
            val path = Path().apply { moveTo(a.x, a.y); lineTo(a.x + width, a.y); lineTo(a.x + width, a.y + height); lineTo(a.x, a.y + height); close() }
            if (filled) drawPath(path, Brush.linearGradient(listOf(accent.copy(.20f), accent.copy(.07f), Color.Transparent)))
            drawPath(path, accent, style = stroke)
        }
        Shape2DType.Triangle, Shape2DType.Polygon -> {
            if (points.size < 3) return
            val path = Path().apply {
                val first = offset(0); moveTo(first.x, first.y)
                points.indices.drop(1).forEach { val point = offset(it); lineTo(point.x, point.y) }
                close()
            }
            if (filled) drawPath(path, Brush.linearGradient(listOf(accent.copy(.20f), accent.copy(.07f), Color.Transparent)))
            drawPath(path, accent, style = stroke)
        }
        Shape2DType.RegularPolygon -> {
            if (points.size < 2) return
            val center = points[0]; val radiusVector = points[1] - center
            val startAngle = kotlin.math.atan2(radiusVector.y, radiusVector.x)
            val radius = radiusVector.distanceTo(Vec2(0.0, 0.0))
            val vertices = (0 until 5).map { index ->
                val angle = startAngle + index * 2.0 * PI / 5.0
                Vec2(center.x + cos(angle) * radius, center.y + sin(angle) * radius)
            }
            val path = Path().apply {
                val first = tx(vertices.first()); moveTo(first.x, first.y)
                vertices.drop(1).forEach { val point = tx(it); lineTo(point.x, point.y) }
                close()
            }
            if (filled) drawPath(path, accent.copy(.18f))
            drawPath(path, accent, style = stroke)
        }
        Shape2DType.Arc -> {
            if (points.size < 2) return
            val a = offset(0); val radius = (offset(points.lastIndex) - a).getDistance().coerceAtLeast(70f)
            drawArc(accent, 205f, 115f, false, Offset(a.x - radius, a.y - radius), Size(radius * 2f, radius * 2f), style = stroke)
        }
    }
}
