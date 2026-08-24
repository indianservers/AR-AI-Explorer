package com.indianservers.aiexplorer.arengine.measurement

import com.indianservers.aiexplorer.arengine.contract.ArVector3
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

enum class ArLengthUnit(val symbol: String, internal val metersPerUnit: Double?) {
    Millimeter("mm", 0.001),
    Centimeter("cm", 0.01),
    Meter("m", 1.0),
    MathematicalUnit("unit", null),
}

data class ArPhysicalScale(val metersPerMathUnit: Double = 0.1) {
    init {
        require(metersPerMathUnit.isFinite() && metersPerMathUnit > 0.0)
    }

    fun lengthToMeters(value: Double, unit: ArLengthUnit): Double = value * factor(unit)
    fun lengthFromMeters(meters: Double, unit: ArLengthUnit): Double = meters / factor(unit)
    fun areaFromSquareMeters(squareMeters: Double, unit: ArLengthUnit): Double = squareMeters / factor(unit).pow(2)
    fun volumeFromCubicMeters(cubicMeters: Double, unit: ArLengthUnit): Double = cubicMeters / factor(unit).pow(3)
    fun calibrated(
        measuredMeters: Double,
        assignedValue: Double,
        assignedUnit: ArLengthUnit,
        spanMathUnits: Double = measuredMeters / metersPerMathUnit,
    ): ArPhysicalScale {
        require(measuredMeters.isFinite() && measuredMeters > 0.0)
        require(assignedValue.isFinite() && assignedValue > 0.0)
        require(spanMathUnits.isFinite() && spanMathUnits > 0.0)
        val assignedMeters = assignedValue * (assignedUnit.metersPerUnit ?: metersPerMathUnit)
        return ArPhysicalScale(assignedMeters / spanMathUnits)
    }

    fun relationshipLabel(): String {
        val centimeters = metersPerMathUnit * 100.0
        return if (metersPerMathUnit >= 1.0) "1 mathematical unit = ${ArMeasurementFormatter.number(metersPerMathUnit, .01)} m"
        else "1 mathematical unit = ${ArMeasurementFormatter.number(centimeters, .1)} cm"
    }

    private fun factor(unit: ArLengthUnit) = unit.metersPerUnit ?: metersPerMathUnit
}

data class ArMeasuredPoint(
    val label: String,
    val positionMeters: ArVector3,
    val uncertaintyMeters: Double = 0.03,
    val confidence: Double = 0.5,
) {
    init {
        require(label.isNotBlank())
        require(uncertaintyMeters.isFinite() && uncertaintyMeters >= 0.0)
        require(confidence in 0.0..1.0)
    }
}

data class ArDistanceResult(
    val distanceMeters: Double,
    val horizontalMeters: Double,
    val verticalMeters: Double,
    val uncertaintyMeters: Double,
)

data class ArAngleResult(val degrees: Double, val uncertaintyDegrees: Double)

data class ArPolygonResult(
    val areaSquareMeters: Double,
    val perimeterMeters: Double,
    val maxPlanarityErrorMeters: Double,
    val planar: Boolean,
    val normal: ArVector3,
)

data class ArCircleFit(
    val centerMeters: ArVector3,
    val radiusMeters: Double,
    val diameterMeters: Double,
    val normal: ArVector3,
)

object ArMeasurementMath {
    fun distance(a: ArMeasuredPoint, b: ArMeasuredPoint): ArDistanceResult {
        val delta = b.positionMeters - a.positionMeters
        return ArDistanceResult(
            distanceMeters = delta.magnitude(),
            horizontalMeters = sqrt(delta.x * delta.x + delta.z * delta.z),
            verticalMeters = abs(delta.y),
            uncertaintyMeters = sqrt(a.uncertaintyMeters.pow(2) + b.uncertaintyMeters.pow(2)),
        )
    }

