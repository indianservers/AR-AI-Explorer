package com.indianservers.aiexplorer.assistant.workspace

import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import kotlin.math.abs

/** Retain an endpoint-dependent operation while the student identifies actual current points. */
internal object RuhiEndpointConversation {
    private val label="[\\p{L}_][\\p{L}\\p{N}_]{0,63}"
    private val midpointName="[\\p{L}_][\\p{L}\\p{N}_]{0,31}"
    private val labelReply=Regex("(?i)^(?:use midpoint label )?($midpointName)[.?]?$")
    private val controls=setOf("cancel","undo","redo","resume","continue","nevermind")
    private val reply=Regex("(?i)^(?:use endpoints )?($label) and ($label)[.?]?$")
    private fun bindings(p:RuhiPending?):Map<String,Pair<String,String>> = (0..3).mapNotNull{i->
        val token=p?.slots?.get("b${i}_token") ?: return@mapNotNull null
        val a=p.slots["b${i}_a"] ?: return@mapNotNull null;val b=p.slots["b${i}_b"] ?: return@mapNotNull null
        token to (a to b)
    }.toMap()
    private fun slots(values:Map<String,Pair<String,String>>,intent:String,chosenLabel:String?=null,shapeId:String?=null):Map<String,String> {
        require(values.size<=4){"Clarify at most four segment references per command."}
        return buildMap{put("intent",intent);chosenLabel?.let{put("label",it)};shapeId?.let{put("shape",it)};values.entries.forEachIndexed{i,e->put("b${i}_token",e.key);put("b${i}_a",e.value.first);put("b${i}_b",e.value.second)}}
    }
    private fun shapeKey(id:String)=java.security.MessageDigest.getInstance("SHA-256").digest(id.toByteArray(Charsets.UTF_8)).joinToString(""){"%02x".format(it.toInt() and 255)}
    fun restored(s:WorkspaceState,p:RuhiPending):RuhiDialogueView {
        if(s.module!=MathModule.Geometry2D)return RuhiDialogueView(RuhiDialogueAction.ASK_CLARIFICATION,emptyList(),true)
        if(p.kind=="endpoint_shape") {
            val candidates=RuhiEndpointResolver.selectableShapes(s,p.source)
            val chips=candidates.take(5).mapIndexed{i,shape->
                val encoded=shapeKey(shape.id)
                val title=if(candidates.count{it.name.equals(shape.name,true)}>1)"${shape.name.take(90)} (${shape.type.name} ${i+1})"else shape.name.take(120)
                RuhiAnswerChip(title,"Use segment $encoded")
            }
            return RuhiDialogueView(RuhiDialogueAction.ASK_CLARIFICATION,chips,true)
        }
        if(p.kind=="midpoint_label") {
            val used=RuhiEndpointResolver.references(s).map{it.first}+s.labSessionValues.keys.filter{it.startsWith(StudioReferenceMetadata.POINT_PREFIX)}.map{it.removePrefix(StudioReferenceMetadata.POINT_PREFIX)}
            val chips=generateSequence(1){it+1}.map{"Mid$it"}.filter{candidate->used.none{it.equals(candidate,true)}}.take(3).map{RuhiAnswerChip(it,"Use midpoint label $it")}.toList()
            return RuhiDialogueView(RuhiDialogueAction.ASK_CLARIFICATION,chips,true)
        }
        val resolver=RuhiEndpointResolver(s,RuhiSceneContext())
        val points=RuhiEndpointResolver.references(s).mapNotNull{(name,_)->runCatching{resolver.point(name)}.getOrNull()?.takeIf{it.position.x.isFinite()&&it.position.y.isFinite()&&abs(it.position.x)<=10000&&abs(it.position.y)<=10000&&it.name.matches(Regex(label))}}.distinctBy{it.index}
        val chips=sequence {for(i in points.indices)for(j in i+1 until points.size){val a=points[i];val b=points[j];if(p.slots["intent"]=="CALCULATE_SLOPE"&&a.position==b.position)continue;yield(RuhiAnswerChip("${a.name} + ${b.name}","Use endpoints ${a.name} and ${b.name}"))}}.take(5).toList()
        return RuhiDialogueView(RuhiDialogueAction.ASK_CLARIFICATION,chips,true)
    }
    fun execute(s:WorkspaceState,source:String,h:WorkspaceHistoryByModule,c:RuhiSceneContext,m:RuhiInputModality,pendingInput:RuhiPending?):RuhiPointConversation.Outcome? {
        val pending=pendingInput?.takeIf{it.kind in setOf("endpoint_reference","midpoint_label","endpoint_shape")}
        if(pending!=null&&pending.module!=s.module.name)return RuhiPointConversation.Outcome(RuhiExecutionResult(s,GraphCommandStatus.CLARIFICATION,"The studio changed. Ask the construction question again.",context=c),null)
        if(s.module!=MathModule.Geometry2D)return null
        val answer=if(pending?.kind=="endpoint_reference")reply.matchEntire(source.trim())else null
        val labelAnswer=if(pending?.kind=="midpoint_label"&&source.trim().lowercase() !in controls)labelReply.matchEntire(source.trim())else null
        val encoded=Regex("(?i)^use (?:endpoints|midpoint label|segment)\\s+").containsMatchIn(source.trim())
        val shapeCandidates=if(pending?.kind=="endpoint_shape")RuhiEndpointResolver.selectableShapes(s,pending.source)else emptyList()
        val plainShapes=shapeCandidates.filter{it.name.equals(source.trim(),true)}
        val encodedShape=if(pending?.kind=="endpoint_shape"&&source.startsWith("Use segment ",true))source.substringAfter(' ').substringAfter(' ').trim().takeIf{it.matches(Regex("[a-f0-9]{64}"))}else null
        val shapeAnswer=if(encodedShape!=null)shapeCandidates.singleOrNull{shapeKey(it.id)==encodedShape}else plainShapes.singleOrNull()
        val attemptedShape=pending?.kind=="endpoint_shape"&&source.trim().lowercase() !in controls&&!RuhiMathLanguage.isStudioInstruction(source)
        val newRequest=answer==null&&labelAnswer==null&&shapeAnswer==null&&!encoded&&plainShapes.isEmpty()&&RuhiGeometry2DLanguage.acceptsEndpointOperation(source)
        val attemptedLabel=pending?.kind=="midpoint_label"&&source.trim().lowercase() !in controls&&!RuhiMathLanguage.isStudioInstruction(source)
        if(!newRequest&&answer==null&&labelAnswer==null&&shapeAnswer==null&&!(pending!=null&&(encoded||attemptedLabel||attemptedShape||plainShapes.isNotEmpty())))return null
        val command=if(newRequest)source else pending!!.source
        val values:MutableMap<String,Pair<String,String>> = if(newRequest)linkedMapOf()else bindings(pending).toMutableMap()
        var chosenLabel=if(newRequest)null else pending?.slots?.get("label")
        var chosenShape=if(newRequest)null else pending?.slots?.get("shape")
        fun ask(message:String,p:RuhiPending,frame:RuhiSemanticFrame?=null)=RuhiPointConversation.Outcome(RuhiExecutionResult(s,GraphCommandStatus.CLARIFICATION,message,context=c,semanticFrame=frame,dialogue=restored(s,p)),p)
        if(!newRequest) {
            if(pending!!.kind=="endpoint_shape") {
                if(shapeAnswer==null)return ask("Choose one uniquely named current line segment, or use a segment choice.",pending)
                chosenShape=shapeAnswer.id
            } else if(pending.kind=="midpoint_label") {
                if(labelAnswer==null)return ask("Give an unused point label of at most 32 letters, digits or underscores, starting with a letter or underscore.",pending)
                chosenLabel=labelAnswer.groupValues[1]
            } else {
                if(answer==null)return ask("Give two current point names separated by and.",pending)
                val resolver=RuhiEndpointResolver(s,c)
                val endpoints=runCatching{resolver.point(answer.groupValues[1]) to resolver.point(answer.groupValues[2])}.getOrElse{return ask(it.message ?: "Choose uniquely named current endpoints.",pending)}
                if(endpoints.first.index==endpoints.second.index)return ask("Choose two distinct native endpoints.",pending)
                val target=pending.targets.singleOrNull() ?: return RuhiPointConversation.Outcome(RuhiExecutionResult(s,GraphCommandStatus.CLARIFICATION,"Ask the endpoint question again for the current scene.",context=c),null)
                values[target]=endpoints.first.name to endpoints.second.name
            }
        }
        val currentShapes=RuhiEndpointResolver.selectableShapes(s,command)
        if(chosenShape!=null&&currentShapes.none{it.id==chosenShape}) {
            val next=RuhiPending("endpoint_shape",command,s.module.name,"",emptyList(),slots(values,pending?.slots?.get("intent") ?: "",chosenLabel))
            return ask("That segment is no longer a compatible current object. Choose a segment again.",next)
        }
        val selectedContext=chosenShape?.let{c.copy(selectedShapeId=it)} ?: c
        val staged=WorkspaceHistoryByModule()
        val result=RuhiGeometry2DLanguage.execute(s,command,staged,selectedContext,m,values,chosenLabel) ?: return null
        val intent=result.semanticFrame?.intent ?: return null
        if(intent !in setOf("FIND_MIDPOINT","CALCULATE_SLOPE"))return null
        val token=result.semanticFrame.parameters["endpoint_reference"] as? String
        val conflict=result.semanticFrame.parameters["midpoint_label"] as? String
        val needsShape=result.semanticFrame.parameters["segment_selection"]==true
        if(result.status==GraphCommandStatus.CLARIFICATION&&(token!=null||conflict!=null||needsShape)) {
            if(!needsShape&&conflict!=null&&Regex("(?i)\\b(selected|this|current) (?:vertical |horizontal )?(?:line|segment|graph)\\b|blue line|red line|without showing the full line").containsMatchIn(command))chosenShape=selectedContext.selectedShapeId?.takeIf{id->currentShapes.any{it.id==id}}
            val next=RuhiPending(if(needsShape)"endpoint_shape"else if(conflict!=null)"midpoint_label"else"endpoint_reference",command,s.module.name,"",listOfNotNull(conflict ?: token),slots(values,intent,chosenLabel,chosenShape))
            return ask(result.message,next,result.semanticFrame)
        }
        if(result.status==GraphCommandStatus.APPLIED) {
            val committed=if(result.state==s)s else h.forModule(s.module).execute(s,GraphWorkspaceCommand(s,GraphWorkspaceCommand.reconcileDocument(s,result.state),"Ruhi: endpoint operation"))
            return RuhiPointConversation.Outcome(result.copy(state=committed),null)
        }
        if(!newRequest)return RuhiPointConversation.Outcome(result.copy(dialogue=restored(s,pending!!)),pending)
        return null
    }
}
