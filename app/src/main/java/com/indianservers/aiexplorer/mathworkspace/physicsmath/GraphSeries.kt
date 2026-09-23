package com.indianservers.aiexplorer.mathworkspace.physicsmath

import kotlin.math.*

object GraphSeries{
    fun value(vm:PhysicsMathViewModel,series:PhysicsGraphSeries,t:Double):Double=when(vm.experiment.value){
        MotionExperiment.Projectile->{val p=vm.projectile()?:return Double.NaN;val s=ProjectileMotionEngine.state(p,t);when(series){PhysicsGraphSeries.X,PhysicsGraphSeries.Position->s.x;PhysicsGraphSeries.Y->s.y;PhysicsGraphSeries.Vx->s.vx;PhysicsGraphSeries.Vy->s.vy;PhysicsGraphSeries.Speed->s.speed;else->Double.NaN}}
        MotionExperiment.Linear->{val p=vm.linear()?:return Double.NaN;val s=PhysicsMathEngine.linear(p,t);when(series){PhysicsGraphSeries.X,PhysicsGraphSeries.Position->s.x;PhysicsGraphSeries.Velocity,PhysicsGraphSeries.Vx->s.velocity;PhysicsGraphSeries.Acceleration,PhysicsGraphSeries.Vy->s.acceleration;PhysicsGraphSeries.Speed->abs(s.velocity);else->Double.NaN}}
        MotionExperiment.SHM->{val p=vm.oscillation()?:return Double.NaN;val s=OscillationEngine.state(p,t);when(series){PhysicsGraphSeries.Displacement,PhysicsGraphSeries.Position,PhysicsGraphSeries.X->s.displacement;PhysicsGraphSeries.Velocity,PhysicsGraphSeries.ShmVelocity,PhysicsGraphSeries.Vx->s.velocity;PhysicsGraphSeries.Acceleration,PhysicsGraphSeries.ShmAcceleration,PhysicsGraphSeries.Vy->s.acceleration;else->Double.NaN}}
    }
    fun choices(experiment:MotionExperiment)=when(experiment){MotionExperiment.Projectile->listOf(PhysicsGraphSeries.X,PhysicsGraphSeries.Y,PhysicsGraphSeries.Vx,PhysicsGraphSeries.Vy,PhysicsGraphSeries.Speed);MotionExperiment.Linear->listOf(PhysicsGraphSeries.Position,PhysicsGraphSeries.Velocity,PhysicsGraphSeries.Acceleration);MotionExperiment.SHM->listOf(PhysicsGraphSeries.Displacement,PhysicsGraphSeries.ShmVelocity,PhysicsGraphSeries.ShmAcceleration)}
    fun autoFit(vm:PhysicsMathViewModel):PhysicsViewport{
        val end=vm.duration().coerceAtLeast(1e-6);val series=vm.graphSeries.value
        val vals=(0..300).map{value(vm,series,end*it/300)}.filter{it.isFinite()}
        val low=vals.minOrNull()?:-1.0;val high=vals.maxOrNull()?:1.0;val padding=((high-low)*.15).coerceAtLeast(.5)
        return PhysicsViewport(-end*.05,end*1.05,low-padding,high+padding)
    }
}
