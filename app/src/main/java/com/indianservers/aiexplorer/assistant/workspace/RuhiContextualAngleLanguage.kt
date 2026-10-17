package com.indianservers.aiexplorer.assistant.workspace

import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import java.util.Base64
import kotlin.math.*

/** Sets an angle through the same native point-edit boundary used by touch editing. */
object RuhiContextualAngleLanguage {
    private const val KIND="context_angle"
    private const val ANSWER="Use contextual angle "
    private val request=Regex("^(?:can we )?(?:make|set) (?:the |this |selected )?angle ([-+]?[0-9]+(?:\\.[0-9]+)?) degrees$")
    fun accepts(source:String)=runCatching{request.matches(RuhiMathLanguage.normalize(source))||source.startsWith(ANSWER)}.getOrDefault(false)
    private fun token(mark:RuhiAngleLanguage.Mark)=listOf(mark.indices.joinToString(","),mark.exterior.toString(),mark.oriented.toString(),mark.polygon.joinToString(",")).joinToString(";")
    private fun scene(s:WorkspaceState)=Base64.getEncoder().encodeToString(java.security.MessageDigest.getInstance("SHA-256").digest((s.id+s.module+s.points+s.shapes+s.pointDependencies+s.geometryConstraints+s.geometryGroups).toByteArray(Charsets.UTF_8)))
    fun restored(s:WorkspaceState,p:RuhiPending):RuhiDialogueView {
        val labels=RuhiEndpointResolver.references(s).associate{it.second to it.first}
        val marks=RuhiAngleLanguage.marks(s)
        val chips=p.targets.take(5).mapNotNull{t->marks.firstOrNull{token(it)==t}?.let{mark->
            RuhiAnswerChip("Angle "+mark.indices.joinToString(", "){labels[it] ?: "P${it+1}"},ANSWER+Base64.getEncoder().encodeToString(t.toByteArray(Charsets.UTF_8)))
        }}
        return RuhiDialogueView(RuhiDialogueAction.ASK_CLARIFICATION,chips,true)
    }
    fun execute(s:WorkspaceState,source:String,h:WorkspaceHistoryByModule,c:RuhiSceneContext,m:RuhiInputModality,pendingInput:RuhiPending?):RuhiPointConversation.Outcome? {
        if(s.module!=MathModule.Geometry2D)return null
        val pending=pendingInput?.takeIf{it.kind==KIND}
        val reply=pending!=null&&source.startsWith(ANSWER)
        val resume=pending!=null&&RuhiMathLanguage.normalize(source) in setOf("resume","continue")
        val parsed=request.matchEntire(RuhiMathLanguage.normalize(if(reply||resume)pending!!.source else source))
        if(parsed==null)return null
        val degrees=parsed.groupValues[1].toDouble()
        val frame=RuhiSemanticFrame("DIALOGUE_CONTEXT",RuhiEngine.GEOMETRY_2D,mapOf("target" to "selected_angle","angle_degrees" to degrees))
        fun result(state:WorkspaceState,status:GraphCommandStatus,message:String,p:RuhiPending?=null)=RuhiPointConversation.Outcome(RuhiExecutionResult(state,status,message,RuhiCommand(engine=RuhiEngine.GEOMETRY_2D,operation=RuhiOperation.PointQuery(frame),modality=m),c,semanticFrame=frame,dialogue=p?.let{restored(state,it)}),p)
        try {
            require(degrees.isFinite()&&degrees>0&&degrees<360){"Use a finite angle greater than 0 and less than 360 degrees."}
            if((reply||resume)&&pending!!.scene!=scene(s))return result(s,GraphCommandStatus.CLARIFICATION,"The angle construction changed. State the requested angle again.")
            if(resume)return result(s,GraphCommandStatus.CLARIFICATION,"Which angle should I change?",pending)
            val marks=RuhiAngleLanguage.marks(s).distinct()
            val selectedShape=s.shapes.firstOrNull{it.id==c.selectedShapeId}
            val shapeCandidates=marks.filter{mark->selectedShape==null||mark.indices.all{it in selectedShape.pointIndices}}
            val vertexCandidates=shapeCandidates.filter{it.indices[1]==c.selectedPointIndex}
            val candidates=vertexCandidates.ifEmpty{shapeCandidates}
            val picked=if(reply)String(Base64.getDecoder().decode(source.removePrefix(ANSWER)),Charsets.UTF_8).also{require(it in pending!!.targets){"Choose a displayed current angle."}}else null
            val mark=if(picked!=null)marks.singleOrNull{token(it)==picked}else candidates.singleOrNull()
            if(mark==null) {
                if(candidates.isEmpty())return result(s,GraphCommandStatus.CLARIFICATION,"Identify the angle's two arms and vertex first.")
                val p=RuhiPending(KIND,source,s.module.name,scene(s),candidates.map(::token))
                return result(s,GraphCommandStatus.CLARIFICATION,"Which angle should I change?",p)
            }
            val ids=mark.indices;val a=s.points[ids[0]];val b=s.points[ids[1]];val d=s.points[ids[2]]
            val u=a-b;val v=d-b;val length=hypot(v.x,v.y);val firstLength=hypot(u.x,u.y)
            require(length.isFinite()&&firstLength.isFinite()&&length>0&&firstLength>0){"Separate coincident arm points first."}
            val interior=if(mark.exterior)180-degrees else degrees
            if(!mark.oriented)require(interior>0&&interior<180){"This angle needs an interior angle between 0 and 180 degrees."}
            val turn=if(mark.oriented)degrees else interior*if(u.x*v.y-u.y*v.x<0)-1 else 1
            val radians=Math.toRadians(turn);val unit=u*(1/firstLength)
            val point=b+Vec2(unit.x*cos(radians)-unit.y*sin(radians),unit.x*sin(radians)+unit.y*cos(radians))*length
            require(point.x.isFinite()&&point.y.isFinite()&&abs(point.x)<=10000&&abs(point.y)<=10000){"Keep the angle points within 10000 units."}
            val lockedShapes=s.shapes.filter{it.locked||s.geometryGroups.any{group->group.locked&&it.id in group.shapeIds}}
            require(lockedShapes.none{ids[2] in it.pointIndices}){"Unlock the shape containing the moving arm first."}
            val controller=Unified2DMathController()
            val edited=controller.editCoordinates(controller.snapshot(s),"point-${ids[2]}",point)
            val next=when(edited){is Unified2DMutation.Applied->edited.snapshot.state;is Unified2DMutation.Rejected->return result(s,GraphCommandStatus.CLARIFICATION,edited.message)}
            require(abs(RuhiAngleLanguage.markDegrees(next,mark)-degrees)<1e-6){"Existing construction constraints conflict with that angle."}
            val committed=h.forModule(s.module).execute(s,GraphWorkspaceCommand(s,GraphWorkspaceCommand.reconcileDocument(s,next),"Ruhi: set native angle"))
            return result(committed,GraphCommandStatus.APPLIED,"Set the native angle to $degrees degrees by moving its arm point; the vertex and other arm are unchanged.")
        }catch(error:RuntimeException){return result(s,GraphCommandStatus.INVALID,error.message ?: "Invalid angle edit.",pending)}
    }
}
