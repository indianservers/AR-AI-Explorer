package com.indianservers.aiexplorer.mathworkspace.mathematicalart

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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.indianservers.aiexplorer.mathworkspace.MathWorkspaceShell
import com.indianservers.aiexplorer.mathworkspace.components.*
import com.indianservers.aiexplorer.mathworkspace.components.MathWorkspacePalette as P
import kotlinx.coroutines.delay

@Composable
fun MathematicalArtWorkspace(viewModel:MathematicalArtViewModel,onBack:()->Unit){
    val vm=viewModel;val mode by vm.mode;val playing by vm.animate;val context= LocalContext.current
    var modeMenu by remember{mutableStateOf(false)};var presetMenu by remember{mutableStateOf(false)};var paletteMenu by remember{mutableStateOf(false)};var equationDialog by remember{mutableStateOf(false)};var exportMenu by remember{mutableStateOf(false)}
    LaunchedEffect(playing){if(!playing)return@LaunchedEffect;var last=0L;while(vm.animate.value){withFrameNanos{now->if(last!=0L)vm.progress.value=(vm.progress.value+(now-last)/1e9*vm.speed.value/5).toFloat().coerceAtMost(1f);last=now};if(vm.progress.value>=1f)vm.animate.value=false}}
    val summary=when(mode){ArtMode.Spirograph->"Spirograph · R ${vm.fixedRadius.value} · r ${vm.rollingRadius.value} · d ${vm.penOffset.value}";ArtMode.Rose->"Rose · k ${vm.k.value}";ArtMode.Lissajous->"Lissajous · ${vm.freqA.value}:${vm.freqB.value}";ArtMode.Polar->"Polar · r = ${vm.polar.value}";ArtMode.Parametric->"x = ${vm.xExpr.value}, y = ${vm.yExpr.value}"}
    MathWorkspaceShell(title="Mathematical Art",subtitle="Explore curves made from equations",onBack=onBack,sheetTitle=if(vm.sheet.value==WorkspaceSheetStop.Collapsed)summary else mode.label,sheetStop=vm.sheet.value,onSheetStopChange={vm.sheet.value=it},
        topBarTrailing={Box{WorkspaceAction("${mode.label} ▾","Choose curve family",{modeMenu=true});DropdownMenu(modeMenu,{modeMenu=false}){ArtMode.entries.forEach{m->DropdownMenuItem(text={Text(m.label)},onClick={vm.selectMode(m);modeMenu=false})}}}},
        canvas={ArtRenderer(vm)},
        tools={if(!vm.focus.value){Row(Modifier.align(Alignment.TopEnd).padding(7.dp).horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(4.dp)){
            WorkspaceAction(if(playing)"Ⅱ"else"▶",if(playing)"Pause trace"else"Animate curve",{if(vm.progress.value>=1f)vm.progress.value=0f;vm.animate.value=!vm.animate.value},selected=playing)
            WorkspaceAction("Fit","Fit artwork",{vm.fitArtwork()});WorkspaceAction("↺","Reset viewport",{vm.reset()})
            Box{WorkspaceAction("Presets","Choose a mathematical preset",{presetMenu=true});DropdownMenu(presetMenu,{presetMenu=false}){presets(mode).forEach{name->DropdownMenuItem(text={Text(name)},onClick={vm.preset(name);presetMenu=false;vm.sheet.value=WorkspaceSheetStop.Collapsed})}}}
            Box{WorkspaceAction("Color","Choose artwork palette",{paletteMenu=true});DropdownMenu(paletteMenu,{paletteMenu=false}){ArtPalette.entries.forEach{palette->DropdownMenuItem(text={Text(palette.label)},onClick={vm.palette.value=palette;vm.layers.value=vm.layers.value.mapIndexed{i,l->l.copy(color=palette.colors[i%palette.colors.size])};paletteMenu=false})}}}
            Box{WorkspaceAction("Export","Share artwork as PNG",{exportMenu=true});DropdownMenu(exportMenu,{exportMenu=false}){listOf("Screen size" to 1200,"2× resolution" to 2400,"4× resolution" to 4800).forEach{(label,size)->DropdownMenuItem(text={Text(label)},onClick={ArtExport.share(context,vm.layers.value,size);exportMenu=false})}}}
            WorkspaceAction("ƒ","Show active equations",{equationDialog=true})
        }}else Box(Modifier.align(Alignment.TopEnd).padding(8.dp)){WorkspaceAction("↗","Exit canvas focus",{vm.focus.value=false})}
            Row(Modifier.align(Alignment.TopStart).padding(7.dp),horizontalArrangement=Arrangement.spacedBy(5.dp)){
                WorkspaceAction("⊞","Toggle grid",{vm.showGrid.value=!vm.showGrid.value},selected=vm.showGrid.value);WorkspaceAction("Axes","Toggle axes",{vm.showAxes.value=!vm.showAxes.value},selected=vm.showAxes.value);WorkspaceAction("Origin","Toggle origin",{vm.showOrigin.value=!vm.showOrigin.value},selected=vm.showOrigin.value)
                WorkspaceAction("Focus","Canvas focus mode",{vm.focus.value=true;vm.sheet.value=WorkspaceSheetStop.Collapsed})
            }
        },
        sheet={if(!vm.focus.value)Column(Modifier.weight(1f,fill=false).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(9.dp)){
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp),verticalAlignment=Alignment.CenterVertically){WorkspaceAction("Randomize","Create a valid random curve",{vm.randomize()});WorkspaceAction("＋ Layer","Duplicate active curve as a new layer",{vm.addLayer()},enabled=vm.layers.value.size<4);WorkspaceAction(if(vm.animate.value)"Pause"else"Trace","Animate curve drawing",{if(vm.progress.value>=1f)vm.progress.value=0f;vm.animate.value=!vm.animate.value},selected=vm.animate.value)}
            Row(verticalAlignment=Alignment.CenterVertically){Text("Trace speed",Modifier.weight(1f),color=P.muted,fontSize=11.sp);Text("${vm.speed.value}×",color=P.ink,fontSize=12.sp)}
            Slider(vm.speed.value,{vm.speed.value=it},valueRange=.25f..4f)
            if(vm.layers.value.size>1){WorkspaceSectionTitle("Curves");vm.layers.value.forEachIndexed{i,layer->Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)){Text(layer.name,Modifier.weight(1f),color=P.ink,fontSize=12.sp);WorkspaceAction(if(layer.visible)"Shown"else"Hidden","Toggle ${layer.name}",{vm.toggleLayer(i)});WorkspaceAction("Delete","Delete ${layer.name}",{vm.deleteLayer(i)})}}}
            WorkspaceSectionTitle("Curve parameters",trailing={WorkspaceAction("Equation","Edit curve equation",{vm.editorOpen.value=!vm.editorOpen.value},selected=vm.editorOpen.value)})
            when(mode){
                ArtMode.Spirograph->{WorkspaceSegmentedControl(listOf("Hypotrochoid","Epitrochoid"),vm.spiroType.value.ordinal){vm.spiroType.value=SpiroType.entries[it];vm.refresh()};NumericPair("R" to vm.fixedRadius.value,"r" to vm.rollingRadius.value,{vm.fixedRadius.value=it;vm.refresh()},{vm.rollingRadius.value=it;vm.refresh()});NumericPair("d" to vm.penOffset.value,"Rotations" to vm.rotations.value,{vm.penOffset.value=it;vm.refresh()},{vm.rotations.value=it;vm.refresh()});NumericField("Phase (rad)",vm.phase.value,{vm.phase.value=it;vm.refresh()},Modifier.fillMaxWidth())}
                ArtMode.Rose->{NumericPair("a" to vm.amplitude.value,"k" to vm.k.value,{vm.amplitude.value=it;vm.refresh()},{vm.k.value=it;vm.refresh()});NumericPair("Phase (rad)" to vm.phase.value,"θ turns" to vm.roseTurns.value,{vm.phase.value=it;vm.refresh()},{vm.roseTurns.value=it;vm.refresh()});Row(verticalAlignment=Alignment.CenterVertically){Text("Use sine form",Modifier.weight(1f),color=P.ink,fontSize=12.sp);Switch(vm.sine.value,{vm.sine.value=it;vm.refresh()})};val petals=vm.k.value.toDoubleOrNull()?.let(RoseCurveEngine::typicalPetals);Text(if(petals==null)"Fractional k creates a non-integer repeating pattern."else"For integer k = ${vm.k.value}, this curve typically has $petals petals.",color=P.muted,fontSize=11.sp)}
                ArtMode.Lissajous->{NumericPair("A" to vm.A.value,"B" to vm.B.value,{vm.A.value=it;vm.refresh()},{vm.B.value=it;vm.refresh()});NumericPair("a" to vm.freqA.value,"b" to vm.freqB.value,{vm.freqA.value=it;vm.refresh()},{vm.freqB.value=it;vm.refresh()});NumericPair("δ (rad)" to vm.delta.value,"t turns" to vm.lissajousTurns.value,{vm.delta.value=it;vm.refresh()},{vm.lissajousTurns.value=it;vm.refresh()})}
                ArtMode.Polar->{if(vm.editorOpen.value)ExpressionField("r = f(θ)",vm.polar.value,{vm.polar.value=it;vm.refresh()},"1 + cos(theta)");else Text("r = ${vm.polar.value}",color=P.ink,fontSize=13.sp,fontWeight=FontWeight.SemiBold);NumericPair("θ min" to vm.polarMin.value,"θ max" to vm.polarMax.value,{vm.polarMin.value=it;vm.refresh()},{vm.polarMax.value=it;vm.refresh()})}
                ArtMode.Parametric->{if(vm.editorOpen.value){ExpressionField("x(t)",vm.xExpr.value,{vm.xExpr.value=it;vm.refresh()},"cos(t)");ExpressionField("y(t)",vm.yExpr.value,{vm.yExpr.value=it;vm.refresh()},"sin(t)")}else Text("x = ${vm.xExpr.value}   ·   y = ${vm.yExpr.value}",color=P.ink,fontSize=13.sp,fontWeight=FontWeight.SemiBold);NumericPair("t min" to vm.tMin.value,"t max" to vm.tMax.value,{vm.tMin.value=it;vm.refresh()},{vm.tMax.value=it;vm.refresh()})}
            }
            vm.error.value?.let{Text(it,color=P.red,fontSize=11.sp)}
            Row(verticalAlignment=Alignment.CenterVertically){Text("Grid",Modifier.weight(1f),color=P.ink,fontSize=12.sp);Switch(vm.showGrid.value,{vm.showGrid.value=it})}
            Row(verticalAlignment=Alignment.CenterVertically){Text("Axes",Modifier.weight(1f),color=P.ink,fontSize=12.sp);Switch(vm.showAxes.value,{vm.showAxes.value=it})}
        }}
    )
    if(equationDialog){AlertDialog(onDismissRequest={equationDialog=false},title={Text("Curve equations")},text={Text(equationText(mode,vm))},confirmButton={TextButton(onClick={equationDialog=false}){Text("Done")}})}
}

