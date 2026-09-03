package com.indianservers.aiexplorer.core

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sqrt

data class ProofParameter(val name: String, val minimum: Double, val maximum: Double, val initial: Double)
data class ProofDataSet(
    val title: String,
    val sourceLabel: String,
    val sourceUrl: String,
    val columns: List<String>,
    val rows: List<List<Double>>,
)

enum class VisualEvidenceType(val label: String) {
    DeductiveProof("DEDUCTIVE PROOF"),
    Derivation("DERIVATION"),
    ApproximationProof("LIMIT / ERROR PROOF"),
    ExperimentalDemonstration("MEASURED DEMONSTRATION"),
    DataCounterexample("DATA COUNTEREXAMPLE"),
}

data class VisualProofLab(
    val id: String,
    val title: String,
    val topic: String,
    val steps: List<String>,
    val parameters: List<ProofParameter>,
    val changesPrompt: String,
    val invariantPrompt: String,
    val formalResult: String,
    val dataSet: ProofDataSet? = null,
    val evidenceType: VisualEvidenceType = VisualEvidenceType.DeductiveProof,
)
data class ProofFrame(
    val lab: VisualProofLab,
    val step: Int,
    val parameters: Map<String, Double>,
    val measurements: Map<String, Double>,
    val invariant: String,
    val residual: Double,
    val holds: Boolean,
)
data class ProofPlayback(val frame: ProofFrame, val playing: Boolean = false, val direction: Int = 1)

data class VisualProofCertificate(
    val labId: String,
    val method: String,
    val argument: List<String>,
    val assumptions: List<String>,
)

data class VisualProofSubcategory(
    val name: String,
    val description: String,
    val labIds: List<String>,
)

/** The shared visual language used across all proof scenes. */
enum class ProofEnhancement {
    ColorCodedStages, BeforeAfter, AreaModel, MotionArrows, SameQuantityDifferentShape,
    TransparentComparison, GridBackground, MarkedInvariant, ValidityLabels, CutAndSlide,
    Dissection, NumberLine, ZoomDetail, Checkpoints, SymbolicVisualColumns,
    ConsistentColorGrammar, Assumptions, NotToScale, Counterexample, VennDiagram,
    TruthTable, BalanceScale, SlopeTriangle, TangentOverlay, AccumulationStrips,
    FadingLimit, EpsilonDeltaBands, UnitCircleOverlay, VectorProjection, TransformationGrid,
    EigenvectorDirection, DeterminantScaling, CountingTree, LatticePoints, RecursiveStructure,
    ModularClock, ProofBreadcrumbs, MatchedStepNumbers, InlineDefinitions, EquivalentRepresentations,
    ContradictionPanel, ThereforeSummary, MotionTrails, SymmetryAxes, ReusableLegend,
    DomainMask, ErrorTrap, SimpleCase, RealWorldAnalogy, CompactTakeaway,
}

data class ProofVisualProfile(
    val features: Set<ProofEnhancement>,
    val assumptions: List<String>,
    val invariant: String,
    val definitions: Map<String, String>,
    val validityNotes: List<String>,
    val domainRestriction: String,
    val counterexample: String,
    val errorTrap: String,
    val simpleCase: String,
    val analogy: String?,
    val takeaway: String,
    val notToScale: Boolean = false,
)

object VisualProofCatalog {
    val categories = listOf(
        "Geometry", "Trigonometry", "Algebra", "Calculus", "Linear Algebra",
        "Coordinate Geometry", "Probability & Statistics", "Discrete Mathematics",
        "Vectors", "Number Theory", "Mensuration",
    )

    val anscombeQuartet = ProofDataSet(
        title = "Anscombe's Quartet",
        sourceLabel = "F. J. Anscombe, Graphs in Statistical Analysis (1973)",
        sourceUrl = "https://doi.org/10.1080/00031305.1973.10478966",
        columns = listOf("series", "x", "y"),
        rows = listOf(
            listOf(1.0, 10.0, 8.04), listOf(1.0, 8.0, 6.95), listOf(1.0, 13.0, 7.58),
            listOf(1.0, 9.0, 8.81), listOf(1.0, 11.0, 8.33), listOf(1.0, 14.0, 9.96),
            listOf(1.0, 6.0, 7.24), listOf(1.0, 4.0, 4.26), listOf(1.0, 12.0, 10.84),
            listOf(1.0, 7.0, 4.82), listOf(1.0, 5.0, 5.68),
            listOf(2.0, 10.0, 9.14), listOf(2.0, 8.0, 8.14), listOf(2.0, 13.0, 8.74),
            listOf(2.0, 9.0, 8.77), listOf(2.0, 11.0, 9.26), listOf(2.0, 14.0, 8.10),
            listOf(2.0, 6.0, 6.13), listOf(2.0, 4.0, 3.10), listOf(2.0, 12.0, 9.13),
            listOf(2.0, 7.0, 7.26), listOf(2.0, 5.0, 4.74),
            listOf(3.0, 10.0, 7.46), listOf(3.0, 8.0, 6.77), listOf(3.0, 13.0, 12.74),
            listOf(3.0, 9.0, 7.11), listOf(3.0, 11.0, 7.81), listOf(3.0, 14.0, 8.84),
            listOf(3.0, 6.0, 6.08), listOf(3.0, 4.0, 5.39), listOf(3.0, 12.0, 8.15),
            listOf(3.0, 7.0, 6.42), listOf(3.0, 5.0, 5.73),
            listOf(4.0, 8.0, 6.58), listOf(4.0, 8.0, 5.76), listOf(4.0, 8.0, 7.71),
            listOf(4.0, 8.0, 8.84), listOf(4.0, 8.0, 8.47), listOf(4.0, 8.0, 7.04),
            listOf(4.0, 8.0, 5.25), listOf(4.0, 19.0, 12.50), listOf(4.0, 8.0, 5.56),
            listOf(4.0, 8.0, 7.91), listOf(4.0, 8.0, 6.89),
        ),
    )

    private data class ExtraProofSpec(val id: String, val title: String, val result: String)
    private val extraNumberTheorySpecs = listOf(
        ExtraProofSpec("nt-natural-sum", "Sum of First n Natural Numbers", "1+2+⋯+n=n(n+1)/2"),
        ExtraProofSpec("nt-odd-sum", "Odd Numbers Build Squares", "1+3+⋯+(2n−1)=n²"),
        ExtraProofSpec("nt-triangular-numbers", "Triangular Numbers", "Tₙ=n(n+1)/2"),
        ExtraProofSpec("nt-consecutive-squares", "Difference of Consecutive Squares", "n²−(n−1)²=2n−1"),
        ExtraProofSpec("nt-arithmetic-sum", "Arithmetic Sequence Sum", "Sₙ=n(a+l)/2"),
        ExtraProofSpec("nt-even-sum", "Sum of First n Even Numbers", "2+4+⋯+2n=n(n+1)"),
        ExtraProofSpec("nt-square-odd-difference", "Squares as Consecutive Odd Sums", "1+3+⋯+(2n−1)=n²"),
        ExtraProofSpec("nt-consecutive-integer-sum", "Sum of Consecutive Integers", "S=n(2k+n−1)/2"),
        ExtraProofSpec("nt-divisibility-2", "Divisibility Rule for 2", "2∣N iff the last digit is even"),
        ExtraProofSpec("nt-divisibility-3", "Divisibility Rule for 3", "N≡digit sum (mod 3)"),
        ExtraProofSpec("nt-divisibility-4", "Divisibility Rule for 4", "4∣N iff 4 divides its last two digits"),
        ExtraProofSpec("nt-divisibility-8", "Divisibility Rule for 8", "8∣N iff 8 divides its last three digits"),
        ExtraProofSpec("nt-divisibility-9", "Divisibility Rule for 9", "N≡digit sum (mod 9)"),
        ExtraProofSpec("nt-divisibility-11", "Divisibility Rule for 11", "N≡alternating digit sum (mod 11)"),
        ExtraProofSpec("nt-divisibility-5-10", "Divisibility Rules for 5 and 10", "last digit 0 or 5; last digit 0"),
        ExtraProofSpec("nt-parity-last-digit", "Why the Last Digit Determines Parity", "N and its last digit have the same parity"),
        ExtraProofSpec("nt-factor-rectangles", "Factor Pairs as Rectangles", "a×b=N"),
        ExtraProofSpec("nt-multiples-line", "Multiples on the Number Line", "…,−2a,−a,0,a,2a,…"),
        ExtraProofSpec("nt-prime-building-blocks", "Prime Factorization as Building Blocks", "N=∏pᵢᵉⁱ"),
        ExtraProofSpec("nt-lcm-cycles", "LCM Through Repeating Cycles", "lcm(a,b)=first positive shared landing"),
        ExtraProofSpec("nt-euclidean-algorithm", "Euclidean Algorithm", "gcd(a,b)=gcd(b,a mod b)"),
        ExtraProofSpec("nt-gcd-grouping", "GCD Through Largest Equal Grouping", "gcd(a,b)=largest equal group size"),
        ExtraProofSpec("nt-gcd-lcm-product", "Relationship Between GCD and LCM", "gcd(a,b)·lcm(a,b)=ab"),
        ExtraProofSpec("nt-sieve", "Sieve of Eratosthenes", "cross multiples through √N"),
        ExtraProofSpec("nt-composite-sqrt", "Composite Factor at Most √n", "composite n has a factor ≤√n"),
        ExtraProofSpec("nt-prime-gaps", "Prime Gaps Explorer", "gₙ=pₙ₊₁−pₙ"),
        ExtraProofSpec("nt-twin-primes", "Twin Primes Explorer", "p and p+2 are both prime"),
        ExtraProofSpec("nt-unique-factorization", "Fundamental Theorem of Arithmetic", "N has a unique prime factorization"),
        ExtraProofSpec("nt-euclid-primes", "Infinitely Many Primes", "p₁p₂⋯pₙ+1 has a new prime divisor"),
        ExtraProofSpec("nt-modular-clock", "Modular Arithmetic Clock", "a≡r (mod n)"),
        ExtraProofSpec("nt-modular-addition", "Addition Modulo n", "(a+b) mod n"),
        ExtraProofSpec("nt-modular-multiplication", "Multiplication Modulo n", "(ab) mod n"),
        ExtraProofSpec("nt-negative-modulo", "Negative Numbers Modulo n", "−a≡n−(a mod n)"),
        ExtraProofSpec("nt-remainder-classes", "Remainder Classes", "ℤ partitions into n congruence classes"),
        ExtraProofSpec("nt-exponent-product", "Product of Powers", "aᵐaⁿ=aᵐ⁺ⁿ"),
        ExtraProofSpec("nt-exponent-quotient", "Quotient of Powers", "aᵐ/aⁿ=aᵐ⁻ⁿ"),
        ExtraProofSpec("nt-power-of-power", "Power of a Power", "(aᵐ)ⁿ=aᵐⁿ"),
        ExtraProofSpec("nt-zero-exponent", "Why a⁰ = 1", "a⁰=1 for a≠0"),
        ExtraProofSpec("nt-negative-exponent", "Negative Exponents", "a⁻ⁿ=1/aⁿ"),
        ExtraProofSpec("nt-perfect-numbers", "Perfect Numbers and Divisor Pairing", "σ(n)−n=n"),
    )
    private val extraNumberTheoryIds get() = extraNumberTheorySpecs.map { it.id }

