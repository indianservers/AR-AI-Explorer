package com.indianservers.aiexplorer.features.numbertheory.visualproofs.calculation

import com.indianservers.aiexplorer.features.numbertheory.visualproofs.domain.NumberTheoryProofEvidence
import kotlin.math.sqrt

internal object NumberTheoryPhase2Evidence {
    fun build(topicId: String, p: Map<String, Int>): NumberTheoryProofEvidence? = when (topicId) {
        "even-sum" -> evenSum(p.getValue("n"))
        "square-odd-difference" -> squareOddDifference(p.getValue("n"))
        "consecutive-integer-sum" -> consecutiveSum(p.getValue("start"), p.getValue("count"))
        "divisibility-2" -> divisibilityByLastDigit(p.getValue("value"), 2)
        "divisibility-9" -> digitSumRule(p.getValue("value"), 9)
        "divisibility-4" -> tailRule(p.getValue("value"), 4, 100)
        "divisibility-8" -> tailRule(p.getValue("value"), 8, 1000)
        "divisibility-5-10" -> fiveAndTen(p.getValue("value"))
        "divisibility-11" -> elevenRule(p.getValue("value"))
        "parity-last-digit" -> parity(p.getValue("value"))
        "multiples-line" -> multiplesLine(p.getValue("a"), p.getValue("b"))
        "gcd-grouping" -> gcdGrouping(p.getValue("a"), p.getValue("b"))
        "gcd-lcm-product" -> gcdLcmProduct(p.getValue("a"), p.getValue("b"))
        "prime-building-blocks" -> primeBlocks(p.getValue("value"))
        "composite-sqrt" -> compositeSquareRoot(p.getValue("value"))
        "euclid-primes" -> euclidPrimes(p.getValue("count"))
        "prime-gaps" -> primeGaps(p.getValue("limit"))
        "twin-primes" -> twinPrimes(p.getValue("limit"))
        else -> null
    }

    private fun evenSum(n: Int): NumberTheoryProofEvidence {
        val sequence = (1..n).map { 2L * it }
        val sum = NumberTheoryMath.evenSum(n)
        return proof(
            values = mapOf("terms" to n.toLong(), "sum" to sum, "rectangle rows" to n.toLong(), "rectangle columns" to (n + 1L)),
            labels = mapOf("formula" to "$sum = $n×${n + 1}", "prediction" to "${2 * (n + 1)}"),
            sequence = sequence,
            holds = sum == sequence.sum(),
            description = "$n even-number rows split into two staircases and form a $n by ${n + 1} rectangle with $sum tiles.",
        )
    }

    private fun squareOddDifference(n: Int): NumberTheoryProofEvidence {
        val outer = n.toLong() * n
        val inner = (n - 1L) * (n - 1L)
        val border = outer - inner
        return proof(
            mapOf("outer square" to outer, "inner square" to inner, "odd border" to border),
            mapOf("formula" to "$n²-${n - 1}²=$border", "prediction" to "${2 * n + 1}"),
            listOf(inner, border, outer),
            holds = border == 2L * n - 1,
            description = "An outer $n by $n square surrounds an ${n - 1} by ${n - 1} square with an L-shaped border of $border tiles.",
        )
    }

    private fun consecutiveSum(start: Int, count: Int): NumberTheoryProofEvidence {
        val sequence = (0 until count).map { start.toLong() + it }
        val sum = sequence.sum()
        val last = sequence.last()
        return proof(
            mapOf("first" to start.toLong(), "last" to last, "terms" to count.toLong(), "sum" to sum),
            mapOf("formula" to "2×$sum = $count×($start+$last)", "prediction" to "${last + 1}"),
            sequence,
            holds = 2 * sum == count.toLong() * (start + last),
            description = "$count consecutive bars from $start to $last balance at their average and total $sum.",
        )
    }

    private fun divisibilityByLastDigit(value: Int, divisor: Int): NumberTheoryProofEvidence {
        val lastDigit = value % 10
        val remainder = value % divisor
        return proof(
            mapOf("number" to value.toLong(), "units digit" to lastDigit.toLong(), "remainder" to remainder.toLong()),
            mapOf(
                "formula" to "$value mod $divisor = $lastDigit mod $divisor",
                "prediction" to yesNo(remainder == 0),
                "reasoningStatus" to "General proof",
            ),
            value.toString().map { it.digitToInt().toLong() },
            holds = remainder == lastDigit % divisor,
            description = "All higher decimal places in $value form complete groups of $divisor. The units digit $lastDigit leaves remainder $remainder.",
        )
    }

