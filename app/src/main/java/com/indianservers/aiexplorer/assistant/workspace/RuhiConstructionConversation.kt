package com.indianservers.aiexplorer.assistant.workspace

import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import java.util.Base64
import kotlin.math.*

/** Explanations and construction steps are evaluated from native controls and dependencies. */
object RuhiConstructionConversation {
    private const val KIND="construction_context"
    private const val PLACE="construction_place"
    private const val POSITION="Place copy center at "
    private const val ANSWER="Use current construction "
    private fun action(source:String)=when(RuhiMathLanguage.normalize(source)) {
        "explain the shape we are viewing"->"explain"
        "now show the measurements"->"measurements"
        "draw it over there"->"place"
        "show me the drawing step by step","show construction steps"->"steps"
        else->null
    }
    fun accepts(source:String)=runCatching{action(source)!=null||source.startsWith(ANSWER)||source.startsWith(POSITION)}.getOrDefault(false)
    private fun refs(s:WorkspaceState)=when(s.module){MathModule.Geometry2D->s.shapes.filter{it.visible}.map{"s:${it.id}"};MathModule.Geometry3D->s.solids.indices.map{"o:$it"};else->emptyList()}
    private fun scene(s:WorkspaceState)=Base64.getEncoder().encodeToString(java.security.MessageDigest.getInstance("SHA-256").digest((s.id+s.module+s.points+s.shapes+s.pointDependencies+s.geometryConstraints+s.geometryGroups+s.solids).toByteArray(Charsets.UTF_8)))
    private fun label(s:WorkspaceState,ref:String)=if(ref.startsWith("s:"))s.shapes.single{it.id==ref.drop(2)}.let{"${it.name} (${it.type} ${s.shapes.indexOf(it)+1})"}else ref.drop(2).toInt().let{(s.labSessionValues["ruhi.geometry3d.name.$it"] ?: s.solids[it].type.name)+" (${s.solids[it].type} ${it+1})"}
    fun restored(s:WorkspaceState,p:RuhiPending):RuhiDialogueView {
        if(p.kind==PLACE){val positions=if(s.module==MathModule.Geometry2D)listOf("(0, 0)","(3, 2)","(-3, 2)")else listOf("(0, 0, 0)","(3, 2, 1)","(-3, 2, 1)");return RuhiDialogueView(RuhiDialogueAction.ASK_CLARIFICATION,positions.map{RuhiAnswerChip(it,POSITION+it)},true)}
        return RuhiDialogueView(RuhiDialogueAction.ASK_CLARIFICATION,p.targets.take(5).filter{it in refs(s)}.map{RuhiAnswerChip(label(s,it),ANSWER+Base64.getEncoder().encodeToString(it.toByteArray(Charsets.UTF_8)))},true)
    }
    private fun names(s:WorkspaceState)=RuhiEndpointResolver.references(s).associate{it.second to it.first}
    fun pointSteps(s:WorkspaceState,shape:Shape2D):List<String> {
        val labels=names(s);fun name(i:Int)=labels[i] ?: "P${i+1}"
        val byOutput=s.pointDependencies.groupBy{it.outputIndex};val visited=mutableSetOf<Int>();val visiting=mutableSetOf<Int>();val steps=mutableListOf<String>()
        fun visit(i:Int) {
            require(i in s.points.indices){"A construction references a missing point."}
            if(i in visited)return
            require(visiting.add(i)){"This construction contains a dependency cycle."}
            val records=byOutput[i].orEmpty();require(records.size<=1){"A point has conflicting construction definitions."}
            val dependency=records.singleOrNull();dependency?.inputIndices?.forEach(::visit)
            val p=s.points[i];require(p.x.isFinite()&&p.y.isFinite())
            val coordinates="(${p.x}, ${p.y})"
            if(dependency==null)steps+="Place ${name(i)} at $coordinates."
            else {
                val operation=dependency.type.name.replace(Regex("([a-z])([A-Z])"),"$1 $2").lowercase()
                steps+="Construct ${name(i)} as $operation of ${dependency.inputIndices.joinToString{ name(it) }}"+(if(dependency.parameters.isEmpty())""else" using ${dependency.parameters.joinToString()}")+"; its current coordinates are $coordinates."
            }
            visiting.remove(i);visited+=i;require(steps.size<=400){"This construction exceeds the point-step budget."}
        }
        shape.pointIndices.forEach(::visit)
        val points=shape.pointIndices.map(::name)
        steps+=when(shape.type) {
            Shape2DType.Circle->"Draw ${shape.name} with center ${points[0]} through ${points[1]}."
            Shape2DType.CircleThreePoints->"Draw ${shape.name} through ${points.joinToString()}."
            Shape2DType.Line->"Extend the line through ${points.joinToString()} in both directions."
            Shape2DType.Ray->"Start a ray at ${points[0]} through ${points[1]}."
            Shape2DType.Segment->"Connect ${points.joinToString(" to ")} to draw ${shape.name}."
            Shape2DType.Vector->"Draw an arrow from ${points[0]} to ${points[1]}."
            Shape2DType.Triangle,Shape2DType.Polygon,Shape2DType.Rectangle,Shape2DType.Square,Shape2DType.RegularPolygon->"Connect ${points.joinToString()} in order and close ${shape.name}."
            else->"Draw ${shape.name} (${shape.type}) using ${points.joinToString()}."
        }
        return steps
    }
    fun describe(s:WorkspaceState,ref:String):String {
        val unit=GeometryMeasurementUnits.read(s)
        if(ref.startsWith("o:")) {
            val solid=s.solids[ref.drop(2).toInt()];val volume=RuhiSolidMeasureMath.quantity(solid,"volume");val area=RuhiSolidMeasureMath.quantity(solid,"surface_area")
            val dimensions=if(unit.known)"${unit.text(solid.width,1)} by ${unit.text(solid.height,1)} by ${unit.text(solid.depth,1)}, radius ${unit.text(solid.radius,1)}"else"${solid.width} by ${solid.height} by ${solid.depth}, radius ${solid.radius}"
            return "${label(s,ref)}: volume ${unit.text(volume.value,3)}; surface area ${unit.text(area.value,2)}. Dimensions $dimensions; center ${solid.position}${if(unit.known)" (native ${unit.base!!.symbol} coordinates)"else""}. Computed from the current native dimensions."
        }
        val shape=s.shapes.single{it.id==ref.drop(2)};val points=shape.pointIndices.map{s.points[it]}
        val prefix="${shape.name} (${shape.type}): "
        if(RuhiCircleLanguage.isCircle(shape)) {
            val circle=requireNotNull(RuhiCircleLanguage.describe(s,shape))
            return prefix+"radius ${unit.text(circle.radius,1)}; area ${unit.text(PI*circle.radius.pow(2),2)}; circumference ${unit.text(2*PI*circle.radius,1)}."
        }
        if(shape.type in setOf(Shape2DType.Segment,Shape2DType.Vector))return prefix+"length ${unit.text(points[0].distanceTo(points[1]),1)}; endpoints ${points.joinToString()}${if(unit.known)" (native ${unit.base!!.symbol} coordinates)"else""}."
        if(shape.type in setOf(Shape2DType.Line,Shape2DType.Ray))return prefix+"through ${points.joinToString()}; this unbounded object has no finite total length."
        if(shape.type in setOf(Shape2DType.Triangle,Shape2DType.Polygon,Shape2DType.Rectangle,Shape2DType.Square,Shape2DType.RegularPolygon)) {
            val area=abs(Geometry2D.polygonArea(points));val perimeter=points.indices.sumOf{i->points[i].distanceTo(points[(i+1)%points.size])}
            return prefix+"area ${unit.text(area,2)}; perimeter ${unit.text(perimeter,1)}. Computed from the current control points."
        }
        return prefix+"current control points ${points.joinToString()}."
    }
    fun execute(s:WorkspaceState,source:String,h:WorkspaceHistoryByModule,c:RuhiSceneContext,m:RuhiInputModality,memory:RuhiDialogueMemory):RuhiPointConversation.Outcome? {
        if(s.module !in setOf(MathModule.Geometry2D,MathModule.Geometry3D))return null
        val pending=memory.pending?.takeIf{it.kind in setOf(KIND,PLACE)};val reply=pending?.kind==KIND&&source.startsWith(ANSWER)
        val placementReply=pending?.kind==PLACE&&(source.trim().startsWith("(")||source.startsWith(POSITION))
        val operation=action(if(reply||placementReply)pending!!.source else source) ?: return null
        val engine=RuhiEngine.from(s.module)!!
        var frame=RuhiSemanticFrame("DIALOGUE_CONTEXT",engine,mapOf("action" to when(operation){"steps"->"step_by_step";"place"->"draw_copy";else->operation},"target" to when(operation){"steps"->"last_construction";"place"->"last_object";"explain"->"current_scene";else->"contextual_object"}))
        fun result(state:WorkspaceState,status:GraphCommandStatus,message:String,ctx:RuhiSceneContext=c,p:RuhiPending?=null):RuhiPointConversation.Outcome {
            val explanation=operation in setOf("explain","steps")
            val interrupted=explanation&&status==GraphCommandStatus.APPLIED&&!reply&&!placementReply&&memory.pending!=null&&!(pending?.kind==KIND&&action(pending.source)==operation)
            val kept=p ?: memory.pending.takeIf{interrupted}
            val view=when { explanation&&status==GraphCommandStatus.APPLIED->RuhiDialogueView(RuhiDialogueAction.EXPLAIN,pending=kept!=null);kept?.kind in setOf(KIND,PLACE)->restored(state,kept!!);status!=GraphCommandStatus.APPLIED->RuhiDialogueView(RuhiDialogueAction.ASK_CLARIFICATION,pending=kept!=null);else->null }
            return RuhiPointConversation.Outcome(RuhiExecutionResult(state,status,message,RuhiCommand(engine=engine,operation=RuhiOperation.PointQuery(frame),modality=m),ctx,dialogue=view,semanticFrame=frame),kept)
        }
        try {
            val available=refs(s)
            if((reply||placementReply)&&pending!!.scene!=scene(s))return result(s,GraphCommandStatus.CLARIFICATION,"The construction changed. Ask again using the current scene.")
            val picked=if(placementReply)pending!!.targets.single().also{require(it in available)}else if(reply)String(Base64.getDecoder().decode(source.removePrefix(ANSWER)),Charsets.UTF_8).also{require(it in pending!!.targets&&it in available)}else null
            val selected=if(s.module==MathModule.Geometry2D)c.selectedShapeId?.let{"s:$it"}else c.selectedSolidIndex?.let{"o:$it"}
            fun remembered(id:String)=if(id.isBlank())null else if(s.module==MathModule.Geometry2D)"s:$id"else id.removePrefix("solid-").toIntOrNull()?.let{"o:$it"}
            val preferred=if(operation=="steps")listOf(remembered(memory.lastCreated),selected)else listOf(selected,remembered(memory.lastModified),remembered(memory.lastCreated))
            val ref=picked ?: preferred.firstOrNull{it in available} ?: available.singleOrNull()
            if(ref==null) {
                if(available.isEmpty())return result(s,GraphCommandStatus.CLARIFICATION,"Which construction should I use? Create or select one first.")
                val p=RuhiPending(KIND,source,s.module.name,scene(s),available)
                return result(s,GraphCommandStatus.CLARIFICATION,"Which current construction should I use?",p=p)
            }
            val ctx=if(ref.startsWith("s:"))c.copy(selectedShapeId=ref.drop(2))else c.copy(selectedSolidIndex=ref.drop(2).toInt())
            frame=frame.copy(parameters=frame.parameters+mapOf("resolved_object_id" to ref)+(if(operation=="measurements")mapOf("measurements" to true)else emptyMap()))
            if(operation=="place") {
                if(!placementReply){val p=RuhiPending(PLACE,if(reply)pending!!.source else source,s.module.name,scene(s),listOf(ref));return result(s,GraphCommandStatus.CLARIFICATION,"Where should I put the copy's center?",ctx,p)}
                val raw=source.removePrefix(POSITION).trim();require(raw.startsWith("(")&&raw.endsWith(")")){"Give the center as coordinates in parentheses."}
                val values=raw.drop(1).dropLast(1).split(',').map{it.trim().toDoubleOrNull() ?: error("Use numeric center coordinates.")}
                require(values.size==if(s.module==MathModule.Geometry2D)2 else 3){"Use ${if(s.module==MathModule.Geometry2D)2 else 3} center coordinates."}
                require(values.all{it.isFinite()&&abs(it)<=10000}){"Use finite center coordinates within 10000 units."}
                val next:WorkspaceState;val output:String;val outputContext:RuhiSceneContext
                if(ref.startsWith("s:")) {
                    require(s.shapes.size<200){"The native shape budget is full."}
                    val old=s.shapes.single{it.id==ref.drop(2)};val controls=old.pointIndices.map{s.points[it]}
                    val center=if(RuhiCircleLanguage.isCircle(old))requireNotNull(RuhiCircleLanguage.describe(s,old)).center else controls.reduce{a,b->a+b}*(1.0/controls.size)
                    val delta=Vec2(values[0],values[1])-center
                    val points=controls.map{it+delta};require(points.all{it.x.isFinite()&&it.y.isFinite()&&abs(it.x)<=10000&&abs(it.y)<=10000}){"The copied controls would leave the supported coordinate range."}
                    val id=java.util.UUID.randomUUID().toString();val name=generateSequence(1){it+1}.map{"${old.name} copy $it"}.first{n->s.shapes.none{it.name==n}}
                    val clone=old.copy(id=id,name=name,pointIndices=points.indices.map{s.points.size+it},locked=false)
                    next=s.copy(points=s.points+points,shapes=s.shapes+clone,labSessionValues=s.labSessionValues+("ruhi.studio.selected.shape" to id));output="s:$id";outputContext=c.copy(selectedShapeId=id)
                }else {
                    require(s.solids.size<24){"The native solid budget is full."}
                    val oldIndex=ref.drop(2).toInt();val old=s.solids[oldIndex];val index=s.solids.size
                    val appearance=s.labSessionValues.toMutableMap()
                    for(prefix in listOf("ruhi.studio.appearance.",RuhiTransform3DLanguage.PREFIX+"opacity.",RuhiTransform3DLanguage.PREFIX+"mode.",RuhiTransform3DLanguage.PREFIX+"explosion."))s.labSessionValues[prefix+oldIndex]?.let{appearance[prefix+index]=it}
                    s.labSessionValues["ruhi.geometry3d.name.$oldIndex"]?.let{appearance["ruhi.geometry3d.name.$index"]="$it copy"}
                    appearance["ruhi.studio.selected.solid"]=index.toString();appearance["ruhi.geometry3d.selection"]=index.toString()
                    if(RuhiTransform3DLanguage.groupIds(s).isNotEmpty()) {
                        val group=appearance.getValue(RuhiTransform3DLanguage.GROUP).split(';').toMutableList();group[3]=(index+1).toString();appearance[RuhiTransform3DLanguage.GROUP]=group.joinToString(";")
                    }
                    next=s.copy(solids=s.solids+old.copy(position=Vec3(values[0],values[1],values[2])),labSessionValues=appearance);output="o:$index";outputContext=c.copy(selectedSolidIndex=index)
                }
                frame=frame.copy(parameters=frame.parameters+mapOf("source_object_id" to ref,"resolved_object_id" to output,"resolved_center" to values))
                val committed=h.forModule(s.module).execute(s,GraphWorkspaceCommand(s,GraphWorkspaceCommand.reconcileDocument(s,next),"Ruhi: draw a copy at its center"))
                return result(committed,GraphCommandStatus.APPLIED,"Drew an independent copy with its center at ${values.joinToString(prefix="(",postfix=")")}; the original is unchanged.",outputContext)
            }
            if(operation=="explain")return result(s,GraphCommandStatus.APPLIED,describe(s,ref),ctx)
            if(operation=="steps") {
                val steps=if(ref.startsWith("s:"))pointSteps(s,s.shapes.single{it.id==ref.drop(2)})else {
                    val solid=s.solids[ref.drop(2).toInt()]
                    val dimensions=when(solid.type){SolidType.Sphere,SolidType.Hemisphere->"radius ${solid.radius}";SolidType.Cylinder,SolidType.Cone->"radius ${solid.radius} and height ${solid.height}";SolidType.Frustum->"bottom radius ${solid.radius}, top radius ${solid.topRadius} and height ${solid.height}";else->"width ${solid.width}, height ${solid.height} and depth ${solid.depth}"}
                    listOf("Place the center at ${solid.position}.","Construct ${solid.type} with $dimensions.","Apply the current rotation ${solid.rotation}"+(if(solid.mirrorX)" and its YZ-plane mirror."else"."),"Draw the ${solid.type} using those dimensions.")
                }
                return result(s,GraphCommandStatus.APPLIED,steps.mapIndexed{i,step->"${i+1}. $step"}.joinToString("\n"),ctx)
            }
            val next=if(ref.startsWith("s:"))s.copy(labSessionValues=s.labSessionValues+(RuhiSceneEditLanguage.PREFIX+"measure.${ref.drop(2)}" to "true"))else {
                val measured=RuhiSolidMeasureLanguage.execute(s,"Show the surface area and volume of the selected solid",WorkspaceHistoryByModule(),ctx,m,null) ?: error("The native solid measurement is unavailable.")
                require(measured.result.status==GraphCommandStatus.APPLIED){measured.result.message};measured.result.state
            }
            val committed=if(next==s)s else h.forModule(s.module).execute(s,GraphWorkspaceCommand(s,GraphWorkspaceCommand.reconcileDocument(s,next),"Ruhi: show current measurements"))
            return result(committed,GraphCommandStatus.APPLIED,"Showing measurements. "+describe(committed,ref),ctx)
        }catch(error:RuntimeException){return result(s,GraphCommandStatus.INVALID,error.message ?: "Invalid native construction.",p=pending)}
    }
}
