package com.indianservers.aiexplorer.features.numbertheory.visualproofs.calculation

import com.indianservers.aiexplorer.features.numbertheory.visualproofs.domain.NumberTheoryProofEvidence

internal object NumberTheoryPhase3Evidence {
    fun build(topicId: String, p: Map<String, Int>): NumberTheoryProofEvidence? = when (topicId) {
        "modular-addition" -> modularAddition(p.getValue("a"), p.getValue("b"), p.getValue("modulus"))
        "modular-multiplication" -> modularMultiplication(p.getValue("a"), p.getValue("b"), p.getValue("modulus"))
        "negative-modulo" -> negativeModulo(p.getValue("value"), p.getValue("modulus"))
        "remainder-classes" -> remainderClasses(p.getValue("value"), p.getValue("modulus"))
        "exponent-product" -> exponentProduct(p.getValue("base"), p.getValue("m"), p.getValue("n"))
        "exponent-quotient" -> exponentQuotient(p.getValue("base"), p.getValue("m"), p.getValue("n"))
        "power-of-power" -> powerOfPower(p.getValue("base"), p.getValue("m"), p.getValue("n"))
        "zero-exponent" -> zeroExponent(p.getValue("base"))
        "negative-exponent" -> negativeExponent(p.getValue("base"), p.getValue("n"))
        "perfect-numbers" -> perfectNumber(p.getValue("value"))
        else -> null
    }

    private fun modularAddition(a: Int, b: Int, modulus: Int): NumberTheoryProofEvidence {
        val first = NumberTheoryMath.normalizedMod(a, modulus)
        val result = NumberTheoryMath.normalizedMod(a + b, modulus)
        val reducedResult = NumberTheoryMath.normalizedMod(first + NumberTheoryMath.normalizedMod(b, modulus), modulus)
        return proof(
            mapOf("first move" to a.toLong(), "second move" to b.toLong(), "first endpoint" to first.toLong(), "full sum" to (a + b).toLong(), "remainder" to result.toLong(), "modulus" to modulus.toLong()),
            mapOf("formula" to "($a+$b) mod $modulus = $result", "prediction" to result.toString(), "reasoningStatus" to "General proof by removing complete turns"),
            sequence = listOf(0L, first.toLong(), result.toLong()),
            holds = result == reducedResult,
            description = "On a $modulus-position clock, move $a steps to $first and then $b more steps to $result. Reducing either move first reaches the same endpoint.",
        )
    }

    private fun modularMultiplication(a: Int, b: Int, modulus: Int): NumberTheoryProofEvidence {
        val positions = (0..b).map { NumberTheoryMath.normalizedMod(a * it, modulus).toLong() }
        val result = NumberTheoryMath.normalizedMod(a * b, modulus)
        val reduced = NumberTheoryMath.normalizedMod(NumberTheoryMath.normalizedMod(a, modulus) * b, modulus)
        return proof(
            mapOf("jump size" to a.toLong(), "jump count" to b.toLong(), "full product" to (a * b).toLong(), "remainder" to result.toLong(), "modulus" to modulus.toLong()),
            mapOf("formula" to "$a×$b mod $modulus = $result", "prediction" to result.toString(), "reasoningStatus" to "General proof from repeated addition"),
            sequence = positions,
            holds = positions.last() == result.toLong() && result == reduced,
            description = "$b repeated jumps of $a visit ${positions.joinToString()} on a $modulus-position clock and finish at $result.",
        )
    }

    private fun negativeModulo(value: Int, modulus: Int): NumberTheoryProofEvidence {
        val remainder = NumberTheoryMath.normalizedMod(value, modulus)
        val equivalent = value + ((-value / modulus) + 2) * modulus
        return proof(
            mapOf("integer" to value.toLong(), "modulus" to modulus.toLong(), "remainder" to remainder.toLong(), "equivalent positive" to equivalent.toLong()),
            mapOf("formula" to "$value ≡ $remainder (mod $modulus)", "prediction" to remainder.toString(), "reasoningStatus" to "General normalization proof"),
            sequence = (value..equivalent step modulus).map(Int::toLong),
            holds = remainder in 0 until modulus && (value - remainder) % modulus == 0,
            description = "Moving backward to $value lands at normalized position $remainder. Adding complete turns of $modulus produces equivalent labels ${sequence(value, equivalent, modulus)}.",
        )
    }

    private fun remainderClasses(value: Int, modulus: Int): NumberTheoryProofEvidence {
        val remainder = NumberTheoryMath.normalizedMod(value, modulus)
        val members = (-3..3).map { remainder.toLong() + it * modulus }
        return proof(
            mapOf("integer" to value.toLong(), "modulus" to modulus.toLong(), "class label" to remainder.toLong(), "visible members" to members.size.toLong()),
            mapOf("formula" to "[$value] mod $modulus = class $remainder", "prediction" to remainder.toString(), "reasoningStatus" to "General proof from the division algorithm"),
            sequence = members,
            holds = members.all { NumberTheoryMath.normalizedMod(it.toInt(), modulus) == remainder },
            description = "$value belongs to remainder class $remainder modulo $modulus. Visible members ${members.joinToString()} differ by whole multiples of $modulus.",
        )
    }

