package com.indianservers.aiexplorer.features.numbertheory.visualproofs.calculation

import com.indianservers.aiexplorer.features.numbertheory.visualproofs.data.NumberTheoryVisualProofCatalog
import com.indianservers.aiexplorer.features.numbertheory.visualproofs.domain.EuclideanDivisionStep
import com.indianservers.aiexplorer.features.numbertheory.visualproofs.domain.NumberTheoryProofAction
import com.indianservers.aiexplorer.features.numbertheory.visualproofs.domain.NumberTheoryProofEvidence
import com.indianservers.aiexplorer.features.numbertheory.visualproofs.domain.NumberTheoryProofState
import kotlin.math.abs

object NumberTheoryMath {
    fun naturalSum(n: Int): Long {
        require(n in 1..100_000)
        return n.toLong() * (n + 1) / 2
    }

    fun oddSum(n: Int): Long {
        require(n in 1..100_000)
        return n.toLong() * n
    }

    fun evenSum(n: Int): Long {
        require(n in 1..100_000)
        return n.toLong() * (n + 1)
    }

    fun arithmeticSum(first: Int, difference: Int, count: Int): Long {
        require(count in 1..100_000)
        val last = first.toLong() + (count - 1L) * difference
        return count.toLong() * (first + last) / 2
    }

    fun digitSum(value: Int): Int = abs(value).toString().sumOf { it.digitToInt() }

    fun factorPairs(value: Int): List<Pair<Int, Int>> {
        require(value in 1..1_000_000)
        return (1..kotlin.math.sqrt(value.toDouble()).toInt()).filter { value % it == 0 }.map { it to value / it }
    }

    tailrec fun gcd(a: Int, b: Int): Int = if (b == 0) abs(a) else gcd(b, a % b)

    fun lcm(a: Int, b: Int): Int {
        require(a != 0 && b != 0)
        return abs(a / gcd(a, b) * b)
    }

    fun euclideanSteps(a: Int, b: Int): List<EuclideanDivisionStep> {
        require(a > 0 && b > 0)
        var dividend = maxOf(a, b)
        var divisor = minOf(a, b)
        val output = mutableListOf<EuclideanDivisionStep>()
        while (divisor != 0) {
            val quotient = dividend / divisor
            val remainder = dividend % divisor
            output += EuclideanDivisionStep(dividend, divisor, quotient, remainder)
            dividend = divisor
            divisor = remainder
        }
        return output
    }

    fun primeFactors(value: Int): Map<Int, Int> {
        require(value >= 2)
        var remaining = value
        var divisor = 2
        val output = linkedMapOf<Int, Int>()
        while (divisor.toLong() * divisor <= remaining) {
            while (remaining % divisor == 0) {
                output[divisor] = output.getOrDefault(divisor, 0) + 1
                remaining /= divisor
            }
            divisor = if (divisor == 2) 3 else divisor + 2
        }
        if (remaining > 1) output[remaining] = output.getOrDefault(remaining, 0) + 1
        return output
    }

    fun primesUpTo(limit: Int): List<Int> {
        require(limit in 2..100_000)
        val composite = BooleanArray(limit + 1)
        var p = 2
        while (p * p <= limit) {
            if (!composite[p]) {
                var multiple = p * p
                while (multiple <= limit) {
                    composite[multiple] = true
                    multiple += p
                }
            }
            p++
        }
        return (2..limit).filterNot { composite[it] }
    }

    fun normalizedMod(value: Int, modulus: Int): Int {
        require(modulus >= 2)
        return ((value % modulus) + modulus) % modulus
    }
}

class NumberTheoryVisualProofEngine {
    fun start(topicId: String): NumberTheoryProofState {
        val topic = NumberTheoryVisualProofCatalog.topic(topicId)
            ?: return NumberTheoryProofState.Error("Unknown Number Theory proof.")
        if (topic.completedInPhase > 3) {
            return NumberTheoryProofState.Error("This proof is scheduled for Phase ${topic.completedInPhase}.")
        }
        val parameters = topic.parameters.associate { it.key to it.initial }
        return NumberTheoryProofState.Ready(topic, 0, parameters, evidence(topic.id, parameters))
    }

