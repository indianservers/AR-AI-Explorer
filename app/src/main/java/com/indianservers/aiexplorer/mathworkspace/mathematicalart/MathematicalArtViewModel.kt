package com.indianservers.aiexplorer.mathworkspace.mathematicalart

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.*

class MathematicalArtViewModel:ViewModel(){
    val mode=mutableStateOf(ArtMode.Spirograph);val viewport=mutableStateOf(ArtViewport());val palette=mutableStateOf(ArtPalette.Azure)
    val layers=mutableStateOf<List<ArtLayer>>(emptyList());val error=mutableStateOf<String?>(null);val busy=mutableStateOf(false)
    val sheet=mutableStateOf(com.indianservers.aiexplorer.mathworkspace.components.WorkspaceSheetStop.Peek)
    val fixedRadius=mutableStateOf("5");val rollingRadius=mutableStateOf("3");val penOffset=mutableStateOf("4");val spiroType=mutableStateOf(SpiroType.Hypotrochoid);val rotations=mutableStateOf("6");val phase=mutableStateOf("0")
    val amplitude=mutableStateOf("3");val k=mutableStateOf("5");val sine=mutableStateOf(false);val roseTurns=mutableStateOf("4")
    val A=mutableStateOf("1");val B=mutableStateOf("1");val freqA=mutableStateOf("3");val freqB=mutableStateOf("2");val delta=mutableStateOf("1.5708")
    val lissajousTurns=mutableStateOf("4")
    val polar=mutableStateOf("1 + cos(theta)");val polarMin=mutableStateOf("0");val polarMax=mutableStateOf("6.28318")
    val xExpr=mutableStateOf("cos(t)");val yExpr=mutableStateOf("sin(t)");val tMin=mutableStateOf("0");val tMax=mutableStateOf("6.28318")
    val showGrid=mutableStateOf(false);val showAxes=mutableStateOf(false);val showOrigin=mutableStateOf(true);val animate=mutableStateOf(false);val progress=mutableStateOf(1f);val speed=mutableStateOf(1f);val focus=mutableStateOf(false);val editorOpen=mutableStateOf(false)
    private var job:Job?=null
    init{refresh(true)}
    fun refresh(fit:Boolean=false){job?.cancel();job=viewModelScope.launch{delay(100);busy.value=true;val m=mode.value;val result=withContext(Dispatchers.Default){runCatching{generate(m)}};result.fold({pts->val old=layers.value;val color=palette.value.colors.first();layers.value=if(old.isEmpty())listOf(ArtLayer(m.label,pts,color))else listOf(old.first().copy(points=pts));error.value=null;if(fit||viewport.value.scale==1.0)fitArtwork()}, {error.value=it.message?:"Check the curve parameters."});busy.value=false}}
    private fun generate(m:ArtMode):List<ArtPoint>{
        fun num(s:String):Double = s.toDoubleOrNull()?.takeIf(Double::isFinite) ?: kotlin.error("Enter finite numeric values.")
        return when(m){
            ArtMode.Spirograph->SpirographEngine.sample(num(fixedRadius.value),num(rollingRadius.value),num(penOffset.value),spiroType.value,num(phase.value),num(rotations.value))
            ArtMode.Rose->RoseCurveEngine.sample(num(amplitude.value),num(k.value),num(phase.value),sine.value,num(roseTurns.value))
            ArtMode.Lissajous->LissajousEngine.sample(num(A.value),num(B.value),num(freqA.value),num(freqB.value),num(delta.value),num(lissajousTurns.value))
            ArtMode.Polar->PolarArtEngine.sample(polar.value,num(polarMin.value),num(polarMax.value))
            ArtMode.Parametric->ParametricCurveEngine.sample(xExpr.value,yExpr.value,num(tMin.value),num(tMax.value))
        }
    }
    fun selectMode(m:ArtMode){mode.value=m;when(m){ArtMode.Polar->{polar.value="1 + cos(theta)"};ArtMode.Parametric->{xExpr.value="cos(t)";yExpr.value="sin(t)"};else->{}};layers.value=emptyList();viewport.value=ArtViewport();refresh(true)}
    fun fitArtwork(){val pts=layers.value.flatMap{it.points}.filter{it.x.isFinite()&&it.y.isFinite()};if(pts.isEmpty())return;val minX=pts.minOf{it.x};val maxX=pts.maxOf{it.x};val minY=pts.minOf{it.y};val maxY=pts.maxOf{it.y};val w=(maxX-minX).coerceAtLeast(1e-6);val h=(maxY-minY).coerceAtLeast(1e-6);viewport.value=ArtViewport((minX+maxX)/2,(minY+maxY)/2,3.2/max(w,h))}
    fun reset(){viewport.value=ArtViewport();fitArtwork()}
    fun randomize(){when(mode.value){ArtMode.Spirograph->{fixedRadius.value=(3..9).random().toString();rollingRadius.value=(2..6).random().toString();penOffset.value=(2..9).random().toString()};ArtMode.Rose->{amplitude.value=(1..5).random().toString();k.value=listOf("2.5","3","5","7","8").random()};ArtMode.Lissajous->{freqA.value=(1..5).random().toString();freqB.value=(1..5).random().toString();delta.value=listOf("0","0.7854","1.5708").random()};ArtMode.Polar->polar.value=listOf("1 + cos(theta)","1.3 + cos(theta)","2*sin(3*theta)","theta/5").random();ArtMode.Parametric->{xExpr.value="sin(2*t)";yExpr.value="sin(3*t)"}};refresh()}
    fun preset(name:String){when(mode.value){ArtMode.Spirograph->when(name){"Classic Bloom"->{fixedRadius.value="5";rollingRadius.value="3";penOffset.value="4";spiroType.value=SpiroType.Hypotrochoid};"Star Orbit"->{fixedRadius.value="8";rollingRadius.value="3";penOffset.value="7";spiroType.value=SpiroType.Epitrochoid};else->{fixedRadius.value="7";rollingRadius.value="2";penOffset.value="5";spiroType.value=SpiroType.Hypotrochoid}};ArtMode.Rose->when(name){"5 Petals"->{k.value="5";sine.value=false};"8 Petals"->{k.value="4";sine.value=false};else->{k.value="2.5";sine.value=true}};ArtMode.Lissajous->{val (a,b)=when(name){"1:1"->1 to 1;"2:1"->2 to 1;"5:4"->5 to 4;else->3 to 2};freqA.value=a.toString();freqB.value=b.toString()};ArtMode.Polar->polar.value=PolarArtEngine.preset(name);ArtMode.Parametric->when(name){"Circle"->{xExpr.value="cos(t)";yExpr.value="sin(t)"};"Ellipse"->{xExpr.value="2*cos(t)";yExpr.value="sin(t)"};"Figure Eight"->{xExpr.value="sin(t)";yExpr.value="sin(2*t)"};else->{xExpr.value="2*cos(t)-cos(2*t)";yExpr.value="2*sin(t)-sin(2*t)"}}};refresh(true)}
    fun addLayer(){if(layers.value.size>=4)return;val base=layers.value.lastOrNull()?:return;layers.value=layers.value+base.copy(name="Curve ${layers.value.size+1}",visible=true,color=palette.value.colors[layers.value.size%palette.value.colors.size])}
    fun toggleLayer(i:Int){layers.value=layers.value.mapIndexed{n,l->if(i==n)l.copy(visible=!l.visible)else l}}
    fun deleteLayer(i:Int){layers.value=layers.value.filterIndexed{n,_->n!=i}}
}


