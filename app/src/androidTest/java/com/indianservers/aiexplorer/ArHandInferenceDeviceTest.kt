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
    @Test fun twoHandsRecognizedFromCpuCameraFormat() = runInference(2)
    @Test fun oneHandRecognizedAndOffRejectsFrames() = runInference(1)
    private fun runInference(expectedHands: Int) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val original = instrumentation.context.assets.open("ar-right-hands.jpg").use { BitmapFactory.decodeStream(it) }
        val bitmap = if (expectedHands == 1) android.graphics.Bitmap.createBitmap(original,0,0,original.width/2,original.height).also { original.recycle() } else original
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
        val frames=java.util.concurrent.LinkedBlockingQueue<ArHandFrame>()
        val status = AtomicReference(""); val result = AtomicReference<ArHandFrame>()
        val detector = ArHandLandmarker(instrumentation.targetContext, { result.set(it); frames.offer(it); done.countDown() }, {
            status.set(it); ready.countDown()
        })
        try {
            assertTrue("Model did not initialize", ready.await(30, TimeUnit.SECONDS))
            assertTrue(status.get(), detector.canAcceptFrame)
            detector.submit(ArCameraImage(SystemClock.uptimeMillis(), width, height,
                listOf(ArCameraImagePlane(y,width,1), ArCameraImagePlane(u,cw,1), ArCameraImagePlane(v,cw,1)),
                listOf(ArVector2(0f,0f), ArVector2(1f,0f), ArVector2(0f,1f))))
            assertTrue(status.get(), done.await(30, TimeUnit.SECONDS))
            assertEquals(expectedHands, result.get().hands.size)
            assertEquals(expectedHands,result.get().hands.map { it.id }.distinct().size)
            result.get().hands.forEach { assertEquals(21,it.landmarks.size); assertEquals(21,it.worldLandmarks.size); assertTrue(it.confidence >= .65f); assertNotNull(ArGestureRecognizer.palmNormal(it)) }
            val stableIds=result.get().hands.map { it.id }.toSet()
            // Repeated real model inference verifies bitmap reuse and identity continuity.
            frames.clear()
            val durations=mutableListOf<Long>()
            repeat(5) {
                val deadline=SystemClock.uptimeMillis()+5000
                while(!detector.canAcceptFrame && SystemClock.uptimeMillis()<deadline) Thread.sleep(5)
                val start=SystemClock.uptimeMillis()
                detector.submit(ArCameraImage(start,width,height,listOf(ArCameraImagePlane(y,width,1),ArCameraImagePlane(u,cw,1),ArCameraImagePlane(v,cw,1)),listOf(ArVector2(0f,0f),ArVector2(1f,0f),ArVector2(0f,1f))))
                val next=frames.poll(15,TimeUnit.SECONDS);assertNotNull("Repeated inference failed",next);assertEquals(stableIds,next!!.hands.map { it.id }.toSet());durations+=SystemClock.uptimeMillis()-start
                val features=com.indianservers.aiexplorer.handintelligence.features.HandFeatureEngine().process(next);assertEquals(expectedHands,features.size)
            }
            // Same pooled bitmap must read NEW pixels rather than a cached MediaPipe image.
            val blankY=ByteArray(y.size) { 16 };val blankU=ByteArray(u.size) { 128.toByte() };val blankV=ByteArray(v.size) { 128.toByte() }
            while(!detector.canAcceptFrame) Thread.sleep(5)
            detector.submit(ArCameraImage(SystemClock.uptimeMillis(),width,height,listOf(ArCameraImagePlane(blankY,width,1),ArCameraImagePlane(blankU,cw,1),ArCameraImagePlane(blankV,cw,1)),listOf(ArVector2(0f,0f),ArVector2(1f,0f),ArVector2(0f,1f))))
            val blank=frames.poll(15,TimeUnit.SECONDS);assertNotNull(blank);assertEquals("Pooled image must reflect replacement pixels",0,blank!!.hands.size)
            android.util.Log.i("HandInferenceMetrics","hands=$expectedHands repeated model inference milliseconds=$durations")
            detector.close()
            assertFalse("OFF must reject camera frames",detector.canAcceptFrame)
        } finally { detector.close() }
    }
}
