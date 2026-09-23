package com.indianservers.aiexplorer.mathworkspace.physicsmath

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.indianservers.aiexplorer.mathworkspace.MathWorkspaceShell
import com.indianservers.aiexplorer.mathworkspace.components.*
import com.indianservers.aiexplorer.mathworkspace.components.MathWorkspacePalette as P
import kotlinx.coroutines.delay
import kotlin.math.*

@Composable
fun PhysicsMathWorkspace(viewModel:PhysicsMathViewModel,onBack:()->Unit){
    val vm=viewModel;val mode by vm.mode;val experiment by vm.experiment
    val projectile=vm.projectile();val linear=vm.linear();val shm=vm.oscillation();val duration=vm.duration().coerceAtLeast(1e-6)
    val playing by vm.playing
    var speedMenu by remember{mutableStateOf(false)}
    LaunchedEffect(mode,experiment,projectile,linear,shm,vm.graphSeries.value){delay(180);vm.autoFit()}
    LaunchedEffect(playing,duration){
        if(!playing)return@LaunchedEffect
        var last=0L
        while(vm.playing.value){withFrameNanos{now->if(last!=0L){val delta=(now-last)/1e9*vm.playbackSpeed.floatValue;val next=vm.time.floatValue+delta.toFloat();if(next>=duration){vm.time.floatValue=duration.toFloat();vm.playing.value=false}else vm.time.floatValue=next};last=now}}
    }
    val valid=when(experiment){MotionExperiment.Projectile->projectile!=null;MotionExperiment.Linear->linear!=null;MotionExperiment.SHM->shm!=null}
    val summary=when(experiment){MotionExperiment.Projectile->projectile?.let{p->val q=ProjectileMotionEngine.summary(p);"Projectile · v₀ ${fmt(p.speed)} m/s · θ ${fmt(p.angleDegrees)}° · R ${q.range?.let(::fmt)?:"—"} m"}?:"Projectile · check parameters";MotionExperiment.Linear->"1D Motion · x₀ ${fmt(linear?.startX?:Double.NaN)} m · v₀ ${fmt(linear?.velocity?:Double.NaN)} m/s";MotionExperiment.SHM->"SHM · A ${fmt(shm?.amplitude?:Double.NaN)} m · f ${fmt(shm?.let{OscillationEngine.angularFrequency(it)/(2*PI)}?:Double.NaN)} Hz"}
    val sheetTitle=if(vm.sheet.value==WorkspaceSheetStop.Collapsed)summary else when(mode){PhysicsMode.Motion->experiment.title;PhysicsMode.Oscillations->"Simple Harmonic Motion";PhysicsMode.Graphs->"Graphs · ${vm.graphSeries.value.title}"}
    MathWorkspaceShell(
        title="Physics–Math Workspace",subtitle="Simulate · visualize · understand",onBack=onBack,
        sheetTitle=sheetTitle,sheetStop=vm.sheet.value,onSheetStopChange={vm.sheet.value=it},
        topBarTrailing={WorkspaceSegmentedControl(PhysicsMode.entries.map{it.title},mode.ordinal){selected->val next=PhysicsMode.entries[selected];vm.mode.value=next;if(next==PhysicsMode.Oscillations)vm.selectExperiment(MotionExperiment.SHM)else if(next==PhysicsMode.Motion&&vm.experiment.value==MotionExperiment.SHM)vm.selectExperiment(MotionExperiment.Projectile);vm.autoFit()}},
        canvas={PhysicsRenderer(vm)},
        tools={
            Row(Modifier.align(Alignment.TopEnd).padding(7.dp).horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp)){
                WorkspaceAction(if(playing)"Ⅱ"else"▶",if(playing)"Pause simulation"else"Play simulation",{if(vm.time.floatValue>=duration)vm.time.floatValue=0f;vm.playing.value=!vm.playing.value},selected=playing)
                WorkspaceAction("↺","Restart simulation",{vm.playing.value=false;vm.time.floatValue=0f})
                WorkspaceAction("Reset","Reset experiment parameters",{vm.resetExperiment()})
                WorkspaceAction("◎","Reset viewport",{vm.autoFit()})
                WorkspaceAction("ƒ","Show equations",{vm.equationOpen.value=true})
                Box{WorkspaceAction("${vm.playbackSpeed.floatValue}×","Playback speed",{speedMenu=true});DropdownMenu(speedMenu,{speedMenu=false}){listOf(.25f,.5f,1f,2f,4f).forEach{rate->DropdownMenuItem(text={Text("$rate×")},onClick={vm.playbackSpeed.floatValue=rate;speedMenu=false})}}}
            }
            PhysicsClock(vm,Modifier.align(Alignment.TopStart).padding(12.dp))
            PhysicsLiveReadout(vm,experiment,Modifier.align(Alignment.TopStart).padding(start=12.dp,top=48.dp))
        },
        sheet={
            val time=vm.time.floatValue.toDouble()
            val live=when(experiment){MotionExperiment.Projectile->projectile?.let{ProjectileMotionEngine.state(it,time)};MotionExperiment.Linear->linear?.let{PhysicsMathEngine.linear(it,time)};MotionExperiment.SHM->shm?.let{OscillationEngine.state(it,time)}}
            Column(Modifier.weight(1f,fill=false).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(9.dp)){
                if(mode==PhysicsMode.Motion||mode==PhysicsMode.Graphs){
                    if(mode==PhysicsMode.Motion){WorkspaceSegmentedControl(listOf(MotionExperiment.Projectile.title,"1D Motion"),if(experiment==MotionExperiment.Projectile)0 else 1){vm.selectExperiment(if(it==0)MotionExperiment.Projectile else MotionExperiment.Linear)}}
                    else {WorkspaceSegmentedControl(listOf("Projectile","1D Motion","SHM"),experiment.ordinal){vm.selectExperiment(MotionExperiment.entries[it])}}
                }
                when(experiment){
                    MotionExperiment.Projectile->ProjectileControls(vm,projectile)
                    MotionExperiment.Linear->LinearControls(vm,linear)
                    MotionExperiment.SHM->OscillationControls(vm,shm)
                }
                if(mode==PhysicsMode.Graphs){
                    WorkspaceSectionTitle("Graph series")
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp)){GraphSeries.choices(experiment).forEach{series->WorkspaceAction(series.title,"Plot ${series.title}",{vm.graphSeries.value=series;vm.autoFit()},selected=series==vm.graphSeries.value)}}
                }
                if(!valid)Text(errorFor(experiment),color=P.red,fontSize=11.sp)
                WorkspaceSectionTitle("Simulation time")
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically){NumericField("t · s",vm.time.floatValue.toString(),{it.toDoubleOrNull()?.takeIf(Double::isFinite)?.let(vm::scrub)},Modifier.weight(1f));Text("${fmt(time.coerceIn(0.0,duration))} / ${fmt(duration)} s",color=P.muted,fontSize=11.sp)}
                Slider(value=time.coerceIn(0.0,duration).toFloat(),onValueChange={vm.scrub(it.toDouble())},valueRange=0f..duration.toFloat(),enabled=valid,modifier=Modifier.semantics{contentDescription="Scrub simulation time"})
                if(mode==PhysicsMode.Motion&&experiment==MotionExperiment.Projectile)OverlayControls(vm)
                if(mode==PhysicsMode.Motion&&experiment==MotionExperiment.Projectile)ToggleSection("Advanced parameters",vm.advancedExpanded.value,{vm.advancedExpanded.value=!vm.advancedExpanded.value}){NumericField("x₀ · m",vm.startX.value,{vm.startX.value=it},Modifier.fillMaxWidth())}
                if(mode==PhysicsMode.Oscillations&&shm?.springMode==true)ToggleSection("Energy",vm.energyExpanded.value,{vm.energyExpanded.value=!vm.energyExpanded.value}){
                    val s=shm.let{OscillationEngine.state(it,time)};val total=s.totalEnergy?:0.0;Text("Kinetic ${fmt(s.kineticEnergy?:0.0)} J  ·  Potential ${fmt(s.potentialEnergy?:0.0)} J  ·  Total ${fmt(total)} J",color=P.ink,fontSize=12.sp)
                }
                ToggleSection("Live values",vm.valuesExpanded.value,{vm.valuesExpanded.value=!vm.valuesExpanded.value}){Text(liveDescription(experiment,live,projectile,time),color=P.muted,fontSize=12.sp)}
            }
        }
    )
    if(vm.equationOpen.value){
        val equations=when(experiment){MotionExperiment.Projectile->"x(t) = x₀ + v₀ cos(θ)t\ny(t) = y₀ + v₀ sin(θ)t − ½gt²\nvx(t) = v₀ cos(θ)\nvy(t) = v₀ sin(θ) − gt\nax = 0, ay = −g";MotionExperiment.Linear->"x(t) = x₀ + v₀t + ½at²\nv(t) = v₀ + at\na(t) = a";MotionExperiment.SHM->"x(t) = xₑq + A cos(ωt + φ)\nv(t) = −Aω sin(ωt + φ)\na(t) = −ω²A cos(ωt + φ)\nω = 2πf"}
        AlertDialog(onDismissRequest={vm.equationOpen.value=false},title={Text("Physics equations")},text={Text(equations)},confirmButton={TextButton(onClick={vm.equationOpen.value=false}){Text("Done")}})
    }
}

