package com.indianservers.aiexplorer.core

data class TrigViewport(val zoom: Float = 1f, val pan: Vec2 = Vec2(0.0, 0.0))

object TrigViewportEngine {
    const val minimumZoom = .25f
    const val maximumZoom = 6f

    fun screenToContent(viewport: TrigViewport, screen: Vec2, width: Double, height: Double): Vec2 {
        require(width > 0 && height > 0)
        val center = Vec2(width / 2.0, height / 2.0)
        return center + (screen - center - viewport.pan) * (1.0 / viewport.zoom)
    }

    fun contentToScreen(viewport: TrigViewport, content: Vec2, width: Double, height: Double): Vec2 {
        require(width > 0 && height > 0)
        val center = Vec2(width / 2.0, height / 2.0)
        return center + (content - center) * viewport.zoom.toDouble() + viewport.pan
    }

    fun transform(
        viewport: TrigViewport,
        width: Double,
        height: Double,
        centroid: Vec2,
        panDelta: Vec2,
        zoomFactor: Float,
    ): TrigViewport {
        val previousCentroid = centroid - panDelta
        val contentUnderFingers = screenToContent(viewport, previousCentroid, width, height)
        val nextZoom = (viewport.zoom * zoomFactor.takeIf { it.isFinite() && it > 0f }.orEmptyZoom()).coerceIn(minimumZoom, maximumZoom)
        val center = Vec2(width / 2.0, height / 2.0)
        val nextPan = centroid - center - (contentUnderFingers - center) * nextZoom.toDouble()
        return TrigViewport(nextZoom, nextPan)
    }

    private fun Float?.orEmptyZoom() = this ?: 1f
}