    fun polyline(points: List<ArMeasuredPoint>, closed: Boolean = false): ArDistanceResult {
        require(points.size >= 2)
        val pairs = points.zipWithNext() + if (closed) listOf(points.last() to points.first()) else emptyList()
        val segments = pairs.map { distance(it.first, it.second) }
        return ArDistanceResult(
            segments.sumOf { it.distanceMeters },
            segments.sumOf { it.horizontalMeters },
            segments.sumOf { it.verticalMeters },
            sqrt(segments.sumOf { it.uncertaintyMeters.pow(2) }),
        )
    }

    fun angle(a: ArMeasuredPoint, vertex: ArMeasuredPoint, c: ArMeasuredPoint): ArAngleResult {
        val left = a.positionMeters - vertex.positionMeters
        val right = c.positionMeters - vertex.positionMeters
        val denominator = left.magnitude() * right.magnitude()
        require(denominator > 1e-9) { "Angle points must be distinct." }
        val degrees = Math.toDegrees(acos((left.dot(right) / denominator).coerceIn(-1.0, 1.0)))
        val relative = (a.uncertaintyMeters + vertex.uncertaintyMeters + c.uncertaintyMeters) / denominator.pow(.5)
        return ArAngleResult(degrees, Math.toDegrees(relative).coerceIn(.1, 30.0))
    }

    fun polygon(points: List<ArMeasuredPoint>, toleranceMeters: Double = 0.025): ArPolygonResult {
        require(points.size >= 3)
        require(toleranceMeters.isFinite() && toleranceMeters > 0.0)
        val origin = points.first().positionMeters
        val normal = firstStableNormal(points.map { it.positionMeters })
        val maxError = points.maxOf { abs((it.positionMeters - origin).dot(normal)) }
        val areaVector = points.indices.fold(ArVector3.Zero) { sum, index ->
            sum + cross(points[index].positionMeters, points[(index + 1) % points.size].positionMeters)
        }
        val perimeter = points.indices.sumOf { index ->
            (points[(index + 1) % points.size].positionMeters - points[index].positionMeters).magnitude()
        }
        return ArPolygonResult(abs(areaVector.dot(normal)) * .5, perimeter, maxError, maxError <= toleranceMeters, normal)
    }

    fun circleThrough(a: ArMeasuredPoint, b: ArMeasuredPoint, c: ArMeasuredPoint): ArCircleFit {
        val u = b.positionMeters - a.positionMeters
        val v = c.positionMeters - a.positionMeters
        val w = cross(u, v)
        val denominator = 2.0 * w.dot(w)
        require(denominator > 1e-12) { "Circle points must be non-collinear." }
        val offset = (cross(v, w) * u.dot(u) + cross(w, u) * v.dot(v)) * (1.0 / denominator)
        val center = a.positionMeters + offset
        val radius = (center - a.positionMeters).magnitude()
        return ArCircleFit(center, radius, radius * 2.0, normalize(w))
    }

    private fun firstStableNormal(points: List<ArVector3>): ArVector3 {
        for (i in 1 until points.lastIndex) {
            val value = cross(points[i] - points[0], points[i + 1] - points[0])
            if (value.magnitude() > 1e-9) return normalize(value)
        }
        throw IllegalArgumentException("Polygon points must not be collinear.")
    }
}

sealed interface ArPrimitiveFit {
    val uncertaintyMeters: Double
    val volumeCubicMeters: Double
    val surfaceAreaSquareMeters: Double
}

data class ArCylinderFit(
    val baseCenter: ArVector3,
    val axis: ArVector3,
    val radiusMeters: Double,
    val heightMeters: Double,
    override val uncertaintyMeters: Double,
) : ArPrimitiveFit {
    val diameterMeters get() = radiusMeters * 2.0
    val curvedSurfaceAreaSquareMeters get() = 2.0 * PI * radiusMeters * heightMeters
    override val surfaceAreaSquareMeters get() = 2.0 * PI * radiusMeters * (radiusMeters + heightMeters)
    override val volumeCubicMeters get() = PI * radiusMeters.pow(2) * heightMeters
}

