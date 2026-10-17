package com.indianservers.aiexplorer.workspace

import com.indianservers.aiexplorer.core.*
import kotlin.math.*

/** Selection labels describe one native object, or the displayed control box for a group. */
object GeometrySelectionMeasurements {
    data class Quantity(val label:String,val value:Double?,val power:Int,val exactFallback:String?=null)
    data class Result(val quantities:List<Quantity>,val note:String="")
    fun measure(points:List<Vec2>,shapes:List<Shape2D>,selected:Set<Int>):Result {
        val chosen=selected.mapNotNull(shapes::getOrNull)
        if(chosen.any{shape->shape.pointIndices.any{it !in points.indices}})return Result(emptyList(),"Missing control point")
        val controls=chosen.flatMap{it.pointIndices}.distinct().mapNotNull(points::getOrNull)
        if(controls.isEmpty())return Result(emptyList(),"No selection")
        if(controls.any{!it.x.isFinite()||!it.y.isFinite()})return Result(emptyList(),"Undefined controls")
        fun box()=Result(listOf(Quantity("box width",controls.maxOf{it.x}-controls.minOf{it.x},1),Quantity("box height",controls.maxOf{it.y}-controls.minOf{it.y},1)))
        val shape=chosen.singleOrNull() ?: return box()
        if(shape.pointIndices.any{it !in points.indices})return Result(emptyList(),"Missing control point")
        val p=shape.pointIndices.map{points[it]}
        if(shape.type in setOf(Shape2DType.Circle,Shape2DType.CircleThreePoints)) {
            val center=if(shape.type==Shape2DType.Circle&&p.size==2)p[0]else if(shape.type==Shape2DType.CircleThreePoints&&p.size==3)Geometry2D.circumcenter(p[0],p[1],p[2])else null
            if(center==null)return Result(emptyList(),"Undefined circle")
            val rim=if(shape.type==Shape2DType.Circle)p[1]else p[0]
            val radius=hypot(rim.x-center.x,rim.y-center.y)
            if(!radius.isFinite()||radius<=0)return Result(emptyList(),"Undefined circle")
            return Result(listOf(Quantity("radius",radius,1),Quantity("circumference",2*PI*radius,1),Quantity("area",PI*radius*radius,2)))
        }
        if(shape.type in setOf(Shape2DType.Triangle,Shape2DType.Polygon,Shape2DType.Rectangle,Shape2DType.Square,Shape2DType.RegularPolygon)) {
            if(p.size<3)return Result(emptyList(),"Undefined polygon")
            return Result(listOf(Quantity("area",Geometry2D.polygonArea(p),2),Quantity("perimeter",p.indices.sumOf{i->p[i].distanceTo(p[(i+1)%p.size])},1)))
        }
        if(shape.type in setOf(Shape2DType.Segment,Shape2DType.Vector,Shape2DType.Line,Shape2DType.Ray)&&p.size==2) {
            val dx=p[1].x-p[0].x;val dy=p[1].y-p[0].y
            val slope=if(dx==0.0)Quantity("slope",null,0)else (dy/dx).let{if(it.isFinite())Quantity("slope",it,0)else Quantity("slope",null,0,"$dy / $dx")}
            val bounded=shape.type in setOf(Shape2DType.Segment,Shape2DType.Vector)
            val quantities=mutableListOf(Quantity(if(bounded)"Δx"else"direction x",dx,1),Quantity(if(bounded)"Δy"else"direction y",dy,1))
            if(bounded)quantities+=Quantity(if(shape.type==Shape2DType.Vector)"magnitude"else"length",hypot(dx,dy),1)
            quantities+=slope
            return Result(quantities)
        }
        return box()
    }
    fun label(result:Result,format:(Double)->String):String=label(result,GeometryMeasurementUnits.Context(),format)
    fun label(result:Result,units:GeometryMeasurementUnits.Context,format:(Double)->String):String=if(result.quantities.isEmpty())result.note else result.quantities.joinToString(" - "){q->
        val value=q.value?.let{units.number(it,q.power,format)} ?: q.exactFallback ?: "undefined"
        "${q.label} $value"+if(units.known&&q.power>0)" ${units.suffix(q.power)}"else""
    }
}
