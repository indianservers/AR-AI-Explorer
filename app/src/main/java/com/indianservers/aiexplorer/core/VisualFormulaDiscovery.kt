package com.indianservers.aiexplorer.core

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow

enum class FormulaDifficulty { Beginner, Intermediate, Advanced }
enum class DiscoveryStage { Experience, Manipulate, Notice, Predict, Prove, Explain, Apply }
enum class FormulaVisualModel { Tiles, SplitArea, Staircase, FractionGrid, AlgebraArea, Balance, Geometry, Circle, Venn, Redistribution }

data class FormulaVariable(
    val symbol: String,
    val name: String,
    val minimum: Double,
    val maximum: Double,
    val defaultValue: Double,
)

data class VisualFormulaDefinition(
    val id: String,
    val title: String,
    val category: String,
    val symbolicExpression: String,
    val spokenFormula: String,
    val visualModel: FormulaVisualModel,
    val variables: List<FormulaVariable>,
    val proofLabId: String?,
    val prompts: List<String>,
    val assemblyTokens: List<String>,
    val explanationOptions: List<String>,
    val correctExplanation: Int,
    val difficulty: FormulaDifficulty,
    val minutes: Int,
    val tags: Set<String>,
)

data class FormulaDiscoveryState(
    val formulaId: String,
    val currentStage: DiscoveryStage = DiscoveryStage.Experience,
    val parameterValues: Map<String, Double> = emptyMap(),
    val investigatedCases: Int = 0,
    val prediction: String = "",
    val proofStepIndex: Int = 0,
    val formulaAssembled: Boolean = false,
    val explanationCompleted: Boolean = false,
    val independentChallengeCompleted: Boolean = false,
    val hintsUsed: Int = 0,
)

data class FormulaEvaluation(
    val measurements: Map<String, Double>,
    val result: Double,
    val invariantResidual: Double,
)

object VisualFormulaRegistry {
    private fun v(symbol: String, name: String, min: Double, max: Double, initial: Double) =
        FormulaVariable(symbol, name, min, max, initial)

    private fun f(
        id: String, title: String, category: String, expression: String, spoken: String,
        model: FormulaVisualModel, variables: List<FormulaVariable>, lab: String? = null,
        tokens: List<String>, explanation: String, tags: Set<String> = emptySet(),
        difficulty: FormulaDifficulty = FormulaDifficulty.Beginner,
    ) = VisualFormulaDefinition(
        id, title, category, expression, spoken, model, variables, lab,
        listOf(
            "What do you notice before using a formula?",
            "Change the controls and test at least three cases.",
            "What changes, and what relationship stays invariant?",
            "Complete the formula from the visual evidence.",
            "Use the transformation controls to make both quantities coincide.",
            "Choose the statement that explains why the transformation is valid.",
            "Solve a fresh case without a visual hint, then verify it.",
        ),
        tokens, listOf(explanation, "The values happen to match only in this example.", "The formula is a rule that must be memorised."), 0,
        difficulty, if (difficulty == FormulaDifficulty.Beginner) 4 else 6,
        tags + title.lowercase().split(" ").toSet() + expression,
    )

