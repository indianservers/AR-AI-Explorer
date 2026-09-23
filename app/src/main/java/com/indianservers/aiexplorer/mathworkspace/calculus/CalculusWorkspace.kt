package com.indianservers.aiexplorer.mathworkspace.calculus

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import kotlin.math.*

@Composable
fun CalculusWorkspace(viewModel:CalculusViewModel,onBack:()->Unit){
    val vm=viewModel;val mode by vm.mode
    val parsed=remember(vm.expression.value){runCatching{FunctionParser.parse(vm.expression.value)}}
    LaunchedEffect(vm.expression.value, parsed.isSuccess) { if(parsed.isSuccess) vm.rememberFunction(vm.expression.value) }
    val f:(Double)->Double={x->parsed.getOrNull()?.value(x)?:Double.NaN}
    LaunchedEffect(Unit) { if(parsed.isSuccess) vm.autoFit(f) }
    val point=vm.pointX.value.toDoubleOrNull();val derivative=if(point!=null)CalculusMath.derivative(f,point)else Double.NaN
    val a=vm.lower.value.toDoubleOrNull();val b=vm.upper.value.toDoubleOrNull()
    val integral=if(a!=null&&b!=null)CalculusMath.integrate(f,a,b)else Double.NaN
    val absolute=if(a!=null&&b!=null)CalculusMath.integrate({x->abs(f(x))},a,b)else Double.NaN
    val limitX=vm.limitAt.value.toDoubleOrNull();val estimate=if(limitX!=null)CalculusMath.limit(f,limitX)else null
    val summary=when(mode){CalculusMode.Function->"f(x) = ${vm.expression.value}";CalculusMode.Derivative->"f′(${point?.let(::fmt)?:"—"}) = ${fmt(derivative)}";CalculusMode.Integral->"∫[${fmt(a?:Double.NaN)}, ${fmt(b?:Double.NaN)}] f(x) dx = ${fmt(integral)}";CalculusMode.Limits->"x → ${fmt(limitX?:Double.NaN)} · ${estimate?.value?.let(::fmt)?:"no finite two-sided limit"}"}
    MathWorkspaceShell(
        title="Calculus Lab",subtitle="Explore functions · derivatives · integrals · limits",onBack=onBack,
        sheetTitle=if(vm.sheet.value==WorkspaceSheetStop.Collapsed)summary else mode.title,
        sheetStop=vm.sheet.value,onSheetStopChange={vm.sheet.value=it},
        topBarTrailing={WorkspaceSegmentedControl(CalculusMode.entries.map{it.title},mode.ordinal){vm.mode.value=CalculusMode.entries[it]}},
        canvas={CalculusRenderer(vm,f)},
        tools={Row(Modifier.align(Alignment.TopEnd).padding(8.dp).horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){
            WorkspaceAction("−","Zoom out",{vm.viewport.value=vm.viewport.value.copy(scaleX=vm.viewport.value.scaleX/1.25,scaleY=vm.viewport.value.scaleY/1.25)})
            WorkspaceAction("+","Zoom in",{vm.viewport.value=vm.viewport.value.copy(scaleX=vm.viewport.value.scaleX*1.25,scaleY=vm.viewport.value.scaleY*1.25)})
            WorkspaceAction("X","Zoom horizontal axis",{vm.viewport.value=vm.viewport.value.copy(scaleX=vm.viewport.value.scaleX*1.25)})
            WorkspaceAction("Y","Zoom vertical axis",{vm.viewport.value=vm.viewport.value.copy(scaleY=vm.viewport.value.scaleY*1.25)})
            WorkspaceAction("↺","Reset view",vm::resetView)
            WorkspaceAction("Fit","Auto-fit graph",{vm.autoFit(f)})
        }
            val info=when(mode){CalculusMode.Function->vm.inspectedX.value?.let{x->"x = ${fmt(x)} · f(x) = ${fmt(f(x))}"};CalculusMode.Derivative->if(point!=null&&derivative.isFinite())"x = ${fmt(point)} · f(x) = ${fmt(f(point))} · f′(x) = ${fmt(derivative)}"else null;CalculusMode.Integral->if(integral.isFinite())"Signed integral = ${fmt(integral)} · geometric area = ${fmt(absolute)}"else null;CalculusMode.Limits->estimate?.let{if(it.converges)"Left ${fmt(it.left)} · Right ${fmt(it.right)} · Limit ${fmt(it.value?:Double.NaN)}"else "Left ${fmt(it.left)} · Right ${fmt(it.right)} · no finite two-sided limit"}}
            if(info!=null)Text(info,Modifier.align(Alignment.TopStart).padding(10.dp).clip(RoundedCornerShape(10.dp)).background(P.panel.copy(alpha=.96f)).padding(9.dp),color=P.ink,fontSize=11.sp,fontWeight=FontWeight.SemiBold)
        },
        sheet={Column(Modifier.weight(1f,fill=false).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){
            Text("Function of x",color=P.muted,fontSize=11.sp)
            BasicTextField(value=vm.expression.value,onValueChange={vm.expression.value=it},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Ascii),textStyle=TextStyle(color=P.ink,fontSize=16.sp,textAlign=TextAlign.Start),modifier=Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp)).background(P.panel).border(1.dp,P.border,RoundedCornerShape(11.dp)).padding(horizontal=12.dp,vertical=12.dp).semantics{contentDescription="Function expression"},decorationBox={inner->Box{if(vm.expression.value.isEmpty())Text("e.g. sin(x) + x^2",color=P.muted);inner()}})
            parsed.exceptionOrNull()?.let{Text(it.message?:"Invalid expression",color=P.red,fontSize=11.sp)}
            if(vm.recent.isNotEmpty())Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp)){vm.recent.forEach{entry->WorkspaceAction(entry,"Use recent function $entry",{vm.expression.value=entry})}}
            when(mode){
                CalculusMode.Function->{Row(verticalAlignment=Alignment.CenterVertically){Text("Show derivative f′(x)",Modifier.weight(1f),color=P.ink,fontSize=12.sp);Switch(vm.showDerivative.value,{vm.showDerivative.value=it})}}
                CalculusMode.Derivative->{Row(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically){NumericField("Tangent x",vm.pointX.value,{vm.pointX.value=it},Modifier.weight(1f));Text("f′(x) = ${fmt(derivative)}",color=P.blue,fontWeight=FontWeight.Bold,fontSize=14.sp)}
                    if(point!=null&&derivative.isFinite()&&f(point).isFinite())Text("Tangent: y = ${fmt(derivative)}(x − ${fmt(point)}) + ${fmt(f(point))}",color=P.muted,fontSize=11.sp)
                }
                CalculusMode.Integral->{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){NumericField("Lower a",vm.lower.value,{vm.lower.value=it},Modifier.weight(1f));NumericField("Upper b",vm.upper.value,{vm.upper.value=it},Modifier.weight(1f))};Text(if(integral.isFinite())"Signed integral = ${fmt(integral)} · absolute area = ${fmt(absolute)}"else "Integral is undefined over this interval.",color=if(integral.isFinite())P.blue else P.red,fontSize=13.sp,fontWeight=FontWeight.SemiBold)}
                CalculusMode.Limits->{NumericField("Approach x →",vm.limitAt.value,{vm.limitAt.value=it},Modifier.fillMaxWidth());estimate?.let{Text(if(it.converges)"Left ${fmt(it.left)} · Right ${fmt(it.right)}\nTwo-sided limit = ${fmt(it.value?:Double.NaN)}"else "Left ${fmt(it.left)} · Right ${fmt(it.right)}\nThe one-sided values disagree; no finite two-sided limit.",color=if(it.converges)P.blue else P.amber,fontSize=12.sp,fontWeight=FontWeight.SemiBold)}}
            }
        }}
    )
}
private fun fmt(v:Double)=if(!v.isFinite())"—"else if(abs(v)>=1e6||(abs(v)<1e-4&&v!=0.0))"%.4e".format(v)else "%.6g".format(v)
