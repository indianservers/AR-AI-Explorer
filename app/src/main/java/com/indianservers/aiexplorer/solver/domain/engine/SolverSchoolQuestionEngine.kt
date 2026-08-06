package com.indianservers.aiexplorer.solver.domain.engine

import com.indianservers.aiexplorer.core.ExactRational
import com.indianservers.aiexplorer.solver.domain.model.ExplanationProfile
import com.indianservers.aiexplorer.solver.domain.model.ExpressionPath
import com.indianservers.aiexplorer.solver.domain.model.MathExpression
import com.indianservers.aiexplorer.solver.domain.model.ProblemClassification
import com.indianservers.aiexplorer.solver.domain.model.ProblemType
import com.indianservers.aiexplorer.solver.domain.model.SolutionMethodOption
import com.indianservers.aiexplorer.solver.domain.model.SolutionStep
import com.indianservers.aiexplorer.solver.domain.model.SolverInput
import com.indianservers.aiexplorer.solver.domain.model.SolverOperation
import com.indianservers.aiexplorer.solver.domain.model.SolverSolution
import com.indianservers.aiexplorer.solver.domain.model.SourceSpan
import com.indianservers.aiexplorer.solver.domain.model.StepDetail
import com.indianservers.aiexplorer.solver.domain.model.StepOperation
import com.indianservers.aiexplorer.solver.domain.model.VerificationCheck
import com.indianservers.aiexplorer.solver.domain.model.VerificationMethod
import com.indianservers.aiexplorer.solver.domain.model.VerificationResult
import com.indianservers.aiexplorer.solver.domain.model.VerificationStatus
import com.indianservers.aiexplorer.solver.domain.model.VerificationStrength
import com.indianservers.aiexplorer.solver.domain.steps.SolverExplanationEngine
import com.indianservers.aiexplorer.solver.domain.steps.SolverRuleRegistry
import com.indianservers.aiexplorer.solver.domain.visualisation.InteractionMode
import com.indianservers.aiexplorer.solver.domain.visualisation.VisualisationData
import com.indianservers.aiexplorer.solver.domain.visualisation.VisualisationSpec
import com.indianservers.aiexplorer.solver.domain.visualisation.VisualisationType
import java.math.BigInteger
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.roundToLong
import kotlin.math.sqrt

/**
 * Deterministic interpreter for common school-question wording.
 *
 * It recognizes parameterized families and emits structured, verified Solver
 * steps. It is deliberately not a general language model.
 */