    fun reduce(state: NumberTheoryProofState, action: NumberTheoryProofAction): NumberTheoryProofState {
        if (state !is NumberTheoryProofState.Ready) return state
        return when (action) {
            NumberTheoryProofAction.Next -> {
                val next = (state.stepIndex + 1).coerceAtMost(state.topic.steps.lastIndex)
                val replayFinished = state.replayTarget?.let { next >= it } == true
                state.copy(
                    stepIndex = next,
                    completed = next == state.topic.steps.lastIndex,
                    playing = state.playing && next < state.topic.steps.lastIndex && !replayFinished,
                    replayTarget = if (replayFinished) null else state.replayTarget,
                )
            }
            NumberTheoryProofAction.Previous -> state.copy(stepIndex = (state.stepIndex - 1).coerceAtLeast(0), completed = false, playing = false, replayTarget = null)
            NumberTheoryProofAction.Reset -> {
                val initial = state.topic.parameters.associate { it.key to it.initial }
                state.copy(stepIndex = 0, parameters = initial, evidence = evidence(state.topic.id, initial), formulaRevealed = false, prediction = "", predictionCorrect = null, playing = false, replayTarget = null, completed = false)
            }
            NumberTheoryProofAction.RevealFormula -> state.copy(formulaRevealed = true)
            NumberTheoryProofAction.TogglePlaying -> state.copy(playing = !state.playing && !state.reducedMotion, replayTarget = null)
            NumberTheoryProofAction.ReplayStep -> {
                if (state.stepIndex == 0 || state.reducedMotion) {
                    state.copy(playing = false, replayTarget = null)
                } else {
                    state.copy(stepIndex = state.stepIndex - 1, playing = true, replayTarget = state.stepIndex)
                }
            }
            is NumberTheoryProofAction.SelectSection -> state.copy(section = action.section)
            is NumberTheoryProofAction.SelectMode -> state.copy(
                mode = action.mode,
                playing = action.mode == com.indianservers.aiexplorer.features.numbertheory.visualproofs.domain.NumberTheoryProofMode.AutoPlay && !state.reducedMotion,
                replayTarget = null,
            )
            is NumberTheoryProofAction.SetReducedMotion -> state.copy(
                reducedMotion = action.enabled,
                playing = if (action.enabled) false else state.playing,
                replayTarget = if (action.enabled) null else state.replayTarget,
            )
            is NumberTheoryProofAction.SubmitPrediction -> {
                val expected = state.evidence.labels["prediction"].orEmpty().trim().lowercase()
                state.copy(prediction = action.answer, predictionCorrect = action.answer.trim().lowercase() == expected)
            }
            is NumberTheoryProofAction.UpdateParameter -> {
                val definition = state.topic.parameters.firstOrNull { it.key == action.key } ?: return state
                val updated = state.parameters + (action.key to action.value.coerceIn(definition.minimum, definition.maximum))
                state.copy(parameters = updated, evidence = evidence(state.topic.id, updated), prediction = "", predictionCorrect = null, playing = false, replayTarget = null, completed = false)
            }
        }
    }

