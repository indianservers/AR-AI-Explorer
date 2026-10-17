package com.indianservers.aiexplorer.assistant.workspace

import com.indianservers.aiexplorer.core.*
import kotlin.math.*

/** Bounded deterministic quadrature; approximating rectangles are separate from the checked integral. */
object RuhiIntegralMath {
    data class Function(val expression:String,val restriction:String?=null)
    data class Values(val signed:Double,val absolute:Double,val positive:Double,val negative:Double,val error:Double)
    data class Estimate(val left:Double,val midpoint:Double,val right:Double,val trapezoid:Double,val count:Int)
    fun function(source:String):Function {
        require(source.length<=1200){"Keep integral formulas within 1200 characters."}
        var depth=0;source.forEach{if(it=='(')depth++;if(it==')')depth--;require(depth in 0..64){"Use balanced formulas with at most 64 nesting levels."}};require(depth==0)
        val typed=TypedGraphExpressionParser.parse(source)
        require(typed is TypedGraphExpression.Explicit&&typed.parameters.isEmpty()){"Choose an explicit function with numeric parameters for integration."}
        ExpressionEngine().compile(typed.expression)
        return Function(typed.expression,typed.restriction)
    }
    fun value(f:Function,x:Double):Double {
        val e=ExpressionEngine();val vars=mapOf("x" to x)
        f.restriction?.let{val allowed=e.compile(it).eval(vars);require(allowed.isFinite()&&allowed!=0.0){"The requested interval leaves the function's defined domain."}}
        return e.compile(f.expression).eval(vars).also{require(it.isFinite()&&abs(it)<=1e150){"The function is undefined or numerically unbounded in the requested interval; improper integration requires separate analysis."}}
    }
    fun integrate(first:Function,from:Double,to:Double,second:Function?=null):Values {
        require(from.isFinite()&&to.isFinite()&&abs(from)<=10000&&abs(to)<=10000){"Use finite bounds within 10000 units."}
        if(from==to){value(first,from);second?.let{value(it,from)};return Values(0.0,0.0,0.0,0.0,0.0)}
        val low=min(from,to);val high=max(from,to)
        // Independent seed panels avoid accepting a whole-period zero from just three samples.
        val n=64;val step=(high-low)/n
        fun guarded(f:Function)=f.restriction?.let{"if(($it),(${f.expression}),sqrt(-1))"} ?: f.expression
        val expression=if(second==null)guarded(first)else "(${guarded(first)})-(${guarded(second)})"
        var signed=0.0;var absolute=0.0;var error=0.0
        for(i in 0 until n){val a=low+i*step;val b=if(i==n-1)high else low+(i+1)*step
            for(x in listOf(a,(a+b)/2,b)){value(first,x);second?.let{value(it,x)}}
            val native=CompetitiveGraphCalculus().integral("y=$expression",a,b,tolerance=1e-10)
            require(native.signedArea.isFinite()&&native.geometricArea.isFinite()&&native.errorEstimate.isFinite()){ "Integral did not converge to a finite value." }
            val compiled=ExpressionEngine().compile(expression)
            val probe=listOf(.3819660112501051,.6180339887498949).maxOf{abs(compiled.eval(mapOf("x" to a+(b-a)*it)))}
            require(probe.isFinite()&&(probe<1e-10||native.geometricArea>(b-a)*probe*1e-8)){"The integration grid did not resolve this function's variation. Narrow the interval before estimating its area."}
            signed+=native.signedArea;absolute+=native.geometricArea;error+=native.errorEstimate
        }
        require(error<=1e-7*max(1.0,absolute)){"Integral did not meet the numerical tolerance; narrow the interval or inspect its singularities."}
        require(absolute+1e-7>=abs(signed)){"Signed and absolute quadrature estimates disagree."}
        val oriented=if(from<=to)signed else -signed
        return Values(oriented,absolute,(absolute+signed)/2,(signed-absolute)/2,error)
    }
    fun estimate(f:Function,from:Double,to:Double,count:Int=12):Estimate {
        require(count in 2..200&&from.isFinite()&&to.isFinite()&&from<to)
        val step=(to-from)/count
        var left=0.0;var midpoint=0.0;var right=0.0
        for(i in 0 until count){val x=from+i*step;left+=value(f,x)*step;midpoint+=value(f,x+step/2)*step;right+=value(f,x+step)*step}
        return Estimate(left,midpoint,right,(left+right)/2,count)
    }
    fun antiderivative(f:Function):String {
        val native=SymbolicCalculusEngine().integrate(f.expression)?.expression ?: throw IllegalArgumentException("A symbolic antiderivative is not available for this expression; supply bounds for numerical integration.")
        ExpressionEngine().compile(native)
        return native
    }
    fun accumulation(f:Function,start:Double,minimum:Double,maximum:Double):String {
        require(start.isFinite()&&abs(start)<=10000)
        // A primitive alone is not evidence for crossing a pole from the accumulation origin.
        integrate(f,start,minimum);integrate(f,start,maximum)
        val primitive=antiderivative(f);val base=ExpressionEngine().compile(primitive).eval(mapOf("x" to start));require(base.isFinite())
        return "($primitive)-($base)"
    }
}
