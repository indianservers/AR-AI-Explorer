package com.indianservers.aiexplorer.phase2.mathstudio

import com.indianservers.aiexplorer.core.ExactRational
import com.indianservers.aiexplorer.core.SymbolicCasEngine
import com.indianservers.aiexplorer.workspace.UniversalMathDocument
import com.indianservers.aiexplorer.workspace.UniversalMathKind
import com.indianservers.aiexplorer.workspace.UniversalMathObject
import com.indianservers.aiexplorer.workspace.UniversalMathObjectFactory
import com.indianservers.aiexplorer.workspace.UniversalMathPayload

enum class AlgebraSortMode(val label: String) {
    Type("Type"), Dependency("Dependency"), Construction("Construction"), Name("Name"), Layer("Layer")
}

enum class AlgebraDisplayMode(val label: String) {
    Value("Value"), Definition("Definition"), Command("Command"), Exact("Exact"), Decimal("Decimal"), Mixed("Mixed"), Spoken("Spoken")
}

enum class AlgebraFilter(val label: String) {
    All("All"), Free("Free"), Dependent("Dependent"), Visible("Visible"), Hidden("Hidden"), Auxiliary("Auxiliary"), Invalid("Invalid"), Selected("Selected")
}

enum class AlgebraLabelMode { Name, NameAndValue, Value, Caption }

enum class AlgebraKeyboardAction { Previous, Next, Edit, Rename, ToggleVisibility, Delete, SelectAll, OpenInspector, None }

enum class AlgebraEquationAction(val label: String) {
    Solve("Solve"), Graph("Graph"), Rearrange("Rearrange"), InspectVariables("Variables"), ConvertForm("Convert form"), Substitute("Substitute"), CreateGeometry("Create geometry")
}

data class AlgebraObjectStyle(
    val visible: Boolean = true,
    val locked: Boolean = false,
    val auxiliary: Boolean = false,
    val layer: Int = 0,
    val caption: String? = null,
    val folder: String = "Workspace",
    val labelMode: AlgebraLabelMode = AlgebraLabelMode.NameAndValue,
    val trace: Boolean = false,
    val animated: Boolean = false,
    val selectionAllowed: Boolean = true,
    val opacity: Float = 1f,
    val strokeWidth: Float = 2f,
    val visibilityCondition: String? = null,
)

data class AlgebraEntry(
    val id: String,
    val name: String,
    val kind: UniversalMathKind,
    val definition: String,
    val value: String,
    val command: String,
    val decimal: String? = null,
    val dependencies: Set<String>,
    val dependents: Set<String>,
    val sourceView: String,
    val style: AlgebraObjectStyle,
    val valid: Boolean,
    val issue: String? = null,
    val spoken: String = "",
) {
    val free: Boolean get() = dependencies.isEmpty()
    fun rendered(mode: AlgebraDisplayMode): String = when (mode) {
        AlgebraDisplayMode.Value, AlgebraDisplayMode.Exact -> value
        AlgebraDisplayMode.Definition -> definition
        AlgebraDisplayMode.Command -> command
        AlgebraDisplayMode.Decimal -> decimal ?: value
        AlgebraDisplayMode.Mixed -> listOf(value, decimal?.takeIf { it != value }?.let { "≈ $it" }).filterNotNull().joinToString(" ")
        AlgebraDisplayMode.Spoken -> spoken
    }
}

data class AlgebraRedefinitionPreview(
    val objectId: String,
    val accepted: Boolean,
    val oldDefinition: String,
    val newDefinition: String,
    val newDependencies: Set<String>,
    val affectedObjects: Set<String>,
    val message: String,
)

data class AlgebraCommandSuggestion(
    val name: String,
    val signature: String,
    val description: String,
    val compatibleObjectIds: List<String> = emptyList(),
)

data class ParsedAlgebraObject(val value: UniversalMathObject, val displayDefinition: String)

/** Typed input parser for the shared Algebra view. It intentionally preserves unsupported symbolic
 * structures as inspectable objects instead of flattening everything into a graph function. */
