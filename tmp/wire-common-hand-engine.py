from pathlib import Path
r=Path(r'C:\Indian Servers\AIExplorer'); p=r/'app/src/main/java/com/indianservers/aiexplorer/MainActivity.kt'; s=p.read_text(encoding='utf-8')
methods='''    /** Apply all inferred channels against one retained baseline and one undo transaction. */
    fun previewIntelligentHand(action:com.indianservers.aiexplorer.handintelligence.MathAction,geometry:com.indianservers.aiexplorer.spatial.SpatialGeometry?) {
        val t=action.transform; val delta=Vec3(t.translation.x,t.translation.y,t.translation.z)
        val rotation=Vec3(t.rotation.x,t.rotation.y,t.rotation.z); val ops=action.interactions
        val solid=action.objectId.removePrefix("solid-").toIntOrNull()?.takeIf(state.solids.indices::contains)
        val vector=action.objectId.removePrefix("vector-").toIntOrNull()?.takeIf(state.vectors3D.indices::contains)
        val shape=state.shapes.indexOfFirst { it.id==action.objectId }
        if(solid!=null) {
            val base=solidGesture?.takeIf { it.index==solid }?.from ?: return
            val f=t.scale.coerceIn(.1,10.0)
            var edited=base.copy(position=base.position+delta,rotation=base.rotation+rotation,width=(base.width*f).coerceIn(.02,100.0),height=(base.height*f).coerceIn(.02,100.0),depth=(base.depth*f).coerceIn(.02,100.0),radius=(base.radius*f).coerceIn(.01,100.0),topRadius=(base.topRadius*f).coerceIn(.01,100.0))
            if(com.indianservers.aiexplorer.handintelligence.MathInteraction.CHANGE_RADIUS in ops) edited=base.copy(radius=(base.radius*f+t.radialDelta).coerceIn(.01,100.0),position=base.position,rotation=base.rotation)
            val axis=action.region.axis
            if(axis!=null && ops.any { it.name.startsWith("STRETCH_") }) {
                val amount=listOf(delta.x,delta.y,delta.z)[axis]*action.region.sign
                val old=listOf(base.width,base.height,base.depth)[axis]; val value=(old+amount).coerceIn(.02,100.0); val shift=(value-old)*action.region.sign*.5
                edited=base.copy(width=if(axis==0) value else base.width,height=if(axis==1) value else base.height,depth=if(axis==2) value else base.depth,position=base.position+when(axis) { 0->Vec3(shift,0.0,0.0); 1->Vec3(0.0,shift,0.0); else->Vec3(0.0,0.0,shift) })
            }
            state=state.copy(solids=state.solids.mapIndexed { i,old->if(i==solid) edited else old },modifiedAt=System.currentTimeMillis()); return
        }
        if(vector!=null) {
            val base=vectorGesture?.takeIf { it.index==vector }?.from ?: return
            val head=com.indianservers.aiexplorer.handintelligence.MathInteraction.EDIT_VECTOR_HEAD in ops
            val origin=com.indianservers.aiexplorer.handintelligence.MathInteraction.EDIT_VECTOR_ORIGIN in ops
            val edited=base.copy(start=if(head) base.start else base.start+delta,end=if(origin) base.end else base.end+delta)
            state=state.copy(vectors3D=state.vectors3D.mapIndexed { i,old->if(i==vector) edited else old },modifiedAt=System.currentTimeMillis()); return
        }
        if(shape>=0) {
            val g=pointGesture ?: return; val center=InteractionGeometry.bounds(g.from)?.center ?: return
            val angle=Math.toRadians(rotation.z); val c=cos(angle); val sn=sin(angle)
            val vertex=action.region.index?.takeIf { com.indianservers.aiexplorer.handintelligence.MathInteraction.MOVE_VERTEX in ops }
            val replacements=g.indices.zip(g.from.mapIndexed { i,point -> if(vertex!=null) { if(i==vertex) point+Vec2(delta.x,delta.y) else point } else { val q=(point-center)*t.scale; center+Vec2(q.x*c-q.y*sn,q.x*sn+q.y*c)+Vec2(delta.x,delta.y) } }).toMap()
            state=state.copy(points=state.points.mapIndexed { i,old->replacements[i] ?: old },modifiedAt=System.currentTimeMillis()).recomputed(); return
        }
        val before=arObjectGestureFrom ?: return
        if(com.indianservers.aiexplorer.handintelligence.MathInteraction.CHANGE_RADIUS in ops) { previewArCadRadius(action.objectId,t.radialDelta,t.scale); return }
        val axis=action.region.axis
        val node=before.arCadNodes()[action.objectId]
        if(axis!=null && node!=null && ops.any { it.name.startsWith("STRETCH_") } && arCadConstraints(action.objectId).isEmpty()) {
            val key=listOf("width","height","depth")[axis]; val old=com.indianservers.aiexplorer.spatial.ArCadTopology.number(node.parameters[key] ?: "2")
            val pose=before.arGraphObject(action.objectId); val local=pose.transform().orientation.conjugate().rotate(t.translation)
            val amount=listOf(local.x,local.y,local.z)[axis]*action.region.sign/pose.scale
            val value=(old+amount).coerceIn(.01,100.0)
            previewArGraphObject(action.objectId,delta*.5,Vec3(0.0,0.0,0.0),1.0)
            previewArCadParameters(action.objectId,node.parameters+(key to value.toString())); return
        }
        val part=when(action.region.kind) { com.indianservers.aiexplorer.handintelligence.RegionKind.VERTEX,com.indianservers.aiexplorer.handintelligence.RegionKind.VECTOR_HEAD,com.indianservers.aiexplorer.handintelligence.RegionKind.VECTOR_ORIGIN -> ArSubObjectKind.Vertex; com.indianservers.aiexplorer.handintelligence.RegionKind.EDGE -> ArSubObjectKind.Edge; com.indianservers.aiexplorer.handintelligence.RegionKind.FACE -> ArSubObjectKind.Face; else->ArSubObjectKind.Whole }
        if(part!=ArSubObjectKind.Whole && geometry!=null && action.region.index!=null) previewArCadSubObject(action.objectId,geometry,part,action.region.index!!,delta,rotation,t.scale)
        else previewArGraphObject(action.objectId,delta,rotation,t.scale)
    }

'''
s=s.replace('    fun previewSolidDrag(index: Int, delta: Vec3) {',methods+'    fun previewSolidDrag(index: Int, delta: Vec3) {')
s=s.replace('    val handController = remember(runtime, arWorkspaceMode)', '    val intelligenceSession = remember(runtime, arWorkspaceMode) { HandIntelligenceSession() }\n    DisposableEffect(intelligenceSession) { onDispose { intelligenceSession.close() } }\n    val handController = remember(runtime, arWorkspaceMode)')
a=s.index('    fun handleHandFrame(result: ArHandFrame) {'); b=s.index('    val currentHandHandler',a)
s=s[:a]+'''    val intelligentObjects=remember(presentedScene,arSelection) { HandMathSceneAdapter.objects(presentedScene,arSelection) }
    fun handleHandFrame(result: ArHandFrame) {
        lastHandFrameMillis=result.timestampMillis
        if(showHandSkeleton || showHandRay) handLandmarkFrame=result
        val frame=arFrame
        val mapper=if(cameraHandsOnly) intelligenceSession.projection else frame?.let { HandArProjection(it,canonicalArScene.placement) }
        if(mapper==null) { finishHandGesture(true); return }
        val ready=handsEnabled && handCameraActive && cameraGranted && graphSheet==null && !showArAddOptions && !graphTouchActive && !presentationLocked &&
            android.os.SystemClock.uptimeMillis()-result.timestampMillis in 0..350 &&
            (cameraHandsOnly || (liveAR && !displayFirstMode && frame?.camera?.trackingState==ArTrackingState.Tracking && activeAnchor?.trackingState==com.indianservers.aiexplorer.arengine.contract.ArAnchorTrackingState.Tracking))
        val ui=buildList {
            add(com.indianservers.aiexplorer.handintelligence.UiRegion(0.0,.89,1.0,1.0))
            if(!arHudHidden) {
                add(com.indianservers.aiexplorer.handintelligence.UiRegion(0.0,0.0,(handHudSize.width+handHudPaddingPixels)/viewportSize.width.coerceAtLeast(1).toDouble(),(handHudSize.height+handHudPaddingPixels)/viewportSize.height.coerceAtLeast(1).toDouble()))
                add(com.indianservers.aiexplorer.handintelligence.UiRegion(.55,.65,1.0,.89))
            }
        }
        intelligenceSession.submit(result,com.indianservers.aiexplorer.handintelligence.MathSceneSnapshot(intelligentObjects,mapper,ready,ui,mode=if(cameraHandsOnly) "camera" else "ar")) { output ->
            val state=output.intelligence; val action=output.action
            val cursor=state.hands.firstOrNull { it.id==state.primaryHandId }?.filteredPinch
            handCursor=cursor?.let { ArVector2(it.x.toFloat(),it.y.toFloat()) }
            when(state.phase) {
                com.indianservers.aiexplorer.handintelligence.InteractionPhase.BEGIN -> if(action!=null && action.interactions.isNotEmpty()) {
                    finishHandGesture(true)
                    handTargetId=action.objectId
                    handCadGeometry=presentedScene.primitives.firstOrNull { it.id==action.objectId }?.geometry
                    val solid=action.objectId.removePrefix("solid-").toIntOrNull()?.takeIf(vm.state.solids.indices::contains)
                    val vector=action.objectId.removePrefix("vector-").toIntOrNull()?.takeIf(vm.state.vectors3D.indices::contains)
                    val shape=vm.state.shapes.indexOfFirst { it.id==action.objectId }
                    when { solid!=null -> { vm.selectSolid(solid); vm.beginSolidDrag(solid) }; vector!=null -> vm.beginVectorDrag(vector); shape>=0 -> vm.beginShapeDrag(shape); else -> vm.beginArGraphObjectGesture() }
                    arSelection=arSelection.copy(objectIds=setOf(action.objectId),primaryObjectId=action.objectId)
                    handGestureActive=true; handStatus="Hold and move naturally"
                }
                com.indianservers.aiexplorer.handintelligence.InteractionPhase.UPDATE -> if(handGestureActive && action!=null && action.interactions.isNotEmpty()) { vm.previewIntelligentHand(action,handCadGeometry); handStatus=action.interactions.joinToString(" · ") { it.name.lowercase().replace('_',' ') } }
                com.indianservers.aiexplorer.handintelligence.InteractionPhase.END -> { finishHandGesture(false); handStatus="Released" }
                com.indianservers.aiexplorer.handintelligence.InteractionPhase.CANCEL -> { finishHandGesture(true); handStatus="Tracking paused" }
                com.indianservers.aiexplorer.handintelligence.InteractionPhase.INSPECT -> handStatus=action?.inspection?.entries?.joinToString(" · ") { "${it.key} ${trim(it.value)}" } ?: "Inspecting"
                else -> if(!handGestureActive) handStatus=if(ready) "Reach toward an object to interact" else "Waiting for camera and placement"
            }
        }
    }
'''+s[b:]
# vector transactions need to commit using their own model history.
s=s.replace('        val shape = vm.state.shapes.indexOfFirst { it.id == handTargetId }.takeIf { it >= 0 }\n        when {','        val shape = vm.state.shapes.indexOfFirst { it.id == handTargetId }.takeIf { it >= 0 }\n        val vector = handTargetId?.removePrefix("vector-")?.toIntOrNull()?.takeIf(vm.state.vectors3D.indices::contains)\n        when {',1)
s=s.replace('            solid != null -> if (cancel) vm.cancelSolidDrag() else vm.endSolidDrag()','            vector != null -> if (cancel) vm.cancelVectorDrag() else vm.endVectorDrag()\n            solid != null -> if (cancel) vm.cancelSolidDrag() else vm.endSolidDrag()',1)
s=s.replace('            arWorkspaceMode == ArMathWorkspaceMode.Graph3D -> vm.endArGraphObjectGesture(cancel)\n            else -> if (cancel) vm.cancelSpatialGesture() else vm.endSpatialGesture()','            else -> vm.endArGraphObjectGesture(cancel)',1)
# Reset common state on every lifecycle/manual ownership transition.
s=s.replace('handController.reset();','intelligenceSession.reset(); handController.reset();')
s=s.replace('transparentBackground=cameraHandsOnly)\n        } else {','transparentBackground=cameraHandsOnly,onProjection={ intelligenceSession.projection=it })\n        } else {',1)
a=s.index('            SpatialPreviewCanvas(',s.index('onProjection={ intelligenceSession.projection=it }')); b=s.index('\n        }',a)
s=s[:a]+'''            ArCadPreviewCanvas(Modifier.fillMaxSize(),presentedScene,arSelection,ArSubObjectKind.Whole,
                onSelect={ hit -> arSelection=if(hit==null) ArSelectionState() else ArSelectionEngine.select(arSelection,hit,false) },
                onStart={ graphTouchActive=true; intelligenceSession.reset(); finishHandGesture(true) },
                onEdit={ _,_,_,_,_ -> },onEnd={ graphTouchActive=false },onInspect={},transparentBackground=cameraHandsOnly,
                onProjection={ intelligenceSession.projection=it })'''+s[b:]
p.write_text(s,encoding='utf-8')
# Preserve frozen coordinate mapping when a support hand joins/leaves.
p=r/'arengine/src/main/java/com/indianservers/aiexplorer/handintelligence/spatial/SpatialProcessingLayer.kt'; s=p.read_text(encoding='utf-8').replace('scene.mapper.atDepth(it.filteredPinch,anchor)','(baselineMapper ?: scene.mapper).atDepth(it.filteredPinch,anchor)');p.write_text(s,encoding='utf-8')