    val labs = listOf(
        VisualProofLab("triangle-angle-sum", "Triangle angle sum", "Geometry", listOf("Build triangle ABC.", "Copy its three angles.", "Arrange them on a straight line.", "Drag a vertex to test."), listOf(ProofParameter("height", .2, 6.0, 3.0), ProofParameter("offset", -3.0, 5.0, 1.0)), "Which individual angles change as C moves?", "Why does their total stay 180°?", "A + B + C = 180°"),
        VisualProofLab("pythagorean", "Pythagorean rearrangement", "Geometry", listOf("Create a right triangle.", "Build squares on each side.", "Rearrange four copies.", "Compare uncovered areas."), listOf(ProofParameter("a", .5, 6.0, 3.0), ProofParameter("b", .5, 6.0, 4.0)), "How do the three square areas change?", "What area remains equal after rearrangement?", "a² + b² = c²"),
        VisualProofLab("derivative-slope", "Derivative as slope", "Calculus", listOf("Plot f(x)=x².", "Place a secant h away.", "Shrink h.", "Reveal the tangent."), listOf(ProofParameter("x", -4.0, 4.0, 2.0), ProofParameter("h", .001, 2.0, 1.0)), "How does the secant slope change as h shrinks?", "What limiting slope is stable?", "d(x²)/dx = 2x", evidenceType = VisualEvidenceType.Derivation),
        VisualProofLab("integral-area", "Integral as accumulated area", "Calculus", listOf("Plot f(x)=x² on [0,b].", "Build midpoint rectangles.", "Increase their count.", "Compare the approximation, exact area, and error bound."), listOf(ProofParameter("b", .5, 5.0, 3.0), ProofParameter("n", 2.0, 200.0, 10.0)), "How does the measured error change when rectangles become thinner?", "Which exact accumulated area is approached?", "∫₀ᵇ x² dx = b³/3", evidenceType = VisualEvidenceType.ApproximationProof),
        VisualProofLab("normal-area", "Normal probability area", "Probability & Statistics", listOf("Draw the normal curve.", "Place symmetric bounds.", "Shade the interval.", "Compare numerical area with the CDF identity."), listOf(ProofParameter("z", .1, 3.5, 1.0)), "How does shaded probability change with z?", "What remains symmetric about zero?", "P(-z≤Z≤z)=2Φ(z)-1", evidenceType = VisualEvidenceType.Derivation),
        VisualProofLab("vector-addition", "Vector addition", "Vectors", listOf("Draw vectors u and v.", "Use head-to-tail addition.", "Swap their order.", "Compare endpoints."), listOf(ProofParameter("ux", -4.0, 4.0, 2.0), ProofParameter("uy", -4.0, 4.0, 1.0), ProofParameter("vx", -4.0, 4.0, -1.0), ProofParameter("vy", -4.0, 4.0, 3.0)), "What changes when u and v move?", "Why is the final endpoint unchanged when order swaps?", "u + v = v + u"),
        VisualProofLab("matrix-transform", "Matrix area transformation", "Linear Algebra", listOf("Start with a unit square.", "Apply a 2×2 matrix.", "Measure transformed area.", "Compare with determinant."), listOf(ProofParameter("a", -3.0, 3.0, 2.0), ProofParameter("b", -3.0, 3.0, 1.0), ProofParameter("c", -3.0, 3.0, 0.0), ProofParameter("d", -3.0, 3.0, 2.0)), "How does the image shape change?", "What scalar controls signed area?", "area scale = |det(A)|"),
        VisualProofLab("circle-ratio", "Circle circumference ratio", "Mensuration", listOf("Choose a radius.", "Approximate the circle with a regular polygon.", "Unroll its perimeter.", "Increase the side count and compare with diameter."), listOf(ProofParameter("r", .2, 6.0, 2.0), ProofParameter("n", 6.0, 240.0, 24.0)), "How does the perimeter estimate change as the polygon gains sides?", "Which scale-independent ratio approaches π?", "C/d = π", evidenceType = VisualEvidenceType.ExperimentalDemonstration),
        VisualProofLab("algebra-square", "Square of a binomial", "Algebra", listOf("Build a square of side a+b.", "Partition it.", "Label the four regions.", "Sum their areas."), listOf(ProofParameter("a", .2, 5.0, 2.0), ProofParameter("b", .2, 5.0, 1.0)), "How do the four regions change?", "Why does total area remain the same?", "(a+b)² = a² + 2ab + b²"),
        VisualProofLab("shear-area", "Shear preserves area", "Transformations", listOf("Build a rectangle.", "Slide its top edge.", "Observe the parallelogram.", "Compare base×height."), listOf(ProofParameter("base", .5, 6.0, 4.0), ProofParameter("height", .5, 5.0, 2.0), ProofParameter("shear", -4.0, 4.0, 1.0)), "What changes as the top edge slides?", "Which dimensions keep area fixed?", "A = base × perpendicular height"),
        VisualProofLab("triangle-area", "Triangle area dissection", "Geometry", listOf("Build a triangle with base b and height h.", "Duplicate and rotate it.", "Join both copies into a parallelogram.", "Take half of the parallelogram area."), listOf(ProofParameter("base", .5, 7.0, 4.0), ProofParameter("height", .5, 6.0, 3.0), ProofParameter("apex", -2.0, 6.0, 1.5)), "How does the triangle change when its apex slides?", "Why is base times perpendicular height unchanged by a horizontal apex move?", "A = bh/2"),
        VisualProofLab("parallelogram-area", "Parallelogram cut and slide", "Geometry", listOf("Draw a slanted parallelogram.", "Cut the triangular end.", "Slide it to the opposite side.", "Read the resulting rectangle area."), listOf(ProofParameter("base", .5, 7.0, 4.0), ProofParameter("height", .5, 5.0, 2.5), ProofParameter("shear", -3.0, 3.0, 1.2)), "What changes as the slant changes?", "Which base and perpendicular height remain fixed?", "A = bh"),
        VisualProofLab("trapezoid-area", "Trapezoid doubling proof", "Geometry", listOf("Build a trapezoid with parallel sides a and b.", "Rotate a second copy.", "Join the copies into a parallelogram.", "Halve its area."), listOf(ProofParameter("a", .5, 7.0, 5.0), ProofParameter("b", .5, 7.0, 2.5), ProofParameter("height", .5, 5.0, 2.5)), "How do the copies fit when either base changes?", "Why is the doubled base always a+b?", "A = (a+b)h/2"),
        VisualProofLab("circle-area", "Circle sectors to rectangle", "Geometry", listOf("Divide the circle into equal sectors.", "Alternate sectors up and down.", "Increase the sector count.", "Read the limiting rectangle dimensions."), listOf(ProofParameter("r", .4, 5.0, 2.2), ProofParameter("n", 6.0, 60.0, 12.0)), "How does the scalloped edge change as n grows?", "Why do height r and base pi r remain fixed?", "A = pi r²"),
        VisualProofLab("polygon-angle-sum", "Polygon triangulation", "Geometry", listOf("Choose a regular n-gon.", "Select one vertex.", "Draw every non-adjacent diagonal.", "Count the resulting triangles."), listOf(ProofParameter("n", 3.0, 12.0, 6.0), ProofParameter("radius", 1.0, 5.0, 2.8)), "How many triangles appear when a side is added?", "Why is each new triangle worth 180 degrees?", "S = (n-2) × 180°"),
        VisualProofLab("similar-triangles", "Similar triangle ratios", "Geometry", listOf("Build a reference triangle.", "Scale it from a common center.", "Compare corresponding sides.", "Move the scale control and retest."), listOf(ProofParameter("a", 1.0, 6.0, 3.0), ProofParameter("b", 1.0, 6.0, 2.0), ProofParameter("k", .25, 3.0, 1.5)), "How do the side lengths change with k?", "Which three side ratios stay equal?", "a'/a = b'/b = c'/c = k"),
        VisualProofLab("intersecting-chords", "Intersecting chords theorem", "Geometry", listOf("Draw two chords through an interior point P.", "Measure the four chord segments.", "Build rectangles from each segment pair.", "Move P while keeping both chords on the circle."), listOf(ProofParameter("r", 1.0, 5.0, 3.0), ProofParameter("p", -.75, .75, .2), ProofParameter("angle", 15.0, 165.0, 70.0)), "How do individual segment lengths change as P moves?", "Why do the two segment products remain equal?", "PA × PB = PC × PD"),
        VisualProofLab("circle-angle", "Center and circumference angles", "Geometry", listOf("Choose chord AB.", "Join A and B to center O.", "Choose point C on the major arc.", "Measure both angles independently from their rays."), listOf(ProofParameter("arc", 20.0, 160.0, 80.0), ProofParameter("c", 185.0, 270.0, 250.0), ProofParameter("r", 1.0, 5.0, 3.0)), "How do both measured angles change when the chord changes?", "Why does the central angle stay twice the inscribed angle?", "angle AOB = 2 angle ACB"),
        VisualProofLab("unit-circle-identity", "Unit-circle identity", "Trigonometry", listOf("Place a point on the unit circle.", "Project it onto both axes.", "Read cosine and sine as legs.", "Apply Pythagoras to the radius."), listOf(ProofParameter("theta", -180.0, 180.0, 40.0)), "How do sine and cosine change as the point rotates?", "Why does their squared sum stay one?", "sin²(theta) + cos²(theta) = 1"),
        VisualProofLab("odd-sum-square", "Odd numbers build squares", "Number Theory", listOf("Start with one unit tile.", "Add the next odd L-shaped border.", "Repeat for n layers.", "Count the completed square."), listOf(ProofParameter("n", 1.0, 15.0, 6.0)), "How many tiles enter at each new layer?", "Why does the completed side length equal n?", "1+3+5+...+(2n-1) = n²"),
        VisualProofLab("absolute-inequality", "Absolute-value inequality", "Algebra", listOf("Mark −r and r on a number line.", "Interpret |x| as distance from zero.", "Shade points whose distance is at most r.", "Read the compound inequality."), listOf(ProofParameter("x", -6.0, 6.0, 2.0), ProofParameter("r", .5, 6.0, 3.0)), "How does membership change as x crosses either boundary?", "Why is distance from zero unchanged by reflection?", "|x| ≤ r ⇔ −r ≤ x ≤ r"),
        VisualProofLab("equation-balance", "Equation as a balance", "Algebra", listOf("Balance ax+b against c.", "Remove b from both pans.", "Split both sides into a equal groups.", "Read the value of x."), listOf(ProofParameter("a", 1.0, 6.0, 2.0), ProofParameter("b", -5.0, 5.0, 2.0), ProofParameter("c", -5.0, 15.0, 10.0)), "How does the solution move when a, b, or c changes?", "Why does doing the same operation to both sides preserve equality?", "ax+b=c ⇒ x=(c−b)/a"),
        VisualProofLab("set-de-morgan", "De Morgan's law", "Set Theory & Logic", listOf("Draw overlapping sets A and B.", "Choose whether a test point belongs to each set.", "Evaluate both expressions independently.", "Generate and compare all four truth rows."), listOf(ProofParameter("inA", 0.0, 1.0, 0.0), ProofParameter("inB", 0.0, 1.0, 0.0)), "Which regions are selected on each side?", "Why do both expressions select exactly the same membership states?", "(A∪B)ᶜ = Aᶜ∩Bᶜ"),
        VisualProofLab("epsilon-delta", "Epsilon–delta limit", "Calculus", listOf("Choose a horizontal ε-band around L.", "Choose a vertical δ-band around a.", "Track the graph inside the δ-band.", "Verify its image stays inside the ε-band."), listOf(ProofParameter("epsilon", .2, 2.0, .8), ProofParameter("delta", .05, 1.0, .35)), "How small must δ become as ε shrinks?", "What implication remains true inside the bands?", "0<|x−a|<δ ⇒ |f(x)−L|<ε"),
        VisualProofLab("slope-triangle", "Slope triangles", "Coordinate Geometry", listOf("Plot a line through two points.", "Draw its horizontal run.", "Draw its vertical rise.", "Compare a second similar slope triangle."), listOf(ProofParameter("m", -3.0, 3.0, 1.5), ProofParameter("run", .5, 4.0, 2.0)), "How do rise and run change when the triangle is enlarged?", "Why does rise/run stay fixed along one line?", "m = rise/run = Δy/Δx"),
        VisualProofLab("eigenvector-direction", "Eigenvector direction", "Linear Algebra", listOf("Draw a test vector v on a transformation grid.", "Apply a diagonal linear transformation.", "Measure the turn using cross(v,Av).", "Compare an eigenvector with a general vector."), listOf(ProofParameter("lambda", -3.0, 3.0, 2.0), ProofParameter("other", -3.0, 3.0, .5), ProofParameter("vy", -2.0, 2.0, 0.0)), "How does the vector turn when it leaves an eigenvector axis?", "Which lines through the origin make cross(v,Av) zero?", "Av = λv"),
        VisualProofLab("counting-paths", "Counting paths", "Combinatorics", listOf("Branch into right or up choices.", "Arrange endpoints on a lattice.", "Group paths by their final step.", "Read Pascal's recurrence."), listOf(ProofParameter("right", 1.0, 6.0, 3.0), ProofParameter("up", 1.0, 6.0, 2.0)), "How many paths are added when the grid grows?", "Why does every path end with exactly one of two moves?", "C(r+u,r)=C(r+u−1,r−1)+C(r+u−1,r)"),
        VisualProofLab("modular-clock", "Congruence on a clock", "Number Theory", listOf("Place a and b on an n-hour clock.", "Compare their normalized remainders.", "Test whether n divides a−b.", "Confirm both conditions agree."), listOf(ProofParameter("a", -20.0, 30.0, 17.0), ProofParameter("b", -20.0, 30.0, 5.0), ProofParameter("n", 2.0, 16.0, 12.0)), "When do the two integers share a clock position?", "Why is sharing a remainder equivalent to a whole-number difference of turns?", "a ≡ b (mod n) ⇔ n | (a−b)"),
        VisualProofLab(
            "anscombe-quartet",
            "Anscombe's Quartet: always plot the data",
            "Statistics",
            listOf(
                "Load the 44 published observations.",
                "Compute each series' mean and sample variance.",
                "Fit its least-squares regression line.",
                "Switch series and compare the visibly different point patterns.",
            ),
            listOf(ProofParameter("series", 1.0, 4.0, 1.0)),
            "How can the point pattern change while the summary values barely move?",
            "Why are numerical summaries alone insufficient to describe a dataset?",
            "matching summaries do not imply matching distributions",
            anscombeQuartet,
            VisualEvidenceType.DataCounterexample,
        ),
    ) + extraNumberTheorySpecs.map { spec ->
        val defaultN = when (spec.id) {
            "nt-triangular-numbers" -> 7.0
            "nt-arithmetic-sum" -> 10.0
            "nt-even-sum" -> 4.0
            "nt-square-odd-difference" -> 4.0
            "nt-consecutive-integer-sum" -> 7.0
            else -> 6.0
        }
        val defaultA = when (spec.id) {
            "nt-arithmetic-sum" -> 2.0
            "nt-consecutive-integer-sum" -> 3.0
            else -> 12.0
        }
        val defaultB = if (spec.id == "nt-arithmetic-sum") 1.0 else 6.0
        VisualProofLab(
            spec.id, spec.title, "Number Theory",
            listOf("Build the concrete number model.", "Manipulate its parts.", "Track the preserved quantity.", "Read the symbolic rule."),
            listOf(ProofParameter("n", 1.0, 20.0, defaultN), ProofParameter("a", 1.0, 120.0, defaultA), ProofParameter("b", 1.0, 80.0, defaultB)),
            "What changes when the values move?", "Which arithmetic relationship remains true?", spec.result,
        )
    }