    private fun digitSumRule(value: Int, divisor: Int): NumberTheoryProofEvidence {
        val sum = NumberTheoryMath.digitSum(value)
        return proof(
            mapOf("number" to value.toLong(), "digit sum" to sum.toLong(), "number remainder" to (value % divisor).toLong(), "digit-sum remainder" to (sum % divisor).toLong()),
            mapOf("formula" to "$value mod $divisor = $sum mod $divisor", "prediction" to yesNo(value % divisor == 0)),
            value.toString().map { it.digitToInt().toLong() },
            holds = value % divisor == sum % divisor,
            description = "$value and its digit sum $sum leave the same remainder modulo $divisor because every decimal place is one modulo $divisor.",
        )
    }

    private fun tailRule(value: Int, divisor: Int, place: Int): NumberTheoryProofEvidence {
        val tail = value % place
        return proof(
            mapOf("number" to value.toLong(), "retained tail" to tail.toLong(), "number remainder" to (value % divisor).toLong(), "tail remainder" to (tail % divisor).toLong()),
            mapOf("formula" to "$value mod $divisor = $tail mod $divisor", "prediction" to yesNo(value % divisor == 0)),
            listOf((value / place).toLong(), tail.toLong()),
            holds = value % divisor == tail % divisor,
            description = "$value splits into ${value / place} blocks of $place and a tail of $tail. The blocks divide exactly by $divisor.",
        )
    }

    private fun fiveAndTen(value: Int): NumberTheoryProofEvidence {
        val digit = value % 10
        val byFive = value % 5 == 0
        val byTen = value % 10 == 0
        return proof(
            mapOf("number" to value.toLong(), "units digit" to digit.toLong(), "divisible by 5" to byFive.asLong(), "divisible by 10" to byTen.asLong()),
            mapOf("formula" to "units $digit: by 5=${yesNo(byFive)}, by 10=${yesNo(byTen)}", "prediction" to if (byTen) "both" else if (byFive) "five" else "neither"),
            sequence = (0..value.coerceAtMost(100) step 5).map(Int::toLong),
            holds = byFive == (digit == 0 || digit == 5) && byTen == (digit == 0),
            description = "$value ends in $digit. Multiples of five end in zero or five; multiples of ten end only in zero.",
        )
    }

    private fun elevenRule(value: Int): NumberTheoryProofEvidence {
        val digits = value.toString().reversed().map { it.digitToInt() }
        val positive = digits.filterIndexed { index, _ -> index % 2 == 0 }.sum()
        val negative = digits.filterIndexed { index, _ -> index % 2 == 1 }.sum()
        val difference = positive - negative
        return proof(
            mapOf("number" to value.toLong(), "positive tray" to positive.toLong(), "negative tray" to negative.toLong(), "difference" to difference.toLong()),
            mapOf("formula" to "$value ≡ $difference (mod 11)", "prediction" to yesNo(difference % 11 == 0)),
            sequence = digits.reversed().map(Int::toLong),
            holds = NumberTheoryMath.normalizedMod(value, 11) == NumberTheoryMath.normalizedMod(difference, 11),
            description = "Alternating digits of $value form trays $positive and $negative. Their difference $difference has the same remainder modulo eleven.",
        )
    }

    private fun parity(value: Int): NumberTheoryProofEvidence {
        val base = divisibilityByLastDigit(value, 2)
        val even = value % 2 == 0
        return base.copy(
            labels = base.labels + ("formula" to "$value is ${if (even) "even" else "odd"}") + ("prediction" to if (even) "even" else "odd"),
            accessibilityDescription = "Every complete ten in $value pairs evenly. The units digit ${value % 10} is ${if (even) "even" else "odd"}, so the whole number has the same parity.",
        )
    }

