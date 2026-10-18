package com.indianservers.aiexplorer.assistant.workspace

import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import kotlin.math.*

/** Reviewed language adapter for native solids. Learned intents never supply coordinates or formulas. */
object RuhiGeometry3DLanguage {
    private const val PREFIX="ruhi.geometry3d."
    private const val SELECTION=PREFIX+"selection"
    private const val SAVED=PREFIX+"saved"
    private val num="[-+]?\\d+(?:\\.\\d+)?"
    private class Missing(message:String):IllegalStateException(message)
    private class Unavailable(message:String):IllegalStateException(message)
    private val colors=mapOf("red" to "#FF0000","blue" to "#0000FF","green" to "#008000","orange" to "#FFA500","purple" to "#800080","yellow" to "#FFFF00","pink" to "#FFC0CB","black" to "#000000")
    private val nouns=linkedMapOf("triangular prism" to SolidType.TriangularPrism,"square pyramid" to SolidType.Pyramid,"triangle bottom" to SolidType.TriangularPyramid,"tetrahedron" to SolidType.Tetrahedron,"pyramid" to SolidType.Pyramid,"cuboid" to SolidType.Cuboid,"box" to SolidType.Cuboid,"cube" to SolidType.Cube,"sphere" to SolidType.Sphere,"ball" to SolidType.Sphere,"cone" to SolidType.Cone,"cylinder" to SolidType.Cylinder,"hemisphere" to SolidType.Hemisphere,"capsule" to SolidType.Capsule,"ellipsoid" to SolidType.Ellipsoid,"torus" to SolidType.Torus,"frustum" to SolidType.Frustum)
    fun normalize(source:String)=RuhiMathLanguage.normalize(source).replace('’','\'').replace(Regex("-\\s+(\\d)"),"-$1").replace(" for me","").replace("a ball","a sphere").replace("like an ice cream cone","").replace("like a tin can","").trim()
    fun accepts(source:String):Boolean {
        val t=runCatching{normalize(source)}.getOrDefault("")
        return t.startsWith("no, ") || t=="explain that more simply" || Regex("^(?:how|what|which|where|is|are|does|do|did|if|find|tell|why|show|give|put|push|turn|spin|flip|reflect|shrink|enlarge|paint|join|add|mark|bring|line up|leave|keep|call|rename|select|hide|lock|unlock|save|load|slice|cut|drop|subtract|delete|change|set|make|draw|create|move|rotate|color|clear|reset|undo|redo)\\b").containsMatchIn(t)
    }
    fun execute(s:WorkspaceState,source:String,h:WorkspaceHistoryByModule,c:RuhiSceneContext,modality:RuhiInputModality=RuhiInputModality.TEXT):RuhiExecutionResult? {
        if(s.module!=MathModule.Geometry3D) return null
        val t=normalize(source)
        if(!accepts(t)) return null
        if(t.startsWith("create a rotating") || t.startsWith("draw a rotating") || t.contains("cylinder with the same height") || t.startsWith("make another") || t=="undo that" || t=="undo the last thing you did" || t=="redo that change") return null
        if(t in setOf("make it taller","make it solid","what's its volume","now show its volume") || t.startsWith("the base should be "))return null
        return Session(s,source,h,c,modality,t).execute()
    }
    private class Session(val s:WorkspaceState,val source:String,val h:WorkspaceHistoryByModule,val c:RuhiSceneContext,val modality:RuhiInputModality,val t:String) {
        private val proposal=runCatching{RuhiLanguageServices.geometryClassifier?.predict(t)}.getOrNull()
        private var selected=c
        fun response(state:WorkspaceState=s,message:String,status:GraphCommandStatus=GraphCommandStatus.APPLIED):RuhiExecutionResult {
            val committed=if(state==s || status!=GraphCommandStatus.APPLIED) state else h.forModule(s.module).execute(s,GraphWorkspaceCommand(s,GraphWorkspaceCommand.reconcileDocument(s,state),"Ruhi geometry: ${source.take(80)}"))
            return RuhiExecutionResult(committed,status,message,command=RuhiCommand(engine=RuhiEngine.GEOMETRY_3D,operation=RuhiOperation.NativeGeometry(proposal?.intent ?: "REVIEWED_NATIVE_COMMAND"),modality=modality,targetObjectId=selected.selectedSolidIndex?.let{"solid-$it"},intentProposal=proposal),context=selected,dialogue=RuhiDialogueView(if(status==GraphCommandStatus.UNSUPPORTED)RuhiDialogueAction.REJECT_UNSUPPORTED else if(status==GraphCommandStatus.CLARIFICATION)RuhiDialogueAction.ASK_CLARIFICATION else RuhiDialogueAction.EXECUTE,languageProposal=proposal))
        }
        fun point(name:String)=s.points3D.singleOrNull{it.name.equals(name,true)} ?: throw Missing("Point ${name.uppercase()} is missing or ambiguous. Add it first.")
        fun type():SolidType?=nouns.entries.firstOrNull{Regex("\\b${Regex.escape(it.key)}${if(it.key=="box")"(?:es)?" else "s?"}\\b").containsMatchIn(t)}?.value
        fun targets():List<Int> {
            val kind=type();var candidates=s.solids.indices.filter{kind==null || s.solids[it].type==kind}
            val color=colors.keys.firstOrNull{Regex("\\b$it (?:solid|cone|sphere|cube|box)").containsMatchIn(t)}
            if(color!=null) candidates=candidates.filter{s.labSessionValues["ruhi.studio.appearance.$it"] in setOf(color,colors[color])}
            if(candidates.isEmpty()) throw Missing("There are no matching solids. Create one first.")
            if(t.contains("smallest")) {
                val v=candidates.minOf{Geometry3D.measure(s.solids[it]).volume};candidates=candidates.filter{abs(Geometry3D.measure(s.solids[it]).volume-v)<1e-9}
                if(candidates.size>1)throw Missing("Several solids share the smallest volume. Which one?")
            }
            if(Regex("\\b(first|second|last)\\b").containsMatchIn(t)) candidates=listOfNotNull(when { t.contains("first")->candidates.firstOrNull();t.contains("second")->candidates.getOrNull(1);else->candidates.lastOrNull() })
            val all=Regex("\\b(all|every|both|them|everything)\\b").containsMatchIn(t)
            if(Regex("\\bthem\\b").containsMatchIn(t)) {
                val group=s.labSessionValues[SELECTION]?.split(',')?.mapNotNull{it.toIntOrNull()}?.filter{it in candidates}.orEmpty()
                if(group.isEmpty())throw Missing("Select the solids you mean by them first.")
                candidates=group
            }
            if(t.contains("both") && candidates.size!=2)throw Missing("Choose exactly two matching solids.")
            if(!all) {
                if(t.contains("selected") && c.selectedSolidIndex !in candidates) throw Missing("Select the solid first.")
                candidates=candidates.firstOrNull{it==c.selectedSolidIndex}?.let(::listOf) ?: candidates.singleOrNull()?.let(::listOf) ?: throw Missing("Which solid do you mean? Select one or say all.")
            }
            if(candidates.isEmpty()) throw Missing("There is no matching solid.")
            selected=c.copy(selectedSolidIndex=candidates.last());return candidates
        }
        fun one()=s.solids[targets().singleOrNull() ?: throw Missing("Select one solid to measure.")]
        fun edit(change:(Solid)->Solid):RuhiExecutionResult {
            val ids=targets();val solids=s.solids.mapIndexed{i,old->if(i in ids)change(old) else old}
            solids.forEach { q->require(listOf(q.width,q.height,q.depth,q.radius,q.topRadius).all{it.isFinite() && it>0 && it<=50}){"Use positive dimensions no larger than 50 units."};require(listOf(q.position.x,q.position.y,q.position.z).all{it.isFinite()&&abs(it)<=10000}){"Keep positions within 10000 units."} }
            val delta=solids[ids.first()].position-s.solids[ids.first()].position
            val next=if(t.startsWith("move everything"))s.copy(solids=solids,points3D=s.points3D.map{it.copy(position=it.position+delta)},vectors3D=s.vectors3D.map{it.copy(start=it.start+delta,end=it.end+delta)}) else s.copy(solids=solids)
            return response(next,"Updated ${ids.size} solid${if(ids.size==1)"" else "s"}. ${solids[ids.last()].type} center ${solids[ids.last()].position}.")
        }
        fun xyz(raw:String):Vec3 {val a=raw.split(',').map{it.trim().toDoubleOrNull() ?: throw IllegalArgumentException("Use three numeric coordinates.")};require(a.size==3 && a.all{it.isFinite()&&abs(it)<=10000}){"Use three coordinates within 10000 units."};return Vec3(a[0],a[1],a[2])}
        fun scaled(q:Solid,f:Double):Solid {require(f.isFinite()&&f>0);return q.copy(width=q.width*f,height=q.height*f,depth=q.depth*f,radius=q.radius*f,topRadius=q.topRadius*f)}
        fun vectorPair():List<Vector3D> { if(s.vectors3D.size!=2) throw Missing("Create exactly two vectors for this comparison.");return s.vectors3D }
        private fun safety():RuhiExecutionResult? {
            if(t.contains("outline") || t.contains("slicing") || t.contains("slice"))throw Unavailable("That outline or slicing operation is not connected to the native assistant yet.")
            if(t.contains("same point twice"))throw IllegalArgumentException("A line needs two distinct points.")
            if(t.contains("three points on 1 straight line") || t.contains("3 points on 1 straight line"))throw IllegalArgumentException("Collinear points do not define a unique plane.")
            if(t.contains("when z is not on the board"))throw Missing("Point Z is missing. Add it before measuring.")
            if(t in setOf("how far apart are these 2 points","are these 4 points on the same plane","what is the distance between the centers of these spheres","is this cube bigger than that sphere by volume","do these 2 boxes have the same volume","how much bigger is this cylinder than that cone","do the same thing to the second cube","what about the blue sphere","why did that answer change","show me the steps for that answer","explain that more simply","did you change both cubes or only 1","turn the cube around its own center")) throw Missing("Specify the exact objects and the calculation or change you want. I have not changed the scene.")
            if(t=="which object did you just change")return response(message=c.selectedSolidIndex?.takeIf{it in s.solids.indices}?.let{"Current target: ${s.solids[it].type} ${it+1}."} ?: "No current target is selected.")
            // Prompts that omit an essential parameter must never silently use a learned default.
            if(t.contains("ask me")) return response(message=if(t.contains("axis"))"Which axis: X, Y, or Z?" else "By what positive scale factor?",status=GraphCommandStatus.CLARIFICATION)
            if(t in setOf("keep the other objects where they are","keep a fixed while moving b")) return response(message="Other objects stay fixed. Specify the movement for the target.",status=GraphCommandStatus.CLARIFICATION)
            if(t.startsWith("no, i meant")) {targets();return response(message="Selected the requested solid; state the change to apply.")}
            if(t in setOf("move them together","move only the selected solid")) return response(message="How far and in which direction should I move the solid(s)?",status=GraphCommandStatus.CLARIFICATION)
            if(t.contains("locked")) throw Unavailable("Solid locking is not connected to the native touch tools yet; no movement was made.")
            return null
        }
        private fun create():RuhiExecutionResult? {
            // Native creation, with explicit bounds and a visible deterministic mesh.
            if(Regex("^(?:draw|create|make) (?:an? |[23] |half a )").containsMatchIn(t) && !t.contains("copy") && !t.contains("bigger") && !t.contains("smaller")) {
                if(t.contains("hollow tube")) throw Unavailable("A hollow tube is not available in the native solid engine yet.")
                if(t.contains("using") || t.contains("through") || t.contains("line ") || t.contains("ray ") || t.contains("plane ") || t.contains("triangle using")) throw Unavailable("This construction is not available in the native geometry assistant yet.")
                val kind=when {t.contains("half a ball")||t.contains("half a sphere")->SolidType.Hemisphere;t.contains("egg-shaped")->SolidType.Ellipsoid;t.contains("ring like")->SolidType.Torus;t.contains("top cut off")->SolidType.Frustum;t.contains("triangle bottom")->SolidType.TriangularPyramid;else->type()}
                if(kind!=null) {
                    fun dim(pattern:String,default:Double)=Regex(pattern).find(t)?.groupValues?.get(1)?.toDouble() ?: default
                    val side=dim("(?:side(?:s| length)?(?: of)?|base side) ($num)",2.0)
                    val radius=dim("radius ($num)",dim("diameter ($num)",2.0)/2)
                    val height=dim("height ($num)",dim("($num) units tall",2.0))
                    var q=Solid(kind,side,height,side,radius)
                    q=when(kind){SolidType.Cube->q.copy(height=side,depth=side,radius=side/2);SolidType.Sphere,SolidType.Hemisphere->q.copy(width=radius*2,height=radius*2,depth=radius*2);SolidType.Cuboid->q.copy(width=3.0,height=2.0,depth=2.0);SolidType.Ellipsoid->q.copy(width=2.0,height=3.0,depth=2.0);SolidType.Capsule->q.copy(height=4.0);SolidType.Torus->q.copy(width=2.0,radius=.5);else->q}
                    Regex("($num) long, ($num) wide, and ($num) high").find(t)?.let{q=q.copy(width=it.groupValues[1].toDouble(),depth=it.groupValues[2].toDouble(),height=it.groupValues[3].toDouble())}
                    if(kind==SolidType.TriangularPrism) q=q.copy(depth=dim("($num) units long",2.0))
                    require(listOf(q.width,q.height,q.depth,q.radius).all{it.isFinite()&&it>0&&it<=50}){"Use positive dimensions no larger than 50 units."}
                    val count=Regex("^(?:draw|create|make) ([23]) ").find(t)?.groupValues?.get(1)?.toInt() ?: 1
                    require(s.solids.size+count<=24){"The solid budget is 24."}
                    val created=List(count){i->val obj=if(t.contains("different sizes"))scaled(q,(i+1).toDouble()) else q;obj.copy(position=Vec3(i*(q.width+2),0.0,0.0))}
                    created.forEach{require(SolidMeshFactory.create(it).vertices.isNotEmpty())}
                    selected=c.copy(selectedSolidIndex=s.solids.size+count-1)
                    return response(s.copy(solids=s.solids+created),"Created $count ${kind.name}. Default unspecified dimensions: ${q.width} × ${q.height} × ${q.depth}; radius ${q.radius}.")
                }
            }
            return null
        }
        private fun points():RuhiExecutionResult? {
            Regex("^(?:draw|put|add|create) (?:a )?(?:3d )?point(?: (?:called )?([a-z]))? at (?:\\(([^)]+)\\)|(the origin))$").matchEntire(t)?.let{m->
                val p=if(m.groupValues[3].isNotEmpty())Vec3(0.0,0.0,0.0) else xyz(m.groupValues[2]);val name=m.groupValues[1].ifEmpty{"P${s.points3D.size+1}"}.uppercase()
                require(s.points3D.none{it.name.equals(name,true)}){"That point name is already used."};require(s.points3D.size<200)
                return response(s.copy(points3D=s.points3D+Point3D("ruhi-point-${java.util.UUID.randomUUID()}",name,p)),"Created point $name at $p.")
            }
            if(t.startsWith("make an arrow from the origin to ")) { val p=xyz(t.substringAfter('(').substringBefore(')'));return response(s.copy(vectors3D=s.vectors3D+Vector3D("ruhi-vector-${java.util.UUID.randomUUID()}",Vec3(0.0,0.0,0.0),p)),"Created vector with components $p.") }
            Regex("^(?:move (?:point )?([a-z]) to \\(([^)]+)\\)|move ([a-z]) 1 unit closer to the xy-plane|reflect ([a-z]) across the xy-plane|rotate point ([a-z]) 90 degrees around the z-axis)$").matchEntire(t)?.let{m->
                val name=listOf(1,3,4,5).map{m.groupValues[it]}.first{it.isNotEmpty()};val old=point(name);require(!old.locked){"Unlock the point first."};val p=old.position
                val to=when {m.groupValues[1].isNotEmpty()->xyz(m.groupValues[2]);m.groupValues[3].isNotEmpty()->p.copy(z=sign(p.z)*max(0.0,abs(p.z)-1));m.groupValues[4].isNotEmpty()->p.copy(z=-p.z);else->Vec3(-p.y,p.x,p.z)}
                return response(s.copy(points3D=s.points3D.map{if(it.id==old.id)it.copy(position=to) else it}),"Moved ${old.name} to $to.")
            }
            return null
        }
        private fun pointQueries():RuhiExecutionResult? {
            // Named-point calculations use the actual scene; missing names are never invented.
            Regex("^(?:what are ([a-z])'s coordinates|where is point ([a-z]))$").matchEntire(t)?.let{m->val p=point(m.groupValues[1].ifEmpty{m.groupValues[2]});return response(message="${p.name} = ${p.position}.")}
            if((t.startsWith("which point is at ") || t.startsWith("which point is closest to ")) && t.contains('(')) {val p=xyz(t.substringAfter('(').substringBefore(')'));val matches=if(t.contains("closest")){val d=s.points3D.minOfOrNull{(it.position-p).magnitude()} ?: throw Missing("Add points first.");s.points3D.filter{abs((it.position-p).magnitude()-d)<1e-9}}else s.points3D.filter{(it.position-p).magnitude()<1e-9};return response(message="Matching points: ${matches.joinToString{it.name}.ifEmpty{"none"}}.")}
            if(t=="is any point exactly at the origin") return response(message="At the origin: ${s.points3D.filter{it.position.magnitude()<1e-9}.joinToString{it.name}.ifEmpty{"none"}}.")
            Regex("^(?:what is the distance from ([a-z]) to ([a-z])|how far is ([a-z]) from the origin|what is the midpoint of ([a-z])([a-z])|mark the halfway point between ([a-z]) and ([a-z]))$").matchEntire(t)?.let{m->
                if(m.groupValues[3].isNotEmpty())return response(message="Distance = ${point(m.groupValues[3]).position.magnitude()} units.")
                val start=if(m.groupValues[1].isNotEmpty())1 else if(m.groupValues[4].isNotEmpty())4 else 6
                val a=point(m.groupValues[start]).position;val b=point(m.groupValues[start+1]).position
                if(start==1)return response(message="Distance = ${(a-b).magnitude()} units.")
                val p=(a+b)*.5
                return if(start==6)response(s.copy(points3D=s.points3D+Point3D("mid-${java.util.UUID.randomUUID()}","M${s.points3D.size}",p)),"Midpoint = $p.") else response(message="Midpoint = $p.")
            }
            if(t.startsWith("which point is highest") || t.contains("smallest x-coordinate") || t.contains("closest to the xy-plane") || t=="which points have z = to 0") {
                if(s.points3D.isEmpty())throw Missing("Add points first.")
                val values=s.points3D.associateWith{if(t.contains("x-coordinate"))it.position.x else if(t.contains("highest"))-it.position.z else abs(it.position.z)}
                val v=if(t.contains("= to 0"))0.0 else values.values.min();return response(message="Points: ${values.filterValues{abs(it-v)<1e-9}.keys.joinToString{it.name}.ifEmpty{"none"}}.")
            }
            if(t in setOf("is a above the xy-plane","is b on the z-axis","do a and b have the same height","how far is a from the x-axis","how far is b from the yz-plane","are a, b, and c on 1 straight line")) {
                val answer=when(t){"is a above the xy-plane"->(point("a").position.z>0).toString();"is b on the z-axis"->point("b").position.let{(hypot(it.x,it.y)<1e-9).toString()};"do a and b have the same height"->(abs(point("a").position.z-point("b").position.z)<1e-9).toString();"how far is a from the x-axis"->point("a").position.let{hypot(it.y,it.z).toString()+" units"};"how far is b from the yz-plane"->abs(point("b").position.x).toString()+" units";else->(AnalyticGeometry3D.cross(point("b").position-point("a").position,point("c").position-point("a").position).magnitude()<1e-9).toString()}
                return response(message="Answer: $answer.")
            }
            return null
        }
        private fun solidQueries():RuhiExecutionResult? {
            // Elementary solid classification and measurement.
            if(t.startsWith("how many solids"))return response(message="There are ${s.solids.size} solids.")
            if(t.startsWith("how many cubes") || t.startsWith("how many spheres"))return response(message="There are ${s.solids.count{it.type==type()}} matching solids.")
            if(t.contains("rotating this solid change its volume"))return response(message="No. Rotation preserves all lengths, so the volume stays the same.")
            if(t.contains("double this cube's sides"))return response(message="Volume multiplies by 2³ = 8.")
            if(t.contains("double this sphere's radius"))return response(message="Surface area multiplies by 2² = 4.")
            if(t.contains("cone 1-third of the cylinder") || t.contains("cone one-third of the cylinder"))return response(message="Yes, when radius and height match: cone πr²h/3, cylinder πr²h.")
            if(t in setOf("which shape looks like a sphere","which shape looks like a ball","which solid has a pointy top","which solid has 2 circular ends","which objects have only flat faces","which objects have curved surfaces","which objects have both flat and curved surfaces","which objects have a circular base")) {
                val kinds=when {t.contains("ball")||t.contains("sphere")->setOf(SolidType.Sphere);t.contains("pointy")->setOf(SolidType.Cone,SolidType.Pyramid,SolidType.Tetrahedron,SolidType.TriangularPyramid);t.contains("2 circular")->setOf(SolidType.Cylinder);t.contains("both")->setOf(SolidType.Cone,SolidType.Cylinder,SolidType.Hemisphere,SolidType.Frustum);t.contains("curved")->setOf(SolidType.Sphere,SolidType.Cone,SolidType.Cylinder,SolidType.Hemisphere,SolidType.Torus,SolidType.Ellipsoid,SolidType.Capsule,SolidType.Frustum);t.contains("circular base")->setOf(SolidType.Cone,SolidType.Cylinder,SolidType.Hemisphere,SolidType.Frustum);else->setOf(SolidType.Cube,SolidType.Cuboid,SolidType.Pyramid,SolidType.TriangularPrism,SolidType.Tetrahedron)}
                return response(message="Matching solids: ${s.solids.withIndex().filter{it.value.type in kinds}.joinToString{"${it.value.type} ${it.index+1}"}.ifEmpty{"none"}}.")
            }
            if(t.startsWith("which object has the most vertices") || t.startsWith("which solid has the biggest volume") || t.startsWith("which solid has the smallest surface area")) {
                if(s.solids.isEmpty())throw Missing("Create solids first.")
                val values=s.solids.map{val m=Geometry3D.measure(it);if(t.contains("vertices"))-m.vertices.toDouble() else if(t.contains("volume"))-m.volume else m.surfaceArea};val v=values.min()
                return response(message="Matching solids: ${s.solids.indices.filter{abs(values[it]-v)<1e-9}.joinToString{"${s.solids[it].type} ${it+1}"}}.")
            }
            if(t in setOf("what is this shape called","what is the name of the selected object","is this object a cube or a cuboid")) {val ids=targets();val i=ids.single();return response(message="${s.labSessionValues[PREFIX+"name.$i"] ?: s.solids[i].type.name}.")}
            if(t.startsWith("how many faces") || t.startsWith("how many edges") || t.startsWith("how many corners") || t.startsWith("does a sphere have") || t.startsWith("does a cylinder have")) {val q=one();val m=Geometry3D.measure(q);val n=when{t.contains("edge")->m.edges;t.contains("corner")||t.contains("vertice")->m.vertices;else->m.faces};return response(message="${q.type}: $n ${if(t.contains("corner")||t.contains("vertice"))"vertices" else if(t.contains("edge"))"edges" else "faces/surfaces"}. Curved surfaces are counted separately from flat faces in school conventions.")}
            if(t=="are all the faces of this cube squares") {one();return response(message="Yes. A cube has six equal square faces.")}
            if(t=="are these 2 solids the same kind of shape") {if(s.solids.size!=2)throw Missing("Choose exactly two solids.");return response(message="Same kind: ${s.solids[0].type==s.solids[1].type}.")}
            if(Regex("^(?:what is|find|how much space|how much wrapping paper|what are|how long|how big|where is|tell me|show)").containsMatchIn(t) && listOf("volume","surface area","space","wrapping paper","base","face of","edge of","height of","radius of","diameter of","length, width","diagonal","sloping side","center of","formula").any(t::contains) && !t.contains("between")) {
                val q=one();val m=Geometry3D.measure(q);val answer=when {
                    t.contains("curved surface area")->when(q.type){SolidType.Cylinder->2*PI*q.radius*q.height;SolidType.Cone->PI*q.radius*hypot(q.radius,q.height);else->throw Unavailable("Curved-area calculation for this solid is not supported.")}.toString()+" square units"
                    t.contains("surface area") || t.contains("wrapping paper")->"${m.surfaceArea} square units"
                    t.contains("base")->(if(q.type in setOf(SolidType.Cylinder,SolidType.Cone))PI*q.radius*q.radius else q.width*q.depth).toString()+" square units"
                    t.contains("face of")->"${q.width*q.width} square units"
                    t.contains("edge of")->"${q.width} units"
                    t.contains("height of")->"${q.height} units"
                    t.contains("radius of")->"${q.radius} units"
                    t.contains("diameter of")->"${q.radius*2} units"
                    t.contains("length, width")->"length ${q.width}, width ${q.depth}, height ${q.height}"
                    t.contains("diagonal across 1 cube face")->"${q.width*sqrt(2.0)} units"
                    t.contains("diagonal")->"${sqrt(q.width*q.width+q.height*q.height+q.depth*q.depth)} units"
                    t.contains("sloping side")->"${hypot(q.radius,q.height)} units"
                    t.contains("center of")->q.position.toString()
                    else->"${Geometry3D.formula(q.type)}; dimensions ${q.width}, ${q.height}, ${q.depth}, r=${q.radius}; Volume = ${m.volume} cubic units"
                };return response(message=answer)
            }
            return null
        }
        private fun editCommands():RuhiExecutionResult? {
            if(t.startsWith("select ")) {if(t=="select nothing")return response(message="Selection cleared.");val ids=targets();return response(s.copy(labSessionValues=s.labSessionValues+(SELECTION to ids.joinToString(","))),"Selected ${ids.size} solid(s).")}
            if(t=="clear my selection") {selected=RuhiSceneContext();return response(s.copy(labSessionValues=s.labSessionValues-SELECTION),"Selection cleared.")}
            Regex("^(?:rename|call) this (?:sphere|cube) (.+)$").matchEntire(t)?.let{m->val ids=targets();require(m.groupValues[1].length<=40);return response(s.copy(labSessionValues=s.labSessionValues+(PREFIX+"name.${ids.single()}" to source.trim().trimEnd('.').substringAfterLast(' '))),"Renamed the selected solid.")}
            if((t.startsWith("make a copy") || t=="draw another 1 like that" || t=="draw another one like that" || t=="duplicate the selected object") && !t.contains("mirror")) {val q=one();require(s.solids.size<24);val old=selected.selectedSolidIndex!!;val index=s.solids.size;selected=c.copy(selectedSolidIndex=index);val value=s.labSessionValues["ruhi.studio.appearance.$old"];return response(s.copy(solids=s.solids+q.copy(position=q.position+Vec3(q.width+1,0.0,0.0)),labSessionValues=s.labSessionValues+value?.let{mapOf("ruhi.studio.appearance.$index" to it)}.orEmpty()),"Created an independent copy beside the original.")}
            if(t.startsWith("color ") || t.startsWith("paint ") || t.startsWith("change all") || Regex("^(?:make|change) .+ (?:red|blue|green|orange|purple|yellow)(?: too)?$").matches(t)) {
                val color=colors.keys.lastOrNull{Regex("\\b$it\\b").containsMatchIn(t)} ?: throw Missing("Which color?")
                val ids=targets();return response(s.copy(labSessionValues=s.labSessionValues+ids.associate{"ruhi.studio.appearance.$it" to color}),"Colored ${ids.size} solid(s) $color.")
            }
            if(t.contains("partly transparent") || t=="make it transparent") {val ids=targets();return response(s.copy(labSessionValues=s.labSessionValues+ids.associate{"ruhi.studio.appearance.$it" to "glass"}),"Applied the native translucent glass material.")}
            if(t.contains("completely see-through"))throw Unavailable("Full invisibility is not supported by the native glass material.")
            if(Regex("^(?:move|push|put) ").containsMatchIn(t) && !listOf("touches","next to","on top","xy-plane","closer","together").any(t::contains)) {
                val coordinates=Regex("\\(([^)]+)\\)").find(t)?.groupValues?.get(1)
                if(coordinates!=null) {val p=xyz(coordinates);return edit{if(t.contains(" by "))it.copy(position=it.position+p) else it.copy(position=p)}}
                if(t.contains("at the origin"))return edit{it.copy(position=Vec3(0.0,0.0,0.0))}
                val amount=Regex("($num) units?").find(t)?.groupValues?.get(1)?.toDouble() ?: throw Missing("How far should I move it?")
                val axis=when {t.contains("z-axis")->"z";t.contains("y-axis")->"y";t.contains("x-axis")||t.contains("x direction")||t.contains("right")||t.contains("left")->"x";t.contains("up")||t.contains("higher")->"z";else->throw Missing("Which direction?")}
                val delta=amount*(if(t.contains("negative")||t.contains("left")||t.contains("down"))-1 else 1)
                return edit{it.copy(position=it.position+when(axis){"x"->Vec3(delta,0.0,0.0);"y"->Vec3(0.0,delta,0.0);else->Vec3(0.0,0.0,delta)})}
            }
            if(Regex("^(?:rotate|turn|spin|flip|make) ").containsMatchIn(t) && (t.contains("degrees")||t.contains("full turn")||t.contains("upside down")||t.contains("point downward")||t.contains("lies along"))) {
                if(t.contains("around the origin")||t.contains("around line"))throw Unavailable("Group orbit and line-axis rotations are not implemented in the native assistant.")
                val degrees=Regex("($num) degrees").find(t)?.groupValues?.get(1)?.toDouble() ?: if(t.contains("full turn"))360.0 else if(t.contains("lies along"))90.0 else 180.0
                val angle=degrees*(if(t.contains("back by"))-1 else 1);require(angle.isFinite()&&abs(angle)<=3600){"Use an angle within 3600 degrees."}
                val axis=when{t.contains("upside down")||t.contains("point downward")->"x";t.contains("lies along the x")->"z";else->Regex("([xyz])-axis").find(t)?.groupValues?.get(1) ?: "y"}
                return edit{it.copy(rotation=it.rotation+when(axis){"x"->Vec3(angle,0.0,0.0);"z"->Vec3(0.0,0.0,angle);else->Vec3(0.0,angle,0.0)})}
            }
            if(t.startsWith("reflect ") || t.startsWith("flip ") || t.startsWith("make a mirror copy")) {
                if(t.contains("triangle"))throw Unavailable("Native triangle reflection is not connected yet.")
                if(t.contains("mirror copy"))throw Unavailable("Mirror-copy composition is not implemented yet.")
                return edit{q->
                    if(q.type !in setOf(SolidType.Cube,SolidType.Sphere))throw Unavailable("Native solid reflection currently supports cubes and sphere centers.")
                    when {t.contains("yz-plane")->q.copy(position=q.position.copy(x=-q.position.x),rotation=Vec3(q.rotation.x,-q.rotation.y,-q.rotation.z));t.contains("xy-plane")->q.copy(position=q.position.copy(z=-q.position.z),rotation=Vec3(-q.rotation.x,-q.rotation.y,q.rotation.z));t.contains("origin") && q.type==SolidType.Sphere->q.copy(position=q.position*-1.0);else->throw Missing("Which reflection plane?")}
                }
            }
            if(Regex("^(?:make|shrink|change|set|enlarge|give|keep) ").containsMatchIn(t) && listOf("big","small","size","tall","wide","height","radius","diameter","side length","dimensions","factor").any(t::contains)) {
                val dimensions=Regex("dimensions to ($num) by ($num) by ($num)").find(t)
                val explicit=Regex("(?:side length|height|radius|diameter) (?:(?:to|of) )?($num)").find(t)
                return edit{q->when {
                    dimensions!=null->q.copy(width=dimensions.groupValues[1].toDouble(),depth=dimensions.groupValues[2].toDouble(),height=dimensions.groupValues[3].toDouble())
                    explicit!=null->{val v=explicit.groupValues[1].toDouble();when{explicit.value.startsWith("side")->q.copy(width=v,height=v,depth=v,radius=v/2);explicit.value.startsWith("height")->q.copy(height=v);else->{val r=if(explicit.value.startsWith("diameter"))v/2 else v;q.copy(radius=r,width=2*r,depth=2*r,height=if(q.type==SolidType.Sphere)2*r else q.height)}}}
                    t.contains("halve its height")->q.copy(height=q.height/2)
                    t.contains("twice as tall")->q.copy(height=q.height*2)
                    t.contains("twice its current radius")->q.copy(radius=q.radius*2,width=q.width*2,height=q.height*2,depth=q.depth*2)
                    t.contains("wider")->q.copy(type=if(q.type==SolidType.Cube)SolidType.Cuboid else q.type,width=q.width*1.25)
                    t.contains("half its size")->scaled(q,.5)
                    t.contains("twice as big")->scaled(q,2.0)
                    t.contains("factor of")->scaled(q,Regex("factor of ($num)").find(t)?.groupValues?.get(1)?.toDouble() ?: throw Missing("Which scale factor?"))
                    t.contains("smaller")->scaled(q,.8)
                    t.contains("bigger")->scaled(q,1.25)
                    else->throw Missing("Specify a positive size or scale factor.")
                }}
            }
            return null
        }
        private fun vectorCommands():RuhiExecutionResult? {
            if(t.startsWith("how long is segment ab"))return response(message="Length = ${(point("b").position-point("a").position).magnitude()} units.")
            if(t in setOf("which way does vector ab point","what are the 3 components of this vector","how long is this arrow","make a unit vector pointing the same way")) {
                val v=if(t.contains("ab"))point("b").position-point("a").position else s.vectors3D.singleOrNull()?.components ?: throw Missing("Select a single vector by leaving one vector in the scene.")
                if(t.contains("unit vector")){require(v.magnitude()>1e-9){"A zero vector has no direction."};return response(s.copy(vectors3D=s.vectors3D+Vector3D("unit-${java.util.UUID.randomUUID()}",Vec3(0.0,0.0,0.0),v.normalized())),"Unit vector = ${v.normalized()}.")}
                return response(message=if(t.contains("how long"))"Length = ${v.magnitude()} units." else "Components = $v.")
            }
            if(t in setOf("add these 2 vectors","what is the angle between these 2 arrows","are these vectors perpendicular","are these vectors parallel","find the dot product of these vectors","find a vector perpendicular to both arrows","subtract vector ac from vector ab")) {
                val v=if(t.contains("subtract"))listOf(point("b").position-point("a").position,point("c").position-point("a").position) else vectorPair().map{it.components};val a=v[0];val b=v[1]
                val cross=AnalyticGeometry3D.cross(a,b);val product=a.magnitude()*b.magnitude()
                if(t.contains("angle")||t.contains("parallel")||t.contains("perpendicular"))require(product>1e-12){"Zero vectors have no defined angle or direction."}
                if(t.startsWith("find a vector"))require(cross.magnitude()>1e-12){"The cross product is zero for parallel vectors; it has no perpendicular direction."}
                val answer=when{t.startsWith("add")->(a+b).toString();t.contains("subtract")->(a-b).toString();t.contains("angle")->"${Math.toDegrees(acos((a.dot(b)/product).coerceIn(-1.0,1.0)))} degrees";t.contains("dot product")->a.dot(b).toString();t.startsWith("find a vector")->cross.toString();t.contains("perpendicular")->(abs(a.dot(b))<1e-9*product).toString();else->(cross.magnitude()<1e-9*product).toString()}
                return response(message="Result = $answer.")
            }
            return null
        }
        private fun sceneCommands():RuhiExecutionResult? {
            if(t=="clear all solids but keep my points and lines")return response(s.copy(solids=emptyList(),labSessionValues=s.labSessionValues.filterKeys{!it.startsWith("ruhi.studio.appearance.")&&!it.startsWith(RuhiMotionEngine.PREFIX)&&!it.startsWith(PREFIX+"name.")&&it!=SELECTION}),"Cleared solids; kept points and vectors.")
            if(t=="save this 3d scene") {val snapshot=s.copy(labSessionValues=s.labSessionValues-SAVED-RuhiDialogueMemory.KEY);val encoded=WorkspaceProjectCodec.encode(snapshot);require(encoded.length<1000000);return response(s.copy(labSessionValues=s.labSessionValues+(SAVED to encoded)),"Saved this scene in the workspace, including sizes, positions, colors, points and vectors.")}
            if(t.startsWith("load my saved scene") || t in setOf("load the saved 3d scene","restore the saved 3d scene")) {
                val snapshot=s.labSessionValues[SAVED] ?: throw Missing("Save a scene first.");val restored=WorkspaceProjectCodec.decode(snapshot).state ?: throw IllegalArgumentException("The saved scene is invalid.");require(restored.id==s.id);selected=RuhiSceneContext()
                fun own(key:String)=key.startsWith(PREFIX)||key.startsWith("ruhi.studio.appearance.")||key.startsWith(RuhiMotionEngine.PREFIX)||key=="ruhi.studio.selected.solid"
                val values=s.labSessionValues.filterKeys{!own(it)}+restored.labSessionValues.filterKeys{own(it)}+(SAVED to snapshot)
                return response(s.copy(solids=restored.solids,points3D=restored.points3D,vectors3D=restored.vectors3D,planes3D=restored.planes3D,linears3D=restored.linears3D,labSessionValues=values),"Restored the saved 3D scene, colors and dimensions.")
            }
            if(t.startsWith("delete the selected sphere")) {
                val ids=targets().toSet();val kept=s.solids.indices.filter{it !in ids};val remap=kept.withIndex().associate{it.value to it.index};val values=s.labSessionValues.filterKeys{!it.startsWith("ruhi.studio.appearance.")&&!it.startsWith(RuhiMotionEngine.PREFIX)&&!it.startsWith(PREFIX+"name.")&&!it.startsWith(RuhiTransform3DLanguage.PREFIX)&&it!=SELECTION}.toMutableMap()
                for((old,new)in remap)for(prefix in listOf("ruhi.studio.appearance.",RuhiMotionEngine.PREFIX,PREFIX+"name.",RuhiTransform3DLanguage.PREFIX+"opacity.",RuhiTransform3DLanguage.PREFIX+"mode.",RuhiTransform3DLanguage.PREFIX+"explosion."))s.labSessionValues[prefix+old]?.let{values[prefix+new]=it}
                selected=RuhiSceneContext();return response(s.copy(solids=kept.map{s.solids[it]},labSessionValues=values),"Deleted ${ids.size} selected sphere(s).")
            }
            if(t=="undo the last thing you did" || t=="redo that change")return null // Existing scoped history owns these operations.
            if(t.startsWith("undo only"))throw Unavailable("Selective color undo is not available; ordinary Undo restores the last complete change.")
            return null
        }
        fun execute():RuhiExecutionResult? {
            try {
                safety()?.let { return it }
                create()?.let { return it }
                points()?.let { return it }
                pointQueries()?.let { return it }
                solidQueries()?.let { return it }
                editCommands()?.let { return it }
                vectorCommands()?.let { return it }
                sceneCommands()?.let { return it }
            // Let existing shared grammar handle previously implemented commands, not an expansive learned rewrite.
            if(t in setOf("undo","redo","stop the rotation","stop rotation") || t.startsWith("make the height ") || t.startsWith("and the radius ") || t.startsWith("calculate ") || t.startsWith("scale ") || t.startsWith("in ") || t.contains("rotating"))return null
            throw Unavailable("I understand this 3D geometry request, but its construction or control is not connected to the native engine yet. No objects were changed.")
        } catch(e:Missing) {return response(message=e.message ?: "Choose the target.",status=GraphCommandStatus.CLARIFICATION)}
          catch(e:Unavailable) {return response(message=e.message ?: "This operation is unavailable.",status=GraphCommandStatus.UNSUPPORTED)}
          catch(e:IllegalArgumentException) {return response(message=e.message ?: "Invalid numeric input.",status=GraphCommandStatus.INVALID)}
    }
    }
}