object UniversalAlgebraParser {
    private val cas = SymbolicCasEngine()
    private val assignment = Regex("^([A-Za-z][A-Za-z0-9_]*)(?:\\(([^)]*)\\))?\\s*[:=]\\s*(.+)$")
    private val identifier = Regex("[A-Za-z][A-Za-z0-9_]*")
    private val inequalities = listOf("<=", ">=", "≤", "≥", "<", ">", "!=", "≠")
    private val relationSymbols = listOf("∈", "∉", "⊂", "⊆", "⊥", "⟂", "∥", "∦")
    private val geometryCommands = setOf("line", "ray", "segment", "circle", "ellipse", "hyperbola", "parabola", "polygon", "intersect", "tangent", "midpoint", "perpendicular", "parallel", "plane")

    fun parse(input: String, document: UniversalMathDocument, generatedIndex: Int): ParsedAlgebraObject {
        val clean = normalizeMath(input.trim())
        require(clean.isNotBlank()) { "Enter a mathematical object." }
        val match = assignment.matchEntire(clean)
        val explicitName = match?.groupValues?.get(1)
        val arguments = match?.groupValues?.get(2).orEmpty().split(',').map(String::trim).filter(String::isNotBlank)
        val body = match?.groupValues?.get(3)?.trim() ?: clean
        val name = explicitName ?: inferredName(body, generatedIndex)
        val id = uniqueId(name, document)
        val boundSymbols = arguments.toSet() + sequenceIterator(body).orEmpty() + parametricIterator(body).orEmpty()
        val dependencies = references(body, document, boundSymbols = boundSymbols)
        val commandName = body.substringBefore('(').trim().lowercase()

        val value = when {
            arguments.isNotEmpty() && body.startsWith("If(", true) -> UniversalMathObject(id, UniversalMathKind.PiecewiseFunction, "$name(${arguments.joinToString()})",
                UniversalMathPayload.Properties(mapOf("definition" to body, "piecewise" to "true")), dependencies, sourceView = "algebra/graph")
            arguments.isNotEmpty() -> symbolic(id, UniversalMathKind.Function, "$name(${arguments.joinToString()})", body, dependencies, "algebra/graph")
            matrix(body) != null -> UniversalMathObjectFactory.matrix(id, name, matrix(body)!!)
            numericList(body) != null -> UniversalMathObjectFactory.dataList(id, name, numericList(body)!!)
            body.startsWith("System(", true) || (';' in body && body.split(';').count { '=' in it } > 1) -> UniversalMathObject(id, UniversalMathKind.EquationSystem, name,
                UniversalMathPayload.Properties(mapOf("definition" to body, "equations" to body.removePrefix("System(").removeSuffix(")").replace(';', ','))), dependencies, sourceView = "algebra/CAS/graph")
            body.startsWith("Parametric(", true) || Regex("^\\(x\\s*,\\s*y(?:\\s*,\\s*z)?\\)\\s*=").containsMatchIn(body) -> UniversalMathObject(id, UniversalMathKind.ParametricEquation, name,
                UniversalMathPayload.Properties(mapOf("definition" to body, "parameter" to "t")), dependencies, sourceView = "algebra/graph")
            relationSymbols.any(body::contains) -> UniversalMathObject(id, UniversalMathKind.Relation, name,
                UniversalMathPayload.Properties(mapOf("definition" to body, "relation" to relationSymbols.first(body::contains), "value" to "dynamic")), dependencies, sourceView = "algebra/geometry")
            tuple(body) != null && name.firstOrNull()?.isUpperCase() == true -> tuple(body)!!.let { values ->
                if (values.size == 2) UniversalMathObjectFactory.point2D(id, name, values[0], values[1], dependencies)
                else UniversalMathObjectFactory.point3D(id, name, values[0], values[1], values[2], dependencies)
            }
            tuple(body) != null -> UniversalMathObjectFactory.vector(id, name, tuple(body)!!).copy(dependencies = dependencies)
            body.startsWith("Parameter(", true) || body.startsWith("Slider(", true) -> UniversalMathObject(id, UniversalMathKind.Parameter, name,
                UniversalMathPayload.Properties(mapOf("definition" to body, "value" to body.substringAfter('(').substringBefore(',').substringBefore(')'))), dependencies, sourceView = "algebra/slider")
            body.startsWith("Text(", true) || (body.startsWith('"') && body.endsWith('"')) -> UniversalMathObject(id, UniversalMathKind.Text, name,
                UniversalMathPayload.Properties(mapOf("definition" to body, "text" to body.removePrefix("Text(").removeSurrounding("\"").removeSuffix(")"))), dependencies, sourceView = "algebra/annotation")
            body.equals("true", true) || body.equals("false", true) || body.startsWith("Boolean(", true) -> UniversalMathObject(id, UniversalMathKind.Boolean, name,
                UniversalMathPayload.Properties(mapOf("definition" to body, "value" to body.substringAfter('(').substringBefore(')').lowercase())), dependencies, sourceView = "algebra/condition")
            body.toDoubleOrNull() != null || runCatching { ExactRational.parse(body) }.isSuccess -> UniversalMathObjectFactory.scalar(id, name, ExactRational.parse(body))
            commandName in geometryCommands -> UniversalMathObject(id, geometryKind(commandName), name,
                UniversalMathPayload.Properties(mapOf("definition" to body, "command" to body, "type" to commandName)), dependencies, sourceView = "algebra/geometry")
            body.startsWith("Sequence(", true) -> UniversalMathObject(id, UniversalMathKind.Sequence, name,
                UniversalMathPayload.Properties(mapOf("definition" to body, "sequence" to "true")), dependencies, sourceView = "algebra/sequence")
            body.startsWith("Angle(", true) -> UniversalMathObject(id, UniversalMathKind.Angle, name,
                UniversalMathPayload.Properties(mapOf("definition" to body, "command" to body)), dependencies, sourceView = "algebra/measurement")
            body.startsWith("Distance(", true) || body.startsWith("Length(", true) || body.startsWith("Area(", true) || body.startsWith("Volume(", true) -> UniversalMathObject(id, UniversalMathKind.Measurement, name,
                UniversalMathPayload.Properties(mapOf("definition" to body, "command" to body)), dependencies, sourceView = "algebra/measurement")
            body.startsWith("CAS(", true) -> UniversalMathObject(id, UniversalMathKind.NotebookCell, name,
                UniversalMathPayload.Properties(mapOf("definition" to body, "result" to body.substringAfter('(').substringBeforeLast(')'))), dependencies, sourceView = "CAS")
            inequalities.any(body::contains) -> UniversalMathObject(id, if (booleanIntent(explicitName)) UniversalMathKind.Boolean else UniversalMathKind.Inequality, name,
                UniversalMathPayload.Properties(mapOf("definition" to body, "relation" to "inequality", "value" to "dynamic")), dependencies, sourceView = "algebra/graph")
            '=' in body -> UniversalMathObject(id, equationKind(body), name,
                UniversalMathPayload.Properties(mapOf("definition" to body, "relation" to "equation")), dependencies, sourceView = "algebra/graph")
            explicitName != null -> symbolic(id, UniversalMathKind.Expression, name, body, dependencies, "algebra/CAS")
            else -> symbolic(id, UniversalMathKind.Expression, name, body, dependencies, "algebra/CAS")
        }
        return ParsedAlgebraObject(value, clean)
    }

