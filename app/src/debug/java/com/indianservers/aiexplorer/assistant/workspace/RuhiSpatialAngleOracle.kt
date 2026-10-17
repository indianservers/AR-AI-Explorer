package com.indianservers.aiexplorer.assistant.workspace

import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import kotlin.math.*

object RuhiSpatialAngleOracle {
    fun check(params:Map<String,Any?>,before:WorkspaceState,r:RuhiExecutionResult,c:RuhiSceneContext):RuhiSeedBehaviorOracle.Check {
        val errors=mutableListOf<String>();fun need(ok:Boolean,text:String){if(!ok)errors+=text}
        need(before.module==MathModule.Geometry3D&&r.semanticFrame?.engine==RuhiEngine.GEOMETRY_3D,"Wrong 3D angle engine.")
        need(r.status==GraphCommandStatus.APPLIED,"3D angle status ${r.status}: ${r.message}")
        need(r.semanticFrame?.intent=="MEASURE_ANGLE","Missing 3D angle intent.")
        params.forEach{(k,v)->need(r.semanticFrame?.parameters?.get(k)==v,"3D angle source slot $k differs.")}
        val ids=c.selectedVectorIds;val vectors=ids.mapNotNull{id->before.vectors3D.singleOrNull{it.id==id}}
        if(ids.size!=2||vectors.size!=2)return RuhiSeedBehaviorOracle.Check(false,errors+"Independent oracle lacks selected 3D vector pair.")
        val u=vectors[0].components;val v=vectors[1].components
        fun norm(p:Vec3)=sqrt(p.x*p.x+p.y*p.y+p.z*p.z)
        val expected=Math.toDegrees(acos(((u.x*v.x+u.y*v.y+u.z*v.z)/(norm(u)*norm(v))).coerceIn(-1.0,1.0)))
        val answer=Regex("= ([0-9.eE+-]+) degrees").find(r.message)?.groupValues?.get(1)?.toDoubleOrNull()
        need(answer!=null&&abs(answer-expected)<1e-8,"Wrong independently computed 3D dot-product angle.")
        need(RuhiSpatialAngleLanguage.marks(r.state)==listOf(ids),"3D angle references the wrong stable vector IDs.")
        val drawing=RuhiSpatialAngleLanguage.drawings(r.state).singleOrNull()
        need(drawing!=null&&abs(drawing.degrees-expected)<1e-8,"Missing/wrong rendered 3D angle.")
        if(drawing!=null) {
            need(drawing.center==vectors[0].start,"Wrong 3D arc origin.")
            val radius=minOf(norm(u),norm(v),3.0)*.35
            need(drawing.arc.size==65&&drawing.arc.all{abs(norm(it-drawing.center)-radius)<1e-8},"3D arc is not a constant-radius angle marker.")
            val first=drawing.arc.first()-drawing.center;val last=drawing.arc.last()-drawing.center
            need(norm(first-u*(radius/norm(u)))<1e-8&&norm(last-v*(radius/norm(v)))<1e-8,"3D arc endpoints do not follow both vector directions.")
        }
        need(r.state.points==before.points&&r.state.shapes==before.shapes&&r.state.points3D==before.points3D&&r.state.vectors3D==before.vectors3D&&r.state.solids==before.solids&&r.state.functions==before.functions&&r.state.surfaceLayers==before.surfaceLayers,"3D angle annotation changed existing geometry.")
        return RuhiSeedBehaviorOracle.Check(errors.isEmpty(),errors)
    }
}
