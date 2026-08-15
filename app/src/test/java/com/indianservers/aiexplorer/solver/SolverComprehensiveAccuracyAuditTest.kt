package com.indianservers.aiexplorer.solver

import com.indianservers.aiexplorer.core.ExactRational
import com.indianservers.aiexplorer.core.ExpressionEngine
import com.indianservers.aiexplorer.core.SymbolicCasEngine
import com.indianservers.aiexplorer.solver.domain.engine.Phase3SolverEngine
import com.indianservers.aiexplorer.solver.domain.model.VerificationStatus
import java.io.File
import kotlin.math.abs
import org.junit.Assert.assertTrue
import org.junit.Test

class SolverComprehensiveAccuracyAuditTest {
    private val engine = Phase3SolverEngine()
    private val cas = SymbolicCasEngine()
    private val expressions = ExpressionEngine()

    @Test
    fun gradeTenThroughPostgraduateAccuracyCorpusIsCorrectOrSafelyUnsupported() {
        val cases = corpus()
        assertTrue("The audit must contain more than 100 independent questions", cases.size > 100)
        assertTrue("Natural-language questions are required", cases.count { it.form == InputForm.NaturalLanguage } >= 10)
        assertTrue("Probability questions are required", cases.count { it.domain == "Probability" } >= 10)
        assertTrue("Postgraduate questions are required", cases.count { it.level == "PG" } >= 10)

        val outcomes = cases.map { case -> evaluate(case) }
        writeReports(outcomes)

        val wrong = outcomes.filter { it.outcome == Outcome.Incorrect }
        assertTrue(
            buildString {
                appendLine("${wrong.size} accuracy cases failed:")
                wrong.forEach { appendLine("${it.case.domain}: ${it.case.input} -> ${it.detail}") }
            },
            wrong.isEmpty(),
        )
    }

    private fun evaluate(case: AccuracyCase): AccuracyOutcome {
        val solution = engine.solve(case.input)
        return when (val expectation = case.expectation) {
            is Expectation.Answer -> {
                val actual = solution.exactAnswer ?: solution.finalAnswer
                val answerMatches = actual != null && expectation.matches(actual)
                val verified = solution.verification.status == VerificationStatus.Verified
                if (solution.canPresentAsCorrect && answerMatches && verified) {
                    AccuracyOutcome(case, Outcome.Correct, actual.orEmpty())
                } else {
                    AccuracyOutcome(
                        case,
                        Outcome.Incorrect,
                        "supported=${solution.supported}; answer=$actual; expected=${expectation.display}; " +
                            "verification=${solution.verification.status}; message=${solution.message}",
                    )
                }
            }
            Expectation.Unsupported -> {
                if (!solution.canPresentAsCorrect && solution.finalAnswer == null) {
                    AccuracyOutcome(case, Outcome.SafelyUnsupported, solution.message)
                } else {
                    AccuracyOutcome(
                        case,
                        Outcome.Incorrect,
                        "Unsupported question was presented as answer=${solution.finalAnswer}; " +
                            "verification=${solution.verification.status}",
                    )
                }
            }
        }
    }