    /** Reparse an edit as a complete typed object while retaining its stable identity. This is also
     * what makes kind-changing redefinitions transactional (for example scalar -> point). */
    fun parseRedefinition(input: String, current: UniversalMathObject, document: UniversalMathDocument): ParsedAlgebraObject {
        val clean = normalizeMath(input.trim())
        require(clean.isNotBlank()) { "Enter a new definition." }
        val currentSymbol = current.name.substringBefore('(')
        val suppliedAssignment = assignment.matchEntire(clean)
        val source = if (suppliedAssignment == null) "${current.name} = $clean" else {
            val suppliedSymbol = suppliedAssignment.groupValues[1]
            require(suppliedSymbol == currentSymbol) { "Redefinition must keep the name ${current.name}; use Rename to change object identity." }
            clean
        }
        val parsed = parse(source, document, document.objects.size + 1)
        val rebound = parsed.value.copy(
            id = current.id,
            name = current.name,
            assumptions = current.assumptions,
            objectRevision = current.objectRevision,
        )
        return ParsedAlgebraObject(rebound, clean)
    }

    fun references(
        source: String,
        document: UniversalMathDocument,
        excludingId: String? = null,
        boundSymbols: Set<String> = emptySet(),
    ): Set<String> {
        val names = identifier.findAll(normalizeMath(source)).map { it.value }.filterNot(boundSymbols::contains).toSet()
        return document.objects.values.filter { it.id != excludingId && (it.id in names || it.name.substringBefore('(') in names) }.mapTo(linkedSetOf()) { it.id }
    }

