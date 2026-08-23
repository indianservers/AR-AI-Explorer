package com.indianservers.aiexplorer.ar3dgraph.rendering

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityManager
import com.indianservers.aiexplorer.ar3dgraph.ar.ARCoreSessionManager
import com.indianservers.aiexplorer.ar3dgraph.gesture.ARGestureAction
import com.indianservers.aiexplorer.ar3dgraph.gesture.ARGestureEvent
import com.indianservers.aiexplorer.ar3dgraph.gesture.ARGestureOutcome
import com.indianservers.aiexplorer.ar3dgraph.gesture.ARGesturePointer
import com.indianservers.aiexplorer.ar3dgraph.gesture.ARGraphGestureController
import com.indianservers.aiexplorer.ar3dgraph.gesture.ARGraphTransformState
import com.indianservers.aiexplorer.ar3dgraph.gesture.ARGestureAccessibilityPolicy
import com.indianservers.aiexplorer.ar3dgraph.integration.ARGraphRenderData

class ARGraphCameraView(
    context: Context,
    sessionManager: ARCoreSessionManager,
    listener: ARGraphRenderListener,
    initialTransform: ARGraphTransformState = ARGraphTransformState(),
    private val onTransformChanged: (ARGraphTransformState) -> Unit = {},
) : GLSurfaceView(context) {
    @Volatile private var disposed = false
    private val gestureController = ARGraphGestureController(
        ViewConfiguration.get(context).scaledTouchSlop.toFloat(),
        initialTransform,
    )
    private val accessibilityManager = context.getSystemService(AccessibilityManager::class.java)
    val graphRenderer = ARGraphRenderer(
        sessionProvider = sessionManager::currentSession,
        displayRotationProvider = { display?.rotation ?: 0 },
        listener = listener,
    )

    init {
        setEGLContextClientVersion(3)
        preserveEGLContextOnPause = true
        setRenderer(graphRenderer)
        renderMode = RENDERMODE_CONTINUOUSLY
        contentDescription = "AR camera viewport. Tap to place, drag to rotate, and pinch to resize the 3D graph."
        isClickable = true
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        graphRenderer.submitTransform(gestureController.transform)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!ARGestureAccessibilityPolicy.shouldHandleViewportGesture(
                accessibilityManager?.isTouchExplorationEnabled == true,
            )
        ) return false
        val action = when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> ARGestureAction.Down
            MotionEvent.ACTION_POINTER_DOWN -> ARGestureAction.PointerDown
            MotionEvent.ACTION_MOVE -> ARGestureAction.Move
            MotionEvent.ACTION_POINTER_UP -> ARGestureAction.PointerUp
            MotionEvent.ACTION_UP -> ARGestureAction.Up
            MotionEvent.ACTION_CANCEL -> ARGestureAction.Cancel
            else -> return true
        }
        val pointers = (0 until event.pointerCount).map { index ->
            ARGesturePointer(event.getPointerId(index), event.getX(index), event.getY(index))
        }
        val inside = pointers.all { it.x in 0f..width.toFloat() && it.y in 0f..height.toFloat() }
        when (val outcome = gestureController.onEvent(
            ARGestureEvent(action, pointers, event.eventTime, inside),
            graphRenderer.gestureAvailability(),
        )) {
            is ARGestureOutcome.TransformChanged -> {
                graphRenderer.submitTransform(outcome.transform)
                onTransformChanged(outcome.transform)
            }
            is ARGestureOutcome.RepositionTap -> {
                graphRenderer.requestPlacement(outcome.x, outcome.y)
                performClick()
            }
            ARGestureOutcome.None -> Unit
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    fun submitGraph(data: ARGraphRenderData) = graphRenderer.submitGraph(data)
    fun resetPlacement() {
        gestureController.cancelGesture()
        graphRenderer.resetPlacement()
    }
    fun resetView() {
        val transform = gestureController.resetView()
        graphRenderer.submitTransform(transform)
        onTransformChanged(transform)
    }
    fun clearGraph() {
        val transform = gestureController.clearGraph()
        graphRenderer.submitTransform(transform)
        onTransformChanged(transform)
        graphRenderer.clearGraph()
    }
    fun dispose() {
        if (disposed) return
        disposed = true
        gestureController.dispose()
        queueEvent(graphRenderer::closeOnGlThread)
        // Anchor and CPU-side ownership must be released even if this view was already paused and
        // its GL queue never receives another frame. GL deletion remains queued on the EGL thread.
        graphRenderer.close()
        requestRender()
    }
}
