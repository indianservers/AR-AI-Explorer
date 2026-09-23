package com.indianservers.aiexplorer.mathworkspace.numbertheory

import java.math.BigInteger

enum class NumberTheoryMode(val title: String) {
    Prime("Prime Explorer"), Factors("Factors & Divisibility"), Euclidean("Euclidean Algorithm"), Modular("Modular Arithmetic"), Congruences("Congruences"), Diophantine("Diophantine Equations")
}
data class PrimalityResult(val isPrime: Boolean, val certainty: String)
data class Factorization(val sign: Int, val factors: Map<BigInteger, Int>) {
    fun display(): String = buildString { if (sign < 0) append("−1 × "); append(factors.entries.joinToString(" × ") { (p,e) -> if(e==1) p.toString() else "$p^$e" }.ifBlank { "1" }) }
    val divisorCount: BigInteger get() = factors.values.fold(BigInteger.ONE) { a,e -> a * BigInteger.valueOf(e.toLong()+1) }
    val divisorSum: BigInteger get() = factors.entries.fold(BigInteger.ONE) { a,(p,e) -> a * (p.pow(e+1)-BigInteger.ONE)/(p-BigInteger.ONE) }
    val properDivisorSum: BigInteger get() = divisorSum - factors.entries.fold(BigInteger.ONE) { a,(p,e) -> a * p.pow(e) }
    val classification: String get() = when(properDivisorSum.compareTo(factors.entries.fold(BigInteger.ONE){a,(p,e)->a*p.pow(e)}) ){0->"Perfect";1->"Abundant";else->"Deficient"}
}
data class EuclideanStep(val dividend: BigInteger,val divisor: BigInteger,val quotient: BigInteger,val remainder: BigInteger)
data class EuclideanResult(val gcd:BigInteger,val lcm:BigInteger,val x:BigInteger,val y:BigInteger,val steps:List<EuclideanStep>)
data class LinearCongruenceResult(val gcd:BigInteger,val residues:List<BigInteger>,val modulus:BigInteger,val hasSolution:Boolean)
data class CrtResult(val residue:BigInteger,val modulus:BigInteger)
data class DiophantineResult(val gcd:BigInteger,val x:BigInteger,val y:BigInteger,val stepX:BigInteger,val stepY:BigInteger,val hasSolution:Boolean,val allIntegerPairs:Boolean=false)
