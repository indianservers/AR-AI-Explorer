package com.indianservers.aiexplorer.assistant.workspace

import com.indianservers.aiexplorer.workspace.*
import java.util.Base64

/** Natural-language adapter for native, point-defined planes and their smaller dihedral angle. */
object RuhiSpatialPlaneLanguage {
    const val PREFIX="ruhi.geometry3d.plane."
    const val SELECTION=PREFIX+"selection"
    private const val MARK=PREFIX+"angle."
    private val label="[\\p{L}_][\\p{L}\\p{N}_]{0,63}"
    private val create=Regex("(?i)^(?:draw|create|make|construct) (?:a |the )?plane(?: ($label))? through (?:points? )?(.+)$")
    private fun encode(ids:List<String>)=ids.joinToString(","){Base64.getUrlEncoder().withoutPadding().encodeToString(it.toByteArray(Charsets.UTF_8))}
    private fun decode(value:String)=runCatching{require(value.length<=12000);value.split(',').map{String(Base64.getUrlDecoder().decode(it),Charsets.UTF_8)}.also{require(it.size==2&&it.distinct().size==2)}}.getOrDefault(emptyList())
    fun selected(s:WorkspaceState)=decode(s.labSessionValues[SELECTION].orEmpty())
    fun marks(s:WorkspaceState)=s.labSessionValues.filterKeys{it.startsWith(MARK)}.values.map(::decode).filter{it.size==2}
    fun patches(s:WorkspaceState)=s.planes3D.filter{it.visible}.mapNotNull{runCatching{NativePlaneGeometry.evaluate(s,it)}.getOrNull()}
    fun drawings(s:WorkspaceState)=marks(s).mapNotNull{ids->runCatching{NativePlaneGeometry.angle(s,ids).let{RuhiSpatialAngleLanguage.Drawing(it.center,it.first,it.second,it.arc,it.degrees)}}.getOrNull()}
    fun accepts(text:String)=text.length<=2000&&(create.matches(text.trim().trimEnd('.','?'))||Regex("(?i)^(?:select|use) planes? .+ and .+$|^clear plane selection$|\\bdihedral\\b").containsMatchIn(text.trim().trimEnd('.','?')))
    private fun chips(s:WorkspaceState,ids:List<String>):List<RuhiAnswerChip> {
        val planes=ids.mapNotNull{id->s.planes3D.singleOrNull{it.id==id}}.filter{a->s.planes3D.count{it.name.equals(a.name,true)}==1}
        return planes.indices.flatMap{i->(i+1 until planes.size).map{j->RuhiAnswerChip("${planes[i].name} + ${planes[j].name}","Use planes ${planes[i].name} and ${planes[j].name}")}}.take(4)
    }
    fun restored(s:WorkspaceState,p:RuhiPending)=RuhiDialogueView(RuhiDialogueAction.ASK_CLARIFICATION,chips(s,p.targets),true)
    private fun scene(s:WorkspaceState)=java.security.MessageDigest.getInstance("SHA-256").digest((s.id+"|"+s.module+"|"+s.planes3D+"|"+s.points3D).toByteArray(Charsets.UTF_8)).joinToString(""){"%02x".format(it)}
    fun execute(s:WorkspaceState,source:String,h:WorkspaceHistoryByModule,c:RuhiSceneContext,m:RuhiInputModality,p:RuhiPending?):RuhiPointConversation.Outcome? {
        if(p!=null&&p.kind!="spatial_plane_pair")return null
        if(s.module!=MathModule.Geometry3D) {if(p==null)return null;return RuhiPointConversation.Outcome(RuhiExecutionResult(s,GraphCommandStatus.CLARIFICATION,"The studio changed. Ask the plane question again.",context=c),null)}
        if(source.length>2000)return null
        val text=source.trim().trimEnd('.','?');val low=text.lowercase()
        if(p==null&&!accepts(text))return null
        if(p==null&&low=="clear plane selection")return RuhiPointConversation.Outcome(RuhiExecutionResult(s.copy(labSessionValues=s.labSessionValues-SELECTION),GraphCommandStatus.APPLIED,"Plane selection cleared.",context=c.copy(selectedPlaneIds=emptyList())),null)
        var frame=RuhiSemanticFrame("MEASURE_ANGLE",RuhiEngine.GEOMETRY_3D,mapOf("planes" to "selected_pair","kind" to "dihedral"))
        fun ask(message:String,pending:RuhiPending?=null)=RuhiPointConversation.Outcome(RuhiExecutionResult(s,GraphCommandStatus.CLARIFICATION,message,context=c,dialogue=RuhiDialogueView(RuhiDialogueAction.ASK_CLARIFICATION,pending?.let{chips(s,it.targets)}.orEmpty(),pending!=null),semanticFrame=frame),pending)
        if(p!=null&&(p.module!=s.module.name||p.scene!=scene(s)))return ask("The plane scene changed. Ask the dihedral-angle question again for the current planes.")
        if(p!=null&&low in setOf("resume","continue","resume pending"))return ask("Which two planes should I use?",p)
        try {
            val creation=if(p==null)create.matchEntire(text)else null
            if(creation!=null) {
                val body=creation.groupValues[2]
                if(Regex("(?i)\\b(?:parallel|perpendicular)\\b").containsMatchIn(body))return null
                require(!body.contains("one straight line",true)){"Three collinear points cannot define a unique plane."}
                val match=Regex("^($label)\\s*,\\s*($label)\\s*,?\\s*(?:and\\s+)?($label)$").matchEntire(body)
                    ?: return ask("Name three non-collinear 3D points, separated by commas.")
                val names=match.groupValues.drop(1)
                val points=names.map{name->s.points3D.singleOrNull{it.name.equals(name,true)} ?: return ask("Point $name is missing or ambiguous. Create it with 3D coordinates or choose a unique point name.")}
                val name=creation.groupValues[1].ifEmpty{generateSequence(1){it+1}.map{"Plane$it"}.first{candidate->s.planes3D.none{it.name.equals(candidate,true)}}}
                if(s.planes3D.any{it.name.equals(name,true)})return ask("Plane $name already exists. Choose an unused plane name.")
                require(s.planes3D.size<100){"Remove a plane before creating more."}
                val plane=Plane3DObject("ruhi-plane-${java.util.UUID.randomUUID()}",name,points.map{it.id});NativePlaneGeometry.evaluate(s,plane)
                frame=RuhiSemanticFrame("CREATE_PLANE",RuhiEngine.GEOMETRY_3D,mapOf("name" to name,"points" to names))
                val next=s.copy(planes3D=s.planes3D+plane)
                val committed=h.forModule(s.module).execute(s,GraphWorkspaceCommand(s,GraphWorkspaceCommand.reconcileDocument(s,next),"Ruhi: plane $name"))
                return RuhiPointConversation.Outcome(RuhiExecutionResult(committed,GraphCommandStatus.APPLIED,"Created plane $name through ${names.joinToString(", ")}. Its displayed patch follows all three points.",RuhiCommand(engine=RuhiEngine.GEOMETRY_3D,operation=RuhiOperation.PointQuery(frame),modality=m),c,semanticFrame=frame),null)
            }
            val explicit=Regex("(?i)^(?:select|use) planes? (.+)$").find(text)?.groupValues?.get(1)
            val contextual=Regex("(?i)\\b(?:two selected|these two|the selected) planes\\b").containsMatchIn(text)
            val body=explicit ?: if(contextual)null else Regex("(?i)dihedral angle between (?:planes )?(.+)$").find(text)?.groupValues?.get(1) ?: if(p!=null)text else null
            val named=body?.let{b->s.planes3D.flatMap{a->s.planes3D.filter{v->v.id!=a.id&&b.equals("${a.name} and ${v.name}",true)}.map{listOf(a.id,it.id)}}.distinct().singleOrNull()}
            val ids=named ?: if(body==null)c.selectedPlaneIds.takeIf{it.size==2} ?: selected(s)else emptyList()
            if(ids.size!=2) {
                val pending=if(s.planes3D.size>=2)RuhiPending("spatial_plane_pair",p?.source ?: source,s.module.name,scene(s),s.planes3D.map{it.id})else null
                return ask(if(s.planes3D.size<2)"Create two planes from non-collinear 3D points first." else "Which two planes should I use? Choose a pair or enter their unique names.",pending)
            }
            if(ids.any{id->s.planes3D.count{it.id==id}!=1})return ask("A selected plane is missing or ambiguous. Select the current pair again.")
            val planes=ids.map{id->s.planes3D.single{it.id==id}}
            if(p==null&&explicit!=null) {
                return RuhiPointConversation.Outcome(RuhiExecutionResult(s.copy(labSessionValues=s.labSessionValues+(SELECTION to encode(ids))),GraphCommandStatus.APPLIED,"Selected planes ${planes.joinToString(" and "){it.name}}.",context=c.copy(selectedPlaneIds=ids)),null)
            }
            val angle=NativePlaneGeometry.angle(s,ids);val key=MARK+encode(ids)
            require(marks(s).size<32||s.labSessionValues.containsKey(key)){"Remove a plane-angle annotation before adding more."}
            val next=s.copy(labSessionValues=s.labSessionValues+mapOf(SELECTION to encode(ids),key to encode(ids)))
            val committed=h.forModule(s.module).execute(s,GraphWorkspaceCommand(s,GraphWorkspaceCommand.reconcileDocument(s,next),"Ruhi: dihedral angle"))
            return RuhiPointConversation.Outcome(RuhiExecutionResult(committed,GraphCommandStatus.APPLIED,"Smaller dihedral angle between ${planes.joinToString(" and "){it.name}} = ${angle.degrees} degrees. ${if(angle.parallel)"The planes are parallel; their orientation marker is placed on the first plane."else "Marked at their intersection; follows the defining points."}",RuhiCommand(engine=RuhiEngine.GEOMETRY_3D,operation=RuhiOperation.PointQuery(frame),modality=m),c.copy(selectedPlaneIds=ids),semanticFrame=frame),null)
        }catch(e:IllegalArgumentException){return RuhiPointConversation.Outcome(RuhiExecutionResult(s,GraphCommandStatus.INVALID,e.message ?: "Invalid plane geometry.",context=c,semanticFrame=frame),null)}
    }
}
