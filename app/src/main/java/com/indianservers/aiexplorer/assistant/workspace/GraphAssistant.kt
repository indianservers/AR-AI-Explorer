package com.indianservers.aiexplorer.assistant.workspace

import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import java.util.Locale

enum class GraphCommandStatus { APPLIED, INVALID, UNSUPPORTED, CLARIFICATION }
enum class GraphIntent { PLOT, DELETE, CLEAR, RESET, VISIBILITY, MATERIAL, OPACITY, COLOR, HIGHLIGHT, WIREFRAME, GRID, UNDO, REDO, DUPLICATE, SELECT, MOVE, SCALE, ROTATE, GROUP, UNGROUP, MIRROR }
data class GraphAction(val intent: GraphIntent, val argument: String = "", val target: String = "", val amount: Double? = null, val position: Vec3 = Vec3(0.0, 0.0, 0.0))
data class GraphCommandResult(
    val state: WorkspaceState,
    val status: GraphCommandStatus,
    val message: String,
    val actions: List<GraphAction> = emptyList(),
    val elapsedNanos: Long = 0,
)

/** Local, deterministic commands. No generated code, model training, network, or shared memory. */
class GraphAssistant(private val maximumLayers: Int = 24, private val maximumClauses: Int = 16) {
    companion object {
        const val SELECTION = "ruhi.graph.selection.Graph3D"
        fun selectionKey(module: MathModule) = if (module == MathModule.Graph2D) "ruhi.graph.selection.Graph2D" else SELECTION
        const val TYPE_PREFIX = "ruhi.graph.type."
        private val instruction = Regex("(?i)^(?:(?:please|can you|could you)\\s+)?(?:relocate|reposition|enlarge|shrink|cluster|erase|dissolve|break apart|combine|create|draw|add|plot|graph|generate|make|delete|remove|clear|reset|move|shift|place|position|put|translate|align|distribute|double|reduce|increase|scale|change|set|stretch|resize|restore|rotate|turn|spin|stop|tilt|animate|give|apply|display|hide|show|highlight|label|stack|arrange|connect|find|measure|group|ungroup|duplicate|mirror|select|undo|redo|repeat)\\b")
        fun isGraphInstruction(text: String): Boolean = instruction.containsMatchIn(text.trim())
        /** Native linked controllers normalize the primary surface ID; keep conversational IDs in sync. */
        fun rebindSurfaceContext(source: List<SpatialSurfaceLayer>, next: WorkspaceState): WorkspaceState {
            val aliases = source.zip(next.surfaceLayers).associate { it.first.id to it.second.id }
            val valid = next.surfaceLayers.map { it.id }.toSet()
            val values = linkedMapOf<String,String>()
            next.labSessionValues.forEach { (key,value) ->
                when {
                    key == SELECTION -> (aliases[value] ?: value).takeIf { it in valid }?.let { values[key] = it }
                    key.startsWith("ruhi.graph.dimension.") -> (aliases[key.removePrefix("ruhi.graph.dimension.")] ?: key.removePrefix("ruhi.graph.dimension.")).takeIf { it in valid }?.let { values["ruhi.graph.dimension.$it"] = value }
                    key.startsWith(TYPE_PREFIX) -> (aliases[key.removePrefix(TYPE_PREFIX)] ?: key.removePrefix(TYPE_PREFIX)).takeIf { it in valid }?.let { values[TYPE_PREFIX + it] = value }
                    key.startsWith("ruhi.graph.highlight.") -> {
                        val old = (aliases.keys + valid).firstOrNull { key.startsWith("ruhi.graph.highlight.$it.") }
                        if (old != null) values[key.replace("ruhi.graph.highlight.$old.", "ruhi.graph.highlight.${aliases[old] ?: old}.")] = value
                    }
                    else -> values[key] = value
                }
            }
            return next.copy(labSessionValues = values)
        }
    }
    private class Rejected(val status: GraphCommandStatus, message: String) : IllegalArgumentException(message)
    private fun unsupported(message: String): Nothing = throw Rejected(GraphCommandStatus.UNSUPPORTED, message)
    private fun invalid(message: String): Nothing = throw Rejected(GraphCommandStatus.INVALID, message)

