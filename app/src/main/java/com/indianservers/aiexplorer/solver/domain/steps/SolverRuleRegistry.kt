package com.indianservers.aiexplorer.solver.domain.steps

data class SolverRule(
    val id: String,
    val name: String,
    val explanation: String,
    val preservesEquivalence: Boolean,
)

object SolverRuleRegistry {
    const val ORDER_OF_OPERATIONS = "arithmetic.order-of-operations"
    const val EXACT_ARITHMETIC = "arithmetic.exact-evaluation"
    const val FRACTION_REDUCTION = "fractions.reduce-gcd"
    const val PERCENT_CONVERSION = "percent.divide-by-100"
    const val RATIO_REDUCTION = "ratio.reduce-gcd"
    const val PROPORTION_CROSS_PRODUCTS = "ratio.cross-products"
    const val DISTRIBUTIVE = "algebra.distributive"
    const val ASSOCIATIVE = "algebra.associative"
    const val COMMUTATIVE = "algebra.commutative"
    const val COMBINE_LIKE_TERMS = "algebra.combine-like-terms"
    const val LEAST_COMMON_DENOMINATOR = "fractions.least-common-denominator"
    const val SIGN_RULES = "arithmetic.sign-rules"
    const val EXPONENT_RULES = "algebra.exponent-rules"
    const val ADDITION_EQUALITY = "equality.addition-property"
    const val SUBTRACTION_EQUALITY = "equality.subtraction-property"
    const val MULTIPLICATION_EQUALITY = "equality.multiplication-property"
    const val DIVISION_EQUALITY = "equality.division-property"
    const val INEQUALITY_NEGATIVE = "inequality.reverse-when-negative"
    const val ELIMINATION = "systems.elimination"
    const val ZERO_PRODUCT = "algebra.zero-product"
    const val VERIFY_SUBSTITUTION = "verification.substitution"
    const val VERIFY_EQUIVALENCE = "verification.sampled-equivalence"
    const val QUADRATIC_FORMULA = "algebra.quadratic-formula"
    const val COMPLETING_SQUARE = "algebra.completing-square"
    const val FACTOR_THEOREM = "algebra.factor-theorem"
    const val REMAINDER_THEOREM = "algebra.remainder-theorem"
    const val RADICAL_SQUARING = "algebra.radical-squaring"
    const val LOG_DOMAIN = "functions.log-domain"
    const val FUNCTION_COMPOSITION = "functions.composition"
    const val PYTHAGOREAN_IDENTITY = "trigonometry.pythagorean-identity"
    const val SINE_RULE = "trigonometry.sine-rule"
    const val COSINE_RULE = "trigonometry.cosine-rule"
    const val GEOMETRIC_SUM = "series.geometric-sum"
    const val DISTANCE_FORMULA = "geometry.distance"
    const val MATRIX_PRODUCT = "matrix.product"
    const val MATRIX_INVERSE = "matrix.inverse"
    const val EUCLIDEAN_ALGORITHM = "number-theory.euclidean-algorithm"
    const val MODULAR_EQUIVALENCE = "number-theory.modular-equivalence"
    const val LIMIT_LAW = "calculus.limit-law"
    const val DERIVATIVE_POWER = "calculus.derivative-power-rule"
    const val PRODUCT_RULE = "calculus.product-rule"
    const val QUOTIENT_RULE = "calculus.quotient-rule"
    const val CHAIN_RULE = "calculus.chain-rule"
    const val INTEGRATION_POWER = "calculus.integration-power-rule"
    const val INTEGRATION_SUBSTITUTION = "calculus.integration-substitution"
    const val INTEGRATION_PARTS = "calculus.integration-by-parts"
    const val COMPLEX_RECTANGULAR = "complex.rectangular-arithmetic"
    const val COMPLEX_POLAR = "complex.polar-form"
    const val DE_MOIVRE = "complex.de-moivre"
    const val FORMULA_SUBSTITUTION = "school.formula-substitution"
    const val SCIENTIFIC_NOTATION = "school.scientific-notation"
    const val MENSURATION = "geometry.mensuration"
    const val COORDINATE_TRANSFORMATION = "geometry.coordinate-transformation"
    const val DATA_SUMMARY = "statistics.data-summary"
    const val EQUALLY_LIKELY_PROBABILITY = "probability.equally-likely-outcomes"
    const val RATIONALISATION = "number-system.rationalisation"
    const val IRRATIONALITY_PROOF = "number-system.irrationality-proof"
    const val POLYNOMIAL_DIVISION = "algebra.polynomial-division"
    const val ARITHMETIC_PROGRESSION = "series.arithmetic-progression"
    const val GEOMETRIC_PROOF = "geometry.deductive-proof"
    const val GEOMETRIC_CONSTRUCTION = "geometry.compass-straightedge-construction"
    const val EMPIRICAL_PROBABILITY = "probability.empirical-frequency"
    const val DATA_VISUALISATION = "statistics.data-visualisation"
    const val SET_MEMBERSHIP = "sets.membership-equivalence"
    const val INCLUSION_EXCLUSION = "sets.inclusion-exclusion"
    const val COMBINATORICS_COUNTING = "combinatorics.product-and-combination"
    const val BINOMIAL_THEOREM = "algebra.binomial-theorem"

