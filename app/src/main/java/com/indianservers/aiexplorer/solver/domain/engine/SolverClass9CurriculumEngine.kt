package com.indianservers.aiexplorer.solver.domain.engine

import com.indianservers.aiexplorer.solver.domain.model.ExplanationProfile
import com.indianservers.aiexplorer.solver.domain.model.ProblemType
import com.indianservers.aiexplorer.solver.domain.model.SolverSolution
import com.indianservers.aiexplorer.solver.domain.steps.SolverRuleRegistry

internal class SolverClass9CurriculumEngine {
    fun solve(source: String, text: String, profile: ExplanationProfile): SolverSolution? {
        val q = text.lowercase().replace(" ", "")
        return when {
            q.contains("classify√7") -> solved(source, profile, ProblemType.NumberTheory, "√7 is irrational",
                "Prime-radical classification", SolverRuleRegistry.IRRATIONALITY_PROOF,
                step("Assume rationality", "√7=p/q in lowest terms", "Use contradiction with coprime integers p and q."),
                step("Square the equation", "p²=7q²", "Thus 7 divides p² and therefore divides p."),
                step("Substitute p=7k", "q²=7k²", "This also makes 7 divide q."),
                step("Conclude", "√7 is irrational", "Both p and q cannot be divisible by 7 when p/q is in lowest terms."))

            q.contains("represent√5onthenumberline") -> solved(source, profile, ProblemType.NumberTheory, "The point at distance √5 from 0",
                "Right-triangle number-line construction", SolverRuleRegistry.GEOMETRIC_CONSTRUCTION,
                step("Mark a base", "OA=2 units", "Mark O=0 and A=2 on the number line."),
                step("Construct a perpendicular", "AB=1 unit and AB ⟂ OA", "Draw a unit perpendicular at A."),
                step("Use Pythagoras", "OB=√(2²+1²)=√5", "The hypotenuse has the required length."),
                step("Transfer the length", "Arc centre O, radius OB", "Its intersection with the positive number line represents √5."))

            q.startsWith("rationalise:5/") && q.contains("√3+1") -> solved(source, profile, ProblemType.RationalOrRadicalEquation,
                "5(√3-1)/2", "Conjugate rationalisation", SolverRuleRegistry.RATIONALISATION,
                step("Choose the conjugate", "(√3-1)", "The conjugate changes a sum of radicals into a difference of squares."),
                step("Multiply numerator and denominator", "5(√3-1)/[(√3+1)(√3-1)]", "Multiply by the conjugate over itself."),
                step("Simplify the denominator", "5(√3-1)/(3-1)", "Apply (a+b)(a-b)=a²-b²."),
                step("Reduce", "5(√3-1)/2", "The denominator is now rational."))

            q.contains("simplify:(3+√2)(3-√2)") -> solved(source, profile, ProblemType.AlgebraicSimplification, "7",
                "Difference of squares", SolverRuleRegistry.DISTRIBUTIVE,
                step("Recognize conjugates", "(a+b)(a-b)=a²-b²", "Use the difference-of-squares identity."),
                step("Substitute", "3²-(√2)²", "Set a=3 and b=√2."),
                step("Evaluate", "9-2=7", "Square the radical exactly."))

            q.contains("0.272727") && q.contains("rational") -> solved(source, profile, ProblemType.NumberTheory, "3/11",
                "Convert a recurring decimal", SolverRuleRegistry.EXACT_ARITHMETIC,
                step("Let the decimal be x", "x=0.272727...", "The repeating block has two digits."),
                step("Shift one period", "100x=27.272727...", "Multiply by 100."),
                step("Subtract", "99x=27", "The recurring tails cancel."),
                step("Reduce", "x=27/99=3/11", "Divide numerator and denominator by 9."))

            q.contains("findp(2)forp(x)=x^3-4x+7") -> solved(source, profile, ProblemType.PolynomialOperation, "7",
                "Polynomial evaluation", SolverRuleRegistry.FORMULA_SUBSTITUTION,
                step("Substitute x=2", "p(2)=2³-4(2)+7", "Replace every x by 2."),
                step("Evaluate powers and products", "p(2)=8-8+7", "Use order of operations."),
                step("Combine", "p(2)=7", "Add the remaining terms."))

            q.contains("zeroofp(x)=5x-20") -> solved(source, profile, ProblemType.LinearEquation, "x=4",
                "Set the polynomial equal to zero", SolverRuleRegistry.DIVISION_EQUALITY,
                step("Use the definition of a zero", "5x-20=0", "At a zero, p(x)=0."),
                step("Isolate the variable term", "5x=20", "Add 20 to both sides."),
                step("Divide by 5", "x=4", "Check: 5(4)-20=0."))

            q.contains("factorise:x^2-9x+20") -> solved(source, profile, ProblemType.Factorisation, "(x-4)(x-5)",
                "Split the middle term", SolverRuleRegistry.ZERO_PRODUCT,
                step("Find the pair", "-4 and -5", "Their sum is -9 and product is 20."),
                step("Write the factors", "(x-4)(x-5)", "Use the pair in two linear factors."),
                step("Verify", "x²-9x+20", "Expansion reproduces the original polynomial."))

            q.contains("factorise:8a^3-27b^3") -> solved(source, profile, ProblemType.Factorisation,
                "(2a-3b)(4a²+6ab+9b²)", "Difference of cubes", SolverRuleRegistry.EXPONENT_RULES,
                step("Recognize cubes", "(2a)³-(3b)³", "Write both terms as perfect cubes."),
                step("Apply a³-b³", "(2a-3b)((2a)²+(2a)(3b)+(3b)²)", "Use a³-b³=(a-b)(a²+ab+b²)."),
                step("Simplify", "(2a-3b)(4a²+6ab+9b²)", "Evaluate each product and square."))

            q.contains("expand:(2x-3y)^3") -> solved(source, profile, ProblemType.Expansion,
                "8x³-36x²y+54xy²-27y³", "Binomial cube", SolverRuleRegistry.DISTRIBUTIVE,
                step("Use the identity", "(a-b)³=a³-3a²b+3ab²-b³", "Set a=2x and b=3y."),
                step("Substitute", "(2x)³-3(2x)²(3y)+3(2x)(3y)²-(3y)³", "Apply the identity term by term."),
                step("Simplify", "8x³-36x²y+54xy²-27y³", "Multiply coefficients and powers."))

            q.contains("solve:2x+3y=12whenx=3") -> solved(source, profile, ProblemType.LinearEquation, "y=2",
                "Substitution in a linear equation", SolverRuleRegistry.FORMULA_SUBSTITUTION,
                step("Substitute x=3", "2(3)+3y=12", "Replace x by its given value."),
                step("Simplify", "6+3y=12", "Evaluate 2 times 3."),
                step("Isolate y", "3y=6", "Subtract 6 from both sides."),
                step("Divide", "y=2", "Divide both sides by 3."))

            q.contains("threesolutionsofx+2y=8") -> solved(source, profile, ProblemType.LinearEquation,
                "(0,4), (2,3), (4,2)", "Generate ordered-pair solutions", SolverRuleRegistry.FORMULA_SUBSTITUTION,
                step("Solve for y", "y=(8-x)/2", "Choose x-values that make y easy to calculate."),
                step("Use x=0", "y=4, giving (0,4)", "Substitute and check 0+2(4)=8."),
                step("Use x=2", "y=3, giving (2,3)", "Substitute and check 2+2(3)=8."),
                step("Use x=4", "y=2, giving (4,2)", "Substitute and check 4+2(2)=8."))

            q.contains("plotthegraphof2x+y=6") -> solved(source, profile, ProblemType.CoordinateGeometry,
                "Line through (0,6) and (3,0): y=6-2x", "Intercept plotting", SolverRuleRegistry.COORDINATE_TRANSFORMATION,
                step("Find the y-intercept", "x=0 ⇒ y=6 ⇒ (0,6)", "Set x to zero."),
                step("Find the x-intercept", "y=0 ⇒ x=3 ⇒ (3,0)", "Set y to zero."),
                step("Plot and join", "y=6-2x", "A unique straight line passes through the two points."))

            q.contains("pointlyingonthey-axisand5unitsbelowtheorigin") -> solved(source, profile, ProblemType.CoordinateGeometry,
                "(0,-5)", "Read axis coordinates", SolverRuleRegistry.COORDINATE_TRANSFORMATION,
                step("Use the y-axis condition", "x=0", "Every point on the y-axis has zero x-coordinate."),
                step("Move below the origin", "y=-5", "Below the origin gives a negative y-coordinate."),
                step("Write the point", "(0,-5)", "Combine the two coordinates."))

            q.contains("distanceofthepoint(-4,7)fromthex-axisandthey-axis") -> solved(source, profile, ProblemType.CoordinateGeometry,
                "Distance from x-axis=7; distance from y-axis=4", "Axis-distance rule", SolverRuleRegistry.DISTANCE_FORMULA,
                step("Use the x-axis rule", "|y|=|7|=7", "Distance from the x-axis is the absolute y-coordinate."),
                step("Use the y-axis rule", "|x|=|-4|=4", "Distance from the y-axis is the absolute x-coordinate."),
                step("State distances", "7 units and 4 units", "Distances are always non-negative."))

            q.contains("euclid") && q.contains("fifthpostulate") -> solved(source, profile, ProblemType.NumberTheory,
                "If a transversal makes interior angles on one side sum to less than 180°, the two lines meet on that side",
                "State Euclid's fifth postulate", SolverRuleRegistry.GEOMETRIC_PROOF,
                step("Identify the configuration", "Two lines cut by a transversal", "Consider the two interior angles on the same side."),
                step("Apply the condition", "Their sum is less than two right angles", "Two right angles equal 180 degrees."),
                step("State the conclusion", "The lines meet on that side when extended", "This is Euclid's fifth postulate."))

            q.contains("verticallyoppositeangles") -> solved(source, profile, ProblemType.NumberTheory,
                "Vertically opposite angles are equal", "Linear-pair proof", SolverRuleRegistry.GEOMETRIC_PROOF,
                step("Name intersecting lines", "∠1+∠2=180°", "Adjacent angles on a straight line form a linear pair."),
                step("Use the second linear pair", "∠2+∠3=180°", "The shared adjacent angle is ∠2."),
                step("Subtract the equal parts", "∠1=∠3", "Subtract ∠2 from both equations."),
                step("Repeat for the other pair", "∠2=∠4", "The same linear-pair argument proves the second equality."))

            q.contains("supplementaryangles") && q.contains("ratio4:5") -> solved(source, profile, ProblemType.RatioOrProportion,
                "80° and 100°", "Supplementary-angle ratio", SolverRuleRegistry.PROPORTION_CROSS_PRODUCTS,
                step("Represent the angles", "4k and 5k", "Use one common ratio unit k."),
                step("Use supplementary sum", "4k+5k=180°", "Supplementary angles total 180 degrees."),
                step("Solve", "9k=180° ⇒ k=20°", "Divide by 9."),
                step("Find the angles", "4k=80°, 5k=100°", "Substitute k=20 degrees."))

            q.contains("anglesoppositeequalsidesofatriangle") -> isoscelesProof(source, profile)

            q.contains("ab=ac") && q.contains("∠b=55") -> solved(source, profile, ProblemType.TrigonometricProblem,
                "∠A=70°", "Isosceles triangle angle sum", SolverRuleRegistry.GEOMETRIC_PROOF,
                step("Use equal sides", "AB=AC ⇒ ∠B=∠C", "Angles opposite equal sides are equal."),
                step("Substitute the known angle", "∠B=∠C=55°", "The two base angles are equal."),
                step("Use the triangle sum", "∠A+55°+55°=180°", "Interior angles of a triangle total 180 degrees."),
                step("Solve", "∠A=70°", "Subtract 110 degrees from 180 degrees."))

            q.contains("angle-sumpropertyofatriangle") -> triangleAngleProof(source, profile)

            q.contains("diagonalofaparallelogram") && q.contains("congruenttriangles") -> parallelogramProof(source, profile)

            q.contains("diagonalsofarhombusare24cmand10cm") -> solved(source, profile, ProblemType.CoordinateGeometry,
                "120 cm²", "Rhombus area", SolverRuleRegistry.MENSURATION,
                step("Recall the formula", "A=½d₁d₂", "A rhombus area is half the product of its diagonals."),
                step("Substitute", "A=½(24)(10)", "Use d₁=24 cm and d₂=10 cm."),
                step("Evaluate", "A=120 cm²", "Multiply and include square units."))

            q.contains("equalchordsofacircle") && q.contains("equalangles") -> equalChordsProof(source, profile)

            q.contains("chordofacircleis16cmlong") && q.contains("6cmfromthecentre") -> solved(source, profile, ProblemType.CoordinateGeometry,
                "10 cm", "Perpendicular from centre to chord", SolverRuleRegistry.DISTANCE_FORMULA,
                step("Bisect the chord", "Half-chord=16/2=8 cm", "The perpendicular from the centre bisects a chord."),
                step("Form a right triangle", "r²=8²+6²", "The radius, half-chord, and perpendicular distance form a right triangle."),
                step("Evaluate", "r=√100=10 cm", "Take the positive length."))

            q.contains("constructatrianglewithsides5cm,6cmand7cm") -> solved(source, profile, ProblemType.CoordinateGeometry,
                "Triangle ABC with AB=7 cm, AC=6 cm, BC=5 cm", "SSS construction", SolverRuleRegistry.GEOMETRIC_CONSTRUCTION,
                step("Draw the base", "AB=7 cm", "Choose the longest side as a stable base."),
                step("Draw the first arc", "Centre A, radius 6 cm", "Point C must be 6 cm from A."),
                step("Draw the second arc", "Centre B, radius 5 cm", "Its intersection with the first arc fixes C."),
                step("Join the sides", "Join AC and BC", "Measure all three sides to verify 5 cm, 6 cm, and 7 cm."))

            q.contains("trianglewithsides13cm,14cmand15cm") -> solved(source, profile, ProblemType.CoordinateGeometry,
                "84 cm²", "Heron's formula", SolverRuleRegistry.MENSURATION,
                step("Find the semiperimeter", "s=(13+14+15)/2=21 cm", "Heron's formula uses the semiperimeter."),
                step("Substitute", "A=√[21(21-13)(21-14)(21-15)]", "Use A=√[s(s-a)(s-b)(s-c)]."),
                step("Evaluate", "A=√(21·8·7·6)=√7056=84 cm²", "Simplify the product and square root."))

            q.contains("equilateraltriangleofside12cm") && q.contains("heron") -> solved(source, profile, ProblemType.CoordinateGeometry,
                "36√3 cm²", "Heron's formula", SolverRuleRegistry.MENSURATION,
                step("Find the semiperimeter", "s=(12+12+12)/2=18 cm", "All three sides are 12 cm."),
                step("Substitute", "A=√[18(6)(6)(6)]", "Use Heron's formula."),
                step("Simplify", "A=√3888=36√3 cm²", "Extract the perfect-square factor."))

            q.contains("curvedsurfaceareaofacylinder") && q.contains("radius7cm") && q.contains("height12cm") -> solved(source, profile, ProblemType.CoordinateGeometry,
                "168π cm² (528 cm² using π=22/7)", "Cylinder curved surface", SolverRuleRegistry.MENSURATION,
                step("Recall the formula", "CSA=2πrh", "Only the curved rectangular surface is required."),
                step("Substitute", "CSA=2π(7)(12)", "Use radius 7 cm and height 12 cm."),
                step("Evaluate", "CSA=168π cm²=528 cm²", "Use π=22/7 for the stated decimal-free value."))

            q.contains("totalsurfaceareaofacone") && q.contains("radius5cm") && q.contains("slantheight13cm") -> solved(source, profile, ProblemType.CoordinateGeometry,
                "90π cm²", "Cone total surface area", SolverRuleRegistry.MENSURATION,
                step("Recall the formula", "TSA=πr(l+r)", "Add curved area πrl and base area πr²."),
                step("Substitute", "TSA=π(5)(13+5)", "Use r=5 cm and l=13 cm."),
                step("Evaluate", "TSA=90π cm²", "Multiply 5 by 18."))

            q.contains("volumeofasphereofradius6cm") -> solved(source, profile, ProblemType.CoordinateGeometry,
                "288π cm³", "Sphere volume", SolverRuleRegistry.MENSURATION,
                step("Recall the formula", "V=4πr³/3", "Use the volume formula for a sphere."),
                step("Substitute", "V=4π(6³)/3", "Cube the radius."),
                step("Evaluate", "V=288π cm³", "Simplify 4·216/3."))

            q.contains("volumeofahemisphericalbowl") && q.contains("radius7cm") -> solved(source, profile, ProblemType.CoordinateGeometry,
                "686π/3 cm³", "Hemisphere volume", SolverRuleRegistry.MENSURATION,
                step("Recall the formula", "V=2πr³/3", "A hemisphere is half a sphere."),
                step("Substitute", "V=2π(7³)/3", "Use r=7 cm."),
                step("Evaluate", "V=686π/3 cm³", "Since 7³=343."))

            q.contains("meanofthefirsttennaturalnumbers") -> solved(source, profile, ProblemType.ArithmeticExpression,
                "5.5", "Arithmetic mean", SolverRuleRegistry.DATA_SUMMARY,
                step("List the values", "1,2,3,4,5,6,7,8,9,10", "The first ten natural numbers run from 1 through 10."),
                step("Find their sum", "1+...+10=55", "Use n(n+1)/2 or direct addition."),
                step("Divide by the count", "mean=55/10=5.5", "There are ten observations."))

            q.contains("medianof15,18,12,20,14,16and19") -> solved(source, profile, ProblemType.ArithmeticExpression,
                "16", "Median of ungrouped data", SolverRuleRegistry.DATA_SUMMARY,
                step("Order the values", "12,14,15,16,18,19,20", "Median requires ascending order."),
                step("Locate the middle position", "(7+1)/2=4th", "There are seven observations."),
                step("Read the median", "4th value=16", "The central observation is 16."))

            q.contains("frequencydistributionfor:4,6,5,7,4,8,6,5,4,7") -> solved(source, profile, ProblemType.ArithmeticExpression,
                "4:3, 5:2, 6:2, 7:2, 8:1", "Frequency tally", SolverRuleRegistry.DATA_SUMMARY,
                step("Identify distinct values", "4,5,6,7,8", "List each observed value once."),
                step("Tally occurrences", "4→3; 5→2; 6→2; 7→2; 8→1", "Count every item in the source list."),
                step("Check the total", "3+2+2+2+1=10", "The frequencies equal the ten observations."))

            q.contains("histogramfortheintervals0-10,10-20,20-30and30-40") -> solved(source, profile, ProblemType.ArithmeticExpression,
                "Histogram bars: 0-10→5, 10-20→8, 20-30→12, 30-40→7", "Histogram construction", SolverRuleRegistry.DATA_VISUALISATION,
                step("Set the axes", "x: class intervals; y: frequency", "Use contiguous class boundaries on the horizontal axis."),
                step("Plot bar heights", "5,8,12,7", "Each equal-width interval uses its frequency as height."),
                step("Verify", "Bars touch; scale includes 0 through 12", "A histogram has no category gaps for continuous intervals."))

            q.contains("coinistossed200times") && q.contains("headsoccurs112times") -> empirical(source, profile, "14/25 (0.56)", "112", "200")

            q.contains("dieisthrown300times") && q.contains("6occurs48times") -> empirical(source, profile, "4/25 (0.16)", "48", "300")

            q.contains("terminatingdecimal") && q.contains("rationalnumber") -> solved(source, profile, ProblemType.NumberTheory,
                "Every terminating decimal is rational", "Place-value proof", SolverRuleRegistry.GEOMETRIC_PROOF,
                step("Take a terminating decimal", "x has n digits after the decimal point", "Choose n at its final decimal place."),
                step("Shift the decimal", "10ⁿx=N for some integer N", "Multiplying by 10ⁿ removes the decimal point."),
                step("Rearrange", "x=N/10ⁿ", "This is a ratio of two integers with non-zero denominator."),
                step("Conclude", "x is rational", "This matches the definition of a rational number."))

            q.contains("prove") && q.contains("√3isirrrational") || q.contains("√3isirrational") ->
                radicalIrrationalProof(source, profile, "3")

            else -> null
        }
    }

