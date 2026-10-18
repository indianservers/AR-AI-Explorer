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
data class RuhiPending(val kind:String,val source:String,val module:String,val scene:String,val targets:List<String> = emptyList(),val slots:Map<String,String> = emptyMap())
data class RuhiDialogueMemory(val workspace:String="",val module:String="",val pending:RuhiPending?=null,val lastCreated:String="",val lastModified:String="",val lastExpression:String="",val lastEdit:String="",val turns:List<RuhiDialogueTurn> = emptyList(),val lastScene:String="",val lastError:String="",val lastChoices:List<RuhiAnswerChip> = emptyList(),val roleValues:Map<String,Double> = emptyMap(),val graphTimeline:List<String> = emptyList(),val pendingPrompt:String="",val distanceContexts:Map<String,RuhiDistanceContext> = emptyMap()) {
    fun encode():String {
        val bytes=ByteArrayOutputStream()
        DataOutputStream(bytes).use { o->
            o.writeInt(7);listOf(workspace,module,lastCreated,lastModified,lastExpression,lastEdit,lastScene).forEach(o::writeUTF)
            o.writeUTF(lastError.take(2000));o.writeInt(lastChoices.take(5).size);lastChoices.take(5).forEach{o.writeUTF(it.label.take(2000));o.writeUTF(it.answer.take(4000))}
            o.writeInt(roleValues.size);roleValues.forEach{(k,v)->o.writeUTF(k);o.writeDouble(v)}
            require(graphTimeline.size<=1024);o.writeInt(graphTimeline.size);graphTimeline.forEach(o::writeUTF);o.writeUTF(pendingPrompt.take(2000))
            require(distanceContexts.size<=2);o.writeInt(distanceContexts.size);distanceContexts.values.forEach{listOf(it.module,it.first,it.second).forEach(o::writeUTF)}
            o.writeBoolean(pending!=null);pending?.let { p->listOf(p.kind,p.source,p.module,p.scene).forEach(o::writeUTF);o.writeInt(p.targets.size);p.targets.forEach(o::writeUTF);require(p.slots.size<=16);o.writeInt(p.slots.size);p.slots.forEach{(k,v)->require(k.length in 1..64&&v.length<=2000);o.writeUTF(k);o.writeUTF(v)} }
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
                val version=i.readInt();require(version in 1..7);val fields=List(if(version==1)6 else 7){i.readUTF()}
                val lastError=if(version>=3)i.readUTF() else ""
                val lastChoices=if(version>=3){val count=i.readInt();require(count in 0..5);List(count){RuhiAnswerChip(i.readUTF(),i.readUTF())}}else emptyList()
                val roleValues=if(version>=4){val count=i.readInt();require(count in 0..64);buildMap{repeat(count){val k=i.readUTF();val v=i.readDouble();require(v.isFinite());put(k,v)}}}else emptyMap()
                val graphTimeline=if(version>=5){val count=i.readInt();require(count in 0..1024);List(count){i.readUTF()}}else emptyList()
                val pendingPrompt=if(version>=5)i.readUTF() else ""
                val distanceContexts=if(version>=6){val count=i.readInt();require(count in 0..2);buildMap{repeat(count){val reference=RuhiDistanceContext(i.readUTF(),i.readUTF(),i.readUTF());require(reference.module in setOf("Geometry2D","Geometry3D")&&reference.first.length in 1..128&&reference.second.length in 1..128);put(reference.module,reference)}}}else emptyMap()
                val pending=if(i.readBoolean()) { val p=List(4){i.readUTF()};val n=i.readInt();require(n in 0..80);val targets=List(n){i.readUTF()};val slots=if(version>=7){val count=i.readInt();require(count in 0..16);buildMap{repeat(count){val k=i.readUTF();val v=i.readUTF();require(k.length in 1..64&&v.length<=2000);put(k,v)}}}else emptyMap();RuhiPending(p[0],p[1],p[2],p[3],targets,slots) } else null
                val n=i.readInt();require(n in 0..24)
                RuhiDialogueMemory(fields[0],fields[1],pending,fields[2],fields[3],fields[4],fields[5],List(n){RuhiDialogueTurn(i.readUTF(),i.readUTF())},fields.getOrElse(6){""},lastError,lastChoices,roleValues,graphTimeline,pendingPrompt,distanceContexts).also { require(it.workspace==s.id) }
            }
        }.getOrElse { RuhiDialogueMemory(s.id,s.module.name) }
    }
}

