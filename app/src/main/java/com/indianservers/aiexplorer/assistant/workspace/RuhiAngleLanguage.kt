package com.indianservers.aiexplorer.assistant.workspace

import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import kotlin.math.*

/** Indexed angle references are evaluated from current coordinates, never cached degrees. */
object RuhiAngleLanguage {
    const val PREFIX="ruhi.geometry2d.angle."
    data class Mark(val indices:List<Int>,val exterior:Boolean=false,val oriented:Boolean=false,val polygon:List<Int> = emptyList())
    fun marks(s:WorkspaceState)=s.labSessionValues.filterKeys{it.startsWith(PREFIX)}.values.mapNotNull { v->
        val fields=v.split('|');val ids=fields[0].split(',').mapNotNull(String::toIntOrNull)
        val polygon=fields.getOrNull(2)?.split(',')?.mapNotNull(String::toIntOrNull).orEmpty()
        if(ids.size==3&&(ids+polygon).all{it in s.points.indices})Mark(ids,fields.getOrNull(1)=="exterior",fields.getOrNull(1)=="oriented",polygon)else null
    }
    fun polygonAngles(s:WorkspaceState,ids:List<Int>):List<Double> {
        require(ids.size in 3..200&&ids.distinct().size==ids.size&&ids.all{it in s.points.indices}){"Choose a simple polygon with distinct vertices."}
        val p=ids.map{s.points[it]};require(p.all{it.x.isFinite()&&it.y.isFinite()}){"Polygon coordinates must be finite."}
        fun cross(a:Vec2,b:Vec2)=a.x*b.y-a.y*b.x
        val area=p.indices.sumOf{i->cross(p[i],p[(i+1)%p.size])};require(area.isFinite()&&abs(area)>1e-12){"A flat polygon has no unique interior."}
        fun side(a:Vec2,b:Vec2,c:Vec2)=cross(b-a,c-a)
        fun on(a:Vec2,b:Vec2,c:Vec2)=c.x in min(a.x,b.x)..max(a.x,b.x)&&c.y in min(a.y,b.y)..max(a.y,b.y)
        for(i in p.indices)for(j in i+1 until p.size)if(j!=i+1&&!(i==0&&j==p.lastIndex)) {
            val a=p[i];val b=p[(i+1)%p.size];val c=p[j];val d=p[(j+1)%p.size]
            val u=side(a,b,c);val v=side(a,b,d);val w=side(c,d,a);val z=side(c,d,b)
            val intersects=(u*v<0&&w*z<0)||(u==0.0&&on(a,b,c))||(v==0.0&&on(a,b,d))||(w==0.0&&on(c,d,a))||(z==0.0&&on(c,d,b))
            require(!intersects){"Select a polygon without crossing edges."}
        }
        return p.indices.map{i->val a=p[(i+p.size-1)%p.size];val b=p[i];val c=p[(i+1)%p.size];val minor=degrees(a,b,c);if(cross(a-b,c-b)*area>0)360-minor else minor}
    }
    fun markDegrees(s:WorkspaceState,mark:Mark):Double {
        val p=mark.indices.map{s.points[it]}
        val interior=if(mark.polygon.isEmpty())degrees(p[0],p[1],p[2])else polygonAngles(s,mark.polygon)[mark.polygon.indexOf(mark.indices[1])]
        return when{mark.exterior->180-interior;mark.oriented->orientedDegrees(p[0],p[1],p[2]);else->interior}
    }
    fun degrees(a:Vec2,b:Vec2,c:Vec2):Double {
        val u=a-b;val v=c-b;val un=hypot(u.x,u.y);val vn=hypot(v.x,v.y)
        require(un.isFinite()&&vn.isFinite()&&un>0&&vn>0){"An angle needs two nonzero arms; separate coincident points first."}
        return Math.toDegrees(atan2(abs((u.x/un)*(v.y/vn)-(u.y/un)*(v.x/vn)),(u.x/un)*(v.x/vn)+(u.y/un)*(v.y/vn)))
    }
    fun accepts(t:String)=t.length<=2000&&Regex("(?i)\\bangles?\\b|∠|\\bcorner\\b").containsMatchIn(t)
    fun orientedDegrees(a:Vec2,b:Vec2,c:Vec2):Double {
        degrees(a,b,c) // Validate nonzero, finite arms before evaluating orientation.
        val u=a-b;val v=c-b
        val un=hypot(u.x,u.y);val vn=hypot(v.x,v.y)
        val d=Math.toDegrees(atan2((u.x/un)*(v.y/vn)-(u.y/un)*(v.x/vn),(u.x/un)*(v.x/vn)+(u.y/un)*(v.y/vn)))
        return (d+360.0)%360.0
    }
    fun execute(s:WorkspaceState,t:String,h:WorkspaceHistoryByModule,c:RuhiSceneContext,m:RuhiInputModality):RuhiExecutionResult? {
        if(s.module!=MathModule.Geometry2D||!accepts(t))return null
        // Preserve the existing contextual triangle summary and rotation/construction handlers.
        if(Regex("(?i)^show (?:the |its )?angles[.?]?$|^(?:rotate|turn|create|make|construct)\\b").containsMatchIn(t.trim()))return null
        RuhiReferenceAngleLanguage.execute(s,t,h,c,m)?.let{return it}
        val text=t.trim().trimEnd('.','?');val low=text.lowercase();val resolver=RuhiEndpointResolver(s,c)
        val names=RuhiEndpointResolver.references(s)
        val label="[\\p{L}_][\\p{L}\\p{N}_]{0,63}"
        var frame=RuhiSemanticFrame("MEASURE_ANGLE",RuhiEngine.GEOMETRY_2D,emptyMap())
        try {
            fun compound(token:String):List<String> {
                val choices=names.flatMap{(a,_)->if(!token.startsWith(a,true))emptyList()else names.mapNotNull{(b,_)->val rest=token.substring(a.length);if(!rest.startsWith(b,true))null else names.singleOrNull{it.first.equals(rest.substring(b.length),true)}?.let{listOf(a,b,it.first)}}}.distinct()
                if(choices.size==1)return choices.single()
                // An explicit three-letter notation also supports asking for missing points.
                if(choices.isEmpty()&&token.length==3&&token.all{it.isLetter()})return token.map{it.toString()}
                throw RuhiReferenceQuestion("Which three points define $token? Separate their names with commas; put the vertex second.")
            }
            val explicit=Regex("(?i)(?:angle\\s+|∠\\s*)($label)\\s*,\\s*($label)\\s*,\\s*($label)").find(text)
            val notation=Regex("(?i)(?:angle\\s+(?!(?:at|between|formed|bisector|from|acute|obtuse|in)\\b)|∠\\s*)($label)").find(text)
            val triangle=Regex("(?i)triangle ($label)").find(text)?.groupValues?.get(1)
            val vertex=Regex("(?i)\\b(?:at|vertex) (?:vertex )?($label)").find(text)?.groupValues?.get(1)
            val triples:List<List<String>>
            var params:Map<String,Any>
            var polygonIds:List<Int> = emptyList()
            val rayPair=Regex("(?i)\\b(?:between|from) ($label) (?:and|to) ($label)\\b").find(text)
            if(rayPair!=null) {
                val a=resolver.segment(rayPair.groupValues[1]);val b=resolver.segment(rayPair.groupValues[2])
                if(a.first.index!=b.first.index)throw RuhiReferenceQuestion("The two rays need the same starting point. Name rays with their common vertex first.")
                triples=listOf(listOf(a.second.name,a.first.name,b.second.name))
                params=mapOf("rays" to rayPair.groupValues.drop(1))
            } else if(explicit!=null||notation!=null&&triangle==null&&vertex==null) {
                val p=explicit?.groupValues?.drop(1) ?: compound(notation!!.groupValues[1]);triples=listOf(p);params=mapOf("points" to p)
            } else {
                val shape=if(triangle!=null) {
                    val p=compound(triangle).map{resolver.point(it).index!!}
                    s.shapes.singleOrNull{it.pointIndices.size==3&&it.pointIndices.toSet()==p.toSet()} ?: throw RuhiReferenceQuestion("Create triangle $triangle first.")
                } else {
                    val polygons=s.shapes.filter{it.type in setOf(Shape2DType.Triangle,Shape2DType.Polygon,Shape2DType.Rectangle,Shape2DType.Square)&&it.pointIndices.size>=3&&it.pointIndices.all{s.points.indices.contains(it)}}
                    val selected=polygons.singleOrNull{it.id==c.selectedShapeId}
                    selected ?: vertex?.let{v->val index=resolver.point(v).index;polygons.filter{index in it.pointIndices}.singleOrNull()}
                    ?: c.selectedPointIndex?.let{index->polygons.filter{index in it.pointIndices}.singleOrNull()}
                    ?: throw RuhiReferenceQuestion("Select the polygon whose angle you want to measure.")
                }
                val polygon=shape.pointIndices.map{index->names.singleOrNull{it.second==index}?.first ?: throw RuhiReferenceQuestion("Give each polygon vertex a unique name first.")}
                polygonIds=shape.pointIndices
                val interiors=polygonAngles(s,polygonIds)
                fun corner(i:Int)=listOf(polygon[(i+polygon.size-1)%polygon.size],polygon[i],polygon[(i+1)%polygon.size])
                val all=low.contains("all angles");val select=when{low.contains("largest")->"largest";low.contains("smallest")->"smallest";else->null}
                params=if(triangle!=null)mapOf("triangle" to triangle) else mapOf("target" to if(polygon.size==4)"selected_quadrilateral" else "selected_triangle")
                if(vertex!=null) {
                    val i=polygon.indexOfFirst{it.equals(vertex,true)};if(i<0)throw RuhiReferenceQuestion("Vertex $vertex is not on this polygon.")
                    triples=listOf(corner(i));params=if(triangle!=null)mapOf("vertex" to polygon[i],"triangle" to triangle)else mapOf("vertex" to polygon[i])
                } else if(all){triples=polygon.indices.map(::corner);params=params+mapOf("all" to true)}
                else if(select!=null) {
                    val candidates=polygon.indices.map(::corner);val values=interiors
                    triples=listOf(candidates[if(select=="largest")values.indices.maxBy{values[it]}else values.indices.minBy{values[it]}]);params=params+mapOf("select" to select)
                } else if(low.contains("right angle")) {
                    val options=interiors.indices.filter{abs(interiors[it]-90)<1e-7}
                    val i=options.singleOrNull() ?: options.singleOrNull{polygonIds[it]==c.selectedPointIndex} ?: throw RuhiReferenceQuestion(if(options.isEmpty())"This triangle has no right angle." else "Select which right-angle vertex to mark.")
                    triples=listOf(corner(i));params=mapOf("target" to "selected_triangle","right_angle" to true)
                } else {
                    val i=polygonIds.indexOf(c.selectedPointIndex)
                    if(i<0)throw RuhiReferenceQuestion("Which vertex should I use? Name the vertex or select it on the canvas.",choices=polygon.map{RuhiAnswerChip("Vertex $it","Use vertex $it")})
                    triples=listOf(corner(i));params=mapOf("target" to if(low.contains("acute")||low.contains("obtuse"))"selected_angle" else "selected_vertex")
                    if(low.contains("acute")||low.contains("obtuse"))params=params+mapOf("classify" to true)
                }
            }
            val exterior=low.contains("exterior");if(exterior)params=params+mapOf("exterior" to true)
            val oriented=low.contains("counterclockwise");if(oriented)params=params+mapOf("oriented" to true)
            if(low.contains("measured angle"))params=params+mapOf("visualize" to true)
            val comparison=Regex("(?i)equals? ([0-9]+(?:\\.[0-9]+)?) degrees").find(text)?.groupValues?.get(1)?.toDouble()
            if(comparison!=null)params=params+mapOf("compare" to comparison)
            val bisector=low.contains("bisector");if(bisector)params=params+mapOf("construction" to "bisector")
            frame=frame.copy(parameters=params)
            val endpoints=triples.map{p->p.map(resolver::point)}
            val values=endpoints.map{p->val mark=Mark(p.map{it.index!!},exterior,oriented,polygonIds);markDegrees(s,mark)}
            require(values.all{it.isFinite()&&it in 0.0..360.0}){"This exterior angle requires a signed turn; request the interior angle instead."}
            if(bisector)require(values.single()>1e-9&&values.single()<180-1e-9){"Choose an angle strictly between 0 and 180 degrees for a unique internal bisector."}
            require(marks(s).size+RuhiReferenceAngleLanguage.marks(s).size+triples.size<=32){"Remove an angle annotation before adding more."}
            val metadata=endpoints.associate{p->val ids=p.map{it.index!!};PREFIX+ids.joinToString(".") to (ids.joinToString(",")+when{exterior->"|exterior";oriented->"|oriented";else->"|interior"}+if(polygonIds.isEmpty())"" else "|"+polygonIds.joinToString(","))}
            val additions=if(bisector){require(s.shapes.size<200){"The shape budget is full."};endpoints.map{p->Shape2D("ruhi-bisector-${java.util.UUID.randomUUID()}",Shape2DType.AngleBisector,p.map{it.index!!},"Angle bisector at ${p[1].name}")}}else emptyList()
            val next=s.copy(labSessionValues=s.labSessionValues+metadata,shapes=s.shapes+additions)
            val committed=h.forModule(s.module).execute(s,GraphWorkspaceCommand(s,GraphWorkspaceCommand.reconcileDocument(s,next),"Ruhi: measure angle"))
            val message=triples.zip(values).joinToString("; "){(p,v)->"Angle ${p.joinToString(", ")} = $v degrees"+(if(comparison!=null)if(abs(v-comparison)<1e-7)" (equals $comparison degrees)" else " (does not equal $comparison degrees)" else "")+(if(params["classify"]==true)" (${when{abs(v-90)<1e-7->"right";abs(v-180)<1e-7->"straight";v>180->"reflex";v>90->"obtuse";v>0->"acute";else->"zero"}})"else "")}
            return RuhiExecutionResult(committed,GraphCommandStatus.APPLIED,"$message. Marked on the canvas; follows the vertices.${if(bisector)" Constructed the internal angle bisector." else ""}",RuhiCommand(engine=RuhiEngine.GEOMETRY_2D,operation=RuhiOperation.PointQuery(frame),modality=m),c,semanticFrame=frame)
        }catch(e:RuhiReferenceQuestion){return RuhiExecutionResult(s,GraphCommandStatus.CLARIFICATION,e.message ?: "Choose three points.",context=c,dialogue=RuhiDialogueView(RuhiDialogueAction.ASK_CLARIFICATION,e.choices.ifEmpty{e.missing.map{RuhiAnswerChip("Create point $it","Create point $it at ")}}),semanticFrame=frame)}
        catch(e:IllegalArgumentException){return RuhiExecutionResult(s,GraphCommandStatus.INVALID,e.message ?: "Invalid angle.",context=c,semanticFrame=frame)}
    }
}

