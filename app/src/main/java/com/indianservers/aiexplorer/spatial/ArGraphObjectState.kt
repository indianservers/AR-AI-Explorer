package com.indianservers.aiexplorer.spatial

import com.indianservers.aiexplorer.core.Vec3
import com.indianservers.aiexplorer.workspace.WorkspaceState
import com.indianservers.aiexplorer.arengine.contract.*
import java.util.Base64

data class ArGraphObjectState(
    val position: Vec3 = Vec3(0.0,0.0,0.0),
    val rotation: Vec3 = Vec3(0.0,0.0,0.0),
    val scale: Double = 1.0,
    val visible: Boolean = true,
    val locked: Boolean = false,
    val deleted: Boolean = false,
    val label: String = "",
    val axisScale: Vec3 = Vec3(1.0,1.0,1.0),
) {
    fun transform() = ArLocalTransform(ArVector3(position.x,position.y,position.z), ArQuaternion.fromEulerDegrees(rotation.x,rotation.y,rotation.z), scale, ArVector3(axisScale.x,axisScale.y,axisScale.z))
    fun encode() = listOf(position.x,position.y,position.z,rotation.x,rotation.y,rotation.z,scale,visible,locked,deleted,Base64.getEncoder().encodeToString(label.toByteArray(Charsets.UTF_8)),axisScale.x,axisScale.y,axisScale.z).joinToString("|")
    companion object {
        fun decode(value: String?) = runCatching {
            val f = requireNotNull(value).split('|')
            ArGraphObjectState(Vec3(f[0].toDouble(),f[1].toDouble(),f[2].toDouble()),Vec3(f[3].toDouble(),f[4].toDouble(),f[5].toDouble()),f[6].toDouble().coerceIn(.1,10.0),f[7].toBooleanStrict(),f[8].toBooleanStrict(),f[9].toBooleanStrict(),String(Base64.getDecoder().decode(f[10]),Charsets.UTF_8),Vec3(f.getOrNull(11)?.toDouble() ?: 1.0,f.getOrNull(12)?.toDouble() ?: 1.0,f.getOrNull(13)?.toDouble() ?: 1.0))
        }.getOrDefault(ArGraphObjectState())
    }
}
fun WorkspaceState.arGraphObject(id: String) = ArGraphObjectState.decode(labSessionValues["arGraph.object.$id"])
class ArGraphGeometryCache {
    private data class Entry(val source: SpatialGeometry, val edits: Map<String,String>, val result: SpatialGeometry)
    private val entries = mutableMapOf<String,Entry>()
    private val overrides=mutableMapOf<String,Pair<String,SpatialGeometry>>()
    fun geometry(id: String, source: SpatialGeometry, state: WorkspaceState): SpatialGeometry {
        while(entries.size>64) entries.remove(entries.keys.first())
        while(overrides.size>64) overrides.remove(overrides.keys.first())
        val base = id.removeSuffix("-wireframe")
        val override = state.labSessionValues["arCad.mesh.$base"]
        if (override != null) return overrides[base]?.takeIf { it.first==override }?.second ?: ArCadTopology.decode(override).also { overrides[base]=override to it }
        val prefix = "arGraph.vertex.$base."
        val edits = state.labSessionValues.filterKeys { it.startsWith(prefix) }
        if (edits.isEmpty()) return source
        val previous = entries[id]
        if (previous?.source === source && previous.edits == edits) return previous.result
        val result = source.copy(vertices = source.vertices.mapIndexed { index, p ->
            val delta = edits["$prefix$index"]?.split(',')?.mapNotNull { it.toDoubleOrNull() }
            if (delta?.size == 3) p + Vec3(delta[0],delta[1],delta[2]) else p
        })
        entries[id] = Entry(source,edits,result)
        return result
    }
}
fun SpatialRenderScene.withArGraphObjects(state: WorkspaceState, cache: ArGraphGeometryCache):SpatialRenderScene {
    val nodes=state.arCadNodes(); val unavailable=mutableSetOf<String>()
    runCatching { ArCadDependencies.order(nodes) }.getOrDefault(nodes.keys.toList()).forEach { id -> if(state.arGraphObject(id).deleted || nodes[id]?.dependencies.orEmpty().any { it in unavailable }) unavailable+=id }
    return copy(primitives = primitives.map { primitive ->
    val id = primitive.id.removeSuffix("-wireframe")
    val ownerId=if(id.endsWith("-parameter-point")) id.removeSuffix("-parameter-point") else id
    val obj = state.arGraphObject(ownerId)
    val hiddenByDependency=ownerId in unavailable || primitive.dependencyIds.any { state.arGraphObject(it).deleted }
    primitive.copy(geometry = cache.geometry(primitive.id,primitive.geometry,state), localTransform = obj.transform(), visible = primitive.visible && obj.visible && !obj.deleted && !hiddenByDependency, selectable = primitive.selectable && !obj.locked, label = obj.label.ifBlank { primitive.label })
})
}
