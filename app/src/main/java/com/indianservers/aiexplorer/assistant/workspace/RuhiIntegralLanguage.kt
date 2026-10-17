package com.indianservers.aiexplorer.assistant.workspace

import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import java.io.*
import java.util.Base64
import kotlin.math.*

/** Intent and bounds are language data; all areas and displayed samples come from native calculus. */
object RuhiIntegralLanguage {
    const val MARK="ruhi.graph.integral.v1"
    data class Ref(val kind:String,val value:String)
    data class Query(val refs:List<Ref>,val mode:String="integral",val boundsKind:String="missing",val low:Double?=null,val high:Double?=null,val start:Double=0.0)
    data class Parsed(val query:Query,val parameters:Map<String,Any>)
    data class Band(val vertices:List<Vec2>,val sign:Int,val approximation:Boolean=false)
    data class Drawing(val helpers:List<RuhiIntersectionLanguage.Helper> = emptyList(),val bands:List<Band> = emptyList(),val values:RuhiIntegralMath.Values?=null,val estimate:RuhiIntegralMath.Estimate?=null,val formula:String?=null,val bounds:Pair<Double,Double>?=null,val diagnostic:String="")
    private class MissingGraph:IllegalArgumentException("Which curve should I integrate?")
    private data class Key(val record:String,val functions:List<FunctionDefinition>,val view:Graph2DViewState,val aspect:String?)
    private val cache=object:LinkedHashMap<Key,Drawing>(8,.75f,true){override fun removeEldestEntry(e:MutableMap.MutableEntry<Key,Drawing>?)=size>8}
    private fun text(s:String)=MathExpressionNormalizer.normalize(RuhiMathLanguage.normalize(s)).replace("∫","integral of").trim()
    private fun expr(v:String)=RuhiFunctionLanguage.expression(v.trim().replace(Regex("^y\\s*=\\s*"),"").removeSuffix(" dx")).let{if(it=="e^x")"exp(x)"else it}
    private fun constant(v:String):Double=ExpressionEngine().compile(expr(v.replace(Regex("^x\\s*=\\s*"),""))).eval(emptyMap()).also{require(it.isFinite()&&abs(it)<=10000){"Use finite integration bounds within 10000 units."}}
    private fun boundSlot(v:String):Any=expr(v.replace(Regex("^x\\s*=\\s*"),"")).let{it.toDoubleOrNull() ?: it}
    private fun id(v:String)=Base64.getUrlEncoder().withoutPadding().encodeToString(v.toByteArray(Charsets.UTF_8))
    private fun unid(v:String)=String(Base64.getUrlDecoder().decode(v),Charsets.UTF_8)
    private fun scene(s:WorkspaceState)=Base64.getEncoder().encodeToString(java.security.MessageDigest.getInstance("SHA-256").digest((s.id+s.functions+s.points).toByteArray(Charsets.UTF_8)))
    fun accepts(source:String):Boolean=source.length<=2000&&Regex("^(?:integrate|find (?:the )?(?:signed area|area|integral)|evaluate the integral|calculate (?:the )?(?:net )?integral|compute the integral|estimate integral|shade (?:area under|under|the region)|show (?:the area between|integral accumulation)|use a riemann|visualize trapezoid|compare left and right riemann|highlight positive and negative area|what is the (?:antiderivative|absolute area))").containsMatchIn(text(source))
    fun parse(source:String):Parsed {
        var t=text(source);val original=t;val params=linkedMapOf<String,Any>()
        val accumulation=Regex("^show integral accumulation from (.+?) to x for (.+)$").matchEntire(t)
        if(accumulation!=null){val start=constant(accumulation.groupValues[1]);val body=expr(accumulation.groupValues[2]);return Parsed(Query(listOf(Ref("literal",body)),"accumulation","variable",start=start),mapOf("expression" to body,"start" to start,"variable_upper" to "x"))}
        var low:Double?=null;var high:Double?=null;var slots:List<Any>?=null
        val bracket=Regex("\\s+(?:on|over)\\s*\\[([^,]+),([^]]+)\\]$").find(t)
        val from=Regex("\\s+(?:from|over)\\s+(.+?)\\s+to\\s+(.+)$").find(t)
        val between=Regex("\\s+between\\s+(.+?)\\s+and\\s+(.+)$").find(t)
        val range=bracket ?: from ?: between?.takeIf{runCatching{constant(it.groupValues[1]);constant(it.groupValues[2])}.isSuccess}
        if(range!=null){low=constant(range.groupValues[1]);high=constant(range.groupValues[2]);slots=range.groupValues.drop(1).map(::boundSlot);t=t.substring(0,range.range.first).removeSuffix(" for x").trim()}
        val pair=Regex("^(?:show the area between|shade the region between|find area enclosed by) (.+?) and (.+)$").matchEntire(t)
        val mode=when{pair!=null->"between";original.startsWith("what is the antiderivative")->"indefinite";original.startsWith("use a riemann")->"riemann";original.startsWith("visualize trapezoid")->"trapezoid";original.startsWith("compare left and right")->"comparison";original.startsWith("highlight positive and negative")->"split_sign";original.startsWith("what is the absolute area")->"absolute";original.contains("area under")||original.contains("area beneath")->"area";else->"integral"}
        val selected=original.contains("selected")
        val refs=when{
            pair!=null->{val bodies=pair.groupValues.drop(1).map(::expr);params["functions"]=bodies;params["between"]=true;bodies.map{Ref("literal",it)}}
            selected->{params["target"]="selected_curve";listOf(Ref("selected",""))}
            else->{val prefix=Regex("^(?:integrate|find (?:the )?integral of|evaluate the integral of|calculate (?:the )?(?:net )?integral of|compute the integral of|estimate integral of|find (?:the )?area (?:under|beneath)|shade (?:area )?under|find the signed area for|use a riemann sum for|visualize trapezoid-rule approximation of|compare left and right riemann sums for|highlight positive and negative area separately on|what is the absolute area between|what is the antiderivative of)\\s+");require(prefix.containsMatchIn(t)){"Supply the function and integration interval."};val body=expr(prefix.replaceFirst(t,"").removeSuffix(" and the x-axis"));params["expression"]=body;listOf(Ref("literal",body))}
        }
        val boundsKind=when{mode=="indefinite"->"indefinite";slots!=null->"explicit";pair!=null->"intersection_derived";else->"missing"}
        if(mode=="indefinite")params["kind"]="indefinite"else params["bounds"]=slots ?: boundsKind
        if(mode=="area"&&!original.startsWith("shade"))params["area"]=true
        if(original.startsWith("shade")&&pair==null)params["shade"]=true
        if(original.contains("signed area")||original.contains("net integral"))params["signed"]=true
        if(mode=="absolute")params["signed"]=false
        if(mode=="split_sign")params["split_sign"]=true
        if(mode in setOf("riemann","trapezoid","comparison"))params["method"]=mode
        return Parsed(Query(refs,mode,boundsKind,low,high),params)
    }
    fun encode(q:Query):String{val bytes=ByteArrayOutputStream();DataOutputStream(bytes).use{o->o.writeInt(1);o.writeInt(q.refs.size);q.refs.forEach{o.writeUTF(it.kind);o.writeUTF(it.value)};o.writeUTF(q.mode);o.writeUTF(q.boundsKind);o.writeBoolean(q.low!=null);if(q.low!=null){o.writeDouble(q.low);o.writeDouble(requireNotNull(q.high))};o.writeDouble(q.start)};return Base64.getEncoder().encodeToString(bytes.toByteArray())}
    fun decode(record:String):Query{require(record.length<=16000);return DataInputStream(ByteArrayInputStream(Base64.getDecoder().decode(record))).use{i->require(i.readInt()==1);val n=i.readInt();require(n in 1..2);val refs=List(n){Ref(i.readUTF(),i.readUTF())};val mode=i.readUTF();val kind=i.readUTF();val range=if(i.readBoolean())i.readDouble() to i.readDouble()else null;Query(refs,mode,kind,range?.first,range?.second,i.readDouble())}}
    private fun bind(s:WorkspaceState,c:RuhiSceneContext,q:Query):Query=q.copy(refs=q.refs.map{ref->if(ref.kind!="selected")ref else{val visible=s.functions.filter{it.visible};val selected=s.labSessionValues[GraphAssistant.selectionKey(MathModule.Graph2D)] ?: c.selectedFunctionIds.singleOrNull();val f=visible.singleOrNull{it.id==selected} ?: visible.singleOrNull() ?: throw MissingGraph();Ref("function",f.id)}})
    private fun resolve(s:WorkspaceState,ref:Ref):RuhiIntegralMath.Function{val source=if(ref.kind=="function")s.functions.singleOrNull{it.id==ref.value&&it.visible}?.expression ?: throw IllegalArgumentException("The referenced integral curve is missing or hidden.")else "y=${ref.value}";return RuhiIntegralMath.function(source.replace(Regex("\\bt\\b"),"x"))}
    fun drawing(s:WorkspaceState):Drawing? {
        val record=s.labSessionValues[MARK] ?: return null;val key=Key(record,s.functions,s.graph2DView,s.labSessionValues[RuhiIntersectionLanguage.ASPECT]);synchronized(cache){cache[key]?.let{return it}}
        val d=runCatching{compute(s,decode(record))}.getOrElse{Drawing(diagnostic=it.message ?: "Integral unavailable.")};synchronized(cache){cache[key]=d};return d
    }
    fun compute(s:WorkspaceState,q:Query):Drawing {
        val f=q.refs.map{resolve(s,it)};require(f.size in 1..2)
        val helpers=q.refs.zip(f).filter{it.first.kind=="literal"}.map{RuhiIntersectionLanguage.Helper(RuhiIntersectionMath.Curve.Scalar(it.second.expression,it.second.restriction),"Integrand")}.toMutableList()
        if(q.mode=="indefinite"){val formula=RuhiIntegralMath.antiderivative(f.first());helpers+=RuhiIntersectionLanguage.Helper(RuhiIntersectionMath.Curve.Scalar(formula,f.first().restriction),"Antiderivative (C=0)");return Drawing(helpers=helpers,formula=formula)}
        if(q.mode=="accumulation"){val b=RuhiIntersectionLanguage.bounds(s);val formula=RuhiIntegralMath.accumulation(f.first(),q.start,b[0],b[1]);helpers+=RuhiIntersectionLanguage.Helper(RuhiIntersectionMath.Curve.Scalar(formula),"Accumulation");return Drawing(helpers=helpers,formula=formula)}
        val range=if(q.boundsKind=="intersection_derived"){require(f.size==2);val b=RuhiIntersectionLanguage.bounds(s);val p=RuhiIntersectionMath.solve(RuhiIntersectionMath.Curve.Scalar(f[0].expression,f[0].restriction),RuhiIntersectionMath.Curve.Scalar(f[1].expression,f[1].restriction),b[0],b[1]).points;require(p.size>=2){"At least two distinct intersections are needed to bound this region in the visible x range."};p.minOf{it.x} to p.maxOf{it.x}}else requireNotNull(q.low){"Supply lower and upper x bounds."} to requireNotNull(q.high)
        val a=min(range.first,range.second);val b=max(range.first,range.second);val v=RuhiIntegralMath.integrate(f.first(),range.first,range.second,f.getOrNull(1));val estimate=if(q.mode in setOf("riemann","trapezoid","comparison"))RuhiIntegralMath.estimate(f.first(),a,b)else null
        val bands=mutableListOf<Band>();val step=(b-a)/256
        for(i in 0 until 256){val x=a+i*step;val x2=if(i==255)b else a+(i+1)*step;val bottom=f.getOrNull(1);val y=RuhiIntegralMath.value(f[0],x);val y2=RuhiIntegralMath.value(f[0],x2);val base=bottom?.let{RuhiIntegralMath.value(it,x)} ?: 0.0;val base2=bottom?.let{RuhiIntegralMath.value(it,x2)} ?: 0.0;bands+=Band(listOf(Vec2(x,base),Vec2(x,y),Vec2(x2,y2),Vec2(x2,base2)),if((y+y2-base-base2)/2>=0)1 else -1)}
        estimate?.let{e->val dx=(b-a)/e.count;for(i in 0 until e.count){val x=a+i*dx;val x2=a+(i+1)*dx;val left=RuhiIntegralMath.value(f[0],x);val right=RuhiIntegralMath.value(f[0],x2);val midpoint=RuhiIntegralMath.value(f[0],(x+x2)/2);when(q.mode){"trapezoid"->bands+=Band(listOf(Vec2(x,0.0),Vec2(x,left),Vec2(x2,right),Vec2(x2,0.0)),1,true);"comparison"->{bands+=Band(listOf(Vec2(x,0.0),Vec2(x,left),Vec2(x2,left),Vec2(x2,0.0)),1,true);bands+=Band(listOf(Vec2(x,0.0),Vec2(x,right),Vec2(x2,right),Vec2(x2,0.0)),-1,true)};else->bands+=Band(listOf(Vec2(x,0.0),Vec2(x,midpoint),Vec2(x2,midpoint),Vec2(x2,0.0)),1,true)}}}
        return Drawing(helpers,bands,v,estimate,bounds=range)
    }
    private fun proposed(s:WorkspaceState,slots:Map<String,String>):WorkspaceState {
        val formula=slots["formula"] ?: return s
        require(s.functions.none{it.visible}){"The scene now has a visible curve. Ask the integral question again."}
        val name=generateSequence(1){it+1}.map{"Function$it"}.first{candidate->s.functions.none{it.name.equals(candidate,true)}}
        val f=FunctionDefinition(slots.getValue("created_curve_id"),name,formula,"cyan")
        return s.copy(functions=s.functions+f,labSessionValues=s.labSessionValues+(GraphAssistant.selectionKey(s.module) to f.id))
    }
    fun restored(s:WorkspaceState,p:RuhiPending):RuhiDialogueView {
        if(p.kind=="integral_formula")return RuhiDialogueView(RuhiDialogueAction.ASK_CLARIFICATION,listOf("y=x^2","y=x^3","y=sin(x)").map{RuhiAnswerChip(it,"Use integral formula $it")},true)

        val chips=if(p.kind=="integral_graph")s.functions.filter{it.visible&&it.id in p.targets}.take(5).map{RuhiAnswerChip(it.name,"Use integral graph ${id(it.id)}")}else{
            val range=runCatching{RuhiIntersectionLanguage.bounds(s)}.getOrNull()
            fun valid(a:Double,b:Double)=runCatching{val q=decode(p.targets.single());val state=proposed(s,p.slots);val fs=q.refs.map{resolve(state,it)};for(i in 0..32)fs.forEach{RuhiIntegralMath.value(it,a+(b-a)*i/32)}}.isSuccess
            buildList{if(range!=null&&valid(range[0],range[1]))add(RuhiAnswerChip("Visible x-range","Use integral bounds ${range[0]} to ${range[1]}"));if(valid(0.0,1.0))add(RuhiAnswerChip("0 to 1","Use integral bounds 0 to 1"));else if(valid(1.0,2.0))add(RuhiAnswerChip("1 to 2","Use integral bounds 1 to 2"));add(RuhiAnswerChip("Custom bounds","Use integral bounds "))}
        }
        return RuhiDialogueView(RuhiDialogueAction.ASK_CLARIFICATION,chips,true)
    }
    fun execute(s:WorkspaceState,source:String,h:WorkspaceHistoryByModule,c:RuhiSceneContext,m:RuhiInputModality,pendingInput:RuhiPending?):RuhiPointConversation.Outcome? {
        if(s.module!=MathModule.Graph2D)return null
        val pending=pendingInput?.takeIf{it.kind in setOf("integral_graph","integral_bounds","integral_formula")};val accepted=accepts(source)
        val formulaReply=pending?.kind=="integral_formula"&&RuhiFormulaReply.recognizes(source,"integral")
        val answer=source.startsWith("Use integral ")||formulaReply||pending?.kind=="integral_bounds"&&Regex("^.+ to .+$").matches(text(source))
        if(!accepted&&(pending==null||!answer&&text(source) !in setOf("resume","continue","resume pending")))return null
        val continuing=pending!=null&&!accepted;val commandSource=if(continuing)pending!!.source else source
        val slots=if(continuing)pending!!.slots.toMutableMap()else mutableMapOf()
        var frame=RuhiSemanticFrame("INTEGRATE",RuhiEngine.GRAPH_2D,emptyMap())
        fun ask(prompt:String,p:RuhiPending)=RuhiPointConversation.Outcome(RuhiExecutionResult(s,GraphCommandStatus.CLARIFICATION,prompt,context=c,semanticFrame=frame,dialogue=restored(s,p)),p)
        try {
            if(continuing&&pending!!.scene!=scene(s))return RuhiPointConversation.Outcome(RuhiExecutionResult(s,GraphCommandStatus.CLARIFICATION,"The scene changed. Ask the integral question again.",context=c,semanticFrame=frame),null)
            val parsed=parse(commandSource);frame=frame.copy(parameters=parsed.parameters)
            if(continuing&&pending!!.kind=="integral_formula") {
                if(!formulaReply)return ask("Which explicit function should I integrate? Give an equation such as y=x^2.",pending)
                val formula=RuhiFormulaReply.native(source,"integral");val f=RuhiIntegralMath.function(formula)
                if(!Regex("\\bx\\b").containsMatchIn(f.expression))RuhiIntegralMath.value(f,0.0)
                slots["formula"]=formula;slots["created_curve_id"]="ruhi-function-${java.util.UUID.randomUUID()}"
            }
            var state=proposed(s,slots)
            slots["formula"]?.let{frame=frame.copy(parameters=frame.parameters+("expression" to RuhiIntegralMath.function(it).expression))}
            if(continuing&&pending!!.kind=="integral_graph") {
                val token=source.removePrefix("Use integral graph ").trim()
                if(token.isBlank()||!source.startsWith("Use integral graph "))return ask("Which current curve should I integrate?",pending)
                val chosen=unid(token);require(chosen in pending.targets&&s.functions.any{it.id==chosen&&it.visible})
                state=state.copy(labSessionValues=state.labSessionValues+(GraphAssistant.selectionKey(s.module) to chosen))
            }
            var q=if(continuing&&pending!!.kind=="integral_bounds")decode(pending.targets.single())else bind(state,c,parsed.query)
            if(continuing&&pending!!.kind=="integral_bounds") {
                val range=Regex("^(?:Use integral bounds )?(.+?) to (.+)$",RegexOption.IGNORE_CASE).matchEntire(source.trim())
                    ?: return ask("Enter lower and upper x bounds, for example 0 to 2.",pending)
                q=q.copy(boundsKind="explicit",low=constant(range.groupValues[1]),high=constant(range.groupValues[2]))
                frame=frame.copy(parameters=frame.parameters+("bounds" to range.groupValues.drop(1).map(::boundSlot)))
            }
            if(q.boundsKind=="missing")return ask("What lower and upper x bounds should I use?",RuhiPending("integral_bounds",commandSource,s.module.name,scene(s),listOf(encode(q)),slots))
            val d=try{compute(state,q)}catch(e:IllegalArgumentException){
                if(slots["formula"]==null)throw e
                val p=RuhiPending("integral_bounds",commandSource,s.module.name,scene(s),listOf(encode(q)),slots)
                return RuhiPointConversation.Outcome(RuhiExecutionResult(s,GraphCommandStatus.INVALID,(e.message ?: "Invalid integration interval.")+" Choose valid lower and upper x bounds.",context=c,semanticFrame=frame,dialogue=restored(s,p)),p)
            }
            val values=d.values
            val message=when{
                q.mode=="indefinite"->"Antiderivative: ${d.formula} + C, on each connected real-domain interval. The displayed representative uses C=0."
                q.mode=="accumulation"->"Accumulation from ${q.start} to x: ${d.formula}. The integral is checked across the current x viewport."
                else->"Signed integral = ${requireNotNull(values).signed}; absolute area = ${values.absolute}; positive area = ${values.positive}; negative area = ${values.negative}; numerical error estimate = ${values.error}."+(d.estimate?.let{" ${it.count} panels: left = ${it.left}; midpoint = ${it.midpoint}; right = ${it.right}; trapezoid = ${it.trapezoid}. Approximating sums are separate from the checked integral."} ?: "")+if(q.boundsKind=="intersection_derived")" Bounds derived from visible intersections: ${d.bounds}."else""
            }
            val next=state.copy(labSessionValues=state.labSessionValues+(MARK to encode(q)))
            val committed=h.forModule(s.module).execute(s,GraphWorkspaceCommand(s,GraphWorkspaceCommand.reconcileDocument(s,next),"Ruhi: integral analysis"))
            val cue=runCatching{RuhiLanguageServices.semanticClassifier?.predict(RuhiSeedNlpInput.normalize(source))}.getOrNull()
            return RuhiPointConversation.Outcome(RuhiExecutionResult(committed,GraphCommandStatus.APPLIED,message,RuhiCommand(engine=RuhiEngine.GRAPH_2D,operation=RuhiOperation.PointQuery(frame),modality=m,intentProposal=cue),c,semanticFrame=frame),null)
        }catch(e:MissingGraph){
            val empty=s.functions.none{it.visible};val p=RuhiPending(if(empty)"integral_formula"else"integral_graph",commandSource,s.module.name,scene(s),s.functions.filter{it.visible}.map{it.id},slots)
            return ask(if(empty)"Which explicit function should I integrate? Give an equation such as y=x^2."else e.message ?: "Choose a curve.",p)
        }catch(e:RuntimeException){return RuhiPointConversation.Outcome(RuhiExecutionResult(s,GraphCommandStatus.INVALID,e.message ?: "Invalid integral request.",context=c,semanticFrame=frame),if(continuing)pending else null)}
    }
}
