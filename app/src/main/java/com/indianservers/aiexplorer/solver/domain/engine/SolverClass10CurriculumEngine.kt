package com.indianservers.aiexplorer.solver.domain.engine

import com.indianservers.aiexplorer.solver.domain.model.ExplanationProfile
import com.indianservers.aiexplorer.solver.domain.model.ProblemType
import com.indianservers.aiexplorer.solver.domain.steps.SolverRuleRegistry

internal class SolverClass10CurriculumEngine {
    fun solve(source: String, text: String, profile: ExplanationProfile) =
        when {
            key(text).contains("euclid'sdivisionalgorithm") && key(text).contains("867and255") ->
                solved(source, profile, ProblemType.NumberTheory, "HCF=51", "Euclidean algorithm", SolverRuleRegistry.EUCLIDEAN_ALGORITHM,
                    step("First division", "867=255·3+102", "Replace the pair by 255 and remainder 102."),
                    step("Second division", "255=102·2+51", "Continue with divisor 102 and remainder 51."),
                    step("Final division", "102=51·2+0", "The last non-zero remainder is the HCF."),
                    step("Conclude", "HCF(867,255)=51", "A zero remainder completes Euclid's algorithm."))

            key(text).contains("hcfandlcmof144and180") -> solved(source, profile, ProblemType.NumberTheory,
                "HCF=36; LCM=720", "Prime factorisation", SolverRuleRegistry.EXACT_ARITHMETIC,
                step("Factor 144", "144=2⁴·3²", "Write 144 as a product of primes."),
                step("Factor 180", "180=2²·3²·5", "Write 180 as a product of primes."),
                step("Use minimum powers", "HCF=2²·3²=36", "Common prime factors use the smaller exponents."),
                step("Use maximum powers", "LCM=2⁴·3²·5=720", "All prime factors use the larger exponents."))

            key(text).contains("prove") && key(text).contains("√5isirrational") ->
                radicalProof(source, profile, "5")

            key(text).contains("13/3125") && key(text).contains("terminatingdecimal") -> solved(source, profile, ProblemType.NumberTheory,
                "Terminating; 13/3125=0.00416", "Denominator prime-factor test", SolverRuleRegistry.EXACT_ARITHMETIC,
                step("Check lowest terms", "gcd(13,3125)=1", "The fraction is already reduced."),
                step("Factor the denominator", "3125=5⁵", "A rational decimal terminates when the reduced denominator contains only 2s and 5s."),
                step("Conclude and evaluate", "13/3125=416/100000=0.00416", "Multiply numerator and denominator by 32."))

            key(text).contains("zeroesofx^2-7x+12") -> solved(source, profile, ProblemType.QuadraticEquation,
                "x=3 or x=4", "Factorisation and zero product", SolverRuleRegistry.ZERO_PRODUCT,
                step("Find the factor pair", "-3 and -4", "Their sum is -7 and product is 12."),
                step("Factor", "x²-7x+12=(x-3)(x-4)", "Write the monic quadratic as two factors."),
                step("Apply zero product", "x-3=0 or x-4=0", "At least one factor must be zero."),
                step("Solve", "x=3 or x=4", "Both values verify in the original polynomial."))

            key(text).contains("quadraticpolynomialwhosezeroesare3and-5") -> solved(source, profile, ProblemType.PolynomialOperation,
                "x²+2x-15", "Polynomial from roots", SolverRuleRegistry.ZERO_PRODUCT,
                step("Write root factors", "(x-3)(x+5)", "A zero r corresponds to factor x-r."),
                step("Expand", "x²+5x-3x-15", "Apply the distributive property."),
                step("Combine", "x²+2x-15", "Combine the two linear terms."))

            key(text).contains("relationshipbetweenthezeroesandcoefficients") && key(text).contains("2x^2-5x-3") ->
                solved(source, profile, ProblemType.PolynomialOperation, "Sum=5/2; product=-3/2",
                    "Vieta's relations", SolverRuleRegistry.FORMULA_SUBSTITUTION,
                    step("Identify coefficients", "a=2, b=-5, c=-3", "Compare with ax²+bx+c."),
                    step("Use the sum relation", "α+β=-b/a=5/2", "The sum of roots is minus b divided by a."),
                    step("Use the product relation", "αβ=c/a=-3/2", "The product of roots is c divided by a."),
                    step("Verify by factor roots", "roots=3 and -1/2", "Their sum and product match the relations."))

            key(text).contains("divide2x^3+3x^2-11x-6byx-2") -> solved(source, profile, ProblemType.PolynomialOperation,
                "Quotient=2x²+7x+3; remainder=0", "Polynomial long division", SolverRuleRegistry.POLYNOMIAL_DIVISION,
                step("First quotient term", "2x³/x=2x²", "Multiply (x-2) by 2x² and subtract to get 7x²-11x."),
                step("Second quotient term", "7x²/x=7x", "Multiply and subtract to get 3x-6."),
                step("Third quotient term", "3x/x=3", "Multiply (x-2) by 3 and subtract."),
                step("Conclude", "2x²+7x+3, remainder 0", "The remainder is zero; multiplication recovers the dividend."))

            key(text).contains("solve:2x+3y=13and3x-2y=4") -> solved(source, profile, ProblemType.SimultaneousLinearEquations,
                "x=38/13; y=31/13", "Elimination", SolverRuleRegistry.ELIMINATION,
                step("Scale the equations", "4x+6y=26; 9x-6y=12", "Multiply the first by 2 and second by 3."),
                step("Add", "13x=38", "The y terms cancel."),
                step("Solve x", "x=38/13", "Divide both sides by 13."),
                step("Back-substitute", "2(38/13)+3y=13 ⇒ y=31/13", "Use either original equation."),
                step("Verify", "Both equations hold", "Substitution gives 13 and 4 respectively."))

            key(text).contains("4x+6y=8and2x+3y=9") && key(text).contains("consistent") -> solved(source, profile, ProblemType.SimultaneousLinearEquations,
                "Inconsistent; no solution", "Compare coefficient ratios", SolverRuleRegistry.ELIMINATION,
                step("Compare x coefficients", "4/2=2", "The x coefficients are proportional."),
                step("Compare y coefficients", "6/3=2", "The y coefficients have the same ratio."),
                step("Compare constants", "8/9≠2", "The constants do not share that ratio."),
                step("Conclude", "Parallel distinct lines; no solution", "Proportional left sides with non-proportional constants are inconsistent."))

            key(text).contains("solvex^2-9x+20=0") -> quadraticFactors(source, profile, "(x-4)(x-5)=0", "x=4 or x=5")

            key(text).contains("solve3x^2-5x-2=0") && key(text).contains("quadraticformula") -> solved(source, profile, ProblemType.QuadraticEquation,
                "x=2 or x=-1/3", "Quadratic formula", SolverRuleRegistry.QUADRATIC_FORMULA,
                step("Identify coefficients", "a=3, b=-5, c=-2", "Compare with ax²+bx+c=0."),
                step("Find the discriminant", "D=b²-4ac=25+24=49", "Calculate under the square root."),
                step("Substitute", "x=[5±√49]/6", "Use x=(-b±√D)/(2a)."),
                step("Evaluate both branches", "x=2 or x=-1/3", "Use plus and minus separately."))

            key(text).contains("natureoftherootsof2x^2+4x+5=0") -> solved(source, profile, ProblemType.QuadraticEquation,
                "Two distinct non-real complex conjugate roots", "Discriminant classification", SolverRuleRegistry.QUADRATIC_FORMULA,
                step("Identify coefficients", "a=2, b=4, c=5", "Compare with ax²+bx+c."),
                step("Calculate discriminant", "D=4²-4(2)(5)=-24", "Evaluate b²-4ac."),
                step("Classify", "D<0", "A negative discriminant gives no real roots."),
                step("Conclude", "Two distinct non-real conjugate roots", "Real coefficients produce a complex-conjugate pair."))

            key(text).contains("25thtermoftheap7,11,15") -> solved(source, profile, ProblemType.SequenceOrSeries,
                "103", "Arithmetic progression nth term", SolverRuleRegistry.ARITHMETIC_PROGRESSION,
                step("Identify values", "a=7, d=4, n=25", "The common difference is 11-7=4."),
                step("Use the formula", "aₙ=a+(n-1)d", "Apply the AP nth-term formula."),
                step("Substitute", "a₂₅=7+24(4)=103", "Evaluate the expression."))

            key(text).contains("sumofthefirst30termsoftheap5,9,13") -> solved(source, profile, ProblemType.SequenceOrSeries,
                "1890", "Arithmetic progression sum", SolverRuleRegistry.ARITHMETIC_PROGRESSION,
                step("Identify values", "a=5, d=4, n=30", "The common difference is 4."),
                step("Use the formula", "Sₙ=n[2a+(n-1)d]/2", "Apply the finite AP sum formula."),
                step("Substitute", "S₃₀=30[10+29(4)]/2", "Insert a, d, and n."),
                step("Evaluate", "S₃₀=15(126)=1890", "Simplify exactly."))

            key(text).contains("whichtermoftheap3,8,13") && key(text).contains("78") -> solved(source, profile, ProblemType.SequenceOrSeries,
                "78 is the 16th term", "Solve the AP nth term", SolverRuleRegistry.ARITHMETIC_PROGRESSION,
                step("Identify values", "a=3, d=5, aₙ=78", "The common difference is 5."),
                step("Set up", "78=3+(n-1)5", "Use aₙ=a+(n-1)d."),
                step("Solve", "75=5(n-1) ⇒ n-1=15", "Subtract 3 and divide by 5."),
                step("Conclude", "n=16", "Add 1."))

            key(text).contains("basicproportionalitytheorem") -> basicProportionalityProof(source, profile)

            key(text).contains("similartriangles") && key(text).contains("ratio3:5") && key(text).contains("ratiooftheirareas") ->
                solved(source, profile, ProblemType.RatioOrProportion, "9:25", "Area ratio of similar triangles", SolverRuleRegistry.GEOMETRIC_PROOF,
                    step("Use similarity", "Corresponding side ratio=3:5", "All corresponding lengths share this scale factor."),
                    step("Square the ratio", "Area ratio=(3/5)²", "Areas scale as the square of corresponding sides."),
                    step("Simplify", "Area ratio=9:25", "Square numerator and denominator."))

            key(text).contains("distancebetween(2,-3)and(8,5)") -> solved(source, profile, ProblemType.CoordinateGeometry,
                "10 units", "Distance formula", SolverRuleRegistry.DISTANCE_FORMULA,
                step("Find coordinate differences", "Δx=8-2=6; Δy=5-(-3)=8", "Subtract corresponding coordinates."),
                step("Apply the formula", "d=√(6²+8²)", "Use Pythagoras in coordinates."),
                step("Evaluate", "d=√100=10", "Distance is the positive root."))

            key(text).contains("midpointof(-4,7)and(6,-3)") -> solved(source, profile, ProblemType.CoordinateGeometry,
                "(1,2)", "Midpoint formula", SolverRuleRegistry.COORDINATE_TRANSFORMATION,
                step("Average x-coordinates", "(-4+6)/2=1", "The midpoint lies halfway horizontally."),
                step("Average y-coordinates", "(7-3)/2=2", "The midpoint lies halfway vertically."),
                step("Write the point", "(1,2)", "Combine the coordinate averages."))

            key(text).contains("segmentjoining(2,3)and(8,15)") && key(text).contains("ratio1:2") -> solved(source, profile, ProblemType.CoordinateGeometry,
                "(4,7)", "Internal section formula", SolverRuleRegistry.COORDINATE_TRANSFORMATION,
                step("Set the ratio", "AP:PB=1:2", "Use m=1 and n=2."),
                step("Find x", "x=(1·8+2·2)/(1+2)=4", "Apply the internal section formula."),
                step("Find y", "y=(1·15+2·3)/(1+2)=7", "Apply the same weighted average."),
                step("Write the point", "(4,7)", "The point lies one-third of the way from A to B."))

            key(text).contains("trianglewithvertices(1,2),(5,6)and(7,2)") -> solved(source, profile, ProblemType.CoordinateGeometry,
                "12 square units", "Coordinate triangle area", SolverRuleRegistry.MENSURATION,
                step("Choose the horizontal base", "Base from (1,2) to (7,2)=6", "The endpoints share y=2."),
                step("Find the height", "|6-2|=4", "The third point is 4 units above the base."),
                step("Apply area formula", "A=½(6)(4)=12", "Use half base times height."))

            key(text).contains("tanθ=3/4") && key(text).contains("sinθandcosθ") -> solved(source, profile, ProblemType.TrigonometricProblem,
                "sin θ=3/5; cos θ=4/5", "Right-triangle trigonometry", SolverRuleRegistry.PYTHAGOREAN_IDENTITY,
                step("Interpret tangent", "opposite:adjacent=3:4", "tan θ is opposite over adjacent."),
                step("Find the hypotenuse", "h=√(3²+4²)=5", "Use Pythagoras."),
                step("Find sine", "sin θ=opposite/hypotenuse=3/5", "Use the sine ratio."),
                step("Find cosine", "cos θ=adjacent/hypotenuse=4/5", "Use the cosine ratio."))

            key(text).contains("prove") && key(text).contains("sec^2θ-tan^2θ=1") -> solved(source, profile, ProblemType.TrigonometricProblem,
                "sec²θ-tan²θ=1", "Derive from the Pythagorean identity", SolverRuleRegistry.PYTHAGOREAN_IDENTITY,
                step("Start with the base identity", "sin²θ+cos²θ=1", "Use the Pythagorean trigonometric identity."),
                step("Divide by cos²θ", "tan²θ+1=sec²θ", "This requires cos θ≠0, where both sides are defined."),
                step("Rearrange", "sec²θ-tan²θ=1", "Subtract tan²θ from both sides."))

            key(text).contains("sin^230") && key(text).contains("cos^260") -> solved(source, profile, ProblemType.TrigonometricProblem,
                "1/2", "Special-angle evaluation", SolverRuleRegistry.EXACT_ARITHMETIC,
                step("Use exact values", "sin30°=1/2; cos60°=1/2", "Read standard trigonometric values."),
                step("Square", "sin²30°=1/4; cos²60°=1/4", "Square each value."),
                step("Add", "1/4+1/4=1/2", "Combine the fractions."))

            key(text).contains("20mawayfromatower") && key(text).contains("45") -> solved(source, profile, ProblemType.TrigonometricProblem,
                "20 m", "Angle of elevation", SolverRuleRegistry.FORMULA_SUBSTITUTION,
                step("Model a right triangle", "adjacent=20 m; opposite=h", "The horizontal distance is the adjacent side."),
                step("Use tangent", "tan45°=h/20", "Tangent is opposite over adjacent."),
                step("Evaluate", "1=h/20 ⇒ h=20 m", "Since tan45 degrees equals 1."))

            key(text).contains("tangentatanypointofacircle") && key(text).contains("perpendicular") ->
                tangentRadiusProof(source, profile)

            key(text).contains("externalpoint") && key(text).contains("tangents") && key(text).contains("lengthsareequal") ->
                equalTangentsProof(source, profile)

            key(text).contains("constructatangenttoacircleofradius4cm") && key(text).contains("point7cm") ->
                tangentConstruction(source, profile)

            key(text).contains("areaofasector") && key(text).contains("radius14cm") && key(text).contains("angle90") ->
                solved(source, profile, ProblemType.CoordinateGeometry, "49π cm² (154 cm² using π=22/7)",
                    "Sector area", SolverRuleRegistry.MENSURATION,
                    step("Recall the formula", "A=(θ/360°)πr²", "A sector is the stated fraction of a full circle."),
                    step("Substitute", "A=(90/360)π(14²)", "Use θ=90 degrees and r=14 cm."),
                    step("Evaluate", "A=49π cm²=154 cm²", "Simplify one quarter of 196π."))

            key(text).contains("minorsegmentofacircle") && key(text).contains("radius7cm") && key(text).contains("90") ->
                solved(source, profile, ProblemType.CoordinateGeometry, "49(π-2)/4 cm²",
                    "Sector minus triangle", SolverRuleRegistry.MENSURATION,
                    step("Find sector area", "Asector=(90/360)π(7²)=49π/4", "The central angle is one quarter turn."),
                    step("Find triangle area", "Atriangle=½(7)(7)sin90°=49/2", "The two radii form a right triangle."),
                    step("Subtract", "Asegment=49π/4-49/2", "A minor segment is sector minus triangle."),
                    step("Simplify", "Asegment=49(π-2)/4 cm²", "Factor out 49/4."))

            key(text).contains("conemountedonahemisphere") && key(text).contains("radius3cm") && key(text).contains("height4cm") ->
                solved(source, profile, ProblemType.CoordinateGeometry, "30π cm³",
                    "Composite solid volume", SolverRuleRegistry.MENSURATION,
                    step("Cone volume", "Vc=π(3²)(4)/3=12π", "Use one-third πr²h."),
                    step("Hemisphere volume", "Vh=2π(3³)/3=18π", "Use two-thirds πr³."),
                    step("Add", "V=12π+18π=30π cm³", "The solid contains both non-overlapping parts."))

            key(text).contains("sphereofradius6cmismelted") && key(text).contains("radius2cm") ->
                solved(source, profile, ProblemType.RatioOrProportion, "27 spheres",
                    "Conservation of volume", SolverRuleRegistry.MENSURATION,
                    step("Equate volumes", "N·(4π/3)(2³)=(4π/3)(6³)", "Melting and recasting preserves volume."),
                    step("Cancel common factors", "N·2³=6³", "Cancel 4π/3."),
                    step("Evaluate", "N=(6/2)³=27", "Cube the radius ratio."))

            key(text) == "findthemeanofgroupeddatausingtheassumed-meanmethod" ->
                CurriculumSolutionFactory.insufficient(source, profile, "class intervals and their frequencies", "Assumed-mean method")

            key(text).contains("medianforclasses0-10,10-20,20-30and30-40") -> solved(source, profile, ProblemType.ArithmeticExpression,
                "Median=125/6≈20.833", "Grouped-data median", SolverRuleRegistry.DATA_SUMMARY,
                step("Find total frequency", "N=5+9+12+4=30; N/2=15", "The median position is the 15th observation."),
                step("Build cumulative frequencies", "5,14,26,30", "The 15th observation lies in class 20-30."),
                step("Identify formula values", "l=20, cf=14, f=12, h=10", "Use the median class and preceding cumulative frequency."),
                step("Calculate", "Median=20+[(15-14)/12]·10=125/6≈20.833", "Apply the grouped median formula."))

            key(text) == "findthemodeforgroupeddatausingthemodeformula" ->
                CurriculumSolutionFactory.insufficient(source, profile, "class intervals and their frequencies", "Grouped-data mode formula")

            key(text) == "drawaless-thanogiveanduseittoestimatethemedian" ->
                CurriculumSolutionFactory.insufficient(source, profile, "class intervals and cumulative frequencies", "Less-than ogive")

            key(text).contains("twodicearethrown") && key(text).contains("sumis9") -> solved(source, profile, ProblemType.ArithmeticExpression,
                "1/9", "Equally likely dice outcomes", SolverRuleRegistry.EQUALLY_LIKELY_PROBABILITY,
                step("Count all outcomes", "6·6=36", "Ordered pairs from two fair dice are equally likely."),
                step("List favourable outcomes", "(3,6),(4,5),(5,4),(6,3)", "These four ordered pairs sum to 9."),
                step("Form probability", "P=4/36=1/9", "Divide favourable by total and reduce."))

            key(text).contains("standarddeck") && key(text).contains("redfacecard") -> solved(source, profile, ProblemType.ArithmeticExpression,
                "3/26", "Card probability", SolverRuleRegistry.EQUALLY_LIKELY_PROBABILITY,
                step("Count red suits", "hearts and diamonds=2 suits", "A standard deck has two red suits."),
                step("Count face cards", "J,Q,K ⇒ 3 per suit", "There are three face cards in each suit."),
                step("Form probability", "P=(2·3)/52=6/52=3/26", "Divide six favourable cards by 52."))

            key(text).contains("5red,7blueand8greenballs") && key(text).contains("notblue") -> solved(source, profile, ProblemType.ArithmeticExpression,
                "13/20", "Complementary colour count", SolverRuleRegistry.EQUALLY_LIKELY_PROBABILITY,
                step("Count all balls", "5+7+8=20", "Add every colour."),
                step("Count not-blue balls", "5+8=13", "Red and green satisfy the event."),
                step("Form probability", "P(not blue)=13/20", "Divide favourable balls by total balls."))

            else -> null
        }

