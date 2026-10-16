package com.indianservers.aiexplorer.assistant.workspace

import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*

data class PreparedGraphCase(val case: GraphAuditCase, val before: WorkspaceState, val history: CommandHistory)
data class GraphAuditOutcome(
    val case: GraphAuditCase, val result: GraphCommandResult, val before: WorkspaceState,
    val assertions: List<String>, val errors: List<String>, val meshVertices: Int,
    val rendering: String = "NOT_RUN", val changedPixels: Int? = null,
) {
    val status: String get() = when {
        errors.isNotEmpty() || rendering == "FAIL" -> "FAILED"
        result.status == GraphCommandStatus.UNSUPPORTED -> "UNSUPPORTED"
        rendering == "PASS" -> "PASSED"
        else -> "SKIPPED_RENDERING"
    }
    fun json(): String = """{"id":${case.id},"category":${quote(case.category)},"command":${quote(case.command)},"setup":${strings(case.setup)},"expected_intent":${quote(case.expectedIntent?.name ?: case.expectedStatus.name)},"expected_parameters":${quote(case.expectation())},"actual_parser":${strings(result.actions.map { it.toString() })},"expected_count_delta":${case.expectedCountDelta},"before":${snapshot(before)},"after":${snapshot(result.state)},"execution":${quote(result.status.name)},"message":${quote(result.message)},"assertions":${strings(assertions)},"errors":${strings(errors)},"mesh_vertices":$meshVertices,"rendering":${quote(rendering)},"changed_pixels":${changedPixels ?: "null"},"status":${quote(status)},"elapsed_ms":${result.elapsedNanos / 1e6},"failure_category":${quote(if (errors.isNotEmpty()) "PARSER_OR_STATE" else if (rendering == "FAIL") "RENDERING" else if (result.status == GraphCommandStatus.UNSUPPORTED) "UNSUPPORTED_CAPABILITY" else if (rendering == "NOT_RUN") "RENDER_NOT_EXECUTED" else "NONE")}}"""
}

