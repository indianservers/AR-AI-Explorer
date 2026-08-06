package com.indianservers.aiexplorer.solver.domain.steps

import com.indianservers.aiexplorer.solver.domain.model.ExplanationProfile

enum class RuleCategory {
    Arithmetic, Equality, Inequality, Algebra, Functions, Trigonometry, Series, Geometry, Matrix, NumberTheory,
}

data class RuleCondition(val statement: String)
data class RuleExample(val expression: String, val explanation: String)

data class MathRule(
    val id: String,
    val name: String,
    val category: RuleCategory,
    val formalStatement: String,
    val conditions: List<RuleCondition>,
    val childExplanation: String,
    val schoolExplanation: String,
    val universityExplanation: String,
    val rigorousExplanation: String,
    val examples: List<RuleExample> = emptyList(),
    val counterExamples: List<RuleExample> = emptyList(),
) {
    fun explanation(profile: ExplanationProfile): String = when (profile) {
        ExplanationProfile.ChildFriendly -> childExplanation
        ExplanationProfile.SchoolExamination -> schoolExplanation
        ExplanationProfile.University -> universityExplanation
        ExplanationProfile.Rigorous -> rigorousExplanation
    }
}

object MathRuleKnowledgeBase {
    private fun rule(
        id: String,
        name: String,
        category: RuleCategory,
        formal: String,
        condition: String,
        child: String,
        school: String,
        university: String,
        rigorous: String,
        example: String,
        counterExample: String? = null,
    ) = MathRule(
        id, name, category, formal, listOf(RuleCondition(condition)), child, school, university, rigorous,
        examples = listOf(RuleExample(example, school)),
        counterExamples = counterExample?.let { listOf(RuleExample(it, "This violates the stated condition.")) }.orEmpty(),
    )

