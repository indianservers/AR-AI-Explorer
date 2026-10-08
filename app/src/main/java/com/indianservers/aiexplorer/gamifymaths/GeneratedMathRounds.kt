package com.indianservers.aiexplorer.gamifymaths

import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow
import kotlin.random.Random

internal const val GeneratedLevelCount = 122
internal enum class RoundInteraction { Answer, BuildSum, BuildPlaceValue, BuildShapes }
internal data class GeneratedRound(
    val topic: String,
    val prompt: String,
    val answer: Double,
    val explanation: String,
    val hint: String,
    val choices: List<String>,
    val interaction: RoundInteraction = RoundInteraction.Answer,
    val pieces: List<Int> = emptyList(),
    val requiredPieces: Int = 0,
    val requiredCurves: Int = 0,
    val operands: List<Double> = emptyList(),
    val weights: Map<Int, Int> = emptyMap(),
    val capacity: Int = 0,
) {
    val signature: String get() = "$prompt|${pieces.sorted().joinToString()}|$weights"
    fun accepts(input: String): Boolean {
        val parts = input.trim().split('/')
        val value = if (parts.size == 2) {
            val denominator = parts[1].trim().toDoubleOrNull() ?: return false
            if (denominator == 0.0) return false
            (parts[0].trim().toDoubleOrNull() ?: return false) / denominator
        } else input.trim().toDoubleOrNull() ?: return false
        // Non-terminating answers are explicitly requested to six decimal places.
        return value.isFinite() && abs(value - answer) < 0.00000051
    }

    fun acceptsPieces(selected: List<Int>): Boolean = when (interaction) {
        RoundInteraction.BuildShapes -> selected.all { it in pieces } && selected.size == requiredPieces &&
            selected.count { it == 0 } == requiredCurves && selected.sum().toDouble() == answer
        RoundInteraction.BuildPlaceValue, RoundInteraction.BuildSum -> selected.sum().toDouble() == answer &&
            (requiredPieces == 0 || selected.size == requiredPieces) &&
            (capacity == 0 || selected.sumOf { weights[it] ?: 0 } <= capacity) &&
            selected.groupingBy { it }.eachCount().all { (value, count) -> count <= pieces.count { it == value } }
        RoundInteraction.Answer -> false
    }
}

internal fun roundNumber(value: Double): String = if (value == value.toLong().toDouble()) value.toLong().toString()
    else String.format(Locale.US, "%.6f", value).trimEnd('0').trimEnd('.')

