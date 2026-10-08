from pathlib import Path
r=Path(r'C:\Indian Servers\AIExplorer');p=r/'app/src/main/java/com/indianservers/aiexplorer/HandMathSceneAdapter.kt';s=p.read_text(encoding='utf-8')
s=s.replace('import kotlin.math.*','import kotlin.math.*\nimport com.indianservers.aiexplorer.workspace.WorkspaceState\nimport com.indianservers.aiexplorer.core.ExpressionEngine')
s=s.replace('selection:ArSelectionState):List<MathObjectSnapshot>', 'selection:ArSelectionState,state:WorkspaceState):List<MathObjectSnapshot>')
s=s.replace('        MathObjectSnapshot(p.id,type,vertices,p.geometry.triangles.toIntArray(),ArCadTopology.edges(p.geometry),locked=p.id in selection.lockedObjectIds,selected=p.id in selection.objectIds,dimensions=dims,radius=dims.x*.5)','''        val node=state.arCadNodes()[p.id]; val pose=state.arGraphObject(p.id)
        val mutableBody=setOf(MathInteraction.TRANSLATE,MathInteraction.ROTATE,MathInteraction.SCALE,MathInteraction.INSPECT)
        val allowed=if(p.dependencyIds.isNotEmpty() || node?.dependencies?.isNotEmpty()==true || p.kind==SpatialPrimitiveKind.Surface || node?.type in setOf(ArCadType.Curve,ArCadType.ParametricSurface,ArCadType.ImplicitSurface)) mutableBody else MathInteraction.entries.toSet()
        val expression=if(node?.type==ArCadType.FunctionSurface) node.parameters["expressionZ"] ?: "x^2+y^2" else if(p.kind==SpatialPrimitiveKind.Surface && node==null) state.surfaceLayers.firstOrNull { it.id==p.id.removePrefix("surface-") }?.expression ?: state.surfaceExpression else null
        val compiled=expression?.let { runCatching { ExpressionEngine().compile(it) }.getOrNull() }
        val parameters=node?.parameters?.filterKeys { it.startsWith("parameter.") }?.mapKeys { it.key.removePrefix("parameter.") }?.mapValues { ArCadTopology.number(it.value) } ?: emptyMap()
        val evaluator:((Vec3)->Map<String,Double>)?=compiled?.let { f -> { point:Vec3 ->
            val local=t.orientation.conjugate().rotate(point-t.offsetMeters)*(1/t.uniformScale)
            val x=local.x/t.axisScale.x; val y=local.y/t.axisScale.y; val h=1e-4
            fun value(a:Double,b:Double)=f.eval(parameters+mapOf("x" to a,"y" to b))
            mapOf("value" to value(x,y),"gradient x" to (value(x+h,y)-value(x-h,y))/(2*h),"gradient y" to (value(x,y+h)-value(x,y-h))/(2*h)).filterValues { it.isFinite() }
        } }
        MathObjectSnapshot(p.id,type,vertices,p.geometry.triangles.toIntArray(),ArCadTopology.edges(p.geometry),locked=p.id in selection.lockedObjectIds || pose.locked,selected=p.id in selection.objectIds,dimensions=dims,radius=dims.x*.5,allowed=allowed,inspect=evaluator)''')
s=s.replace('    @Volatile var projection:CoordinateMapper?=null','''    val recorder=com.indianservers.aiexplorer.handintelligence.debug.GestureReplayRecorder()
    @Volatile var projection:CoordinateMapper?=null
    @Volatile var latest:MathInteractionFrame?=null
    @Volatile var latestTracking:com.indianservers.aiexplorer.arengine.interaction.ArHandFrame?=null
    @Volatile var processingMicros=0L
    @Volatile var droppedFrames=0L
    @Volatile var developerEnabled=false
    @Volatile var profile=IntelligenceProfile.BALANCED
    @Volatile var trainingLabel:HandIntent?=null
    fun export(directory:java.io.File):java.io.File {
        directory.mkdirs(); val file=java.io.File(directory,"hand-replay-${System.currentTimeMillis()}.json")
        file.bufferedWriter().use(recorder::exportJson); java.io.File(directory,file.nameWithoutExtension+".csv").bufferedWriter().use(recorder::exportCsv); return file
    }
    fun replay(onResult:(List<MathInteractionFrame>)->Unit) { executor.execute { val frames=com.indianservers.aiexplorer.handintelligence.debug.GestureReplayPlayer().replay(recorder.snapshot()); main.post { onResult(frames) } } }''')