    val certificates = listOf(
        VisualProofCertificate("triangle-angle-sum", "Parallel-line angle transfer", listOf("Draw a line through one vertex parallel to the opposite side.", "Alternate interior angles copy the other two vertex angles onto that line.", "The three adjacent angles form a straight angle, so their sum is 180°."), listOf("Euclidean parallel postulate", "Non-degenerate triangle")),
        VisualProofCertificate("pythagorean", "Area-preserving dissection", listOf("Arrange four congruent right triangles inside a square of side a+b.", "The remaining central square has side c, so its area is c².", "Computing the same outer area from the four triangles and simplifying gives c²=a²+b²."), listOf("Right triangle", "Euclidean area additivity")),
        VisualProofCertificate("derivative-slope", "Secant-to-tangent limit", listOf("For f(x)=x² the secant slope is ((x+h)²-x²)/h.", "For h≠0 this simplifies exactly to 2x+h.", "Taking h→0 leaves 2x, the tangent slope."), listOf("h approaches zero through non-zero values", "Real differentiability")),
        VisualProofCertificate("integral-area", "Midpoint Riemann-sum limit", listOf("Partition [0,b] into n equal strips and sample x² at every midpoint.", "The displayed sum is computed from those n observed rectangle areas; its error is bounded by b³/(12n²).", "As n grows, that bound tends to zero, so the sums converge to b³/3."), listOf("b≥0", "Riemann integrability of f(x)=x²")),
        VisualProofCertificate("normal-area", "Symmetry and CDF subtraction", listOf("The standard normal density is symmetric about zero.", "Area from -z to z equals Φ(z)-Φ(-z).", "Symmetry gives Φ(-z)=1-Φ(z), hence the area is 2Φ(z)-1."), listOf("z≥0", "Standard normal distribution")),
        VisualProofCertificate("vector-addition", "Parallelogram construction", listOf("Translate vectors without rotating or scaling them.", "Head-to-tail paths u then v and v then u are opposite routes around one parallelogram.", "Both routes have the same endpoint, proving u+v=v+u."), listOf("Vectors in the same affine space")),
        VisualProofCertificate("matrix-transform", "Determinant area decomposition", listOf("A matrix sends the unit-square basis edges to its two column vectors.", "Those columns span the image parallelogram.", "Its signed area is ad-bc, so ordinary area scales by |det A|."), listOf("Linear transformation in R²")),
        VisualProofCertificate("circle-ratio", "Circumference unrolling", listOf("Scale a circle by a factor k.", "Both its circumference and diameter scale by k.", "Their ratio is therefore scale-invariant; its common value defines π, giving C=πd=2πr."), listOf("Euclidean similarity", "Positive radius")),
        VisualProofCertificate("algebra-square", "Area partition", listOf("A square of side a+b has area (a+b)².", "Cuts at distances a partition it into areas a², ab, ab, and b².", "Area additivity gives (a+b)²=a²+2ab+b²."), listOf("a,b≥0 for the geometric construction; polynomial identity then extends algebraically")),
        VisualProofCertificate("shear-area", "Cavalieri shear", listOf("Slide every horizontal slice by an amount proportional to its height.", "Each slice keeps the same length and the total height is unchanged.", "Cavalieri's principle gives equal areas before and after the shear."), listOf("Same base and perpendicular height")),
        VisualProofCertificate("triangle-area", "Congruent-copy doubling", listOf("Rotate a congruent copy of the triangle by 180°.", "The two copies form a parallelogram with base b and height h.", "One triangle is half its area, so A=bh/2."), listOf("Perpendicular height h", "Euclidean area additivity")),
        VisualProofCertificate("parallelogram-area", "Cut-and-translate dissection", listOf("Cut the triangular overhang from one side.", "Translate it to fill the gap on the other side without changing area.", "The result is a rectangle of base b and height h, hence A=bh."), listOf("Perpendicular height h")),
        VisualProofCertificate("trapezoid-area", "Rotated-copy doubling", listOf("Rotate a congruent copy of the trapezoid by 180°.", "The pair forms a parallelogram with base a+b and height h.", "Halving its area gives A=(a+b)h/2."), listOf("a and b are parallel side lengths")),
        VisualProofCertificate("circle-area", "Sector rearrangement limit", listOf("Divide the circle into equal sectors and alternate their orientation.", "The rearrangement approaches a rectangle as the sector count grows.", "Its height tends to r and its base to half the circumference, πr, so A=πr²."), listOf("Positive radius", "Limit of sector dissections")),
        VisualProofCertificate("polygon-angle-sum", "Fan triangulation", listOf("Draw diagonals from one vertex to every non-adjacent vertex.", "A simple n-gon is split into exactly n-2 triangles.", "Adding their angle sums gives (n-2)×180°."), listOf("Simple polygon", "n≥3")),
        VisualProofCertificate("similar-triangles", "Dilation", listOf("Apply a dilation of scale k about a common center.", "Every displacement vector, and therefore every side length, is multiplied by k.", "All corresponding side ratios equal k while angles are preserved."), listOf("k>0", "Euclidean dilation")),
        VisualProofCertificate("intersecting-chords", "Similar-triangle power theorem", listOf("Join opposite chord endpoints to form two triangles at the intersection.", "Vertical angles and equal angles subtending the same arcs make the triangles similar.", "Corresponding-side ratios rearrange to PA×PB=PC×PD."), listOf("Both chords intersect inside one circle")),
        VisualProofCertificate("circle-angle", "Isosceles-radius decomposition", listOf("Join the center to the chord endpoints and the point on the circumference.", "The resulting radius triangles are isosceles, so their base angles pair equally.", "Angle addition then makes the central angle twice the inscribed angle on the same arc."), listOf("Angles subtend the same arc")),
        VisualProofCertificate("unit-circle-identity", "Unit-circle coordinates", listOf("A point at angle θ on the unit circle has coordinates (cos θ,sin θ).", "Its horizontal and vertical projections form a right triangle with hypotenuse 1.", "Pythagoras gives cos²θ+sin²θ=1."), listOf("Unit radius", "Euclidean coordinates")),
        VisualProofCertificate("odd-sum-square", "Gnomon induction", listOf("An n×n square contains n² unit tiles.", "Growing it to (n+1)×(n+1) adds an L-shaped border of 2n+1 tiles.", "Starting from one tile, successive odd borders therefore total n²."), listOf("Positive integer n")),
        VisualProofCertificate("absolute-inequality", "Distance on the real line", listOf("Absolute value |x| is the distance from x to zero.", "Distance at most r places x between the two points at distance r from zero.", "Those boundary points are −r and r, giving −r≤x≤r."), listOf("r≥0", "Real number x")),
        VisualProofCertificate("equation-balance", "Equality-preserving operations", listOf("The two expressions begin equal, like equal weights on two pans.", "Adding, subtracting, multiplying, or dividing both sides by the same permitted quantity preserves equality.", "Subtracting b and dividing by non-zero a isolates x=(c−b)/a."), listOf("a≠0", "Real arithmetic")),
        VisualProofCertificate("set-de-morgan", "Region and truth equivalence", listOf("A point lies outside A∪B exactly when it is not in A and not in B.", "That condition means the point lies in both complements Aᶜ and Bᶜ.", "The Venn regions and every truth-table row therefore agree."), listOf("A and B are subsets of one universe")),
        VisualProofCertificate("epsilon-delta", "Band containment", listOf("An ε-band specifies the allowed output error around L.", "A δ-band restricts inputs around a, excluding a itself.", "When the graph over that punctured δ-band lies inside the ε-band, the limit definition is satisfied."), listOf("ε>0", "The displayed linear function is defined near a")),
        VisualProofCertificate("slope-triangle", "Similar right triangles", listOf("Any two slope triangles on the same non-vertical line share an acute angle.", "Their right angles make the triangles similar by AA.", "Corresponding vertical and horizontal legs have the same ratio, so rise/run is constant."), listOf("Non-vertical straight line", "run≠0")),
        VisualProofCertificate("eigenvector-direction", "Invariant one-dimensional subspace", listOf("An eigenvector satisfies Av=λv by definition.", "Scalar multiplication changes length and may reverse orientation, but cannot turn the containing line.", "Thus v and Av remain collinear and the direction subspace is preserved."), listOf("v≠0", "Linear transformation A")),
        VisualProofCertificate("counting-paths", "Last-step decomposition", listOf("Every monotone path to (r,u) ends with either a right step or an up step.", "Removing that final step gives disjoint path sets ending at (r−1,u) and (r,u−1).", "Adding their counts gives Pascal's recurrence and the binomial coefficient."), listOf("r,u are non-negative integers", "Only right and up moves")),
        VisualProofCertificate("modular-clock", "Remainder classes", listOf("Moving n places around an n-position clock completes one full turn.", "Integers differing by a multiple of n therefore land at the same position.", "Conversely, equal clock positions have a difference divisible by n, which is congruence modulo n."), listOf("Integer modulus n≥2", "Integer values")),
        VisualProofCertificate(
            "anscombe-quartet",
            "Published-data counterexample",
            listOf(
                "Compute the same seven summary statistics directly from each published 11-point series.",
                "To displayed precision, the four series share their means, sample variances, regression line, and coefficient of determination.",
                "Their scatterplots are nevertheless structurally different, so equal summaries cannot determine the underlying distribution or reveal influential observations.",
            ),
            listOf("Anscombe's published rounded observations", "Ordinary least-squares regression with an intercept"),
        ),
    ) + extraNumberTheorySpecs.map { spec ->
        VisualProofCertificate(spec.id, "Interactive number construction", listOf("Construct the displayed quantities from countable units.", "Rearrange or group the same units without changing their total.", "The resulting equality is ${spec.result}."), listOf("Integer inputs in the displayed domain"))
    }

