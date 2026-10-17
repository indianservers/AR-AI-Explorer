package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.assistant.workspace.*
import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.Base64

class RuhiLexicalPointLabelTest {
    private val labels=listOf("distance","point","line","slope","and","from","midpoint","gradient","is","circle","triangle","angle","Vertex907","alpha","Δelta42","点甲","origin")
    private fun scene(module:MathModule,a:String,b:String="Omega811")=WorkspaceState(module=module,
        points=listOf(Vec2(1.0,2.0),Vec2(4.0,6.0)),points3D=listOf(Point3D("left",a,Vec3(1.0,2.0,3.0)),Point3D("right",b,Vec3(4.0,6.0,15.0))),
        labSessionValues=mapOf("ruhi.studio.point.$a" to "0","ruhi.studio.point.$b" to "1"))
    @Test fun lexicalNamesDoNotRedirectDistanceOrAddSlopeAnnotations() {
        for(module in listOf(MathModule.Geometry2D,MathModule.Geometry3D))for(a in labels)for(text in listOf("distance between $a and Omega811","how far is $a from Omega811")) {
            val initial=scene(module,a);val restored=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(initial)).state!!;val h=WorkspaceHistoryByModule()
            val r=RuhiDialogueManager().execute(restored,text,h)
            assertEquals("$module $text: ${r.message}",GraphCommandStatus.APPLIED,r.status)
            assertEquals("$module $text",if(module==MathModule.Geometry3D)13.0 else 5.0,(r.semanticFrame!!.parameters["distance_native"] as? Number)?.toDouble() ?: Double.NaN,1e-10)
            assertEquals(initial.points,r.state.points);assertEquals(initial.points3D,r.state.points3D);assertTrue(h.forModule(module).protocol.isEmpty())
        }
    }
    @Test fun intentNormalizationMasksEntityOccurrencesWithoutErasingOperationWords() {
        for(a in labels)for(template in listOf("distance between %s and Omega811","how far is %s from Omega811")) {
            assertEquals(template.format(a),RuhiSeedNlpInput.normalize(template.format("Vertex907")),RuhiSeedNlpInput.normalize(template.format(a)))
        }
        assertEquals("distance between entity and entity",RuhiSeedNlpInput.normalize("distance between distance and and"))
    }
    @Test fun tokenFeaturesAreInvariantWhenSceneLabelsAreAlsoLexicalTokens() {
        for(a in labels)for(template in listOf("distance between %s and Omega811","how far is %s from Omega811")) {
            val expected=RuhiPointSlotInput.tokens(template.format("Vertex907")).map{it.features}
            val actual=RuhiPointSlotInput.tokens(template.format(a))
            assertEquals(template.format(a),expected,actual.map{it.features})
            assertTrue(actual.any{it.value==a})
        }
    }
    @Test fun ordinarySlopeAndMidpointCommandsStillKeepTheirOwnOperations() {
        for(name in labels+"Left907") {
        val s=scene(MathModule.Geometry2D,name,"Omega811");val h=WorkspaceHistoryByModule();val manager=RuhiDialogueManager()
        val slope=manager.execute(s,"Find the slope between $name and Omega811",h)
        assertEquals(slope.message,GraphCommandStatus.APPLIED,slope.status);assertTrue(slope.message.contains("Slope = 1.3333333333333333"))
        assertEquals("CALCULATE_SLOPE",slope.semanticFrame!!.intent);assertEquals(s.points,slope.state.points);assertTrue(h.forModule(s.module).protocol.isEmpty())
        val midpoint=manager.execute(s,"Find the midpoint between $name and Omega811",WorkspaceHistoryByModule())
        assertEquals(midpoint.message,GraphCommandStatus.APPLIED,midpoint.status);assertTrue(midpoint.message.contains("2.5"));assertTrue(midpoint.message.contains("4.0"))
        assertEquals("FIND_MIDPOINT",midpoint.semanticFrame!!.intent);assertEquals(Vec2(2.5,4.0),midpoint.state.points.last())
        }
    }
    @Test fun all150OriginalDistanceRowsPreserveIntentAndMathForLexicalAndUnicodeLabels() {
        val root=requireNotNull(generateSequence(File(requireNotNull(System.getProperty("user.dir")))){it.parentFile}.firstOrNull{File(it,"docs/qa/ruhi-500/replay.tsv").exists()})
        val rows=File(root,"docs/qa/ruhi-500/replay.tsv").readLines().take(150);assertEquals(150,rows.size)
        for(row in rows)for(name in labels)for(module in listOf(MathModule.Geometry2D,MathModule.Geometry3D)) {
            val p=row.split('\t');val original=String(Base64.getDecoder().decode(p[1]),Charsets.UTF_8)
            val text=original.replace(Regex("\\b${Regex.escape(p[4])}\\b"),name).replace(Regex("\\b${Regex.escape(p[5])}\\b"),"Omega811")
            assertEquals("${p[0]} $name",RuhiSeedNlpInput.normalize(original),RuhiSeedNlpInput.normalize(text))
            val s=scene(module,name);val h=WorkspaceHistoryByModule();val r=RuhiDialogueManager().execute(s,text,h)
            assertEquals("${p[0]} $name ${r.message}",GraphCommandStatus.APPLIED,r.status)
            assertEquals(if(module==MathModule.Geometry3D)13.0 else 5.0,(r.semanticFrame!!.parameters["distance_native"] as Number).toDouble(),1e-10)
            assertEquals(s.points,r.state.points);assertEquals(s.points3D,r.state.points3D);assertTrue(h.forModule(module).protocol.isEmpty())
        }
    }
    @Test fun actualOriginNamedPointUsesSceneCoordinatesWhileAbsentOriginRemainsVirtual() {
        for(module in listOf(MathModule.Geometry2D,MathModule.Geometry3D)) {
            val manager=RuhiDialogueManager();val s=scene(module,"origin");val h=WorkspaceHistoryByModule()
            val r=manager.execute(s,"distance between origin and Omega811",h)
            assertEquals(if(module==MathModule.Geometry3D)13.0 else 5.0,(r.semanticFrame!!.parameters["distance_native"] as Number).toDouble(),1e-10)
            val q=manager.execute(s,"distance between Missing53 and Omega811",h)
            val chip=q.dialogue!!.suggestions.single{it.label=="Use origin for Missing53"}
            val chosen=manager.execute(WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(q.state)).state!!,chip.answer,h)
            assertEquals(GraphCommandStatus.APPLIED,chosen.status);assertTrue(chosen.message.contains(if(module==MathModule.Geometry3D)"= 13.0 units"else"= 5.0 units"))
            val absent=scene(module,"Anchor907");val virtual=manager.execute(absent,"distance between origin and Omega811",h)
            assertEquals(kotlin.math.sqrt(if(module==MathModule.Geometry3D)277.0 else 52.0),(virtual.semanticFrame!!.parameters["distance_native"] as Number).toDouble(),1e-10)
            assertTrue(h.forModule(module).protocol.isEmpty())
        }
    }
}