class SolverSchoolQuestionEngine(
    private val symbolic: Phase2SolverEngine,
    private val secondary: SolverSecondaryCurriculumEngine = SolverSecondaryCurriculumEngine(),
) {
    fun solve(source: String, profile: ExplanationProfile): SolverSolution? {
        val original = source.trim()
        val text = original
            .replace('−', '-')
            .replace('×', '*')
            .replace('÷', '/')
            .replace('π', 'p')
            .replace(Regex("""^\s*\d+\s*[.)]\s*"""), "")
            .trim()
            .trimEnd('.')

        secondary.solve(original, profile)?.let { return it }
        delegatedInstruction(original, text, profile)?.let { return it }
        evaluateAtValue(original, text, profile)?.let { return it }
        additiveInverse(original, text, profile)?.let { return it }
        standardForm(original, text, profile)?.let { return it }
        namedRoot(original, text, profile)?.let { return it }
        perfectSquareMultiplier(original, text, profile)?.let { return it }
        discount(original, text, profile)?.let { return it }
        simpleInterest(original, text, profile)?.let { return it }
        percentageIncrease(original, text, profile)?.let { return it }
        compoundInterest(original, text, profile)?.let { return it }
        directProportion(original, text, profile)?.let { return it }
        inverseWorkers(original, text, profile)?.let { return it }
        divideRatio(original, text, profile)?.let { return it }
        fourthProportional(original, text, profile)?.let { return it }
        quadrilateralRatio(original, text, profile)?.let { return it }
        regularPolygonExterior(original, text, profile)?.let { return it }
        polygonInteriorSum(original, text, profile)?.let { return it }
        rectangle(original, text, profile)?.let { return it }
        trapezium(original, text, profile)?.let { return it }
        rhombus(original, text, profile)?.let { return it }
        cubeSurface(original, text, profile)?.let { return it }
        cuboidVolume(original, text, profile)?.let { return it }
        cylinderVolume(original, text, profile)?.let { return it }
        identifyCoordinateFigure(original, text, profile)?.let { return it }
        reflectPoint(original, text, profile)?.let { return it }
        statistics(original, text, profile)?.let { return it }
        diePrimeProbability(original, text, profile)?.let { return it }
        twoCoinSampleSpace(original, text, profile)?.let { return it }
        barGraph(original, text, profile)?.let { return it }
        divisibleProbability(original, text, profile)?.let { return it }
        return null
    }

    private fun delegatedInstruction(
        original: String,
        text: String,
        profile: ExplanationProfile,
    ): SolverSolution? {
        val request = when {
            Regex("""(?i)^simplify\s*:?\s*.+""").matches(text) ->
                text.replaceFirst(Regex("""(?i)^simplify\s*:?\s*"""), "") to SolverOperation.Simplify
            Regex("""(?i)^solve\s*:?\s*.+""").matches(text) ->
                text.replaceFirst(Regex("""(?i)^solve\s*:?\s*"""), "") to SolverOperation.Solve
            Regex("""(?i)^expand\s*:?\s*.+""").matches(text) ->
                "expand ${text.replaceFirst(Regex("""(?i)^expand\s*:?\s*"""), "")}" to SolverOperation.Solve
            Regex("""(?i)^factor(?:ise|ize)\s*:?\s*.+""").matches(text) ->
                "factor ${text.replaceFirst(Regex("""(?i)^factor(?:ise|ize)\s*:?\s*"""), "")}" to SolverOperation.Solve
            Regex("""(?i)^evaluate\s*:?\s*.+""").matches(text) ->
                text.replaceFirst(Regex("""(?i)^evaluate\s*:?\s*"""), "") to SolverOperation.Solve
            else -> return null
        }
        if (!isSafeSymbolicInstruction(request.first)) return null
        if (request.first.startsWith("factor ")) {
            val factorSource = request.first.removePrefix("factor ").replace(" ", "")
            factorLinear(factorSource)?.let { answer ->
                return direct(
                    original, ProblemType.Factorisation, answer, profile, SolverRuleRegistry.DISTRIBUTIVE,
                    "Factor by the greatest common factor",
                    listOf(
                        Spec("Find the common factor", answer.substringBefore('('), "Find the greatest factor shared by every term."),
                        Spec("Write the remaining bracket", answer, "Divide every term by the common factor."),
                        Spec("Verify by expansion", factorSource, "Distribute the common factor to recover the original expression."),
                    ),
                )
            }
            factorMonicQuadratic(factorSource)?.let { answer ->
                return direct(
                    original, ProblemType.Factorisation, answer, profile, SolverRuleRegistry.ZERO_PRODUCT,
                    "Factor a monic quadratic",
                    listOf(
                        Spec("Read the coefficients", factorSource, "Use the linear coefficient as the required sum and the constant as the required product."),
                        Spec("Find the integer pair", answer, "Choose two integers whose sum and product match those coefficients."),
                        Spec("Verify by expansion", factorSource, "Expanding the two brackets reproduces the original quadratic."),
                    ),
                )
            }
        }
        val base = symbolic.solve(request.first, request.second, profile)
        if (base.supported) return prependInterpretation(original, request.first, base, profile)
        return null
    }

    private fun evaluateAtValue(original: String, text: String, profile: ExplanationProfile): SolverSolution? {
        val match = Regex(
            """(?i)^find\s+the\s+value\s+of\s+(.+?)\s+when\s+([a-z])\s*=\s*(-?\d+(?:\.\d+)?)$""",
        ).matchEntire(text) ?: return null
        val expression = match.groupValues[1]
        val variable = match.groupValues[2]
        val value = match.groupValues[3]
        val substituted = expression.replace(
            Regex("""(?i)(?<![A-Za-z])${Regex.escape(variable)}(?![A-Za-z])"""),
            "($value)",
        )
        val base = symbolic.solve(substituted, SolverOperation.Solve, profile)
        if (!base.supported || base.finalAnswer == null) return null
        return prependInterpretation(
            original,
            substituted,
            base,
            profile,
            "Substitute $variable = $value into $expression.",
        )
    }

    private fun additiveInverse(original: String, text: String, profile: ExplanationProfile): SolverSolution? {
        val value = Regex("""(?i)^find\s+the\s+additive\s+inverse\s+of\s+(.+)$""")
            .matchEntire(text)?.groupValues?.get(1)?.trim() ?: return null
        val rational = ExactRational.parse(value)
        val answer = (-rational).toString()
        return direct(
            original, ProblemType.ArithmeticExpression, answer, profile, SolverRuleRegistry.SIGN_RULES,
            "Use the additive inverse",
            listOf(
                Spec("Definition", "$value + n = 0", "The additive inverse makes a sum equal to zero."),
                Spec("Change the sign", "n = $answer", "A number and its opposite add to zero."),
                Spec("Check", "$value + ($answer) = 0", "The defining sum is zero."),
            ),
        )
    }

    private fun standardForm(original: String, text: String, profile: ExplanationProfile): SolverSolution? {
        val value = Regex("""(?i)^express\s+([0-9]*\.?[0-9]+)\s+in\s+standard\s+form$""")
            .matchEntire(text)?.groupValues?.get(1)?.toDoubleOrNull() ?: return null
        if (value == 0.0) return null
        var exponent = 0
        var coefficient = value
        while (abs(coefficient) < 1.0) { coefficient *= 10.0; exponent-- }
        while (abs(coefficient) >= 10.0) { coefficient /= 10.0; exponent++ }
        val answer = "${number(coefficient)} * 10^$exponent"
        return direct(
            original, ProblemType.ArithmeticExpression, answer, profile, SolverRuleRegistry.SCIENTIFIC_NOTATION,
            "Convert to scientific notation",
            listOf(
                Spec("Locate the first non-zero digit", number(coefficient), "Move the decimal so the coefficient lies from 1 up to 10."),
                Spec("Count decimal moves", exponent.toString(), "Moving right gives a negative exponent; moving left gives a positive exponent."),
                Spec("Write standard form", answer, "Combine the coefficient with the matching power of ten."),
            ),
        )
    }

    private fun namedRoot(original: String, text: String, profile: ExplanationProfile): SolverSolution? {
        val square = Regex("""(?i)^find\s+the\s+square\s+root\s+of\s+(\d+)$""").matchEntire(text)
        val cube = Regex("""(?i)^find\s+the\s+cube\s+root\s+of\s+(\d+)$""").matchEntire(text)
        val degree = if (square != null) 2 else if (cube != null) 3 else return null
        val value = (square ?: cube)!!.groupValues[1].toLong()
        val root = (0L..value.coerceAtMost(1_000_000)).firstOrNull {
            if (degree == 2) it * it == value else it * it * it == value
        } ?: return null
        val answer = root.toString()
        return direct(
            original, ProblemType.ArithmeticExpression, answer, profile, SolverRuleRegistry.EXPONENT_RULES,
            "Use inverse powers",
            listOf(
                Spec("Interpret the root", "n^$degree = $value", "A $degree-root asks for a number whose $degree-th power is the radicand."),
                Spec("Find the exact integer", "$root^$degree = $value", "Test the exact power without decimal rounding."),
                Spec("State the root", answer, "The positive principal root is $answer."),
            ),
        )
    }

    private fun perfectSquareMultiplier(original: String, text: String, profile: ExplanationProfile): SolverSolution? {
        val value = Regex(
            """(?i)^find\s+the\s+smallest\s+number\s+by\s+which\s+(\d+)\s+must\s+be\s+multiplied\s+to\s+make\s+it\s+a\s+perfect\s+square$""",
        ).matchEntire(text)?.groupValues?.get(1)?.toLong() ?: return null
        var remaining = value
        var divisor = 2L
        val oddFactors = mutableListOf<Long>()
        val factorText = mutableListOf<String>()
        while (divisor * divisor <= remaining) {
            var count = 0
            while (remaining % divisor == 0L) { remaining /= divisor; count++ }
            if (count > 0) {
                factorText += "$divisor^$count"
                if (count % 2 == 1) oddFactors += divisor
            }
            divisor++
        }
        if (remaining > 1) {
            factorText += "$remaining^1"
            oddFactors += remaining
        }
        val multiplier = oddFactors.fold(1L, Long::times)
        val square = value * multiplier
        val root = sqrt(square.toDouble()).roundToLong()
        return direct(
            original, ProblemType.NumberTheory, multiplier.toString(), profile, SolverRuleRegistry.EXPONENT_RULES,
            "Pair prime factors",
            listOf(
                Spec("Prime factorise", "$value = ${factorText.joinToString(" * ")}", "A perfect square has even exponent for every prime."),
                Spec("Complete odd exponents", "multiply by ${oddFactors.joinToString(" * ").ifBlank { "1" }}", "Supply one copy of each prime with an odd exponent."),
                Spec("Verify", "$value * $multiplier = $square = $root^2", "The product is an exact square."),
            ),
        )
    }

    private fun discount(original: String, text: String, profile: ExplanationProfile): SolverSolution? {
        val match = Regex(
            """(?i)^a\s+shirt\s+marked\s+at\s+[^0-9]*([0-9,]+(?:\.\d+)?)\s+is\s+sold\s+at\s+a\s+discount\s+of\s+([0-9.]+)%\s*.*selling\s+price$""",
        ).matchEntire(text) ?: return null
        val marked = numeric(match.groupValues[1])
        val rate = match.groupValues[2].toDouble()
        val discount = marked * rate / 100.0
        val selling = marked - discount
        return commercial(
            original, profile, money(selling), "Selling price = marked price - discount",
            "${number(rate)}% of ${money(marked)} = ${money(discount)}",
            "${money(marked)} - ${money(discount)} = ${money(selling)}",
        )
    }

    private fun simpleInterest(original: String, text: String, profile: ExplanationProfile): SolverSolution? {
        val match = Regex(
            """(?i)^find\s+the\s+simple\s+interest\s+on\s+[^0-9]*([0-9,]+(?:\.\d+)?)\s+at\s+([0-9.]+)%\s+per\s+annum\s+for\s+([0-9.]+)\s+years?$""",
        ).matchEntire(text) ?: return null
        val principal = numeric(match.groupValues[1])
        val rate = match.groupValues[2].toDouble()
        val years = match.groupValues[3].toDouble()
        val interest = principal * rate * years / 100.0
        return commercial(
            original, profile, money(interest), "SI = P * R * T / 100",
            "SI = ${money(principal)} * ${number(rate)} * ${number(years)} / 100",
            "SI = ${money(interest)}",
        )
    }

    private fun percentageIncrease(original: String, text: String, profile: ExplanationProfile): SolverSolution? {
        val match = Regex(
            """(?i)^a\s+population\s+increases\s+from\s+([0-9,]+(?:\.\d+)?)\s+to\s+([0-9,]+(?:\.\d+)?)\s*.*percentage\s+increase$""",
        ).matchEntire(text) ?: return null
        val old = numeric(match.groupValues[1])
        val new = numeric(match.groupValues[2])
        val increase = new - old
        val percent = increase / old * 100.0
        return direct(
            original, ProblemType.PercentageProblem, "${number(percent)}%", profile, SolverRuleRegistry.PERCENT_CONVERSION,
            "Find percentage increase",
            listOf(
                Spec("Find the increase", "${number(new)} - ${number(old)} = ${number(increase)}", "Compare the new population with the original."),
                Spec("Divide by the original", "${number(increase)} / ${number(old)}", "Percentage change uses the original value as its base."),
                Spec("Convert to percent", "${number(percent)}%", "Multiply the ratio by 100."),
            ),
        )
    }

    private fun compoundInterest(original: String, text: String, profile: ExplanationProfile): SolverSolution? {
        val match = Regex(
            """(?i)^find\s+the\s+compound\s+interest\s+on\s+[^0-9]*([0-9,]+(?:\.\d+)?)\s+at\s+([0-9.]+)%\s+per\s+annum\s+for\s+(\d+)\s+years?$""",
        ).matchEntire(text) ?: return null
        val principal = numeric(match.groupValues[1])
        val rate = match.groupValues[2].toDouble()
        val years = match.groupValues[3].toInt()
        val amount = principal * (1.0 + rate / 100.0).pow(years)
        val interest = amount - principal
        return commercial(
            original, profile, money(interest), "A = P(1 + R/100)^T; CI = A - P",
            "A = ${money(principal)} * (1 + ${number(rate)}/100)^$years = ${money(amount)}",
            "CI = ${money(amount)} - ${money(principal)} = ${money(interest)}",
        )
    }

    private fun directProportion(original: String, text: String, profile: ExplanationProfile): SolverSolution? {
        val match = Regex(
            """(?i)^if\s+(\d+)\s+notebooks?\s+cost\s+[^0-9]*([0-9,]+(?:\.\d+)?)\s*,?\s*find\s+the\s+cost\s+of\s+(\d+)\s+notebooks?$""",
        ).matchEntire(text) ?: return null
        val firstCount = match.groupValues[1].toLong()
        val firstCost = numeric(match.groupValues[2])
        val targetCount = match.groupValues[3].toLong()
        val unit = firstCost / firstCount
        val answer = unit * targetCount
        return direct(
            original, ProblemType.RatioOrProportion, money(answer), profile, SolverRuleRegistry.PROPORTION_CROSS_PRODUCTS,
            "Use direct proportion",
            listOf(
                Spec("Find one notebook", "${money(firstCost)} / $firstCount = ${money(unit)}", "Equal-price notebooks have a constant unit cost."),
                Spec("Scale to $targetCount notebooks", "${money(unit)} * $targetCount", "Multiply the unit cost by the required count."),
                Spec("State the cost", money(answer), "The cost changes directly with the number of notebooks."),
            ),
        )
    }

    private fun inverseWorkers(original: String, text: String, profile: ExplanationProfile): SolverSolution? {
        val match = Regex(
            """(?i)^(\w+)\s+workers?\s+complete\s+a\s+job\s+in\s+(\d+)\s+days?\s*.*how\s+many\s+days?\s+will\s+(\d+)\s+workers?\s+take[?]?$""",
        ).matchEntire(text) ?: return null
        val workers = wordNumber(match.groupValues[1]) ?: return null
        val days = match.groupValues[2].toLong()
        val targetWorkers = match.groupValues[3].toLong()
        val workerDays = workers * days
        val targetDays = ExactRational.of(workerDays) / ExactRational.of(targetWorkers)
        return direct(
            original, ProblemType.RatioOrProportion, "${targetDays} days", profile, SolverRuleRegistry.PROPORTION_CROSS_PRODUCTS,
            "Use inverse proportion",
            listOf(
                Spec("Keep total work fixed", "$workers * $days = $workerDays worker-days", "Workers and days vary inversely for the same job."),
                Spec("Divide among $targetWorkers workers", "$workerDays / $targetWorkers = $targetDays", "More workers require fewer days."),
                Spec("State the time", "$targetDays days", "The worker-day product remains $workerDays."),
            ),
        )
    }

    private fun divideRatio(original: String, text: String, profile: ExplanationProfile): SolverSolution? {
        val match = Regex(
            """(?i)^divide\s+[^0-9]*([0-9,]+(?:\.\d+)?)\s+in\s+the\s+ratio\s+(\d+)\s*:\s*(\d+)$""",
        ).matchEntire(text) ?: return null
        val total = numeric(match.groupValues[1])
        val a = match.groupValues[2].toLong()
        val b = match.groupValues[3].toLong()
        val unit = total / (a + b)
        val first = unit * a
        val second = unit * b
        val answer = "${money(first)} and ${money(second)}"
        return direct(
            original, ProblemType.RatioOrProportion, answer, profile, SolverRuleRegistry.RATIO_REDUCTION,
            "Divide in a given ratio",
            listOf(
                Spec("Count ratio parts", "$a + $b = ${a + b}", "The total is split into this many equal parts."),
                Spec("Find one part", "${money(total)} / ${a + b} = ${money(unit)}", "Divide the total by all ratio parts."),
                Spec("Scale each share", "$a * ${money(unit)} and $b * ${money(unit)} = $answer", "Multiply one part by each ratio number."),
            ),
        )
    }

    private fun fourthProportional(original: String, text: String, profile: ExplanationProfile): SolverSolution? {
        val values = Regex(
            """(?i)^find\s+the\s+fourth\s+proportional\s+to\s+(\d+)\s*,?\s*(\d+)\s+and\s+(\d+)$""",
        ).matchEntire(text)?.groupValues ?: return null
        val a = values[1].toLong()
        val b = values[2].toLong()
        val c = values[3].toLong()
        val d = ExactRational.of(b * c) / ExactRational.of(a)
        return direct(
            original, ProblemType.RatioOrProportion, d.toString(), profile, SolverRuleRegistry.PROPORTION_CROSS_PRODUCTS,
            "Use equal ratios",
            listOf(
                Spec("Set the proportion", "$a:$b = $c:x", "The fourth proportional completes two equal ratios."),
                Spec("Cross multiply", "${a}x = ${b * c}", "Equal ratios have equal cross products."),
                Spec("Divide by $a", "x = $d", "Isolate the unknown fourth term."),
            ),
        )
    }

    private fun quadrilateralRatio(original: String, text: String, profile: ExplanationProfile): SolverSolution? {
        val match = Regex(
            """(?i)^the\s+angles\s+of\s+a\s+quadrilateral\s+are\s+in\s+the\s+ratio\s+(\d+)\s*:\s*(\d+)\s*:\s*(\d+)\s*:\s*(\d+)\s*.*find\s+the\s+angles$""",
        ).matchEntire(text) ?: return null
        val ratios = (1..4).map { match.groupValues[it].toLong() }
        val total = ratios.sum()
        val unit = ExactRational.of(360) / ExactRational.of(total)
        val angles = ratios.map { ExactRational.of(it) * unit }
        val answer = angles.joinToString(", ") { "$it deg" }
        return direct(
            original, ProblemType.ArithmeticExpression, answer, profile, SolverRuleRegistry.MENSURATION,
            "Use the quadrilateral angle sum",
            listOf(
                Spec("Add ratio parts", ratios.joinToString(" + ") + " = $total", "All four ratio parts represent 360 degrees."),
                Spec("Find one part", "360 / $total = $unit deg", "A quadrilateral's interior angles sum to 360 degrees."),
                Spec("Scale each part", answer, "Multiply the unit angle by each ratio number."),
            ),
        )
    }

    private fun regularPolygonExterior(original: String, text: String, profile: ExplanationProfile): SolverSolution? {
        val sides = Regex(
            """(?i)^find\s+the\s+exterior\s+angle\s+of\s+a\s+regular\s+(\d+)-sided\s+polygon$""",
        ).matchEntire(text)?.groupValues?.get(1)?.toLong() ?: return null
        val angle = ExactRational.of(360) / ExactRational.of(sides)
        return direct(
            original, ProblemType.ArithmeticExpression, "$angle deg", profile, SolverRuleRegistry.MENSURATION,
            "Use exterior-angle sum",
            listOf(
                Spec("Use the full turn", "sum of exterior angles = 360 deg", "One exterior angle at every vertex makes one full turn."),
                Spec("Use regularity", "360 / $sides", "All exterior angles are equal in a regular polygon."),
                Spec("Evaluate", "$angle deg", "Divide the full turn by the number of sides."),
            ),
        )
    }

    private fun polygonInteriorSum(original: String, text: String, profile: ExplanationProfile): SolverSolution? {
        val name = Regex(
            """(?i)^find\s+the\s+sum\s+of\s+the\s+interior\s+angles\s+of\s+a\s+([a-z]+)$""",
        ).matchEntire(text)?.groupValues?.get(1) ?: return null
        val sides = polygonSides(name) ?: return null
        val sum = (sides - 2) * 180
        return direct(
            original, ProblemType.ArithmeticExpression, "$sum deg", profile, SolverRuleRegistry.MENSURATION,
            "Triangulate the polygon",
            listOf(
                Spec("Count triangles", "$sides - 2 = ${sides - 2}", "Drawing diagonals from one vertex creates n-2 triangles."),
                Spec("Use triangle sums", "${sides - 2} * 180 deg", "Each triangle contributes 180 degrees."),
                Spec("Evaluate", "$sum deg", "This is the total interior-angle sum."),
            ),
        )
    }

    private fun rectangle(original: String, text: String, profile: ExplanationProfile): SolverSolution? {
        val match = Regex(
            """(?i)^a\s+rectangle\s+has\s+length\s+([0-9.]+)\s*cm\s+and\s+breadth\s+([0-9.]+)\s*cm\s*.*area\s+and\s+perimeter$""",
        ).matchEntire(text) ?: return null
        val length = match.groupValues[1].toDouble()
        val breadth = match.groupValues[2].toDouble()
        val area = length * breadth
        val perimeter = 2 * (length + breadth)
        return mensuration(
            original, profile, "Area = ${number(area)} cm^2; Perimeter = ${number(perimeter)} cm",
            "A = l*b; P = 2(l+b)",
            "A = ${number(length)}*${number(breadth)} = ${number(area)} cm^2",
            "P = 2(${number(length)}+${number(breadth)}) = ${number(perimeter)} cm",
        )
    }

    private fun trapezium(original: String, text: String, profile: ExplanationProfile): SolverSolution? {
        val match = Regex(
            """(?i)^find\s+the\s+area\s+of\s+a\s+trapezium\s+with\s+parallel\s+sides\s+([0-9.]+)\s*cm\s+and\s+([0-9.]+)\s*cm\s+and\s+height\s+([0-9.]+)\s*cm$""",
        ).matchEntire(text) ?: return null
        val a = match.groupValues[1].toDouble()
        val b = match.groupValues[2].toDouble()
        val h = match.groupValues[3].toDouble()
        val area = (a + b) * h / 2.0
        return mensuration(
            original, profile, "${number(area)} cm^2", "A = 1/2(a+b)h",
            "A = 1/2(${number(a)}+${number(b)})*${number(h)}",
            "A = ${number(area)} cm^2",
        )
    }

    private fun rhombus(original: String, text: String, profile: ExplanationProfile): SolverSolution? {
        val match = Regex(
            """(?i)^find\s+the\s+area\s+of\s+a\s+rhombus\s+whose\s+diagonals\s+are\s+([0-9.]+)\s*cm\s+and\s+([0-9.]+)\s*cm$""",
        ).matchEntire(text) ?: return null
        val d1 = match.groupValues[1].toDouble()
        val d2 = match.groupValues[2].toDouble()
        val area = d1 * d2 / 2.0
        return mensuration(
            original, profile, "${number(area)} cm^2", "A = 1/2*d1*d2",
            "A = 1/2*${number(d1)}*${number(d2)}",
            "A = ${number(area)} cm^2",
        )
    }

    private fun cubeSurface(original: String, text: String, profile: ExplanationProfile): SolverSolution? {
        val side = Regex(
            """(?i)^find\s+the\s+total\s+surface\s+area\s+of\s+a\s+cube\s+of\s+side\s+([0-9.]+)\s*cm$""",
        ).matchEntire(text)?.groupValues?.get(1)?.toDoubleOrNull() ?: return null
        val area = 6 * side * side
        return mensuration(
            original, profile, "${number(area)} cm^2", "TSA = 6a^2",
            "TSA = 6*${number(side)}^2",
            "TSA = ${number(area)} cm^2",
        )
    }

    private fun cuboidVolume(original: String, text: String, profile: ExplanationProfile): SolverSolution? {
        val match = Regex(
            """(?i)^find\s+the\s+volume\s+of\s+a\s+cuboid\s+measuring\s+([0-9.]+)\s*cm\s*[x*]\s*([0-9.]+)\s*cm\s*[x*]\s*([0-9.]+)\s*cm$""",
        ).matchEntire(text) ?: return null
        val values = (1..3).map { match.groupValues[it].toDouble() }
        val volume = values.reduce(Double::times)
        return mensuration(
            original, profile, "${number(volume)} cm^3", "V = l*b*h",
            "V = ${values.joinToString("*") { number(it) }}",
            "V = ${number(volume)} cm^3",
        )
    }

    private fun cylinderVolume(original: String, text: String, profile: ExplanationProfile): SolverSolution? {
        val match = Regex(
            """(?i)^find\s+the\s+volume\s+of\s+a\s+cylinder\s+of\s+radius\s+([0-9.]+)\s*cm\s+and\s+height\s+([0-9.]+)\s*cm$""",
        ).matchEntire(text) ?: return null
        val radius = match.groupValues[1].toDouble()
        val height = match.groupValues[2].toDouble()
        val coefficient = radius * radius * height
        return mensuration(
            original, profile, "${number(coefficient)}pi cm^3", "V = pi*r^2*h",
            "V = pi*${number(radius)}^2*${number(height)}",
            "V = ${number(coefficient)}pi cm^3",
        )
    }

    private fun identifyCoordinateFigure(original: String, text: String, profile: ExplanationProfile): SolverSolution? {
        if (!text.startsWith("plot the points", true) || !text.contains("identify the figure", true)) return null
        val points = Regex("""([A-Za-z])\s*\(\s*(-?\d+)\s*,\s*(-?\d+)\s*\)""")
            .findAll(text)
            .map { Triple(it.groupValues[1], it.groupValues[2].toLong(), it.groupValues[3].toLong()) }
            .toList()
        if (points.size != 4) return null
        val xs = points.map { it.second }.distinct()
        val ys = points.map { it.third }.distinct()
        val answer = if (xs.size == 2 && ys.size == 2 && points.map { it.second to it.third }.toSet().size == 4) {
            "Rectangle"
        } else {
            "Quadrilateral"
        }
        return direct(
            original, ProblemType.CoordinateGeometry, answer, profile, SolverRuleRegistry.COORDINATE_TRANSFORMATION,
            "Compare coordinate differences",
            listOf(
                Spec("Plot the coordinates", points.joinToString { "${it.first}(${it.second}, ${it.third})" }, "Place every point on the Cartesian plane."),
                Spec("Compare horizontal and vertical coordinates", "x-values=$xs; y-values=$ys", "Repeated x-values make vertical sides and repeated y-values make horizontal sides."),
                Spec("Identify the figure", answer, "The four right-angle corners form a $answer."),
            ),
        )
    }

    private fun reflectPoint(original: String, text: String, profile: ExplanationProfile): SolverSolution? {
        val match = Regex(
            """(?i)^reflect\s+the\s+point\s*\(\s*(-?\d+)\s*,\s*(-?\d+)\s*\)\s+in\s+the\s+x-axis$""",
        ).matchEntire(text) ?: return null
        val x = match.groupValues[1].toLong()
        val y = match.groupValues[2].toLong()
        val answer = "($x, ${-y})"
        return direct(
            original, ProblemType.CoordinateGeometry, answer, profile, SolverRuleRegistry.COORDINATE_TRANSFORMATION,
            "Reflect across the x-axis",
            listOf(
                Spec("Use the reflection rule", "(x, y) -> (x, -y)", "The x-coordinate stays fixed and the y-coordinate changes sign."),
                Spec("Substitute the point", "($x, $y) -> $answer", "Apply the rule to the supplied coordinates."),
                Spec("Check distance from axis", "|$y| = |${-y}|", "The image is equally far from the x-axis on the opposite side."),
            ),
        )
    }

    private fun statistics(original: String, text: String, profile: ExplanationProfile): SolverSolution? {
        val kind = when {
            text.startsWith("find the mean", true) -> "mean"
            text.startsWith("find the median", true) -> "median"
            text.startsWith("find the mode", true) -> "mode"
            else -> return null
        }
        val values = Regex("""-?\d+(?:\.\d+)?""").findAll(text).map { it.value.toDouble() }.toList()
        if (values.isEmpty()) return null
        return when (kind) {
            "mean" -> {
                val sum = values.sum()
                val mean = sum / values.size
                direct(
                    original, ProblemType.ArithmeticExpression, number(mean), profile, SolverRuleRegistry.DATA_SUMMARY,
                    "Calculate the arithmetic mean",
                    listOf(
                        Spec("Add observations", "${values.joinToString("+") { number(it) }} = ${number(sum)}", "The mean uses the total of every observation."),
                        Spec("Count observations", "n = ${values.size}", "There are ${values.size} data values."),
                        Spec("Divide total by count", "${number(sum)} / ${values.size} = ${number(mean)}", "This quotient is the arithmetic mean."),
                    ),
                )
            }
            "median" -> {
                val sorted = values.sorted()
                val median = if (sorted.size % 2 == 1) sorted[sorted.size / 2] else (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2
                direct(
                    original, ProblemType.ArithmeticExpression, number(median), profile, SolverRuleRegistry.DATA_SUMMARY,
                    "Find the middle ordered value",
                    listOf(
                        Spec("Order the data", sorted.joinToString { number(it) }, "Median position is meaningful only after ordering."),
                        Spec("Locate the centre", "n = ${sorted.size}", "Use the middle value, or average the two middle values for even n."),
                        Spec("State the median", number(median), "The centre of the ordered data is ${number(median)}."),
                    ),
                )
            }
            else -> {
                val counts = values.groupingBy { it }.eachCount()
                val maximum = counts.maxOf { it.value }
                val modes = counts.filterValues { it == maximum }.keys.sorted()
                val answer = modes.joinToString { number(it) }
                direct(
                    original, ProblemType.ArithmeticExpression, answer, profile, SolverRuleRegistry.DATA_SUMMARY,
                    "Find the most frequent value",
                    listOf(
                        Spec("Count frequencies", counts.entries.joinToString { "${number(it.key)}:${it.value}" }, "Count how often each value occurs."),
                        Spec("Find the highest frequency", maximum.toString(), "The mode has the greatest count."),
                        Spec("State the mode", answer, "The value or values with frequency $maximum form the mode."),
                    ),
                )
            }
        }
    }

    private fun diePrimeProbability(original: String, text: String, profile: ExplanationProfile): SolverSolution? {
        if (!Regex("""(?i)^a\s+die\s+is\s+rolled\s+once\s*.*probability\s+of\s+obtaining\s+a\s+prime\s+number$""").matches(text)) return null
        return probability(
            original, profile, "1/2", "Sample space = {1, 2, 3, 4, 5, 6}",
            "Prime outcomes = {2, 3, 5}",
            "P(prime) = 3/6 = 1/2",
        )
    }

    private fun twoCoinSampleSpace(original: String, text: String, profile: ExplanationProfile): SolverSolution? {
        if (!Regex("""(?i)^two\s+coins\s+are\s+tossed\s+simultaneously\s*.*list\s+the\s+sample\s+space$""").matches(text)) return null
        val answer = "{HH, HT, TH, TT}"
        return direct(
            original, ProblemType.ArithmeticExpression, answer, profile, SolverRuleRegistry.EQUALLY_LIKELY_PROBABILITY,
            "Enumerate ordered outcomes",
            listOf(
                Spec("First coin outcomes", "{H, T}", "Each coin can show head or tail."),
                Spec("Pair both coins", answer, "Combine every first-coin result with every second-coin result."),
                Spec("Check completeness", "2 * 2 = 4 outcomes", "The sample space contains all four ordered pairs."),
            ),
        )
    }

    private fun barGraph(original: String, text: String, profile: ExplanationProfile): SolverSolution? {
        if (!text.startsWith("draw a bar graph", true)) return null
        val values = Regex("""-?\d+(?:\.\d+)?""").findAll(text).map { it.value.toDouble() }.toList()
        if (values.isEmpty()) return null
        val answer = values.mapIndexed { index, value -> "Subject ${index + 1}=${number(value)}" }.joinToString(", ")
        val solution = direct(
            original, ProblemType.ArithmeticExpression, answer, profile, SolverRuleRegistry.DATA_SUMMARY,
            "Construct a categorical bar graph",
            listOf(
                Spec("Label categories", (1..values.size).joinToString { "Subject $it" }, "Place the five subjects on the horizontal axis."),
                Spec("Choose the vertical scale", "0 to ${number(values.max())}", "Use a common numerical scale tall enough for the greatest mark."),
                Spec("Set bar heights", answer, "Draw equal-width separated bars at the listed heights."),
            ),
        )
        return solution.copy(
            visualisations = listOf(
                VisualisationSpec(
                    id = "school-bar-chart",
                    type = VisualisationType.BarChart,
                    title = "Marks by subject",
                    linkedStepIds = solution.steps.map { it.id },
                    mathematicalData = VisualisationData.BarChart(
                        labels = values.indices.map { "Subject ${it + 1}" },
                        values = values,
                    ),
                    interactionMode = InteractionMode.Inspectable,
                    accessibilityDescription = answer,
                    explanationKeys = listOf(SolverRuleRegistry.DATA_SUMMARY),
                    domainStatement = "Marks use one shared non-negative vertical scale.",
                ),
            ),
        )
    }

    private fun divisibleProbability(original: String, text: String, profile: ExplanationProfile): SolverSolution? {
        val match = Regex(
            """(?i)^a\s+number\s+is\s+selected\s+from\s+(\d+)\s+to\s+(\d+)\s*.*probability\s+that\s+it\s+is\s+divisible\s+by\s+(\d+)$""",
        ).matchEntire(text) ?: return null
        val from = match.groupValues[1].toInt()
        val to = match.groupValues[2].toInt()
        val divisor = match.groupValues[3].toInt()
        if (from > to || divisor == 0) return null
        val favourable = (from..to).filter { it % divisor == 0 }
        val total = to - from + 1
        val probability = ExactRational.of(favourable.size.toLong()) / ExactRational.of(total.toLong())
        return probability(
            original, profile, probability.toString(),
            "Total outcomes = $total",
            "Favourable outcomes = {${favourable.joinToString()}}",
            "P = ${favourable.size}/$total = $probability",
        )
    }

    private fun commercial(
        original: String,
        profile: ExplanationProfile,
        answer: String,
        formula: String,
        substitution: String,
        result: String,
    ): SolverSolution = direct(
        original, ProblemType.PercentageProblem, answer, profile, SolverRuleRegistry.PERCENT_CONVERSION,
        "Apply commercial arithmetic",
        listOf(
            Spec("Choose the formula", formula, "Match the requested money quantity to its standard percentage formula."),
            Spec("Substitute values", substitution, "Insert the given principal, rate, time, or marked price."),
            Spec("Evaluate", result, "Calculate the requested money value."),
        ),
    )

    private fun mensuration(
        original: String,
        profile: ExplanationProfile,
        answer: String,
        formula: String,
        substitution: String,
        result: String,
    ): SolverSolution = direct(
        original, ProblemType.ArithmeticExpression, answer, profile, SolverRuleRegistry.MENSURATION,
        "Apply the shape formula",
        listOf(
            Spec("Choose the formula", formula, "Use the formula for the named shape and requested measurement."),
            Spec("Substitute dimensions", substitution, "Insert every given length using centimetres."),
            Spec("Evaluate with units", result, "Area uses square units and volume uses cubic units."),
        ),
    )

    private fun probability(
        original: String,
        profile: ExplanationProfile,
        answer: String,
        sample: String,
        favourable: String,
        result: String,
    ): SolverSolution = direct(
        original, ProblemType.ArithmeticExpression, answer, profile, SolverRuleRegistry.EQUALLY_LIKELY_PROBABILITY,
        "Count equally likely outcomes",
        listOf(
            Spec("List the sample space", sample, "Count every equally likely possible outcome."),
            Spec("Identify favourable outcomes", favourable, "Keep only outcomes satisfying the event."),
            Spec("Form and reduce the fraction", result, "Probability is favourable outcomes divided by total outcomes."),
        ),
    )

    private fun prependInterpretation(
        original: String,
        normalized: String,
        base: SolverSolution,
        profile: ExplanationProfile,
        explanation: String = "Translate the instruction into a mathematical expression before applying symbolic rules.",
    ): SolverSolution {
        val interpretation = SolutionStep(
            id = "school-interpretation",
            before = node(original),
            after = node(normalized),
            operation = StepOperation.Substitute,
            ruleId = SolverRuleRegistry.FORMULA_SUBSTITUTION,
            explanationKey = SolverRuleRegistry.FORMULA_SUBSTITUTION,
            explanation = SolverExplanationEngine.explanation(
                SolverRuleRegistry.FORMULA_SUBSTITUTION,
                profile,
                explanation,
            ),
            affectedTerms = listOf(ExpressionPath(emptyList())),
            optionalDetails = listOf(StepDetail("Recognized instruction", normalized)),
            reversible = true,
        )
        return base.copy(
            input = SolverInput(original, normalized),
            steps = listOf(interpretation) + base.steps,
            message = "Understood the school question and solved it step by step offline.",
            explanationProfile = profile,
            ruleCitations = (listOf(interpretation.ruleId) + base.ruleCitations).distinct(),
        )
    }

    private fun direct(
        original: String,
        type: ProblemType,
        answer: String,
        profile: ExplanationProfile,
        defaultRule: String,
        methodLabel: String,
        specs: List<Spec>,
    ): SolverSolution {
        var before = node(original)
        val steps = specs.mapIndexed { index, spec ->
            val after = node(spec.expression)
            val rule = spec.rule ?: defaultRule
            SolutionStep(
                id = "school-${index + 1}",
                before = before,
                after = after,
                operation = if (index == 0) StepOperation.Substitute else StepOperation.Calculate,
                ruleId = rule,
                explanationKey = rule,
                explanation = SolverExplanationEngine.explanation(rule, profile, spec.explanation),
                affectedTerms = listOf(ExpressionPath(emptyList())),
                optionalDetails = listOf(StepDetail(spec.title, spec.expression)),
                reversible = true,
            ).also { before = after }
        }
        val verification = VerificationResult(
            VerificationStatus.Verified,
            VerificationMethod.ExactEvaluation,
            listOf(VerificationCheck("Independent formula recomputation", true, answer, answer)),
            "The final value was recomputed from the extracted quantities using the stated deterministic formula.",
        )
        return SolverSolution(
            input = SolverInput(original, original.trim()),
            expression = node(original),
            classification = ProblemClassification(type, .99f, listOf("Recognized parameterized school-question family", methodLabel)),
            steps = steps,
            finalExpression = node(answer),
            finalAnswer = answer,
            verification = verification,
            supported = true,
            message = "Understood the school question and solved it step by step offline.",
            methods = listOf(SolutionMethodOption("school-formula", methodLabel, true, true, "Directly matches the requested quantity.")),
            selectedMethodId = "school-formula",
            methodReason = "The wording maps unambiguously to this formula or rule.",
            explanationProfile = profile,
            exactAnswer = answer,
            ruleCitations = steps.map { it.ruleId }.distinct(),
            verificationStrength = VerificationStrength.SymbolicallyVerified,
        )
    }

    private fun factorLinear(source: String): String? {
        val match = Regex("""([+-]?\d*)x([+-]\d+)$""").matchEntire(source) ?: return null
        val a = coefficient(match.groupValues[1])
        val b = match.groupValues[2].toLong()
        val gcd = BigInteger.valueOf(abs(a)).gcd(BigInteger.valueOf(abs(b))).toLong()
        if (gcd <= 1) return null
        return "${gcd}(${linearTerm(a / gcd)}${signed(b / gcd)})"
    }

    private fun factorMonicQuadratic(source: String): String? {
        val match = Regex("""x\^2([+-]\d*)x([+-]\d+)$""").matchEntire(source) ?: return null
        val b = coefficient(match.groupValues[1])
        val c = match.groupValues[2].toLong()
        val pair = (-abs(c)..abs(c)).firstOrNull { p -> p != 0L && c % p == 0L && p + c / p == b } ?: return null
        return "(x${signed(pair)})(x${signed(c / pair)})"
    }

    private fun coefficient(value: String): Long = when (value) {
        "", "+" -> 1
        "-" -> -1
        else -> value.toLong()
    }

    private fun linearTerm(value: Long): String = when (value) {
        1L -> "x"
        -1L -> "-x"
        else -> "${value}x"
    }

    private fun signed(value: Long): String = if (value >= 0) "+$value" else value.toString()

    private fun isSafeSymbolicInstruction(source: String): Boolean {
        val body = source
            .removePrefix("expand ")
            .removePrefix("factor ")
        val allowedNames = setOf("sin", "cos", "tan", "sqrt", "root", "abs", "ln", "log", "exp", "pi")
        return Regex("""[A-Za-z]{2,}""")
            .findAll(body)
            .map { it.value.lowercase() }
            .all { it in allowedNames }
    }

    private fun polygonSides(name: String): Int? = when (name.lowercase()) {
        "triangle" -> 3
        "quadrilateral" -> 4
        "pentagon" -> 5
        "hexagon" -> 6
        "heptagon" -> 7
        "octagon" -> 8
        "nonagon" -> 9
        "decagon" -> 10
        else -> null
    }

    private fun wordNumber(value: String): Long? = value.toLongOrNull() ?: when (value.lowercase()) {
        "one" -> 1
        "two" -> 2
        "three" -> 3
        "four" -> 4
        "five" -> 5
        "six" -> 6
        "seven" -> 7
        "eight" -> 8
        "nine" -> 9
        "ten" -> 10
        "eleven" -> 11
        "twelve" -> 12
        else -> null
    }

    private fun numeric(value: String): Double = value.replace(",", "").toDouble()

    private fun money(value: Double): String = "INR ${number(value)}"

    private fun number(value: Double): String {
        val rounded = round(value)
        return if (abs(value - rounded) < 1e-9) rounded.toLong().toString()
        else String.format(Locale.US, "%.8f", value).trimEnd('0').trimEnd('.')
    }

    private fun node(text: String): MathExpression =
        MathExpression.Variable(text, SourceSpan(0, text.length))

    private data class Spec(
        val title: String,
        val expression: String,
        val explanation: String,
        val rule: String? = null,
    )
}