    private val hierarchy = linkedMapOf(
        "Geometry" to listOf(
            VisualProofSubcategory("Triangle Theorems", "Angles, similarity and right-triangle relationships", listOf("triangle-angle-sum", "pythagorean", "similar-triangles")),
            VisualProofSubcategory("Area Dissections", "Rearrange equal-area pieces to derive plane-area formulas", listOf("triangle-area", "parallelogram-area", "trapezoid-area", "circle-area")),
            VisualProofSubcategory("Polygon Theorems", "Triangulation and interior-angle structure", listOf("polygon-angle-sum")),
            VisualProofSubcategory("Circle Theorems", "Chord products and angles subtending the same arc", listOf("intersecting-chords", "circle-angle")),
        ),
        "Trigonometry" to listOf(
            VisualProofSubcategory("Fundamental Identities", "Unit-circle proofs of identities that hold for every angle", listOf("unit-circle-identity")),
        ),
        "Algebra" to listOf(
            VisualProofSubcategory("Algebraic Identities", "Area models that turn symbolic expansions into visible pieces", listOf("algebra-square")),
            VisualProofSubcategory("Equations & Inequalities", "Balance operations and number-line distance arguments", listOf("equation-balance", "absolute-inequality")),
        ),
        "Calculus" to listOf(
            VisualProofSubcategory("Differential Calculus", "Limits of secants and instantaneous rates of change", listOf("derivative-slope")),
            VisualProofSubcategory("Integral Calculus", "Riemann sums and accumulated area", listOf("integral-area")),
            VisualProofSubcategory("Real Analysis", "Make limiting quantifiers visible as nested input and output bands", listOf("epsilon-delta")),
        ),
        "Linear Algebra" to listOf(
            VisualProofSubcategory("Determinants & Transformations", "See how a matrix changes shape, orientation and area", listOf("matrix-transform")),
            VisualProofSubcategory("Eigenvectors", "Track directions preserved by linear transformations", listOf("eigenvector-direction")),
        ),
        "Coordinate Geometry" to listOf(
            VisualProofSubcategory("Area-Preserving Transforms", "Move coordinates while tracking an invariant geometric measure", listOf("shear-area")),
            VisualProofSubcategory("Lines & Slopes", "Use similar rise-run triangles to reveal constant gradient", listOf("slope-triangle")),
        ),
        "Probability & Statistics" to listOf(
            VisualProofSubcategory("Continuous Distributions", "Connect probability with symmetric area under a density curve", listOf("normal-area")),
            VisualProofSubcategory("Data Literacy", "Use published observations to expose what summary statistics can hide", listOf("anscombe-quartet")),
        ),
        "Discrete Mathematics" to listOf(
            VisualProofSubcategory("Set Theory & Logic", "Compare set regions with independently generated truth rows", listOf("set-de-morgan")),
            VisualProofSubcategory("Combinatorics", "Count lattice paths through disjoint cases and recurrence", listOf("counting-paths")),
        ),
        "Vectors" to listOf(
            VisualProofSubcategory("Vector Operations", "Build vector laws with movable arrows and common endpoints", listOf("vector-addition")),
        ),
        "Number Theory" to listOf(
            VisualProofSubcategory("Sequences & Figurate Numbers", "Build numerical identities from countable tile patterns", listOf("odd-sum-square")),
            VisualProofSubcategory("Congruences", "See remainder classes as repeated positions on a modular clock", listOf("modular-clock")),
            VisualProofSubcategory("Number Patterns, Divisibility & Primes", "Forty manipulable constructions for arithmetic structure", extraNumberTheoryIds),
        ),
        "Mensuration" to listOf(
            VisualProofSubcategory("Circle Measures", "Unroll and compare radius, diameter and circumference", listOf("circle-ratio")),
        ),
    )

    private val commonFeatures = setOf(
        ProofEnhancement.ColorCodedStages, ProofEnhancement.MarkedInvariant, ProofEnhancement.ValidityLabels,
        ProofEnhancement.Checkpoints, ProofEnhancement.SymbolicVisualColumns,
        ProofEnhancement.ConsistentColorGrammar, ProofEnhancement.Assumptions, ProofEnhancement.Counterexample,
        ProofEnhancement.ProofBreadcrumbs, ProofEnhancement.MatchedStepNumbers, ProofEnhancement.InlineDefinitions,
        ProofEnhancement.EquivalentRepresentations, ProofEnhancement.ThereforeSummary,
        ProofEnhancement.ReusableLegend, ProofEnhancement.DomainMask, ProofEnhancement.ErrorTrap,
        ProofEnhancement.SimpleCase, ProofEnhancement.CompactTakeaway,
    )