    private fun multiplesLine(a: Int, b: Int): NumberTheoryProofEvidence {
        val lcm = NumberTheoryMath.lcm(a, b)
        val common = (lcm..lcm * 5 step lcm).map(Int::toLong)
        return proof(
            mapOf("jump A" to a.toLong(), "jump B" to b.toLong(), "first shared landing" to lcm.toLong()),
            mapOf("formula" to "lcm($a,$b)=$lcm", "prediction" to lcm.toString()),
            common,
            holds = common.all { it % a == 0L && it % b == 0L },
            description = "Jumps of $a and $b first share the positive landing $lcm, then meet at ${common.drop(1).joinToString()}.",
        )
    }

    private fun gcdGrouping(a: Int, b: Int): NumberTheoryProofEvidence {
        val gcd = NumberTheoryMath.gcd(a, b)
        val commonDivisors = (1..minOf(a, b)).filter { a % it == 0 && b % it == 0 }
        return proof(
            mapOf("red counters" to a.toLong(), "blue counters" to b.toLong(), "largest group" to gcd.toLong(), "red groups" to (a / gcd).toLong(), "blue groups" to (b / gcd).toLong()),
            mapOf("formula" to "gcd($a,$b)=$gcd", "prediction" to gcd.toString()),
            commonDivisors.map(Int::toLong),
            holds = commonDivisors.last() == gcd,
            description = "$a red and $b blue counters form complete equal groups for sizes ${commonDivisors.joinToString()}; the greatest size is $gcd.",
        )
    }

    private fun gcdLcmProduct(a: Int, b: Int): NumberTheoryProofEvidence {
        val gcd = NumberTheoryMath.gcd(a, b)
        val lcm = NumberTheoryMath.lcm(a, b)
        return proof(
            mapOf("a" to a.toLong(), "b" to b.toLong(), "gcd" to gcd.toLong(), "lcm" to lcm.toLong(), "each product" to a.toLong() * b),
            mapOf("formula" to "$gcd×$lcm = $a×$b", "prediction" to (a.toLong() * b).toString()),
            primeFactors = mergePrimeFactors(a, b),
            holds = gcd.toLong() * lcm == a.toLong() * b,
            description = "The GCD and LCM prime trays combine to the same ${a.toLong() * b} prime-block product as $a times $b.",
        )
    }

    private fun primeBlocks(value: Int): NumberTheoryProofEvidence {
        val factors = NumberTheoryMath.primeFactors(value)
        val rebuilt = factors.entries.fold(1L) { total, (prime, exponent) -> total * prime.power(exponent) }
        return proof(
            mapOf("number" to value.toLong(), "prime blocks" to factors.values.sum().toLong(), "rebuilt" to rebuilt),
            mapOf("formula" to factors.formatFactors(), "prediction" to factors.keys.first().toString()),
            primeFactors = factors,
            holds = rebuilt == value.toLong(),
            description = "$value breaks into ${factors.formatFactors()}. Multiplying those irreducible prime blocks rebuilds $value.",
        )
    }

    private fun compositeSquareRoot(value: Int): NumberTheoryProofEvidence {
        val pairs = NumberTheoryMath.factorPairs(value)
        val composite = pairs.size > 1
        val nontrivialSmall = pairs.drop(1).firstOrNull()?.first
        val boundary = sqrt(value.toDouble())
        val holds = !composite || nontrivialSmall != null && nontrivialSmall <= boundary
        return proof(
            mapOf("number" to value.toLong(), "floor square root" to boundary.toLong(), "is composite" to composite.asLong(), "small factor" to (nontrivialSmall ?: 1).toLong()),
            mapOf(
                "formula" to if (composite) "$nontrivialSmall ≤ √$value" else "$value is prime",
                "prediction" to (nontrivialSmall ?: 1).toString(),
                "reasoningStatus" to "General proof with exact finite example",
            ),
            factorPairs = pairs,
            holds = holds,
            description = if (composite) {
                "$value has factor pair ${pairs.drop(1).first()}; its smaller side $nontrivialSmall does not exceed square root $boundary."
            } else {
                "$value has no nontrivial factor pair, so it is prime. The small-factor claim applies conditionally to composites."
            },
        )
    }

