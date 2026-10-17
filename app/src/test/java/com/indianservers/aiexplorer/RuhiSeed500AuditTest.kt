package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.assistant.workspace.*
import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import java.io.File
import java.util.Base64
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

/** All seed rows execute, but an APPLIED status alone never certifies the workbook behavior. */
class RuhiSeed500AuditTest {
    private val root:File=requireNotNull(generateSequence(File(requireNotNull(System.getProperty("user.dir")))){it.parentFile}.firstOrNull{File(it,"docs/qa/ruhi-500/replay.tsv").exists()})
    private fun decode(s:String)=String(Base64.getDecoder().decode(s),Charsets.UTF_8)
    private fun quote(s:String)="\""+s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","")+"\""
    @Test fun executeEverySeedAndRecordBehaviorGapsWithoutRewritingItsOracle() {
        val labels=('A'..'Z').map(Char::toString)+listOf("A1","B2","Alpha7","Beta9")
        val coordinates=labels.indices.map{i->Vec3(i.toDouble(),(i*i%17).toDouble(),(i%5).toDouble())}
        val cases=File(root,"docs/qa/ruhi-500/replay.tsv").readLines().map{it.split('\t')}
        assertEquals(500,cases.size)
        val results=cases.map{p->
            val state=RuhiSeed500Fixture.state(p[3],p[0].toInt());val context=RuhiSeed500Fixture.context(p[0].toInt(),state)
            val r=RuhiDialogueManager().execute(state,decode(p[1]),RuhiSeed500Fixture.history(p[0].toInt(),state),context)
            val check=RuhiSeedBehaviorOracle.check(p[2],RuhiSeedJson.objectValue(decode(p[6])),state,r,context)
            val verified=check.verified
            val unchangedOnFailure=r.status==GraphCommandStatus.APPLIED || (r.state.points==state.points&&r.state.points3D==state.points3D&&r.state.planes3D==state.planes3D&&r.state.shapes==state.shapes&&r.state.solids==state.solids&&r.state.functions==state.functions&&r.state.surfaceLayers==state.surfaceLayers)
            assertTrue("Row ${p[0]} mutated the scene on failure",unchangedOnFailure)
            "{\"id\":${p[0]},\"intent\":${quote(p[2])},\"status\":${quote(r.status.name)},\"behavior_verified\":$verified,\"diagnostics\":[${check.diagnostics.joinToString{quote(it)}}],\"message\":${quote(r.message)}}"
        }
        val phase=(System.getenv("RUHI_AUDIT_CYCLE") ?: "current").also{require(it.matches(Regex("[a-z0-9-]+")))}
        val file=File(root,"docs/qa/ruhi-500/$phase-scene-results.json")
        file.writeText("{\"total\":500,\"results\":[${results.joinToString()}],\"complete\":false,\"note\":\"Unverified behavior remains a gap, even when the old adapter reports APPLIED.\"}")
        assertEquals(500,results.size)
    }
}