    private fun corpus(): List<AccuracyCase> = buildList {
        // Grade 10 arithmetic, fractions, percentages, ratios, equations and inequalities.
        for (i in 1..12) {
            answer("Grade 10", "Arithmetic", "$i + ${i + 1} * 2", (3 * i + 2).toString())
        }
        for (i in 1..10) {
            val expected = ExactRational.of(i.toLong()) / ExactRational.of((i + 1).toLong()) +
                ExactRational.of(1) / ExactRational.of((i + 1).toLong())
            answer("Grade 10", "Fractions", "$i/${i + 1} + 1/${i + 1}", expected.toString())
        }
        listOf(
            "25% of 80" to "20",
            "15% of 200" to "30",
            "12.5% of 160" to "20",
            "40% * 75" to "30",
            "5% of 360" to "18",
            "12:18" to "2:3",
            "2:3 = 4:6" to "True",
            "x:3 = 4:6" to "x = 2",
        ).forEach { (input, expected) -> answer("Grade 10", "Percentages and ratios", input, expected) }
        for (target in -6..6) {
            val coefficient = target.absoluteValue + 2
            val constant = target + 8
            val right = coefficient * target + constant
            answer("Grade 10", "Linear algebra", "${coefficient}x + $constant = $right", "x = $target")
        }
        for (target in -4..4) {
            val right = 3 * target + 4
            answer("Grade 10", "Inequalities", "3x + 4 <= $right", "x <= $target")
        }
        for (x in 1..5) {
            val y = x + 2
            answer("Grade 10", "Linear systems", "x + y = ${x + y}; x - y = ${x - y}", "x = $x, y = $y")
        }

        // Grade 10-12 trigonometry, coordinate geometry, functions and sequences.
        listOf(
            "sin(0deg)" to "0",
            "sin(30deg)" to "1/2",
            "sin(90deg)" to "1",
            "cos(0deg)" to "1",
            "cos(60deg)" to "1/2",
            "cos(180deg)" to "-1",
            "tan(0deg)" to "0",
            "tan(45deg)" to "1",
            "tan(135deg)" to "-1",
        ).forEach { (input, expected) -> answer("Grade 10-12", "Trigonometry", input, expected) }
        for (k in 1..5) {
            answer("Grade 10", "Coordinate geometry", "distance(0,0,${3 * k},${4 * k})", (5 * k).toString())
        }
        for (k in 1..4) {
            answer("Grade 10", "Coordinate geometry", "midpoint(0,0,${2 * k},${4 * k})", "($k, ${2 * k})")
        }
        listOf(
            "gradient(1,2,3,6)" to "2",
            "gradient(0,0,4,12)" to "3",
            "gradient(-2,1,2,9)" to "2",
        ).forEach { (input, expected) -> answer("Grade 10", "Coordinate geometry", input, expected) }
        listOf(
            Triple("f(x)=x^2+1; f(3)", "10", "Grade 10"),
            Triple("f(t)=2*t+5; f(4)", "13", "Grade 10"),
            Triple("f(x)=x^3-2*x; f(2)", "4", "Grade 11"),
            Triple("f(y)=y^2-y+1; f(5)", "21", "Grade 11"),
        ).forEach { (input, expected, level) -> answer(level, "Functions", input, expected) }
        for (n in 1..5) {
            answer("Grade 10", "Sequences", "arithmeticTerm(3,2,$n)", (3 + 2 * (n - 1)).toString())
        }
        for (n in 1..4) {
            answer("Grade 10", "Sequences", "arithmeticSum(2,3,$n)", (n * (2 * 2 + (n - 1) * 3) / 2).toString())
            answer("Grade 11", "Series", "geometricSum(3,2,$n)", (3 * ((1 shl n) - 1)).toString())
        }
        answer("Grade 12", "Series", "infiniteGeometricSum(1,1/2)", "2")

        // Grade 10-undergraduate number theory and matrices.
        for (n in 1..6) answer("Grade 10", "Number theory", "gcd($n,${n * 6})", n.toString())
        for (n in 1..5) answer("Grade 10", "Number theory", "lcm($n,${n + 1})", (n * (n + 1)).toString())
        listOf(
            "mod(31,7)" to "3",
            "mod(100,9)" to "1",
            "mod(257,16)" to "1",
            "primeFactors(84)" to "2*2*3*7",
            "primeFactors(97)" to "97",
            "primeFactors(360)" to "2*2*2*3*3*5",
            "base(FF,16,10)" to "255",
            "base(101101,2,10)" to "45",
        ).forEach { (input, expected) -> answer("Grade 10-UG", "Number theory", input, expected) }
        listOf(
            "matrixAdd([[1,2],[3,4]],[[4,3],[2,1]])" to "[[5, 5], [5, 5]]",
            "matrixSubtract([[5,4],[3,2]],[[1,2],[3,4]])" to "[[4, 2], [0, -2]]",
            "matrixMultiply([[1,2]],[[3],[4]])" to "[[11]]",
            "matrixMultiply([[1,0],[0,1]],[[2,3],[4,5]])" to "[[2, 3], [4, 5]]",
            "transpose([[1,2,3],[4,5,6]])" to "[[1, 4], [2, 5], [3, 6]]",
            "det([[1,2],[3,4]])" to "-2",
            "rank([[1,2],[2,4]])" to "1",
            "rref([[1,2],[3,4]])" to "[[1, 0], [0, 1]]",
        ).forEach { (input, expected) -> answer("Grade 11-UG", "Matrices", input, expected) }

        // Senior-secondary and undergraduate calculus.
        exactIgnoringSpaces("Grade 12-UG", "Calculus", "differentiate x^3", "3*x^2")
        exactIgnoringSpaces("Grade 12-UG", "Calculus", "differentiate 4*x^5-2*x+7", "20*x^4-2")
        exactIgnoringSpaces("Grade 12-UG", "Calculus", "differentiate sin(x)", "cos(x)")
        exactIgnoringSpaces("Grade 12-UG", "Calculus", "differentiate cos(x)", "-sin(x)")
        exactIgnoringSpaces("Grade 12-UG", "Calculus", "differentiate exp(x)", "exp(x)")
        exactIgnoringSpaces("Grade 12-UG", "Calculus", "differentiate ln(x)", "1/x")
        exactIgnoringSpaces("Grade 12-UG", "Calculus", "integrate x^2", "x^3/3+C")
        exactIgnoringSpaces("Grade 12-UG", "Calculus", "integrate sin(x)", "-cos(x)+C")
        exactIgnoringSpaces("Grade 12-UG", "Calculus", "integrate cos(x)", "sin(x)+C")
        exactIgnoringSpaces("Grade 12-UG", "Calculus", "integrate exp(x)", "exp(x)+C")
        exactIgnoringSpaces("UG", "Calculus", "integrate ln(x)", "x*ln(x)-x+C")
        answer("Grade 12-UG", "Calculus", "limit (x^2-4)/(x-2) as x -> 2", "4")

        // Undergraduate complex arithmetic.
        answer("Grade 11-UG", "Complex numbers", "complex 3+4i", "3 + 4i")
        answer("Grade 11-UG", "Complex numbers", "complex -2+5i", "-2 + 5i")
        answer("Grade 11-UG", "Complex numbers", "complex multiply 1+2i ; 3-4i", "11 + 2i")
        answer("Grade 11-UG", "Complex numbers", "complex multiply 2+i ; 2-i", "5")
        answer("Grade 11-UG", "Complex numbers", "complex multiply i ; i", "-1")

        // Natural-language, probability, statistics, geometry, vectors and PG breadth.
        // These are expected to fail closed until a verified route exists.
        listOf(
            "What is 25 percent of 80?",
            "Solve three x plus five equals twenty.",
            "Find the distance between (0,0) and (3,4).",
            "A shop gives 15 percent discount on 200. Find the discount.",
            "The sum of two consecutive integers is 31. Find them.",
            "Find the derivative of x cubed.",
            "Integrate x squared with respect to x.",
            "What is the sine of thirty degrees?",
            "If f of x is x squared plus one, find f of three.",
            "Two coins are tossed. What is the probability of two heads?",
        ).forEach { unsupported("Grade 10-UG", "Natural-language reasoning", it, InputForm.NaturalLanguage) }
        answer(
            "Grade 10-UG", "Natural-language reasoning",
            "A train travels 120 km in 2 hours. What is its average speed?",
            "60 km/h", InputForm.NaturalLanguage,
        )
        answer(
            "Grade 10",
            "Natural-language reasoning",
            "Find the HCF of 84 and 30.",
            "6",
            InputForm.NaturalLanguage,
        )
        listOf(
            "probability of exactly 3 successes in binomial n=10 p=0.5",
            "P(A union B) when P(A)=0.4 P(B)=0.5 P(A intersection B)=0.2",
            "conditional probability P(A|B) with P(A intersection B)=0.1 and P(B)=0.4",
            "Bayes theorem sensitivity=0.9 specificity=0.95 prevalence=0.01",
            "expected value of fair die",
            "variance of Bernoulli p=0.3",
            "Poisson probability lambda=4 k=2",
            "normal probability mean=0 sd=1 between -1 and 1",
            "geometric probability first success on trial 5 with p=0.2",
            "hypergeometric N=20 K=7 n=5 k=2",
            "Markov chain stationary distribution [[0.8,0.2],[0.3,0.7]]",
            "moment generating function of exponential lambda=2",
        ).forEach { unsupported("Grade 10-PG", "Probability", it) }
        listOf(
            "sample variance([2,4,6,8])",
            "linear regression y on x for (1,2),(2,4),(3,5)",
            "95 percent confidence interval mean=10 sd=2 n=25",
            "one sample t test mean=5 sampleMean=5.5 sd=1 n=16",
            "chi square goodness of fit observed=[10,20,30] expected=[20,20,20]",
            "ANOVA groups [[1,2,3],[4,5,6],[7,8,9]]",
            "principal component analysis covariance [[2,1],[1,2]]",
        ).forEach { unsupported("Grade 10-PG", "Statistics", it) }
        answer("Grade 10", "Statistics", "mean([2,4,6,8])", "Mean = 5")
        answer("Grade 10", "Statistics", "median([1,3,9,10,12])", "Median = 9")
        answer(
            "Grade 10",
            "Statistics",
            "standard deviation([1,2,3,4,5])",
            "Population standard deviation = 1.414214",
        )
        listOf(
            "area of triangle with sides 3,4,5",
            "solve triangle a=7 b=9 C=60deg",
            "volume of cone radius=3 height=4",
            "surface area of sphere radius=5",
            "prove opposite angles of a cyclic quadrilateral are supplementary",
            "find circumcentre of triangle (0,0),(4,0),(0,3)",
            "find equation of circle through (0,0),(1,0),(0,1)",
            "find locus equidistant from points (1,0) and (-1,0)",
        ).forEach { unsupported("Grade 10-UG", "Geometry", it) }
        listOf(
            "dot([1,2,3],[4,5,6])",
            "cross([1,0,0],[0,1,0])",
            "magnitude([3,4,12])",
            "angle between vectors [1,1,0] and [1,0,1]",
            "projection of [2,3] on [1,0]",
            "divergence of vector field [x^2,y^2,z^2]",
            "curl of vector field [-y,x,0]",
            "line integral of [y,x] along unit circle",
            "flux of [x,y,z] through unit sphere",
            "Gram Schmidt vectors [1,1,0],[1,0,1]",
        ).forEach { unsupported("Grade 11-PG", "Vectors", it) }
        listOf(
            "laplace transform t^2",
            "inverse laplace 1/(s^2+1)",
            "fourier transform exp(-x^2)",
            "solve PDE u_t = u_xx with u(0,t)=u(1,t)=0",
            "residue of 1/(z^2+1) at z=i",
            "eigenvalues([[2,1],[1,2]])",
            "diagonalize([[2,1],[1,2]])",
            "Jordan form([[1,1],[0,1]])",
            "solve y''+y=0",
            "evaluate contour integral 1/z over unit circle",
            "prove every finite integral domain is a field",
            "homology groups of torus",
        ).forEach { unsupported("PG", "Postgraduate mathematics", it) }
    }

