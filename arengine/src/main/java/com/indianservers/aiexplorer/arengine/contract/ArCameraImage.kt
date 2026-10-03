package com.indianservers.aiexplorer.arengine.contract

/** CPU-owned YUV copy. Native camera Images are closed before this crosses the renderer boundary. */
data class ArCameraImagePlane(val bytes: ByteArray, val rowStride: Int, val pixelStride: Int)
data class ArCameraImage(
    val timestampMillis: Long,
    val width: Int,
    val height: Int,
    val planes: List<ArCameraImagePlane>,
    /** Image-normalized origin, X endpoint and Y endpoint transformed into view-normalized coordinates. */
    val viewCorners: List<ArVector2>,
) {
    fun imagePointToView(point: ArVector2): ArVector2 {
        val o = viewCorners[0]; val x = viewCorners[1]; val y = viewCorners[2]
        return ArVector2(o.x + point.x*(x.x-o.x) + point.y*(y.x-o.x), o.y + point.x*(x.y-o.y) + point.y*(y.y-o.y))
    }
}
