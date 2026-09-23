package com.indianservers.aiexplorer.mathworkspace.mathematicalart

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MathematicalArtEngineTest {
    @Test fun hypotrochoidHandlesReferenceAndLargeInputs(){
        val reference=SpirographEngine.sample(5.0,3.0,5.0,SpiroType.Hypotrochoid,0.0,6.0)
        val large=SpirographEngine.sample(5000.0,1700.0,2300.0,SpiroType.Epitrochoid,0.0,6.0)
        assertTrue(reference.size>1000);assertTrue(reference.all{it.x.isFinite()&&it.y.isFinite()});assertTrue(large.all{it.x.isFinite()&&it.y.isFinite()})
    }
    @Test fun roseSupportsIntegerAndFractionalFrequency(){
        assertEquals(5,RoseCurveEngine.typicalPetals(5.0));assertEquals(8,RoseCurveEngine.typicalPetals(4.0));assertEquals(null,RoseCurveEngine.typicalPetals(2.5))
        assertTrue(RoseCurveEngine.sample(1.0,2.5,0.0,false).size>1000)
    }
    @Test fun lissajousAndParametricPatternsAreFinite(){
        assertTrue(LissajousEngine.sample(1.0,1.0,3.0,2.0,Math.PI/2).all{it.x.isFinite()&&it.y.isFinite()})
        assertTrue(ParametricCurveEngine.sample("cos(t)","sin(t)",0.0,2*Math.PI).all{it.x.isFinite()&&it.y.isFinite()})
        assertTrue(ParametricCurveEngine.sample("1/t","sin(t)",-1.0,1.0).any{!it.x.isFinite()})
    }
    @Test fun polarParserEvaluatesCardioidAndSpiralPresets(){
        val cardioid=PolarArtEngine.sample("1 + cos(theta)",0.0,2*Math.PI);val spiral=PolarArtEngine.sample("theta/5",0.0,6*Math.PI)
        assertEquals(2.0,cardioid.first().x,1e-8);assertTrue(spiral.last().x.isFinite())
    }
}
