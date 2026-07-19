package com.indianservers.aiexplorer.curriculum.production

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun CurriculumDiagramView(spec: CurriculumDiagramSpecification) {
    require(spec.id in CurriculumRenderedDiagramRegistry.ids) { "No renderer for ${spec.id}" }
    var labelsVisible by remember(spec.id) { mutableStateOf(true) }
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.tertiary
    val ink = MaterialTheme.colorScheme.onSurface
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(spec.title, style = MaterialTheme.typography.titleSmall)
            Canvas(Modifier.fillMaxWidth().height(190.dp)) {
                val w = size.width; val h = size.height; val stroke = 4f
                fun line(a: Offset, b: Offset, color: Color = ink, width: Float = stroke) = drawLine(color, a, b, width, StrokeCap.Round)
                fun node(x: Float, y: Float, color: Color = primary, radius: Float = 18f) = drawCircle(color, radius, Offset(x, y))
                when (spec.id) {
                    "math-c9-sequence-table-graph" -> {
                        line(Offset(w*.12f,h*.85f), Offset(w*.92f,h*.85f)); line(Offset(w*.12f,h*.85f), Offset(w*.12f,h*.12f))
                        val pts=(0..4).map { Offset(w*(.2f+it*.16f), h*(.76f-it*.13f)) }; pts.zipWithNext().forEach { line(it.first,it.second,secondary) }; pts.forEach { node(it.x,it.y) }
                        line(Offset(pts[1].x,pts[1].y),Offset(pts[2].x,pts[1].y),secondary,2f); line(Offset(pts[2].x,pts[1].y),pts[2],secondary,2f)
                    }
                    "physics-c9-position-time-graph" -> {
                        line(Offset(w*.12f,h*.85f),Offset(w*.92f,h*.85f)); line(Offset(w*.12f,h*.85f),Offset(w*.12f,h*.12f))
                        val a=Offset(w*.2f,h*.72f); val b=Offset(w*.48f,h*.4f); val c=Offset(w*.7f,h*.4f); val d=Offset(w*.88f,h*.68f)
                        line(a,b,primary); line(b,c,primary); line(c,d,primary); listOf(a,b,c,d).forEach { node(it.x,it.y,secondary,12f) }
                        line(Offset(a.x,b.y),b,secondary,2f); line(a,Offset(a.x,b.y),secondary,2f)
                    }
                    "physics-c9-path-displacement" -> {
                        val path=Path().apply { moveTo(w*.12f,h*.72f); cubicTo(w*.3f,h*.08f,w*.62f,h*.95f,w*.88f,h*.25f) }
                        drawPath(path,primary,style=Stroke(6f)); line(Offset(w*.12f,h*.72f),Offset(w*.88f,h*.25f),secondary,5f); node(w*.12f,h*.72f); node(w*.88f,h*.25f)
                    }
                    "chem-c10-reaction-conservation-model" -> {
                        line(Offset(w*.49f,h*.15f),Offset(w*.49f,h*.85f),ink,2f)
                        listOf(.18f to .3f,.3f to .55f,.18f to .72f,.36f to .24f).forEach { node(w*it.first,h*it.second,primary) }
                        listOf(.62f to .3f,.72f to .3f,.66f to .58f,.78f to .58f).forEach { node(w*it.first,h*it.second,if(it.first<.7f)primary else secondary) }
                        listOf(.26f to .38f,.34f to .68f,.82f to .3f,.6f to .58f).forEach { node(w*it.first,h*it.second,secondary,13f) }
                    }
                    "chem-c10-reaction-apparatus" -> {
                        drawRoundRect(primary,Offset(w*.18f,h*.18f),androidx.compose.ui.geometry.Size(w*.18f,h*.55f),CornerRadius(18f,18f),style=Stroke(5f)); line(Offset(w*.27f,h*.18f),Offset(w*.27f,h*.08f),primary); line(Offset(w*.27f,h*.08f),Offset(w*.72f,h*.08f),primary); line(Offset(w*.72f,h*.08f),Offset(w*.72f,h*.56f),primary)
                        drawRect(secondary.copy(alpha=.25f),Offset(w*.58f,h*.48f),androidx.compose.ui.geometry.Size(w*.28f,h*.35f)); line(Offset(w*.58f,h*.48f),Offset(w*.58f,h*.83f)); line(Offset(w*.86f,h*.48f),Offset(w*.86f,h*.83f)); line(Offset(w*.58f,h*.83f),Offset(w*.86f,h*.83f))
                    }
                    "bio-c10-life-process-network" -> {
                        val centre=Offset(w*.5f,h*.5f); node(centre.x,centre.y,secondary,30f)
                        listOf(Offset(w*.18f,h*.22f),Offset(w*.82f,h*.22f),Offset(w*.18f,h*.78f),Offset(w*.82f,h*.78f)).forEach { line(it,centre,primary); node(it.x,it.y,primary,23f) }
                    }
                    "bio-c10-nephron-structure-function" -> {
                        drawCircle(primary.copy(alpha=.25f),28f,Offset(w*.25f,h*.25f)); repeat(4){i->drawCircle(primary,8f,Offset(w*(.21f+i*.025f),h*(.23f+(i%2)*.05f)))}
                        val tubule=Path().apply { moveTo(w*.28f,h*.28f); cubicTo(w*.5f,h*.1f,w*.35f,h*.65f,w*.58f,h*.55f); cubicTo(w*.75f,h*.48f,w*.62f,h*.8f,w*.8f,h*.78f) }
                        drawPath(tubule,secondary,style=Stroke(7f)); line(Offset(w*.8f,h*.2f),Offset(w*.8f,h*.9f),primary,9f); line(Offset(w*.8f,h*.78f),Offset(w*.8f,h*.78f),primary)
                    }
                    "math-c10-real-factor-exponent-map" -> {
                        (0..4).forEach { i -> line(Offset(w*.12f+i*w*.18f,h*.18f),Offset(w*.12f+i*w*.18f,h*.82f),ink,2f) }
                        (0..4).forEach { i -> line(Offset(w*.12f,h*.18f+i*h*.16f),Offset(w*.84f,h*.18f+i*h*.16f),ink,2f) }
                        listOf(Offset(w*.3f,h*.42f),Offset(w*.48f,h*.42f),Offset(w*.3f,h*.58f),Offset(w*.66f,h*.58f)).forEach { node(it.x,it.y,primary,10f) }
                        line(Offset(w*.12f,h*.66f),Offset(w*.84f,h*.66f),secondary,5f)
                    }
                    "physics-c10-electric-circuit" -> {
                        val left=w*.15f; val right=w*.85f; val top=h*.25f; val bottom=h*.75f
                        line(Offset(left,top),Offset(right,top),primary); line(Offset(right,top),Offset(right,bottom),primary); line(Offset(right,bottom),Offset(left,bottom),primary); line(Offset(left,bottom),Offset(left,top),primary)
                        drawCircle(ink,24f,Offset(w*.42f,top),style=Stroke(5f)); drawCircle(ink,24f,Offset(w*.62f,bottom),style=Stroke(5f)); line(Offset(w*.55f,top-18f),Offset(w*.7f,top-18f),secondary,6f)
                        line(Offset(w*.55f,top+18f),Offset(w*.7f,top+18f),secondary,6f); line(Offset(w*.55f,top-18f),Offset(w*.55f,top+18f),secondary,3f); line(Offset(w*.7f,top-18f),Offset(w*.7f,top+18f),secondary,3f)
                    }
                    "physics-c10-vi-graph" -> {
                        line(Offset(w*.12f,h*.85f),Offset(w*.92f,h*.85f)); line(Offset(w*.12f,h*.85f),Offset(w*.12f,h*.12f)); line(Offset(w*.16f,h*.8f),Offset(w*.86f,h*.2f),primary,6f)
                        listOf(.25f to .72f,.4f to .58f,.56f to .48f,.72f to .32f).forEach { node(w*it.first,h*it.second,secondary,10f) }
                    }
                    "chem-c10-ph-scale" -> {
                        (0..14).forEach { i -> val color=Color.hsv(i*240f/14f,0.7f,0.9f); drawRect(color,Offset(w*(.08f+i*.058f),h*.4f),androidx.compose.ui.geometry.Size(w*.058f,h*.25f)) }
                        line(Offset(w*.08f,h*.7f),Offset(w*.892f,h*.7f),ink,3f); line(Offset(w*.486f,h*.32f),Offset(w*.486f,h*.72f),ink,5f)
                    }
                    "bio-c10-reflex-arc" -> {
                        val pts=listOf(Offset(w*.12f,h*.55f),Offset(w*.3f,h*.3f),Offset(w*.5f,h*.5f),Offset(w*.7f,h*.3f),Offset(w*.88f,h*.55f)); pts.zipWithNext().forEach { line(it.first,it.second,primary,6f) }; pts.forEachIndexed { i,p -> node(p.x,p.y,if(i==2)secondary else primary,20f) }; line(Offset(w*.5f,h*.5f),Offset(w*.62f,h*.82f),secondary,4f)
                    }
                }
            }
            if (labelsVisible) Text(spec.requiredLabels.joinToString("  •  ") { it.label }, style = MaterialTheme.typography.bodySmall)
            Text(spec.accessibilityDescription, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { labelsVisible = !labelsVisible }) { Text(if(labelsVisible) "Hide labels" else "Show labels") }
        }
    }
}
