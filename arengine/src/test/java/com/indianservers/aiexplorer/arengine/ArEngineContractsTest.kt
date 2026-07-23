package com.indianservers.aiexplorer.arengine

import com.indianservers.aiexplorer.arengine.contract.ArAnchorTrackingState
import com.indianservers.aiexplorer.arengine.contract.ArAvailability
import com.indianservers.aiexplorer.arengine.contract.ArCameraSnapshot
import com.indianservers.aiexplorer.arengine.contract.ArCapabilities
import com.indianservers.aiexplorer.arengine.contract.ArColor
import com.indianservers.aiexplorer.arengine.contract.ArCoordinateTransform
import com.indianservers.aiexplorer.arengine.contract.ArFrameSnapshot
import com.indianservers.aiexplorer.arengine.contract.ArHitCandidate
import com.indianservers.aiexplorer.arengine.contract.ArHitPolicy
import com.indianservers.aiexplorer.arengine.contract.ArHitType
import com.indianservers.aiexplorer.arengine.contract.ArLocalTransform
import com.indianservers.aiexplorer.arengine.contract.ArMaterial
import com.indianservers.aiexplorer.arengine.contract.ArMesh
import com.indianservers.aiexplorer.arengine.contract.ArObjectKind
import com.indianservers.aiexplorer.arengine.contract.ArPose
import com.indianservers.aiexplorer.arengine.contract.ArQuaternion
import com.indianservers.aiexplorer.arengine.contract.ArRuntimeState
import com.indianservers.aiexplorer.arengine.contract.ArScene
import com.indianservers.aiexplorer.arengine.contract.ArSceneObject
import com.indianservers.aiexplorer.arengine.contract.ArScenePlacement
import com.indianservers.aiexplorer.arengine.contract.ArTrackingState
import com.indianservers.aiexplorer.arengine.contract.ArVector2
import com.indianservers.aiexplorer.arengine.contract.ArVector3
import com.indianservers.aiexplorer.arengine.simulator.FakeArRuntime
import com.indianservers.aiexplorer.arengine.simulator.FakeArRuntimeConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArEngineContractsTest {
    private val epsilon = 1e-9

    @Test
    fun quaternionAndPlacementRoundTripPreserveMathematicalCoordinates() {
        val quarterTurn = ArQuaternion.fromEulerDegrees(0.0, 0.0, 90.0)
        val rotated = quarterTurn.rotate(ArVector3(1.0, 0.0, 0.0))
        assertEquals(0.0, rotated.x, epsilon)
        assertEquals(1.0, rotated.y, epsilon)
        assertEquals(0.0, rotated.z, epsilon)

        val placement = ArScenePlacement(
            anchorId = "anchor",
            anchorPose = ArPose(ArVector3(1.2, 0.4, -2.0), ArQuaternion.fromEulerDegrees(5.0, 30.0, -8.0)),
            localTransform = ArLocalTransform(
                offsetMeters = ArVector3(0.15, -0.05, 0.2),
                orientation = ArQuaternion.fromEulerDegrees(12.0, -15.0, 20.0),
                uniformScale = 1.75,
            ),
            metersPerMathUnit = 0.12,
        )
        val mathematical = ArVector3(2.5, -1.25, 0.75)
        val world = ArCoordinateTransform.mathToWorld(mathematical, placement)
        val restored = ArCoordinateTransform.worldToMath(world, placement)
        assertEquals(mathematical.x, restored.x, epsilon)
        assertEquals(mathematical.y, restored.y, epsilon)
        assertEquals(mathematical.z, restored.z, epsilon)
    }

    @Test
    fun fakeRuntimeModelsPermissionInstallResumeFramesAndClose() {
        val frame = frame(100L)
        val runtime = FakeArRuntime(
            config = FakeArRuntimeConfig(
                capabilities = ArCapabilities(ArAvailability.InstallRequired, depthSupported = true),
            ),
            frames = listOf(frame),
        )

        assertTrue(runtime.checkAvailability() is ArRuntimeState.InstallRequired)
        assertTrue(runtime.prepare(cameraPermissionGranted = false) is ArRuntimeState.PermissionRequired)
        assertTrue(runtime.prepare(cameraPermissionGranted = true) is ArRuntimeState.InstallRequested)
        assertTrue(runtime.prepare(cameraPermissionGranted = true, userRequestedInstall = false) is ArRuntimeState.Ready)
        assertTrue(runtime.resume().getOrThrow() is ArRuntimeState.Running)
        runtime.setCameraTextureName(17)
        runtime.setDisplayGeometry(1, 1080, 1920)
        assertEquals(17, runtime.currentCameraTextureName())
        assertEquals(Triple(1, 1080, 1920), runtime.currentDisplayGeometry())
        assertEquals(frame, runtime.updateFrame().getOrThrow())
        assertTrue(runtime.pause() is ArRuntimeState.Paused)
        runtime.close()
        assertEquals(ArRuntimeState.Closed, runtime.state)
        assertTrue(runtime.resume().isFailure)
    }

    @Test
    fun hitPolicyIsDeterministicAndAnchorsHaveExplicitOwnership() {
        val hits = listOf(
            hit("instant", ArHitType.InstantPlacement, 0.95, 0.03, 0.7),
            hit("far-plane", ArHitType.Plane, 0.8, 0.02, 2.0),
            hit("depth", ArHitType.Depth, 0.99, 0.01, 0.5),
            hit("near-plane", ArHitType.Plane, 0.9, 0.01, 1.0),
        )
        assertEquals(listOf("near-plane", "far-plane", "depth", "instant"), ArHitPolicy.rank(hits).map { it.id })

        val runtime = FakeArRuntime(frames = listOf(frame(1L)), hits = hits)
        runtime.prepare(cameraPermissionGranted = true)
        runtime.resume().getOrThrow()
        assertEquals("near-plane", runtime.hitTest(ArVector2(100f, 200f)).first().id)
        val anchor = runtime.createAnchor("near-plane", 20L).getOrThrow()
        assertEquals(ArAnchorTrackingState.Tracking, anchor.trackingState)
        assertEquals(listOf(anchor), runtime.anchors())
        assertTrue(runtime.detachAnchor(anchor.id))
        assertFalse(runtime.detachAnchor(anchor.id))
    }

    @Test
    fun sceneContractsPreserveCanonicalIdentityAndRendererNeutralGeometry() {
        val point = ArSceneObject(
            id = "A",
            kind = ArObjectKind.Point,
            label = "A",
            mesh = ArMesh(listOf(ArVector3(2.0, 3.0, 0.0))),
            material = ArMaterial(ArColor(0.1f, 0.8f, 1f)),
        )
        val vector = ArSceneObject(
            id = "v",
            kind = ArObjectKind.Vector,
            label = "v",
            mesh = ArMesh(
                vertices = listOf(ArVector3.Zero, ArVector3(2.0, 3.0, 0.0)),
                lineIndices = listOf(0, 1),
            ),
            material = ArMaterial(ArColor(0.8f, 0.3f, 1f)),
            dependencyIds = setOf("A", "a"),
        )
        val scene = ArScene("linked-math", revision = 7L, objects = listOf(point, vector))
        assertEquals(listOf("A", "v"), scene.objects.map { it.id })
        // Canonical dependencies may include non-renderable scalars that are intentionally absent from the scene.
        assertEquals(setOf("A", "a"), scene.objects.last().dependencyIds)
        assertEquals(7L, scene.revision)
    }

    @Test(expected = IllegalArgumentException::class)
    fun meshRejectsOutOfRangeIndices() {
        ArMesh(vertices = listOf(ArVector3.Zero), triangleIndices = listOf(0, 1, 0))
    }

    private fun frame(timestamp: Long) = ArFrameSnapshot(
        timestampNanos = timestamp,
        camera = ArCameraSnapshot(ArPose(), ArTrackingState.Tracking),
    )

    private fun hit(
        id: String,
        type: ArHitType,
        confidence: Double,
        uncertainty: Double,
        distance: Double,
    ) = ArHitCandidate(
        id = id,
        type = type,
        pose = ArPose(ArVector3(0.0, 0.0, -distance)),
        distanceMeters = distance,
        confidence = confidence,
        uncertaintyMeters = uncertainty,
    )
}