    /** The UI and test harness call this exact boundary, sharing the workspace's Undo/Redo history. */
    fun execute(state: WorkspaceState, text: String, history: CommandHistory): GraphCommandResult {
        val started = System.nanoTime()
        var actions = emptyList<GraphAction>()
        return try {
            require(state.module == MathModule.Graph2D || state.module == MathModule.Graph3D) { "Open 2D Graph or 3D Graph first." }
            require(text.length <= 2000) { "Please keep commands under 2,000 characters." }
            val normalized = text.trim().replace(Regex("(?i)^(?:please|can you|could you)\\s+"), "").replace(Regex("\\s+"), " ").trimEnd('.', '?')
            val clauses = normalized.split(Regex("(?i)\\s*(?:,?\\s+then\\s+|;\\s*then\\s+|\\s+and immediately\\s+|,\\s*(?:and )?(?=(?:create|group|ungroup|rotate|delete|reset|move)\\b)|\\s+and (?=(?:move|color)\\b))"))
            require(clauses.size <= maximumClauses) { "Too many instructions in one command." }
            actions = clauses.flatMapIndexed { index, clause -> expand(if (index > 0 && clause.trim().startsWith("a ")) "create ${clause.trim()}" else clause, state.module) }
            // History operations cannot be rolled back by a staged immutable scene. Reject mixed batches.
            if (actions.any { it.intent in setOf(GraphIntent.UNDO, GraphIntent.REDO) }) {
                require(actions.size == 1) { "Run Undo or Redo separately from scene edits." }
                val action = actions.single()
                val count = action.amount?.toInt() ?: 1
                require(count in 1..80) { "Undo/Redo count must be between 1 and 80." }
                val available = if (action.intent == GraphIntent.UNDO) history.protocol.size else history.timeline.size - history.protocol.size
                require(available >= count) { "Only $available action(s) available to ${action.intent.name.lowercase()}." }
                val next = if (action.intent == GraphIntent.UNDO) history.undoMany(state, count) else history.redoMany(state, count)
                GraphCommandResult(next, GraphCommandStatus.APPLIED, "${action.intent.name.lowercase().replaceFirstChar(Char::uppercase)}: $count action(s).", actions)
            } else {
                var staged = state
                actions.forEach { staged = apply(staged, it) }
                val unchanged = staged == state
                val next = history.execute(state, GraphWorkspaceCommand(state, GraphWorkspaceCommand.reconcileDocument(state, staged), "Ruhi: ${text.take(100)}"))
                val message = if (unchanged) "The command completed with no net scene change." else "Updated ${if (state.module == MathModule.Graph3D) "3D" else "2D"} Graph: ${actions.size} action(s)."
                GraphCommandResult(next, GraphCommandStatus.APPLIED, message, actions)
            }
        } catch (error: RuntimeException) {
            GraphCommandResult(state, (error as? Rejected)?.status ?: GraphCommandStatus.INVALID, error.message ?: "Invalid graph command.", actions)
        }.let { it.copy(elapsedNanos = System.nanoTime() - started) }
    }

    private fun expand(source: String, module: MathModule): List<GraphAction> {
        val text = source.trim().trimEnd('.').lowercase(Locale.US)
        val bounded="cube|sphere|cylinder|cone|torus|pyramid|octahedron|rectangular prism"
        Regex("place (?:a|an) ($bounded) and (?:a|an) ($bounded) side by side").matchEntire(text)?.let { match ->
            return listOf(parse("create a ${match.groupValues[1]}", module).copy(position=Vec3(-3.0,0.0,0.0)),
                parse("create a ${match.groupValues[2]}", module).copy(position=Vec3(3.0,0.0,0.0)))
        }
        Regex("(?:place|stack|distribute) (three|five|six|\\d+) (cubes|spheres) (?:in a row|evenly|vertically)").matchEntire(text)?.let { m ->
            val count=m.groupValues[1].toIntOrNull() ?: mapOf("three" to 3,"five" to 5,"six" to 6).getValue(m.groupValues[1]);require(count in 1..maximumLayers)
            val vertical=text.endsWith("vertically");val action=parse("create a ${m.groupValues[2].removeSuffix("s")}",module)
            return List(count) { i -> action.copy(position=if(vertical) Vec3(0.0,0.0,i*2.0) else Vec3((i-(count-1)/2.0)*5,0.0,0.0)) }
        }
        Regex("(?:create|add|draw|generate|make) (\\d+|two|three|five|six|ten|one thousand) (random )?(cubes|spheres)").matchEntire(text)?.let { match ->
            val count = match.groupValues[1].toIntOrNull() ?: mapOf("two" to 2, "three" to 3, "six" to 6, "five" to 5, "ten" to 10, "one thousand" to 1000).getValue(match.groupValues[1])
            require(count in 1..maximumLayers) { "The graph layer budget is $maximumLayers; requested $count. No objects were created." }
            val action = parse("create a ${match.groupValues[3].removeSuffix("s")}", module)
            val random = kotlin.random.Random(text.hashCode())
            return List(count) { index -> action.copy(position = if (match.groupValues[2].isNotEmpty()) Vec3(random.nextDouble(-5.0,5.0),random.nextDouble(-5.0,5.0),random.nextDouble(-3.0,3.0)) else Vec3((index-(count-1)/2.0)*5,0.0,0.0)) }
        }
        return listOf(parse(source,module))
    }

