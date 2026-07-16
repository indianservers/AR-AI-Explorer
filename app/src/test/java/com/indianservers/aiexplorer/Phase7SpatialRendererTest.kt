package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.Graph3D
import com.indianservers.aiexplorer.core.Solid
import com.indianservers.aiexplorer.core.SolidType
import com.indianservers.aiexplorer.core.Vec3
import com.indianservers.aiexplorer.core.Vector3D
import com.indianservers.aiexplorer.spatial.ARPrivacySafetyChecklist
import com.indianservers.aiexplorer.spatial.AnchorTrackingState
import com.indianservers.aiexplorer.spatial.PersistentSpatialAnchor
import com.indianservers.aiexplorer.spatial.RenderQuality
import com.indianservers.aiexplorer.spatial.SharedGpuSceneCompiler
import com.indianservers.aiexplorer.spatial.SharedSpatialSceneBuilder
import com.indianservers.aiexplorer.spatial.SpatialAnchorPersistence
import com.indianservers.aiexplorer.spatial.SpatialHit
import com.indianservers.aiexplorer.spatial.SpatialHitType
import com.indianservers.aiexplorer.spatial.SpatialLessonCatalog
import com.indianservers.aiexplorer.spatial.SpatialMeasurementEngine
import com.indianservers.aiexplorer.spatial.SpatialPicking
import com.indianservers.aiexplorer.spatial.SpatialPlacementEngine
import com.indianservers.aiexplorer.spatial.SpatialPrimitiveKind
import com.indianservers.aiexplorer.spatial.SpatialRay
import com.indianservers.aiexplorer.spatial.SpatialScenePlacement
import com.indianservers.aiexplorer.spatial.SpatialPerformanceManager
import com.indianservers.aiexplorer.spatial.ThermalLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase7SpatialRendererTest {
    @Test fun oneSceneCompilesSolidsSurfacesVectorsAndProbabilityFor3dAndAr() {
        val surface = Graph3D().mesh("x^2+y^2", density = 8)
        val scene = SharedSpatialSceneBuilder.build(
            "shared",
            solids = listOf(Solid(SolidType.Cube, 1.0)),
            surface = surface,
            vectors = listOf(Vector3D("v", Vec3(0.0, 0.0, 0.0), Vec3(1.0, 2.0, 0.0))),
            probabilitySurface = surface,
        )
        assertTrue(scene.primitives.map { it.kind }.containsAll(setOf(SpatialPrimitiveKind.Solid, SpatialPrimitiveKind.Surface, SpatialPrimitiveKind.Curve, SpatialPrimitiveKind.ProbabilitySurface)))
        val normal3dPlan = SharedGpuSceneCompiler.compile(scene)
        val arPlan = SharedGpuSceneCompiler.compile(scene.copy(depthOcclusion = true))
        assertTrue(normal3dPlan.vertices.isNotEmpty())
        assertTrue(normal3dPlan.triangleIndices.isNotEmpty())
        assertTrue(normal3dPlan.lineIndices.isNotEmpty())
        assertEquals(normal3dPlan.vertices.size, arPlan.vertices.size)
    }

    @Test fun pickingFindsTypedObjectAndNearestPoint() {
        val scene = SharedSpatialSceneBuilder.build("pick", listOf(Solid(SolidType.Cube, 1.0)))
        val hits = SpatialPicking.hitTest(scene, SpatialRay(Vec3(0.0, 0.0, 3.0), Vec3(0.0, 0.0, -1.0)))
        assertTrue(hits.isNotEmpty())
        assertEquals(SpatialPrimitiveKind.Solid, hits.first().kind)
        val solidVertex = scene.primitives.first { it.kind == SpatialPrimitiveKind.Solid }.geometry.vertices.first()
        assertTrue(SpatialPicking.nearestPoint(scene, solidVertex, .001) != null)
    }

    @Test fun safePlacementRejectsWeakHitAndAcceptsReliablePlane() {
        val weak = SpatialHit(SpatialHitType.Depth, Vec3(0.0, 0.0, -1.0), distanceMeters = 1.0, confidence = .2, uncertaintyMeters = .4)
        val (_, rejected) = SpatialPlacementEngine.place(SpatialScenePlacement(), weak, 1L)
        assertFalse(rejected.accepted)
        val plane = SpatialHit(SpatialHitType.Plane, Vec3(0.0, .5, -1.0), distanceMeters = 1.0, confidence = .9, uncertaintyMeters = .02, trackableId = "plane")
        val (placed, accepted) = SpatialPlacementEngine.place(SpatialScenePlacement(), plane, 2L)
        assertTrue(accepted.accepted)
        assertTrue(placed.isPlaced)
        assertEquals(.02, placed.measurementUncertaintyMeters, 1e-9)
    }

    @Test fun measurementsAlwaysExposeUncertaintyAndEstimateStatus() {
        val measurement = SpatialMeasurementEngine.measure(Vec3(0.0, 0.0, 0.0), Vec3(1.0, 0.0, 0.0), .01, .02)
        assertTrue(measurement.uncertainty > 0)
        assertTrue(measurement.display.contains("educational estimate"))
    }

    @Test fun anchorsPersistAndRelocalizeWithoutCameraImagery() {
        val anchor = PersistentSpatialAnchor("a", SpatialScenePlacement().pose, 1L, 1L, lessonId = "conic-sections")
        val payload = SpatialAnchorPersistence.serialize(listOf(anchor))
        assertTrue(payload.contains("conic-sections"))
        assertFalse(payload.contains("image"))
        assertEquals(AnchorTrackingState.Relocalizing, SpatialAnchorPersistence.relocalize(anchor, null, 2L).trackingState)
        assertEquals(AnchorTrackingState.Tracking, SpatialAnchorPersistence.relocalize(anchor, anchor.pose, 3L).trackingState)
    }

    @Test fun thermalPolicyDegradesBeforeCriticalAndSimulatorRemainsComplete() {
        assertEquals(RenderQuality.Ultra, SpatialPerformanceManager.policy(ThermalLevel.Nominal, 12.0).quality)
        assertEquals(RenderQuality.Safety, SpatialPerformanceManager.policy(ThermalLevel.Critical, 12.0).quality)
        val fallback = SpatialPlacementEngine.place(SpatialScenePlacement(), Vec3(0.0, 0.0, -1.2), 5L)
        assertTrue(fallback.isPlaced)
    }

    @Test fun lessonAndSafetyCatalogsCoverAcceptanceScope() {
        val ids = SpatialLessonCatalog.lessons.map { it.id }.toSet()
        assertTrue(ids.containsAll(setOf("conic-sections", "vector-addition-ar", "surface-gradients-ar", "cross-sections-ar", "probability-surface-ar", "orbital-vectors-bridge")))
        assertTrue(SpatialLessonCatalog.lessons.all { it.steps.size >= 3 && it.futureNetworkShareable })
        assertTrue(ARPrivacySafetyChecklist.items.size >= 8)
        assertTrue(ARPrivacySafetyChecklist.items.all { it.mandatory })
    }
}
