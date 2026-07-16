package com.indianservers.aiexplorer.core

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

enum class TrigAngleUnit { Degrees, Radians, Gradians }
enum class TrigFunction { Sine, Cosine, Tangent, Secant, Cosecant, Cotangent }
enum class InverseTrigFunction { ArcSine, ArcCosine, ArcTangent }
enum class PolarCurveType { Rose, Cardioid, Spiral, Lemniscate }

data class TrigDisplayValue(val radians: Double, val display: Double, val suffix: String)
data class TrigEquationRoot(val radians: Double, val degrees: Double, val exactLabel: String? = null)
data class HarmonicComponent(val amplitude: Double = 1.0, val multiple: Int = 1, val phase: Double = 0.0, val enabled: Boolean = true)

data class TrigTransform(
    val amplitude: Double = 1.0,
    val period: Double = 2 * PI,
    val phaseShift: Double = 0.0,
    val verticalShift: Double = 0.0,
) {
    init { require(amplitude.isFinite() && period > 0.0 && phaseShift.isFinite() && verticalShift.isFinite()) }
    val angularFrequency: Double get() = 2 * PI / period
    fun valueAt(x: Double, function: TrigFunction): Double {
        val argument = angularFrequency * (x - phaseShift)
        val sine = sin(argument)
        val cosine = cos(argument)
        val base = when (function) {
            TrigFunction.Sine -> sine
            TrigFunction.Cosine -> cosine
            TrigFunction.Tangent -> if (abs(cosine) < 1e-10) Double.NaN else sine / cosine
            TrigFunction.Secant -> if (abs(cosine) < 1e-10) Double.NaN else 1 / cosine
            TrigFunction.Cosecant -> if (abs(sine) < 1e-10) Double.NaN else 1 / sine
            TrigFunction.Cotangent -> if (abs(sine) < 1e-10) Double.NaN else cosine / sine
        }
        return amplitude * base + verticalShift
    }
}

data class UnitCircleSnapshot(
    val radians: Double,
    val degrees: Double,
    val sine: Double,
    val cosine: Double,
    val tangent: Double?,
    val quadrant: Int,
    val referenceAngleDegrees: Double,
    val exactSine: String?,
    val exactCosine: String?,
    val exactTangent: String?,
)

/** One angle authority shared by the unit circle, wave cursor and triangle lab. */
object InteractiveTrigEngine {
    fun toRadians(value: Double, unit: TrigAngleUnit): Double = when (unit) {
        TrigAngleUnit.Degrees -> value * PI / 180.0
        TrigAngleUnit.Radians -> value
        TrigAngleUnit.Gradians -> value * PI / 200.0
    }

    fun fromRadians(radians: Double, unit: TrigAngleUnit): TrigDisplayValue = when (unit) {
        TrigAngleUnit.Degrees -> TrigDisplayValue(radians, radians * 180.0 / PI, "°")
        TrigAngleUnit.Radians -> TrigDisplayValue(radians, radians, " rad")
        TrigAngleUnit.Gradians -> TrigDisplayValue(radians, radians * 200.0 / PI, " gon")
    }

    fun inverse(value: Double, function: InverseTrigFunction): Double = when (function) {
        InverseTrigFunction.ArcSine -> asin(value.coerceIn(-1.0, 1.0))
        InverseTrigFunction.ArcCosine -> acos(value.coerceIn(-1.0, 1.0))
        InverseTrigFunction.ArcTangent -> kotlin.math.atan(value)
    }

    fun normalizeRadians(value: Double): Double {
        val wrapped = value % (2 * PI)
        return if (wrapped < 0) wrapped + 2 * PI else wrapped
    }

    fun angleFromPoint(x: Double, y: Double): Double = normalizeRadians(atan2(y, x))

    fun snapshot(radians: Double): UnitCircleSnapshot {
        val value = normalizeRadians(radians)
        val degrees = value * 180 / PI
        val quadrant = when { degrees < 90 -> 1; degrees < 180 -> 2; degrees < 270 -> 3; else -> 4 }
        val reference = when (quadrant) { 1 -> degrees; 2 -> 180 - degrees; 3 -> degrees - 180; else -> 360 - degrees }
        val special = specialAngle(degrees)
        val c = cos(value); val s = sin(value)
        return UnitCircleSnapshot(value, degrees, clean(s), clean(c), if (abs(c) < 1e-10) null else clean(tan(value)), quadrant, clean(reference), special?.second, special?.first, special?.third)
    }