@Composable private fun PhysicsClock(vm:PhysicsMathViewModel,modifier:Modifier){val t=vm.time.floatValue.toDouble();Text("t = ${fmt(t)} s",modifier.clip(RoundedCornerShape(9.dp)).background(P.panel.copy(alpha=.95f)).padding(horizontal=10.dp,vertical=6.dp),color=P.ink,fontSize=12.sp,fontWeight=FontWeight.SemiBold)}
@Composable private fun PhysicsLiveReadout(vm:PhysicsMathViewModel,experiment:MotionExperiment,modifier:Modifier){
    if(!vm.valuesExpanded.value)return
    val t=vm.time.floatValue.toDouble();val p=vm.projectile();val text=when(experiment){MotionExperiment.Projectile->p?.let{val s=ProjectileMotionEngine.state(it,t);"x ${fmt(s.x)} m · y ${fmt(s.y)} m\nvx ${fmt(s.vx)} · vy ${fmt(s.vy)} m/s · |v| ${fmt(s.speed)} m/s"};MotionExperiment.Linear->vm.linear()?.let{val s=PhysicsMathEngine.linear(it,t);"x ${fmt(s.x)} m · v ${fmt(s.velocity)} m/s · a ${fmt(s.acceleration)} m/s²"};MotionExperiment.SHM->vm.oscillation()?.let{val s=OscillationEngine.state(it,t);"x ${fmt(s.x)} m · v ${fmt(s.velocity)} m/s · a ${fmt(s.acceleration)} m/s²"}}
    if(text!=null)Text(text,modifier.clip(RoundedCornerShape(10.dp)).background(P.panel.copy(alpha=.96f)).padding(9.dp),color=P.ink,fontSize=11.sp)
}

