package com.indianservers.aiexplorer.assistant.workspace

import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import kotlin.math.*

/** Circle acceptance uses the original slots and independent center/radius/side-distance calculations. */
object RuhiCircleOracle {
    private data class Value(val center:Vec2,val radius:Double)
    private fun circum(p:List<Vec2>):Value {
        val a=p[0];val b=p[1];val c=p[2];val u=b-a;val v=c-a;val det=2*(u.x*v.y-u.y*v.x)
        require(abs(det)>1e-12);val du=u.x*u.x+u.y*u.y;val dv=v.x*v.x+v.y*v.y
        val center=a+Vec2((du*v.y-dv*u.y)/det,(u.x*dv-v.x*du)/det)
        return Value(center,hypot(center.x-a.x,center.y-a.y))
    }
    private fun value(s:WorkspaceState,shape:Shape2D):Value {
        val p=shape.pointIndices.map{s.points[it]};return if(shape.type==Shape2DType.CircleThreePoints)circum(p)else Value(p[0],hypot(p[1].x-p[0].x,p[1].y-p[0].y))
    }
    private fun matches(a:Any?,b:Any?):Boolean=when{a is Number&&b is Number->abs(a.toDouble()-b.toDouble())<1e-9;a is List<*>&&b is List<*>->a.size==b.size&&a.zip(b).all{matches(it.first,it.second)};else->a==b}
    fun check(params:Map<String,Any?>,before:WorkspaceState,r:RuhiExecutionResult,c:RuhiSceneContext):RuhiSeedBehaviorOracle.Check {
        val errors=mutableListOf<String>();fun need(ok:Boolean,msg:String){if(!ok)errors+=msg}
        need(r.status==GraphCommandStatus.APPLIED,"Circle status ${r.status}: ${r.message}")
        need(r.semanticFrame?.intent=="CREATE_CIRCLE"&&r.semanticFrame?.engine==RuhiEngine.GEOMETRY_2D,"Missing circle semantic frame/engine.")
        params.forEach{(k,v)->need(matches(v,r.semanticFrame?.parameters?.get(k)),"Original circle slot $k differs: ${r.semanticFrame?.parameters?.get(k)}")}
        val old=before.shapes.filter{it.type in setOf(Shape2DType.Circle,Shape2DType.CircleThreePoints)}
        val selected=old.singleOrNull{it.id==c.selectedShapeId}
        val label=params["label"] as? String;val existing=label?.let{n->old.singleOrNull{it.name==n}}
        val modified=if(params["action"]=="update")selected else existing
        val outputs=if(modified!=null)r.state.shapes.filter{it.id==modified.id}else r.state.shapes.filter{shape->before.shapes.none{it.id==shape.id}}
        val expectedCount=(params["count"] as? Number)?.toInt() ?: 1
        need(outputs.size==expectedCount&&outputs.all{it.type in setOf(Shape2DType.Circle,Shape2DType.CircleThreePoints)},"Wrong circle count/type.")
        need(r.state.shapes.size==before.shapes.size+if(modified==null)expectedCount else 0,"Unexpected number of shape changes.")
        fun point(name:String):Vec2 {val i=before.labSessionValues["ruhi.studio.point.$name"]?.toIntOrNull() ?: throw IllegalArgumentException("Oracle point $name missing.");return before.points[i]}
        fun coords(v:Any?)=(v as? List<*>)?.takeIf{it.size==2&&it.all{it is Number}}?.let{Vec2((it[0] as Number).toDouble(),(it[1] as Number).toDouble())}
        fun tri():List<Vec2> {val name=params["triangle"] as? String;val shape=if(name!=null)before.shapes.singleOrNull{it.name==name&&it.type==Shape2DType.Triangle}else before.shapes.singleOrNull{it.id==c.selectedShapeId&&it.type==Shape2DType.Triangle};return (shape ?: throw IllegalArgumentException("Oracle triangle missing.")).pointIndices.map{before.points[it]}}
        val expected=runCatching {
            val through=params["through"];val diameter=params["diameter"]
            when {
                expectedCount==2->listOf(Value(Vec2(0.0,0.0),1.0),Value(Vec2(if(params["relation"]=="externally_tangent")2.0 else 1.0,0.0),1.0))
                params["relation"]=="inside"->{val ref=old.maxByOrNull{value(before,it).radius} ?: error("Largest circle missing");val d=value(before,ref);listOf(Value(d.center,d.radius*.5))}
                params["relation"]=="concentric"->{val d=value(before,selected ?: error("Selected circle missing"));listOf(Value(d.center,d.radius*2))}
                params["radius_ref"]=="first_circle"->listOf(Value(Vec2(0.0,0.0),value(before,old.first()).radius))
                diameter is List<*>->{val a=point(diameter[0].toString());val b=point(diameter[1].toString());listOf(Value((a+b)*.5,hypot(b.x-a.x,b.y-a.y)*.5))}
                through is List<*>->listOf(circum(through.map{point(it.toString())}))
                params["kind"] in setOf("incircle","circumcircle")->{val p=tri();if(params["kind"]=="circumcircle")listOf(circum(p))else {
                    val a=hypot(p[1].x-p[2].x,p[1].y-p[2].y);val b=hypot(p[0].x-p[2].x,p[0].y-p[2].y);val d=hypot(p[0].x-p[1].x,p[0].y-p[1].y);val total=a+b+d
                    val center=(p[0]*a+p[1]*b+p[2]*d)*(1/total);val twiceArea=abs((p[1].x-p[0].x)*(p[2].y-p[0].y)-(p[1].y-p[0].y)*(p[2].x-p[0].x));listOf(Value(center,twiceArea/total))
                }}
                else->{val center=if(params["action"]=="update")value(before,selected ?: error("Selected circle missing")).center else coords(params["center"]) ?: (params["center"] as? String)?.let(::point) ?: Vec2(0.0,0.0)
                    val radius=when{params["radius"] is Number->(params["radius"] as Number).toDouble();diameter is Number->diameter.toDouble()/2;through is String->point(through).let{hypot(it.x-center.x,it.y-center.y)};params["tangent"]=="x_axis"->abs(center.y);params["equation"]!=null->sqrt(params["equation"].toString().substringAfter('=').toDouble());existing!=null->value(before,existing).radius;else->1.0}
                    listOf(Value(center,radius))
                }
            }
        }.getOrElse{need(false,"Independent circle oracle lacks context: ${it.message}");emptyList()}
        outputs.forEachIndexed{i,shape->
            val actual=runCatching{value(r.state,shape)}.getOrNull();val e=expected.getOrNull(i)
            need(actual!=null&&e!=null&&hypot(actual.center.x-e.center.x,actual.center.y-e.center.y)<1e-8&&abs(actual.radius-e.radius)<1e-8,"Wrong independent circle center/radius.")
            if(params["color"]=="red")need(shape.styleKey.equals("#FF0000",true),"Circle color was not applied to its native style.")
            if(label!=null)need(shape.name==label,"Circle label was lost.")
            if(params["kind"]=="incircle"&&actual!=null) {
                val p=tri();p.indices.forEach{j->val a=p[j];val b=p[(j+1)%3];val distance=abs((b.x-a.x)*(actual.center.y-a.y)-(b.y-a.y)*(actual.center.x-a.x))/hypot(b.x-a.x,b.y-a.y);need(abs(distance-actual.radius)<1e-8,"Incircle is not tangent to every triangle side.")}
                need(r.state.pointDependencies.any{it.type==PointDependencyType.Incenter&&it.outputIndex==shape.pointIndices[0]},"Incircle center is not linked to triangle parents.")
            }
            if(params["diameter"] is List<*>)need(r.state.pointDependencies.any{it.type==PointDependencyType.Midpoint&&it.outputIndex==shape.pointIndices[0]},"Diameter midpoint dependency missing.")
        }
        before.shapes.filter{it.id!=modified?.id}.forEach{shape->need(r.state.shapes.singleOrNull{it.id==shape.id}==shape,"Circle command changed an unrelated shape.")}
        need(r.state.points.take(before.points.size)==before.points,"Circle command moved existing parent/control points.")
        need(r.state.solids==before.solids&&r.state.points3D==before.points3D&&r.state.planes3D==before.planes3D&&r.state.vectors3D==before.vectors3D&&r.state.functions==before.functions&&r.state.surfaceLayers==before.surfaceLayers,"Circle command changed another engine.")
        return RuhiSeedBehaviorOracle.Check(errors.isEmpty(),errors)
    }
}
