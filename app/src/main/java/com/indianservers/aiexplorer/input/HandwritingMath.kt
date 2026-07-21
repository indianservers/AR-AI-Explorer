package com.indianservers.aiexplorer.input

import kotlin.math.abs

data class InkPoint(val x: Float, val y: Float)
data class HandwritingRecognition(val primary: String, val alternatives: List<String>, val confidence: Double, val explanation: String)

/** Small, deterministic, offline recognizer for one handwritten maths symbol at a time. */
object LocalHandwritingMathRecognizer {
    fun recognize(strokes: List<List<InkPoint>>): HandwritingRecognition {
        val valid = strokes.filter { it.size >= 2 }
        if (valid.isEmpty()) return HandwritingRecognition("", emptyList(), 0.0, "Draw one symbol.")
        val points = valid.flatten()
        val width = (points.maxOf { it.x } - points.minOf { it.x }).coerceAtLeast(1f)
        val height = (points.maxOf { it.y } - points.minOf { it.y }).coerceAtLeast(1f)
        val aspect = width / height
        val start = points.first(); val end = points.last()
        val closure = distance(start, end) / maxOf(width, height)
        val result = when {
            valid.size == 2 && isHorizontal(valid[0]) && isVertical(valid[1]) || valid.size == 2 && isVertical(valid[0]) && isHorizontal(valid[1]) -> HandwritingRecognition("+", listOf("x", "t"), .92, "Two crossing horizontal and vertical strokes.")
            valid.size == 2 && valid.all(::isDiagonal) -> HandwritingRecognition("x", listOf("+", "×"), .86, "Two diagonal strokes.")
            closure < .32 && aspect in .55f..1.55f -> HandwritingRecognition("0", listOf("O", "6"), .84, "Closed loop.")
            aspect > 2.6f -> HandwritingRecognition("-", listOf("_", "/"), .91, "Horizontal stroke.")
            aspect < .38f -> HandwritingRecognition("1", listOf("|", "l"), .88, "Vertical stroke.")
            valid.size == 1 && hasValley(valid[0]) -> HandwritingRecognition("v", listOf("√", "u"), .72, "Down-up valley stroke.")
            abs(end.y - start.y) > abs(end.x - start.x) -> HandwritingRecognition("1", listOf("7", "/"), .55, "Mostly vertical stroke.")
            else -> HandwritingRecognition("x", listOf("-", "/"), .45, "Ambiguous symbol; select a correction if needed.")
        }
        return result
    }

    private fun distance(a: InkPoint, b: InkPoint) = kotlin.math.hypot((a.x - b.x).toDouble(), (a.y - b.y).toDouble()).toFloat()
    private fun isHorizontal(points: List<InkPoint>): Boolean { val a=points.first();val b=points.last();return abs(b.x-a.x)>abs(b.y-a.y)*2 }
    private fun isVertical(points: List<InkPoint>): Boolean { val a=points.first();val b=points.last();return abs(b.y-a.y)>abs(b.x-a.x)*2 }
    private fun isDiagonal(points: List<InkPoint>): Boolean { val a=points.first();val b=points.last();val dx=abs(b.x-a.x);val dy=abs(b.y-a.y);return dx>8&&dy>8&&dx/dy in .35f..2.8f }
    private fun hasValley(points: List<InkPoint>): Boolean { val bottom=points.indices.maxByOrNull{points[it].y}?:return false;return bottom in 1 until points.lastIndex }
}
