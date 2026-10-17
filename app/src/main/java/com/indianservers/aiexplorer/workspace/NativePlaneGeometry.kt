package com.indianservers.aiexplorer.workspace

import com.indianservers.aiexplorer.core.Vec3
import kotlin.math.*

data class Plane3DObject(val id:String,val name:String,val pointIds:List<String>,val visible:Boolean=true,val locked:Boolean=false,val styleKey:String="cyan") {
    init {require(id.isNotBlank()&&name.isNotBlank()&&pointIds.size==3&&pointIds.distinct().size==3)}
}
/** Stable point references define infinite planes; bounded patches are only a display projection. */
object NativePlaneGeometry {
    data class Plane(val id:String,val name:String,val origin:Vec3,val normal:Vec3,val tangent:Vec3,val patch:List<Vec3>)
    data class Angle(val center:Vec3,val first:Vec3,val second:Vec3,val arc:List<Vec3>,val degrees:Double,val parallel:Boolean)
    fun cross(a:Vec3,b:Vec3)=Vec3(a.y*b.z-a.z*b.y,a.z*b.x-a.x*b.z,a.x*b.y-a.y*b.x)
    fun norm(v:Vec3)=hypot(hypot(v.x,v.y),v.z)
    private fun unit(v:Vec3):Vec3 {val n=norm(v);require(n.isFinite()&&n>1e-12){"Three non-collinear points are required to define a plane."};return v*(1/n)}
    fun evaluate(s:WorkspaceState,obj:Plane3DObject):Plane {
        val p=obj.pointIds.map{id->s.points3D.singleOrNull{it.id==id}?.position ?: throw IllegalArgumentException("A defining plane point is missing or ambiguous.")}
        require(p.all{v->listOf(v.x,v.y,v.z).all{it.isFinite()&&abs(it)<=10000}}){"Use finite plane coordinates within 10000 units."}
        val u=unit(p[1]-p[0]);val normal=unit(cross(u,unit(p[2]-p[0])));val v=unit(cross(normal,u))
        val center=(p[0]+p[1]+p[2])*(1.0/3);val radius=p.maxOf{norm(it-center)}.coerceIn(1.0,5.0)
        return Plane(obj.id,obj.name,p[0],normal,u,listOf(center-u*radius-v*radius,center+u*radius-v*radius,center+u*radius+v*radius,center-u*radius+v*radius))
    }
    fun angle(s:WorkspaceState,ids:List<String>):Angle {
        require(ids.size==2&&ids.distinct().size==2){"Select two different planes."}
        val planes=ids.map{id->evaluate(s,s.planes3D.singleOrNull{it.id==id} ?: throw IllegalArgumentException("A referenced plane is missing or ambiguous."))}
        val a=planes[0];val b=planes[1];val d=cross(a.normal,b.normal);val sine=norm(d);val parallel=sine<1e-12
        val theta=if(parallel)0.0 else atan2(sine,abs(a.normal.dot(b.normal)))
        val center:Vec3;val u:Vec3;val v:Vec3
        if(parallel){center=a.origin;u=a.tangent;v=u}else {
            center=(cross(b.normal,d)*a.normal.dot(a.origin)+cross(d,a.normal)*b.normal.dot(b.origin))*(1/d.dot(d))
            val line=d*(1/sine);u=unit(cross(line,a.normal));val other=unit(cross(line,b.normal));v=if(u.dot(other)<0)other*(-1.0)else other
        }
        require(listOf(center.x,center.y,center.z).all{it.isFinite()&&abs(it)<=10000}){"The planes intersect outside the supported range."}
        val tangent=if(parallel)unit(cross(a.normal,u))else unit(v-u*u.dot(v))
        val arc=(0..64).map{i->val t=theta*i/64;center+(u*cos(t)+tangent*sin(t))*.8}
        return Angle(center,center+u*1.2,center+v*1.2,arc,Math.toDegrees(theta),parallel)
    }
}
