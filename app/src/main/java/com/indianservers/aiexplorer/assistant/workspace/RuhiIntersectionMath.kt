package com.indianservers.aiexplorer.assistant.workspace

import com.indianservers.aiexplorer.core.*
import kotlin.math.*

/** Numeric intersections validate both original curves, so poles and squared extraneous roots are discarded. */
object RuhiIntersectionMath {
    sealed interface Curve {
        data class Scalar(val expression:String,val restriction:String?=null):Curve
        data class Vertical(val x:Double):Curve
        data class Circle(val center:Vec2,val radius:Double):Curve
        data class Line(val a:Vec2,val b:Vec2):Curve
        data class Implicit(val residual:String):Curve
    }
    data class Solution(val points:List<Vec2>,val infinite:Boolean=false)
    fun native(source:String):Curve {
        require(source.length<=1200){"Keep intersection formulas within 1200 characters."};var depth=0;source.forEach{if(it=='(')depth++;if(it==')')depth--;require(depth in 0..64){"Use balanced formulas with at most 64 nesting levels."}};require(depth==0)
        val typed=TypedGraphExpressionParser.parse(source);require(typed.parameters.isEmpty()){"Set graph parameters to numeric values before intersection analysis."}
        return when(typed){
            is TypedGraphExpression.Explicit->Curve.Scalar(typed.expression,typed.restriction)
            is TypedGraphExpression.Piecewise->Curve.Scalar(typed.branches.asReversed().fold("sqrt(-1)"){tail,b->"if((${b.condition}),(${b.expression}),($tail))"})
            is TypedGraphExpression.Implicit->{require('{' !in source){"Restricted implicit graphs need explicit domain analysis before intersection marking."};val raw=MathExpressionNormalizer.normalize(source).replace(" ","");val constant=Regex("^x=(.+)$").matchEntire(raw)?.groupValues?.get(1)?.let{runCatching{ExpressionEngine().compile(it).eval(emptyMap())}.getOrNull()};if(constant!=null&&constant.isFinite())Curve.Vertical(constant)else Curve.Implicit(typed.residual)}
            else->throw IllegalArgumentException("Use explicit functions, vertical lines, or native named geometry for these intersections.")
        }
    }
    private fun asFunction(c:Curve):Curve=if(c is Curve.Line){val d=c.b-c.a;require(hypot(d.x,d.y)>1e-10){"A line requires distinct points."};if(abs(d.x)<1e-12)Curve.Vertical(c.a.x)else Curve.Scalar("(${d.y/d.x})*x+(${c.a.y-d.y/d.x*c.a.x})")}else c
    fun y(c:Curve.Scalar,x:Double):Double?=runCatching {
        val engine=ExpressionEngine();val values=mapOf("x" to x)
        if(c.restriction!=null&&engine.compile(c.restriction).eval(values)==0.0)return null
        engine.compile(c.expression).eval(values).takeIf{it.isFinite()&&abs(it)<=1e12}
    }.getOrNull()
    fun solve(first:Curve,second:Curve,minimum:Double,maximum:Double,minimumY:Double=-10000.0,maximumY:Double=10000.0):Solution {
        require(minimum.isFinite()&&maximum.isFinite()&&minimum<maximum&&maximum-minimum<=20000)
        require(minimumY.isFinite()&&maximumY.isFinite()&&minimumY<maximumY&&maximumY-minimumY<=20000)
        if(first is Curve.Line&&second is Curve.Circle)return lineCircle(first,second).bounded(minimum,maximum)
        if(second is Curve.Line&&first is Curve.Circle)return lineCircle(second,first).bounded(minimum,maximum)
        val a=asFunction(first);val b=asFunction(second)
        val result=when {
            a is Curve.Circle&&b is Curve.Circle->circles(a,b)
            a is Curve.Vertical&&b is Curve.Vertical->Solution(emptyList(),abs(a.x-b.x)<1e-10)
            a is Curve.Vertical&&b is Curve.Scalar->Solution(listOfNotNull(y(b,a.x)?.let{Vec2(a.x,it)}))
            b is Curve.Vertical&&a is Curve.Scalar->Solution(listOfNotNull(y(a,b.x)?.let{Vec2(b.x,it)}))
            a is Curve.Vertical&&b is Curve.Circle->verticalCircle(a,b)
            b is Curve.Vertical&&a is Curve.Circle->verticalCircle(b,a)
            a is Curve.Scalar&&b is Curve.Scalar->scalars(a,b,minimum,maximum)
            a is Curve.Scalar&&b is Curve.Circle->scalarCircle(a,b,minimum,maximum)
            b is Curve.Scalar&&a is Curve.Circle->scalarCircle(b,a,minimum,maximum)
            a is Curve.Implicit&&b is Curve.Scalar->implicitScalar(a,b,minimum,maximum)
            b is Curve.Implicit&&a is Curve.Scalar->implicitScalar(b,a,minimum,maximum)
            a is Curve.Implicit&&b is Curve.Vertical->implicitVertical(a,b,minimumY,maximumY)
            b is Curve.Implicit&&a is Curve.Vertical->implicitVertical(b,a,minimumY,maximumY)
            else->throw IllegalArgumentException("This pair needs an implicit/implicit or parametric intersection solver. Choose an explicit curve or vertical line for one side.")
        }
        return result.bounded(minimum,maximum)
    }
    // Substitute whole variable tokens only. Every candidate is re-evaluated in the original
    // equation and explicit domain; roots of a denominator or of a squared equation are not proof.
    private fun substitute(source:String,variable:String,value:String)=Regex("\\b${Regex.escape(variable)}\\b").replace(source){"($value)"}
    private fun implicitScalar(a:Curve.Implicit,b:Curve.Scalar,low:Double,high:Double):Solution {
        val residual=substitute(a.residual,"y",b.expression)
        require(!identicallyZero(residual)){"The explicit curve lies on the implicit equation; there is no isolated intersection list."}
        val original=ExpressionEngine().compile(a.residual)
        val candidates=implicitCandidates(residual,low,high)
        return Solution(candidates.mapNotNull{x->y(b,x)?.let{v->val p=Vec2(x,v);if(validImplicit(original,p))p else null}})
    }
    private fun implicitVertical(a:Curve.Implicit,b:Curve.Vertical,low:Double,high:Double):Solution {
        val residual=substitute(substitute(a.residual,"x",b.x.toString()),"y","x")
        require(!identicallyZero(residual)){"The vertical line lies on the implicit equation; there is no isolated intersection list."}
        val original=ExpressionEngine().compile(a.residual)
        val candidates=implicitCandidates(residual,low,high)
        return Solution(candidates.mapNotNull{v->Vec2(b.x,v).takeIf{validImplicit(original,it)}})
    }
    private fun identicallyZero(source:String)=runCatching{SymbolicCasEngine().simplify(source).exact=="0"}.getOrDefault(false)
    private fun implicitCandidates(residual:String,low:Double,high:Double):List<Double> {
        val compiled=ExpressionEngine().compile(residual)
        val magnitudes=(0..32).mapNotNull{i->runCatching{abs(compiled.eval(mapOf("x" to low+(high-low)*i/32)))}.getOrNull()?.takeIf{it.isFinite()&&it>0.0}}.sorted()
        require(magnitudes.isNotEmpty()){ "No finite isolated residual values in this viewport; narrow the range or inspect an overlapping curve." }
        val scale=magnitudes[magnitudes.size/2];require(scale in 1e-200..1e200){"Rescale this equation to a numerically stable magnitude before intersection analysis."}
        return RobustGraphAnalysisEngine().roots("y=($residual)/($scale)",low,high).map{it.point.x}+listOf(low,high)
    }
    private fun validImplicit(expression:Expression,p:Vec2):Boolean=runCatching {
        fun at(x:Double,y:Double)=expression.eval(mapOf("x" to x,"y" to y))
        val value=at(p.x,p.y)
        if(!value.isFinite())return false
        if(value==0.0)return true
        val h=1e-5*max(1.0,max(abs(p.x),abs(p.y)))
        val dx=(at(p.x+h,p.y)-at(p.x-h,p.y))/(2*h);val dy=(at(p.x,p.y+h)-at(p.x,p.y-h))/(2*h)
        val gradient=hypot(dx,dy)
        gradient.isFinite()&&gradient>0.0&&abs(value)/gradient<=1e-7
    }.getOrDefault(false)
    private fun Solution.bounded(low:Double,high:Double)=copy(points=points.filter{it.x.isFinite()&&it.y.isFinite()&&it.x>=low-1e-8&&it.x<=high+1e-8}.sortedWith(compareBy<Vec2>{it.x}.thenBy{it.y}).fold(mutableListOf<Vec2>()){out,p->if(out.none{it.distanceTo(p)<1e-6})out+=Vec2(if(abs(p.x)<1e-10)0.0 else p.x,if(abs(p.y)<1e-10)0.0 else p.y);out})
    private fun scalars(a:Curve.Scalar,b:Curve.Scalar,low:Double,high:Double):Solution {
        val identical=a.expression==b.expression || runCatching{SymbolicCasEngine().simplify("(${a.expression})-(${b.expression})").exact=="0"}.getOrDefault(false)
        if(identical){require(a.restriction==null&&b.restriction==null){"Coincident restricted formulas need a separate overlap-domain analysis."};return Solution(emptyList(),true)}
        val candidates=RobustGraphAnalysisEngine().intersections("y=${a.expression}","y=${b.expression}",low,high).map{it.point.x}+listOf(low,high)
        val points=candidates.mapNotNull{x->val ay=y(a,x);val by=y(b,x);if(ay!=null&&by!=null&&abs(ay-by)<=1e-7*max(1.0,max(abs(ay),abs(by))))Vec2(x,(ay+by)/2)else null}
        return Solution(points)
    }
    private fun scalarCircle(a:Curve.Scalar,b:Curve.Circle,low:Double,high:Double):Solution {
        val equation="(x-(${b.center.x}))^2+((${a.expression})-(${b.center.y}))^2-(${b.radius})^2"
        val candidates=RobustGraphAnalysisEngine().roots("y=$equation",low,high).map{it.point.x}+listOf(low,high)
        return Solution(candidates.mapNotNull{x->y(a,x)?.let{v->val p=Vec2(x,v);if(abs(p.distanceTo(b.center)-b.radius)<1e-6)p else null}})
    }
    private fun verticalCircle(a:Curve.Vertical,b:Curve.Circle):Solution {
        val square=b.radius*b.radius-(a.x-b.center.x).pow(2);if(square< -1e-9)return Solution(emptyList());val dy=sqrt(max(0.0,square))
        return Solution(listOf(Vec2(a.x,b.center.y-dy),Vec2(a.x,b.center.y+dy)))
    }
    private fun lineCircle(a:Curve.Line,b:Curve.Circle):Solution {
        val d=a.b-a.a;val length=d.x*d.x+d.y*d.y;require(length>1e-18){"A line needs two distinct points."}
        val q=a.a-b.center;val u=-(q.x*d.x+q.y*d.y)/length;val closest=a.a+d*u;val square=b.radius*b.radius-(closest-b.center).let{it.x*it.x+it.y*it.y}
        if(square< -1e-9)return Solution(emptyList());val dt=sqrt(max(0.0,square)/length);return Solution(listOf(a.a+d*(u-dt),a.a+d*(u+dt)))
    }
    private fun circles(a:Curve.Circle,b:Curve.Circle):Solution {
        val delta=b.center-a.center;val distance=hypot(delta.x,delta.y)
        if(distance<1e-12)return Solution(emptyList(),abs(a.radius-b.radius)<1e-9)
        if(distance>a.radius+b.radius+1e-9||distance<abs(a.radius-b.radius)-1e-9)return Solution(emptyList())
        val along=(a.radius*a.radius-b.radius*b.radius+distance*distance)/(2*distance);val height=sqrt(max(0.0,a.radius*a.radius-along*along));val center=a.center+delta*(along/distance);val offset=Vec2(-delta.y,delta.x)*(height/distance)
        return Solution(listOf(center-offset,center+offset))
    }
}
