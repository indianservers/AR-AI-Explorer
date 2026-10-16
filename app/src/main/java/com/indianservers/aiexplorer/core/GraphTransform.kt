package com.indianservers.aiexplorer.core

import kotlin.math.*

/** Local scale, orthogonal orientation, then world translation; Z is up. */
data class GraphTransform(
    val position: Vec3 = Vec3(0.0,0.0,0.0),
    val scale: Vec3 = Vec3(1.0,1.0,1.0),
    val orientation: List<Double> = listOf(1.0,0.0,0.0,0.0,1.0,0.0,0.0,0.0,1.0),
) {
    init { require(orientation.size == 9 && (orientation + listOf(position.x,position.y,position.z,scale.x,scale.y,scale.z)).all(Double::isFinite)) }
    fun direction(v: Vec3) = Vec3(orientation[0]*v.x+orientation[1]*v.y+orientation[2]*v.z,orientation[3]*v.x+orientation[4]*v.y+orientation[5]*v.z,orientation[6]*v.x+orientation[7]*v.y+orientation[8]*v.z)
    fun point(v: Vec3) = direction(Vec3(v.x*scale.x,v.y*scale.y,v.z*scale.z)) + position
    fun rotated(axis: String, degrees: Double, pivot: Vec3): GraphTransform {
        require(degrees.isFinite() && abs(degrees) <= 360000) { "Rotation must be finite and at most 360000 degrees." }
        val angle = Math.toRadians(degrees % 360); val c=cos(angle); val s=sin(angle)
        val m = when(axis) { "x" -> listOf(1.0,0.0,0.0,0.0,c,-s,0.0,s,c); "y" -> listOf(c,0.0,s,0.0,1.0,0.0,-s,0.0,c); else -> listOf(c,-s,0.0,s,c,0.0,0.0,0.0,1.0) }
        val rot = GraphTransform(orientation=m)
        val product=List(9) { i -> (0..2).sumOf { k -> m[i/3*3+k]*orientation[k*3+i%3] } }
        return copy(position=pivot+rot.direction(position-pivot),orientation=product)
    }
    fun encode() = (listOf(position.x,position.y,position.z,scale.x,scale.y,scale.z)+orientation).joinToString(",")
    companion object {
        fun decode(text: String): GraphTransform {
            val v=text.split(',').map(String::toDouble); require(v.size==15)
            return GraphTransform(Vec3(v[0],v[1],v[2]),Vec3(v[3],v[4],v[5]),v.drop(6))
        }
    }
}
