package com.indianservers.aiexplorer.core

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sqrt

data class ProofParameter(val name: String, val minimum: Double, val maximum: Double, val initial: Double)
data class VisualProofLab(
    val id: String,
    val title: String,
    val topic: String,
    val steps: List<String>,
    val parameters: List<ProofParameter>,
    val changesPrompt: String,
    val invariantPrompt: String,
    val formalResult: String,
)
data class ProofFrame(
    val lab: VisualProofLab,
    val step: Int,
    val parameters: Map<String, Double>,
    val measurements: Map<String, Double>,
    val invariant: String,
    val residual: Double,
    val holds: Boolean,
)
data class ProofPlayback(val frame: ProofFrame, val playing: Boolean = false, val direction: Int = 1)

object VisualProofCatalog {
    val labs = listOf(
        VisualProofLab("triangle-angle-sum", "Triangle angle sum", "Geometry", listOf("Build triangle ABC.", "Copy its three angles.", "Arrange them on a straight line.", "Drag a vertex to test."), listOf(ProofParameter("height", .2, 6.0, 3.0), ProofParameter("offset", -3.0, 5.0, 1.0)), "Which individual angles change as C moves?", "Why does their total stay 180°?", "A + B + C = 180°"),
        VisualProofLab("pythagorean", "Pythagorean rearrangement", "Geometry", listOf("Create a right triangle.", "Build squares on each side.", "Rearrange four copies.", "Compare uncovered areas."), listOf(ProofParameter("a", .5, 6.0, 3.0), ProofParameter("b", .5, 6.0, 4.0)), "How do the three square areas change?", "What area remains equal after rearrangement?", "a² + b² = c²"),
        VisualProofLab("derivative-slope", "Derivative as slope", "Calculus", listOf("Plot f(x)=x².", "Place a secant h away.", "Shrink h.", "Reveal the tangent."), listOf(ProofParameter("x", -4.0, 4.0, 2.0), ProofParameter("h", .001, 2.0, 1.0)), "How does the secant slope change as h shrinks?", "What limiting slope is stable?", "d(x²)/dx = 2x"),
        VisualProofLab("integral-area", "Integral as accumulated area", "Calculus", listOf("Partition [0,b].", "Build rectangles.", "Increase their count.", "Compare with the exact area."), listOf(ProofParameter("b", .5, 5.0, 3.0), ProofParameter("n", 2.0, 200.0, 10.0)), "What changes when rectangles become thinner?", "Which exact accumulated area is approached?", "∫₀ᵇ x dx = b²/2"),
        VisualProofLab("normal-area", "Normal probability area", "Probability", listOf("Draw the normal curve.", "Place symmetric bounds.", "Shade the interval.", "Compare tail areas."), listOf(ProofParameter("z", .1, 3.5, 1.0)), "How does shaded probability change with z?", "What remains symmetric about zero?", "P(-z≤Z≤z)=2Φ(z)-1"),
        VisualProofLab("vector-addition", "Vector addition", "Vectors", listOf("Draw vectors u and v.", "Use head-to-tail addition.", "Swap their order.", "Compare endpoints."), listOf(ProofParameter("ux", -4.0, 4.0, 2.0), ProofParameter("uy", -4.0, 4.0, 1.0), ProofParameter("vx", -4.0, 4.0, -1.0), ProofParameter("vy", -4.0, 4.0, 3.0)), "What changes when u and v move?", "Why is the final endpoint unchanged when order swaps?", "u + v = v + u"),
        VisualProofLab("matrix-transform", "Matrix area transformation", "Linear algebra", listOf("Start with a unit square.", "Apply a 2×2 matrix.", "Measure transformed area.", "Compare with determinant."), listOf(ProofParameter("a", -3.0, 3.0, 2.0), ProofParameter("b", -3.0, 3.0, 1.0), ProofParameter("c", -3.0, 3.0, 0.0), ProofParameter("d", -3.0, 3.0, 2.0)), "How does the image shape change?", "What scalar controls signed area?", "area scale = |det(A)|"),
        VisualProofLab("circle-ratio", "Circle circumference ratio", "Geometry", listOf("Choose a radius.", "Unroll the circumference.", "Compare with diameter.", "Drag radius and retest."), listOf(ProofParameter("r", .2, 6.0, 2.0)), "How do circumference and diameter change?", "Which ratio remains π?", "C/d = π"),
        VisualProofLab("algebra-square", "Square of a binomial", "Algebra", listOf("Build a square of side a+b.", "Partition it.", "Label the four regions.", "Sum their areas."), listOf(ProofParameter("a", .2, 5.0, 2.0), ProofParameter("b", .2, 5.0, 1.0)), "How do the four regions change?", "Why does total area remain the same?", "(a+b)² = a² + 2ab + b²"),
        VisualProofLab("shear-area", "Shear preserves area", "Transformations", listOf("Build a rectangle.", "Slide its top edge.", "Observe the parallelogram.", "Compare base×height."), listOf(ProofParameter("base", .5, 6.0, 4.0), ProofParameter("height", .5, 5.0, 2.0), ProofParameter("shear", -4.0, 4.0, 1.0)), "What changes as the top edge slides?", "Which dimensions keep area fixed?", "A = base × perpendicular height"),
    )
}

class VisualProofEngine {
    fun start(id: String): ProofPlayback {
        val lab = VisualProofCatalog.labs.first { it.id == id }
        return ProofPlayback(frame(lab, 0, lab.parameters.associate { it.name to it.initial }))
    }

