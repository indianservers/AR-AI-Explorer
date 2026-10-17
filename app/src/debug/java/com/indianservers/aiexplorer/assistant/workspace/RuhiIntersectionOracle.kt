package com.indianservers.aiexplorer.assistant.workspace

import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import kotlin.math.*

/** Original source expectations use independent closed-form answers, not the production root finder. */
object RuhiIntersectionOracle {
    private fun matches(a:Any?,b:Any?):Boolean=when{a is Number&&b is Number->abs(a.toDouble()-b.toDouble())<1e-9;a is List<*>&&b is List<*>->a.size==b.size&&a.zip(b).all{matches(it.first,it.second)};else->a==b}
    fun check(params:Map<String,Any?>,before:WorkspaceState,r:RuhiExecutionResult,c:RuhiSceneContext):RuhiSeedBehaviorOracle.Check {
        val errors=mutableListOf<String>();fun need(v:Boolean,msg:String){if(!v)errors+=msg}
        need(r.status==GraphCommandStatus.APPLIED,"Intersection status ${r.status}: ${r.message}")
        need(r.semanticFrame?.intent=="FIND_INTERSECTION"&&r.semanticFrame?.engine==RuhiEngine.GRAPH_2D,"Intersection semantic frame/engine missing.")
        params.forEach{(k,v)->need(matches(v,r.semanticFrame?.parameters?.get(k)),"Original intersection slot $k differs: ${r.semanticFrame?.parameters?.get(k)}")}
        need(before.functions==r.state.functions&&before.points==r.state.points&&before.shapes==r.state.shapes&&before.pointDependencies==r.state.pointDependencies&&before.points3D==r.state.points3D&&before.planes3D==r.state.planes3D&&before.vectors3D==r.state.vectors3D&&before.solids==r.state.solids&&before.surfaceLayers==r.state.surfaceLayers,"Intersection analysis changed source geometry/curves or another engine.")
        need(before.graph2DView==r.state.graph2DView,"Analysis reset the viewport.")
        runCatching {
            val drawing=RuhiIntersectionLanguage.drawing(r.state) ?: error("Native marker query missing.");need(drawing.diagnostic.isEmpty()&&!drawing.infinite,"Invalid/infinite marker record.")
            val base=listOf(Vec2(0.0,0.0),Vec2(1.0,1.0));val functions=params["functions"] as? List<*>
            val expected=when {
                functions==listOf("x","x^2")->base
                functions==listOf("2*x","4")->listOf(Vec2(2.0,4.0))
                functions==listOf("sin(x)","cos(x)")->{val radius=7.0/before.graph2DView.zoom;val low=before.graph2DView.centerX-radius;val high=before.graph2DView.centerX+radius;val first=ceil((low-PI/4)/PI).toInt();val last=floor((high-PI/4)/PI).toInt();(first..last).map{k->val x=PI/4+k*PI;Vec2(x,sin(x))}}
                functions==listOf("x^2","4")->listOf(Vec2(-2.0,4.0),Vec2(2.0,4.0))
                functions==listOf("3*x+2","11")->listOf(Vec2(3.0,11.0))
                functions==listOf("x+1","-x+5")->listOf(Vec2(2.0,3.0))
                functions==listOf("x^3","x")->listOf(Vec2(-1.0,-1.0),Vec2(0.0,0.0),Vec2(1.0,1.0))
                functions==listOf("sqrt(x)","x-2")->listOf(Vec2(4.0,2.0))
                params["expression"]=="x^2-9"->listOf(Vec2(-3.0,0.0),Vec2(3.0,0.0))
                params["equations"]==listOf("x=2","y=x^2")->listOf(Vec2(2.0,4.0))
                params["target"]=="selected_graph"->listOf(Vec2(0.0,0.0))
                params["targets"]=="circles_centered_A_B"->listOf(Vec2(0.0,-sqrt(3.0)),Vec2(0.0,sqrt(3.0)))
                params["targets"]==listOf("AB","C1")->listOf(Vec2(-2.0,0.0),Vec2(2.0,0.0))
                params["equation"]=="sin(x)=0.5"->listOf(Vec2(PI/6,.5),Vec2(5*PI/6,.5))
                params["domain"]=="x>0"->listOf(Vec2(1.0,1.0))
                params["target"] in setOf("two_visible_graphs","all_current_graphs","selected_curves","visible_curves")||params["targets"]==listOf("red_line","blue_curve")->base
                else->error("Independent intersection answer missing for original parameters.")
            }
            val view=before.graph2DView;val aspect=before.labSessionValues[RuhiIntersectionLanguage.ASPECT]?.toDoubleOrNull() ?: 2.0;val half=7.0/view.zoom
            val visible=expected.filter{it.x>=view.centerX-half-1e-8&&it.x<=view.centerX+half+1e-8&&it.y>=view.centerY-half*aspect-1e-8&&it.y<=view.centerY+half*aspect+1e-8}
            need(drawing.points.size==visible.size,"Expected ${visible.size} crossings, got ${drawing.points}.")
            need(visible.all{wanted->drawing.points.any{it.distanceTo(wanted)<1e-5}},"Missing/wrong exact intersection coordinates: ${drawing.points}.")
            need(drawing.points.all{actual->visible.any{it.distanceTo(actual)<1e-5}},"Extraneous or out-of-viewport intersection.")
            need(drawing.labels==drawing.points.indices.map{"I${it+1}"},"Intersection labels/order differ.")
            if(params["select"]=="closest_origin")need(drawing.highlighted==setOf(0),"Closest-origin marker not highlighted.")
            if(params["select"]=="near_x=1")need(drawing.highlighted==setOf(1),"Nearest-x marker not highlighted.")
            if(params["precision"]==4)need(r.message.contains("(0.0000, 0.0000)")&&r.message.contains("(1.0000, 1.0000)"),"Four-decimal coordinates missing.")
            if(params["count_only"]==true)need(r.message.contains("Intersection count: 2"),"Intersection count differs.")
            if(params["existence_only"]==true)need(r.message.startsWith("Yes."),"Existence query differs.")
            if(params["targets"]=="circles_centered_A_B"||params["targets"]==listOf("AB","C1"))need(drawing.helpers.size==2,"Native referenced geometry is not displayed in the graph canvas.")
        }.onFailure{errors+="Intersection oracle error: ${it.message}"}
        return RuhiSeedBehaviorOracle.Check(errors.isEmpty(),errors)
    }
}
