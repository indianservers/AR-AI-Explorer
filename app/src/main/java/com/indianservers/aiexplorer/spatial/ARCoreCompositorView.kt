package com.indianservers.aiexplorer.spatial

import android.content.Context
import android.hardware.display.DisplayManager
import android.opengl.GLES11Ext
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.Handler
import android.os.Looper
import android.view.Display
import com.indianservers.aiexplorer.arengine.contract.ArFrameSnapshot
import com.indianservers.aiexplorer.arengine.contract.ArMatrix4
import com.indianservers.aiexplorer.arengine.contract.ArQuaternion
import com.indianservers.aiexplorer.arengine.contract.ArRuntime
import com.indianservers.aiexplorer.arengine.contract.ArVector3
import com.indianservers.aiexplorer.arengine.rendering.ArBoundingSphere
import com.indianservers.aiexplorer.arengine.rendering.ArFrustumCuller
import com.indianservers.aiexplorer.arengine.rendering.ArLightingNormalizer
import com.indianservers.aiexplorer.arengine.rendering.ArModelMatrix
import com.indianservers.aiexplorer.arengine.rendering.ArRenderQualityController
import com.indianservers.aiexplorer.arengine.rendering.ArThermalState
import com.indianservers.aiexplorer.core.Vec3
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

data class SpatialCompositorScene(val scene: SpatialRenderScene, val placement: SpatialScenePlacement)

/**
 * Production camera compositor over the renderer-neutral [ArRuntime].
 * ARCore owns camera/environment state; the existing shared spatial renderer owns mathematics.
 */
