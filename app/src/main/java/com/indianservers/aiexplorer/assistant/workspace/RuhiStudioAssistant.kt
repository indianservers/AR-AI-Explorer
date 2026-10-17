package com.indianservers.aiexplorer.assistant.workspace

import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import kotlin.math.*

/** Shared immutable transaction boundary. The model proposes; this adapter validates and computes. */
class RuhiStudioAssistant(private val clockMillis:()->Long={System.nanoTime()/1_000_000}) {
    private class Clarify(message:String):IllegalArgumentException(message)
    private class Unsupported(message:String):IllegalArgumentException(message)
    private val number="[-+]?\\d+(?:\\.\\d+)?"
    private fun value(s:String):Double=s.toDoubleOrNull()?.takeIf { it.isFinite() && abs(it)<=10000 } ?: throw IllegalArgumentException("Use a finite numeric value within 10000 units.")
    private fun dimension(s:String)=value(s).also { require(it>0 && it<=50) { "Dimensions must be positive and no larger than 50 units." } }
    private fun selectedShape(state:WorkspaceState,c:RuhiSceneContext)=state.shapes.firstOrNull { it.id==c.selectedShapeId } ?: throw Clarify("Select a geometry shape first.")
    private fun solidIndex(state:WorkspaceState,c:RuhiSceneContext)=c.selectedSolidIndex?.takeIf { it in state.solids.indices } ?: throw Clarify("Select a solid first.")