    fun parse(source: String, module: MathModule): GraphAction {
        val text = source.trim().trimEnd('.').lowercase(Locale.US).replace('’', '\'')
        if (Regex("(?:place|put) (?:a|an) .+ (?:inside|on top of) .+").matches(text)) unsupported("Create the objects first, then position a selected object relative to another layer.")
        GraphLanguageRouter.canonical(text)?.let { return parse(it,module) }
        GraphTransformCommands.parse(text)?.let { return it }
        if (text.startsWith("rotate ") && !Regex("[0-9]").containsMatchIn(text)) throw Rejected(GraphCommandStatus.CLARIFICATION,"Specify the rotation angle and axis.")
        if (Regex("(?:move .+ along the [xyz]-axis|make .+ (?:taller|smaller|bigger)|stretch .+ along the [xyz]-axis|rotate .+ (?:around its own center|around the world origin|together))").matches(text)) throw Rejected(GraphCommandStatus.CLARIFICATION,"Specify a numeric distance, scale, or rotation angle and axis.")
        if (text == "move them apart") return GraphAction(GraphIntent.SCALE,"uniform","all spheres",2.0)
        Regex("make another (.+?) just like the previous one").matchEntire(text)?.let { return GraphAction(GraphIntent.DUPLICATE,target="last ${it.groupValues[1]}") }
        Regex("color (.+?) (red|green|blue)").matchEntire(text)?.let { return GraphAction(GraphIntent.COLOR,mapOf("red" to "#FF0000","green" to "#008000","blue" to "#0000FF").getValue(it.groupValues[2]),it.groupValues[1]) }
        if (text.isBlank()) invalid("Enter a graph command.")
        if ("parametric curve" in text) unsupported("3D parametric curves are not connected to the surface-layer renderer. Parametric surfaces use x=...; y=...; z=... with u and v.")
        if (Regex("\\b(?:radius|side length|height) (?:zero|negative|−|-)|\\bnegative (?:height|radius|side length)").containsMatchIn(text)) invalid("Dimensions must be positive finite numbers.")
        Regex("(?:undo|redo)(?: (?:the )?(?:last |undone )?action)?(?: (\\d+) actions?)?").matchEntire(text)?.let {
            return GraphAction(if (text.startsWith("undo")) GraphIntent.UNDO else GraphIntent.REDO, amount = it.groupValues[1].toDoubleOrNull() ?: 1.0)
        }
        if (text in setOf("clear all", "clear all shapes", "clear the graph", "clear the graph but keep the coordinate axes", "remove everything from the scene")) return GraphAction(GraphIntent.CLEAR)
        if (text in setOf("reset all", "reset everything")) return GraphAction(GraphIntent.RESET)
        if (text in setOf("hide the coordinate grid", "hide the grid")) return GraphAction(GraphIntent.GRID, "false")
        if (text in setOf("show the coordinate grid again", "show the grid", "show the coordinate grid")) return GraphAction(GraphIntent.GRID, "true")
        if (text in setOf("show all hidden objects", "show all hidden graphs")) return GraphAction(GraphIntent.VISIBILITY, "true", "all")
        // Specific appearance removal precedes the general object-removal intent.
        if (text == "remove the highlight") return GraphAction(GraphIntent.HIGHLIGHT, "false", "selected")
        Regex("(?:delete|remove) (.+)").matchEntire(text)?.let { return GraphAction(GraphIntent.DELETE, target = it.groupValues[1]) }
        Regex("hide (.+)").matchEntire(text)?.let { return GraphAction(GraphIntent.VISIBILITY, "false", it.groupValues[1]) }
        Regex("(?:duplicate|copy) (.+)").matchEntire(text)?.let { return GraphAction(GraphIntent.DUPLICATE, target = it.groupValues[1]) }
        Regex("select (.+)").matchEntire(text)?.let { return GraphAction(GraphIntent.SELECT, target = it.groupValues[1]) }
        Regex("(?:make|set) (.+?) (?:opacity to |opacity |to )?(\\d+(?:\\.\\d+)?)%").matchEntire(text)?.let {
            val opacity = it.groupValues[2].toDouble() / 100
            require(opacity in 0.0..1.0) { "Opacity must be between 0% and 100%." }
            return GraphAction(GraphIntent.OPACITY, target = it.groupValues[1], amount = opacity)
        }
        Regex("make (.+?) transparent").matchEntire(text)?.let { return GraphAction(GraphIntent.OPACITY, target = it.groupValues[1], amount = .35) }
        Regex("(?:display|show) (.+?) in wireframe mode").matchEntire(text)?.let { return GraphAction(GraphIntent.WIREFRAME, target = it.groupValues[1]) }
        Regex("(?:give (.+?) a metallic finish|make (.+?) look like glass)").matchEntire(text)?.let {
            return GraphAction(GraphIntent.MATERIAL, if (it.groupValues[1].isNotEmpty()) "Metal" else "Glass", it.groupValues[1].ifEmpty { it.groupValues[2] })
        }
        if (text == "restore the default material") return GraphAction(GraphIntent.MATERIAL, "Gloss", "selected")
        val colors = mapOf("red" to "#FF0000", "blue" to "#0000FF", "green" to "#008000", "yellow" to "#FFFF00", "purple" to "#800080", "cyan" to "#00FFFF")
        Regex("(?:change (.+?) color to |make (.+?) |change (.+?) to )(${colors.keys.joinToString("|")})").matchEntire(text)?.let {
            return GraphAction(GraphIntent.COLOR, colors.getValue(it.groupValues[4]), it.groupValues.drop(1).take(3).first { value -> value.isNotEmpty() }.removeSuffix("'s").removeSuffix("'"))
        }
        if (text == "highlight the selected object") return GraphAction(GraphIntent.HIGHLIGHT, "true", "selected")
        // Accept complete equations, not sentences containing an incidental number or object name.
        Regex("(?:plot|graph|draw|add) (?:the (?:surface|plane|curve|cone) )?(.+)").matchEntire(text)?.let {
            if ('=' in it.groupValues[1]) return GraphAction(GraphIntent.PLOT, it.groupValues[1])
        }
        val creation = Regex("(?:create|draw|add|generate|make) (?:a |an |the )?(cube|sphere|cylinder|cone|torus|pyramid|octahedron|rectangular prism|plane)(?: with (radius|side length|height) ([-+]?\\d+(?:\\.\\d+)?))?").matchEntire(text)
        if (creation != null) {
            require(module == MathModule.Graph3D) { "Use 3D Graph for solid surface equations." }
            val parameter = creation.groupValues[2]
            val supportedParameter = when(creation.groupValues[1]) {
                "cube" -> "side length"
                "sphere", "torus" -> "radius"
                "cylinder", "cone", "pyramid" -> "height"
                else -> ""
            }
            if (parameter.isNotEmpty() && parameter != supportedParameter) unsupported("This creation form does not support $parameter for ${creation.groupValues[1]}. Specify a surface equation for other dimensions.")
            val dimension = creation.groupValues[3].toDoubleOrNull() ?: 2.0
            require(dimension.isFinite() && dimension > 0 && dimension <= 50) { "Dimensions must be greater than zero and at most 50 units." }
            return GraphAction(GraphIntent.PLOT, primitive(creation.groupValues[1], dimension), creation.groupValues[1], dimension)
        }
        unsupported("This instruction is not supported in ${module.label}. Ruhi can plot equations, add supported solid surfaces, delete/select/duplicate graph layers, set visibility, opacity, wireframe or material, clear, reset, Undo and Redo. Numeric movement, scaling, rotation and grouping are supported. Geometric constructions and animation are not connected to Graph workspaces.")
    }

