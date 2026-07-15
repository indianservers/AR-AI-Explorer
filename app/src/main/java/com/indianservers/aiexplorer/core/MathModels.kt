package com.indianservers.aiexplorer.core

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

data class Vec2(val x: Double, val y: Double) {
    operator fun plus(other: Vec2) = Vec2(x + other.x, y + other.y)
    operator fun minus(other: Vec2) = Vec2(x - other.x, y - other.y)
    operator fun times(scale: Double) = Vec2(x * scale, y * scale)
    fun distanceTo(other: Vec2) = hypot(x - other.x, y - other.y)
}

data class Vec3(val x: Double, val y: Double, val z: Double) {
    operator fun plus(other: Vec3) = Vec3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vec3) = Vec3(x - other.x, y - other.y, z - other.z)
    operator fun times(scale: Double) = Vec3(x * scale, y * scale, z * scale)
    fun magnitude() = sqrt(x * x + y * y + z * z)
}

data class Vector3D(
    val id: String,
    val start: Vec3,
    val end: Vec3,
    val name: String = id,
) {
    val components: Vec3 get() = end - start
    val magnitude: Double get() = components.magnitude()
}

data class SegmentMeasurements(
    val midpoint: Vec2,
    val slope: Double?,
    val distance: Double,
    val exactDistance: String,
)

object Geometry2D {
    fun segment(a: Vec2, b: Vec2): SegmentMeasurements {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val square = dx * dx + dy * dy
        return SegmentMeasurements(
            midpoint = Vec2((a.x + b.x) / 2.0, (a.y + b.y) / 2.0),
            slope = if (abs(dx) < 1e-9) null else dy / dx,
            distance = sqrt(square),
            exactDistance = "sqrt(${trim(square)})",
        )
    }

    fun polygonArea(points: List<Vec2>): Double {
        if (points.size < 3) return 0.0
        return abs(points.indices.sumOf { i ->
            val a = points[i]
            val b = points[(i + 1) % points.size]
            a.x * b.y - b.x * a.y
        }) / 2.0
    }

    fun angle(a: Vec2, b: Vec2, c: Vec2): Double {
        val u = a - b
        val v = c - b
        val dot = u.x * v.x + u.y * v.y
        val mag = max(1e-9, u.distanceTo(Vec2(0.0, 0.0)) * v.distanceTo(Vec2(0.0, 0.0)))
        return Math.toDegrees(acos((dot / mag).coerceIn(-1.0, 1.0)))
    }
}

class ExpressionEngine {
    fun compile(source: String): Expression = Parser(source).parse()
}

class Expression internal constructor(private val root: Node, val source: String) {
    fun eval(variables: Map<String, Double> = emptyMap()): Double = root.eval(variables)
}

internal sealed interface Node {
    fun eval(v: Map<String, Double>): Double
}

private data class NumberNode(val value: Double) : Node {
    override fun eval(v: Map<String, Double>) = value
}

private data class VariableNode(val name: String) : Node {
    override fun eval(v: Map<String, Double>) = when (name) {
        "pi", "π" -> PI
        "e" -> kotlin.math.E
        else -> v[name] ?: error("Missing value for $name")
    }
}

private data class UnaryNode(val op: Char, val node: Node) : Node {
    override fun eval(v: Map<String, Double>) = when (op) {
        '-' -> -node.eval(v)
        else -> node.eval(v)
    }
}

private data class BinaryNode(val op: Char, val left: Node, val right: Node) : Node {
    override fun eval(v: Map<String, Double>): Double {
        val a = left.eval(v)
        val b = right.eval(v)
        return when (op) {
            '+' -> a + b
            '-' -> a - b
            '*' -> a * b
            '/' -> a / b
            '^' -> a.pow(b)
            else -> error("Unknown operator $op")
        }
    }
}

private data class FunctionNode(val name: String, val arg: Node) : Node {
    override fun eval(v: Map<String, Double>): Double {
        val x = arg.eval(v)
        return when (name.lowercase()) {
            "sin" -> sin(x)
            "cos" -> cos(x)
            "tan" -> tan(x)
            "asin" -> asin(x)
            "acos" -> acos(x)
            "atan" -> atan(x)
            "sqrt" -> sqrt(x)
            "abs" -> abs(x)
            "exp" -> exp(x)
            "ln" -> ln(x)
            "log" -> kotlin.math.log10(x)
            else -> error("Unknown function $name")
        }
    }
}

private class Parser(private val input: String) {
    private var index = 0

    fun parse(): Expression {
        val node = parseExpression()
        skip()
        require(index == input.length) { "Unexpected token '${input[index]}'" }
        return Expression(node, input)
    }

    private fun parseExpression(): Node {
        var node = parseTerm()
        while (true) {
            skip()
            node = when {
                match('+') -> BinaryNode('+', node, parseTerm())
                match('-') -> BinaryNode('-', node, parseTerm())
                else -> return node
            }
        }
    }

