package com.indianservers.aiexplorer.assistant.workspace

import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import java.io.*
import java.util.Base64
import java.security.MessageDigest

/** Exact preimage for correcting a completed edit; replay still uses validated native commands. */
object RuhiAppliedTargetCorrection {
    const val KEY="ruhi.dialogue.applied-target.v1"
    private const val KIND="applied_target"
    private const val ANSWER="Use corrected object "
    private fun native(s:WorkspaceState)=s.copy(universalMathDocument=null,labSessionValues=s.labSessionValues.filterKeys {
        it!=KEY&&it!=RuhiDialogueMemory.KEY&&it!=RuhiActionContext.KEY&&!it.startsWith("ruhi.qa.")&&!it.contains("::ruhi.measure.")
    })
    private fun guard(s:WorkspaceState):String {
        val n=native(s)
        val content=listOf(n.id,n.module,n.points,n.shapes,n.pointDependencies,n.geometryConstraints,n.geometryGroups,n.solids,n.points3D,n.planes3D,n.vectors3D,n.functions,n.surfaceLayers,n.labSessionValues.toSortedMap()).toString()
        return Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(content.toByteArray(Charsets.UTF_8)))
    }
    fun record(before:WorkspaceState,result:RuhiExecutionResult):WorkspaceState {
        val s=result.state
        if(result.status!=GraphCommandStatus.APPLIED||result.semanticFrame?.parameters?.get("action")=="display_units")return s
        val task=RuhiActionContext.read(s)
        val valid=task!=null&&task.kind in setOf("scale","rotate","position","translate","color")&&s.module in setOf(MathModule.Geometry2D,MathModule.Geometry3D)&&guard(before)!=guard(s)
        val encoded=if(valid)runCatching {
            val a=WorkspaceProjectCodec.encode(native(before));val b=WorkspaceProjectCodec.encode(native(s))
            require(a.length<=100_000&&b.length<=100_000)
            val out=ByteArrayOutputStream();DataOutputStream(out).use{it.writeInt(1);for(text in listOf(a,b)){val bytes=text.toByteArray(Charsets.UTF_8);it.writeInt(bytes.size);it.write(bytes)}}
            Base64.getEncoder().encodeToString(out.toByteArray())
        }.getOrNull()else null
        return s.copy(labSessionValues=if(encoded==null)s.labSessionValues-KEY else s.labSessionValues+(KEY to encoded))
    }
    private fun read(s:WorkspaceState):Pair<WorkspaceState,WorkspaceState>?=runCatching {
        val raw=s.labSessionValues[KEY] ?: return null;require(raw.length<=1_100_000)
        DataInputStream(ByteArrayInputStream(Base64.getDecoder().decode(raw))).use{i->
            require(i.readInt()==1)
            val states=List(2){val n=i.readInt();require(n in 1..400_000);val bytes=ByteArray(n);i.readFully(bytes);WorkspaceProjectCodec.decode(String(bytes,Charsets.UTF_8)).state ?: error("Invalid edit snapshot")}
            require(i.available()==0);states[0] to states[1]
        }
    }.getOrNull()
    fun accepts(source:String)=source.startsWith(ANSWER)
    fun execute(s:WorkspaceState,source:String,h:WorkspaceHistoryByModule,c:RuhiSceneContext,m:RuhiInputModality,memory:RuhiDialogueMemory):RuhiPointConversation.Outcome {
        fun outcome(message:String,p:RuhiPending?=null,status:GraphCommandStatus=GraphCommandStatus.CLARIFICATION,state:WorkspaceState=s,ctx:RuhiSceneContext=c,frame:RuhiSemanticFrame?=null,command:RuhiCommand?=null,chips:List<RuhiAnswerChip> = emptyList())=RuhiPointConversation.Outcome(RuhiExecutionResult(state,status,message,command=command,context=ctx,semanticFrame=frame,dialogue=RuhiDialogueView(if(status==GraphCommandStatus.APPLIED)RuhiDialogueAction.EXECUTE else RuhiDialogueAction.ASK_CLARIFICATION,chips,p!=null)),p)
        val task=RuhiActionContext.read(s) ?: return outcome("Which action should I apply to the blue object?")
        val snapshots=read(s) ?: return outcome("Which action should I apply to the blue object?")
        val (before,after)=snapshots
        if(task.module!=s.module.name||before.id!=s.id||after.id!=s.id||guard(after)!=guard(s))return outcome("The scene changed. State the action and target again for the current objects.")
        val command=RuhiActionContext.command(task) ?: return outcome("Which action should I apply to the blue object?")
        fun blue(color:String?)=color?.lowercase() in setOf("blue","#0000ff","#ff0000ff")
        val candidates=when(s.module){MathModule.Geometry3D->s.solids.indices.filter{blue(s.labSessionValues["ruhi.studio.appearance.$it"])}.map{"o:$it"};MathModule.Geometry2D->s.shapes.filter{blue(it.styleKey)}.map{"s:${it.id}"};else->emptyList()}
        val pending=memory.pending?.takeIf{it.kind==KIND}
        val picked=if(source.startsWith(ANSWER))runCatching{String(Base64.getDecoder().decode(source.removePrefix(ANSWER)),Charsets.UTF_8)}.getOrNull()?.takeIf{pending!=null&&pending.scene==guard(s)&&it in pending.targets&&it in candidates}else candidates.singleOrNull()
        if(picked==null){
            if(candidates.isEmpty())return outcome("No blue object matches the completed action. Which object should I use?")
            val p=RuhiPending(KIND,source,s.module.name,guard(s),candidates)
            val chips=candidates.take(5).map{ref->RuhiAnswerChip(if(ref.startsWith("o:"))"${s.solids[ref.drop(2).toInt()].type} ${ref.drop(2).toInt()+1}" else s.shapes.first{it.id==ref.drop(2)}.name,ANSWER+Base64.getEncoder().encodeToString(ref.toByteArray(Charsets.UTF_8)))}
            return outcome("More than one blue object matches. Which one should receive the completed action?",p,chips=chips)
        }
        if(picked==task.target)return outcome("The completed action already targets that blue object.",status=GraphCommandStatus.APPLIED)
        // Restore only fields changed by the original edit, retaining current view/unit/dialogue metadata.
        val restored=GraphWorkspaceCommand(after,before,"Restore original target").apply(s)
        val clean=restored.copy(labSessionValues=(restored.labSessionValues-KEY-RuhiActionContext.KEY)+(RuhiDialogueMemory.KEY to memory.copy(pending=null,lastChoices=emptyList(),pendingPrompt="").encode()))
        val ctx=if(picked.startsWith("o:"))RuhiSceneContext(selectedSolidIndex=picked.drop(2).toInt())else RuhiSceneContext(selectedShapeId=picked.drop(2))
        val result=RuhiDialogueManager().execute(clean,command,WorkspaceHistoryByModule(),ctx,m)
        if(result.status!=GraphCommandStatus.APPLIED)return outcome("${result.message} State the action again for the blue object.")
        val originalIntact=if(task.target.startsWith("o:")) {
            val i=task.target.drop(2).toIntOrNull()
            i!=null&&result.state.solids.getOrNull(i)==restored.solids.getOrNull(i)
        }else {
            val shape=restored.shapes.firstOrNull{it.id==task.target.drop(2)}
            shape!=null&&result.state.shapes.firstOrNull{it.id==shape.id}==shape&&shape.pointIndices.all{result.state.points.getOrNull(it)==restored.points.getOrNull(it)}
        }
        if(!originalIntact)return outcome("That correction would also change the original object. State an independent target and action.")
        val next=result.state.copy(labSessionValues=result.state.labSessionValues-KEY-RuhiActionContext.KEY)
        val committed=h.forModule(s.module).execute(s,GraphWorkspaceCommand(s,GraphWorkspaceCommand.reconcileDocument(s,next),"Ruhi: correct completed target"))
        return outcome("Restored the original object. ${result.message}",status=GraphCommandStatus.APPLIED,state=committed,ctx=result.context,frame=result.semanticFrame,command=result.command)
    }
}
