from pathlib import Path
r=Path(r'C:\Indian Servers\AIExplorer');p=r/'arengine/src/main/java/com/indianservers/aiexplorer/handintelligence/debug/GestureReplay.kt';s=p.read_text(encoding='utf-8');a=s.index('    fun exportJson(');b=s.index('\n}',a)
s=s[:a]+'''    fun exportJson(writer:Writer) {
        fun q(value:Any?)="\\\"${(value?.toString() ?: "").replace("\\\\","\\\\\\\\").replace("\\\"","\\\\\\\"").replace("\\n","\\\\n")}\\\""
        fun vec(p:Vec3)="[${p.x},${p.y},${p.z}]"
        fun features(h:IntelligentHand):String {
            val p=h.pose;val m=h.motion
            return "{\\\"id\\\":${q(h.id)},\\\"handedness\\\":${q(p.handedness)},\\\"palm\\\":${vec(p.palm)},\\\"pinch\\\":${vec(p.pinch)},\\\"filteredPinch\\\":${vec(h.filteredPinch)},\\\"pinchDistance\\\":${p.pinchDistance},\\\"pinchStrength\\\":${p.pinchStrength},\\\"grabStrength\\\":${p.grabStrength},\\\"openness\\\":${p.openness},\\\"pointScore\\\":${p.pointScore},\\\"palmNormal\\\":${vec(p.palmNormal)},\\\"handScale\\\":${p.handScale},\\\"quality\\\":${p.quality},\\\"velocity\\\":${vec(m.velocity)},\\\"acceleration\\\":${vec(m.acceleration)},\\\"jerk\\\":${vec(m.jerk)},\\\"angularVelocity\\\":${m.angularVelocity},\\\"pinchVelocity\\\":${m.pinchVelocity},\\\"stability\\\":${m.stabilityScore},\\\"motionConsistency\\\":${m.motionConsistency},\\\"dwellMillis\\\":${m.dwellMillis}}"
        }
        writer.write("[")
        snapshot().forEachIndexed { index,sample ->
            if(index>0) writer.write(",")
            val state=sample.result.intelligence;val action=sample.result.action;val t=action?.transform ?: SpatialTransformIntent()
            writer.write("{\\\"timestamp\\\":${sample.tracking.timestampMillis},\\\"mode\\\":${q(sample.scene.mode)},\\\"profile\\\":${q(sample.scene.profile)},\\\"intent\\\":${q(state.primaryIntent)},\\\"label\\\":${q(sample.label)},\\\"target\\\":${q(state.targetObjectId)},\\\"region\\\":${q(state.targetRegionId)},\\\"state\\\":${q(state.state)},\\\"phase\\\":${q(state.phase)},\\\"confidence\\\":${state.intentConfidence},\\\"targetConfidence\\\":${state.targetConfidence},\\\"trackingQuality\\\":${state.trackingQuality},\\\"precision\\\":${state.precisionMode},\\\"targetLocked\\\":${state.targetLocked},\\\"translation\\\":${vec(t.translation)},\\\"rotation\\\":${vec(t.rotation)},\\\"scale\\\":${t.scale},\\\"radialDelta\\\":${t.radialDelta},\\\"snapped\\\":${t.snapped},\\\"interactions\\\":[${action?.interactions?.joinToString(",") { q(it) } ?: ""}],\\\"features\\\":[${state.hands.joinToString(",",transform=::features)}],\\\"hands\\\":[")
            sample.tracking.hands.forEachIndexed { n,hand ->
                if(n>0) writer.write(",")
                writer.write("{\\\"id\\\":${q(hand.id)},\\\"handedness\\\":${q(hand.handedness)},\\\"confidence\\\":${hand.confidence},\\\"landmarks\\\":[${hand.landmarks.joinToString(",") { "[${it.x},${it.y}]" }}],\\\"worldLandmarks\\\":[${hand.worldLandmarks.joinToString(",",transform=::vec)}]}")
            }
            writer.write("]}")
        }
        writer.write("]");writer.flush()
    }
'''+s[b:];p.write_text(s,encoding='utf-8')
p=r/'app/src/main/java/com/indianservers/aiexplorer/MainActivity.kt';s=p.read_text(encoding='utf-8')
s=s.replace('    val semanticRecognizer = remember(runtime) { ArGestureRecognizer() }\n','').replace('    val stableHandHover = remember(runtime) { ArStableHover() }\n','')
a=s.index('    var recognizedGesture by remember');b=s.index('    var showHandSkeleton',a);s=s[:a]+s[b:]
a=s.index('    val handController = remember(runtime, arWorkspaceMode)');b=s.index('\n',a);s=s[:a]+s[b+1:]
s=s.replace('intelligenceSession.reset(); handController.reset();','intelligenceSession.reset();').replace('semanticRecognizer.reset(); stableHandHover.reset(); ','').replace('handController.reset()','intelligenceSession.reset()').replace('; handLandmarkFrame = null','')
s=s.replace('    LaunchedEffect(handTool, arWorkspaceMode, liveAR, displayFirstMode, showArAddOptions)', '    LaunchedEffect(arWorkspaceMode, liveAR, displayFirstMode, showArAddOptions,graphSheet,graphInteractionMode)')
s=s.replace('graphSheet==null && !showArAddOptions && !graphTouchActive && !presentationLocked &&','graphSheet==null && !showArAddOptions && !graphTouchActive && !presentationLocked && (arWorkspaceMode!=ArMathWorkspaceMode.Graph3D || graphInteractionMode!=ArGraphInteractionMode.Touch) &&')
s=s.replace('frame?.camera?.trackingState==ArTrackingState.Tracking && activeAnchor','frame?.camera?.trackingState==ArTrackingState.Tracking && frame.camera.trackingFailure==com.indianservers.aiexplorer.arengine.contract.ArTrackingFailure.None && activeAnchor')
s=s.replace('            val state=output.intelligence; val action=output.action','''            val state=output.intelligence; val action=output.action
            if(tutorialStep==0 && state.hands.isNotEmpty()) tutorialStep=1
            if(tutorialStep==1 && state.primaryIntent in setOf(com.indianservers.aiexplorer.handintelligence.HandIntent.POINT,com.indianservers.aiexplorer.handintelligence.HandIntent.INSPECT)) tutorialStep=2
            if(tutorialStep in 1..2 && state.phase==com.indianservers.aiexplorer.handintelligence.InteractionPhase.BEGIN) tutorialStep=3
            if(tutorialStep==3 && (action?.transform?.translation?.magnitude() ?: 0.0)>.025) tutorialStep=4
            if(tutorialStep==4 && state.twoHandMode==com.indianservers.aiexplorer.handintelligence.TwoHandMode.COMBINED && kotlin.math.abs((action?.transform?.scale ?: 1.0)-1.0)>.02) completeHandTutorial()''')
s=s.replace('        if(handsEnabled && (liveAR || cameraHandsOnly) && gestureFeedback.isNotBlank()) Text(gestureFeedback,color=Color.White,modifier=Modifier.align(Alignment.BottomStart).padding(bottom=20.dp).background(Color(0xDD08131B),RoundedCornerShape(8.dp)).padding(8.dp),fontSize=12.sp)','        if(handsEnabled && !cameraHandsOnly && liveAR && handStatus.isNotBlank()) Text(handStatus,color=Color.White,modifier=Modifier.align(Alignment.BottomStart).padding(bottom=20.dp).background(Color(0xDD08131B),RoundedCornerShape(8.dp)).padding(8.dp),fontSize=12.sp)')
p.write_text(s,encoding='utf-8')
p=r/'app/src/main/java/com/indianservers/aiexplorer/HandCameraPreview.kt';s=p.read_text(encoding='utf-8').replace('var disposed=false','val disposed=java.util.concurrent.atomic.AtomicBoolean(false)').replace('!disposed &&','!disposed.get() &&').replace('onDispose { disposed=true;','onDispose { disposed.set(true);');p.write_text(s,encoding='utf-8')
