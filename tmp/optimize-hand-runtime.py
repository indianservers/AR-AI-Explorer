from pathlib import Path
r=Path(r'C:\Indian Servers\AIExplorer');p=r/'arengine/src/main/java/com/indianservers/aiexplorer/handintelligence/spatial/ContactSolver.kt';s=p.read_text(encoding='utf-8');a=s.index('                val e1=b-a;');b=s.index('            }\n            var region=',a)
s=s[:a]+'''                val ex=b.x-a.x;val ey=b.y-a.y;val ez=b.z-a.z
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
'''+s[b:];p.write_text(s,encoding='utf-8')
p=r/'arengine/src/main/java/com/indianservers/aiexplorer/handintelligence/intelligence/HandIntelligenceEngine.kt';s=p.read_text(encoding='utf-8').replace('val candidateTarget=targets.predict(hand,scene,time,locked?.objectId ?: lastState?.targetObjectId)','val candidateTarget=if(locked==null) targets.predict(hand,scene,time,lastState?.targetObjectId) else null')
s=s.replace('val otherTarget=other?.let { contacts.candidates(it,scene,locked!!.objectId,180).firstOrNull { c -> c.objectId==locked!!.objectId && c.finalScore>.8 } }','val otherTarget=other?.let { if(it.id==support) heldTarget else contacts.candidates(it,scene,locked!!.objectId,180).firstOrNull { c -> c.objectId==locked!!.objectId && c.finalScore>.8 } }')
p.write_text(s,encoding='utf-8')
p=r/'app/src/main/java/com/indianservers/aiexplorer/spatial/ARCoreCompositorView.kt';s=p.read_text(encoding='utf-8').replace('if(quality.targetFramesPerSecond<=24) 180 else 100','if(quality.targetFramesPerSecond<=24) 42 else 33');p.write_text(s,encoding='utf-8')
p=r/'app/src/main/java/com/indianservers/aiexplorer/MainActivity.kt';s=p.read_text(encoding='utf-8').replace('val edited=base.copy(start=if(head) base.start else base.start+delta,end=if(origin) base.end else base.end+delta)','''val center=(base.start+base.end)*.5
            val orientation=com.indianservers.aiexplorer.arengine.contract.ArQuaternion.fromEulerDegrees(rotation.x,rotation.y,rotation.z)
            fun moved(point:Vec3):Vec3 { val p=point-center;val q=orientation.rotate(ArVector3(p.x,p.y,p.z)*t.scale);return center+Vec3(q.x,q.y,q.z)+delta }
            val edited=if(head || origin) base.copy(start=if(head) base.start else base.start+delta,end=if(origin) base.end else base.end+delta) else base.copy(start=moved(base.start),end=moved(base.end))''')
p.write_text(s,encoding='utf-8')
p=r/'app/src/main/java/com/indianservers/aiexplorer/HandMathSceneAdapter.kt';s=p.read_text(encoding='utf-8')
s=s.replace('object HandMathSceneAdapter {','''object HandMathSceneAdapter {
    private val expressions=object:LinkedHashMap<String,com.indianservers.aiexplorer.core.Expression>(16,.75f,true) { override fun removeEldestEntry(eldest:MutableMap.MutableEntry<String,com.indianservers.aiexplorer.core.Expression>?)=size>16 }
    @Synchronized private fun compile(expression:String)=expressions[expression] ?: runCatching { ExpressionEngine().compile(expression).also { expressions[expression]=it } }.getOrNull()
    private val snapshots=java.util.IdentityHashMap<SpatialPrimitive,MathObjectSnapshot>()''')
s=s.replace('        val t=p.localTransform','''        val pose=state.arGraphObject(p.id)
        val locked=p.id in selection.lockedObjectIds || pose.locked || state.shapes.firstOrNull { it.id==p.id }?.locked==true
        val selected=p.id in selection.objectIds
        snapshots[p]?.takeIf { it.locked==locked && it.selected==selected }?.let { return@map it }
        val t=p.localTransform''',1)
s=s.replace('val nodes=state.arCadNodes(); val node=nodes[p.id]; val pose=state.arGraphObject(p.id)','val nodes=state.arCadNodes(); val node=nodes[p.id]')
s=s.replace('expression?.let { runCatching { ExpressionEngine().compile(it) }.getOrNull() }','expression?.let(::compile)')
s=s.replace('locked=p.id in selection.lockedObjectIds || pose.locked || state.shapes.firstOrNull { it.id==p.id }?.locked==true,selected=p.id in selection.objectIds','locked=locked,selected=selected')
s=s.replace('else t.orientation)\n    }','else t.orientation).also { if(snapshots.size>=64) snapshots.clear(); snapshots[p]=it }\n    }')
p.write_text(s,encoding='utf-8')