    private fun symbolic(id: String, kind: UniversalMathKind, name: String, source: String, dependencies: Set<String>, sourceView: String): UniversalMathObject {
        val parsed = runCatching { cas.parse(source) }
        return UniversalMathObject(id, kind, name, UniversalMathPayload.Symbolic(source, parsed.getOrNull(), parsed.exceptionOrNull()?.message), dependencies, sourceView = sourceView)
    }

    private fun inferredName(body: String, index: Int) = when {
        inequalities.any(body::contains) || '=' in body -> "eq$index"
        else -> "a$index"
    }

    private fun geometryKind(command: String) = when (command) {
        "line", "perpendicular", "parallel", "tangent" -> UniversalMathKind.Line
        "ray" -> UniversalMathKind.Ray
        "segment" -> UniversalMathKind.Segment
        "circle" -> UniversalMathKind.Circle
        "ellipse", "hyperbola", "parabola" -> UniversalMathKind.Conic
        "plane" -> UniversalMathKind.Plane
        else -> UniversalMathKind.GeometryConstruction
    }

    private fun booleanIntent(name: String?) = name?.lowercase() in setOf("b", "bool", "condition", "visible", "enabled", "valid")

    private fun equationKind(source: String): UniversalMathKind {
        val compact = source.replace(" ", "").lowercase()
        val coordinateRelation = 'x' in compact || 'y' in compact
        if (!coordinateRelation) return UniversalMathKind.Equation
        val quadratic = Regex("(?:x|y|\\([^)]*[xy][^)]*\\))\\^2").containsMatchIn(compact)
        if (quadratic) {
            val circleLike = Regex("(?:x|\\([^)]*x[^)]*\\))\\^2\\+(?:y|\\([^)]*y[^)]*\\))\\^2").containsMatchIn(compact)
            return if (circleLike) UniversalMathKind.Circle else UniversalMathKind.Conic
        }
        val nonlinear = Regex("[xy]\\^[2-9]|(?:sin|cos|tan|sqrt|abs|exp|log|ln)\\(").containsMatchIn(compact)
        return if (!nonlinear) UniversalMathKind.Line else UniversalMathKind.Equation
    }

    private fun normalizeMath(source: String): String = source
        .replace("\u2212", "-")
        .replace("\u00D7", "*")
        .replace("\u00F7", "/")
        .replace("\u00B2", "^2")
        .replace("\u00B3", "^3")
        .replace("\u2070", "^0")
        .replace("\u00B9", "^1")
        .replace("\u2264", "<=")
        .replace("\u2265", ">=")
        .replace("\u2260", "!=")

    private fun sequenceIterator(source: String): Set<String> {
        if (!source.startsWith("Sequence(", ignoreCase = true) || !source.endsWith(')')) return emptySet()
        val arguments = topLevelArguments(source.substringAfter('(').dropLast(1))
        return arguments.getOrNull(1)?.takeIf { identifier.matches(it) }?.let(::setOf).orEmpty()
    }

    private fun parametricIterator(source: String): Set<String> =
        if (source.startsWith("Parametric(", ignoreCase = true)) setOf("t") else emptySet()

    private fun topLevelArguments(source: String): List<String> {
        val result = mutableListOf<String>()
        var depth = 0
        var start = 0
        source.forEachIndexed { index, character ->
            when (character) {
                '(', '{', '[' -> depth++
                ')', '}', ']' -> depth--
                ',' -> if (depth == 0) {
                    result += source.substring(start, index).trim()
                    start = index + 1
                }
            }
        }
        result += source.substring(start).trim()
        return result
    }

    private fun uniqueId(name: String, document: UniversalMathDocument): String {
        val base = "algebra-" + name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifBlank { "object" }
        return generateSequence(base) { previous -> "$base-${previous.substringAfterLast('-').toIntOrNull()?.plus(1) ?: 2}" }.first { it !in document.objects }
    }

    private fun tuple(source: String): List<Double>? {
        if (!source.startsWith('(') || !source.endsWith(')')) return null
        return source.drop(1).dropLast(1).split(',').map { it.trim().toDoubleOrNull() ?: return null }.takeIf { it.size in 2..3 }
    }

    private fun numericList(source: String): List<Double>? {
        if (!source.startsWith('{') || !source.endsWith('}') || source.startsWith("{{")) return null
        return source.drop(1).dropLast(1).split(',').map { it.trim().toDoubleOrNull() ?: return null }
    }

