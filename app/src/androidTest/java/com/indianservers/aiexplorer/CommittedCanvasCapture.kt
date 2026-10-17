package com.indianservers.aiexplorer

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlin.math.roundToInt

/** Capture a committed window buffer without reentering Compose measurement or a fixed 2-second callback. */
internal fun captureCommittedCanvas(compose:ComposeTestRule,matcher:SemanticsMatcher,output:File):Bitmap {
        compose.waitForIdle()
        val bounds=compose.onNode(matcher,useUnmergedTree=true).fetchSemanticsNode().boundsInWindow
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val committed=java.util.concurrent.CountDownLatch(1);var window:android.view.Window?=null
        instrumentation.runOnMainSync{
            val activity=androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED).first{it is GraphVerificationActivity}
            window=activity.window
            val decor=activity.window.decorView
            check(decor.isHardwareAccelerated){"Canvas capture requires the hardware-rendered test window."}
            decor.viewTreeObserver.registerFrameCommitCallback{committed.countDown()}
            decor.invalidate()
        }
        check(committed.await(15,java.util.concurrent.TimeUnit.SECONDS)){"The canvas did not commit a rendered frame within 15 seconds."}
        val rect=android.graphics.Rect(bounds.left.roundToInt(),bounds.top.roundToInt(),bounds.right.roundToInt(),bounds.bottom.roundToInt())
        check(rect.width()>0&&rect.height()>0)
        val bitmap=Bitmap.createBitmap(rect.width(),rect.height(),Bitmap.Config.ARGB_8888)
        val copied=java.util.concurrent.CountDownLatch(1);var result=-1
        val callbacks=android.os.HandlerThread("RuhiCanvasCapture").apply{start()}
        try {
            android.view.PixelCopy.request(requireNotNull(window),rect,bitmap,{status->result=status;copied.countDown()},android.os.Handler(callbacks.looper))
            check(copied.await(15,java.util.concurrent.TimeUnit.SECONDS)){"Canvas PixelCopy callback exceeded 15 seconds."}
            check(result==android.view.PixelCopy.SUCCESS){"Canvas PixelCopy failed with code $result."}
        }finally{callbacks.quitSafely()}
        output.outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
        return bitmap
}