data class ArConeFit(
    val baseCenter: ArVector3,
    val axis: ArVector3,
    val radiusMeters: Double,
    val heightMeters: Double,
    override val uncertaintyMeters: Double,
) : ArPrimitiveFit {
    val slantHeightMeters get() = sqrt(radiusMeters.pow(2) + heightMeters.pow(2))
    val curvedSurfaceAreaSquareMeters get() = PI * radiusMeters * slantHeightMeters
    override val surfaceAreaSquareMeters get() = PI * radiusMeters * (slantHeightMeters + radiusMeters)
    override val volumeCubicMeters get() = PI * radiusMeters.pow(2) * heightMeters / 3.0
}

data class ArSphereFit(
    val center: ArVector3,
    val radiusMeters: Double,
    val radialDeviationMeters: Double,
    override val uncertaintyMeters: Double,
) : ArPrimitiveFit {
    override val surfaceAreaSquareMeters get() = 4.0 * PI * radiusMeters.pow(2)
    override val volumeCubicMeters get() = 4.0 * PI * radiusMeters.pow(3) / 3.0
}

data class ArCuboidFit(
    val origin: ArVector3,
    val lengthMeters: Double,
    val widthMeters: Double,
    val heightMeters: Double,
    val baseRightAngleErrorDegrees: Double,
    override val uncertaintyMeters: Double,
) : ArPrimitiveFit {
    val spaceDiagonalMeters get() = sqrt(lengthMeters.pow(2) + widthMeters.pow(2) + heightMeters.pow(2))
    override val surfaceAreaSquareMeters get() = 2.0 * (lengthMeters * widthMeters + lengthMeters * heightMeters + widthMeters * heightMeters)
    override val volumeCubicMeters get() = lengthMeters * widthMeters * heightMeters
}

object ArPrimitiveFitter {
    fun cylinder(baseCenter: ArMeasuredPoint, baseRim: ArMeasuredPoint, topCenter: ArMeasuredPoint): ArCylinderFit {
        val axisVector = topCenter.positionMeters - baseCenter.positionMeters
        val height = axisVector.magnitude()
        require(height > 1e-6) { "Cylinder height must be positive." }
        val axis = axisVector * (1.0 / height)
        val rim = baseRim.positionMeters - baseCenter.positionMeters
        val radius = (rim - axis * rim.dot(axis)).magnitude()
        require(radius > 1e-6) { "Cylinder radius must be positive." }
        return ArCylinderFit(baseCenter.positionMeters, axis, radius, height, combinedUncertainty(baseCenter, baseRim, topCenter))
    }

    fun cone(baseCenter: ArMeasuredPoint, baseRim: ArMeasuredPoint, apex: ArMeasuredPoint): ArConeFit {
        val cylinder = cylinder(baseCenter, baseRim, apex)
        return ArConeFit(cylinder.baseCenter, cylinder.axis, cylinder.radiusMeters, cylinder.heightMeters, cylinder.uncertaintyMeters)
    }

    fun sphere(center: ArMeasuredPoint, surfacePoints: List<ArMeasuredPoint>): ArSphereFit {
        require(surfacePoints.isNotEmpty())
        val radii = surfacePoints.map { (it.positionMeters - center.positionMeters).magnitude() }
        val radius = radii.average()
        require(radius > 1e-6) { "Sphere radius must be positive." }
        val deviation = sqrt(radii.sumOf { (it - radius).pow(2) } / radii.size)
        return ArSphereFit(center.positionMeters, radius, deviation, combinedUncertainty(center, *surfacePoints.toTypedArray()))
    }

