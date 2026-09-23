package com.indianservers.aiexplorer.mathworkspace.tiles

enum class TileKind(val label: String, val term: String, val sign: Int, val family: TileFamily) {
    XSquared("x²", "x²", 1, TileFamily.Square),
    PositiveX("+x", "x", 1, TileFamily.Linear),
    NegativeX("−x", "x", -1, TileFamily.Linear),
    PositiveOne("+1", "1", 1, TileFamily.Unit),
    NegativeOne("−1", "1", -1, TileFamily.Unit),
}

enum class TileFamily { Square, Linear, Unit }

data class AlgebraTile(
    val id: String,
    val kind: TileKind,
    val x: Float,
    val y: Float,
    val groupId: String? = null,
)

data class TileZeroPair(val positiveId: String, val negativeId: String, val family: TileFamily)

data class TileExpression(val text: String, val tiles: List<TileKind>)
