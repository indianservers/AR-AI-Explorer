package com.indianservers.aiexplorer.mathworkspace.physicsmath

enum class PhysicsMode(val title:String){Motion("Motion"),Oscillations("Oscillations"),Graphs("Graphs")}
enum class MotionExperiment(val title:String){Projectile("Projectile Motion"),Linear("1D Motion"),SHM("Simple Harmonic Motion")}
enum class PhysicsGraphSeries(val title:String){X("x(t)"),Y("y(t)"),Vx("vx(t)"),Vy("vy(t)"),Speed("Speed"),Position("Position"),Velocity("Velocity"),Acceleration("Acceleration"),Displacement("Displacement"),ShmVelocity("Velocity"),ShmAcceleration("Acceleration")}
data class ProjectileParameters(val speed:Double=20.0,val angleDegrees:Double=45.0,val gravity:Double=9.80665,val launchHeight:Double=0.0,val startX:Double=0.0)
data class ProjectileState(val time:Double,val x:Double,val y:Double,val vx:Double,val vy:Double,val ax:Double,val ay:Double){val speed get()=kotlin.math.hypot(vx,vy)}
data class ProjectileSummary(val flightTime:Double?,val range:Double?,val maxHeight:Double?)
data class LinearMotionParameters(val startX:Double=0.0,val velocity:Double=10.0,val acceleration:Double=0.0)
data class LinearMotionState(val time:Double,val x:Double,val velocity:Double,val acceleration:Double)
data class OscillationParameters(val amplitude:Double=2.0,val frequency:Double=.5,val phaseRadians:Double=0.0,val equilibrium:Double=0.0,val springMode:Boolean=false,val mass:Double=1.0,val springConstant:Double=4.0)
data class OscillationState(val time:Double,val x:Double,val displacement:Double,val velocity:Double,val acceleration:Double,val kineticEnergy:Double?,val potentialEnergy:Double?,val totalEnergy:Double?)
data class PhysicsViewport(val minX:Double=0.0,val maxX:Double=1.0,val minY:Double=0.0,val maxY:Double=1.0){val spanX get()=(maxX-minX).coerceAtLeast(1e-12);val spanY get()=(maxY-minY).coerceAtLeast(1e-12)}
data class PhysicsOverlays(val trajectory:Boolean=true,val position:Boolean=true,val velocity:Boolean=true,val acceleration:Boolean=false,val grid:Boolean=true,val labels:Boolean=true)
data class ForceVector(val magnitude:Double,val angleDegrees:Double){val x get()=magnitude*kotlin.math.cos(Math.toRadians(angleDegrees));val y get()=magnitude*kotlin.math.sin(Math.toRadians(angleDegrees))}
