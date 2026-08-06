package com.indianservers.aiexplorer.solver.domain.catalogue

enum class SolverCalculatorGroup(val label: String) {
    ArithmeticPreAlgebra("Arithmetic and pre-algebra"),
    Algebra("Algebra"),
    FunctionsGraphs("Functions and graphs"),
    Trigonometry("Trigonometry"),
    Calculus("Calculus"),
    LinearAlgebra("Linear algebra"),
    SequencesSeries("Sequences and series"),
    ComplexNumbers("Complex numbers"),
}

data class SolverCalculatorPreset(
    val id: String,
    val title: String,
    val group: SolverCalculatorGroup,
    val starterExpression: String,
    val supported: Boolean,
    val description: String,
)

object SolverCalculatorCatalogue {
    val presets = listOf(
        preset("fraction", "Fraction calculator", SolverCalculatorGroup.ArithmeticPreAlgebra, "3/4 + 5/6"),
        preset("percentage", "Percentage calculator", SolverCalculatorGroup.ArithmeticPreAlgebra, "25% of 80"),
        preset("ratio", "Ratio calculator", SolverCalculatorGroup.ArithmeticPreAlgebra, "2/3 = x/12"),
        preset("scientific-notation", "Scientific notation", SolverCalculatorGroup.ArithmeticPreAlgebra, "3.2*10^5"),
        preset("prime-factor", "Prime factorisation", SolverCalculatorGroup.ArithmeticPreAlgebra, "primeFactors(84)"),
        preset("gcd-lcm", "GCD and LCM", SolverCalculatorGroup.ArithmeticPreAlgebra, "gcd(84,30)"),
        preset("base", "Base converter", SolverCalculatorGroup.ArithmeticPreAlgebra, "base(101101,2,10)"),
        preset("equation", "Equation solver", SolverCalculatorGroup.Algebra, "3x+5=20"),
        preset("inequality", "Inequality solver", SolverCalculatorGroup.Algebra, "-2x+3<=9"),
        preset("simplifier", "Simplifier", SolverCalculatorGroup.Algebra, "2(x+3)+4x"),
        preset("expander", "Expander", SolverCalculatorGroup.Algebra, "(x+2)(x-3)"),
        preset("factoriser", "Factoriser", SolverCalculatorGroup.Algebra, "x^2-5x+6=0"),
        preset("polynomial-division", "Polynomial division", SolverCalculatorGroup.Algebra, "divide x^3-1 by x-1"),
        preset("system", "System solver", SolverCalculatorGroup.Algebra, "2x+y=7; x-y=2"),
        preset(
            "partial-fraction",
            "Partial fractions",
            SolverCalculatorGroup.Algebra,
            "partial fractions (2*x+3)/((x-1)*(x+2))",
            supported = false,
        ),
        preset("domain", "Domain calculator", SolverCalculatorGroup.FunctionsGraphs, "domain sqrt(x-2)/(x-3)"),
        preset("inverse-function", "Inverse-function calculator", SolverCalculatorGroup.FunctionsGraphs, "inverse function f(x)=2x+3", supported = false),
        preset("composition", "Composition calculator", SolverCalculatorGroup.FunctionsGraphs, "f(x)=x^2+1; f(3)"),
        preset("intercept", "Intercept calculator", SolverCalculatorGroup.FunctionsGraphs, "intercepts y=x^2-4", supported = false),
        preset("line", "Line calculator", SolverCalculatorGroup.FunctionsGraphs, "gradient(1,2,4,8)"),
        preset("circle", "Circle calculator", SolverCalculatorGroup.FunctionsGraphs, "distance(0,0,3,4)"),
        preset("triangle", "Triangle solver", SolverCalculatorGroup.Trigonometry, "triangle sides 3,4,5", supported = false),
        preset("trig-identity", "Identity simplifier", SolverCalculatorGroup.Trigonometry, "simplify sin(x)^2+cos(x)^2", supported = false),
        preset("trig-equation", "Trigonometric equation solver", SolverCalculatorGroup.Trigonometry, "solve sin(x)=1/2", supported = false),
        preset("degree-radian", "Degree-radian converter", SolverCalculatorGroup.Trigonometry, "180deg to rad"),
        preset("unit-circle", "Unit-circle explorer", SolverCalculatorGroup.Trigonometry, "sin(30deg)"),
        preset("limit", "Limit calculator", SolverCalculatorGroup.Calculus, "limit (x^2-4)/(x-2) as x -> 2"),
        preset("derivative", "Derivative calculator", SolverCalculatorGroup.Calculus, "differentiate x^3+2*x"),
        preset("tangent-normal", "Tangent and normal calculator", SolverCalculatorGroup.Calculus, "tangent x^2 at x=1", supported = false),
        preset("integral", "Integral calculator", SolverCalculatorGroup.Calculus, "integrate x^2"),
        preset("area", "Area calculator", SolverCalculatorGroup.Calculus, "integrate x^2 from 0 to 2"),
        preset("differential-equation", "Differential-equation calculator", SolverCalculatorGroup.Calculus, "ode dy/dx=y", supported = false),
        preset("matrix", "Matrix calculator", SolverCalculatorGroup.LinearAlgebra, "matrixadd([[1,2],[3,4]],[[2,3],[4,5]])"),
        preset("determinant", "Determinant calculator", SolverCalculatorGroup.LinearAlgebra, "det([[1,2],[3,4]])"),
        preset("inverse", "Inverse calculator", SolverCalculatorGroup.LinearAlgebra, "inverse([[1,2],[3,4]])"),
        preset("row-reduction", "Row-reduction calculator", SolverCalculatorGroup.LinearAlgebra, "rref([[1,2],[3,4]])"),
        preset("linear-system", "Linear-system calculator", SolverCalculatorGroup.LinearAlgebra, "2x+y=7; x-y=2"),
        preset("eigenvalue", "Eigenvalue calculator", SolverCalculatorGroup.LinearAlgebra, "eigenvalues([[2,0],[0,3]])", supported = false),
        preset("arithmetic-sequence", "Arithmetic sequence", SolverCalculatorGroup.SequencesSeries, "arithmeticterm(3,2,10)"),
        preset("geometric-sequence", "Geometric sequence", SolverCalculatorGroup.SequencesSeries, "geometricsum(2,1/2,5)"),
        preset("sigma", "Sigma evaluator", SolverCalculatorGroup.SequencesSeries, "sum k from 1 to 10", supported = false),
        preset("convergence", "Convergence checker", SolverCalculatorGroup.SequencesSeries, "infinitegeometricsum(2,1/2)"),
        preset("complex", "Complex arithmetic", SolverCalculatorGroup.ComplexNumbers, "complex multiply 1+2i ; 3-4i"),
        preset("polar", "Polar conversion", SolverCalculatorGroup.ComplexNumbers, "complex 3+4i"),
        preset("complex-roots", "Complex roots", SolverCalculatorGroup.ComplexNumbers, "complex roots 1+i order 3"),
        preset("argand", "Argand representation", SolverCalculatorGroup.ComplexNumbers, "complex -2+5i"),
    )

    fun search(query: String): List<SolverCalculatorPreset> = presets.filter {
        query.isBlank() || it.title.contains(query, true) || it.group.label.contains(query, true) || it.description.contains(query, true)
    }

    private fun preset(
        id: String,
        title: String,
        group: SolverCalculatorGroup,
        starter: String,
        supported: Boolean = true,
    ) = SolverCalculatorPreset(
        id,
        title,
        group,
        starter,
        supported,
        if (supported) {
            "Routes through the shared offline Solver pipeline."
        } else {
            "Listed for future support and hidden from the active calculator picker."
        },
    )
}
