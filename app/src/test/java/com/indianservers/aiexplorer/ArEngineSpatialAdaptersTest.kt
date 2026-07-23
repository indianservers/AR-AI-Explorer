package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.arengine.contract.ArAvailability
import com.indianservers.aiexplorer.arengine.contract.ArCameraSnapshot
import com.indianservers.aiexplorer.arengine.contract.ArCapabilities
import com.indianservers.aiexplorer.arengine.contract.ArFrameSnapshot
import com.indianservers.aiexplorer.arengine.contract.ArLightEstimate
import com.indianservers.aiexplorer.arengine.contract.ArPose
import com.indianservers.aiexplorer.arengine.contract.ArQuaternion
import com.indianservers.aiexplorer.arengine.contract.ArRuntimeState
import com.indianservers.aiexplorer.arengine.contract.ArTrackingState
import com.indianservers.aiexplorer.arengine.contract.ArVector3
import com.indianservers.aiexplorer.arengine.contract.ArAnchorHandle
import com.indianservers.aiexplorer.arengine.contract.ArAnchorTrackingState
import com.indianservers.aiexplorer.arengine.interaction.ArSelectionState
import com.indianservers.aiexplorer.core.Solid
import com.indianservers.aiexplorer.core.SolidType
import com.indianservers.aiexplorer.spatial.ArPhase4SpatialBridge
import com.indianservers.aiexplorer.spatial.SharedSpatialSceneBuilder
import com.indianservers.aiexplorer.spatial.SpatialPose
import com.indianservers.aiexplorer.spatial.SpatialScenePlacement
import com.indianservers.aiexplorer.spatial.TrackingQuality
import com.indianservers.aiexplorer.spatial.toSpatialCapabilities
import com.indianservers.aiexplorer.spatial.toSpatialFrame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArEngineSpatialAdaptersTest {
    @Test
    fun runtimeCapabilitiesReachTheExistingSpatialUi() {
        val result = ArRuntimeState.Ready(
            ArCapabilities(
                availability = ArAvailability.Ready,
                depthSupported = true,
                environmentalHdrSupported = true,
            ),
        ).toSpatialCapabilities()

        assertEquals(com.indianservers.aiexplorer.spatial.ARAvailability.Ready, result.availability)
        assertTrue(result.depthSupported)
        assertTrue(result.message.contains("environmental HDR"))
    }

    @Test
    fun frameAdapterPreservesTrackingPoseAndHdrInputs() {
        val frame = ArFrameSnapshot(
            timestampNanos = 42L,
            camera = ArCameraSnapshot(
                pose = ArPose(
                    positionMeters = ArVector3(1.0, 2.0, 3.0),
                    orientation = ArQuaternion.fromEulerDegrees(0.0, 90.0, 0.0),
                ),
                trackingState = ArTrackingState.Tracking,
            ),
            lighting = ArLightEstimate(
                valid = true,
                pixelIntensity = 1.5f,
                mainLightDirection = ArVector3(0.0, -1.0, 0.0),
                mainLightIntensity = ArVector3(2.0, 1.0, .5),
                sphericalHarmonics = listOf(.1f, .2f, .3f),
            ),
        ).toSpatialFrame()

        assertEquals(TrackingQuality.Tracking, frame.trackingQuality)
        assertEquals(1.0, frame.cameraPose.positionMeters.x, 0.0)
        assertEquals(1.5f, frame.lighting.pixelIntensity)
        assertEquals(3, frame.lighting.sphericalHarmonics.size)
    }

    @Test
    fun phase4BridgeKeepsNativeAnchorSeparateAndAppliesSelectionVisibility() {
        val anchor = ArAnchorHandle(
            id = "anchor",
            pose = ArPose(
                ArVector3(1.0, 2.0, 3.0),
                ArQuaternion.fromEulerDegrees(0.0, 90.0, 0.0),
            ),
            trackingState = ArAnchorTrackingState.Tracking,
            createdAtMillis = 1L,
            updatedAtMillis = 1L,
        )
        val source = SharedSpatialSceneBuilder.build(
            id = "scene",
            solids = listOf(
                Solid(SolidType.Cube, 1.0),
                Solid(SolidType.Sphere, 1.0),
            ),
        )
        val selection = ArSelectionState(
            objectIds = setOf("solid-0"),
            primaryObjectId = "solid-0",
            hiddenObjectIds = setOf("solid-1"),
        )
        val bridged = ArPhase4SpatialBridge.scene(
            source,
            SpatialScenePlacement(
                anchorId = "anchor",
                pose = SpatialPose(positionMeters = com.indianservers.aiexplorer.core.Vec3(1.2, 2.0, 3.0)),
            ),
            anchor,
            selection,
        )

        assertEquals(anchor.pose, bridged.placement.anchorPose)
        assertEquals("anchor", bridged.placement.anchorId)
        assertTrue(bridged.objects.first { it.id == "solid-0" }.visible)
        assertEquals(false, bridged.objects.first { it.id == "solid-1" }.visible)
        assertTrue(bridged.placement.localTransform.offsetMeters.magnitude() > 0.0)
    }
}