    fun setParameter(playback: ProofPlayback, name: String, value: Double): ProofPlayback {
        val definition = playback.frame.lab.parameters.first { it.name == name }
        val parameters = playback.frame.parameters + (name to value.coerceIn(definition.minimum, definition.maximum))
        return playback.copy(frame = frame(playback.frame.lab, playback.frame.step, parameters))
    }

    fun next(playback: ProofPlayback): ProofPlayback {
        val last = playback.frame.lab.steps.lastIndex
        val next = (playback.frame.step + playback.direction).coerceIn(0, last)
        val direction = if (next == last) -1 else if (next == 0) 1 else playback.direction
        return playback.copy(frame = frame(playback.frame.lab, next, playback.frame.parameters), direction = direction)
    }

    fun reveal(playback: ProofPlayback) = playback.copy(frame = frame(playback.frame.lab, playback.frame.lab.steps.lastIndex, playback.frame.parameters))
    fun togglePlaying(playback: ProofPlayback) = playback.copy(playing = !playback.playing)

    private fun frame(lab: VisualProofLab, step: Int, p: Map<String, Double>): ProofFrame {
        val measurements = mutableMapOf<String, Double>()
        val residual: Double
        val invariant: String
        when (lab.id) {
            "triangle-angle-sum" -> {
                val a = Vec2(0.0, 0.0); val b = Vec2(4.0, 0.0); val c = Vec2(p.getValue("offset"), p.getValue("height"))
                val angles = listOf(angle(b - a, c - a), angle(a - b, c - b), angle(a - c, b - c))
                measurements["angle sum"] = angles.sum(); residual = abs(angles.sum() - 180); invariant = "angle sum = 180°"
            }
            "pythagorean" -> {
                val a = p.getValue("a"); val b = p.getValue("b"); val c2 = a * a + b * b
                measurements.putAll(mapOf("a²" to a * a, "b²" to b * b, "c²" to c2)); residual = abs(c2 - a * a - b * b); invariant = "a²+b²=c²"
            }
            "derivative-slope" -> {
                val x = p.getValue("x"); val h = p.getValue("h"); val secant = ((x + h).pow(2) - x.pow(2)) / h; val tangent = 2 * x
                measurements.putAll(mapOf("secant slope" to secant, "tangent slope" to tangent)); residual = abs(secant - tangent); invariant = "secant → 2x as h → 0"
            }
            "integral-area" -> {
                val b = p.getValue("b"); val n = p.getValue("n").toInt().coerceAtLeast(1); val width = b / n; val sum = (0 until n).sumOf { (it + .5) * width * width }; val exact = b * b / 2
                measurements.putAll(mapOf("rectangle sum" to sum, "exact area" to exact)); residual = abs(sum - exact); invariant = "rectangle sum → b²/2"
            }
            "normal-area" -> {
                val z = p.getValue("z"); val middle = 2 * normalCdf(z) - 1
                measurements.putAll(mapOf("middle area" to middle, "left tail" to (1 - middle) / 2, "right tail" to (1 - middle) / 2)); residual = 0.0; invariant = "left tail = right tail"
            }
            "vector-addition" -> {
                val ux = p.getValue("ux"); val uy = p.getValue("uy"); val vx = p.getValue("vx"); val vy = p.getValue("vy")
                measurements.putAll(mapOf("result x" to ux + vx, "result y" to uy + vy)); residual = abs((ux + vx) - (vx + ux)) + abs((uy + vy) - (vy + uy)); invariant = "u+v=v+u"
            }
            "matrix-transform" -> {
                val determinant = p.getValue("a") * p.getValue("d") - p.getValue("b") * p.getValue("c")
                measurements.putAll(mapOf("determinant" to determinant, "area scale" to abs(determinant))); residual = 0.0; invariant = "area scale = |det(A)|"
            }
            "circle-ratio" -> {
                val r = p.getValue("r"); val circumference = 2 * PI * r; val diameter = 2 * r
                measurements.putAll(mapOf("circumference" to circumference, "diameter" to diameter, "ratio" to circumference / diameter)); residual = abs(circumference / diameter - PI); invariant = "C/d=π"
            }
            "algebra-square" -> {
                val a = p.getValue("a"); val b = p.getValue("b"); val whole = (a + b).pow(2); val parts = a * a + 2 * a * b + b * b
                measurements.putAll(mapOf("whole" to whole, "parts" to parts)); residual = abs(whole - parts); invariant = "(a+b)²=a²+2ab+b²"
            }
            else -> {
                val area = p.getValue("base") * p.getValue("height")
                measurements.putAll(mapOf("area before" to area, "area after shear" to area, "shear" to p.getValue("shear"))); residual = 0.0; invariant = "base×height stays constant"
            }
        }
        val tolerance = when (lab.id) { "derivative-slope" -> p.getValue("h") + 1e-9; "integral-area" -> 1.0 / p.getValue("n").coerceAtLeast(1.0); else -> 1e-7 }
        return ProofFrame(lab, step.coerceIn(0, lab.steps.lastIndex), p, measurements, invariant, residual, residual <= tolerance)
    }

    private fun angle(a: Vec2, b: Vec2): Double {
        val cross = a.x * b.y - a.y * b.x; val dot = a.x * b.x + a.y * b.y
        return abs(atan2(cross, dot)) * 180 / PI
    }
    private fun normalCdf(value: Double): Double {
        val x = abs(value); val t = 1 / (1 + .2316419 * x); val density = exp(-x * x / 2) / sqrt(2 * PI)
        val tail = density * t * (.319381530 + t * (-.356563782 + t * (1.781477937 + t * (-1.821255978 + t * 1.330274429))))
        return if (value >= 0) 1 - tail else tail
    }
}
