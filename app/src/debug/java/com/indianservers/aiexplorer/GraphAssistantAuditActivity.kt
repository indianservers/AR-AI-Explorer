package com.indianservers.aiexplorer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.indianservers.aiexplorer.assistant.workspace.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Separate debug source set: never packaged into the student release application. */
class GraphAssistantAuditActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                var outcomes by remember { mutableStateOf<List<GraphAuditOutcome>>(emptyList()) }
                var busy by remember { mutableStateOf(false) }
                var category by remember { mutableStateOf<String?>(null) }
                val scope=rememberCoroutineScope()
                fun run(cases:List<GraphAuditCase>) {
                    busy=true
                    scope.launch {
                        try {
                            val next=withContext(Dispatchers.Default) { cases.map(GraphAuditRunner::run) }
                            outcomes=(outcomes.filter { old -> next.none { it.case.id==old.case.id } }+next).sortedBy { it.case.id }
                            File(getExternalFilesDir(null),"ruhi-graph-audit.json").writeText(GraphAuditRunner.json(outcomes))
                        } finally { busy=false }
                    }
                }
                Surface(Modifier.fillMaxSize()) {
                    LazyColumn(Modifier.safeDrawingPadding().padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        item {
                            Text("Ruhi graph audit — developer only",style=MaterialTheme.typography.titleLarge)
                            Text("Total 150 · Recorded ${outcomes.size} · Failed ${outcomes.count { it.errors.isNotEmpty() }} · Unsupported ${outcomes.count { it.result.status==GraphCommandStatus.UNSUPPORTED }}")
                            Text("State verified ${outcomes.count { it.errors.isEmpty() && it.result.status!=GraphCommandStatus.UNSUPPORTED }} · Rendering skipped ${outcomes.count { it.rendering=="NOT_RUN" }}")
                            Text("No rendered passes are inferred here. Run RuhiGraphRenderingTest on a device for canvas pixels. No neural-network training occurs.")
                            val supported=outcomes.filter { it.case.expectedIntent!=null }
                            val parserCorrect=supported.count { it.result.actions.firstOrNull()?.intent==it.case.expectedIntent }
                            val context=supported.filter { it.case.id in setOf(23,27,82,88,89,90,119,121,124,127,129,133,134,145,146) }
                            Text("Supported parser accuracy: $parserCorrect/${supported.size}. Supported execution accuracy: ${supported.count { it.errors.isEmpty() }}/${supported.size}.")
                            Text("Context resolution: ${context.count { it.errors.isEmpty() }}/${context.size}. Rendering accuracy: unavailable until device run. Regression failures: ${outcomes.count { it.errors.isNotEmpty() }}.")
                            Text("Average command time: ${outcomes.map { it.result.elapsedNanos/1e6 }.average().takeIf { it.isFinite() } ?: 0.0} ms")
                            Row {
                                Button(enabled=!busy,onClick={ run(GraphAuditCases.all) }) { Text("Run all") }
                                Button(enabled=!busy,onClick={ run(outcomes.filter { it.errors.isNotEmpty() }.map { it.case }) }) { Text("Rerun failures") }
                            }
                        }
                        items(GraphAuditCases.all.map { it.category }.distinct()) { name ->
                            Row {
                                TextButton(onClick={ category=if(category==name)null else name }) { Text("$name (${outcomes.count { it.case.category==name && it.errors.isNotEmpty() }} failures)") }
                                TextButton(enabled=!busy,onClick={ run(GraphAuditCases.all.filter { it.category==name }) }) { Text("Run category") }
                            }
                        }
                        items(GraphAuditCases.all.filter { category==null || it.category==category }) { case ->
                            val outcome=outcomes.firstOrNull { it.case.id==case.id }
                            Column {
                                Text("${case.id}. ${case.command}")
                                Text("Expected: ${case.expectation()}")
                                Text("Actual: ${outcome?.result?.message ?: "Not run"}")
                                Text("${outcome?.status ?: "NOT_RUN"} ${outcome?.errors?.joinToString().orEmpty()}")
                                TextButton(enabled=!busy,onClick={ run(listOf(case)) }) { Text("Rerun ${case.id}") }
                            }
                        }
                    }
                }
            }
        }
    }
}
