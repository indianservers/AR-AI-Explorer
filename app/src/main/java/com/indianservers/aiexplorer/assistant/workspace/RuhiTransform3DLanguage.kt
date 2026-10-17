package com.indianservers.aiexplorer.assistant.workspace

import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import kotlin.math.*

/** Intent proposals never supply dimensions, poses, camera parameters or mesh vertices. */
object RuhiTransform3DLanguage {
    const val PREFIX="ruhi.transform3d."
    const val CAMERA="Geometry3D::"+PREFIX+"camera"
    const val GROUP=PREFIX+"groupMotion"
    const val ROTATION_LABEL="Ruhi3D rotation:"
    data class Camera(val x:Double=25.0,val y:Double=-35.0,val z:Double=0.0,val zoom:Double=2.0,val panX:Double=0.0,val panY:Double=0.0) {
        fun encode()=listOf(x,y,z,zoom,panX,panY).joinToString(",")
        companion object {fun decode(s:String):Camera {val v=s.split(',').map(String::toDouble);require(v.size==6&&v.all{it.isFinite()}&&v[3] in .25..8.0);return Camera(v[0],v[1],v[2],v[3],v[4],v[5])}}
    }
    data class Plan(val target:String,val parameters:Map<String,Any>,val operation:String)
    private class Missing(val candidates:List<Int>,val pair:Boolean=false):IllegalArgumentException("Select the solid to transform.")
    private val number="[-+]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:[eE][-+]?[0-9]+)?"
    private val kinds=linkedMapOf("hemisphere" to SolidType.Hemisphere,"sphere" to SolidType.Sphere,"cube" to SolidType.Cube,"cuboid" to SolidType.Cuboid,"box" to SolidType.Cuboid,"cylinder" to SolidType.Cylinder,"cone" to SolidType.Cone,"pyramid" to SolidType.Pyramid,"torus" to SolidType.Torus,"wedge" to SolidType.Wedge,"tetrahedron" to SolidType.Tetrahedron)
    private fun normalized(s:String)=RuhiMathLanguage.normalize(s).replace('−','-')
    fun camera(s:WorkspaceState)=s.labSessionValues[CAMERA]?.let{runCatching{Camera.decode(it)}.getOrNull()} ?: Camera()
    fun opacity(s:WorkspaceState,i:Int)=s.labSessionValues[PREFIX+"opacity.$i"]?.toDoubleOrNull()?.takeIf{it in 0.0..1.0} ?: 1.0
    fun explosion(s:WorkspaceState,i:Int)=s.labSessionValues[PREFIX+"explosion.$i"]?.toDoubleOrNull()?.takeIf{it in 0.0..3.0} ?: 0.0
    fun wireframe(s:WorkspaceState,i:Int)=s.labSessionValues[PREFIX+"mode.$i"]=="wireframe"
    fun groupIds(s:WorkspaceState):List<Int> = s.labSessionValues[GROUP]?.let{runCatching{val v=it.split(';');require(v.size==4&&v[3].toInt()==s.solids.size);v[0].split(',').map(String::toInt).also{ids->require(ids.size==2&&ids.distinct().size==2&&ids.all{it in s.solids.indices})}}.getOrNull()}.orEmpty()
    fun groupPoses(s:WorkspaceState,poses:List<Solid>,now:Long):List<Solid> {
        val ids=groupIds(s);if(ids.isEmpty())return poses
        return runCatching{val v=s.labSessionValues.getValue(GROUP).split(';');val xyz=v[1].split(',').map(String::toDouble);val pivot=Vec3(xyz[0],xyz[1],xyz[2]);val motion=RuhiAngularMotion.decode(v[2]);poses.mapIndexed{i,q->if(i in ids)SolidPoseMath.rotateWorld(q,motion.axis,motion.angle(now),pivot)else q}}.getOrDefault(poses)
    }
    fun parse(source:String):Plan? {
        val t=normalized(source)
        if(Regex("\\b(?:locked|partly|unless|except|without|red|blue|green|yellow|orange|purple|pink|black|smallest|largest|first|second|last|then)\\b").containsMatchIn(t)||t.contains(";"))return null
        if(Regex("\\b(?:all|every|both)\\b").containsMatchIn(t)&&!t.contains("both solids"))return null
        if(t.contains(" and ")&&!t.startsWith("duplicate "))return null
        val kind=kinds.keys.firstOrNull{Regex("\\b$it\\b").containsMatchIn(t)}
        val target=when{t.contains("both solids")->"both_selected";t.contains("newest solid")->"last_solid";kind!=null->if(Regex("\\b(?:this|selected) $kind\\b").containsMatchIn(t))"selected_${if(kind=="box")"cuboid"else kind}"else if(kind=="box")"cuboid"else kind;Regex("\\b(?:selected|this) (?:object|solid)\\b").containsMatchIn(t)->"selected";else->return null}
        val p=linkedMapOf<String,Any>("target" to target)
        fun plan(op:String)=Plan(target,p,op)
        val axis=Regex("(?:around|about|along|positive) (?:the )?([xyz])(?:-axis| axis)?\\b").find(t)?.groupValues?.get(1)
        val degrees=Regex("($number) degrees?\\b").find(t)?.groupValues?.get(1)?.toDouble()
        if(t.startsWith("undo ")&&t.contains("3d rotation"))return Plan("",mapOf("action" to "undo","scope" to "last_3d_rotation"),"undo_rotation")
        if((t.startsWith("rotate camera")&&t.contains("above"))||t.contains("isometric angle"))return Plan("camera",mapOf("target" to "camera","view" to if("isometric" in t)"isometric"else"top"),"view")
        if(t.startsWith("orbit the camera")&&degrees!=null)return Plan("camera",mapOf("target" to "camera","orbit_degrees" to if("counterclockwise" in t)-degrees else degrees),"orbit")
        if(t.startsWith("zoom closer")){p["camera"]="zoom_in";return plan("zoom")}
        if((t.startsWith("spin ")||t.startsWith("rotate ")&&t.contains("both solids"))&&degrees==null){if(axis==null)return null;p["axis"]=axis;p["animation"]="spin";return plan("spin")}
        if((t.startsWith("rotate ")||t.startsWith("turn ")||t.startsWith("spin "))&&degrees!=null&&axis!=null){p["axis"]=axis;p["degrees"]=degrees;return plan("rotate")}
        if(t.startsWith("reset ")&&t.contains("orientation")){p["reset"]="rotation";return plan("reset")}
        if(t.startsWith("mirror ")&&t.contains("yz-plane")){p["mirror"]="yz";return plan("mirror")}
        if(t.startsWith("duplicate ")&&t.contains("shift")&&t.contains("right")){p["duplicate"]=true;val shift=Regex("($number) units? right").find(t)?.groupValues?.get(1)?.toDouble() ?: 3.0;p["translation"]=listOf(shift,0.0,0.0);return plan("duplicate")}
        if(t.startsWith("move ")&&Regex("\\bto\\s*\\(").containsMatchIn(t)){val xyz=Regex("\\bto\\s*\\(([^()]*)\\)\\s*$").find(t)?.groupValues?.get(1)?.split(',')?.map{it.trim().toDoubleOrNull() ?: error("Use three numeric coordinates.")};require(xyz?.size==3){"Use three numeric coordinates."};p["position"]=xyz;return plan("position")}
        if(t.startsWith("translate ")&&t.contains("by vector")){val xyz=Regex("by vector\\s*\\(([^()]*)\\)\\s*$").find(t)?.groupValues?.get(1)?.split(',')?.map{it.trim().toDoubleOrNull() ?: error("Use three numeric vector components.")};require(xyz?.size==3){"Use three numeric vector components."};p["translation"]=xyz;return plan("translate")}
        if(t.startsWith("move ")&&!t.contains("point")&&!t.contains("vector")) {
            val amount=Regex("($number) units? (left|right|up|down)\\b").find(t)
            if(amount!=null){val n=amount.groupValues[1].toDouble();val v=when(amount.groupValues[2]){"left"->listOf(-n,0.0,0.0);"right"->listOf(n,0.0,0.0);"up"->listOf(0.0,0.0,n);else->listOf(0.0,0.0,-n)};p["translation"]=v;return plan("translate")}
        }
        if(t.startsWith("slide ")&&axis!=null){val amount=Regex("by ($number)$").find(t)?.groupValues?.get(1)?.toDouble() ?: return null;p["translation"]=when(axis){"x"->listOf(amount,0.0,0.0);"y"->listOf(0.0,amount,0.0);else->listOf(0.0,0.0,amount)};return plan("translate")}
        if(t.startsWith("scale ")){val factor=Regex("by ($number)$").find(t)?.groupValues?.get(1)?.toDouble() ?: return null;p["scale"]=factor;return plan("scale")}
        if(t.startsWith("double the size")){p["scale"]=2;return plan("scale")}
        if(t.startsWith("make ")&&t.endsWith("twice as tall")){p["height_factor"]=2;return plan("height")}
        if(t.startsWith("stretch ")&&axis!=null){p["axis"]=axis;p["scale_factor"]=Regex("by ($number)$").find(t)?.groupValues?.get(1)?.toDouble() ?: 1.5;return plan("stretch")}
        if(t.startsWith("make ")&&t.endsWith("transparent")){p["opacity"]=.5;return plan("opacity")}
        if(t.startsWith("give ")&&t.endsWith("percent opacity")){p["opacity"]=(Regex("($number) percent").find(t)?.groupValues?.get(1)?.toDouble() ?: return null)/100;return plan("opacity")}
        if(t.startsWith("change ")&&t.endsWith("wireframe")){p["render_mode"]="wireframe";return plan("wireframe")}
        if(t.startsWith("explode the faces")){p["mode"]="exploded";return plan("explode")}
        if(t.startsWith("put the exploded")&&t.endsWith("back together")){p["mode"]="assembled";return plan("assemble")}
        return null
    }
    fun accepts(source:String)=runCatching{parse(source)!=null||normalized(source).let{it.startsWith("undo ")&&it.contains("3d rotation")||it.startsWith("rotate camera")||it.startsWith("orbit the camera")}}.getOrDefault(false)
    private fun scene(s:WorkspaceState)=java.util.Base64.getEncoder().encodeToString(java.security.MessageDigest.getInstance("SHA-256").digest((s.id+s.solids+s.labSessionValues["ruhi.geometry3d.selection"]).toByteArray(Charsets.UTF_8)))
    fun restored(s:WorkspaceState,p:RuhiPending):RuhiDialogueView {
        val chips=if(p.kind=="transform_pair")p.targets.chunked(2).filter{v->v.size==2&&v.all{it.toIntOrNull() in s.solids.indices}}.take(5).map{v->RuhiAnswerChip("${s.solids[v[0].toInt()].type} ${v[0].toInt()+1} + ${s.solids[v[1].toInt()].type} ${v[1].toInt()+1}","Use transform pair ${v.joinToString(",")}")}else p.targets.mapNotNull{it.toIntOrNull()}.filter{it in s.solids.indices}.take(5).map{i->RuhiAnswerChip("${s.solids[i].type} ${i+1}","Use transform solid $i")}
        return RuhiDialogueView(RuhiDialogueAction.ASK_CLARIFICATION,chips,true)
    }
    private fun targets(s:WorkspaceState,c:RuhiSceneContext,target:String,picked:List<Int>?):List<Int> {
        if(picked!=null){require(picked.all{it in s.solids.indices});return picked}
        if(target=="last_solid")return s.solids.indices.lastOrNull()?.let(::listOf) ?: throw Missing(emptyList())
        if(target=="both_selected"){val ids=s.labSessionValues["ruhi.geometry3d.selection"]?.split(',')?.mapNotNull{it.toIntOrNull()}?.distinct()?.filter{it in s.solids.indices}.orEmpty();if(ids.size==2)return ids;throw Missing(s.solids.indices.toList(),true)}
        val type=kinds[target.removePrefix("selected_")];val candidates=s.solids.indices.filter{type==null||s.solids[it].type==type}
        return (c.selectedSolidIndex?.takeIf{it in candidates} ?: candidates.singleOrNull())?.let(::listOf) ?: throw Missing(candidates)
    }
    private fun vec(p:Map<String,Any>,key:String):Vec3 {val v=(p.getValue(key) as List<*>).map{(it as Number).toDouble()};require(v.size==3&&v.all{it.isFinite()&&abs(it)<=10000});return Vec3(v[0],v[1],v[2])}
    fun execute(s:WorkspaceState,source:String,h:WorkspaceHistoryByModule,c:RuhiSceneContext,m:RuhiInputModality,pendingInput:RuhiPending?,now:Long=System.nanoTime()/1_000_000):RuhiPointConversation.Outcome? {
        if(s.module!=MathModule.Geometry3D)return null
        if(normalized(source) in setOf("stop the rotation","stop rotation","stop the animation","stop animation")&&s.labSessionValues.keys.any{it==GROUP||it.startsWith(RuhiMotionEngine.PREFIX)}) {
            val poses=RuhiMotionEngine.solids(s,now);val next=s.copy(solids=poses,labSessionValues=s.labSessionValues.filterKeys{it!=GROUP&&!it.startsWith(RuhiMotionEngine.PREFIX)})
            val committed=h.forModule(s.module).execute(s,RuhiSolidTransformCommand(s,GraphWorkspaceCommand.reconcileDocument(s,next),"Ruhi3D transform: stop motion"))
            return RuhiPointConversation.Outcome(RuhiExecutionResult(committed,GraphCommandStatus.APPLIED,"Stopped 3D motion.",context=c),null)
        }
        val pending=pendingInput?.takeIf{it.kind in setOf("transform_solid","transform_pair")};val reply=source.startsWith("Use transform ")&&pending!=null
        if(!reply&&!accepts(source))return null
        var frame=RuhiSemanticFrame("TRANSFORM_3D",RuhiEngine.GEOMETRY_3D,emptyMap())
        val cue=runCatching{RuhiLanguageServices.semanticClassifier?.predict(RuhiSeedNlpInput.normalize(source))}.getOrNull()
        fun result(state:WorkspaceState,status:GraphCommandStatus,message:String,ctx:RuhiSceneContext=c,p:RuhiPending?=null)=RuhiPointConversation.Outcome(RuhiExecutionResult(state,status,message,RuhiCommand(engine=RuhiEngine.GEOMETRY_3D,operation=RuhiOperation.PointQuery(frame),modality=m,intentProposal=cue),ctx,dialogue=if(p!=null)restored(state,p)else null,semanticFrame=frame),p)
        try {
            if(reply&&pending!!.scene!=scene(s))return result(s,GraphCommandStatus.CLARIFICATION,"The solid scene changed. State the transform again.")
            val text=if(reply)pending!!.source else source
            val p=if(normalized(text).let{it.startsWith("undo ")&&it.contains("3d rotation")})Plan("",mapOf("action" to "undo","scope" to "last_3d_rotation"),"undo_rotation")else if(normalized(text).startsWith("orbit the camera")) {
                val t=normalized(text);val n=Regex("($number) degrees?").find(t)?.groupValues?.get(1)?.toDouble() ?: error("Specify an orbit angle.");Plan("camera",mapOf("target" to "camera","orbit_degrees" to if("counterclockwise" in t)-n else n),"orbit")
            }else if(normalized(text).startsWith("rotate camera")&&normalized(text).contains("above"))Plan("camera",mapOf("target" to "camera","view" to "top"),"view")else requireNotNull(parse(text))
            frame=frame.copy(parameters=p.parameters)
            if(p.operation=="undo_rotation") {val restored=h.forModule(s.module).undoMatching(s){it.label.startsWith(ROTATION_LABEL)} ?: return result(s,GraphCommandStatus.CLARIFICATION,"There is no recorded 3D rotation to undo in this workspace.");return result(restored,GraphCommandStatus.APPLIED,"Undid the last 3D rotation; preserved independent later edits.")}
            var next=s;val picked=if(reply)source.removePrefix(if(pending!!.kind=="transform_pair")"Use transform pair "else"Use transform solid ").split(',').map{it.toInt()}else null
            if(reply)require(if(pending!!.kind=="transform_pair")picked!!.size==2&&picked.distinct().size==2&&pending.targets.chunked(2).any{it==picked.map(Int::toString)}else picked!!.size==1&&picked.single().toString() in pending.targets){"Choose one of the current transform targets."}
            val ids=if(p.target=="camera")emptyList()else targets(s,c,p.target,picked)
            val params=p.parameters
            fun n(key:String)=(params.getValue(key) as Number).toDouble().also{require(it.isFinite())}
            var values=s.labSessionValues.toMutableMap();var solids=s.solids
            if(p.operation !in setOf("opacity","wireframe","explode","assemble","view","orbit","zoom")){
                val group=groupIds(s);val bake=ids.toSet()+if(ids.any{it in group})group else emptyList()
                val poses=RuhiMotionEngine.solids(s,now);solids=s.solids.mapIndexed{i,q->if(i in bake)poses[i]else q}
                bake.forEach{values.remove(RuhiMotionEngine.PREFIX+it)};if(ids.any{it in group})values.remove(GROUP)
            }
            fun edit(f:(Solid)->Solid){solids=solids.mapIndexed{i,q->if(i in ids)f(q)else q}}
            when(p.operation){
                "rotate"->{val angle=n("degrees");require(abs(angle)<=1e9){"Use a finite angle within one billion degrees."};val v=when(params["axis"]){"x"->Vec3(angle%360,0.0,0.0);"y"->Vec3(0.0,angle%360,0.0);else->Vec3(0.0,0.0,angle%360)};edit{it.copy(rotation=it.rotation+v)}}
                "reset"->edit{it.copy(rotation=Vec3(0.0,0.0,0.0))}
                "translate"->edit{it.copy(position=it.position+vec(params,"translation"))}
                "position"->edit{it.copy(position=vec(params,"position"))}
                "scale"->{val f=n("scale");require(f>0&&f<=100);edit{it.copy(width=it.width*f,height=it.height*f,depth=it.depth*f,radius=it.radius*f,topRadius=it.topRadius*f)}}
                "height"->edit{it.copy(height=it.height*n("height_factor"))}
                "stretch"->{val f=n("scale_factor");edit{when(params["axis"]){"x"->it.copy(width=it.width*f);"y"->it.copy(height=it.height*f);else->it.copy(depth=it.depth*f)}}}
                "mirror"->edit{it.copy(position=it.position.copy(x=-it.position.x),rotation=Vec3(it.rotation.x,-it.rotation.y,-it.rotation.z),mirrorX=!it.mirrorX)}
                "duplicate"->{require(ids.size==1&&solids.size<24);val old=ids.single();val new=solids.size;solids=solids+solids[old].copy(position=solids[old].position+vec(params,"translation"));listOf("ruhi.studio.appearance.",PREFIX+"opacity.",PREFIX+"mode.",PREFIX+"explosion.").forEach{prefix->values[prefix+old]?.let{values[prefix+new]=it}}}
                "spin"->{val axis=params["axis"].toString();if(ids.size==1)values[RuhiMotionEngine.PREFIX+ids.single()]=RuhiAngularMotion(axis,30.0,now).encode()else{require(ids.size==2);val center=ids.map{solids[it].position}.reduce{a,b->a+b}*.5;values[GROUP]="${ids.joinToString(",")};${center.x},${center.y},${center.z};${RuhiAngularMotion(axis,30.0,now).encode()};${solids.size}"}}
                "opacity"->{val alpha=n("opacity");require(alpha in 0.0..1.0);ids.forEach{values[PREFIX+"opacity.$it"]=alpha.toString()}}
                "wireframe"->ids.forEach{values[PREFIX+"mode.$it"]="wireframe"}
                "explode"->ids.forEach{values[PREFIX+"explosion.$it"]="1.0"}
                "assemble"->ids.forEach{values.remove(PREFIX+"explosion.$it")}
                "view"->{val old=camera(s);values[CAMERA]=(if(params["view"]=="top")old.copy(x=90.0,y=0.0,z=0.0)else old.copy(x=25.0,y=-35.0,z=15.0)).encode()}
                "orbit"->{val old=camera(s);val angle=n("orbit_degrees");require(abs(angle)<=1e9);values[CAMERA]=old.copy(y=(old.y+angle)%360).encode()}
                "zoom"->{val old=camera(s);val q=solids[ids.single()];val z=(old.zoom*1.25).coerceAtMost(8.0);val point=GraphTransform().rotated("x",old.x,Vec3(0.0,0.0,0.0)).rotated("y",old.y,Vec3(0.0,0.0,0.0)).rotated("z",old.z,Vec3(0.0,0.0,0.0)).point(q.position);val perspective=1.0/(1.0+(point.z+5.0)*.08);values[CAMERA]=old.copy(zoom=z,panX=-point.x*74*z*perspective,panY=point.y*74*z*perspective-point.z*18).encode();values[PREFIX+"focus"]="${q.position.x},${q.position.y},${q.position.z}"}
            }
            require(solids.all{q->listOf(q.width,q.height,q.depth,q.radius,q.topRadius).all{it.isFinite()&&it>0&&it<=10000}&&listOf(q.position.x,q.position.y,q.position.z,q.rotation.x,q.rotation.y,q.rotation.z).all{it.isFinite()&&abs(it)<=1e9}}){"Use positive finite dimensions within 10000 units and finite poses."}
            next=s.copy(solids=solids,labSessionValues=values);val reconciled=GraphWorkspaceCommand.reconcileDocument(s,next)
            val label=(if(p.operation in setOf("rotate","spin","reset"))ROTATION_LABEL else"Ruhi3D transform:")+" ${p.operation}"
            val command=if(s.solids.size==solids.size)RuhiSolidTransformCommand(s,reconciled,label)else GraphWorkspaceCommand(s,reconciled,label)
            val committed=h.forModule(s.module).execute(s,command)
            return result(committed,GraphCommandStatus.APPLIED,"Applied 3D ${p.operation}.",if(p.operation=="duplicate")c.copy(selectedSolidIndex=solids.lastIndex)else ids.lastOrNull()?.let{c.copy(selectedSolidIndex=it)} ?: c)
        }catch(e:Missing){val sourceText=if(reply)pending!!.source else source;val targets=if(e.pair)e.candidates.flatMapIndexed{i,a->e.candidates.drop(i+1).map{b->listOf(a,b)}}.take(5).flatten().map(Int::toString)else e.candidates.map(Int::toString);val p=RuhiPending(if(e.pair)"transform_pair"else"transform_solid",sourceText,s.module.name,scene(s),targets);return result(s,GraphCommandStatus.CLARIFICATION,if(e.pair)"Which two solids should rotate together?"else"Which solid should I transform?",p=p)}
        catch(e:RuntimeException){return result(s,GraphCommandStatus.INVALID,e.message ?: "Invalid 3D transformation.")}
    }
}
