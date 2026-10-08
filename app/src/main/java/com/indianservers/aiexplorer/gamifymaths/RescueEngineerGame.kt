package com.indianservers.aiexplorer.gamifymaths

internal data class RescueBridgeAssessment(
    val span: Int,
    val exactSpan: Boolean,
    val connected: Boolean,
    val strong: Boolean,
) {
    val success: Boolean get() = exactSpan && connected && strong
}

internal fun assessRescueBridge(targetSpan: Int, beams: List<Int>, joints: Int, minimumSegments: Int): RescueBridgeAssessment {
    val span = beams.sum()
    val connected = joints >= (beams.size - 1).coerceAtLeast(0)
    return RescueBridgeAssessment(
        span = span,
        exactSpan = span == targetSpan,
        connected = connected,
        strong = beams.size >= minimumSegments && connected,
    )
}
