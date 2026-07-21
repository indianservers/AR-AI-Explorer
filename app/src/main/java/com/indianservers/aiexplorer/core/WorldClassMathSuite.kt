package com.indianservers.aiexplorer.core

import com.indianservers.aiexplorer.spatial.SurfaceDefinition3D
import java.nio.charset.StandardCharsets
import java.util.Base64
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

data class GraphDifferentialInsight(
    val point: Vec2,
    val derivative: Double,
    val secondDerivative: Double,
    val tangent: String,
    val normal: String?,
    val classification: String,
)

data class GraphAreaInsight(val signedArea: Double, val geometricArea: Double, val intervals: Int, val errorEstimate: Double)
data class GraphValueTable(val headings: List<String>, val rows: List<List<Double?>>)

/** High-accuracy graph calculus shared by canvas tools, tables, summaries and accessibility. */
class CompetitiveGraphCalculus(private val expressions: ExpressionEngine = ExpressionEngine()) {
    fun differential(source: String, x: Double): GraphDifferentialInsight {
        val expression = expressions.compile(strip(source)); val y = value(expression, x)
        val scale = max(1.0, abs(x)); val h = 1e-4 * scale
        val first = (-value(expression, x + 2 * h) + 8 * value(expression, x + h) - 8 * value(expression, x - h) + value(expression, x - 2 * h)) / (12 * h)
        val second = (-value(expression, x + 2 * h) + 16 * value(expression, x + h) - 30 * y + 16 * value(expression, x - h) - value(expression, x - 2 * h)) / (12 * h * h)
        val tangent = "y=${clean(y)}+${clean(first)}*(x-${clean(x)})"
        val normal = if (abs(first) < 1e-10) "x=${clean(x)}" else "y=${clean(y)}+${clean(-1.0 / first)}*(x-${clean(x)})"
        val classification = when { abs(first) > 1e-5 -> "regular point"; second > 1e-5 -> "local minimum"; second < -1e-5 -> "local maximum"; else -> "stationary or higher-order point" }
        return GraphDifferentialInsight(Vec2(x, y), first, second, tangent, normal, classification)
    }

    fun integral(source: String, from: Double, to: Double, tolerance: Double = 1e-9): GraphAreaInsight {
        require(from.isFinite() && to.isFinite() && from != to && tolerance in 1e-12..1e-3)
        val expression = expressions.compile(strip(source)); var intervals = 0
        fun f(x: Double): Double { intervals++; return value(expression, x) }
        fun simpson(a: Double, b: Double): Double { val m = (a + b) * .5; return (b - a) * (f(a) + 4 * f(m) + f(b)) / 6 }
        fun adaptive(a: Double, b: Double, whole: Double, epsilon: Double, depth: Int, absolute: Boolean): Pair<Double, Double> {
            val m = (a + b) * .5; val left = if (absolute) absoluteSimpson(::f, a, m) else simpson(a, m); val right = if (absolute) absoluteSimpson(::f, m, b) else simpson(m, b)
            val error = abs(left + right - whole) / 15
            if (depth == 0 || error <= epsilon) return (left + right + (left + right - whole) / 15) to error
            val l = adaptive(a, m, left, epsilon * .5, depth - 1, absolute); val r = adaptive(m, b, right, epsilon * .5, depth - 1, absolute)
            return l.first + r.first to l.second + r.second
        }
        val sign = if (from < to) 1.0 else -1.0; val a = minOf(from, to); val b = maxOf(from, to)
        val signedWhole = simpson(a, b); val signed = adaptive(a, b, signedWhole, tolerance, 20, false)
        val absoluteWhole = absoluteSimpson(::f, a, b); val geometric = adaptive(a, b, absoluteWhole, tolerance, 20, true)
        return GraphAreaInsight(sign * signed.first, geometric.first, intervals, signed.second + geometric.second)
    }

    fun areaBetween(first: String, second: String, from: Double, to: Double, tolerance: Double = 1e-8): GraphAreaInsight {
        val a = expressions.compile(strip(first)); val b = expressions.compile(strip(second))
        return integral("(${strip(first)})-(${strip(second)})", from, to, tolerance).let { result ->
            // integral() already computes the absolute difference as geometric area.
            result.copy(signedArea = result.signedArea, geometricArea = result.geometricArea)
        }
    }

