package com.indianservers.aiexplorer.mathworkspace.tiles

object AlgebraTileEngine {
    fun expression(tiles: List<AlgebraTile>): String {
        val squares = tiles.count { it.kind == TileKind.XSquared }
        val x = tiles.sumOf { if (it.kind.family == TileFamily.Linear) it.kind.sign else 0 }
        val one = tiles.sumOf { if (it.kind.family == TileFamily.Unit) it.kind.sign else 0 }
        val terms = listOf(squares to "x²", x to "x", one to "1").filter { it.first != 0 }
        return terms.mapIndexed { index, (coefficient, symbol) ->
            val sign = if (coefficient < 0) "−" else "+"
            val magnitude = kotlin.math.abs(coefficient)
            val value = when {
                symbol == "1" -> magnitude.toString()
                magnitude == 1 -> symbol
                else -> "$magnitude$symbol"
            }
            if (index == 0) (if (coefficient < 0) "−" else "") + value else "$sign $value"
        }.joinToString(" ").ifBlank { "0" }
    }

    fun zeroPairs(tiles: List<AlgebraTile>): List<TileZeroPair> = TileFamily.entries
        .filter { it != TileFamily.Square }
        .flatMap { family ->
            val positive = tiles.filter { it.kind.family == family && it.kind.sign > 0 }
            val negative = tiles.filter { it.kind.family == family && it.kind.sign < 0 }
            (0 until minOf(positive.size, negative.size)).map { TileZeroPair(positive[it].id, negative[it].id, family) }
        }

    /** Parses signed integer-coefficient polynomials of degree at most two into unit tiles. */
    fun parse(expression: String): Result<TileExpression> = runCatching {
        val source = expression.replace(Regex("\\s+"), "")
        require(source.isNotBlank()) { "Enter a polynomial such as x^2 + 3x + 2." }
        val tiles = mutableListOf<TileKind>()
        var index = 0
        while (index < source.length) {
            var sign = 1
            if (source[index] == '+' || source[index] == '-') {
                sign = if (source[index] == '-') -1 else 1
                index++
            } else require(index == 0) { "Separate each term with + or −." }
            val numberStart = index
            while (index < source.length && source[index].isDigit()) index++
            val coefficientText = source.substring(numberStart, index)
            val hasX = index < source.length && source[index].lowercaseChar() == 'x'
            val square: Boolean
            val family: TileFamily
            val count: Int
            if (hasX) {
                index++
                square = when {
                    source.startsWith("^2", index) -> { index += 2; true }
                    source.startsWith("²", index) -> { index++; true }
                    else -> false
                }
                family = if (square) TileFamily.Square else TileFamily.Linear
                count = coefficientText.toIntOrNull() ?: 1
            } else {
                require(coefficientText.isNotEmpty()) { "Expected x, x², or an integer constant." }
                family = TileFamily.Unit
                square = false
                count = coefficientText.toInt()
            }
            require(count > 0) { "Use a positive coefficient and put its sign before the term." }
            if (square && sign < 0) error("Negative x² tiles are not supported yet.")
            val kind = when (family) {
                TileFamily.Square -> TileKind.XSquared
                TileFamily.Linear -> if (sign > 0) TileKind.PositiveX else TileKind.NegativeX
                TileFamily.Unit -> if (sign > 0) TileKind.PositiveOne else TileKind.NegativeOne
            }
            repeat(count) { tiles += kind }
        }
        require(tiles.isNotEmpty()) { "The expression produced no tiles." }
        TileExpression(expression(tiles.mapIndexed { i, kind -> AlgebraTile("parsed-$i", kind, 0f, 0f) }), tiles)
    }

}