    private fun euclidPrimes(count: Int): NumberTheoryProofEvidence {
        val listed = listOf(2, 3, 5, 7, 11).take(count)
        val product = listed.fold(1L) { total, prime -> total * prime }
        val constructed = (product + 1).toInt()
        val factors = NumberTheoryMath.primeFactors(constructed)
        return proof(
            mapOf("listed primes" to count.toLong(), "product" to product, "product plus one" to constructed.toLong(), "new prime factor" to factors.keys.first().toLong()),
            mapOf(
                "formula" to "${listed.joinToString("×")}+1=$constructed",
                "prediction" to "1",
                "reasoningStatus" to "Proof by contradiction",
                "counterexample" to "$constructed ${if (factors.size == 1 && factors.values.single() == 1) "is prime here, but need not be prime in general" else "is composite, showing product-plus-one need not be prime"}",
            ),
            sequence = listed.map(Int::toLong),
            primeFactors = factors,
            holds = listed.all { constructed % it == 1 },
            description = "The product $product plus one is $constructed. It leaves remainder one for every listed prime ${listed.joinToString()}, and has a new prime factor ${factors.keys.first()}.",
        )
    }

    private fun primeGaps(limit: Int): NumberTheoryProofEvidence {
        val primes = NumberTheoryMath.primesUpTo(limit)
        val gaps = primes.zipWithNext { a, b -> (b - a).toLong() }
        val max = gaps.maxOrNull() ?: 0
        return proof(
            mapOf("range end" to limit.toLong(), "prime count" to primes.size.toLong(), "largest observed gap" to max),
            mapOf("formula" to "gap = next prime - current prime", "prediction" to max.toString(), "reasoningStatus" to "Finite exploration, not a universal theorem"),
            sequence = gaps,
            primes = primes,
            holds = gaps.all { it > 0 },
            description = "There are ${primes.size} primes through $limit. Their exact consecutive gaps are ${gaps.joinToString()}; the largest visible gap is $max.",
        )
    }

    private fun twinPrimes(limit: Int): NumberTheoryProofEvidence {
        val primes = NumberTheoryMath.primesUpTo(limit)
        val pairs = primes.zipWithNext().filter { (a, b) -> b - a == 2 }
        return proof(
            mapOf("range end" to limit.toLong(), "prime count" to primes.size.toLong(), "twin pairs" to pairs.size.toLong()),
            mapOf(
                "formula" to pairs.joinToString { "(${it.first},${it.second})" },
                "prediction" to pairs.size.toString(),
                "reasoningStatus" to "Open conjecture: finite evidence only",
                "counterexample" to "This finite list cannot prove that infinitely many twin primes exist.",
            ),
            sequence = pairs.flatMap { listOf(it.first.toLong(), it.second.toLong()) },
            primes = primes,
            holds = pairs.all { (a, b) -> b - a == 2 && a in primes && b in primes },
            description = "Through $limit, exact testing finds ${pairs.size} twin-prime pairs: ${pairs.joinToString()}. Infinitely many twin primes remain unproved.",
        )
    }

    private fun proof(
        values: Map<String, Long>,
        labels: Map<String, String>,
        sequence: List<Long> = emptyList(),
        factorPairs: List<Pair<Int, Int>> = emptyList(),
        primeFactors: Map<Int, Int> = emptyMap(),
        primes: List<Int> = emptyList(),
        holds: Boolean,
        description: String,
    ) = NumberTheoryProofEvidence(
        values = values,
        labels = labels,
        sequence = sequence,
        factorPairs = factorPairs,
        primeFactors = primeFactors,
        primes = primes,
        holds = holds,
        accessibilityDescription = description,
    )

    private fun yesNo(value: Boolean) = if (value) "yes" else "no"
    private fun Boolean.asLong() = if (this) 1L else 0L
    private fun Int.power(exponent: Int) = (1..exponent).fold(1L) { result, _ -> result * this }
    private fun Map<Int, Int>.formatFactors() =
        entries.joinToString(" × ") { (prime, exponent) -> if (exponent == 1) "$prime" else "$prime^$exponent" }

    private fun mergePrimeFactors(a: Int, b: Int): Map<Int, Int> {
        val merged = linkedMapOf<Int, Int>()
        (NumberTheoryMath.primeFactors(a).entries + NumberTheoryMath.primeFactors(b).entries).forEach { (prime, exponent) ->
            merged[prime] = merged.getOrDefault(prime, 0) + exponent
        }
        return merged
    }
}
