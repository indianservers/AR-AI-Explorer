package com.indianservers.aiexplorer.handintelligence.spatial
import com.indianservers.aiexplorer.handintelligence.*
import kotlin.math.*

class ContactSolver {
    fun candidates(hand:IntelligentHand,scene:MathSceneSnapshot,focus:String?,dwell:Long):List<TargetCandidate> {
        val finger=if(hand.pose.pointScore>.55) hand.pose.indexTip else hand.filteredPinch
        val ray=scene.mapper.screenToRay(finger)
        val predicted=finger+hand.motion.velocity*.08
        val tolerance=(hand.pose.handScale*.22).coerceIn(.008,.045)
        val candidates=scene.objects.asSequence().filter { it.visible && !it.locked && it.vertices.isNotEmpty() }.mapNotNull { obj ->
            val projectedCenter=scene.mapper.project(obj.center) ?: return@mapNotNull null
            val projectedSize=max(.025,obj.boundsCorners.maxOfOrNull { v -> scene.mapper.project(v)?.let { (it-projectedCenter).magnitude() } ?: 0.0 } ?: 0.0)
            if((finger-projectedCenter).magnitude()>projectedSize*2.5+tolerance && !obj.selected) return@mapNotNull null
            var hit:Vec3?=null; var hitIndex:Int?=null; var nearest=Double.MAX_VALUE
            val triangles=obj.triangles
            var i=0
            while(i+2<triangles.size) {
                val a=obj.vertices[triangles[i]]; val b=obj.vertices[triangles[i+1]]; val c=obj.vertices[triangles[i+2]]
                val ex=b.x-a.x;val ey=b.y-a.y;val ez=b.z-a.z
                val fx=c.x-a.x;val fy=c.y-a.y;val fz=c.z-a.z
                val dx=ray.direction.x;val dy=ray.direction.y;val dz=ray.direction.z
                val hx=dy*fz-dz*fy;val hy=dz*fx-dx*fz;val hz=dx*fy-dy*fx
                val det=ex*hx+ey*hy+ez*hz
                if(abs(det)>1e-9) {
                    val inv=1/det;val sx=ray.origin.x-a.x;val sy=ray.origin.y-a.y;val sz=ray.origin.z-a.z
                    val u=(sx*hx+sy*hy+sz*hz)*inv
                    if(u>=0 && u<=1) {
                        val qx=sy*ez-sz*ey;val qy=sz*ex-sx*ez;val qz=sx*ey-sy*ex
                        val v=(dx*qx+dy*qy+dz*qz)*inv
                        if(v>=0 && u+v<=1) { val t=(fx*qx+fy*qy+fz*qz)*inv; if(t>0 && t<nearest) { nearest=t; hit=ray.origin+ray.direction*t;hitIndex=i/3 } }
                    }
                }
                i+=3
            }
            var region=SemanticHitRegion("body",RegionKind.BODY,position=hit ?: obj.center)
            var distance=if(hit!=null) 0.0 else Double.MAX_VALUE
            var position=hit ?: obj.center
            // Vertices/edges are actual topology, with a hand-size-relative selection aperture.
            if(obj.vertices.size<=256 && obj.semanticType !in setOf(MathSemanticType.SPHERE,MathSemanticType.CIRCLE,MathSemanticType.FUNCTION_SURFACE)) obj.vertices.forEachIndexed { index,v ->
                if(hit!=null && (v-ray.origin).dot(ray.direction)>nearest+obj.size*.03) return@forEachIndexed
                val screen=scene.mapper.project(v) ?: return@forEachIndexed
                val d=(screen-finger).magnitude()
                if(d<tolerance && d<distance.coerceAtLeast(tolerance)) {
                    val kind=if(obj.semanticType==MathSemanticType.VECTOR) if(index==0) RegionKind.VECTOR_ORIGIN else RegionKind.VECTOR_HEAD else RegionKind.VERTEX
                    val semanticIndex=if(obj.semanticType==MathSemanticType.VECTOR && index>0) 1 else index
                    position=obj.vertices[semanticIndex];region=SemanticHitRegion("vertex-$semanticIndex",kind,semanticIndex,position=position,precision=.95f);distance=d
                }
            }
            if(region.kind==RegionKind.BODY) obj.edges.forEachIndexed { index,(ia,ib) ->
                val a=obj.vertices[ia]; val b=obj.vertices[ib]; val sa=scene.mapper.project(a) ?: return@forEachIndexed; val sb=scene.mapper.project(b) ?: return@forEachIndexed
                val d=sb-sa; val t=((finger-sa).dot(d)/d.dot(d).coerceAtLeast(1e-12)).coerceIn(0.0,1.0); val q=sa+d*t; val gap=(q-finger).magnitude()
                val onEdge=a+(b-a)*t
                if(hit!=null && (onEdge-ray.origin).dot(ray.direction)>nearest+obj.size*.03) return@forEachIndexed
                if(gap<tolerance*.7) { position=onEdge; distance=gap; region=SemanticHitRegion("edge-$index",if(obj.semanticType==MathSemanticType.CIRCLE) RegionKind.RADIUS else if(obj.triangles.isEmpty()) RegionKind.CURVE else RegionKind.EDGE,index,position=position,normal=if(obj.semanticType==MathSemanticType.CIRCLE) (position-obj.center).unit() else Vec3.Zero,precision=.8f) }
            }
            if(region.kind==RegionKind.BODY && hit!=null) {
                val radial=(position-obj.center).unit()
                region=when(obj.semanticType) {
                    MathSemanticType.SPHERE,MathSemanticType.CIRCLE -> if((finger-projectedCenter).magnitude()<projectedSize*.25) region else SemanticHitRegion("surface",RegionKind.SURFACE,position=position,normal=radial)
                    MathSemanticType.GRAPH_3D,MathSemanticType.FUNCTION_SURFACE -> SemanticHitRegion("surface",RegionKind.SURFACE,hitIndex,position=position,normal=radial)
                    MathSemanticType.CUBE,MathSemanticType.CUBOID,MathSemanticType.CYLINDER,MathSemanticType.CONE -> {
                        if((finger-projectedCenter).magnitude()<projectedSize*.25) region else {
                            var normal=if(hitIndex!=null) { val f=hitIndex!!*3; (obj.vertices[triangles[f+1]]-obj.vertices[triangles[f]]).cross(obj.vertices[triangles[f+2]]-obj.vertices[triangles[f]]).unit() } else radial
                            if(normal.dot(position-obj.center)<0) normal=normal*(-1.0)
                            val localNormal=obj.orientation.conjugate().rotate(normal)
                            val axis=if(abs(localNormal.x)>abs(localNormal.y) && abs(localNormal.x)>abs(localNormal.z)) 0 else if(abs(localNormal.y)>abs(localNormal.z)) 1 else 2
                            SemanticHitRegion("face-${hitIndex ?: axis}",RegionKind.FACE,hitIndex,axis,if(listOf(localNormal.x,localNormal.y,localNormal.z)[axis]>=0) 1.0 else -1.0,position,normal,.4f)
                        }
                    }
                    else -> region
                }
            }
            if(hit==null && distance>tolerance) return@mapNotNull null
            val operation=when(region.kind) { RegionKind.VERTEX->MathInteraction.MOVE_VERTEX; RegionKind.EDGE->MathInteraction.MOVE_EDGE; RegionKind.FACE->MathInteraction.MOVE_FACE; RegionKind.VECTOR_HEAD->MathInteraction.EDIT_VECTOR_HEAD; RegionKind.VECTOR_ORIGIN->MathInteraction.EDIT_VECTOR_ORIGIN; RegionKind.RADIUS->MathInteraction.CHANGE_RADIUS; else->null }
            if(operation!=null && operation !in obj.allowed) region=SemanticHitRegion("body",RegionKind.BODY,position=position)
            val alignment=if(hit!=null) 1f else clamp(1-distance/tolerance)
            val proximity=clamp(1-distance.coerceAtMost(tolerance)/tolerance)
            val hover=clamp(dwell/180.0); val focusScore=if(obj.objectId==focus) 1f else if(obj.selected) .7f else .25f
            val trajectory=clamp(1-(predicted-(scene.mapper.project(position) ?: finger)).magnitude()/(projectedSize+tolerance))
            val rayDistance=if(hit!=null) nearest else (position-ray.origin).dot(ray.direction)
            val depth=clamp(1/(1+abs(rayDistance)*.01))
            val score=alignment*.35f+proximity*.20f+hover*.15f+trajectory*.10f+focusScore*.10f+depth*.05f+.05f
            TargetCandidate(obj.objectId,region,position,alignment,proximity,hover,depth,focusScore,1f,trajectory,score,rayDistance)
        }.toList()
        val front=candidates.minOfOrNull { it.rayDistance } ?: return emptyList()
        // An overlapping rear surface cannot win simply because it was previously selected.
        return candidates.filter { it.rayDistance<=front+.02 }.sortedByDescending { it.finalScore }.take(8)
    }
}
class SpatialConflictResolver { fun uiOwns(p:Vec3,scene:MathSceneSnapshot)=scene.uiRegions.any { it.contains(p) } }
class MotionPredictor { fun predict(p:Vec3,v:Vec3,horizon:Double=.08):Vec3 { val offset=v*horizon.coerceIn(.05,.15); return p+offset*(min(1.0,.04/offset.magnitude().coerceAtLeast(1e-9))) } }