    fun waveSamples(transform: TrigTransform, function: TrigFunction, start: Double, end: Double, count: Int = 361): List<Vec2> {
        require(start < end && count in 2..20_000)
        return (0 until count).mapNotNull { index ->
            val x = start + (end - start) * index / (count - 1)
            val y = transform.valueAt(x, function)
            if (y.isFinite() && abs(y) < 1e6) Vec2(x, y) else null
        }
    }

    fun equationRoots(function: TrigFunction, target: Double, start: Double = -2 * PI, end: Double = 2 * PI): List<TrigEquationRoot> {
        require(start < end)
        val transform = TrigTransform()
        val samples = 4096
        val roots = mutableListOf<Double>()
        var previousX = start
        var previousY = transform.valueAt(previousX, function) - target
        for (index in 1..samples) {
            val x = start + (end - start) * index / samples
            val y = transform.valueAt(x, function) - target
            if (y.isFinite() && previousY.isFinite() && (abs(y) < 1e-7 || y * previousY < 0)) {
                var low = previousX
                var high = x
                repeat(42) {
                    val middle = (low + high) / 2
                    val middleY = transform.valueAt(middle, function) - target
                    if (!middleY.isFinite() || middleY * (transform.valueAt(low, function) - target) > 0) low = middle else high = middle
                }
                val root = (low + high) / 2
                if (roots.none { abs(it - root) < 1e-4 }) roots += root
            }
            previousX = x
            previousY = y
        }
        return roots.map { TrigEquationRoot(it, it * 180 / PI, specialAngleLabel(it)) }
    }

    fun polarSamples(type: PolarCurveType, parameter: Double, count: Int = 721): List<Vec2> = (0 until count).map { index ->
        val theta = 2 * PI * index / (count - 1)
        val r = when (type) {
            PolarCurveType.Rose -> cos(parameter.coerceAtLeast(1.0) * theta)
            PolarCurveType.Cardioid -> 1.0 + cos(theta)
            PolarCurveType.Spiral -> parameter.coerceAtLeast(.05) * theta / (2 * PI)
            PolarCurveType.Lemniscate -> kotlin.math.sqrt(abs(cos(2 * theta))) * if (cos(2 * theta) >= 0) 1 else -1
        }
        Vec2(r * cos(theta), r * sin(theta))
    }

    fun harmonicValue(x: Double, components: Collection<HarmonicComponent>): Double = components.filter { it.enabled }.sumOf {
        it.amplitude * sin(it.multiple * x + it.phase)
    }

    fun heightFromObservation(distance: Double, elevationDegrees: Double, observerHeight: Double = 0.0): Double {
        require(distance >= 0 && elevationDegrees > 0 && elevationDegrees < 90)
        return observerHeight + distance * tan(elevationDegrees * PI / 180)
    }

    private fun specialAngleLabel(radians: Double): String? {
        val ratio = radians / PI
        val denominator = (1..12).firstOrNull { abs(ratio * it - kotlin.math.round(ratio * it)) < 1e-6 } ?: return null
        val numerator = kotlin.math.round(ratio * denominator).toInt()
        return when {
            numerator == 0 -> "0"
            denominator == 1 -> if (numerator == 1) "π" else "${numerator}π"
            numerator == 1 -> "π/$denominator"
            else -> "${numerator}π/$denominator"
        }
    }

    private fun specialAngle(degrees: Double): Triple<String, String, String>? {
        val index = (degrees / 30.0).toInt()
        if (abs(degrees - index * 30.0) > 1e-7) {
            val quarterIndex = (degrees / 45.0).toInt()
            if (abs(degrees - quarterIndex * 45.0) > 1e-7) return null
        }
        val key = ((degrees.roundToInt() % 360) + 360) % 360
        val values = mapOf(
            0 to Triple("1", "0", "0"), 30 to Triple("√3/2", "1/2", "1/√3"),
            45 to Triple("√2/2", "√2/2", "1"), 60 to Triple("1/2", "√3/2", "√3"),
            90 to Triple("0", "1", "undefined"), 120 to Triple("−1/2", "√3/2", "−√3"),
            135 to Triple("−√2/2", "√2/2", "−1"), 150 to Triple("−√3/2", "1/2", "−1/√3"),
            180 to Triple("−1", "0", "0"), 210 to Triple("−√3/2", "−1/2", "1/√3"),
            225 to Triple("−√2/2", "−√2/2", "1"), 240 to Triple("−1/2", "−√3/2", "√3"),
            270 to Triple("0", "−1", "undefined"), 300 to Triple("1/2", "−√3/2", "−√3"),
            315 to Triple("√2/2", "−√2/2", "−1"), 330 to Triple("√3/2", "−1/2", "−1/√3"),
        )
        return values[key]
    }

