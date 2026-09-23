package com.indianservers.aiexplorer.mathworkspace.physicsmath

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.indianservers.aiexplorer.mathworkspace.components.MathWorkspacePalette as P
import kotlin.math.*

@androidx.compose.runtime.Composable
fun PhysicsRenderer(vm:PhysicsMathViewModel,modifier:Modifier=Modifier){
    val mode=vm.mode.value;val exp=vm.experiment.value;val viewport=vm.viewport.value;val time=vm.time.floatValue.toDouble()
    Canvas(modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)).background(P.snow)
        .pointerInput(mode,exp,viewport,vm.speed.value,vm.angle.value,vm.launchHeight.value,vm.startX.value,vm.position1d.value,vm.velocity1d.value,vm.acceleration1d.value,vm.amplitude.value,vm.frequency.value,vm.phase.value,vm.equilibrium.value){
            detectTransformGestures { centroid,pan,zoom,_ ->
                val worldX=viewport.minX+centroid.x/size.width*viewport.spanX
                val worldY=viewport.maxY-centroid.y/size.height*viewport.spanY
                if(zoom!=1f)vm.viewportZoom(zoom.toDouble(),worldX,worldY)
                val launch=vm.projectile();val lin=vm.linear();val osc=vm.oscillation()
                val screenX=(worldX-viewport.minX)/viewport.spanX*size.width
                val screenY=(viewport.maxY-worldY)/viewport.spanY*size.height
                val direct=when {
                    mode==PhysicsMode.Graphs->false
                    exp==MotionExperiment.Projectile&&launch!=null->{
                        val a=Math.toRadians(launch.angleDegrees);val scale=.18*min(viewport.spanX,viewport.spanY)/max(launch.speed,1.0)
                        val launchPos=Offset(((launch.startX-viewport.minX)/viewport.spanX*size.width).toFloat(),((viewport.maxY-launch.launchHeight)/viewport.spanY*size.height).toFloat())
                        val tip=Offset(((launch.startX+launch.speed*cos(a)*scale-viewport.minX)/viewport.spanX*size.width).toFloat(),((viewport.maxY-(launch.launchHeight+launch.speed*sin(a)*scale))/viewport.spanY*size.height).toFloat())
                        val distLaunch=hypot(screenX-launchPos.x,screenY-launchPos.y);val distTip=hypot(screenX-tip.x,screenY-tip.y)
                        when{distTip<distLaunch&&distTip<58->{val dx=worldX-launch.startX;val dy=worldY-launch.launchHeight;val angle=atan2(dy,dx);vm.speed.value=(hypot(dx,dy)/scale).toString();vm.angle.value=Math.toDegrees(angle).toString();true};distLaunch<55->{vm.dragLaunch(worldX,worldY);true};else->false}
                    }
                    exp==MotionExperiment.Linear&&lin!=null->{
                        val state=PhysicsMathEngine.linear(lin,time);val ox=((state.x-viewport.minX)/viewport.spanX*size.width).toFloat();val oy=((viewport.maxY)/viewport.spanY*size.height).toFloat()
                        if(hypot(screenX-ox,screenY-oy)<55){val initial=worldX-lin.velocity*time-.5*lin.acceleration*time*time;vm.position1d.value=initial.toString();true}else false
                    }
                    exp==MotionExperiment.SHM&&osc!=null->{
                        val state=OscillationEngine.state(osc,time);val ox=((state.x-viewport.minX)/viewport.spanX*size.width).toFloat();val oy=((viewport.maxY)/viewport.spanY*size.height).toFloat()
                        if(hypot(screenX-ox,screenY-oy)<55){vm.equilibrium.value=(worldX-state.displacement).toString();true}else false
                    }
                    else->false
                }
                if(!direct){
                    val next=vm.viewport.value
                    vm.viewportPan(-pan.x/size.width*next.spanX,pan.y/size.height*next.spanY)
                }
            }
        }
        .pointerInput(mode,exp,viewport,vm.projectile(),vm.linear(),vm.oscillation()){
            detectTapGestures { pos ->
                if(mode==PhysicsMode.Graphs){val x=viewport.minX+pos.x/size.width*viewport.spanX;vm.scrub(x.coerceIn(0.0,vm.duration()))}
                else if(exp==MotionExperiment.Projectile){val p=vm.projectile();if(p!=null){val s=ProjectileMotionEngine.summary(p);val end=s.flightTime?.takeIf{it>0}?:vm.duration();var nearest=Double.POSITIVE_INFINITY;var nearestT=0.0
                    for(i in 0..300){val t=end*i/300;val state=ProjectileMotionEngine.state(p,t);val sx=((state.x-viewport.minX)/viewport.spanX*size.width).toFloat();val sy=((viewport.maxY-state.y)/viewport.spanY*size.height).toFloat();val d=hypot((pos.x-sx).toDouble(),(pos.y-sy).toDouble());if(d<nearest){nearest=d;nearestT=t}}
                    if(nearest<100)vm.scrub(nearestT)
                }}
            }
        }) {
        val w=size.width.toDouble();val h=size.height.toDouble();val xMin=viewport.minX;val xMax=viewport.maxX;val yMin=viewport.minY;val yMax=viewport.maxY
        fun sx(x:Double)=((x-xMin)/(xMax-xMin).coerceAtLeast(1e-12)*w).toFloat()
        fun sy(y:Double)=(h-(y-yMin)/(yMax-yMin).coerceAtLeast(1e-12)*h).toFloat()
        if(vm.overlays.value.grid||mode==PhysicsMode.Graphs){
            val stepX=niceStep(viewport.spanX/8);var x=ceil(xMin/stepX)*stepX
            while(x<=xMax){val px=sx(x);drawLine(P.border.copy(alpha=.66f),Offset(px,0f),Offset(px,size.height),1f);if(vm.overlays.value.labels||mode==PhysicsMode.Graphs)drawContext.canvas.nativeCanvas.drawText(tick(x),px+3f,size.height-5f,android.graphics.Paint().apply{color=android.graphics.Color.rgb(101,122,155);textSize=10f});x+=stepX}
            val stepY=niceStep(viewport.spanY/7);var y=ceil(yMin/stepY)*stepY
            while(y<=yMax){val py=sy(y);drawLine(P.border.copy(alpha=.6f),Offset(0f,py),Offset(size.width,py),1f);y+=stepY}
        }
        val axisX=if(0.0 in xMin..xMax)sx(0.0)else 0f;val axisY=if(0.0 in yMin..yMax)sy(0.0)else size.height
        drawLine(P.muted.copy(alpha=.78f),Offset(0f,axisY),Offset(size.width,axisY),1.4f);drawLine(P.muted.copy(alpha=.78f),Offset(axisX,0f),Offset(axisX,size.height),1.4f)
        when{
            mode==PhysicsMode.Graphs->drawSeries(vm,viewport,::sx,::sy)
            mode==PhysicsMode.Oscillations->drawOscillator(vm,viewport,::sx,::sy)
            exp==MotionExperiment.Projectile->drawProjectile(vm,viewport,::sx,::sy)
            exp==MotionExperiment.Linear->drawLinear(vm,viewport,::sx,::sy)
            exp==MotionExperiment.SHM->drawOscillator(vm,viewport,::sx,::sy)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawProjectile(vm:PhysicsMathViewModel,v:PhysicsViewport,sx:(Double)->Float,sy:(Double)->Float){
    val p=vm.projectile()?:return;val summary=ProjectileMotionEngine.summary(p);val duration=summary.flightTime?.takeIf{it>0}?:vm.duration();val time=vm.time.floatValue.toDouble();val state=ProjectileMotionEngine.state(p,time)
    val trajectory=Path();var started=false
    if(vm.overlays.value.trajectory){for(i in 0..360){val t=duration*i/360;val s=ProjectileMotionEngine.state(p,t);if(!s.y.isFinite()||!s.x.isFinite()){started=false;continue};val x=sx(s.x);val y=sy(s.y);if(!started){trajectory.moveTo(x,y);started=true}else trajectory.lineTo(x,y)};drawPath(trajectory,P.blue,style=Stroke(3f,cap=StrokeCap.Round))}
    val launch=ProjectileMotionEngine.state(p,0.0);val scale=.18*min(v.spanX,v.spanY)/max(p.speed,1.0)
    if(vm.overlays.value.velocity&&p.speed>0){arrow(Offset(sx(launch.x),sy(launch.y)),Offset(sx(launch.x+launch.vx*scale),sy(launch.y+launch.vy*scale)),P.blue);if(vm.overlays.value.labels)label("v₀",Offset(sx(launch.x+launch.vx*scale),sy(launch.y+launch.vy*scale)),P.blue)}
    drawCircle(P.blue,5f,Offset(sx(p.startX),sy(p.launchHeight)))
    if(summary.flightTime!=null&&summary.flightTime>0){val land=ProjectileMotionEngine.state(p,summary.flightTime);drawCircle(P.blue.copy(alpha=.58f),5f,Offset(sx(land.x),sy(0.0)))}
    summary.maxHeight?.let{height->val t=if(p.gravity>0)p.speed*sin(Math.toRadians(p.angleDegrees))/p.gravity else 0.0;if(t>=0){val peak=ProjectileMotionEngine.state(p,t);drawLine(P.violet.copy(alpha=.6f),Offset(sx(peak.x),sy(0.0)),Offset(sx(peak.x),sy(peak.y)),1.4f);drawCircle(P.violet,5f,Offset(sx(peak.x),sy(height)));label("H ${fmt(height)} m",Offset(sx(peak.x),sy(height)-10),P.violet)}}
    if(state.y>=0){val pos=Offset(sx(state.x),sy(state.y));drawCircle(P.blue,8f,pos);if(vm.overlays.value.position)arrow(Offset(sx(0.0),sy(0.0)),pos,P.blue)
        val vScale=.15*min(v.spanX,v.spanY)/max(state.speed,1.0);if(vm.overlays.value.velocity)arrow(pos,Offset(sx(state.x+state.vx*vScale),sy(state.y+state.vy*vScale)),P.green)
        if(vm.overlays.value.acceleration&&p.gravity>0){val aScale=.12*min(v.spanX,v.spanY)/p.gravity;arrow(pos,Offset(sx(state.x),sy(state.y-p.gravity*aScale)),P.red)}
        if(vm.overlays.value.labels)label("t ${fmt(time)} s · (${fmt(state.x)}, ${fmt(state.y)}) m",Offset(pos.x+10,pos.y-12),P.ink)
    }
    val angle=Math.toRadians(p.angleDegrees);val origin=Offset(sx(p.startX),sy(p.launchHeight));val ground=Offset(sx(p.startX+max(v.spanX*.08,1.0)),sy(p.launchHeight));val tip=Offset(sx(p.startX+cos(angle)*max(v.spanX*.08,1.0)),sy(p.launchHeight+sin(angle)*max(v.spanX*.08,1.0)));drawLine(P.muted,origin,ground,1f);drawLine(P.muted,origin,tip,1f);drawArc(P.amber,if(p.angleDegrees>=0)-p.angleDegrees.toFloat() else 0f,min(abs(p.angleDegrees).toFloat(),180f),false,origin,androidx.compose.ui.geometry.Size(36f,36f),style=Stroke(1.5f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLinear(vm:PhysicsMathViewModel,v:PhysicsViewport,sx:(Double)->Float,sy:(Double)->Float){
    val p=vm.linear()?:return;val t=vm.time.floatValue.toDouble();val s=PhysicsMathEngine.linear(p,t);val mid=sy(0.0);drawLine(P.muted,Offset(0f,mid),Offset(size.width,mid),2f)
    for(i in 1..6){val past=max(0.0,t-i*max(t/6,.3));val ghost=PhysicsMathEngine.linear(p,past);drawCircle(P.blue.copy(alpha=.1f+i*.04f),5f,Offset(sx(ghost.x),mid))}
    val objectPos=Offset(sx(s.x),mid);drawCircle(P.blue,12f,objectPos);val vscale=.13*v.spanX/max(abs(s.velocity),1.0);arrow(objectPos,Offset(sx(s.x+s.velocity*vscale),mid),P.green);if(abs(s.acceleration)>0){val ascale=.13*v.spanX/max(abs(s.acceleration),1.0);arrow(objectPos,Offset(sx(s.x+sign(s.acceleration)*ascale),mid-14),P.red)}
    label("x ${fmt(s.x)} m · v ${fmt(s.velocity)} m/s · t ${fmt(t)} s",Offset(objectPos.x+8,objectPos.y-20),P.ink)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawOscillator(vm:PhysicsMathViewModel,v:PhysicsViewport,sx:(Double)->Float,sy:(Double)->Float){
    val p=vm.oscillation()?:return;val s=OscillationEngine.state(p,vm.time.floatValue.toDouble());val mid=sy(0.0);val left=p.equilibrium-1.4*abs(p.amplitude).coerceAtLeast(1.0);val x1=sx(left);val x2=sx(s.x);drawLine(P.border.copy(alpha=.8f),Offset(0f,mid),Offset(size.width,mid),1.4f)
    drawLine(P.violet.copy(alpha=.6f),Offset(sx(p.equilibrium),mid-30),Offset(sx(p.equilibrium),mid+30),1.4f);drawLine(P.violet.copy(alpha=.5f),Offset(sx(p.equilibrium-abs(p.amplitude)),mid-14),Offset(sx(p.equilibrium+abs(p.amplitude)),mid-14),1.2f);drawCircle(P.violet,4f,Offset(sx(p.equilibrium-abs(p.amplitude)),mid-14));drawCircle(P.violet,4f,Offset(sx(p.equilibrium+abs(p.amplitude)),mid-14))
    val spring=Path().apply{moveTo(x1,mid);val n=18;for(i in 1..n){val xx=x1+(x2-x1)*i/n;val yy=mid+if(i==n)0f else if(i%2==0)-9f else 9f;lineTo(xx,yy)}};drawPath(spring,P.muted,style=Stroke(2f));val pos=Offset(x2,mid);drawCircle(P.blue,12f,pos)
    val vvscale=.12*v.spanX/max(abs(s.velocity),1.0);if(s.velocity!=0.0)arrow(pos,Offset(sx(s.x+s.velocity*vvscale),mid),P.green);label("x ${fmt(s.displacement)} m · v ${fmt(s.velocity)} m/s",Offset(pos.x+12,pos.y-18),P.ink)
    if(p.springMode&&s.totalEnergy!=null){val width=100f;val ratio=(s.kineticEnergy!!/s.totalEnergy).toFloat().coerceIn(0f,1f);drawLine(P.green,Offset(12f,size.height-15),Offset(12f+width*ratio,size.height-15),7f);drawLine(P.amber,Offset(12f+width*ratio,size.height-15),Offset(12f+width,size.height-15),7f)}
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSeries(vm:PhysicsMathViewModel,v:PhysicsViewport,sx:(Double)->Float,sy:(Double)->Float){
    val series=vm.graphSeries.value;val duration=vm.duration();val path=Path();for(i in 0..420){val t=duration*i/420;val y=GraphSeries.value(vm,series,t);if(!y.isFinite()){continue};if(i==0)path.moveTo(sx(t),sy(y))else path.lineTo(sx(t),sy(y))};val color=when(series){PhysicsGraphSeries.Acceleration,PhysicsGraphSeries.ShmAcceleration,PhysicsGraphSeries.Vy->P.red;PhysicsGraphSeries.Velocity,PhysicsGraphSeries.ShmVelocity,PhysicsGraphSeries.Vx->P.green;PhysicsGraphSeries.Speed->P.violet;else->P.blue};drawPath(path,color,style=Stroke(3f,cap=StrokeCap.Round))
    val t=vm.time.floatValue.toDouble().coerceIn(0.0,duration);val y=GraphSeries.value(vm,series,t);if(y.isFinite()){val x=sx(t);drawLine(P.amber,Offset(x,0f),Offset(x,size.height),1.7f);drawCircle(P.amber,6f,Offset(x,sy(y)));label("t ${fmt(t)} s · ${series.title} ${fmt(y)}",Offset(x+9,sy(y)-12),P.ink)}
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.arrow(from:Offset,to:Offset,color:androidx.compose.ui.graphics.Color){drawLine(color,from,to,2.4f,cap=StrokeCap.Round);val a=atan2(to.y-from.y,to.x-from.x);val r=8f;drawLine(color,to,Offset(to.x-r*cos(a-.55f),to.y-r*sin(a-.55f)),2.4f,cap=StrokeCap.Round);drawLine(color,to,Offset(to.x-r*cos(a+.55f),to.y-r*sin(a+.55f)),2.4f,cap=StrokeCap.Round)}
private fun androidx.compose.ui.graphics.drawscope.DrawScope.label(text:String,pos:Offset,color:androidx.compose.ui.graphics.Color){drawContext.canvas.nativeCanvas.drawText(text,pos.x,pos.y,android.graphics.Paint().apply{this.color=android.graphics.Color.WHITE;textSize=12f;setShadowLayer(4f,0f,1f,android.graphics.Color.WHITE)});drawContext.canvas.nativeCanvas.drawText(text,pos.x,pos.y,android.graphics.Paint().apply{this.color=color.toArgb();textSize=12f;isAntiAlias=true})}
private fun androidx.compose.ui.graphics.Color.toArgb()=android.graphics.Color.argb((alpha*255).toInt(),(red*255).toInt(),(green*255).toInt(),(blue*255).toInt())
private fun niceStep(raw:Double):Double{if(!raw.isFinite()||raw<=0)return 1.0;val base=10.0.pow(floor(log10(raw)));val f=raw/base;return (if(f<1.5)1 else if(f<3.5)2 else if(f<7.5)5 else 10)*base}
private fun tick(x:Double)=if(abs(x)>=1e5||(abs(x)<1e-3&&x!=0.0))"%.1e".format(x) else "%.3g".format(x)
private fun fmt(x:Double)=if(!x.isFinite())"—"else if(abs(x)>=1e6||(abs(x)<1e-4&&x!=0.0))"%.3e".format(x)else "%.5g".format(x)
