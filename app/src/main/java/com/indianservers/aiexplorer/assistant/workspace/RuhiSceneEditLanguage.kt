package com.indianservers.aiexplorer.assistant.workspace

import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import java.util.UUID
import kotlin.math.*

/** Scene edits resolve current native IDs; no model prediction supplies geometry or persistence bytes. */
object RuhiSceneEditLanguage {
    const val PREFIX="ruhi.scene."
    const val SAVED=PREFIX+"saved"
    const val ASPECT="Geometry2D::ruhi.viewport.aspect"
    data class Plan(val operation:String,val parameters:Map<String,Any>,val target:String="")
    private class Missing(val refs:List<String>,message:String):IllegalArgumentException(message)
    private val colors=mapOf("red" to "#FF0000","green" to "#008000","blue" to "#0000FF")
    fun parse(source:String):Plan? {
        val t=RuhiMathLanguage.normalize(source)
        val raw=source.trim().replace(Regex("^ruhi[,:]?\\s+",RegexOption.IGNORE_CASE),"").replace(Regex("^(?:please|can you|could you)\\s+",RegexOption.IGNORE_CASE),"")
        fun p(op:String,target:String,vararg params:Pair<String,Any>)=Plan(op,linkedMapOf("target" to target,*params),target)
        Regex("^make the last graph (red|green|blue)$").matchEntire(t)?.let{return p("color","last_graph","color" to it.groupValues[1])}
        Regex("^(?:color|change) the selected (triangle|line)(?: to)? (red|green|blue)$").matchEntire(t)?.let{return p("color","selected_${it.groupValues[1]}","color" to it.groupValues[2])}
        if(t in setOf("hide the coordinate grid","show the coordinate grid"))return p("grid","grid","visible" to t.startsWith("show"))
        if(t in setOf("show the x and y axes","hide the x and y axes"))return p("axes","axes_2d","visible" to t.startsWith("show"))
        Regex("(?i)^label the circle as ([A-Za-z][A-Za-z0-9_]*)[.!]?$",RegexOption.IGNORE_CASE).matchEntire(raw)?.let{return p("label","selected_circle","label" to it.groupValues[1])}
        if(t=="thicken the red line")return p("width","red_line","line_width" to "increase")
        if(t=="make the plotted curve dashed")return p("dash","selected_curve","line_style" to "dashed")
        Regex("(?i)^put a label next to point ([A-Za-z][A-Za-z0-9_]*)[.!]?$",RegexOption.IGNORE_CASE).matchEntire(raw)?.let{return p("point_label",it.groupValues[1],"visible_label" to true)}
        if(t in setOf("hide measurements on the triangle","show measurements on the triangle"))return p("measurements","selected_triangle","measurements" to t.startsWith("show"))
        Regex("(?i)^highlight the endpoints of ([A-Za-z][A-Za-z0-9_]*)[.!]?$").matchEntire(raw)?.let{return p("endpoints","segment_${it.groupValues[1]}","highlight_endpoints" to true)}
        if(t in setOf("clear the canvas","clear the workspace"))return Plan("clear",mapOf("action" to "clear_scene","destructive" to true))
        if(t=="delete the last object")return Plan("delete",mapOf("action" to "delete","target" to "last_object"),"last_object")
        if(t=="duplicate the selected circle")return Plan("duplicate",mapOf("action" to "duplicate","target" to "selected_circle"),"selected_circle")
        Regex("(?i)^rename the current scene to (.+?)[.!]?$",RegexOption.IGNORE_CASE).matchEntire(raw)?.let{return Plan("rename",mapOf("scene_name" to it.groupValues[1]))}
        if(t=="save the current construction")return Plan("save",mapOf("action" to "save_scene"))
        if(t=="undo the last change")return Plan("undo",mapOf("action" to "undo"))
        if(t=="redo what i just undid")return Plan("redo",mapOf("action" to "redo"))
        if(t=="reset zoom to fit all objects")return Plan("fit",mapOf("action" to "zoom_to_fit"))
        Regex("(?i)^center the viewport on point ([A-Za-z][A-Za-z0-9_]*)[.!]?$",RegexOption.IGNORE_CASE).matchEntire(raw)?.let{return Plan("center",mapOf("action" to "camera_center","target" to it.groupValues[1]),it.groupValues[1])}
        Regex("^make the mesh ([0-9]+(?:\\.[0-9]+)?) percent see-through$").matchEntire(t)?.let{return p("opacity","selected_mesh","opacity" to (1-it.groupValues[1].toDouble()/100))}
        if(t=="show only the second graph")return Plan("solo",mapOf("action" to "solo","target" to "graph_2"),"graph_2")
        if(t=="remove labels from all points")return p("labels","all_points","labels" to false)
        if(t=="show a darker graph grid")return p("contrast","grid","contrast" to "increase")
        if(t=="restore the default scene colors")return Plan("theme",mapOf("action" to "reset_theme"))
        return null
    }
    fun accepts(source:String)=parse(source)!=null
    fun flag(s:WorkspaceState,key:String,default:Boolean=true)=s.labSessionValues[PREFIX+key]?.toBooleanStrictOrNull() ?: default
    fun grid(s:WorkspaceState)=if(s.module==MathModule.Graph2D)s.graph2DView.gridVisible else flag(s,"grid")
    fun contrast(s:WorkspaceState)=s.labSessionValues[PREFIX+"contrast"]?.toDoubleOrNull()?.takeIf{it in .1..4.0} ?: 1.0
    fun pointLabelVisible(s:WorkspaceState,i:Int)=flag(s,"point.$i.label",flag(s,"labels"))
    fun measurements(s:WorkspaceState,id:String)=flag(s,"measure.$id")
    private fun scene(s:WorkspaceState)=java.util.Base64.getEncoder().encodeToString(java.security.MessageDigest.getInstance("SHA-256").digest((s.id+s.module+s.functions+s.shapes+s.points+s.solids+s.surfaceLayers).toByteArray(Charsets.UTF_8)))
    private fun display(s:WorkspaceState,ref:String):String=when{ref.startsWith("f:")->s.functions.firstOrNull{it.id==ref.drop(2)}?.name;ref.startsWith("s:")->s.shapes.firstOrNull{it.id==ref.drop(2)}?.name;ref.startsWith("m:")->s.surfaceLayers.firstOrNull{it.id==ref.drop(2)}?.let{"Surface ${s.surfaceLayers.indexOf(it)+1}"};else->null} ?: ref
    private fun answer(ref:String)="Use scene object "+java.util.Base64.getEncoder().encodeToString(ref.toByteArray(Charsets.UTF_8))
    fun restored(s:WorkspaceState,p:RuhiPending)=RuhiDialogueView(RuhiDialogueAction.ASK_CLARIFICATION,p.targets.take(5).map{RuhiAnswerChip(display(s,it),answer(it))},true)
    private fun isLine(f:FunctionDefinition):Boolean=runCatching {
        val typed=TypedGraphExpressionParser.parse(f.expression)
        val expression=when(typed){is TypedGraphExpression.Explicit->"y-(${typed.expression})";is TypedGraphExpression.Implicit->typed.residual;else->return@runCatching false}
        val cas=SymbolicCalculusEngine()
        val xx=cas.differentiate(expression,"x",2).expression;val yy=cas.differentiate(expression,"y",2).expression
        val xy=cas.differentiate(cas.differentiate(expression,"x").expression,"y").expression
        val gradient=listOf(cas.differentiate(expression,"x").expression,cas.differentiate(expression,"y").expression).map{ExpressionEngine().compile(it).eval()}
        listOf(xx,yy,xy).all{ExpressionEngine().compile(it).eval()==0.0}&&gradient.all{it.isFinite()}&&gradient.any{it!=0.0}
    }.getOrDefault(false)
    private fun ref(s:WorkspaceState,c:RuhiSceneContext,p:Plan,picked:String?):String {
        val target=p.target
        val refs=when {
            target in setOf("last_graph","selected_curve","red_line","graph_2")&&s.module==MathModule.Graph2D->s.functions.filter{target!="red_line"||(it.colorKey.lowercase() in setOf("red","#ff0000")&&isLine(it))}.map{"f:${it.id}"}
            target in setOf("last_graph","selected_mesh","graph_2")&&s.module==MathModule.Graph3D->s.surfaceLayers.map{"m:${it.id}"}
            (target in setOf("selected_triangle","selected_line","selected_circle")||target.startsWith("segment_"))&&s.module==MathModule.Geometry2D->s.shapes.filter{when(target){"selected_triangle"->it.type==Shape2DType.Triangle;"selected_circle"->RuhiCircleLanguage.isCircle(it);"selected_line"->it.type in setOf(Shape2DType.Line,Shape2DType.Segment,Shape2DType.Ray);else->it.type in setOf(Shape2DType.Segment,Shape2DType.Vector)&&it.name.equals(target.removePrefix("segment_"),true)}}.map{"s:${it.id}"}
            target=="last_object"->when(s.module){MathModule.Geometry2D->s.shapes.map{"s:${it.id}"};MathModule.Graph2D->s.functions.map{"f:${it.id}"};MathModule.Graph3D->s.surfaceLayers.map{"m:${it.id}"};MathModule.Geometry3D->s.solids.indices.map{"o:$it"};else->emptyList()}
            else->emptyList()
        }
        if(picked!=null){require(picked in refs){"Choose a current matching object."};return picked}
        if(target in setOf("last_graph","last_object"))return refs.lastOrNull() ?: throw Missing(refs,"Create an object in this workspace first.")
        if(target=="graph_2")return refs.getOrNull(1) ?: throw Missing(refs,"Create a second graph first.")
        val selected=if(s.module==MathModule.Geometry2D)c.selectedShapeId?.let{"s:$it"}else (c.selectedFunctionIds.singleOrNull() ?: s.labSessionValues[GraphAssistant.selectionKey(s.module)])?.let{if(s.module==MathModule.Graph3D)"m:$it"else"f:$it"}
        return selected?.takeIf{it in refs} ?: refs.singleOrNull() ?: throw Missing(refs,"Which current object should I edit?")
    }
    private fun point(s:WorkspaceState,label:String):Int {
        require(s.module in setOf(MathModule.Geometry2D,MathModule.Graph2D)){"Open a 2D workspace for this point command."}
        return s.labSessionValues.entries.filter{it.key.startsWith("ruhi.studio.point.")&&it.key.removePrefix("ruhi.studio.point.").equals(label,true)}.mapNotNull{it.value.toIntOrNull()?.takeIf{it in s.points.indices}}.distinct().singleOrNull() ?: throw Missing(emptyList(),"Point $label is missing or ambiguous. Add or identify it first.")
    }
    fun bounds(s:WorkspaceState):List<Vec2> {
        if(s.module==MathModule.Geometry2D)return s.points+s.shapes.filter{RuhiCircleLanguage.isCircle(it)}.flatMap{shape->RuhiCircleLanguage.describe(s,shape)?.let{circle->listOf(circle.center+Vec2(circle.radius,0.0),circle.center-Vec2(circle.radius,0.0),circle.center+Vec2(0.0,circle.radius),circle.center-Vec2(0.0,circle.radius))}.orEmpty()}
        if(s.module==MathModule.Graph2D)return s.points+s.functions.filter{it.visible}.flatMap{runCatching{GraphAnalysis().sampleDefinition(it.expression,-10.0,10.0,240).points}.getOrDefault(emptyList())}.filter{it.x.isFinite()&&it.y.isFinite()}
        return emptyList()
    }
    fun execute(s:WorkspaceState,source:String,h:WorkspaceHistoryByModule,c:RuhiSceneContext,m:RuhiInputModality,pendingInput:RuhiPending?):RuhiPointConversation.Outcome? {
        val pending=pendingInput?.takeIf{it.kind=="scene_edit"};val reply=pending!=null&&source.startsWith("Use scene object ")
        val plan=parse(if(reply)pending!!.source else source) ?: return null
        val cue=runCatching{RuhiLanguageServices.semanticClassifier?.predict(RuhiSeedNlpInput.normalize(source))}.getOrNull()
        val engine=RuhiEngine.from(s.module) ?: return null;val frame=RuhiSemanticFrame("EDIT_SCENE",engine,plan.parameters)
        fun result(state:WorkspaceState,status:GraphCommandStatus,message:String,p:RuhiPending?=null)=RuhiPointConversation.Outcome(RuhiExecutionResult(state,status,message,RuhiCommand(engine=engine,operation=RuhiOperation.PointQuery(frame),modality=m,intentProposal=cue),c,if(p==null)null else restored(state,p),frame),p)
        try {
            if(reply&&pending!!.scene!=scene(s))return result(s,GraphCommandStatus.CLARIFICATION,"The scene changed. State the edit again.")
            val picked=if(reply)String(java.util.Base64.getDecoder().decode(source.removePrefix("Use scene object ")),Charsets.UTF_8).also{require(it in pending!!.targets)}else null
            var next=s;val values=s.labSessionValues.toMutableMap();val p=plan.parameters
            when(plan.operation){
                "color","width","dash","label","measurements","endpoints","opacity","solo","delete","duplicate"->{
                    val ref=ref(s,c,plan,picked);val id=ref.drop(2)
                    when(plan.operation){
                        "color"->{val color=colors.getValue(p["color"].toString());next=if(ref.startsWith("f:"))s.copy(functions=s.functions.map{if(it.id==id)it.copy(colorKey=color)else it})else if(ref.startsWith("s:"))s.copy(shapes=s.shapes.map{if(it.id==id)it.copy(styleKey=color)else it})else s.copy(labSessionValues=values+(PREFIX+"color.$id" to color))}
                        "width"->next=s.copy(functions=s.functions.map{if(it.id==id)it.copy(appearance=it.appearance.copy(width=(it.appearance.width*1.5f).coerceAtMost(24f)))else it})
                        "dash"->next=s.copy(functions=s.functions.map{if(it.id==id)it.copy(appearance=it.appearance.copy(line=GraphLineStyle.Dashed))else it})
                        "label"->next=s.copy(shapes=s.shapes.map{if(it.id==id)it.copy(name=p["label"].toString())else it},labSessionValues=values+(PREFIX+"shapeLabel.$id" to "true"))
                        "measurements"->next=s.copy(labSessionValues=values+(PREFIX+"measure.$id" to p["measurements"].toString()))
                        "endpoints"->next=s.copy(labSessionValues=values+(PREFIX+"endpoints.$id" to "true"))
                        "opacity"->{val opacity=(p["opacity"] as Number).toDouble();require(opacity.isFinite()&&opacity in 0.0..1.0);next=s.copy(surfaceLayers=s.surfaceLayers.map{if(it.id==id)it.copy(opacity=opacity)else it})}
                        "solo"->next=if(s.module==MathModule.Graph2D)s.copy(functions=s.functions.map{it.copy(visible=it.id==id)})else s.copy(surfaceLayers=s.surfaceLayers.map{it.copy(visible=it.id==id)})
                        "delete"->next=when {ref.startsWith("s:")->s.copy(shapes=s.shapes.filterNot{it.id==id},geometryGroups=s.geometryGroups.map{it.copy(shapeIds=it.shapeIds-id)},geometryConstraints=s.geometryConstraints.filterNot{id in it.shapeIds});ref.startsWith("f:")->s.copy(functions=s.functions.filterNot{it.id==id},graphRowMetadata=s.graphRowMetadata-id);ref.startsWith("m:")->s.copy(surfaceLayers=s.surfaceLayers.filterNot{it.id==id});else->{val i=id.toInt();StudioReferenceMetadata.requireSafeSolidRemoval(s,setOf(i));val keep=s.solids.indices.filter{it!=i};s.copy(solids=keep.map(s.solids::get),labSessionValues=StudioReferenceMetadata.remapSolidAppearance(values,keep))}}
                        "duplicate"->{val old=s.shapes.single{it.id==id};val controls=old.pointIndices.map(s.points::get);val radius=RuhiCircleLanguage.describe(s,old)!!.radius;val shift=Vec2(2*radius+1,0.0);val indices=controls.indices.map{s.points.size+it};val clone=old.copy(id=UUID.randomUUID().toString(),pointIndices=indices,name=old.name+" copy");next=s.copy(points=s.points+controls.map{it+shift},shapes=s.shapes+clone)}
                    }
                }
                "grid"->next=if(s.module==MathModule.Graph2D)s.copy(graph2DView=s.graph2DView.copy(gridVisible=p["visible"] as Boolean))else s.copy(labSessionValues=values+(PREFIX+"grid" to p["visible"].toString()))
                "axes"->next=s.copy(labSessionValues=values+(PREFIX+"axes" to p["visible"].toString()))
                "contrast"->next=s.copy(labSessionValues=values+(PREFIX+"contrast" to (contrast(s)*1.5).coerceAtMost(4.0).toString()))
                "point_label"->{val index=point(s,plan.target);next=s.copy(labSessionValues=values+(PREFIX+"point.$index.label" to "true"))}
                "labels"->next=s.copy(labSessionValues=values.filterKeys{!it.startsWith(PREFIX+"point.")}+(PREFIX+"labels" to "false"))
                "rename"->{val name=p.getValue("scene_name").toString();require(name.isNotBlank()&&name.length<=100);next=s.copy(name=name)}
                "save"->{val snapshot=s.copy(labSessionValues=values-SAVED-RuhiDialogueMemory.KEY);next=s.copy(labSessionValues=values+(SAVED to WorkspaceProjectCodec.encode(snapshot)))}
                "undo","redo"->{val history=h.forModule(s.module);if(if(plan.operation=="undo")!history.canUndo else !history.canRedo)return result(s,GraphCommandStatus.CLARIFICATION,"There is no ${plan.operation} step in this workspace.");return result(if(plan.operation=="undo")history.undo(s)else history.redo(s),GraphCommandStatus.APPLIED,"Applied scene ${plan.operation}.")}
                "center"->{val point=s.points[point(s,plan.target)];next=s.copy(graph2DView=s.graph2DView.copy(centerX=point.x,centerY=point.y))}
                "fit"->{val points=bounds(s);if(points.isEmpty())throw Missing(emptyList(),"Add finite 2D objects to fit first.");val aspect=s.labSessionValues[if(s.module==MathModule.Graph2D)RuhiIntersectionLanguage.ASPECT else ASPECT]?.toDoubleOrNull()?.takeIf{it.isFinite()&&it in .1..10.0} ?: 2.0;val minX=points.minOf{it.x};val maxX=points.maxOf{it.x};val minY=points.minOf{it.y};val maxY=points.maxOf{it.y};val half=maxOf((maxX-minX)/2,(maxY-minY)/2/aspect,.05)*1.15;next=s.copy(graph2DView=s.graph2DView.copy(centerX=(minX+maxX)/2,centerY=(minY+maxY)/2,zoom=(7/half).coerceIn(.00001,100.0).toFloat()))}
                "clear"->next=when(s.module){MathModule.Geometry2D->s.copy(points=emptyList(),shapes=emptyList(),pointDependencies=emptyList(),geometryConstraints=emptyList(),geometryGroups=emptyList(),labSessionValues=values.filterKeys{!it.startsWith("ruhi.studio.point.")&&!it.startsWith("ruhi.geometry2d.")&&!it.startsWith(PREFIX+"point.")});MathModule.Graph2D->s.copy(functions=emptyList(),points=emptyList(),shapes=emptyList(),pointDependencies=emptyList(),graphRowMetadata=emptyMap());MathModule.Graph3D->s.copy(surfaceLayers=emptyList(),surfaceExpression="0");MathModule.Geometry3D->s.copy(solids=emptyList(),points3D=emptyList(),vectors3D=emptyList(),planes3D=emptyList(),linears3D=emptyList(),labSessionValues=values.filterKeys{!it.startsWith(RuhiMotionEngine.PREFIX)&&!it.startsWith(RuhiTransform3DLanguage.PREFIX)});else->s}
                "theme"->next=when(s.module){MathModule.Graph2D->s.copy(functions=s.functions.mapIndexed{i,f->f.copy(colorKey=listOf("cyan","violet","green","orange")[i%4])});MathModule.Geometry2D->s.copy(shapes=s.shapes.map{it.copy(styleKey="default")});else->s.copy(labSessionValues=values.filterKeys{!it.startsWith("ruhi.studio.appearance.")&&!it.startsWith(PREFIX+"color.")})}
            }
            val reconciled=GraphWorkspaceCommand.reconcileDocument(s,next);val committed=if(plan.operation=="save")reconciled else h.forModule(s.module).execute(s,GraphWorkspaceCommand(s,reconciled,"Ruhi scene: ${plan.operation}"))
            return result(committed,GraphCommandStatus.APPLIED,"Applied scene ${plan.operation}.")
        }catch(e:Missing){val p=RuhiPending("scene_edit",if(reply)pending!!.source else source,s.module.name,scene(s),e.refs);return result(s,GraphCommandStatus.CLARIFICATION,e.message ?: "Identify the object.",p)}
        catch(e:RuntimeException){return result(s,GraphCommandStatus.INVALID,e.message ?: "Invalid scene edit.")}
    }
}
