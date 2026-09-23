package com.indianservers.aiexplorer.mathworkspace.physicsmath

import kotlin.math.*

object PhysicsMathEngine{
    fun linear(p:LinearMotionParameters,t:Double)=LinearMotionState(t,p.startX+p.velocity*t+.5*p.acceleration*t*t,p.velocity+p.acceleration*t,p.acceleration)
    fun linearAutoFit(p:LinearMotionParameters,duration:Double=10.0):PhysicsViewport{
        val positions=mutableListOf(p.startX,linear(p,duration).x)
        if(p.acceleration!=0.0){val vertex=-p.velocity/p.acceleration;if(vertex in 0.0..duration)positions+=linear(p,vertex).x}
        val low=positions.minOrNull()?:p.startX;val high=positions.maxOrNull()?:p.startX;val padding=((high-low)*.2).coerceAtLeast(2.0)
        return PhysicsViewport(low-padding,high+padding,-2.0,2.0)
    }
    fun forceSum(forces:List<ForceVector>,mass:Double):Pair<ForceVector,ForceVector>?{
        if(!mass.isFinite()||mass<=0||forces.any{!it.magnitude.isFinite()||it.magnitude<0||!it.angleDegrees.isFinite()})return null
        val x=forces.sumOf{it.x};val y=forces.sumOf{it.y};val resultant=ForceVector(hypot(x,y),Math.toDegrees(atan2(y,x)))
        return resultant to ForceVector(resultant.magnitude/mass,resultant.angleDegrees)
    }
}
