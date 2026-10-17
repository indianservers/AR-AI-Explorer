package com.indianservers.aiexplorer.assistant.workspace

import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import kotlin.math.*

/** Independent acceptance derives distances/angles from coordinates, never parser-generated measurements. */
object RuhiTriangleOracle {
    private fun matches(a:Any?,b:Any?):Boolean=when{a is Number&&b is Number->abs(a.toDouble()-b.toDouble())<1e-8;a is List<*>&&b is List<*>->a.size==b.size&&a.zip(b).all{matches(it.first,it.second)};else->a==b}
    fun check(params:Map<String,Any?>,before:WorkspaceState,r:RuhiExecutionResult,c:RuhiSceneContext):RuhiSeedBehaviorOracle.Check {
        val errors=mutableListOf<String>();fun need(v:Boolean,msg:String){if(!v)errors+=msg};fun near(a:Double,b:Double)=abs(a-b)<1e-7*max(1.0,abs(b))
        need(r.status==GraphCommandStatus.APPLIED,"Triangle status ${r.status}: ${r.message}")
        need(r.semanticFrame?.intent=="CREATE_TRIANGLE"&&r.semanticFrame?.engine==RuhiEngine.GEOMETRY_2D,"Triangle frame/engine missing.")
        params.forEach{(k,v)->need(matches(v,r.semanticFrame?.parameters?.get(k)),"Original triangle slot $k differs: ${r.semanticFrame?.parameters?.get(k)}")}
        val modified=if(params["action"]=="modify")before.shapes.singleOrNull{it.id==c.selectedShapeId} else null
        val outputs=r.state.shapes.filter{shape->if(modified!=null)shape.id==modified.id else before.shapes.none{it.id==shape.id}}
        need(outputs.size==1&&outputs.firstOrNull()?.type==Shape2DType.Triangle,"Exactly one native triangle must be constructed/modified.")
        need(r.state.shapes.size==before.shapes.size+if(modified==null)1 else 0,"Unexpected shape count.")
        before.shapes.filter{it.id!=modified?.id}.forEach{old->need(r.state.shapes.singleOrNull{it.id==old.id}==old,"Unrelated shape changed.")}
        need(before.points==r.state.points.take(before.points.size),"Original parent coordinates changed.")
        need(before.pointDependencies.all{it in r.state.pointDependencies},"Original dependency removed.")
        need(before.points3D==r.state.points3D&&before.planes3D==r.state.planes3D&&before.vectors3D==r.state.vectors3D&&before.solids==r.state.solids&&before.functions==r.state.functions&&before.surfaceLayers==r.state.surfaceLayers,"Another engine changed.")
        runCatching {
            val shape=outputs.single();require(shape.pointIndices.size==3&&shape.pointIndices.distinct().size==3)
            val p=shape.pointIndices.map{r.state.points[it]};val side=p.indices.map{val d=p[(it+1)%3]-p[it];hypot(d.x,d.y)}
            val area2=abs((p[1].x-p[0].x)*(p[2].y-p[0].y)-(p[1].y-p[0].y)*(p[2].x-p[0].x));need(area2>1e-9,"Degenerate triangle.")
            val angle=p.indices.map{i->val a=side[i];val b=side[(i+2)%3];val opposite=side[(i+1)%3];Math.toDegrees(acos(((a*a+b*b-opposite*opposite)/(2*a*b)).coerceIn(-1.0,1.0)))}
            need(near(angle.sum(),180.0),"Interior angles do not total 180.")
            val expectedLengths=Regex("lengths ([^;]+); angles").find(r.message)?.groupValues?.get(1)?.split(',')?.map{it.trim().toDouble()}
            val expectedAngles=Regex("angles (.+?) degrees").find(r.message)?.groupValues?.get(1)?.split(',')?.map{it.trim().toDouble()}
            need(expectedLengths?.zip(side)?.all{near(it.first,it.second)}==true&&expectedLengths.size==3,"Computed side lengths missing/wrong.")
            need(expectedAngles?.zip(angle)?.all{near(it.first,it.second)}==true&&expectedAngles.size==3,"Computed angles missing/wrong.")
            val refs=RuhiEndpointResolver.references(r.state);need(shape.pointIndices.all{i->refs.any{it.second==i}},"Vertex labels missing.")
            fun point(name:String):Vec2 {val i=before.labSessionValues["ruhi.studio.point.$name"]!!.toInt();return before.points[i]}
            fun value(key:String)=(params[key] as? Number)?.toDouble()
            fun numbers(key:String)=(params[key] as? List<*>)?.map{(it as Number).toDouble()}
            when(params["kind"]) {
                "equilateral"->need(side.all{near(it,side[0])},"Not equilateral.")
                "isosceles"->need(near(side[0],side[1])||near(side[0],side[2])||near(side[1],side[2]),"Not isosceles.")
                "right"->need(angle.any{near(it,90.0)},"No right angle.")
                "acute"->need(angle.all{it<90-1e-7},"Not acute.")
                "obtuse"->need(angle.any{it>90+1e-7},"Not obtuse.")
                "scalene"->need(side.indices.all{i->side.indices.all{j->i==j||!near(side[i],side[j])}},"Not scalene.")
            }
            value("side")?.let{v->need(side.all{near(it,v)},"Equilateral side length differs.")}
            value("equal_sides")?.let{v->need(side.count{near(it,v)}>=2,"Equal side lengths differ.")}
            numbers("sides")?.let{v->need(side.sorted().zip(v.sorted()).all{near(it.first,it.second)},"SSS constraints differ.")}
            numbers("legs")?.let{v->need(near(side[0],v[0])&&near(side[2],v[1]),"Right legs differ.")}
            value("hypotenuse")?.let{v->need(near(side.max(),v),"Hypotenuse differs.")}
            value("leg")?.let{v->need(side.any{near(it,v)},"Right leg differs.")}
            value("base")?.let{v->need(near(side[0],v),"Base differs.")}
            value("height")?.let{v->need(near(area2/side[0],v),"Perpendicular height differs.")}
            numbers("center")?.let{v->need(near(p.sumOf{it.x}/3,v[0])&&near(p.sumOf{it.y}/3,v[1]),"Centroid is not requested center.")}
            value("rotation_degrees")?.let{v->val d=p[1]-p[0];need(near(atan2(d.y,d.x),Math.toRadians(v)),"Baseline rotation differs.")}
            (params["vertices"] as? List<*>)?.let{v->need(p==v.map{point(it.toString())},"Named parent vertices differ.");need(shape.pointIndices==v.map{before.labSessionValues["ruhi.studio.point.$it"]!!.toInt()},"Named parent indices not reused.")}
            (params["coords"] as? List<*>)?.let{v->val q=v.map{a->val n=a as List<*>;Vec2((n[0] as Number).toDouble(),(n[1] as Number).toDouble())};need(p==q,"Explicit coordinates differ.")}
            (params["labels"] as? List<*>)?.let{v->need(shape.pointIndices.indices.all{i->r.state.labSessionValues["ruhi.studio.point.${v[i]}"]==shape.pointIndices[i].toString()},"Requested vertex labels differ.")}
            params.filterKeys{it.startsWith("angle_at_")}.forEach{(key,v)->val name=key.removePrefix("angle_at_");val q=point(name);val i=p.indexOf(q);need(i>=0&&near(angle.getOrElse(i){-1.0},(v as Number).toDouble()),"Named vertex angle differs.")}
            if(params["fixed_side"]!=null){val name=params["fixed_side"].toString();val ref=before.shapes.single{it.name==name&&it.pointIndices.size==2};need(shape.pointIndices.take(2)==ref.pointIndices&&near(side[0],value("length")!!),"Fixed side differs.")}
            if(params["source"]!=null){val name=params["source"];val ref=if(name=="first_triangle")before.shapes.first{it.type==Shape2DType.Triangle}else before.shapes.single{it.name==name};val q=ref.pointIndices.map{before.points[it]};val factor=value("scale") ?: 1.0;need(side.indices.all{i->near(side[i],q[i].distanceTo(q[(i+1)%3])*factor)},"Congruent/similar lengths differ.");need(shape.pointIndices.all{i->r.state.pointDependencies.any{it.outputIndex==i&&it.inputIndices.singleOrNull() in ref.pointIndices}},"Copy lacks live source dependency.")}
            if(params["placement"]=="above_x")need(p.all{it.y>0},"Triangle is not above X-axis.")
            if(params["demo"]=="pythagorean")need(near(side.sorted()[0].pow(2)+side.sorted()[1].pow(2),side.max().pow(2)),"Pythagoras not satisfied.")
            if(params.isEmpty())need(p==listOf(Vec2(0.0,0.0),Vec2(2.0,0.0),Vec2(1.0,sqrt(3.0))),"Default triangle changed without explicit defaults.")
        }.onFailure{errors+="Triangle oracle error: ${it.message}"}
        return RuhiSeedBehaviorOracle.Check(errors.isEmpty(),errors)
    }
}