    private fun primitive(type: String, size: Double): String {
        val r = size / 2
        return when (type) {
            "cube" -> "max(max(abs(x),abs(y)),abs(z))=$r"
            "rectangular prism" -> "max(max(abs(x)/$size,abs(y)/$r),abs(z)/$r)=1"
            "sphere" -> "x^2+y^2+z^2=${size * size}" // supplied dimension is radius
            "cylinder" -> "max(sqrt(x^2+y^2)-1,abs(z)-$r)=0"
            "cone" -> "max(max(sqrt(x^2+y^2)-(1-z/$size),-z),z-$size)=0"
            "torus" -> "(sqrt(x^2+y^2)-$size)^2+z^2=0.25"
            "pyramid" -> "max(max(max(abs(x),abs(y))-(1-z/$size),-z),z-$size)=0"
            "octahedron" -> "abs(x)+abs(y)+abs(z)=$size"
            else -> "z=0"
        }
    }

    private fun ids(state: WorkspaceState) = if (state.module == MathModule.Graph3D) state.surfaceLayers.map { it.id } else state.functions.map { it.id }
    private fun newId(state: WorkspaceState) = generateSequence(1) { it + 1 }.map { "ruhi-graph-$it" }.first { id -> state.surfaceLayers.none { it.id == id } && state.functions.none { it.id == id } && id !in state.universalMathDocument?.objects.orEmpty() }
    private fun targets(state: WorkspaceState, raw: String): Set<String> {
        val ids = ids(state)
        val target = raw.trim().removePrefix("the ").removePrefix("only the ").trim().removeSuffix("'s")
        if (target in setOf("all", "all objects", "all graphs", "all shapes", "all layers")) return ids.toSet()
        if (target in setOf("selected group","group","the group","grouped objects")) {
            val group=state.surfaceLayers.firstOrNull { it.id==state.labSessionValues[selectionKey(state.module)] }?.graphGroup.orEmpty()
            require(group.isNotEmpty()) { "Select a grouped object first." };return state.surfaceLayers.filter { it.graphGroup==group }.map { it.id }.toSet()
        }
        if(target.startsWith("objects inside ")) return targets(state,target.removePrefix("objects inside "))
        if(target=="them" || target=="selected objects") return state.surfaceLayers.firstOrNull { it.id==state.labSessionValues[selectionKey(state.module)] }?.graphGroup?.takeIf(String::isNotEmpty)?.let { g->state.surfaceLayers.filter{it.graphGroup==g}.map{it.id}.toSet() } ?: ids.toSet()
        Regex("(?:third (?:one|cube)|selected cubes|selected spheres)").matchEntire(target)?.let { return if(target.startsWith("third")) setOf(ids.filter { state.labSessionValues[TYPE_PREFIX+it]=="cube" }.getOrNull(2) ?: invalid("There is no third cube.")) else targets(state,"all "+target.removePrefix("selected ")) }
        if(target.startsWith("all objects except ")) { val color=target.substringAfter("except the ").substringBefore(' ');val hex=mapOf("red" to "#FF0000","blue" to "#0000FF","green" to "#008000")[color] ?: invalid("Specify a supported color.");return state.surfaceLayers.filterNot { it.colorHex==hex }.map { it.id }.toSet() }
        if(target=="smallest sphere") return setOf(state.surfaceLayers.filter{state.labSessionValues[TYPE_PREFIX+it.id]=="sphere"}.minByOrNull { layer-> val p=GraphSurfaceGeometry.mesh(layer,12).vertices; (p.maxOf{it.x}-p.minOf{it.x})*(p.maxOf{it.y}-p.minOf{it.y})*(p.maxOf{it.z}-p.minOf{it.z}) }?.id ?: invalid("No sphere exists."))
        val type = Regex("(?:(?:all|first|last) )?(cube|sphere|cylinder|cone|torus|pyramid|octahedron|plane|rectangular prism)s?").matchEntire(target)?.groupValues?.get(1)
        val candidates = if (type != null) ids.filter { state.labSessionValues[TYPE_PREFIX + it] == type } else ids
        if (type == null && target !in setOf("selected", "selected object", "selected shape", "selected graph", "selected layer", "selected curve", "this graph", "this", "it", "its", "that", "that same object", "last object", "last created object", "first object", "last graph", "first graph")) invalid("No matching graph layer exists. Select one, or say first, last, or all graphs.")
        if (candidates.isEmpty()) invalid("No matching graph layer exists.")
        if (target.startsWith("all ") || target.endsWith("s") && type != null) return candidates.toSet()
        if (target.startsWith("first")) return setOf(candidates.first())
        if (target.startsWith("last")) return setOf(candidates.last())
        val selected = state.labSessionValues[selectionKey(state.module)]?.takeIf(candidates::contains)
        if (selected != null) return setOf(selected)
        if (target.startsWith("selected")) invalid("Select a graph layer first.")
        if (candidates.size == 1) return candidates.toSet()
        throw Rejected(GraphCommandStatus.CLARIFICATION, "Which graph layer should I use? Select one or say first, last, or all graphs.")
    }