    private fun clean(value: Double) = if (abs(value) < 1e-12) 0.0 else value
    private fun Double.roundToInt() = kotlin.math.round(this).toInt()
}

data class TriangleSolution(
    val a: Double, val b: Double, val c: Double,
    val angleA: Double, val angleB: Double, val angleC: Double,
) {
    val area: Double get() { val s = (a + b + c) / 2; return sqrt((s * (s - a) * (s - b) * (s - c)).coerceAtLeast(0.0)) }
    val perimeter: Double get() = a + b + c
    val circumradius: Double get() = a * b * c / (4 * area)
    val inradius: Double get() = 2 * area / perimeter
}

/** Solves SSS, SAS and ambiguous SSA; returned list explicitly exposes the two-solution case. */
object TriangleTrigSolver {
    fun sss(a: Double, b: Double, c: Double): TriangleSolution {
        require(a > 0 && b > 0 && c > 0 && a + b > c && a + c > b && b + c > a) { "Sides must form a non-degenerate triangle." }
        val A = degrees(acos(clamp((b * b + c * c - a * a) / (2 * b * c))))
        val B = degrees(acos(clamp((a * a + c * c - b * b) / (2 * a * c))))
        return TriangleSolution(a, b, c, A, B, 180 - A - B)
    }

    fun sas(sideA: Double, sideB: Double, includedAngleC: Double): TriangleSolution {
        require(sideA > 0 && sideB > 0 && includedAngleC in 1e-9..179.999999)
        val c = sqrt(sideA * sideA + sideB * sideB - 2 * sideA * sideB * cos(radians(includedAngleC)))
        return sss(sideA, sideB, c)
    }

    fun ssa(sideA: Double, sideB: Double, angleA: Double): List<TriangleSolution> {
        require(sideA > 0 && sideB > 0 && angleA in 1e-9..179.999999)
        val ratio = sideB * sin(radians(angleA)) / sideA
        if (ratio > 1 + 1e-12) return emptyList()
        val firstB = degrees(asin(clamp(ratio)))
        return listOf(firstB, 180 - firstB).distinctBy { kotlin.math.round(it * 1e9) }.mapNotNull { B ->
            val C = 180 - angleA - B
            if (C <= 1e-9) null else {
                val c = sideA * sin(radians(C)) / sin(radians(angleA))
                TriangleSolution(sideA, sideB, c, angleA, B, C)
            }
        }
    }

    fun asa(sideC: Double, angleA: Double, angleB: Double): TriangleSolution {
        require(sideC > 0 && angleA > 0 && angleB > 0 && angleA + angleB < 180)
        val angleC = 180 - angleA - angleB
        val scale = sideC / sin(radians(angleC))
        return TriangleSolution(scale * sin(radians(angleA)), scale * sin(radians(angleB)), sideC, angleA, angleB, angleC)
    }

    fun aas(sideA: Double, angleA: Double, angleB: Double): TriangleSolution {
        require(sideA > 0 && angleA > 0 && angleB > 0 && angleA + angleB < 180)
        val angleC = 180 - angleA - angleB
        val scale = sideA / sin(radians(angleA))
        return TriangleSolution(sideA, scale * sin(radians(angleB)), scale * sin(radians(angleC)), angleA, angleB, angleC)
    }

    private fun radians(value: Double) = value * PI / 180
    private fun degrees(value: Double) = value * 180 / PI
    private fun clamp(value: Double) = value.coerceIn(-1.0, 1.0)
}

data class TrigIdentityCheck(val left: String, val right: String, val label: String, val evidence: EquivalenceEvidence)

class InteractiveTrigIdentityLab(private val kernel: TrustedMathKernel = TrustedMathKernel()) {
    val catalog = listOf(
        Triple("Pythagorean", "sin(x)^2 + cos(x)^2", "1"),
        Triple("Tangent ratio", "tan(x)", "sin(x)/cos(x)"),
        Triple("Double-angle sine", "sin(2*x)", "2*sin(x)*cos(x)"),
        Triple("Double-angle cosine", "cos(2*x)", "cos(x)^2-sin(x)^2"),
        Triple("Complementary", "sin(pi/2-x)", "cos(x)"),
    )

    fun verify(index: Int): TrigIdentityCheck {
        val item = catalog[index.coerceIn(catalog.indices)]
        return TrigIdentityCheck(item.second, item.third, item.first, kernel.equivalence(item.second, item.third, requestedSamples = 31))
    }
}
