package com.indianservers.aiexplorer.assistant.workspace

import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import kotlin.math.*

object RuhiTransform3DOracle {
    private fun rotate(v:Vec3,r:Vec3):Vec3 {val x=Math.toRadians(r.x);val y=Math.toRadians(r.y);val z=Math.toRadians(r.z);val a=Vec3(v.x,v.y*cos(x)-v.z*sin(x),v.y*sin(x)+v.z*cos(x));val b=Vec3(a.x*cos(y)+a.z*sin(y),a.y,-a.x*sin(y)+a.z*cos(y));return Vec3(b.x*cos(z)-b.y*sin(z),b.x*sin(z)+b.y*cos(z),b.z)}
    fun check(p:Map<String,Any?>,before:WorkspaceState,r:RuhiExecutionResult,c:RuhiSceneContext):RuhiSeedBehaviorOracle.Check {
        val errors=mutableListOf<String>();fun need(v:Boolean,s:String){if(!v)errors+=s}
        fun same(a:Any?,b:Any?):Boolean=when{a is Number&&b is Number->abs(a.toDouble()-b.toDouble())<1e-9;a is List<*>&&b is List<*>->a.size==b.size&&a.zip(b).all{same(it.first,it.second)};else->a==b}
        fun close(a:Vec3,b:Vec3)=(a-b).magnitude()<1e-7
        need(r.status==GraphCommandStatus.APPLIED,"Status ${r.status}: ${r.message}");need(r.semanticFrame?.intent=="TRANSFORM_3D","Missing transform intent");p.forEach{(k,v)->need(same(v,r.semanticFrame?.parameters?.get(k)),"Original $k differs: expected $v got ${r.semanticFrame?.parameters?.get(k)}")}
        need(before.points==r.state.points&&before.points3D==r.state.points3D&&before.vectors3D==r.state.vectors3D&&before.planes3D==r.state.planes3D&&before.functions==r.state.functions&&before.surfaceLayers==r.state.surfaceLayers&&before.geometryGroups==r.state.geometryGroups,"Unrelated geometry or hierarchy changed")
        runCatching {
            val target=p["target"].toString();val ids=when(target){"camera"->emptyList();"last_solid"->listOf(before.solids.lastIndex);"both_selected"->before.labSessionValues.getValue("ruhi.geometry3d.selection").split(',').map(String::toInt);"selected"->listOf(requireNotNull(c.selectedSolidIndex));"null"->listOf(0);else->{val type=target.removePrefix("selected_");listOf(before.solids.indices.single{before.solids[it].type.name.lowercase()==type})}}
            fun n(key:String)=(p.getValue(key) as Number).toDouble()
            fun v(key:String)=(p.getValue(key) as List<*>).map{(it as Number).toDouble()}.let{Vec3(it[0],it[1],it[2])}
            val expected=before.solids.toMutableList()
            ids.forEach{i->val q=before.solids[i];expected[i]=when {
                p["action"]=="undo"->q.copy(rotation=Vec3(0.0,0.0,0.0))
                p["mirror"]=="yz"->q.copy(position=q.position.copy(x=-q.position.x),rotation=Vec3(q.rotation.x,-q.rotation.y,-q.rotation.z),mirrorX=!q.mirrorX)
                p["reset"]=="rotation"->q.copy(rotation=Vec3(0.0,0.0,0.0))
                p["degrees"]!=null->{val a=n("degrees")%360;q.copy(rotation=q.rotation+when(p["axis"]){"x"->Vec3(a,0.0,0.0);"y"->Vec3(0.0,a,0.0);else->Vec3(0.0,0.0,a)})}
                p["duplicate"]==true->q
                p["position"]!=null->q.copy(position=v("position"))
                p["translation"]!=null->q.copy(position=q.position+v("translation"))
                p["scale"]!=null->{val f=n("scale");q.copy(width=q.width*f,height=q.height*f,depth=q.depth*f,radius=q.radius*f,topRadius=q.topRadius*f)}
                p["height_factor"]!=null->q.copy(height=q.height*n("height_factor"))
                p["scale_factor"]!=null->{val f=n("scale_factor");when(p["axis"]){"x"->q.copy(width=q.width*f);"y"->q.copy(height=q.height*f);else->q.copy(depth=q.depth*f)}}
                else->q
            }}
            if(p["duplicate"]==true)expected+=before.solids[ids.single()].copy(position=before.solids[ids.single()].position+v("translation"))
            need(expected==r.state.solids,"Actual dimensions, position, orientation, parity or unrelated solids differ")
            if(p["mirror"]!=null){val i=ids.single();val a=before.solids[i];val b=r.state.solids[i];val old=SolidMeshFactory.create(a).vertices;val new=SolidMeshFactory.create(b).vertices;old.zip(new).forEach{(u,v)->val w=rotate(u,a.rotation)+a.position;need(close(w.copy(x=-w.x),rotate(v,b.rotation)+b.position),"Mirrored world mesh vertex is wrong")}}
            if(p["animation"]=="spin"){
                val record=if(ids.size==1)r.state.labSessionValues.getValue(RuhiMotionEngine.PREFIX+ids.single())else r.state.labSessionValues.getValue(RuhiTransform3DLanguage.GROUP).split(';')[2]
                val m=RuhiAngularMotion.decode(record);need(m.axis==p["axis"]&&m.degreesPerSecond==30.0,"Wrong deterministic angular velocity")
                val poses=RuhiMotionEngine.solids(r.state,m.startMillis+3000);val pivot=if(ids.size==1)before.solids[ids.single()].position else ids.map{before.solids[it].position}.reduce{a,b->a+b}*.5
                val angle=when(p["axis"]){"x"->Vec3(90.0,0.0,0.0);"y"->Vec3(0.0,90.0,0.0);else->Vec3(0.0,0.0,90.0)}
                ids.forEach{i->val q=before.solids[i];val a=SolidMeshFactory.create(q).vertices;val b=SolidMeshFactory.create(poses[i]).vertices;a.zip(b).forEach{(u,v)->val old=rotate(u,q.rotation)+q.position;val wanted=rotate(old-pivot,angle)+pivot;need(close(wanted,rotate(v,poses[i].rotation)+poses[i].position),"Animated vertex or group pivot is wrong")}}
            }
            ids.forEach{i->p["opacity"]?.let{need(abs(RuhiTransform3DLanguage.opacity(r.state,i)-(it as Number).toDouble())<1e-9,"Wrong per-object opacity")};if(p["render_mode"]=="wireframe")need(RuhiTransform3DLanguage.wireframe(r.state,i),"Missing native wireframe style")
                if(p["mode"]!=null){val amount=RuhiTransform3DLanguage.explosion(r.state,i);need(amount==if(p["mode"]=="exploded")1.0 else 0.0,"Wrong face explosion amount");val mesh=SolidMeshFactory.exploded(r.state.solids[i],amount);need(mesh.faces.size==6&&mesh.vertices.size==if(amount==0.0)8 else 24,"Logical cube faces were not separated/reassembled")}}
            if(target=="camera"||p["camera"]!=null){val old=RuhiTransform3DLanguage.camera(before);val next=RuhiTransform3DLanguage.camera(r.state);when{p["view"]=="top"->need(next==old.copy(x=90.0,y=0.0,z=0.0),"Wrong top camera");p["view"]=="isometric"->need(next==old.copy(x=25.0,y=-35.0,z=15.0),"Wrong isometric camera");p["orbit_degrees"]!=null->need(next==old.copy(y=(old.y+n("orbit_degrees"))%360),"Wrong camera orbit");p["camera"]=="zoom_in"->need(abs(next.zoom-(old.zoom*1.25).coerceAtMost(8.0))<1e-9,"Wrong camera zoom")}}
            val saved=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(r.state),recover=false).state!!;need(saved.solids==r.state.solids,"Transform did not persist")
        }.onFailure{errors+=it.message ?: "Transform oracle failed"}
        return RuhiSeedBehaviorOracle.Check(errors.isEmpty(),errors)
    }
}
