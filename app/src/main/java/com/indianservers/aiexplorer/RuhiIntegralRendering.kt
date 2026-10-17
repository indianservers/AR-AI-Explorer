package com.indianservers.aiexplorer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.indianservers.aiexplorer.assistant.workspace.RuhiIntegralLanguage
import com.indianservers.aiexplorer.assistant.workspace.RuhiIntersectionLanguage
import com.indianservers.aiexplorer.core.Vec2

/** Integral fills and approximating panels share the native graph's exact world-to-canvas transform. */
internal fun DrawScope.drawRuhiIntegralOverlay(d:RuhiIntegralLanguage.Drawing,tx:(Vec2)->Offset,minX:Double,maxX:Double,minY:Double,maxY:Double) {
    if(d.diagnostic.isNotEmpty())return
    d.bands.forEach{band->
        val vertices=band.vertices.map(tx);if(vertices.any{!it.x.isFinite()||!it.y.isFinite()})return@forEach
        val path=Path().apply{vertices.forEachIndexed{i,p->if(i==0)moveTo(p.x,p.y)else lineTo(p.x,p.y)};close()}
        val color=if(band.sign>=0)WorkspaceVisualStyles.ReferenceCyan else WorkspaceVisualStyles.ReferenceMagenta
        drawPath(path,color.copy(alpha=if(band.approximation).10f else .23f))
        if(band.approximation)drawPath(path,color.copy(alpha=.80f),style=Stroke(1.5f))
    }
    drawRuhiIntersectionOverlay(RuhiIntersectionLanguage.Drawing(emptyList(),emptyList(),emptySet(),d.helpers,emptySet()),tx,minX,maxX,minY,maxY)
}