    fun table(sources: List<String>, minimum: Double, maximum: Double, rows: Int): GraphValueTable {
        require(sources.isNotEmpty() && rows in 2..100_000 && minimum < maximum)
        val compiled = sources.map { expressions.compile(strip(it)) }
        val values = (0 until rows).map { index ->
            val x = minimum + (maximum - minimum) * index / (rows - 1)
            listOf<Double?>(x) + compiled.map { runCatching { value(it, x) }.getOrNull() }
        }
        return GraphValueTable(listOf("x") + sources, values)
    }

    fun roots(source: String, minimum: Double, maximum: Double, samples: Int = 2048): List<Double> {
        require(minimum < maximum && samples in 32..100_000); val expression = expressions.compile(strip(source)); val roots = mutableListOf<Double>()
        var left = minimum; var leftValue = runCatching { value(expression, left) }.getOrNull()
        for (index in 1..samples) {
            val right = minimum + (maximum - minimum) * index / samples; val rightValue = runCatching { value(expression, right) }.getOrNull()
            if (leftValue != null && rightValue != null) {
                if (abs(leftValue) < 1e-10) roots += left
                if (leftValue * rightValue < 0) roots += bisect(expression, left, right)
            }
            left = right; leftValue = rightValue
        }
        return roots.sorted().fold(mutableListOf()) { result, root -> if (result.none { abs(it - root) < 1e-7 }) result += root; result }
    }

    private fun bisect(expression: Expression, from: Double, to: Double): Double {
        var a = from; var b = to; var fa = value(expression, a)
        repeat(80) { val m = (a + b) * .5; val fm = value(expression, m); if (abs(fm) < 1e-13) return m; if (fa * fm <= 0) b = m else { a = m; fa = fm } }
        return (a + b) * .5
    }
    private fun value(expression: Expression, x: Double) = expression.eval(mapOf("x" to x)).also { require(it.isFinite()) { "The graph is undefined at x=$x." } }
    private fun absoluteSimpson(f: (Double) -> Double, a: Double, b: Double): Double { val m = (a + b) * .5; return (b - a) * (abs(f(a)) + 4 * abs(f(m)) + abs(f(b))) / 6 }
    private fun strip(source: String) = source.substringAfter('=').trim()
    private fun clean(value: Double) = "%.10g".format(value)
}

data class CompetitiveSurfaceDifferential(
    val point: Vec3,
    val gradient: Vec3,
    val unitNormal: Vec3,
    val tangentPlane: String,
    val classification: String,
)
data class SurfaceContour(val level: Double, val segments: List<Pair<Vec3, Vec3>>)

/** Differential and contour analysis for explicit surfaces, independent of GPU availability. */
class CompetitiveSurfaceCalculus(private val expressions: ExpressionEngine = ExpressionEngine()) {
    fun differential(surface: SurfaceDefinition3D.Explicit, x: Double, y: Double): CompetitiveSurfaceDifferential {
        val expression = expressions.compile(surface.z.substringAfter('=').trim()); val h = 1e-4 * max(1.0, max(abs(x), abs(y)))
        fun z(a: Double, b: Double) = expression.eval(mapOf("x" to a, "y" to b)).also { require(it.isFinite()) }
        val value = z(x, y); val dx = (z(x + h, y) - z(x - h, y)) / (2 * h); val dy = (z(x, y + h) - z(x, y - h)) / (2 * h)
        val dxx = (z(x + h, y) - 2 * value + z(x - h, y)) / (h * h); val dyy = (z(x, y + h) - 2 * value + z(x, y - h)) / (h * h)
        val dxy = (z(x + h, y + h) - z(x + h, y - h) - z(x - h, y + h) + z(x - h, y - h)) / (4 * h * h)
        val determinant = dxx * dyy - dxy * dxy
        val classification = when { abs(dx) + abs(dy) > 1e-4 -> "regular surface point"; determinant > 1e-5 && dxx > 0 -> "local minimum"; determinant > 1e-5 -> "local maximum"; determinant < -1e-5 -> "saddle point"; else -> "degenerate critical point" }
        val normal = Vec3(-dx, -dy, 1.0).normalized()
        return CompetitiveSurfaceDifferential(Vec3(x, y, value), Vec3(dx, dy, 0.0), normal, "z=${clean(value)}+${clean(dx)}*(x-${clean(x)})+${clean(dy)}*(y-${clean(y)})", classification)
    }

