package com.indianservers.aiexplorer.assistant.workspace

import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import kotlin.math.*

internal class RuhiReferenceQuestion(message:String,val missing:List<String> = emptyList(),val choices:List<RuhiAnswerChip> = emptyList(),val endpointToken:String?=null,val midpointLabel:String?=null,val segmentSelection:Boolean=false):IllegalStateException(message)
internal data class RuhiEndpoint(val name:String,val position:Vec2,val index:Int?)
internal data class RuhiEndpointPair(val first:RuhiEndpoint,val second:RuhiEndpoint,val parameters:Map<String,Any>)

/** Resolve references from the current scene. No A/B/C/D vocabulary is built into this resolver. */
internal class RuhiEndpointResolver(private val s:WorkspaceState,private val c:RuhiSceneContext,private val bindings:Map<String,Pair<String,String>> = emptyMap()) {
    companion object {
        fun selectableShapes(s:WorkspaceState,source:String):List<Shape2D> {
            val lower=source.lowercase()
            val color=when { lower.contains("blue line")->"blue";lower.contains("red line")->"red";else->null }
            return s.shapes.filter{shape->
                shape.type in setOf(Shape2DType.Segment,Shape2DType.Line,Shape2DType.Ray)&&
                    shape.id.length in 1..2000&&s.shapes.count{it.id==shape.id}==1&&
                    shape.pointIndices.size==2&&shape.pointIndices.distinct().size==2&&
                    shape.pointIndices.all{i->i in s.points.indices&&listOf(s.points[i].x,s.points[i].y).all{it.isFinite()&&abs(it)<=10000}}&&
                    (color==null||shape.styleKey.equals(color,true)||shape.styleKey.equals(if(color=="blue")"#0000FF" else "#FF0000",true))
            }
        }
        fun references(s:WorkspaceState):List<Pair<String,Int>> {
            val named=s.labSessionValues.entries.filter{it.key.startsWith(StudioReferenceMetadata.POINT_PREFIX)}.mapNotNull{e->e.value.toIntOrNull()?.takeIf{it in s.points.indices}?.let{e.key.removePrefix(StudioReferenceMetadata.POINT_PREFIX) to it}}
            return named+s.points.indices.filter{i->named.none{it.second==i}}.map{"P${it+1}" to it}
        }
    }
    private val label="[\\p{L}_][\\p{L}\\p{N}_]{0,63}"
    private val named=references(s)
    fun point(name:String):RuhiEndpoint {
        val candidates=named.filter{it.first.equals(name,true)}.distinctBy{it.second}
        if(candidates.size!=1)throw RuhiReferenceQuestion(if(candidates.isEmpty())"Point $name is missing. Create it with coordinates first." else "Point $name is ambiguous. Give the conflicting points unique names.",if(candidates.isEmpty())listOf(name) else emptyList())
        val (actual,i)=candidates.single();return RuhiEndpoint(actual,s.points[i],i)
    }
    private fun endpoint(i:Int)=RuhiEndpoint(named.firstOrNull{it.second==i}?.first ?: "P${i+1}",s.points[i],i)
    private fun fromShape(shape:Shape2D,target:String):RuhiEndpointPair {
        if(shape.pointIndices.size!=2||shape.pointIndices.any{it !in s.points.indices})throw RuhiReferenceQuestion("Choose a line segment with two valid endpoints.")
        return RuhiEndpointPair(endpoint(shape.pointIndices[0]),endpoint(shape.pointIndices[1]),mapOf("target" to target))
    }
    private fun supplied(names:Pair<String,String>):RuhiEndpointPair {
        val a=point(names.first);val b=point(names.second)
        require(a.index!=b.index){"Choose two distinct native endpoints."}
        return RuhiEndpointPair(a,b,mapOf("a" to a.name,"b" to b.name))
    }
    fun segment(token:String):RuhiEndpointPair {
        bindings.entries.singleOrNull{it.key.equals(token,true)}?.value?.let{return supplied(it)}

        val shape=s.shapes.filter{it.pointIndices.size==2&&it.name.equals(token,true)}.singleOrNull()
        if(shape!=null){val p=fromShape(shape,"named_segment");return p.copy(parameters=mapOf("a" to p.first.name,"b" to p.second.name))}
        val pairs=named.filter{token.startsWith(it.first,true)}.mapNotNull{(name,index)->
            val rest=token.substring(name.length)
            named.filter{it.first.equals(rest,true)}.singleOrNull()?.let{(other,j)->RuhiEndpointPair(RuhiEndpoint(name,s.points[index],index),RuhiEndpoint(other,s.points[j],j),mapOf("a" to name,"b" to other))}
        }.distinctBy{it.first.index to it.second.index}
        if(pairs.size!=1)throw RuhiReferenceQuestion("Which two endpoints define $token? Write their names separated by ‘and’.",endpointToken=token)
        return pairs.single()
    }
    fun resolve(source:String):RuhiEndpointPair {
        bindings["@pair"]?.let{return supplied(it)}
        val t=source.trim().trimEnd('.','?');val lower=t.lowercase()
        val tuples=Regex("\\(([^()]*)\\)").findAll(t).toList()
        if(tuples.isNotEmpty()) {
            require(tuples.size==2){"Provide two coordinate pairs."}
            val p=tuples.map{m->val values=m.groupValues[1].split(',').map{it.trim().toDoubleOrNull() ?: throw IllegalArgumentException("Use numeric coordinates.")};require(values.size==2&&values.all{it.isFinite()&&abs(it)<=10000}){"Provide two finite 2D coordinates within 10000 units."};Vec2(values[0],values[1])}
            return RuhiEndpointPair(RuhiEndpoint("given first point",p[0],null),RuhiEndpoint("given second point",p[1],null),mapOf("aCoords" to listOf(p[0].x,p[0].y),"bCoords" to listOf(p[1].x,p[1].y)))
        }
        val lines=selectableShapes(s,source)
        if(lower.contains("blue line")||lower.contains("red line")) {
            val color=if(lower.contains("blue"))"blue" else "red";val hex=if(color=="blue")"#0000FF" else "#FF0000"
            val choices=lines.filter{it.styleKey.equals(color,true)||it.styleKey.equals(hex,true)}
            val chosen=choices.singleOrNull() ?: choices.singleOrNull{it.id==c.selectedShapeId} ?: throw RuhiReferenceQuestion("Choose one $color segment first.",segmentSelection=true)
            return fromShape(chosen,if(color=="blue")"blue_segment" else "red_line")
        }
        if(Regex("\\b(last|newest) (?:line|segment)\\b|line i just drew").containsMatchIn(lower))return fromShape(lines.lastOrNull() ?: throw RuhiReferenceQuestion("Draw a segment first."),if(lower.contains("last line"))"last_line" else "last_segment")
        if(Regex("\\b(selected|this|current) (?:vertical |horizontal )?(?:line|segment|graph)\\b|without showing the full line").containsMatchIn(lower)) {
            val selected=lines.singleOrNull{it.id==c.selectedShapeId} ?: throw RuhiReferenceQuestion("Select a line segment first.",segmentSelection=true)
            return fromShape(selected,if(lower.contains("segment")||lower.contains("without showing"))"selected_segment" else "selected_line")
        }
        Regex("(?i)\\b(?:between|from|connecting) (?:points? )?($label) (?:and|to) (?:point )?($label)\\b").find(t)?.let{m->val a=point(m.groupValues[1]);val b=point(m.groupValues[2]);return RuhiEndpointPair(a,b,mapOf("a" to a.name,"b" to b.name))}
        Regex("(?i)\\b(?:of|on|along|for|segment) (?:line )?(?:segment )?($label)\\b").findAll(t).toList().asReversed().forEach{m->val token=m.groupValues[1];if(token.lowercase() !in setOf("the","a","this","selected","slope","midpoint","line","segment"))return segment(token)}
        Regex("(?i)^(?:split|is) ($label)\\b").find(t)?.let{return segment(it.groupValues[1])}
        Regex("(?i)\\bif ($label) (?:is|has)\\b").find(t)?.let{return segment(it.groupValues[1])}
        val cue=runCatching{RuhiLanguageServices.semanticClassifier?.predict(RuhiSeedNlpInput.normalize(source))}.getOrNull()
        if(cue?.intent in setOf("FIND_MIDPOINT","CALCULATE_SLOPE")&&cue!!.confidence.isFinite()&&cue.margin.isFinite()&&cue.confidence>=.9f&&cue.margin>=.4f) {
            val slots=runCatching{RuhiLanguageServices.pointSlotClassifier?.predict(source)}.getOrNull().orEmpty().filter{it.confidence.isFinite()&&it.margin.isFinite()&&it.confidence>=.9f&&it.margin>=.4f&&it.start>=0&&it.end<=source.length&&it.start<it.end&&source.substring(it.start,it.end)==it.value}
            val a=slots.singleOrNull{it.role=="POINT_1"};val b=slots.singleOrNull{it.role=="POINT_2"}
            if(a!=null&&b!=null){val first=point(a.value);val second=point(b.value);return RuhiEndpointPair(first,second,mapOf("a" to first.name,"b" to second.name))}
            slots.singleOrNull{it.role=="SEGMENT"}?.let{return segment(it.value)}
        }
        throw RuhiReferenceQuestion("Which two points or selected segment should I use?",endpointToken="@pair")
    }
}

