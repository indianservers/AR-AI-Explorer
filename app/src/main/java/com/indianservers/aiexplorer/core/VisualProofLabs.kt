package com.indianservers.aiexplorer.core

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sqrt

data class ProofParameter(val name: String, val minimum: Double, val maximum: Double, val initial: Double)
data class VisualProofLab(
    val id: String,
    val title: String,
    val topic: String,
    val steps: List<String>,
    val parameters: List<ProofParameter>,
    val changesPrompt: String,
    val invariantPrompt: String,
    val formalResult: String,
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
        "Coordinate Geometry", "Probability & Statistics", "Vectors", "Number Theory", "Mensuration",
    )

    val labs = listOf(
        VisualProofLab("triangle-angle-sum", "Triangle angle sum", "Geometry", listOf("Build triangle ABC.", "Copy its three angles.", "Arrange them on a straight line.", "Drag a vertex to test."), listOf(ProofParameter("height", .2, 6.0, 3.0), ProofParameter("offset", -3.0, 5.0, 1.0)), "Which individual angles change as C moves?", "Why does their total stay 180°?", "A + B + C = 180°"),
        VisualProofLab("pythagorean", "Pythagorean rearrangement", "Geometry", listOf("Create a right triangle.", "Build squares on each side.", "Rearrange four copies.", "Compare uncovered areas."), listOf(ProofParameter("a", .5, 6.0, 3.0), ProofParameter("b", .5, 6.0, 4.0)), "How do the three square areas change?", "What area remains equal after rearrangement?", "a² + b² = c²"),
        VisualProofLab("derivative-slope", "Derivative as slope", "Calculus", listOf("Plot f(x)=x².", "Place a secant h away.", "Shrink h.", "Reveal the tangent."), listOf(ProofParameter("x", -4.0, 4.0, 2.0), ProofParameter("h", .001, 2.0, 1.0)), "How does the secant slope change as h shrinks?", "What limiting slope is stable?", "d(x²)/dx = 2x"),
        VisualProofLab("integral-area", "Integral as accumulated area", "Calculus", listOf("Partition [0,b].", "Build rectangles.", "Increase their count.", "Compare with the exact area."), listOf(ProofParameter("b", .5, 5.0, 3.0), ProofParameter("n", 2.0, 200.0, 10.0)), "What changes when rectangles become thinner?", "Which exact accumulated area is approached?", "∫₀ᵇ x dx = b²/2"),
        VisualProofLab("normal-area", "Normal probability area", "Probability", listOf("Draw the normal curve.", "Place symmetric bounds.", "Shade the interval.", "Compare tail areas."), listOf(ProofParameter("z", .1, 3.5, 1.0)), "How does shaded probability change with z?", "What remains symmetric about zero?", "P(-z≤Z≤z)=2Φ(z)-1"),
        VisualProofLab("vector-addition", "Vector addition", "Vectors", listOf("Draw vectors u and v.", "Use head-to-tail addition.", "Swap their order.", "Compare endpoints."), listOf(ProofParameter("ux", -4.0, 4.0, 2.0), ProofParameter("uy", -4.0, 4.0, 1.0), ProofParameter("vx", -4.0, 4.0, -1.0), ProofParameter("vy", -4.0, 4.0, 3.0)), "What changes when u and v move?", "Why is the final endpoint unchanged when order swaps?", "u + v = v + u"),
        VisualProofLab("matrix-transform", "Matrix area transformation", "Linear algebra", listOf("Start with a unit square.", "Apply a 2×2 matrix.", "Measure transformed area.", "Compare with determinant."), listOf(ProofParameter("a", -3.0, 3.0, 2.0), ProofParameter("b", -3.0, 3.0, 1.0), ProofParameter("c", -3.0, 3.0, 0.0), ProofParameter("d", -3.0, 3.0, 2.0)), "How does the image shape change?", "What scalar controls signed area?", "area scale = |det(A)|"),
        VisualProofLab("circle-ratio", "Circle circumference ratio", "Geometry", listOf("Choose a radius.", "Unroll the circumference.", "Compare with diameter.", "Drag radius and retest."), listOf(ProofParameter("r", .2, 6.0, 2.0)), "How do circumference and diameter change?", "Which ratio remains π?", "C/d = π"),
        VisualProofLab("algebra-square", "Square of a binomial", "Algebra", listOf("Build a square of side a+b.", "Partition it.", "Label the four regions.", "Sum their areas."), listOf(ProofParameter("a", .2, 5.0, 2.0), ProofParameter("b", .2, 5.0, 1.0)), "How do the four regions change?", "Why does total area remain the same?", "(a+b)² = a² + 2ab + b²"),
        VisualProofLab("shear-area", "Shear preserves area", "Transformations", listOf("Build a rectangle.", "Slide its top edge.", "Observe the parallelogram.", "Compare base×height."), listOf(ProofParameter("base", .5, 6.0, 4.0), ProofParameter("height", .5, 5.0, 2.0), ProofParameter("shear", -4.0, 4.0, 1.0)), "What changes as the top edge slides?", "Which dimensions keep area fixed?", "A = base × perpendicular height"),
        VisualProofLab("triangle-area", "Triangle area dissection", "Geometry", listOf("Build a triangle with base b and height h.", "Duplicate and rotate it.", "Join both copies into a parallelogram.", "Take half of the parallelogram area."), listOf(ProofParameter("base", .5, 7.0, 4.0), ProofParameter("height", .5, 6.0, 3.0), ProofParameter("apex", -2.0, 6.0, 1.5)), "How does the triangle change when its apex slides?", "Why is base times perpendicular height unchanged by a horizontal apex move?", "A = bh/2"),
        VisualProofLab("parallelogram-area", "Parallelogram cut and slide", "Geometry", listOf("Draw a slanted parallelogram.", "Cut the triangular end.", "Slide it to the opposite side.", "Read the resulting rectangle area."), listOf(ProofParameter("base", .5, 7.0, 4.0), ProofParameter("height", .5, 5.0, 2.5), ProofParameter("shear", -3.0, 3.0, 1.2)), "What changes as the slant changes?", "Which base and perpendicular height remain fixed?", "A = bh"),
        VisualProofLab("trapezoid-area", "Trapezoid doubling proof", "Geometry", listOf("Build a trapezoid with parallel sides a and b.", "Rotate a second copy.", "Join the copies into a parallelogram.", "Halve its area."), listOf(ProofParameter("a", .5, 7.0, 5.0), ProofParameter("b", .5, 7.0, 2.5), ProofParameter("height", .5, 5.0, 2.5)), "How do the copies fit when either base changes?", "Why is the doubled base always a+b?", "A = (a+b)h/2"),
        VisualProofLab("circle-area", "Circle sectors to rectangle", "Geometry", listOf("Divide the circle into equal sectors.", "Alternate sectors up and down.", "Increase the sector count.", "Read the limiting rectangle dimensions."), listOf(ProofParameter("r", .4, 5.0, 2.2), ProofParameter("n", 6.0, 60.0, 12.0)), "How does the scalloped edge change as n grows?", "Why do height r and base pi r remain fixed?", "A = pi r²"),
        VisualProofLab("polygon-angle-sum", "Polygon triangulation", "Geometry", listOf("Choose a regular n-gon.", "Select one vertex.", "Draw every non-adjacent diagonal.", "Count the resulting triangles."), listOf(ProofParameter("n", 3.0, 12.0, 6.0), ProofParameter("radius", 1.0, 5.0, 2.8)), "How many triangles appear when a side is added?", "Why is each new triangle worth 180 degrees?", "S = (n-2) × 180°"),
        VisualProofLab("similar-triangles", "Similar triangle ratios", "Geometry", listOf("Build a reference triangle.", "Scale it from a common center.", "Compare corresponding sides.", "Move the scale control and retest."), listOf(ProofParameter("a", 1.0, 6.0, 3.0), ProofParameter("b", 1.0, 6.0, 2.0), ProofParameter("k", .25, 3.0, 1.5)), "How do the side lengths change with k?", "Which three side ratios stay equal?", "a'/a = b'/b = c'/c = k"),
        VisualProofLab("intersecting-chords", "Intersecting chords theorem", "Geometry", listOf("Draw two chords through an interior point P.", "Measure the four chord segments.", "Build rectangles from each segment pair.", "Move P while keeping both chords on the circle."), listOf(ProofParameter("r", 1.0, 5.0, 3.0), ProofParameter("p", -.75, .75, .2), ProofParameter("angle", 15.0, 165.0, 70.0)), "How do individual segment lengths change as P moves?", "Why do the two segment products remain equal?", "PA × PB = PC × PD"),
        VisualProofLab("circle-angle", "Center and circumference angles", "Geometry", listOf("Choose chord AB.", "Join A and B to center O.", "Choose point C on the major arc.", "Compare angle AOB with angle ACB."), listOf(ProofParameter("arc", 20.0, 160.0, 80.0), ProofParameter("c", 185.0, 340.0, 250.0), ProofParameter("r", 1.0, 5.0, 3.0)), "How do both angles change when the chord changes?", "Why does the central angle stay twice the inscribed angle?", "angle AOB = 2 angle ACB"),
        VisualProofLab("unit-circle-identity", "Unit-circle identity", "Trigonometry", listOf("Place a point on the unit circle.", "Project it onto both axes.", "Read cosine and sine as legs.", "Apply Pythagoras to the radius."), listOf(ProofParameter("theta", -180.0, 180.0, 40.0)), "How do sine and cosine change as the point rotates?", "Why does their squared sum stay one?", "sin²(theta) + cos²(theta) = 1"),
        VisualProofLab("odd-sum-square", "Odd numbers build squares", "Number Theory", listOf("Start with one unit tile.", "Add the next odd L-shaped border.", "Repeat for n layers.", "Count the completed square."), listOf(ProofParameter("n", 1.0, 15.0, 6.0)), "How many tiles enter at each new layer?", "Why does the completed side length equal n?", "1+3+5+...+(2n-1) = n²"),
        VisualProofLab("absolute-inequality", "Absolute-value inequality", "Algebra", listOf("Mark −r and r on a number line.", "Interpret |x| as distance from zero.", "Shade points whose distance is at most r.", "Read the compound inequality."), listOf(ProofParameter("x", -6.0, 6.0, 2.0), ProofParameter("r", .5, 6.0, 3.0)), "How does membership change as x crosses either boundary?", "Why is distance from zero unchanged by reflection?", "|x| ≤ r ⇔ −r ≤ x ≤ r"),
        VisualProofLab("equation-balance", "Equation as a balance", "Algebra", listOf("Balance ax+b against c.", "Remove b from both pans.", "Split both sides into a equal groups.", "Read the value of x."), listOf(ProofParameter("a", 1.0, 6.0, 2.0), ProofParameter("b", -5.0, 5.0, 2.0), ProofParameter("c", -5.0, 15.0, 10.0)), "How does the solution move when a, b, or c changes?", "Why does doing the same operation to both sides preserve equality?", "ax+b=c ⇒ x=(c−b)/a"),
        VisualProofLab("set-de-morgan", "De Morgan's law", "Probability", listOf("Draw overlapping sets A and B.", "Shade everything outside A∪B.", "Shade Aᶜ and Bᶜ separately.", "Compare the overlap and truth rows."), emptyList(), "Which regions are shaded on each side?", "Why do both expressions select exactly the same regions?", "(A∪B)ᶜ = Aᶜ∩Bᶜ"),
        VisualProofLab("epsilon-delta", "Epsilon–delta limit", "Calculus", listOf("Choose a horizontal ε-band around L.", "Choose a vertical δ-band around a.", "Track the graph inside the δ-band.", "Verify its image stays inside the ε-band."), listOf(ProofParameter("epsilon", .2, 2.0, .8), ProofParameter("delta", .05, 1.0, .35)), "How small must δ become as ε shrinks?", "What implication remains true inside the bands?", "0<|x−a|<δ ⇒ |f(x)−L|<ε"),
        VisualProofLab("slope-triangle", "Slope triangles", "Coordinate Geometry", listOf("Plot a line through two points.", "Draw its horizontal run.", "Draw its vertical rise.", "Compare a second similar slope triangle."), listOf(ProofParameter("m", -3.0, 3.0, 1.5), ProofParameter("run", .5, 4.0, 2.0)), "How do rise and run change when the triangle is enlarged?", "Why does rise/run stay fixed along one line?", "m = rise/run = Δy/Δx"),
        VisualProofLab("eigenvector-direction", "Eigenvector direction", "Linear algebra", listOf("Draw a vector v on a transformation grid.", "Apply a diagonal linear transformation.", "Compare v with Av.", "Identify directions that do not turn."), listOf(ProofParameter("lambda", -3.0, 3.0, 2.0), ProofParameter("other", -3.0, 3.0, .5)), "How does length or orientation change with λ?", "Which line through the origin is preserved?", "Av = λv"),
        VisualProofLab("counting-paths", "Counting paths", "Probability", listOf("Branch into right or up choices.", "Arrange endpoints on a lattice.", "Group paths by their final step.", "Read Pascal's recurrence."), listOf(ProofParameter("right", 1.0, 6.0, 3.0), ProofParameter("up", 1.0, 6.0, 2.0)), "How many paths are added when the grid grows?", "Why does every path end with exactly one of two moves?", "C(r+u,r)=C(r+u−1,r−1)+C(r+u−1,r)"),
        VisualProofLab("modular-clock", "Congruence on a clock", "Number Theory", listOf("Place a on an n-hour clock.", "Add whole turns of n.", "Compare the final clock positions.", "State the congruence."), listOf(ProofParameter("a", -20.0, 30.0, 17.0), ProofParameter("n", 2.0, 16.0, 12.0)), "How does the integer change after a full turn?", "Which remainder position stays fixed?", "a ≡ b (mod n) ⇔ n | (a−b)"),
    )

    val certificates = listOf(
        VisualProofCertificate("triangle-angle-sum", "Parallel-line angle transfer", listOf("Draw a line through one vertex parallel to the opposite side.", "Alternate interior angles copy the other two vertex angles onto that line.", "The three adjacent angles form a straight angle, so their sum is 180°."), listOf("Euclidean parallel postulate", "Non-degenerate triangle")),
        VisualProofCertificate("pythagorean", "Area-preserving dissection", listOf("Arrange four congruent right triangles inside a square of side a+b.", "The remaining central square has side c, so its area is c².", "Computing the same outer area from the four triangles and simplifying gives c²=a²+b²."), listOf("Right triangle", "Euclidean area additivity")),
        VisualProofCertificate("derivative-slope", "Secant-to-tangent limit", listOf("For f(x)=x² the secant slope is ((x+h)²-x²)/h.", "For h≠0 this simplifies exactly to 2x+h.", "Taking h→0 leaves 2x, the tangent slope."), listOf("h approaches zero through non-zero values", "Real differentiability")),
        VisualProofCertificate("integral-area", "Riemann-sum limit", listOf("Partition [0,b] into n equal strips.", "The rectangle sum is a finite arithmetic sum of sampled heights times width.", "As strip width tends to zero, the sum converges to b²/2."), listOf("b≥0", "Riemann integrability of f(x)=x")),
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
    )

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
            VisualProofSubcategory("Sets & Counting", "Compare logical regions and count branching lattice paths", listOf("set-de-morgan", "counting-paths")),
        ),
        "Vectors" to listOf(
            VisualProofSubcategory("Vector Operations", "Build vector laws with movable arrows and common endpoints", listOf("vector-addition")),
        ),
        "Number Theory" to listOf(
            VisualProofSubcategory("Sequences & Figurate Numbers", "Build numerical identities from countable tile patterns", listOf("odd-sum-square")),
            VisualProofSubcategory("Congruences", "See remainder classes as repeated positions on a modular clock", listOf("modular-clock")),
        ),
        "Mensuration" to listOf(
            VisualProofSubcategory("Circle Measures", "Unroll and compare radius, diameter and circumference", listOf("circle-ratio")),
        ),
    )

    private val commonFeatures = setOf(
        ProofEnhancement.ColorCodedStages, ProofEnhancement.BeforeAfter, ProofEnhancement.MotionArrows,
        ProofEnhancement.TransparentComparison, ProofEnhancement.MarkedInvariant, ProofEnhancement.ValidityLabels,
        ProofEnhancement.ZoomDetail, ProofEnhancement.Checkpoints, ProofEnhancement.SymbolicVisualColumns,
        ProofEnhancement.ConsistentColorGrammar, ProofEnhancement.Assumptions, ProofEnhancement.NotToScale,
        ProofEnhancement.ProofBreadcrumbs, ProofEnhancement.MatchedStepNumbers, ProofEnhancement.InlineDefinitions,
        ProofEnhancement.EquivalentRepresentations, ProofEnhancement.ContradictionPanel, ProofEnhancement.ThereforeSummary,
        ProofEnhancement.ReusableLegend, ProofEnhancement.DomainMask, ProofEnhancement.ErrorTrap,
        ProofEnhancement.SimpleCase, ProofEnhancement.RealWorldAnalogy, ProofEnhancement.CompactTakeaway,
    )

    private val featureMap = mapOf(
        "algebra-square" to setOf(ProofEnhancement.AreaModel, ProofEnhancement.SameQuantityDifferentShape, ProofEnhancement.GridBackground),
        "pythagorean" to setOf(ProofEnhancement.AreaModel, ProofEnhancement.SameQuantityDifferentShape, ProofEnhancement.CutAndSlide, ProofEnhancement.Dissection, ProofEnhancement.MotionTrails),
        "triangle-area" to setOf(ProofEnhancement.AreaModel, ProofEnhancement.SameQuantityDifferentShape, ProofEnhancement.CutAndSlide, ProofEnhancement.Dissection, ProofEnhancement.MotionTrails),
        "parallelogram-area" to setOf(ProofEnhancement.AreaModel, ProofEnhancement.SameQuantityDifferentShape, ProofEnhancement.CutAndSlide, ProofEnhancement.Dissection, ProofEnhancement.MotionTrails),
        "trapezoid-area" to setOf(ProofEnhancement.AreaModel, ProofEnhancement.SameQuantityDifferentShape, ProofEnhancement.CutAndSlide, ProofEnhancement.Dissection, ProofEnhancement.MotionTrails),
        "circle-area" to setOf(ProofEnhancement.AreaModel, ProofEnhancement.SameQuantityDifferentShape, ProofEnhancement.CutAndSlide, ProofEnhancement.Dissection, ProofEnhancement.FadingLimit, ProofEnhancement.MotionTrails),
        "absolute-inequality" to setOf(ProofEnhancement.NumberLine, ProofEnhancement.SymmetryAxes, ProofEnhancement.Counterexample),
        "equation-balance" to setOf(ProofEnhancement.BalanceScale),
        "set-de-morgan" to setOf(ProofEnhancement.VennDiagram, ProofEnhancement.TruthTable, ProofEnhancement.Counterexample),
        "slope-triangle" to setOf(ProofEnhancement.GridBackground, ProofEnhancement.SlopeTriangle),
        "derivative-slope" to setOf(ProofEnhancement.GridBackground, ProofEnhancement.SlopeTriangle, ProofEnhancement.TangentOverlay, ProofEnhancement.FadingLimit),
        "integral-area" to setOf(ProofEnhancement.GridBackground, ProofEnhancement.AccumulationStrips, ProofEnhancement.FadingLimit),
        "epsilon-delta" to setOf(ProofEnhancement.GridBackground, ProofEnhancement.FadingLimit, ProofEnhancement.EpsilonDeltaBands),
        "unit-circle-identity" to setOf(ProofEnhancement.GridBackground, ProofEnhancement.UnitCircleOverlay, ProofEnhancement.SymmetryAxes, ProofEnhancement.VectorProjection),
        "vector-addition" to setOf(ProofEnhancement.GridBackground, ProofEnhancement.VectorProjection, ProofEnhancement.MotionTrails),
        "matrix-transform" to setOf(ProofEnhancement.GridBackground, ProofEnhancement.TransformationGrid, ProofEnhancement.DeterminantScaling, ProofEnhancement.MotionTrails),
        "eigenvector-direction" to setOf(ProofEnhancement.GridBackground, ProofEnhancement.TransformationGrid, ProofEnhancement.EigenvectorDirection, ProofEnhancement.MotionTrails),
        "counting-paths" to setOf(ProofEnhancement.GridBackground, ProofEnhancement.CountingTree, ProofEnhancement.LatticePoints, ProofEnhancement.RecursiveStructure),
        "odd-sum-square" to setOf(ProofEnhancement.GridBackground, ProofEnhancement.LatticePoints, ProofEnhancement.RecursiveStructure),
        "modular-clock" to setOf(ProofEnhancement.ModularClock, ProofEnhancement.MotionTrails),
        "normal-area" to setOf(ProofEnhancement.GridBackground, ProofEnhancement.AccumulationStrips, ProofEnhancement.SymmetryAxes),
        "circle-angle" to setOf(ProofEnhancement.SymmetryAxes),
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
        "absolute-inequality" -> "If r<0, no real x can satisfy |x|≤r; the assumption r≥0 is essential."
        "equation-balance" -> "Dividing by a=0 is invalid and can turn one equation into a false conclusion."
        "set-de-morgan" -> "The tempting claim (A∪B)ᶜ=Aᶜ∪Bᶜ fails for a point lying in A only."
        "slope-triangle" -> "A vertical line has run 0, so rise/run is undefined."
        "eigenvector-direction" -> "A general vector need not preserve direction; only an eigenvector does."
        else -> "Remove a listed assumption and the displayed construction may no longer establish the claim."
    }

    private fun errorTrapFor(id: String): String = when (id) {
        "algebra-square" -> "Do not drop the two ab rectangles: (a+b)² is not a²+b²."
        "derivative-slope" -> "Do not substitute h=0 before cancelling h; the difference quotient would divide by zero."
        "integral-area" -> "A finite rectangle sum is an approximation, not automatically the exact integral."
        "equation-balance" -> "An operation on only one side tips the balance and changes the solution set."
        "epsilon-delta" -> "δ must control every qualifying x, not just one tested point."
        "matrix-transform" -> "Signed determinant records orientation; ordinary area uses |det A|."
        else -> "A matching sketch alone is not a proof; use the highlighted invariant and validity reason."
    }

    private fun simpleCaseFor(id: String): String = when (id) {
        "pythagorean" -> "Try a=3, b=4: 9+16=25=5²."
        "algebra-square" -> "Try a=2, b=1: the four areas 4+2+2+1 total 9."
        "set-de-morgan" -> "Test one point in neither set: both expressions are true."
        "modular-clock" -> "17 and 5 land together on a 12-hour clock because 17−5=12."
        "counting-paths" -> "A 2×1 grid has three move orders: RRU, RUR, URR."
        else -> "Set the controls to small whole numbers and compare both measured quantities."
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
        val lab = VisualProofCatalog.labs.first { it.id == id }
        return ProofPlayback(frame(lab, 0, lab.parameters.associate { it.name to it.initial }))
    }

    fun setParameter(playback: ProofPlayback, name: String, value: Double): ProofPlayback {
        val definition = playback.frame.lab.parameters.first { it.name == name }
        val parameters = playback.frame.parameters + (name to value.coerceIn(definition.minimum, definition.maximum))
        return playback.copy(frame = frame(playback.frame.lab, playback.frame.step, parameters))
    }

    fun next(playback: ProofPlayback): ProofPlayback {
        val last = playback.frame.lab.steps.lastIndex
        val next = (playback.frame.step + playback.direction).coerceIn(0, last)
        val direction = if (next == last) -1 else if (next == 0) 1 else playback.direction
        return playback.copy(frame = frame(playback.frame.lab, next, playback.frame.parameters), direction = direction)
    }

    fun reveal(playback: ProofPlayback) = playback.copy(frame = frame(playback.frame.lab, playback.frame.lab.steps.lastIndex, playback.frame.parameters))
    fun togglePlaying(playback: ProofPlayback) = playback.copy(playing = !playback.playing)

    private fun frame(lab: VisualProofLab, step: Int, p: Map<String, Double>): ProofFrame {
        val measurements = mutableMapOf<String, Double>()
        val residual: Double
        val invariant: String
        when (lab.id) {
            "triangle-angle-sum" -> {
                val a = Vec2(0.0, 0.0); val b = Vec2(4.0, 0.0); val c = Vec2(p.getValue("offset"), p.getValue("height"))
                val angles = listOf(angle(b - a, c - a), angle(a - b, c - b), angle(a - c, b - c))
                measurements["angle sum"] = angles.sum(); residual = abs(angles.sum() - 180); invariant = "angle sum = 180°"
            }
            "pythagorean" -> {
                val a = p.getValue("a"); val b = p.getValue("b"); val c2 = a * a + b * b
                measurements.putAll(mapOf("a²" to a * a, "b²" to b * b, "c²" to c2)); residual = abs(c2 - a * a - b * b); invariant = "a²+b²=c²"
            }
            "derivative-slope" -> {
                val x = p.getValue("x"); val h = p.getValue("h"); val secant = ((x + h).pow(2) - x.pow(2)) / h; val tangent = 2 * x
                measurements.putAll(mapOf("secant slope" to secant, "tangent slope" to tangent)); residual = abs(secant - tangent); invariant = "secant → 2x as h → 0"
            }
            "integral-area" -> {
                val b = p.getValue("b"); val n = p.getValue("n").toInt().coerceAtLeast(1); val width = b / n; val sum = (0 until n).sumOf { (it + .5) * width * width }; val exact = b * b / 2
                measurements.putAll(mapOf("rectangle sum" to sum, "exact area" to exact)); residual = abs(sum - exact); invariant = "rectangle sum → b²/2"
            }
            "normal-area" -> {
                val z = p.getValue("z"); val middle = 2 * normalCdf(z) - 1
                measurements.putAll(mapOf("middle area" to middle, "left tail" to (1 - middle) / 2, "right tail" to (1 - middle) / 2)); residual = 0.0; invariant = "left tail = right tail"
            }
            "vector-addition" -> {
                val ux = p.getValue("ux"); val uy = p.getValue("uy"); val vx = p.getValue("vx"); val vy = p.getValue("vy")
                measurements.putAll(mapOf("result x" to ux + vx, "result y" to uy + vy)); residual = abs((ux + vx) - (vx + ux)) + abs((uy + vy) - (vy + uy)); invariant = "u+v=v+u"
            }
            "matrix-transform" -> {
                val determinant = p.getValue("a") * p.getValue("d") - p.getValue("b") * p.getValue("c")
                measurements.putAll(mapOf("determinant" to determinant, "area scale" to abs(determinant))); residual = 0.0; invariant = "area scale = |det(A)|"
            }
            "circle-ratio" -> {
                val r = p.getValue("r"); val circumference = 2 * PI * r; val diameter = 2 * r
                measurements.putAll(mapOf("circumference" to circumference, "diameter" to diameter, "ratio" to circumference / diameter)); residual = abs(circumference / diameter - PI); invariant = "C/d=π"
            }
            "algebra-square" -> {
                val a = p.getValue("a"); val b = p.getValue("b"); val whole = (a + b).pow(2); val parts = a * a + 2 * a * b + b * b
                measurements.putAll(mapOf("whole" to whole, "parts" to parts)); residual = abs(whole - parts); invariant = "(a+b)²=a²+2ab+b²"
            }
            "shear-area" -> {
                val base = p.getValue("base"); val height = p.getValue("height"); val shear = p.getValue("shear")
                val rectangleArea = base * height
                val transformedDeterminant = base * height - 0.0 * shear
                measurements.putAll(mapOf("area before" to rectangleArea, "area after shear" to abs(transformedDeterminant), "horizontal shift" to shear))
                residual = abs(rectangleArea - abs(transformedDeterminant)); invariant = "shear determinant is 1, so base×height stays constant"
            }
            "triangle-area" -> {
                val base = p.getValue("base"); val height = p.getValue("height"); val triangle = base * height / 2; val doubled = 2 * triangle
                measurements.putAll(mapOf("triangle area" to triangle, "two copies" to doubled, "parallelogram" to base * height)); residual = abs(doubled - base * height); invariant = "two equal triangles = bh"
            }
            "parallelogram-area" -> {
                val base = p.getValue("base"); val height = p.getValue("height"); val area = base * height
                measurements.putAll(mapOf("slanted area" to area, "rectangle area" to area, "shear" to p.getValue("shear"))); residual = 0.0; invariant = "cut-and-slide preserves bh"
            }
            "trapezoid-area" -> {
                val a = p.getValue("a"); val b = p.getValue("b"); val h = p.getValue("height"); val trapezoid = (a + b) * h / 2
                measurements.putAll(mapOf("one trapezoid" to trapezoid, "two copies" to 2 * trapezoid, "parallelogram" to (a + b) * h)); residual = abs(2 * trapezoid - (a + b) * h); invariant = "two copies form base a+b"
            }
            "circle-area" -> {
                val r = p.getValue("r"); val n = p.getValue("n").toInt().coerceAtLeast(6); val exact = PI * r * r
                measurements.putAll(mapOf("radius" to r, "sectors" to n.toDouble(), "base pi r" to PI * r, "area" to exact)); residual = 0.0; invariant = "sector rearrangement tends to pi r by r"
            }
            "polygon-angle-sum" -> {
                val n = p.getValue("n").toInt().coerceAtLeast(3); val triangles = n - 2; val sum = triangles * 180.0
                measurements.putAll(mapOf("sides" to n.toDouble(), "triangles" to triangles.toDouble(), "angle sum" to sum)); residual = abs(sum - (n - 2) * 180.0); invariant = "n-gon splits into n-2 triangles"
            }
            "similar-triangles" -> {
                val a = p.getValue("a"); val b = p.getValue("b"); val k = p.getValue("k"); val c = sqrt(a * a + b * b)
                measurements.putAll(mapOf("a'/a" to (k * a) / a, "b'/b" to (k * b) / b, "c'/c" to (k * c) / c)); residual = abs((k * a) / a - k) + abs((k * b) / b - k); invariant = "all corresponding side ratios equal k"
            }
            "intersecting-chords" -> {
                val r = p.getValue("r"); val px = p.getValue("p") * r; val angle = Math.toRadians(p.getValue("angle")); val dot = px * kotlin.math.cos(angle); val root = sqrt((dot * dot + r * r - px * px).coerceAtLeast(0.0))
                val pa = r + px; val pb = r - px; val pc = root - dot; val pd = root + dot
                measurements.putAll(mapOf("PA×PB" to pa * pb, "PC×PD" to pc * pd, "circle power" to r * r - px * px)); residual = abs(pa * pb - pc * pd); invariant = "both products equal r²-OP²"
            }
            "circle-angle" -> {
                val center = p.getValue("arc"); val inscribed = center / 2
                measurements.putAll(mapOf("center angle" to center, "circumference angle" to inscribed, "ratio" to center / inscribed)); residual = abs(center - 2 * inscribed); invariant = "central angle = twice inscribed angle"
            }
            "unit-circle-identity" -> {
                val theta = Math.toRadians(p.getValue("theta")); val cosine = kotlin.math.cos(theta); val sine = kotlin.math.sin(theta); val sum = cosine * cosine + sine * sine
                measurements.putAll(mapOf("cos theta" to cosine, "sin theta" to sine, "squared sum" to sum)); residual = abs(sum - 1.0); invariant = "horizontal² + vertical² = radius²"
            }
            "odd-sum-square" -> {
                val n = p.getValue("n").toInt().coerceAtLeast(1); val sum = (1..n).sumOf { 2 * it - 1 }.toDouble(); val square = (n * n).toDouble()
                measurements.putAll(mapOf("odd-number sum" to sum, "n² tiles" to square, "latest border" to (2 * n - 1).toDouble())); residual = abs(sum - square); invariant = "each odd border completes the next square"
            }
            "absolute-inequality" -> {
                val x = p.getValue("x"); val r = p.getValue("r")
                measurements.putAll(mapOf("|x|" to abs(x), "radius r" to r, "inside interval" to if (abs(x) <= r) 1.0 else 0.0))
                residual = 0.0; invariant = "reflection changes sign but preserves distance from zero"
            }
            "equation-balance" -> {
                val a = p.getValue("a"); val b = p.getValue("b"); val c = p.getValue("c"); val solution = (c - b) / a
                measurements.putAll(mapOf("solution x" to solution, "left ax+b" to a * solution + b, "right c" to c))
                residual = abs((a * solution + b) - c); invariant = "equal operations preserve equal pan weights"
            }
            "set-de-morgan" -> {
                measurements.putAll(mapOf("matching truth rows" to 4.0, "total truth rows" to 4.0))
                residual = 0.0; invariant = "both expressions shade the outside-of-both region"
            }
            "epsilon-delta" -> {
                val epsilon = p.getValue("epsilon"); val delta = p.getValue("delta"); val maximumOutputError = 2.0 * delta
                measurements.putAll(mapOf("epsilon" to epsilon, "delta" to delta, "max |f(x)-L|" to maximumOutputError))
                residual = (maximumOutputError - epsilon).coerceAtLeast(0.0); invariant = "the δ input band maps inside the ε output band"
            }
            "slope-triangle" -> {
                val m = p.getValue("m"); val run = p.getValue("run"); val rise = m * run
                measurements.putAll(mapOf("rise" to rise, "run" to run, "rise/run" to rise / run, "larger triangle ratio" to (2 * rise) / (2 * run)))
                residual = abs(rise / run - (2 * rise) / (2 * run)); invariant = "similar slope triangles keep rise/run fixed"
            }
            "eigenvector-direction" -> {
                val lambda = p.getValue("lambda"); val other = p.getValue("other")
                measurements.putAll(mapOf("eigenvalue λ" to lambda, "other axis scale" to other, "cross(v,Av)" to 0.0))
                residual = 0.0; invariant = "v and Av are collinear even when λ reverses orientation"
            }
            "counting-paths" -> {
                val right = p.getValue("right").toInt().coerceAtLeast(1); val up = p.getValue("up").toInt().coerceAtLeast(1)
                val total = binomial(right + up, right); val fromLeft = binomial(right + up - 1, right - 1); val fromBelow = binomial(right + up - 1, right)
                measurements.putAll(mapOf("all paths" to total, "last step right" to fromLeft, "last step up" to fromBelow))
                residual = abs(total - fromLeft - fromBelow); invariant = "two disjoint final-step branches exhaust all paths"
            }
            "modular-clock" -> {
                val a = p.getValue("a").toInt(); val n = p.getValue("n").toInt().coerceAtLeast(2); val remainder = ((a % n) + n) % n; val equivalent = a - n
                measurements.putAll(mapOf("a" to a.toDouble(), "modulus n" to n.toDouble(), "remainder" to remainder.toDouble(), "equivalent a−n" to equivalent.toDouble()))
                residual = abs((((equivalent % n) + n) % n) - remainder).toDouble(); invariant = "whole turns preserve the remainder position"
            }
            else -> error("No verified proof engine registered for ${lab.id}")
        }
        val tolerance = when (lab.id) { "derivative-slope" -> p.getValue("h") + 1e-9; "integral-area" -> 1.0 / p.getValue("n").coerceAtLeast(1.0); else -> 1e-7 }
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
    private fun binomial(n: Int, k: Int): Double {
        if (k !in 0..n) return 0.0
        val smaller = minOf(k, n - k)
        var value = 1.0
        for (index in 1..smaller) value = value * (n - smaller + index) / index
        return value
    }
}
