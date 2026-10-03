package com.indianservers.aiexplorer

import android.graphics.BitmapFactory
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import com.indianservers.aiexplorer.arengine.contract.*
import com.indianservers.aiexplorer.arengine.interaction.*
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** Exercises the real bundled model and YUV bridge; does not simulate ARCore tracking. */
class ArHandInferenceDeviceTest {
    @Test fun twoHandsRecognizedFromCpuCameraFormat() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap = instrumentation.context.assets.open("ar-right-hands.jpg").use { BitmapFactory.decodeStream(it) }
        val width = bitmap.width; val height = bitmap.height
        val y = ByteArray(width * height)
        val cw = (width + 1) / 2; val ch = (height + 1) / 2
        val u = ByteArray(cw * ch); val v = ByteArray(cw * ch)
        for (row in 0 until height) for (col in 0 until width) {
            val color = bitmap.getPixel(col, row)
            val r = color shr 16 and 255; val g = color shr 8 and 255; val b = color and 255
            y[row * width + col] = (((66*r + 129*g + 25*b + 128) shr 8) + 16).coerceIn(0,255).toByte()
            if (row % 2 == 0 && col % 2 == 0) {
                u[row/2*cw+col/2] = (((-38*r-74*g+112*b+128) shr 8)+128).coerceIn(0,255).toByte()
                v[row/2*cw+col/2] = (((112*r-94*g-18*b+128) shr 8)+128).coerceIn(0,255).toByte()
            }
        }
        bitmap.recycle()
        val ready = CountDownLatch(1); val done = CountDownLatch(1)
        val status = AtomicReference(""); val result = AtomicReference<ArHandFrame>()
        val detector = ArHandLandmarker(instrumentation.targetContext, { result.set(it); done.countDown() }, {
            status.set(it); ready.countDown()
        })
        try {
            assertTrue("Model did not initialize", ready.await(30, TimeUnit.SECONDS))
            assertTrue(status.get(), detector.canAcceptFrame)
            detector.submit(ArCameraImage(SystemClock.uptimeMillis(), width, height,
                listOf(ArCameraImagePlane(y,width,1), ArCameraImagePlane(u,cw,1), ArCameraImagePlane(v,cw,1)),
                listOf(ArVector2(0f,0f), ArVector2(1f,0f), ArVector2(0f,1f))))
            assertTrue(status.get(), done.await(30, TimeUnit.SECONDS))
            assertEquals(2, result.get().hands.size)
            result.get().hands.forEach { assertEquals(21, it.landmarks.size) }
        } finally { detector.close() }
    }
}
