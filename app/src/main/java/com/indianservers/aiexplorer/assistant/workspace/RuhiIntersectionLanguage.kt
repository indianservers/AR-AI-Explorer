package com.indianservers.aiexplorer.assistant.workspace

import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import java.io.*
import java.util.Base64
import java.util.Locale
import kotlin.math.*

/** Queries persist references, not cached answers. Canvas markers follow current curves, geometry and viewport. */
object RuhiIntersectionLanguage {
    const val MARK="ruhi.graph.intersection.v1"
    const val SELECTION="ruhi.graph.intersection.selection"
    const val ASPECT="Graph2D::ruhi.viewport.aspect"
    data class Ref(val kind:String,val value:String)
    data class Query(val refs:List<Ref>,val all:Boolean=false,val low:Double?=null,val high:Double?=null,val positive:Boolean=false,val label:String="I",val precision:Int=6,val mode:String="all",val near:Double=0.0)
    data class Parsed(val query:Query,val params:Map<String,Any>)
    data class Helper(val curve:RuhiIntersectionMath.Curve,val name:String)
    data class Drawing(val points:List<Vec2>,val labels:List<String>,val highlighted:Set<Int>,val helpers:List<Helper>,val functionIds:Set<String>,val infinite:Boolean=false,val diagnostic:String="")
    private class ContextMissing(message:String):IllegalArgumentException(message)
    private class Missing(val single:Boolean,val messageText:String):IllegalArgumentException(messageText)
    private data class Key(val record:String,val functions:List<FunctionDefinition>,val shapes:List<Shape2D>,val points:List<Vec2>,val view:Graph2DViewState,val aspect:Double)
    private val cache=object:LinkedHashMap<Key,Drawing>(8,.75f,true){override fun removeEldestEntry(e:MutableMap.MutableEntry<Key,Drawing>?)=size>8}
    private fun id(s:String)=Base64.getUrlEncoder().withoutPadding().encodeToString(s.toByteArray(Charsets.UTF_8))
    private fun unid(s:String)=String(Base64.getUrlDecoder().decode(s),Charsets.UTF_8)
    private fun ids(s:WorkspaceState)=s.labSessionValues[SELECTION]?.split(' ')?.mapNotNull{runCatching{unid(it)}.getOrNull()}.orEmpty()
    fun selected(s:WorkspaceState,c:RuhiSceneContext)=c.selectedFunctionIds.ifEmpty{ids(s)}.filter{id->s.functions.any{it.id==id&&it.visible}}.distinct()
    private val clears=setOf("clear intersections","clear intersection markers","clear all intersection markers","delete intersections","delete intersection markers","remove intersections","remove intersection markers")
    private fun instruction(source:String):String {
        var text=source.trim().trimEnd('.','?').replace(Regex("(?i)^ruhi[,:]?\\s+"),"")
        repeat(3){text=text.replace(Regex("(?i)^(?:please|can you|could you)\\s+"),"")};return text
    }
    fun accepts(source:String):Boolean {
        if(source.length>2000)return false
        val text=instruction(source).lowercase()
        if(text in clears||text=="clear curve pair selection"||Regex("^(?:select|use) curves? ").containsMatchIn(text))return true
        if(!Regex("^(?:where|find|mark|show|calculate|intersect|compute|do|does|give|count|label|which|highlight|display|draw|what|how)\\b").containsMatchIn(text))return false
        return Regex("\\b(?:intersect|intersects|intersection|intersections|cross|crossing|crossings|meet|roots|x[- ]intercepts?|solutions)\\b|touch.+y[- ]axis|^find x where ").containsMatchIn(text)
    }
    private fun scene(s:WorkspaceState)=java.security.MessageDigest.getInstance("SHA-256").digest((s.id+s.module+s.functions+s.shapes+s.points).toByteArray(Charsets.UTF_8)).joinToString(""){"%02x".format(it)}
    private fun chips(s:WorkspaceState,p:RuhiPending):List<RuhiAnswerChip> {
        val f=s.functions.filter{it.visible&&it.id in p.targets}
        fun name(v:FunctionDefinition)=v.name+if(f.count{it.name==v.name}>1)" [${f.indexOf(v)+1}]"else""
        return if(p.kind=="intersection_single")f.take(5).map{RuhiAnswerChip(name(it),"Use intersection graph ${id(it.id)}")}else f.indices.flatMap{i->(i+1 until f.size).map{j->RuhiAnswerChip("${name(f[i])} + ${name(f[j])}","Use intersection pair ${id(f[i].id)} ${id(f[j].id)}")}}.take(5)
    }
    fun restored(s:WorkspaceState,p:RuhiPending)=RuhiDialogueView(RuhiDialogueAction.ASK_CLARIFICATION,chips(s,p),true)
    private fun encode(q:Query):String {val bytes=ByteArrayOutputStream();DataOutputStream(bytes).use{o->o.writeInt(1);o.writeInt(q.refs.size);q.refs.forEach{o.writeUTF(it.kind);o.writeUTF(it.value)};o.writeBoolean(q.all);o.writeBoolean(q.low!=null);if(q.low!=null){o.writeDouble(q.low);o.writeDouble(requireNotNull(q.high))};o.writeBoolean(q.positive);o.writeUTF(q.label);o.writeInt(q.precision);o.writeUTF(q.mode);o.writeDouble(q.near)};return Base64.getEncoder().encodeToString(bytes.toByteArray())}
    private fun decode(value:String):Query {require(value.length<=60000);return DataInputStream(ByteArrayInputStream(Base64.getDecoder().decode(value))).use{i->require(i.readInt()==1);val n=i.readInt();require(n in 2..24);val refs=List(n){Ref(i.readUTF(),i.readUTF())};val all=i.readBoolean();val range=if(i.readBoolean())i.readDouble() to i.readDouble()else null;Query(refs,all,range?.first,range?.second,i.readBoolean(),i.readUTF(),i.readInt().also{require(it in 0..10)},i.readUTF(),i.readDouble())}}
    fun bounds(s:WorkspaceState):List<Double> {
        val view=s.graph2DView;val aspect=s.labSessionValues[ASPECT]?.toDoubleOrNull()?.takeIf{it.isFinite()&&it in .1..10.0} ?: 2.0
        val half=7.0/view.zoom;val x0=view.centerX-half;val x1=view.centerX+half;val y0=view.centerY-half*aspect;val y1=view.centerY+half*aspect
        val out=listOf(if(view.xLogarithmic)10.0.pow(x0)else x0,if(view.xLogarithmic)10.0.pow(x1)else x1,if(view.yLogarithmic)10.0.pow(y0)else y0,if(view.yLogarithmic)10.0.pow(y1)else y1)
        require(out.all{it.isFinite()&&abs(it)<=10000}&&out[0]<out[1]&&out[2]<out[3]){"Zoom to a finite graph viewport within 10000 units."};return out
    }
    private fun resolve(s:WorkspaceState,ref:Ref):RuhiIntersectionMath.Curve=when(ref.kind) {
        "literal"->RuhiIntersectionMath.native(ref.value)
        "axis"->if(ref.value=="x")RuhiIntersectionMath.Curve.Scalar("0")else RuhiIntersectionMath.Curve.Vertical(0.0)
        "function"->{val f=s.functions.singleOrNull{it.id==ref.value&&it.visible} ?: throw IllegalArgumentException("A referenced curve is missing or hidden.");RuhiIntersectionMath.native(f.expression)}
        "shape"->{val shape=s.shapes.singleOrNull{it.id==ref.value&&it.visible} ?: throw IllegalArgumentException("A referenced construction is missing or hidden.");if(RuhiCircleLanguage.isCircle(shape)){val circle=RuhiCircleLanguage.describe(s,shape);RuhiIntersectionMath.Curve.Circle(circle.center,circle.radius)}else{require(shape.pointIndices.size==2);RuhiIntersectionMath.Curve.Line(s.points[shape.pointIndices[0]],s.points[shape.pointIndices[1]])}}
        else->throw IllegalArgumentException("Unknown intersection reference.")
    }
    fun drawing(s:WorkspaceState):Drawing? {
        val record=s.labSessionValues[MARK] ?: return null;val aspect=s.labSessionValues[ASPECT]?.toDoubleOrNull() ?: 2.0;val key=Key(record,s.functions,s.shapes,s.points,s.graph2DView,aspect)
        synchronized(cache){cache[key]?.let{return it}}
        val result=runCatching{compute(s,decode(record))}.getOrElse{Drawing(emptyList(),emptyList(),emptySet(),emptyList(),emptySet(),diagnostic=it.message ?: "Intersection references are no longer valid.")}
        synchronized(cache){cache[key]=result};return result
    }
    fun compute(s:WorkspaceState,q:Query):Drawing {
        require(q.label.length in 1..32&&q.precision in 0..10&&q.near.isFinite()){ "Invalid intersection annotation settings." }
        val bounds=bounds(s);val low=q.low ?: bounds[0];val high=q.high ?: bounds[1];require(low<high&&low.isFinite()&&high.isFinite()&&abs(low)<=10000&&abs(high)<=10000)
        val refs=if(q.all)s.functions.filter{it.visible}.map{Ref("function",it.id)}else q.refs;require(refs.size in 2..24){"Choose at least two current visible curves."}
        val curves=refs.map{resolve(s,it)};val points=mutableListOf<Vec2>();var infinite=false
        for(i in curves.indices)for(j in i+1 until curves.size){val solution=RuhiIntersectionMath.solve(curves[i],curves[j],low,high,bounds[2],bounds[3]);infinite=infinite||solution.infinite;points+=solution.points}
        val all=points.filter{it.x>=bounds[0]-1e-8&&it.x<=bounds[1]+1e-8&&it.y>=bounds[2]-1e-8&&it.y<=bounds[3]+1e-8&&(!q.positive||it.x>1e-8)}.sortedWith(compareBy<Vec2>{it.x}.thenBy{it.y}).fold(mutableListOf<Vec2>()){out,p->if(out.none{it.distanceTo(p)<1e-6})out+=p;out}
        require(all.size<=256){"More than 256 crossings were found. Narrow the viewport before marking them."}
        val metric:((Vec2)->Double)?=when(q.mode){"closest"->{p->hypot(p.x,p.y)};"near"->{p->abs(p.x-q.near)};else->null}
        val minimum=metric?.let{f->all.minOfOrNull(f)};val highlighted=if(metric==null||minimum==null)emptySet()else all.indices.filter{abs(metric(all[it])-minimum)<1e-7}.toSet()
        val helpers=refs.zip(curves).mapNotNull{(ref,curve)->if(ref.kind=="shape"||ref.kind=="literal")Helper(curve,if(ref.kind=="shape")s.shapes.first{it.id==ref.value}.name else ref.value)else null}
        return Drawing(all,all.indices.map{"${q.label}${it+1}"},highlighted,helpers,refs.filter{it.kind=="function"}.map{it.value}.toSet(),infinite)
    }
    private class Parser(val s:WorkspaceState,val c:RuhiSceneContext) {
        val functions=s.functions.filter{it.visible}
        fun named(name:String)=functions.singleOrNull{it.name.equals(name,true)||it.name.substringBefore('(').equals(name.removeSuffix("(x)"),true)} ?: throw ContextMissing("Choose a uniquely named current curve for $name.")
        fun pair():List<Ref> {if(functions.size<2)throw ContextMissing("Plot at least two curves first.");val chosen=selected(s,c);val primary=s.labSessionValues[GraphAssistant.selectionKey(MathModule.Graph2D)];if(chosen.size!=2||(primary!=null&&primary !in chosen))throw Missing(false,"Which two curves should I use?");return chosen.map{Ref("function",it)}}
        fun single():Ref {if(functions.isEmpty())throw ContextMissing("Plot a graph first.");val selected=s.labSessionValues[GraphAssistant.selectionKey(MathModule.Graph2D)]?.let{id->functions.singleOrNull{it.id==id}} ?: selected(s,c).singleOrNull()?.let{id->functions.singleOrNull{it.id==id}} ?: functions.singleOrNull() ?: throw Missing(true,"Which graph should I use?");return Ref("function",selected.id)}
        fun expression(v:String)=RuhiFunctionLanguage.expression(v)
        fun literal(v:String)=Ref("literal","y=${expression(v)}")
        fun parse(source:String):Parsed {
            val text=MathExpressionNormalizer.normalize(instruction(source)).replace(Regex("(?i)\\s+(?:meet|intersect|cross)$"),"");val low=text.lowercase();val counting=low.startsWith("count ")||low.startsWith("how many ");val label="[\\p{L}_][\\p{L}\\p{N}_]{0,63}"
            val circleCenters=Regex("(?i)circles centered at ($label) and ($label)").find(text)
            if(circleCenters!=null){val names=circleCenters.groupValues.drop(1);val refs=names.map{name->val center=RuhiEndpointResolver(s,c).point(name).position;val shape=s.shapes.filter{it.visible&&RuhiCircleLanguage.isCircle(it)&&runCatching{RuhiCircleLanguage.describe(s,it).center.distanceTo(center)<1e-9}.getOrDefault(false)}.singleOrNull() ?: throw ContextMissing("Which existing circle centered at $name should I use? Create it first if it is missing.");Ref("shape",shape.id)};return Parsed(Query(refs),mapOf("targets" to "circles_centered_${names[0]}_${names[1]}"))}
            val lineCircle=Regex("(?i)line ($label) with circle ($label)").find(text)
            if(lineCircle!=null){val line=s.shapes.singleOrNull{it.visible&&it.name.equals(lineCircle.groupValues[1],true)&&it.pointIndices.size==2&&it.type in setOf(Shape2DType.Line,Shape2DType.Segment,Shape2DType.Ray)} ?: throw ContextMissing("Create or uniquely name line ${lineCircle.groupValues[1]} first.");val circle=s.shapes.singleOrNull{it.visible&&it.name.equals(lineCircle.groupValues[2],true)&&RuhiCircleLanguage.isCircle(it)} ?: throw ContextMissing("Create or uniquely name circle ${lineCircle.groupValues[2]} first.");return Parsed(Query(listOf(Ref("shape",line.id),Ref("shape",circle.id))),mapOf("targets" to lineCircle.groupValues.drop(1)))}
            val equationPair=Regex("(?i)\\by\\s*=\\s*(.+?)\\s+(?:and|with)\\s+y\\s*=\\s*(.+)$").find(text)
            if(equationPair!=null){val bodies=equationPair.groupValues.drop(1).map(::expression);return Parsed(Query(bodies.map{Ref("literal","y=$it")}),mapOf("functions" to bodies))}
            val vertical=Regex("(?i)\\bx\\s*=\\s*(.+?)\\s+with\\s+y\\s*=\\s*(.+)$").find(text)
            if(vertical!=null){val bodies=listOf("x=${expression(vertical.groupValues[1])}","y=${expression(vertical.groupValues[2])}");return Parsed(Query(bodies.map{Ref("literal",it)}),mapOf("equations" to bodies))}
            val root=Regex("(?i)roots? of y\\s*=\\s*(.+)$").find(text)
            if(root!=null){val expression=expression(root.groupValues[1]);return Parsed(Query(listOf(Ref("literal","y=$expression"),Ref("axis","x"))),mapOf("expression" to expression,"with" to "x_axis"))}
            val solutions=Regex("(?i)^find solutions to (.+?)\\s*=\\s*(.+?) within (.+?) to (.+)$").matchEntire(text)
            if(solutions!=null){val a=expression(solutions.groupValues[1]);val b=expression(solutions.groupValues[2]);val bounds=solutions.groupValues.drop(3).map(::expression);val values=bounds.map{ExpressionEngine().compile(it).eval(emptyMap())};return Parsed(Query(listOf(Ref("literal","y=$a"),Ref("literal","y=$b")),low=values[0],high=values[1]),mapOf("equation" to "$a=$b","domain" to bounds.map{it.toDoubleOrNull() ?: it}))}
            val equality=Regex("(?i)^find x where (.+?)\\s*(?:equals|=)\\s*(.+)$").matchEntire(text)
            if(equality!=null){val bodies=equality.groupValues.drop(1).map(::expression);return Parsed(Query(bodies.map{Ref("literal","y=$it")}),mapOf("functions" to bodies))}
            val trig=Regex("(?i)\\b(sine|cosine|tangent|sin|cos|tan) and (sine|cosine|tangent|sin|cos|tan)\\b").find(text)
            if(trig!=null){val expressions=trig.groupValues.drop(1).map{when(it.lowercase()){ "sine","sin"->"sin(x)";"cosine","cos"->"cos(x)";else->"tan(x)"}};return Parsed(Query(expressions.map{Ref("literal","y=$it")}),mapOf("functions" to expressions))}
            if(low.contains("red line")&&low.contains("blue curve")){val refs=listOf("red","blue").map{color->val f=functions.singleOrNull{it.colorKey.equals(color,true)||it.colorKey.equals(if(color=="red")"#FF0000"else"#0000FF",true)} ?: throw ContextMissing("Which uniquely colored $color curve should I use?");Ref("function",f.id)};return Parsed(Query(refs),mapOf("targets" to listOf("red_line","blue_curve")))}
            if(Regex("x[- ]intercepts?|y[- ]axis").containsMatchIn(low)){val axis=if(Regex("y[- ]axis").containsMatchIn(low))"y"else"x";return Parsed(Query(listOf(single(),Ref("axis",axis))),mapOf("target" to "selected_graph","with" to "${axis}_axis"))}
            val namedPair=Regex("(?i)^where do (.+?) and (.+)$").matchEntire(text)
            if(namedPair!=null){val names=namedPair.groupValues.drop(1);return Parsed(Query(names.map{Ref("function",named(it).id)}),mapOf("targets" to names))}
            var params:Map<String,Any>;var query:Query
            when {
                low.contains("two graphs")-> {query=Query(if(functions.size==2)functions.map{Ref("function",it.id)}else pair());params=mapOf("target" to "two_visible_graphs")}
                low.contains("viewport")-> {if(functions.size<2)throw ContextMissing("Plot at least two curves first.");query=Query(functions.map{Ref("function",it.id)},all=true);params=mapOf("target" to "all_current_graphs","scope" to "viewport")}
                counting||low.startsWith("label each ")||low.contains("closest to the origin")-> {if(functions.size<2)throw ContextMissing("Plot at least two curves first.");query=Query(functions.map{Ref("function",it.id)},all=true);params=mapOf("target" to "visible_curves")}
                else->{query=Query(pair());params=mapOf("target" to "selected_curves")}
            }
            if(low.contains("at all")){params=params+mapOf("existence_only" to true);query=query.copy(mode="existence")}
            if(Regex("\\b(?:four|4) decimals\\b").containsMatchIn(low)){params=params+mapOf("precision" to 4);query=query.copy(precision=4)}
            if(Regex("\\bpositive[- ]x\\b").containsMatchIn(low)){params=params+mapOf("domain" to "x>0");query=query.copy(positive=true)}
            if(counting){params=params+mapOf("count_only" to true);query=query.copy(mode="count")}
            if(low.startsWith("label each ")){val prefix=Regex("(?i)intersection ([\\p{L}_]+)1").find(text)?.groupValues?.get(1) ?: "I";params=params+mapOf("label_prefix" to prefix);query=query.copy(label=prefix)}
            if(low.contains("closest to the origin")){params=params+mapOf("select" to "closest_origin");query=query.copy(mode="closest")}
            val near=Regex("(?i)nearest (?:to )?x\\s*(?:=|equals)\\s*(.+)$").find(text)
            if(near!=null){val token=expression(near.groupValues[1]);val value=ExpressionEngine().compile(token).eval(emptyMap());require(value.isFinite());params=params+mapOf("select" to "near_x=$token");query=query.copy(mode="near",near=value)}
            return Parsed(query,params)
        }
    }
    fun execute(s:WorkspaceState,source:String,h:WorkspaceHistoryByModule,c:RuhiSceneContext,m:RuhiInputModality,pending:RuhiPending?):RuhiPointConversation.Outcome? {
        if(pending!=null&&pending.kind !in setOf("intersection_pair","intersection_single"))return null
        if(s.module!=MathModule.Graph2D)return null
        if(pending==null&&!accepts(source))return null
        if(pending==null&&instruction(source).lowercase() in setOf("show its roots","show the roots","show where they intersect"))return null
        val text=instruction(source)
        if(pending!=null&&!text.startsWith("Use intersection ")&&!Regex("(?i)^(?:select|use) curves? ").containsMatchIn(text)&&accepts(source)&&text.lowercase() !in setOf("resume","continue","resume pending"))return execute(s,source,h,c,m,null)
        if(pending!=null&&!accepts(source)&&RuhiMathLanguage.isStudioInstruction(source))return null
        var frame=RuhiSemanticFrame("FIND_INTERSECTION",RuhiEngine.GRAPH_2D,emptyMap())
        fun ask(message:String,p:RuhiPending?=null)=RuhiPointConversation.Outcome(RuhiExecutionResult(s,GraphCommandStatus.CLARIFICATION,message,context=c,dialogue=RuhiDialogueView(RuhiDialogueAction.ASK_CLARIFICATION,p?.let{chips(s,it)}.orEmpty(),p!=null),semanticFrame=frame),p)
        if(pending!=null&&(pending.module!=s.module.name||pending.scene!=scene(s)))return ask("The referenced scene changed. Ask the intersection question again.")
        if(pending!=null&&text.lowercase() in setOf("resume","continue","resume pending"))return ask(if(pending.kind=="intersection_single")"Which graph should I use?"else"Which two curves should I use?",pending)
        try {
            if(source.length>2000)return ask("Keep the request under 2000 characters.")
            if(text.lowercase() in clears){val next=s.copy(labSessionValues=s.labSessionValues-MARK-"ruhi.dialogue.graph.markers");return RuhiPointConversation.Outcome(RuhiExecutionResult(h.forModule(s.module).execute(s,GraphWorkspaceCommand(s,next,"Ruhi: clear intersection markers")),GraphCommandStatus.APPLIED,"Intersection markers cleared.",context=c),null)}
            val selection=Regex("(?i)^(?:select|use) curves? (.+?) and (.+)$").matchEntire(text)
            val encodedPair=Regex("^Use intersection pair (\\S+) (\\S+)$").matchEntire(text)
            val encodedSingle=Regex("^Use intersection graph (\\S+)$").matchEntire(text)
            var state=s;var context=c
            if(selection!=null||encodedPair!=null||encodedSingle!=null) {
                val chosen=selection?.groupValues?.drop(1)?.map{Parser(s,c).named(it).id} ?: (encodedPair ?: encodedSingle)!!.groupValues.drop(1).map(::unid)
                require(chosen.distinct().size==chosen.size&&chosen.all{id->s.functions.any{it.id==id&&it.visible}}){"Choose distinct current visible curves."}
                state=s.copy(labSessionValues=s.labSessionValues+(SELECTION to chosen.joinToString(" ",transform=::id))+(GraphAssistant.selectionKey(MathModule.Graph2D) to chosen.first()));context=c.copy(selectedFunctionIds=chosen)
                if(pending==null)return RuhiPointConversation.Outcome(RuhiExecutionResult(state,GraphCommandStatus.APPLIED,"Selected curves ${chosen.joinToString{value->s.functions.first{it.id==value}.name}}.",context=context),null)
            } else if(pending!=null)return ask("Choose a current graph suggestion.",pending)
            if(text.equals("clear curve pair selection",true))return RuhiPointConversation.Outcome(RuhiExecutionResult(s.copy(labSessionValues=s.labSessionValues-SELECTION),GraphCommandStatus.APPLIED,"Curve pair selection cleared.",context=c.copy(selectedFunctionIds=emptyList())),null)
            val parsed=Parser(state,context).parse(pending?.source ?: text);frame=frame.copy(parameters=parsed.params);val drawing=compute(state,parsed.query)
            val record=encode(parsed.query);val next=state.copy(labSessionValues=state.labSessionValues-"ruhi.dialogue.graph.markers"+(MARK to record));val committed=h.forModule(s.module).execute(s,GraphWorkspaceCommand(s,GraphWorkspaceCommand.reconcileDocument(s,next),"Ruhi: intersection analysis"))
            fun coordinate(v:Double)=String.format(Locale.US,"%.${parsed.query.precision}f",if(abs(v)<.5*10.0.pow(-parsed.query.precision))0.0 else v)
            val details=drawing.points.mapIndexed{i,point->"${drawing.labels[i]}=(${coordinate(point.x)}, ${coordinate(point.y)})"}
            val message=when {drawing.infinite->"The curves coincide, so their intersections are not an isolated finite list.";parsed.query.mode=="existence"->if(drawing.points.isEmpty())"No intersections in the visible domain."else"Yes. ${drawing.points.size} intersections in the visible domain.";parsed.query.mode=="count"->"Intersection count: ${drawing.points.size}.";else->"Intersections: ${details.joinToString("; ").ifEmpty{"none in the visible domain"}}.${if(drawing.highlighted.isNotEmpty())" Highlighted ${drawing.highlighted.joinToString{drawing.labels[it]}}."else""}"}
            val cue=runCatching{RuhiLanguageServices.semanticClassifier?.predict(RuhiSeedNlpInput.normalize(source))}.getOrNull()
            return RuhiPointConversation.Outcome(RuhiExecutionResult(committed,GraphCommandStatus.APPLIED,message,RuhiCommand(engine=RuhiEngine.GRAPH_2D,operation=RuhiOperation.PointQuery(frame),modality=m,intentProposal=cue),context,semanticFrame=frame),null)
        } catch(e:ContextMissing){return ask(e.message ?: "Choose the missing graph references.")}
        catch(e:RuhiReferenceQuestion){return ask(e.message ?: "Create the referenced point first.")}
        catch(e:Missing){val p=pending ?: RuhiPending(if(e.single)"intersection_single"else"intersection_pair",source,s.module.name,scene(s),s.functions.filter{it.visible}.map{it.id});return ask(e.messageText,p)}
        catch(e:RuntimeException){return RuhiPointConversation.Outcome(RuhiExecutionResult(s,GraphCommandStatus.INVALID,e.message ?: "Invalid intersection request.",context=c,semanticFrame=frame),pending)}
    }
}