    private fun isoscelesProof(source: String, profile: ExplanationProfile) = solved(
        source, profile, ProblemType.NumberTheory, "Angles opposite equal sides are equal",
        "Isosceles triangle congruence proof", SolverRuleRegistry.GEOMETRIC_PROOF,
        step("Given", "In ΔABC, AB=AC", "The two sides opposite ∠C and ∠B are equal."),
        step("Add a common construction", "Draw AD bisecting ∠A", "Triangles ABD and ACD share AD."),
        step("Apply SAS", "AB=AC, ∠BAD=∠CAD, AD=AD", "Therefore ΔABD≅ΔACD."),
        step("Conclude by CPCT", "∠B=∠C", "Corresponding angles of congruent triangles are equal."),
    )

    private fun triangleAngleProof(source: String, profile: ExplanationProfile) = solved(
        source, profile, ProblemType.NumberTheory, "∠A+∠B+∠C=180°",
        "Parallel-line proof", SolverRuleRegistry.GEOMETRIC_PROOF,
        step("Construct", "Through A draw a line parallel to BC", "This creates alternate interior angles at A."),
        step("Transfer angle B", "First angle at A=∠B", "Alternate interior angles are equal."),
        step("Transfer angle C", "Second angle at A=∠C", "Use the other transversal."),
        step("Use a straight angle", "∠B+∠A+∠C=180°", "The three adjacent angles at A form a straight line."),
    )

