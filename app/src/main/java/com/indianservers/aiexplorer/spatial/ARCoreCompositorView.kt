package com.indianservers.aiexplorer.spatial

import android.content.Context
import android.opengl.GLES11Ext
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.Handler
import android.os.Looper
import android.view.Surface
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

data class SpatialCompositorScene(val scene: SpatialRenderScene, val placement: SpatialScenePlacement)

/** Live AR camera compositor and math scene loop. The renderer-neutral scene is also used by non-AR 3D. */
class ARCoreCompositorView(
    context: Context,
    private val controller: ARCoreSessionController,
    sceneProvider: () -> SpatialCompositorScene,
    onFrame: (ARFrameState) -> Unit,
    onError: (String) -> Unit,
) : GLSurfaceView(context) {
    private val compositor = CompositorRenderer(controller, sceneProvider, onFrame, onError)

    init {
        setEGLContextClientVersion(3)
        setPreserveEGLContextOnPause(true)
        setRenderer(compositor)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    private class CompositorRenderer(
        private val controller: ARCoreSessionController,
        private val sceneProvider: () -> SpatialCompositorScene,
        private val onFrame: (ARFrameState) -> Unit,
        private val onError: (String) -> Unit,
    ) : Renderer {
        private val mainHandler = Handler(Looper.getMainLooper())
        private val spatialRenderer = OpenGlEsSpatialRenderer()
        private var cameraProgram = 0
        private var cameraTexture = 0
        private var quadBuffer = 0
        private var uploadedSignature = 0

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            cameraTexture = IntArray(1).also { GLES30.glGenTextures(1, it, 0) }[0]
            GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTexture)
            GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
            GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
            GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
            GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
            controller.setCameraTextureName(cameraTexture)
            cameraProgram = cameraProgram()
            val quad = floatArrayOf(-1f, -1f, 0f, 1f, 1f, -1f, 1f, 1f, -1f, 1f, 0f, 0f, 1f, 1f, 1f, 0f)
            quadBuffer = IntArray(1).also { GLES30.glGenBuffers(1, it, 0) }[0]
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, quadBuffer)
            GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, quad.size * 4, ByteBuffer.allocateDirect(quad.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(quad); position(0) }, GLES30.GL_STATIC_DRAW)
            spatialRenderer.initialize()
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            GLES30.glViewport(0, 0, width, height)
            controller.setDisplayGeometry(Surface.ROTATION_0, width, height)
        }

        override fun onDrawFrame(gl: GL10?) {
            val frame = controller.updateFrame().getOrElse { error -> mainHandler.post { onError(error.message ?: "AR frame failed") }; return }
            drawCamera()
            val current = sceneProvider()
            val plan = SharedGpuSceneCompiler.compile(current.scene)
            val signature = plan.vertices.contentHashCode() * 31 + plan.triangleIndices.contentHashCode() * 17 + plan.lineIndices.contentHashCode()
            if (signature != uploadedSignature) { spatialRenderer.upload(plan); uploadedSignature = signature }
            val model = FloatArray(16).also { Matrix.setIdentityM(it, 0) }
            val pose = current.placement.pose
            Matrix.translateM(model, 0, pose.positionMeters.x.toFloat(), pose.positionMeters.y.toFloat(), pose.positionMeters.z.toFloat())
            Matrix.rotateM(model, 0, pose.rotationDegrees.x.toFloat(), 1f, 0f, 0f)
            Matrix.rotateM(model, 0, pose.rotationDegrees.y.toFloat(), 0f, 1f, 0f)
            Matrix.rotateM(model, 0, pose.rotationDegrees.z.toFloat(), 0f, 0f, 1f)
            val scale = (pose.uniformScale * current.placement.metersPerMathUnit).toFloat()
            Matrix.scaleM(model, 0, scale, scale, scale)
            val viewModel = FloatArray(16); val mvp = FloatArray(16)
            Matrix.multiplyMM(viewModel, 0, frame.viewMatrix.toFloatArray(), 0, model, 0)
            Matrix.multiplyMM(mvp, 0, frame.projectionMatrix.toFloatArray(), 0, viewModel, 0)
            spatialRenderer.render(mvp, clear = false, environmentIntensity = current.scene.environmentIntensity)
            mainHandler.post { onFrame(frame) }
        }

        private fun drawCamera() {
            GLES30.glDisable(GLES30.GL_DEPTH_TEST)
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
            GLES30.glUseProgram(cameraProgram)
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTexture)
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, quadBuffer)
            GLES30.glEnableVertexAttribArray(0); GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 16, 0)
            GLES30.glEnableVertexAttribArray(1); GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 16, 8)
            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        }

        private fun cameraProgram(): Int {
            val vertex = shader(GLES30.GL_VERTEX_SHADER, "#version 300 es\nlayout(location=0) in vec2 p; layout(location=1) in vec2 uv; out vec2 vUv; void main(){vUv=uv;gl_Position=vec4(p,0.,1.);}")
            val fragment = shader(GLES30.GL_FRAGMENT_SHADER, "#version 300 es\n#extension GL_OES_EGL_image_external_essl3 : require\nprecision mediump float; uniform samplerExternalOES cameraTexture; in vec2 vUv; out vec4 color; void main(){color=texture(cameraTexture,vUv);}")
            return GLES30.glCreateProgram().also { GLES30.glAttachShader(it, vertex); GLES30.glAttachShader(it, fragment); GLES30.glLinkProgram(it) }
        }

        private fun shader(type: Int, source: String): Int = GLES30.glCreateShader(type).also { GLES30.glShaderSource(it, source); GLES30.glCompileShader(it) }
    }
}