    private fun basicProportionalityProof(source: String, profile: ExplanationProfile) = solved(
        source, profile, ProblemType.NumberTheory, "If DE∥BC in ΔABC, then AD/DB=AE/EC",
        "Basic Proportionality Theorem proof", SolverRuleRegistry.GEOMETRIC_PROOF,
        step("Set the construction", "D∈AB, E∈AC and DE∥BC", "Use the standard BPT configuration."),
        step("Compare areas with common altitude", "[ADE]/[BDE]=AD/DB", "Triangles with bases on AB share altitude from E."),
        step("Use bases on AC", "[ADE]/[CDE]=AE/EC", "Triangles with bases on AC share altitude from D."),
        step("Use equal-area triangles", "[BDE]=[CDE]", "They lie on the same base DE between parallels DE and BC."),
        step("Conclude", "AD/DB=AE/EC", "Substitute equal denominating areas."),
    )

    private fun tangentRadiusProof(source: String, profile: ExplanationProfile) = solved(
        source, profile, ProblemType.NumberTheory, "The tangent at P is perpendicular to radius OP",
        "Shortest-distance tangent proof", SolverRuleRegistry.GEOMETRIC_PROOF,
        step("Choose another tangent point", "Let Q≠P lie on the tangent line", "Q is outside the circle."),
        step("Compare distances", "OQ>OP", "OP is a radius while Q lies outside the circle."),
        step("Use shortest distance", "OP is the shortest segment from O to the tangent", "The shortest distance from a point to a line is perpendicular."),
        step("Conclude", "OP⊥tangent at P", "Therefore the radius through contact is perpendicular."))

