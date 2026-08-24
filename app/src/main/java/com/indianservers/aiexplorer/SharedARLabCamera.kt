package com.indianservers.aiexplorer

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.indianservers.aiexplorer.arengine.arcore.ArCoreRuntime
import com.indianservers.aiexplorer.arengine.contract.ArRuntimeState
import com.indianservers.aiexplorer.arengine.session.ArLabSessionController
import com.indianservers.aiexplorer.spatial.ARCoreCompositorView
import com.indianservers.aiexplorer.spatial.SpatialCompositorScene
import com.indianservers.aiexplorer.spatial.SpatialRenderScene
import com.indianservers.aiexplorer.spatial.SpatialScenePlacement

@Composable
internal fun SharedARLabCamera(
    modifier: Modifier = Modifier,
    labId: String,
    needsDepth: Boolean = true,
    onStateChanged: (live: Boolean, status: String) -> Unit = { _, _ -> },
    onSessionReady: (ArLabSessionController?) -> Unit = {},
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val runtime = remember(activity) { activity?.let(::ArCoreRuntime) }
    val controller = remember(runtime) { runtime?.let(::ArLabSessionController) }
    var compositor by remember { mutableStateOf<ARCoreCompositorView?>(null) }
    var cameraGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var live by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Checking ARCore") }
    val currentLive by rememberUpdatedState(live)
    val currentCompositor by rememberUpdatedState(compositor)
    val scene = remember {
        SpatialCompositorScene(
            scene = SpatialRenderScene(id = "ar-lab-camera", primitives = emptyList(), axesVisible = false),
            placement = SpatialScenePlacement(),
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        cameraGranted = granted
        if (!granted) status = "Camera permission required"
    }

    LaunchedEffect(runtime, cameraGranted) {
        if (runtime == null) {
            status = "AR preview unavailable"
            return@LaunchedEffect
        }
        val availability = runtime.checkAvailability()
        if (!cameraGranted) {
            status = "Camera permission required"
            permissionLauncher.launch(Manifest.permission.CAMERA)
            return@LaunchedEffect
        }
        val activated = controller?.activate(labId, cameraPermissionGranted = true, userRequestedInstall = false)?.runtimeState
        when (activated) {
            is ArRuntimeState.Running -> {
                controller.setDepthNeeded(needsDepth)
                live = true
                status = "AR Active"
                onSessionReady(controller)
            }
            else -> {
                live = false
                status = when (availability) {
                    is ArRuntimeState.Unsupported -> availability.reason
                    else -> "ARCore setup required"
                }
            }
        }
    }

    LaunchedEffect(live, status) {
        onStateChanged(live, status)
        if (!live) onSessionReady(null)
    }

    DisposableEffect(runtime, activity) {
        val observer = object : DefaultLifecycleObserver {
            override fun onPause(owner: LifecycleOwner) {
                currentCompositor?.onPause()
                controller?.pause()
            }

            override fun onResume(owner: LifecycleOwner) {
                if (currentLive) {
                    controller?.resume()
                    currentCompositor?.onResume()
                }
            }
        }
        activity?.lifecycle?.addObserver(observer)
        onDispose {
            activity?.lifecycle?.removeObserver(observer)
            currentCompositor?.releaseRenderer()
            currentCompositor?.onPause()
            controller?.close()
        }
    }

    Box(modifier) {
        if (live && runtime != null) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { viewContext ->
                    ARCoreCompositorView(
                        context = viewContext,
                        runtime = runtime,
                        sceneProvider = { scene },
                        onFrame = { frame ->
                            val sessionState = controller?.onFrame(frame)
                            status = sessionState?.guidance?.title ?: "AR Active"
                        },
                        onError = {
                            live = false
                            status = it
                        },
                    ).also {
                        compositor = it
                        it.onResume()
                    }
                },
            )
        }
    }
}
