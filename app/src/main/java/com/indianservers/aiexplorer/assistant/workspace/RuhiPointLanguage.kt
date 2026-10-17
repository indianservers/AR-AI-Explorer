package com.indianservers.aiexplorer.assistant.workspace

import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import kotlin.math.abs

data class RuhiSemanticFrame(val intent:String,val engine:RuhiEngine,val parameters:Map<String,Any>)
data class RuhiPointSlot(val role:String,val value:String,val start:Int,val end:Int,val confidence:Float,val margin:Float)
data class RuhiDistanceContext(val module:String,val first:String,val second:String)
fun interface RuhiPointSlotClassifier {fun predict(source:String):List<RuhiPointSlot>}

/** Entity values come from the utterance and scene, never a point-name vocabulary. */
object RuhiPointLanguage {
    data class Measurement(val first:String,val second:String,val delta:Vec3,val distance:Double)
    private fun positions(s:WorkspaceState,name:String):List<Vec3> {
        val actual=if(s.module==MathModule.Geometry3D)s.points3D.filter{it.name.equals(name,true)}.map{it.position}
        else s.labSessionValues.filterKeys{it.startsWith("ruhi.studio.point.")&&it.removePrefix("ruhi.studio.point.").equals(name,true)}.values.mapNotNull(String::toIntOrNull).filter{it in s.points.indices}.distinct().map{s.points[it].let{p->Vec3(p.x,p.y,0.0)}}
        return if(actual.isEmpty()&&name.equals("origin",true))listOf(Vec3(0.0,0.0,0.0))else actual
    }
    fun current(s:WorkspaceState,reference:RuhiDistanceContext):Measurement? {
        if(reference.module!=s.module.name||s.module !in setOf(MathModule.Geometry2D,MathModule.Geometry3D))return null
        val a=positions(s,reference.first).singleOrNull() ?: return null;val b=positions(s,reference.second).singleOrNull() ?: return null
        if(listOf(a.x,a.y,a.z,b.x,b.y,b.z).any{!it.isFinite()||abs(it)>10000})return null
        val delta=b-a;return Measurement(reference.first,reference.second,delta,delta.magnitude())
    }
    fun distanceLabel(s:WorkspaceState,m:Measurement)="Distance ${m.first} to ${m.second} = ${GeometryMeasurementUnits.read(s).text(m.distance,1)}"
    private val label="[\\p{L}_][\\p{L}\\p{N}_]{0,31}"
    fun availableNames(s:WorkspaceState):List<String> {
        val names=when(s.module) {
            MathModule.Geometry3D->s.points3D.map{it.name}
            MathModule.Geometry2D->s.labSessionValues.keys.filter{it.startsWith("ruhi.studio.point.")}.map{it.removePrefix("ruhi.studio.point.")}
            else->emptyList()
        }
        return names.distinct().filter{it.matches(Regex(label))&&positions(s,it).singleOrNull()?.let{p->listOf(p.x,p.y,p.z).all{v->v.isFinite()&&abs(v)<=10000}}==true}
    }
    private val end="(?![-\\p{L}\\p{N}_])"
    private val patterns=listOf(
        Regex("(?i)\\b(?:distance|separation|length) (?:from|between) (?:the )?(?:points? )?($label) (?:to|and) (?:the )?(?:point )?($label)$end"),
        Regex("(?i)\\bhow far is (?:the )?(?:point )?($label) from (?:the )?(?:point )?($label)$end"),
        Regex("(?i)\\bhow far are (?:the )?(?:points? )?($label) and (?:the )?(?:point )?($label) apart\\b"),
    )
    private fun pairMatch(source:String):MatchResult?=patterns.firstNotNullOfOrNull{it.find(source)}
    fun pair(source:String):Pair<String,String>?=pairMatch(source)?.let{it.groupValues[1] to it.groupValues[2]}
    private val betweenReferences=Regex("(?i)\\bbetween (?:the )?(?:points? )?($label) and (?:the )?(?:point )?($label)$end")
    fun endpointRanges(source:String):List<IntRange> = (pairMatch(source) ?: betweenReferences.find(source))?.let{m->listOfNotNull(m.groups[1]?.range,m.groups[2]?.range)}.orEmpty()
    fun maskEndpoints(source:String):String {
        var text=source
        endpointRanges(source).sortedByDescending{it.first}.forEach{text=text.replaceRange(it,"entity")}
        return text
    }
    private fun understoodPair(source:String):Pair<String,String>? {
        if(Regex("(?i)(?:-| )axis\\b|(?:-| )plane\\b|\\b(?:this|that|selected) (?:line|solid|sphere|cube)\\b").containsMatchIn(source))return null
        pair(source)?.let{return it}
        val cue=runCatching{RuhiLanguageServices.semanticClassifier?.predict(RuhiSeedNlpInput.normalize(source))}.getOrNull() ?: return null
        if(cue.intent!="MEASURE_DISTANCE"||cue.confidence<.9f||cue.margin<.4f)return null
        val slots=runCatching{RuhiLanguageServices.pointSlotClassifier?.predict(source)}.getOrNull().orEmpty().filter{it.confidence>=.9f&&it.margin>=.4f&&it.start>=0&&it.end<=source.length&&it.start<it.end&&source.substring(it.start,it.end)==it.value&&it.value.matches(Regex(label))}
        val a=slots.singleOrNull{it.role=="POINT_1"} ?: return null;val b=slots.singleOrNull{it.role=="POINT_2"} ?: return null
        if(a.start>=b.start)return null
        return a.value to b.value
    }
    private val creation=Regex("(?i)^(?:please )?(?:create|add|draw|put) (?:a )?(?:3d )?point (?:called )?($label)(?: at(?: (.*))?)?[.?]?$" )
    fun accepts(source:String)=source.length<=2000 && (creation.matches(source.trim())||understoodPair(source)!=null)
    fun create(s:WorkspaceState,source:String,h:WorkspaceHistoryByModule,c:RuhiSceneContext,modality:RuhiInputModality):RuhiExecutionResult? {
        val clean=source.trim().replace(Regex("(?i)^ruhi[,:]?\\s+"),"").replace(Regex("(?i)^(?:please|can you|could you|kindly)\\s+"),"")
        val match=creation.matchEntire(clean) ?: return null
        val engine=RuhiEngine.from(s.module)?.takeIf{it in setOf(RuhiEngine.GEOMETRY_2D,RuhiEngine.GEOMETRY_3D)} ?: return RuhiExecutionResult(s,GraphCommandStatus.CLARIFICATION,"Open 2D or 3D Geometry to create the point.",context=c)
        val name=match.groupValues[1];val raw=match.groupValues[2].trim().trimEnd('.','?')
        val frame=RuhiSemanticFrame("CREATE_POINT",engine,mapOf("name" to name))
        if(raw.isBlank())return RuhiExecutionResult(s,GraphCommandStatus.CLARIFICATION,"Where should point $name go? Give ${if(engine==RuhiEngine.GEOMETRY_3D)"three" else "two"} coordinates.",context=c,semanticFrame=frame)
        return try {
            require(raw.startsWith('(')&&raw.endsWith(')')){"Write coordinates in parentheses."}
            val values=raw.substring(1,raw.length-1).split(',').map{it.trim().toDoubleOrNull() ?: throw IllegalArgumentException("Use numeric coordinates.")}
            require(values.size==if(engine==RuhiEngine.GEOMETRY_3D)3 else 2){"Provide the studio's coordinate count."};require(values.all{it.isFinite()&&abs(it)<=10000}){"Keep coordinates finite and within 10000 units."}
            val next=if(engine==RuhiEngine.GEOMETRY_3D) {
                require(s.points3D.size<200 && s.points3D.none{it.name.equals(name,true)}){"That name is already used, or the point budget is full."}
                s.copy(points3D=s.points3D+Point3D("ruhi-point-${java.util.UUID.randomUUID()}",name,Vec3(values[0],values[1],values[2])))
            } else {
                require(s.points.size<200 && s.labSessionValues.keys.none{it.startsWith("ruhi.studio.point.")&&it.removePrefix("ruhi.studio.point.").equals(name,true)}){"That name is already used, or the point budget is full."}
                s.copy(points=s.points+Vec2(values[0],values[1]),labSessionValues=s.labSessionValues+("ruhi.studio.point.$name" to s.points.size.toString()))
            }
            val detailed=frame.copy(parameters=frame.parameters+("coordinates" to values))
            val committed=h.forModule(s.module).execute(s,GraphWorkspaceCommand(s,GraphWorkspaceCommand.reconcileDocument(s,next),"Ruhi: create point $name"))
            RuhiExecutionResult(committed,GraphCommandStatus.APPLIED,"Created point $name at (${values.joinToString()}).",RuhiCommand(engine=engine,operation=RuhiOperation.PointQuery(detailed),modality=modality),c,semanticFrame=detailed)
        } catch(e:IllegalArgumentException){RuhiExecutionResult(s,GraphCommandStatus.INVALID,e.message ?: "Invalid coordinates.",context=c,semanticFrame=frame)}
    }
    fun execute(s:WorkspaceState,source:String,c:RuhiSceneContext,modality:RuhiInputModality):RuhiExecutionResult? {
        val pair=understoodPair(source) ?: return null
        val engine=RuhiEngine.from(s.module)
        if(engine !in setOf(RuhiEngine.GEOMETRY_2D,RuhiEngine.GEOMETRY_3D))return RuhiExecutionResult(s,GraphCommandStatus.CLARIFICATION,"Open 2D or 3D Geometry to measure these scene points.",context=c)
        val frame=RuhiSemanticFrame("MEASURE_DISTANCE",engine!!,mapOf("point1" to pair.first,"point2" to pair.second))
        val cue=runCatching{RuhiLanguageServices.semanticClassifier?.predict(RuhiSeedNlpInput.normalize(source))}.getOrNull()
        val missing=mutableListOf<String>();val ambiguous=mutableListOf<String>()
        fun resolve(name:String):Vec3? {
            val points=positions(s,name)
            if(points.size!=1){if(points.isEmpty())missing+=name else ambiguous+=name;return null}
            return points.single()
        }
        val a=resolve(pair.first);val b=resolve(pair.second)
        val command=RuhiCommand(engine=engine,operation=RuhiOperation.PointQuery(frame),modality=modality,intentProposal=cue)
        if(a==null||b==null) {
            if(ambiguous.isNotEmpty())return RuhiExecutionResult(s,GraphCommandStatus.CLARIFICATION,"More than one point matches ${ambiguous.distinct().joinToString()}. Give the conflicting points unique names first.",command,c,semanticFrame=frame)
            val chips=missing.distinct().take(2).map{RuhiAnswerChip("Create point $it","Create point $it at ")}
            return RuhiExecutionResult(s,GraphCommandStatus.CLARIFICATION,"Point ${missing.distinct().joinToString(" and ")} is missing or ambiguous. Create it with coordinates, or choose a uniquely named point.",command,c,RuhiDialogueView(RuhiDialogueAction.ASK_CLARIFICATION,chips,languageProposal=cue),frame)
        }
        if(listOf(a.x,a.y,a.z,b.x,b.y,b.z).any{!it.isFinite()||abs(it)>10000})return RuhiExecutionResult(s,GraphCommandStatus.INVALID,"Use finite scene coordinates within 10000 units.",command,c,semanticFrame=frame)
        val delta=b-a;val distance=delta.magnitude()
        val unit=GeometryMeasurementUnits.read(s)
        val root="√((${delta.x})² + (${delta.y})²${if(s.module==MathModule.Geometry3D)" + (${delta.z})²" else ""})"
        val formula=if(unit.known)" Native-coordinate formula: $root ${unit.base!!.symbol}."+(if(unit.base!=unit.display)" 1 ${unit.base.symbol} = ${unit.number(1.0,1){it.toString()}} ${unit.display!!.symbol}."else"")else" $root."
        val measured=frame.copy(parameters=frame.parameters+mapOf("distance_native" to distance,"distance_display" to distance*unit.factor(1),"coordinate_unit" to (unit.base?.symbol ?: "unit"),"display_units" to (unit.display?.symbol ?: "unit")))
        return RuhiExecutionResult(s,GraphCommandStatus.APPLIED,distanceLabel(s,Measurement(pair.first,pair.second,delta,distance))+"."+formula,command.copy(operation=RuhiOperation.PointQuery(measured)),c,RuhiDialogueView(RuhiDialogueAction.EXECUTE,languageProposal=cue),measured)
    }
}