    private fun MutableList<AccuracyCase>.answer(
        level: String,
        domain: String,
        input: String,
        expected: String,
        form: InputForm = InputForm.Symbolic,
    ) {
        add(AccuracyCase(level, domain, input, form, Expectation.Answer(expected) { actual -> actual == expected }))
    }

    private fun MutableList<AccuracyCase>.exactIgnoringSpaces(
        level: String,
        domain: String,
        input: String,
        expected: String,
    ) {
        add(
            AccuracyCase(
                level,
                domain,
                input,
                InputForm.Symbolic,
                Expectation.Answer(expected) { actual ->
                    actual.replace(" ", "") == expected.replace(" ", "") ||
                        runCatching {
                            val difference = cas.simplify("($actual)-($expected)")
                            difference.supported && difference.exact.replace(" ", "") in setOf("0", "0/1")
                        }.getOrDefault(false) ||
                        numericallyEquivalent(actual, expected)
                },
            ),
        )
    }

    private fun MutableList<AccuracyCase>.unsupported(
        level: String,
        domain: String,
        input: String,
        form: InputForm = InputForm.Symbolic,
    ) {
        add(AccuracyCase(level, domain, input, form, Expectation.Unsupported))
    }

    private fun writeReports(outcomes: List<AccuracyOutcome>) {
        val reportDirectory = File("build/reports/solver-accuracy").apply { mkdirs() }
        File(reportDirectory, "solver-accuracy-cases.csv").writeText(
            buildString {
                appendLine("level,domain,form,outcome,input,detail")
                outcomes.forEach { row ->
                    appendLine(
                        listOf(
                            row.case.level,
                            row.case.domain,
                            row.case.form.name,
                            row.outcome.name,
                            row.case.input,
                            row.detail,
                        ).joinToString(",") { csv(it) },
                    )
                }
            },
        )
        val answered = outcomes.filter { it.case.expectation is Expectation.Answer }
        val unsupported = outcomes.filter { it.case.expectation == Expectation.Unsupported }
        val correct = outcomes.count { it.outcome == Outcome.Correct }
        val wrong = outcomes.count { it.outcome == Outcome.Incorrect }
        File(reportDirectory, "summary.txt").writeText(
            "total=${outcomes.size}\n" +
                "answerable=${answered.size}\n" +
                "correct=$correct\n" +
                "safelyUnsupported=${outcomes.count { it.outcome == Outcome.SafelyUnsupported }}\n" +
                "incorrect=$wrong\n" +
                "answerAccuracy=${percent(correct, answered.size)}\n" +
                "coverage=${percent(correct, outcomes.size)}\n" +
                "unsupportedContractCases=${unsupported.size}\n",
        )
    }

