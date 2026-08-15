package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.FunctionDefinition
import com.indianservers.aiexplorer.workspace.Shape2D
import com.indianservers.aiexplorer.workspace.Shape2DType
import com.indianservers.aiexplorer.workspace.UniversalMathDefinition
import com.indianservers.aiexplorer.workspace.UniversalMathDocument
import com.indianservers.aiexplorer.workspace.UniversalMathDocumentCodec
import com.indianservers.aiexplorer.workspace.UniversalMathPresentation
import com.indianservers.aiexplorer.workspace.UniversalMathValueStatus
import com.indianservers.aiexplorer.workspace.UniversalWorkspaceBridge
import com.indianservers.aiexplorer.workspace.WorkspaceState
import com.indianservers.aiexplorer.core.Vec2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UniversalGeometryGraphSchemaTest {
    @Test fun bridgePreservesGraphAndGeometryDefinitionsAndPresentation() {
        val state = WorkspaceState(
            functions = listOf(FunctionDefinition("f", "f(x)", "x^2", "violet", visible = false)),
            points = listOf(Vec2(0.0, 0.0), Vec2(2.0, 0.0)),
            shapes = listOf(Shape2D("segment", Shape2DType.Segment, listOf(0, 1), "AB", visible = true, locked = true, styleKey = "dashed")),
        )

        val document = UniversalWorkspaceBridge.fromWorkspace(state)
        val function = document.objects.getValue("f")
        val segment = document.objects.getValue("segment")

        assertEquals(UniversalMathDefinition.Symbolic("x^2"), function.definition)
        assertFalse(function.presentation.visible)
        assertEquals("violet", function.presentation.colorKey)
        assertEquals(UniversalMathDefinition.Construction("Segment", listOf("point-0", "point-1")), segment.definition)
        assertTrue(segment.presentation.locked)
        assertEquals("dashed", segment.presentation.styleKey)
    }

    @Test fun schemaThreeRoundTripPersistsAuthorityFields() {
        val source = UniversalWorkspaceBridge.fromWorkspace(WorkspaceState()).objects.getValue("f").copy(
            presentation = UniversalMathPresentation(false, "amber", "thick", true, 3, "quadratics", false),
        )
        val document = UniversalMathDocument(objects = mapOf(source.id to source))

        val encoded = UniversalMathDocumentCodec.encode(document)
        val recovery = UniversalMathDocumentCodec.decode(encoded)
        val decoded = recovery.document!!.objects.getValue("f")

        assertTrue(recovery.checksumValid)
        assertEquals(UniversalMathDocument.CURRENT_SCHEMA, recovery.document!!.schemaVersion)
        assertEquals(source.definition, decoded.definition)
        assertEquals(source.valueState, decoded.valueState)
        assertEquals(source.presentation, decoded.presentation)
    }

    @Test fun invalidSymbolicInputHasExplicitValueState() {
        val document = UniversalWorkspaceBridge.fromWorkspace(
            WorkspaceState(functions = listOf(FunctionDefinition("bad", "bad(x)", "sin(", "red"))),
        )

        val value = document.objects.getValue("bad")
        assertEquals(UniversalMathValueStatus.ParseError, value.valueState.status)
        assertFalse(value.valueState.usable)
        assertTrue(value.valueState.diagnostic?.isNotBlank() == true)
    }
}
