package com.indianservers.aiexplorer.solver.domain.model

import com.indianservers.aiexplorer.core.ExactRational
import com.indianservers.aiexplorer.solver.domain.visualisation.FormulaUnderstanding
import com.indianservers.aiexplorer.solver.domain.visualisation.VisualVerificationResult
import com.indianservers.aiexplorer.solver.domain.visualisation.VisualisationSpec

data class SourceSpan(val start: Int, val endExclusive: Int) {
    init {
        require(start >= 0 && endExclusive >= start)
    }
}

enum class UnaryOperator { Positive, Negative, Percent }
enum class BinaryOperator { Add, Subtract, Multiply, Divide }
enum class InequalityOperator(val symbol: String) {
    Less("<"), LessOrEqual("<="), Greater(">"), GreaterOrEqual(">="),
}

sealed interface MathExpression {
    val span: SourceSpan

    data class Number(val value: ExactRational, override val span: SourceSpan) : MathExpression
    data class Variable(val name: String, override val span: SourceSpan) : MathExpression
    data class Constant(val name: String, override val span: SourceSpan) : MathExpression
    data class UnaryOperation(val operator: UnaryOperator, val operand: MathExpression, override val span: SourceSpan) : MathExpression
    data class BinaryOperation(
        val operator: BinaryOperator,
        val left: MathExpression,
        val right: MathExpression,
        override val span: SourceSpan,
    ) : MathExpression
    data class Power(val base: MathExpression, val exponent: MathExpression, override val span: SourceSpan) : MathExpression
    data class Root(val radicand: MathExpression, val degree: MathExpression?, override val span: SourceSpan) : MathExpression
    data class Fraction(val numerator: MathExpression, val denominator: MathExpression, override val span: SourceSpan) : MathExpression
    data class Function(val name: String, val arguments: List<MathExpression>, override val span: SourceSpan) : MathExpression
    data class Equation(val left: MathExpression, val right: MathExpression, override val span: SourceSpan) : MathExpression
    data class Inequality(
        val left: MathExpression,
        val operator: InequalityOperator,
        val right: MathExpression,
        override val span: SourceSpan,
    ) : MathExpression
    data class ExpressionList(val values: List<MathExpression>, override val span: SourceSpan) : MathExpression
    data class SystemOfEquations(val equations: List<Equation>, override val span: SourceSpan) : MathExpression
}

enum class ProblemType {
    ArithmeticExpression,
    FractionSimplification,
    PercentageProblem,
    RatioOrProportion,
    AlgebraicSimplification,
    Expansion,
    Factorisation,
    LinearEquation,
    LinearInequality,
    SimultaneousLinearEquations,
    PolynomialOperation,
    QuadraticEquation,
    RationalOrRadicalEquation,
    FunctionAnalysis,
    TrigonometricProblem,
    SequenceOrSeries,
    CoordinateGeometry,
    MatrixOperation,
    NumberTheory,
    Calculus,
    ComplexNumbers,
    UnsupportedOrAmbiguous,
}

data class ProblemClassification(
    val type: ProblemType,
    val confidence: Float,
    val evidence: List<String>,
)

data class ExpressionPath(val indices: List<Int>)
data class StepDetail(val label: String, val value: String)

enum class StepOperation {
    Interpret,
    Normalize,
    Calculate,
    Simplify,
    CombineLikeTerms,
    Expand,
    Factor,
    AddBothSides,
    SubtractBothSides,
    MultiplyBothSides,
    DivideBothSides,
    ReverseInequality,
    Substitute,
    Verify,
}

data class SolutionStep(
    val id: String,
    val before: MathExpression,
    val after: MathExpression,
    val operation: StepOperation,
    val ruleId: String,
    val explanationKey: String,
    val explanation: String,
    val affectedTerms: List<ExpressionPath>,
    val optionalDetails: List<StepDetail> = emptyList(),
    val reversible: Boolean,
)

