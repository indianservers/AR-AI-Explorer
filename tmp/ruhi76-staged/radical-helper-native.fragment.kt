    @Test fun realRadicalDerivativeHelpersHaveFiniteSmoothZerosAndFollowSavedCurrentCurves() {
        TensorFlowGraphNlp.install(context)
        val inputs=listOf(Triple("x^2",0.0,0.0),Triple("sqrt(x^4)",0.0,0.0),Triple("sqrt(x^4)",2.0,4.0),Triple("sqrt((x-1.25)^4)",1.25,0.0),Triple("sqrt(x^2)-abs(x)+x",0.0,1.0),Triple("sqrt(x^2)",0.0,null),Triple("sqrt(x^6)",0.0,0.0),Triple("sqrt(x^2)",-2.0,-1.0))
        val records=JSONArray();val engine=ExpressionEngine()
        fun derivative(s:WorkspaceState)=RuhiDerivativeLanguage.drawing(s)!!.helpers.single{it.name=="Derivative"}.curve as RuhiIntersectionMath.Curve.Scalar
        for((formula,x,expected) in inputs) {
            val initial=WorkspaceState(module=MathModule.Graph2D,functions=emptyList(),surfaceLayers=emptyList());val r=RuhiDialogueManager().execute(initial,"Differentiate $formula",WorkspaceHistoryByModule())
            assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertEquals(initial.functions,r.state.functions);val c=derivative(r.state);val raw=engine.compile(c.expression).eval(mapOf("x" to x));val actual=raw.takeIf{it.isFinite()}
            if(expected==null)assertNull(actual)else assertEquals(formula,expected,actual!!,1e-10)
            records.put(JSONObject().put("formula",formula).put("x",x).put("expected",expected ?: JSONObject.NULL).put("actual",actual ?: JSONObject.NULL).put("passed",true))
        }
        ActivityScenario.launch<GraphVerificationActivity>(Intent(context,GraphVerificationActivity::class.java).putExtra("verify_ux_workspace","graph2d")).use{scenario->
            val matcher=hasContentDescription("Interactive graphing canvas with axes, curves, trace point, and annotations")
            compose.waitUntil(30_000){compose.onAllNodes(matcher,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
            val initial=WorkspaceState(module=MathModule.Graph2D,functions=listOf(FunctionDefinition("root-helper,/907","Root907","y=sqrt(x^4)","cyan")),surfaceLayers=emptyList())
            scenario.onActivity{it.verificationViewModel.importWorkspace(initial,false,emptyList())};compose.waitForIdle();val before=captureCanvas(matcher,"radical-helper-before")
            askConstruction(scenario,"Plot the derivative of the selected curve","Derivative")
            fun sampled(center:Double):Int {
                var count=0
                scenario.onActivity{val s=it.verificationViewModel.state;val c=derivative(s);assertEquals(0.0,engine.compile(c.expression).eval(mapOf("x" to center)),0.0);assertEquals(1,s.functions.size)
                    val native="y=${c.expression}"+(c.restriction?.let{" {$it}"} ?: "")
                    val sample=TypedGraphEngine().sample(TypedGraphExpressionParser.parse(native),GraphDomain(center-2,center+2),GraphDomain(-5.0,5.0,"y"),samples=256)
                    assertTrue(sample.curves.any{segment->segment.points.any{it.x<center}&&segment.points.any{it.x>center}});assertTrue(sample.curves.flatMap{it.points}.all{abs(it.y-2*(it.x-center))<1e-8});count=sample.curves.sumOf{it.points.size}}
                return count
            }
            val samples=sampled(0.0);val after=captureCanvas(matcher,"radical-helper-smooth-zero");val a=IntArray(before.width*before.height);val b=IntArray(a.size);before.getPixels(a,0,before.width,0,0,before.width,before.height);after.getPixels(b,0,after.width,0,0,after.width,after.height);val pixels=a.indices.count{a[it]!=b[it]};assertTrue(pixels>100)
            scenario.onActivity{val vm=it.verificationViewModel;vm.updateFunction(0){it.copy(expression="y=sqrt((x-1.25)^4)")};vm.importWorkspace(WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(vm.state)).state!!,false,emptyList())};compose.waitForIdle()
            sampled(1.25);captureCanvas(matcher,"radical-helper-edited").recycle();askConstruction(scenario,"Plot the derivative of the selected curve","Derivative");captureDistanceReply("Derivative","radical-helper-answer")
            File(folder,"radical-helper-canvas.json").writeText(JSONObject().put("passed",true).put("cases",8).put("results",records).put("actual_helper_values",true).put("true_cusp_preserved",true).put("original_curve_preserved",true).put("native_sampler_bridges_smooth_zero",true).put("current_saved_curve",true).put("sample_points",samples).put("changed_pixels",pixels).put("edited_zero",1.25).put("committed_capture",true).toString(2));before.recycle();after.recycle()
        }
    }