/** Debug-only audit. A mesh assertion is deliberately not called a rendering assertion. */
object GraphAuditRunner {
    fun prepare(case: GraphAuditCase): PreparedGraphCase {
        val assistant = GraphAssistant()
        val history = CommandHistory()
        var state = WorkspaceState(module = MathModule.Graph3D, surfaceExpression = "0", surfaceLayers = emptyList())
        case.setup.forEach { command ->
            val result = assistant.execute(state, command, history)
            check(result.status == GraphCommandStatus.APPLIED) { "Case ${case.id} precondition failed: $command: ${result.message}" }
            state = result.state
        }
        return PreparedGraphCase(case, state, history)
    }
    fun run(case: GraphAuditCase): GraphAuditOutcome {
        val prepared = prepare(case)
        val result = GraphAssistant().execute(prepared.before, case.command, prepared.history)
        return verify(prepared, result)
    }
    fun verify(prepared: PreparedGraphCase, result: GraphCommandResult): GraphAuditOutcome {
        val case = prepared.case
        val before = prepared.before
        val after = result.state
        val checks = mutableListOf<String>()
        val errors = mutableListOf<String>()
        fun check(label: String, valid: Boolean) { if (valid) checks += label else errors += label }
        check("Expected ${case.expectedStatus}, got ${result.status}", result.status == case.expectedStatus)
        check("Expected count delta ${case.expectedCountDelta}", after.surfaceLayers.size - before.surfaceLayers.size == case.expectedCountDelta)
        check("2D content and module preserved", before.functions == after.functions && before.module == after.module)
        check("Stable unique object IDs", after.surfaceLayers.map { it.id }.distinct().size == after.surfaceLayers.size)
        if (case.expectedIntent != null) check("Expected structured intent ${case.expectedIntent}", result.actions.firstOrNull()?.intent == case.expectedIntent)
        if (result.status != GraphCommandStatus.APPLIED) check("Rejected instruction leaves complete scene unchanged", before == after)
        fun selected() = after.surfaceLayers.firstOrNull { it.id == after.labSessionValues[GraphAssistant.SELECTION] }
        when (case.id) {
            20 -> {
                val bounds=after.surfaceLayers.map { layer -> GraphSurfaceGeometry.mesh(layer).vertices.let { points -> points.minOf { it.x } to points.maxOf { it.x } } }
                check("Side-by-side surface bounds do not overlap",bounds.size==2 && bounds[0].second<bounds[1].first)
            }
            121 -> check("Compound creation then appearance resolves the new cube", after.surfaceLayers.singleOrNull()?.colorHex=="#0000FF" && after.labSessionValues[GraphAssistant.TYPE_PREFIX+after.surfaceLayers.single().id]=="cube")
            129 -> check("Compound deletion retains the sphere", after.surfaceLayers.singleOrNull()?.let { after.labSessionValues[GraphAssistant.TYPE_PREFIX+it.id]=="sphere" }==true)
            24 -> check("Clear preserves grid preference and view", after.labSessionValues["ruhi.graph.grid"] == "false" && after.graph3DView == before.graph3DView)
            26 -> check("Reset restores default grid and view", after.labSessionValues["ruhi.graph.grid"] != "false" && after.graph3DView == Graph3DViewState())
            21 -> check("Only cubes deleted", after.surfaceLayers.size == 1 && after.labSessionValues[GraphAssistant.TYPE_PREFIX + after.surfaceLayers.single().id] == "sphere")
            27 -> check("Last-created identity removed", after.surfaceLayers.map { it.id } == before.surfaceLayers.dropLast(1).map { it.id })
            28 -> check("Cylinder removed, sphere retained", after.surfaceLayers.singleOrNull()?.let { after.labSessionValues[GraphAssistant.TYPE_PREFIX + it.id] == "sphere" } == true)
            60 -> check("Sphere has radius 10 and an enclosing domain", selected()?.expression == "x^2+y^2+z^2=100.0" && (selected()?.domain?.uMax ?: 0.0) > 10)
            76 -> check("Cube is exact red", selected()?.colorHex == "#FF0000")
            77 -> check("All spheres are exact blue", after.surfaceLayers.all { it.colorHex == "#0000FF" })
            78 -> check("Transparency enabled", selected()?.opacity == .35)
            79 -> check("Cone opacity is 50%", selected()?.opacity == .5)
            80 -> check("Metal material applied", selected()?.material == SpatialMaterial.Metal)
            81 -> check("Glass material applied", selected()?.material == SpatialMaterial.Glass)
            82 -> check("Selected color is green", selected()?.colorHex == "#008000")
            84 -> check("Wireframe applied", selected()?.renderMode == SpatialSurfaceRenderMode.Wireframe)
            85 -> check("Sphere hidden", selected()?.visible == false)
            86 -> check("Hidden sphere shown", after.surfaceLayers.all { it.visible })
            87 -> check("Pyramid yellow", selected()?.colorHex == "#FFFF00")
            88 -> check("Selection highlighted visibly", selected()?.colorHex == "#FFFF00")
            89 -> check("Highlight restores original color", selected()?.colorHex == "")
            90 -> check("Default material restored", selected()?.material == SpatialMaterial.Gloss)
            91 -> check("Paraboloid equation retained", selected()?.expression?.replace(" ", "") == "x^2+y^2")
            94 -> check("Implicit sphere retained", selected()?.kind == SpatialSurfaceKind.Implicit)
            102 -> check("Grid hidden independently", after.labSessionValues["ruhi.graph.grid"] == "false")
            103 -> check("Grid shown again", after.labSessionValues["ruhi.graph.grid"] == "true")
            119 -> check("Duplicate has a distinct ID and visible translation", after.surfaceLayers.size == 2 && after.surfaceLayers[0].id != after.surfaceLayers[1].id && after.surfaceLayers[1].expression.contains("x-0.8") && after.surfaceLayers[0].material == after.surfaceLayers[1].material)
            124 -> check("First cube removed, second identity retained", after.surfaceLayers.map { it.id } == before.surfaceLayers.drop(1).map { it.id })
            127 -> check("Pronoun resolves to selected sphere", selected()?.colorHex == "#800080")
            133,134,146 -> check("History restores exact empty scene", after.surfaceLayers.isEmpty())
        }
        if(result.status==GraphCommandStatus.APPLIED) {
            fun near(a:Double,b:Double)=kotlin.math.abs(a-b)<1e-6
            fun position(v:Vec3)=selected()?.graphTransform?.position?.let { (it-v).magnitude()<1e-6 }==true
            fun scale(v:Vec3)=selected()?.graphTransform?.scale?.let { (it-v).magnitude()<1e-6 }==true
            fun bounds(layer:SpatialSurfaceLayer)=GraphSurfaceGeometry.mesh(layer).vertices.let { p->Vec3(p.maxOf{it.x}-p.minOf{it.x},p.maxOf{it.y}-p.minOf{it.y},p.maxOf{it.z}-p.minOf{it.z}) }
            when(case.id) {
                29->check("Only grouped members deleted",after.surfaceLayers.size==1 && after.labSessionValues[GraphAssistant.TYPE_PREFIX+after.surfaceLayers.single().id]=="sphere")
                31->check("Absolute coordinates exact",position(Vec3(3.0,2.0,1.0)))
                32->check("Right is positive X",position(Vec3(5.0,0.0,0.0)))
                33->check("Up is positive Z",position(Vec3(0.0,0.0,2.0)))
                34->check("Backward is positive Y",near(selected()!!.graphTransform.position.y,3.0))
                35,39->check("Center at origin",position(Vec3(0.0,0.0,0.0)))
                36->check("Every cube shifted left exactly two",before.surfaceLayers.zip(after.surfaceLayers).all { (a,b)->(b.graphTransform.position-a.graphTransform.position-Vec3(-2.0,0.0,0.0)).magnitude()<1e-6 })
                37,38->check("Relative placement separated by a gap",after.surfaceLayers.last().graphTransform.position.let { if(case.id==37) it.z>1.0 else it.x>1.0 })
                40->check("Vector translation exact",position(Vec3(2.0,3.0,4.0)))
                42->check("All centers on Y axis",after.surfaceLayers.all { near(it.graphTransform.position.x,0.0) && near(it.graphTransform.position.z,0.0) })
                43,44->check("Even X spacing",after.surfaceLayers.zipWithNext().all { (a,b)->near(b.graphTransform.position.x-a.graphTransform.position.x,5.0) })
                45->check("Every surface lies in positive X",after.surfaceLayers.all { GraphSurfaceGeometry.mesh(it).vertices.all { p->p.x>0 } })
                46->check("Uniform scale doubled",scale(Vec3(2.0,2.0,2.0)))
                47->check("Uniform scale halved",scale(Vec3(.5,.5,.5)))
                49->check("Cube side is five",bounds(selected()!!).let { (it-Vec3(5.0,5.0,5.0)).magnitude()<.05 })
                50->check("Cone height is eight",near(selected()!!.graphTransform.scale.z,4.0) && kotlin.math.abs(bounds(selected()!!).z-8)<.3)
                51->check("Every layer scaled by 1.5",after.surfaceLayers.all { (it.graphTransform.scale-Vec3(1.5,1.5,1.5)).magnitude()<1e-8 })
                52->check("Only local width doubled",scale(Vec3(2.0,1.0,1.0)))
                53->check("Sphere diameter is six",(bounds(selected()!!)-Vec3(6.0,6.0,6.0)).magnitude()<.05)
                54->check("Cylinder diameter four, height preserved",bounds(selected()!!).let { kotlin.math.abs(it.x-4)<.05 && kotlin.math.abs(it.y-4)<.05 && kotlin.math.abs(it.z-2)<.05 })
                57->check("Local dimensions 4 by 5 by 6",(bounds(selected()!!)-Vec3(4.0,5.0,6.0)).magnitude()<.05)
                58->check("Cube scales normalized equally",after.surfaceLayers.map { it.graphTransform.scale }.distinct().size==1)
                59->check("Original scale restored",scale(Vec3(1.0,1.0,1.0)))
                61,62,63,64,67,68,74,128->check("Orientation updated on resolved layers",after.surfaceLayers.filter { it.graphTransform.orientation!=GraphTransform().orientation }.isNotEmpty())
                69,70->check("Orientation reset to Z-up",selected()?.graphTransform?.orientation==GraphTransform().orientation)
                108->check("Cubes vertically stacked",after.surfaceLayers.map{it.graphTransform.position.z}==listOf(0.0,2.0,4.0))
                117->check("Real persistent group membership",after.surfaceLayers.map{it.graphGroup}.distinct().singleOrNull()?.isNotEmpty()==true)
                118->check("Group removed without deleting members",after.surfaceLayers.all{it.graphGroup.isEmpty()} && after.surfaceLayers.map{it.id}==before.surfaceLayers.map{it.id})
                120->check("Reflected center across YZ",position(Vec3(-3.0,0.0,0.0)))
                122->check("Spheres moved apart",after.surfaceLayers.map{it.graphTransform.position.x}==listOf(-5.0,5.0))
                126->check("Pronoun translated upward",position(Vec3(0.0,0.0,3.0)))
                130->check("Only third cube green",after.surfaceLayers.map{it.colorHex}==listOf("","","#008000","",""))
                131->check("Smallest sphere selected",selected()?.id==after.surfaceLayers.last().id)
                132->check("Red cube survives exclusion deletion",after.surfaceLayers.singleOrNull()?.colorHex=="#FF0000")
                149->check("Reset, creation and translation committed together",after.surfaceLayers.size==1 && position(Vec3(5.0,5.0,5.0)))
            }
        }
        var vertices = 0
        if (result.status == GraphCommandStatus.APPLIED) after.surfaceLayers.forEach { layer ->
            try {
                val mesh = GraphSurfaceGeometry.mesh(layer, 24)
                vertices += mesh.vertices.size
                check("${layer.id}: real finite nonempty geometry", mesh.vertices.isNotEmpty() && mesh.vertices.all { it.x.isFinite() && it.y.isFinite() && it.z.isFinite() })
                check("${layer.id}: valid topology", if (mesh.triangleIndices.isNotEmpty()) mesh.triangleIndices.size % 3 == 0 && mesh.triangleIndices.all { it in mesh.vertices.indices } else mesh.rows > 1 && mesh.rows * mesh.columns == mesh.vertices.size)
                if (case.id == 91) check("Paraboloid samples satisfy z=x²+y²", mesh.vertices.all { kotlin.math.abs(it.z - (it.x*it.x + it.y*it.y)) < 1e-8 })
                if (case.id == 144) check("Pole omitted rather than fabricated", mesh.triangleIndices.isNotEmpty() && mesh.vertices.none { kotlin.math.abs(it.x) < 1e-12 && kotlin.math.abs(it.y) < 1e-12 })
            } catch (error: RuntimeException) { errors += "Mesh failure: ${error.message}" }
        }
        return GraphAuditOutcome(case, result, before, checks, errors, vertices)
    }
    fun json(outcomes: List<GraphAuditOutcome>): String = """{"schema_version":1,"pipeline":"Ruhi GraphAssistant.execute → WorkspaceState → GraphSurfaceGeometry → SurfaceCanvas3D","model_training":"Offline TensorFlow intent classifier trained on authored synthetic instructions; no student-data training","results":[${outcomes.joinToString(",") { it.json() }}]}"""
}

