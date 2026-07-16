package com.indianservers.aiexplorer.core

import kotlin.math.abs
import kotlin.math.round

enum class ManipulativeKind {
    AlgebraX, AlgebraUnit, FractionBar, IntegerChip, NumberLinePoint, NumberLineInterval,
    BalanceWeight, PatternBlock, GeometricTile, Ruler, Protractor, AngleTool,
}
enum class ManipulativeTray { Algebra, Fractions, Numbers, Balance, Geometry, Measure }

data class ManipulativeItem(
    val id: String,
    val kind: ManipulativeKind,
    val position: Vec2,
    val width: Double = 1.0,
    val height: Double = 1.0,
    val rotationDegrees: Double = 0.0,
    val scale: Double = 1.0,
    val groupId: String? = null,
    val locked: Boolean = false,
    val label: String = kind.name,
    val annotation: String = "",
    val value: Double = 1.0,
    val numerator: Int = 1,
    val denominator: Int = 1,
    val side: String = "left",
)

data class ManipulativeScene(
    val id: String = "scene",
    val title: String = "Manipulatives Lab",
    val items: List<ManipulativeItem> = emptyList(),
    val snapSize: Double = .5,
    val revision: Int = 0,
)

enum class FormalMathDestination { Equation, Graph, Notebook }
data class FormalMathLink(val destination: FormalMathDestination, val content: String, val explanation: String)

class ManipulativeEngine {
    fun create(scene: ManipulativeScene, kind: ManipulativeKind, position: Vec2, label: String = kind.name): ManipulativeScene {
        val id = "item-" + (scene.items.mapNotNull { it.id.substringAfterLast('-').toIntOrNull() }.maxOrNull() ?: 0).plus(1)
        return scene.copy(items = scene.items + ManipulativeItem(id, kind, snap(position, scene.snapSize), label = label), revision = scene.revision + 1)
    }

    fun move(scene: ManipulativeScene, id: String, position: Vec2): ManipulativeScene {
        val selected = scene.items.firstOrNull { it.id == id } ?: return scene
        if (selected.locked) return scene
        val delta = snap(position, scene.snapSize) - selected.position
        val group = selected.groupId
        return scene.copy(items = scene.items.map { item ->
            if (item.id == id || group != null && item.groupId == group) item.copy(position = item.position + delta) else item
        }, revision = scene.revision + 1)
    }

    fun duplicate(scene: ManipulativeScene, id: String): ManipulativeScene {
        val source = scene.items.first { it.id == id }
        val next = "item-" + ((scene.items.mapNotNull { it.id.substringAfterLast('-').toIntOrNull() }.maxOrNull() ?: 0) + 1)
        return scene.copy(items = scene.items + source.copy(id = next, position = source.position + Vec2(scene.snapSize, scene.snapSize), groupId = null, locked = false), revision = scene.revision + 1)
    }

    fun group(scene: ManipulativeScene, ids: Set<String>): ManipulativeScene {
        require(ids.size >= 2 && ids.all { id -> scene.items.any { it.id == id } })
        val groupId = "group-" + (scene.revision + 1)
        return scene.copy(items = scene.items.map { if (it.id in ids) it.copy(groupId = groupId) else it }, revision = scene.revision + 1)
    }

    fun ungroup(scene: ManipulativeScene, groupId: String) =
        scene.copy(items = scene.items.map { if (it.groupId == groupId) it.copy(groupId = null) else it }, revision = scene.revision + 1)

    fun transform(scene: ManipulativeScene, id: String, rotationDelta: Double = 0.0, scaleFactor: Double = 1.0): ManipulativeScene =
        scene.copy(items = scene.items.map {
            if (it.id == id && !it.locked) it.copy(rotationDegrees = normalizeAngle(it.rotationDegrees + rotationDelta), scale = (it.scale * scaleFactor).coerceIn(.25, 4.0)) else it
        }, revision = scene.revision + 1)

    fun setLocked(scene: ManipulativeScene, id: String, locked: Boolean) =
        scene.copy(items = scene.items.map { if (it.id == id) it.copy(locked = locked) else it }, revision = scene.revision + 1)

    fun annotate(scene: ManipulativeScene, id: String, label: String, annotation: String) =
        scene.copy(items = scene.items.map { if (it.id == id) it.copy(label = label.take(32), annotation = annotation.take(160)) else it }, revision = scene.revision + 1)

    fun configure(
        scene: ManipulativeScene,
        id: String,
        value: Double? = null,
        numerator: Int? = null,
        denominator: Int? = null,
        side: String? = null,
        width: Double? = null,
        height: Double? = null,
    ): ManipulativeScene = scene.copy(items = scene.items.map { item ->
        if (item.id != id || item.locked) item else item.copy(
            value = value ?: item.value,
            numerator = numerator ?: item.numerator,
            denominator = (denominator ?: item.denominator).coerceAtLeast(1),
            side = side ?: item.side,
            width = (width ?: item.width).coerceAtLeast(.1),
            height = (height ?: item.height).coerceAtLeast(.1),
        )
    }, revision = scene.revision + 1)

