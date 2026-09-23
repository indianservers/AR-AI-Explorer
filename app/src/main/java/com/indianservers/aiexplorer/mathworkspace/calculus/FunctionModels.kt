package com.indianservers.aiexplorer.mathworkspace.calculus

enum class CalculusMode(val title: String) { Function("Function"), Derivative("Derivative"), Integral("Integral"), Limits("Limits") }
data class FunctionViewport(val centerX: Double = 0.0, val centerY: Double = 0.0, val scaleX: Double = 1.0, val scaleY: Double = 1.0)
data class LimitEstimate(val left: Double, val right: Double, val value: Double?, val converges: Boolean)
