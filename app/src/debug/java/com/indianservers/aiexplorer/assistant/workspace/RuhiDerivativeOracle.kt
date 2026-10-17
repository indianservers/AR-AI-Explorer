package com.indianservers.aiexplorer.assistant.workspace

import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import kotlin.math.*

/** Independent closed-form identities from the unchanged workbook, not the derivative implementation. */
object RuhiDerivativeOracle {
    fun check(p:Map<String,Any?>,before:WorkspaceState,r:RuhiExecutionResult,c:RuhiSceneContext):RuhiSeedBehaviorOracle.Check {
        val errors=mutableListOf<String>();fun need(ok:Boolean,why:String){if(!ok)errors+=why}
        fun same(a:Any?,b:Any?):Boolean=when {a is Number&&b is Number->abs(a.toDouble()-b.toDouble())<1e-9;a is List<*>&&b is List<*>->a.size==b.size&&a.zip(b).all{same(it.first,it.second)};else->a==b}
        need(r.status==GraphCommandStatus.APPLIED,"Status ${r.status}: ${r.message}");need(r.semanticFrame?.intent=="DIFFERENTIATE","Wrong derivative intent")
        p.forEach{(k,v)->need(same(v,r.semanticFrame?.parameters?.get(k)),"Slot $k expected $v got ${r.semanticFrame?.parameters?.get(k)}")}
        need(before.functions==r.state.functions&&before.points==r.state.points&&before.shapes==r.state.shapes,"Derivative analysis mutated source geometry.")
        need(before.graph2DView==r.state.graph2DView&&before.solids==r.state.solids&&before.surfaceLayers==r.state.surfaceLayers&&before.points3D==r.state.points3D,"Another scene/view changed.")
        runCatching {
            val q=RuhiDerivativeLanguage.decode(requireNotNull(r.state.labSessionValues[RuhiDerivativeLanguage.MARK]));val d=requireNotNull(RuhiDerivativeLanguage.drawing(r.state));need(d.diagnostic.isEmpty(),d.diagnostic)
            val source=p["expression"]?.toString() ?: "x^2"
            fun derivative(x:Double):Double=when(source){"x^3"->3*x*x;"sin(x)"->cos(x);"x^2","x^2-4"->2*x;"exp(x)"->exp(x);"3*x^2+2*x"->6*x+2;"ln(x)"->if(x>0)1/x else Double.NaN;"x^3-3*x"->3*x*x-3;"cos(x)"->-sin(x);"1/x"->-1/(x*x);"x^4"->12*x*x;"x^4-2*x^2"->4*x*x*x-4*x;"sqrt(x)"->if(x>0)1/(2*sqrt(x))else Double.NaN;"tan(x)"->1/cos(x).pow(2);"2*x+3"->2.0;else->error("No independent derivative identity for $source")}
            fun original(x:Double)=ExpressionEngine().compile(source).eval(mapOf("x" to x))
            val at=p["at_x"];val xs=when(at){is List<*>->at.map{(it as Number).toDouble()};is Number->listOf(at.toDouble());else->if(p["target"]=="selected_point_on_curve")listOf(before.points[requireNotNull(c.selectedPointIndex)].x)else emptyList()}
            fun near(a:Double,b:Double)=abs(a-b)<1e-6*max(1.0,abs(b))
            if(p["animation"]=="tangent_slider"){
                need(q.mode=="slider","Missing live tangent mode")
                for(x in listOf(-2.25,.5,1.75)){val live=requireNotNull(RuhiDerivativeLanguage.drawing(r.state,x));need(live.points.singleOrNull()?.distanceTo(Vec2(x,x*x))?.let{it<1e-7}==true,"Live trace point failed");val line=live.helpers.singleOrNull{it.name=="slider"}?.curve;need(line is RuhiIntersectionMath.Curve.Scalar,"Live tangent missing");if(line is RuhiIntersectionMath.Curve.Scalar)need(RuhiIntersectionMath.y(line,x+.5)?.let{near(it,x*x+x)}==true,"Wrong changing tangent slope")}
                return@runCatching
            }
            if(p["explain"]==true){need(r.message.contains("not differentiable")&&r.message.contains("-1")&&r.message.contains("right-hand slope is 1"),"Missing unequal one-sided slopes.");return@runCatching}
            if(p["analysis"]!=null){val expected=when(source){"x^3-3*x"->listOf(Vec2(-1.0,2.0),Vec2(1.0,-2.0));"x^4-2*x^2"->listOf(Vec2(-1.0,-1.0),Vec2(0.0,0.0),Vec2(1.0,-1.0));else->listOf(Vec2(0.0,0.0))};need(d.points.size==expected.size&&expected.all{v->d.points.any{it.distanceTo(v)<1e-6}},"Wrong stationary/critical coordinates.");return@runCatching}
            xs.forEach{x->need(d.points.any{near(it.x,x)&&near(it.y,original(x))},"Missing evaluation point.")}
            val slopes=Regex("slope = ([-+0-9.eE]+)").findAll(r.message).map{it.groupValues[1].toDouble()}.toList()
            if(xs.isNotEmpty()){need(slopes.size==xs.size,"Wrong slope answer count.");xs.forEachIndexed{i,x->need(slopes.getOrNull(i)?.let{near(it,derivative(x))}==true,"Wrong independent slope at $x.")}}
            if(p["draw"] in setOf("tangent","normal","tangents")){
                xs.forEachIndexed{i,x->val h=d.helpers.filter{it.name in setOf("tangent","normal","tangents")}.getOrNull(i)?.curve;val expected=if(p["draw"]=="normal")-1/derivative(x)else derivative(x);need(h is RuhiIntersectionMath.Curve.Scalar,"Missing native tangent/normal line");if(h is RuhiIntersectionMath.Curve.Scalar){for(v in listOf(x-.5,x,x+.5))need(RuhiIntersectionMath.y(h,v)?.let{near(it,original(x)+expected*(v-x))}==true,"Wrong line equation.")}}
            }else {
                val helper=d.helpers.singleOrNull{it.name=="Derivative"}?.curve;need(helper is RuhiIntersectionMath.Curve.Scalar,"Missing native derivative overlay.")
                if(helper is RuhiIntersectionMath.Curve.Scalar)for(x in listOf(-2.25,-.75,.25,1.25,2.5)){val wanted=derivative(x);val actual=RuhiIntersectionMath.y(helper,x);if(wanted.isFinite())need(actual!=null&&near(actual,wanted),"Wrong derivative at $x: expected $wanted got $actual; curve=$helper")else need(actual==null,"Original domain was not preserved")}
            }
            need(q.order==((p["order"] as? Number)?.toInt() ?: 1),"Wrong derivative order")
        }.onFailure{errors+=it.message ?: "Derivative oracle failed"}
        return RuhiSeedBehaviorOracle.Check(errors.isEmpty(),errors)
    }
}
