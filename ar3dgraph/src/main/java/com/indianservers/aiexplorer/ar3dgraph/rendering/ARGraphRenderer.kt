package com.indianservers.aiexplorer.ar3dgraph.rendering

import android.opengl.GLES11Ext
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.SystemClock
import com.google.ar.core.Anchor
import com.google.ar.core.Coordinates2d
import com.google.ar.core.Frame
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.indianservers.aiexplorer.ar3dgraph.ar.ARTrackingState
import com.indianservers.aiexplorer.ar3dgraph.ar.ARTrackingStabilizer
import com.indianservers.aiexplorer.ar3dgraph.ar.AnchorPlacementController
import com.indianservers.aiexplorer.ar3dgraph.ar.AnchorPlacementResult
import com.indianservers.aiexplorer.ar3dgraph.ar.PlacementAnchor
import com.indianservers.aiexplorer.ar3dgraph.ar.PlacementMath
import com.indianservers.aiexplorer.ar3dgraph.ar.WorldAnchorFactory
import com.indianservers.aiexplorer.ar3dgraph.ar.WorldVector3
import com.indianservers.aiexplorer.ar3dgraph.integration.ARGraphRenderData
import com.indianservers.aiexplorer.ar3dgraph.integration.ARMeshRenderData
import com.indianservers.aiexplorer.ar3dgraph.gesture.ARGestureAvailability
import com.indianservers.aiexplorer.ar3dgraph.gesture.ARGraphTransformState
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.util.concurrent.atomic.AtomicReference
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

interface ARGraphRenderListener {
    fun onTrackingChanged(state: ARTrackingState, message: String)
    fun onPlacementStarted()
    fun onPlacementResult(result: AnchorPlacementResult)
    fun onRendererError(message: String)
}

/**
 * ARCore/OpenGL renderer. Geometry is uploaded only when graph data changes; each frame changes only
 * camera and graph-root transforms. No plane query, hit test, or depth placement exists here.
 */