fun GraphAuditCase.expectation(): String = when(expectedIntent) {
    GraphIntent.PLOT -> "Create the requested number of drawable layers from the supplied mathematical definitions"
    GraphIntent.DELETE,GraphIntent.CLEAR,GraphIntent.RESET -> "Remove only the specified graph content; preserve unrelated workspace content"
    GraphIntent.UNDO,GraphIntent.REDO -> "Restore workspace snapshot, including materials, visibility, and selection"
    else -> if(expectedStatus == GraphCommandStatus.UNSUPPORTED) "Explicit unsupported result; no scene mutation" else if(expectedStatus == GraphCommandStatus.INVALID) "Reject invalid instruction without changing state" else "Apply the requested property to the resolved graph-layer IDs"
}
private fun quote(value: String): String = "\"" + value.flatMap { char -> when(char) { '"' -> "\\\""; '\\' -> "\\\\"; '\n' -> "\\n"; '\r' -> "\\r"; '\t' -> "\\t"; else -> char.toString() }.toList() }.joinToString("") + "\""
private fun strings(values: List<String>) = values.joinToString(",", "[", "]", transform = ::quote)
private fun snapshot(state: WorkspaceState): String = """{"count":${state.surfaceLayers.size},"selected":${quote(state.labSessionValues[GraphAssistant.SELECTION].orEmpty())},"layers":${strings(state.surfaceLayers.map { it.toString() })},"settings":${quote(state.graph3DView.toString())},"metadata":${quote(state.labSessionValues.toSortedMap().toString())}}"""
