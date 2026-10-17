package com.indianservers.aiexplorer.assistant.workspace

import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import kotlin.math.*

/** Virtual reference arms are evaluated for drawing; they never create helper points in the scene. */
object RuhiReferenceAngleLanguage {
    const val PREFIX="ruhi.geometry2d.referenceAngle."
    data class Mark(val kind:String,val indices:List<Int>)
    data class Measurement(val arms:List<Vec2>,val degrees:Double)
    fun marks(s:WorkspaceState)=s.labSessionValues.filterKeys{it.startsWith(PREFIX)}.values.mapNotNull{v->
        val p=v.split('|');val ids=p.getOrNull(1)?.split(',')?.mapNotNull(String::toIntOrNull).orEmpty()
        val size=if(p.firstOrNull() in setOf("origin","axis_x","axis_y"))2 else 4
        if(p.size==2&&p[0] in setOf("origin","axis_x","axis_y","lines","segments","vectors")&&ids.size==size&&ids.all{it in s.points.indices})Mark(p[0],ids)else null
    }
    fun measure(s:WorkspaceState,mark:Mark):Measurement {
        val p=mark.indices.map{s.points[it]}
        require(p.all{it.x.isFinite()&&it.y.isFinite()}){"Use finite coordinates."}
        fun unit(v:Vec2):Vec2 {val length=hypot(v.x,v.y);require(length.isFinite()&&length>0){"An angle needs nonzero directions."};return v*(1/length)}
        val arms=when(mark.kind) {
            "origin"->listOf(p[0],Vec2(0.0,0.0),p[1])
            "axis_x","axis_y"->{val length=hypot(p[1].x-p[0].x,p[1].y-p[0].y);listOf(p[0]+if(mark.kind=="axis_x")Vec2(length,0.0)else Vec2(0.0,length),p[0],p[1])}
            "vectors"->{val u=unit(p[1]-p[0]);val v=unit(p[3]-p[2]);listOf(p[0]+u,p[0],p[0]+v)}
            else->{
                val common=if(mark.kind=="segments")mark.indices.take(2).toSet().intersect(mark.indices.drop(2).toSet()).singleOrNull()else null
                if(common!=null) {
                    val a=mark.indices.take(2).singleOrNull{it!=common} ?: throw IllegalArgumentException("A segment has zero length.")
                    val b=mark.indices.drop(2).singleOrNull{it!=common} ?: throw IllegalArgumentException("A segment has zero length.")
                    listOf(s.points[a],s.points[common],s.points[b])
                }else {
                    val u=unit(p[1]-p[0]);val v=unit(p[3]-p[2]);val delta=p[2]-p[0]
                    val cross=u.x*v.y-u.y*v.x;require(abs(cross)>1e-12){"Parallel or coincident lines have no unique intersection to mark."}
                    val t=(delta.x*v.y-delta.y*v.x)/cross;val q=(delta.x*u.y-delta.y*u.x)/cross
                    if(mark.kind=="segments")require(t in 0.0..hypot(p[1].x-p[0].x,p[1].y-p[0].y)&&q in 0.0..hypot(p[3].x-p[2].x,p[3].y-p[2].y)){"These segments do not intersect. Choose segments with a common vertex or intersection."}
                    val center=p[0]+u*t;require(center.x.isFinite()&&center.y.isFinite()&&abs(center.x)<=10000&&abs(center.y)<=10000){"The intersection is outside the supported range."}
                    val second=if(u.x*v.x+u.y*v.y<0)v*(-1.0)else v
                    listOf(center+u,center,center+second)
                }
            }
        }
        return Measurement(arms,RuhiAngleLanguage.degrees(arms[0],arms[1],arms[2]))
    }
    fun execute(s:WorkspaceState,source:String,h:WorkspaceHistoryByModule,c:RuhiSceneContext,m:RuhiInputModality):RuhiExecutionResult? {
        if(s.module!=MathModule.Geometry2D)return null
        val text=source.trim().trimEnd('.','?');val label="[\\p{L}_][\\p{L}\\p{N}_]{0,63}"
        val origin=Regex("(?i)angle at (?:the )?origin between ($label) and ($label)$").find(text)
        val axis=Regex("(?i)angle between (?:the )?([xy])-axis and ($label)$").find(text)
        val lines=Regex("(?i)angle between (?:intersecting )?lines ($label) and ($label)$").find(text)
        val vectors=Regex("(?i)angle (?:formed by|between) vectors ($label) and ($label)$").find(text)
        val last=Regex("(?i)angle between the last two segments$").containsMatchIn(text)
        if(origin==null&&axis==null&&lines==null&&vectors==null&&!last)return null
        val resolver=RuhiEndpointResolver(s,c);var frame=RuhiSemanticFrame("MEASURE_ANGLE",RuhiEngine.GEOMETRY_2D,emptyMap())
        try {
            val mark:Mark;val params:Map<String,Any>;val title:String
            fun segment(name:String)=resolver.segment(name).let{listOf(it.first.index!!,it.second.index!!)}
            when {
                origin!=null->{val names=origin.groupValues.drop(1);mark=Mark("origin",names.map{resolver.point(it).index!!});params=mapOf("vertex" to "O","points" to names);title="at the origin between ${names.joinToString(" and ")}"}
                axis!=null->{val name=axis.groupValues[2];val axisName=axis.groupValues[1].lowercase();mark=Mark("axis_$axisName",segment(name));params=mapOf("line" to name,"reference" to "${axisName}_axis");title="from the positive ${axisName.uppercase()}-axis to $name"}
                lines!=null->{val names=lines.groupValues.drop(1);mark=Mark("lines",names.flatMap(::segment));params=mapOf("lines" to names);title="between lines ${names.joinToString(" and ")} (smaller angle)"}
                vectors!=null->{val names=vectors.groupValues.drop(1)
                    val shapes=names.map{name->s.shapes.singleOrNull{it.name.equals(name,true)&&it.type==Shape2DType.Vector&&it.pointIndices.size==2&&it.pointIndices.all{s.points.indices.contains(it)}} ?: throw RuhiReferenceQuestion("Create or uniquely name vector $name first.")}
                    mark=Mark("vectors",shapes.flatMap{it.pointIndices});params=mapOf("vectors" to names);title="between vectors ${names.joinToString(" and ")}"
                }
                else->{val segments=s.shapes.filter{it.type==Shape2DType.Segment&&it.pointIndices.size==2&&it.pointIndices.all{s.points.indices.contains(it)}}.takeLast(2);if(segments.size!=2)throw RuhiReferenceQuestion("Draw two segments first.");mark=Mark("segments",segments.flatMap{it.pointIndices});params=mapOf("target" to "last_two_segments");title="between the last two segments"}
            }
            frame=frame.copy(parameters=params);val value=measure(s,mark)
            require(marks(s).size+RuhiAngleLanguage.marks(s).size<32){"Remove an angle annotation before adding more."}
            val key=PREFIX+mark.kind+"."+mark.indices.joinToString(".");val next=s.copy(labSessionValues=s.labSessionValues+(key to (mark.kind+"|"+mark.indices.joinToString(","))))
            val committed=h.forModule(s.module).execute(s,GraphWorkspaceCommand(s,GraphWorkspaceCommand.reconcileDocument(s,next),"Ruhi: reference angle"))
            return RuhiExecutionResult(committed,GraphCommandStatus.APPLIED,"Angle $title = ${value.degrees} degrees. Marked on the canvas; follows its reference points.",RuhiCommand(engine=RuhiEngine.GEOMETRY_2D,operation=RuhiOperation.PointQuery(frame),modality=m),c,semanticFrame=frame)
        }catch(e:RuhiReferenceQuestion){return RuhiExecutionResult(s,GraphCommandStatus.CLARIFICATION,e.message ?: "Choose the angle references.",context=c,dialogue=RuhiDialogueView(RuhiDialogueAction.ASK_CLARIFICATION,e.missing.map{RuhiAnswerChip("Create point $it","Create point $it at ")}),semanticFrame=frame)}
        catch(e:IllegalArgumentException){return RuhiExecutionResult(s,GraphCommandStatus.INVALID,e.message ?: "Invalid angle references.",context=c,semanticFrame=frame)}
    }
}
