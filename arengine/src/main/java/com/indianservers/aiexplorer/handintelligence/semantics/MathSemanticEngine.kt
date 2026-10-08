package com.indianservers.aiexplorer.handintelligence.semantics
import com.indianservers.aiexplorer.handintelligence.*
import kotlin.math.*

class MathSemanticEngine {
    fun resolve(state:HandIntelligenceState,spatial:SpatialFrame,scene:MathSceneSnapshot):MathInteractionFrame {
        val target=state.target ?: spatial.primaryTarget ?: return MathInteractionFrame(state,spatial,null)
        val obj=scene.objects.firstOrNull { it.objectId==target.objectId } ?: return MathInteractionFrame(state,spatial,null)
        if(obj.locked || !obj.visible) return MathInteractionFrame(state,spatial,null)
        val t=spatial.twoHandTransform ?: spatial.oneHandTransform ?: SpatialTransformIntent()
        val operations=linkedSetOf<MathInteraction>()
        if(state.phase==InteractionPhase.INSPECT) operations+=MathInteraction.INSPECT
        else when(target.region.kind) {
            RegionKind.VECTOR_ORIGIN -> operations+=MathInteraction.EDIT_VECTOR_ORIGIN
            RegionKind.VECTOR_HEAD -> operations+=MathInteraction.EDIT_VECTOR_HEAD
            RegionKind.VERTEX -> operations+=if(obj.semanticType in setOf(MathSemanticType.GRAPH_2D,MathSemanticType.GRAPH_3D)) MathInteraction.EDIT_GRAPH_POINT else MathInteraction.MOVE_VERTEX
            RegionKind.EDGE -> operations+=MathInteraction.MOVE_EDGE
            RegionKind.FACE -> {
                val axis=target.region.axis
                val local=obj.orientation.conjugate().rotate(t.translation)
                val d=listOf(local.x,local.y,local.z)
                val stretch=obj.semanticType in setOf(MathSemanticType.CUBE,MathSemanticType.CUBOID,MathSemanticType.CONE,MathSemanticType.CYLINDER) && axis!=null
                if(stretch) operations+=listOf(MathInteraction.STRETCH_X,MathInteraction.STRETCH_Y,MathInteraction.STRETCH_Z)[axis!!] else operations+=MathInteraction.MOVE_FACE
            }
            RegionKind.RADIUS,RegionKind.SURFACE -> {
                if(obj.semanticType in setOf(MathSemanticType.SPHERE,MathSemanticType.CIRCLE)) operations+=MathInteraction.CHANGE_RADIUS else operations+=MathInteraction.TRANSLATE
            }
            else -> operations+=MathInteraction.TRANSLATE
        }
        if(state.phase==InteractionPhase.UPDATE && target.region.kind in setOf(RegionKind.BODY,RegionKind.SURFACE,RegionKind.CURVE)) {
            if(t.rotation.magnitude()>.01) operations+=MathInteraction.ROTATE
            if(abs(t.scale-1)>.001 && MathInteraction.CHANGE_RADIUS !in operations) operations+=if(obj.semanticType in setOf(MathSemanticType.SPHERE,MathSemanticType.CIRCLE)) MathInteraction.CHANGE_RADIUS else MathInteraction.SCALE
        }
        operations.retainAll(obj.allowed)
        val p=target.position
        val inspection=if(state.phase==InteractionPhase.INSPECT) linkedMapOf("x" to p.x,"y" to p.y,"z" to p.z).apply {
            if(obj.semanticType==MathSemanticType.VECTOR && obj.vertices.size>=2) { val d=obj.vertices[1]-obj.vertices[0]; put("magnitude",d.magnitude()); put("dx",d.x); put("dy",d.y); put("dz",d.z) }
            if(obj.semanticType==MathSemanticType.SPHERE) { put("radius",obj.radius); put("volume",4*PI*obj.radius.pow(3)/3) }
                    if(target.region.kind==RegionKind.CURVE) obj.edges.getOrNull(target.region.index ?: -1)?.let { (a,b) -> val d=obj.vertices[b]-obj.vertices[a]; if(abs(d.x)>1e-8) put("sampled slope",d.y/d.x) }
            obj.inspect?.let { evaluate -> putAll(runCatching { evaluate(p) }.getOrDefault(emptyMap())) }
        } else emptyMap()
        return MathInteractionFrame(state,spatial,MathAction(obj.objectId,target.region,operations,t,inspection))
    }
}