    fun evidence(topicId: String, p: Map<String, Int>): NumberTheoryProofEvidence = when (topicId) {
        "natural-sum" -> {
            val n = p.getValue("n")
            val sum = NumberTheoryMath.naturalSum(n)
            NumberTheoryProofEvidence(
                values = mapOf("n" to n.toLong(), "staircase dots" to sum, "doubled dots" to 2 * sum, "rectangle dots" to n.toLong() * (n + 1)),
                labels = mapOf("formula" to "$sum = $n×${n + 1}÷2", "prediction" to "${n + 1}"),
                sequence = (1..n).map(Int::toLong),
                holds = 2 * sum == n.toLong() * (n + 1),
                accessibilityDescription = "A staircase with $n rows contains $sum dots. Two copies form a $n by ${n + 1} rectangle.",
            )
        }
        "odd-sum" -> {
            val n = p.getValue("n")
            val sum = NumberTheoryMath.oddSum(n)
            NumberTheoryProofEvidence(mapOf("side" to n.toLong(), "tiles" to sum, "latest layer" to (2L * n - 1)), mapOf("formula" to "${(1..n).joinToString("+") { (2 * it - 1).toString() }} = $n²", "prediction" to "${2 * n + 1}"), (1..n).map { (2L * it - 1) }, holds = sum == (1..n).sumOf { 2L * it - 1 }, accessibilityDescription = "$n odd-number layers form an $n by $n square with $sum tiles.")
        }
        "triangular-numbers" -> evidence("natural-sum", p)
        "consecutive-squares" -> {
            val n = p.getValue("n")
            val difference = (n + 1L) * (n + 1) - n.toLong() * n
            NumberTheoryProofEvidence(mapOf("inner square" to n.toLong() * n, "outer square" to (n + 1L) * (n + 1), "border" to difference), mapOf("formula" to "(${n + 1})²−$n²=$difference", "prediction" to "${2 * n + 1}"), listOf(n.toLong() * n, difference), holds = difference == 2L * n + 1, accessibilityDescription = "Growing an $n by $n square adds a border of $difference tiles.")
        }
        "arithmetic-sum" -> {
            val first = p.getValue("a")
            val d = p.getValue("d")
            val n = p.getValue("n")
            val sequence = (0 until n).map { first.toLong() + it * d }
            val sum = NumberTheoryMath.arithmeticSum(first, d, n)
            NumberTheoryProofEvidence(mapOf("first" to first.toLong(), "last" to sequence.last(), "terms" to n.toLong(), "sum" to sum), mapOf("formula" to "$sum = $n(${first}+${sequence.last()})÷2", "prediction" to "${first + n * d}"), sequence, holds = sum == sequence.sum(), accessibilityDescription = "$n bars from $first to ${sequence.last()} pair to a constant total; their sum is $sum.")
        }
        "divisibility-3" -> {
            val value = p.getValue("value")
            val digitSum = NumberTheoryMath.digitSum(value)
            NumberTheoryProofEvidence(mapOf("number" to value.toLong(), "digit sum" to digitSum.toLong(), "number remainder" to (value % 3).toLong(), "digit-sum remainder" to (digitSum % 3).toLong()), mapOf("formula" to "$value mod 3 = $digitSum mod 3", "prediction" to if (value % 3 == 0) "yes" else "no"), value.toString().map { it.digitToInt().toLong() }, holds = value % 3 == digitSum % 3, accessibilityDescription = "$value and its digit sum $digitSum leave the same remainder when divided by three.")
        }
        "factor-rectangles" -> {
            val value = p.getValue("value")
            val pairs = NumberTheoryMath.factorPairs(value)
            NumberTheoryProofEvidence(mapOf("tiles" to value.toLong(), "factor pairs" to pairs.size.toLong()), mapOf("formula" to pairs.joinToString { "${it.first}×${it.second}" }, "prediction" to pairs.size.toString()), factorPairs = pairs, holds = pairs.all { it.first * it.second == value }, accessibilityDescription = "$value tiles form ${pairs.size} distinct rectangles: ${pairs.joinToString { "${it.first} by ${it.second}" }}.")
        }
        "lcm-cycles" -> {
            val a = p.getValue("a")
            val b = p.getValue("b")
            val lcm = NumberTheoryMath.lcm(a, b)
            NumberTheoryProofEvidence(mapOf("cycle A" to a.toLong(), "cycle B" to b.toLong(), "first alignment" to lcm.toLong()), mapOf("formula" to "lcm($a,$b)=$lcm", "prediction" to lcm.toString()), sequence = (lcm..lcm * 4 step lcm).map(Int::toLong), holds = lcm % a == 0 && lcm % b == 0, accessibilityDescription = "Cycles of $a and $b first align at step $lcm.")
        }
        "euclidean-algorithm" -> {
            val a = p.getValue("a")
            val b = p.getValue("b")
            val steps = NumberTheoryMath.euclideanSteps(a, b)
            val gcd = NumberTheoryMath.gcd(a, b)
            NumberTheoryProofEvidence(mapOf("a" to a.toLong(), "b" to b.toLong(), "gcd" to gcd.toLong(), "divisions" to steps.size.toLong()), mapOf("formula" to "gcd($a,$b)=$gcd", "prediction" to gcd.toString()), euclideanSteps = steps, holds = steps.last().divisor == gcd && steps.last().remainder == 0, accessibilityDescription = steps.joinToString(". ") { "${it.dividend} equals ${it.quotient} times ${it.divisor} plus ${it.remainder}" } + ". The final nonzero divisor is $gcd.")
        }
        "unique-factorization" -> {
            val value = p.getValue("value")
            val factors = NumberTheoryMath.primeFactors(value)
            val product = factors.entries.fold(1L) { total, entry -> total * entry.key.toLong().let { base -> (1..entry.value).fold(1L) { power, _ -> power * base } } }
            NumberTheoryProofEvidence(mapOf("number" to value.toLong(), "rebuilt product" to product), mapOf("formula" to factors.entries.joinToString(" × ") { if (it.value == 1) "${it.key}" else "${it.key}^${it.value}" }, "prediction" to factors.keys.last().toString()), primeFactors = factors, holds = product == value.toLong(), accessibilityDescription = "$value breaks into prime blocks ${factors.entries.joinToString { "${it.key} repeated ${it.value} times" }}; rebuilding them returns $value.")
        }
        "sieve" -> {
            val limit = p.getValue("limit")
            val primes = NumberTheoryMath.primesUpTo(limit)
            val crossed = (2..limit).filterNot { it in primes }.toSet()
            NumberTheoryProofEvidence(mapOf("limit" to limit.toLong(), "prime count" to primes.size.toLong(), "stop after" to kotlin.math.sqrt(limit.toDouble()).toLong()), mapOf("formula" to "primes ≤ $limit: ${primes.joinToString()}", "prediction" to primes.last().toString()), primes = primes, crossed = crossed, holds = primes.all { NumberTheoryMath.factorPairs(it).size == 1 }, accessibilityDescription = "The sieve up to $limit leaves ${primes.size} primes and crosses ${crossed.size} composite numbers.")
        }
        "modular-clock" -> {
            val value = p.getValue("value")
            val modulus = p.getValue("modulus")
            val remainder = NumberTheoryMath.normalizedMod(value, modulus)
            NumberTheoryProofEvidence(mapOf("integer" to value.toLong(), "modulus" to modulus.toLong(), "remainder" to remainder.toLong()), mapOf("formula" to "$value ≡ $remainder (mod $modulus)", "prediction" to remainder.toString()), sequence = (value - 2 * modulus..value + 2 * modulus step modulus).map(Int::toLong), holds = (value - remainder) % modulus == 0, accessibilityDescription = "$value lands at position $remainder on a $modulus-position clock. Integers differing by $modulus share that position.")
        }
        else -> NumberTheoryPhase2Evidence.build(topicId, p)
            ?: NumberTheoryPhase3Evidence.build(topicId, p)
            ?: NumberTheoryProofEvidence(emptyMap(), emptyMap(), holds = false, accessibilityDescription = "Proof evidence is scheduled for a later phase.")
    }
}
