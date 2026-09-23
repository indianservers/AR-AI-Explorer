package com.indianservers.aiexplorer.mathworkspace.numbertheory

import java.math.BigInteger
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

object PrimeEngine {
    private val TWO=BigInteger.valueOf(2)
    private val UINT64_LIMIT=BigInteger.ONE.shiftLeft(64)
    private val deterministicBases=listOf(2L,325L,9375L,28178L,450775L,9780504L,1795265022L).map(BigInteger::valueOf)

    fun test(n:BigInteger):PrimalityResult {
        if(n<TWO)return PrimalityResult(false,"Integers below 2 are not prime")
        if(n==TWO)return PrimalityResult(true,"Prime")
        if(!n.testBit(0))return PrimalityResult(false,"Even composite")
        smallPrimes.forEach{p->if(n==p)return PrimalityResult(true,"Prime");if(n.mod(p)==BigInteger.ZERO)return PrimalityResult(false,"Composite (divisible by $p)")}
        return if(n<UINT64_LIMIT) PrimalityResult(millerRabin(n,deterministicBases),"Deterministic for unsigned 64-bit integers")
        else PrimalityResult(n.isProbablePrime(100),"Probable-prime test (certainty 100)")
    }

    fun factorSmall(n:BigInteger):Factorization {
        require(n!=BigInteger.ZERO){"Zero has infinitely many divisors"}
        var value=n.abs();val out=sortedMapOf<BigInteger,Int>();var p=BigInteger.TWO
        while(p*p<=value){var e=0;while(value.mod(p)==BigInteger.ZERO){value/=p;e++};if(e>0)out[p]=e;p=if(p==TWO)BigInteger.valueOf(3)else p+TWO}
        if(value>BigInteger.ONE)out[value]=(out[value]?:0)+1
        return Factorization(if(n.signum()<0)-1 else 1,out)
    }

    fun generateRange(from:BigInteger,to:BigInteger,limit:Int=200_000):List<BigInteger>{
        require(from>=BigInteger.TWO){"Range must start at 2 or above"};require(to>=from){"End must be at least start"}
        val width=to-from+BigInteger.ONE;require(width<=BigInteger.valueOf(limit.toLong())){"Choose a range of at most $limit integers"}
        require(to<=BigInteger.valueOf(Int.MAX_VALUE.toLong())){"Range endpoint must be at most ${Int.MAX_VALUE}"}
        val lo=from.toInt();val hi=to.toInt();val composite=BooleanArray(hi-lo+1)
        var p=2;while(p.toLong()*p<=hi){
            val first=maxOf(p.toLong()*p,((lo.toLong()+p-1)/p)*p)
            var multiple=first
            while(multiple<=hi){composite[(multiple-lo).toInt()]=true;multiple+=p}
            p++
        }
        return (lo..hi).asSequence().filter{it>=2&&!composite[it-lo]}.map{BigInteger.valueOf(it.toLong())}.toList()
    }

    suspend fun generateRangeCancellable(from: BigInteger, to: BigInteger, limit: Int = 200_000): List<BigInteger> = withContext(Dispatchers.Default) {
        require(from >= BigInteger.TWO) { "Range must start at 2 or above" }; require(to >= from) { "End must be at least start" }
        val width = to - from + BigInteger.ONE
        require(width <= BigInteger.valueOf(limit.toLong())) { "Choose a range of at most $limit integers" }
        require(to <= BigInteger.valueOf(Int.MAX_VALUE.toLong())) { "Range endpoint must be at most ${Int.MAX_VALUE}" }
        val lo = from.toInt(); val hi = to.toInt(); val composite = BooleanArray(hi - lo + 1)
        var p = 2
        while (p.toLong() * p <= hi) {
            coroutineContext.ensureActive()
            var multiple = maxOf(p.toLong() * p, ((lo.toLong() + p - 1) / p) * p)
            while (multiple <= hi) { composite[(multiple - lo).toInt()] = true; multiple += p }
            p++
        }
        coroutineContext.ensureActive()
        (lo..hi).asSequence().filter { it >= 2 && !composite[it - lo] }.map { BigInteger.valueOf(it.toLong()) }.toList()
    }

    private fun millerRabin(n:BigInteger,bases:List<BigInteger>):Boolean {
        val nMinus=n-BigInteger.ONE;var d=nMinus;var s=0;while(!d.testBit(0)){d=d.shiftRight(1);s++}
        for(raw in bases){val a=raw.mod(n);if(a==BigInteger.ZERO)continue;var x=a.modPow(d,n);if(x==BigInteger.ONE||x==nMinus)continue;var passed=false;repeat(s-1){x=x.multiply(x).mod(n);if(x==nMinus)passed=true};if(!passed)return false}
        return true
    }
    private val smallPrimes=listOf(3,5,7,11,13,17,19,23,29,31,37,41,43,47).map{BigInteger.valueOf(it.toLong())}
}