/** All live worlds use this seedable, offline generator. Templates describe skills, never question banks. */
internal fun generateMathRound(id: String, level: Int, difficulty: Int, seed: Long, choiceCount: Int = 4): GeneratedRound {
    require(level in 0 until GeneratedLevelCount)
    require(difficulty in 0..3 && choiceCount in listOf(4, 6))
    val random = Random(seed)
    val tier = (difficulty + level / 30).coerceAtMost(3)
    val limit = 10 + tier * 12
    fun n(low: Int = 2, high: Int = limit): Int = random.nextInt(low, high + 1)
    val a = n(); val b = n(); val c = n(2, 6 + tier * 2)
    val skill = level % 6
    fun answer(topic: String, prompt: String, value: Double, explanation: String, hint: String,
               operands: List<Double> = emptyList(), step: Double = 1.0): GeneratedRound {
        val tokens = mutableSetOf(roundNumber(value))
        while (tokens.size < choiceCount) {
            val offset = random.nextInt(-choiceCount * 3, choiceCount * 3 + 1)
            tokens.add(roundNumber(value + offset * step))
        }
        return GeneratedRound(topic, prompt, value, explanation, hint, tokens.shuffled(random), operands = operands)
    }
    fun integer(topic: String, prompt: String, value: Int, explanation: String, hint: String,
                operands: List<Int> = emptyList()) = answer(topic, prompt, value.toDouble(), explanation, hint, operands.map(Int::toDouble))
    fun build(topic: String, prompt: String, target: Int, tiles: List<Int>, count: Int = 0,
              kind: RoundInteraction = RoundInteraction.BuildSum, curves: Int = 0): GeneratedRound =
        GeneratedRound(topic, prompt, target.toDouble(), "Check every piece against the target, then add its contribution. The completed construction satisfies every stated condition.",
            "Check the total and the number of pieces separately. Undo a piece if either condition fails.", emptyList(), kind, tiles, count, curves)
    fun combination(total: Int, chosen: Int): Int {
        var result = 1L
        for (i in 1..chosen) result = result * (total - chosen + i) / i
        return result.toInt()
    }
    return when (id) {
        "forge" -> {
            val high = intArrayOf(99,999,9999,99999)[tier]
            val target = n(if(tier==0) 10 else high/10+1, high)
            val places = (0..tier+1).map { 10.0.pow(it).toInt() }
            val tiles = places.flatMap { value -> List(9) { value } }
            build("Place-value workshop", "Build $target using place-value blocks. Use the fewest blocks.", target, tiles,
                target.toString().sumOf { it.digitToInt() }, RoundInteraction.BuildPlaceValue)
        }
        "number-bonds" -> {
            val tiles = List(6 + tier) { n(1) }
            val selected = tiles.shuffled(random).take(2 + tier)
            build("Number partners", "Connect ${selected.size} number tiles to make ${selected.sum()}. Each tile can be used once.", selected.sum(), tiles, selected.size)
        }
        "measure" -> {
            if (skill >= 3) {
                val tiles = List(6 + tier) { n(2, 8 + tier * 3) }
                val solution = tiles.shuffled(random).take(3 + tier)
                build("Bridge workshop", "Span ${solution.sum()} m with exactly ${solution.size} beams. Join them end to end; use each beam once.", solution.sum(), tiles, solution.size)
            } else when (skill) {
                0 -> integer("Perimeter", "A rectangular rescue base is $a m × $b m. Find its perimeter in metres.", 2*(a+b), "Walk all four edges: $a + $b + $a + $b = ${2*(a+b)} metres.", "A perimeter measures the full boundary, not the inside.", listOf(a,b))
                1 -> integer("Area", "A triangular canopy has base ${2*a} m and perpendicular height $b m. Find its area in m².", a*b, "A triangle occupies half the matching rectangle: (${2*a} × $b) ÷ 2 = ${a*b} square metres.", "Use half of base times perpendicular height.", listOf(2*a,b))
                else -> integer("Volume", "A storage box measures $a × $b × $c cm. Find its volume in cm³.", a*b*c, "Multiply all three dimensions: $a × $b × $c = ${a*b*c} cubic centimetres.", "Volume counts layers of area.", listOf(a,b,c))
            }
        }
        "shapes" -> {
            val count = 3 + tier + random.nextInt(3)
            val solution = List(count) { listOf(0,3,4).random(random) }
            val curves = solution.count { it == 0 }
            build("Geometry construction", "Build with $count pieces: ${solution.sum()} straight sides in total and $curves curved pieces. Pieces may repeat.", solution.sum(), listOf(0,3,4), count, RoundInteraction.BuildShapes, curves)
        }
        "kitchen" -> when(skill) {
            0 -> integer("Combine batches", "Combine $a portions and $b portions. How many portions?", a+b, "Combine both quantities: $a + $b = ${a+b} portions in the recipe.", "Combine the two quantities.", listOf(a,b))
            1 -> integer("Portions remaining", "Prepare ${a+b} portions and serve $a. How many remain?", b, "Subtract what was served from the starting amount: ${a+b} − $a = $b.", "Subtract the served quantity.", listOf(a+b,a))
            2 -> integer("Batch scaling", "Each batch needs $a grams. Make $c batches. How many grams?", a*c, "Equal batches require multiplication: $a × $c = ${a*c} grams.", "Multiply the quantity per batch by the batch count.", listOf(a,c))
            3 -> integer("Equal sharing", "Share ${a*c} portions equally between $c tables. Portions per table?", a, "Equal sharing is division: ${a*c} ÷ $c = $a portions for each table.", "Divide the total by the number of tables.", listOf(a*c,c))
            4 -> integer("Order of operations", "The recipe uses $a + $b × $c grams. Calculate the total.", a+b*c, "Multiply first: $b × $c = ${b*c}. Then add $a to get ${a+b*c}.", "Multiplication comes before addition.", listOf(a,b,c))
            else -> { val x=n(101,499); val y=n(101,499); val rx=((x+50)/100)*100; val ry=((y+50)/100)*100
                integer("Estimate", "Round EACH amount to the nearest 100, then add: $x g + $y g.", rx+ry, "Round $x to $rx and $y to $ry. Adding the rounded amounts gives ${rx+ry} grams.", "Round each amount separately before adding.", listOf(x,y)) }
        }
        "fractions" -> {
            val denominator=n(3,12); val numerator=n(1,denominator-1); val multiplier=n(2,7)
            when(skill) {
                0 -> integer("Equivalent fractions", "Complete $numerator/$denominator = ?/${denominator*multiplier}. Enter the numerator.", numerator*multiplier, "The denominator grew by a factor of $multiplier. Multiply the numerator by the same factor: $numerator × $multiplier.", "Apply the same scale factor to numerator and denominator.", listOf(numerator,denominator,multiplier))
                1 -> integer("Common denominators", "Add $numerator/$denominator + $multiplier/$denominator. Enter the numerator over $denominator BEFORE simplifying.", numerator+multiplier, "Equal denominators represent equal-sized pieces. Add the numerators: $numerator + $multiplier = ${numerator+multiplier}.", "Keep the denominator and add the numerators.", listOf(numerator,multiplier,denominator))
                2 -> integer("Subtract fractions", "Subtract ${numerator+multiplier}/$denominator − $numerator/$denominator. Enter the numerator over $denominator BEFORE simplifying.", multiplier, "Subtract the numerators while keeping their shared denominator: ${numerator+multiplier} − $numerator = $multiplier.", "Subtract counts of equal-sized pieces.", listOf(numerator+multiplier,numerator,denominator))
                3 -> integer("Mixed numbers", "Convert $c $numerator/$denominator to an improper fraction. Enter its numerator over $denominator.", c*denominator+numerator, "Each whole contains $denominator parts: $c × $denominator + $numerator = ${c*denominator+numerator}.", "Convert the wholes into denominator-sized parts first.", listOf(c,numerator,denominator))
                else -> { val d=listOf(4,5,10,20,25).random(random); val p=n(1,d-1)
                    integer("Fraction percentage", "Convert $p/$d to a percentage. Enter the number before %.", p*100/d, "Divide $p by $d and multiply by 100. This gives ${p*100/d} percent.", "A percentage counts parts out of one hundred.", listOf(p,d)) }
            }
        }
        "potions", "ratio-rangers" -> when(skill % 3) {
            0 -> integer("Ratio mixing", "Blue:red = $a:$b. Mix ${a*c} ml blue. How many ml red preserve the ratio?", b*c, "Blue was scaled by $c. Scale red by the same factor: $b × $c = ${b*c} ml.", "Find the scale factor from the known ingredient.", listOf(a,b,c))
            1 -> integer("Unit rate", "A mixer fills ${a*c} ml in $c seconds at a constant rate. Find ml per second.", a, "Divide volume by time: ${a*c} ÷ $c = $a ml per second.", "A unit rate is the amount in one unit of time.", listOf(a*c,c))
            else -> integer("Inverse proportion", "$c identical pumps fill a tank in ${a*b} minutes. How long would ${c*b} pumps take?", a, "The new team has $b times as many pumps, so time divides by $b: ${a*b} ÷ $b = $a minutes.", "More identical pumps reduce time in inverse proportion.", listOf(c,a*b,c*b))
        }
        "balance" -> when(skill) {
            0 -> integer("One-step lock", "Solve x + $b = ${a+b}.", a, "Subtract $b from both sides. The remaining value is x = $a; substitution checks the balance.", "Apply the same inverse operation to both sides.", listOf(b,a+b))
            1 -> integer("Two-step lock", "Solve ${c}x + $b = ${c*a+b}.", a, "Subtract $b, then divide by $c: x = $a. Checking gives $c × $a + $b = ${c*a+b}.", "Undo addition before undoing multiplication.", listOf(c,b,c*a+b))
            2 -> integer("Variables on both sides", "Solve ${c+2}x + $b = ${c}x + ${2*a+b}.", a, "Remove ${c}x from both sides, then subtract $b. The result 2x = ${2*a} gives x = $a.", "Collect variable terms on one side and constants on the other.", listOf(c+2,b,c,2*a+b))
            3 -> integer("Inequality boundary", "For ${c}x + $b < ${c*a+b}, the solution is x < ?. Enter the boundary.", a, "Subtract $b and divide by positive $c. The inequality direction stays the same: x < $a.", "Dividing by a positive coefficient keeps the inequality direction.", listOf(c,b,c*a+b))
            4 -> integer("Simultaneous equations", "x + y = ${a+b}, x − y = ${a-b}. Find x.", a, "Add the equations to cancel y. This gives 2x = ${2*a}, so x = $a.", "Add the equations to eliminate one variable.", listOf(a+b,a-b))
            else -> integer("Quadratic lock", "Solve (x − $a)(x + $b) = 0. Enter the POSITIVE root.", a, "A zero product requires one factor to be zero. The roots are $a and ${-b}; the positive root is $a.", "Set each factor equal to zero and check the requested sign.", listOf(a,b))
        }
        "patterns" -> when(skill % 3) {
            0 -> integer("Arithmetic signals", "Use a constant difference: $a, ${a+b}, ${a+2*b}, ${a+3*b}, ?. Find the next term.", a+4*b, "Every transition adds $b. Extend the same rule: ${a+3*b} + $b = ${a+4*b}.", "Compare consecutive terms to find their common difference.", listOf(a,b))
            1 -> integer("Geometric signals", "Use a constant multiplier: $a, ${a*c}, ${a*c*c}, ?. Find the next term.", a*c*c*c, "Every transition multiplies by $c. The next term is ${a*c*c} × $c = ${a*c*c*c}.", "Compare the ratio of consecutive terms.", listOf(a,c))
            else -> integer("Term detective", "For term n = ${c}n + $b, find term $a.", c*a+b, "Substitute the requested position: $c × $a + $b = ${c*a+b}.", "Replace n with the requested term position.", listOf(c,b,a))
        }
        "vectors" -> when(skill % 3) {
            0 -> integer("Flight slope", "From ($a, $b) to (${a+c}, ${b+c*c}), find the slope.", c, "Slope is rise/run: ${c*c} ÷ $c = $c. Both coordinate changes are measured from the first point.", "Divide the change in y by the change in x.", listOf(a,b,c))
            1 -> integer("Navigation midpoint", "Find the x-coordinate of the midpoint of ($a, $b) and (${a+2*c}, ${b+2*a}).", a+c, "Average the two x-coordinates: ($a + ${a+2*c}) ÷ 2 = ${a+c}.", "A midpoint averages corresponding coordinates.", listOf(a,b,c))
            else -> integer("Translation", "Translate ($a, $b) by (${c}, ${-a}). Enter the new y-coordinate.", b-a, "Add the vector's y-component to y: $b + (${-a}) = ${b-a}. The x-component does not change this calculation.", "Add each vector component to its matching coordinate.", listOf(a,b,c))
        }
        "data", "data-story" -> {
            val values=List(5+2*tier) { n(1,limit*2) }.sorted()
            when(skill % 3) {
                0 -> { val sum=values.sum(); answer("Mean evidence", "Data: ${values.joinToString()}. Find the mean (fraction or decimal to 6 places).", sum.toDouble()/values.size, "Add every observation to obtain $sum. Divide this total by ${values.size}: the mean is $sum/${values.size}.", "Include every observation exactly once, then divide by the count.", values.map(Int::toDouble), 1.0/values.size) }
                1 -> integer("Median evidence", "Data: ${values.shuffled(random).joinToString()}. Find the median.", values[values.size/2], "Sort the observations. With ${values.size} values, the middle observation is ${values[values.size/2]}.", "Sort before selecting the middle value.", values)
                else -> integer("Range evidence", "Data: ${values.shuffled(random).joinToString()}. Find the range.", values.last()-values.first(), "Range is maximum minus minimum: ${values.last()} − ${values.first()} = ${values.last()-values.first()}.", "Locate the two extreme values, then subtract.", values)
            }
        }
        "chance" -> {
            val red=n(2,10+tier*5); val blue=n(2,10+tier*5); val total=red+blue
            val draws=n(2,8)
            fun probability(topic:String, prompt:String, value:Double, explanation:String, hint:String, inputs:List<Double>):GeneratedRound {
                val correct=roundNumber(value)
                val choices=mutableSetOf(correct)
                while(choices.size<choiceCount) {
                    val candidate=random.nextInt(0,101)/100.0
                    if(abs(candidate-value)>.000001) choices.add(roundNumber(candidate))
                }
                return GeneratedRound(topic, "$prompt Enter a fraction or decimal to 6 places.", value, explanation, hint, choices.shuffled(random), operands=inputs)
            }
            val trialCount=n(3,8); val wins=n(1,trialCount-1)
            when(level%15) {
                0 -> probability("Chance Explorer", "A bag has $red red and $blue blue counters. Draw one uniformly. Find P(red).", red.toDouble()/total,
                    "There are $red favourable counters among $total equally likely counters. Therefore P(red) = $red/$total.", "Count favourable outcomes and divide by all outcomes.", listOf(red.toDouble(),blue.toDouble()))
                1 -> integer("Spin and Win", "A fair spinner has $total equal sectors, $red marked WIN. Spin ${total*draws} times. What is the expected number of wins?", red*draws,
                    "Expected wins = trials × win probability = ${total*draws} × $red/$total = ${red*draws}. Actual wins may differ.", "Multiply trials by the probability of a win.", listOf(red,blue,draws))
                2 -> probability("Coin Flipper", "Flip a fair coin $trialCount times. Find P(exactly $wins heads).", combination(trialCount,wins)/2.0.pow(trialCount),
                    "Choose $wins head positions among $trialCount flips, then divide by the ${2.0.pow(trialCount).toInt()} equally likely sequences.", "Count the head-position combinations, then divide by all sequences.", listOf(trialCount.toDouble(),wins.toDouble()))
                3 -> probability("Card Picker", "A shuffled deck contains $red star cards and $blue moon cards. Pick one uniformly. Find P(moon).", blue.toDouble()/total,
                    "The deck contains $total cards and $blue favourable moon cards. The probability is $blue/$total.", "Count the requested cards among the entire deck.", listOf(red.toDouble(),blue.toDouble()))
                4 -> probability("Tree Builder", "A bag has $red red and $blue blue counters. Draw two WITHOUT replacement. Find P(both red).", red.toDouble()/total*(red-1)/(total-1),
                    "The first red probability is $red/$total. After a red draw, it becomes ${red-1}/${total-1}. Multiply these conditional probabilities.", "Update both counts after the first draw; it is not replaced.", listOf(red.toDouble(),blue.toDouble()))
                5 -> { val count=n(4,10); val pick=n(2,3); var result=1; repeat(pick){result*=count-it}
                    integer("Permutations Pro", "Arrange $pick different robots chosen from $count robots. How many ordered arrangements?", result,
                        "Multiply the decreasing available robot counts. Order matters, so do not divide by the arrangements of the selected robots.", "The next position has one fewer available robot.", listOf(count,pick)) }
                6 -> { val count=n(4,12); val pick=n(2,3)
                    integer("Combinations Champ", "Choose a team of $pick from $count players. How many unordered teams?", combination(count,pick),
                        "Count ordered selections, then divide by the $pick! orders of each team. This gives ${combination(count,pick)} teams.", "Divide away the repeated orders of the same team.", listOf(count,pick)) }
                7 -> { val p=n(1,4)/5.0; val chance=combination(trialCount,wins)*p.pow(wins)*(1-p).pow(trialCount-wins)
                    probability("Binomial Boost", "An independent trial succeeds with probability ${roundNumber(p)}. Run $trialCount trials. Find P(exactly $wins successes).", chance,
                        "Use C(n,k) × p^k × (1−p)^(n−k). The combination counts the positions of the $wins successes.", "Combine the position count with the success and failure probabilities.", listOf(trialCount.toDouble(),wins.toDouble(),p)) }
                8,9 -> { val z=n(-3,3); val mean=n(20,80); val sd=n(2,10); val x=mean+z*sd
                    integer(if(level%15==8) "Normal Navigator" else "Z-score Quest", "A normal model has mean $mean and standard deviation $sd. Observation $x has what z-score?", z,
                        "Standardize the observation: z = ($x − $mean) ÷ $sd = $z. The sign tells which side of the mean it lies on.", "Subtract the mean, then divide by the standard deviation.", listOf(mean,sd,x)) }
                10 -> { val fifths=n(1,19)
                    integer("Sampling Safari", "A population is ${fifths*5}% supporters. In a random sample of ${draws*20} people, what is the expected supporter count?", fifths*draws,
                    "Multiply sample size by the supporter proportion: ${draws*20} × ${fifths*5}/100 = ${fifths*draws}. A random sample can vary.", "Expected counts use population proportion times sample size.", listOf(fifths*5,draws*20)) }
                else -> {
                    val center=n(10,limit*2); val spread=n(1,8)
                    val values=listOf(center-2*spread,center-spread,center,center+spread,center+2*spread)
                    when(level%15) {
                        11 -> integer("Mean Machine", "Find the mean of ${values.shuffled(random).joinToString()}.", center,
                            "The deviations around $center cancel. The total is ${5*center}; dividing by five gives the mean $center.", "Add all observations and divide by the count.", values)
                        12 -> integer("Median Mission", "Find the median of ${values.shuffled(random).joinToString()}.", center,
                            "Sorting the five observations puts $center in the third, middle position. That observation is the median.", "Sort the observations before choosing the middle.", values)
                        13 -> answer("Standard Deviation Detective", "Find the POPULATION standard deviation of ${values.shuffled(random).joinToString()} (6 decimal places).", kotlin.math.sqrt(2.0)*spread,
                            "The mean is $center. Squared deviations total ${10*spread*spread}; divide by 5, then take the square root of ${2*spread*spread}.", "Average squared deviations using the population count, then take the square root.", values.map(Int::toDouble), .5)
                        else -> integer("Statistics Challenge", "Find the POPULATION variance of ${values.shuffled(random).joinToString()}.", 2*spread*spread,
                            "The mean is $center. The five squared deviations sum to ${10*spread*spread}; divide by the population count 5 to get ${2*spread*spread}.", "Variance is the mean squared deviation, without a square root.", values)
                    }
                }
            }
        }
        "logic", "combinatorics" -> {
            val total=n(4,20+tier*5); val chosen=n(2,3)
            when(skill%3) {
                0 -> integer("Team selection", "Choose $chosen people from $total distinct people. Order does not matter. How many teams?", combination(total,chosen),
                    "Count ordered selections and divide by the $chosen! orders of each team. This gives ${combination(total,chosen)} distinct teams.", "Use combinations when rearranging a team does not make a new team.", listOf(total,chosen))
                1 -> { var product=1; repeat(chosen) { product*=total-it }
                    integer("Ordered code", "Choose $chosen different symbols from $total symbols for an ordered code. No repeats. How many codes?", product,
                        "The available choices decrease after each selection. Multiply those counts to obtain $product distinct ordered codes.", "Order matters and used symbols are removed.", listOf(total,chosen)) }
                else -> integer("Grid routes", "Take $chosen right moves and ${total-chosen} up moves. How many shortest routes?", combination(total,chosen),
                    "Choose which $chosen of the $total move positions contain a right move. There are ${combination(total,chosen)} possible shortest routes.", "Choose the positions of one move type among all moves.", listOf(total,chosen))
            }
        }
        "times-table" -> if(skill%2==0) integer("Star arrays", "$a rows contain $c stars each. How many stars?", a*c,
            "Count equal groups by multiplication: $a × $c = ${a*c} stars across all rows.", "Multiply the row count by the stars per row.", listOf(a,c))
            else integer("Missing factor", "Complete ? × $c = ${a*c}.", a, "Use the inverse operation: ${a*c} ÷ $c = $a, which checks by multiplication.", "Divide the product by the known factor.", listOf(c,a*c))
        "decimal-harbor" -> {
            val x=a*10+c; val y=b*10+c
            when(skill) {
                0 -> answer("Decimal cargo", "Add ${roundNumber(x/10.0)} + ${roundNumber(y/10.0)}.", (x+y)/10.0,
                    "Align place values. $x tenths + $y tenths = ${x+y} tenths, or ${roundNumber((x+y)/10.0)}.", "Align decimal points before combining place values.", listOf(x/10.0,y/10.0), .1)
                1 -> answer("Cargo difference", "Subtract ${roundNumber((x+y)/10.0)} − ${roundNumber(y/10.0)}.", x/10.0,
                    "Subtract aligned tenths: ${x+y} − $y = $x tenths, or ${roundNumber(x/10.0)}.", "Align decimal points before subtracting.", listOf((x+y)/10.0,y/10.0), .1)
                2 -> answer("Decimal product", "Multiply ${roundNumber(a/10.0)} × ${roundNumber(b/10.0)}.", a*b/100.0,
                    "Multiplying tenths by tenths produces hundredths. The product is ${a*b} hundredths, or ${roundNumber(a*b/100.0)}.", "Multiply the whole-number digits, then restore both decimal places.", listOf(a/10.0,b/10.0), .01)
                3 -> integer("Channel division", "Divide ${roundNumber(a*c/10.0)} by ${roundNumber(c/10.0)}.", a,
                    "Scale dividend and divisor by ten. The equivalent quotient ${a*c} ÷ $c equals $a.", "Multiply both numbers by the same power of ten.", listOf(a*c,c))
                4 -> answer("Harbor rounding", "Round ${roundNumber((a*100+b)/100.0)} to ONE decimal place.", (a*10+(b+5)/10)/10.0,
                    "Inspect the hundredths digit. Five or more raises the tenths digit; otherwise retain it. Keep one decimal place.", "Use the hundredths digit to decide how the tenths should round.", listOf((a*100+b)/100.0), .1)
                else -> answer("Place-value shift", "Multiply ${roundNumber(x/100.0)} by 100.", x.toDouble(),
                    "Multiplying by 100 moves each digit two places to the left: ${roundNumber(x/100.0)} × 100 = $x.", "Track the place value of each digit.", listOf(x/100.0))
            }
        }
        "math-market" -> {
            val price=a*100+b; val budget=(a+c)*100; val change=budget-price
            when(skill%3) {
                0 -> answer("Checkout change", "Pay ₹${roundNumber(budget/100.0)} for an item costing ₹${roundNumber(price/100.0)}. Enter change in rupees.", change/100.0,
                    "Work in paise to avoid rounding: $budget − $price = $change paise. Convert to ₹${roundNumber(change/100.0)}.", "Subtract the cost from the payment; keep decimal places aligned.", listOf(budget/100.0,price/100.0), .1)
                1 -> answer("Basket total", "Buy $c notebooks at ₹${roundNumber(price/100.0)} each. Enter the total in rupees.", price*c/100.0,
                    "Multiply the unit price by quantity: $price paise × $c = ${price*c} paise. Convert this to rupees.", "Multiply price per item by the number of items.", listOf(price/100.0,c.toDouble()), .1)
                else -> { val prices=List(6+tier){n(2,limit)}; val items=prices.shuffled(random).take(2+tier)
                    build("Exact-budget basket", "Spend exactly ₹${items.sum()} on ${items.size} items. Each price tile represents one available item.", items.sum(), prices, items.size) }
            }
        }
        "integer-expedition" -> {
            val x=n(-limit,limit); val y=n(-limit,limit)
            when(skill%3) {
                0 -> integer("Signed journey", "Start at $x on a number line and move by $y. Where do you finish?", x+y,
                    "Combine signed displacements: $x + ($y) = ${x+y}. Negative moves travel left and positive moves travel right.", "Track the direction of each signed move.", listOf(x,y))
                1 -> integer("Integer difference", "Calculate $x − ($y).", x-y,
                    "Subtracting a signed number means adding its opposite: $x − ($y) = ${x-y}.", "Change subtraction into addition of the opposite.", listOf(x,y))
                else -> integer("Signed product", "Calculate $x × ($y).", x*y,
                    "Multiply the magnitudes, then use the signs. Matching signs make a nonnegative product, unlike signs make a negative product: ${x*y}.", "Determine the product's sign before multiplying magnitudes.", listOf(x,y))
            }
        }
        "percent-studio" -> {
            val p=n(1,19)*5; val base=n(2,limit)*20; val part=p*base/100
            when(skill%3) {
                0 -> integer("Percent studio", "Find $p% of $base.", part,
                    "A percent is a fraction of one hundred: ($p ÷ 100) × $base = $part.", "Convert the percentage to a fraction of one hundred.", listOf(p,base))
                1 -> integer("Discount studio", "An item costs ₹$base before a $p% discount. Enter its sale price in rupees.", base-part,
                    "The discount is $p% of $base, or ₹$part. Subtract it from the original price to obtain ₹${base-part}.", "Find the discount first, then subtract it from the original price.", listOf(p,base))
                else -> integer("Reverse percentage", "$p% of an original amount equals $part. Find the original amount.", base,
                    "Undo the percentage scale: $part × 100 ÷ $p = $base. This checks by applying the original percentage.", "Divide by the percentage expressed as a fraction of one hundred.", listOf(p,part))
            }
        }
        "function-machine" -> integer("Function machine", "For f(x) = ${c}x${if(skill%2==0) "" else "²"} + $b, find f($a).", c*(if(skill%2==0) a else a*a)+b,
            "Substitute the input, evaluate any power, multiply by $c, then add $b. Follow that order to preserve the function rule.", "Substitute first, then follow the order of operations.", listOf(c,b,a))
        "geometry-proof" -> when(skill) {
            0 -> { val first=n(15,75); val second=n(15,75)
                integer("Triangle proof", "A triangle has angles $first° and $second°. Find the third angle in degrees.", 180-first-second,
                    "Triangle angles sum to 180°. Subtract both known angles: 180 − $first − $second = ${180-first-second}°.", "Use the fixed total of a triangle's interior angles.", listOf(first,second)) }
            1 -> { val sides=n(3,12)
                integer("Polygon proof", "Find the sum of interior angles of a $sides-sided polygon, in degrees.", (sides-2)*180,
                    "Split the polygon into ${sides-2} triangles from one vertex. Each contributes 180°, for a total of ${(sides-2)*180}°.", "Count the triangles formed from one vertex.", listOf(sides)) }
            2 -> integer("Similarity proof", "Similar triangles have corresponding sides $a and ${a*c}. A second side on the small triangle is $b. Find its matching large side.", b*c,
                "The scale factor is ${a*c} ÷ $a = $c. Multiply the second side by the same factor: $b × $c = ${b*c}.", "Corresponding lengths share one scale factor.", listOf(a,c,b))
            3 -> integer("Right-triangle proof", "A right triangle has legs ${3*c} and ${4*c}. Find its hypotenuse.", 5*c,
                "Pythagoras gives hypotenuse² = ${3*c}² + ${4*c}² = ${25*c*c}. The positive square root is ${5*c}.", "Square the perpendicular sides, add, and take the positive square root.", listOf(3*c,4*c))
            4 -> integer("Circle proof", "A circle has diameter ${2*a} cm. Find its radius in centimetres.", a,
                "The diameter crosses the centre and is twice the radius. Divide ${2*a} by two to obtain $a centimetres.", "The radius is half the diameter.", listOf(2*a))
            else -> integer("Straight-line proof", "Two adjacent angles on a straight line are $a° and x°. Find x in degrees.", 180-a,
                "A straight angle totals 180°. Subtract the known angle: 180 − $a = ${180-a}°.", "Adjacent angles on a straight line total 180 degrees.", listOf(a))
        }
        "calculus-climber" -> when(skill%3) {
            0 -> integer("Derivative slope", "For f(x) = ${c}x² + ${b}x, find f′($a).", 2*c*a+b,
                "Differentiate term by term: f′(x) = ${2*c}x + $b. Substitute $a to obtain ${2*c*a+b}.", "Use the power rule before substituting the input.", listOf(c,b,a))
            1 -> integer("Area accumulation", "Find the definite integral of ${2*c}x from 0 to $a.", c*a*a,
                "An antiderivative is ${c}x². Evaluating at the two limits gives $c × $a² − 0 = ${c*a*a}.", "Find an antiderivative and subtract its value at the lower bound.", listOf(2*c,a))
            else -> integer("Limit", "Find lim as x → $a of (x² − ${a*a}) / (x − $a).", 2*a,
                "Factor the numerator into (x − $a)(x + $a). Cancel away from the limit point, then substitute to get ${2*a}.", "Factor before trying direct substitution.", listOf(a))
        }
        "matrix-mission" -> { val d=n(); if(skill%2==0) integer("Determinant", "Find the determinant of the 2×2 matrix [$a, $b; $c, $d].", a*d-b*c,
            "A 2×2 determinant is ad − bc: $a × $d − $b × $c = ${a*d-b*c}.", "Subtract the off-diagonal product from the main-diagonal product.", listOf(a,b,c,d))
            else integer("Matrix transform", "Apply matrix [$a, $b; $c, $d] to vector ($c, $a). Enter the first component.", a*c+b*a,
                "Dot the first row with the vector: $a × $c + $b × $a = ${a*c+b*a}.", "Multiply corresponding row and vector entries, then add.", listOf(a,b,c,d)) }
        "number-theory" -> { val x=a*c; val y=b*c; fun gcd(i:Int,j:Int):Int = if(j==0) abs(i) else gcd(j,i%j)
            if(skill%2==0) integer("GCD crypt", "Find the greatest common divisor of $x and $y.", gcd(x,y),
                "Apply the Euclidean algorithm: replace the larger number by its remainder repeatedly until the remainder is zero. The last divisor is ${gcd(x,y)}.", "Use repeated division with remainders.", listOf(x,y))
            else integer("Modular code", "Find the remainder when ${a*b+c} is divided by $b.", c%b,
                "The term $a × $b is divisible by $b, so only $c contributes to the remainder. Its remainder is ${c%b}.", "Remove complete multiples of the divisor.", listOf(a*b+c,b)) }
        "optimization-arena" -> {
            val tiles=(2..limit*3).shuffled(random).take(7+tier)
            val weights=tiles.associateWith { n(2,10) }
            val capacity=weights.values.sum()/2
            val maximum=(0 until (1 shl tiles.size)).maxOf { mask ->
                val selected=tiles.filterIndexed { index,_ -> mask and (1 shl index)!=0 }
                if(selected.sumOf { weights.getValue(it) } <= capacity) selected.sum() else 0
            }
            build("Cargo optimizer", "Pack cargo to maximize total value within $capacity kg. Each tile shows value / weight and is available once.", maximum, tiles).copy(
                weights=weights,capacity=capacity,
                explanation="The best attainable value is $maximum within $capacity kg. Compare combinations: choosing the largest item alone or using only value-per-weight ratios can miss the optimum.",
                hint="Track both value and weight. Compare alternative combinations rather than filling the space with the largest item first.")
        }
        else -> error("Missing dynamic generator for $id")
    }
}