/** Keep the original question while asking for one existing vertex; no geometry is added by a question. */
object RuhiAngleConversation {
    private fun scene(s:WorkspaceState):String {
        val text=listOf(s.id,s.module.name,s.points.toString(),s.shapes.toString(),RuhiEndpointResolver.references(s).toString()).joinToString("|")
        return java.security.MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8)).joinToString(""){"%02x".format(it)}
    }
    fun execute(s:WorkspaceState,source:String,h:WorkspaceHistoryByModule,c:RuhiSceneContext,m:RuhiInputModality,p:RuhiPending?):RuhiPointConversation.Outcome? {
        if(p!=null&&p.kind!="angle_vertex")return null
        if(source.trim().lowercase() in setOf("cancel","cancel that"))return null
        if(p!=null) {
            if(s.module.name!=p.module||scene(s)!=p.scene.substringBefore('|'))return RuhiPointConversation.Outcome(RuhiExecutionResult(s,GraphCommandStatus.CLARIFICATION,"The scene changed. Ask the angle question again for the current vertices.",context=c),null)
            val chips=p.targets.map{RuhiAnswerChip("Vertex $it","Use vertex $it")}
            if(source.trim().lowercase() in setOf("resume","continue","resume pending"))return RuhiPointConversation.Outcome(RuhiExecutionResult(s,GraphCommandStatus.CLARIFICATION,"Which vertex should I use?",context=c,dialogue=RuhiDialogueView(RuhiDialogueAction.RESUME_PENDING,chips,true)),p)
            val answer=source.trim().removeSuffix(".").replace(Regex("(?i)^(?:use )?vertex "),"")
            val chosen=p.targets.singleOrNull{it.equals(answer,true)}
            if(chosen==null)return RuhiPointConversation.Outcome(RuhiExecutionResult(s,GraphCommandStatus.CLARIFICATION,"Choose one of the current polygon's vertices.",context=c,dialogue=RuhiDialogueView(RuhiDialogueAction.ASK_CLARIFICATION,chips,true)),p)
            val index=RuhiEndpointResolver.references(s).filter{it.first.equals(chosen,true)}.map{it.second}.distinct().singleOrNull()
                ?: return RuhiPointConversation.Outcome(RuhiExecutionResult(s,GraphCommandStatus.CLARIFICATION,"That vertex name is ambiguous. Give the points unique names and ask again.",context=c),null)
            val result=RuhiAngleLanguage.execute(s,p.source,h,c.copy(selectedShapeId=p.scene.substringAfter('|',"").takeIf{it.isNotBlank()},selectedPointIndex=index),m) ?: return null
            return RuhiPointConversation.Outcome(result,null)
        }
        if(s.module!=MathModule.Geometry2D||!RuhiAngleLanguage.accepts(source))return null
        val result=RuhiAngleLanguage.execute(s,source,h,c,m) ?: return null
        val choices=result.dialogue?.suggestions.orEmpty().filter{it.label.startsWith("Vertex ")}
        if(choices.isNotEmpty())return RuhiPointConversation.Outcome(result.copy(dialogue=result.dialogue!!.copy(pending=true)),RuhiPending("angle_vertex",source,s.module.name,scene(s)+"|"+c.selectedShapeId.orEmpty(),choices.map{it.label.removePrefix("Vertex ")}))
        // Point creation/resumption is owned by the existing point conversation handler.
        if(result.status==GraphCommandStatus.CLARIFICATION)return null
        return RuhiPointConversation.Outcome(result,null)
    }
}
