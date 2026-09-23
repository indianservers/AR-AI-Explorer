package com.indianservers.aiexplorer.mathworkspace.physicsmath

import kotlin.math.*

object ProjectileMotionEngine{
    fun state(p:ProjectileParameters,t:Double):ProjectileState{
        val angle=Math.toRadians(p.angleDegrees);val vx=p.speed*cos(angle);val vy0=p.speed*sin(angle)
        return ProjectileState(t,p.startX+vx*t,p.launchHeight+vy0*t-.5*p.gravity*t*t,vx,vy0-p.gravity*t,0.0,-p.gravity)
    }
    fun summary(p:ProjectileParameters):ProjectileSummary{
        val angle=Math.toRadians(p.angleDegrees);val vy=p.speed*sin(angle);val discriminant=vy*vy+2*p.gravity*p.launchHeight
        val flight=when{
            p.gravity<0->null
            p.gravity==0.0->null
            discriminant<0.0->null
            else->{val root=sqrt(discriminant);val candidates=listOf((vy+root)/p.gravity,(vy-root)/p.gravity).filter{it>=0&&it.isFinite()};candidates.maxOrNull()}
        }
        val maxHeight=if(p.gravity>0) p.launchHeight+max(0.0,vy).pow(2)/(2*p.gravity) else null
        val range=flight?.let{state(p,it).x-p.startX}
        return ProjectileSummary(flight,range,maxHeight)
    }
    fun flatGroundRange(speed:Double,angleDegrees:Double,gravity:Double):Double?=if(gravity>0)speed*speed*sin(Math.toRadians(2*angleDegrees))/gravity else null
    fun autoFit(p:ProjectileParameters):PhysicsViewport{
        val s=summary(p);val angle=Math.toRadians(p.angleDegrees);val vx=p.speed*cos(angle);val vy=p.speed*sin(angle)
        val tEnd=s.flightTime?:when{p.gravity==0.0->10.0;p.gravity>1e-12&&vy>0->2*vy/p.gravity;abs(vx)>1e-9->20_000.0/abs(vx);else->10.0}
        val duration=if(tEnd.isFinite()&&tEnd>0)tEnd else 10.0
        val endX=p.startX+vx*duration
        val peak=s.maxHeight?:max(p.launchHeight,p.launchHeight+vy*duration-.5*p.gravity*duration*duration)
        val minX=min(p.startX,endX);val maxX=max(p.startX,endX)
        val minY=min(0.0,p.launchHeight);val maxY=max(peak,p.launchHeight).coerceAtLeast(minY+1.0)
        val padX=((maxX-minX)*.12).coerceAtLeast(1.0);val padY=((maxY-minY)*.16).coerceAtLeast(1.0)
        return PhysicsViewport(minX-padX,maxX+padX,minY-padY,maxY+padY)
    }
}
