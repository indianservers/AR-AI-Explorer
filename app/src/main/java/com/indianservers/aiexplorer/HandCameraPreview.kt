package com.indianservers.aiexplorer

import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.indianservers.aiexplorer.arengine.contract.*
import java.util.concurrent.Executors

/** CameraX owns only the standalone camera use cases. ARCore retains its own frame adapter. */
@Composable
internal fun HandCameraPreview(modifier:Modifier,enabled:Boolean,wantsFrame:()->Boolean,onFrame:(ArCameraImage)->Unit,onStatus:(String)->Unit) {
    val context=LocalContext.current
    val owner=context as? ComponentActivity
    val wants by rememberUpdatedState(wantsFrame); val receive by rememberUpdatedState(onFrame); val status by rememberUpdatedState(onStatus)
    var view by remember { mutableStateOf<PreviewView?>(null) }
    DisposableEffect(enabled,view,owner) {
        val executor=Executors.newSingleThreadExecutor(); val disposed=java.util.concurrent.atomic.AtomicBoolean(false)
        val preview=Preview.Builder().build()
        @Suppress("DEPRECATION") val analysis=ImageAnalysis.Builder().setTargetResolution(android.util.Size(480,360)).setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
        var provider:ProcessCameraProvider?=null
        val copies=Array(3) { ByteArray(0) }
        analysis.setAnalyzer(executor) { image ->
            try {
                if(!disposed.get() && wants()) {
                    val rotation=image.imageInfo.rotationDegrees
                    val uprightWidth=if(rotation%180==0) image.width else image.height
                    val uprightHeight=if(rotation%180==0) image.height else image.width
                    val surface=view
                    val vw=(surface?.width ?: uprightWidth).coerceAtLeast(1); val vh=(surface?.height ?: uprightHeight).coerceAtLeast(1)
                    val scale=minOf(vw.toDouble()/uprightWidth,vh.toDouble()/uprightHeight)
                    val width=uprightWidth*scale/vw; val height=uprightHeight*scale/vh; val left=(1-width)/2; val top=(1-height)/2
                    fun point(x:Double,y:Double)=ArVector2((left+x*width).toFloat(),(top+y*height).toFloat())
                    val corners=when(rotation) {
                        90 -> listOf(point(1.0,0.0),point(1.0,1.0),point(0.0,0.0))
                        180 -> listOf(point(1.0,1.0),point(0.0,1.0),point(1.0,0.0))
                        270 -> listOf(point(0.0,1.0),point(0.0,0.0),point(1.0,1.0))
                        else -> listOf(point(0.0,0.0),point(1.0,0.0),point(0.0,1.0))
                    }
                    val planes=image.planes.mapIndexed { i,plane ->
                        val buffer=plane.buffer.duplicate(); val length=buffer.remaining()
                        if(copies[i].size!=length) copies[i]=ByteArray(length)
                        buffer.get(copies[i]); ArCameraImagePlane(copies[i],plane.rowStride,plane.pixelStride)
                    }
                    receive(ArCameraImage(SystemClock.uptimeMillis(),image.width,image.height,planes,corners))
                }
            } finally { image.close() }
        }
        val future=ProcessCameraProvider.getInstance(context)
        future.addListener({
            if(!disposed.get() && enabled && owner!=null && view!=null) {
                runCatching {
                    provider=future.get(); preview.setSurfaceProvider(view!!.surfaceProvider)
                    provider!!.bindToLifecycle(owner,CameraSelector.DEFAULT_BACK_CAMERA,preview,analysis)
                    status("Reach toward an object to interact")
                }.onFailure { status("Camera unavailable: ${it.message}") }
            }
        },ContextCompat.getMainExecutor(context))
        onDispose { disposed.set(true); analysis.clearAnalyzer(); provider?.unbind(preview,analysis); executor.shutdown() }
    }
    AndroidView(modifier=modifier.semantics { contentDescription="Camera preview without AR" },factory={ PreviewView(it).apply { implementationMode=PreviewView.ImplementationMode.COMPATIBLE; scaleType=PreviewView.ScaleType.FIT_CENTER }.also { view=it } })
}
