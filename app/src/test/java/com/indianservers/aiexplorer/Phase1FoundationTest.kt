package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.FunctionDefinition
import com.indianservers.aiexplorer.core.Solid
import com.indianservers.aiexplorer.core.SolidType
import com.indianservers.aiexplorer.core.TrustedMathKernel
import com.indianservers.aiexplorer.core.Vec2
import com.indianservers.aiexplorer.core.Vec3
import com.indianservers.aiexplorer.core.Vector3D
import com.indianservers.aiexplorer.spatial.ARScaleMode
import com.indianservers.aiexplorer.spatial.AnchorTrackingState
import com.indianservers.aiexplorer.spatial.SpatialPose
import com.indianservers.aiexplorer.spatial.SpatialScenePlacement
import com.indianservers.aiexplorer.spatial.TrackingQuality
import com.indianservers.aiexplorer.workspace.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase1FoundationTest {
    @Test fun projectRoundTripIsLosslessAcross2dGraph3dAndSpatialState() {
        val state = WorkspaceState(
            id = "proof/project-1", name = "Quadratic & solid", module = MathModule.Geometry3D,
            points = listOf(Vec2(-1.25, 3.5), Vec2(4.0, -2.75), Vec2(1.375, .375)),
            shapes = listOf(Shape2D("triangle-A", Shape2DType.Triangle, listOf(0, 1, 2), "A|B triangle", false, true, "violet")),
            pointDependencies = listOf(PointDependency(2, listOf(0, 1), PointDependencyType.Midpoint, "midpoint", listOf(.5))),
            functions = listOf(FunctionDefinition("fn-1", "f(x)", "x^2-4*x+3", "cyan", false)),
            graphRowMetadata = mapOf("fn-1" to GraphRowMetadataState(true, "vertex | roots", "Quadratics")),
            graphSliderMetadata = mapOf("a" to GraphSliderMetadataState(1.75, GraphSliderPlaybackMode.Bounce, -1)),
            solids = listOf(Solid(SolidType.Frustum, 4.0, 5.0, 3.0, 2.0, .8, Vec3(1.0, 2.0, 3.0), Vec3(15.0, 25.0, 35.0))),
            vectors3D = listOf(Vector3D("v|1", Vec3(-1.0, .5, 2.0), Vec3(3.0, 4.0, 5.0), "force vector")),
            surfaceExpression = "sin(x)+cos(y)",
            spatialPlacement = SpatialScenePlacement("anchor-7", SpatialPose(Vec3(.1, .2, -.8), Vec3(10.0, 20.0, 30.0), 1.4), ARScaleMode.OneToOne, .25, TrackingQuality.Limited, false, true, .012, AnchorTrackingState.Relocalizing, .7f, "Move slowly", 12345L),
            modifiedAt = 987654321L,
        )
        val restored = WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(state), recover = false)
        assertTrue(restored.diagnostics.joinToString(), !restored.recovered)
        assertEquals(state, restored.state)
    }

    @Test fun damagedEnvelopeRecoversIndependentlyChecksummedWorkspace() {
        val source = WorkspaceProjectCodec.encode(WorkspaceState(modifiedAt = 42L))
        val damaged = source.replaceFirst(Regex("END\\|[a-f0-9]+"), "END|0000")
        val restored = WorkspaceProjectCodec.decode(damaged, recover = true)
        assertNotNull(restored.state)
        assertTrue(restored.recovered)
        assertTrue(restored.diagnostics.any { it.contains("checksum", ignoreCase = true) })
    }

    @Test fun trustedKernelMeetsPhaseOneGeneratedCorrectnessGate() {
        val kernel = TrustedMathKernel()
        val evidence = buildList {
            for (a in -10..10) for (b in -10..10) {
                add(kernel.equivalence("($a+$b)^2", "${a * a + 2 * a * b + b * b}"))
            }
        }
        val passRate = evidence.count { it.equivalent }.toDouble() / evidence.size
        assertTrue("Expected >=99.5% but got ${passRate * 100}%", passRate >= .995)
    }
}
