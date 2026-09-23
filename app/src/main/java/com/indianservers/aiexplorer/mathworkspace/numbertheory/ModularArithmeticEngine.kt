package com.indianservers.aiexplorer.mathworkspace.numbertheory

import java.math.BigInteger

object ModularArithmeticEngine {
    fun normalize(value: BigInteger, modulus: BigInteger): BigInteger { require(modulus > BigInteger.ZERO); return value.mod(modulus) }
    fun add(a: BigInteger, b: BigInteger, modulus: BigInteger) = (a + b).mod(positive(modulus))
    fun subtract(a: BigInteger, b: BigInteger, modulus: BigInteger) = (a - b).mod(positive(modulus))
    fun multiply(a: BigInteger, b: BigInteger, modulus: BigInteger) = (a * b).mod(positive(modulus))
    fun power(base: BigInteger, exponent: BigInteger, modulus: BigInteger): BigInteger {
        require(exponent.signum() >= 0) { "Exponent must be non-negative" }
        return base.modPow(exponent, positive(modulus))
    }
    fun inverse(value: BigInteger, modulus: BigInteger): BigInteger? {
        val m = positive(modulus)
        if (m == BigInteger.ONE) return BigInteger.ZERO
        return runCatching { value.mod(m).modInverse(m) }.getOrNull()
    }
    private fun positive(modulus: BigInteger) = modulus.also { require(it > BigInteger.ZERO) { "Modulus must be positive" } }
}