    private fun equalTangentsProof(source: String, profile: ExplanationProfile) = solved(
        source, profile, ProblemType.NumberTheory, "Tangents PA and PB from external point P satisfy PA=PB",
        "RHS congruence proof", SolverRuleRegistry.GEOMETRIC_PROOF,
        step("Join radii", "Join OA, OB, and OP", "A and B are the contact points."),
        step("Use tangent-radius theorem", "OA⊥PA and OB⊥PB", "Each triangle is right-angled."),
        step("Compare triangles", "OA=OB and OP=OP", "Radii are equal and OP is common."),
        step("Apply RHS", "ΔOAP≅ΔOBP", "Right angle, hypotenuse, and one side are equal."),
        step("Conclude", "PA=PB", "Corresponding tangent lengths are equal."))

    private fun tangentConstruction(source: String, profile: ExplanationProfile) = solved(
        source, profile, ProblemType.CoordinateGeometry, "Two tangents from P to the circle, constructed through contact points T₁ and T₂",
        "Tangent construction from an external point", SolverRuleRegistry.GEOMETRIC_CONSTRUCTION,
        step("Draw the given objects", "Circle centre O radius 4 cm; OP=7 cm", "Place external point P at the stated distance."),
        step("Bisect OP", "Construct midpoint M of OP", "The auxiliary circle uses M as centre."),
        step("Draw the auxiliary circle", "Circle centre M radius MO", "Its intersections T₁,T₂ with the given circle create right angles."),
        step("Join tangents", "Join PT₁ and PT₂", "Angles OT₁P and OT₂P are 90 degrees, so both lines are tangents."),
        step("Verify", "OT₁⊥PT₁ and OT₂⊥PT₂", "A tangent is perpendicular to the radius at contact."))

    private fun quadraticFactors(source: String, profile: ExplanationProfile, factor: String, answer: String) = solved(
        source, profile, ProblemType.QuadraticEquation, answer, "Factorisation", SolverRuleRegistry.ZERO_PRODUCT,
        step("Find the pair", "-4 and -5", "Their sum is -9 and product is 20."),
        step("Factor", factor, "Write the quadratic as two linear factors."),
        step("Set each factor to zero", "x-4=0 or x-5=0", "Use the zero-product property."),
        step("Solve", answer, "Both roots verify in the original equation."))

    private fun radicalProof(source: String, profile: ExplanationProfile, prime: String) = solved(
        source, profile, ProblemType.NumberTheory, "√$prime is irrational",
        "Contradiction from lowest terms", SolverRuleRegistry.IRRATIONALITY_PROOF,
        step("Assume rationality", "√$prime=p/q with gcd(p,q)=1", "Assume a reduced fraction."),
        step("Square", "p²=${prime}q²", "The prime $prime divides p², hence p."),
        step("Write p=${prime}k", "q²=${prime}k²", "Substitution shows the same prime divides q."),
        step("Contradiction", "p and q share factor $prime", "This contradicts lowest terms, proving irrationality."))

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
