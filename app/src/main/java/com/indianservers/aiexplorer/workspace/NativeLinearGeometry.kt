package com.indianservers.aiexplorer.workspace

import com.indianservers.aiexplorer.core.Vec3
import kotlin.math.*

enum class Linear3DKind { Line, Segment, Ray }

/** Ordered stable point references; clipping never changes the mathematical domain. */
data class Linear3DObject(
    val id:String, val name:String, val kind:Linear3DKind, val pointIds:List<String>,
    val visible:Boolean=true, val locked:Boolean=false, val styleKey:String="cyan",
) {
    init { require(id.isNotBlank() && name.isNotBlank() && pointIds.size==2 && pointIds.distinct().size==2) }
}

object NativeLinearGeometry {
    data class Linear(val id:String,val name:String,val kind:Linear3DKind,val origin:Vec3,val direction:Vec3) {
        fun containsParameter(t:Double)=t.isFinite() && when(kind) {
            Linear3DKind.Line -> true
            Linear3DKind.Ray -> t>=0.0
            Linear3DKind.Segment -> t in 0.0..1.0
        }
        fun at(t:Double):Vec3 {require(containsParameter(t));return origin+direction*t}
    }
    data class Display(val linear:Linear,val start:Vec3,val end:Vec3,val arrowStart:Boolean,val arrowEnd:Boolean)
    fun evaluate(s:WorkspaceState,obj:Linear3DObject):Linear {
        val points=obj.pointIds.map{id->s.points3D.singleOrNull{it.id==id}?.position
            ?: throw IllegalArgumentException("A defining point is missing or ambiguous.")}
        require(points.all{p->listOf(p.x,p.y,p.z).all{it.isFinite() && abs(it)<=10000}}){"Use finite coordinates within 10000 units."}
        val d=points[1]-points[0]
        require(hypot(hypot(d.x,d.y),d.z)>1e-12){"Two distinct point positions are required."}
        return Linear(obj.id,obj.name,obj.kind,points[0],d)
    }
    /** Slab clipping against a display box, applied after choosing the exact line/ray/segment domain. */
    fun clip(linear:Linear,radius:Double):Display? {
        require(radius.isFinite() && radius>0)
        var lo=if(linear.kind==Linear3DKind.Line)Double.NEGATIVE_INFINITY else 0.0
        var hi=if(linear.kind==Linear3DKind.Segment)1.0 else Double.POSITIVE_INFINITY
        val p=listOf(linear.origin.x,linear.origin.y,linear.origin.z)
        val d=listOf(linear.direction.x,linear.direction.y,linear.direction.z)
        for(i in 0..2) {
            if(d[i]==0.0){if(abs(p[i])>radius)return null;continue}
            val a=(-radius-p[i])/d[i];val b=(radius-p[i])/d[i]
            lo=max(lo,min(a,b));hi=min(hi,max(a,b))
            if(lo>hi)return null
        }
        if(!lo.isFinite() || !hi.isFinite() || hi-lo<=1e-12)return null
        return Display(linear,linear.at(lo),linear.at(hi),linear.kind==Linear3DKind.Line,linear.kind!=Linear3DKind.Segment)
    }
    fun drawings(s:WorkspaceState,radius:Double)=s.linears3D.filter{it.visible}.mapNotNull {
        runCatching{clip(evaluate(s,it),radius)}.getOrNull()
    }
}
