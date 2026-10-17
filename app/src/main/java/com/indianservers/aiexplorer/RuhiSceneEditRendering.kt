package com.indianservers.aiexplorer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.indianservers.aiexplorer.assistant.workspace.*
import com.indianservers.aiexplorer.core.Vec2
import com.indianservers.aiexplorer.workspace.WorkspaceState

internal fun DrawScope.drawRuhiSceneGeometryOverlay(s:WorkspaceState,tx:(Vec2)->Offset) {
    val color=WorkspaceVisualStyles.ReferenceYellow
    s.shapes.filter{it.visible}.forEach{shape->
        if(s.labSessionValues[RuhiSceneEditLanguage.PREFIX+"shapeLabel.${shape.id}"]=="true") {
            val anchor=RuhiCircleLanguage.describe(s,shape)?.let{it.center+Vec2(0.0,it.radius)} ?: shape.pointIndices.mapNotNull(s.points::getOrNull).takeIf{it.isNotEmpty()}?.let{points->points.reduce{a,b->a+b}*(1.0/points.size)}
            anchor?.let{val point=tx(it);if(point.x in 0f..size.width&&point.y in 0f..size.height)drawGraphLabel(shape.name,point+Offset(15f,-40f),color)}
        }
        if(s.labSessionValues[RuhiSceneEditLanguage.PREFIX+"endpoints.${shape.id}"]=="true")shape.pointIndices.take(2).mapNotNull(s.points::getOrNull).forEach{point->val p=tx(point);drawCircle(color.copy(.2f),18f,p);drawCircle(color,12f,p,style=Stroke(3f))}
    }
}