    private fun matrix(source: String): List<List<Double>>? {
        if (!source.startsWith("{{") || !source.endsWith("}}")) return null
        val rows = mutableListOf<List<Double>>()
        for (match in Regex("\\{([^{}]+)}").findAll(source)) {
            val row = match.groupValues[1].split(',').map { it.trim().toDoubleOrNull() ?: return null }
            rows += row
        }
        return rows.takeIf { it.isNotEmpty() && it.map { row -> row.size }.distinct().size == 1 }
    }
}

object UniversalAlgebraProjection {
    fun entries(session: UnifiedStudioSession): List<AlgebraEntry> = session.document.objects.values.map { value ->
        val style = session.algebraStyles[value.id] ?: AlgebraObjectStyle(visible = value.id !in session.hiddenIds)
        val payload = value.payload
        val definition = when (payload) {
            is UniversalMathPayload.Symbolic -> payload.source
            is UniversalMathPayload.Coordinates -> payload.definition ?: "(${payload.values.joinToString { compact(it) }})"
            is UniversalMathPayload.Dataset -> "{${payload.values.joinToString()}}"
            is UniversalMathPayload.Properties -> propertiesDefinition(value.kind, payload)
        }
        val valueText = when (payload) {
            is UniversalMathPayload.Symbolic -> payload.ast?.let { SymbolicCasEngine().render(it) } ?: payload.source
            is UniversalMathPayload.Coordinates -> "(${payload.values.joinToString { compact(it) }})"
            is UniversalMathPayload.Dataset -> "{${payload.values.joinToString { compact(it) }}}"
            is UniversalMathPayload.Properties -> payload.entries["exact"] ?: payload.entries["definition"] ?: definition
        }
        val decimal = (payload as? UniversalMathPayload.Properties)?.entries?.get("decimal")
        val issue = (payload as? UniversalMathPayload.Symbolic)?.parseError
        val spoken = spokenMath(value.name, value.kind, definition, valueText, value.dependencies.size)
        AlgebraEntry(value.id, value.name, value.kind, definition, valueText,
            (payload as? UniversalMathPayload.Properties)?.entries?.get("command") ?: (payload as? UniversalMathPayload.Coordinates)?.definition ?: definition,
            decimal, value.dependencies, session.document.dependentsOf(value.id, false), value.sourceView,
            style.copy(visible = value.id !in session.hiddenIds && style.visible), issue == null, issue, spoken)
    }

    fun visible(session: UnifiedStudioSession, query: String = ""): List<AlgebraEntry> {
        val filtered = entries(session).filter { entry ->
            val matchesText = query.isBlank() || listOf(entry.name, entry.definition, entry.kind.name).any { it.contains(query, ignoreCase = true) }
            val matchesFilter = when (session.algebraFilter) {
                AlgebraFilter.All -> true
                AlgebraFilter.Free -> entry.free
                AlgebraFilter.Dependent -> !entry.free
                AlgebraFilter.Visible -> entry.style.visible
                AlgebraFilter.Hidden -> !entry.style.visible
                AlgebraFilter.Auxiliary -> entry.style.auxiliary
                AlgebraFilter.Invalid -> !entry.valid
                AlgebraFilter.Selected -> entry.id in session.algebraSelection
            }
            matchesText && matchesFilter && (session.showAuxiliary || !entry.style.auxiliary)
        }
        return when (session.algebraSort) {
            AlgebraSortMode.Type -> filtered.sortedWith(compareBy({ it.kind.name }, { it.name.lowercase() }))
            AlgebraSortMode.Dependency -> filtered.sortedWith(compareBy({ !it.free }, { it.dependencies.size }, { it.name.lowercase() }))
            AlgebraSortMode.Construction -> {
                val order = session.document.objects.keys.withIndex().associate { it.value to it.index }
                filtered.sortedBy { order[it.id] ?: Int.MAX_VALUE }
            }
            AlgebraSortMode.Name -> filtered.sortedBy { it.name.lowercase() }
            AlgebraSortMode.Layer -> filtered.sortedWith(compareBy({ it.style.layer }, { it.name.lowercase() }))
        }
    }

    private fun compact(value: Double) = if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()