object RuhiGeometry2DLanguage {
    private val midpoint=Regex("(?i)\\b(?:midpoint|halfway|bisect|bisecting|middle)\\b|center point between|marker at the center of|divide equally|^split .+ equal segments")
    fun acceptsEndpointOperation(source:String):Boolean {
        val cues=RuhiPointLanguage.maskEndpoints(source)
        return source.length<=2000&&(midpoint.containsMatchIn(cues)||RuhiSlopeLanguage.accepts(cues))
    }
    fun accepts(source:String):Boolean {
        val cues=RuhiPointLanguage.maskEndpoints(source)
        return source.length<=2000&&(midpoint.containsMatchIn(cues)||RuhiSlopeLanguage.accepts(cues)||RuhiAngleLanguage.accepts(cues)||RuhiCircleLanguage.accepts(cues)||RuhiTriangleLanguage.accepts(cues))
    }
    @JvmOverloads
    fun execute(s:WorkspaceState,source:String,h:WorkspaceHistoryByModule,c:RuhiSceneContext,modality:RuhiInputModality,endpointBindings:Map<String,Pair<String,String>> = emptyMap(),midpointLabel:String?=null):RuhiExecutionResult? {
        if(s.module!=MathModule.Geometry2D)return null
        val cues=RuhiPointLanguage.maskEndpoints(source)
        if(RuhiCircleLanguage.accepts(cues))RuhiCircleLanguage.execute(s,source,h,c,modality)?.let{return it}
        if(RuhiTriangleLanguage.accepts(cues))RuhiTriangleLanguage.execute(s,source,h,c,modality)?.let{return it}
        if(RuhiAngleLanguage.accepts(cues))RuhiAngleLanguage.execute(s,source,h,c,modality)?.let{return it}
        if(!midpoint.containsMatchIn(cues))return if(RuhiSlopeLanguage.accepts(cues))RuhiSlopeLanguage.execute(s,source,h,c,modality,endpointBindings)else null
        var frame=RuhiSemanticFrame("FIND_MIDPOINT",RuhiEngine.GEOMETRY_2D,emptyMap())
        val cue=runCatching{RuhiLanguageServices.semanticClassifier?.predict(RuhiSeedNlpInput.normalize(source))}.getOrNull()
        try {
            val pair=RuhiEndpointResolver(s,c,endpointBindings).resolve(source)
            val custom=midpointLabel ?: Regex("(?i)\\bas ([\\p{L}_][\\p{L}\\p{N}_]{0,31})[.?]?$" ).find(source.trim())?.groupValues?.get(1)
            val labels=s.labSessionValues.keys.filter{it.startsWith("ruhi.studio.point.")}.map{it.removePrefix("ruhi.studio.point.")}
            require(custom==null||custom.matches(Regex("[\\p{L}_][\\p{L}\\p{N}_]{0,31}"))){"Use a point label of at most 32 letters, digits or underscores, starting with a letter or underscore."}
            val used=labels+RuhiEndpointResolver.references(s).map{it.first}
            if(custom!=null&&used.any{it.equals(custom,true)})throw RuhiReferenceQuestion("Point $custom already exists. Choose an unused midpoint label.",midpointLabel=custom)
            val name=custom ?: generateSequence(1){it+1}.map{"Mid$it"}.first{candidate->labels.none{it.equals(candidate,true)}}
            val p=(pair.first.position+pair.second.position)*.5
            require(s.points.size<200&&listOf(p.x,p.y).all{it.isFinite()&&abs(it)<=10000}){"The midpoint is outside the point budget."}
            val split=source.trim().startsWith("split ",true)
            frame=frame.copy(parameters=pair.parameters+custom?.let{mapOf("label" to it)}.orEmpty()+(if(source.contains("without showing",true))mapOf("show_segment" to false) else emptyMap())+(if(split)mapOf("split" to true) else emptyMap()))
            val index=s.points.size
            val dependency=if(pair.first.index!=null&&pair.second.index!=null)listOf(PointDependency(index,listOf(pair.first.index,pair.second.index),PointDependencyType.Midpoint,name)) else emptyList()
            val pieces=if(split) {
                require(pair.first.index!=null&&pair.second.index!=null)
                listOf(Shape2D("ruhi-segment-${java.util.UUID.randomUUID()}",Shape2DType.Segment,listOf(pair.first.index,index),"${pair.first.name}$name"),Shape2D("ruhi-segment-${java.util.UUID.randomUUID()}",Shape2DType.Segment,listOf(index,pair.second.index),"$name${pair.second.name}"))
            } else emptyList()
            val next=s.copy(points=s.points+p,pointDependencies=s.pointDependencies+dependency,shapes=s.shapes+pieces,labSessionValues=s.labSessionValues+("ruhi.studio.point.$name" to index.toString())).recomputed()
            val committed=h.forModule(s.module).execute(s,GraphWorkspaceCommand(s,GraphWorkspaceCommand.reconcileDocument(s,next),"Ruhi: midpoint $name"))
            return RuhiExecutionResult(committed,GraphCommandStatus.APPLIED,"Midpoint $name = (${p.x}, ${p.y}). Marked on the canvas${if(dependency.isNotEmpty())"; it follows both endpoints" else ""}${if(split)" and divided the segment into two equal parts" else ""}.",RuhiCommand(engine=RuhiEngine.GEOMETRY_2D,operation=RuhiOperation.PointQuery(frame),modality=modality,intentProposal=cue),c,RuhiDialogueView(RuhiDialogueAction.EXECUTE,languageProposal=cue),frame)
        } catch(e:RuhiReferenceQuestion) {
            val chips=e.missing.distinct().take(2).map{RuhiAnswerChip("Create point $it","Create point $it at ")}
            return RuhiExecutionResult(s,GraphCommandStatus.CLARIFICATION,e.message ?: "Choose the endpoints.",context=c,dialogue=RuhiDialogueView(RuhiDialogueAction.ASK_CLARIFICATION,chips,languageProposal=cue),semanticFrame=when{e.segmentSelection->frame.copy(parameters=frame.parameters+("segment_selection" to true));e.midpointLabel!=null->frame.copy(parameters=frame.parameters+("midpoint_label" to e.midpointLabel));e.endpointToken!=null->frame.copy(parameters=frame.parameters+("endpoint_reference" to e.endpointToken));else->frame})
        } catch(e:IllegalArgumentException){return RuhiExecutionResult(s,GraphCommandStatus.INVALID,e.message ?: "Invalid midpoint input.",context=c,semanticFrame=frame)}
    }
}