object RuhiPointConversation {
    data class Outcome(val result:RuhiExecutionResult,val pending:RuhiPending?)
    private fun queryText(p:RuhiPending):String {
        val a=p.slots["resolved_first"] ?: p.slots["point1"] ?: return p.source
        val b=p.slots["resolved_second"] ?: p.slots["point2"] ?: return p.source
        return "Distance between $a and $b"
    }
    private fun choices(result:RuhiExecutionResult,p:RuhiPending):Outcome {
        val create=result.dialogue?.suggestions.orEmpty().filter{it.label.startsWith("Create point ")}
        val missing=create.map{it.label.removePrefix("Create point ")}
        val slots=if(p.slots.containsKey("point1"))p.slots else result.semanticFrame?.parameters.orEmpty().filterKeys{it in setOf("point1","point2")}.mapValues{it.value.toString()}
        val next=p.copy(targets=missing,slots=slots)
        val used=listOf(slots["resolved_first"] ?: slots["point1"],slots["resolved_second"] ?: slots["point2"]).filterNotNull().filter{it !in missing}
        val role=missing.firstOrNull()
        val existing=if(role==null||!slots.keys.containsAll(listOf("point1","point2")))emptyList()else RuhiPointLanguage.availableNames(result.state).filter{candidate->used.none{it.equals(candidate,true)}}.take(5-create.size).map{RuhiAnswerChip("Use $it for $role","Use $it for $role")}
        return Outcome(result.copy(dialogue=(result.dialogue ?: RuhiDialogueView(RuhiDialogueAction.ASK_CLARIFICATION)).copy(suggestions=create+existing,pending=true)),next)
    }
    fun execute(s:WorkspaceState,source:String,h:WorkspaceHistoryByModule,c:RuhiSceneContext,modality:RuhiInputModality,pending:RuhiPending?):Outcome? {
        if(pending!=null&&pending.kind !in setOf("point_pair","point_create"))return null
        if(source.trim().lowercase() in setOf("cancel","cancel that","resume","continue"))return null
        if(pending!=null&&pending.module!=s.module.name)return Outcome(RuhiExecutionResult(s,GraphCommandStatus.CLARIFICATION,"The studio changed. Ask the distance question again.",context=c),null)
        val staging=WorkspaceHistoryByModule()
        fun query(state:WorkspaceState,text:String)=RuhiPointLanguage.execute(state,text,c,modality) ?: RuhiGeometry2DLanguage.execute(state,text,staging,c,modality)
        if(pending?.kind=="point_pair") {
            val use=Regex("(?i)^use ([\\p{L}_][\\p{L}\\p{N}_]{0,31}) for ([\\p{L}_][\\p{L}\\p{N}_]{0,31})[.?]?$").matchEntire(source.trim())
            if(use!=null) {
                val refreshed=query(s,queryText(pending)) ?: return null
                if(refreshed.status!=GraphCommandStatus.CLARIFICATION)return Outcome(refreshed,null)
                val base=choices(refreshed,pending)
                val name=RuhiPointLanguage.availableNames(s).singleOrNull{it.equals(use.groupValues[1],true)}
                val role=base.pending!!.targets.singleOrNull{it.equals(use.groupValues[2],true)}
                if(name==null||role==null)return base.copy(result=base.result.copy(message="Choose a current uniquely named point for a missing endpoint."))
                val bindings=buildMap {if(role.equals(base.pending.slots["point1"],true))put("resolved_first",name);if(role.equals(base.pending.slots["point2"],true))put("resolved_second",name)}
                val bound=base.pending.copy(slots=base.pending.slots+bindings)
                val measured=query(s,queryText(bound)) ?: return null
                if(measured.status==GraphCommandStatus.APPLIED)return Outcome(measured.copy(message="Using $name for $role. "+measured.message),null)
                return choices(measured,bound)
            }
            if(source.trim().equals("choose point",true)) {
                val measured=query(s,queryText(pending)) ?: return null
                if(measured.status==GraphCommandStatus.APPLIED)return Outcome(measured,null)
                return choices(measured,pending)
            }
        }
        fun commit(result:RuhiExecutionResult):RuhiExecutionResult {
            if(result.status!=GraphCommandStatus.APPLIED||result.state==s)return result
            return result.copy(state=h.forModule(s.module).execute(s,GraphWorkspaceCommand(s,GraphWorkspaceCommand.reconcileDocument(s,result.state),"Ruhi point conversation: ${source.take(80)}")))
        }
        val command=if(pending!=null&&source.trim().startsWith('(')) "Create point ${pending.targets.firstOrNull() ?: return null} at ${source.trim()}" else source
        val creation=RuhiPointLanguage.create(s,command,staging,c,modality)
        if(creation!=null) {
            if(creation.status!=GraphCommandStatus.APPLIED) {
                val name=creation.semanticFrame?.parameters?.get("name") as? String
                val next=pending?.copy(targets=listOfNotNull(name)+pending.targets.filter{it!=name}) ?: name?.let{RuhiPending("point_create",source,s.module.name,"",listOf(it))}
                val examples=if(s.module==MathModule.Geometry3D)listOf("(0,0,0)","(3,4,0)") else listOf("(0,0)","(3,4)")
                return Outcome(creation.copy(dialogue=RuhiDialogueView(RuhiDialogueAction.ASK_CLARIFICATION,examples.map{RuhiAnswerChip(it,it)},next!=null)),next)
            }
            if(pending==null||pending.kind=="point_create")return Outcome(commit(creation),null)
            val measured=query(creation.state,queryText(pending)) ?: return Outcome(commit(creation),pending)
            if(measured.status==GraphCommandStatus.APPLIED)return Outcome(commit(measured.copy(message=creation.message+" "+measured.message,dialogue=measured.dialogue?.copy(pending=false))),null)
            // Creating a requested point succeeded even when another endpoint is still needed.
            return choices(commit(measured.copy(status=GraphCommandStatus.APPLIED,message=creation.message+" "+measured.message,dialogue=measured.dialogue?.copy(pending=true))),pending)
        }
        val measured=query(s,source) ?: return null
        if(measured.status==GraphCommandStatus.CLARIFICATION && measured.semanticFrame!=null && measured.dialogue?.suggestions?.isNotEmpty()==true) {
            return choices(measured,RuhiPending("point_pair",source,s.module.name,""))
        }
        return Outcome(commit(measured),if(measured.status==GraphCommandStatus.APPLIED)null else pending)
    }
}

