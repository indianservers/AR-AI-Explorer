package com.indianservers.aiexplorer

import androidx.compose.ui.geometry.Offset
import com.indianservers.aiexplorer.core.Vec3
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class SpatialPerspectiveProjectionTest {
    @Test fun projectedStraightLinesContainEveryProjectedDefiningPoint() {
        val a=Vec3(-2.0,1.0,-1.0);val b=Vec3(3.0,2.0,2.0)
        for(perspective in listOf(false,true)) {
            val pa=projectSpatialPoint(a,Offset(500f,600f),74f,perspective)
            val pb=projectSpatialPoint(b,Offset(500f,600f),74f,perspective)
            for(t in listOf(-1.0,0.25,0.5,0.75,2.0)) {
                val p=projectSpatialPoint(a+(b-a)*t,Offset(500f,600f),74f,perspective)
                val d=pb-pa;val e=p-pa
                val distance=abs(d.x*e.y-d.y*e.x)/d.getDistance()
                assertTrue("Perspective=$perspective t=$t distance=$distance",distance<0.001)
            }
        }
    }
}