class ARCoreCompositorView(
    context: Context,
    runtime: ArRuntime,
    sceneProvider: () -> SpatialCompositorScene,
    onFrame: (ArFrameSnapshot) -> Unit,
    onError: (String) -> Unit,
) : GLSurfaceView(context) {
    private val compositor = CompositorRenderer(
        runtime = runtime,
        sceneProvider = sceneProvider,
        onFrame = onFrame,
        onError = onError,
        rotationProvider = ::currentDisplayRotation,
    )

    init {
        setEGLContextClientVersion(3)
        setPreserveEGLContextOnPause(true)
        setRenderer(compositor)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    override fun onDetachedFromWindow() {
        releaseRenderer()
        super.onDetachedFromWindow()
    }

    fun releaseRenderer() {
        queueEvent(compositor::release)
    }

    private fun currentDisplayRotation(): Int {
        val attached = display
        if (attached != null) return attached.rotation
        val manager = context.getSystemService(DisplayManager::class.java)
        return manager?.getDisplay(Display.DEFAULT_DISPLAY)?.rotation ?: android.view.Surface.ROTATION_0
    }

    private class CompositorRenderer(
        private val runtime: ArRuntime,
        private val sceneProvider: () -> SpatialCompositorScene,
        private val onFrame: (ArFrameSnapshot) -> Unit,
        private val onError: (String) -> Unit,
        private val rotationProvider: () -> Int,
    ) : Renderer {
        private val mainHandler = Handler(Looper.getMainLooper())
        private val spatialRenderer = OpenGlEsSpatialRenderer()
        private val environmentRenderer = OpenGlEsSpatialRenderer()
        private var cameraProgram = 0
        private var cameraTexture = 0
        private var quadBuffer = 0
        private var lastTextureCoordinates: List<com.indianservers.aiexplorer.arengine.contract.ArVector2> = emptyList()
        private var uploadedScene: SpatialRenderScene? = null
        private var uploadedPlan: GpuRenderPlan? = null
        private var environmentSignature = 0
        private var frameAverageMillis = 16.7
        private var previousFrameNanos = 0L
        private var viewportWidth = 0
        private var viewportHeight = 0
        private var configuredRotation = -1
        private var released = false

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            released = false
            cameraTexture = IntArray(1).also { GLES30.glGenTextures(1, it, 0) }[0]
            check(cameraTexture > 0) { "OpenGL did not allocate the external camera texture." }
            GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTexture)
            GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
            GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
            GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
            GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
            runtime.setCameraTextureName(cameraTexture)
            cameraProgram = createCameraProgram()
            quadBuffer = IntArray(1).also { GLES30.glGenBuffers(1, it, 0) }[0]
            check(quadBuffer > 0) { "OpenGL did not allocate the camera quad buffer." }
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, quadBuffer)
            GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, 16 * Float.SIZE_BYTES, null, GLES30.GL_DYNAMIC_DRAW)
            spatialRenderer.initialize()
            environmentRenderer.initialize()
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            viewportWidth = width
            viewportHeight = height
            GLES30.glViewport(0, 0, width, height)
            updateDisplayGeometry(force = true)
        }

        override fun onDrawFrame(gl: GL10?) {
            if (released) return
            updateDisplayGeometry()
            val frame = runtime.updateFrame().getOrElse { error ->
                mainHandler.post { onError(error.message ?: "AR frame failed") }
                return
            }
            updateFrameTime(frame.timestampNanos)
            uploadCameraCoordinates(frame)
            drawCamera()
            drawTrackedPlanes(frame)

            val current = sceneProvider()
            val plan = if (uploadedScene !== current.scene) {
                SharedGpuSceneCompiler.compile(current.scene).also {
                    spatialRenderer.upload(it)
                    uploadedScene = current.scene
                    uploadedPlan = it
                }
            } else {
                uploadedPlan
            }
            if (plan != null) {
                val anchor = current.placement.anchorId
                    .takeIf(String::isNotBlank)
                    ?.let { id -> runtime.anchors().firstOrNull { it.id == id } }
                val pose = current.placement.pose
                val orientation = anchor?.pose?.orientation ?: ArQuaternion.Identity
                val model = ArModelMatrix.compose(
                    position = ArVector3(pose.positionMeters.x, pose.positionMeters.y, pose.positionMeters.z),
                    orientation = orientation,
                    scale = pose.uniformScale * current.placement.metersPerMathUnit,
                )
                applyLocalEulerRotation(model, pose)
                val viewModel = FloatArray(16)
                val mvp = FloatArray(16)
                Matrix.multiplyMM(viewModel, 0, frame.camera.viewMatrix.values.toFloatArray(), 0, model, 0)
                Matrix.multiplyMM(mvp, 0, frame.camera.projectionMatrix.values.toFloatArray(), 0, viewModel, 0)
                val bounds = plan.boundingSphere()
                if (bounds == null || ArFrustumCuller.visible(ArMatrix4(mvp.toList()), bounds)) {
                    val quality = ArRenderQualityController.choose(
                        thermalState = ArThermalState.Nominal,
                        averageFrameMillis = frameAverageMillis,
                    )
                    val light = ArLightingNormalizer.normalize(frame.lighting)
                    spatialRenderer.render(
                        viewProjection = mvp,
                        clear = false,
                        lighting = if (quality.environmentalHdr) light else null,
                        fallbackEnvironmentIntensity = current.scene.environmentIntensity,
                    )
                }
            }
            mainHandler.post { onFrame(frame) }
        }

        fun release() {
            if (released) return
            released = true
            spatialRenderer.release()
            environmentRenderer.release()
            if (quadBuffer != 0) GLES30.glDeleteBuffers(1, intArrayOf(quadBuffer), 0)
            if (cameraTexture != 0) GLES30.glDeleteTextures(1, intArrayOf(cameraTexture), 0)
            if (cameraProgram != 0) GLES30.glDeleteProgram(cameraProgram)
            quadBuffer = 0
            cameraTexture = 0
            cameraProgram = 0
            uploadedScene = null
            uploadedPlan = null
            environmentSignature = 0
            lastTextureCoordinates = emptyList()
        }

        private fun updateFrameTime(timestampNanos: Long) {
            if (previousFrameNanos > 0L && timestampNanos > previousFrameNanos) {
                val elapsed = (timestampNanos - previousFrameNanos) / 1_000_000.0
                frameAverageMillis = frameAverageMillis * .9 + elapsed.coerceAtMost(100.0) * .1
            }
            previousFrameNanos = timestampNanos
        }

        private fun updateDisplayGeometry(force: Boolean = false) {
            if (viewportWidth <= 0 || viewportHeight <= 0) return
            val rotation = rotationProvider()
            if (!force && rotation == configuredRotation) return
            runtime.setDisplayGeometry(rotation, viewportWidth, viewportHeight)
            configuredRotation = rotation
        }

        private fun uploadCameraCoordinates(frame: ArFrameSnapshot) {
            val textureCoordinates = frame.camera.textureCoordinates
            if (textureCoordinates == lastTextureCoordinates) return
            require(textureCoordinates.size == 4) { "AR camera UV mapping must contain four coordinates." }
            val positions = arrayOf(-1f to -1f, 1f to -1f, -1f to 1f, 1f to 1f)
            val quad = FloatArray(16)
            positions.indices.forEach { index ->
                quad[index * 4] = positions[index].first
                quad[index * 4 + 1] = positions[index].second
                quad[index * 4 + 2] = textureCoordinates[index].x
                quad[index * 4 + 3] = textureCoordinates[index].y
            }
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, quadBuffer)
            GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, 0, quad.size * Float.SIZE_BYTES, floatBuffer(quad))
            lastTextureCoordinates = textureCoordinates
        }

        private fun drawCamera() {
            GLES30.glDisable(GLES30.GL_DEPTH_TEST)
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
            GLES30.glUseProgram(cameraProgram)
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTexture)
            GLES30.glUniform1i(GLES30.glGetUniformLocation(cameraProgram, "cameraTexture"), 0)
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, quadBuffer)
            GLES30.glEnableVertexAttribArray(0)
            GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 16, 0)
            GLES30.glEnableVertexAttribArray(1)
            GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 16, 8)
            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        }

        private fun drawTrackedPlanes(frame: ArFrameSnapshot) {
            val tracked = frame.planes.filter {
                it.trackingState == com.indianservers.aiexplorer.arengine.contract.ArTrackingState.Tracking &&
                    it.polygonMeters.size >= 3
            }
            if (tracked.isEmpty()) return
            val signature = tracked.fold(1) { value, plane ->
                31 * value + plane.id.hashCode() + plane.polygonMeters.hashCode()
            }
            if (signature != environmentSignature) {
                val material = SpatialMaterial(
                    name = "tracked plane",
                    colorRgba = listOf(.12f, .85f, 1f, .16f),
                    roughness = .8f,
                    emissive = .08f,
                    blendMode = SpatialBlendMode.Transparent,
                    doubleSided = true,
                )
                val primitives = tracked.map { plane ->
                    val vertices = plane.polygonMeters.map { Vec3(it.x, it.y, it.z) }
                    val triangles = (1 until vertices.lastIndex).flatMap { listOf(0, it, it + 1) }
                    val lines = vertices.indices.map { it to (it + 1) % vertices.size }
                    SpatialPrimitive(
                        id = "tracked-${plane.id}",
                        kind = SpatialPrimitiveKind.Surface,
                        geometry = SpatialGeometry(vertices, triangles, lines),
                        material = material,
                        label = plane.orientation.name,
                        selectable = false,
                    )
                }
                environmentRenderer.upload(
                    SharedGpuSceneCompiler.compile(
                        SpatialRenderScene("tracked-environment", primitives, axesVisible = false),
                    ),
                )
                environmentSignature = signature
            }
            val viewProjection = FloatArray(16)
            Matrix.multiplyMM(
                viewProjection,
                0,
                frame.camera.projectionMatrix.values.toFloatArray(),
                0,
                frame.camera.viewMatrix.values.toFloatArray(),
                0,
            )
            environmentRenderer.render(
                viewProjection = viewProjection,
                clear = false,
                lighting = null,
                fallbackEnvironmentIntensity = .8f,
            )
        }

        private fun createCameraProgram(): Int {
            val vertex = compileShader(
                GLES30.GL_VERTEX_SHADER,
                "#version 300 es\nlayout(location=0) in vec2 p; layout(location=1) in vec2 uv; out vec2 vUv; void main(){vUv=uv;gl_Position=vec4(p,0.,1.);}",
                "camera vertex",
            )
            val fragment = compileShader(
                GLES30.GL_FRAGMENT_SHADER,
                "#version 300 es\n#extension GL_OES_EGL_image_external_essl3 : require\nprecision mediump float; uniform samplerExternalOES cameraTexture; in vec2 vUv; out vec4 color; void main(){color=texture(cameraTexture,vUv);}",
                "camera fragment",
            )
            return GLES30.glCreateProgram().also { program ->
                GLES30.glAttachShader(program, vertex)
                GLES30.glAttachShader(program, fragment)
                GLES30.glLinkProgram(program)
                val status = IntArray(1)
                GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, status, 0)
                val diagnostics = GLES30.glGetProgramInfoLog(program)
                GLES30.glDetachShader(program, vertex)
                GLES30.glDetachShader(program, fragment)
                GLES30.glDeleteShader(vertex)
                GLES30.glDeleteShader(fragment)
                check(status[0] == GLES30.GL_TRUE) {
                    GLES30.glDeleteProgram(program)
                    "Camera shader program failed to link: ${diagnostics.ifBlank { "no driver diagnostics" }}"
                }
            }
        }

        private fun compileShader(type: Int, source: String, label: String): Int =
            GLES30.glCreateShader(type).also { shader ->
                GLES30.glShaderSource(shader, source)
                GLES30.glCompileShader(shader)
                val status = IntArray(1)
                GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, status, 0)
                if (status[0] != GLES30.GL_TRUE) {
                    val diagnostics = GLES30.glGetShaderInfoLog(shader)
                    GLES30.glDeleteShader(shader)
                    error("$label shader failed to compile: ${diagnostics.ifBlank { "no driver diagnostics" }}")
                }
            }

        private fun applyLocalEulerRotation(matrix: FloatArray, pose: SpatialPose) {
            Matrix.rotateM(matrix, 0, pose.rotationDegrees.x.toFloat(), 1f, 0f, 0f)
            Matrix.rotateM(matrix, 0, pose.rotationDegrees.y.toFloat(), 0f, 1f, 0f)
            Matrix.rotateM(matrix, 0, pose.rotationDegrees.z.toFloat(), 0f, 0f, 1f)
        }

        private fun GpuRenderPlan.boundingSphere(): ArBoundingSphere? {
            if (vertices.isEmpty()) return null
            var minX = Float.POSITIVE_INFINITY
            var minY = Float.POSITIVE_INFINITY
            var minZ = Float.POSITIVE_INFINITY
            var maxX = Float.NEGATIVE_INFINITY
            var maxY = Float.NEGATIVE_INFINITY
            var maxZ = Float.NEGATIVE_INFINITY
            var index = 0
            while (index + 2 < vertices.size) {
                minX = minOf(minX, vertices[index])
                minY = minOf(minY, vertices[index + 1])
                minZ = minOf(minZ, vertices[index + 2])
                maxX = maxOf(maxX, vertices[index])
                maxY = maxOf(maxY, vertices[index + 1])
                maxZ = maxOf(maxZ, vertices[index + 2])
                index += 10
            }
            val center = ArVector3(
                ((minX + maxX) / 2f).toDouble(),
                ((minY + maxY) / 2f).toDouble(),
                ((minZ + maxZ) / 2f).toDouble(),
            )
            val dx = (maxX - minX) / 2.0
            val dy = (maxY - minY) / 2.0
            val dz = (maxZ - minZ) / 2.0
            return ArBoundingSphere(center, kotlin.math.sqrt(dx * dx + dy * dy + dz * dz))
        }

        private fun floatBuffer(values: FloatArray) =
            ByteBuffer.allocateDirect(values.size * Float.SIZE_BYTES)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .apply {
                    put(values)
                    position(0)
                }
    }
}
