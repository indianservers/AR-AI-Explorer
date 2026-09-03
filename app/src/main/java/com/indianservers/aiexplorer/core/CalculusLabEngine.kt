package com.indianservers.aiexplorer.core

enum class RiemannSample { Left, Midpoint, Right }

data class RiemannRectangle(val from: Double, val to: Double, val height: Double) {
    val signedArea: Double get() = (to - from) * height
}

data class DerivativeLabResult(
    val symbolic: CasRow,
    val numerical: GraphDifferentialInsight,
    val curve: List<Vec2>,
)

data class IntegralLabResult(
    val symbolic: CasRow,
    val numerical: GraphAreaInsight,
    val rectangles: List<RiemannRectangle>,
    val riemannEstimate: Double,
    val curve: List<Vec2>,
)

data class LimitLabResult(
    val symbolic: CasRow,
    val rigorous: RigorousLimitReport,
)

data class LinearizationLabResult(
    val centre: Vec2,
    val targetX: Double,
    val slope: Double,
    val predictedY: Double,
    val actualY: Double,
    val absoluteError: Double,
    val equation: String,
)

data class AreaBetweenLabResult(
    val firstCurve: List<Vec2>,
    val secondCurve: List<Vec2>,
    val numerical: GraphAreaInsight,
)

class CalculusLabEngine(
    private val expressions: ExpressionEngine = ExpressionEngine(),
    private val symbolic: SymbolicCasEngine = SymbolicCasEngine(),
    private val graph: CompetitiveGraphCalculus = CompetitiveGraphCalculus(expressions),
    private val rigorous: RigorousCalculusEngine = RigorousCalculusEngine(expressions),
) {
    fun derivative(source: String, at: Double, range: ClosedFloatingPointRange<Double> = -5.0..5.0): DerivativeLabResult {
        require(at.isFinite() && range.start < range.endInclusive) { "Enter a valid point and graph interval." }
        return DerivativeLabResult(
            symbolic = symbolic.derivative(clean(source)),
            numerical = graph.differential(clean(source), at),
            curve = sample(source, range.start, range.endInclusive),
        )
    }

    fun linearization(source: String, at: Double, targetX: Double): LinearizationLabResult {
        require(at.isFinite() && targetX.isFinite()) { "Enter finite linearization points." }
        val compiled = expressions.compile(clean(source))
        val centreY = compiled.eval(mapOf("x" to at)).also { require(it.isFinite()) { "The function is undefined at the centre." } }
        val slope = graph.differential(clean(source), at).derivative
        val predicted = centreY + slope * (targetX - at)
        val actual = compiled.eval(mapOf("x" to targetX)).also { require(it.isFinite()) { "The function is undefined at the target." } }
        return LinearizationLabResult(
            centre = Vec2(at, centreY), targetX = targetX, slope = slope,
            predictedY = predicted, actualY = actual,
            absoluteError = kotlin.math.abs(actual - predicted),
            equation = "L(x) = ${formatCoordinate(centreY)} + ${formatCoordinate(slope)}(x − ${formatCoordinate(at)})",
        )
    }

    fun integral(
        source: String,
        from: Double,
        to: Double,
        rectangleCount: Int = 12,
        sample: RiemannSample = RiemannSample.Midpoint,
    ): IntegralLabResult {
        require(from.isFinite() && to.isFinite() && from < to) { "Integral bounds must satisfy lower < upper." }
        require(rectangleCount in 2..200) { "Use between 2 and 200 rectangles." }
        val compiled = expressions.compile(clean(source))
        val width = (to - from) / rectangleCount
        val rectangles = List(rectangleCount) { index ->
            val left = from + index * width
            val right = left + width
            val x = when (sample) {
                RiemannSample.Left -> left
                RiemannSample.Midpoint -> (left + right) / 2.0
                RiemannSample.Right -> right
            }
            RiemannRectangle(left, right, compiled.eval(mapOf("x" to x)).also { require(it.isFinite()) { "The function is undefined inside the interval." } })
        }
        val padding = ((to - from) * .18).coerceAtLeast(.5)
        return IntegralLabResult(
            symbolic = symbolic.integral(clean(source)),
            numerical = graph.integral(clean(source), from, to),
            rectangles = rectangles,
            riemannEstimate = rectangles.sumOf(RiemannRectangle::signedArea),
            curve = sample(source, from - padding, to + padding),
        )
    }

    fun areaBetween(first: String, second: String, from: Double, to: Double): AreaBetweenLabResult {
        require(from.isFinite() && to.isFinite() && from < to) { "Area bounds must satisfy lower < upper." }
        return AreaBetweenLabResult(
            firstCurve = sample(first, from, to),
            secondCurve = sample(second, from, to),
            numerical = graph.areaBetween(clean(first), clean(second), from, to),
        )
    }

    fun limit(source: String, approaching: Double): LimitLabResult {
        require(approaching.isFinite()) { "Enter a finite approach value." }
        return LimitLabResult(
            symbolic = symbolic.limit(clean(source), approaching = approaching.toString()),
            rigorous = rigorous.limit(clean(source), approaching),
        )
    }

    fun applications(source: String, from: Double, to: Double): DerivativeApplicationReport =
        rigorous.derivativeApplications(clean(source), from, to)

    fun sample(source: String, from: Double, to: Double, count: Int = 320): List<Vec2> {
        require(from < to && count in 16..2_000)
        val compiled = expressions.compile(clean(source))
        return (0..count).mapNotNull { index ->
            val x = from + (to - from) * index / count
            runCatching { compiled.eval(mapOf("x" to x)) }.getOrNull()?.takeIf(Double::isFinite)?.let { Vec2(x, it) }
        }
    }

    private fun clean(source: String): String = source.substringAfter('=').trim().ifBlank { "x" }
}