class ARGraphRenderer(
    private val sessionProvider: () -> Session? = { null },
    private val displayRotationProvider: () -> Int = { 0 },
    private val listener: ARGraphRenderListener? = null,
) : GLSurfaceView.Renderer, AutoCloseable {
    private data class Tap(val x: Float, val y: Float)
    private data class ArAnchor(val value: Anchor) : PlacementAnchor {
        override fun detach() = value.detach()
    }
    private data class MeshBuffers(
        val positions: FloatBuffer,
        val colors: FloatBuffer,
        val indices: IntBuffer,
        val indexCount: Int,
    )
    private data class LineBuffers(val positions: FloatBuffer, val colors: FloatBuffer, val vertexCount: Int)

    @Volatile var closed: Boolean = false
        private set
    @Volatile var scene: ARGraphScene = ARGraphScene()
        private set

    private val pendingGraph = AtomicReference<ARGraphRenderData?>()
    private val pendingTap = AtomicReference<Tap?>()
    private val pendingTransform = AtomicReference(ARGraphTransformState())
    @Volatile private var graphRevision = 0
    private var uploadedRevision = -1
    @Volatile private var resetRequested = false
    @Volatile private var clearRequested = false
    private var currentGraph: ARGraphRenderData? = null
    private var currentTransform = ARGraphTransformState()
    private var meshBuffers = emptyList<MeshBuffers>()
    private var lineBuffers: LineBuffers? = null
    private var activeAnchor: Anchor? = null
    private var anchorsCreated = 0
    private var viewportWidth = 0
    private var viewportHeight = 0
    private var cameraTexture = 0
    private var backgroundProgram = 0
    private var graphProgram = 0
    private var backgroundPositionLocation = -1
    private var backgroundTextureLocation = -1
    private var backgroundSamplerLocation = -1
    private var graphMvpLocation = -1
    private var graphPositionLocation = -1
    private var graphColorLocation = -1
    private val trackingStabilizer = ARTrackingStabilizer()
    @Volatile private var trackingUsable = false
    @Volatile private var graphReady = false
    @Volatile private var graphPlaced = false
    private val backgroundVertices = directFloat(floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f))
    private val backgroundTextureCoordinates = directFloat(FloatArray(8))
    private val viewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val anchorMatrix = FloatArray(16)
    private val userMatrix = FloatArray(16)
    private val localMatrix = FloatArray(16)
    private val userLocalMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val viewModelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private var graphCentreX = 0f
    private var graphCentreY = 0f
    private var graphCentreZ = 0f

    private val anchorController = AnchorPlacementController(
        factory = WorldAnchorFactory { worldPose ->
            val session = sessionProvider() ?: error("ARCore session is unavailable.")
            val anchor = session.createAnchor(
                Pose(
                    floatArrayOf(worldPose.position.x, worldPose.position.y, worldPose.position.z),
                    floatArrayOf(worldPose.rotation.x, worldPose.rotation.y, worldPose.rotation.z, worldPose.rotation.w),
                ),
            )
            ArAnchor(anchor)
        },
        attach = { placementAnchor -> activeAnchor = (placementAnchor as ArAnchor).value },
    )

    @Synchronized fun submitGraph(data: ARGraphRenderData) {
        if (closed) return
        pendingGraph.set(data)
        graphRevision++
    }

    fun requestPlacement(x: Float, y: Float) {
        if (closed) return
        pendingTap.set(Tap(x, y))
    }

    fun submitTransform(transform: ARGraphTransformState) {
        if (!closed) pendingTransform.set(transform)
    }

    fun gestureAvailability() = ARGestureAvailability(graphReady, graphPlaced, trackingUsable)

    fun resetPlacement() {
        if (closed) return
        resetRequested = true
    }

    @Synchronized fun clearGraph() {
        if (closed) return
        clearRequested = true
        pendingGraph.set(null)
        graphRevision++
    }

    fun renderedGeometryCount(): Int = scene.renderedMeshes

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        if (closed) return
        releaseGlResources()
        GLES30.glClearColor(0.02f, 0.04f, 0.08f, 1f)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
        GLES30.glDisable(GLES30.GL_CULL_FACE)
        cameraTexture = createExternalTexture()
        backgroundProgram = program(BACKGROUND_VERTEX, BACKGROUND_FRAGMENT)
        graphProgram = program(GRAPH_VERTEX, GRAPH_FRAGMENT)
        backgroundPositionLocation = GLES30.glGetAttribLocation(backgroundProgram, "aPosition")
        backgroundTextureLocation = GLES30.glGetAttribLocation(backgroundProgram, "aTexCoord")
        backgroundSamplerLocation = GLES30.glGetUniformLocation(backgroundProgram, "uTexture")
        graphMvpLocation = GLES30.glGetUniformLocation(graphProgram, "uMvp")
        graphPositionLocation = GLES30.glGetAttribLocation(graphProgram, "aPosition")
        graphColorLocation = GLES30.glGetAttribLocation(graphProgram, "aColor")
        sessionProvider()?.setCameraTextureName(cameraTexture)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewportWidth = width
        viewportHeight = height
        GLES30.glViewport(0, 0, width, height)
        sessionProvider()?.setDisplayGeometry(displayRotationProvider(), width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        if (closed) return
        try {
            applyPendingState()
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
            val session = sessionProvider() ?: return
            session.setCameraTextureName(cameraTexture)
            val frame = session.update()
            drawCamera(frame)
            val camera = frame.camera
            val tracking = camera.trackingState == TrackingState.TRACKING
            trackingUsable = tracking
            reportTracking(camera.trackingState, camera.trackingFailureReason.name)
            pendingTap.getAndSet(null)?.let { tap ->
                if (currentGraph == null) {
                    listener?.onPlacementResult(AnchorPlacementResult.Failed("Generate a graph before placing it."))
                } else if (!tracking) {
                    listener?.onPlacementResult(AnchorPlacementResult.TrackingPaused)
                } else {
                    listener?.onPlacementStarted()
                    place(tap, frame)
                }
            }
            if (tracking && activeAnchor?.trackingState == TrackingState.TRACKING) drawGraph(frame)
        } catch (error: Throwable) {
            listener?.onRendererError(error.message ?: error::class.java.simpleName)
        }
    }

    private fun applyPendingState() {
        val nextTransform = pendingTransform.get()
        if (nextTransform != currentTransform) {
            currentTransform = nextTransform
            scene = scene.copy(
                userScale = nextTransform.uniformScale,
                userYawDegrees = nextTransform.yawDegrees,
                userPitchDegrees = nextTransform.pitchDegrees,
            )
        }
        if (clearRequested) {
            clearRequested = false
            anchorController.clear()
            activeAnchor = null
            currentGraph = null
            currentTransform = ARGraphTransformState()
            pendingTransform.set(currentTransform)
            graphReady = false
            graphPlaced = false
            meshBuffers = emptyList()
            lineBuffers = null
            scene = ARGraphScene()
        } else if (resetRequested) {
            resetRequested = false
            anchorController.reset()
            activeAnchor = null
            graphPlaced = false
            scene = scene.copy(activeAnchors = 0)
        }
        if (uploadedRevision != graphRevision) {
            uploadedRevision = graphRevision
            val next = pendingGraph.get()
            if (next != null) {
                val nextMeshBuffers = next.meshes.map(::buffers)
                val nextLineBuffers = lineBuffers(next)
                val centre = next.bounds.centre
                currentGraph = next
                graphCentreX = centre.x.toFloat()
                graphCentreY = centre.y.toFloat()
                graphCentreZ = centre.z.toFloat()
                graphReady = true
                meshBuffers = nextMeshBuffers
                lineBuffers = nextLineBuffers
                scene = scene.copy(graphGeometryAttached = true, renderedMeshes = next.meshes.size)
            }
        }
    }

    private fun place(tap: Tap, frame: Frame) {
        val camera = frame.camera
        val view = FloatArray(16).also { camera.getViewMatrix(it, 0) }
        val projection = FloatArray(16).also { camera.getProjectionMatrix(it, 0, 0.05f, 100f) }
        val translation = camera.pose.translation
        val cameraPosition = WorldVector3(translation[0], translation[1], translation[2])
        val ray = PlacementMath.screenRay(tap.x, tap.y, viewportWidth, viewportHeight, cameraPosition, view, projection)
            .getOrElse {
                listener?.onPlacementResult(AnchorPlacementResult.Failed(it.message ?: "Unable to calculate placement ray."))
                return
            }
        val pose = PlacementMath.placementPose(ray, cameraPosition).getOrElse {
            listener?.onPlacementResult(AnchorPlacementResult.Failed(it.message ?: "Unable to calculate placement pose."))
            return
        }
        val result = anchorController.place(pose, tracking = true)
        if (result is AnchorPlacementResult.Placed || result is AnchorPlacementResult.Replaced) {
            anchorsCreated++
            graphPlaced = true
            scene = scene.copy(activeAnchors = 1, anchorsCreated = anchorsCreated)
        }
        listener?.onPlacementResult(result)
    }

    private fun drawCamera(frame: Frame) {
        if (frame.timestamp == 0L || backgroundProgram == 0) return
        GLES30.glDisable(GLES30.GL_DEPTH_TEST)
        GLES30.glDepthMask(false)
        GLES30.glUseProgram(backgroundProgram)
        val vertices = backgroundVertices
        val texture = backgroundTextureCoordinates
        frame.transformCoordinates2d(
            Coordinates2d.OPENGL_NORMALIZED_DEVICE_COORDINATES,
            vertices,
            Coordinates2d.TEXTURE_NORMALIZED,
            texture,
        )
        vertices.position(0); texture.position(0)
        val position = backgroundPositionLocation
        val texCoord = backgroundTextureLocation
        GLES30.glEnableVertexAttribArray(position)
        GLES30.glEnableVertexAttribArray(texCoord)
        GLES30.glVertexAttribPointer(position, 2, GLES30.GL_FLOAT, false, 0, vertices)
        GLES30.glVertexAttribPointer(texCoord, 2, GLES30.GL_FLOAT, false, 0, texture)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTexture)
        GLES30.glUniform1i(backgroundSamplerLocation, 0)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        GLES30.glDisableVertexAttribArray(position)
        GLES30.glDisableVertexAttribArray(texCoord)
        GLES30.glDepthMask(true)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
    }

    private fun drawGraph(frame: Frame) {
        if (currentGraph == null) return
        val anchor = activeAnchor ?: return
        frame.camera.getViewMatrix(viewMatrix, 0)
        frame.camera.getProjectionMatrix(projectionMatrix, 0, 0.05f, 100f)
        anchor.pose.toMatrix(anchorMatrix, 0)
        quaternionMatrix(currentTransform, userMatrix)
        Matrix.scaleM(userMatrix, 0, currentTransform.uniformScale, currentTransform.uniformScale, currentTransform.uniformScale)
        Matrix.setIdentityM(localMatrix, 0)
        Matrix.rotateM(localMatrix, 0, -90f, 1f, 0f, 0f)
        Matrix.scaleM(localMatrix, 0, METRES_PER_MATH_UNIT, METRES_PER_MATH_UNIT, METRES_PER_MATH_UNIT)
        Matrix.translateM(localMatrix, 0, -graphCentreX, -graphCentreY, -graphCentreZ)
        Matrix.multiplyMM(userLocalMatrix, 0, userMatrix, 0, localMatrix, 0)
        Matrix.multiplyMM(modelMatrix, 0, anchorMatrix, 0, userLocalMatrix, 0)
        Matrix.multiplyMM(viewModelMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewModelMatrix, 0)
        GLES30.glUseProgram(graphProgram)
        GLES30.glUniformMatrix4fv(graphMvpLocation, 1, false, mvpMatrix, 0)
        val positionLocation = graphPositionLocation
        val colorLocation = graphColorLocation
        meshBuffers.forEach { mesh ->
            mesh.positions.position(0); mesh.colors.position(0); mesh.indices.position(0)
            GLES30.glEnableVertexAttribArray(positionLocation)
            GLES30.glEnableVertexAttribArray(colorLocation)
            GLES30.glVertexAttribPointer(positionLocation, 3, GLES30.GL_FLOAT, false, 0, mesh.positions)
            GLES30.glVertexAttribPointer(colorLocation, 4, GLES30.GL_FLOAT, false, 0, mesh.colors)
            GLES30.glDrawElements(GLES30.GL_TRIANGLES, mesh.indexCount, GLES30.GL_UNSIGNED_INT, mesh.indices)
        }
        lineBuffers?.let { lines ->
            lines.positions.position(0); lines.colors.position(0)
            GLES30.glVertexAttribPointer(positionLocation, 3, GLES30.GL_FLOAT, false, 0, lines.positions)
            GLES30.glVertexAttribPointer(colorLocation, 4, GLES30.GL_FLOAT, false, 0, lines.colors)
            GLES30.glLineWidth(2f)
            GLES30.glDrawArrays(GLES30.GL_LINES, 0, lines.vertexCount)
        }
        GLES30.glDisableVertexAttribArray(positionLocation)
        GLES30.glDisableVertexAttribArray(colorLocation)
    }

    private fun buffers(mesh: ARMeshRenderData): MeshBuffers = MeshBuffers(
        positions = directFloat(mesh.vertices.flatMap { listOf(it.x.toFloat(), it.y.toFloat(), it.z.toFloat()) }.toFloatArray()),
        colors = directFloat(mesh.colors.flatMap { listOf(it.red, it.green, it.blue, it.alpha) }.toFloatArray()),
        indices = directInt(mesh.indices.toIntArray()),
        indexCount = mesh.indices.size,
    )

    private fun lineBuffers(graph: ARGraphRenderData): LineBuffers {
        val lines = graph.grid + graph.axes
        return LineBuffers(
            positions = directFloat(lines.flatMap { line ->
                listOf(
                    line.start.x.toFloat(), line.start.y.toFloat(), line.start.z.toFloat(),
                    line.end.x.toFloat(), line.end.y.toFloat(), line.end.z.toFloat(),
                )
            }.toFloatArray()),
            colors = directFloat(lines.flatMap { line ->
                List(2) { listOf(line.color.red, line.color.green, line.color.blue, line.color.alpha) }.flatten()
            }.toFloatArray()),
            vertexCount = lines.size * 2,
        )
    }

    private fun reportTracking(state: TrackingState, reason: String) {
        val mapped = when (state) {
            TrackingState.TRACKING -> ARTrackingState.Tracking
            TrackingState.PAUSED -> ARTrackingState.Paused
            TrackingState.STOPPED -> ARTrackingState.Stopped
        }
        val stable = trackingStabilizer.update(mapped, SystemClock.elapsedRealtime()) ?: return
        val message = when (stable) {
            ARTrackingState.Tracking -> "Tracking active."
            ARTrackingState.Paused -> if (reason == "NONE") "Tracking paused." else "Tracking paused: ${reason.lowercase().replace('_', ' ')}."
            ARTrackingState.Stopped -> "Tracking stopped."
            ARTrackingState.Initializing -> "Tracking initializing."
            ARTrackingState.Error -> "Tracking error."
        }
        listener?.onTrackingChanged(mapped, message)
    }

    override fun close() {
        if (closed) return
        anchorController.close()
        activeAnchor = null
        meshBuffers = emptyList()
        lineBuffers = null
        currentGraph = null
        graphReady = false
        graphPlaced = false
        trackingUsable = false
        trackingStabilizer.reset()
        scene = ARGraphScene()
        closed = true
    }

    /** Must be called through GLSurfaceView.queueEvent so an EGL context owns the deletions. */
    fun closeOnGlThread() {
        releaseGlResources()
        close()
    }

    private fun releaseGlResources() {
        if (cameraTexture != 0) {
            GLES30.glDeleteTextures(1, intArrayOf(cameraTexture), 0)
            cameraTexture = 0
        }
        if (backgroundProgram != 0) {
            GLES30.glDeleteProgram(backgroundProgram)
            backgroundProgram = 0
        }
        if (graphProgram != 0) {
            GLES30.glDeleteProgram(graphProgram)
            graphProgram = 0
        }
        backgroundPositionLocation = -1
        backgroundTextureLocation = -1
        backgroundSamplerLocation = -1
        graphMvpLocation = -1
        graphPositionLocation = -1
        graphColorLocation = -1
    }

    private fun quaternionMatrix(transform: ARGraphTransformState, output: FloatArray) {
        val q = transform.rotation
        val xx = q.x * q.x; val yy = q.y * q.y; val zz = q.z * q.z
        val xy = q.x * q.y; val xz = q.x * q.z; val yz = q.y * q.z
        val wx = q.w * q.x; val wy = q.w * q.y; val wz = q.w * q.z
        output[0] = 1f - 2f * (yy + zz); output[1] = 2f * (xy + wz); output[2] = 2f * (xz - wy); output[3] = 0f
        output[4] = 2f * (xy - wz); output[5] = 1f - 2f * (xx + zz); output[6] = 2f * (yz + wx); output[7] = 0f
        output[8] = 2f * (xz + wy); output[9] = 2f * (yz - wx); output[10] = 1f - 2f * (xx + yy); output[11] = 0f
        output[12] = 0f; output[13] = 0f; output[14] = 0f; output[15] = 1f
    }

    private fun createExternalTexture(): Int {
        val textures = IntArray(1)
        GLES30.glGenTextures(1, textures, 0)
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0])
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        return textures[0]
    }

    private fun program(vertex: String, fragment: String): Int {
        fun shader(type: Int, source: String): Int {
            val value = GLES30.glCreateShader(type)
            GLES30.glShaderSource(value, source)
            GLES30.glCompileShader(value)
            val status = IntArray(1)
            GLES30.glGetShaderiv(value, GLES30.GL_COMPILE_STATUS, status, 0)
            require(status[0] == GLES30.GL_TRUE) { GLES30.glGetShaderInfoLog(value) }
            return value
        }
        val program = GLES30.glCreateProgram()
        val vertexShader = shader(GLES30.GL_VERTEX_SHADER, vertex)
        val fragmentShader = shader(GLES30.GL_FRAGMENT_SHADER, fragment)
        GLES30.glAttachShader(program, vertexShader)
        GLES30.glAttachShader(program, fragmentShader)
        GLES30.glLinkProgram(program)
        GLES30.glDeleteShader(vertexShader)
        GLES30.glDeleteShader(fragmentShader)
        val status = IntArray(1)
        GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, status, 0)
        require(status[0] == GLES30.GL_TRUE) { GLES30.glGetProgramInfoLog(program) }
        return program
    }

    private fun directFloat(values: FloatArray): FloatBuffer =
        ByteBuffer.allocateDirect(values.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
            put(values); position(0)
        }

    private fun directInt(values: IntArray): IntBuffer =
        ByteBuffer.allocateDirect(values.size * 4).order(ByteOrder.nativeOrder()).asIntBuffer().apply {
            put(values); position(0)
        }

    private companion object {
        const val METRES_PER_MATH_UNIT = 0.1f
        const val BACKGROUND_VERTEX = """
            #version 300 es
            in vec2 aPosition;
            in vec2 aTexCoord;
            out vec2 vTexCoord;
            void main() { gl_Position = vec4(aPosition, 0.0, 1.0); vTexCoord = aTexCoord; }
        """
        const val BACKGROUND_FRAGMENT = """
            #version 300 es
            #extension GL_OES_EGL_image_external_essl3 : require
            precision mediump float;
            uniform samplerExternalOES uTexture;
            in vec2 vTexCoord;
            out vec4 outColor;
            void main() { outColor = texture(uTexture, vTexCoord); }
        """
        const val GRAPH_VERTEX = """
            #version 300 es
            uniform mat4 uMvp;
            in vec3 aPosition;
            in vec4 aColor;
            out vec4 vColor;
            void main() { gl_Position = uMvp * vec4(aPosition, 1.0); vColor = aColor; }
        """
        const val GRAPH_FRAGMENT = """
            #version 300 es
            precision mediump float;
            in vec4 vColor;
            out vec4 outColor;
            void main() { outColor = vColor; }
        """
    }
}
