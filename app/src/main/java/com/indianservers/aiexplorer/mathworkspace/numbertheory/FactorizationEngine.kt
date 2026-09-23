package com.indianservers.aiexplorer.mathworkspace.numbertheory

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.math.BigInteger
import kotlin.coroutines.coroutineContext

object FactorizationEngine {
    private val two = BigInteger.TWO
    suspend fun factor(n: BigInteger): Factorization = withContext(Dispatchers.Default) {
        require(n != BigInteger.ZERO) { "Zero has infinitely many divisors" }
        val found = mutableListOf<BigInteger>()
        split(n.abs(), found)
        Factorization(n.signum(), found.groupingBy { it }.eachCount().toSortedMap())
    }

    private suspend fun split(n: BigInteger, out: MutableList<BigInteger>) {
        coroutineContext.ensureActive()
        if (n == BigInteger.ONE) return
        if (n.isProbablePrime(100)) { out += n; return }
        val divisor = rho(n)
        split(divisor, out); split(n / divisor, out)
    }

    private suspend fun rho(n: BigInteger): BigInteger {
        if (n.mod(two) == BigInteger.ZERO) return two
        var c = BigInteger.ONE
        while (true) {
            coroutineContext.ensureActive()
            var x = BigInteger.TWO; var y = BigInteger.TWO; var d = BigInteger.ONE
            while (d == BigInteger.ONE) {
                coroutineContext.ensureActive()
                x = (x * x + c).mod(n)
                y = (y * y + c).mod(n); y = (y * y + c).mod(n)
                d = (x - y).abs().gcd(n)
            }
            if (d != n) return d
            c += BigInteger.ONE
        }
    }
}