/** Same feature normalization is used for TensorFlow training and Android inference. */
object RuhiSeedNlpInput {
    private val words=setOf("a","an","i","find","what","calculate","measure","show","draw","create","make","plot","graph","compute","tell","give","work","please","can","could","kindly","how","where","is","are","does","do","which","construct","place","mark","bisect","split","locate","put","add","slope","angle","circle","triangle","volume","integrate","differentiate","distance","use","no","actually","both","just","cancel","continue","explain","compare","start","now","why","choose","rotate","move","spin","turn","scale","double","slide","change","reset","duplicate","stretch","orbit","zoom","explode","undo","redo","hide","label","thicken","highlight","clear","delete","rename","save","center","remove","restore","render","display","visualize","sketch","evaluate","shade","estimate","check","label","verify","keep","replace","build","circle","point","line","segment")
    fun normalize(source:String):String {
        var t=RuhiPointLanguage.maskEndpoints(source)
        t=t.replace('’','\'').replace("²","^2").replace("³","^3").replace("⁴","^4").replace("π","pi").replace("θ","theta").replace("∠","angle ").replace("∫","integral ")
        t=Regex("\\b[A-Z][A-Za-z0-9_]*\\b").replace(t){m->if(m.value.length>1&&!m.value.matches(Regex("[A-Z][0-9]+"))&&m.value.lowercase() in words)m.value else "entity"}
        return t.lowercase(java.util.Locale.US).replace(Regex("\\s+")," ").trim().trimEnd('.','?')
    }
}

