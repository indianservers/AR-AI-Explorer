from pathlib import Path
r=Path(r'C:\Indian Servers\AIExplorer');p=r/'arengine/src/main/java/com/indianservers/aiexplorer/handintelligence/spatial/ContactSolver.kt';s=p.read_text(encoding='utf-8')
s=s.replace('if(index==0) RegionKind.VECTOR_ORIGIN else if(index==1) RegionKind.VECTOR_HEAD else RegionKind.VERTEX','if(index==0) RegionKind.VECTOR_ORIGIN else RegionKind.VECTOR_HEAD')
s=s.replace('region=SemanticHitRegion("vertex-$index",kind,index,position=v,precision=.95f); position=v; distance=d','val semanticIndex=if(obj.semanticType==MathSemanticType.VECTOR && index>0) 1 else index\n                    position=obj.vertices[semanticIndex];region=SemanticHitRegion("vertex-$semanticIndex",kind,semanticIndex,position=position,precision=.95f);distance=d')
p.write_text(s,encoding='utf-8')
p=r/'arengine/src/main/java/com/indianservers/aiexplorer/handintelligence/semantics/MathSemanticEngine.kt';s=p.read_text(encoding='utf-8')
s=s.replace('            obj.inspect?.let', '''            if(target.region.kind==RegionKind.CURVE) obj.edges.getOrNull(target.region.index ?: -1)?.let { (a,b) -> val d=obj.vertices[b]-obj.vertices[a]; if(abs(d.x)>1e-8) put("sampled slope",d.y/d.x) }
            obj.inspect?.let''')
p.write_text(s,encoding='utf-8')
p=r/'app/src/main/java/com/indianservers/aiexplorer/HandMathSceneAdapter.kt';s=p.read_text(encoding='utf-8')
s=s.replace('        val evaluator:((Vec3)->Map<String,Double>)?=compiled?.let', '''        val curveExpressions=if(node?.type==ArCadType.Curve) listOf("expressionX","expressionY","expressionZ").mapIndexed { i,key -> compile(node.parameters[key] ?: listOf("cos(t)","sin(t)","t/4")[i]) } else emptyList()
        val curveInspector:((Vec3)->Map<String,Double>)?=if(node?.type==ArCadType.Curve && curveExpressions.all { it!=null }) { point:Vec3 ->
            val raw=t.orientation.conjugate().rotate(point-t.offsetMeters)*(1/t.uniformScale);val local=Vec3(raw.x/t.axisScale.x,raw.y/t.axisScale.y,raw.z/t.axisScale.z)
            var nearest=Double.MAX_VALUE;var fraction=0.0
            p.geometry.lines.forEach { (a,b) -> val va=p.geometry.vertices[a];val vb=p.geometry.vertices[b];val start=Vec3(va.x,va.y,va.z);val d=Vec3(vb.x-va.x,vb.y-va.y,vb.z-va.z);val q=((local-start).dot(d)/d.dot(d).coerceAtLeast(1e-12)).coerceIn(0.0,1.0);val gap=(local-start-d*q).magnitude();if(gap<nearest) { nearest=gap;fraction=(a+(b-a)*q)/(p.geometry.vertices.size-1).coerceAtLeast(1) } }
            val min=ArCadTopology.number(node.parameters["tMin"] ?: "0");val max=ArCadTopology.number(node.parameters["tMax"] ?: "2*pi");val parameter=min+(max-min)*fraction;val h=1e-4
            val derivative=curveExpressions.map { expression -> (expression!!.eval(parameters+mapOf("t" to parameter+h))-expression.eval(parameters+mapOf("t" to parameter-h)))/(2*h) }
            linkedMapOf("t" to parameter,"dx/dt" to derivative[0],"dy/dt" to derivative[1],"dz/dt" to derivative[2]).apply { if(abs(derivative[0])>1e-8) put("slope",derivative[1]/derivative[0]) }.filterValues { it.isFinite() }
        } else null
        val evaluator:((Vec3)->Map<String,Double>)?=compiled?.let''')
s=s.replace('allowed=allowed,inspect=evaluator,','allowed=allowed,inspect=evaluator ?: curveInspector,')
s=s.replace('fun replay(onResult:(List<MathInteractionFrame>)->Unit) { executor.execute', 'fun replay(onResult:(List<MathInteractionFrame>)->Unit) { recorder.enabled=false; executor.execute')
p.write_text(s,encoding='utf-8')
p=r/'app/src/main/java/com/indianservers/aiexplorer/MainActivity.kt';s=p.read_text(encoding='utf-8')
s=s.replace('    var lastHandUiUpdate by remember', '    var handModeUiRegion by remember { mutableStateOf<com.indianservers.aiexplorer.handintelligence.UiRegion?>(null) }\n    var handToolbarUiRegion by remember { mutableStateOf<com.indianservers.aiexplorer.handintelligence.UiRegion?>(null) }\n    var lastHandUiUpdate by remember',1)
s=s.replace('                add(com.indianservers.aiexplorer.handintelligence.UiRegion(.55,.65,1.0,.89))','                handModeUiRegion?.let(::add)\n                handToolbarUiRegion?.let(::add)')
s=s.replace('Column(Modifier.align(Alignment.BottomEnd).windowInsetsPadding', '''Column(Modifier.align(Alignment.BottomEnd).then(Modifier.onGloballyPositioned { coordinates -> val b=coordinates.boundsInParent(); val w=viewportSize.width.coerceAtLeast(1).toDouble();val h=viewportSize.height.coerceAtLeast(1).toDouble();handModeUiRegion=com.indianservers.aiexplorer.handintelligence.UiRegion(b.left/w,b.top/h,b.right/w,b.bottom/h) }).windowInsetsPadding''',1)
s=s.replace('modifier = Modifier.align(Alignment.TopStart).padding(10.dp).onSizeChanged { handHudSize = it },','modifier = Modifier.align(Alignment.TopStart).padding(10.dp).onSizeChanged { handHudSize = it }.onGloballyPositioned { coordinates -> val b=coordinates.boundsInParent();val w=viewportSize.width.coerceAtLeast(1).toDouble();val h=viewportSize.height.coerceAtLeast(1).toDouble();handToolbarUiRegion=com.indianservers.aiexplorer.handintelligence.UiRegion(b.left/w,b.top/h,b.right/w,b.bottom/h) },',1)
s=s.replace('import androidx.compose.ui.layout.onSizeChanged','import androidx.compose.ui.layout.onGloballyPositioned\nimport androidx.compose.ui.layout.boundsInParent\nimport androidx.compose.ui.layout.onSizeChanged',1)
s=s.replace('"${it.intelligence.primaryIntent} · ${it.intelligence.intentConfidence}', '"expected ${intelligenceSession.trainingLabel ?: "unlabelled"} · detected ${it.intelligence.primaryIntent} · ${it.intelligence.intentConfidence}')
p.write_text(s,encoding='utf-8')
