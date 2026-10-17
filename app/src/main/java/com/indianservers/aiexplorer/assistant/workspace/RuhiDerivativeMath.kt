package com.indianservers.aiexplorer.assistant.workspace

import com.indianservers.aiexplorer.core.*
import kotlin.math.*

/** Symbolic formulas and checked one-sided slopes; no mathematical values come from the NLP model. */
object RuhiDerivativeMath {
    data class Result(val original:String,val derivative:String,val order:Int,val restriction:String?)
    data class Slope(val point:Vec2,val value:Double,val error:Double)
    /** Exact branch rule for abs(x); a fixed numerical step can cross its corner near zero. */
    fun absoluteValueSlope(x:Double):Double? {
        require(x.isFinite()&&abs(x)<=10000){"Use a finite x coordinate within 10000 units."}
        return when {x==0.0->null;x>0.0->1.0;else->-1.0}
    }
    fun derive(source:String,order:Int=1):Result {
        require(source.length<=1200&&order in 1..2){"Use a formula within 1200 characters and derivative order 1 or 2."}
        val typed=TypedGraphExpressionParser.parse(source)
        require(typed is TypedGraphExpression.Explicit&&typed.parameters.isEmpty()){"Choose an explicit function with numeric parameters for differentiation."}
        val derivative=SymbolicCalculusEngine().differentiate(typed.expression,order=order).expression
        ExpressionEngine().compile(derivative)
        return Result(typed.expression,derivative,order,typed.restriction)
    }
    fun value(result:Result,x:Double):Double?=runCatching {
        require(x.isFinite()&&abs(x)<=10000)
        val vars=mapOf("x" to x);val e=ExpressionEngine()
        if(result.restriction!=null)require(e.compile(result.restriction).eval(vars)!=0.0)
        e.compile(result.original).eval(vars).also{require(it.isFinite())}
    }.getOrNull()
    fun slope(result:Result,x:Double):Slope {
        require(result.order==1)
        val y=requireNotNull(value(result,x)){"The function is undefined at x=$x."}
        if(RuhiDerivativeBranches.present(result)) {
            val sides=RuhiDerivativeBranches.sides(result,x)
            require(sides.agree){"The one-sided slopes do not agree on a finite derivative here."}
            return Slope(Vec2(x,y),(sides.left+sides.right)/2,abs(sides.left-sides.right))
        }
        val derivative=ExpressionEngine().compile(result.derivative).eval(mapOf("x" to x))
        require(derivative.isFinite()){"The derivative is undefined at x=$x."}
        val h=1e-4*max(1.0,abs(x))
        fun sides(step:Double):Pair<Double,Double>{val left=requireNotNull(value(result,x-step)){"A two-sided derivative requires points on both sides."};val right=requireNotNull(value(result,x+step)){"A two-sided derivative requires points on both sides."};return (y-left)/step to (right-y)/step}
        val coarse=sides(h);val fine=sides(h/2)
        val left=2*fine.first-coarse.first;val right=2*fine.second-coarse.second
        val tolerance=2e-5*max(1.0,abs(derivative))
        require(abs(left-right)<=tolerance&&abs((left+right)/2-derivative)<=tolerance){"The one-sided slopes do not agree on a finite derivative here."}
        return Slope(Vec2(x,y),derivative,max(abs(left-right),abs((left+right)/2-derivative)))
    }
    fun line(s:Slope,normal:Boolean):RuhiIntersectionMath.Curve {
        if(normal&&abs(s.value)<1e-12)return RuhiIntersectionMath.Curve.Vertical(s.point.x)
        val m=if(normal)-1/s.value else s.value
        return RuhiIntersectionMath.Curve.Scalar("($m)*(x-(${s.point.x}))+(${s.point.y})")
    }
    fun stationary(r:Result,low:Double,high:Double):List<Vec2> {
        require(r.order==1)
        if(runCatching{SymbolicCasEngine().simplify(r.derivative).exact=="0"}.getOrDefault(false))throw IllegalArgumentException("The slope is zero throughout the function's domain; stationary points are not an isolated list.")
        val compiled=ExpressionEngine().compile(r.derivative)
        val scale=(0..16).mapNotNull{i->val x=low+(high-low)*i/16;runCatching{if(value(r,x)==null)null else compiled.eval(mapOf("x" to x)).takeIf{it.isFinite()}?.let(::abs)}.getOrNull()}.maxOrNull() ?: 0.0
        require(scale>0.0){"The sampled slope is zero throughout this range; stationary interval analysis is required."}
        val ordinary=RobustGraphAnalysisEngine().roots("y=(${r.derivative})/($scale)",low,high).mapNotNull{root->runCatching{slope(r,root.point.x)}.getOrNull()?.takeIf{abs(it.value)<=1e-8*scale}?.point}
        val branches=RuhiDerivativeBranches.candidates(r,low,high).mapNotNull{c->
            val y=value(r,c.x) ?: return@mapNotNull null
            val sides=runCatching{RuhiDerivativeBranches.sides(r,c.x,c.zeros)}.getOrNull() ?: return@mapNotNull null
            if(sides.stationary)Vec2(c.x,y)else null
        }
        return distinct(ordinary+branches)
    }
    fun critical(r:Result,low:Double,high:Double):List<Vec2> {
        val ordinary=stationary(r,low,high)
        val branches=RuhiDerivativeBranches.candidates(r,low,high).mapNotNull{c->
            val y=value(r,c.x) ?: return@mapNotNull null
            val h=1e-7*max(1.0,abs(c.x))
            if(value(r,c.x-h)==null||value(r,c.x+h)==null)return@mapNotNull null
            val sides=RuhiDerivativeBranches.sides(r,c.x,c.zeros)
            if(!sides.agree||sides.stationary)Vec2(c.x,y)else null
        }
        return distinct(ordinary+branches)
    }
    private fun distinct(points:List<Vec2>)=points.sortedBy{it.x}.fold(mutableListOf<Vec2>()){out,p->if(out.none{it.distanceTo(p)<1e-6})out+=p;out}
}
