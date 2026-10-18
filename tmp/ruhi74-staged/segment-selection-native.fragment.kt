    @Test fun selectedNativeSegmentRepliesResumeSavedMidpointAndSlopeWithoutEndpointSubstitution() {
        TensorFlowGraphNlp.install(context)
        ActivityScenario.launch<GraphVerificationActivity>(Intent(context,GraphVerificationActivity::class.java).putExtra("verify_ux_workspace","geometry2d")).use{scenario->
            val matcher=hasContentDescription("Interactive coordinate geometry canvas.",substring=true)
            compose.waitUntil(30_000){compose.onAllNodes(matcher,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
            val initial=WorkspaceState(module=MathModule.Geometry2D,points=listOf(Vec2(0.0,0.0),Vec2(6.0,8.0)),shapes=listOf(Shape2D("native-segment,/907",Shape2DType.Segment,listOf(0,1),"SunMoon907")),labSessionValues=mapOf("ruhi.studio.point.Sun907" to "0","ruhi.studio.point.Moon811" to "1"))
            fun reset(){scenario.onActivity{val vm=it.verificationViewModel;vm.importWorkspace(initial,false,emptyList());vm.selectShape(-1)};compose.waitForIdle()}
            fun restore(){scenario.onActivity{val vm=it.verificationViewModel;vm.importWorkspace(WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(vm.state)).state!!,false,emptyList());vm.selectShape(-1)};compose.waitForIdle()}
            reset();askConstruction(scenario,"Mark the midpoint of the selected segment","Select a line segment");restore()
            scenario.onActivity{val vm=it.verificationViewModel;assertEquals("endpoint_shape",RuhiDialogueMemory.read(vm.state).pending!!.kind);vm.movePoint(1,Vec2(4.0,6.0))};compose.waitForIdle()
            compose.onNodeWithContentDescription("Open AI Assistant").performClick();awaitRuhiInput();compose.onAllNodesWithText("SunMoon907",substring=false).onLast().performScrollTo().assertIsDisplayed();captureCanvas(matcher,"segment-selection-question").recycle();compose.onAllNodesWithText("SunMoon907",substring=false).onLast().performClick()
            compose.waitUntil(30_000){var done=false;scenario.onActivity{done=RuhiDialogueMemory.read(it.verificationViewModel.state).turns.last().assistant.contains("Midpoint Mid1 = (2.0, 3.0)")};done}
            androidx.test.espresso.Espresso.closeSoftKeyboard();compose.onNodeWithContentDescription("Close AI Assistant").performClick();compose.waitForIdle()
            scenario.onActivity{val vm=it.verificationViewModel;assertEquals(Vec2(2.0,3.0),vm.state.points.last());assertEquals(listOf(0,1),vm.state.pointDependencies.single().inputIndices);assertEquals(initial.shapes,vm.state.shapes);vm.undo();assertEquals(listOf(Vec2(0.0,0.0),Vec2(4.0,6.0)),vm.state.points);vm.redo();assertEquals(Vec2(2.0,3.0),vm.state.points.last())};compose.waitForIdle()
            captureCanvas(matcher,"segment-selection-midpoint").recycle()
            scenario.onActivity{val vm=it.verificationViewModel;vm.movePoint(1,Vec2(2.0,4.0));assertEquals(Vec2(1.0,2.0),vm.state.points.last())};restore();captureCanvas(matcher,"segment-selection-midpoint-live").recycle()
            reset();askConstruction(scenario,"Measure the slope of the selected line","Select a line segment");restore();scenario.onActivity{it.verificationViewModel.movePoint(1,Vec2(4.0,6.0))};compose.waitForIdle()
            askConstruction(scenario,"SunMoon907","Slope = 1.5; rise = 6.0, run = 4.0")
            scenario.onActivity{val vm=it.verificationViewModel;assertEquals(2,vm.state.points.size);assertEquals(initial.shapes,vm.state.shapes);assertTrue(vm.state.pointDependencies.isEmpty());assertNull(RuhiDialogueMemory.read(vm.state).pending)}
            captureDistanceReply("Slope = 1.5; rise = 6.0, run = 4.0","segment-selection-slope-answer")
            scenario.onActivity{val vm=it.verificationViewModel;vm.undo();assertEquals(initial.points,vm.state.points)}
            File(folder,"segment-selection-canvas.json").writeText(JSONObject().put("passed",true).put("source_rows",JSONArray(listOf(162,179))).put("saved_object_chip",true).put("plain_name_reply",true).put("native_shape_id","native-segment,/907").put("original_target_roles",true).put("current_coordinates",true).put("midpoint",JSONArray(listOf(2,3))).put("edited_midpoint",JSONArray(listOf(1,2))).put("native_parent_dependency",true).put("atomic_undo_redo",true).put("slope",1.5).put("slope_query_no_undo",true).put("shapes_preserved",true).put("committed_capture",true).toString(2))
        }
    }
