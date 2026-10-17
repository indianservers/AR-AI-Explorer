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
    @Test fun dependencyIdsAndConstructionArgumentsWithDelimitersRoundTripWithoutSplitting() {
        val first=com.indianservers.aiexplorer.workspace.UniversalMathObject("node,one",com.indianservers.aiexplorer.workspace.UniversalMathKind.Point2D,"First",com.indianservers.aiexplorer.workspace.UniversalMathPayload.Coordinates(listOf(0.0,0.0)))
        val second=first.copy(id="node|two",name="Second")
        val joined=com.indianservers.aiexplorer.workspace.UniversalMathObject("joined",com.indianservers.aiexplorer.workspace.UniversalMathKind.Segment,"Joined",com.indianservers.aiexplorer.workspace.UniversalMathPayload.Properties(mapOf("kind" to "segment")),setOf(first.id,second.id),definition=UniversalMathDefinition.Construction("Join",listOf(first.id,second.id,"literal, with | separators")))
        val source=UniversalMathDocument(objects=listOf(first,second,joined).associateBy{it.id});val decoded=UniversalMathDocumentCodec.decode(UniversalMathDocumentCodec.encode(source),recover=false)
        assertTrue(decoded.diagnostics.toString(),decoded.checksumValid&&decoded.document!=null);assertEquals(joined.dependencies,decoded.document!!.objects.getValue("joined").dependencies);assertEquals(joined.definition,decoded.document!!.objects.getValue("joined").definition)
    }
    @Test fun schemaThreeRawDependenciesAndLegacyConstructionArgumentsStillLoad() {
        val source=UniversalWorkspaceBridge.fromWorkspace(WorkspaceState(points=listOf(Vec2(0.0,0.0),Vec2(1.0,1.0)),shapes=listOf(Shape2D("edge",Shape2DType.Segment,listOf(0,1)))))
        val encoded=UniversalMathDocumentCodec.encode(source)
        fun pack(v:String)=java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(v.toByteArray(Charsets.UTF_8))
        fun unpack(v:String)=String(java.util.Base64.getUrlDecoder().decode(v),Charsets.UTF_8)
        val block=Regex("\\\"records\\\":\\[(.*?)]",RegexOption.DOT_MATCHES_ALL).find(encoded)!!.groupValues[1]
        val records=Regex("\\\"([^\\\"]+)\\\"").findAll(block).map{match->val f=match.groupValues[1].split('.').toMutableList();f[4]=pack(unpack(f[4]).split(',').filter{it.isNotBlank()}.joinToString(","){unpack(it)});f[8]="3";val d=unpack(f[9]).split('|');if(d.first()=="G4")f[9]=pack("G|"+d[1]+"|"+pack(unpack(d[2]).split(',').filter{it.isNotBlank()}.joinToString(","){unpack(it)}));f.joinToString(".")}.toList()
        val checksum=java.security.MessageDigest.getInstance("SHA-256").digest(records.joinToString("\n").toByteArray(Charsets.UTF_8)).joinToString(""){"%02x".format(it)}
        val legacy=encoded.replace("\"schemaVersion\":4","\"schemaVersion\":3").replace(Regex("\\\"checksum\\\":\\\"[a-f0-9]+\\\""),"\"checksum\":\"$checksum\"").replace(block,records.joinToString(","){"\"$it\""})
        val decoded=UniversalMathDocumentCodec.decode(legacy,recover=false)
        assertTrue(decoded.diagnostics.toString(),decoded.checksumValid&&decoded.document!=null);assertEquals(source.objects.getValue("edge").dependencies,decoded.document!!.objects.getValue("edge").dependencies);assertEquals(source.objects.getValue("edge").definition,decoded.document!!.objects.getValue("edge").definition)
    }
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

    @Test fun currentSchemaRoundTripPersistsAuthorityFields() {
        val source = UniversalWorkspaceBridge.fromWorkspace(WorkspaceState(functions=listOf(FunctionDefinition("f","f(x)","x^2","violet")))).objects.getValue("f").copy(
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