    private fun propertiesDefinition(kind: UniversalMathKind, payload: UniversalMathPayload.Properties): String =
        payload.entries["definition"] ?: when (kind) {
            UniversalMathKind.Scalar, UniversalMathKind.UnitMeasurement -> payload.entries["exact"] ?: payload.entries["value"].orEmpty()
            UniversalMathKind.Matrix -> {
                val columns = payload.entries["columns"]?.toIntOrNull() ?: 0
                val values = payload.entries["values"].orEmpty().split(',').filter(String::isNotBlank)
                if (columns > 0 && values.isNotEmpty()) values.chunked(columns).joinToString(prefix = "{", postfix = "}") { row -> row.joinToString(prefix = "{", postfix = "}") }
                else payload.entries.entries.joinToString { "${it.key}=${it.value}" }
            }
            else -> payload.entries.entries.joinToString { "${it.key}=${it.value}" }
        }

    private fun spokenMath(name: String, kind: UniversalMathKind, definition: String, value: String, dependencyCount: Int): String = buildString {
        append(kind.name.replace(Regex("([a-z])([A-Z])"), "$1 $2").replace(Regex("([A-Za-z])(\\d)"), "$1 $2").replace(Regex("(\\d)([A-Za-z])"), "$1 $2").lowercase()).append(' ').append(name).append(". ")
        append(if (dependencyCount == 0) "Free object. " else "Dependent on $dependencyCount object${if (dependencyCount == 1) "" else "s"}. ")
        append("Definition: ").append(definition.replace("^2", " squared").replace("^3", " cubed").replace("<=", " less than or equal to ").replace(">=", " greater than or equal to ").replace("!=", " not equal to "))
        if (value != definition) append(". Current value: ").append(value)
    }
}

object AlgebraCommandCatalog {
    private val catalog = listOf(
        AlgebraCommandSuggestion("Line", "Line(A, B)", "Line through two points"),
        AlgebraCommandSuggestion("Ray", "Ray(A, B)", "Ray from A through B"),
        AlgebraCommandSuggestion("Segment", "Segment(A, B)", "Segment joining two points"),
        AlgebraCommandSuggestion("Circle", "Circle(center, point)", "Circle through a point"),
        AlgebraCommandSuggestion("Intersect", "Intersect(object1, object2)", "Intersection objects"),
        AlgebraCommandSuggestion("Midpoint", "Midpoint(A, B)", "Midpoint of two points"),
        AlgebraCommandSuggestion("Tangent", "Tangent(point, conic)", "Tangent line"),
        AlgebraCommandSuggestion("Perpendicular", "Perpendicular(point, line)", "Perpendicular line"),
        AlgebraCommandSuggestion("Parallel", "Parallel(point, line)", "Parallel line"),
        AlgebraCommandSuggestion("Sequence", "Sequence(expression, variable, from, to)", "Generate a dynamic sequence"),
        AlgebraCommandSuggestion("Solve", "Solve(equation, variable)", "Solve exactly"),
        AlgebraCommandSuggestion("Factor", "Factor(expression)", "Factor a polynomial"),
        AlgebraCommandSuggestion("Expand", "Expand(expression)", "Expand products and powers"),
        AlgebraCommandSuggestion("Derivative", "Derivative(function)", "Create a derivative"),
        AlgebraCommandSuggestion("Integral", "Integral(function)", "Create an antiderivative"),
        AlgebraCommandSuggestion("Vector", "Vector(A, B)", "Vector from A to B"),
        AlgebraCommandSuggestion("Polygon", "Polygon(A, B, C)", "Polygon through selected points"),
        AlgebraCommandSuggestion("Ellipse", "Ellipse(focus1, focus2, point)", "Ellipse from foci and a point"),
        AlgebraCommandSuggestion("Parabola", "Parabola(focus, directrix)", "Parabola from focus and directrix"),
        AlgebraCommandSuggestion("Reflect", "Reflect(object, line)", "Reflect an object"),
        AlgebraCommandSuggestion("Rotate", "Rotate(object, angle, center)", "Rotate an object"),
        AlgebraCommandSuggestion("Translate", "Translate(object, vector)", "Translate an object"),
        AlgebraCommandSuggestion("Dilate", "Dilate(object, factor, center)", "Dilate an object"),
        AlgebraCommandSuggestion("Locus", "Locus(dependentPoint, driverPoint)", "Create an exact locus"),
        AlgebraCommandSuggestion("Distance", "Distance(object1, object2)", "Measure distance"),
        AlgebraCommandSuggestion("Angle", "Angle(A, B, C)", "Measure an angle"),
        AlgebraCommandSuggestion("Area", "Area(object)", "Measure area"),
        AlgebraCommandSuggestion("Determinant", "Determinant(matrix)", "Exact determinant"),
        AlgebraCommandSuggestion("Transpose", "Transpose(matrix)", "Transpose a matrix"),
        AlgebraCommandSuggestion("Element", "Element(list, index)", "Read a list or matrix element"),
        AlgebraCommandSuggestion("Sum", "Sum(list)", "Sum list values"),
        AlgebraCommandSuggestion("Product", "Product(list)", "Multiply list values"),
    )

