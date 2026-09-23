package com.indianservers.aiexplorer.mathworkspace.numbertheory

import java.math.BigInteger

object CongruenceEngine {
    fun solveLinear(a: BigInteger, b: BigInteger, modulus: BigInteger): LinearCongruenceResult {
        require(modulus > BigInteger.ZERO) { "Modulus must be positive" }
        val g = a.abs().gcd(modulus)
        if (b.mod(g) != BigInteger.ZERO) return LinearCongruenceResult(g, emptyList(), modulus, false)
        require(g <= BigInteger.valueOf(10_000)) { "This congruence has $g residue solutions; reduce the modulus to display them (limit 10,000)" }
        val reduced = modulus / g
        val x0 = if (reduced == BigInteger.ONE) BigInteger.ZERO else
            ((a / g).mod(reduced).modInverse(reduced) * (b / g)).mod(reduced)
        val roots = (0 until g.intValueExact()).map { x0 + reduced * BigInteger.valueOf(it.toLong()) }
        return LinearCongruenceResult(g, roots, modulus, true)
    }

    fun chineseRemainder(congruences: List<Pair<BigInteger, BigInteger>>): CrtResult? {
        require(congruences.all { it.second > BigInteger.ZERO }) { "Moduli must be positive" }
        var x = BigInteger.ZERO; var modulus = BigInteger.ONE
        for ((rawResidue, nextModulus) in congruences) {
            val residue = rawResidue.mod(nextModulus)
            val g = modulus.gcd(nextModulus); val difference = residue - x
            if (difference.mod(g) != BigInteger.ZERO) return null
            val reducedNext = nextModulus / g
            val t = if (reducedNext == BigInteger.ONE) BigInteger.ZERO else
                ((difference / g) * (modulus / g).mod(reducedNext).modInverse(reducedNext)).mod(reducedNext)
            val combinedModulus = modulus * reducedNext
            x = (x + modulus * t).mod(combinedModulus)
            modulus = combinedModulus
        }
        return CrtResult(x, modulus)
    }
}