    val formulas = listOf(
        f("multiply-array", "Multiplication as an Array", "Arithmetic", "a × b = b × a", "rows times columns equals columns times rows", FormulaVisualModel.Tiles, listOf(v("a","rows",1.0,12.0,3.0),v("b","columns",1.0,12.0,4.0)), tokens=listOf("a","×","b","=","b","×","a"), explanation="Rotating the same array swaps rows and columns without changing its tile count."),
        f("distributive", "Distributive Property", "Arithmetic", "a(b+c)=ab+ac", "a times b plus c equals a b plus a c", FormulaVisualModel.SplitArea, listOf(v("a","height",1.0,10.0,4.0),v("b","left width",1.0,10.0,3.0),v("c","right width",1.0,10.0,2.0)), tokens=listOf("a","(","b","+","c",")","=","ab","+","ac"), explanation="The dividing line changes one rectangle into two non-overlapping parts while preserving total area."),
        f("natural-sum", "Sum of First n Natural Numbers", "Number Patterns", "1+2+…+n=n(n+1)/2", "the first n numbers sum to n times n plus one divided by two", FormulaVisualModel.Staircase, listOf(v("n","terms",1.0,15.0,5.0)), tokens=listOf("Sₙ","=","n","(","n+1",")","/","2"), explanation="A reversed copy completes an n by n plus one rectangle, so one staircase is half."),
        f("odd-sum-square", "Sum of First n Odd Numbers", "Number Patterns", "1+3+…+(2n−1)=n²", "the first n odd numbers sum to n squared", FormulaVisualModel.Tiles, listOf(v("n","layers",1.0,15.0,6.0)), "odd-sum-square", listOf("1+3+…+(2n−1)","=","n²"), "Each next odd number is exactly the L-shaped border needed to grow an n minus one square into an n square."),
        f("equivalent-fractions", "Equivalent Fractions", "Fractions", "a/b=ka/kb", "a over b equals k a over k b", FormulaVisualModel.FractionGrid, listOf(v("a","shaded parts",1.0,5.0,2.0),v("b","parts",2.0,8.0,3.0),v("k","refinement",1.0,5.0,2.0)), tokens=listOf("a","/","b","=","ka","/","kb"), explanation="Refining every part into k equal pieces changes the count but not the shaded physical area."),
        f("fraction-product", "Fraction Multiplication", "Fractions", "a/b × c/d = ac/bd", "a over b times c over d equals a c over b d", FormulaVisualModel.FractionGrid, listOf(v("a","vertical shaded",1.0,6.0,2.0),v("b","vertical parts",2.0,8.0,3.0),v("c","horizontal shaded",1.0,6.0,3.0),v("d","horizontal parts",2.0,8.0,4.0)), tokens=listOf("a/b","×","c/d","=","ac/bd"), explanation="The overlap contains a times c cells out of the b times d equal cells."),
        f("binomial-square", "Square of a Binomial", "Algebra", "(a+b)²=a²+2ab+b²", "a plus b squared equals a squared plus two a b plus b squared", FormulaVisualModel.AlgebraArea, listOf(v("a","first length",.2,5.0,2.0),v("b","second length",.2,5.0,1.0)), "algebra-square", listOf("(a+b)²","=","a²","+","2ab","+","b²"), "The large square partitions exactly into one a squared, two a b rectangles, and one b squared."),
        f("difference-squares", "Difference of Squares", "Algebra", "a²−b²=(a−b)(a+b)", "a squared minus b squared equals a minus b times a plus b", FormulaVisualModel.AlgebraArea, listOf(v("a","large side",2.0,10.0,6.0),v("b","removed side",1.0,9.0,2.0)), tokens=listOf("a²","−","b²","=","(","a−b",")","(","a+b",")"), explanation="Cutting and translating the remaining L-shape forms a rectangle with sides a minus b and a plus b."),
        f("linear-balance", "Linear Equation Balance", "Algebra", "ax+b=c ⇒ x=(c−b)/a", "a x plus b equals c", FormulaVisualModel.Balance, listOf(v("a","x groups",1.0,6.0,2.0),v("b","constant",0.0,8.0,2.0),v("c","right side",2.0,20.0,10.0)), "equation-balance", listOf("x","=","(","c−b",")","/","a"), "Removing equal weights and then splitting both pans equally preserves balance."),
        f("rectangle-area", "Rectangle Area", "Geometry", "A=lw", "area equals length times width", FormulaVisualModel.Tiles, listOf(v("l","length",1.0,12.0,6.0),v("w","width",1.0,10.0,4.0)), tokens=listOf("A","=","l","×","w"), explanation="Every row contains l unit squares and there are w identical rows."),
        f("triangle-area", "Triangle Area", "Geometry", "A=½bh", "area equals one half base times perpendicular height", FormulaVisualModel.Geometry, listOf(v("base","base",.5,7.0,4.0),v("height","perpendicular height",.5,6.0,3.0),v("apex","apex position",-2.0,6.0,1.5)), "triangle-area", listOf("A","=","½","b","h"), "A congruent copy forms a parallelogram, so the original triangle occupies exactly half its area."),
        f("parallelogram-area", "Parallelogram Area", "Geometry", "A=bh", "area equals base times perpendicular height", FormulaVisualModel.Geometry, listOf(v("base","base",.5,7.0,4.0),v("height","height",.5,5.0,2.5),v("shear","slant",-3.0,3.0,1.2)), "parallelogram-area", listOf("A","=","b","h"), "Cutting and sliding the triangular end makes a rectangle without adding or removing area."),
        f("trapezium-area", "Trapezium Area", "Geometry", "A=½(a+b)h", "area equals one half the sum of parallel sides times height", FormulaVisualModel.Geometry, listOf(v("a","first base",.5,7.0,5.0),v("b","second base",.5,7.0,2.5),v("height","height",.5,5.0,2.5)), "trapezoid-area", listOf("A","=","½","(","a","+","b",")","h"), "A rotated copy forms a parallelogram with base a plus b; one trapezium is half."),
        f("circle-circumference", "Circle Circumference", "Geometry", "C=2πr=πd", "circumference equals two pi r", FormulaVisualModel.Circle, listOf(v("r","radius",.2,6.0,2.0)), "circle-ratio", listOf("C","=","2","π","r"), "Unrolling different circles shows circumference and diameter scale together while their ratio remains pi."),
        f("circle-area", "Circle Area", "Geometry", "A=πr²", "area equals pi r squared", FormulaVisualModel.Circle, listOf(v("r","radius",.4,5.0,2.2),v("n","sectors",6.0,60.0,12.0)), "circle-area", listOf("A","=","π","r²"), "Alternating thin sectors approaches a rectangle of base pi r and height r."),
        f("pythagorean", "Pythagorean Theorem", "Geometry", "a²+b²=c²", "a squared plus b squared equals c squared", FormulaVisualModel.Geometry, listOf(v("a","leg a",.5,6.0,3.0),v("b","leg b",.5,6.0,4.0)), "pythagorean", listOf("a²","+","b²","=","c²"), "Rearranging congruent triangles preserves the outer area, forcing the uncovered square areas to match."),
        f("polygon-sum", "Polygon Interior-Angle Sum", "Geometry", "S=(n−2)×180°", "angle sum equals n minus two times 180 degrees", FormulaVisualModel.Geometry, listOf(v("n","sides",3.0,12.0,6.0),v("radius","size",1.0,5.0,2.8)), "polygon-angle-sum", listOf("S","=","(","n−2",")","×","180°"), "Diagonals from one vertex partition a simple n-gon into exactly n minus two triangles."),
        f("arithmetic-series", "Arithmetic-Series Sum", "Sequences & Series", "Sₙ=n(a₁+aₙ)/2", "sum equals n times first plus last divided by two", FormulaVisualModel.Staircase, listOf(v("n","terms",2.0,12.0,6.0),v("a₁","first term",1.0,8.0,2.0),v("d","difference",1.0,6.0,2.0)), tokens=listOf("Sₙ","=","n","(","a₁","+","aₙ",")","/","2"), explanation="Forward and reversed copies make n equal pairs, each with sum first plus last."),
        f("probability-union", "Probability Union Formula", "Probability", "P(A∪B)=P(A)+P(B)−P(A∩B)", "probability of A union B adds A and B then subtracts their intersection", FormulaVisualModel.Venn, listOf(v("A","A count",1.0,20.0,8.0),v("B","B count",1.0,20.0,7.0),v("I","overlap",0.0,10.0,3.0)), tokens=listOf("P(A∪B)","=","P(A)","+","P(B)","−","P(A∩B)"), explanation="Adding A and B counts their overlap twice, so subtracting one overlap leaves every union outcome once."),
        f("arithmetic-mean", "Arithmetic Mean", "Statistics", "x̄=Σx/n", "mean equals the sum of values divided by their count", FormulaVisualModel.Redistribution, listOf(v("x₁","value 1",0.0,12.0,2.0),v("x₂","value 2",0.0,12.0,5.0),v("x₃","value 3",0.0,12.0,8.0),v("x₄","value 4",0.0,12.0,5.0)), tokens=listOf("x̄","=","Σx","/","n"), explanation="Redistributing blocks equally preserves the total; each of n towers receives total divided by n."),
    )