    fun execute(initial:WorkspaceState,source:String,histories:WorkspaceHistoryByModule,context:RuhiSceneContext=RuhiSceneContext(),modality:RuhiInputModality=RuhiInputModality.TEXT):RuhiExecutionResult {
        var envelope:RuhiCommand?=null
        return try {
            var text=RuhiMathLanguage.normalize(source)
            if(Regex("^(?:do not|don't|never)\\b").containsMatchIn(text)) throw Clarify("No scene edit was made. State the action you want to perform.")
            val proposal=RuhiLanguageServices.engineClassifier?.predict(text)
            val intentProposal=GraphLanguageRouter.classifier?.predict(text)
            var engine=RuhiEngine.from(initial.module) ?: throw Clarify("Open a graph or geometry studio first.")
            var reason="active scene context"
            val explicit=Regex("^(?:in (?:the )?)?(2d|3d) (graph|geometry)(?: studio)?[:,]?\\s+(.+)").matchEntire(text)
            if(explicit!=null) {
                engine=RuhiEngine.valueOf((if(explicit.groupValues[2]=="graph") "GRAPH_" else "GEOMETRY_")+explicit.groupValues[1].uppercase())
                text=explicit.groupValues[3];reason="explicit studio request"
            } else if(Regex("(?:create|draw|make|add) (?:a |an )?(?:rotating )?3d ").containsMatchIn(text)) {
                engine=RuhiEngine.GEOMETRY_3D;reason="explicit 3D solid entity";text=text.replace("3d ","")
            } else if(engine==RuhiEngine.GRAPH_2D && Regex("^(?:create|draw|add|make) (?:a |an )?(?:cube|cuboid|sphere|cone|cylinder)\\b").containsMatchIn(text)) {
                if(proposal?.engine==RuhiEngine.GEOMETRY_3D && proposal.confidence>=.9f && proposal.margin>=.4f) { engine=RuhiEngine.GEOMETRY_3D;reason="confident engine proposal for a solid incompatible with 2D graph" }
                else throw Clarify("Use 3D Geometry or 3D Graph for a solid. Specify the studio or open it first.")
            }
            if(explicit==null && engine in setOf(RuhiEngine.GEOMETRY_2D,RuhiEngine.GEOMETRY_3D) && Regex("^(?:plot|graph|draw|show) .*=.+").matches(text)) {
                val requested=if(Regex("\\bz\\s*=").containsMatchIn(text)) RuhiEngine.GRAPH_3D else RuhiEngine.GRAPH_2D
                if(proposal?.engine==requested && proposal.confidence>=.9f && proposal.margin>=.4f) { engine=requested;reason="confident engine proposal for an equation outside the active geometry adapter" }
                else throw Clarify("Specify 2D Graph or 3D Graph for this equation; the engine proposal was not confident enough to switch studios.")
            }
            val state=initial.copy(module=engine.module)
            val history=histories.forModule(engine.module)
            val historyMatch=Regex("(undo|redo)(?: (?:the )?(?:last |undone |previous )?(?:action|command))?(?: (\\d+) actions?)?").matchEntire(text)
            if(historyMatch!=null) {
                val count=historyMatch.groupValues[2].toIntOrNull() ?: 1;require(count in 1..80)
                val op=RuhiOperation.History(historyMatch.groupValues[1]=="redo",count)
                envelope=RuhiCommand(engine=engine,operation=op,modality=modality,engineProposal=proposal,intentProposal=intentProposal,routingReason=reason)
                val next=if(op.redo) history.redoMany(state,count) else history.undoMany(state,count)
                return RuhiExecutionResult(next,GraphCommandStatus.APPLIED,"${if(op.redo) "Redid" else "Undid"} $count action(s).",envelope,contextFrom(next,context))
            }
            if(engine in setOf(RuhiEngine.GRAPH_2D,RuhiEngine.GRAPH_3D)) {
                if(engine==RuhiEngine.GRAPH_2D && text in setOf("show the vertex","show its vertex","show the vertex of this parabola","find the vertex")) {
                    val row=state.functions.firstOrNull { it.id==state.labSessionValues[GraphAssistant.selectionKey(state.module)] } ?: state.functions.singleOrNull() ?: throw Clarify("Select one quadratic graph first.")
                    val body=stripEquation(row.expression);require(polynomialDegree(SymbolicCasEngine().parse(body))==2) { "The selected curve must be a quadratic polynomial." }
                    val expression=ExpressionEngine().compile(body);val c0=expression.eval(mapOf("x" to 0.0));val fp=expression.eval(mapOf("x" to 1.0));val fm=expression.eval(mapOf("x" to -1.0));val a=(fp+fm)/2-c0;val b=(fp-fm)/2
                    require(a.isFinite() && b.isFinite() && c0.isFinite() && abs(a)>1e-12)
                    val vertex=GraphAnalysis().quadratic(a,b,c0).vertex
                    require(listOf(vertex.x,vertex.y).all { it.isFinite() && abs(it)<=10000 }) { "The vertex is outside the finite annotation budget of 10000 units." }
                    val next=state.copy(labSessionValues=state.labSessionValues+("ruhi.graph2d.vertex.${row.id}" to "${row.expression}\t${vertex.x},${vertex.y}"))
                    envelope=RuhiCommand(engine=engine,operation=RuhiOperation.ShowVertex,modality=modality,targetObjectId=row.id,engineProposal=proposal,intentProposal=intentProposal,routingReason=reason)
                    return RuhiExecutionResult(history.execute(state,GraphWorkspaceCommand(state,next,"Ruhi: show vertex")),GraphCommandStatus.APPLIED,"Vertex = (${vertex.x}, ${vertex.y}), calculated from the quadratic coefficients and marked on the graph.",envelope,context)
                }
                // Stage against a private history, then commit the typed actions and validated snapshot once.
                val canonical=when(text) { "draw a parabola","draw the parabola"->"plot y = x^2";else->text.replace(Regex("^(?:draw|plot) (?:the )?graph (?:of )?"),"plot ") }
                val prepared=GraphAssistant().execute(state,canonical,CommandHistory())
                envelope=RuhiCommand(engine=engine,operation=RuhiOperation.GraphProgram(prepared.actions),modality=modality,engineProposal=proposal,intentProposal=intentProposal,routingReason=reason)
                if(prepared.status!=GraphCommandStatus.APPLIED) return RuhiExecutionResult(initial,prepared.status,prepared.message,envelope,context)
                val next=history.execute(state,GraphWorkspaceCommand(state,prepared.state,"Ruhi: ${source.take(100)}"))
                return RuhiExecutionResult(next,prepared.status,prepared.message,envelope,context)
            }
            text=GraphLanguageRouter.canonical(text) ?: text
            val op=parseGeometry(text,engine)
            val resolved=if(op is RuhiOperation.CreateSolid || op is RuhiOperation.CreateShape || op is RuhiOperation.CreatePoint || op is RuhiOperation.ConnectPoints || op is RuhiOperation.Distance || op is RuhiOperation.Midpoint) context else resolveContext(state,text,context)
            envelope=RuhiCommand(engine=engine,operation=op,modality=modality,targetObjectId=if(engine==RuhiEngine.GEOMETRY_2D) resolved.selectedShapeId else resolved.selectedSolidIndex?.let{"solid-$it"},engineProposal=proposal,intentProposal=intentProposal,routingReason=reason)
            val (rawStage,explanation)=applyGeometry(state,op,resolved)
            val created=op is RuhiOperation.CreateShape || op is RuhiOperation.CreateSolid
            val staged=if(!created && op !is RuhiOperation.Measure && op !is RuhiOperation.Distance && op !is RuhiOperation.Midpoint && op !is RuhiOperation.CreatePoint && op !is RuhiOperation.ConnectPoints && op!=RuhiOperation.DeleteSelected) {
                rawStage.copy(labSessionValues=rawStage.labSessionValues + if(engine==RuhiEngine.GEOMETRY_2D) mapOf("ruhi.studio.selected.shape" to selectedShape(state,resolved).id) else mapOf("ruhi.studio.selected.solid" to solidIndex(state,resolved).toString()))
            } else rawStage
            val next=if(staged==state) state else history.execute(state,GraphWorkspaceCommand(state,GraphWorkspaceCommand.reconcileDocument(state,staged),"Ruhi: ${source.take(100)}"))
            val nextContext=when {
                op==RuhiOperation.DeleteSelected -> if(engine==RuhiEngine.GEOMETRY_2D) resolved.copy(selectedShapeId=null) else resolved.copy(selectedSolidIndex=null)
                created || op is RuhiOperation.ConnectPoints -> contextFrom(next,RuhiSceneContext())
                else->resolved
            }
            RuhiExecutionResult(next,GraphCommandStatus.APPLIED,explanation,envelope,nextContext)
        } catch(error:RuntimeException) {
            RuhiExecutionResult(initial,when(error){is Clarify->GraphCommandStatus.CLARIFICATION;is Unsupported->GraphCommandStatus.UNSUPPORTED;else->GraphCommandStatus.INVALID},error.message ?: "The command could not be validated.",envelope,context)
        }
    }
    private fun resolveContext(state:WorkspaceState,text:String,c:RuhiSceneContext):RuhiSceneContext {
        val noun=Regex("\\b(cube|cuboid|sphere|cone|cylinder|square|circle|triangle|rectangle)\\b").find(text)?.value ?: return c
        if(state.module==MathModule.Geometry3D) {
            val candidates=state.solids.indices.filter{state.solids[it].type.name.equals(noun,true)}
            val selected=(if(Regex("\\bfirst\\b").containsMatchIn(text)) candidates.firstOrNull() else if(Regex("\\blast\\b").containsMatchIn(text)) candidates.lastOrNull() else c.selectedSolidIndex?.takeIf{it in candidates}) ?: candidates.singleOrNull() ?: throw Clarify("Select the $noun you mean.")
            return c.copy(selectedSolidIndex=selected)
        }
        val candidates=state.shapes.filter{it.type.name.equals(noun,true)}.map{it.id}
        val selected=(if(Regex("\\bfirst\\b").containsMatchIn(text)) candidates.firstOrNull() else if(Regex("\\blast\\b").containsMatchIn(text)) candidates.lastOrNull() else c.selectedShapeId?.takeIf{it in candidates}) ?: candidates.singleOrNull() ?: throw Clarify("Select the $noun you mean.")
        return c.copy(selectedShapeId=selected)
    }
    private fun polynomialDegree(e:SymbolicExpression):Int = when(e) {
        is SymbolicExpression.Number->0
        is SymbolicExpression.Variable->if(e.name=="x") 1 else throw IllegalArgumentException("Resolve graph parameters before finding the vertex.")
        is SymbolicExpression.UnaryMinus->polynomialDegree(e.value)
        is SymbolicExpression.Sum->e.terms.maxOf(::polynomialDegree)
        is SymbolicExpression.Product->e.factors.sumOf(::polynomialDegree)
        is SymbolicExpression.Power->{val n=(e.exponent as? SymbolicExpression.Number)?.value?.toDouble() ?: throw IllegalArgumentException("Use a quadratic polynomial.");require(n in 0.0..2.0 && n==n.toInt().toDouble());polynomialDegree(e.base)*n.toInt()}
        else->throw IllegalArgumentException("Use a quadratic polynomial.")
    }
    private fun parseGeometry(text:String,engine:RuhiEngine):RuhiOperation {
        if(text in setOf("explain this","explain the selected object","explain what i am currently looking at","explain the remaining circle")) return RuhiOperation.Measure(RuhiMeasure.EXPLAIN)
        Regex("(?:calculate|show|display|measure) (?:the )?(volume|surface area|total surface area|area|perimeter)(?: of .+)?").matchEntire(text)?.let {
            val reference=text.substringAfter(" of ","").removePrefix("the ").removePrefix("this ").removePrefix("selected ")
            if(reference.isNotEmpty() && reference !in setOf("object","shape","solid","cube","cuboid","sphere","cone","cylinder","circle","square","triangle","rectangle")) throw Clarify("Select the object to measure or name its supported shape type.")
            return RuhiOperation.Measure(when(it.groupValues[1]) { "volume"->RuhiMeasure.VOLUME;"surface area","total surface area"->RuhiMeasure.SURFACE_AREA;"perimeter"->RuhiMeasure.PERIMETER;else->RuhiMeasure.AREA })
        }
        if(text in setOf("stop the rotation","stop rotation","stop the animation","stop animation")) return RuhiOperation.StopMotion
        Regex("(?:make|color) (?:it|this|the selected object|the selected shape|the selected solid) (red|blue|green|purple|yellow)").matchEntire(text)?.let { return RuhiOperation.SetColor(mapOf("red" to "#FF0000","blue" to "#0000FF","green" to "#008000","purple" to "#800080","yellow" to "#FFFF00").getValue(it.groupValues[1])) }
        Regex("(?:delete|remove) (?:the )?(?:first |last )?(?:cube|cuboid|sphere|cone|cylinder|square|circle|triangle|rectangle)").matchEntire(text)?.let { return RuhiOperation.DeleteSelected }
        Regex("move (?:it|this|the (?:selected object|cube|cuboid|sphere|cone|cylinder|square|circle|triangle|rectangle)) to \\(([^)]+)\\)").matchEntire(text)?.let {
            val v=it.groupValues[1].split(',').map { n->value(n.trim()) };require(v.size==if(engine==RuhiEngine.GEOMETRY_2D) 2 else 3) { "Provide the studio's coordinate count." };return RuhiOperation.SetPosition(Vec3(v[0],v[1],v.getOrElse(2){0.0}))
        }
        if(text in setOf("delete it","delete the selected object","delete the currently selected object","delete the selected shape","delete the selected solid")) return RuhiOperation.DeleteSelected
        Regex("(?:scale|enlarge) (?:it|this|the (?:selected )?(?:object|shape|solid|cuboid|triangle|square|circle|cube|sphere|cone|cylinder)) by (?:a (?:scale )?factor of )?($number)").matchEntire(text)?.let { val n=value(it.groupValues[1]);require(n>0 && n<=100);return RuhiOperation.Scale(n) }
        if(text in setOf("make it twice as large without moving its center","double its size")) return RuhiOperation.Scale(2.0)
        Regex("(?:move|shift|translate) (?:it|this|the selected (?:object|shape|solid)|the cone|the polygon) ($number) units? (right|left|up|upward|down|downward)").matchEntire(text)?.let {
            val n=value(it.groupValues[1]);val d=when(it.groupValues[2]){"left"->Vec3(-n,0.0,0.0);"right"->Vec3(n,0.0,0.0);"down","downward"->Vec3(0.0,-n,0.0);else->Vec3(0.0,n,0.0)};return RuhiOperation.Translate(d)
        }
        Regex("rotate (?:it|this|the selected (?:object|shape|solid|square|sphere)|the sphere|the cube) (?:by )?($number) degrees(?: (?:around|about) the ([xyz])-axis)?").matchEntire(text)?.let { return RuhiOperation.Rotate(it.groupValues[2].ifEmpty { if(engine==RuhiEngine.GEOMETRY_2D) "z" else "y" },value(it.groupValues[1])) }
        if(engine==RuhiEngine.GEOMETRY_2D) {
            Regex("create point ([a-z]) at \\(($number),\\s*($number)\\)").matchEntire(text)?.let { return RuhiOperation.CreatePoint(it.groupValues[1].uppercase(),Vec2(value(it.groupValues[2]),value(it.groupValues[3]))) }
            Regex("(?:draw|create) (?:a )?(?:line )?segment (?:connecting |from )?([a-z]) (?:and|to) ([a-z])").matchEntire(text)?.let { return RuhiOperation.ConnectPoints(it.groupValues[1].uppercase(),it.groupValues[2].uppercase()) }
            Regex("(?:calculate|measure|find) (?:the )?distance between ([a-z]) and ([a-z])").matchEntire(text)?.let { return RuhiOperation.Distance(it.groupValues[1].uppercase(),it.groupValues[2].uppercase()) }
            Regex("find the midpoint of (?:segment )?([a-z])([a-z])").matchEntire(text)?.let { return RuhiOperation.Midpoint(it.groupValues[1].uppercase(),it.groupValues[2].uppercase()) }
            Regex("(?:draw|create) (?:a )?circle(?: centered at the origin)? with radius ($number)").matchEntire(text)?.let { return RuhiOperation.CreateShape(Shape2DType.Circle,listOf(dimension(it.groupValues[1]))) }
            Regex("(?:draw|create) (?:a )?square with side length ($number)").matchEntire(text)?.let { return RuhiOperation.CreateShape(Shape2DType.Square,listOf(dimension(it.groupValues[1]))) }
            Regex("(?:draw|create) (?:a )?rectangle measuring ($number) by ($number)").matchEntire(text)?.let { return RuhiOperation.CreateShape(Shape2DType.Rectangle,listOf(dimension(it.groupValues[1]),dimension(it.groupValues[2]))) }
            Regex("(?:draw|create) (?:an? )?(equilateral|right) triangle with (?:side length |legs )($number)(?: and ($number))?").matchEntire(text)?.let {
                val d=dimension(it.groupValues[2]);val e=if(it.groupValues[1]=="equilateral") d else it.groupValues[3].takeIf(String::isNotEmpty)?.let(::dimension) ?: throw Clarify("Specify both perpendicular legs.")
                return RuhiOperation.CreateShape(Shape2DType.Triangle,if(it.groupValues[1]=="equilateral") listOf(d,d/2,sqrt(3.0)*d/2) else listOf(d,0.0,e))
            }
        } else {
            Regex("(?:create|draw|make|add) (?:a |an )?(rotating )?(cube|cuboid|sphere|cylinder|cone|square pyramid)(.*)").matchEntire(text)?.let {
                val kind=it.groupValues[2];val suffix=it.groupValues[3];val d=2.0
                val solid=when(kind) {
                    "cube" -> { val n=if(suffix.isEmpty()) d else Regex(" with side length ($number)").matchEntire(suffix)?.groupValues?.get(1)?.let(::dimension) ?: throw Clarify("Specify the cube side length.");Solid(SolidType.Cube,n) }
                    "sphere" -> { val n=if(suffix.isEmpty()) 1.0 else Regex(" with radius ($number)").matchEntire(suffix)?.groupValues?.get(1)?.let(::dimension) ?: throw Clarify("Specify the sphere radius.");Solid(SolidType.Sphere,2*n,radius=n) }
                    "cuboid" -> { val m=Regex(" measuring ($number) by ($number) by ($number)").matchEntire(suffix) ?: throw Clarify("Specify width, height and depth.");Solid(SolidType.Cuboid,dimension(m.groupValues[1]),dimension(m.groupValues[2]),dimension(m.groupValues[3])) }
                    "cylinder","cone" -> { val m=Regex(" with radius ($number) and height ($number)").matchEntire(suffix);val onlyRadius=Regex(" with radius ($number)").matchEntire(suffix);val onlyHeight=Regex(" with height ($number)").matchEntire(suffix);if(suffix.isNotEmpty() && m==null && onlyRadius==null && onlyHeight==null) throw Clarify("Specify a numeric radius or height.");val radius=(m?.groupValues?.get(1) ?: onlyRadius?.groupValues?.get(1))?.let(::dimension) ?: 1.0;val height=(m?.groupValues?.get(2) ?: onlyHeight?.groupValues?.get(1))?.let(::dimension) ?: 2.0;Solid(if(kind=="cone") SolidType.Cone else SolidType.Cylinder,2*radius,height,2*radius,radius) }
                    else -> { val m=Regex(" with base side ($number) and height ($number)").matchEntire(suffix) ?: throw Clarify("Specify base side and height.");val side=dimension(m.groupValues[1]);Solid(SolidType.Pyramid,side,dimension(m.groupValues[2]),side) }
                }
                return RuhiOperation.CreateSolid(solid,if(it.groupValues[1].isNotEmpty()) RuhiAngularMotion(startMillis=clockMillis()) else null)
            }
        }
        if(Regex("(?:bigger|smaller|larger|rotate|move|scale)").containsMatchIn(text)) throw Clarify("Provide the target, a numeric value and the required axis or direction.")
        throw Unsupported("That operation is not connected to this studio adapter yet. Existing tools remain available.")
    }
    private fun contextFrom(state:WorkspaceState,old:RuhiSceneContext):RuhiSceneContext {
        val shape=state.labSessionValues["ruhi.studio.selected.shape"]?.takeIf { id->state.shapes.any{it.id==id} } ?: old.selectedShapeId?.takeIf { id->state.shapes.any{it.id==id} }
        val solid=state.labSessionValues["ruhi.studio.selected.solid"]?.toIntOrNull()?.takeIf { it in state.solids.indices } ?: old.selectedSolidIndex?.takeIf { it in state.solids.indices }
        return RuhiSceneContext(shape,solid)
    }
    private fun applyGeometry(state:WorkspaceState,op:RuhiOperation,c:RuhiSceneContext):Pair<WorkspaceState,String> {
        fun point(name:String):Int = state.labSessionValues["ruhi.studio.point.$name"]?.toIntOrNull()?.takeIf{it in state.points.indices} ?: throw Clarify("Create point $name first.")
        fun newShape(type:Shape2DType,points:List<Vec2>):WorkspaceState {
            require(state.shapes.size<80 && state.points.size+points.size<=400) { "Geometry object budget reached." }
            val id=generateSequence(1){it+1}.map{"ruhi-shape-$it"}.first{id->state.shapes.none{it.id==id}}
            val shape=Shape2D(id,type,points.indices.map{state.points.size+it})
            return state.copy(points=state.points+points,shapes=state.shapes+shape,labSessionValues=state.labSessionValues+("ruhi.studio.selected.shape" to id))
        }
        when(op) {
            is RuhiOperation.CreatePoint -> { require(state.points.size<400);require("ruhi.studio.point.${op.name}" !in state.labSessionValues) { "Point ${op.name} already exists." };return state.copy(points=state.points+op.position,labSessionValues=state.labSessionValues+("ruhi.studio.point.${op.name}" to state.points.size.toString())) to "Created point ${op.name} at (${op.position.x}, ${op.position.y})." }
            is RuhiOperation.CreateShape -> {
                val d=op.dimensions;val center=op.center
                val points=when(op.type) { Shape2DType.Circle->listOf(center,center+Vec2(d[0],0.0));Shape2DType.Square,Shape2DType.Rectangle->{val w=d[0];val h=d.getOrElse(1){w};listOf(Vec2(-w/2,-h/2),Vec2(w/2,-h/2),Vec2(w/2,h/2),Vec2(-w/2,h/2)).map{it+center}};else->listOf(center,center+Vec2(d[0],0.0),center+Vec2(d[1],d[2])) }
                return newShape(op.type,points) to "Created ${op.type.name.lowercase()} with the specified dimensions. Measurements use the same control points as touch editing."
            }
            is RuhiOperation.ConnectPoints -> { val first=point(op.first);val second=point(op.second);require(first!=second);val id=generateSequence(1){it+1}.map{"ruhi-segment-$it"}.first{id->state.shapes.none{it.id==id}};val shape=Shape2D(id,Shape2DType.Segment,listOf(first,second),"${op.first}${op.second}");return state.copy(shapes=state.shapes+shape,labSessionValues=state.labSessionValues+("ruhi.studio.selected.shape" to id)) to "Connected ${op.first} and ${op.second}; moving either point updates the segment." }
            is RuhiOperation.Distance -> { val d=state.points[point(op.first)].distanceTo(state.points[point(op.second)]);return state to "Distance ${op.first}${op.second} = $d units, computed from their current coordinates." }
            is RuhiOperation.Midpoint -> { val a=point(op.first);val b=point(op.second);val next=state.points.size;val p=(state.points[a]+state.points[b])*.5;return state.copy(points=state.points+p,pointDependencies=state.pointDependencies+PointDependency(next,listOf(a,b),PointDependencyType.Midpoint,"Midpoint ${op.first}${op.second}")) to "Midpoint = (${p.x}, ${p.y}); it remains dependent on both endpoints." }
            is RuhiOperation.CreateSolid -> { require(state.solids.size<24) { "Solid budget reached." };val mesh=SolidMeshFactory.create(op.solid);require(mesh.vertices.isNotEmpty() && mesh.vertices.all{it.x.isFinite()&&it.y.isFinite()&&it.z.isFinite()});val i=state.solids.size;var values=state.labSessionValues+("ruhi.studio.selected.solid" to i.toString());if(op.animation!=null) values=values+(RuhiMotionEngine.PREFIX+i to op.animation.encode());return state.copy(solids=state.solids+op.solid,labSessionValues=values) to "Created ${op.solid.type.name.lowercase()}${if(op.animation!=null) "; rotating around Y at 30 degrees per second" else ""}. Volume = ${Geometry3D.measure(op.solid).volume} cubic units." }
            is RuhiOperation.Measure -> {
                if(state.module==MathModule.Geometry3D) { val solid=state.solids[solidIndex(state,c)];val m=Geometry3D.measure(solid);return state to when(op.kind){RuhiMeasure.VOLUME->"Volume = ${m.volume} cubic units.";RuhiMeasure.SURFACE_AREA->"Surface area = ${m.surfaceArea} square units.";RuhiMeasure.EXPLAIN->"${solid.type}: volume ${m.volume} cubic units; surface area ${m.surfaceArea} square units. Computed from current dimensions.";else->throw Unsupported("Use volume or surface area for a solid.")} }
                val shape=selectedShape(state,c);val p=shape.pointIndices.map(state.points::get);val circle=RuhiCircleLanguage.isCircle(shape);val radius=if(circle) requireNotNull(RuhiCircleLanguage.describe(state,shape)).radius else 0.0;val area=if(circle) Math.PI*radius.pow(2) else abs(Geometry2D.polygonArea(p));val perimeter=if(circle) 2*Math.PI*radius else if(p.size==2) p[0].distanceTo(p[1]) else p.indices.sumOf{i->p[i].distanceTo(p[(i+1)%p.size])}
                return state to when(op.kind){RuhiMeasure.AREA->"Area = $area square units.";RuhiMeasure.PERIMETER->"Perimeter = $perimeter units.";RuhiMeasure.EXPLAIN->"${shape.type}: area $area square units; perimeter $perimeter units. Measurements recompute from current control points.";else->throw Unsupported("Use area or perimeter for a 2D shape.")}
            }
            else -> Unit
        }
        if(state.module==MathModule.Geometry3D) {
            val i=solidIndex(state,c);val pose=RuhiMotionEngine.solids(state,clockMillis())[i];var values=state.labSessionValues-RuhiMotionEngine.PREFIX.plus(i)
            val next=when(op) {
                is RuhiOperation.SetPosition->pose.copy(position=op.center)
                is RuhiOperation.Translate->pose.copy(position=pose.position+op.delta)
                is RuhiOperation.Scale->pose.copy(width=pose.width*op.factor,height=pose.height*op.factor,depth=pose.depth*op.factor,radius=pose.radius*op.factor,topRadius=pose.topRadius*op.factor)
                is RuhiOperation.Rotate->pose.copy(rotation=pose.rotation+when(op.axis){"x"->Vec3(op.degrees,0.0,0.0);"y"->Vec3(0.0,op.degrees,0.0);else->Vec3(0.0,0.0,op.degrees)})
                RuhiOperation.StopMotion->pose
                RuhiOperation.DeleteSelected->{ StudioReferenceMetadata.requireSafeSolidRemoval(state,setOf(i));val keep=state.solids.indices.filter{it!=i};values=values.filterKeys{!it.startsWith(RuhiMotionEngine.PREFIX)}-"ruhi.studio.selected.solid";keep.forEachIndexed { new,old->state.labSessionValues[RuhiMotionEngine.PREFIX+old]?.let{values=values+(RuhiMotionEngine.PREFIX+new to it)} };return state.copy(solids=keep.map(state.solids::get),labSessionValues=StudioReferenceMetadata.remapSolidAppearance(values,keep)) to "Deleted the selected solid." }
                is RuhiOperation.SetColor->throw Unsupported("Solid appearance remains controlled by the existing object inspector in this adapter.")
                else->throw Unsupported("Unsupported solid action.")
            }
            require(listOf(next.width,next.height,next.depth,next.radius).all{it.isFinite()&&it>0&&it<=500}) { "Resulting dimensions exceed the solid budget." }
            require(listOf(next.position.x,next.position.y,next.position.z).all{it.isFinite()&&abs(it)<=10000})
            return state.copy(solids=state.solids.mapIndexed{index,s->if(index==i) next else s},labSessionValues=values) to "Updated the selected ${next.type.name.lowercase()} using validated dimensions and coordinates."
        }
        val shape=selectedShape(state,c);require(!shape.locked) { "Unlock the selected shape first." }
        if(op==RuhiOperation.DeleteSelected) { require(state.geometryGroups.none{shape.id in it.shapeIds} && state.pointDependencies.none{d->d.inputIndices.any{it in shape.pointIndices}}) { "Remove dependent constructions or groups first." };return state.copy(shapes=state.shapes.filterNot{it.id==shape.id},labSessionValues=state.labSessionValues-"ruhi.studio.selected.shape") to "Deleted the selected shape; shared construction points were retained." }
        if(op is RuhiOperation.SetColor) return state.copy(shapes=state.shapes.map{if(it.id==shape.id) it.copy(styleKey=op.hex) else it}) to "Changed the selected shape color."
        require(state.geometryConstraints.none { shape.id in it.shapeIds || it.pointIndices.any { p->p in shape.pointIndices } }) { "Use the construction tools for a constrained shape; the assistant will not bypass its constraints." }
        require(shape.pointIndices.none{p->state.pointDependencies.any{it.outputIndex==p}}) { "Move the parent construction points instead of a dependent point." }
        val points=shape.pointIndices.map(state.points::get);val center=if(shape.type==Shape2DType.Circle) points.first() else points.reduce(Vec2::plus)*(1.0/points.size)
        val updated=state.points.mapIndexed { index,p->if(index !in shape.pointIndices) p else when(op) {
            is RuhiOperation.SetPosition->p+Vec2(op.center.x,op.center.y)-center
            is RuhiOperation.Translate->p+Vec2(op.delta.x,op.delta.y)
            is RuhiOperation.Scale->center+(p-center)*op.factor
            is RuhiOperation.Rotate->{require(op.axis=="z") { "2D shapes rotate about Z." };val a=Math.toRadians(op.degrees);val v=p-center;center+Vec2(v.x*cos(a)-v.y*sin(a),v.x*sin(a)+v.y*cos(a))}
            else->throw Unsupported("Unsupported geometry action.")
        } }
        require(updated.all{it.x.isFinite()&&it.y.isFinite()&&abs(it.x)<=10000&&abs(it.y)<=10000})
        return state.copy(points=updated).recomputed() to "Updated the selected shape; shared points and dependent measurements recompute in the geometry engine."
    }
}