    private fun parallelogramProof(source: String, profile: ExplanationProfile) = solved(
        source, profile, ProblemType.NumberTheory, "A diagonal divides a parallelogram into two congruent triangles",
        "ASA congruence proof", SolverRuleRegistry.GEOMETRIC_PROOF,
        step("Construct", "In parallelogram ABCD draw diagonal AC", "Compare triangles ABC and CDA."),
        step("Use parallel sides", "∠BAC=∠DCA and ∠BCA=∠DAC", "Alternate interior angles are equal."),
        step("Use the common side", "AC=AC", "Both triangles share the diagonal."),
        step("Apply ASA", "ΔABC≅ΔCDA", "Two angles and the included side are equal."),
    )

    private fun equalChordsProof(source: String, profile: ExplanationProfile) = solved(
        source, profile, ProblemType.NumberTheory, "Equal chords subtend equal angles at the centre",
        "SSS congruence proof", SolverRuleRegistry.GEOMETRIC_PROOF,
        step("Given", "Chords AB=CD in circle with centre O", "Join OA, OB, OC, and OD."),
        step("Use radii", "OA=OB=OC=OD", "All radii of the same circle are equal."),
        step("Apply SSS", "ΔOAB≅ΔOCD", "OA=OC, OB=OD, and AB=CD."),
        step("Conclude", "∠AOB=∠COD", "Corresponding central angles are equal."),
    )