    private fun parseTerm(): Node {
        var node = parseUnary()
        while (true) {
            skip()
            node = when {
                match('*') -> BinaryNode('*', node, parseUnary())
                match('/') -> BinaryNode('/', node, parseUnary())
                shouldImplicitMultiply() -> BinaryNode('*', node, parseUnary())
                else -> return node
            }
        }
    }

    private fun parsePower(): Node {
        var node = parsePrimary()
        skip()
        if (match('^')) node = BinaryNode('^', node, parseUnary())
        return node
    }

    private fun parseUnary(): Node {
        skip()
        return when {
            match('-') -> UnaryNode('-', parsePower())
            match('+') -> parseUnary()
            else -> parsePower()
        }
    }

    private fun parsePrimary(): Node {
        skip()
        if (match('(')) {
            val node = parseExpression()
            require(match(')')) { "Missing closing parenthesis" }
            return node
        }
        if (peek()?.isDigit() == true || peek() == '.') return parseNumber()
        if (peek()?.isLetter() == true || peek() == 'π') {
            val name = parseIdentifier()
            skip()
            return if (match('(')) {
                val arg = parseExpression()
                require(match(')')) { "Missing closing parenthesis" }
                FunctionNode(name, arg)
            } else {
                VariableNode(name)
            }
        }
        error("Expected number, variable, or function")
    }

    private fun parseNumber(): Node {
        val start = index
        while (peek()?.isDigit() == true || peek() == '.') index++
        return NumberNode(input.substring(start, index).toDouble())
    }

    private fun parseIdentifier(): String {
        val start = index
        while (peek()?.isLetter() == true || peek() == 'π') index++
        return input.substring(start, index)
    }

    private fun shouldImplicitMultiply(): Boolean {
        val c = peek() ?: return false
        return c == '(' || c == 'π' || c.isLetter()
    }

    private fun match(c: Char): Boolean {
        skip()
        if (peek() == c) {
            index++
            return true
        }
        return false
    }

    private fun peek(): Char? = input.getOrNull(index)
    private fun skip() {
        while (peek()?.isWhitespace() == true) index++
    }
}

data class FunctionDefinition(
    val id: String,
    val name: String,
    val expression: String,
    val colorKey: String,
    val visible: Boolean = true,
)

data class CurveSample(val points: List<Vec2>, val breaks: Set<Int>)
data class QuadraticInsight(
    val vertex: Vec2,
    val axis: Double,
    val roots: List<Double>,
    val yIntercept: Double,
    val range: String,
    val factored: String?,
)

data class LinearInsight(val slope: Double, val xIntercept: Double?, val yIntercept: Double)

class GraphAnalysis(private val engine: ExpressionEngine = ExpressionEngine()) {
    fun sample(expression: String, minX: Double, maxX: Double, steps: Int = 360): CurveSample {
        val compiled = engine.compile(stripEquation(expression))
        val points = mutableListOf<Vec2>()
        val breaks = mutableSetOf<Int>()
        var lastY: Double? = null
        for (i in 0..steps) {
            val x = minX + (maxX - minX) * i / steps
            val y = runCatching { compiled.eval(mapOf("x" to x)) }.getOrElse { Double.NaN }
            val previousY = lastY
            if (!y.isFinite() || (previousY != null && abs(y - previousY) > 20.0)) {
                breaks += points.size
                lastY = null
            } else {
                points += Vec2(x, y)
                lastY = y
            }
        }
        return CurveSample(points, breaks)
    }

    fun quadratic(a: Double, b: Double, c: Double): QuadraticInsight {
        val vx = -b / (2.0 * a)
        val vy = a * vx * vx + b * vx + c
        val d = b * b - 4.0 * a * c
        val roots = if (d >= 0.0) {
            listOf((-b - sqrt(d)) / (2.0 * a), (-b + sqrt(d)) / (2.0 * a)).sorted()
        } else {
            emptyList()
        }
        return QuadraticInsight(
            vertex = Vec2(vx, vy),
            axis = vx,
            roots = roots,
            yIntercept = c,
            range = if (a > 0) "y >= ${trim(vy)}" else "y <= ${trim(vy)}",
            factored = if (roots.size == 2) "(x - ${trim(roots[0])})(x - ${trim(roots[1])})" else null,
        )
    }

    fun linear(m: Double, b: Double) = LinearInsight(
        slope = m,
        xIntercept = if (abs(m) < 1e-9) null else -b / m,
        yIntercept = b,
    )

    fun intersections(f: Expression, g: Expression, minX: Double, maxX: Double): List<Vec2> {
        val roots = mutableListOf<Vec2>()
        var prevX = minX
        var prev = f.eval(mapOf("x" to prevX)) - g.eval(mapOf("x" to prevX))
        val n = 400
        for (i in 1..n) {
            val x = minX + (maxX - minX) * i / n
            val value = f.eval(mapOf("x" to x)) - g.eval(mapOf("x" to x))
            if (prev == 0.0 || value == 0.0 || prev.sign() != value.sign()) {
                val root = bisect(f, g, prevX, x)
                roots += Vec2(root, f.eval(mapOf("x" to root)))
            }
            prevX = x
            prev = value
        }
        return roots.distinctBy { trim(it.x) }
    }

