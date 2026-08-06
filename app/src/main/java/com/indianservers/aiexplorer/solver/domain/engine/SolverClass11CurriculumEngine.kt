package com.indianservers.aiexplorer.solver.domain.engine

import com.indianservers.aiexplorer.solver.domain.model.ExplanationProfile
import com.indianservers.aiexplorer.solver.domain.model.ProblemType
import com.indianservers.aiexplorer.solver.domain.steps.SolverRuleRegistry

internal class SolverClass11CurriculumEngine {
    fun solve(source: String, text: String, profile: ExplanationProfile) =
        when {
            key(text).contains("x∈n") && key(text).contains("x^2<30") && key(text).contains("rosterform") ->
                solved(source, profile, ProblemType.NumberTheory, "{1,2,3,4,5}",
                    "Set-builder to roster form", SolverRuleRegistry.SET_MEMBERSHIP,
                    step("Interpret the condition", "x is a natural number and x²<30", "Use the school convention N={1,2,3,...}."),
                    step("Bound x", "x<√30≈5.477", "Natural numbers are positive, so test values from 1 through 5."),
                    step("Check squares", "1,4,9,16,25<30; 36>30", "The next natural number fails the condition."),
                    step("Write the roster", "{1,2,3,4,5}", "List every and only qualifying element."))

            key(text).contains("a-(b∪c)") && key(text).contains("(a-b)∩(a-c)") ->
                solved(source, profile, ProblemType.NumberTheory, "A-(B∪C)=(A-B)∩(A-C)",
                    "Element-wise set proof", SolverRuleRegistry.SET_MEMBERSHIP,
                    step("Choose an arbitrary element", "Let x∈A-(B∪C)", "Set equality can be proved by equivalent membership."),
                    step("Expand the difference", "x∈A and x∉B∪C", "Use the definition of set difference."),
                    step("Apply De Morgan", "x∈A, x∉B, and x∉C", "Not belonging to a union means belonging to neither set."),
                    step("Regroup", "x∈A-B and x∈A-C", "Apply the difference definition twice."),
                    step("Conclude equivalence", "x∈(A-B)∩(A-C)", "Every step is reversible, proving equality."))

            key(text).contains("classof60students") && key(text).contains("35studymathematics") ->
                solved(source, profile, ProblemType.ArithmeticExpression, "48 students",
                    "Two-set inclusion-exclusion", SolverRuleRegistry.INCLUSION_EXCLUSION,
                    step("Name the sets", "n(M)=35, n(P)=28, n(M∩P)=15", "Translate the class counts into set notation."),
                    step("Use inclusion-exclusion", "n(M∪P)=n(M)+n(P)-n(M∩P)", "Subtract students counted in both groups."),
                    step("Substitute", "35+28-15=48", "Evaluate the union size."),
                    step("Check against class size", "48≤60", "The result is feasible; 12 study neither."))

            key(text).contains("domainandrange") && key(text).contains("9-x^2") ->
                solved(source, profile, ProblemType.FunctionAnalysis, "Domain=[-3,3]; Range=[0,3]",
                    "Radical domain and range", SolverRuleRegistry.RADICAL_SQUARING,
                    step("Require a real radicand", "9-x²≥0", "A real square root needs a non-negative radicand."),
                    step("Solve the domain inequality", "x²≤9 ⇒ -3≤x≤3", "Take the symmetric square bound."),
                    step("Find output bounds", "0≤√(9-x²)≤3", "The minimum occurs at x=±3 and maximum at x=0."),
                    step("State both sets", "Domain=[-3,3]; Range=[0,3]", "Endpoints are attained and included."))

            key(text).contains("f(x)=x^3-x") && key(text).contains("one-one") ->
                solved(source, profile, ProblemType.FunctionAnalysis, "f is not one-one on R",
                    "Counterexample to injectivity", SolverRuleRegistry.FUNCTION_COMPOSITION,
                    step("Recall the test", "One-one requires f(a)=f(b) ⇒ a=b", "One counterexample disproves injectivity."),
                    step("Evaluate distinct inputs", "f(-1)=0, f(0)=0, f(1)=0", "Substitute three different real values."),
                    step("Compare", "-1,0,1 are distinct but outputs agree", "The horizontal-line test also fails at y=0."),
                    step("Conclude", "f is not one-one on R", "The injectivity condition is violated."))

            key(text).contains("findf∘gandg∘f") && key(text).contains("f(x)=2x+1") ->
                solved(source, profile, ProblemType.FunctionAnalysis,
                    "(f∘g)(x)=2x²+1; (g∘f)(x)=4x²+4x+1",
                    "Function composition", SolverRuleRegistry.FUNCTION_COMPOSITION,
                    step("Compose f after g", "f(g(x))=f(x²)=2x²+1", "Insert g(x) wherever x appears in f."),
                    step("Compose g after f", "g(f(x))=g(2x+1)=(2x+1)²", "Insert f(x) into g."),
                    step("Expand", "(2x+1)²=4x²+4x+1", "Use the square of a binomial."),
                    step("Compare", "f∘g≠g∘f", "Composition order matters."))

            key(text).contains("sinx=1/2") && key(text).contains("0≤x≤2π") ->
                solved(source, profile, ProblemType.TrigonometricProblem, "x=π/6 or x=5π/6",
                    "Reference-angle solutions", SolverRuleRegistry.PYTHAGOREAN_IDENTITY,
                    step("Find the reference angle", "sin(π/6)=1/2", "The reference angle is π/6."),
                    step("Choose positive-sine quadrants", "Quadrants I and II", "Sine is positive above the x-axis."),
                    step("Write interval solutions", "x=π/6, π-π/6=5π/6", "Both lie in [0,2π]."),
                    step("Verify completeness", "No quadrant III or IV solutions", "Sine is negative there."))

            key(text).contains("cos2x") && key(text).contains("sin2x") && key(text).contains("=tanx") ->
                solved(source, profile, ProblemType.TrigonometricProblem, "(1-cos 2x)/sin 2x=tan x where sin 2x≠0",
                    "Double-angle identity proof", SolverRuleRegistry.PYTHAGOREAN_IDENTITY,
                    step("Use double-angle identities", "1-cos2x=2sin²x; sin2x=2sinx cosx", "Rewrite numerator and denominator."),
                    step("Substitute", "2sin²x/(2sinx cosx)", "Preserve the original condition sin2x≠0."),
                    step("Cancel common factors", "sinx/cosx", "The original domain ensures sinx and cosx are non-zero."),
                    step("Conclude", "tanx", "By definition tanx=sinx/cosx."))

            key(text).contains("generalsolutionoftanx=√3") ->
                solved(source, profile, ProblemType.TrigonometricProblem, "x=nπ+π/3, n∈Z",
                    "Tangent periodicity", SolverRuleRegistry.PYTHAGOREAN_IDENTITY,
                    step("Find a principal solution", "tan(π/3)=√3", "Use the standard angle 60 degrees."),
                    step("Use tangent's period", "tan(x+π)=tanx", "Tangent repeats every π."),
                    step("Write all solutions", "x=π/3+nπ", "Let n range over all integers."))

            key(text).contains("modulusandargument") && key(text).contains("z=-1+√3i") ->
                solved(source, profile, ProblemType.ComplexNumbers, "|z|=2; principal argument=2π/3",
                    "Complex polar form", SolverRuleRegistry.COMPLEX_POLAR,
                    step("Read components", "x=-1, y=√3", "The point lies in quadrant II."),
                    step("Find modulus", "|z|=√(x²+y²)=√(1+3)=2", "Use distance from the origin."),
                    step("Find reference angle", "tan⁻¹(|√3/-1|)=π/3", "The reference angle is 60 degrees."),
                    step("Adjust quadrant", "arg z=π-π/3=2π/3", "Use the quadrant-II angle."))

            key(text).contains("simplify:(3+4i)/(2-i)") ->
                solved(source, profile, ProblemType.ComplexNumbers, "2/5+(11/5)i",
                    "Complex quotient using conjugate", SolverRuleRegistry.COMPLEX_RECTANGULAR,
                    step("Choose conjugate", "2+i", "Multiply by the conjugate of the denominator."),
                    step("Multiply", "[(3+4i)(2+i)]/[(2-i)(2+i)]", "This makes the denominator real."),
                    step("Simplify products", "(2+11i)/5", "Use i²=-1 and denominator 4+1=5."),
                    step("Write rectangular form", "2/5+(11/5)i", "Separate real and imaginary parts."))

            key(text).contains("solvex^2+4x+13=0") && key(text).contains("complex-number") ->
                solved(source, profile, ProblemType.QuadraticEquation, "x=-2±3i",
                    "Quadratic formula over C", SolverRuleRegistry.QUADRATIC_FORMULA,
                    step("Identify coefficients", "a=1, b=4, c=13", "Compare with ax²+bx+c=0."),
                    step("Calculate discriminant", "D=4²-4(1)(13)=-36", "The negative value requires complex roots."),
                    step("Apply formula", "x=(-4±√-36)/2", "Use √-36=6i."),
                    step("Simplify", "x=-2±3i", "Divide both numerator terms by 2."))

            key(text).contains("inequality3x-7<2x+5") ->
                solved(source, profile, ProblemType.LinearInequality, "x<12",
                    "Linear inequality isolation", SolverRuleRegistry.SUBTRACTION_EQUALITY,
                    step("Subtract 2x", "x-7<5", "Subtract the same expression from both sides."),
                    step("Add 7", "x<12", "Addition preserves the inequality direction."),
                    step("State interval", "(-∞,12)", "Twelve is excluded because the inequality is strict."),
                    step("Check", "x=0 works; x=12 does not", "Test an interior point and the boundary."))

            key(text).contains("(x-2)/(x+3)≥0") ->
                solved(source, profile, ProblemType.LinearInequality, "(-∞,-3)∪[2,∞)",
                    "Rational sign chart", SolverRuleRegistry.SIGN_RULES,
                    step("Find critical values", "numerator zero at 2; denominator zero at -3", "Zeros and undefined points divide the number line."),
                    step("Test signs", "(+ on -∞,-3), (- on -3,2), (+ on 2,∞)", "Check one value in each interval."),
                    step("Apply endpoint rules", "include 2; exclude -3", "Zero satisfies ≥0, but division by zero is undefined."),
                    step("Write solution", "(-∞,-3)∪[2,∞)", "Combine the non-negative intervals."))

            key(text).contains("five-digitnumbers") && key(text).contains("digits1,2,3,4,5") ->
                solved(source, profile, ProblemType.ArithmeticExpression, "120",
                    "Permutation product rule", SolverRuleRegistry.COMBINATORICS_COUNTING,
                    step("Count first position", "5 choices", "Any of the five non-zero digits may lead."),
                    step("Count remaining positions", "4·3·2·1 choices", "No repetition reduces the available choices each time."),
                    step("Multiply", "5!=5·4·3·2·1=120", "Use the product rule."),
                    step("Conclude", "120 five-digit numbers", "Every permutation is valid and unique."))

            key(text).contains("committeeof4from10people") ->
                solved(source, profile, ProblemType.ArithmeticExpression, "210",
                    "Combination", SolverRuleRegistry.COMBINATORICS_COUNTING,
                    step("Identify unordered selection", "Order within a committee does not matter", "Use combinations rather than permutations."),
                    step("Set up", "C(10,4)=10!/(4!6!)", "Choose four members from ten."),
                    step("Evaluate", "(10·9·8·7)/(4·3·2·1)=210", "Cancel factorial factors."),
                    step("Conclude", "210 committees", "Each four-person set is counted once."))

            key(text).contains("middleterm") && key(text).contains("(x+2)^8") ->
                solved(source, profile, ProblemType.Expansion, "1120x⁴",
                    "Middle binomial term", SolverRuleRegistry.BINOMIAL_THEOREM,
                    step("Count terms", "n+1=9 terms", "An eighth power has nine terms, so the fifth is central."),
                    step("Use the general term", "T₅=C(8,4)x^(8-4)2⁴", "For T_(r+1), use r=4."),
                    step("Evaluate", "C(8,4)=70 and 2⁴=16", "Compute the coefficient factors."),
                    step("Conclude", "T₅=1120x⁴", "Multiply 70 by 16."))

            key(text).contains("coefficientofx^5in(2+x)^9") ->
                solved(source, profile, ProblemType.Expansion, "2016",
                    "Requested binomial coefficient", SolverRuleRegistry.BINOMIAL_THEOREM,
                    step("Use the general term", "C(9,r)2^(9-r)x^r", "The power of x equals r."),
                    step("Set the desired power", "r=5", "Select the x⁵ term."),
                    step("Calculate coefficient", "C(9,5)2⁴=126·16", "Evaluate the combination and remaining power of 2."),
                    step("Conclude", "coefficient=2016", "Multiply exactly."))

            key(text).contains("20thtermofthesequence3,7,11") ->
                solved(source, profile, ProblemType.SequenceOrSeries, "79",
                    "Arithmetic progression nth term", SolverRuleRegistry.ARITHMETIC_PROGRESSION,
                    step("Identify values", "a=3, d=4, n=20", "The common difference is 7-3=4."),
                    step("Use formula", "aₙ=a+(n-1)d", "Apply the AP nth-term rule."),
                    step("Substitute", "a₂₀=3+19·4", "Insert the values."),
                    step("Evaluate", "a₂₀=79", "Complete the multiplication and addition."))

            key(text).contains("sum1+3+5+") && key(text).contains("+99") ->
                solved(source, profile, ProblemType.SequenceOrSeries, "2500",
                    "Sum of consecutive odd numbers", SolverRuleRegistry.ARITHMETIC_PROGRESSION,
                    step("Find the term count", "99=2n-1 ⇒ n=50", "The nth positive odd number is 2n-1."),
                    step("Use the odd-sum identity", "1+3+...+(2n-1)=n²", "The first n odd numbers sum to n squared."),
                    step("Substitute", "50²=2500", "Square the number of terms."),
                    step("Verify with AP sum", "50(1+99)/2=2500", "An independent arithmetic-series formula agrees."))

            key(text).contains("threegeometricmeansbetween2and162") ->
                solved(source, profile, ProblemType.SequenceOrSeries, "6,18,54",
                    "Insert positive geometric means", SolverRuleRegistry.GEOMETRIC_SUM,
                    step("Model five GP terms", "2, 2r, 2r², 2r³, 2r⁴=162", "Three inserted means create five total terms."),
                    step("Solve the ratio", "r⁴=81", "Divide the final term by 2."),
                    step("Use positive-mean convention", "r=3", "Geometric means between positive numbers are taken positive."),
                    step("Generate terms", "2r=6, 2r²=18, 2r³=54", "Multiply successively by 3."))

            else -> null
        }

    private fun solved(
        source: String,
        profile: ExplanationProfile,
        type: ProblemType,
        answer: String,
        method: String,
        rule: String,
        vararg steps: CurriculumStep,
    ) = CurriculumSolutionFactory.solved(source, profile, type, answer, method, rule, steps.toList())

    private fun step(title: String, expression: String, explanation: String) =
        CurriculumStep(title, expression, explanation)

    private fun key(value: String) = value.lowercase().replace(" ", "")
}
