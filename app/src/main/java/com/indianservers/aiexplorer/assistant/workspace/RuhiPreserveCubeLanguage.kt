package com.indianservers.aiexplorer.assistant.workspace

import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import java.util.Base64

/** A pending-action constraint is validated before the one public history transaction. */
object RuhiPreserveCubeLanguage {
    private const val KIND="preserve_cube"
    fun accepts(source:String)=RuhiMathLanguage.normalize(source).replace('’','\'') in setOf("do not change the existing cube","don't change the existing cube")||source.startsWith("Preserve cube ")||source.startsWith("Use preserved object ")
    private fun guard(s:WorkspaceState):String {
        val native=s.labSessionValues.filterKeys{it.startsWith("ruhi.studio.appearance.")||it.startsWith(RuhiMotionEngine.PREFIX)||it.startsWith(RuhiTransform3DLanguage.PREFIX)||it.startsWith("ruhi.geometry3d.name.")}.toSortedMap()
        return Base64.getEncoder().encodeToString(java.security.MessageDigest.getInstance("SHA-256").digest((s.id+s.module+s.solids+native).toByteArray(Charsets.UTF_8)))
    }
    private fun field(p:RuhiPending,name:String)=p.targets.firstOrNull{it.startsWith("$name:")}?.substringAfter(':').orEmpty()
    private fun candidates(p:RuhiPending)=p.targets.filter{it.startsWith("candidate:")}.map{it.substringAfter(':')}
    private fun index(ref:String,kind:String)=if(kind=="target")ref.removePrefix("solid-").toIntOrNull()else ref.toIntOrNull()
    private fun targetAnswer(ref:String)="Use preserved object "+Base64.getEncoder().encodeToString(ref.toByteArray(Charsets.UTF_8))
    private fun label(s:WorkspaceState,i:Int)="${s.solids[i].type} ${i+1}"
    fun restored(s:WorkspaceState,p:RuhiPending):RuhiDialogueView {
        val chips=if(field(p,"phase")=="cube")s.solids.indices.filter{s.solids[it].type==SolidType.Cube}.take(5).map{RuhiAnswerChip(label(s,it),"Preserve cube $it")}else candidates(p).mapNotNull{ref->index(ref,field(p,"kind"))?.takeIf{it in s.solids.indices}?.let{RuhiAnswerChip(label(s,it),targetAnswer(ref))}}.take(5)
        return RuhiDialogueView(RuhiDialogueAction.ASK_CLARIFICATION,chips,true)
    }
    private fun wrapped(s:WorkspaceState,original:RuhiPending,cube:Int?,refs:List<String>,phase:String)=RuhiPending(KIND,original.source,s.module.name,guard(s),listOf("phase:$phase","kind:${original.kind}","scene:${original.scene}")+cube?.let{listOf("protected:$it")}.orEmpty()+refs.map{"candidate:$it"})
    private fun protectedValues(s:WorkspaceState,i:Int)=listOf("ruhi.studio.appearance.",RuhiMotionEngine.PREFIX,RuhiTransform3DLanguage.PREFIX+"opacity.",RuhiTransform3DLanguage.PREFIX+"mode.",RuhiTransform3DLanguage.PREFIX+"explosion.","ruhi.geometry3d.name.").associateWith{s.labSessionValues[it+i]}
    fun execute(s:WorkspaceState,source:String,h:WorkspaceHistoryByModule,c:RuhiSceneContext,m:RuhiInputModality,memory:RuhiDialogueMemory):RuhiPointConversation.Outcome? {
        val saved=memory.pending?.takeIf{it.kind==KIND}
        if(!accepts(source)&&saved==null)return null
        fun outcome(message:String,p:RuhiPending?=saved ?: memory.pending,status:GraphCommandStatus=GraphCommandStatus.CLARIFICATION,state:WorkspaceState=s,ctx:RuhiSceneContext=c,parameters:Map<String,Any> = emptyMap(),command:RuhiCommand?=null):RuhiPointConversation.Outcome {
            val frame=RuhiSemanticFrame("DIALOGUE_CONTEXT",RuhiEngine.GEOMETRY_3D,mapOf("constraint" to "preserve_cube","target" to "pending_action")+parameters)
            val view=if(p?.kind==KIND)restored(state,p)else RuhiDialogueView(RuhiDialogueAction.ASK_CLARIFICATION,pending=p!=null)
            return RuhiPointConversation.Outcome(RuhiExecutionResult(state,status,message,command=command,context=ctx,dialogue=if(status==GraphCommandStatus.APPLIED)RuhiDialogueView(RuhiDialogueAction.EXECUTE)else view,semanticFrame=frame),p)
        }
        if(s.module!=MathModule.Geometry3D)return outcome("Open the 3D geometry workspace and identify the cube and pending action.")
        if(saved!=null&&saved.scene!=guard(s))return outcome("The solid scene changed. State the action and cube to preserve again.",p=null)
        val original=if(saved==null)memory.pending else RuhiPending(field(saved,"kind"),saved.source,saved.module,field(saved,"scene"),candidates(saved))
        if(original==null)return outcome("Which pending action should leave the existing cube unchanged?",p=null)
        if(original.kind !in setOf("target","transform_solid"))return outcome("Which single-object edit should leave the cube unchanged? Complete or cancel the current request first.")
        if(memory.module!=s.module.name||memory.lastScene.isNotEmpty()&&memory.lastScene!="${s.id}:${s.module}:${listOf(s.points,s.shapes,s.solids,s.points3D,s.planes3D,s.vectors3D,s.functions,s.surfaceLayers).hashCode()}")return outcome("The scene changed. State the action again for the current objects.",p=null)
        val cubes=s.solids.indices.filter{s.solids[it].type==SolidType.Cube}
        if(cubes.isEmpty())return outcome("There is no cube in this scene. Which object should remain unchanged?")
        val explicit=c.selectedSolidIndex?.takeIf{it in cubes}
        val cube=if(source.startsWith("Preserve cube ")&&saved!=null&&field(saved,"phase")=="cube")source.removePrefix("Preserve cube ").toIntOrNull()?.takeIf{it in cubes}else saved?.let{field(it,"protected").toIntOrNull()} ?: explicit ?: cubes.singleOrNull()
        if(cube==null){val p=wrapped(s,original,null,original.targets,"cube");return outcome("Which existing cube must remain unchanged?",p)}
        val allowed=original.targets.filter{ref->index(ref,original.kind)?.let{it in s.solids.indices&&it!=cube}==true}
        val p=wrapped(s,original,cube,allowed,"target")
        if(allowed.isEmpty())return outcome("The pending action targets only the protected cube. Which different action should I perform? Cancel this request to replace it.",p)
        val picked=if(source.startsWith("Use preserved object "))runCatching{String(Base64.getDecoder().decode(source.removePrefix("Use preserved object ")),Charsets.UTF_8)}.getOrNull()?.takeIf{it in allowed}else if(saved==null||source.startsWith("Preserve cube "))allowed.singleOrNull()else null
        if(picked==null)return outcome("Which object should receive the pending edit while the cube stays unchanged?",p)
        val target=index(picked,original.kind)!!
        val answer=if(original.kind=="transform_solid")"Use transform solid $target"else "${s.solids[target].type.name} ${target+1}"
        val pending=original.copy(targets=allowed)
        val staged=s.copy(labSessionValues=s.labSessionValues+(RuhiDialogueMemory.KEY to memory.copy(pending=pending,lastChoices=emptyList(),pendingPrompt="").encode()))
        val result=RuhiDialogueManager().execute(staged,answer,WorkspaceHistoryByModule(),c,m)
        if(result.status!=GraphCommandStatus.APPLIED)return outcome(result.message,p,status=result.status)
        val next=result.state
        val now=System.nanoTime()/1_000_000
        val intact=next.solids.getOrNull(cube)==s.solids[cube]&&protectedValues(next,cube)==protectedValues(s,cube)&&RuhiMotionEngine.solids(s,now).getOrNull(cube)==RuhiMotionEngine.solids(next,now).getOrNull(cube)
        if(!intact)return outcome("That edit would change the protected cube. Which different action should I perform?",p)
        val committed=h.forModule(s.module).execute(s,GraphWorkspaceCommand(s,GraphWorkspaceCommand.reconcileDocument(s,next),"Ruhi: preserve cube during pending edit"))
        return outcome(result.message+" Kept ${label(s,cube)} unchanged.",p=null,status=GraphCommandStatus.APPLIED,state=committed,ctx=result.context,parameters=mapOf("protected_solid" to cube,"resolved_object_id" to "o:$target","applied_intent" to result.semanticFrame?.intent.orEmpty(),"applied_parameters" to result.semanticFrame?.parameters.orEmpty()),command=result.command)
    }
}