    fun remove(scene: ManipulativeScene, id: String) =
        scene.copy(items = scene.items.filterNot { it.id == id || it.groupId != null && it.groupId == scene.items.firstOrNull { selected -> selected.id == id }?.groupId }, revision = scene.revision + 1)

    fun links(scene: ManipulativeScene): List<FormalMathLink> = buildList {
        val xCoefficient = scene.items.filter { it.kind == ManipulativeKind.AlgebraX }.sumOf { it.value }
        val constant = scene.items.filter { it.kind in setOf(ManipulativeKind.AlgebraUnit, ManipulativeKind.IntegerChip) }.sumOf { it.value }
        if (abs(xCoefficient) > 1e-12 || abs(constant) > 1e-12) {
            val expression = term(xCoefficient, "x") + signed(constant)
            add(FormalMathLink(FormalMathDestination.Equation, expression + " = 0", "Algebra tiles become coefficients and constants."))
            add(FormalMathLink(FormalMathDestination.Graph, expression, "Plot the tile expression as a function of x."))
        }
        val fractions = scene.items.filter { it.kind == ManipulativeKind.FractionBar }
        if (fractions.isNotEmpty()) {
            val denominator = fractions.map { it.denominator }.reduce(::lcm)
            val numerator = fractions.sumOf { it.numerator * (denominator / it.denominator) }
            val divisor = gcd(abs(numerator), denominator)
            add(FormalMathLink(FormalMathDestination.Notebook, (numerator / divisor).toString() + "/" + (denominator / divisor), "Fraction bars combine into an exact rational value."))
        }
        val interval = scene.items.firstOrNull { it.kind == ManipulativeKind.NumberLineInterval }
        if (interval != null) add(FormalMathLink(FormalMathDestination.Equation, interval.annotation.ifBlank { "x ∈ [" + format(interval.position.x) + ", " + format(interval.position.x + interval.width) + "]" }, "Number-line selections become interval notation."))
        val weights = scene.items.filter { it.kind == ManipulativeKind.BalanceWeight }
        if (weights.isNotEmpty()) {
            val left = weights.filter { it.side.equals("left", true) }.sumOf { it.value }
            val right = weights.filter { it.side.equals("right", true) }.sumOf { it.value }
            add(FormalMathLink(FormalMathDestination.Equation, format(left) + " = " + format(right), "Both scale pans become the two sides of an equation."))
        }
        val tiles = scene.items.filter { it.kind == ManipulativeKind.GeometricTile || it.kind == ManipulativeKind.PatternBlock }
        if (tiles.isNotEmpty()) {
            val area = tiles.sumOf { it.width * it.height * it.scale * it.scale }
            val perimeter = tiles.sumOf { 2 * (it.width + it.height) * it.scale }
            add(FormalMathLink(FormalMathDestination.Notebook, "area = " + format(area) + "; perimeter = " + format(perimeter), "Geometric tile dimensions generate measurement formulas."))
        }
    }

    fun serialize(scene: ManipulativeScene): String = buildString {
        append("{\"schemaVersion\":1,\"id\":\"").append(scene.id.escape()).append("\",\"title\":\"").append(scene.title.escape())
        append("\",\"snapSize\":").append(scene.snapSize).append(",\"revision\":").append(scene.revision).append(",\"items\":[")
        append(scene.items.joinToString(",") { item ->
            "{\"id\":\"" + item.id.escape() + "\",\"kind\":\"" + item.kind.name + "\",\"x\":" + item.position.x + ",\"y\":" + item.position.y +
                ",\"width\":" + item.width + ",\"height\":" + item.height + ",\"rotation\":" + item.rotationDegrees + ",\"scale\":" + item.scale +
                ",\"group\":" + (item.groupId?.let { "\"" + it.escape() + "\"" } ?: "null") + ",\"locked\":" + item.locked +
                ",\"label\":\"" + item.label.escape() + "\",\"annotation\":\"" + item.annotation.escape() + "\",\"value\":" + item.value +
                ",\"numerator\":" + item.numerator + ",\"denominator\":" + item.denominator + ",\"side\":\"" + item.side.escape() + "\"}"
        })
        append("]}")
    }

    private fun snap(point: Vec2, size: Double) = if (size <= 0) point else Vec2(round(point.x / size) * size, round(point.y / size) * size)
    private fun normalizeAngle(value: Double) = ((value % 360) + 360) % 360
    private fun term(value: Double, variable: String) = when { abs(value) < 1e-12 -> "0"; abs(value - 1) < 1e-12 -> variable; abs(value + 1) < 1e-12 -> "-" + variable; else -> format(value) + variable }
    private fun signed(value: Double) = when { value > 1e-12 -> " + " + format(value); value < -1e-12 -> " - " + format(abs(value)); else -> "" }
    private fun gcd(a0: Int, b0: Int): Int { var a = a0; var b = b0; while (b != 0) { val t = a % b; a = b; b = t }; return a.coerceAtLeast(1) }
    private fun lcm(a: Int, b: Int) = abs(a / gcd(a, b) * b)
    private fun format(value: Double) = if (abs(value - value.toLong()) < 1e-9) value.toLong().toString() else String.format(java.util.Locale.US, "%.6f", value).trimEnd('0').trimEnd('.')
}

private fun String.escape() = replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
