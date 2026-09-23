package com.indianservers.aiexplorer.mathworkspace.numbertheory

import java.math.BigInteger

/** Small derived views shared by the number-theory canvas and inspector. */
object NumberTheoryEngine {
    fun primeGaps(primes: List<BigInteger>) = primes.zipWithNext { x, y -> y - x }
    fun divisors(factorization: Factorization, limit: Int = 10_000): List<BigInteger> {
        var values = listOf(BigInteger.ONE)
        factorization.factors.forEach { (prime, exponent) ->
            val powers = (0..exponent).map { prime.pow(it) }
            require(values.size.toLong() * powers.size <= limit) { "Too many divisors to display; use the exact divisor count" }
            values = values.flatMap { base -> powers.map { base * it } }
        }
        return values.sorted()
    }
}
