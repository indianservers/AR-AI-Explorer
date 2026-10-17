package com.indianservers.aiexplorer.assistant.workspace

import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import com.indianservers.aiexplorer.workspace.GeometryMeasurementUnits.Length

object RuhiMeasurementUnitLanguage {
    private const val KIND="measurement_units"
    private fun text(source:String)=RuhiMathLanguage.normalize(source)
    fun accepts(source:String)=text(source) in setOf("use centimeters instead","use centimetres instead","use meters instead","use metres instead","use meters for coordinate units","use metres for coordinate units","use centimeters for coordinate units")||source.startsWith("Use measurement base ")
    private fun guard(s:WorkspaceState)="${s.id}:${s.module}:${listOf(s.points,s.shapes,s.solids,GeometryMeasurementUnits.read(s)).hashCode()}"
    fun current(s:WorkspaceState,p:RuhiPending)=p.kind!=KIND||p.module==s.module.name&&p.scene==guard(s)
    fun restored()=RuhiDialogueView(RuhiDialogueAction.ASK_CLARIFICATION,listOf(RuhiAnswerChip("Coordinates are in meters","Use measurement base m"),RuhiAnswerChip("Coordinates are in centimeters","Use measurement base cm"),RuhiAnswerChip("Coordinates are in millimeters","Use measurement base mm")),true)
    fun execute(s:WorkspaceState,source:String,h:WorkspaceHistoryByModule,c:RuhiSceneContext,m:RuhiInputModality,memory:RuhiDialogueMemory):RuhiPointConversation.Outcome? {
        if(!accepts(source))return null
        val pending=memory.pending?.takeIf{it.kind==KIND}
        val requested=if(text(pending?.source ?: source).let{"centimeter" in it||"centimetre" in it})Length.CENTIMETER else Length.METER
        fun result(state:WorkspaceState,status:GraphCommandStatus,message:String,p:RuhiPending?=pending ?: memory.pending,base:Length?=null,display:Length?=null):RuhiPointConversation.Outcome {
            val frame=RuhiSemanticFrame("DIALOGUE_CONTEXT",RuhiEngine.from(s.module) ?: RuhiEngine.GEOMETRY_2D,mapOf("units" to (display?.symbol ?: requested.symbol),"target" to "current_measurements","action" to "display_units")+base?.let{mapOf("base_unit" to it.symbol)}.orEmpty())
            return RuhiPointConversation.Outcome(RuhiExecutionResult(state,status,message,RuhiCommand(engine=frame.engine,operation=RuhiOperation.PointQuery(frame),modality=m),c,dialogue=if(p?.kind==KIND)restored()else RuhiDialogueView(RuhiDialogueAction.EXECUTE,pending=p!=null),semanticFrame=frame),p)
        }
        if(s.module !in setOf(MathModule.Geometry2D,MathModule.Geometry3D))return result(s,GraphCommandStatus.CLARIFICATION,"Open a geometry workspace to set physical measurement units.")
        if(pending!=null&&pending.scene!=guard(s))return result(s,GraphCommandStatus.CLARIFICATION,"The geometry or units changed. Ask for the display units again.",p=null)
        val raw=if(source.startsWith("Use measurement base "))pending?.source ?: return result(s,GraphCommandStatus.CLARIFICATION,"Ask which display unit to use first.")else source
        val t=text(raw);val declaration=t.endsWith("for coordinate units")
        val display=if("centimeter" in t||"centimetre" in t)Length.CENTIMETER else Length.METER
        val old=GeometryMeasurementUnits.read(s)
        val supplied=if(source.startsWith("Use measurement base "))Length.entries.firstOrNull{it.symbol==source.removePrefix("Use measurement base ")}else null
        if(source.startsWith("Use measurement base ")&&supplied==null)return result(s,GraphCommandStatus.INVALID,"Choose the actual coordinate unit.")
        val base=if(declaration)display else supplied ?: old.base
        if(base==null) {
            if(memory.pending!=null&&pending==null)return result(s,GraphCommandStatus.CLARIFICATION,"Complete or cancel the pending request before choosing coordinate units.")
            return result(s,GraphCommandStatus.CLARIFICATION,"What unit do the existing coordinates represent? I need that before converting measurements.",RuhiPending(KIND,raw,s.module.name,guard(s)))
        }
        val next=GeometryMeasurementUnits.set(s,base,display)
        val units=GeometryMeasurementUnits.read(next)
        val distance=memory.distanceContexts[s.module.name]?.let{RuhiPointLanguage.current(s,it)}
        val quantities=distance?.let{listOf(RuhiPointLanguage.distanceLabel(next,it))}.orEmpty()+if(s.module==MathModule.Geometry2D){
            val index=s.shapes.indexOfFirst{it.id==c.selectedShapeId}.takeIf{it>=0} ?: s.shapes.indices.singleOrNull()
            index?.let{GeometrySelectionMeasurements.measure(s.points,s.shapes,setOf(it)).quantities.mapNotNull{q->q.value?.let{v->"${q.label} ${units.text(v,q.power,::trim)}"}}}.orEmpty()
        }else RuhiSolidMeasureLanguage.drawing(s)?.entities?.flatMap{e->e.quantities.map{q->if(q.power==0)q.exact else "${e.solid.type} ${q.name.replace('_',' ')} ${units.text(q.value,q.power,::trim)}"}}.orEmpty()
        val committed=if(next==s)s else h.forModule(s.module).execute(s,GraphWorkspaceCommand(s,GraphWorkspaceCommand.reconcileDocument(s,next),"Ruhi: geometry display units"))
        return result(committed,GraphCommandStatus.APPLIED,"Displaying measurements in ${display.symbol}. Coordinates remain in ${base.symbol}."+quantities.takeIf{it.isNotEmpty()}?.joinToString("; "," ").orEmpty(),p=memory.pending?.takeUnless{it.kind==KIND},base=base,display=display)
    }
}