    private val rules = listOf(
        rule(SolverRuleRegistry.ORDER_OF_OPERATIONS, "Order of operations", RuleCategory.Arithmetic, "Grouping, powers, products, then sums.", "Every displayed operation is defined.", "Work on the grouped pieces first.", "Apply the standard operation order.", "Evaluate the syntax tree by precedence.", "The precedence relation determines the unique parse before evaluation.", "2+3*4=14"),
        rule(SolverRuleRegistry.FRACTION_REDUCTION, "Fraction reduction", RuleCategory.Arithmetic, "a/b=(a/d)/(b/d)", "d is a non-zero common divisor.", "Split both numbers into equal groups.", "Divide numerator and denominator by their GCD.", "Normalize the rational pair by gcd(a,b).", "Multiplication by d/d preserves the represented rational number for d non-zero.", "18/24=3/4"),
        rule(SolverRuleRegistry.DISTRIBUTIVE, "Distributive law", RuleCategory.Algebra, "a(b+c)=ab+ac", "Products and sums are defined.", "Give the outside factor to every term.", "Multiply the outside factor by each bracketed term.", "Distribute multiplication over addition.", "This is the distributive axiom in the underlying ring.", "3(x+2)=3x+6", "(a+b)^2=a^2+b^2"),
        rule(SolverRuleRegistry.COMBINE_LIKE_TERMS, "Combine like terms", RuleCategory.Algebra, "ax+bx=(a+b)x", "Variable factors are identical.", "Count matching variable pieces together.", "Add coefficients of identical variable terms.", "Factor the common monomial and add coefficients.", "The reverse distributive law applies only to identical monomial factors.", "2x+5x=7x", "2x+5y=7xy"),
        rule(SolverRuleRegistry.DIVISION_EQUALITY, "Division property of equality", RuleCategory.Equality, "a=b implies a/c=b/c", "c is non-zero.", "Share both sides into the same number of groups.", "Divide both sides by the same non-zero value.", "Apply the bijection t -> t/c.", "Division by a non-zero scalar preserves equivalence; division by a possibly zero expression does not.", "6x=18 implies x=3", "x^2=x; divide by x and lose x=0"),
        rule(SolverRuleRegistry.INEQUALITY_NEGATIVE, "Negative inequality rule", RuleCategory.Inequality, "a<b implies ca>cb for c<0", "The multiplier or divisor is strictly negative.", "A negative flips the number line.", "Reverse the inequality after multiplying or dividing by a negative.", "Order reverses under multiplication by a negative scalar.", "The map t -> ct is order-reversing exactly when c<0.", "-2x<6 implies x>-3"),
        rule(SolverRuleRegistry.ZERO_PRODUCT, "Zero-product property", RuleCategory.Algebra, "ab=0 implies a=0 or b=0", "The coefficient domain has no zero divisors.", "A product is zero when at least one factor is zero.", "Set each factor equal to zero.", "Use the integral-domain zero-product property.", "Over an integral domain, the product vanishes iff at least one factor vanishes.", "(x-2)(x+3)=0"),
        rule(SolverRuleRegistry.QUADRATIC_FORMULA, "Quadratic formula", RuleCategory.Algebra, "x=(-b +/- sqrt(b^2-4ac))/(2a)", "a is non-zero.", "Put a, b and c into the quadratic recipe.", "Substitute coefficients into the quadratic formula.", "Solve the normalized quadratic using its discriminant.", "Completing the square derives both branches; the discriminant classifies the roots.", "2x^2-3x-2=0"),
        rule(SolverRuleRegistry.COMPLETING_SQUARE, "Completing the square", RuleCategory.Algebra, "x^2+bx=(x+b/2)^2-b^2/4", "The coefficient domain permits division by 2.", "Add the missing corner to make a square.", "Add and subtract (b/2)^2.", "Translate the quadratic into vertex form.", "The identity is an exact ring expansion and preserves equivalence when applied to both sides.", "x^2+6x=(x+3)^2-9"),
        rule(SolverRuleRegistry.FACTOR_THEOREM, "Factor theorem", RuleCategory.Algebra, "p(r)=0 iff (x-r) divides p(x)", "p is a polynomial over a field.", "A zero tells us a matching factor.", "Evaluate p(r); zero confirms the factor x-r.", "Use the evaluation homomorphism kernel.", "Polynomial division gives p(x)=(x-r)q(x)+p(r), proving the equivalence.", "p(2)=0 implies x-2 is a factor"),
        rule(SolverRuleRegistry.REMAINDER_THEOREM, "Remainder theorem", RuleCategory.Algebra, "remainder of p(x)/(x-r) is p(r)", "p is a polynomial.", "Put r into the polynomial to get the leftover.", "Evaluate p(r) to obtain the remainder.", "Apply Euclidean division by a monic linear polynomial.", "Uniqueness of polynomial division identifies the constant remainder as p(r).", "p(3)=5 gives remainder 5"),
        rule(SolverRuleRegistry.RADICAL_SQUARING, "Squaring a radical equation", RuleCategory.Algebra, "a=b implies a^2=b^2", "Every candidate is checked in the original equation.", "Square both sides, then check every answer.", "Squaring may introduce extraneous roots, so substitute back.", "Squaring is implication, not generally equivalence.", "The squaring map is not injective over the reals; reverse validation is mandatory.", "sqrt(x)=x-2"),
        rule(SolverRuleRegistry.LOG_DOMAIN, "Logarithm domain", RuleCategory.Functions, "log_b(x) exists for x>0", "b>0 and b!=1.", "A real logarithm only accepts a positive input.", "Require positive arguments and a valid base.", "Restrict every logarithm argument to the positive reals.", "The real exponential maps R bijectively to (0,infinity), with base positive and not one.", "ln(x-1) requires x>1", "ln(-2)"),
        rule(SolverRuleRegistry.FUNCTION_COMPOSITION, "Function composition", RuleCategory.Functions, "(f o g)(x)=f(g(x))", "g(x) lies in the domain of f.", "Do the inside function first.", "Substitute g(x) for the input of f.", "Compose maps while respecting codomain-domain compatibility.", "Composition is defined only on g^-1(domain(f)).", "f(x)=x^2, g(x)=x+1"),
        rule(SolverRuleRegistry.PYTHAGOREAN_IDENTITY, "Pythagorean identity", RuleCategory.Trigonometry, "sin^2(t)+cos^2(t)=1", "Angles share the same argument.", "The two unit-circle squares add to one.", "Use the Pythagorean identity.", "Apply the unit-circle norm identity.", "It follows from x^2+y^2=1 under x=cos(t), y=sin(t).", "sin^2(t)+cos^2(t)=1"),
        rule(SolverRuleRegistry.SINE_RULE, "Sine rule", RuleCategory.Trigonometry, "a/sin(A)=b/sin(B)=c/sin(C)", "Each side is opposite its named angle.", "Match each side with the angle across from it.", "Use the sine rule with opposite side-angle pairs.", "Apply the extended law of sines.", "The relation follows from equal area expressions or the circumradius theorem.", "a/sin(A)=b/sin(B)"),
        rule(SolverRuleRegistry.COSINE_RULE, "Cosine rule", RuleCategory.Trigonometry, "c^2=a^2+b^2-2ab cos(C)", "C is the included angle between a and b.", "Use two sides and the angle between them.", "Apply the cosine rule with the included angle.", "Resolve one side by an inner-product identity.", "This is the polarization identity for the difference of two side vectors.", "c^2=a^2+b^2-2ab cos(C)"),
        rule(SolverRuleRegistry.GEOMETRIC_SUM, "Geometric series sum", RuleCategory.Series, "S_n=a(1-r^n)/(1-r)", "r!=1; infinite sum additionally needs |r|<1.", "Multiply by r and subtract to make most terms disappear.", "Use the finite geometric-sum formula and check convergence for infinity.", "Apply the telescoping identity after multiplying by r.", "For infinity, r^n tends to zero iff |r|<1.", "1+1/2+1/4+...=2", "1+2+4+... has no finite sum"),
        rule(SolverRuleRegistry.DISTANCE_FORMULA, "Distance formula", RuleCategory.Geometry, "d=sqrt((x2-x1)^2+(y2-y1)^2)", "Coordinates are in a Euclidean Cartesian plane.", "Make a right triangle between the points.", "Apply Pythagoras to horizontal and vertical differences.", "Use the Euclidean norm of the displacement vector.", "Distance is the norm induced by the standard inner product.", "(0,0) to (3,4) is 5"),
        rule(SolverRuleRegistry.MATRIX_PRODUCT, "Matrix multiplication", RuleCategory.Matrix, "(AB)_ij=sum_k A_ik B_kj", "Columns of A equal rows of B.", "Match a row with a column.", "Use row-by-column products.", "Contract the shared matrix dimension.", "Composition of linear maps determines the non-commutative product.", "[[1,2]]*[[3],[4]]=[[11]]", "AB=BA"),
        rule(SolverRuleRegistry.MATRIX_INVERSE, "Matrix inverse", RuleCategory.Matrix, "AA^-1=A^-1A=I", "A is square and det(A)!=0.", "The inverse undoes the matrix.", "Row reduce [A|I] and require a pivot in every column.", "Invert the associated bijective linear map.", "A finite square matrix is invertible iff its determinant is non-zero.", "inverse([[1,0],[0,2]])"),
        rule(SolverRuleRegistry.EUCLIDEAN_ALGORITHM, "Euclidean algorithm", RuleCategory.NumberTheory, "gcd(a,b)=gcd(b,a mod b)", "a and b are integers, not both zero.", "Keep replacing with the remainder.", "Repeat division with remainder until zero.", "Use invariance of common divisors under remainder replacement.", "The ideals generated by (a,b) and (b,a-qb) are equal.", "gcd(48,18)=6"),
        rule(SolverRuleRegistry.MODULAR_EQUIVALENCE, "Modular equivalence", RuleCategory.NumberTheory, "a congruent b mod n iff n divides a-b", "n is a positive integer modulus.", "The numbers leave the same remainder.", "Reduce both integers modulo n.", "Work in the quotient ring Z/nZ.", "Congruence is the equivalence relation induced by the ideal nZ.", "17 congruent 5 mod 12"),
    ).associateBy(MathRule::id)

    fun get(id: String): MathRule? = rules[id]
    fun all(): Collection<MathRule> = rules.values
}

object SolverExplanationEngine {
    fun explanation(ruleId: String, profile: ExplanationProfile, fallback: String): String =
        MathRuleKnowledgeBase.get(ruleId)?.explanation(profile) ?: fallback
}