    fun cuboid(origin: ArMeasuredPoint, lengthCorner: ArMeasuredPoint, widthCorner: ArMeasuredPoint, topPoint: ArMeasuredPoint): ArCuboidFit {
        val lengthVector = lengthCorner.positionMeters - origin.positionMeters
        val widthVector = widthCorner.positionMeters - origin.positionMeters
        val length = lengthVector.magnitude()
        val width = widthVector.magnitude()
        require(length > 1e-6 && width > 1e-6) { "Cuboid base edges must be positive." }
        val normal = normalize(cross(lengthVector, widthVector))
        val height = abs((topPoint.positionMeters - origin.positionMeters).dot(normal))
        require(height > 1e-6) { "Cuboid height must be positive." }
        val baseAngle = Math.toDegrees(acos((lengthVector.dot(widthVector) / (length * width)).coerceIn(-1.0, 1.0)))
        return ArCuboidFit(origin.positionMeters, length, width, height, abs(90.0 - baseAngle), combinedUncertainty(origin, lengthCorner, widthCorner, topPoint))
    }

    private fun combinedUncertainty(vararg points: ArMeasuredPoint) = sqrt(points.sumOf { it.uncertaintyMeters.pow(2) })
}

enum class ArFitShape(val requiredPoints: Int, val pointPrompts: List<String>) {
    Cylinder(3, listOf("Tap the centre of the base.", "Tap a point on the base rim.", "Tap the top centre.")),
    Cone(3, listOf("Tap the centre of the base.", "Tap a point on the base rim.", "Tap the apex.")),
    Sphere(2, listOf("Tap the centre of the sphere.", "Tap a point on its surface.")),
    Cuboid(4, listOf("Tap the first base corner.", "Tap the length corner.", "Tap the width corner.", "Tap a top corner to set height.")),
}

data class ArPrimitiveFitWorkflow(
    val shape: ArFitShape,
    val points: List<ArMeasuredPoint> = emptyList(),
    val locked: Boolean = false,
) {
    init {
        require(points.size <= shape.requiredPoints)
    }

    val complete get() = points.size == shape.requiredPoints
    val progress get() = points.size.toDouble() / shape.requiredPoints
    val instruction get() = if (complete) "Fit ready. Fine-tune the dimensions, then lock the result." else shape.pointPrompts[points.size]

    fun add(point: ArMeasuredPoint): ArPrimitiveFitWorkflow {
        require(!locked && !complete)
        return copy(points = points + point)
    }

    fun undo() = if (locked || points.isEmpty()) this else copy(points = points.dropLast(1))
    fun restart() = copy(points = emptyList(), locked = false)
    fun lock() = if (complete) copy(locked = true) else this

    fun result(): ArPrimitiveFit? {
        if (!complete) return null
        return when (shape) {
            ArFitShape.Cylinder -> ArPrimitiveFitter.cylinder(points[0], points[1], points[2])
            ArFitShape.Cone -> ArPrimitiveFitter.cone(points[0], points[1], points[2])
            ArFitShape.Sphere -> ArPrimitiveFitter.sphere(points[0], points.drop(1))
            ArFitShape.Cuboid -> ArPrimitiveFitter.cuboid(points[0], points[1], points[2], points[3])
        }
    }
}

object ArMeasurementFormatter {
    fun number(value: Double, uncertainty: Double): String {
        require(value.isFinite())
        val decimals = decimalsFor(uncertainty)
        return String.format(Locale.US, "%.${decimals}f", value)
    }

    fun approximate(value: Double, uncertainty: Double, unit: String): String =
        "Approximate AR measurement: ${number(value, uncertainty)} $unit"

    fun confidence(confidence: Double): String = when {
        confidence >= .8 -> "High confidence"
        confidence >= .55 -> "Medium confidence"
        else -> "Low confidence - move slowly and improve lighting"
    }

    private fun decimalsFor(uncertainty: Double): Int {
        if (!uncertainty.isFinite() || uncertainty <= 0.0) return 2
        val significantPlace = floor(-log10(uncertainty)).toInt()
        return min(3, max(0, significantPlace))
    }
}

private fun cross(a: ArVector3, b: ArVector3) = ArVector3(
    a.y * b.z - a.z * b.y,
    a.z * b.x - a.x * b.z,
    a.x * b.y - a.y * b.x,
)

private fun normalize(value: ArVector3): ArVector3 {
    val magnitude = value.magnitude()
    require(magnitude > 1e-12)
    return value * (1.0 / magnitude)
}
