package com.indianservers.aiexplorer.assistant.workspace

import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import java.io.*
import java.util.Base64
import kotlin.math.*

/** Persist source references, not derivative answers, so annotations follow edits and undo. */
object RuhiDerivativeLanguage {
    const val MARK="ruhi.graph.derivative.v1"
    const val POINT_PREFIX="ruhi.derivative.point."
    data class Query(val kind:String,val source:String,val mode:String="derivative",val order:Int=1,val xs:List<Double> = emptyList(),val pointKey:String="",val pointIndex:Int?=null)
    data class Parsed(val query:Query,val parameters:Map<String,Any>)
    private class Missing(val point:Boolean,val prompt:String):IllegalArgumentException(prompt)
    private fun text(s:String)=RuhiMathLanguage.normalize(s).replace(Regex("^ruhi[,:]?\\s*"),"").replace(Regex("^(?:please|can you|could you)\\s+"),"").trim().trimEnd('.','?')
    fun accepts(s:String):Boolean {val t=text(s);return s.length<=2000&&Regex("^(?:differentiate|find the derivative|show derivative of|what is the derivative|compute d/dx|find the rate of change|show dy/dx|show how the tangent changes|calculate the second derivative|plot f prime|plot the derivative|draw (?:a tangent|the normal|tangents)|show the (?:slope|tangent slope)|calculate (?:derivative|the slope)|find (?:the stationary|critical) points|where is the slope (?:zero|0)|what is the instantaneous gradient|explain why absolute value)").containsMatchIn(t)}
    private fun scene(s:WorkspaceState)=Base64.getEncoder().encodeToString(java.security.MessageDigest.getInstance("SHA-256").digest((s.id+s.functions+s.points).toByteArray(Charsets.UTF_8)))
    private fun id(v:String)=Base64.getUrlEncoder().withoutPadding().encodeToString(v.toByteArray(Charsets.UTF_8))
    private fun unid(v:String)=String(Base64.getUrlDecoder().decode(v),Charsets.UTF_8)
    fun restored(s:WorkspaceState,p:RuhiPending):RuhiDialogueView {
        if(p.kind=="derivative_formula")return RuhiDialogueView(RuhiDialogueAction.ASK_CLARIFICATION,listOf("y=x^2","y=x^3","y=sin(x)").map{RuhiAnswerChip(it,"Use derivative formula $it")},true)

        if(p.kind=="derivative_x"&&text(p.source).startsWith("explain why absolute value"))return RuhiDialogueView(RuhiDialogueAction.ASK_CLARIFICATION,listOf(-1,0,1).map{RuhiAnswerChip("x = $it","Use derivative x $it")},true)
        val chips=if(p.kind=="derivative_graph")s.functions.filter{it.visible&&it.id in p.targets}.take(5).map{RuhiAnswerChip(it.name,"Use derivative graph ${id(it.id)}")}else {
            val target=p.slots["curve_id"]?.let{id->s.functions.singleOrNull{it.visible&&it.id==id}}
                ?: runCatching{selected(s,RuhiSceneContext())}.getOrNull()
            val derivative=(p.slots["formula"] ?: target?.expression)?.let{runCatching{RuhiDerivativeMath.derive(it)}.getOrNull()}
            val examples=listOf(-1.0,0.0,1.0,2.0,-2.0,.5,-.5,3.0,-3.0,10.0,-10.0)
                .filter{target==null&&p.slots["formula"]==null||derivative!=null&&runCatching{RuhiDerivativeMath.slope(derivative,it)}.isSuccess}.take(3)
            examples.map{val label=if(it==it.toLong().toDouble())it.toLong().toString()else it.toString();RuhiAnswerChip("x = $label","Use derivative x $label")}
        }
        return RuhiDialogueView(RuhiDialogueAction.ASK_CLARIFICATION,chips,true)
    }
    fun encode(q:Query):String {val bytes=ByteArrayOutputStream();DataOutputStream(bytes).use{o->o.writeInt(2);o.writeUTF(q.kind);o.writeUTF(q.source);o.writeUTF(q.mode);o.writeInt(q.order);o.writeInt(q.xs.size);q.xs.forEach(o::writeDouble);o.writeUTF(q.pointKey)};return Base64.getEncoder().encodeToString(bytes.toByteArray())}
    fun decode(record:String):Query {require(record.length<=12000);return DataInputStream(ByteArrayInputStream(Base64.getDecoder().decode(record))).use{i->val version=i.readInt();require(version in 1..2);val kind=i.readUTF();val source=i.readUTF();val mode=i.readUTF();val order=i.readInt();val count=i.readInt();require(count in 0..8);val xs=List(count){i.readDouble()};val key=if(version>=2)i.readUTF()else "";require(key.isEmpty()||key.startsWith(POINT_PREFIX)&&key.length<=128);Query(kind,source,mode,order,xs,key)}}
    private fun selected(s:WorkspaceState,c:RuhiSceneContext):FunctionDefinition {
        val f=s.functions.filter{it.visible};val chosen=s.labSessionValues[GraphAssistant.selectionKey(MathModule.Graph2D)] ?: c.selectedFunctionIds.singleOrNull()
        return f.singleOrNull{it.id==chosen} ?: f.singleOrNull() ?: throw Missing(false,if(f.isEmpty())"Plot an explicit function first."else"Which curve should I differentiate?")
    }
    fun parse(s:WorkspaceState,c:RuhiSceneContext,source:String,overrideX:Double?=null):Parsed {
        val t=text(source);fun expr(v:String)=RuhiFunctionLanguage.expression(v.removePrefix("y = ").removePrefix("y=").trim()).let{if(it=="e^x")"exp(x)"else it}
        fun number(v:String)=v.trim().replace(Regex("^([+-])\\s+"),"\$1").toDouble().also{require(it.isFinite()&&abs(it)<=10000){"Use a finite x coordinate within 10000 units."}}
        val selected=t.contains("selected")||t.contains("this curve")||t.contains("gradient here")||t.contains("how the tangent changes")
        if(t.startsWith("explain why absolute value")) {
            val explanation=Regex("^explain why absolute value (?:is not|isn't|is) differentiable(?: at (?:x\\s*=\\s*)?(.+))?$").matchEntire(t)
                ?: throw IllegalArgumentException("Ask about abs(x) at one numeric x coordinate.")
            val raw=explanation.groupValues[1]
            val x=if(raw.isNotBlank())number(raw)else overrideX?.also{number(it.toString())} ?: throw Missing(true,"At which x coordinate should I explain differentiability of abs(x)?")
            return Parsed(Query("literal","abs(x)","explain",xs=listOf(x)),mapOf("expression" to "abs(x)","at_x" to x,"explain" to true))
        }
        val mode=when{t.contains("how the tangent changes")->"slider";t.startsWith("draw the normal")->"normal";t.startsWith("draw tangents")->"tangents";t.startsWith("draw a tangent")->"tangent";t.contains("stationary points")||t.contains("slope zero")||t.contains("slope 0")->"stationary";t.contains("critical points")->"critical";else->"derivative"}
        val order=if(t.contains("second derivative"))2 else 1
        var body:String?=null;var xs=emptyList<Double>()
        val multi=Regex("^draw tangents at x\\s*=\\s*(.+?) and x\\s*=\\s*(.+?) to (.+)$").matchEntire(t)
        val atFirst=Regex("^calculate (?:derivative|the slope) at x\\s*=\\s*(.+?) (?:for|on) (.+)$").matchEntire(t)
        val atLast=Regex("^(?:show the slope of|draw a tangent to|draw the normal to) (.+?) at x\\s*=\\s*(.+)$").matchEntire(t)
        when {
            multi!=null->{body=expr(multi.groupValues[3]);xs=multi.groupValues.slice(1..2).map(::number)}
            atFirst!=null->{body=expr(atFirst.groupValues[2]);xs=listOf(number(atFirst.groupValues[1]))}
            atLast!=null->{body=expr(atLast.groupValues[1]);xs=listOf(number(atLast.groupValues[2]))}
            selected->{if(t.contains("selected point")||t.contains("gradient here"))xs=listOf(overrideX ?: c.selectedPointIndex?.let{s.points.getOrNull(it)?.x} ?: throw Missing(true,"At which x coordinate should I evaluate the derivative?"))}
            else->{val prefix=Regex("^(?:differentiate|find the derivative of|show derivative of|what is the derivative of|compute d/dx of|find the rate of change of|show dy/dx for|calculate the second derivative of|plot f prime for|find the stationary points of|find critical points of)\\s+");require(prefix.containsMatchIn(t)){"Supply an explicit function to differentiate."};body=prefix.replaceFirst(t,"").replace(Regex("\\s+(?:where defined|excluding singularities)$"),"");body=body.replace(Regex("^(?:y|[a-z_][a-z0-9_]*\\(x\\))\\s*=\\s*"),"");body=expr(body)}
        }
        val targetPoint=t.contains("selected point")||t.contains("gradient here")
        val params=linkedMapOf<String,Any>();val q=if(selected){val f=selected(s,c);params["target"]=if(targetPoint)"selected_point_on_curve"else"selected_curve";Query("function",f.id,mode,order,xs,pointIndex=if(targetPoint&&overrideX==null)c.selectedPointIndex else null)}else{params["expression"]=requireNotNull(body);Query("literal",body,mode,order,xs)}
        if(q.pointIndex!=null)try{evaluationXs(s,q)}catch(e:IllegalArgumentException){throw Missing(true,"${e.message} At which x coordinate should I evaluate the curve instead?")}
        if(xs.isNotEmpty()&&!targetPoint)params["at_x"]=if(xs.size==1)xs.single()else xs
        if(mode=="slider")params["animation"]="tangent_slider"
        if(order!=1)params["order"]=order
        if(mode in setOf("tangent","normal","tangents"))params["draw"]=mode
        if(t.startsWith("plot "))params["draw"]="derivative"
        if(mode in setOf("stationary","critical"))params["analysis"]=mode
        return Parsed(q,params)
    }
    fun resolve(s:WorkspaceState,q:Query):RuhiDerivativeMath.Result {
        require(q.xs.all{it.isFinite()&&abs(it)<=10000})
        val source=if(q.kind=="function")s.functions.singleOrNull{it.visible&&it.id==q.source}?.expression ?: throw IllegalArgumentException("The referenced curve is missing or hidden.")else "y=${q.source}"
        return RuhiDerivativeMath.derive(source,q.order)
    }
    private fun evaluationXs(s:WorkspaceState,q:Query):List<Double> {
        if(q.pointKey.isEmpty()&&q.pointIndex==null)return q.xs
        val index=if(q.pointKey.isNotEmpty())s.labSessionValues[q.pointKey]?.toIntOrNull()else q.pointIndex
        val p=requireNotNull(index?.let{s.points.getOrNull(it)}){"The referenced point is missing."}
        require(p.x.isFinite()&&p.y.isFinite()&&abs(p.x)<=10000&&abs(p.y)<=10000){"Use a finite point within 10000 units."}
        val y=requireNotNull(RuhiDerivativeMath.value(resolve(s,q),p.x)){"The curve is undefined at the selected point."}
        require(abs(y-p.y)<=1e-8*max(1.0,max(abs(y),abs(p.y)))){"The selected point is not on the current curve."}
        return listOf(p.x)
    }
    fun drawing(s:WorkspaceState,traceX:Double=s.labSessionValues["Graph2D::ruhi.derivative.trace_x"]?.toDoubleOrNull() ?: 2.0):RuhiIntersectionLanguage.Drawing? {
        val record=s.labSessionValues[MARK] ?: return null
        return runCatching {
            val q=decode(record);val r=resolve(s,q);val xs=evaluationXs(s,q);val helpers=mutableListOf<RuhiIntersectionLanguage.Helper>();val points=mutableListOf<Vec2>()
            if(q.kind=="literal")helpers+=RuhiIntersectionLanguage.Helper(RuhiIntersectionMath.Curve.Scalar(r.original,r.restriction),"Original function")
            when(q.mode){
                "tangent","normal","tangents","slider"->(if(q.mode=="slider")listOf(traceX)else xs).forEach{x->val slope=RuhiDerivativeMath.slope(r,x);points+=slope.point;helpers+=RuhiIntersectionLanguage.Helper(RuhiDerivativeMath.line(slope,q.mode=="normal"),q.mode)}
                "stationary","critical"->{val b=RuhiIntersectionLanguage.bounds(s);points+=if(q.mode=="critical")RuhiDerivativeMath.critical(r,b[0],b[1])else RuhiDerivativeMath.stationary(r,b[0],b[1])}
                "explain"->xs.forEach{x->points+=Vec2(x,requireNotNull(RuhiDerivativeMath.value(r,x)))}
                else->{val restriction=listOfNotNull(r.restriction,"abs(${r.original})<1e100").joinToString("&&"){"($it)"};helpers+=RuhiIntersectionLanguage.Helper(RuhiIntersectionMath.Curve.Scalar(r.derivative,restriction),"Derivative");xs.forEach{points+=RuhiDerivativeMath.slope(r,it).point}}
            }
            RuhiIntersectionLanguage.Drawing(points,points.indices.map{"D${it+1}"},emptySet(),helpers,if(q.kind=="function")setOf(q.source)else emptySet())
        }.getOrElse{RuhiIntersectionLanguage.Drawing(emptyList(),emptyList(),emptySet(),emptyList(),emptySet(),diagnostic=it.message ?: "Derivative unavailable.")}
    }
    fun execute(s:WorkspaceState,source:String,h:WorkspaceHistoryByModule,c:RuhiSceneContext,m:RuhiInputModality,pendingInput:RuhiPending?):RuhiPointConversation.Outcome? {
        if(s.module!=MathModule.Graph2D)return null
        val pending=pendingInput?.takeIf{it.kind in setOf("derivative_graph","derivative_x","derivative_formula")}
        if(!accepts(source)&&pending==null)return null
        val formulaReply=pending?.kind=="derivative_formula"&&RuhiFormulaReply.recognizes(source,"derivative")
        val numeric=Regex("^(?:x\\s*=\\s*)?([-+]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)(?:[eE][-+]?\\d+)?)$").matchEntire(text(source))
        val named=s.functions.filter{it.visible&&text(it.name)==text(source)}.singleOrNull()
        if(pending!=null&&!accepts(source)&&!source.startsWith("Use derivative ")&&!(pending.kind=="derivative_x"&&numeric!=null)&&!(pending.kind=="derivative_graph"&&named!=null)&&!formulaReply&&text(source) !in setOf("resume","continue","resume pending"))return null
        var frame=RuhiSemanticFrame("DIFFERENTIATE",RuhiEngine.GRAPH_2D,emptyMap())
        fun ask(prompt:String,p:RuhiPending?)=RuhiPointConversation.Outcome(RuhiExecutionResult(s,GraphCommandStatus.CLARIFICATION,prompt,context=c,semanticFrame=frame,dialogue=p?.let{restored(s,it)}),p)
        val continuing=pending!=null&&!accepts(source)
        val slots=if(continuing)pending!!.slots.toMutableMap()else mutableMapOf()
        val literalExplanation=text(if(continuing)pending!!.source else source).startsWith("explain why absolute value")
        if(literalExplanation){slots.remove("curve_id");slots.remove("point_index")}
        try {
            if(pending!=null&&pending.scene!=scene(s)&&!literalExplanation)return ask("The scene changed. Ask the derivative question again.",null)
            var state=s;var overrideX:Double?=slots["x"]?.toDouble()
            var suppliedFormula=false
            if(continuing&&pending!!.kind=="derivative_formula"&&formulaReply) {
                val formula=RuhiFormulaReply.native(source,"derivative")
                val derived=RuhiDerivativeMath.derive(formula)
                if(!Regex("\\bx\\b").containsMatchIn(derived.original))require(RuhiDerivativeMath.value(derived,0.0)!=null){"Give a finite constant or an explicit function of x."}
                slots["formula"]=formula
                slots["created_curve_id"]="ruhi-function-${java.util.UUID.randomUUID()}"
                suppliedFormula=true
            }
            slots["formula"]?.let{formula->
                require(s.functions.none{it.visible}){"The scene now has a curve. Ask the analysis question again."}
                val name=generateSequence(1){it+1}.map{"Function$it"}.first{candidate->state.functions.none{it.name.equals(candidate,true)}}
                val created=FunctionDefinition(slots.getValue("created_curve_id"),name,formula,"cyan")
                state=state.copy(functions=state.functions+created)
                slots["curve_id"]=created.id
            }
            var ctx=c
            if(continuing&&slots["point_index"]!=null)ctx=ctx.copy(selectedPointIndex=slots.getValue("point_index").toInt().also{require(it in s.points.indices)})
            if(!literalExplanation)ctx.selectedPointIndex?.takeIf{it in s.points.indices}?.let{slots["point_index"]=it.toString()}
            if(continuing&&slots["curve_id"]!=null){val chosen=slots.getValue("curve_id");require(state.functions.any{it.visible&&it.id==chosen}){"The referenced curve is missing or hidden."};state=state.copy(labSessionValues=state.labSessionValues+(GraphAssistant.selectionKey(s.module) to chosen))}
            if(continuing){
                val graph=Regex("^Use derivative graph (\\S+)$").matchEntire(source);val x=Regex("^Use derivative x (.+)$").matchEntire(source)
                if(suppliedFormula)Unit
                else if(pending!!.kind=="derivative_graph"&&(graph!=null||named!=null)){val chosen=graph?.let{unid(it.groupValues[1])} ?: named!!.id;require(chosen in pending.targets&&s.functions.any{it.id==chosen&&it.visible});slots["curve_id"]=chosen;state=state.copy(labSessionValues=state.labSessionValues+(GraphAssistant.selectionKey(s.module) to chosen))}
                else if(pending.kind=="derivative_x"&&(x!=null||numeric!=null)){overrideX=(x?.groupValues?.get(1) ?: numeric!!.groupValues[1]).toDouble();require(overrideX.isFinite()&&abs(overrideX)<=10000){"Use a finite x coordinate within 10000 units."};slots["x"]=overrideX.toString()}
                else return ask("Choose a current curve or supply the requested x coordinate.",pending)
            }
            if(!literalExplanation)runCatching{selected(state,ctx)}.getOrNull()?.let{slots["curve_id"]=it.id}
            val p=parse(state,ctx,if(continuing)pending!!.source else source,overrideX);frame=frame.copy(parameters=p.parameters+slots["formula"]?.let{mapOf("expression" to RuhiDerivativeMath.derive(it).original)}.orEmpty());val r=resolve(state,p.query);val xs=evaluationXs(state,p.query)
            if(p.query.kind=="function"&&xs.isNotEmpty())try {
                xs.forEach{RuhiDerivativeMath.slope(r,it)}
            }catch(e:IllegalArgumentException){
                slots.remove("x")
                throw Missing(true,"${e.message} At which x coordinate should I evaluate the curve instead?")
            }
            val message=when {
                p.query.mode=="slider"->"Move the existing Trace x slider to change the tangent; the selected function remains unchanged."
                p.query.mode=="explain"->{val x=xs.single();val slope=RuhiDerivativeMath.absoluteValueSlope(x);if(slope==null)"abs(x) is not differentiable at zero: its left-hand slope is -1 and its right-hand slope is 1, so no single tangent slope exists."else "abs(x) is differentiable at x=$x: both one-sided slopes are $slope, so its derivative is $slope."}
                p.query.mode in setOf("stationary","critical")-> {val b=RuhiIntersectionLanguage.bounds(state);val critical=p.query.mode=="critical";val points=if(critical)RuhiDerivativeMath.critical(r,b[0],b[1])else RuhiDerivativeMath.stationary(r,b[0],b[1]);"${if(critical)"Critical"else"Stationary"} points in the visible x range: ${points.joinToString{"(${it.x}, ${it.y})"}.ifEmpty{"none"}}."+(if(critical)" Includes stationary points and verified real branch points; other domain boundaries require separate analysis."else"")}
                xs.isNotEmpty()->xs.joinToString("; "){x->val slope=RuhiDerivativeMath.slope(r,x);"x=$x: slope = ${slope.value}${if(p.query.mode in setOf("tangent","normal","tangents"))"; drawn ${p.query.mode}"else""}"}
                else->"Derivative${if(r.order==2)" (order 2)"else""}: ${r.derivative}. Defined where the original function and derivative are finite."
            }
            val key=if(p.query.pointIndex!=null)POINT_PREFIX+java.util.UUID.randomUUID().toString()else ""
            val query=p.query.copy(pointKey=key,pointIndex=null)
            val binding=if(key.isNotEmpty())mapOf(key to p.query.pointIndex.toString())else emptyMap()
            val next=state.copy(labSessionValues=state.labSessionValues.filterKeys{!it.startsWith(POINT_PREFIX)}+binding+(MARK to encode(query))+(if(p.query.kind=="function")mapOf(GraphAssistant.selectionKey(s.module) to p.query.source)else emptyMap()))
            val d=requireNotNull(drawing(next));require(d.diagnostic.isEmpty()){d.diagnostic}
            val committed=h.forModule(s.module).execute(s,GraphWorkspaceCommand(s,GraphWorkspaceCommand.reconcileDocument(s,next),"Ruhi: derivative analysis"))
            val cue=runCatching{RuhiLanguageServices.semanticClassifier?.predict(RuhiSeedNlpInput.normalize(source))}.getOrNull()
            return RuhiPointConversation.Outcome(RuhiExecutionResult(committed,GraphCommandStatus.APPLIED,message,RuhiCommand(engine=RuhiEngine.GRAPH_2D,operation=RuhiOperation.PointQuery(frame),modality=m,intentProposal=cue),c,semanticFrame=frame),null)
        }catch(e:Missing){
            val needsFormula=!e.point&&s.functions.none{it.visible}&&slots["formula"]==null
            val p=RuhiPending(if(e.point)"derivative_x"else if(needsFormula)"derivative_formula"else"derivative_graph",if(continuing)pending!!.source else source,s.module.name,scene(s),s.functions.filter{it.visible}.map{it.id},slots)
            return ask(if(needsFormula)"Which explicit function should I use? Give an equation such as y=x^2."else e.prompt,p)
        }
        catch(e:RuntimeException){return RuhiPointConversation.Outcome(RuhiExecutionResult(s,GraphCommandStatus.INVALID,e.message ?: "Invalid derivative request.",context=c,semanticFrame=frame),pending)}
    }
}