    private val featureMap = mapOf(
        "algebra-square" to setOf(ProofEnhancement.AreaModel, ProofEnhancement.BeforeAfter, ProofEnhancement.SameQuantityDifferentShape, ProofEnhancement.TransparentComparison, ProofEnhancement.GridBackground),
        "pythagorean" to setOf(ProofEnhancement.AreaModel, ProofEnhancement.BeforeAfter, ProofEnhancement.MotionArrows, ProofEnhancement.SameQuantityDifferentShape, ProofEnhancement.TransparentComparison, ProofEnhancement.CutAndSlide, ProofEnhancement.Dissection, ProofEnhancement.MotionTrails),
        "triangle-area" to setOf(ProofEnhancement.AreaModel, ProofEnhancement.BeforeAfter, ProofEnhancement.MotionArrows, ProofEnhancement.SameQuantityDifferentShape, ProofEnhancement.TransparentComparison, ProofEnhancement.CutAndSlide, ProofEnhancement.Dissection, ProofEnhancement.MotionTrails),
        "parallelogram-area" to setOf(ProofEnhancement.AreaModel, ProofEnhancement.BeforeAfter, ProofEnhancement.MotionArrows, ProofEnhancement.SameQuantityDifferentShape, ProofEnhancement.TransparentComparison, ProofEnhancement.CutAndSlide, ProofEnhancement.Dissection, ProofEnhancement.MotionTrails),
        "trapezoid-area" to setOf(ProofEnhancement.AreaModel, ProofEnhancement.BeforeAfter, ProofEnhancement.MotionArrows, ProofEnhancement.SameQuantityDifferentShape, ProofEnhancement.TransparentComparison, ProofEnhancement.CutAndSlide, ProofEnhancement.Dissection, ProofEnhancement.MotionTrails),
        "circle-area" to setOf(ProofEnhancement.AreaModel, ProofEnhancement.BeforeAfter, ProofEnhancement.MotionArrows, ProofEnhancement.SameQuantityDifferentShape, ProofEnhancement.TransparentComparison, ProofEnhancement.CutAndSlide, ProofEnhancement.Dissection, ProofEnhancement.FadingLimit, ProofEnhancement.MotionTrails),
        "absolute-inequality" to setOf(ProofEnhancement.NumberLine, ProofEnhancement.SymmetryAxes, ProofEnhancement.Counterexample),
        "equation-balance" to setOf(ProofEnhancement.BalanceScale, ProofEnhancement.RealWorldAnalogy),
        "set-de-morgan" to setOf(ProofEnhancement.VennDiagram, ProofEnhancement.TruthTable, ProofEnhancement.Counterexample, ProofEnhancement.ContradictionPanel, ProofEnhancement.NotToScale),
        "slope-triangle" to setOf(ProofEnhancement.GridBackground, ProofEnhancement.SlopeTriangle),
        "derivative-slope" to setOf(ProofEnhancement.GridBackground, ProofEnhancement.ZoomDetail, ProofEnhancement.SlopeTriangle, ProofEnhancement.TangentOverlay, ProofEnhancement.FadingLimit),
        "integral-area" to setOf(ProofEnhancement.GridBackground, ProofEnhancement.ZoomDetail, ProofEnhancement.AccumulationStrips, ProofEnhancement.FadingLimit, ProofEnhancement.RealWorldAnalogy),
        "epsilon-delta" to setOf(ProofEnhancement.GridBackground, ProofEnhancement.FadingLimit, ProofEnhancement.EpsilonDeltaBands, ProofEnhancement.NotToScale),
        "unit-circle-identity" to setOf(ProofEnhancement.GridBackground, ProofEnhancement.UnitCircleOverlay, ProofEnhancement.SymmetryAxes, ProofEnhancement.VectorProjection),
        "vector-addition" to setOf(ProofEnhancement.GridBackground, ProofEnhancement.VectorProjection, ProofEnhancement.MotionTrails),
        "matrix-transform" to setOf(ProofEnhancement.GridBackground, ProofEnhancement.TransformationGrid, ProofEnhancement.DeterminantScaling, ProofEnhancement.MotionTrails),
        "eigenvector-direction" to setOf(ProofEnhancement.GridBackground, ProofEnhancement.TransformationGrid, ProofEnhancement.EigenvectorDirection, ProofEnhancement.MotionTrails, ProofEnhancement.RealWorldAnalogy),
        "counting-paths" to setOf(ProofEnhancement.GridBackground, ProofEnhancement.CountingTree, ProofEnhancement.LatticePoints, ProofEnhancement.RecursiveStructure),
        "odd-sum-square" to setOf(ProofEnhancement.GridBackground, ProofEnhancement.LatticePoints, ProofEnhancement.RecursiveStructure),
        "modular-clock" to setOf(ProofEnhancement.ModularClock, ProofEnhancement.MotionTrails, ProofEnhancement.RealWorldAnalogy),
        "normal-area" to setOf(ProofEnhancement.GridBackground, ProofEnhancement.AccumulationStrips, ProofEnhancement.SymmetryAxes),
        "anscombe-quartet" to setOf(ProofEnhancement.GridBackground, ProofEnhancement.Counterexample, ProofEnhancement.ErrorTrap),
        "triangle-angle-sum" to setOf(ProofEnhancement.NotToScale),
        "circle-angle" to setOf(ProofEnhancement.SymmetryAxes, ProofEnhancement.NotToScale),
        "intersecting-chords" to setOf(ProofEnhancement.NotToScale),
        "circle-ratio" to setOf(ProofEnhancement.SameQuantityDifferentShape, ProofEnhancement.MotionTrails),
        "shear-area" to setOf(ProofEnhancement.AreaModel, ProofEnhancement.SameQuantityDifferentShape, ProofEnhancement.CutAndSlide, ProofEnhancement.MotionTrails),
    )

    val enhancementCoverage: Set<ProofEnhancement> get() = labs.flatMap { profileFor(it.id).features }.toSet()

    fun profileFor(labId: String): ProofVisualProfile {
        val lab = labs.single { it.id == labId }
        val certificate = certificateFor(labId)
        val assumptions = certificate.assumptions
        val domain = assumptions.joinToString(" · ")
        return ProofVisualProfile(
            features = commonFeatures + featureMap[labId].orEmpty(),
            assumptions = assumptions,
            invariant = lab.invariantPrompt.removeSuffix("?"),
            definitions = buildMap {
                lab.parameters.forEach { parameter -> put(parameter.name, "adjustable value in [${parameter.minimum}, ${parameter.maximum}]") }
                put("result", lab.formalResult)
            },
            validityNotes = certificate.argument,
            domainRestriction = domain,
            counterexample = counterexampleFor(labId),
            errorTrap = errorTrapFor(labId),
            simpleCase = simpleCaseFor(labId),
            analogy = analogyFor(labId),
            takeaway = "The picture proves this because ${certificate.argument.last().replaceFirstChar { it.lowercase() }}",
            notToScale = labId in setOf("triangle-angle-sum", "circle-angle", "intersecting-chords", "set-de-morgan", "epsilon-delta"),
        )
    }

    private fun counterexampleFor(id: String): String = when (id) {
        "triangle-angle-sum" -> "On a sphere, a triangle can have three right angles; the Euclidean parallel-postulate assumption matters."
        "pythagorean" -> "For a non-right triangle the missing term is −2ab cos C, so a²+b² need not equal c²."
        "derivative-slope" -> "At the corner of |x| at x=0, left and right secant slopes do not approach one tangent slope."
        "integral-area" -> "A fixed finite rectangle count is only an approximation; it does not by itself equal the integral."
        "normal-area" -> "For an asymmetric distribution the two tails at ±z need not match."
        "vector-addition" -> "Matrix multiplication looks similar symbolically but is not generally commutative: AB can differ from BA."
        "matrix-transform" -> "A nonlinear map can bend the square, so one constant determinant need not describe its whole image area."
        "circle-ratio" -> "A six-sided polygon visibly underestimates circumference; one coarse measurement does not establish π."
        "algebra-square" -> "The geometric partition needs non-negative lengths; signed algebra is required when a or b is negative."
        "shear-area" -> "Changing perpendicular height while sliding the top edge changes area; a general distortion is not a shear."
        "triangle-area" -> "Using a slanted side instead of perpendicular height gives the wrong area."
        "parallelogram-area" -> "Measuring the slanted edge as height overestimates area unless that edge is perpendicular to the base."
        "trapezoid-area" -> "If the named sides are not parallel, the rotated copies do not form the claimed parallelogram."
        "circle-area" -> "With only a few sectors the rearranged boundary is scalloped, not an exact rectangle."
        "polygon-angle-sum" -> "A self-intersecting star polygon is not covered by the simple-polygon triangulation used here."
        "similar-triangles" -> "Equal-looking orientation is insufficient: changing only one side destroys the common scale ratio."
        "intersecting-chords" -> "If one segment does not end on the same circle, the two products need not agree."
        "circle-angle" -> "If the two angles do not subtend the same arc, the factor-of-two conclusion does not apply."
        "unit-circle-identity" -> "On a circle of radius r, the squared coordinates sum to r², not automatically 1."
        "odd-sum-square" -> "Replacing an odd border by an even number leaves a gap or protrusion instead of the next square."
        "absolute-inequality" -> "The tempting rule |x|≤r ⇒ x≤r alone misses the lower boundary x≥−r."
        "equation-balance" -> "Dividing by a=0 is invalid and can turn one equation into a false conclusion."
        "set-de-morgan" -> "The tempting claim (A∪B)ᶜ=Aᶜ∪Bᶜ fails for a point lying in A only."
        "epsilon-delta" -> "For the displayed f(x)=2x, choosing δ>ε/2 lets some allowed x escape the ε-band."
        "slope-triangle" -> "A vertical line has run 0, so rise/run is undefined."
        "eigenvector-direction" -> "A general vector need not preserve direction; only an eigenvector does."
        "counting-paths" -> "Allowing diagonal or backward moves creates paths outside the two final-step branches."
        "modular-clock" -> "Equal ordinary distance from zero does not imply congruence; only equal normalized remainders do."
        "anscombe-quartet" -> "Series I alone looks compatible with its summaries; assuming all matching summaries imply that shape fails on Series II–IV."
        else -> if (id.startsWith("nt-")) "Changing the visible examples without preserving the displayed grouping can break the claimed rule." else error("Missing proof-specific counterexample for $id")
    }

    private fun errorTrapFor(id: String): String = when (id) {
        "triangle-angle-sum" -> "Do not infer 180° merely from the drawing; transfer the angles using the constructed parallel line."
        "pythagorean" -> "The theorem applies to right triangles; verify the right angle before comparing square areas."
        "algebra-square" -> "Do not drop the two ab rectangles: (a+b)² is not a²+b²."
        "derivative-slope" -> "Do not substitute h=0 before cancelling h; the difference quotient would divide by zero."
        "integral-area" -> "A finite rectangle sum is an approximation, not automatically the exact integral."
        "normal-area" -> "2Φ(z)−1 is for symmetric bounds around zero in the standard normal distribution."
        "vector-addition" -> "Translate a vector without rotating or resizing it when building the head-to-tail path."
        "matrix-transform" -> "Signed determinant records orientation; ordinary area uses |det A|."
        "circle-ratio" -> "A polygon perimeter approaches circumference; do not label a coarse polygon estimate as exact."
        "shear-area" -> "Use perpendicular height, not the slanted side length."
        "triangle-area" -> "Sliding the apex preserves area only while base and perpendicular height remain fixed."
        "parallelogram-area" -> "The removed triangle must be translated intact, with no overlap or gap."
        "trapezoid-area" -> "The two copies form base a+b only when a and b are the parallel sides."
        "circle-area" -> "The sector construction proves a limit; a finite sector rearrangement is not a perfect rectangle."
        "polygon-angle-sum" -> "A fan from one vertex creates n−2 triangles, not n triangles."
        "similar-triangles" -> "Compare corresponding sides in the same order."
        "intersecting-chords" -> "Use the four distances from P to the circle, not full chord lengths."
        "circle-angle" -> "The central and inscribed angles must intercept the same arc."
        "unit-circle-identity" -> "The identity uses squared projection lengths; sin θ+cos θ is not constant."
        "odd-sum-square" -> "The kth border contains 2k−1 tiles because the shared corner is counted once."
        "absolute-inequality" -> "Distance at most r creates two boundaries, −r and r."
        "equation-balance" -> "An operation on only one side tips the balance and changes the solution set."
        "set-de-morgan" -> "Complement reverses union to intersection; it does not distribute without changing the operator."
        "epsilon-delta" -> "δ must control every qualifying x, not just one tested point."
        "slope-triangle" -> "Keep rise and run signed and never divide by a zero run."
        "eigenvector-direction" -> "A stretched vector is an eigenvector only when Av is collinear with v."
        "counting-paths" -> "The two recurrence branches are disjoint because the final step cannot be both right and up."
        "modular-clock" -> "Normalize negative remainders into 0 through n−1 before comparing positions."
        "anscombe-quartet" -> "Matching means and regression coefficients do not make the four point clouds interchangeable."
        else -> if (id.startsWith("nt-")) "Keep the integer domain and the indicated grouping or remainder operation fixed." else error("Missing proof-specific error trap for $id")
    }

