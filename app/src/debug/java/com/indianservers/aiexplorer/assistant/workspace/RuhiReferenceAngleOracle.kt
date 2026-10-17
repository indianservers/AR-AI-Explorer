package com.indianservers.aiexplorer.assistant.workspace

import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import kotlin.math.*

/** Independent raw-coordinate dot-product/intersection oracle for workbook reference angles. */
object RuhiReferenceAngleOracle {
    fun check(params:Map<String,Any?>,s:WorkspaceState,r:RuhiExecutionResult):RuhiSeedBehaviorOracle.Check {
        val errors=mutableListOf<String>();fun need(ok:Boolean,text:String){if(!ok)errors+=text}
        need(r.status==GraphCommandStatus.APPLIED,"Reference angle status ${r.status}: ${r.message}")
        need(r.semanticFrame?.intent=="MEASURE_ANGLE","Reference angle intent missing.")
        params.forEach{(k,v)->need(v==r.semanticFrame?.parameters?.get(k),"Source reference-angle slot $k differs.")}
        val registry=s.labSessionValues.filterKeys{it.startsWith(StudioReferenceMetadata.POINT_PREFIX)}.mapKeys{it.key.removePrefix(StudioReferenceMetadata.POINT_PREFIX)}.mapValues{it.value.toIntOrNull()}
        fun pair(name:String):List<Int> {
            s.shapes.singleOrNull{it.name==name&&it.pointIndices.size==2}?.let{return it.pointIndices}
            val options=registry.keys.flatMap{a->registry.keys.mapNotNull{b->if(a+b==name)listOfNotNull(registry[a],registry[b]).takeIf{it.size==2}else null}}.distinct()
            return options.singleOrNull().orEmpty()
        }
        val kind:String;val ids:List<Int>
        when {
            params["lines"] is List<*>->{kind="lines";ids=(params["lines"] as List<*>).flatMap{pair(it.toString())}}
            params["vectors"] is List<*>->{kind="vectors";ids=(params["vectors"] as List<*>).flatMap{pair(it.toString())}}
            params["reference"]!=null->{kind=if(params["reference"]=="x_axis")"axis_x"else "axis_y";ids=pair(params["line"].toString())}
            params["target"]=="last_two_segments"->{kind="segments";ids=s.shapes.filter{it.type==Shape2DType.Segment}.takeLast(2).flatMap{it.pointIndices}}
            else->{kind="origin";ids=(params["points"] as? List<*>)?.mapNotNull{registry[it.toString()]}.orEmpty()}
        }
        val count=if(kind in setOf("origin","axis_x","axis_y"))2 else 4
        if(ids.size!=count||ids.any{it !in s.points.indices})return RuhiSeedBehaviorOracle.Check(false,errors+"Reference-angle oracle lacks endpoints.")
        val p=ids.map{s.points[it]};var center:Vec2;var u:Vec2;var v:Vec2;var acute=false
        when(kind) {
            "origin"->{center=Vec2(0.0,0.0);u=p[0];v=p[1]}
            "axis_x","axis_y"->{center=p[0];u=if(kind=="axis_x")Vec2(1.0,0.0)else Vec2(0.0,1.0);v=p[1]-p[0]}
            "vectors"->{center=p[0];u=p[1]-p[0];v=p[3]-p[2]}
            else->{
                val common=if(kind=="segments")ids.take(2).toSet().intersect(ids.drop(2).toSet()).singleOrNull()else null
                if(common!=null){center=s.points[common];u=s.points[ids.take(2).first{it!=common}]-center;v=s.points[ids.drop(2).first{it!=common}]-center}
                else {u=p[1]-p[0];v=p[3]-p[2];val delta=p[2]-p[0];val cross=u.x*v.y-u.y*v.x;if(abs(cross)<1e-12)return RuhiSeedBehaviorOracle.Check(false,errors+"Fixture lines do not intersect uniquely.");center=p[0]+u*((delta.x*v.y-delta.y*v.x)/cross);acute=true}
            }
        }
        val cosine=(u.x*v.x+u.y*v.y)/(hypot(u.x,u.y)*hypot(v.x,v.y));val expected=Math.toDegrees(acos((if(acute)abs(cosine)else cosine).coerceIn(-1.0,1.0)))
        val answer=Regex("= ([0-9.eE+-]+) degrees").find(r.message)?.groupValues?.get(1)?.toDoubleOrNull()
        need(answer!=null&&abs(answer-expected)<1e-7,"Wrong independent reference-angle answer.")
        val mark=RuhiReferenceAngleLanguage.marks(r.state).singleOrNull()
        need(mark?.kind==kind&&mark.indices==ids,"Wrong live reference-angle endpoints or mode.")
        if(mark!=null) {
            val rendered=runCatching{RuhiReferenceAngleLanguage.measure(r.state,mark)}.getOrNull()
            need(rendered!=null&&abs(rendered.degrees-expected)<1e-7,"Renderer angle differs from independent calculation.")
            need(rendered!=null&&hypot(rendered.arms[1].x-center.x,rendered.arms[1].y-center.y)<1e-8,"Wrong live reference-angle anchor.")
        }
        need(s.points==r.state.points&&s.shapes==r.state.shapes&&s.points3D==r.state.points3D&&s.solids==r.state.solids&&s.vectors3D==r.state.vectors3D&&s.functions==r.state.functions&&s.surfaceLayers==r.state.surfaceLayers,"Reference-angle annotation changed existing geometry.")
        return RuhiSeedBehaviorOracle.Check(errors.isEmpty(),errors)
    }
}