    private fun csv(value: String): String = "\"${value.replace("\"", "\"\"")}\""

    private fun percent(part: Int, whole: Int): String =
        if (whole == 0) "0.00%" else "%.2f%%".format(part * 100.0 / whole)

    private fun numericallyEquivalent(first: String, second: String): Boolean = runCatching {
        val left = expressions.compile(first)
        val right = expressions.compile(second)
        listOf(-3.0, -1.0, 0.0, 1.0, 2.0, 4.0).all { x ->
            val a = left.eval(mapOf("x" to x))
            val b = right.eval(mapOf("x" to x))
            a.isFinite() && b.isFinite() && abs(a - b) <= 1e-9 * maxOf(1.0, abs(a), abs(b))
        }
    }.getOrDefault(false)

    private sealed interface Expectation {
        data class Answer(
            val display: String,
            val matches: (String) -> Boolean,
        ) : Expectation

        data object Unsupported : Expectation
    }

    private data class AccuracyCase(
        val level: String,
        val domain: String,
        val input: String,
        val form: InputForm,
        val expectation: Expectation,
    )

    private data class AccuracyOutcome(
        val case: AccuracyCase,
        val outcome: Outcome,
        val detail: String,
    )

    private enum class InputForm { Symbolic, NaturalLanguage }
    private enum class Outcome { Correct, SafelyUnsupported, Incorrect }

    private val Int.absoluteValue: Int
        get() = if (this < 0) -this else this
}
