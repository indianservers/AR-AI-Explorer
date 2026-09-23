package com.indianservers.aiexplorer.mathworkspace.numbertheory

import java.math.BigInteger

object EuclideanAlgorithmEngine {
    fun solve(a: BigInteger, b: BigInteger): EuclideanResult {
        var oldR = a.abs(); var r = b.abs()
        var oldS = BigInteger.ONE; var s = BigInteger.ZERO
        var oldT = BigInteger.ZERO; var t = BigInteger.ONE
        val steps = mutableListOf<EuclideanStep>()
        while (r != BigInteger.ZERO) {
            val q = oldR / r; val next = oldR % r
            steps += EuclideanStep(oldR, r, q, next)
            oldR = r; r = next
            val ns = oldS - q * s; oldS = s; s = ns
            val nt = oldT - q * t; oldT = t; t = nt
        }
        val x = if (a.signum() < 0) -oldS else oldS
        val y = if (b.signum() < 0) -oldT else oldT
        val lcm = if (oldR == BigInteger.ZERO) BigInteger.ZERO else (a / oldR * b).abs()
        return EuclideanResult(oldR, lcm, x, y, steps)
    }
}
