package com.indianservers.aiexplorer.ar3dgraph.integration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GraphEngineAdapterTest {
    private val request = GraphGenerationRequest(
        equations = listOf(GraphEquationRequest("one", "z=x+2*y")),
        domainMinimum = -4.0,
        domainMaximum = 5.0,
        density = 8,
    )

    @Test fun disconnectedAdapterReportsControlledError() {
        val adapter = GraphEngineAdapter()
        assertFalse(adapter.isConnected())
        assertTrue(adapter.generate(request) is ARGraphAdapterResult.GenerationError)
    }

    @Test fun preservesVerticesIndicesBoundsColoursOpacityAndSettings() {
        val source = EngineMeshSnapshot(
            equationId = "one",
            canonicalEquation = "z = x + 2*y",
            vertices = listOf(
                EngineVector3(-1.0, -2.0, 0.0),
                EngineVector3(1.0, -2.0, 1.0),
                EngineVector3(-1.0, 2.0, 2.0),
                EngineVector3(1.0, 2.0, 3.0),
            ),
            rows = 2,
            columns = 2,
            triangleIndices = listOf(0, 2, 1, 1, 2, 3),
            palette = listOf(EngineColor(1f, 0f, 0f), EngineColor(0f, 0f, 1f)),
            lineColor = EngineColor(1f, 1f, 1f),
            opacity = .5f,
        )
        var calls = 0
        val adapter = GraphEngineAdapter { received ->
            calls++
            assertEquals(request, received)
            EngineGraphResult.Success(received, listOf(source))
        }

        val output = (adapter.generate(request) as ARGraphAdapterResult.Success).data
        assertEquals(1, calls)
        assertEquals(source.vertices.map { ARVector3(it.x, it.y, it.z) }, output.meshes.single().vertices)
        assertEquals(source.triangleIndices, output.meshes.single().indices)
        assertEquals(4, output.meshes.single().normals.size)
        assertEquals(.5f, output.meshes.single().colors.first().alpha)
        assertEquals(ARVector3(-1.0, -2.0, 0.0), output.bounds.minimum)
        assertEquals(ARVector3(1.0, 2.0, 3.0), output.bounds.maximum)
        assertEquals(-4.0, output.domainMinimum, 0.0)
        assertEquals(5.0, output.domainMaximum, 0.0)
        assertEquals(8, output.density)
    }

    @Test fun regularGridTopologyIsRepresentationOnlyAndDeterministic() {
        val source = source("grid", 0.0).copy(triangleIndices = emptyList())
        val output = (GraphEngineAdapter { EngineGraphResult.Success(request, listOf(source)) }
            .generate(request) as ARGraphAdapterResult.Success).data.meshes.single()
        assertEquals(listOf(0, 2, 1, 1, 2, 3), output.indices)
        assertTrue(output.normals.all { it.finite })
    }

    @Test fun multipleEquationIdentityAndOrderArePreserved() {
        val output = (GraphEngineAdapter {
            EngineGraphResult.Success(it, listOf(source("first", 0.0), source("second", 1.0)))
        }.generate(request) as ARGraphAdapterResult.Success).data
        assertEquals(listOf("first", "second"), output.meshes.map(ARMeshRenderData::equationId))
    }

    @Test fun validationAndEngineFailuresRemainStructured() {
        assertTrue(GraphEngineAdapter { EngineGraphResult.ValidationError("bad") }.generate(request) is ARGraphAdapterResult.ValidationError)
        assertTrue(GraphEngineAdapter { EngineGraphResult.GenerationError("failed") }.generate(request) is ARGraphAdapterResult.GenerationError)
        assertTrue(GraphEngineAdapter { error("injected engine exception") }.generate(request) is ARGraphAdapterResult.GenerationError)
    }

    @Test fun emptyMeshesInvalidIndicesNonFiniteVerticesAndMissingPaletteNeverReachRenderer() {
        val empty = source("empty", 0.0).copy(vertices = emptyList())
        val invalidIndices = source("indices", 0.0).copy(triangleIndices = listOf(0, 1, 99))
        val nonFinite = source("finite", 0.0).copy(
            vertices = listOf(EngineVector3(Double.NaN, 0.0, 0.0)),
            rows = 1,
            columns = 1,
            triangleIndices = listOf(0, 0, 0),
        )
        val noPalette = source("palette", 0.0).copy(palette = emptyList())
        listOf(empty, invalidIndices, nonFinite, noPalette).forEach { mesh ->
            val result = GraphEngineAdapter { EngineGraphResult.Success(request, listOf(mesh)) }.generate(request)
            assertTrue(result is ARGraphAdapterResult.GenerationError)
        }
    }

    @Test fun unsafeArAllocationEstimateIsRejectedBeforeTopologyAllocation() {
        val oversized = source("oversized", 0.0).copy(
            rows = 2_000,
            columns = 2_000,
            triangleIndices = emptyList(),
        )
        val result = GraphEngineAdapter { EngineGraphResult.Success(request, listOf(oversized)) }.generate(request)
        assertTrue(result is ARGraphAdapterResult.GenerationError)
        assertTrue((result as ARGraphAdapterResult.GenerationError).message.contains("too large"))
    }

    private fun source(id: String, offset: Double) = EngineMeshSnapshot(
        id, id,
        listOf(
            EngineVector3(0.0, 0.0, offset), EngineVector3(0.0, 1.0, offset),
            EngineVector3(1.0, 0.0, offset), EngineVector3(1.0, 1.0, offset),
        ),
        2, 2, listOf(0, 2, 1, 1, 2, 3),
        listOf(EngineColor(1f, 1f, 1f)), EngineColor(1f, 1f, 1f), 1f,
    )
}