@Composable private fun NumericPair(a:Pair<String,String>,b:Pair<String,String>,onA:(String)->Unit,onB:(String)->Unit){Row(horizontalArrangement=Arrangement.spacedBy(7.dp)){NumericField(a.first,a.second,onA,Modifier.weight(1f));NumericField(b.first,b.second,onB,Modifier.weight(1f))}}
@Composable private fun ExpressionField(label:String,value:String,onChange:(String)->Unit,hint:String){Column(verticalArrangement=Arrangement.spacedBy(3.dp)){Text(label,color=P.muted,fontSize=10.sp);BasicTextField(value,onChange,Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color.White).border(1.dp,P.border,RoundedCornerShape(10.dp)).padding(10.dp).semantics{contentDescription=label},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Ascii),textStyle=TextStyle(color=P.ink,fontSize=13.sp),decorationBox={inner->Box{if(value.isEmpty())Text(hint,color=P.muted);inner()}})}}
private fun presets(mode:ArtMode)=when(mode){ArtMode.Spirograph->listOf("Classic Bloom","Star Orbit","Nested Petals");ArtMode.Rose->listOf("5 Petals","8 Petals","Fractional Rose");ArtMode.Lissajous->listOf("1:1","2:1","3:2","5:4");ArtMode.Polar->listOf("Cardioid","Limacon","Spiral","Lemniscate");ArtMode.Parametric->listOf("Circle","Ellipse","Figure Eight","Hypotrochoid-like")}
private fun equationText(mode:ArtMode,vm:MathematicalArtViewModel)=when(mode){ArtMode.Spirograph->if(vm.spiroType.value==SpiroType.Hypotrochoid)"x(t) = (R−r)cos(t) + d cos((R−r)t/r)\ny(t) = (R−r)sin(t) − d sin((R−r)t/r)"else"x(t) = (R+r)cos(t) − d cos((R+r)t/r)\ny(t) = (R+r)sin(t) − d sin((R+r)t/r)";ArtMode.Rose->"r = a ${if(vm.sine.value)"sin"else"cos"}(kθ + φ)\nx = r cos(θ), y = r sin(θ)";ArtMode.Lissajous->"x(t) = A sin(a t + δ)\ny(t) = B sin(b t)";ArtMode.Polar->"r(θ) = ${vm.polar.value}\nx = r cos(θ), y = r sin(θ)";ArtMode.Parametric->"x(t) = ${vm.xExpr.value}\ny(t) = ${vm.yExpr.value}"}

