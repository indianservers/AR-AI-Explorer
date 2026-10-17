package com.indianservers.aiexplorer.assistant.workspace

import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import java.io.*
import java.util.Base64

/** Resolves existing graph identities; comparison never creates replacement plots. */
object RuhiContextualGraphLanguage {
    private const val KIND="context_graph_compare"
    private const val ANSWER="Use existing graph comparison "
    fun accepts(source:String)=runCatching{RuhiMathLanguage.normalize(source)=="compare this with the previous graph"||source.startsWith(ANSWER)}.getOrDefault(false)
    private fun scene(s:WorkspaceState)=Base64.getEncoder().encodeToString(java.security.MessageDigest.getInstance("SHA-256").digest((s.id+s.module+s.functions).toByteArray(Charsets.UTF_8)))
    private fun encode(ids:List<String>):String {val out=ByteArrayOutputStream();DataOutputStream(out).use{stream->ids.forEach(stream::writeUTF)};return Base64.getEncoder().encodeToString(out.toByteArray())}
    private fun decode(encoded:String)=DataInputStream(ByteArrayInputStream(Base64.getDecoder().decode(encoded))).use{val ids=listOf(it.readUTF(),it.readUTF());require(it.available()==0);ids}
    private fun chips(s:WorkspaceState,p:RuhiPending)=p.targets.take(5).mapNotNull{encoded->runCatching{val pair=decode(encoded).map{id->s.functions.single{it.id==id}};RuhiAnswerChip(pair.joinToString(" and "){"${it.name} (${s.functions.indexOf(it)+1}): ${it.expression}"},ANSWER+encoded)}.getOrNull()}
    fun restored(s:WorkspaceState,p:RuhiPending)=RuhiDialogueView(RuhiDialogueAction.ASK_CLARIFICATION,chips(s,p),true)
    fun execute(s:WorkspaceState,source:String,c:RuhiSceneContext,m:RuhiInputModality,pendingInput:RuhiPending?):RuhiPointConversation.Outcome? {
        if(s.module!=MathModule.Graph2D)return null
        val pending=pendingInput?.takeIf{it.kind==KIND}
        val reply=pending!=null&&source.startsWith(ANSWER)
        val resume=pending!=null&&RuhiMathLanguage.normalize(source) in setOf("resume","continue")
        if(!accepts(source)&&!resume)return null
        var frame=RuhiSemanticFrame("DIALOGUE_CONTEXT",RuhiEngine.GRAPH_2D,mapOf("targets" to listOf("selected_graph","previous_graph")))
        fun result(status:GraphCommandStatus,message:String,p:RuhiPending?=null)=RuhiPointConversation.Outcome(RuhiExecutionResult(s,status,message,RuhiCommand(engine=RuhiEngine.GRAPH_2D,operation=RuhiOperation.PointQuery(frame),modality=m),c,semanticFrame=frame,dialogue=p?.let{restored(s,it)}),p)
        try {
            if((reply||resume)&&pending!!.scene!=scene(s))return result(GraphCommandStatus.CLARIFICATION,"The graph scene changed. Ask the comparison again using the current graphs.")
            if(resume)return result(GraphCommandStatus.CLARIFICATION,"Which existing graphs should I compare?",pending)
            var ids:List<String>?=null
            if(reply) {
                val encoded=source.removePrefix(ANSWER);require(encoded in pending!!.targets){"Choose a displayed existing graph pair."};ids=decode(encoded)
            } else {
                val memory=RuhiDialogueMemory.read(s)
                val selected=c.selectedFunctionIds.singleOrNull() ?: s.labSessionValues[GraphAssistant.selectionKey(s.module)]
                val timeline=memory.graphTimeline.filter{id->s.functions.any{it.id==id}}
                val index=timeline.indexOf(selected)
                if(index>0)ids=listOf(requireNotNull(selected),timeline[index-1])
                if(ids==null) {
                    val current=s.functions.singleOrNull{it.id==selected}
                    val candidates=if(current!=null)s.functions.filter{it.id!=current.id}.map{listOf(current.id,it.id)}else s.functions.flatMapIndexed{i,a->s.functions.drop(i+1).map{b->listOf(a.id,b.id)}}
                    if(candidates.isEmpty())return result(GraphCommandStatus.CLARIFICATION,"Choose two existing graphs to compare; there is no previous graph available.")
                    val p=RuhiPending(KIND,source,s.module.name,scene(s),candidates.take(5).map(::encode))
                    return result(GraphCommandStatus.CLARIFICATION,if(current==null)"Which existing graph pair should I compare?"else"Which existing graph is the previous one?",p)
                }
            }
            val chosenIds=requireNotNull(ids)
            require(chosenIds.size==2&&chosenIds.distinct().size==2){"Choose two distinct graphs."}
            val graphs=chosenIds.map{id->s.functions.single{it.id==id}}
            val engine=TypedGraphEngine()
            val values=graphs.map{graph->engine.evaluate(TypedGraphExpressionParser.parse(graph.expression),0.0)?.y}
            frame=frame.copy(parameters=frame.parameters+mapOf("resolved_graph_ids" to chosenIds,"expressions" to graphs.map{it.expression},"sample_x" to 0.0))
            val descriptions=graphs.mapIndexed{i,graph->"${graph.name}: ${graph.expression}; at x = 0, "+(values[i]?.takeIf{it.isFinite()}?.let{"y = $it"} ?: "no finite y value is defined")}
            val difference=if(values.all{it!=null&&it.isFinite()})"; first minus second at x = 0 is ${values[0]!!-values[1]!!}"else""
            return result(GraphCommandStatus.APPLIED,descriptions.joinToString(". ")+difference+". Compared the existing graphs using their current formulas.")
        }catch(error:RuntimeException){return result(GraphCommandStatus.INVALID,error.message ?: "Invalid existing graph comparison.",pending)}
    }
}
