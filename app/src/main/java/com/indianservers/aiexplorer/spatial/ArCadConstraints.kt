package com.indianservers.aiexplorer.spatial

import com.indianservers.aiexplorer.core.*
import kotlin.math.*

enum class ArCadConstraintKind { Coincident, Parallel, Perpendicular, EqualLength, FixedLength, FixedAngle, PointOnLine, PointOnPlane, PointOnSurface, Locked }
data class ArCadConstraint(val id: String,val kind: ArCadConstraintKind,val target: List<Int>,val reference: List<Int> = emptyList(),val value: Double = 0.0) {
    fun encode()=listOf(id,kind.name,target.joinToString(","),reference.joinToString(","),value).joinToString("|")
    companion object { fun decode(s:String): ArCadConstraint { val f=s.split('|'); fun ids(s:String)=s.split(',').filter(String::isNotBlank).map(String::toInt)
        return ArCadConstraint(f[0],ArCadConstraintKind.valueOf(f[1]),ids(f[2]),ids(f[3]),f[4].toDouble()) } }
}
object ArCadConstraintSolver {
    fun solve(g: SpatialGeometry,constraints: List<ArCadConstraint>,baseline: SpatialGeometry=g): SpatialGeometry {
        require(constraints.all { (it.target+it.reference).all { i -> i in g.vertices.indices } && it.value.isFinite() }) { "Constraint refers to missing geometry" }
        val points=g.vertices.toMutableList(); val locked=constraints.filter { it.kind==ArCadConstraintKind.Locked }.flatMap { it.target }.toSet()
        fun set(index:Int,p:Vec3) { if(index !in locked) points[index]=p }
        fun direction(ids:List<Int>):Vec3 { require(ids.size>=2); val d=points[ids[1]]-points[ids[0]]; require(d.magnitude()>1e-10) { "Constraint has a zero-length edge" }; return d.normalized() }
        fun setEdge(c:ArCadConstraint,dir:Vec3,length:Double) { require(c.target.size==2 && length>0); set(c.target[1],points[c.target[0]]+dir*length) }
        repeat(24) {
            constraints.forEach { c ->
                val length=if(c.target.size>=2) (points[c.target[1]]-points[c.target[0]]).magnitude() else 0.0
                when(c.kind) {
                    ArCadConstraintKind.Locked -> c.target.forEach { points[it]=baseline.vertices[it] }
                    ArCadConstraintKind.Coincident -> { require(c.target.size==1 && c.reference.size==1); set(c.target[0],points[c.reference[0]]) }
                    ArCadConstraintKind.FixedLength -> setEdge(c,direction(c.target),c.value)
                    ArCadConstraintKind.EqualLength -> { require(c.reference.size==2); setEdge(c,direction(c.target),(points[c.reference[1]]-points[c.reference[0]]).magnitude()) }
                    ArCadConstraintKind.Parallel -> setEdge(c,direction(c.reference),length)
                    ArCadConstraintKind.Perpendicular -> { val ref=direction(c.reference); val d=direction(c.target); val projected=d-ref*d.dot(ref); val perpendicular=if(projected.magnitude()>1e-8) projected.normalized() else AnalyticGeometry3D.cross(ref,if(abs(ref.y)<.9) Vec3(0.0,1.0,0.0) else Vec3(1.0,0.0,0.0)).normalized(); setEdge(c,perpendicular,length) }
                    ArCadConstraintKind.FixedAngle -> { val ref=direction(c.reference); val d=direction(c.target); val axis=(d-ref*d.dot(ref)).normalized().takeIf { it.magnitude()>1e-8 } ?: AnalyticGeometry3D.cross(ref,if(abs(ref.y)<.9) Vec3(0.0,1.0,0.0) else Vec3(1.0,0.0,0.0)).normalized(); setEdge(c,ref*cos(c.value)+axis*sin(c.value),length) }
                    ArCadConstraintKind.PointOnLine -> { require(c.target.size==1); val a=points[c.reference[0]]; val d=direction(c.reference); set(c.target[0],a+d*(points[c.target[0]]-a).dot(d)) }
                    ArCadConstraintKind.PointOnPlane -> { require(c.target.size==1 && c.reference.size>=3); val a=points[c.reference[0]]; val n=AnalyticGeometry3D.cross(points[c.reference[1]]-a,points[c.reference[2]]-a).normalized(); require(n.magnitude()>1e-8); set(c.target[0],points[c.target[0]]-n*(points[c.target[0]]-a).dot(n)) }
                    ArCadConstraintKind.PointOnSurface -> { require(c.target.size==1 && c.reference.size>=3); val mesh=SpatialGeometry(c.reference.map(points::get),(1 until c.reference.size-1).flatMap { listOf(0,it,it+1) }); set(c.target[0],SpatialAnalysisTools3D.project(points[c.target[0]],mesh) ?: error("No reference surface")) }
                }
            }
        }
        val solved=g.copy(vertices=points)
        constraints.forEach { c ->
            val a=points[c.target.first()]
            val residual=when(c.kind) {
                ArCadConstraintKind.Locked -> c.target.maxOf { (points[it]-baseline.vertices[it]).magnitude() }
                ArCadConstraintKind.Coincident -> (a-points[c.reference[0]]).magnitude()
                ArCadConstraintKind.FixedLength -> abs((points[c.target[1]]-a).magnitude()-c.value)
                ArCadConstraintKind.EqualLength -> abs((points[c.target[1]]-a).magnitude()-(points[c.reference[1]]-points[c.reference[0]]).magnitude())
                ArCadConstraintKind.Parallel -> AnalyticGeometry3D.cross(direction(c.target),direction(c.reference)).magnitude()
                ArCadConstraintKind.Perpendicular -> abs(direction(c.target).dot(direction(c.reference)))
                ArCadConstraintKind.FixedAngle -> abs(acos(direction(c.target).dot(direction(c.reference)).coerceIn(-1.0,1.0))-c.value)
                ArCadConstraintKind.PointOnLine -> AnalyticGeometry3D.cross(a-points[c.reference[0]],direction(c.reference)).magnitude()
                ArCadConstraintKind.PointOnPlane -> { val p=points[c.reference[0]]; abs((a-p).dot(AnalyticGeometry3D.cross(points[c.reference[1]]-p,points[c.reference[2]]-p).normalized())) }
                ArCadConstraintKind.PointOnSurface -> { val mesh=SpatialGeometry(c.reference.map(points::get),(1 until c.reference.size-1).flatMap { listOf(0,it,it+1) }); (a-(SpatialAnalysisTools3D.project(a,mesh) ?: a)).magnitude() }
            }
            require(residual<1e-6) { "Conflicting constraint ${c.kind}: residual $residual" }
        }
        ArCadTopology.validate(solved); return solved
    }
}
