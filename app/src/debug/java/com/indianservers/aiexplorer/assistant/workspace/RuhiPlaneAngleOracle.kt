package com.indianservers.aiexplorer.assistant.workspace

import com.indianservers.aiexplorer.workspace.*
import com.indianservers.aiexplorer.core.Vec3
import kotlin.math.*

/** Independent plane equations and normal dot product; no runtime plane-normal helper is reused. */
object RuhiPlaneAngleOracle {
    fun check(params:Map<String,Any?>,s:WorkspaceState,r:RuhiExecutionResult,c:RuhiSceneContext):RuhiSeedBehaviorOracle.Check {
        val errors=mutableListOf<String>();fun need(ok:Boolean,message:String){if(!ok)errors+=message}
        need(s.module==MathModule.Geometry3D&&r.semanticFrame?.engine==RuhiEngine.GEOMETRY_3D,"Wrong dihedral engine.")
        need(r.status==GraphCommandStatus.APPLIED,"Dihedral status ${r.status}: ${r.message}")
        need(r.semanticFrame?.intent=="MEASURE_ANGLE","Wrong dihedral intent.")
        params.forEach{(k,v)->need(v==r.semanticFrame?.parameters?.get(k),"Original dihedral slot $k differs.")}
        val planes=c.selectedPlaneIds.mapNotNull{id->s.planes3D.singleOrNull{it.id==id}}
        if(planes.size!=2)return RuhiSeedBehaviorOracle.Check(false,errors+"Oracle needs selected native planes.")
        val points=planes.map{plane->plane.pointIds.mapNotNull{id->s.points3D.singleOrNull{it.id==id}?.position}}
        if(points.any{it.size!=3})return RuhiSeedBehaviorOracle.Check(false,errors+"Oracle plane points missing.")
        fun normal(p:List<Vec3>):Vec3 {val a=p[1]-p[0];val b=p[2]-p[0];return Vec3(a.y*b.z-a.z*b.y,a.z*b.x-a.x*b.z,a.x*b.y-a.y*b.x)}
        fun norm(v:Vec3)=sqrt(v.x*v.x+v.y*v.y+v.z*v.z)
        val normals=points.map(::normal);val a=normals[0];val b=normals[1]
        val expected=Math.toDegrees(acos((abs(a.x*b.x+a.y*b.y+a.z*b.z)/(norm(a)*norm(b))).coerceIn(0.0,1.0)))
        val answer=Regex("= ([0-9.eE+-]+) degrees").find(r.message)?.groupValues?.get(1)?.toDoubleOrNull()
        need(answer!=null&&abs(answer-expected)<1e-8,"Wrong independently computed smaller dihedral angle.")
        need(RuhiSpatialPlaneLanguage.marks(r.state)==listOf(c.selectedPlaneIds),"Wrong stable plane-angle references.")
        val drawing=RuhiSpatialPlaneLanguage.drawings(r.state).singleOrNull();need(drawing!=null&&abs(drawing.degrees-expected)<1e-8,"Missing or wrong live dihedral arc.")
        if(drawing!=null) {
            points.indices.forEach{i->val d=drawing.center-points[i][0];val n=normals[i];need(abs(d.x*n.x+d.y*n.y+d.z*n.z)<1e-8,"Arc anchor is not on both planes.")}
            val first=drawing.arc.first()-points[0][0];val last=drawing.arc.last()-points[1][0]
            need(abs(first.dot(a))<1e-8&&abs(last.dot(b))<1e-8,"Dihedral arc endpoints do not lie in their planes.")
            need(drawing.arc.size==65&&drawing.arc.all{abs(norm(it-drawing.center)-.8)<1e-8},"Dihedral marker is not a circular 3D arc.")
        }
        need(s.points3D==r.state.points3D&&s.planes3D==r.state.planes3D&&s.vectors3D==r.state.vectors3D&&s.solids==r.state.solids&&s.points==r.state.points&&s.shapes==r.state.shapes&&s.functions==r.state.functions&&s.surfaceLayers==r.state.surfaceLayers,"Dihedral query changed existing geometry.")
        return RuhiSeedBehaviorOracle.Check(errors.isEmpty(),errors)
    }
}