    fun contour(surface: SurfaceDefinition3D.Explicit, level: Double, cells: Int = 64): SurfaceContour {
        require(cells in 8..256); val expression = expressions.compile(surface.z.substringAfter('=').trim()); val domain = surface.domain; val segments = mutableListOf<Pair<Vec3, Vec3>>()
        fun point(ix: Int, iy: Int): Vec3 { val x = domain.first.start + (domain.first.endInclusive - domain.first.start) * ix / cells; val y = domain.second.start + (domain.second.endInclusive - domain.second.start) * iy / cells; return Vec3(x, y, expression.eval(mapOf("x" to x, "y" to y))) }
        for (ix in 0 until cells) for (iy in 0 until cells) {
            val corners = listOf(point(ix, iy), point(ix + 1, iy), point(ix + 1, iy + 1), point(ix, iy + 1)); val hits = mutableListOf<Vec3>()
            for (edge in 0..3) { val a = corners[edge]; val b = corners[(edge + 1) % 4]; val da = a.z - level; val db = b.z - level
                if (da == 0.0) hits += Vec3(a.x, a.y, level) else if (da * db < 0) { val t = da / (da - db); hits += Vec3(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t, level) }
            }
            if (hits.size == 2) segments += hits[0] to hits[1] else if (hits.size == 4) { segments += hits[0] to hits[1]; segments += hits[2] to hits[3] }
        }
        return SurfaceContour(level, segments)
    }

    fun gradientPath(surface: SurfaceDefinition3D.Explicit, start: Vec2, ascending: Boolean, steps: Int = 80, stepSize: Double = .04): List<Vec3> {
        require(steps in 1..2000 && stepSize > 0); val result = mutableListOf<Vec3>(); var current = start
        repeat(steps) { val d = differential(surface, current.x, current.y); result += d.point; val direction = Vec2(d.gradient.x, d.gradient.y); val magnitude = direction.distanceTo(Vec2(0.0, 0.0)); if (magnitude < 1e-10) return result; val sign = if (ascending) 1.0 else -1.0; current += direction * (sign * stepSize / magnitude) }
        return result
    }
    private fun clean(value: Double) = "%.10g".format(value)
}

enum class CasNotebookOperation { Simplify, Expand, Factor, PartialFractions, Derivative, Integral, Limit, Determinant, RowReduce, Eigenvalues, Ode }
data class CasNotebookEntry(val id: String, val operation: CasNotebookOperation, val result: CasRow)
data class CasNotebookState(val assumptions: MathAssumptionSet = MathAssumptionSet(), val entries: List<CasNotebookEntry> = emptyList())

class CompetitiveCasNotebook(private val engine: SymbolicCasEngine = SymbolicCasEngine()) {
    fun assume(state: CasNotebookState, assumption: VariableAssumption) = state.copy(assumptions = state.assumptions.with(assumption))
    fun evaluate(state: CasNotebookState, id: String, operation: CasNotebookOperation, source: String, variable: String = "x", approaching: String = "0"): CasNotebookState {
        require(state.entries.none { it.id == id }); val result = when (operation) {
            CasNotebookOperation.Simplify -> engine.simplify(source, state.assumptions)
            CasNotebookOperation.Expand -> engine.expand(source)
            CasNotebookOperation.Factor -> engine.factor(source, variable)
            CasNotebookOperation.PartialFractions -> engine.partialFractions(source, variable)
            CasNotebookOperation.Derivative -> engine.derivative(source, variable)
            CasNotebookOperation.Integral -> engine.integral(source, variable)
            CasNotebookOperation.Limit -> engine.limit(source, variable, approaching)
            CasNotebookOperation.Determinant -> engine.determinant(source)
            CasNotebookOperation.RowReduce -> engine.rowReduce(source)
            CasNotebookOperation.Eigenvalues -> engine.eigenvalues(source)
            CasNotebookOperation.Ode -> engine.solveOde(source)
        }
        return state.copy(entries = state.entries + CasNotebookEntry(id, operation, result))
    }
    fun solveSystem(state: CasNotebookState, id: String, equations: List<String>, variables: List<String>) = state.copy(entries = state.entries + CasNotebookEntry(id, CasNotebookOperation.Simplify, engine.solveSystem(equations, variables)))
    fun solveInequalities(state: CasNotebookState, id: String, inequalities: List<String>, variable: String = "x") = state.copy(entries = state.entries + CasNotebookEntry(id, CasNotebookOperation.Simplify, engine.solveInequalities(inequalities, variable)))
}

