package com.indianservers.aiexplorer.assistant.workspace

import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import kotlin.math.*

/** NLP proposes an intent; a bounded parser constructs native typed graph expressions. */
object RuhiFunctionLanguage {
    data class Parsed(val native:String,val parameters:Map<String,Any>,val name:String?=null)
    private val head=Regex("(?i)^(?:plot|graph|draw|show|visualize|sketch|display)\\s+(?:the\\s+)?")
    fun accepts(source:String):Boolean {
        if(source.length>2000)return false
        val t=RuhiMathLanguage.normalize(source)
        return head.containsMatchIn(t)&&('='.let{it in t}||Regex("\\b(?:sine|cosine|tangent|absolute.value|natural logarithm)\\b").containsMatchIn(t))||Regex("^[xy]\\s*=").containsMatchIn(t)
    }
    /** Insert implicit multiplication without splitting identifiers or scientific numeric literals. */
    fun expression(source:String):String {
        var t=MathExpressionNormalizer.normalize(RuhiMathLanguage.normalize(source)).replace("θ","theta")
        t=t.replace(Regex("(?i)\\be to the x\\b"),"exp(x)").replace(Regex("(?i)\\b([0-9.]+) to the x\\b"),"$1^x")
            .replace(Regex("(?i)\\bover\\b"),"/")
        if(t.startsWith("square root of "))t="sqrt(${t.removePrefix("square root of ")})"
        val tokens=Regex("(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:[eE][-+]?[0-9]+)?|[A-Za-z_][A-Za-z_0-9]*|[^\\s]").findAll(t).map{it.value}.toList()
        val variables=setOf("x","t","theta","pi","e")
        fun number(s:String)=s.toDoubleOrNull()!=null
        return buildString {tokens.forEachIndexed{i,token->val previous=tokens.getOrNull(i-1);if(previous!=null&&(number(previous)||previous==")"||previous in variables)&&(token=="("||token.firstOrNull()?.isLetter()==true||number(token)))append('*');append(token)}}
    }
    private fun arguments(text:String):List<String> {
        val out=mutableListOf<String>();var depth=0;var start=0
        text.forEachIndexed{i,ch->when(ch){'('->depth++;')'->depth--;','->if(depth==0){out+=text.substring(start,i);start=i+1}};require(depth>=0){"Unbalanced formula parentheses."}}
        require(depth==0){"Unbalanced formula parentheses."};out+=text.substring(start);return out
    }
    fun parse(source:String):Parsed? {
        var raw=source.trim().trimEnd('.','?').replace(Regex("(?i)^ruhi[,:]?\\s+"),"").replace(Regex("(?i)^(?:please|can you|could you)\\s+"),"")
        if(Regex("(?i)\\bthen\\b|;\\s*(?:then|plot|draw)|\\band (?:move|color|rotate|plot|draw)\\b").containsMatchIn(raw))return null
        raw=head.replaceFirst(raw,"")
        raw=raw.replace(Regex("(?i)^(?:line|parabola|cubic|constant function|function|polar curve|piecewise function)\\s+"),"")
        val alias=when(raw.lowercase()){ "sine curve","sine function"->"sin(x)";"cosine curve","cosine function"->"cos(x)";"tangent curve","tangent function"->"tan(x)";"absolute-value graph","absolute value graph","absolute value function"->"abs(x)";"natural logarithm function","natural logarithm graph"->"ln(x)";else->null}
        if(alias!=null)return Parsed("y=$alias",mapOf("expression" to alias))
        val domainMatch=Regex("(?i)\\s+(?:only\\s+)?for x between (.+?) and (.+)$").find(raw)
        val domain=domainMatch?.let { m->m.groupValues.drop(1).map{RuhiMathLanguage.normalize(it).toDoubleOrNull() ?: throw IllegalArgumentException("Use numeric domain endpoints.")}.also{require(it.all{v->v.isFinite()&&abs(v)<=10000}&&it[0]<it[1]){"Use a finite increasing domain within 10000 units."}} }
        if(domainMatch!=null)raw=raw.substring(0,domainMatch.range.first)
        val upper=Regex("(?i)^upper half of (.+)$").matchEntire(raw)
        if(upper!=null){require(domain==null);val equation=expression(upper.groupValues[1]);val radius2=Regex("^x\\^2\\+y\\^2=(.+)$").matchEntire(equation)?.groupValues?.get(1)?.toDoubleOrNull() ?: throw IllegalArgumentException("Use a circle centered at the origin for this upper-half form.");require(radius2.isFinite()&&radius2>0&&radius2<=1e6);return Parsed("y=sqrt($radius2-x^2)",mapOf("equation" to equation,"branch" to "upper"))}
        val param=Regex("(?i)^x\\s*=\\s*(.+),\\s*y\\s*=\\s*(.+)$").matchEntire(raw)
        if(param!=null){require(domain==null){"This command uses the native default parameter range; remove the x-domain restriction."};val x=expression(param.groupValues[1]);val y=expression(param.groupValues[2]);return Parsed("x(t)=$x; y(t)=$y",mapOf("parametric" to listOf(x,y)))}
        val polar=Regex("(?i)^r\\s*=\\s*(.+)$").matchEntire(raw)
        if(polar!=null){require(domain==null);val radius=expression(polar.groupValues[1]);return Parsed("r=$radius",mapOf("polar" to radius))}
        val named=Regex("(?i)^([a-z_][a-z0-9_]{0,63})\\s*\\(x\\)\\s*=\\s*(.+)$").matchEntire(raw)
        val explicit=Regex("(?i)^y\\s*=\\s*(.*)$").matchEntire(raw)
        if(explicit!=null||named!=null){val rhs=explicit?.groupValues?.get(1) ?: named!!.groupValues[2];require(rhs.isNotBlank()){"Supply the right-hand expression."};var params:Map<String,Any>
            val piece=Regex("(?i)^(.+?) if (.+?),\\s*otherwise\\s*(.+)$").matchEntire(rhs)
            val body=if(piece!=null){require(domain==null){"Specify the domain inside the piecewise conditions."};val yes=expression(piece.groupValues[1]);val condition=expression(piece.groupValues[2]);val no=expression(piece.groupValues[3]);params=mapOf("expression" to "piecewise($condition,$yes,$no)");"piecewise{$condition:$yes;1:$no}"}else{val e=expression(rhs);params=mapOf("expression" to e);if(e.startsWith("piecewise(")&&e.endsWith(')')){require(domain==null);val args=arguments(e.removePrefix("piecewise(").dropLast(1));require(args.size==3){"A piecewise rule needs a condition and two expressions."};"piecewise{${args[0]}:${args[1]};1:${args[2]}}"}else "y=$e"}
            if(domain!=null)params=params+mapOf("domain" to domain)
            if(named!=null)params=params+mapOf("function_name" to named.groupValues[1])
            return Parsed(body+if(domain==null)""else" {x>=${domain[0]}&&x<=${domain[1]}}",params,named?.groupValues?.get(1))
        }
        if(raw.contains('=')&&Regex("^[xX][0-9\\s^+*/().=-]*$").containsMatchIn(raw)){require(domain==null);val e=expression(raw);return Parsed(e,mapOf("equation" to e))}
        return null
    }
    fun execute(s:WorkspaceState,source:String,h:WorkspaceHistoryByModule,c:RuhiSceneContext,m:RuhiInputModality):RuhiExecutionResult? {
        if(s.module!=MathModule.Graph2D||!accepts(source))return null
        var frame=RuhiSemanticFrame("PLOT_FUNCTION_2D",RuhiEngine.GRAPH_2D,emptyMap())
        return try {
            val p=parse(source) ?: return null;frame=frame.copy(parameters=p.parameters)
            require(p.native.length<=1200){"Keep a graph formula within 1200 characters."}
            var depth=0;p.native.forEach{ch->if(ch=='(')depth++;if(ch==')')depth--;require(depth in 0..64){"Keep formula nesting within 64 levels."}};require(depth==0){"Unbalanced formula parentheses."}
            val typed=TypedGraphExpressionParser.parse(p.native)
            if(typed.parameters.isNotEmpty())return RuhiExecutionResult(s,GraphCommandStatus.CLARIFICATION,"What values should I use for ${typed.parameters.joinToString()}? Supply the complete equation with numeric values.",context=c,semanticFrame=frame)
            val sample=TypedGraphEngine().sample(typed,samples=520)
            require(sample.curves.any{it.points.size>=2}||sample.implicitSegments.isNotEmpty()){"The equation has no finite drawable curve in the default graph window [-10,10]. Supply a different expression or domain."}
            val existing=p.name?.let{n->s.functions.filter{it.name.substringBefore('(').equals(n,true)}.also{require(it.size<=1){"The function name is ambiguous."}}.singleOrNull()}
            require(existing!=null||s.functions.size<24){"The graph layer budget is 24."}
            val id=existing?.id ?: generateSequence(1){it+1}.map{"ruhi-graph-$it"}.first{candidate->s.functions.none{it.id==candidate}&&candidate !in s.universalMathDocument?.objects.orEmpty()}
            val f=existing?.copy(expression=p.native,visible=true) ?: FunctionDefinition(id,p.name ?: GraphEquationNames.next(s.functions.map{it.name}.toSet()),p.native,"cyan")
            val next=s.copy(functions=if(existing==null)s.functions+f else s.functions.map{if(it.id==id)f else it},labSessionValues=s.labSessionValues+(GraphAssistant.selectionKey(s.module) to id))
            val committed=h.forModule(s.module).execute(s,GraphWorkspaceCommand(s,GraphWorkspaceCommand.reconcileDocument(s,next),"Ruhi: plot typed function"))
            val cue=runCatching{RuhiLanguageServices.semanticClassifier?.predict(RuhiSeedNlpInput.normalize(source))}.getOrNull()
            RuhiExecutionResult(committed,GraphCommandStatus.APPLIED,"Plotted ${f.name}: ${p.native}. Finite sampled segments preserve domain gaps. ${if(typed is TypedGraphExpression.Polar)"Using the native angle range 0 to 2*pi radians."else if(typed is TypedGraphExpression.Parametric)"Using the native parameter range -2*pi to 2*pi."else""}",RuhiCommand(engine=RuhiEngine.GRAPH_2D,operation=RuhiOperation.PointQuery(frame),modality=m,intentProposal=cue),c,semanticFrame=frame)
        }catch(e:RuntimeException){RuhiExecutionResult(s,GraphCommandStatus.INVALID,e.message ?: "Invalid graph formula.",context=c,semanticFrame=frame)}
    }
}
