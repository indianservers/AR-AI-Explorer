package com.indianservers.aiexplorer

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.indianservers.aiexplorer.spatial.*
import com.indianservers.aiexplorer.arengine.interaction.*
import com.indianservers.aiexplorer.arengine.contract.ArVector3

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ArCadSceneSubstructure(obj:SpatialPrimitive,onSelect:(ArPickHit)->Unit) {
    var expanded by remember(obj.id) { mutableStateOf(false) }
    GlowButton(if(expanded) "Hide substructure" else "Faces / edges / vertices") { expanded=!expanded }
    if(expanded) {
        Text("${obj.metadata["cadType"] ?: obj.kind.name} · dependencies: ${obj.dependencyIds.joinToString()}",color=Color.White)
        ArSubObjectKind.entries.filter { it!=ArSubObjectKind.Whole }.forEach { kind ->
            val size=when(kind) { ArSubObjectKind.Vertex -> obj.geometry.vertices.size; ArSubObjectKind.Edge -> ArCadTopology.edges(obj.geometry).size; else -> (0 until obj.geometry.triangles.size/3).map { ArCadTopology.face(obj.geometry,it).min() }.distinct().size }
            Text("${kind.name}: $size",color=Color.White)
            FlowRow { (0 until size.coerceAtMost(48)).forEach { index -> GlowButton("${kind.name} ${index+1}") { val actualIndex=if(kind==ArSubObjectKind.Face) (0 until obj.geometry.triangles.size/3).map { ArCadTopology.face(obj.geometry,it).min() }.distinct()[index] else index; val ids=ArCadTopology.vertices(obj.geometry,kind,actualIndex); val p=obj.geometry.vertices[ids.first()]; onSelect(ArPickHit(obj.id,kind,actualIndex,0.0,ArVector3(p.x,p.y,p.z))) } } }
            if(size>48) Text("Use viewport picking for all $size entities",color=Color.White)
        }
    }
}