    private fun bisect(f: Expression, g: Expression, left: Double, right: Double): Double {
        var lo = left
        var hi = right
        repeat(40) {
            val mid = (lo + hi) / 2.0
            val a = f.eval(mapOf("x" to lo)) - g.eval(mapOf("x" to lo))
            val b = f.eval(mapOf("x" to mid)) - g.eval(mapOf("x" to mid))
            if (a.sign() == b.sign()) lo = mid else hi = mid
        }
        return (lo + hi) / 2.0
    }
}

enum class SolidType { Cube, Cuboid, Sphere, Cylinder, Cone, Pyramid, Torus }

data class Solid(
    val type: SolidType,
    val width: Double,
    val height: Double = width,
    val depth: Double = width,
    val radius: Double = width / 2.0,
    val position: Vec3 = Vec3(0.0, 0.0, 0.0),
)

data class SolidMeasurements(val volume: Double, val surfaceArea: Double, val faces: Int, val edges: Int, val vertices: Int)

object Geometry3D {
    fun measure(solid: Solid): SolidMeasurements = when (solid.type) {
        SolidType.Cube -> SolidMeasurements(solid.width.pow(3), 6 * solid.width.pow(2), 6, 12, 8)
        SolidType.Cuboid -> SolidMeasurements(
            solid.width * solid.height * solid.depth,
            2 * (solid.width * solid.height + solid.width * solid.depth + solid.height * solid.depth),
            6,
            12,
            8,
        )
        SolidType.Sphere -> SolidMeasurements(4.0 / 3.0 * PI * solid.radius.pow(3), 4 * PI * solid.radius.pow(2), 1, 0, 0)
        SolidType.Cylinder -> SolidMeasurements(PI * solid.radius.pow(2) * solid.height, 2 * PI * solid.radius * (solid.radius + solid.height), 3, 2, 0)
        SolidType.Cone -> {
            val slant = sqrt(solid.radius.pow(2) + solid.height.pow(2))
            SolidMeasurements(PI * solid.radius.pow(2) * solid.height / 3.0, PI * solid.radius * (solid.radius + slant), 2, 1, 1)
        }
        SolidType.Pyramid -> SolidMeasurements(solid.width * solid.depth * solid.height / 3.0, solid.width * solid.depth + 2 * solid.width * solid.height / 2 + 2 * solid.depth * solid.height / 2, 5, 8, 5)
        SolidType.Torus -> SolidMeasurements(2 * PI.pow(2) * solid.width * solid.radius.pow(2), 4 * PI.pow(2) * solid.width * solid.radius, 1, 0, 0)
    }
}

data class SurfaceMesh(val vertices: List<Vec3>, val rows: Int, val columns: Int)
data class SurfaceInsight(val classification: String, val vertex: Vec3?, val range: String, val symmetry: String)

class Graph3D(private val engine: ExpressionEngine = ExpressionEngine()) {
    fun mesh(expression: String, min: Double = -3.0, max: Double = 3.0, density: Int = 32): SurfaceMesh {
        val compiled = engine.compile(stripEquation(expression).replace("y", "yy"))
        val vertices = mutableListOf<Vec3>()
        for (i in 0..density) {
            val x = min + (max - min) * i / density
            for (j in 0..density) {
                val y = min + (max - min) * j / density
                val z = runCatching { compiled.eval(mapOf("x" to x, "yy" to y)) }.getOrDefault(Double.NaN)
                if (z.isFinite()) vertices += Vec3(x, y, z.coerceIn(-8.0, 8.0))
            }
        }
        return SurfaceMesh(vertices, density + 1, density + 1)
    }

    fun insight(expression: String): SurfaceInsight {
        val normalized = stripEquation(expression).replace(" ", "")
        return if (normalized in setOf("x^2+y^2", "x*x+y*y")) {
            SurfaceInsight(
                classification = "Elliptic paraboloid",
                vertex = Vec3(0.0, 0.0, 0.0),
                range = "z >= 0",
                symmetry = "Rotational symmetry about the z-axis",
            )
        } else {
            SurfaceInsight("Sampled surface", null, "Computed on selected domain", "Inspect by slices and contours")
        }
    }
}

fun stripEquation(value: String): String = value.substringAfter("=").trim().ifBlank { value.trim() }

fun trim(value: Double): String {
    if (!value.isFinite()) return value.toString()
    val rounded = kotlin.math.round(value * 1_000_000.0) / 1_000_000.0
    return if (abs(rounded - rounded.toLong()) < 1e-9) rounded.toLong().toString() else rounded.toString()
}

private fun Double.sign(): Int = when {
    this > 0 -> 1
    this < 0 -> -1
    else -> 0
}
