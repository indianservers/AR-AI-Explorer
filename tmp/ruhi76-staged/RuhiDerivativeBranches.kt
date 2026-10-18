package com.indianservers.aiexplorer.assistant.workspace

import com.indianservers.aiexplorer.core.*
import kotlin.math.*

/** Local real branches of absolute values and even-power radicals; original domain stays authoritative. */
internal object RuhiDerivativeBranches {
    data class Sides(val left:Double,val right:Double) {
        val agree get()=abs(left-right)<=1e-9*max(1e-300,max(abs(left),abs(right)))
        val stationary get()=agree&&left==0.0&&right==0.0
    }
    data class Candidate(val x:Double,val zeros:Set<String>)
    private data class RealExpression(val tree:SymbolicExpression,val active:Boolean)
    private fun realBranches(source:String):RealExpression {
        val cas=SymbolicCasEngine();var active=false
        fun integer(e:SymbolicExpression):Int? {
            val value=(cas.simplify(e) as? SymbolicExpression.Number)?.value ?: return null
            return if(value.denominator==java.math.BigInteger.ONE&&value.numerator.bitLength()<=7)value.numerator.toInt().takeIf{abs(it)<=64}else null
        }
        fun nonnegative(e:SymbolicExpression):Boolean = when(e) {
            is SymbolicExpression.Number->e.value.numerator.signum()>=0
            is SymbolicExpression.Power->nonnegative(e.base)||(integer(e.exponent)?.let{it%2==0} ?: false)
            is SymbolicExpression.Product->e.factors.all(::nonnegative)
            is SymbolicExpression.Function->e.name in setOf("abs","sqrt","exp")
            else->false
        }
        fun absolute(e:SymbolicExpression):SymbolicExpression {
            if(e is SymbolicExpression.UnaryMinus)return absolute(e.value)
            if(nonnegative(e))return e
            if(e is SymbolicExpression.Power&&integer(e.exponent)!=null)return e.copy(base=absolute(e.base))
            if(e is SymbolicExpression.Product) {
                val numbers=e.factors.filterIsInstance<SymbolicExpression.Number>()
                if(numbers.isNotEmpty()) {
                    val coefficient=numbers.fold(ExactRational.ONE){a,b->a*b.value}.let{if(it.numerator.signum()<0)-it else it}
                    val rest=e.factors.filterNot{it is SymbolicExpression.Number}
                    val remainder=when(rest.size){0->SymbolicExpression.Number(ExactRational.ONE);1->rest.single();else->SymbolicExpression.Product(rest)}
                    return cas.simplify(SymbolicExpression.Product(listOf(SymbolicExpression.Number(coefficient),absolute(remainder))))
                }
            }
            return SymbolicExpression.Function("abs",listOf(e))
        }
        fun rootPower(e:SymbolicExpression):SymbolicExpression? {
            if(e is SymbolicExpression.Power) {
                val exponent=(cas.simplify(e.exponent) as? SymbolicExpression.Number)?.value
                if(exponent!=null&&exponent.denominator==java.math.BigInteger.ONE&&exponent.numerator.bitLength()<=7) {
                    val n=exponent.numerator.toInt()
                    if(n!=0&&n%2==0&&abs(n)<=64)return if(n==2)e.base else SymbolicExpression.Power(e.base,SymbolicExpression.Number(ExactRational.parse((n/2).toString())))
                }
            }
            return null
        }
        fun radical(argument:SymbolicExpression):SymbolicExpression? {
            rootPower(argument)?.let{active=true;return absolute(it)}
            if(argument !is SymbolicExpression.Product)return null
            val factors=argument.factors.map{factor->
                rootPower(factor) ?: (factor as? SymbolicExpression.Number)?.takeIf{it.value.numerator.signum()>0}?.let{SymbolicExpression.Function("sqrt",listOf(it))} ?: return null
            }
            active=true;return absolute(cas.simplify(SymbolicExpression.Product(factors)))
        }
        fun rewrite(e:SymbolicExpression):SymbolicExpression = when(e) {
            is SymbolicExpression.Function->{if(e.name=="abs")active=true;val children=e.arguments.map(::rewrite);when{e.name=="abs"&&children.size==1->absolute(children.single());e.name=="sqrt"&&children.size==1->radical(children.single()) ?: e.copy(arguments=children);else->e.copy(arguments=children)}}
            is SymbolicExpression.Power->{
                val base=rewrite(e.base);val exponent=rewrite(e.exponent)
                val value=(cas.simplify(exponent) as? SymbolicExpression.Number)?.value
                if(value==ExactRational.parse("0.5"))radical(base) ?: e.copy(base=base,exponent=exponent)else e.copy(base=base,exponent=exponent)
            }
            is SymbolicExpression.UnaryMinus->e.copy(value=rewrite(e.value))
            is SymbolicExpression.Sum->e.copy(terms=e.terms.map(::rewrite))
            is SymbolicExpression.Product->e.copy(factors=e.factors.map(::rewrite))
            else->e
        }
        val tree=cas.simplify(rewrite(cas.parse(source)));return RealExpression(tree,active)
    }
    fun derivativeSource(source:String):String {
        val expression=realBranches(source)
        return if(expression.active)SymbolicCasEngine().render(expression.tree)else source
    }
    fun present(r:RuhiDerivativeMath.Result):Boolean {
        if(!Regex("(?i)\\b(?:abs|sqrt)\\s*\\(|\\^").containsMatchIn(r.original))return false
        return realBranches(r.original).active
    }

    fun candidates(r:RuhiDerivativeMath.Result,low:Double,high:Double):List<Candidate> {
        if(!present(r))return emptyList()
        val cas=SymbolicCasEngine();val arguments=linkedMapOf<String,SymbolicExpression>()
        fun walk(e:SymbolicExpression) {when(e) {
            is SymbolicExpression.Function->{if(e.name=="abs")arguments[cas.render(e.arguments.single())]=e.arguments.single();e.arguments.forEach(::walk)}
            is SymbolicExpression.UnaryMinus->walk(e.value)
            is SymbolicExpression.Sum->e.terms.forEach(::walk)
            is SymbolicExpression.Product->e.factors.forEach(::walk)
            is SymbolicExpression.Power->{walk(e.base);walk(e.exponent)}
            else->Unit
        }}
        walk(realBranches(r.original).tree)
        fun zeroEquations(e:SymbolicExpression):List<SymbolicExpression> = when(e) {
            is SymbolicExpression.Number->emptyList()
            is SymbolicExpression.UnaryMinus->zeroEquations(e.value)
            is SymbolicExpression.Product->e.factors.flatMap(::zeroEquations)
            is SymbolicExpression.Power->{val exponent=(cas.simplify(e.exponent) as? SymbolicExpression.Number)?.value;if(exponent==null)listOf(e)else if(exponent.numerator.signum()>0)zeroEquations(e.base)else emptyList()}
            is SymbolicExpression.Function->if(e.name in setOf("sqrt","abs"))zeroEquations(e.arguments.single())else listOf(e)
            else->listOf(e)
        }
        val roots=arguments.flatMap{(argument,tree)->zeroEquations(tree).flatMap{equation->RobustGraphAnalysisEngine().roots("y=${cas.render(equation)}",low,high).map{Candidate(it.point.x,setOf(argument))}}}.sortedBy{it.x}
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
        val cas=SymbolicCasEngine();val original=realBranches(r.original).tree;val engine=ExpressionEngine()
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
            // Preserve shifted factors: expanded polynomials can cancel to a false zero near a flat root.
            val derivative=SymbolicCalculusEngine().differentiate(source).expression
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
