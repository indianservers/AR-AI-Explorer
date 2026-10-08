from pathlib import Path
p=Path('app/src/main/java/com/indianservers/aiexplorer/MainActivity.kt');s=p.read_text(encoding='utf-8')
s=s.replace('var cameraHandsOnly by rememberSaveable { mutableStateOf(false) }','var cameraHandsOnly by rememberSaveable { mutableStateOf(true) }',1)
s=s.replace('var handsEnabled by rememberSaveable { mutableStateOf(false) }','var handsEnabled by rememberSaveable { mutableStateOf(true) }',1)
s=s.replace('var displayFirstMode by remember { mutableStateOf(false) }','var displayFirstMode by remember { mutableStateOf(cameraHandsOnly) }',1)
s=s.replace('if (runtime != null) capabilities = runtime.checkAvailability().toSpatialCapabilities()','if (runtime != null && !cameraHandsOnly) capabilities = runtime.checkAvailability().toSpatialCapabilities()',1)
needle='    LaunchedEffect(runtime, cameraGranted) {\n        if (!autoStartAttempted'
s=s.replace(needle,'    LaunchedEffect(runtime, cameraGranted) {\n        if (cameraHandsOnly) {\n            if (!cameraGranted) requestCameraPermission = true\n            return@LaunchedEffect\n        }\n        if (!autoStartAttempted',1)
s=s.replace('            startLiveAr()\n        } else {\n            compositorView?.onPause()', '        } else {\n            compositorView?.onPause()',1)
s=s.replace('    Box(Modifier.fillMaxSize().onSizeChanged { viewportSize = it }) {','    LaunchedEffect(cameraHandsOnly) {\n        if (!cameraHandsOnly) {\n            delay(150) // Let the independent camera release before ARCore opens it.\n            startLiveAr()\n        }\n    }\n    Box(Modifier.fillMaxSize().onSizeChanged { viewportSize = it }) {',1)
s=s.replace('        displayFirstMode = false\n        placementMode = activeAnchor == null\n        reticleHit = null\n        if (!liveAR) startLiveAr(userRequestedInstall = true)\n    }\n    fun plotArGraphExpression', '        displayFirstMode = cameraHandsOnly\n        placementMode = !cameraHandsOnly && activeAnchor == null\n        reticleHit = null\n        if (!liveAR) startLiveAr(userRequestedInstall = true)\n    }\n    fun plotArGraphExpression',1)
s=s.replace('val target = arSelection.primaryObjectId ?: presentedScene.primitives.firstOrNull { it.visible && it.selectable }?.id','val target = arSelection.primaryObjectId?.takeIf { selected -> presentedScene.primitives.any { it.id == selected && it.visible && it.selectable } } ?: presentedScene.primitives.firstOrNull { it.visible && it.selectable }?.id',1)
p.write_text(s,encoding='utf-8')
