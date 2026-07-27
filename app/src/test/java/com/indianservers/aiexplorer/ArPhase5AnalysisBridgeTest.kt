package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.arengine.analysis.ArMeasurementTruth
import com.indianservers.aiexplorer.core.Solid
import com.indianservers.aiexplorer.core.SolidType
import com.indianservers.aiexplorer.core.SurfaceMesh
import com.indianservers.aiexplorer.core.Vec3
import com.indianservers.aiexplorer.spatial.ArPhase5AnalysisBridge
import com.indianservers.aiexplorer.spatial.ArPhase5AnalysisOptions
import com.indianservers.aiexplorer.spatial.SharedSpatialSceneBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArPhase5AnalysisBridgeTest {
    private val surface = SurfaceMesh(
        vertices = listOf(
            Vec3(-1.0, -1.0, -2.0),
            Vec3(1.0, -1.0, 0.0),
            Vec3(-1.0, 1.0, 0.0),
            Vec3(1.0, 1.0, 2.0),
        ),
        rows = 2,
        columns = 2,
    )

    @Test
    fun liveAnalysisAddsConstrainedDifferentialContourAndPlaybackPrimitives() {
        val source = SharedSpatialSceneBuilder.build("phase5", surface = surface)
        val result = ArPhase5AnalysisBridge.enrich(
            source = source,
            surface = surface,
            expression = "x + y",
            solids = emptyList(),
            selectedObjectIds = emptySet(),
            options = ArPhase5AnalysisOptions(enabled = true, contourLevel = 0.0),
            metersPerMathUnit = .1,
            poseUncertaintyMeters = .01,
            depthAvailable = true,
        )

        val ids = result.scene.primitives.map { it.id }.toSet()
        assertTrue("analysis-trace" in ids)
        assertTrue("analysis-normal" in ids)
        assertTrue("analysis-tangent-plane" in ids)
        assertTrue(ids.any { it.startsWith("analysis-contour-") })
        assertTrue(result.gradientSteps > 1)
    }

    @Test
    fun selectedObjectsExposeExactMathematicsAndUncertainPhysicalDistance() {
        val solids = listOf(
            Solid(SolidType.Cube, 2.0, position = Vec3(0.0, 0.0, 0.0)),
            Solid(SolidType.Cube, 1.0, position = Vec3(3.0, 4.0, 0.0)),
        )
        val source = SharedSpatialSceneBuilder.build("measure", solids = solids)
        val result = ArPhase5AnalysisBridge.enrich(
            source = source,
            surface = null,
            expression = "x + y",
            solids = solids,
            selectedObjectIds = setOf("solid-0", "solid-1"),
            options = ArPhase5AnalysisOptions(),
            metersPerMathUnit = .1,
            poseUncertaintyMeters = .02,
            depthAvailable = false,
        )

        val distances = result.measurements.filter { it.kind.name == "Distance" }
        assertEquals(2, distances.size)
        assertEquals(5.0, distances.first { it.truth == ArMeasurementTruth.ExactMathematical }.value, 1e-9)
        val environmental = distances.first { it.truth == ArMeasurementTruth.EnvironmentalEstimate }
        assertEquals(.5, environmental.value, 1e-9)
        assertTrue(environmental.uncertainty > .05)
        assertTrue(result.measurements.any { it.kind.name == "Area" && it.truth == ArMeasurementTruth.ExactMathematical })
        assertTrue(result.measurements.any { it.kind.name == "Volume" && it.truth == ArMeasurementTruth.ExactMathematical })
    }

    @Test
    fun editablePlaneProducesSectionGeometryAndExactPerimeter() {
        val solids = listOf(Solid(SolidType.Cube, 2.0))
        val source = SharedSpatialSceneBuilder.build("section", solids = solids)
        val result = ArPhase5AnalysisBridge.enrich(
            source = source,
            surface = null,
            expression = "x + y",
            solids = solids,
            selectedObjectIds = setOf("solid-0"),
            options = ArPhase5AnalysisOptions(enabled = true, sectionNormal = Vec3(0.0, 1.0, 0.0)),
            metersPerMathUnit = .1,
            poseUncertaintyMeters = .01,
            depthAvailable = true,
        )

        assertTrue(result.scene.primitives.any { it.id == "analysis-section-plane" })
        assertTrue(result.scene.primitives.any { it.id.startsWith("analysis-section-") && it.id != "analysis-section-plane" })
        assertTrue(result.measurements.any { it.kind.name == "SectionPerimeter" && it.value > 0.0 })
    }
}
