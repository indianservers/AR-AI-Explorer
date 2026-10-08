package com.indianservers.aiexplorer.spatial

import com.indianservers.aiexplorer.core.*
import kotlin.math.*

fun arCadWorldGeometry(g: SpatialGeometry, pose: ArGraphObjectState): SpatialGeometry {
    val t=pose.transform()
    return g.copy(vertices=g.vertices.map { p -> val v=t.orientation.rotate(com.indianservers.aiexplorer.arengine.contract.ArVector3(p.x*t.axisScale.x,p.y*t.axisScale.y,p.z*t.axisScale.z)*t.uniformScale)+t.offsetMeters; Vec3(v.x,v.y,v.z) })
}
fun arCadEquationAndMeasurements(node: ArCadNode, geometry: SpatialGeometry, pose: ArGraphObjectState, analytic: Boolean = true): Map<String,String> {
    val g=arCadWorldGeometry(geometry,pose); val values=linkedMapOf<String,String>(); val vertices=g.vertices
    if(vertices.isEmpty()) return values
    val center=if(node.type==ArCadType.Sphere) Vec3((vertices.minOf { it.x }+vertices.maxOf { it.x })*.5,(vertices.minOf { it.y }+vertices.maxOf { it.y })*.5,(vertices.minOf { it.z }+vertices.maxOf { it.z })*.5) else vertices.reduce(Vec3::plus)*(1.0/vertices.size)
    values["Position"]="${center.x}, ${center.y}, ${center.z}"
    if(analytic && node.type==ArCadType.Sphere && abs(pose.axisScale.x-pose.axisScale.y)<1e-8 && abs(pose.axisScale.y-pose.axisScale.z)<1e-8) {
        val r=ArCadTopology.number(node.parameters["radius"] ?: "1")*pose.scale*pose.axisScale.x
        values["Sphere equation"]="(x-(${center.x}))^2+(y-(${center.y}))^2+(z-(${center.z}))^2=${r*r}"
        values["Radius"]=r.toString(); values["Diameter"]=(2*r).toString(); values["Circumference"]=(2*PI*r).toString(); values["Surface area"]=(4*PI*r*r).toString(); values["Volume"]=(4*PI*r*r*r/3).toString()
    }
    if(analytic && node.type==ArCadType.Circle && vertices.size>=3) {
        val radius=(vertices.first()-center).magnitude()
        if(vertices.all { abs((it-center).magnitude()-radius)<1e-6 }) {
            values["Radius"]=radius.toString(); values["Diameter"]=(2*radius).toString(); values["Circumference"]=(2*PI*radius).toString(); values["Area"]=(PI*radius*radius).toString()
        }
    }
    if(node.type==ArCadType.Plane && vertices.size>=3) {
        var normal=AnalyticGeometry3D.cross(vertices[1]-vertices[0],vertices[2]-vertices[0]).normalized()
        if(normal.z<0 || abs(normal.z)<1e-10 && normal.y<0 || abs(normal.z)+abs(normal.y)<1e-10 && normal.x<0) normal=normal*(-1.0)
        if(vertices.all { abs((it-vertices[0]).dot(normal))<1e-7 }) {
            values["Plane normal"]="${normal.x}, ${normal.y}, ${normal.z}"
            values["Plane equation"]="${normal.x}x+${normal.y}y+${normal.z}z=${normal.dot(vertices[0])}"
        } else values["Geometry"]="Non-planar mesh; no unique plane equation"
    }
    if(node.type in setOf(ArCadType.Vector,ArCadType.Segment,ArCadType.Line,ArCadType.Ray) && vertices.size>=2) {
        val d=vertices[1]-vertices[0]; values["Components"]="${d.x}, ${d.y}, ${d.z}"; values[if(node.type in setOf(ArCadType.Line,ArCadType.Ray)) "Displayed extent" else "Magnitude"]=d.magnitude().toString(); values["Direction"]="${d.normalized()}"
    }
    if(g.triangles.isNotEmpty()) values["Mesh area"]=g.triangles.chunked(3).sumOf { f -> AnalyticGeometry3D.cross(vertices[f[1]]-vertices[f[0]],vertices[f[2]]-vertices[f[0]]).magnitude()*.5 }.toString()
    if(node.type in setOf(ArCadType.Cube,ArCadType.Cuboid,ArCadType.Cylinder,ArCadType.Cone,ArCadType.Prism,ArCadType.Pyramid)) values["Mesh volume"]=abs(g.triangles.chunked(3).sumOf { f -> vertices[f[0]].dot(AnalyticGeometry3D.cross(vertices[f[1]],vertices[f[2]]))/6.0 }).toString()
    return values
}
object ArCadVectorMath {
    fun calculate(a: Vec3,b: Vec3): Map<String,Vec3> = mapOf("Addition" to a+b,"Subtraction" to a-b,"Cross product" to AnalyticGeometry3D.cross(a,b),"Projection" to if(b.dot(b)>1e-12) b*(a.dot(b)/b.dot(b)) else error("Cannot project onto zero vector"))
    fun angle(a: Vec3,b: Vec3): Double { require(a.magnitude()*b.magnitude()>1e-12); return acos((a.dot(b)/(a.magnitude()*b.magnitude())).coerceIn(-1.0,1.0)) }
}
