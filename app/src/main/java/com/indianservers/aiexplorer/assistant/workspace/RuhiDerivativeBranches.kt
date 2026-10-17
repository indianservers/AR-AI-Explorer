package com.indianservers.aiexplorer.assistant.workspace

import com.indianservers.aiexplorer.core.*
import kotlin.math.*

/** Local analytic branches of abs compositions; original values/domain remain authoritative. */
internal object RuhiDerivativeBranches {
    data class Sides(val left:Double,val right:Double) {
        val agree get()=abs(left-right)<=1e-9*max(1e-300,max(abs(left),abs(right)))
        val stationary get()=agree&&left==0.0&&right==0.0
    }
    data class Candidate(val x:Double,val zeros:Set<String>)
    fun present(r:RuhiDerivativeMath.Result)=Regex("(?i)\\babs\\s*\\(").containsMatchIn(r.original)
    fun candidates(r:RuhiDerivativeMath.Result,low:Double,high:Double):List<Candidate> {
        if(!present(r))return emptyList()
        val cas=SymbolicCasEngine();val arguments=linkedSetOf<String>()
        fun walk(e:SymbolicExpression) {when(e) {
            is SymbolicExpression.Function->{if(e.name=="abs")arguments+=cas.render(e.arguments.single());e.arguments.forEach(::walk)}
            is SymbolicExpression.UnaryMinus->walk(e.value)
            is SymbolicExpression.Sum->e.terms.forEach(::walk)
            is SymbolicExpression.Product->e.factors.forEach(::walk)
            is SymbolicExpression.Power->{walk(e.base);walk(e.exponent)}
            else->Unit
        }}
        walk(cas.simplify(cas.parse(r.original)))
        val roots=arguments.flatMap{argument->RobustGraphAnalysisEngine().roots("y=$argument",low,high).map{Candidate(it.point.x,setOf(argument))}}.sortedBy{it.x}
        return roots.fold(mutableListOf()){out,c->
            val previous=out.lastOrNull()
            if(previous!=null&&abs(previous.x-c.x)<1e-6)out[out.lastIndex]=previous.copy(zeros=previous.zeros+c.zeros)else out+=c
            out
        }
    }
    fun sides(r:RuhiDerivativeMath.Result,x:Double,zeros:Set<String> = emptySet()):Sides {
        require(r.order==1)
        val center=requireNotNull(RuhiDerivativeMath.value(r,x)){"The function is undefined at x=$x."}
        val domainStep=1e-7*max(1.0,abs(x))
        require(RuhiDerivativeMath.value(r,x-domainStep)!=null&&RuhiDerivativeMath.value(r,x+domainStep)!=null){"A two-sided derivative requires points on both sides."}
        val cas=SymbolicCasEngine();val original=cas.simplify(cas.parse(r.original));val engine=ExpressionEngine()
        fun branch(direction:Double):Double {
            fun rewrite(e:SymbolicExpression):SymbolicExpression = when(e) {
                is SymbolicExpression.Function->{
                    val children=e.arguments.map(::rewrite)
                    if(e.name!="abs")e.copy(arguments=children)else {
                        val source=cas.render(e.arguments.single());val expression=engine.compile(source)
                        val at=runCatching{expression.eval(mapOf("x" to x))}.getOrNull()?.takeIf{it.isFinite()}
                        val sign=if(at!=null&&at!=0.0&&source !in zeros)if(at>0)1 else -1 else {
                            val samples=listOf(24,28,32,36,40).mapNotNull{k->
                                val sample=x+direction*max(1.0,abs(x))*2.0.pow(-k)
                                if(sample==x)null else runCatching{expression.eval(mapOf("x" to sample))}.getOrNull()?.takeIf{it.isFinite()&&it!=0.0}?.let{if(it>0)1 else -1}
                            }
                            val tail=samples.takeLast(3)
                            if(tail.isEmpty()&&cas.simplify(source).exact=="0")0 else {
                                require(tail.size==3&&tail.distinct().size==1){"The absolute-value branch is not resolved near x=$x."};tail.last()
                            }
                        }
                        when(sign){0->SymbolicExpression.Number(ExactRational.ZERO);1->children.single();else->SymbolicExpression.UnaryMinus(children.single())}
                    }
                }
                is SymbolicExpression.UnaryMinus->e.copy(value=rewrite(e.value))
                is SymbolicExpression.Sum->e.copy(terms=e.terms.map(::rewrite))
                is SymbolicExpression.Product->e.copy(factors=e.factors.map(::rewrite))
                is SymbolicExpression.Power->e.copy(base=rewrite(e.base),exponent=rewrite(e.exponent))
                else->e
            }
            val source=cas.render(cas.simplify(rewrite(original)))
            val expression=engine.compile(source);val branchValue=expression.eval(mapOf("x" to x))
            require(branchValue.isFinite()&&abs(branchValue-center)<=1e-7*max(1.0,abs(center))){"The local branch is not continuous at x=$x."}
            val exact=cas.derivative(source)
            val derivative=if(exact.supported)exact.exact else SymbolicCalculusEngine().differentiate(source).expression
            val slope=engine.compile(derivative).eval(mapOf("x" to x))
            require(slope.isFinite()){"The branch derivative requires additional domain analysis at x=$x."}
            // Verify the analytic branch rather than stepping across a different abs region.
            fun numeric(h:Double):Pair<Double,Double> {
                val left=expression.eval(mapOf("x" to x-h));val right=expression.eval(mapOf("x" to x+h))
                require(left.isFinite()&&right.isFinite()){"The local branch is undefined nearby."}
                return (branchValue-left)/h to (right-branchValue)/h
            }
            val h=1e-4*max(1.0,abs(x));val coarse=numeric(h);val fine=numeric(h/2)
            require(abs(2*fine.first-coarse.first-slope)<=2e-5*max(1.0,abs(slope))&&abs(2*fine.second-coarse.second-slope)<=2e-5*max(1.0,abs(slope))){"The analytic branch slope is not numerically verified."}
            return slope
        }
        return Sides(branch(-1.0),branch(1.0))
    }
}