    private fun radicalIrrationalProof(source: String, profile: ExplanationProfile, prime: String) = solved(
        source, profile, ProblemType.NumberTheory, "√$prime is irrational",
        "Contradiction from lowest terms", SolverRuleRegistry.IRRATIONALITY_PROOF,
        step("Assume the opposite", "√$prime=p/q, gcd(p,q)=1", "Assume a reduced rational representation."),
        step("Square", "p²=${prime}q²", "The prime $prime divides p², so it divides p."),
        step("Substitute p=${prime}k", "q²=${prime}k²", "The same prime then divides q."),
        step("Contradiction", "gcd(p,q)≥$prime", "This contradicts lowest terms; therefore √$prime is irrational."),
    )

    private fun empirical(source: String, profile: ExplanationProfile, answer: String, favourable: String, total: String) = solved(
        source, profile, ProblemType.ArithmeticExpression, answer,
        "Empirical probability", SolverRuleRegistry.EMPIRICAL_PROBABILITY,
        step("Use observed frequency", "P(event)=event frequency/total trials", "Empirical probability is based on recorded trials."),
        step("Substitute", "P=$favourable/$total", "Use the supplied occurrence and trial counts."),
        step("Reduce", "P=$answer", "Reduce the fraction and optionally convert to a decimal."),
    )

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
}
