package com.indianservers.aiexplorer.assistant.workspace

import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import kotlin.math.*

/** Original-row checks use closed-form primitives, not production quadrature or NLP predictions. */
object RuhiIntegralOracle {
    fun check(p:Map<String,Any?>,before:WorkspaceState,r:RuhiExecutionResult,c:RuhiSceneContext):RuhiSeedBehaviorOracle.Check {
        val errors=mutableListOf<String>();fun need(ok:Boolean,why:String){if(!ok)errors+=why}
        fun same(a:Any?,b:Any?):Boolean=when{a is Number&&b is Number->abs(a.toDouble()-b.toDouble())<1e-9;a is List<*>&&b is List<*>->a.size==b.size&&a.zip(b).all{same(it.first,it.second)};else->a==b}
        fun near(a:Double,b:Double)=abs(a-b)<=1e-7*max(1.0,abs(b))
        need(r.semanticFrame?.intent=="INTEGRATE","Wrong integral intent")
        p.forEach{(k,v)->need(same(v,r.semanticFrame?.parameters?.get(k)),"Slot $k expected $v got ${r.semanticFrame?.parameters?.get(k)}")}
        need(before.functions==r.state.functions&&before.points==r.state.points&&before.shapes==r.state.shapes&&before.pointDependencies==r.state.pointDependencies,"Integration changed source geometry")
        need(before.graph2DView==r.state.graph2DView&&before.solids==r.state.solids&&before.surfaceLayers==r.state.surfaceLayers&&before.points3D==r.state.points3D,"Integration changed another scene or viewport")
        if(p["bounds"]=="missing") {
            need(r.status==GraphCommandStatus.CLARIFICATION,"Missing bounds must ask instead of executing")
            need(r.message.contains("lower")&&r.message.contains("upper"),"Ask one question for both bounds")
            val chips=r.dialogue?.suggestions.orEmpty();need(chips.map{it.label}.containsAll(listOf("Visible x-range","0 to 1","Custom bounds")),"Required viewport/preset/custom bounds choices missing")
            need(RuhiDialogueMemory.read(r.state).pending?.kind=="integral_bounds","Missing resumable bounds question")
            val resumed=RuhiDialogueManager().execute(r.state,"Use integral bounds 0 to 2",WorkspaceHistoryByModule(),c)
            need(resumed.status==GraphCommandStatus.APPLIED,"Bounds reply did not resume")
            val d=RuhiIntegralLanguage.drawing(resumed.state);need(d?.values?.signed?.let{near(it,8.0/3)}==true,"Resumed x^2 integral is wrong")
            if(p["method"]=="comparison")need(d?.estimate!=null,"Resumed comparison lost the method")
            return RuhiSeedBehaviorOracle.Check(errors.isEmpty(),errors)
        }
        need(r.status==GraphCommandStatus.APPLIED,"Status ${r.status}: ${r.message}")
        runCatching {
            val d=requireNotNull(RuhiIntegralLanguage.drawing(r.state));need(d.diagnostic.isEmpty(),d.diagnostic)
            val expression=p["expression"]?.toString() ?: "x^2"
            fun value(f:String,x:Double):Double=when(f){"x^2"->x*x;"x^3"->x*x*x;"x"->x;"2*x"->2*x;"3*x^2"->3*x*x;"sin(x)","sin(t)"->sin(x);"cos(x)"->cos(x);"exp(x)"->exp(x);"4"->4.0;"ln(x)"->ln(x);"1/(1+x^2)"->1/(1+x*x);else->error("No independent integrand identity for $f")}
            fun primitive(f:String,x:Double):Double=when(f){"x^2"->x*x*x/3;"x^3"->x.pow(4)/4;"x"->x*x/2;"2*x"->x*x;"3*x^2"->x*x*x;"sin(x)","sin(t)"->-cos(x);"cos(x)"->sin(x);"exp(x)"->exp(x);"4"->4*x;"ln(x)"->x*ln(x)-x;"1/(1+x^2)"->atan(x);else->error("No independent primitive for $f")}
            if(p["kind"]=="indefinite"){
                val helper=d.helpers.single{it.name=="Antiderivative (C=0)"}.curve as RuhiIntersectionMath.Curve.Scalar
                val base=requireNotNull(RuhiIntersectionMath.y(helper,0.0));for(x in listOf(-2.3,.4,1.7))need(RuhiIntersectionMath.y(helper,x)?.let{near(it-base,x*x)}==true,"Wrong antiderivative")
                need(r.message.contains("+ C"),"Missing arbitrary constant")
                return@runCatching
            }
            if(p["variable_upper"]=="x"){
                val helper=d.helpers.single{it.name=="Accumulation"}.curve as RuhiIntersectionMath.Curve.Scalar
                for(x in listOf(-2.3,.4,1.7))need(RuhiIntersectionMath.y(helper,x)?.let{near(it,1-cos(x))}==true,"Wrong accumulation value")
                return@runCatching
            }
            fun constant(v:Any)=when(v){is Number->v.toDouble();"pi"->PI;"pi/2"->PI/2;"2*pi"->2*PI;"e"->E;else->error("Unknown bound $v")}
            val pair=(p["functions"] as? List<*>)?.map{it.toString()}
            val supplied=p["bounds"] as? List<*>
            val expectedBounds=if(supplied!=null)constant(supplied[0]!!) to constant(supplied[1]!!)else if(pair==listOf("x^2","4"))-2.0 to 2.0 else 0.0 to 2.0
            val a=expectedBounds.first;val b=expectedBounds.second
            need(d.bounds?.let{near(it.first,a)&&near(it.second,b)}==true,"Incorrect supplied/intersection-derived bounds")
            val f=pair?.first() ?: expression;val g=pair?.last()
            fun p0(x:Double)=primitive(f,x)-(g?.let{primitive(it,x)} ?: 0.0)
            val signed=p0(b)-p0(a)
            val cuts=(listOf(a,b)+if(f=="sin(x)")(-8..8).map{it*PI}.filter{it>a&&it<b}else if(f in setOf("x","x^3")&&a<0&&b>0)listOf(0.0)else emptyList()).distinct().sorted()
            val absolute=cuts.zipWithNext().sumOf{abs(p0(it.second)-p0(it.first))}
            val v=requireNotNull(d.values);need(near(v.signed,signed),"Incorrect signed integral");need(near(v.absolute,absolute),"Incorrect absolute area");need(near(v.positive,(absolute+signed)/2)&&near(v.negative,(signed-absolute)/2),"Incorrect sign separation")
            need(v.error<=1e-7*max(1.0,absolute),"Numerical error exceeds tolerance")
            val exactBands=d.bands.filterNot{it.approximation};need(exactBands.size==256,"Missing bounded shaded region")
            exactBands.forEach{band->need(band.vertices.all{it.x>=a-1e-9&&it.x<=b+1e-9},"Fill escaped the bounds");val pts=band.vertices;need(near(pts[1].y,value(f,pts[1].x))&&near(pts[2].y,value(f,pts[2].x)),"Fill does not follow the function");need(near(pts[0].y,g?.let{value(it,pts[0].x)} ?: 0.0)&&near(pts[3].y,g?.let{value(it,pts[3].x)} ?: 0.0),"Wrong fill baseline")}
            if(p["method"]!=null){val e=requireNotNull(d.estimate);val n=e.count.toDouble();need(e.count==12,"Unexplained panel count")
                if(f=="x^2"&&a==0.0&&b==2.0){val h=2/n;need(near(e.left,h.pow(3)*n*(n-1)*(2*n-1)/6),"Incorrect left sum");need(near(e.right,h.pow(3)*n*(n+1)*(2*n+1)/6),"Incorrect right sum");need(near(e.midpoint,8.0/3-2/(3*n*n)),"Incorrect midpoint sum")}
                if(f=="sin(x)")need(near(e.trapezoid,(PI/n)/tan(PI/(2*n))),"Incorrect trapezoid rule")
                need(d.bands.count{it.approximation}==e.count*(if(p["method"]=="comparison")2 else 1),"Missing approximation panels")
            }
            if(p["split_sign"]==true)need(exactBands.any{it.sign>0}&&exactBands.any{it.sign<0},"Sign-separated shading missing")
        }.onFailure{errors+=it.message ?: "Integral oracle failed"}
        return RuhiSeedBehaviorOracle.Check(errors.isEmpty(),errors)
    }
}