@Composable private fun ProjectileControls(vm:PhysicsMathViewModel,p:ProjectileParameters?){
    ParametersRow(listOf("v₀ · m/s" to vm.speed.value,"θ · °" to vm.angle.value),listOf({vm.speed.value=it},{vm.angle.value=it}),listOf("Initial speed","Launch angle"))
    ParametersRow(listOf("g · m/s²" to vm.gravity.value,"y₀ · m" to vm.launchHeight.value),listOf({vm.gravity.value=it},{vm.launchHeight.value=it}),listOf("Gravity","Launch height"))
    val q=p?.let(ProjectileMotionEngine::summary)
    Text("Range ${q?.range?.let(::fmt)?:"—"} m   ·   Flight ${q?.flightTime?.let(::fmt)?:"—"} s   ·   Max height ${q?.maxHeight?.let(::fmt)?:"—"} m",color=P.blue,fontWeight=FontWeight.SemiBold,fontSize=12.sp)
}

@Composable private fun LinearControls(vm:PhysicsMathViewModel,p:LinearMotionParameters?){
    ParametersRow(listOf("x₀ · m" to vm.position1d.value,"v₀ · m/s" to vm.velocity1d.value),listOf({vm.position1d.value=it},{vm.velocity1d.value=it}),listOf("Initial position","Initial velocity"))
    ParametersRow(listOf("a · m/s²" to vm.acceleration1d.value),listOf({vm.acceleration1d.value=it}),listOf("Acceleration"))
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp)){listOf("Constant velocity","Positive acceleration","Negative acceleration","Free fall").forEach{preset->WorkspaceAction(preset,preset,{vm.setPreset(preset)})}}
    p?.let{val s=PhysicsMathEngine.linear(it,vm.time.floatValue.toDouble());Text("x(t) = ${fmt(s.x)} m   ·   v(t) = ${fmt(s.velocity)} m/s   ·   a = ${fmt(s.acceleration)} m/s²",color=P.blue,fontSize=12.sp,fontWeight=FontWeight.SemiBold)}
}

