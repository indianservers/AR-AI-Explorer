package com.indianservers.aiexplorer.assistant.workspace

import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import kotlin.math.*

/** Source slots and numerical identities are checked independently of the language parser. */
object RuhiFunctionOracle {
    private fun matches(a:Any?,b:Any?):Boolean=when{a is Number&&b is Number->abs(a.toDouble()-b.toDouble())<1e-9;a is List<*>&&b is List<*>->a.size==b.size&&a.zip(b).all{matches(it.first,it.second)};else->a==b}
    private fun expected(source:String,x:Double):Double=when(source){
        "x^2"->x*x;"2*x+1"->2*x+1;"sin(x)"->sin(x);"cos(x)"->cos(x);"x^3"->x*x*x;"abs(x)"->abs(x);"1/x"->1/x;"ln(x)"->ln(x);"sqrt(x)"->sqrt(x);"exp(x)"->exp(x);"tan(x)"->tan(x);"x^2-4*x+3"->x*x-4*x+3;"sin(3*x)"->sin(3*x);"2^x"->2.0.pow(x);"5"->5.0;"x^3-x"->x*x*x-x;"1/(x-2)"->1/(x-2);"3*x-7"->3*x-7;"floor(x)"->floor(x);"piecewise(x>=0,x,-x)"->if(x>=0)x else -x;else->error("Independent function identity missing: $source")}
    fun check(params:Map<String,Any?>,before:WorkspaceState,r:RuhiExecutionResult,c:RuhiSceneContext):RuhiSeedBehaviorOracle.Check {
        val errors=mutableListOf<String>();fun need(v:Boolean,msg:String){if(!v)errors+=msg};fun near(a:Double,b:Double)=a.isFinite()&&b.isFinite()&&abs(a-b)<1e-7*max(1.0,abs(b))
        need(r.status==GraphCommandStatus.APPLIED,"Plot status ${r.status}: ${r.message}")
        need(r.semanticFrame?.intent=="PLOT_FUNCTION_2D"&&r.semanticFrame?.engine==RuhiEngine.GRAPH_2D,"Missing function intent/engine.")
        params.forEach{(k,v)->need(matches(v,r.semanticFrame?.parameters?.get(k)),"Original plot slot $k differs: ${r.semanticFrame?.parameters?.get(k)}")}
        val existing=(params["function_name"] as? String)?.let{n->before.functions.singleOrNull{it.name==n}}
        val outputs=r.state.functions.filter{f->if(existing!=null)f.id==existing.id else before.functions.none{it.id==f.id}}
        need(outputs.size==1,"Exactly one requested function should change.")
        need(r.state.functions.size==before.functions.size+if(existing==null)1 else 0,"Wrong function count.")
        before.functions.filter{it.id!=existing?.id}.forEach{old->need(r.state.functions.singleOrNull{it.id==old.id}==old,"Unrelated function changed.")}
        need(before.points==r.state.points&&before.shapes==r.state.shapes&&before.pointDependencies==r.state.pointDependencies&&before.points3D==r.state.points3D&&before.planes3D==r.state.planes3D&&before.vectors3D==r.state.vectors3D&&before.solids==r.state.solids&&before.surfaceLayers==r.state.surfaceLayers,"Another engine changed.")
        need(before.graph2DView==r.state.graph2DView,"Plotting reset the interactive axes/view.")
        runCatching {
            val f=outputs.single();need(f.visible,"Requested curve is hidden.");need(r.state.labSessionValues[GraphAssistant.selectionKey(MathModule.Graph2D)]==f.id,"New/updated curve is not selected.")
            (params["function_name"] as? String)?.let{need(f.name==it,"Function label differs.")}
            val typed=TypedGraphExpressionParser.parse(f.expression);val engine=TypedGraphEngine();val sampled=engine.sample(typed,samples=520);val points=sampled.curves.flatMap{it.points}
            need(points.size>=2||sampled.implicitSegments.isNotEmpty(),"No native drawable geometry.")
            need(points.all{it.x.isFinite()&&it.y.isFinite()},"Non-finite native points.")
            val formula=params["expression"] as? String
            if(formula!=null){
                need(typed is TypedGraphExpression.Explicit||typed is TypedGraphExpression.Piecewise,"Wrong native function type.")
                val domain=(params["domain"] as? List<*>)?.map{(it as Number).toDouble()}
                for(x in listOf(-3.25,-1.25,-.25,0.0,.25,1.25,2.25,3.25)) {val wanted=expected(formula,x);val actual=engine.evaluate(typed,x);val allowed=domain==null||x in domain[0]..domain[1];if(wanted.isFinite()&&allowed)need(actual!=null&&near(actual.y,wanted),"Numerical identity differs at x=$x.")else need(actual==null,"Domain/pole should be undefined at x=$x.")}
                need(points.all{near(it.y,expected(formula,it.x))},"Sampled points do not satisfy the requested function.")
                if(domain!=null){need(typed is TypedGraphExpression.Explicit&&typed.restriction!=null,"Native domain restriction missing.");need(points.all{it.x>=domain[0]-1e-9&&it.x<=domain[1]+1e-9},"Samples escaped the requested domain.");domain.forEach{need(engine.evaluate(typed,it)!=null,"Closed domain endpoint was removed.")}}
                val poles=when(formula){"1/x"->listOf(0.0);"1/(x-2)"->listOf(2.0);"tan(x)"->(-4..4).map{PI/2+it*PI}.filter{it in -10.0..10.0};else->emptyList()}
                poles.forEach{pole->need(sampled.curves.none{seg->seg.points.zipWithNext().any{(a,b)->a.x<pole&&b.x>pole}},"A segment falsely joins across x=$pole.")}
                if(poles.isNotEmpty())need(sampled.curves.size>1,"Discontinuous function lacks separate branches.")
                if(formula=="floor(x)")need(sampled.curves.all{segment->segment.points.zipWithNext().all{near(it.first.y,it.second.y)}},"Floor steps have false vertical/diagonal joins.")
            } else if(params["equation"]=="x=4") {
                need(typed is TypedGraphExpression.Implicit,"Vertical line is not implicit.");need(sampled.implicitSegments.isNotEmpty()&&sampled.implicitSegments.all{near(it.start.x,4.0)&&near(it.end.x,4.0)},"Vertical line moved away from x=4.")
            } else if(params["branch"]=="upper") {
                need(typed is TypedGraphExpression.Explicit,"Upper circle branch should be an explicit half-circle.");need(points.all{it.y>=-1e-9&&near(it.x*it.x+it.y*it.y,9.0)},"Wrong circle radius/branch.");need(engine.evaluate(typed,4.0)==null&&engine.evaluate(typed,-4.0)==null,"Circle branch has out-of-domain values.")
            } else if(params["polar"]!=null) {
                need(typed is TypedGraphExpression.Polar,"Polar definition lost.");listOf(0.0,.2,.7,1.3,2.6).forEach{t->val radius=2*cos(3*t);val actual=engine.evaluate(typed,t);need(actual!=null&&near(actual.x,radius*cos(t))&&near(actual.y,radius*sin(t)),"Polar radius/angle identity differs.")}
                need(points.all{p->val radius=hypot(p.x,p.y);radius<1e-8||near(radius,2*cos(3*atan2(p.y,p.x)))},"Native rose samples differ.")
            } else if(params["parametric"]!=null) {
                need(typed is TypedGraphExpression.Parametric,"Parametric definition lost.");listOf(-2.2,0.0,.4,1.8).forEach{t->val actual=engine.evaluate(typed,t);need(actual!=null&&near(actual.x,cos(t))&&near(actual.y,sin(t)),"Parametric identities differ.")};need(points.all{near(it.x*it.x+it.y*it.y,1.0)},"Parametric curve is not a unit circle.")
            } else error("Independent graph oracle does not cover these constraints.")
        }.onFailure{errors+="Function oracle error: ${it.message}"}
        return RuhiSeedBehaviorOracle.Check(errors.isEmpty(),errors)
    }
}
