package com.indianservers.aiexplorer.core

import kotlin.math.*

/** Native solids use Euler X, then Y, then Z, all in degrees. */
object SolidPoseMath {
    fun transform(s:Solid):GraphTransform=GraphTransform(position=s.position).rotated("x",s.rotation.x%360,s.position).rotated("y",s.rotation.y%360,s.position).rotated("z",s.rotation.z%360,s.position)
    fun world(s:Solid,v:Vec3)=transform(s).point(v)
    fun rotateWorld(s:Solid,axis:String,degrees:Double,pivot:Vec3):Solid {
        val t=transform(s).rotated(axis,degrees%360,pivot);val m=t.orientation
        val y=asin((-m[6]).coerceIn(-1.0,1.0));val x:Double;val z:Double
        if(abs(cos(y))>1e-9){x=atan2(m[7],m[8]);z=atan2(m[3],m[0])}else{x=atan2(-m[5],m[4]);z=0.0}
        return s.copy(position=t.position,rotation=Vec3(Math.toDegrees(x),Math.toDegrees(y),Math.toDegrees(z)))
    }
}
