package com.indianservers.aiexplorer.mathworkspace.physicsmath

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs

class PhysicsMathTest {
    @Test fun projectileMatchesEarthAndMoonCases() {
        val earth=ProjectileParameters(20.0,45.0,9.80665,0.0)
        val result=ProjectileMotionEngine.summary(earth)
        assertEquals(2.884, result.flightTime!!, .002)
        assertEquals(40.79, result.range!!, .02)
        assertEquals(10.20, result.maxHeight!!, .02)
        val moon=ProjectileMotionEngine.summary(earth.copy(gravity=1.62))
        assertTrue(moon.flightTime!!>result.flightTime!!*2)
        assertTrue(moon.range!!>result.range!!*2)
    }
    @Test fun projectileSupportsVerticalHorizontalCustomHeightAndZeroGravity() {
        val vertical=ProjectileMotionEngine.summary(ProjectileParameters(20.0,90.0,9.80665,0.0))
        assertEquals(0.0,vertical.range!!,1e-10)
        assertEquals(20.0*20.0/(2*9.80665),vertical.maxHeight!!,1e-10)
        val horizontal=ProjectileMotionEngine.summary(ProjectileParameters(20.0,0.0,9.80665,0.0))
        assertEquals(0.0,horizontal.range!!,1e-12)
        val raised=ProjectileMotionEngine.summary(ProjectileParameters(20.0,0.0,9.80665,5.0))
        assertEquals(20.0*kotlin.math.sqrt(10.0/9.80665),raised.range!!,1e-9)
        assertNull(ProjectileMotionEngine.summary(ProjectileParameters(20.0,45.0,0.0,0.0)).flightTime)
        assertTrue(ProjectileMotionEngine.autoFit(ProjectileParameters(20_000.0,45.0,9.80665)).maxX>1e7)
    }
    @Test fun oneDimensionalMotionEquationsAndPresets() {
        val a=PhysicsMathEngine.linear(LinearMotionParameters(0.0,10.0,2.0),5.0)
        assertEquals(75.0,a.x,1e-12);assertEquals(20.0,a.velocity,1e-12)
        val b=PhysicsMathEngine.linear(LinearMotionParameters(10.0,-5.0,0.0),2.0)
        assertEquals(0.0,b.x,1e-12);assertEquals(-5.0,b.velocity,1e-12)
    }
    @Test fun shmValuesPeriodAndSpringEnergyAreConsistent() {
        val p=OscillationParameters(2.0,.5,0.0,0.0)
        assertEquals(2.0,OscillationEngine.state(p,0.0).displacement,1e-12)
        assertEquals(0.0,OscillationEngine.state(p,.5).displacement,1e-12)
        assertEquals(-2.0,OscillationEngine.state(p,1.0).displacement,1e-12)
        assertEquals(2.0,OscillationEngine.period(p),1e-12)
        val spring=OscillationParameters(2.0,.5,0.0,0.0,true,1.0,4.0)
        val energy=OscillationEngine.state(spring,0.0).totalEnergy
        assertEquals(8.0,energy!!,1e-12)
        for(i in 0..100)assertEquals(energy,OscillationEngine.state(spring,i/100.0).totalEnergy!!,1e-10)
    }
    @Test fun graphsUseTheSameEquationsAsTheSimulation() {
        val vm=PhysicsMathViewModel();vm.experiment.value=MotionExperiment.Projectile;vm.speed.value="20";vm.angle.value="45";vm.gravity.value="9.80665"
        val projectile=ProjectileMotionEngine.state(vm.projectile()!!,1.0)
        assertEquals(projectile.y,GraphSeries.value(vm,PhysicsGraphSeries.Y,1.0),0.0)
        vm.experiment.value=MotionExperiment.SHM;vm.amplitude.value="2";vm.frequency.value="0.5"
        assertEquals(0.0,GraphSeries.value(vm,PhysicsGraphSeries.Displacement,.5),1e-12)
    }
    @Test fun forceSumFoundationRejectsInvalidMass() {
        val sum=PhysicsMathEngine.forceSum(listOf(ForceVector(10.0,0.0),ForceVector(10.0,180.0)),2.0)!!
        assertTrue(abs(sum.first.magnitude)<1e-12);assertTrue(abs(sum.second.magnitude)<1e-12)
        assertNull(PhysicsMathEngine.forceSum(emptyList(),0.0))
    }
}
