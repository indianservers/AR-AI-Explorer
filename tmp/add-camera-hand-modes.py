from pathlib import Path
p=Path('app/src/main/java/com/indianservers/aiexplorer/MainActivity.kt')
s=p.read_text(encoding='utf-8')
s=s.replace('var handsEnabled by rememberSaveable { mutableStateOf(false) }','var cameraHandsOnly by rememberSaveable { mutableStateOf(false) }\n    var handsEnabled by rememberSaveable { mutableStateOf(false) }',1)
s=s.replace('fun startLiveAr(userRequestedInstall: Boolean = true) {','fun startLiveAr(userRequestedInstall: Boolean = true) {\n        if (cameraHandsOnly) return',1)
s=s.replace('if (granted) startLiveAr(userRequestedInstall = true)','if (granted && !cameraHandsOnly) startLiveAr(userRequestedInstall = true)',1)
s=s.replace('        val camera = arFrame\n        val observation', '        val camera = arFrame\n        val observation',1)
needle='        val reliable = handsEnabled && graphSheet==null'
idx=s.index(needle)
s=s[:idx]+'''        if (cameraHandsOnly) {
            val reliable = handsEnabled && handCameraActive && cameraGranted && graphSheet == null && !showArAddOptions && !graphTouchActive &&
                android.os.SystemClock.uptimeMillis()-result.timestampMillis in 0..350
            val tool = if (observation.gesture == ArRecognizedGesture.TWO_HAND_SCALE && handTool == ArHandTool.Move) ArHandTool.Scale else handTool
            val action = handController.update(result, tool, reliable, viewportSize.width.toFloat()/viewportSize.height.coerceAtLeast(1))
            handCursor = action.cursor
            when (action.phase) {
                ArHandPhase.Begin -> {
                    val target = arSelection.primaryObjectId ?: presentedScene.primitives.firstOrNull { it.visible && it.selectable }?.id
                    if (arWorkspaceMode == ArMathWorkspaceMode.Graph3D && target == null) { handStatus = "Create or select an object first"; return }
                    handTargetId = target
                    val solid = target?.removePrefix("solid-")?.toIntOrNull()?.takeIf(vm.state.solids.indices::contains)
                    val shape = vm.state.shapes.indexOfFirst { it.id == target }.takeIf { it >= 0 }
                    if (shape != null && vm.state.shapes[shape].locked) { handTargetId = null; return }
                    when {
                        solid != null -> { vm.selectSolid(solid); vm.beginSolidDrag(solid) }
                        shape != null -> vm.beginShapeDrag(shape)
                        arWorkspaceMode == ArMathWorkspaceMode.Graph3D -> vm.beginArGraphObjectGesture()
                        else -> vm.beginSpatialGesture()
                    }
                    handGestureActive = true
                }
                ArHandPhase.Update -> if (handGestureActive) {
                    val delta = Vec3(action.pan.x.toDouble()*10, -action.pan.y.toDouble()*10, 0.0)
                    val solid = handTargetId?.removePrefix("solid-")?.toIntOrNull()?.takeIf(vm.state.solids.indices::contains)
                    val shape = vm.state.shapes.indexOfFirst { it.id == handTargetId }.takeIf { it >= 0 }
                    when {
                        solid != null -> when(tool) {
                            ArHandTool.Move -> vm.previewSolidDrag(solid, delta)
                            ArHandTool.Rotate -> vm.previewSolidRotation(solid, Vec3(0.0,action.rotationDegrees.toDouble(),0.0))
                            ArHandTool.Scale -> vm.previewSolidScale(solid,action.scale.toDouble())
                        }
                        shape != null -> when(tool) {
                            ArHandTool.Move -> vm.previewShapeDrag(Vec2(delta.x,delta.y))
                            ArHandTool.Rotate -> vm.previewShapeRotation(action.rotationDegrees.toDouble())
                            ArHandTool.Scale -> vm.previewShapeScale(action.scale.toDouble())
                        }
                        arWorkspaceMode == ArMathWorkspaceMode.Graph3D -> handTargetId?.let { vm.previewArGraphObject(it,delta,Vec3(0.0,action.rotationDegrees.toDouble(),0.0),action.scale.toDouble()) }
                        else -> vm.previewSpatialHandGesture(delta*.1,action.rotationDegrees,action.scale)
                    }
                    handStatus = "${tool.name} · release to finish"
                }
                ArHandPhase.End -> { finishHandGesture(false); handStatus = "Released" }
                ArHandPhase.Cancel -> { finishHandGesture(true); handStatus = "Show your hand to the camera" }
                ArHandPhase.Idle -> handStatus = "Select an object, then pinch · ${handTool.name}"
            }
            return
        }
''' +s[idx:]
s=s.replace('DisposableEffect(handsEnabled, runtime, handCameraActive, liveAR) {','DisposableEffect(handsEnabled, runtime, handCameraActive, liveAR, cameraHandsOnly, cameraGranted) {',1)
s=s.replace('if (handsEnabled && handCameraActive && liveAR) {','if (handsEnabled && handCameraActive && cameraGranted && (liveAR || cameraHandsOnly)) {',1)
s=s.replace('(!handCameraActive || arFrame?.camera?.trackingState != ArTrackingState.Tracking ||','(!handCameraActive || (!cameraHandsOnly && arFrame?.camera?.trackingState != ArTrackingState.Tracking) ||',1)
s=s.replace('> 350 || !liveAR))','> 350 || (!liveAR && !cameraHandsOnly)))',1)
idx=s.index('    Box(Modifier.fillMaxSize().onSizeChanged { viewportSize = it })',s.index('private fun SpatialARScreen'))
s=s[:idx]+'''    fun selectCameraMode(withAr: Boolean) {
        finishHandGesture(true)
        handController.reset()
        handsEnabled = true
        graphSheet = null
        cameraHandsOnly = !withAr
        if (withAr) {
            displayFirstMode = false
            arPlacementMode = ArPlacementMode.FloorTable
            placementMode = activeAnchor == null
            startLiveAr()
        } else {
            compositorView?.onPause()
            runtime?.pause()
            liveAR = false
            arFrame = null
            frameState = null
            displayFirstMode = true
            placementMode = false
            if (!cameraGranted) requestCameraPermission = true
        }
    }
''' +s[idx:]
s=s.replace('    Box(Modifier.fillMaxSize().onSizeChanged { viewportSize = it }) {\n', '''    Box(Modifier.fillMaxSize().onSizeChanged { viewportSize = it }) {
        if (cameraHandsOnly && cameraGranted) HandCameraPreview(Modifier.fillMaxSize(), handCameraActive,
            wantsFrame = { currentHandsEnabled && currentHandDetector?.canAcceptFrame == true },
            onFrame = { currentHandDetector?.submit(it) }, onStatus = { handStatus = it })
''',1)
s=s.replace('onEnd={ cancel -> vm.endArGraphObjectGesture(cancel); cadSnapLabel="" },onInspect={ graphSheet="Precision inspector" },showLabels=vm.state.labSessionValues["arAnalysis.labels"]=="true")','onEnd={ cancel -> vm.endArGraphObjectGesture(cancel); cadSnapLabel="" },onInspect={ graphSheet="Precision inspector" },showLabels=vm.state.labSessionValues["arAnalysis.labels"]=="true", transparentBackground=cameraHandsOnly)',1)
s=s.replace('onGestureEnd = vm::endSpatialGesture,\n            )','onGestureEnd = vm::endSpatialGesture,\n                transparentBackground = cameraHandsOnly,\n            )',1)
s=s.replace('handsEnabled && liveAR && arWorkspaceMode', 'handsEnabled && (liveAR || cameraHandsOnly) && arWorkspaceMode')
s=s.replace('if(handsEnabled && liveAR && gestureFeedback','if(handsEnabled && (liveAR || cameraHandsOnly) && gestureFeedback',1)
s=s.replace('if (handsEnabled && liveAR) handCursor','if (handsEnabled && (liveAR || cameraHandsOnly)) handCursor',1)
s=s.replace('"Hands need a live AR camera"','"Choose a camera mode to use hands"',1)
s=s.replace('Text(if (!liveAR) "Choose a camera mode to use hands" else handStatus','Text(if (!liveAR && !cameraHandsOnly) "Choose a camera mode to use hands" else handStatus',1)
# Visible mode chooser accessible in all workspaces.
idx=s.index('        if(cadBuildError.isNotBlank()',s.index('private fun SpatialARScreen'))
s=s[:idx]+'''        if (!arHudHidden) Column(Modifier.align(Alignment.BottomEnd).padding(10.dp).background(Color(0xED131D2D), RoundedCornerShape(12.dp)).padding(6.dp)) {
            GlowButton(if (cameraHandsOnly) "• Camera + Hand gestures" else "Camera + Hand gestures") { selectCameraMode(false) }
            GlowButton(if (!cameraHandsOnly && liveAR) "• Camera + Hand gestures + AR" else "Camera + Hand gestures + AR") { selectCameraMode(true) }
            if (cameraHandsOnly) Text(handStatus, color = Cyan, fontSize = 10.sp)
        }
''' +s[idx:]
# Existing explicit AR controls also leave the independent camera mode.
s=s.replace('                    GlowButton(if (liveAR && !displayFirstMode)', '                    GlowButton(if (liveAR && !displayFirstMode)',1)
s=s.replace('                        arPlacementMode = ArPlacementMode.FloorTable\n                        displayFirstMode = false\n                        placementMode = activeAnchor == null\n                        restartLiveAr', '                        cameraHandsOnly = false\n                        arPlacementMode = ArPlacementMode.FloorTable\n                        displayFirstMode = false\n                        placementMode = activeAnchor == null\n                        restartLiveAr',1)
p.write_text(s,encoding='utf-8')
p=Path('app/src/main/java/com/indianservers/aiexplorer/ArCadPreviewCanvas.kt');s=p.read_text();s=s.replace('showLabels:Boolean=false)', 'showLabels:Boolean=false,transparentBackground:Boolean=false)');s=s.replace('        drawRect(Color(0xFF08131B))','        if (!transparentBackground) drawRect(Color(0xFF08131B))');p.write_text(s)
p=Path('app/src/main/java/com/indianservers/aiexplorer/ui/ar/ArSpatialPreviewCanvas.kt');s=p.read_text();s=s.replace('    onGestureEnd: () -> Unit,','    onGestureEnd: () -> Unit,\n    transparentBackground: Boolean = false,');s=s.replace('        drawRect(\n            Brush.verticalGradient(', '        if (!transparentBackground) drawRect(\n            Brush.verticalGradient(',1);p.write_text(s)
