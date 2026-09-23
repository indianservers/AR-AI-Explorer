package com.indianservers.aiexplorer.mathworkspace.numbertheory

import java.math.BigInteger

object DiophantineEngine {
    fun solve(a: BigInteger, b: BigInteger, c: BigInteger): DiophantineResult {
        val base = EuclideanAlgorithmEngine.solve(a, b)
        val g = base.gcd
        if (g == BigInteger.ZERO) return DiophantineResult(g, BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO, c == BigInteger.ZERO, allIntegerPairs = c == BigInteger.ZERO)
        val possible = c.mod(g) == BigInteger.ZERO
        val scale = if (possible) c / g else BigInteger.ZERO
        return DiophantineResult(g, base.x * scale, base.y * scale, b / g, -a / g, possible)
    }
}