    fun find(id: String) = formulas.firstOrNull { it.id == id }
    fun search(query: String, category: String?): List<VisualFormulaDefinition> {
        val q = query.trim().lowercase()
        return formulas.filter { (category == null || it.category == category) &&
            (q.isBlank() || q in it.title.lowercase() || q in it.symbolicExpression.lowercase() || it.tags.any { tag -> q in tag }) }
    }
}

object FormulaEvaluationEngine {
    fun defaults(formula: VisualFormulaDefinition) = formula.variables.associate { it.symbol to it.defaultValue }

    fun evaluate(formula: VisualFormulaDefinition, raw: Map<String, Double>): FormulaEvaluation {
        val p = formula.variables.associate { variable ->
            variable.symbol to (raw[variable.symbol] ?: variable.defaultValue).coerceIn(variable.minimum, variable.maximum)
        }
        fun g(name: String) = p.getValue(name)
        val measurements = linkedMapOf<String, Double>()
        val result = when (formula.id) {
            "multiply-array" -> g("a") * g("b")
            "distributive" -> g("a") * (g("b") + g("c"))
            "natural-sum" -> g("n").toInt().let { it * (it + 1) / 2.0 }
            "odd-sum-square" -> g("n").toInt().toDouble().pow(2)
            "equivalent-fractions" -> g("a") / g("b")
            "fraction-product" -> g("a") * g("c") / (g("b") * g("d"))
            "binomial-square" -> (g("a") + g("b")).pow(2)
            "difference-squares" -> g("a").coerceAtLeast(g("b")) .pow(2) - g("b").pow(2)
            "linear-balance" -> (g("c") - g("b")) / g("a")
            "rectangle-area" -> g("l") * g("w")
            "triangle-area" -> g("base") * g("height") / 2
            "parallelogram-area" -> g("base") * g("height")
            "trapezium-area" -> (g("a") + g("b")) * g("height") / 2
            "circle-circumference" -> 2 * PI * g("r")
            "circle-area" -> PI * g("r").pow(2)
            "pythagorean" -> g("a").pow(2) + g("b").pow(2)
            "polygon-sum" -> (g("n").toInt() - 2) * 180.0
            "arithmetic-series" -> g("n").toInt().let { n -> n * (2 * g("a₁") + (n - 1) * g("d")) / 2 }
            "probability-union" -> g("A") + g("B") - g("I").coerceAtMost(minOf(g("A"), g("B")))
            else -> p.values.average()
        }
        measurements.putAll(p)
        measurements["result"] = result
        return FormulaEvaluation(measurements, result, 0.0)
    }

    fun validateAssembly(formula: VisualFormulaDefinition, tokens: List<String>) =
        tokens.map(::canonical) == formula.assemblyTokens.map(::canonical)

    private fun canonical(value: String) = value.replace(" ", "").replace("×", "*").replace("−", "-").lowercase()
}

object DiscoverySequenceEngine {
    fun start(formula: VisualFormulaDefinition) = FormulaDiscoveryState(formula.id, parameterValues = FormulaEvaluationEngine.defaults(formula))
    fun setParameter(state: FormulaDiscoveryState, formula: VisualFormulaDefinition, symbol: String, value: Double): FormulaDiscoveryState {
        val variable = formula.variables.firstOrNull { it.symbol == symbol } ?: return state
        val next = state.parameterValues + (symbol to value.coerceIn(variable.minimum, variable.maximum))
        return state.copy(parameterValues = next, investigatedCases = state.investigatedCases + 1, currentStage = maxOf(state.currentStage, DiscoveryStage.Manipulate))
    }
    fun advance(state: FormulaDiscoveryState): FormulaDiscoveryState {
        val next = DiscoveryStage.entries.getOrElse(state.currentStage.ordinal + 1) { DiscoveryStage.Apply }
        return state.copy(currentStage = next)
    }
}
