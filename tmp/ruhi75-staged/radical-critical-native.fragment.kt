    @Test fun realRadicalCriticalAndStationaryPointsPreserveDomainsAndFollowCurrentCurveEdits() {
        TensorFlowGraphNlp.install(context)
        val inputs=listOf(
            "Find critical points of y = abs(x)" to listOf(Vec2(0.0,0.0)),
            "Find critical points of y = sqrt(x^2)" to listOf(Vec2(0.0,0.0)),
            "Find critical points of y = sqrt((x-1.25)^2)+2*x" to listOf(Vec2(1.25,2.5)),
            "Find the stationary points of sqrt(x^2)" to emptyList(),
            "Find critical points of y = sqrt(x^4)" to listOf(Vec2(0.0,0.0)),
            "Find the stationary points of sqrt(x^4)" to listOf(Vec2(0.0,0.0)),
            "Find critical points of y = sqrt(x^2)-sqrt(x^2)+x" to emptyList(),
            "Find critical points of y = 1/sqrt(x^2)" to emptyList())
        val records=JSONArray()
        for((source,expected) in inputs) {
            val s=RuhiSeed500Fixture.state("GRAPH_2D",344);val r=RuhiDialogueManager().execute(s,source,WorkspaceHistoryByModule(),RuhiSeed500Fixture.context(344,s))
            assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertEquals(s.functions,r.state.functions);assertEquals(s.points,r.state.points)
            val actual=RuhiDerivativeLanguage.drawing(r.state)!!.points;assertEquals(source,expected.size,actual.size);assertTrue(source,expected.all{e->actual.any{it.distanceTo(e)<1e-6}})
            records.put(JSONObject().put("input",source).put("expected_points",JSONArray(expected.map{listOf(it.x,it.y)})).put("actual_points",JSONArray(actual.map{listOf(it.x,it.y)})).put("passed",true))
        }
        ActivityScenario.launch<GraphVerificationActivity>(Intent(context,GraphVerificationActivity::class.java).putExtra("verify_ux_workspace","graph2d")).use{scenario->
            val matcher=hasContentDescription("Interactive graphing canvas with axes, curves, trace point, and annotations")
            compose.waitUntil(30_000){compose.onAllNodes(matcher,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
            val initial=WorkspaceState(module=MathModule.Graph2D,functions=listOf(FunctionDefinition("radical,/907","Root907","y=sqrt(x^2)","cyan")),surfaceLayers=emptyList())
            scenario.onActivity{it.verificationViewModel.importWorkspace(initial,false,emptyList())};compose.waitForIdle()
            askConstruction(scenario,"Find critical points of the selected curve","Critical points")
            fun point(expected:Vec2) {
                scenario.onActivity{assertEquals(listOf(expected),RuhiDerivativeLanguage.drawing(it.verificationViewModel.state)!!.points);assertTrue(it.verificationViewModel.state.points.isEmpty())}
                val rendered=compose.onNode(matcher,useUnmergedTree=true).fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.StateDescription];assertTrue(rendered,rendered.contains("(${expected.x}, ${expected.y})"))
            }
            point(Vec2(0.0,0.0));captureCanvas(matcher,"radical-critical-corner").recycle()
            scenario.onActivity{val vm=it.verificationViewModel;vm.updateFunction(0){it.copy(expression="y=sqrt((x-2)^2)+2*x")};vm.importWorkspace(WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(vm.state)).state!!,false,emptyList())};compose.waitForIdle()
            point(Vec2(2.0,4.0));captureCanvas(matcher,"radical-critical-edited").recycle()
            askConstruction(scenario,"Find critical points of the selected curve","Critical points");captureDistanceReply("(2.0, 4.0)","radical-critical-answer")
            File(folder,"radical-critical-canvas.json").writeText(JSONObject().put("passed",true).put("cases",8).put("results",records).put("original_domain_preserved",true).put("native_current_curve",true).put("actual_rendered_coordinates",true).put("initial_point",JSONArray(listOf(0,0))).put("edited_point",JSONArray(listOf(2,4))).put("committed_capture",true).toString(2))
        }
    }