    private val rules = listOf(
        SolverRule(ORDER_OF_OPERATIONS, "Order of operations", "Evaluate grouped expressions, powers, multiplication or division, then addition or subtraction.", true),
        SolverRule(EXACT_ARITHMETIC, "Exact arithmetic", "Calculate with reduced rational numbers instead of rounding to decimals.", true),
        SolverRule(FRACTION_REDUCTION, "Fraction reduction", "Divide numerator and denominator by their greatest common divisor.", true),
        SolverRule(PERCENT_CONVERSION, "Percentage conversion", "A percentage means the given number divided by 100.", true),
        SolverRule(RATIO_REDUCTION, "Ratio reduction", "Divide both parts of a ratio by their greatest common divisor.", true),
        SolverRule(PROPORTION_CROSS_PRODUCTS, "Cross-products property", "For two equal ratios with non-zero denominators, the cross products are equal.", true),
        SolverRule(DISTRIBUTIVE, "Distributive property", "Multiply the outside factor by every term inside the brackets.", true),
        SolverRule(ASSOCIATIVE, "Associative property", "Regroup addition or multiplication without changing the order of the terms.", true),
        SolverRule(COMMUTATIVE, "Commutative property", "Reorder terms joined by addition or multiplication.", true),
        SolverRule(COMBINE_LIKE_TERMS, "Combining like terms", "Add coefficients only when the variable parts are identical.", true),
        SolverRule(LEAST_COMMON_DENOMINATOR, "Least common denominator", "Rewrite fractions using the smallest shared denominator before combining them.", true),
        SolverRule(SIGN_RULES, "Sign rules", "Apply the multiplication and division rules for positive and negative values.", true),
        SolverRule(EXPONENT_RULES, "Exponent rules", "Apply exponent identities only when their base and exponent conditions are satisfied.", true),
        SolverRule(ADDITION_EQUALITY, "Addition property of equality", "Add the same value to both sides to keep the equation balanced.", true),
        SolverRule(SUBTRACTION_EQUALITY, "Subtraction property of equality", "Subtract the same value from both sides to keep the equation balanced.", true),
        SolverRule(MULTIPLICATION_EQUALITY, "Multiplication property of equality", "Multiply both sides by the same non-zero value.", true),
        SolverRule(DIVISION_EQUALITY, "Division property of equality", "Divide both sides by the same non-zero value.", true),
        SolverRule(INEQUALITY_NEGATIVE, "Negative inequality rule", "Multiplying or dividing by a negative reverses the inequality direction.", true),
        SolverRule(ELIMINATION, "Elimination", "Combine equations so that one variable cancels.", true),
        SolverRule(ZERO_PRODUCT, "Zero-product property", "If a product is zero, at least one factor must be zero.", true),
        SolverRule(VERIFY_SUBSTITUTION, "Substitution check", "Put the proposed value into the original relation and compare both sides.", true),
        SolverRule(VERIFY_EQUIVALENCE, "Equivalence sampling", "Evaluate both expressions at several safe exact values.", true),
        SolverRule(QUADRATIC_FORMULA, "Quadratic formula", "Substitute a, b and c and retain both square-root branches.", true),
        SolverRule(COMPLETING_SQUARE, "Completing the square", "Add and subtract the square of half the linear coefficient.", true),
        SolverRule(FACTOR_THEOREM, "Factor theorem", "A zero p(r) corresponds exactly to a factor x-r.", true),
        SolverRule(REMAINDER_THEOREM, "Remainder theorem", "The remainder on division by x-r equals p(r).", true),
        SolverRule(RADICAL_SQUARING, "Radical squaring", "Squaring removes a principal radical but every candidate must be checked.", false),
        SolverRule(LOG_DOMAIN, "Logarithm domain", "A real logarithm needs a positive argument and valid base.", true),
        SolverRule(FUNCTION_COMPOSITION, "Function composition", "Substitute the inner function into the outer function.", true),
        SolverRule(PYTHAGOREAN_IDENTITY, "Pythagorean identity", "Sine squared plus cosine squared equals one.", true),
        SolverRule(SINE_RULE, "Sine rule", "Opposite side-to-sine ratios are equal in a triangle.", true),
        SolverRule(COSINE_RULE, "Cosine rule", "Relate three sides and one included angle.", true),
        SolverRule(GEOMETRIC_SUM, "Geometric sum", "Multiply by the common ratio and subtract to telescope.", true),
        SolverRule(DISTANCE_FORMULA, "Distance formula", "Apply Pythagoras to coordinate differences.", true),
        SolverRule(MATRIX_PRODUCT, "Matrix product", "Multiply rows by columns across the shared dimension.", true),
        SolverRule(MATRIX_INVERSE, "Matrix inverse", "Row reduce an augmented identity and require full rank.", true),
        SolverRule(EUCLIDEAN_ALGORITHM, "Euclidean algorithm", "Replace a pair by divisor and remainder without changing the GCD.", true),
        SolverRule(MODULAR_EQUIVALENCE, "Modular equivalence", "Reduce integers to the same residue class.", true),
        SolverRule(LIMIT_LAW, "Limit law", "Transform only within a punctured neighbourhood and preserve the stated approach direction.", true),
        SolverRule(DERIVATIVE_POWER, "Derivative power rule", "Differentiate x to power n as n times x to power n minus one.", true),
        SolverRule(PRODUCT_RULE, "Product rule", "Differentiate a product as the first derivative times the second plus the first times the second derivative.", true),
        SolverRule(QUOTIENT_RULE, "Quotient rule", "Differentiate a quotient while requiring its denominator to remain non-zero.", true),
        SolverRule(CHAIN_RULE, "Chain rule", "Multiply the outer derivative by the derivative of the inner expression.", true),
        SolverRule(INTEGRATION_POWER, "Integration power rule", "Increase the power by one and divide by the new power, except at exponent negative one.", true),
        SolverRule(INTEGRATION_SUBSTITUTION, "Integration by substitution", "Replace an inner expression and its differential, then substitute back.", true),
        SolverRule(INTEGRATION_PARTS, "Integration by parts", "Reverse the product rule using integral u dv equals uv minus integral v du.", true),
        SolverRule(COMPLEX_RECTANGULAR, "Complex rectangular arithmetic", "Combine real components together and imaginary components together.", true),
        SolverRule(COMPLEX_POLAR, "Complex polar form", "Represent a complex value by its non-negative modulus and principal argument.", true),
        SolverRule(DE_MOIVRE, "De Moivre's theorem", "Multiply arguments for powers and distribute root arguments across all branches.", true),
        SolverRule(FORMULA_SUBSTITUTION, "Formula substitution", "Identify the required quantities, substitute them into the named formula, and evaluate with units.", true),
        SolverRule(SCIENTIFIC_NOTATION, "Scientific notation", "Move the decimal point so the leading number is at least one and less than ten, then record the signed power of ten.", true),
        SolverRule(MENSURATION, "Mensuration formula", "Select the formula matching the shape and substitute every given dimension using consistent units.", true),
        SolverRule(COORDINATE_TRANSFORMATION, "Coordinate transformation", "Apply the coordinate rule to every point while preserving the stated axis or transformation.", true),
        SolverRule(DATA_SUMMARY, "Data summary", "Order or aggregate the observations according to the requested mean, median, mode, or chart.", true),
        SolverRule(EQUALLY_LIKELY_PROBABILITY, "Equally likely probability", "Divide the number of favourable outcomes by the total number of equally likely outcomes.", true),
        SolverRule(RATIONALISATION, "Rationalising a denominator", "Multiply numerator and denominator by a conjugate so the denominator becomes rational.", true),
        SolverRule(IRRATIONALITY_PROOF, "Irrationality by contradiction", "Assume a radical is a reduced rational fraction, derive a common factor in numerator and denominator, and contradict lowest terms.", true),
        SolverRule(POLYNOMIAL_DIVISION, "Polynomial division", "Divide leading terms, multiply the divisor, subtract, and repeat until the remainder has lower degree.", true),
        SolverRule(ARITHMETIC_PROGRESSION, "Arithmetic progression formula", "Use the first term and common difference in the nth-term or finite-sum formula.", true),
        SolverRule(GEOMETRIC_PROOF, "Deductive geometry proof", "State the given facts, apply an established congruence or angle theorem, and explicitly reach the required conclusion.", true),
        SolverRule(GEOMETRIC_CONSTRUCTION, "Compass and straightedge construction", "Construct each required length or perpendicular with arcs, then verify the resulting figure against the given measurements.", true),
        SolverRule(EMPIRICAL_PROBABILITY, "Empirical probability", "Divide the observed frequency of the event by the total number of trials and reduce the fraction.", true),
        SolverRule(DATA_VISUALISATION, "Statistical visualisation", "Pair every class or category with its frequency, choose a consistent scale, and plot the data without changing the observations.", true),
        SolverRule(SET_MEMBERSHIP, "Set membership equivalence", "Prove two sets are equal by showing an arbitrary element belongs to the left side exactly when it belongs to the right side.", true),
        SolverRule(INCLUSION_EXCLUSION, "Inclusion-exclusion", "Add individual set sizes and subtract their overlap so shared members are counted exactly once.", true),
        SolverRule(COMBINATORICS_COUNTING, "Combinatorial counting", "Use the product rule for ordered choices and combinations when order does not matter.", true),
        SolverRule(BINOMIAL_THEOREM, "Binomial theorem", "Use the general term choose(n,r) a^(n-r) b^r to locate a requested term or coefficient.", true),
    ).associateBy(SolverRule::id)

    fun get(id: String): SolverRule = rules[id] ?: error("Unknown Solver rule '$id'")
    fun all(): Collection<SolverRule> = rules.values
}