    private fun apply(state: WorkspaceState, action: GraphAction): WorkspaceState {
        val is3D = state.module == MathModule.Graph3D
        if (action.intent == GraphIntent.GRID) {
            if (is3D) return state.copy(labSessionValues = state.labSessionValues + ("ruhi.graph.grid" to action.argument))
            return state.copy(graph2DView = state.graph2DView.copy(gridVisible = action.argument.toBoolean()))
        }
        if (action.intent == GraphIntent.CLEAR || action.intent == GraphIntent.RESET) {
            val active = ids(state).toSet()
            val values = state.labSessionValues.filter { (key,value) ->
                !(key == selectionKey(state.module) && value in active) &&
                    !(key.startsWith(TYPE_PREFIX) && key.removePrefix(TYPE_PREFIX) in active) &&
                    !(key.removePrefix("ruhi.graph.dimension.") in active) &&
                    !(key.removePrefix("ruhi.graph2d.vertex.") in active) &&
                    active.none { key.startsWith("ruhi.graph.highlight.$it.") } &&
                    !(is3D && action.intent == GraphIntent.RESET && key == "ruhi.graph.grid")
            }
            return if (is3D) state.copy(surfaceLayers = emptyList(), surfaceExpression = "0", labSessionValues = values,
                graph3DView = if (action.intent == GraphIntent.RESET) Graph3DViewState() else state.graph3DView)
            else state.copy(functions = emptyList(), graphRowMetadata = emptyMap(), labSessionValues = values,
                graph2DView = if (action.intent == GraphIntent.RESET) Graph2DViewState() else state.graph2DView)
        }
        if (action.intent == GraphIntent.PLOT) {
            require(ids(state).size < maximumLayers) { "The graph layer budget is $maximumLayers. Delete some layers first." }
            val id = newId(state)
            val values = state.labSessionValues + (selectionKey(state.module) to id) + (TYPE_PREFIX + id to action.target) + ("ruhi.graph.dimension.$id" to (action.amount ?: 2.0).toString())
            return if (is3D) {
                val parsed = SurfaceInputInterpreter.interpret(action.argument).getOrElse { invalid(it.message ?: "Invalid surface.") }
                val extent = action.amount?.let { maxOf(3.0, it * 1.6) } ?: if (parsed.kind == SpatialSurfaceKind.Implicit) 10.0 else 3.0
                val p = action.position
                val definition = parsed.expression
                val layer = SpatialSurfaceLayer(id, definition, parsed.kind, parsed.expressionY, parsed.expressionZ,
                    domain = SurfaceDomain3D(-extent, extent, -extent, extent), graphTransform = GraphTransform(position=p))
                val mesh = GraphSurfaceGeometry.mesh(layer, 24)
                require(mesh.vertices.isNotEmpty() && (mesh.triangleIndices.isNotEmpty() || mesh.rows > 1)) { "This equation produces no drawable surface in the current domain." }
                state.copy(surfaceLayers = state.surfaceLayers + layer, surfaceExpression = state.surfaceLayers.firstOrNull()?.expression ?: layer.expression, labSessionValues = values,
                    graph3DView = state.graph3DView.copy(zoom = minOf(state.graph3DView.zoom, (3.0 / (p.magnitude()+maxOf(3.0,action.amount ?: 2.0))).coerceIn(.1,1.0).toFloat())))
            } else {
                val expression = MathExpressionNormalizer.normalize(action.argument)
                val graph = GraphAnalysis()
                val drawable = if (graph.definitionKind(expression) == GraphDefinitionKind.Implicit) graph.implicitSegments(expression, -10.0, 10.0, -10.0, 10.0).isNotEmpty()
                    else graph.sampleDefinition(expression, -10.0, 10.0).points.isNotEmpty()
                require(drawable) { "This equation produces no drawable curve in the current domain." }
                state.copy(functions = state.functions + FunctionDefinition(id, GraphEquationNames.next(state.functions.map { it.name }.toSet()), expression, "cyan"), labSessionValues = values)
            }
        }
        val targets = targets(state, action.target)
        if (!is3D && action.intent == GraphIntent.MOVE) {
            if(action.argument!="relative" || action.position.y!=0.0) unsupported("Use a horizontal or vertical relative translation for a 2D graph.")
            val rows=state.functions.map { row ->
                if(row.id !in targets) row else {
                    require(GraphAnalysis().definitionKind(row.expression)==GraphDefinitionKind.Explicit) { "Translation currently requires an explicit y=f(x) curve." }
                    val body=stripEquation(row.expression).replace(Regex("\\bx\\b"),"(x-${action.position.x})")
                    row.copy(expression="($body)+${action.position.z}")
                }
            }
            return state.copy(functions=rows)
        }
        if (action.intent in GraphTransformCommands.intents) {
            if (!is3D) unsupported("Object transforms and groups currently require 3D Graph; 2D equations and appearance remain available.")
            val group = state.surfaceLayers.firstOrNull { it.id in targets }?.graphGroup.orEmpty()
            val affected=if(group.isNotEmpty() && action.target in setOf("selected object","it","its","that same object") && action.intent !in setOf(GraphIntent.GROUP,GraphIntent.UNGROUP)) state.surfaceLayers.filter{it.graphGroup==group}.map{it.id}.toSet() else targets
            return GraphTransformCommands.apply(state,action,affected) { targets(state,it) }
        }
        if (action.intent == GraphIntent.SELECT) return state.copy(labSessionValues = state.labSessionValues + (selectionKey(state.module) to targets.last()))
        if (action.intent == GraphIntent.DUPLICATE) {
            require(ids(state).size + targets.size <= maximumLayers) { "The graph layer budget is $maximumLayers." }
            var next = state
            targets.forEach { source ->
                val id = newId(next)
                next = if (is3D) next.copy(surfaceLayers = next.surfaceLayers + offsetDuplicate(next.surfaceLayers.first { it.id == source }, id))
                    else {
                        val original = next.functions.first { it.id == source }
                        require(GraphAnalysis().definitionKind(original.expression) == GraphDefinitionKind.Explicit) { "Duplicate currently supports explicit 2D curves only." }
                        val shifted = "(${stripEquation(original.expression).replace(Regex("\\bx\\b"), "(x-0.8)" )})+0.5"
                        next.copy(functions = next.functions + original.copy(id = id, name = GraphEquationNames.next(next.functions.map { it.name }.toSet()), expression = shifted))
                    }
                next = next.copy(labSessionValues = next.labSessionValues + (selectionKey(state.module) to id) + (TYPE_PREFIX + id to (state.labSessionValues[TYPE_PREFIX + source] ?: "")) + ("ruhi.graph.dimension.$id" to state.labSessionValues["ruhi.graph.dimension.$source"].orEmpty()))
            }
            return next
        }
        if (!is3D && action.intent in setOf(GraphIntent.MATERIAL, GraphIntent.WIREFRAME)) unsupported("3D materials and wireframe apply to surface layers, not 2D curves.")
        val layers = state.surfaceLayers.filterNot { action.intent == GraphIntent.DELETE && it.id in targets }.map { layer ->
            if (layer.id !in targets) layer else when (action.intent) {
                GraphIntent.VISIBILITY -> layer.copy(visible = action.argument.toBoolean())
                GraphIntent.OPACITY -> layer.copy(opacity = action.amount!!)
                GraphIntent.MATERIAL -> layer.copy(material = SpatialMaterial.valueOf(action.argument))
                GraphIntent.COLOR -> layer.copy(colorHex = action.argument)
                GraphIntent.HIGHLIGHT -> if (action.argument.toBoolean()) layer.copy(colorHex = "#FFFF00", glow = true)
                    else if ("ruhi.graph.highlight.${layer.id}.color" in state.labSessionValues) layer.copy(colorHex = state.labSessionValues["ruhi.graph.highlight.${layer.id}.color"].orEmpty(), glow = state.labSessionValues["ruhi.graph.highlight.${layer.id}.glow"]?.toBoolean() ?: layer.glow)
                    else layer
                GraphIntent.WIREFRAME -> layer.copy(renderMode = SpatialSurfaceRenderMode.Wireframe)
                else -> layer
            }
        }
        val functions = state.functions.filterNot { action.intent == GraphIntent.DELETE && it.id in targets }.map { row ->
            if (row.id !in targets) row else when (action.intent) {
                GraphIntent.VISIBILITY -> row.copy(visible = action.argument.toBoolean())
                GraphIntent.OPACITY -> row.copy(appearance = row.appearance.copy(opacity = action.amount!!.toFloat()))
                GraphIntent.COLOR -> row.copy(colorKey = action.argument)
                GraphIntent.HIGHLIGHT -> row.copy(appearance = row.appearance.copy(width = if (action.argument.toBoolean()) 8f else state.labSessionValues["ruhi.graph.highlight.${row.id}.width"]?.toFloatOrNull() ?: row.appearance.width))
                else -> row
            }
        }
        var values = if (action.intent == GraphIntent.DELETE) state.labSessionValues.filterKeys { key -> key !in targets.flatMap { id -> listOf(TYPE_PREFIX + id,"ruhi.graph.dimension.$id","ruhi.graph2d.vertex.$id") } && !(key == selectionKey(state.module) && state.labSessionValues[selectionKey(state.module)] in targets) && targets.none { key.startsWith("ruhi.graph.highlight.$it.") } } else state.labSessionValues
        if (is3D && action.intent == GraphIntent.HIGHLIGHT && action.argument.toBoolean()) {
            state.surfaceLayers.filter { it.id in targets }.forEach { layer ->
                if ("ruhi.graph.highlight.${layer.id}.color" !in values) values = values + ("ruhi.graph.highlight.${layer.id}.color" to layer.colorHex) + ("ruhi.graph.highlight.${layer.id}.glow" to layer.glow.toString())
            }
        }
        if (!is3D && action.intent == GraphIntent.HIGHLIGHT && action.argument.toBoolean()) state.functions.filter { it.id in targets }.forEach { row ->
            if ("ruhi.graph.highlight.${row.id}.width" !in values) values = values + ("ruhi.graph.highlight.${row.id}.width" to row.appearance.width.toString())
        }
        if (action.intent == GraphIntent.HIGHLIGHT && !action.argument.toBoolean()) values = values.filterKeys { key -> targets.none { key.startsWith("ruhi.graph.highlight.$it.") } }
        return if (is3D) state.copy(surfaceLayers = layers, surfaceExpression = layers.firstOrNull()?.expression ?: "0", labSessionValues = values)
            else state.copy(functions = functions, graphRowMetadata = state.graphRowMetadata.filterKeys { id -> functions.any { it.id == id } }, labSessionValues = values)
    }

    private fun offsetDuplicate(layer: SpatialSurfaceLayer, id: String): SpatialSurfaceLayer {
        val shift = { expression: String -> Regex("\\b[xyz]\\b").replace(expression) { match -> when(match.value) { "x" -> "(x-0.8)"; "y" -> "(y-0.8)"; else -> "z" } } }
        val domain = layer.domain.copy(uMax = layer.domain.uMax + 1, vMax = layer.domain.vMax + 1)
        return when (layer.kind) {
            SpatialSurfaceKind.Explicit -> layer.copy(id = id, expression = shift(layer.expression), domain = domain)
            SpatialSurfaceKind.Implicit -> layer.copy(id = id, expression = shift(layer.expression), domain = domain)
            SpatialSurfaceKind.Parametric -> layer.copy(id = id, expression = "(${layer.expression})+0.8", expressionY = "(${layer.expressionY})+0.8")
        }
    }

}
