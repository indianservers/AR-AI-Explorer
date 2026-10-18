package com.indianservers.aiexplorer.assistant.workspace

import com.indianservers.aiexplorer.workspace.*

/** Entity names are matched verbatim, independently of learned intent features. */
object RuhiSpatialLinearLanguage {
    private val label="[\\p{L}_][\\p{L}\\p{M}\\p{N}_]{0,63}"
    private val patterns=listOf(
        Linear3DKind.Line to Regex("(?i)^(?:draw|create|make|construct) (?:a |the )?(?:3d )?line (?:from|through) ($label) (?:to|and) ($label)$"),
        Linear3DKind.Segment to Regex("(?i)^join ($label) and ($label) with (?:a |the )?(?:line )?segment$"),
        Linear3DKind.Segment to Regex("(?i)^(?:draw|create|make|construct) (?:a |the )?(?:3d )?(?:line )?segment (?:from|between) ($label) (?:to|and) ($label)$"),
        Linear3DKind.Ray to Regex("(?i)^(?:draw|create|make|construct) (?:a |the )?(?:3d )?ray (?:starting at|from) ($label) (?:and going through|through) ($label)$"),
    )
    fun execute(s:WorkspaceState,source:String,h:WorkspaceHistoryByModule,c:RuhiSceneContext,m:RuhiInputModality,p:RuhiPending?):RuhiExecutionResult? {
        if(s.module!=MathModule.Geometry3D || p!=null || source.length>2000)return null
        val text=source.trim().trimEnd('.','?')
        val (kind,match)=patterns.firstNotNullOfOrNull{(k,r)->r.matchEntire(text)?.let{k to it}} ?: return null
        val names=match.groupValues.drop(1)
        val intent=when(kind){Linear3DKind.Line->"CREATE_LINE";Linear3DKind.Segment->"CREATE_SEGMENT";Linear3DKind.Ray->"CREATE_RAY"}
        val frame=RuhiSemanticFrame(intent,RuhiEngine.GEOMETRY_3D,mapOf("points" to names,"domain" to kind.name))
        fun result(state:WorkspaceState=s,status:GraphCommandStatus,message:String)=RuhiExecutionResult(state,status,message,
            RuhiCommand(engine=RuhiEngine.GEOMETRY_3D,operation=RuhiOperation.PointQuery(frame),modality=m),c,semanticFrame=frame)
        val points=names.map{name->s.points3D.singleOrNull{it.name.equals(name,true)}
            ?: return result(status=GraphCommandStatus.CLARIFICATION,message="Point $name is missing or ambiguous. Create it with 3D coordinates or use a unique point name.")}
        return try {
            require(s.linears3D.size<200){"Remove a line, segment or ray before adding more."}
            val name=generateSequence(1){it+1}.map{"${kind.name}$it"}.first{n->s.linears3D.none{it.name.equals(n,true)}}
            val obj=Linear3DObject("ruhi-linear-${java.util.UUID.randomUUID()}",name,kind,points.map{it.id})
            NativeLinearGeometry.evaluate(s,obj)
            val next=s.copy(linears3D=s.linears3D+obj)
            val committed=h.forModule(s.module).execute(s,GraphWorkspaceCommand(s,GraphWorkspaceCommand.reconcileDocument(s,next),"Ruhi: $name"))
            val domain=when(kind){Linear3DKind.Line->"extends in both directions";Linear3DKind.Segment->"ends at both points";Linear3DKind.Ray->"starts at ${names[0]} and extends through ${names[1]}"}
            result(committed,GraphCommandStatus.APPLIED,"Created ${kind.name.lowercase()} $name through ${names.joinToString(" and ")}; $domain. It follows its defining points.")
        }catch(e:IllegalArgumentException){result(status=GraphCommandStatus.INVALID,message=e.message ?: "Invalid 3D construction.")}
    }
}
