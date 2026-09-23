package com.indianservers.aiexplorer.mathworkspace.physicsmath

import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.indianservers.aiexplorer.mathworkspace.components.WorkspaceSheetStop
import kotlin.math.*

class PhysicsMathViewModel : ViewModel(){
    val mode=mutableStateOf(PhysicsMode.Motion)
    val experiment=mutableStateOf(MotionExperiment.Projectile)
    val graphSeries=mutableStateOf(PhysicsGraphSeries.Y)
    val speed=mutableStateOf("20");val angle=mutableStateOf("45");val gravity=mutableStateOf("9.80665");val launchHeight=mutableStateOf("0");val startX=mutableStateOf("0")
    val position1d=mutableStateOf("0")
    val velocity1d=mutableStateOf("10");val acceleration1d=mutableStateOf("0")
    val amplitude=mutableStateOf("2");val frequency=mutableStateOf("0.5");val phase=mutableStateOf("0");val equilibrium=mutableStateOf("0")
    val springMode=mutableStateOf(false);val mass=mutableStateOf("1");val springConstant=mutableStateOf("4")
    val time=mutableFloatStateOf(0f);val playing=mutableStateOf(false);val playbackSpeed=mutableFloatStateOf(1f)
    val viewport=mutableStateOf(PhysicsViewport());val overlays=mutableStateOf(PhysicsOverlays())
    val sheet=mutableStateOf(WorkspaceSheetStop.Peek);val advancedExpanded=mutableStateOf(false);val valuesExpanded=mutableStateOf(false);val energyExpanded=mutableStateOf(false);val equationOpen=mutableStateOf(false)

    fun projectile():ProjectileParameters?{
        val v=speed.value.toDoubleOrNull()?:return null;val theta=angle.value.toDoubleOrNull()?:return null;val g=gravity.value.toDoubleOrNull()?:return null;val y=launchHeight.value.toDoubleOrNull()?:return null;val x=startX.value.toDoubleOrNull()?:return null
        return ProjectileParameters(v,theta,g,y,x).takeIf{v>=0&&g>=0&&listOf(v,theta,g,y,x).all(Double::isFinite)}
    }
    fun linear():LinearMotionParameters?{val x=position1d.value.toDoubleOrNull()?:return null;val v=velocity1d.value.toDoubleOrNull()?:return null;val a=acceleration1d.value.toDoubleOrNull()?:return null;return LinearMotionParameters(x,v,a).takeIf{listOf(x,v,a).all(Double::isFinite)}}
    fun oscillation():OscillationParameters?{val a=amplitude.value.toDoubleOrNull()?:return null;val f=frequency.value.toDoubleOrNull()?:return null;val ph=phase.value.toDoubleOrNull()?:return null;val eq=equilibrium.value.toDoubleOrNull()?:return null;val m=mass.value.toDoubleOrNull()?:return null;val k=springConstant.value.toDoubleOrNull()?:return null;return OscillationParameters(a,f,ph*Math.PI/180,eq,springMode.value,m,k).takeIf{a>=0&&(springMode.value||f>0)&&listOf(a,f,ph,eq,m,k).all(Double::isFinite)&&(!springMode.value||(m>0&&k>0))}}
    fun duration():Double=when(experiment.value){
        MotionExperiment.Projectile->{val p=projectile();if(p==null)5.0 else if(p.gravity==0.0)10.0 else {val flight=ProjectileMotionEngine.summary(p).flightTime;flight?.takeIf{it>1e-4}?:max(2*abs(p.speed*sin(Math.toRadians(p.angleDegrees))/p.gravity),5.0).coerceAtMost(10000.0)}}
        MotionExperiment.Linear->10.0
        MotionExperiment.SHM->(oscillation()?.let{OscillationEngine.period(it)*4}?:8.0).coerceIn(1e-6,10000.0)
    }
    fun autoFit(){
        viewport.value=when(experiment.value){MotionExperiment.Projectile->projectile()?.let(ProjectileMotionEngine::autoFit)?:PhysicsViewport(-1.0,1.0,-1.0,1.0);MotionExperiment.Linear->linear()?.let{PhysicsMathEngine.linearAutoFit(it,duration())}?:PhysicsViewport(-1.0,1.0,-1.0,1.0);MotionExperiment.SHM->oscillation()?.let(OscillationEngine::autoFit)?:PhysicsViewport(-1.0,1.0,-1.0,1.0)}
        if(mode.value==PhysicsMode.Graphs)viewport.value=GraphSeries.autoFit(this)
    }
    fun resetExperiment(){playing.value=false;time.floatValue=0f;when(experiment.value){MotionExperiment.Projectile->{speed.value="20";angle.value="45";gravity.value="9.80665";launchHeight.value="0";startX.value="0"};MotionExperiment.Linear->{position1d.value="0";velocity1d.value="10";acceleration1d.value="0"};MotionExperiment.SHM->{amplitude.value="2";frequency.value="0.5";phase.value="0";equilibrium.value="0";springMode.value=false;mass.value="1";springConstant.value="4"}};autoFit()}
    fun selectExperiment(experiment:MotionExperiment){playing.value=false;this.experiment.value=experiment;graphSeries.value=when(experiment){MotionExperiment.Projectile->PhysicsGraphSeries.Y;MotionExperiment.Linear->PhysicsGraphSeries.Position;MotionExperiment.SHM->PhysicsGraphSeries.Displacement};time.floatValue=0f;autoFit()}
    fun setPreset(preset:String){when(preset){"Constant velocity"->{velocity1d.value="10";acceleration1d.value="0"};"Positive acceleration"->{velocity1d.value="0";acceleration1d.value="2"};"Negative acceleration"->{velocity1d.value="10";acceleration1d.value="-2"};"Free fall"->{velocity1d.value="0";acceleration1d.value="-${gravity.value.toDoubleOrNull()?:9.80665}"}};time.floatValue=0f;autoFit()}
    fun currentProjectile()=projectile()?.let{ProjectileMotionEngine.state(it,time.floatValue.toDouble())}
    fun currentLinear()=linear()?.let{PhysicsMathEngine.linear(it,time.floatValue.toDouble())}
    fun currentOscillation()=oscillation()?.let{OscillationEngine.state(it,time.floatValue.toDouble())}
    fun dragLaunch(x:Double,y:Double){startX.value=x.toString();launchHeight.value=y.toString()}
    fun dragVelocityTip(x:Double,y:Double){val p=projectile()?:return;val dx=x-p.startX;val dy=y-p.launchHeight;speed.value=hypot(dx,dy).toString();angle.value=Math.toDegrees(atan2(dy,dx)).toString()}
    fun scrub(t:Double){time.floatValue=t.coerceAtLeast(0.0).toFloat()}
    fun viewportPan(dx:Double,dy:Double){val v=viewport.value;viewport.value=v.copy(minX=v.minX+dx,maxX=v.maxX+dx,minY=v.minY+dy,maxY=v.maxY+dy)}
    fun viewportZoom(factor:Double,worldX:Double,worldY:Double){if(!factor.isFinite()||factor<=0)return;val v=viewport.value;val z=factor.coerceIn(.03,30.0);viewport.value=PhysicsViewport(worldX+(v.minX-worldX)/z,worldX+(v.maxX-worldX)/z,worldY+(v.minY-worldY)/z,worldY+(v.maxY-worldY)/z)}
}