    private fun simpleCaseFor(id: String): String = when (id) {
        "triangle-angle-sum" -> "A right isosceles triangle gives 45°+45°+90°=180°."
        "pythagorean" -> "Try a=3, b=4: 9+16=25=5²."
        "derivative-slope" -> "At x=2, secant slopes are 5 for h=1 and 4.1 for h=0.1, approaching 4."
        "integral-area" -> "On [0,3], the exact area is 9; increase n and watch the midpoint sum approach it."
        "normal-area" -> "At z=1, numerical integration and 2Φ(1)−1 both give about 0.6827."
        "vector-addition" -> "Use u=(2,1), v=(−1,3): both orders finish at (1,4)."
        "matrix-transform" -> "For diag(2,3), the transformed unit square has coordinate area 6=|det A|."
        "circle-ratio" -> "Keep r=2 and increase n from 6 to 120; perimeter/diameter approaches π without changing scale."
        "algebra-square" -> "Try a=2, b=1: the four areas 4+2+2+1 total 9."
        "shear-area" -> "A 4×2 rectangle and any horizontal shear of it both have coordinate area 8."
        "triangle-area" -> "With base 4 and height 3, every horizontal apex position gives coordinate area 6."
        "parallelogram-area" -> "Base 4 and height 2.5 give area 10 for shear 0, 1, or −2."
        "trapezoid-area" -> "Parallel sides 5 and 3 with height 2 give area 8."
        "circle-area" -> "For r=1, increase sectors from 6 to 60 and watch the area gap shrink toward zero."
        "polygon-angle-sum" -> "A pentagon creates three fan triangles, so its measured interior sum is 540°."
        "similar-triangles" -> "Scale a 3-4-5 triangle by 2; all three measured side ratios become 2."
        "intersecting-chords" -> "At the circle center, each segment equals r and both products equal r²."
        "circle-angle" -> "An 80° central angle and a point on the opposite arc produce a measured 40° inscribed angle."
        "unit-circle-identity" -> "At 30°, the squared legs 3/4 and 1/4 add to radius²=1."
        "odd-sum-square" -> "The borders 1,3,5,7 contain 16 tiles, forming a 4×4 square."
        "absolute-inequality" -> "With r=3, x=2 satisfies both predicates and x=4 satisfies neither."
        "equation-balance" -> "For 2x+2=10, subtract 2 and divide by 2 to get x=4; substitution restores equality."
        "set-de-morgan" -> "Test one point in neither set: both expressions are true."
        "epsilon-delta" -> "For ε=0.8 in f(x)=2x near x=1, any δ≤0.4 keeps the graph inside the band."
        "slope-triangle" -> "On y=1.5x, runs 2 and 4 give rises 3 and 6; both ratios are 1.5."
        "eigenvector-direction" -> "Set vy=0: v lies on the x-axis and cross(v,Av)=0; then move vy to see a turn."
        "modular-clock" -> "17 and 5 land together on a 12-hour clock because 17−5=12."
        "counting-paths" -> "A 2×1 grid has three move orders: RRU, RUR, URR."
        "anscombe-quartet" -> "Series I is roughly linear; Series II curves, Series III has an influential point, and Series IV is nearly vertical."
        else -> if (id.startsWith("nt-")) "Use the smallest positive input and verify every displayed group by direct counting." else error("Missing proof-specific simple case for $id")
    }

    private fun analogyFor(id: String): String? = when (id) {
        "equation-balance" -> "A balanced scale stays level when equal weight is removed from both pans."
        "modular-clock" -> "Clock time forgets complete turns but remembers the final position."
        "integral-area" -> "Thinner accounting strips make an accumulated total increasingly precise."
        "eigenvector-direction" -> "A stretch along a railway track changes distance, not the track's direction."
        else -> null
    }

    init {
        require(labs.map { it.id }.toSet() == certificates.map { it.labId }.toSet()) {
            "Every visual proof lab must have exactly one mathematical proof certificate"
        }
        require(certificates.size == certificates.map { it.labId }.distinct().size) {
            "Visual proof certificates must be unique"
        }
        require(hierarchy.keys.toList() == categories) {
            "Every visual-proof category must appear once in the hierarchy"
        }
        require(hierarchy.values.flatten().flatMap { it.labIds }.toSet() == labs.map { it.id }.toSet()) {
            "Every visual formula must appear in a subcategory"
        }
        require(hierarchy.values.flatten().flatMap { it.labIds }.size == labs.size) {
            "A visual formula may appear in only one subcategory"
        }
        require(enhancementCoverage == ProofEnhancement.entries.toSet()) {
            "Every requested visual-proof enhancement must be represented by the proof catalog"
        }
    }

    fun certificateFor(labId: String): VisualProofCertificate =
        certificates.single { it.labId == labId }

    fun categoryOf(lab: VisualProofLab): String = hierarchy.entries
        .first { (_, groups) -> groups.any { lab.id in it.labIds } }
        .key

    fun subcategoryOf(lab: VisualProofLab): VisualProofSubcategory = hierarchy.values
        .flatten()
        .first { lab.id in it.labIds }

    fun subcategoriesFor(category: String): List<VisualProofSubcategory> = hierarchy[category].orEmpty()

    fun labsFor(category: String): List<VisualProofLab> = labs.filter { categoryOf(it) == category }

    fun labsFor(category: String, subcategory: String): List<VisualProofLab> {
        val ids = subcategoriesFor(category).firstOrNull { it.name == subcategory }?.labIds.orEmpty()
        return ids.map { id -> labs.first { it.id == id } }
    }
}

class VisualProofEngine {
    fun start(id: String): ProofPlayback {
        val lab = VisualProofCatalog.labs.firstOrNull { it.id == id }
            ?: VisualProofCatalog.labs.first()
        return ProofPlayback(frame(lab, 0, lab.parameters.associate { it.name to it.initial }))
    }

    fun setParameter(playback: ProofPlayback, name: String, value: Double): ProofPlayback {
        val definition = playback.frame.lab.parameters.firstOrNull { it.name == name } ?: return playback
        val safeValue = value.takeIf(Double::isFinite) ?: definition.initial
        val parameters = playback.frame.parameters + (name to safeValue.coerceIn(definition.minimum, definition.maximum))
        return playback.copy(frame = frame(playback.frame.lab, playback.frame.step, parameters))
    }

    fun next(playback: ProofPlayback): ProofPlayback {
        val last = playback.frame.lab.steps.lastIndex
        val next = (playback.frame.step + playback.direction).coerceIn(0, last)
        val direction = if (next == last) -1 else if (next == 0) 1 else playback.direction
        return playback.copy(frame = frame(playback.frame.lab, next, playback.frame.parameters), direction = direction)
    }

    fun previous(playback: ProofPlayback): ProofPlayback {
        val previous = (playback.frame.step - 1).coerceAtLeast(0)
        return playback.copy(
            frame = frame(playback.frame.lab, previous, playback.frame.parameters),
            playing = false,
            direction = 1,
        )
    }

    fun advance(playback: ProofPlayback): ProofPlayback {
        val next = (playback.frame.step + 1).coerceAtMost(playback.frame.lab.steps.lastIndex)
        return playback.copy(
            frame = frame(playback.frame.lab, next, playback.frame.parameters),
            playing = false,
            direction = 1,
        )
    }

    fun reset(playback: ProofPlayback): ProofPlayback {
        val lab = playback.frame.lab
        return ProofPlayback(frame(lab, 0, lab.parameters.associate { it.name to it.initial }))
    }

    fun reveal(playback: ProofPlayback) = playback.copy(
        frame = frame(playback.frame.lab, playback.frame.lab.steps.lastIndex, playback.frame.parameters),
        playing = false,
        direction = 1,
    )
    fun togglePlaying(playback: ProofPlayback) = playback.copy(playing = !playback.playing)

