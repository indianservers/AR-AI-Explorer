package com.indianservers.aiexplorer.mathworkspace.lineartransform

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.indianservers.aiexplorer.mathworkspace.MathWorkspaceShell
import com.indianservers.aiexplorer.mathworkspace.components.*
import com.indianservers.aiexplorer.mathworkspace.components.MathWorkspacePalette as P

@Composable
fun MatrixTransformWorkspace(viewModel:MatrixTransformViewModel,onBack:()->Unit){
    val vm=viewModel;val mode by vm.mode;val playing by vm.playing;var modeMenu by remember{mutableStateOf(false)};var presetsMenu by remember{mutableStateOf(false)}
    LaunchedEffect(playing){if(!playing)return@LaunchedEffect;var last=0L;while(vm.playing.value){withFrameNanos{now->if(last!=0L){val next=vm.progress.floatValue+(now-last)/1e9*vm.speed.floatValue/3;vm.progress.floatValue=next.toFloat().coerceAtMost(1f)};last=now};if(vm.progress.floatValue>=1f)vm.playing.value=false}}
    val matrix=vm.target;val visualMatrix=if(mode==MatrixMode.Composition)vm.displayed else matrix;val det=visualMatrix?.determinant;val summary=if(matrix==null)"Enter a valid 2 × 2 matrix"else if(mode==MatrixMode.Composition)"${if(vm.reverseOrder.value)"BA"else"AB"} · det ${det?.let(::short)?:"—"}"else "A = [[${short(matrix.a)}, ${short(matrix.b)}], [${short(matrix.c)}, ${short(matrix.d)}]] · det ${det?.let(::short)?:"—"}"
    MathWorkspaceShell(title="Matrices & Linear Transformations",subtitle="See how matrices move space",onBack=onBack,sheetTitle=if(vm.sheet.value==WorkspaceSheetStop.Collapsed)summary else mode.title,sheetStop=vm.sheet.value,onSheetStopChange={vm.sheet.value=it},
        topBarTrailing={Box{WorkspaceAction("${mode.title} ▾","Choose matrix workspace mode",{modeMenu=true});DropdownMenu(modeMenu,{modeMenu=false}){MatrixMode.entries.forEach{m->DropdownMenuItem(text={Text(m.title)},onClick={vm.mode.value=m;vm.sheet.value=WorkspaceSheetStop.Peek;modeMenu=false})}}}},
        canvas={TransformRenderer(vm)},
        tools={
            Row(Modifier.align(Alignment.TopEnd).padding(6.dp).horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(4.dp)){
                WorkspaceAction(if(playing)"Ⅱ"else"▶",if(playing)"Pause transform animation"else"Animate identity to matrix",{vm.previewInverse.value=false;if(vm.progress.floatValue>=1f)vm.progress.floatValue=0f;vm.playing.value=!vm.playing.value},selected=playing)
                WorkspaceAction("↻","Reset transformation progress",{vm.playing.value=false;vm.progress.floatValue=0f})
                WorkspaceAction("Fit","Fit transformed object",{vm.fit()});WorkspaceAction("↺","Reset view",{vm.reset()})
                Box{WorkspaceAction("Presets","Choose a matrix preset",{presetsMenu=true});DropdownMenu(presetsMenu,{presetsMenu=false}){MatrixPresets.all.forEach{p->DropdownMenuItem(text={Text(p.name)},onClick={vm.preset(p);presetsMenu=false})}}}
                WorkspaceAction("ƒ","Show matrix equations",{vm.equationOpen.value=true})
            }
            Column(Modifier.align(Alignment.TopStart).padding(8.dp).clip(RoundedCornerShape(10.dp)).background(P.panel.copy(alpha=.94f)).padding(9.dp),verticalArrangement=Arrangement.spacedBy(2.dp)){
                Text("det = ${det?.let(::short)?:"—"} · area scale ${det?.let{short(kotlin.math.abs(it))}?:"—"}",color=P.ink,fontSize=11.sp,fontWeight=FontWeight.SemiBold)
                val orientation=visualMatrix?.let{DeterminantEngine.orientation(it)}
                Text(when(orientation){Orientation.Preserved->"↺ Orientation preserved";Orientation.Reversed->"↻ Orientation reversed";Orientation.Collapsed->"⚠ Singular · dimension collapse";null->"Invalid matrix"},color=when(orientation){Orientation.Reversed->P.red;Orientation.Collapsed->P.amber;else->P.muted},fontSize=10.sp)
            }
        },
        sheet={Column(Modifier.weight(1f,fill=false).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){
            when(mode){
                MatrixMode.Transform->{MatrixInput(vm,"A");MatrixPresetRow(vm);ShapeSelector(vm)}
                MatrixMode.Basis->{MatrixInput(vm,"A");Row(verticalAlignment=Alignment.CenterVertically){Text("Lock Ae₁",Modifier.weight(1f),color=P.ink,fontSize=11.sp);Switch(vm.lockE1.value,{vm.lockE1.value=it});Text("Lock Ae₂",color=P.ink,fontSize=11.sp);Switch(vm.lockE2.value,{vm.lockE2.value=it})};Text("Drag the blue basis handles to edit matrix columns. e₁ → (${short(matrix?.a?:0.0)}, ${short(matrix?.c?:0.0)}) · e₂ → (${short(matrix?.b?:0.0)}, ${short(matrix?.d?:0.0)})",color=P.muted,fontSize=11.sp);WorkspaceAction("Reset basis","Reset matrix to identity",{vm.setMatrix(MatrixMathEngine.identity)})}
                MatrixMode.Eigen->{MatrixInput(vm,"A");EigenSummary(vm)}
                MatrixMode.Composition->{MatrixInput(vm,"A");MatrixInput(vm,"B",second=true);ShapeSelector(vm);Row(verticalAlignment=Alignment.CenterVertically){Text(if(vm.reverseOrder.value)"Order: BA"else"Order: AB",Modifier.weight(1f),color=P.ink,fontSize=12.sp,fontWeight=FontWeight.SemiBold);WorkspaceAction("⇄","Switch composition order AB versus BA",{vm.reverseOrder.value=!vm.reverseOrder.value;vm.progress.floatValue=1f})};WorkspaceAction(if(vm.compare.value)"Hide order comparison"else"Compare AB and BA","Show both composition results on the canvas",{vm.compare.value=!vm.compare.value});Text("AB = ${matrixText(vm.source*vm.bMatrix)}\nBA = ${matrixText(vm.bMatrix*vm.source)}",color=P.muted,fontSize=11.sp);WorkspaceAction("Apply composition","Use selected composition as A",{vm.applyComposition()})}
                MatrixMode.Tools->{MatrixInput(vm,"A");MatrixPresetRow(vm);WorkspaceSectionTitle("Matrix operations");Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(4.dp)){WorkspaceAction("Transpose","Apply transpose",{vm.applyTranspose()});WorkspaceAction("Inverse","Apply inverse when nonsingular",{vm.applyInverse()},enabled=matrix?.inverse()!=null);WorkspaceAction(if(vm.previewInverse.value)"Hide preview"else"Preview inverse","Preview inverse transform without changing A",{vm.previewInverse.value=!vm.previewInverse.value;vm.fit()},selected=vm.previewInverse.value,enabled=matrix?.inverse()!=null);WorkspaceAction("Fit","Fit canvas",{vm.fit()})};MatrixInput(vm,"B",second=true);Text("AB = ${matrix?.let{matrixText(it*vm.bMatrix)}?:"—"}",color=P.ink,fontSize=12.sp);WorkspaceAction("Apply AB","Set A to the product AB",{matrix?.let{vm.setMatrix(it*vm.bMatrix)}});NumericField("Rotation θ · °",vm.rotationDegrees.value,{vm.rotationDegrees.value=it},Modifier.fillMaxWidth());WorkspaceAction("Apply rotation","Generate rotation matrix from angle",{vm.applyRotation()});Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){NumericField("Vector x",vm.vectorX.value,{vm.vectorX.value=it},Modifier.weight(1f));NumericField("Vector y",vm.vectorY.value,{vm.vectorY.value=it},Modifier.weight(1f))};Text("v = ${vectorText(vm.vector)} · Av = ${vectorText((matrix?:MatrixMathEngine.identity)*vm.vector)}",color=P.blue,fontSize=11.sp);Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){NumericField("b₁",vm.rhsX.value,{vm.rhsX.value=it},Modifier.weight(1f));NumericField("b₂",vm.rhsY.value,{vm.rhsY.value=it},Modifier.weight(1f))};val solve=matrix?.let{MatrixMathEngine.solve(it,vm.rhs)};Text(when(solve){is LinearSolveResult.Unique->"Ax=b solution x = ${vectorText(solve.value)}";LinearSolveResult.Infinite->"Ax=b has infinitely many solutions";LinearSolveResult.None->"Ax=b has no solution";null->"Enter valid matrix values"},color=P.muted,fontSize=11.sp)}
            }
            if(matrix==null)Text("Use finite numbers such as −2.75, 1e−5, or 3000.",color=P.red,fontSize=11.sp)
            Row(horizontalArrangement=Arrangement.spacedBy(7.dp),verticalAlignment=Alignment.CenterVertically){Text("Animation speed",Modifier.weight(1f),color=P.muted,fontSize=10.sp);Text("${vm.speed.floatValue}×",color=P.ink,fontSize=10.sp)}
            Slider(vm.speed.floatValue,{vm.speed.floatValue=it},valueRange=.25f..3f)
            Row(horizontalArrangement=Arrangement.spacedBy(7.dp),verticalAlignment=Alignment.CenterVertically){Text("Transform progress · t",Modifier.weight(1f),color=P.muted,fontSize=10.sp);Text("${(vm.progress.floatValue*100).toInt()}%",color=P.ink,fontSize=10.sp)}
            Slider(vm.progress.floatValue,{vm.progress.floatValue=it;vm.playing.value=false},valueRange=0f..1f)
            WorkspaceSectionTitle("Canvas layers")
            Toggle("Base grid",vm.baseGrid.value){vm.baseGrid.value=it};Toggle("Transformed grid",vm.transformedGrid.value){vm.transformedGrid.value=it};Toggle("Axes",vm.showAxes.value){vm.showAxes.value=it};Toggle("Labels",vm.showLabels.value){vm.showLabels.value=it};Toggle("Original shape",vm.showOriginal.value){vm.showOriginal.value=it};Toggle("Transformed shape",vm.showTransformed.value){vm.showTransformed.value=it};Toggle("Basis vectors",vm.showBasis.value){vm.showBasis.value=it}
            if(mode==MatrixMode.Tools){val m=matrix?:MatrixMathEngine.identity;Text("tr(A) = ${short(m.trace)} · det(A) = ${short(m.determinant)} · rank ${MatrixMathEngine.rank(m)}",color=P.ink,fontSize=11.sp);if(m.inverse()==null)Text("A is singular: inverse unavailable.",color=P.amber,fontSize=11.sp)else Text("A⁻¹ = ${matrixText(m.inverse()!!)}",color=P.muted,fontSize=11.sp)}
        }}
    )
    if(vm.equationOpen.value) AlertDialog(
        onDismissRequest={vm.equationOpen.value=false},
        title={Text("Linear transformation")},
        text={Text("p′ = Ap\ndet(A) = ad − bc\nFor invertible A: A⁻¹ = 1/det(A) [[d, −b], [−c, a]]\nEigenvectors satisfy Av = λv\nComposition: (AB)v = A(Bv)")},
        confirmButton={TextButton(onClick={vm.equationOpen.value=false}){Text("Done")}},
    )
}