s=s.replace('if(!busy.compareAndSet(false,true)) return','if(!busy.compareAndSet(false,true)) { droppedFrames++; return }')
s=s.replace('val result=controller.processFrame(frame,scene); main.post', 'val start=System.nanoTime(); val actualScene=scene.copy(profile=profile); val result=controller.processFrame(frame,actualScene); processingMicros=(System.nanoTime()-start)/1000; latest=result; if(developerEnabled) latestTracking=frame; recorder.record(frame,actualScene,result,trainingLabel); main.post')
s=s.replace('fun reset() { generation.incrementAndGet(); controller.reset() }','fun reset() { generation.incrementAndGet(); controller.reset(); latest=null; latestTracking=null }')
p.write_text(s,encoding='utf-8')
p=r/'app/src/main/java/com/indianservers/aiexplorer/MainActivity.kt';s=p.read_text(encoding='utf-8')
s=s.replace('HandMathSceneAdapter.objects(presentedScene,arSelection)','HandMathSceneAdapter.objects(presentedScene,arSelection,vm.state)')
s=s.replace('    var showHandRay by rememberSaveable { mutableStateOf(true) }','    var showHandRay by rememberSaveable { mutableStateOf(false) }\n    var developerHands by rememberSaveable { mutableStateOf(false) }\n    var handDebugText by remember { mutableStateOf("") }\n    var handReplayText by remember { mutableStateOf("") }\n    var lastHandUiUpdate by remember { mutableLongStateOf(0L) }')
s=s.replace('        if(showHandSkeleton || showHandRay) handLandmarkFrame=result','')
s=s.replace('            handCursor=cursor?.let { ArVector2(it.x.toFloat(),it.y.toFloat()) }','''            val refresh=state.timestampNanos/1_000_000-lastHandUiUpdate>=200 || state.phase in setOf(com.indianservers.aiexplorer.handintelligence.InteractionPhase.BEGIN,com.indianservers.aiexplorer.handintelligence.InteractionPhase.END,com.indianservers.aiexplorer.handintelligence.InteractionPhase.CANCEL)
            if(refresh) { lastHandUiUpdate=state.timestampNanos/1_000_000
                if(developerHands) { val t=action?.transform; handDebugText="${state.state} · ${state.primaryIntent}\\n${state.targetObjectId} / ${state.targetRegionId}\\nintent ${state.intentConfidence} target ${state.targetConfidence} quality ${state.trackingQuality}\\nprecision ${state.precisionMode} support ${state.supportHandId}\\ntranslation ${t?.translation} rotation ${t?.rotation} scale ${t?.scale}\\n${intelligenceSession.processingMicros} µs · dropped ${intelligenceSession.droppedFrames}" }
            }''')
s=s.replace('vm.previewIntelligentHand(action,handCadGeometry); handStatus=action.interactions.joinToString', 'vm.previewIntelligentHand(action,handCadGeometry); if(refresh) handStatus=action.interactions.joinToString')
s=s.replace('InteractionPhase.INSPECT -> handStatus=', 'InteractionPhase.INSPECT -> if(refresh) handStatus=')
s=s.replace('else -> if(!handGestureActive) handStatus=if(ready)', 'else -> if(refresh && !handGestureActive) handStatus=if(ready)')
# Make existing manual tools explicitly touch-only; ordinary hands always infer automatically.
s=s.replace('                    FlowRow { ArHandTool.entries.forEach { tool ->', '                    Text("Touch transform tools · hand edits are automatic",color=Muted,fontSize=11.sp)\n                    FlowRow { ArHandTool.entries.forEach { tool ->')
s=s.replace('                    GlowButton(if (showHandSkeleton)', '''                    if(BuildConfig.DEBUG) {
                        GlowButton(if(developerHands) "Developer hand diagnostics on" else "Developer hand diagnostics off") { developerHands=!developerHands; intelligenceSession.developerEnabled=developerHands; if(!developerHands) intelligenceSession.recorder.enabled=false }
                        if(developerHands) {
                            Text(handDebugText,color=Muted,fontSize=10.sp)
                            GlowButton(if(intelligenceSession.recorder.enabled) "Stop local recording" else "Record locally") { intelligenceSession.recorder.enabled=!intelligenceSession.recorder.enabled }
                            GlowButton("Export local replay") { handReplayText=runCatching { intelligenceSession.export(java.io.File(context.filesDir,"hand-replays")).absolutePath }.getOrElse { it.message ?: "Export failed" } }
                            GlowButton("Replay recorded frames") { intelligenceSession.replay { frames -> handReplayText=frames.takeLast(10).joinToString("\\n") { "${it.intelligence.primaryIntent} · ${it.intelligence.intentConfidence} · ${it.intelligence.targetObjectId}/${it.intelligence.targetRegionId} · ${it.intelligence.state} · ${it.action?.transform}" } } }
                            Text(handReplayText,color=Muted,fontSize=10.sp)
                            Text("Confirmed training label",color=Muted)
                            FlowRow { com.indianservers.aiexplorer.handintelligence.HandIntent.entries.forEach { intent -> GlowButton(intent.name) { intelligenceSession.trainingLabel=intent } } }
                        }
                    }
                    Text("Response profile",color=Ink)
                    FlowRow { com.indianservers.aiexplorer.handintelligence.IntelligenceProfile.entries.forEach { profile -> GlowButton(profile.name.lowercase().replaceFirstChar { it.uppercase() }) { intelligenceSession.profile=profile; intelligenceSession.reset(); finishHandGesture(true) } } }
                    GlowButton(if (showHandSkeleton)''',1)
