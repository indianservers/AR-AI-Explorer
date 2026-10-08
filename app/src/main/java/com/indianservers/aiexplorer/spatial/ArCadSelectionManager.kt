package com.indianservers.aiexplorer.spatial

import com.indianservers.aiexplorer.arengine.interaction.*

enum class ArCadEntityKind { Object, Face, Edge, Vertex, Point, Curve, Plane, Vector, ControlPoint, Intersection }
data class ArCadEntitySelection(val objectId:String,val kind:ArCadEntityKind,val index:Int? = null,val connectedVertices:Set<Int> = emptySet())
/** Semantic hierarchy resolved from the same exact hit used by touch and hand rays. */
object ArCadSelectionManager {
    fun resolve(scene:SpatialRenderScene,selection:ArSelectionState):ArCadEntitySelection? {
        val id=selection.primaryObjectId ?: return null
        val obj=scene.primitives.firstOrNull { it.id==id } ?: return null
        val hit=selection.subObject
        val kind=when(hit?.kind) {
            ArSubObjectKind.Face -> ArCadEntityKind.Face
            ArSubObjectKind.Edge -> ArCadEntityKind.Edge
            ArSubObjectKind.Vertex -> if(obj.metadata["cadType"] in setOf("Curve","Vector")) ArCadEntityKind.ControlPoint else ArCadEntityKind.Vertex
            else -> when(obj.metadata["cadType"]) { "Point","Midpoint" -> ArCadEntityKind.Point; "Intersection" -> ArCadEntityKind.Intersection; "Curve","Circle","Line","Segment","Ray" -> ArCadEntityKind.Curve; "Plane" -> ArCadEntityKind.Plane; "Vector" -> ArCadEntityKind.Vector; else -> ArCadEntityKind.Object }
        }
        val indices=hit?.subObjectIndex?.let { index -> runCatching { ArCadTopology.vertices(obj.geometry,hit.kind,index) }.getOrDefault(emptySet()) } ?: emptySet()
        return ArCadEntitySelection(id,kind,hit?.subObjectIndex,indices)
    }
}