    fun suggest(source: String, selected: List<AlgebraEntry> = emptyList()): List<AlgebraCommandSuggestion> {
        val token = Regex("[A-Za-z]+$").find(source)?.value?.lowercase().orEmpty()
        return catalog.filter { token.isBlank() || it.name.lowercase().startsWith(token) || it.description.contains(token, true) }.take(6).map { suggestion ->
            suggestion.copy(compatibleObjectIds = compatible(suggestion.name, selected))
        }
    }

    private fun compatible(command: String, selected: List<AlgebraEntry>): List<String> = when (command) {
        "Line", "Ray", "Segment", "Midpoint" -> selected.filter { it.kind in setOf(UniversalMathKind.Point2D, UniversalMathKind.Point3D) }.map { it.id }
        "Circle" -> selected.filter { it.kind == UniversalMathKind.Point2D }.map { it.id }
        "Factor", "Expand", "Derivative", "Integral", "Solve" -> selected.filter { it.kind in setOf(UniversalMathKind.Expression, UniversalMathKind.Function, UniversalMathKind.Equation) }.map { it.id }
        else -> selected.map { it.id }
    }
}

object AlgebraEquationActions {
    private val equationKinds = setOf(UniversalMathKind.Equation, UniversalMathKind.EquationSystem, UniversalMathKind.ParametricEquation,
        UniversalMathKind.Inequality, UniversalMathKind.Relation, UniversalMathKind.Line, UniversalMathKind.Circle, UniversalMathKind.Conic)
    fun available(entry: AlgebraEntry): List<AlgebraEquationAction> = if (entry.kind !in equationKinds) emptyList() else buildList {
        add(AlgebraEquationAction.InspectVariables)
        add(AlgebraEquationAction.Graph)
        add(AlgebraEquationAction.ConvertForm)
        add(AlgebraEquationAction.CreateGeometry)
        if (entry.kind !in setOf(UniversalMathKind.Relation, UniversalMathKind.ParametricEquation)) add(AlgebraEquationAction.Solve)
        if ('=' in entry.definition && entry.kind != UniversalMathKind.EquationSystem) add(AlgebraEquationAction.Rearrange)
        if (Regex("[A-Za-z]").containsMatchIn(entry.definition)) add(AlgebraEquationAction.Substitute)
    }

    fun variables(entry: AlgebraEntry): List<String> = Regex("[A-Za-z][A-Za-z0-9_]*").findAll(entry.definition).map { it.value }
        .filterNot { it.lowercase() in setOf("sin", "cos", "tan", "sqrt", "exp", "log", "ln", "system", "parametric") }.distinct().sorted().toList()
}

object AlgebraKeyboardController {
    fun action(key: String, ctrl: Boolean = false): AlgebraKeyboardAction = when {
        ctrl && key.equals("a", true) -> AlgebraKeyboardAction.SelectAll
        key.equals("arrowup", true) -> AlgebraKeyboardAction.Previous
        key.equals("arrowdown", true) -> AlgebraKeyboardAction.Next
        key.equals("enter", true) -> AlgebraKeyboardAction.Edit
        key.equals("f2", true) -> AlgebraKeyboardAction.Rename
        key.equals("space", true) -> AlgebraKeyboardAction.ToggleVisibility
        key.equals("delete", true) || key.equals("backspace", true) -> AlgebraKeyboardAction.Delete
        key.equals("contextmenu", true) -> AlgebraKeyboardAction.OpenInspector
        else -> AlgebraKeyboardAction.None
    }

    fun move(entries: List<AlgebraEntry>, currentId: String?, direction: Int): String? {
        if (entries.isEmpty()) return null
        val current = entries.indexOfFirst { it.id == currentId }.takeIf { it >= 0 } ?: 0
        return entries[(current + direction).coerceIn(0, entries.lastIndex)].id
    }
}