    private fun exponentProduct(base: Int, m: Int, n: Int): NumberTheoryProofEvidence {
        val left = power(base, m)
        val right = power(base, n)
        val joined = power(base, m + n)
        return exponentProof(
            base, m, n,
            values = mapOf("base" to base.toLong(), "first factors" to m.toLong(), "second factors" to n.toLong(), "joined factors" to (m + n).toLong(), "value" to joined),
            formula = "$base^$m × $base^$n = $base^${m + n}",
            result = m + n,
            holds = left * right == joined,
            description = "A chain of $m base-$base factors joins a chain of $n factors. The combined ${m + n} factors have value $joined.",
        )
    }

    private fun exponentQuotient(base: Int, m: Int, n: Int): NumberTheoryProofEvidence {
        val numerator = power(base, m)
        val denominator = power(base, n)
        val remaining = m - n
        val result = power(base, remaining)
        return exponentProof(
            base, m, n,
            values = mapOf("base" to base.toLong(), "numerator factors" to m.toLong(), "cancelled pairs" to n.toLong(), "remaining factors" to remaining.toLong(), "value" to result),
            formula = "$base^$m ÷ $base^$n = $base^$remaining",
            result = remaining,
            holds = numerator / denominator == result && numerator % denominator == 0L,
            description = "$n denominator factors cancel $n of the $m numerator factors, leaving $remaining base-$base factors with value $result.",
        )
    }

    private fun powerOfPower(base: Int, m: Int, n: Int): NumberTheoryProofEvidence {
        val factorCount = m * n
        val expanded = power(power(base, m).toInt(), n)
        val flattened = power(base, factorCount)
        return exponentProof(
            base, m, n,
            values = mapOf("base" to base.toLong(), "factors per group" to m.toLong(), "groups" to n.toLong(), "total factors" to factorCount.toLong(), "value" to flattened),
            formula = "($base^$m)^$n = $base^$factorCount",
            result = factorCount,
            holds = expanded == flattened,
            description = "$n groups each contain $m base-$base factors. Opening them gives $factorCount factors with value $flattened.",
        )
    }

    private fun zeroExponent(base: Int): NumberTheoryProofEvidence {
        val ordinaryPower = power(base, 3)
        return exponentProof(
            base, 3, 3,
            values = mapOf("base" to base.toLong(), "cancelled factors" to 3L, "remaining factors" to 0L, "value" to 1L),
            formula = "$base^3 ÷ $base^3 = $base^0 = 1",
            result = 0,
            holds = ordinaryPower / ordinaryPower == 1L,
            description = "Three equal numerator and denominator factors of nonzero base $base cancel completely. The empty product is one, so $base to exponent zero is one.",
        )
    }

    private fun negativeExponent(base: Int, n: Int): NumberTheoryProofEvidence {
        val denominator = power(base, n)
        return exponentProof(
            base, 0, n,
            values = mapOf("base" to base.toLong(), "negative exponent" to (-n).toLong(), "denominator factors" to n.toLong(), "denominator value" to denominator),
            formula = "$base^-${n} = 1 ÷ $base^$n = 1 ÷ $denominator",
            result = -n,
            holds = denominator > 0,
            description = "Moving $n base-$base factors below the fraction bar gives denominator $denominator. The negative exponent represents its reciprocal.",
        )
    }

    private fun perfectNumber(value: Int): NumberTheoryProofEvidence {
        val factorPairs = NumberTheoryMath.factorPairs(value)
        val divisors = factorPairs.flatMap { listOf(it.first, it.second) }.distinct().sorted()
        val proper = divisors.filter { it < value }
        val sum = proper.sum()
        val classification = when {
            sum == value -> "perfect"
            sum < value -> "deficient"
            else -> "abundant"
        }
        return proof(
            mapOf("number" to value.toLong(), "proper divisor count" to proper.size.toLong(), "proper divisor sum" to sum.toLong()),
            mapOf("formula" to "${proper.joinToString("+")} = $sum", "prediction" to classification, "reasoningStatus" to "Exact classification for the selected number"),
            sequence = proper.map(Int::toLong),
            factorPairs = factorPairs,
            holds = proper.all { value % it == 0 } && proper.distinct().size == proper.size,
            description = "$value has proper divisors ${proper.joinToString()}. Their sum is $sum, so the selected number is $classification.",
        )
    }

    private fun exponentProof(
        base: Int,
        m: Int,
        n: Int,
        values: Map<String, Long>,
        formula: String,
        result: Int,
        holds: Boolean,
        description: String,
    ) = proof(
        values = values,
        labels = mapOf("formula" to formula, "prediction" to result.toString(), "reasoningStatus" to "General proof by factor counting"),
        sequence = List(m.coerceAtLeast(0)) { base.toLong() } + List(n.coerceAtLeast(0)) { base.toLong() },
        holds = holds,
        description = description,
    )

    private fun proof(
        values: Map<String, Long>,
        labels: Map<String, String>,
        sequence: List<Long> = emptyList(),
        factorPairs: List<Pair<Int, Int>> = emptyList(),
        holds: Boolean,
        description: String,
    ) = NumberTheoryProofEvidence(
        values = values,
        labels = labels,
        sequence = sequence,
        factorPairs = factorPairs,
        holds = holds,
        accessibilityDescription = description,
    )

    private fun power(base: Int, exponent: Int): Long =
        (1..exponent.coerceAtLeast(0)).fold(1L) { result, _ -> Math.multiplyExact(result, base.toLong()) }

    private fun sequence(start: Int, end: Int, step: Int) =
        (start..end step step).joinToString()
}