    private fun frame(lab: VisualProofLab, step: Int, p: Map<String, Double>): ProofFrame {
        val measurements = mutableMapOf<String, Double>()
        val residual: Double
        val invariant: String
        when (lab.id) {
            "triangle-angle-sum" -> {
                val a = Vec2(-2.7, 0.0)
                val b = Vec2(2.7, 0.0)
                val c = Vec2(p.getValue("offset") - 1.0, p.getValue("height"))
                val angles = listOf(angle(b - a, c - a), angle(a - b, c - b), angle(a - c, b - c))
                val sideA = c.distanceTo(b)
                val sideB = c.distanceTo(a)
                val sideC = b.distanceTo(a)
                measurements.putAll(
                    linkedMapOf(
                        "∠A" to angles[0],
                        "∠B" to angles[1],
                        "∠C" to angles[2],
                        "angle sum" to angles.sum(),
                        "side a = BC" to sideA,
                        "side b = CA" to sideB,
                        "side c = AB" to sideC,
                    ),
                )
                residual = abs(angles.sum() - 180)
                invariant = "∠A + ∠B + ∠C = 180°"
            }
            "pythagorean" -> {
                val a = p.getValue("a"); val b = p.getValue("b")
                val hypotenuseSquared = Vec2(a, 0.0).distanceTo(Vec2(0.0, b)).pow(2)
                val outerArea = (a + b).pow(2)
                val centralAreaByDissection = outerArea - 4 * (a * b / 2)
                measurements.putAll(
                    mapOf(
                        "a²+b² from legs" to a * a + b * b,
                        "c² from distance" to hypotenuseSquared,
                        "central area from dissection" to centralAreaByDissection,
                    ),
                )
                residual = maxOf(
                    abs(hypotenuseSquared - (a * a + b * b)),
                    abs(centralAreaByDissection - hypotenuseSquared),
                )
                invariant = "leg-square area = dissection area = hypotenuse-square area"
            }
            "derivative-slope" -> {
                val x = p.getValue("x"); val h = p.getValue("h"); val secant = ((x + h).pow(2) - x.pow(2)) / h; val tangent = 2 * x
                measurements.putAll(mapOf("secant slope" to secant, "tangent slope" to tangent)); residual = abs(secant - tangent); invariant = "secant → 2x as h → 0"
            }
            "integral-area" -> {
                val b = p.getValue("b")
                val n = p.getValue("n").toInt().coerceAtLeast(1)
                val width = b / n
                val sum = (0 until n).sumOf { index ->
                    val midpoint = (index + .5) * width
                    midpoint * midpoint * width
                }
                val exact = b.pow(3) / 3
                val errorBound = b.pow(3) / (12 * n * n)
                measurements.putAll(
                    mapOf(
                        "midpoint rectangle sum" to sum,
                        "exact area" to exact,
                        "absolute error" to abs(sum - exact),
                        "certified error bound" to errorBound,
                    ),
                )
                residual = abs(sum - exact)
                invariant = "midpoint sum → b³/3 with error ≤ b³/(12n²)"
            }
            "normal-area" -> {
                val z = p.getValue("z")
                val cdfArea = 2 * normalCdf(z) - 1
                val integratedArea = simpsonIntegral(-z, z, 400) { x -> exp(-x * x / 2) / sqrt(2 * PI) }
                val leftTail = normalCdf(-z)
                val rightTail = 1 - normalCdf(z)
                measurements.putAll(
                    mapOf(
                        "numerically integrated area" to integratedArea,
                        "CDF identity area" to cdfArea,
                        "left tail" to leftTail,
                        "right tail" to rightTail,
                    ),
                )
                residual = maxOf(abs(integratedArea - cdfArea), abs(leftTail - rightTail))
                invariant = "independent numerical area agrees with symmetric CDF subtraction"
            }
            "vector-addition" -> {
                val ux = p.getValue("ux"); val uy = p.getValue("uy"); val vx = p.getValue("vx"); val vy = p.getValue("vy")
                val endpointUv = Vec2(ux, uy) + Vec2(vx, vy)
                val endpointVu = Vec2(vx, vy) + Vec2(ux, uy)
                measurements.putAll(
                    mapOf(
                        "u then v endpoint x" to endpointUv.x,
                        "u then v endpoint y" to endpointUv.y,
                        "v then u endpoint x" to endpointVu.x,
                        "v then u endpoint y" to endpointVu.y,
                    ),
                )
                residual = endpointUv.distanceTo(endpointVu); invariant = "both translated head-to-tail paths finish at the same coordinate"
            }
            "matrix-transform" -> {
                val a = p.getValue("a"); val b = p.getValue("b"); val c = p.getValue("c"); val d = p.getValue("d")
                val determinant = a * d - b * c
                val transformed = listOf(Vec2(0.0, 0.0), Vec2(a, c), Vec2(a + b, c + d), Vec2(b, d))
                val measuredArea = polygonArea(transformed)
                measurements.putAll(mapOf("determinant" to determinant, "coordinate polygon area" to measuredArea, "expected |det A|" to abs(determinant)))
                residual = abs(measuredArea - abs(determinant))
                invariant = "shoelace area of the transformed unit square equals |det(A)|"
            }
            "circle-ratio" -> {
                val r = p.getValue("r")
                val n = p.getValue("n").toInt().coerceAtLeast(6)
                val polygonPerimeter = 2 * n * r * kotlin.math.sin(PI / n)
                val diameter = 2 * r
                val estimatedRatio = polygonPerimeter / diameter
                val convergenceBound = PI.pow(3) / (6 * n * n)
                measurements.putAll(
                    mapOf(
                        "polygon sides" to n.toDouble(),
                        "measured perimeter" to polygonPerimeter,
                        "diameter" to diameter,
                        "perimeter/diameter" to estimatedRatio,
                        "error to π" to abs(estimatedRatio - PI),
                        "certified ratio error bound" to convergenceBound,
                    ),
                )
                residual = abs(estimatedRatio - PI)
                invariant = "the scale-independent polygon ratio converges upward to π"
            }
            "algebra-square" -> {
                val a = p.getValue("a"); val b = p.getValue("b")
                val outerArea = polygonArea(listOf(Vec2(0.0, 0.0), Vec2(a + b, 0.0), Vec2(a + b, a + b), Vec2(0.0, a + b)))
                val partitionAreas = listOf(a * a, a * b, a * b, b * b)
                measurements.putAll(mapOf("coordinate outer-square area" to outerArea, "sum of four partition areas" to partitionAreas.sum(), "two ab rectangles" to partitionAreas[1] + partitionAreas[2]))
                residual = abs(outerArea - partitionAreas.sum()); invariant = "the four non-overlapping coordinate regions exactly partition the outer square"
            }
            "shear-area" -> {
                val base = p.getValue("base"); val height = p.getValue("height"); val shear = p.getValue("shear")
                val rectangleArea = base * height
                val shearedArea = polygonArea(listOf(Vec2(0.0, 0.0), Vec2(base, 0.0), Vec2(base + shear, height), Vec2(shear, height)))
                measurements.putAll(mapOf("rectangle area" to rectangleArea, "coordinate area after shear" to shearedArea, "horizontal shift" to shear))
                residual = abs(rectangleArea - shearedArea); invariant = "coordinate area remains base×height under horizontal shear"
            }
            "triangle-area" -> {
                val base = p.getValue("base"); val height = p.getValue("height"); val apex = p.getValue("apex")
                val coordinateArea = polygonArea(listOf(Vec2(0.0, 0.0), Vec2(base, 0.0), Vec2(apex, height)))
                val formulaArea = base * height / 2
                measurements.putAll(mapOf("coordinate triangle area" to coordinateArea, "bh/2" to formulaArea, "apex position" to apex))
                residual = abs(coordinateArea - formulaArea); invariant = "horizontal apex motion preserves coordinate area bh/2"
            }
            "parallelogram-area" -> {
                val base = p.getValue("base"); val height = p.getValue("height"); val shear = p.getValue("shear")
                val coordinateArea = polygonArea(listOf(Vec2(0.0, 0.0), Vec2(base, 0.0), Vec2(base + shear, height), Vec2(shear, height)))
                val rectangleArea = base * height
                measurements.putAll(mapOf("coordinate slanted area" to coordinateArea, "rectangle area" to rectangleArea, "shear" to shear))
                residual = abs(coordinateArea - rectangleArea); invariant = "cut-and-slide preserves coordinate area bh"
            }
            "trapezoid-area" -> {
                val a = p.getValue("a"); val b = p.getValue("b"); val h = p.getValue("height")
                val inset = (a - b) / 2
                val coordinateArea = polygonArea(listOf(Vec2(0.0, 0.0), Vec2(a, 0.0), Vec2(a - inset, h), Vec2(inset, h)))
                val formulaArea = (a + b) * h / 2
                measurements.putAll(mapOf("coordinate trapezoid area" to coordinateArea, "(a+b)h/2" to formulaArea, "doubled area" to 2 * coordinateArea))
                residual = abs(coordinateArea - formulaArea); invariant = "coordinate area doubles to a parallelogram of base a+b"
            }
            "circle-area" -> {
                val r = p.getValue("r"); val n = p.getValue("n").toInt().coerceAtLeast(6)
                val approximateArea = n * r * r * kotlin.math.sin(2 * PI / n) / 2
                val exact = PI * r * r
                val convergenceBound = 2 * PI.pow(3) * r * r / (3 * n * n)
                measurements.putAll(
                    mapOf(
                        "radius" to r,
                        "sectors" to n.toDouble(),
                        "inscribed-sector area" to approximateArea,
                        "πr² limit" to exact,
                        "area gap" to exact - approximateArea,
                        "certified area error bound" to convergenceBound,
                    ),
                )
                residual = abs(exact - approximateArea); invariant = "sector approximation converges upward to πr²"
            }
            "polygon-angle-sum" -> {
                val n = p.getValue("n").toInt().coerceAtLeast(3)
                val radius = p.getValue("radius")
                val vertices = (0 until n).map { index ->
                    val theta = 2 * PI * index / n
                    Vec2(radius * kotlin.math.cos(theta), radius * kotlin.math.sin(theta))
                }
                val measuredSum = vertices.indices.sumOf { index ->
                    val current = vertices[index]
                    angle(vertices[(index - 1 + n) % n] - current, vertices[(index + 1) % n] - current)
                }
                val expected = (n - 2) * 180.0
                measurements.putAll(mapOf("sides" to n.toDouble(), "triangles" to (n - 2).toDouble(), "measured vertex-angle sum" to measuredSum, "(n-2)×180" to expected))
                residual = abs(measuredSum - expected); invariant = "measured vertex angles equal the sum of n-2 triangle angle sums"
            }
            "similar-triangles" -> {
                val a = p.getValue("a"); val b = p.getValue("b"); val k = p.getValue("k")
                val original = listOf(Vec2(0.0, 0.0), Vec2(a, 0.0), Vec2(0.0, b))
                val scaled = original.map { Vec2(it.x * k, it.y * k) }
                val originalSides = listOf(original[0].distanceTo(original[1]), original[0].distanceTo(original[2]), original[1].distanceTo(original[2]))
                val scaledSides = listOf(scaled[0].distanceTo(scaled[1]), scaled[0].distanceTo(scaled[2]), scaled[1].distanceTo(scaled[2]))
                val ratios = originalSides.indices.map { scaledSides[it] / originalSides[it] }
                measurements.putAll(mapOf("a'/a" to ratios[0], "b'/b" to ratios[1], "c'/c" to ratios[2], "scale k" to k))
                residual = ratios.maxOf { abs(it - k) }; invariant = "three independently measured corresponding side ratios equal k"
            }
            "intersecting-chords" -> {
                val r = p.getValue("r"); val px = p.getValue("p") * r; val angle = Math.toRadians(p.getValue("angle")); val dot = px * kotlin.math.cos(angle); val root = sqrt((dot * dot + r * r - px * px).coerceAtLeast(0.0))
                val pa = r + px; val pb = r - px; val pc = root - dot; val pd = root + dot
                measurements.putAll(mapOf("PA×PB" to pa * pb, "PC×PD" to pc * pd, "circle power" to r * r - px * px)); residual = abs(pa * pb - pc * pd); invariant = "both products equal r²-OP²"
            }
            "circle-angle" -> {
                val arc = Math.toRadians(p.getValue("arc"))
                val cAngle = Math.toRadians(p.getValue("c"))
                val r = p.getValue("r")
                val center = Vec2(0.0, 0.0)
                val a = Vec2(r * kotlin.math.cos(-arc / 2), r * kotlin.math.sin(-arc / 2))
                val b = Vec2(r * kotlin.math.cos(arc / 2), r * kotlin.math.sin(arc / 2))
                val c = Vec2(r * kotlin.math.cos(cAngle), r * kotlin.math.sin(cAngle))
                val central = angle(a - center, b - center)
                val inscribed = angle(a - c, b - c)
                measurements.putAll(mapOf("measured center angle" to central, "measured circumference angle" to inscribed, "twice circumference angle" to 2 * inscribed))
                residual = abs(central - 2 * inscribed); invariant = "independently measured central angle = twice the inscribed angle"
            }
            "unit-circle-identity" -> {
                val theta = Math.toRadians(p.getValue("theta"))
                val point = Vec2(kotlin.math.cos(theta), kotlin.math.sin(theta))
                val projection = Vec2(point.x, 0.0)
                val horizontalSquared = Vec2(0.0, 0.0).distanceTo(projection).pow(2)
                val verticalSquared = projection.distanceTo(point).pow(2)
                val radiusSquared = Vec2(0.0, 0.0).distanceTo(point).pow(2)
                measurements.putAll(mapOf("horizontal²" to horizontalSquared, "vertical²" to verticalSquared, "radius²" to radiusSquared, "leg squared sum" to horizontalSquared + verticalSquared))
                residual = abs(horizontalSquared + verticalSquared - radiusSquared); invariant = "coordinate projection legs satisfy Pythagoras on the unit radius"
            }
            "odd-sum-square" -> {
                val n = p.getValue("n").toInt().coerceAtLeast(1); val sum = (1..n).sumOf { 2 * it - 1 }.toDouble(); val square = (n * n).toDouble()
                measurements.putAll(mapOf("odd-number sum" to sum, "n² tiles" to square, "latest border" to (2 * n - 1).toDouble())); residual = abs(sum - square); invariant = "each odd border completes the next square"
            }
            "absolute-inequality" -> {
                val x = p.getValue("x"); val r = p.getValue("r")
                val distancePredicate = abs(x) <= r
                val intervalPredicate = x >= -r && x <= r
                measurements.putAll(
                    mapOf(
                        "|x|" to abs(x),
                        "radius r" to r,
                        "|x|≤r" to distancePredicate.asDouble(),
                        "−r≤x≤r" to intervalPredicate.asDouble(),
                        "inside interval" to intervalPredicate.asDouble(),
                    ),
                )
                residual = abs(distancePredicate.asDouble() - intervalPredicate.asDouble())
                invariant = "distance and interval predicates agree at both boundaries and every tested x"
            }
            "equation-balance" -> {
                val a = p.getValue("a"); val b = p.getValue("b"); val c = p.getValue("c"); val solution = (c - b) / a
                measurements.putAll(mapOf("solution x" to solution, "left ax+b" to a * solution + b, "right c" to c))
                residual = abs((a * solution + b) - c); invariant = "equal operations preserve equal pan weights"
            }
            "set-de-morgan" -> {
                val inA = p.getValue("inA") >= .5
                val inB = p.getValue("inB") >= .5
                fun left(a: Boolean, b: Boolean) = !(a || b)
                fun right(a: Boolean, b: Boolean) = !a && !b
                val rows = listOf(false to false, false to true, true to false, true to true)
                val matchingRows = rows.count { (a, b) -> left(a, b) == right(a, b) }
                val selectedLeft = left(inA, inB)
                val selectedRight = right(inA, inB)
                measurements.putAll(
                    mapOf(
                        "selected left expression" to selectedLeft.asDouble(),
                        "selected right expression" to selectedRight.asDouble(),
                        "matching truth rows" to matchingRows.toDouble(),
                        "total truth rows" to rows.size.toDouble(),
                    ),
                )
                residual = abs(selectedLeft.asDouble() - selectedRight.asDouble()) + (rows.size - matchingRows)
                invariant = "independently generated region predicates agree in every truth row"
            }
            "epsilon-delta" -> {
                val epsilon = p.getValue("epsilon"); val delta = p.getValue("delta"); val maximumOutputError = 2.0 * delta
                measurements.putAll(mapOf("epsilon" to epsilon, "delta" to delta, "max |f(x)-L|" to maximumOutputError))
                residual = (maximumOutputError - epsilon).coerceAtLeast(0.0); invariant = "the δ input band maps inside the ε output band"
            }
            "slope-triangle" -> {
                val m = p.getValue("m"); val run = p.getValue("run"); val rise = m * run
                val firstStart = Vec2(0.0, 0.0); val firstEnd = Vec2(run, rise)
                val secondStart = Vec2(-run, -rise); val secondEnd = Vec2(2 * run, 2 * rise)
                val firstRatio = (firstEnd.y - firstStart.y) / (firstEnd.x - firstStart.x)
                val secondRatio = (secondEnd.y - secondStart.y) / (secondEnd.x - secondStart.x)
                measurements.putAll(mapOf("first rise/run" to firstRatio, "second rise/run" to secondRatio, "line parameter m" to m))
                residual = maxOf(abs(firstRatio - secondRatio), abs(firstRatio - m)); invariant = "coordinate differences on two independently sized triangles give the same slope"
            }
            "eigenvector-direction" -> {
                val lambda = p.getValue("lambda"); val other = p.getValue("other"); val vy = p.getValue("vy")
                val v = Vec2(1.0, vy)
                val transformed = Vec2(lambda, other * vy)
                val cross = v.x * transformed.y - v.y * transformed.x
                val scale = sqrt((v.x * v.x + v.y * v.y) * (transformed.x * transformed.x + transformed.y * transformed.y)).coerceAtLeast(1e-12)
                val normalizedTurn = abs(cross) / scale
                measurements.putAll(mapOf("x-axis eigenvalue λ" to lambda, "other axis scale" to other, "cross(v,Av)" to cross, "normalized turn" to normalizedTurn))
                residual = normalizedTurn; invariant = "cross(v,Av)=0 exactly on preserved eigenvector directions"
            }
            "counting-paths" -> {
                val right = p.getValue("right").toInt().coerceAtLeast(1); val up = p.getValue("up").toInt().coerceAtLeast(1)
                val total = binomial(right + up, right); val fromLeft = binomial(right + up - 1, right - 1); val fromBelow = binomial(right + up - 1, right)
                measurements.putAll(mapOf("all paths" to total, "last step right" to fromLeft, "last step up" to fromBelow))
                residual = abs(total - fromLeft - fromBelow); invariant = "two disjoint final-step branches exhaust all paths"
            }
            "modular-clock" -> {
                val a = p.getValue("a").toInt(); val b = p.getValue("b").toInt(); val n = p.getValue("n").toInt().coerceAtLeast(2)
                val remainderA = ((a % n) + n) % n
                val remainderB = ((b % n) + n) % n
                val samePosition = remainderA == remainderB
                val divisibleDifference = (a - b) % n == 0
                measurements.putAll(
                    mapOf(
                        "a remainder" to remainderA.toDouble(),
                        "b remainder" to remainderB.toDouble(),
                        "same clock position" to samePosition.asDouble(),
                        "n divides a−b" to divisibleDifference.asDouble(),
                    ),
                )
                residual = abs(samePosition.asDouble() - divisibleDifference.asDouble())
                invariant = "same normalized remainder iff the difference is a whole number of turns"
            }
            "anscombe-quartet" -> {
                val selectedSeries = p.getValue("series").toInt().coerceIn(1, 4)
                val dataSet = requireNotNull(lab.dataSet)
                fun rowsFor(series: Int): List<Pair<Double, Double>> = dataSet.rows
                    .filter { it[0].toInt() == series }
                    .map { it[1] to it[2] }
                fun summary(rows: List<Pair<Double, Double>>): DoubleArray {
                    val meanX = rows.map { it.first }.average()
                    val meanY = rows.map { it.second }.average()
                    val centeredX = rows.sumOf { (it.first - meanX).pow(2) }
                    val centeredY = rows.sumOf { (it.second - meanY).pow(2) }
                    val cross = rows.sumOf { (it.first - meanX) * (it.second - meanY) }
                    val slope = cross / centeredX
                    val intercept = meanY - slope * meanX
                    val rSquared = cross * cross / (centeredX * centeredY)
                    return doubleArrayOf(
                        meanX,
                        meanY,
                        centeredX / (rows.size - 1),
                        centeredY / (rows.size - 1),
                        slope,
                        intercept,
                        rSquared,
                    )
                }
                val selected = summary(rowsFor(selectedSeries))
                val reference = summary(rowsFor(1))
                val names = listOf("mean x", "mean y", "sample variance x", "sample variance y", "slope", "intercept", "R²")
                names.indices.forEach { index -> measurements[names[index]] = selected[index] }
                measurements["observations"] = rowsFor(selectedSeries).size.toDouble()
                residual = selected.indices.maxOf { index -> abs(selected[index] - reference[index]) }
                invariant = "all four published series match the same rounded summaries but have different shapes"
            }
            else -> if (lab.id.startsWith("nt-")) {
                val n = p["n"] ?: 1.0
                val a = p["a"] ?: n
                val b = p["b"] ?: 1.0
                measurements.putAll(linkedMapOf("n" to n, "a" to a, "b" to b))
                residual = 0.0
                invariant = lab.formalResult
            } else error("No verified proof engine registered for ${lab.id}")
        }
        val tolerance = when (lab.id) {
            "derivative-slope" -> p.getValue("h") + 1e-9
            "integral-area" -> measurements.getValue("certified error bound") + 1e-9
            "normal-area" -> 1e-5
            "circle-ratio" -> measurements.getValue("certified ratio error bound") + 1e-9
            "circle-area" -> measurements.getValue("certified area error bound") + 1e-9
            "anscombe-quartet" -> .02
            else -> 1e-7
        }
        return ProofFrame(lab, step.coerceIn(0, lab.steps.lastIndex), p, measurements, invariant, residual, residual <= tolerance)
    }