/** Model cues are advisory. Required slots, targets, defaults and transactions are deterministic. */
class RuhiDialogueManager(private val studio:RuhiStudioAssistant=RuhiStudioAssistant()) {
    fun recordPreparationFailure(s:WorkspaceState,user:String,message:String):WorkspaceState {
        val saved=RuhiDialogueMemory.read(s)
        val updated=saved.copy(lastError=message.take(2000),turns=(saved.turns+RuhiDialogueTurn(user.take(2000),message.take(2000))).takeLast(24))
        return s.copy(labSessionValues=s.labSessionValues+(RuhiDialogueMemory.KEY to updated.encode()))
    }

    fun restoredView(s:WorkspaceState):RuhiDialogueView? {
        val saved=RuhiDialogueMemory.read(s)
        val p=saved.pending ?: return null
        if(p.kind in setOf("endpoint_reference","midpoint_label","endpoint_shape"))return RuhiEndpointConversation.restored(s,p)
        if(p.kind=="measurement_units")return RuhiMeasurementUnitLanguage.restored()
        if(p.kind=="preserve_cube")return RuhiPreserveCubeLanguage.restored(s,p)
        if(saved.lastChoices.isNotEmpty())return RuhiDialogueView(RuhiDialogueAction.ASK_CLARIFICATION,saved.lastChoices,true)
        if(p.kind in setOf("construction_context","construction_place"))return RuhiConstructionConversation.restored(s,p)
        if(p.kind=="context_graph_compare")return RuhiContextualGraphLanguage.restored(s,p)
        if(p.kind=="context_angle")return RuhiContextualAngleLanguage.restored(s,p)
        if(p.kind in setOf("intersection_pair","intersection_single"))return RuhiIntersectionLanguage.restored(s,p)
        if(p.kind in setOf("derivative_graph","derivative_x","derivative_formula"))return RuhiDerivativeLanguage.restored(s,p)
        if(p.kind in setOf("integral_graph","integral_bounds","integral_formula"))return RuhiIntegralLanguage.restored(s,p)
        if(p.kind=="scene_edit")return RuhiSceneEditLanguage.restored(s,p)
        if(p.kind in setOf("transform_solid","transform_pair"))return RuhiTransform3DLanguage.restored(s,p)
        if(p.kind in setOf("solid_measure","solid_measure_dimension"))return RuhiSolidMeasureLanguage.restored(s,p)
        if(p.kind=="spatial_plane_pair")return RuhiSpatialPlaneLanguage.restored(s,p)
        if(p.kind=="spatial_vector_pair")return RuhiSpatialAngleLanguage.restored(s,p)
        if(p.kind=="angle_vertex")return RuhiDialogueView(RuhiDialogueAction.ASK_CLARIFICATION,p.targets.map{RuhiAnswerChip("Vertex $it","Use vertex $it")},true)
        if(p.kind in setOf("point_pair","point_create"))return RuhiDialogueView(RuhiDialogueAction.ASK_CLARIFICATION,p.targets.take(3).map{RuhiAnswerChip("Create point $it","Create point $it at ")},true)
        val chips=if(p.kind=="geo_axis") listOf("X","Y","Z").map{RuhiAnswerChip(it,it)} else if(p.kind=="geo_scale") listOf("1.5","2").map{RuhiAnswerChip(it,it)} else if(p.kind=="target") objects(s).filter{it.id in p.targets}.take(4).map{RuhiAnswerChip(it.label,it.label)} else if(p.kind=="compare") listOf(RuhiAnswerChip("sin and cos","sin and cos")) else listOf("y = x^2","y = sin(x)","y = 2*x + 3").map{RuhiAnswerChip(it,it)}
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
    private fun scene(s:WorkspaceState)="${s.id}:${s.module}:${listOf(s.points,s.shapes,s.solids,s.points3D,s.planes3D,s.vectors3D,s.functions,s.surfaceLayers).hashCode()}"
    private data class ColorTarget(val answer:String?=null,val message:String="",val chips:List<RuhiAnswerChip> = emptyList(),val stale:Boolean=false)
    private fun blueTarget(s:WorkspaceState):ColorTarget {
        val memory=RuhiDialogueMemory.read(s);val pending=memory.pending
            ?: return ColorTarget(message="Which action should I apply to the blue object?")
        if(pending.module!=s.module.name||memory.lastScene!=scene(s))return ColorTarget(message="The scene changed. State the action again for the current objects.",stale=true)
        fun blue(color:String?)=color?.lowercase() in setOf("blue","#0000ff","#ff0000ff")
        fun colored(ref:String):Boolean=when {
            ref.startsWith("s:")->blue(s.shapes.firstOrNull{it.id==ref.drop(2)}?.styleKey)
            ref.startsWith("f:")->blue(s.functions.firstOrNull{it.id==ref.drop(2)}?.colorKey)
            ref.startsWith("m:")->blue(s.labSessionValues[RuhiSceneEditLanguage.PREFIX+"color.${ref.drop(2)}"])
            ref.startsWith("o:")->blue(s.labSessionValues["ruhi.studio.appearance.${ref.drop(2)}"])
            else->false
        }
        val choices=when(pending.kind) {
            "scene_edit"->pending.targets.filter(::colored).map{ref->RuhiAnswerChip(s.shapes.firstOrNull{ref=="s:${it.id}"}?.name ?: s.functions.firstOrNull{ref=="f:${it.id}"}?.name ?: ref,"Use scene object "+Base64.getEncoder().encodeToString(ref.toByteArray(Charsets.UTF_8)))}
            "transform_solid"->pending.targets.mapNotNull{it.toIntOrNull()}.filter{colored("o:$it")}.map{i->RuhiAnswerChip("${s.solids[i].type} ${i+1}","Use transform solid $i")}
            "target"->objects(s).filter{it.id in pending.targets}.filter{r->when(s.module){MathModule.Geometry2D->colored("s:${r.id}");MathModule.Geometry3D->colored("o:${r.id.removePrefix("solid-")}");MathModule.Graph2D->colored("f:${r.id}");MathModule.Graph3D->colored("m:${r.id}");else->false}}.map{RuhiAnswerChip(it.label,it.label)}
            else->return ColorTarget(message="Which pending answer should refer to the blue object?",chips=restoredView(s)?.suggestions.orEmpty())
        }
        return if(choices.size==1)ColorTarget(answer=choices.single().answer)else ColorTarget(message=if(choices.isEmpty())"No blue object matches the pending action. Which object should I use?"else"More than one blue object matches. Which one should I use?",chips=if(choices.isEmpty())restoredView(s)?.suggestions.orEmpty()else choices.take(5))
    }
    fun accepts(s:WorkspaceState,source:String):Boolean {
        if(RuhiAppliedTargetCorrection.accepts(source))return true
        if(RuhiMeasurementUnitLanguage.accepts(source))return true
        if(RuhiPreserveCubeLanguage.accepts(source))return true
        if(RuhiActionContext.accepts(source))return true
        if(s.module in setOf(MathModule.Geometry2D,MathModule.Geometry3D)&&RuhiConstructionConversation.accepts(source))return true
        if(s.module==MathModule.Graph2D&&RuhiContextualGraphLanguage.accepts(source))return true
        if(s.module==MathModule.Geometry2D&&RuhiContextualAngleLanguage.accepts(source))return true
        if(RuhiDialogueFollowUp.command(source,RuhiDialogueMemory.read(s).pending,restoredView(s),RuhiDialogueMemory.read(s).roleValues,s.module.name)!=null||RuhiDialogueFollowUp.asksOptions(source)||RuhiDialogueFollowUp.asksError(source))return true
        if(RuhiSceneEditLanguage.accepts(source))return true
        if(s.module==MathModule.Geometry3D&&RuhiTransform3DLanguage.accepts(source))return true
        if(s.module==MathModule.Geometry3D&&RuhiSolidMeasureLanguage.accepts(source))return true
        if(s.module==MathModule.Graph3D&&RuhiSurfaceLanguage.accepts(source))return true
        if(s.module==MathModule.Graph2D&&(RuhiIntersectionLanguage.accepts(source)||RuhiDerivativeLanguage.accepts(source)||RuhiIntegralLanguage.accepts(source)))return true
        if(s.module==MathModule.Graph2D&&RuhiFunctionLanguage.accepts(source))return true
        if(s.module==MathModule.Geometry3D&&RuhiSpatialPlaneLanguage.accepts(source))return true
        if(s.module==MathModule.Geometry3D&&RuhiSpatialAngleLanguage.accepts(source))return true
        if(s.module==MathModule.Geometry2D&&RuhiGeometry2DLanguage.accepts(source))return true
        if(RuhiPointLanguage.accepts(source))return true
        if(s.module==MathModule.Geometry3D && RuhiGeometry3DLanguage.accepts(source)) return true
        val t=runCatching{RuhiMathLanguage.normalize(source)}.getOrDefault("")
        if(t.startsWith("teach ") || t.startsWith("explain ") || t.startsWith("first explain ")) return RuhiDialogueMemory.read(s).pending!=null && t.contains("quadratic")
        return RuhiDialogueMemory.read(s).pending!=null || RuhiMathLanguage.isStudioInstruction(source) || Regex("^[yz]\\s*=").containsMatchIn(t) || Regex("^(?:compare|i want to compare|make another|increase it again|undo that|cancel|never mind|resume|continue|side length|and the|actually|what's its|what is its|now show|the base|show its roots|show where they intersect|equilateral|isosceles|right triangle)\\b").containsMatchIn(t)
    }
    fun execute(initial:WorkspaceState,source:String,histories:WorkspaceHistoryByModule,context:RuhiSceneContext=RuhiSceneContext(),modality:RuhiInputModality=RuhiInputModality.TEXT):RuhiExecutionResult = RuhiDialogueSemantics.annotate(source,executeCore(initial,source,histories,context,modality))
    private fun executeCore(initial:WorkspaceState,source:String,histories:WorkspaceHistoryByModule,context:RuhiSceneContext,modality:RuhiInputModality):RuhiExecutionResult {
        val colorTarget=if(runCatching{RuhiMathLanguage.normalize(source)}.getOrDefault("").replace(",","")=="actually i meant the blue 1")blueTarget(initial)else null
        val other=if(RuhiActionContext.accepts(source))RuhiActionContext.resolve(initial,source,context,RuhiDialogueMemory.read(initial).pending)else null
        val mapped=other?.command ?: colorTarget?.answer ?: RuhiDialogueFollowUp.command(source,RuhiDialogueMemory.read(initial).pending,restoredView(initial),RuhiDialogueMemory.read(initial).roleValues,initial.module.name)
        if(mapped!=null&&mapped!=source) {
            val savedContext=RuhiDialogueMemory.read(initial)
            val pronoun=runCatching{RuhiMathLanguage.normalize(source)}.getOrDefault("") in setOf("make it larger","rotate that")
            val available=objects(initial)
            val explicit=when(initial.module){MathModule.Geometry2D->context.selectedShapeId?.takeIf{id->available.any{it.id==id}};MathModule.Geometry3D->context.selectedSolidIndex?.takeIf{it in initial.solids.indices}?.let{"solid-$it"};else->null}
            val remembered=if(pronoun&&explicit==null&&savedContext.module==initial.module.name&&(initial.module!=MathModule.Geometry3D||savedContext.lastScene==scene(initial)))listOf(savedContext.lastModified,savedContext.lastCreated).firstNotNullOfOrNull{id->available.firstOrNull{it.id==id}}else null
            val resolvedContext=other?.context ?: remembered?.context ?: context
            val ready=if(other!=null)initial.copy(labSessionValues=initial.labSessionValues+(RuhiDialogueMemory.KEY to savedContext.copy(pending=null,lastChoices=emptyList(),pendingPrompt="").encode()))else initial
            val result=execute(ready,mapped,histories,resolvedContext,modality)
            val saved=RuhiDialogueMemory.read(result.state)
            val turns=saved.turns.dropLast(1)+saved.turns.takeLast(1).map{it.copy(user=source.take(2000))}
            return result.copy(state=result.state.copy(labSessionValues=result.state.labSessionValues+(RuhiDialogueMemory.KEY to saved.copy(turns=turns).encode())))
        }
        val visibleSource=RuhiDialogueMemory.read(initial).lastChoices.firstOrNull{it.answer==source}?.label ?: source
        var memory=RuhiDialogueMemory.read(initial)
        if(memory.lastScene.isNotEmpty() && memory.lastScene!=scene(initial)) {
            val stable=if(initial.module in setOf(MathModule.Geometry2D,MathModule.Graph2D,MathModule.Graph3D))objects(initial).map{it.id}.toSet()else emptySet()
            memory=memory.copy(lastCreated=memory.lastCreated.takeIf{it in stable}.orEmpty(),lastModified=memory.lastModified.takeIf{it in stable}.orEmpty())
        }
        var text=runCatching{RuhiMathLanguage.normalize(source)}.getOrElse { return RuhiExecutionResult(initial,GraphCommandStatus.INVALID,it.message ?: "Invalid instruction.",context=context) }
        if(text=="cancel that")text="cancel"
        if(text=="continue where we stopped")text="resume"
        val cue=runCatching { RuhiLanguageServices.dialogueClassifier?.predict("${if(memory.pending==null) "idle" else "pending ${memory.pending.kind}"} $text") }.getOrNull()
        if(cue!=null && cue.confidence.isFinite() && cue.margin.isFinite() && cue.confidence>=.85f && cue.margin>=.35f) {
            if(cue.intent=="CANCEL_PENDING" && text in setOf("drop this request","abort this request")) text="cancel"
            if(cue.intent=="RESUME_PENDING" && text=="pick up where we left off") text="resume"
        }
        fun finish(input:RuhiExecutionResult,view:RuhiDialogueView=RuhiDialogueView(RuhiDialogueAction.EXECUTE)):RuhiExecutionResult {
            val recorded=input.copy(state=RuhiActionContext.record(initial,input))
            val result=recorded.copy(state=RuhiAppliedTargetCorrection.record(initial,recorded))
            val before=objects(initial);val after=objects(result.state)
            val created=after.lastOrNull { a->before.none{it.id==a.id} }?.id ?: memory.lastCreated.takeIf{id->after.any{it.id==id}}.orEmpty()
            val selected=when(result.state.module){MathModule.Geometry2D->result.context.selectedShapeId;MathModule.Geometry3D->result.context.selectedSolidIndex?.let{"solid-$it"};else->result.state.labSessionValues[GraphAssistant.selectionKey(result.state.module)]}.takeIf{id->after.any{it.id==id}}.orEmpty()
            val bound=result.semanticFrame?.parameters?.get("resolved_object_id") as? String
            val referenced=bound?.let{if(it.startsWith("s:"))it.drop(2)else if(it.startsWith("o:"))"solid-${it.drop(2)}"else it}?.takeIf{id->after.any{it.id==id}}
            val rememberedModified=memory.lastModified.takeIf{id->after.any{it.id==id}}.orEmpty()
            val learned=linkedMapOf<String,Double>()
            if(result.status==GraphCommandStatus.APPLIED) {
                result.semanticFrame?.parameters?.forEach{(key,value)->if(key in setOf("radius","height","side","scale")&&value is Number&&value.toDouble().isFinite())learned["${result.state.module.name}:$key"]=value.toDouble()}
                val index=result.context.selectedSolidIndex
                if(result.state.module==MathModule.Geometry3D&&index!=null&&index in result.state.solids.indices) {
                    val solid=result.state.solids[index]
                    if(solid.type in setOf(SolidType.Sphere,SolidType.Hemisphere,SolidType.Cylinder,SolidType.Cone,SolidType.Frustum)&&initial.solids.getOrNull(index)?.radius!=solid.radius)learned["${result.state.module.name}:radius"]=solid.radius
                }
            }
            val additions=if(result.status==GraphCommandStatus.APPLIED)result.state.functions.filter{f->initial.functions.none{it.id==f.id}}.map{it.id}else emptyList()
            val timeline=(memory.graphTimeline+additions).distinct()
            val liveIds=result.state.functions.map{it.id}.toSet()
            val keep=liveIds+timeline.filter{it !in liveIds}.takeLast(200)
            val prompt=if(memory.pending==null)"" else if(memory.pending!=RuhiDialogueMemory.read(initial).pending||memory.pendingPrompt.isBlank())result.message else memory.pendingPrompt
            if(result.status==GraphCommandStatus.APPLIED&&result.semanticFrame?.intent=="MEASURE_DISTANCE") {
                val p=result.semanticFrame.parameters;val a=p["point1"] as? String;val b=p["point2"] as? String
                if(a!=null&&b!=null)memory=memory.copy(distanceContexts=memory.distanceContexts+(result.state.module.name to RuhiDistanceContext(result.state.module.name,a,b)))
            }
            memory=memory.copy(graphTimeline=timeline.filter{it in keep},pendingPrompt=prompt,roleValues=memory.roleValues+learned,workspace=initial.id,module=result.state.module.name,lastCreated=created,lastModified=if(result.status==GraphCommandStatus.APPLIED && (result.state!=initial||referenced!=null)) referenced ?: selected.ifEmpty{rememberedModified} else rememberedModified,turns=(memory.turns+RuhiDialogueTurn(visibleSource.take(2000),result.message)).takeLast(24),lastScene=scene(result.state),lastError=if(result.status in setOf(GraphCommandStatus.INVALID,GraphCommandStatus.UNSUPPORTED))result.message else memory.lastError,lastChoices=if(memory.pending==null)emptyList() else if(view.suggestions.isNotEmpty())view.suggestions.take(5) else if(memory.pending==RuhiDialogueMemory.read(initial).pending)memory.lastChoices else emptyList())
            return result.copy(state=result.state.copy(labSessionValues=result.state.labSessionValues+(RuhiDialogueMemory.KEY to memory.encode())),dialogue=view.copy(languageProposal=view.languageProposal ?: cue))
        }
        fun reply(message:String,status:GraphCommandStatus=GraphCommandStatus.CLARIFICATION,action:RuhiDialogueAction=RuhiDialogueAction.ASK_CLARIFICATION,chips:List<RuhiAnswerChip> = emptyList())=finish(RuhiExecutionResult(initial,status,message,context=context),RuhiDialogueView(action,chips,memory.pending!=null))
        fun ask(kind:String,command:String,message:String,chips:List<RuhiAnswerChip>,targets:List<String> = emptyList(),pendingScene:String=scene(initial)):RuhiExecutionResult {
            memory=memory.copy(pending=RuhiPending(kind,command,initial.module.name,pendingScene,targets))
            return reply(message,chips=chips.take(5))
        }
        if(other!=null)return if(other.targets.isNotEmpty())ask("other_object",source,other.message,other.choices,other.targets,RuhiActionContext.sceneGuard(initial))else reply(other.message)
        if(RuhiAppliedTargetCorrection.accepts(source)||colorTarget!=null&&(memory.pending==null||memory.pending?.kind=="applied_target")) {
            val correction=RuhiAppliedTargetCorrection.execute(initial,source,histories,context,modality,memory)
            memory=memory.copy(pending=correction.pending)
            return finish(correction.result,correction.result.dialogue ?: RuhiDialogueView(RuhiDialogueAction.EXECUTE))
        }
        if(colorTarget!=null) {
            if(colorTarget.stale)memory=memory.copy(pending=null)
            return reply(colorTarget.message,chips=colorTarget.chips)
        }
        if(text=="use the same radius as before"&&memory.pending?.kind=="solid_measure_dimension"&&memory.pending?.targets?.firstOrNull()=="radius")return reply("What radius should I use? There is no previous validated radius for this workspace.",chips=restoredView(initial)?.suggestions.orEmpty())
        if(text in setOf("resume","continue","resume pending")&&memory.pending!=null) {
            if(memory.pending!!.module!=initial.module.name||memory.lastScene.isNotEmpty()&&memory.lastScene!=scene(initial)||!RuhiMeasurementUnitLanguage.current(initial,memory.pending!!)) {
                memory=memory.copy(pending=null)
                return reply("The workspace or scene changed. State the request again for the current scene.")
            }
            val prompt=memory.pendingPrompt.ifBlank{when(memory.pending!!.kind){"equation"->"What would you like to graph?";"compare"->"Which functions should I compare?";"geo_axis"->"Which axis should I use?";"geo_scale"->"Which positive scale factor should I use?";else->"Which answer should I use for your pending request?"}}
            return reply(prompt,action=RuhiDialogueAction.RESUME_PENDING,chips=restoredView(initial)?.suggestions.orEmpty())
        }
        if(RuhiDialogueFollowUp.asksOptions(source)) {
            val view=restoredView(initial)
            return if(view!=null)reply("Choose an answer for the pending request.",chips=view.suggestions)
                else reply("What would you like to do next?")
        }
        if(RuhiDialogueFollowUp.asksError(source))return reply(memory.lastError.takeIf{it.isNotBlank()}?.let{"The last request failed because: $it"} ?: "There is no recorded failed request in this conversation.",GraphCommandStatus.APPLIED,RuhiDialogueAction.EXPLAIN)
        val equationChips=listOf("y = x^2","y = sin(x)","y = 2*x + 3").map{RuhiAnswerChip(it,it)}
        if(text in setOf("cancel","cancel it","cancel pending","never mind","nevermind","forget it")) { memory=memory.copy(pending=null);return reply("Cancelled the pending request. The scene is unchanged.",GraphCommandStatus.APPLIED,RuhiDialogueAction.CANCEL_PENDING) }
        RuhiPreserveCubeLanguage.execute(initial,source,histories,context,modality,memory)?.let{outcome->memory=memory.copy(pending=outcome.pending);return finish(outcome.result,outcome.result.dialogue ?: RuhiDialogueView(RuhiDialogueAction.EXECUTE))}
        if(Regex("^(?:do not|don't|never)\\b").containsMatchIn(text))return reply("No scene edit was made. State the action you want me to perform.")
        RuhiMeasurementUnitLanguage.execute(initial,source,histories,context,modality,memory)?.let{outcome->memory=memory.copy(pending=outcome.pending);return finish(outcome.result,outcome.result.dialogue ?: RuhiDialogueView(RuhiDialogueAction.EXECUTE))}
        RuhiConstructionConversation.execute(initial,source,histories,context,modality,memory)?.let{outcome->memory=memory.copy(pending=outcome.pending);return finish(outcome.result,outcome.result.dialogue ?: RuhiDialogueView(RuhiDialogueAction.EXECUTE))}
        RuhiContextualGraphLanguage.execute(initial,source,context,modality,memory.pending)?.let{outcome->memory=memory.copy(pending=outcome.pending);return finish(outcome.result,outcome.result.dialogue ?: RuhiDialogueView(RuhiDialogueAction.EXECUTE))}
        RuhiContextualAngleLanguage.execute(initial,source,histories,context,modality,memory.pending)?.let{outcome->memory=memory.copy(pending=outcome.pending);return finish(outcome.result,outcome.result.dialogue ?: RuhiDialogueView(RuhiDialogueAction.EXECUTE))}
        RuhiSpatialPlaneLanguage.execute(initial,if(text in setOf("resume","continue"))text else source,histories,context,modality,memory.pending)?.let{outcome->memory=memory.copy(pending=outcome.pending);return finish(outcome.result,outcome.result.dialogue ?: RuhiDialogueView(RuhiDialogueAction.EXECUTE))}
        RuhiSpatialAngleLanguage.execute(initial,if(text in setOf("resume","continue"))text else source,histories,context,modality,memory.pending)?.let{outcome->memory=memory.copy(pending=outcome.pending);return finish(outcome.result,outcome.result.dialogue ?: RuhiDialogueView(RuhiDialogueAction.EXECUTE))}
        RuhiAngleConversation.execute(initial,when(text){"cancel","cancel it","cancel pending","never mind","nevermind","forget it"->"cancel";"resume","continue"->text;else->source},histories,context,modality,memory.pending)?.let{outcome->memory=memory.copy(pending=outcome.pending);return finish(outcome.result,outcome.result.dialogue ?: RuhiDialogueView(RuhiDialogueAction.EXECUTE))}
        RuhiEndpointConversation.execute(initial,source,histories,context,modality,memory.pending)?.let{outcome->memory=memory.copy(pending=outcome.pending);return finish(outcome.result,outcome.result.dialogue ?: RuhiDialogueView(RuhiDialogueAction.EXECUTE))}
        RuhiPointConversation.execute(initial,source,histories,context,modality,memory.pending)?.let{outcome->memory=memory.copy(pending=outcome.pending);return finish(outcome.result,outcome.result.dialogue ?: RuhiDialogueView(RuhiDialogueAction.EXECUTE))}
        RuhiSceneEditLanguage.execute(initial,source,histories,context,modality,memory.pending)?.let{outcome->memory=memory.copy(pending=outcome.pending);return finish(outcome.result,outcome.result.dialogue ?: RuhiDialogueView(RuhiDialogueAction.EXECUTE))}
        RuhiTransform3DLanguage.execute(initial,source,histories,context,modality,memory.pending)?.let{outcome->memory=memory.copy(pending=outcome.pending);return finish(outcome.result,outcome.result.dialogue ?: RuhiDialogueView(RuhiDialogueAction.EXECUTE))}
        RuhiSolidMeasureLanguage.execute(initial,source,histories,context,modality,memory.pending)?.let{outcome->memory=memory.copy(pending=outcome.pending);return finish(outcome.result,outcome.result.dialogue ?: RuhiDialogueView(RuhiDialogueAction.EXECUTE))}
        RuhiSurfaceLanguage.execute(initial,source,histories,context,modality)?.let{result->if(result.status==GraphCommandStatus.APPLIED)memory=memory.copy(pending=null);return finish(result)}
        RuhiIntegralLanguage.execute(initial,source,histories,context,modality,memory.pending)?.let{outcome->memory=memory.copy(pending=outcome.pending);return finish(outcome.result,outcome.result.dialogue ?: RuhiDialogueView(RuhiDialogueAction.EXECUTE))}
        RuhiDerivativeLanguage.execute(initial,source,histories,context,modality,memory.pending)?.let{outcome->memory=memory.copy(pending=outcome.pending);return finish(outcome.result,outcome.result.dialogue ?: RuhiDialogueView(RuhiDialogueAction.EXECUTE))}
        RuhiIntersectionLanguage.execute(initial,source,histories,context,modality,memory.pending)?.let{outcome->memory=memory.copy(pending=outcome.pending);return finish(outcome.result,outcome.result.dialogue ?: RuhiDialogueView(RuhiDialogueAction.EXECUTE))}
        if(memory.pending==null && initial.module==MathModule.Geometry3D && text.contains("ask me")) {
            val index=context.selectedSolidIndex?.takeIf{it in initial.solids.indices}
                ?: memory.lastModified.removePrefix("solid-").toIntOrNull()?.takeIf{it in initial.solids.indices}
                ?: return reply("Select the solid first.")
            val axis=text.contains("axis")
            return ask(if(axis)"geo_axis" else "geo_scale",text,if(axis)"Which axis: X, Y, or Z? I will use 45Â° unless you specify an angle." else "By what positive scale factor?",(if(axis)listOf("X","Y","Z") else listOf("1.5","2")).map{RuhiAnswerChip(it,it)},listOf("solid-$index"))
        }
        if(memory.pending?.kind in setOf("geo_axis","geo_scale") && text !in setOf("cancel","resume","continue")) {
            val p=memory.pending!!
            if(p.scene!=scene(initial)) {memory=memory.copy(pending=null);return reply("The scene changed. State the edit again for the current scene.")}
            val index=p.targets.singleOrNull()?.removePrefix("solid-")?.toIntOrNull()?.takeIf{it in initial.solids.indices} ?: return reply("Select the solid again.")
            val command=if(p.kind=="geo_axis") {
                val axis=text.removeSuffix("-axis").removePrefix("the ")
                if(axis !in setOf("x","y","z"))return reply("Choose X, Y, or Z.",chips=listOf("X","Y","Z").map{RuhiAnswerChip(it,it)})
                "rotate the selected solid 45 degrees around the $axis-axis"
            } else {
                val factor=text.toDoubleOrNull()?.takeIf{it.isFinite()&&it>0&&it<=100} ?: return reply("Give a positive numeric scale factor, for example 2.",chips=listOf(RuhiAnswerChip("2","2")))
                "enlarge the selected solid by a factor of $factor"
            }
            val result=RuhiGeometry3DLanguage.execute(initial,command,histories,RuhiSceneContext(selectedSolidIndex=index))!!
            if(result.status==GraphCommandStatus.APPLIED)memory=memory.copy(pending=null)
            return finish(result,result.dialogue?.copy(pending=memory.pending!=null) ?: RuhiDialogueView(RuhiDialogueAction.EXECUTE,pending=memory.pending!=null))
        }
        if(memory.pending==null && initial.module==MathModule.Geometry3D) {
            RuhiGeometry3DLanguage.execute(initial,source,histories,context,modality)?.let { return finish(it,it.dialogue ?: RuhiDialogueView(RuhiDialogueAction.EXECUTE)) }
        }
        if(memory.pending!=null && text.contains("quadratic") && Regex("^(?:first )?(?:explain|teach)").containsMatchIn(text)) return reply("A quadratic function has the form y = axÂ² + bx + c, with a â‰  0. Its graph is a parabola. Your comparison is still waiting for functions.",GraphCommandStatus.APPLIED,RuhiDialogueAction.EXPLAIN)
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
        if(command in setOf("draw a graph","plot a graph","draw graph","plot graph","i want a graph","plot a function","plot function","draw a function","draw the function","graph a function","plot","graph")) return ask("equation",command,"What would you like to graph?",equationChips)
        if(initial.module==MathModule.Graph2D) RuhiFunctionLanguage.execute(initial,if(completingPending&&command!=text)command else source,histories,context,modality)?.let { result->
            if(result.status==GraphCommandStatus.APPLIED)memory=memory.copy(pending=null,lastExpression=result.state.functions.singleOrNull{it.id==result.state.labSessionValues[GraphAssistant.selectionKey(initial.module)]}?.expression.orEmpty())
            else if(completingPending)memory=memory.copy(pending=pending)
            return finish(result,result.dialogue ?: RuhiDialogueView(if(result.status==GraphCommandStatus.APPLIED)RuhiDialogueAction.EXECUTE else RuhiDialogueAction.ASK_CLARIFICATION,if(memory.pending?.kind=="equation")equationChips else emptyList(),memory.pending!=null))
        }
        if(command in setOf("compare 2 functions","compare 2 graphs","i want to compare 2 graphs","i want to compare 2 functions")) return ask("compare",command,"Which functions should I compare?",listOf(RuhiAnswerChip("x and xÂ²","x and x^2"),RuhiAnswerChip("sin and cos","sin and cos"),RuhiAnswerChip("Enter functions","Enter functions")))
        if(command.startsWith("compare ")) {
            val expressions=command.removePrefix("compare ").split(Regex("\\s+and\\s+"))
            if(expressions.size!=2) return ask("compare",command,"Give two functions separated by â€˜andâ€™.",listOf(RuhiAnswerChip("sin and cos","sin and cos")))
            fun expression(s:String)=s.removePrefix("y = ").removePrefix("y=").trim().let { if(it in setOf("sin","cos","tan")) "$it(x)" else it }
            command="in 2d graph plot y = ${expression(expressions[0])}; then plot y = ${expression(expressions[1])}"
        }
        if(Regex("^[yz]\\s*=").containsMatchIn(command)) command="in ${if(command.startsWith("z")) "3d" else "2d"} graph plot $command"
        if(command in setOf("undo that","undo the last thing you did")) command="undo"
        if(command=="redo that change") command="redo"
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
            return finish(result.copy(state=committed,message=result.message+if(text in setOf("rotate it","rotate this","rotate the selected object")) " Used a standard 45Â° rotation." else ""),RuhiDialogueView(RuhiDialogueAction.EXECUTE,optional,memory.pending!=null))
        } catch(error:RuntimeException) { if(completingPending) memory=memory.copy(pending=pending);return reply(error.message ?: "The request could not be validated.",GraphCommandStatus.INVALID,chips=if(memory.pending?.kind=="equation")equationChips else emptyList()) }
    }
}