# Non-graph touch editing retains actual model transactions.
s=s.replace('onStart={ graphTouchActive=true; intelligenceSession.reset(); finishHandGesture(true) },\n                onEdit={ _,_,_,_,_ -> },onEnd={ graphTouchActive=false },onInspect={}', '''onStart={ graphTouchActive=true; intelligenceSession.reset(); finishHandGesture(true)
                    val id=arSelection.primaryObjectId; val solid=id?.removePrefix("solid-")?.toIntOrNull()?.takeIf(vm.state.solids.indices::contains); val vector=id?.removePrefix("vector-")?.toIntOrNull()?.takeIf(vm.state.vectors3D.indices::contains); val shape=vm.state.shapes.indexOfFirst { it.id==id }
                    when { solid!=null -> vm.beginSolidDrag(solid); vector!=null -> vm.beginVectorDrag(vector); shape>=0 -> vm.beginShapeDrag(shape); else -> vm.beginArGraphObjectGesture() }; handTargetId=id; handGestureActive=true
                },
                onEdit={ hit,g,pan,rotation,scale -> vm.previewIntelligentHand(com.indianservers.aiexplorer.handintelligence.MathAction(hit.objectId,com.indianservers.aiexplorer.handintelligence.SemanticHitRegion("body",com.indianservers.aiexplorer.handintelligence.RegionKind.BODY,position=ArVector3.Zero),setOf(com.indianservers.aiexplorer.handintelligence.MathInteraction.TRANSLATE),com.indianservers.aiexplorer.handintelligence.SpatialTransformIntent(ArVector3(pan.x,pan.y,pan.z),ArVector3(0.0,0.0,rotation),scale)),g) },onEnd={ cancel -> finishHandGesture(cancel); graphTouchActive=false },onInspect={ vm.status="${arSelection.primaryObjectId ?: "Object"} selected" }''')
# Remove normal per-landmark state canvas and replace with isolated draw-only feedback.
a=s.index('        if (handsEnabled && (liveAR || cameraHandsOnly) && arWorkspaceMode == ArMathWorkspaceMode.Graph3D) {\n            Canvas');b=s.index('        if (arWorkspaceMode == ArMathWorkspaceMode.Graph3D && tutorialStep',a)
s=s[:a]+'''        if(handsEnabled && (liveAR || cameraHandsOnly)) HandIntelligenceFeedback(Modifier.fillMaxSize(),intelligenceSession,developerHands,showHandSkeleton,showHandRay)
        if(developerHands && BuildConfig.DEBUG && graphSheet==null) Text(handDebugText,color=Color.White,fontSize=10.sp,modifier=Modifier.align(Alignment.CenterStart).background(Color(0xDD08131B)).padding(8.dp))
'''+s[b:]
s=s.replace('"Touch thumb and index finger together", "Place a graph, pinch it, then move your hand", "Pinch with both hands, then spread to scale"','"Reach for an object and hold it", "Move your hand to move the object", "Reach with your other hand to resize or turn the object"')
p.write_text(s,encoding='utf-8')
