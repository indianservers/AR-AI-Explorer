package com.indianservers.aiexplorer.assistant.workspace

import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import java.io.*
import java.util.Base64
import kotlin.math.*

enum class RuhiDialogueAction { EXECUTE, ASK_CLARIFICATION, EXPLAIN, CANCEL_PENDING, RESUME_PENDING, REJECT_UNSUPPORTED }
data class RuhiAnswerChip(val label:String,val answer:String)
data class RuhiDialogueView(val action:RuhiDialogueAction,val suggestions:List<RuhiAnswerChip> = emptyList(),val pending:Boolean=false,val languageProposal:GraphIntentPrediction?=null)
data class RuhiDialogueTurn(val user:String,val assistant:String)
data class RuhiPending(val kind:String,val source:String,val module:String,val scene:String,val targets:List<String> = emptyList())
data class RuhiDialogueMemory(val workspace:String="",val module:String="",val pending:RuhiPending?=null,val lastCreated:String="",val lastModified:String="",val lastExpression:String="",val lastEdit:String="",val turns:List<RuhiDialogueTurn> = emptyList(),val lastScene:String="") {
    fun encode():String {
        val bytes=ByteArrayOutputStream()
        DataOutputStream(bytes).use { o->
            o.writeInt(2);listOf(workspace,module,lastCreated,lastModified,lastExpression,lastEdit,lastScene).forEach(o::writeUTF)
            o.writeBoolean(pending!=null);pending?.let { p->listOf(p.kind,p.source,p.module,p.scene).forEach(o::writeUTF);o.writeInt(p.targets.size);p.targets.forEach(o::writeUTF) }
            o.writeInt(turns.takeLast(24).size);turns.takeLast(24).forEach{ o.writeUTF(it.user.take(2000));o.writeUTF(it.assistant.take(2000)) }
        }
        return Base64.getEncoder().encodeToString(bytes.toByteArray())
    }
    companion object {
        const val KEY="ruhi.dialogue.v1"
        fun read(s:WorkspaceState):RuhiDialogueMemory = runCatching {
            val encoded=s.labSessionValues[KEY] ?: return RuhiDialogueMemory(s.id,s.module.name)
            require(encoded.length<=150000)
            DataInputStream(ByteArrayInputStream(Base64.getDecoder().decode(encoded))).use { i->
                val version=i.readInt();require(version in 1..2);val fields=List(if(version==1)6 else 7){i.readUTF()}
                val pending=if(i.readBoolean()) { val p=List(4){i.readUTF()};val n=i.readInt();require(n in 0..80);RuhiPending(p[0],p[1],p[2],p[3],List(n){i.readUTF()}) } else null
                val n=i.readInt();require(n in 0..24)
                RuhiDialogueMemory(fields[0],fields[1],pending,fields[2],fields[3],fields[4],fields[5],List(n){RuhiDialogueTurn(i.readUTF(),i.readUTF())},fields.getOrElse(6){""}).also { require(it.workspace==s.id) }
            }
        }.getOrElse { RuhiDialogueMemory(s.id,s.module.name) }
    }
}