object RuhiPointSlotInput {
    private val lexical=("distance from to between and how far is are apart tell me give compute calculate find measure work out please can you could kindly what the straight line euclidean separation length now in units point points midpoint middle halfway bisect bisecting center segment of on along for where mark show construct put locate add label as draw selected blue last without showing full a at coordinates into two equal segments marker divide equally drawing i just drew ? . -").split(' ').toSet()
    data class Token(val value:String,val start:Int,val end:Int,val features:String)
    fun tokens(source:String):List<Token> {
        val matches=Regex("[\\p{L}_][\\p{L}\\p{N}_]*|[-+]?\\d+(?:\\.\\d+)?|[^\\s\\p{L}\\p{N}_]").findAll(source).toList()
        val endpoints=RuhiPointLanguage.endpointRanges(source)
        fun word(i:Int):String {
            val match=matches.getOrNull(i) ?: return "boundary"
            if(endpoints.any{match.range.first>=it.first&&match.range.last<=it.last})return "entity"
            val value=match.value.lowercase(java.util.Locale.US)
            return if(value.length==1&&value[0].isLetter())"entity"else if(value in lexical)value else "entity"
        }
        return matches.mapIndexed{i,m->Token(m.value,m.range.first,m.range.last+1,"current ${word(i)} before ${word(i-1)} after ${word(i+1)} earlier ${word(i-2)} later ${word(i+2)}")}
    }
}
