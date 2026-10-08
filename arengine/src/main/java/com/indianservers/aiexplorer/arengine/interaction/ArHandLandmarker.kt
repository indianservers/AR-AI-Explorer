package com.indianservers.aiexplorer.arengine.interaction

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.indianservers.aiexplorer.arengine.contract.ArCameraImage
import com.indianservers.aiexplorer.arengine.contract.ArVector2
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.max

/** Bounded, pooled on-device inference shared by copied CameraX and ARCore images. */
class ArHandLandmarker(context: Context, private val onFrame: (ArHandFrame) -> Unit, private val onStatus: (String) -> Unit) : AutoCloseable {
    private val identities = ArHandIdentityTracker()
    private val executor = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private val closed = AtomicBoolean(false)
    private val busy = AtomicBoolean(false)
    private var detector: HandLandmarker? = null
    private var reusableRaw: Bitmap? = null
    private var reusableUpright: Bitmap? = null
    private var reusablePixels = IntArray(0)
    private var rawImage: com.google.mediapipe.framework.image.MPImage? = null
    private var uprightImage: com.google.mediapipe.framework.image.MPImage? = null
    private val rotationMatrix = Matrix()
    private val rotationPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    @Volatile private var ready = false
    val canAcceptFrame: Boolean get() = ready && !closed.get() && !busy.get()

    init {
        executor.execute {
            runCatching {
                detector = HandLandmarker.createFromOptions(context.applicationContext,
                    HandLandmarker.HandLandmarkerOptions.builder()
                        .setBaseOptions(BaseOptions.builder().setModelAssetPath("hand_landmarker.task").build())
                        .setRunningMode(RunningMode.VIDEO).setNumHands(2)
                        .setMinHandDetectionConfidence(.65f).setMinHandPresenceConfidence(.65f).setMinTrackingConfidence(.65f).build())
                ready = true
                status("Show your hand to the rear camera")
            }.onFailure { status("Hand tracking unavailable: ${it.message ?: "model could not start"}") }
        }
    }

    fun submit(image: ArCameraImage) {
        if (!canAcceptFrame || !busy.compareAndSet(false, true)) return
        try { executor.execute {
            var raw: Bitmap? = null
            var upright: Bitmap? = null
            try {
                if (closed.get()) return@execute
                raw = bitmap(image)
                val x = image.viewCorners[1]; val origin = image.viewCorners[0]
                val rotation = if (abs(x.x-origin.x) > abs(x.y-origin.y)) {
                    if (x.x >= origin.x) 0 else 180
                } else if (x.y >= origin.y) 90 else 270
                upright = if (rotation == 0) raw else {
                    val w = if (rotation == 90 || rotation == 270) raw.height else raw.width
                    val h = if (rotation == 90 || rotation == 270) raw.width else raw.height
                    if (reusableUpright?.width != w || reusableUpright?.height != h) {
                        uprightImage?.close(); uprightImage=null
                        reusableUpright?.takeUnless { it.isRecycled }?.recycle(); reusableUpright = Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)
                    }
                    rotationMatrix.reset(); rotationMatrix.postRotate(rotation.toFloat())
                    rotationMatrix.postTranslate(if (rotation == 90 || rotation == 180) w.toFloat() else 0f, if (rotation == 180 || rotation == 270) h.toFloat() else 0f)
                    reusableUpright!!.also { Canvas(it).drawBitmap(raw,rotationMatrix,rotationPaint) }
                }
                // BitmapImageBuilder owns its bitmap. Reuse the wrapper too and close it only
                // when its buffer is resized or the detector is disposed.
                val mpImage = if(rotation==0) rawImage ?: BitmapImageBuilder(upright).build().also { rawImage=it }
                    else uprightImage ?: BitmapImageBuilder(upright).build().also { uprightImage=it }
                val result = detector?.detectForVideo(mpImage, image.timestampMillis)
                val hands = result?.landmarks()?.mapIndexed { i, landmarks ->
                    val category = result.handedness().getOrNull(i)?.firstOrNull()
                    val side = category?.categoryName() ?: "hand-$i"
                    ArTrackedHand(side, landmarks.map { point ->
                        val original = when (rotation) {
                            90 -> ArVector2(point.y(), 1f-point.x())
                            180 -> ArVector2(1f-point.x(), 1f-point.y())
                            270 -> ArVector2(1f-point.y(), point.x())
                            else -> ArVector2(point.x(), point.y())
                        }
                        image.imagePointToView(original)
                    }, confidence = category?.score() ?: 0f, handedness = side,
                        worldLandmarks = result.worldLandmarks().getOrNull(i)?.map { p ->
                            com.indianservers.aiexplorer.arengine.contract.ArVector3(p.x().toDouble(), p.y().toDouble(), p.z().toDouble())
                        }.orEmpty())
                }.orEmpty()
                val tracked = identities.assign(hands, image.timestampMillis)
                main.post { if (!closed.get()) onFrame(ArHandFrame(image.timestampMillis, tracked)) }
            } catch (error: Exception) {
                status("Hand tracking paused: ${error.message ?: "could not read frame"}")
            } finally {
                busy.set(false)
            }
        } } catch (_: java.util.concurrent.RejectedExecutionException) { busy.set(false) }
    }

    private fun bitmap(image: ArCameraImage): Bitmap {
        require(image.planes.size == 3 && image.width > 0 && image.height > 0)
        val divisor = max(1, (max(image.width, image.height) + 479) / 480)
        val width = image.width / divisor; val height = image.height / divisor
        if (reusablePixels.size != width*height) reusablePixels = IntArray(width*height)
        val pixels = reusablePixels
        fun sample(plane: Int, x: Int, y: Int): Int {
            val p = image.planes[plane]
            return p.bytes[y*p.rowStride+x*p.pixelStride].toInt() and 255
        }
        for (y in 0 until height) for (x in 0 until width) {
            val sx = x*divisor; val sy = y*divisor
            val luminance = (sample(0, sx, sy)-16).coerceAtLeast(0)
            val u = sample(1, sx/2, sy/2)-128; val v = sample(2, sx/2, sy/2)-128
            val red = ((298*luminance+409*v+128) shr 8).coerceIn(0,255)
            val green = ((298*luminance-100*u-208*v+128) shr 8).coerceIn(0,255)
            val blue = ((298*luminance+516*u+128) shr 8).coerceIn(0,255)
            pixels[y*width+x] = (255 shl 24) or (red shl 16) or (green shl 8) or blue
        }
        if (reusableRaw?.width != width || reusableRaw?.height != height) {
            rawImage?.close(); rawImage=null
            reusableRaw?.takeUnless { it.isRecycled }?.recycle(); reusableRaw = Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888)
        }
        return reusableRaw!!.also { it.setPixels(pixels,0,width,0,0,width,height) }
    }
    private fun status(value: String) = main.post { if (!closed.get()) onStatus(value) }
    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        ready = false
        executor.execute { detector?.close(); detector = null; rawImage?.close(); uprightImage?.close(); rawImage=null; uprightImage=null; reusableRaw?.takeUnless { it.isRecycled }?.recycle(); reusableUpright?.takeUnless { it.isRecycled }?.recycle(); reusableRaw=null; reusableUpright=null }
        executor.shutdown()
    }
}