/** Model cues are advisory. Required slots, targets, defaults and transactions are deterministic. */
class RuhiDialogueManager(private val studio:RuhiStudioAssistant=RuhiStudioAssistant()) {
    fun restoredView(s:WorkspaceState):RuhiDialogueView? {
        val p=RuhiDialogueMemory.read(s).pending ?: return null
        val chips=if(p.kind=="target") objects(s).filter{it.id in p.targets}.take(4).map{RuhiAnswerChip(it.label,it.label)} else if(p.kind=="compare") listOf(RuhiAnswerChip("sin and cos","sin and cos")) else listOf("y = x^2","y = sin(x)","y = 2*x + 3").map{RuhiAnswerChip(it,it)}
        return RuhiDialogueView(RuhiDialogueAction.ASK_CLARIFICATION,chips,true)
    }
    private data class ObjectRef(val id:String,val label:String,val context:RuhiSceneContext)
    private fun objects(s:WorkspaceState):List<ObjectRef> = when(s.module) {
        MathModule.Graph2D->s.functions.mapIndexed{i,f->ObjectRef(f.id,"Curve ${i+1}",RuhiSceneContext())}
        MathModule.Graph3D->s.surfaceLayers.mapIndexed{i,f->ObjectRef(f.id,"${s.labSessionValues[GraphAssistant.TYPE_PREFIX+f.id] ?: "Surface"} ${i+1}",RuhiSceneContext())}
        MathModule.Geometry2D->s.shapes.mapIndexed{i,f->ObjectRef(f.id,"${f.type.name} ${i+1}",RuhiSceneContext(selectedShapeId=f.id))}
        MathModule.Geometry3D->s.solids.mapIndexed{i,f->ObjectRef("solid-$i","${f.type.name} ${i+1}",RuhiSceneContext(selectedSolidIndex=i))}
        else->emptyList()
    }
    private fun scene(s:WorkspaceState)="${s.id}:${s.module}:${listOf(s.points,s.shapes,s.solids,s.functions,s.surfaceLayers).hashCode()}"
    fun accepts(s:WorkspaceState,source:String):Boolean {
        val t=runCatching{RuhiMathLanguage.normalize(source)}.getOrDefault("")
        if(t.startsWith("teach ") || t.startsWith("explain ") || t.startsWith("first explain ")) return RuhiDialogueMemory.read(s).pending!=null && t.contains("quadratic")
        return RuhiDialogueMemory.read(s).pending!=null || RuhiMathLanguage.isStudioInstruction(source) || Regex("^[yz]\\s*=").containsMatchIn(t) || Regex("^(?:compare|i want to compare|make another|increase it again|undo that|cancel|never mind|resume|continue|side length|and the|actually|what's its|what is its|now show|the base|show its roots|show where they intersect|equilateral|isosceles|right triangle)\\b").containsMatchIn(t)
    }
    fun execute(initial:WorkspaceState,source:String,histories:WorkspaceHistoryByModule,context:RuhiSceneContext=RuhiSceneContext(),modality:RuhiInputModality=RuhiInputModality.TEXT):RuhiExecutionResult {
        var memory=RuhiDialogueMemory.read(initial)
        if(memory.lastScene.isNotEmpty() && memory.lastScene!=scene(initial)) memory=memory.copy(lastCreated="",lastModified="")
        var text=runCatching{RuhiMathLanguage.normalize(source)}.getOrElse { return RuhiExecutionResult(initial,GraphCommandStatus.INVALID,it.message ?: "Invalid instruction.",context=context) }
        val cue=runCatching { RuhiLanguageServices.dialogueClassifier?.predict("${if(memory.pending==null) "idle" else "pending ${memory.pending.kind}"} $text") }.getOrNull()
        if(cue!=null && cue.confidence.isFinite() && cue.margin.isFinite() && cue.confidence>=.85f && cue.margin>=.35f) {
            if(cue.intent=="CANCEL_PENDING" && text in setOf("drop this request","abort this request")) text="cancel"
            if(cue.intent=="RESUME_PENDING" && text=="pick up where we left off") text="resume"
        }
        fun finish(result:RuhiExecutionResult,view:RuhiDialogueView=RuhiDialogueView(RuhiDialogueAction.EXECUTE)):RuhiExecutionResult {
            val before=objects(initial);val after=objects(result.state)
            val created=after.lastOrNull { a->before.none{it.id==a.id} }?.id ?: memory.lastCreated
            val selected=when(result.state.module){MathModule.Geometry2D->result.context.selectedShapeId;MathModule.Geometry3D->result.context.selectedSolidIndex?.let{"solid-$it"};else->result.state.labSessionValues[GraphAssistant.selectionKey(result.state.module)]}.orEmpty()
            memory=memory.copy(workspace=initial.id,module=result.state.module.name,lastCreated=created,lastModified=if(result.status==GraphCommandStatus.APPLIED && result.state!=initial) selected.ifEmpty{memory.lastModified} else memory.lastModified,turns=(memory.turns+RuhiDialogueTurn(source.take(2000),result.message)).takeLast(24),lastScene=scene(result.state))
            return result.copy(state=result.state.copy(labSessionValues=result.state.labSessionValues+(RuhiDialogueMemory.KEY to memory.encode())),dialogue=view.copy(languageProposal=cue))
        }
        fun reply(message:String,status:GraphCommandStatus=GraphCommandStatus.CLARIFICATION,action:RuhiDialogueAction=RuhiDialogueAction.ASK_CLARIFICATION,chips:List<RuhiAnswerChip> = emptyList())=finish(RuhiExecutionResult(initial,status,message,context=context),RuhiDialogueView(action,chips,memory.pending!=null))
        fun ask(kind:String,command:String,message:String,chips:List<RuhiAnswerChip>,targets:List<String> = emptyList()):RuhiExecutionResult {
            memory=memory.copy(pending=RuhiPending(kind,command,initial.module.name,scene(initial),targets))
            return reply(message,chips=chips.take(5))
        }
        val equationChips=listOf("y = x^2","y = sin(x)","y = 2*x + 3").map{RuhiAnswerChip(it,it)}
        if(text in setOf("cancel","cancel it","cancel pending","never mind","nevermind","forget it")) { memory=memory.copy(pending=null);return reply("Cancelled the pending request. The scene is unchanged.",GraphCommandStatus.APPLIED,RuhiDialogueAction.CANCEL_PENDING) }
        if(memory.pending!=null && text.contains("quadratic") && Regex("^(?:first )?(?:explain|teach)").containsMatchIn(text)) return reply("A quadratic function has the form y = ax² + bx + c, with a ≠ 0. Its graph is a parabola. Your comparison is still waiting for functions.",GraphCommandStatus.APPLIED,RuhiDialogueAction.EXPLAIN)
        var command=text
        var targets:List<String> = emptyList()
        var completingPending=false
        val pending=memory.pending
        if(pending!=null) {
            if(pending.module!=initial.module.name) { memory=memory.copy(pending=null);return reply("The studio changed. State the request for this studio again.") }
            if(text in setOf("resume","continue","resume pending")) return reply(if(pending.kind=="compare") "Which functions should I compare?" else if(pending.kind=="equation") "What would you like to graph?" else "Complete the pending request.",action=RuhiDialogueAction.RESUME_PENDING,chips=if(pending.kind in setOf("equation","compare")) equationChips else objects(initial).filter{it.id in pending.targets}.map{RuhiAnswerChip(it.label,it.label)})
            if(pending.kind=="target") {
                if(scene(initial)!=pending.scene) { memory=memory.copy(pending=null);return reply("The scene changed. State the action again so I can use the current objects.") }
                val candidates=objects(initial).filter{it.id in pending.targets}
                val chosen=when {
                    text in setOf("both","all","all of them")->candidates
                    else->candidates.filter { it.label.equals(text,true) || (it.label.substringBeforeLast(' ').equals(text,true) && candidates.count{r->r.label.substringBeforeLast(' ').equals(text,true)}==1) }
                }
                if(chosen.isEmpty()) return reply("Choose a current object or type its displayed name.",chips=candidates.take(4).map{RuhiAnswerChip(it.label,it.label)})
                targets=chosen.map{it.id};command=pending.source;memory=memory.copy(pending=null)
            } else if(pending.kind=="move") {
                if(scene(initial)!=pending.scene) { memory=memory.copy(pending=null);return reply("The scene changed. State the movement again for the current objects.") }
                val answer=Regex("([-+]?\\d+(?:\\.\\d+)?) units? (right|left|up|upward|down|downward)").matchEntire(text)
                if(answer==null) return reply("How far and in which direction? For example, 3 units right.",chips=listOf(RuhiAnswerChip("3 units right","3 units right"),RuhiAnswerChip("2 units up","2 units up")))
                targets=pending.targets;command="move the selected object ${answer.value}";memory=memory.copy(pending=null)
            } else if(pending.kind in setOf("equation","compare")) {
                if(!Regex("^(?:create|delete|move|rotate|undo|redo|make|clear|reset)\\b").containsMatchIn(text)) {
                    completingPending=true
                    command=if(pending.kind=="compare") "compare "+text.removePrefix("okay, ").removePrefix("compare ") else text
                    if(command in setOf("quadratic","linear","sine wave")) {
                        val examples=when(command){"quadratic"->listOf("y = x^2","y = x^2 - 4","y = 2*x^2");"linear"->listOf("y = x","y = 2*x + 3");else->listOf("y = sin(x)","y = 2*sin(x)")}
                        return ask(pending.kind,"${pending.source}; family=$command","Which $command function would you like?",examples.map{RuhiAnswerChip(it,it)})
                    }
                    if(command in setOf("custom","enter my equation","enter functions")) return reply("Type the equation${if(pending.kind=="compare") "s" else ""} you want to graph.")
                    if(pending.kind=="equation" && !command.contains('=')) {
                        if(!Regex("[x0-9()]").containsMatchIn(command)) return reply("Type a function, for example y = x^2.",chips=equationChips)
                        command="y = $command"
                    }
                    memory=memory.copy(pending=null)
                }
            }
        }
        if(command in setOf("draw a graph","plot a graph","draw graph","plot graph","i want a graph")) return ask("equation",command,"What would you like to graph?",equationChips)
        if(command in setOf("compare 2 functions","compare 2 graphs","i want to compare 2 graphs","i want to compare 2 functions")) return ask("compare",command,"Which functions should I compare?",listOf(RuhiAnswerChip("x and x²","x and x^2"),RuhiAnswerChip("sin and cos","sin and cos"),RuhiAnswerChip("Enter functions","Enter functions")))
        if(command.startsWith("compare ")) {
            val expressions=command.removePrefix("compare ").split(Regex("\\s+and\\s+"))
            if(expressions.size!=2) return ask("compare",command,"Give two functions separated by ‘and’.",listOf(RuhiAnswerChip("sin and cos","sin and cos")))
            fun expression(s:String)=s.removePrefix("y = ").removePrefix("y=").trim().let { if(it in setOf("sin","cos","tan")) "$it(x)" else it }
            command="in 2d graph plot y = ${expression(expressions[0])}; then plot y = ${expression(expressions[1])}"
        }
        if(Regex("^[yz]\\s*=").containsMatchIn(command)) command="in ${if(command.startsWith("z")) "3d" else "2d"} graph plot $command"
        if(command=="undo that") command="undo"
        if(command=="increase it again") command=memory.lastEdit.takeIf{it.startsWith("scale ")} ?: return reply("Which size change should I repeat?")
        if(command in setOf("make another one","make another 1","make another object","same as before")) command="duplicate the selected object"
        val refs=objects(initial)
        var selected=when(initial.module){MathModule.Geometry2D->context.selectedShapeId;MathModule.Geometry3D->context.selectedSolidIndex?.let{"solid-$it"};else->initial.labSessionValues[GraphAssistant.selectionKey(initial.module)]}
        val ordinal=Regex("\\b(first|second|previous|last) (?:object|one|cube|sphere|circle|curve)\\b").find(command)?.groupValues?.get(1)
        if(ordinal!=null) selected=when(ordinal){"first"->refs.firstOrNull()?.id;"second"->refs.getOrNull(1)?.id;"previous"->memory.lastCreated.takeIf(String::isNotEmpty);else->refs.lastOrNull()?.id}
        if(command.contains("red curve")) selected=initial.functions.singleOrNull{it.colorKey=="#FF0000"}?.id
        if(command.contains("larger circle")) {
            val radii=initial.shapes.filter{it.type==Shape2DType.Circle}.map{shape->val p=shape.pointIndices.map(initial.points::get);shape.id to p[0].distanceTo(p[1])}
            val max=radii.maxOfOrNull{it.second};selected=radii.filter{it.second==max}.singleOrNull()?.first
        }
        if(command.contains("object on the left")) {
            val positions=when(initial.module){MathModule.Geometry3D->initial.solids.mapIndexed{i,s->"solid-$i" to s.position.x};MathModule.Geometry2D->initial.shapes.map{shape->shape.id to shape.pointIndices.map{initial.points[it].x}.average()};MathModule.Graph3D->initial.surfaceLayers.map{it.id to it.graphTransform.position.x};else->emptyList()}
            val min=positions.minOfOrNull{it.second};selected=positions.filter{it.second==min}.singleOrNull()?.first
        }
        if(command.contains("graph we just created")) selected=memory.lastCreated.takeIf(String::isNotEmpty)
        val explicitReference=ordinal!=null || listOf("red curve","larger circle","object on the left","graph we just created").any(command::contains)
        if(explicitReference && selected==null) {
            if(refs.isEmpty()) { memory=memory.copy(pending=null);return reply("There are no matching objects in this scene. Create an object first.") }
            val canonical=command.replace(Regex("(?:the )?(?:red curve|larger circle|object on the left|graph we just created|(?:first|second|previous|last) (?:object|one))"),"it")
            return ask("target",canonical,"Which current object do you mean?",refs.take(4).map{RuhiAnswerChip(it.label,it.label)},refs.map{it.id})
        }
        if(Regex("^(?:make|color|scale|rotate|move) ").containsMatchIn(command) && Regex("\\b(?:both|all of them)\\b").containsMatchIn(command)) { targets=refs.map{it.id};command=command.replace(Regex("\\b(?:both|all of them)\\b"),"the selected object") }
        selected=selected?.takeIf{id->refs.any{it.id==id}} ?: memory.lastModified.takeIf{id->refs.any{it.id==id}} ?: refs.singleOrNull()?.id
        if(targets.isEmpty() && Regex("\\b(?:it|this|its|selected|that same object)\\b").containsMatchIn(command) && selected==null && refs.size>1) return ask("target",command,"Which object do you mean?",refs.take(4).map{RuhiAnswerChip(it.label,it.label)}+RuhiAnswerChip("Both","Both").takeIf{refs.size==2}.let{listOfNotNull(it)},refs.map{it.id})
        if(targets.isEmpty() && selected!=null) targets=listOf(selected)
        if(Regex("^make (?:the (?:last|first|second) object|the larger circle|the red curve|the object on the left) (bigger|smaller|red|blue|green)$").matches(command)) command=command.replace(Regex("^make .+ (bigger|smaller|red|blue|green)$"),"make it $1")
        if(Regex("^(?:move|shift|translate) (?:it|this|the selected object)$").matches(command)) return ask("move",command,"How far and in which direction?",listOf(RuhiAnswerChip("3 units right","3 units right"),RuhiAnswerChip("2 units up","2 units up")),targets)
        Regex("(?:create|draw) a cylinder with the same height as (?:that |the )?cone").matchEntire(command)?.let {
            val cones=initial.solids.withIndex().filter{it.value.type==SolidType.Cone}
            val cone=cones.firstOrNull{it.index==context.selectedSolidIndex} ?: cones.singleOrNull()
            if(cone==null) return reply("Select the cone whose height I should reuse.")
            command="create a cylinder with radius 1 and height ${cone.value.height}"
        }
        command=when(command) {
            "draw a circle","create a circle"->"create a circle with radius 1"
            "draw a square","create a square"->"create a square with side length 2"
            "draw a triangle","create a triangle"->"create an equilateral triangle with side length 2"
            "draw a rectangle","create a rectangle"->"create a rectangle measuring 3 by 2"
            "create a square pyramid","draw a square pyramid"->"create a square pyramid with base side 2 and height 2"
            "rotate it","rotate this","rotate the selected object"->"rotate the selected object 45 degrees"
            "make it bigger","make it larger"->"scale the selected object by 1.25"
            "make it smaller"->"scale the selected object by 0.8"
            "what's its volume","what is its volume","now show its volume"->"calculate the volume"
            "what's its area","show its area"->"calculate the area"
            else->command
        }
        if(initial.module in setOf(MathModule.Graph2D,MathModule.Graph3D) && Regex("^(?:draw|create) (?:an? )?(?:equilateral |right )?(?:circle|square|rectangle|triangle)\\b").containsMatchIn(command)) command="in 2d geometry $command"
        var stage=initial
        var last:RuhiExecutionResult?=null
        val privateHistories=WorkspaceHistoryByModule()
        val contexts=targets.ifEmpty{listOf("")}
        try {
            if(command in setOf("show its roots","show where they intersect")) {
                val analysis=RuhiDialogueMath.analyze(initial,command.contains("intersect"),targets.firstOrNull())
                stage=analysis.first;last=RuhiExecutionResult(stage,GraphCommandStatus.APPLIED,analysis.second,context=context)
            } else for(id in contexts) {
                val ref=objects(stage).firstOrNull{it.id==id}
                if(ref!=null && stage.module in setOf(MathModule.Graph2D,MathModule.Graph3D)) stage=stage.copy(labSessionValues=stage.labSessionValues+(GraphAssistant.selectionKey(stage.module) to id))
                val c=ref?.context ?: context
                val custom=RuhiDialogueMath.edit(stage,command,c)
                val result=custom?.let{RuhiExecutionResult(it.first,GraphCommandStatus.APPLIED,it.second,context=if(command=="duplicate the selected object")RuhiSceneContext(it.first.labSessionValues["ruhi.studio.selected.shape"],it.first.labSessionValues["ruhi.studio.selected.solid"]?.toIntOrNull()) else c)} ?: studio.execute(stage,command,if(command in setOf("undo","redo")) histories else privateHistories,c,modality)
                if(result.status!=GraphCommandStatus.APPLIED) {
                    if(completingPending) memory=memory.copy(pending=pending)
                    return finish(result.copy(state=initial),RuhiDialogueView(if(result.status==GraphCommandStatus.UNSUPPORTED) RuhiDialogueAction.REJECT_UNSUPPORTED else RuhiDialogueAction.ASK_CLARIFICATION,if(memory.pending?.kind=="equation")equationChips else emptyList(),memory.pending!=null))
                }
                stage=result.state;last=result
            }
            val result=requireNotNull(last)
            val committed=if(command in setOf("undo","redo") || stage==initial) stage else histories.forModule(stage.module).execute(initial.copy(module=stage.module),GraphWorkspaceCommand(initial.copy(module=stage.module),GraphWorkspaceCommand.reconcileDocument(initial,stage),"Ruhi dialogue: ${source.take(80)}"))
            memory=memory.copy(lastEdit=command.takeIf{it.startsWith("scale ")} ?: memory.lastEdit,lastExpression=if(committed.functions!=initial.functions) committed.functions.lastOrNull()?.expression.orEmpty() else memory.lastExpression)
            val optional=if(text in setOf("draw a triangle","create a triangle"))listOf(RuhiAnswerChip("Equilateral","Make it equilateral"),RuhiAnswerChip("Right triangle","Make it a right triangle"),RuhiAnswerChip("Isosceles","Make it isosceles")) else emptyList()
            return finish(result.copy(state=committed,message=result.message+if(text in setOf("rotate it","rotate this","rotate the selected object")) " Used a standard 45° rotation." else ""),RuhiDialogueView(RuhiDialogueAction.EXECUTE,optional,memory.pending!=null))
        } catch(error:RuntimeException) { if(completingPending) memory=memory.copy(pending=pending);return reply(error.message ?: "The request could not be validated.",GraphCommandStatus.INVALID,chips=if(memory.pending?.kind=="equation")equationChips else emptyList()) }
    }
}
