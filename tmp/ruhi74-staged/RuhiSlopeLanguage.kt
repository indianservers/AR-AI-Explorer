package com.indianservers.aiexplorer.assistant.workspace

import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import kotlin.math.*

object RuhiSlopeLanguage {
    const val PREFIX="ruhi.geometry2d.slope."
    data class Mark(val first:Int,val second:Int,val triangle:Boolean)
    fun marks(s:WorkspaceState):List<Mark> = s.labSessionValues.filterKeys{it.startsWith(PREFIX)}.values.mapNotNull{value->
        val parts=value.split('|');val ids=parts.firstOrNull()?.split(',')?.mapNotNull(String::toIntOrNull).orEmpty()
        if(parts.size==2&&ids.size==2&&ids.all{it in s.points.indices})Mark(ids[0],ids[1],parts[1]=="triangle")else null
    }.take(32)
    fun accepts(source:String)=Regex("(?i)\\b(?:slopes?|gradients?)\\b|rise over run|rise and run|how steep|change in y per x|increasing or decreasing|\\bis horizontal\\b").containsMatchIn(source)
    private fun linear(e:SymbolicExpression):Pair<ExactRational,ExactRational> = when(e) {
        is SymbolicExpression.Number->{require(e.value.numerator.bitLength()<=4096&&e.value.denominator.bitLength()<=4096);ExactRational.ZERO to e.value}
        is SymbolicExpression.Variable->{require(e.name=="x"){"Resolve the line's parameters first."};ExactRational.ONE to ExactRational.ZERO}
        is SymbolicExpression.UnaryMinus->linear(e.value).let{-it.first to -it.second}
        is SymbolicExpression.Sum->e.terms.map(::linear).fold(ExactRational.ZERO to ExactRational.ZERO){a,b->a.first+b.first to a.second+b.second}
        is SymbolicExpression.Product->e.factors.map(::linear).fold(ExactRational.ZERO to ExactRational.ONE){a,b->require(a.first.isZero||b.first.isZero){"This curve has a varying slope. Specify a point for its derivative."};a.first*b.second+a.second*b.first to a.second*b.second}
        is SymbolicExpression.Power->{val n=(e.exponent as? SymbolicExpression.Number)?.value ?: throw IllegalArgumentException("Use a linear expression.");require(n.denominator==java.math.BigInteger.ONE&&abs(n.toDouble())<=64);val p=linear(e.base);when{n.isZero->ExactRational.ZERO to ExactRational.ONE;n.isOne->p;p.first.isZero->ExactRational.ZERO to p.second.pow(n.toDouble().toInt());else->throw IllegalArgumentException("This curve has a varying slope. Specify a point for its derivative.")}}
        else->throw IllegalArgumentException("Use a straight-line expression for a constant slope.")
    }
    @JvmOverloads
    fun execute(s:WorkspaceState,source:String,h:WorkspaceHistoryByModule,c:RuhiSceneContext,modality:RuhiInputModality,endpointBindings:Map<String,Pair<String,String>> = emptyMap()):RuhiExecutionResult? {
        if(s.module!=MathModule.Geometry2D||!accepts(source))return null
        var frame=RuhiSemanticFrame("CALCULATE_SLOPE",RuhiEngine.GEOMETRY_2D,emptyMap())
        val cue=runCatching{RuhiLanguageServices.semanticClassifier?.predict(RuhiSeedNlpInput.normalize(source))}.getOrNull()
        fun result(state:WorkspaceState,message:String)=RuhiExecutionResult(state,GraphCommandStatus.APPLIED,message,RuhiCommand(engine=RuhiEngine.GEOMETRY_2D,operation=RuhiOperation.PointQuery(frame),modality=modality,intentProposal=cue),c,RuhiDialogueView(RuhiDialogueAction.EXECUTE,languageProposal=cue),frame)
        try {
            Regex("(?i)\\by\\s*=\\s*(.+?)(?:\\s+have)?[.?]?$" ).find(source.trim())?.let{m->
                val body=m.groupValues[1].replace("²","^2").replace("³","^3").replace(Regex("\\s+"),"").replace(Regex("(?<=\\d)(?=x\\b)"),"*")
                val coefficient=linear(SymbolicCasEngine().parse(body)).first;require(coefficient.toDouble().isFinite())
                frame=frame.copy(parameters=mapOf("expression" to body))
                return result(s,"Slope = ${coefficient.toDouble()} (exactly $coefficient). In y = mx + b, the coefficient m is the slope.")
            }
            val resolver=RuhiEndpointResolver(s,c,endpointBindings)
            fun slope(pair:RuhiEndpointPair):Double? {
                val a=pair.first.position;val b=pair.second.position;require(listOf(a.x,a.y,b.x,b.y).all{it.isFinite()&&abs(it)<=10000})
                require(a!=b){"Coincident points do not determine a unique line."}
                val run=b.x-a.x;return if(run==0.0)null else ((b.y-a.y)/run).also{require(it.isFinite()){"The slope exceeds the finite numeric budget."}}
            }
            Regex("(?i)^compare slopes of ([\\p{L}\\p{N}_]+) and ([\\p{L}\\p{N}_]+)[.?]?$" ).matchEntire(source.trim())?.let{m->
                val a=resolver.segment(m.groupValues[1]);val b=resolver.segment(m.groupValues[2]);val ma=slope(a);val mb=slope(b)
                frame=frame.copy(parameters=mapOf("pairs" to listOf(listOf(a.first.name,a.second.name),listOf(b.first.name,b.second.name))))
                return result(s,"First slope = ${ma ?: "undefined"}; second slope = ${mb ?: "undefined"}. ${if(ma==null||mb==null)"A vertical line has no finite slope." else if(abs(ma-mb)<1e-10)"The slopes are equal." else "The slopes differ."}")
            }
            val pair=resolver.resolve(source);val value=slope(pair);val run=pair.second.position.x-pair.first.position.x;val rise=pair.second.position.y-pair.first.position.y
            val lower=source.lowercase();val visualize=lower.contains("slope triangle");val annotate=lower.startsWith("annotate")||lower.contains("rise and run")||lower.contains("next to")||lower.startsWith("mark ")
            val classification=lower.contains("increasing")||lower.contains("decreasing")||lower.contains("positive slope")||lower.contains("is horizontal")
            val explain=lower.startsWith("why ");val fraction=lower.contains("as a fraction")
            val parameters=(if(pair.first.index==null)mapOf("points" to listOf(listOf(pair.first.position.x,pair.first.position.y),listOf(pair.second.position.x,pair.second.position.y)))else pair.parameters).toMutableMap()
            if(visualize)parameters["visualize"]=true;if(annotate)parameters["annotate"]=true;if(classification)parameters["classification"]=true;if(explain)parameters["explain"]=true;if(fraction)parameters["format"]="fraction"
            frame=frame.copy(parameters=parameters)
            var state=s
            if(visualize||annotate) {
                require(pair.first.index!=null&&pair.second.index!=null){"Name the line's endpoints before annotating it."}
                val key=PREFIX+pair.first.index+"."+pair.second.index;require(s.labSessionValues.keys.count{it.startsWith(PREFIX)}<32||key in s.labSessionValues){"Slope annotation budget reached."}
                val next=s.copy(labSessionValues=s.labSessionValues+(key to "${pair.first.index},${pair.second.index}|${if(visualize)"triangle" else "label"}"))
                state=h.forModule(s.module).execute(s,GraphWorkspaceCommand(s,next,"Ruhi: slope annotation"))
            }
            val type=when{value==null->"Vertical line: slope is undefined because run = 0.";value==0.0->"Horizontal line.";value>0->"Increasing (positive slope).";else->"Decreasing (negative slope)."}
            val exact=if(fraction&&value!=null)(ExactRational.parse(rise.toString())/ExactRational.parse(run.toString())).let{" Fraction = ${it.numerator}/${it.denominator}."} else ""
            return result(state,"Slope = ${value ?: "undefined"}; rise = $rise, run = $run. $type$exact${if(visualize)" The rise/run triangle is shown on the canvas." else if(annotate)" Annotated next to the line." else ""}")
        }catch(e:RuhiReferenceQuestion){return RuhiExecutionResult(s,GraphCommandStatus.CLARIFICATION,e.message ?: "Choose a line.",context=c,dialogue=RuhiDialogueView(RuhiDialogueAction.ASK_CLARIFICATION,e.missing.map{RuhiAnswerChip("Create point $it","Create point $it at ")}),semanticFrame=when{e.segmentSelection->frame.copy(parameters=frame.parameters+("segment_selection" to true));e.endpointToken!=null->frame.copy(parameters=frame.parameters+("endpoint_reference" to e.endpointToken));else->frame})}
         catch(e:IllegalArgumentException){return RuhiExecutionResult(s,GraphCommandStatus.INVALID,e.message ?: "Invalid slope input.",context=c,semanticFrame=frame)}
    }
}
