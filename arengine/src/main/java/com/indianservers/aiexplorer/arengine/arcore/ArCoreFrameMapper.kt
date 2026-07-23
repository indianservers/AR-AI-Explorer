package com.indianservers.aiexplorer.arengine.arcore

import android.media.Image
import com.google.ar.core.Coordinates2d
import com.google.ar.core.Frame
import com.google.ar.core.LightEstimate
import com.google.ar.core.Plane
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.NotYetAvailableException
import com.indianservers.aiexplorer.arengine.contract.ArCameraSnapshot
import com.indianservers.aiexplorer.arengine.contract.ArDepthSnapshot
import com.indianservers.aiexplorer.arengine.contract.ArFrameSnapshot
import com.indianservers.aiexplorer.arengine.contract.ArLightEstimate
import com.indianservers.aiexplorer.arengine.contract.ArMatrix4
import com.indianservers.aiexplorer.arengine.contract.ArPlaneSnapshot
import com.indianservers.aiexplorer.arengine.contract.ArVector3
import com.indianservers.aiexplorer.arengine.contract.ArVector2
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

internal class ArCoreFrameMapper {
    private val planeIds = mutableMapOf<Plane, String>()

    fun map(session: Session, frame: Frame, depthEnabled: Boolean): ArFrameSnapshot {
        val camera = frame.camera
        val view = FloatArray(16).also { camera.getViewMatrix(it, 0) }
        val projection = FloatArray(16).also { camera.getProjectionMatrix(it, 0, 0.05f, 100f) }
        val timestamp = frame.timestamp.coerceAtLeast(0L)
        val planes = session.getAllTrackables(Plane::class.java)
            .asSequence()
            .filter { it.subsumedBy == null && it.trackingState != TrackingState.STOPPED }
            .map(::mapPlane)
            .sortedBy { it.id }
            .toList()
        return ArFrameSnapshot(
            timestampNanos = timestamp,
            camera = ArCameraSnapshot(
                pose = ArCorePoseMapper.map(camera.displayOrientedPose),
                trackingState = ArCoreStateMapper.tracking(camera.trackingState),
                trackingFailure = ArCoreStateMapper.trackingFailure(camera.trackingFailureReason, camera.trackingState),
                viewMatrix = ArMatrix4(view.toList()),
                projectionMatrix = ArMatrix4(projection.toList()),
                textureCoordinates = mapCameraTextureCoordinates(frame),
            ),
            planes = planes,
            lighting = mapLight(frame.lightEstimate),
            depth = if (depthEnabled) acquireDepth(frame, timestamp) else null,
            displayGeometryChanged = frame.hasDisplayGeometryChanged(),
        )
    }

    private fun mapCameraTextureCoordinates(frame: Frame): List<ArVector2> {
        val input = floatBuffer(
            floatArrayOf(
                -1f, -1f,
                1f, -1f,
                -1f, 1f,
                1f, 1f,
            ),
        )
        val output = floatBuffer(FloatArray(8))
        frame.transformCoordinates2d(
            Coordinates2d.OPENGL_NORMALIZED_DEVICE_COORDINATES,
            input,
            Coordinates2d.TEXTURE_NORMALIZED,
            output,
        )
        output.position(0)
        return List(4) { ArVector2(output.get(), output.get()) }
    }

    private fun mapPlane(plane: Plane): ArPlaneSnapshot {
        val center = plane.centerPose
        val polygon = plane.polygon.duplicate().let { buffer ->
            buildList {
                while (buffer.remaining() >= 2) {
                    val local = floatArrayOf(buffer.get(), 0f, buffer.get())
                    val world = center.transformPoint(local)
                    add(ArVector3(world[0].toDouble(), world[1].toDouble(), world[2].toDouble()))
                }
            }
        }
        return ArPlaneSnapshot(
            id = planeIds.getOrPut(plane) { "plane-${UUID.randomUUID()}" },
            centerPose = ArCorePoseMapper.map(center),
            orientation = ArCoreStateMapper.planeOrientation(plane.type),
            extentXMeters = plane.extentX.toDouble().coerceAtLeast(0.0),
            extentZMeters = plane.extentZ.toDouble().coerceAtLeast(0.0),
            polygonMeters = polygon,
            trackingState = ArCoreStateMapper.tracking(plane.trackingState),
        )
    }

    private fun mapLight(estimate: LightEstimate): ArLightEstimate {
        if (estimate.state != LightEstimate.State.VALID) return ArLightEstimate()
        val direction = runCatching { estimate.environmentalHdrMainLightDirection }.getOrNull()
        val intensity = runCatching { estimate.environmentalHdrMainLightIntensity }.getOrNull()
        val harmonics = runCatching { estimate.environmentalHdrAmbientSphericalHarmonics.toList() }.getOrDefault(emptyList())
        return ArLightEstimate(
            valid = true,
            pixelIntensity = estimate.pixelIntensity.takeIf(Float::isFinite)?.coerceAtLeast(0f) ?: 1f,
            mainLightDirection = direction.vectorOr(ArVector3(0.0, -1.0, 0.0)),
            mainLightIntensity = intensity.vectorOr(ArVector3(1.0, 1.0, 1.0)),
            sphericalHarmonics = harmonics.filter(Float::isFinite),
        )
    }

    private fun acquireDepth(frame: Frame, timestamp: Long): ArDepthSnapshot? {
        val image = try {
            frame.acquireDepthImage16Bits()
        } catch (_: NotYetAvailableException) {
            return null
        }
        return image.use { copyDepth(frame, it, timestamp) }
    }

    private fun copyDepth(frame: Frame, image: Image, timestamp: Long): ArDepthSnapshot {
        val plane = image.planes.first()
        val buffer = plane.buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        val values = ArrayList<Int>(image.width * image.height)
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                val offset = y * plane.rowStride + x * plane.pixelStride
                values += buffer.getShort(offset).toInt() and 0xffff
            }
        }
        return ArDepthSnapshot(
            timestampNanos = timestamp,
            width = image.width,
            height = image.height,
            millimeters = values,
            textureCoordinates = mapDepthTextureCoordinates(frame),
        )
    }

    private fun mapDepthTextureCoordinates(frame: Frame): List<ArVector2> {
        val input = floatBuffer(floatArrayOf(0f, 1f, 1f, 1f, 0f, 0f, 1f, 0f))
        val output = floatBuffer(FloatArray(8))
        frame.transformCoordinates2d(
            Coordinates2d.VIEW_NORMALIZED,
            input,
            Coordinates2d.IMAGE_NORMALIZED,
            output,
        )
        output.position(0)
        return List(4) { ArVector2(output.get(), output.get()) }
    }

    private fun FloatArray?.vectorOr(fallback: ArVector3): ArVector3 {
        if (this == null || size < 3 || !this[0].isFinite() || !this[1].isFinite() || !this[2].isFinite()) return fallback
        return ArVector3(this[0].toDouble(), this[1].toDouble(), this[2].toDouble())
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