enum class VerificationStatus { Verified, Failed, Inconclusive, NotApplicable }
enum class VerificationMethod { ExactEvaluation, Substitution, SystemSubstitution, SampledEquivalence, BoundarySampling, Expansion }

data class VerificationCheck(
    val label: String,
    val passed: Boolean,
    val expected: String,
    val actual: String,
)

data class VerificationResult(
    val status: VerificationStatus,
    val method: VerificationMethod,
    val checks: List<VerificationCheck>,
    val message: String,
)

enum class SolverOperation { Solve, Simplify, Check }

data class SolverInput(
    val original: String,
    val normalized: String,
    val sourceName: String = "keyboard",
)

sealed interface SolverInputResult {
    data class Success(val input: SolverInput) : SolverInputResult
    data class Error(val message: String) : SolverInputResult
}

data class SolverParseError(
    val message: String,
    val span: SourceSpan,
    val expected: Set<String> = emptySet(),
)

sealed interface SolverParseResult {
    data class Success(val expression: MathExpression, val normalized: String) : SolverParseResult
    data class Error(val error: SolverParseError, val normalized: String) : SolverParseResult
}

data class SolverSolution(
    val input: SolverInput,
    val expression: MathExpression?,
    val classification: ProblemClassification,
    val steps: List<SolutionStep>,
    val finalExpression: MathExpression?,
    val finalAnswer: String?,
    val verification: VerificationResult,
    val supported: Boolean,
    val message: String,
    val parseError: SolverParseError? = null,
    val methods: List<SolutionMethodOption> = emptyList(),
    val selectedMethodId: String? = null,
    val methodReason: String? = null,
    val explanationProfile: ExplanationProfile = ExplanationProfile.SchoolExamination,
    val assumptions: List<SolverAssumption> = emptyList(),
    val restrictions: List<DomainRestriction> = emptyList(),
    val exactAnswer: String? = finalAnswer,
    val approximateAnswer: String? = null,
    val commonMistakes: List<CommonMistake> = emptyList(),
    val ruleCitations: List<String> = steps.map(SolutionStep::ruleId).distinct(),
    val verificationStrength: VerificationStrength = when (verification.status) {
        VerificationStatus.Verified -> VerificationStrength.SymbolicallyVerified
        VerificationStatus.Failed -> VerificationStrength.Failed
        VerificationStatus.Inconclusive -> VerificationStrength.Inconclusive
        VerificationStatus.NotApplicable -> VerificationStrength.PartiallyVerified
    },
    val visualisations: List<VisualisationSpec> = emptyList(),
    val visualVerification: VisualVerificationResult? = null,
    val formulaUnderstanding: FormulaUnderstanding? = null,
) {
    val canPresentAsCorrect: Boolean
        get() = supported && verification.status != VerificationStatus.Failed
}

enum class ExplanationProfile(val label: String) {
    ChildFriendly("Child-friendly"),
    SchoolExamination("School examination"),
    University("University"),
    Rigorous("Rigorous"),
}

data class SolutionMethodOption(
    val id: String,
    val label: String,
    val applicable: Boolean,
    val recommended: Boolean,
    val reason: String,
)

data class SolverAssumption(
    val statement: String,
    val reason: String,
)

data class DomainRestriction(
    val expression: String,
    val condition: String,
    val reason: String,
    val excludedValues: List<String> = emptyList(),
)

data class CommonMistake(
    val id: String,
    val incorrectTransformation: String,
    val whyInvalid: String,
    val counterExample: String,
    val correctMethod: String,
    val observedInUserWork: Boolean = false,
)

enum class VerificationStrength {
    SymbolicallyVerified,
    NumericallyVerified,
    PartiallyVerified,
    Inconclusive,
    Failed,
}

enum class AdvancedProblemCategory {
    Algebra,
    Functions,
    Trigonometry,
    SequencesAndSeries,
    CoordinateGeometry,
    Matrices,
    NumberTheory,
}

