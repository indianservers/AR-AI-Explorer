package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.assistant.workspace.*
import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.Base64
import kotlin.math.*

class RuhiDialogueScenarioTest {
    @Test fun all1100GeneratedConversationsExecuteAgainstRealSceneModels() {
        val dataset=File("../docs/qa/ruhi-dialogue-evidence/test-replay.tsv")
        assertTrue(dataset.absolutePath,dataset.isFile)
        val scenarios=dataset.readLines().groupBy{it.substringBefore('\t')}
        assertEquals(1100,scenarios.size)
        var turns=0
        scenarios.forEach{(id,rows)->
            val module=MathModule.valueOf(rows.first().split('\t')[1])
            val manager=RuhiDialogueManager();val histories=WorkspaceHistoryByModule()
            var state=WorkspaceState(module=module,surfaceLayers=emptyList());var context=RuhiSceneContext()
            rows.forEachIndexed{index,line->
                val fields=line.split('\t');val text=String(Base64.getDecoder().decode(fields[2]),Charsets.UTF_8)
                val r=manager.execute(state,text,histories,context)
                val label="$id turn $index: $text -> ${r.message}"
                assertEquals(label,GraphCommandStatus.valueOf(fields[6]),r.status)
                state=r.state;context=r.context;turns++
                assertEquals(label,module,state.module)
                assertEquals(label,fields[3].toBoolean(),RuhiDialogueMemory.read(state).pending!=null)
                fields[4].split(',').forEach{entry->val (key,value)=entry.split('=');val count=when(key){"functions"->state.functions.size;"surfaceLayers"->state.surfaceLayers.size;"shapes"->state.shapes.size;else->state.solids.size};assertEquals(label,value.toInt(),count)}
                if(fields.size>5 && fields[5].isNotBlank()) fields[5].split(';').forEach{entry->
                    val (key,value)=entry.split('=',limit=2)
                    when(key) {
                        "f0"->assertEquals(label,value.toDouble(),ExpressionEngine().compile(stripEquation(state.functions.single().expression)).eval(mapOf("x" to 0.0)),1e-7)
                        "color"->assertEquals(label,value,state.functions.single().colorKey)
                        "radius"->assertEquals(label,value.toDouble(),state.solids.single().radius,1e-8)
                        "height"->assertEquals(label,value.toDouble(),state.solids.single().height,1e-8)
                        "volume"->assertEquals(label,value.toDouble(),Geometry3D.measure(state.solids.single()).volume,1e-7)
                        "appearance"->assertEquals(label,value,state.labSessionValues["ruhi.studio.appearance.0"])
                        "scale"->assertEquals(label,value.toDouble(),state.surfaceLayers.single().graphTransform.scale.x,1e-8)
                        "side"->{val p=state.shapes.single().pointIndices.map(state.points::get);p.indices.forEach{i->assertEquals(label,value.toDouble(),p[i].distanceTo(p[(i+1)%p.size]),1e-7)}}
                        "angles"->assertTrue(label,r.message.contains("60.00°"))
                        "analysis"->assertTrue(label,state.labSessionValues.containsKey("ruhi.dialogue.graph.markers"))
                    }
                }
            }
        }
        File("build/reports/ruhi-dialogue-scenarios.json").apply{parentFile.mkdirs()}.writeText("{\"scenarios\":${scenarios.size},\"turns\":$turns,\"scene_assertions_passed\":true,\"rendering\":\"NOT_RUN\"}")
    }
}