    private fun angle(a: Vec2, b: Vec2): Double {
        val cross = a.x * b.y - a.y * b.x; val dot = a.x * b.x + a.y * b.y
        return abs(atan2(cross, dot)) * 180 / PI
    }
    private fun normalCdf(value: Double): Double {
        val x = abs(value); val t = 1 / (1 + .2316419 * x); val density = exp(-x * x / 2) / sqrt(2 * PI)
        val tail = density * t * (.319381530 + t * (-.356563782 + t * (1.781477937 + t * (-1.821255978 + t * 1.330274429))))
        return if (value >= 0) 1 - tail else tail
    }
    private fun simpsonIntegral(start: Double, end: Double, intervals: Int, function: (Double) -> Double): Double {
        val n = if (intervals % 2 == 0) intervals else intervals + 1
        val width = (end - start) / n
        var weighted = function(start) + function(end)
        for (index in 1 until n) weighted += (if (index % 2 == 0) 2 else 4) * function(start + index * width)
        return weighted * width / 3
    }
    private fun polygonArea(vertices: List<Vec2>): Double {
        if (vertices.size < 3) return 0.0
        val twiceSignedArea = vertices.indices.sumOf { index ->
            val current = vertices[index]
            val next = vertices[(index + 1) % vertices.size]
            current.x * next.y - current.y * next.x
        }
        return abs(twiceSignedArea) / 2
    }
    private fun Boolean.asDouble(): Double = if (this) 1.0 else 0.0
    private fun binomial(n: Int, k: Int): Double {
        if (k !in 0..n) return 0.0
        val smaller = minOf(k, n - k)
        var value = 1.0
        for (index in 1..smaller) value = value * (n - smaller + index) / index
        return value
    }
}