/** Atomic undo/redo across Graph, Geometry and 3D instead of one history per canvas. */
class UnifiedConstructionHistory(initial: UnifiedConstructionSession = UnifiedConstructionSession(), private val engine: UnifiedConstructionEngine = UnifiedConstructionEngine()) {
    private val undo = mutableListOf<UnifiedConstructionSession>(); private val redo = mutableListOf<UnifiedConstructionSession>()
    var current: UnifiedConstructionSession = initial; private set
    fun execute(command: String): UnifiedConstructionSession { val next = engine.execute(current, command); undo += current; current = next; redo.clear(); return current }
    fun apply(next: UnifiedConstructionSession): UnifiedConstructionSession { if (next != current) { undo += current; current = next; redo.clear() }; return current }
    fun undo(): UnifiedConstructionSession { if (undo.isNotEmpty()) { redo += current; current = undo.removeAt(undo.lastIndex) }; return current }
    fun redo(): UnifiedConstructionSession { if (redo.isNotEmpty()) { undo += current; current = redo.removeAt(redo.lastIndex) }; return current }
    fun canUndo() = undo.isNotEmpty(); fun canRedo() = redo.isNotEmpty()
}

/** Replay-based, bounded and versioned persistence for unified sessions. */
object UnifiedConstructionSessionCodec {
    private const val header = "AIEXPLORER_UNIFIED|1"
    fun encode(session: UnifiedConstructionSession): String = buildString {
        appendLine(header); session.commands.forEach { appendLine("C|${encodeText(it)}") }
        session.links.forEach { link -> appendLine("L|${encodeText(link.id)}|${link.members.joinToString(",") { encodeText(it) }}") }
    }.also { require(it.length <= 4_000_000) }

    fun decode(source: String): UnifiedConstructionSession {
        require(source.length <= 4_000_000); val lines = source.lineSequence().filter(String::isNotBlank).toList(); require(lines.firstOrNull() == header)
        val engine = UnifiedConstructionEngine(); var session = UnifiedConstructionSession()
        lines.drop(1).filter { it.startsWith("C|") }.forEach { session = engine.execute(session, decodeText(it.substringAfter("C|"))) }
        val inspector = CrossViewConstructionInspector(engine)
        lines.drop(1).filter { it.startsWith("L|") }.forEach { line -> val parts = line.split('|', limit = 3); val members = parts[2].split(',').filter(String::isNotBlank).map(::decodeText).toSet(); session = inspector.link(session, decodeText(parts[1]), members) }
        return session
    }
    private fun encodeText(value: String) = Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(StandardCharsets.UTF_8))
    private fun decodeText(value: String) = String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)
}

object ConstructionMacroRecorder {
    fun record(session: UnifiedConstructionSession, name: String, ids: Set<String>, parameters: List<String> = emptyList()): ConstructionMacro {
        require(ids.isNotEmpty()); val selected = session.commands.filter { command -> runCatching { UnifiedConstructionCommandParser.parse(command).id in ids }.getOrDefault(false) }
        require(selected.isNotEmpty()) { "No recorded commands matched the selection." }
        return ConstructionMacro(name, parameters, selected)
    }
}

data class CrossViewValueRegistry(val values: Map<String, Double> = emptyMap()) {
    fun with(name: String, value: Double): CrossViewValueRegistry { require(name.matches(Regex("[A-Za-z][A-Za-z0-9_]*")) && value.isFinite()); return copy(values = values + (name to value)) }
    fun evaluate(expression: String): Double = ExpressionEngine().compile(expression).eval(values).also { require(it.isFinite()) }
}
