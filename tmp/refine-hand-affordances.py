from pathlib import Path
r=Path(r'C:\Indian Servers\AIExplorer');p=r/'arengine/src/main/java/com/indianservers/aiexplorer/handintelligence/spatial/ContactSolver.kt';s=p.read_text(encoding='utf-8')
s=s.replace('position=position,precision=.8f)','position=position,normal=if(obj.semanticType==MathSemanticType.CIRCLE) (position-obj.center).unit() else Vec3.Zero,precision=.8f)')
s=s.replace('MathSemanticType.SPHERE,MathSemanticType.CIRCLE -> SemanticHitRegion("surface",RegionKind.SURFACE,position=position,normal=radial)','MathSemanticType.SPHERE,MathSemanticType.CIRCLE -> if((finger-projectedCenter).magnitude()<projectedSize*.25) region else SemanticHitRegion("surface",RegionKind.SURFACE,position=position,normal=radial)')
s=s.replace('val normal=if(hitIndex!=null)', 'var normal=if(hitIndex!=null)').replace('                            val localNormal=obj.orientation', '                            if(normal.dot(position-obj.center)<0) normal=normal*(-1.0)\n                            val localNormal=obj.orientation')
s=s.replace('            val alignment=if(hit!=null)', '''            val operation=when(region.kind) { RegionKind.VERTEX->MathInteraction.MOVE_VERTEX; RegionKind.EDGE->MathInteraction.MOVE_EDGE; RegionKind.FACE->MathInteraction.MOVE_FACE; RegionKind.VECTOR_HEAD->MathInteraction.EDIT_VECTOR_HEAD; RegionKind.VECTOR_ORIGIN->MathInteraction.EDIT_VECTOR_ORIGIN; RegionKind.RADIUS->MathInteraction.CHANGE_RADIUS; else->null }
            if(operation!=null && operation !in obj.allowed) region=SemanticHitRegion("body",RegionKind.BODY,position=position)
            val alignment=if(hit!=null)''')
p.write_text(s,encoding='utf-8')
p=r/'arengine/src/main/java/com/indianservers/aiexplorer/handintelligence/semantics/MathSemanticEngine.kt';s=p.read_text(encoding='utf-8').replace(' && (abs(d[axis])>=t.translation.magnitude()*.65 || state.phase==InteractionPhase.BEGIN)','').replace(' && (abs(t.scale-1)>.001 || abs(t.radialDelta)>=t.translation.magnitude()*.45 || state.phase==InteractionPhase.BEGIN)','');p.write_text(s,encoding='utf-8')
p=r/'arengine/src/main/java/com/indianservers/aiexplorer/handintelligence/intelligence/HandIntelligenceEngine.kt';s=p.read_text(encoding='utf-8').replace('support=null; candidate=null; releasingAt=null;', 'support=null; supportCandidate=null; candidate=null; armedAt=0; supportAt=0; releasingAt=null;');p.write_text(s,encoding='utf-8')
p=r/'app/src/main/java/com/indianservers/aiexplorer/MainActivity.kt';s=p.read_text(encoding='utf-8')
needle='''            state=state.copy(solids=state.solids.mapIndexed { i,old->if(i==solid) edited else old },modifiedAt=System.currentTimeMillis()); return'''
s=s.replace(needle,'''            if(base.type in setOf(SolidType.Cube,SolidType.Cuboid) && ops.any { it in setOf(com.indianservers.aiexplorer.handintelligence.MathInteraction.MOVE_VERTEX,com.indianservers.aiexplorer.handintelligence.MathInteraction.MOVE_EDGE) }) {
                val orientation=com.indianservers.aiexplorer.arengine.contract.ArQuaternion.fromEulerDegrees(base.rotation.x,base.rotation.y,base.rotation.z)
                val local=orientation.conjugate().rotate(t.translation)
                val target=orientation.conjugate().rotate(action.region.position-ArVector3(base.position.x,base.position.y,base.position.z))
                val signs=listOf(target.x,target.y,target.z).map { if(it>=0) 1.0 else -1.0 }
                val fixedAxis=if(com.indianservers.aiexplorer.handintelligence.MathInteraction.MOVE_EDGE in ops && geometry!=null) {
                    val edge=com.indianservers.aiexplorer.spatial.ArCadTopology.edges(geometry).getOrNull(action.region.index ?: -1)
                    edge?.let { (a,b) -> val dir=geometry.vertices[b]-geometry.vertices[a]; val v=orientation.conjugate().rotate(ArVector3(dir.x,dir.y,dir.z)); listOf(kotlin.math.abs(v.x),kotlin.math.abs(v.y),kotlin.math.abs(v.z)).indices.maxByOrNull { listOf(kotlin.math.abs(v.x),kotlin.math.abs(v.y),kotlin.math.abs(v.z))[it] } }
                } else null
                val old=listOf(base.width,base.height,base.depth);val shifts=MutableList(3) { 0.0 }
                val dims=old.mapIndexed { i,v -> val n=if(i==fixedAxis) v else (v+listOf(local.x,local.y,local.z)[i]*signs[i]).coerceIn(.02,100.0);shifts[i]=(n-v)*signs[i]*.5;n }
                val shifted=orientation.rotate(ArVector3(shifts[0],shifts[1],shifts[2]))
                edited=base.copy(type=if(dims!=old) SolidType.Cuboid else base.type,width=dims[0],height=dims[1],depth=dims[2],position=base.position+Vec3(shifted.x,shifted.y,shifted.z))
            }
'''+needle,1)
s=s.replace('val vertex=action.region.index?.takeIf { com.indianservers.aiexplorer.handintelligence.MathInteraction.MOVE_VERTEX in ops }', '''val vertex=action.region.index?.takeIf { com.indianservers.aiexplorer.handintelligence.MathInteraction.MOVE_VERTEX in ops }
            val edge=if(com.indianservers.aiexplorer.handintelligence.MathInteraction.MOVE_EDGE in ops && geometry!=null) com.indianservers.aiexplorer.spatial.ArCadTopology.edges(geometry).getOrNull(action.region.index ?: -1) else null''')
s=s.replace('if(vertex!=null) { if(i==vertex) point+Vec2(delta.x,delta.y) else point } else {','if(vertex!=null || edge!=null) { if(i==vertex || i==edge?.first || i==edge?.second) point+Vec2(delta.x,delta.y) else point } else {')
p.write_text(s,encoding='utf-8')
p=r/'app/src/main/java/com/indianservers/aiexplorer/HandMathSceneAdapter.kt';s=p.read_text(encoding='utf-8')
s=s.replace('val node=state.arCadNodes()[p.id];','val nodes=state.arCadNodes(); val node=nodes[p.id];')
s=s.replace('val allowed=if(p.dependencyIds', 'val solid=state.solids.getOrNull(p.id.removePrefix("solid-").toIntOrNull() ?: -1)\n        val allowed=if((solid!=null && solid.type !in setOf(com.indianservers.aiexplorer.core.SolidType.Cube,com.indianservers.aiexplorer.core.SolidType.Cuboid,com.indianservers.aiexplorer.core.SolidType.Sphere)) || p.dependencyIds')
s=s.replace('locked=p.id in selection.lockedObjectIds || pose.locked','locked=p.id in selection.lockedObjectIds || pose.locked || state.shapes.firstOrNull { it.id==p.id }?.locked==true')
p.write_text(s,encoding='utf-8')