@Composable private fun OscillationControls(vm:PhysicsMathViewModel,p:OscillationParameters?){
    if(vm.springMode.value) ParametersRow(listOf("A · m" to vm.amplitude.value),listOf({vm.amplitude.value=it}),listOf("Amplitude"))
    else ParametersRow(listOf("A · m" to vm.amplitude.value,"f · Hz" to vm.frequency.value),listOf({vm.amplitude.value=it},{vm.frequency.value=it}),listOf("Amplitude","Frequency"))
    ParametersRow(listOf("φ · °" to vm.phase.value,"xₑq · m" to vm.equilibrium.value),listOf({vm.phase.value=it},{vm.equilibrium.value=it}),listOf("Phase","Equilibrium"))
    Row(verticalAlignment=Alignment.CenterVertically){Text("Spring–mass model",Modifier.weight(1f),color=P.ink,fontSize=12.sp);Switch(vm.springMode.value,{vm.springMode.value=it})}
    if(vm.springMode.value)ParametersRow(listOf("m · kg" to vm.mass.value,"k · N/m" to vm.springConstant.value),listOf({vm.mass.value=it},{vm.springConstant.value=it}),listOf("Mass","Spring constant"))
    p?.let{val s=OscillationEngine.state(it,vm.time.floatValue.toDouble());Text("x ${fmt(s.x)} m · v ${fmt(s.velocity)} m/s · a ${fmt(s.acceleration)} m/s² · T ${fmt(OscillationEngine.period(it))} s",color=P.blue,fontSize=12.sp,fontWeight=FontWeight.SemiBold)}
}

@Composable private fun OverlayControls(vm:PhysicsMathViewModel){
    WorkspaceSectionTitle("Canvas overlays")
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp)){
        listOf("Path" to vm.overlays.value.trajectory,"Position" to vm.overlays.value.position,"Velocity" to vm.overlays.value.velocity,"Acceleration" to vm.overlays.value.acceleration,"Grid" to vm.overlays.value.grid,"Labels" to vm.overlays.value.labels).forEach{(name,selected)->WorkspaceAction(if(selected)"✓ $name"else name,"Toggle $name overlay",{val old=vm.overlays.value;vm.overlays.value=when(name){"Path"->old.copy(trajectory=!old.trajectory);"Position"->old.copy(position=!old.position);"Velocity"->old.copy(velocity=!old.velocity);"Acceleration"->old.copy(acceleration=!old.acceleration);"Grid"->old.copy(grid=!old.grid);else->old.copy(labels=!old.labels)}} ,selected=selected)}
    }
}
@Composable private fun ParametersRow(fields:List<Pair<String,String>>,setters:List<(String)->Unit>,descriptions:List<String>){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically){fields.forEachIndexed{i,f->NumericField(f.first,f.second,setters[i],Modifier.weight(1f).semantics{contentDescription=descriptions[i]})}}}
@Composable private fun ToggleSection(title:String,expanded:Boolean,onToggle:()->Unit,content:@Composable ColumnScope.()->Unit){Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp)).background(P.panelBlue.copy(alpha=.55f)).padding(8.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){WorkspaceSectionTitle(title){WorkspaceAction(if(expanded)"⌃"else"⌄","Toggle $title",onToggle)};if(expanded)content()}}
private fun errorFor(experiment:MotionExperiment)=when(experiment){MotionExperiment.Projectile->"Use finite values with v₀ ≥ 0 and g ≥ 0. g = 0 is supported.";MotionExperiment.Linear->"Enter finite position, velocity, and acceleration values.";MotionExperiment.SHM->"Use A ≥ 0 and frequency > 0. Spring mass and k must be positive."}
private fun liveDescription(experiment:MotionExperiment,live:Any?,p:ProjectileParameters?,t:Double)=when(live){is ProjectileState->"t ${fmt(t)} s · x ${fmt(live.x)} m · y ${fmt(live.y)} m\nvx ${fmt(live.vx)} m/s · vy ${fmt(live.vy)} m/s · speed ${fmt(live.speed)} m/s · a ${fmt(live.ay)} m/s²\nRange ${p?.let{ProjectileMotionEngine.summary(it).range}?.let(::fmt)?:"not defined"} m · flight ${p?.let{ProjectileMotionEngine.summary(it).flightTime}?.let(::fmt)?:"not defined"} s · max height ${p?.let{ProjectileMotionEngine.summary(it).maxHeight}?.let(::fmt)?:"not defined"} m";is LinearMotionState->"t ${fmt(t)} s · x ${fmt(live.x)} m · v ${fmt(live.velocity)} m/s · a ${fmt(live.acceleration)} m/s²";is OscillationState->"t ${fmt(t)} s · x ${fmt(live.x)} m · v ${fmt(live.velocity)} m/s · a ${fmt(live.acceleration)} m/s²${live.totalEnergy?.let{"\nK ${fmt(live.kineticEnergy!!)} J · U ${fmt(live.potentialEnergy!!)} J · E ${fmt(it)} J"}.orEmpty()}";else->"Values are unavailable until the parameters are valid."}
private fun fmt(x:Double)=if(!x.isFinite())"—"else if(abs(x)>=1e6||(abs(x)<1e-4&&x!=0.0))"%.4e".format(x)else "%.5g".format(x)