data class AdvancedMathProblem(
    val source: String,
    val category: AdvancedProblemCategory,
    val operation: String,
    val arguments: List<String>,
    val variable: String? = null,
)

fun MathExpression.children(): List<MathExpression> = when (this) {
    is MathExpression.Number, is MathExpression.Variable, is MathExpression.Constant -> emptyList()
    is MathExpression.UnaryOperation -> listOf(operand)
    is MathExpression.BinaryOperation -> listOf(left, right)
    is MathExpression.Power -> listOf(base, exponent)
    is MathExpression.Root -> listOfNotNull(radicand, degree)
    is MathExpression.Fraction -> listOf(numerator, denominator)
    is MathExpression.Function -> arguments
    is MathExpression.Equation -> listOf(left, right)
    is MathExpression.Inequality -> listOf(left, right)
    is MathExpression.ExpressionList -> values
    is MathExpression.SystemOfEquations -> equations
}

fun MathExpression.variables(): Set<String> = when (this) {
    is MathExpression.Variable -> setOf(name)
    else -> children().flatMapTo(linkedSetOf()) { it.variables() }
}

fun MathExpression.containsFraction(): Boolean =
    this is MathExpression.Fraction || children().any(MathExpression::containsFraction)

fun MathExpression.containsPower(): Boolean =
    this is MathExpression.Power || children().any(MathExpression::containsPower)

object SolverExpressionRenderer {
    fun render(expression: MathExpression, parentPrecedence: Int = 0): String {
        val precedence = when (expression) {
            is MathExpression.Equation, is MathExpression.Inequality -> 0
            is MathExpression.BinaryOperation -> when (expression.operator) {
                BinaryOperator.Add, BinaryOperator.Subtract -> 1
                BinaryOperator.Multiply, BinaryOperator.Divide -> 2
            }
            is MathExpression.Fraction -> 2
            is MathExpression.Power -> 3
            is MathExpression.UnaryOperation -> 4
            else -> 5
        }
        val text = when (expression) {
            is MathExpression.Number -> expression.value.toString()
            is MathExpression.Variable -> expression.name
            is MathExpression.Constant -> expression.name
            is MathExpression.UnaryOperation -> when (expression.operator) {
                UnaryOperator.Positive -> "+${render(expression.operand, precedence)}"
                UnaryOperator.Negative -> "-${render(expression.operand, precedence)}"
                UnaryOperator.Percent -> "${render(expression.operand, precedence)}%"
            }
            is MathExpression.BinaryOperation -> {
                val symbol = when (expression.operator) {
                    BinaryOperator.Add -> " + "
                    BinaryOperator.Subtract -> " - "
                    BinaryOperator.Multiply -> "*"
                    BinaryOperator.Divide -> "/"
                }
                render(expression.left, precedence) + symbol + render(expression.right, precedence + if (expression.operator == BinaryOperator.Subtract) 1 else 0)
            }
            is MathExpression.Power -> "${render(expression.base, precedence)}^${render(expression.exponent, precedence)}"
            is MathExpression.Root -> if (expression.degree == null) "sqrt(${render(expression.radicand)})" else "root(${render(expression.radicand)}, ${render(expression.degree)})"
            is MathExpression.Fraction -> "${render(expression.numerator, precedence)}/${render(expression.denominator, precedence + 1)}"
            is MathExpression.Function -> "${expression.name}(${expression.arguments.joinToString { render(it) }})"
            is MathExpression.Equation -> "${render(expression.left)} = ${render(expression.right)}"
            is MathExpression.Inequality -> "${render(expression.left)} ${expression.operator.symbol} ${render(expression.right)}"
            is MathExpression.ExpressionList -> expression.values.joinToString(", ") { render(it) }
            is MathExpression.SystemOfEquations -> expression.equations.joinToString("; ") { render(it) }
        }
        return if (precedence < parentPrecedence) "($text)" else text
    }
}