@Composable private fun MatrixInput(vm:MatrixTransformViewModel,title:String,second:Boolean=false){
    WorkspaceSectionTitle("Matrix $title")
    val fields=if(second)listOf(vm.ba,vm.bb,vm.bc,vm.bd)else listOf(vm.a,vm.b,vm.c,vm.d)
    Column(verticalArrangement=Arrangement.spacedBy(4.dp)){for(row in 0..1)Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){for(col in 0..1){val state=fields[row*2+col];MatrixCell("$title row ${row+1}, column ${col+1}",state.value,Modifier.weight(1f)){state.value=it;if(!second)vm.progress.floatValue=1f}}}}
}
@Composable private fun MatrixCell(label:String,value:String,modifier:Modifier,onChange:(String)->Unit){BasicTextField(value,onChange,modifier.heightIn(min=40.dp).clip(RoundedCornerShape(9.dp)).background(Color.White).border(1.dp,P.border,RoundedCornerShape(9.dp)).padding(horizontal=8.dp,vertical=8.dp).semantics{contentDescription=label},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Ascii),textStyle=TextStyle(color=P.ink,fontSize=14.sp,textAlign=TextAlign.Center),decorationBox={inner->Box(Modifier.fillMaxWidth(),contentAlignment=Alignment.Center){if(value.isEmpty())Text("0",color=P.muted);inner()}})}
@Composable private fun MatrixPresetRow(vm:MatrixTransformViewModel){WorkspaceSectionTitle("Presets");Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(4.dp)){MatrixPresets.all.forEach{p->WorkspaceAction(p.name,"Set matrix to ${p.name}",{vm.preset(p)})}};Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){NumericField("Rotation θ · °",vm.rotationDegrees.value,{vm.rotationDegrees.value=it},Modifier.weight(1f));WorkspaceAction("Apply rotation","Generate rotation matrix from angle",{vm.applyRotation()})}}
@Composable private fun ShapeSelector(vm:MatrixTransformViewModel){WorkspaceSectionTitle("Sample shape");Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp)){ShapeKind.entries.forEach{s->WorkspaceAction(s.title,"Show ${s.title}",{vm.shape.value=s;vm.fit()},selected=vm.shape.value==s)}}}
@Composable private fun EigenSummary(vm:MatrixTransformViewModel){val m=vm.target?:return;val e=EigenEngine.compute(m);Text("tr(A) = ${short(e.trace)} · det(A) = ${short(e.determinant)} · Δ = ${short(e.discriminant)}",color=P.muted,fontSize=11.sp);if(!e.real)Text("Complex eigenvalues: ${short(e.trace/2)} ± ${short(kotlin.math.sqrt(-e.discriminant)/2)}i. No real eigen-directions; the canvas shows the real rotation / scaling.",color=P.violet,fontSize=12.sp,fontWeight=FontWeight.SemiBold)else{e.eigenvalues.forEachIndexed{i,value->Text("λ${i+1} = ${short(value)}${e.vectors.getOrNull(i)?.let{" · v = ${vectorText(it)}"}?:""}",color=P.violet,fontSize=12.sp,fontWeight=FontWeight.SemiBold)};if(e.eigenspaceDimensions.firstOrNull()==2)Text("Repeated eigenvalue · every direction is an eigen-direction.",color=P.muted,fontSize=11.sp)else if(e.eigenvalues.size==1)Text("Repeated eigenvalue with one independent eigen-direction (defective).",color=P.amber,fontSize=11.sp);Text("λ₁ + λ₂ = tr(A); λ₁λ₂ = det(A) when eigenvalues are counted with multiplicity.",color=P.muted,fontSize=10.sp)}}
@Composable private fun Toggle(label:String,value:Boolean,onChange:(Boolean)->Unit){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(label,Modifier.weight(1f),color=P.ink,fontSize=11.sp);Switch(value,onChange)}}
private fun short(x:Double)=if(!x.isFinite())"∞"else if(kotlin.math.abs(x)>=1e5||kotlin.math.abs(x)<1e-4&&x!=0.0)"%.2e".format(x)else "%.3g".format(x)
private fun matrixText(m:Matrix2)="[[${short(m.a)}, ${short(m.b)}], [${short(m.c)}, ${short(m.d)}]]"
private fun vectorText(v:Vec2)="(${short(v.x)}, ${short(v.y)})"
