package com.indianservers.aiexplorer.mathworkspace.physicsmath

import kotlin.math.*

object OscillationEngine{
    fun angularFrequency(p:OscillationParameters):Double=if(p.springMode&&p.mass>0&&p.springConstant>0)sqrt(p.springConstant/p.mass) else 2*PI*p.frequency
    fun state(p:OscillationParameters,t:Double):OscillationState{
        val omega=angularFrequency(p);val phase=omega*t+p.phaseRadians;val d=p.amplitude*cos(phase);val v=-p.amplitude*omega*sin(phase);val a=-omega*omega*d
        val kinetic=if(p.springMode&&p.mass>0).5*p.mass*v*v else null
        val potential=if(p.springMode&&p.mass>0&&p.springConstant>0).5*p.springConstant*d*d else null
        val total=if(kinetic!=null&&potential!=null)kinetic+potential else null
        return OscillationState(t,p.equilibrium+d,d,v,a,kinetic,potential,total)
    }
    fun period(p:OscillationParameters)=2*PI/angularFrequency(p)
    fun autoFit(p:OscillationParameters):PhysicsViewport{
        val left=p.equilibrium-1.5*abs(p.amplitude);val right=p.equilibrium+1.5*abs(p.amplitude)
        val bound=3*abs(p.amplitude).coerceAtLeast(1.0)
        return PhysicsViewport(left,right,-bound,bound)
    }
}
