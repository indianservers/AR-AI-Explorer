package com.indianservers.aiexplorer.learning

enum class KnowledgeLevel(val label: String) { School("School"), UG("UG"), PG("PG") }
enum class KnowledgeTopic(val label: String) { Algebra("Algebra"), Calculus("Calculus"), Geometry("Geometry"), Statistics("Statistics"), Probability("Probability") }
enum class DictionaryClassBand(val label: String) { CLASS_6_8("Classes 6–8"), CLASS_9_10("Classes 9–10"), CLASS_11_12("Classes 11–12"), UNIVERSITY("University") }
enum class DictionaryDifficulty(val label: String) { FOUNDATION("Foundation"), STANDARD("Standard"), ADVANCED("Advanced") }
enum class QuizSubject(val label: String) { Maths("Maths"), Physics("Physics"), Chemistry("Chemistry"), Biology("Biology"), AstroPhysics("Astro Physics"), IQLabs("IQ Labs") }
enum class QuizLevel(val label: String, val difficulty: Int) { Basic("Basic", 1), Intermediate("Intermediate", 2), Advanced("Advanced", 3) }
enum class FormulaCategory(val label: String, val topic: KnowledgeTopic) {
    Algebra("Algebra", KnowledgeTopic.Algebra),
    Geometry("Geometry", KnowledgeTopic.Geometry),
    Trigonometry("Trigonometry", KnowledgeTopic.Geometry),
    Calculus("Calculus", KnowledgeTopic.Calculus),
    DifferentialEquations("Differential Equations", KnowledgeTopic.Calculus),
    LinearAlgebra("Linear Algebra", KnowledgeTopic.Algebra),
    CoordinateGeometry("Coordinate Geometry", KnowledgeTopic.Geometry),
    Vectors3D("Vectors & 3D", KnowledgeTopic.Geometry),
    Probability("Probability", KnowledgeTopic.Probability),
    Statistics("Statistics", KnowledgeTopic.Statistics),
    Distributions("Distributions", KnowledgeTopic.Statistics),
    NumberTheory("Number Theory", KnowledgeTopic.Algebra),
    Combinatorics("Combinatorics", KnowledgeTopic.Probability),
    ComplexNumbers("Complex Numbers", KnowledgeTopic.Algebra),
    NumericalMethods("Numerical Methods", KnowledgeTopic.Calculus),
}

data class FormulaCard(
    val id: String,
    val title: String,
    val topic: KnowledgeTopic,
    val category: FormulaCategory,
    val level: KnowledgeLevel,
    val expression: String,
    val variables: List<String>,
    val useCase: String,
    val relatedTerms: List<String> = emptyList(),
)

data class TheoremCard(
    val id: String,
    val title: String,
    val topic: KnowledgeTopic,
    val level: KnowledgeLevel,
    val statement: String,
    val conditions: List<String>,
    val applications: List<String>,
    val proofSketch: List<String>,
)

data class VisualProofCard(
    val id: String,
    val title: String,
    val topic: KnowledgeTopic,
    val level: KnowledgeLevel,
    val workspace: MathModuleTarget,
    val constructionSteps: List<String>,
    val invariant: String,
    val learnerPrompt: String,
)

data class DictionaryTerm(
    val term: String,
    val topic: KnowledgeTopic,
    val level: KnowledgeLevel,
    val definition: String,
    val notation: String,
    val example: String,
    val nonExample: String,
    val classBands: Set<DictionaryClassBand>,
    val difficulty: DictionaryDifficulty,
)

/** Lightweight dictionary data that can open without initializing the full formula and quiz catalogs. */
object MathDictionaryCatalog {
    val terms = listOf(
        DictionaryTerm("Discriminant", KnowledgeTopic.Algebra, KnowledgeLevel.School, "The quantity b^2 - 4ac that classifies roots of a quadratic.", "Delta", "For x^2 - 5x + 6, Delta = 1, so roots are real and distinct.", "b^2 - 4ac is not the quadratic formula itself.", setOf(DictionaryClassBand.CLASS_9_10, DictionaryClassBand.CLASS_11_12), DictionaryDifficulty.STANDARD),
        DictionaryTerm("Limit", KnowledgeTopic.Calculus, KnowledgeLevel.UG, "The value a function approaches as the input approaches a point.", "lim x->a f(x)", "lim x->0 sin(x)/x = 1.", "A limit is not always the function value f(a).", setOf(DictionaryClassBand.CLASS_11_12, DictionaryClassBand.UNIVERSITY), DictionaryDifficulty.ADVANCED),
        DictionaryTerm("Derivative", KnowledgeTopic.Calculus, KnowledgeLevel.School, "Instantaneous rate of change, represented by tangent slope.", "dy/dx or f'(x)", "If f(x)=x^2, f'(x)=2x.", "f(x)/x is not generally the derivative of f.", setOf(DictionaryClassBand.CLASS_11_12, DictionaryClassBand.UNIVERSITY), DictionaryDifficulty.ADVANCED),
        DictionaryTerm("Median", KnowledgeTopic.Statistics, KnowledgeLevel.School, "The middle value after ordering data.", "Q2", "For 2, 4, 9, the median is 4.", "The median is not found before arranging unordered data.", setOf(DictionaryClassBand.CLASS_6_8, DictionaryClassBand.CLASS_9_10), DictionaryDifficulty.FOUNDATION),
        DictionaryTerm("Posterior", KnowledgeTopic.Probability, KnowledgeLevel.UG, "A Bayesian probability updated after evidence is observed.", "P(A|B)", "Disease probability after a positive test.", "P(A|B) is not automatically equal to P(B|A).", setOf(DictionaryClassBand.CLASS_11_12, DictionaryClassBand.UNIVERSITY), DictionaryDifficulty.ADVANCED),
        DictionaryTerm("Eigenvector", KnowledgeTopic.Algebra, KnowledgeLevel.PG, "A nonzero vector whose direction is preserved by a linear transformation.", "Av=lambda v", "Principal component directions are eigenvectors of a covariance matrix.", "The zero vector is not an eigenvector.", setOf(DictionaryClassBand.UNIVERSITY), DictionaryDifficulty.ADVANCED),
    ).sortedBy { it.term }

    fun search(query: String, topic: KnowledgeTopic?, level: KnowledgeLevel?, initial: Char?, classBand: DictionaryClassBand? = null, difficulty: DictionaryDifficulty? = null): List<DictionaryTerm> {
        val normalized = query.trim().lowercase()
        return terms.filter { term ->
            (topic == null || term.topic == topic) &&
                (level == null || term.level == level) &&
                (initial == null || term.term.firstOrNull()?.uppercaseChar() == initial) &&
                (classBand == null || classBand in term.classBands) &&
                (difficulty == null || term.difficulty == difficulty) &&
                (normalized.isBlank() || listOf(term.term, term.definition, term.notation, term.example, term.nonExample).any { normalized in it.lowercase() })
        }
    }
}

data class McqQuestion(
    val id: String,
    val topic: KnowledgeTopic,
    val level: KnowledgeLevel,
    val prompt: String,
    val choices: List<String>,
    val answerIndex: Int,
    val explanation: String,
    val difficulty: Int = 1,
    val subject: QuizSubject = QuizSubject.Maths,
    val quizLevel: QuizLevel = when (difficulty.coerceIn(1, 3)) {
        1 -> QuizLevel.Basic
        2 -> QuizLevel.Intermediate
        else -> QuizLevel.Advanced
    },
    val category: String = topic.label,
) {
    fun check(choiceIndex: Int): McqResult {
        val correct = choiceIndex == answerIndex
        return McqResult(
            questionId = id,
            correct = correct,
            message = if (correct) "Correct" else "Review: ${choices[answerIndex]}",
            explanation = explanation,
            nextDifficulty = (difficulty + if (correct) 1 else -1).coerceIn(1, 5),
        )
    }
}

data class McqResult(
    val questionId: String,
    val correct: Boolean,
    val message: String,
    val explanation: String,
    val nextDifficulty: Int,
)

data class QuizAnswer(
    val questionId: String,
    val selectedIndex: Int,
    val correct: Boolean,
)

data class QuizSession(
    val id: String,
    val subject: QuizSubject,
    val level: QuizLevel,
    val questions: List<McqQuestion>,
    val answers: List<QuizAnswer> = emptyList(),
) {
    val currentIndex: Int get() = answers.size.coerceAtMost(questions.size)
    val currentQuestion: McqQuestion? get() = questions.getOrNull(currentIndex)
    val score: Int get() = answers.count { it.correct }
    val completed: Boolean get() = answers.size >= questions.size
    val percent: Int get() = if (questions.isEmpty()) 0 else (score * 100 / questions.size)
}

object QuizEngine {
    const val defaultQuestionCount = 15

    fun start(
        questions: List<McqQuestion>,
        subject: QuizSubject,
        level: QuizLevel,
        count: Int = defaultQuestionCount,
    ): QuizSession {
        val targetCount = count.coerceAtLeast(1)
        val exact = questions.filter { it.subject == subject && it.quizLevel == level }
        val sameSubject = questions.filter { it.subject == subject && it.id !in exact.map(McqQuestion::id).toSet() }
        val selected = (exact + sameSubject + questions)
            .distinctBy { it.id }
            .sortedWith(compareBy<McqQuestion> { if (it.subject == subject) 0 else 1 }.thenBy { kotlin.math.abs(it.difficulty - level.difficulty) }.thenBy { it.id })
            .take(targetCount)
        return QuizSession("quiz-${subject.name}-${level.name}-${selected.joinToString("-") { it.id }.hashCode()}", subject, level, selected)
    }

    fun answer(session: QuizSession, selectedIndex: Int): QuizSession {
        val question = session.currentQuestion ?: return session
        if (selectedIndex !in question.choices.indices) return session
        return session.copy(
            answers = session.answers + QuizAnswer(
                questionId = question.id,
                selectedIndex = selectedIndex,
                correct = selectedIndex == question.answerIndex,
            ),
        )
    }
}

enum class MathModuleTarget(val label: String) { Geometry2D("2D Geometry"), Graph2D("Graph"), Graph3D("3D Graph"), Statistics("Statistics") }

data class KnowledgeSearchResult(
    val formulas: List<FormulaCard>,
    val theorems: List<TheoremCard>,
    val visualProofs: List<VisualProofCard>,
    val dictionary: List<DictionaryTerm>,
    val mcqs: List<McqQuestion>,
) {
    val total: Int get() = formulas.size + theorems.size + visualProofs.size + dictionary.size + mcqs.size
}

object MathKnowledgeCatalog {
    val formulas = (formulaGroups() + additionalFormulaGroups()).flatMap { group ->
        group.items.mapIndexed { index, item ->
            FormulaCard(
                id = "${group.category.name}-${item.title}".slug(),
                title = item.title,
                topic = group.category.topic,
                category = group.category,
                level = item.level,
                expression = item.expression,
                variables = item.variables,
                useCase = item.useCase,
                relatedTerms = item.relatedTerms + group.category.label,
            )
        }
    }

    val theorems = listOf(
        TheoremCard("pythagoras", "Pythagorean theorem", KnowledgeTopic.Geometry, KnowledgeLevel.School, "In a right triangle, the square on the hypotenuse equals the sum of the squares on the legs.", listOf("Right triangle", "Euclidean plane"), listOf("Distance formula", "Circle equations", "Vector length"), listOf("Build squares on each side.", "Rearrange equal-area pieces.", "Compare total areas.")),
        TheoremCard("fundamental-calculus", "Fundamental theorem of calculus", KnowledgeTopic.Calculus, KnowledgeLevel.UG, "Differentiation and accumulation are inverse processes under continuity.", listOf("Continuous function on [a,b]", "Antiderivative exists"), listOf("Evaluate definite integrals", "Connect area and rates"), listOf("Partition the interval.", "Use accumulated area F(x).", "Show F'(x)=f(x).")),
        TheoremCard("central-limit", "Central limit theorem", KnowledgeTopic.Statistics, KnowledgeLevel.UG, "Standardized sums of many independent variables approach a normal distribution under broad conditions.", listOf("Independent samples", "Finite variance", "Large sample size"), listOf("Confidence intervals", "Sampling distributions", "Hypothesis tests"), listOf("Track sample means.", "Scale by standard error.", "Observe convergence to the normal curve.")),
        TheoremCard("bayes-rule", "Bayes theorem", KnowledgeTopic.Probability, KnowledgeLevel.UG, "Posterior odds combine prior belief with evidence likelihood.", listOf("P(B) not zero"), listOf("Diagnosis", "Machine learning", "Decision analysis"), listOf("Start from P(A and B).", "Write it both as P(A|B)P(B) and P(B|A)P(A).", "Rearrange.")),
        TheoremCard("spectral", "Spectral theorem foundation", KnowledgeTopic.Algebra, KnowledgeLevel.PG, "A real symmetric matrix has orthogonal eigenvectors and real eigenvalues.", listOf("Real symmetric matrix"), listOf("Quadratic forms", "PCA", "Optimization"), listOf("Use symmetry of inner products.", "Show distinct eigenspaces are orthogonal.", "Build an orthonormal basis.")),
    )

    val visualProofs = listOf(
        VisualProofCard("triangle-angle-sum", "Triangle angle sum", KnowledgeTopic.Geometry, KnowledgeLevel.School, MathModuleTarget.Geometry2D, listOf("Create triangle ABC.", "Draw a parallel through A to BC.", "Drag vertices and watch the three copied angles form a straight line."), "The total angle remains 180 degrees.", "Move one vertex until the triangle is nearly flat, then explain what stays constant."),
        VisualProofCard("derivative-slope", "Derivative as moving slope", KnowledgeTopic.Calculus, KnowledgeLevel.UG, MathModuleTarget.Graph2D, listOf("Plot f(x)=x^2.", "Place two nearby points.", "Move the second point toward the first."), "Secant slopes approach the tangent slope.", "Predict the slope at x=2 before tracing it."),
        VisualProofCard("normal-area", "Normal probability as area", KnowledgeTopic.Statistics, KnowledgeLevel.School, MathModuleTarget.Statistics, listOf("Open Normal distribution.", "Move lower and upper bounds.", "Observe shaded area and CDF difference."), "Interval probability equals area under the curve.", "Find the middle 68 percent using only the sliders."),
        VisualProofCard("surface-gradient", "Gradient on a surface", KnowledgeTopic.Calculus, KnowledgeLevel.UG, MathModuleTarget.Graph3D, listOf("Plot z=x^2+y^2.", "Show tangent plane and gradient arrow.", "Move the trace point."), "The gradient points toward steepest increase.", "Tap around a level circle and compare arrow direction."),
    )

    val dictionary = MathDictionaryCatalog.terms

    val mcqs = listOf(
        McqQuestion("mcq-discriminant", KnowledgeTopic.Algebra, KnowledgeLevel.School, "If b^2 - 4ac is negative, a real quadratic has:", listOf("Two real roots", "One repeated real root", "No real roots", "Infinitely many roots"), 2, "A negative discriminant means the square root part is imaginary.", 1),
        McqQuestion("mcq-product-rule", KnowledgeTopic.Calculus, KnowledgeLevel.UG, "The derivative of x sin(x) is:", listOf("sin(x)", "x cos(x)", "sin(x) + x cos(x)", "cos(x) - x sin(x)"), 2, "Use the product rule with u=x and v=sin(x).", 2),
        McqQuestion("mcq-median", KnowledgeTopic.Statistics, KnowledgeLevel.School, "The median of 3, 8, 8, 20, 30 is:", listOf("8", "13.8", "20", "30"), 0, "The middle ordered value is 8.", 1),
        McqQuestion("mcq-bayes", KnowledgeTopic.Probability, KnowledgeLevel.UG, "Bayes theorem is most directly used to compute:", listOf("A derivative", "A posterior probability", "A matrix inverse", "A tangent plane"), 1, "Bayes theorem updates probability after evidence.", 2),
        McqQuestion("mcq-eigen", KnowledgeTopic.Algebra, KnowledgeLevel.PG, "In Av=lambda v, v must be:", listOf("Zero only", "Nonzero", "Always perpendicular to Av", "A scalar"), 1, "An eigenvector is nonzero; otherwise every lambda would work trivially.", 3),
    ) + generatedMcqs()

    fun search(query: String, topic: KnowledgeTopic? = null, level: KnowledgeLevel? = null, formulaCategory: FormulaCategory? = null): KnowledgeSearchResult {
        val normalized = query.trim().lowercase()
        fun topicLevelMatches(itemTopic: KnowledgeTopic, itemLevel: KnowledgeLevel) =
            (topic == null || topic == itemTopic) && (level == null || level == itemLevel)
        fun textMatches(vararg parts: String) = normalized.isBlank() || parts.any { it.lowercase().contains(normalized) }
        return KnowledgeSearchResult(
            formulas = formulas.filter { (formulaCategory == null || it.category == formulaCategory) && topicLevelMatches(it.topic, it.level) && textMatches(it.title, it.expression, it.useCase, it.relatedTerms.joinToString(), it.category.label) },
            theorems = theorems.filter { topicLevelMatches(it.topic, it.level) && textMatches(it.title, it.statement, it.applications.joinToString()) },
            visualProofs = visualProofs.filter { topicLevelMatches(it.topic, it.level) && textMatches(it.title, it.invariant, it.learnerPrompt) },
            dictionary = dictionary.filter { topicLevelMatches(it.topic, it.level) && textMatches(it.term, it.definition, it.notation, it.example) },
            mcqs = mcqs.filter { topicLevelMatches(it.topic, it.level) && textMatches(it.prompt, it.choices.joinToString(), it.explanation) },
        )
    }

    fun recommendedMcqs(topic: KnowledgeTopic? = null, level: KnowledgeLevel? = null, targetDifficulty: Int = 1): List<McqQuestion> =
        mcqs.filter { (topic == null || it.topic == topic) && (level == null || it.level == level) }
            .sortedWith(compareBy<McqQuestion> { kotlin.math.abs(it.difficulty - targetDifficulty) }.thenBy { it.id })

    private data class FormulaItem(
        val title: String,
        val expression: String,
        val variables: List<String>,
        val useCase: String,
        val level: KnowledgeLevel = KnowledgeLevel.School,
        val relatedTerms: List<String> = emptyList(),
    )

    private data class FormulaGroup(val category: FormulaCategory, val items: List<FormulaItem>)

    private fun item(title: String, expression: String, vars: String, useCase: String, level: KnowledgeLevel = KnowledgeLevel.School, related: List<String> = emptyList()) =
        FormulaItem(title, expression, vars.split(',').map { it.trim() }.filter { it.isNotBlank() }, useCase, level, related)

    private fun formulaGroups() = listOf(
        FormulaGroup(FormulaCategory.Algebra, listOf(
            item("Quadratic roots", """x=\frac{-b\pm\sqrt{b^{2}-4ac}}{2a}""", "a,b,c", "Solve quadratic equations.", related = listOf("discriminant", "roots")),
            item("Discriminant", """\Delta=b^{2}-4ac""", "a,b,c", "Classify roots of a quadratic."),
            item("Vertex of parabola", """\left(h,k\right)=\left(\frac{-b}{2a},f\left(\frac{-b}{2a}\right)\right)""", "a,b,h,k", "Find the turning point of a quadratic."),
            item("Completing square", """ax^{2}+bx+c=a\left(x+\frac{b}{2a}\right)^{2}+c-\frac{b^{2}}{4a}""", "a,b,c,x", "Rewrite quadratics in vertex form."),
            item("Difference of squares", """a^{2}-b^{2}=\left(a-b\right)\left(a+b\right)""", "a,b", "Factor paired squares."),
            item("Perfect square expansion", """\left(a\pm b\right)^{2}=a^{2}\pm2ab+b^{2}""", "a,b", "Expand or factor square binomials."),
            item("Cubic sum", """a^{3}+b^{3}=\left(a+b\right)\left(a^{2}-ab+b^{2}\right)""", "a,b", "Factor sum of cubes."),
            item("Cubic difference", """a^{3}-b^{3}=\left(a-b\right)\left(a^{2}+ab+b^{2}\right)""", "a,b", "Factor difference of cubes."),
            item("Arithmetic sequence", """a_{n}=a_{1}+\left(n-1\right)d""", "a_1,n,d", "Find terms with constant difference."),
            item("Arithmetic series", """S_{n}=\frac{n}{2}\left(a_{1}+a_{n}\right)""", "S_n,n,a_1,a_n", "Sum an arithmetic progression."),
            item("Geometric sequence", """a_{n}=a_{1}r^{n-1}""", "a_1,r,n", "Find terms with constant ratio."),
            item("Geometric series", """S_{n}=a_{1}\frac{1-r^{n}}{1-r}""", "S_n,a_1,r,n", "Sum a finite geometric progression."),
        )),
        FormulaGroup(FormulaCategory.Geometry, listOf(
            item("Triangle area", """A=\frac{1}{2}bh""", "A,b,h", "Find area from base and height."),
            item("Heron's formula", """A=\sqrt{s\left(s-a\right)\left(s-b\right)\left(s-c\right)}""", "A,s,a,b,c", "Find triangle area from three sides."),
            item("Semiperimeter", """s=\frac{a+b+c}{2}""", "s,a,b,c", "Prepare Heron's formula."),
            item("Pythagorean theorem", """a^{2}+b^{2}=c^{2}""", "a,b,c", "Relate sides of a right triangle."),
            item("Circle area", """A=\pi r^{2}""", "A,r", "Find area of a circle."),
            item("Circle circumference", """C=2\pi r""", "C,r", "Find circle boundary length."),
            item("Sector area", """A=\frac{\theta}{360^{\circ}}\pi r^{2}""", "A,theta,r", "Find area of a circular sector."),
            item("Arc length", """L=\frac{\theta}{360^{\circ}}2\pi r""", "L,theta,r", "Find length of a circular arc."),
            item("Rectangle diagonal", """d=\sqrt{l^{2}+w^{2}}""", "d,l,w", "Find diagonal of a rectangle."),
            item("Trapezium area", """A=\frac{1}{2}\left(a+b\right)h""", "A,a,b,h", "Find area between parallel sides."),
            item("Cylinder volume", """V=\pi r^{2}h""", "V,r,h", "Find volume of a cylinder."),
            item("Sphere volume", """V=\frac{4}{3}\pi r^{3}""", "V,r", "Find volume of a sphere."),
        )),
        FormulaGroup(FormulaCategory.Trigonometry, listOf(
            item("Sine ratio", """\sin\theta=\frac{\text{opposite}}{\text{hypotenuse}}""", "theta", "Use right-triangle sine."),
            item("Cosine ratio", """\cos\theta=\frac{\text{adjacent}}{\text{hypotenuse}}""", "theta", "Use right-triangle cosine."),
            item("Tangent ratio", """\tan\theta=\frac{\sin\theta}{\cos\theta}""", "theta", "Connect tangent to sine and cosine."),
            item("Pythagorean identity", """\sin^{2}\theta+\cos^{2}\theta=1""", "theta", "Simplify trig expressions."),
            item("Secant identity", """1+\tan^{2}\theta=\sec^{2}\theta""", "theta", "Transform tangent and secant."),
            item("Cosecant identity", """1+\cot^{2}\theta=\csc^{2}\theta""", "theta", "Transform cotangent and cosecant."),
            item("Sine addition", """\sin\left(A+B\right)=\sin A\cos B+\cos A\sin B""", "A,B", "Expand sine of a sum."),
            item("Cosine addition", """\cos\left(A+B\right)=\cos A\cos B-\sin A\sin B""", "A,B", "Expand cosine of a sum."),
            item("Double-angle sine", """\sin2\theta=2\sin\theta\cos\theta""", "theta", "Simplify double angles."),
            item("Double-angle cosine", """\cos2\theta=\cos^{2}\theta-\sin^{2}\theta""", "theta", "Simplify double angles."),
            item("Law of sines", """\frac{a}{\sin A}=\frac{b}{\sin B}=\frac{c}{\sin C}""", "a,b,c,A,B,C", "Solve non-right triangles."),
            item("Law of cosines", """c^{2}=a^{2}+b^{2}-2ab\cos C""", "a,b,c,C", "Solve triangles with included angle."),
        )),
        FormulaGroup(FormulaCategory.Calculus, listOf(
            item("Derivative definition", """f'\left(x\right)=\lim_{h\to0}\frac{f\left(x+h\right)-f\left(x\right)}{h}""", "f,x,h", "Define instantaneous rate.", KnowledgeLevel.UG),
            item("Power rule", """\frac{d}{dx}x^{n}=nx^{n-1}""", "x,n", "Differentiate powers.", KnowledgeLevel.UG),
            item("Product rule", """\frac{d}{dx}\left(uv\right)=u\frac{dv}{dx}+v\frac{du}{dx}""", "u,v,x", "Differentiate products.", KnowledgeLevel.UG, listOf("derivative", "chain rule")),
            item("Quotient rule", """\frac{d}{dx}\left(\frac{u}{v}\right)=\frac{v\frac{du}{dx}-u\frac{dv}{dx}}{v^{2}}""", "u,v,x", "Differentiate quotients.", KnowledgeLevel.UG),
            item("Chain rule", """\frac{d}{dx}f\left(g\left(x\right)\right)=f'\left(g\left(x\right)\right)g'\left(x\right)""", "f,g,x", "Differentiate composites.", KnowledgeLevel.UG),
            item("Fundamental theorem", """\frac{d}{dx}\int_{a}^{x}f\left(t\right)\,dt=f\left(x\right)""", "a,x,t", "Connect accumulation and derivative.", KnowledgeLevel.UG),
            item("Integration by parts", """\int u\,dv=uv-\int v\,du""", "u,v", "Integrate products.", KnowledgeLevel.UG, listOf("integral", "product rule")),
            item("Substitution rule", """\int f\left(g\left(x\right)\right)g'\left(x\right)\,dx=\int f\left(u\right)\,du""", "f,g,u,x", "Change variables in integrals.", KnowledgeLevel.UG),
            item("Taylor series", """f\left(x\right)=\sum_{n=0}^{\infty}\frac{f^{\left(n\right)}\left(a\right)}{n!}\left(x-a\right)^{n}""", "f,x,a,n", "Approximate functions locally.", KnowledgeLevel.PG),
            item("Gradient", """\nabla f=\left\langle\frac{\partial f}{\partial x},\frac{\partial f}{\partial y},\frac{\partial f}{\partial z}\right\rangle""", "f,x,y,z", "Find steepest increase.", KnowledgeLevel.UG, listOf("partial derivative", "tangent plane")),
            item("Divergence", """\nabla\cdot\mathbf{F}=\frac{\partial P}{\partial x}+\frac{\partial Q}{\partial y}+\frac{\partial R}{\partial z}""", "F,P,Q,R", "Measure vector-field source strength.", KnowledgeLevel.PG),
            item("Curl", """\nabla\times\mathbf{F}=\left\langle R_{y}-Q_{z},P_{z}-R_{x},Q_{x}-P_{y}\right\rangle""", "F,P,Q,R", "Measure vector-field rotation.", KnowledgeLevel.PG),
        )),
        FormulaGroup(FormulaCategory.DifferentialEquations, listOf(
            item("Separable equation", """\frac{dy}{dx}=g\left(x\right)h\left(y\right)""", "x,y,g,h", "Recognize separable ODEs.", KnowledgeLevel.UG),
            item("Separated integral", """\int\frac{1}{h\left(y\right)}\,dy=\int g\left(x\right)\,dx+C""", "x,y,g,h,C", "Solve separable ODEs.", KnowledgeLevel.UG),
            item("Linear first order", """\frac{dy}{dx}+P\left(x\right)y=Q\left(x\right)""", "x,y,P,Q", "Recognize linear ODEs.", KnowledgeLevel.UG),
            item("Integrating factor", """\mu\left(x\right)=e^{\int P\left(x\right)\,dx}""", "mu,x,P", "Build integrating factor.", KnowledgeLevel.UG),
            item("Linear solution", """y\mu=\int \mu Q\left(x\right)\,dx+C""", "y,mu,Q,C", "Solve first-order linear ODE.", KnowledgeLevel.UG),
            item("Exponential growth", """y=Ce^{kt}""", "y,C,k,t", "Model growth and decay.", KnowledgeLevel.School),
            item("Logistic model", """\frac{dP}{dt}=rP\left(1-\frac{P}{K}\right)""", "P,t,r,K", "Model bounded population growth.", KnowledgeLevel.UG),
            item("Logistic solution", """P\left(t\right)=\frac{K}{1+Ae^{-rt}}""", "P,t,K,A,r", "Solve logistic growth.", KnowledgeLevel.UG),
            item("Second-order linear", """a\frac{d^{2}y}{dx^{2}}+b\frac{dy}{dx}+cy=0""", "a,b,c,y,x", "Model oscillations.", KnowledgeLevel.UG),
            item("Characteristic equation", """ar^{2}+br+c=0""", "a,b,c,r", "Solve constant-coefficient ODEs.", KnowledgeLevel.UG),
            item("Euler method", """y_{n+1}=y_{n}+hf\left(x_{n},y_{n}\right)""", "y,h,x,f", "Approximate ODE solutions.", KnowledgeLevel.UG),
            item("RK4 update", """y_{n+1}=y_{n}+\frac{h}{6}\left(k_{1}+2k_{2}+2k_{3}+k_{4}\right)""", "y,h,k", "Approximate ODEs accurately.", KnowledgeLevel.PG),
        )),
        FormulaGroup(FormulaCategory.LinearAlgebra, listOf(
            item("Matrix product", """\left(AB\right)_{ij}=\sum_{k=1}^{n}a_{ik}b_{kj}""", "A,B,i,j,k", "Multiply matrices.", KnowledgeLevel.UG),
            item("Determinant 2 by 2", """\det\begin{pmatrix}a&b\\c&d\end{pmatrix}=ad-bc""", "a,b,c,d", "Find area scale and invertibility.", KnowledgeLevel.UG),
            item("Inverse 2 by 2", """A^{-1}=\frac{1}{ad-bc}\begin{pmatrix}d&-b\\-c&a\end{pmatrix}""", "A,a,b,c,d", "Invert a 2 by 2 matrix.", KnowledgeLevel.UG),
            item("Eigenvalue equation", """A\mathbf{v}=\lambda\mathbf{v}""", "A,v,lambda", "Analyze linear transforms.", KnowledgeLevel.PG, listOf("matrix", "linear transformation")),
            item("Characteristic polynomial", """\det\left(A-\lambda I\right)=0""", "A,lambda,I", "Find eigenvalues.", KnowledgeLevel.PG),
            item("Dot product", """\mathbf{a}\cdot\mathbf{b}=\sum_{i=1}^{n}a_{i}b_{i}""", "a,b,i", "Measure projection and angle.", KnowledgeLevel.UG),
            item("Vector norm", """\left\lVert\mathbf{v}\right\rVert=\sqrt{\mathbf{v}\cdot\mathbf{v}}""", "v", "Find vector length.", KnowledgeLevel.UG),
            item("Projection", """\operatorname{proj}_{\mathbf{b}}\mathbf{a}=\frac{\mathbf{a}\cdot\mathbf{b}}{\mathbf{b}\cdot\mathbf{b}}\mathbf{b}""", "a,b", "Project one vector onto another.", KnowledgeLevel.UG),
            item("Rank-nullity", """\operatorname{rank}\left(A\right)+\operatorname{nullity}\left(A\right)=n""", "A,n", "Relate columns and null space.", KnowledgeLevel.UG),
            item("Trace", """\operatorname{tr}\left(A\right)=\sum_{i=1}^{n}a_{ii}""", "A,i", "Sum diagonal entries.", KnowledgeLevel.UG),
            item("Orthogonality", """\mathbf{u}\cdot\mathbf{v}=0""", "u,v", "Test perpendicular vectors.", KnowledgeLevel.UG),
            item("Least squares", """\hat{\beta}=\left(X^{T}X\right)^{-1}X^{T}y""", "X,y,beta", "Fit linear models.", KnowledgeLevel.PG),
        )),
        FormulaGroup(FormulaCategory.CoordinateGeometry, listOf(
            item("Distance formula", """d=\sqrt{\left(x_{2}-x_{1}\right)^{2}+\left(y_{2}-y_{1}\right)^{2}}""", "d,x_1,x_2,y_1,y_2", "Find distance between points."),
            item("Midpoint", """M=\left(\frac{x_{1}+x_{2}}{2},\frac{y_{1}+y_{2}}{2}\right)""", "M,x_1,x_2,y_1,y_2", "Find middle of a segment."),
            item("Slope", """m=\frac{y_{2}-y_{1}}{x_{2}-x_{1}}""", "m,x_1,x_2,y_1,y_2", "Find line steepness."),
            item("Point-slope line", """y-y_{1}=m\left(x-x_{1}\right)""", "x,y,x_1,y_1,m", "Build a line from point and slope."),
            item("Two-point line", """y-y_{1}=\frac{y_{2}-y_{1}}{x_{2}-x_{1}}\left(x-x_{1}\right)""", "x,y,x_1,x_2,y_1,y_2", "Build a line through two points."),
            item("Circle equation", """\left(x-h\right)^{2}+\left(y-k\right)^{2}=r^{2}""", "x,y,h,k,r", "Represent a circle."),
            item("Parabola standard", """\left(x-h\right)^{2}=4p\left(y-k\right)""", "x,y,h,k,p", "Represent vertical parabola."),
            item("Ellipse standard", """\frac{\left(x-h\right)^{2}}{a^{2}}+\frac{\left(y-k\right)^{2}}{b^{2}}=1""", "x,y,h,k,a,b", "Represent an ellipse."),
            item("Hyperbola standard", """\frac{\left(x-h\right)^{2}}{a^{2}}-\frac{\left(y-k\right)^{2}}{b^{2}}=1""", "x,y,h,k,a,b", "Represent a hyperbola."),
            item("Point-line distance", """d=\frac{\left|Ax_{0}+By_{0}+C\right|}{\sqrt{A^{2}+B^{2}}}""", "d,A,B,C,x_0,y_0", "Find distance from point to line.", KnowledgeLevel.UG),
            item("Polygon area", """A=\frac{1}{2}\left|\sum_{i=1}^{n}x_{i}y_{i+1}-y_{i}x_{i+1}\right|""", "A,x,y,i,n", "Use shoelace formula.", KnowledgeLevel.UG),
            item("Centroid of triangle", """G=\left(\frac{x_{1}+x_{2}+x_{3}}{3},\frac{y_{1}+y_{2}+y_{3}}{3}\right)""", "G,x,y", "Find triangle centroid."),
        )),
        FormulaGroup(FormulaCategory.Vectors3D, listOf(
            item("3D distance", """d=\sqrt{\left(x_{2}-x_{1}\right)^{2}+\left(y_{2}-y_{1}\right)^{2}+\left(z_{2}-z_{1}\right)^{2}}""", "d,x,y,z", "Find distance in space."),
            item("Cross product", """\mathbf{a}\times\mathbf{b}=\left\langle a_{2}b_{3}-a_{3}b_{2},a_{3}b_{1}-a_{1}b_{3},a_{1}b_{2}-a_{2}b_{1}\right\rangle""", "a,b", "Find perpendicular vector.", KnowledgeLevel.UG),
            item("Scalar triple product", """V=\left|\mathbf{a}\cdot\left(\mathbf{b}\times\mathbf{c}\right)\right|""", "V,a,b,c", "Find parallelepiped volume.", KnowledgeLevel.UG),
            item("Plane equation", """\mathbf{n}\cdot\left(\mathbf{r}-\mathbf{r}_{0}\right)=0""", "n,r,r_0", "Represent a plane.", KnowledgeLevel.UG),
            item("Line in space", """\mathbf{r}=\mathbf{r}_{0}+t\mathbf{v}""", "r,r_0,t,v", "Represent a 3D line.", KnowledgeLevel.UG),
            item("Sphere equation", """\left(x-a\right)^{2}+\left(y-b\right)^{2}+\left(z-c\right)^{2}=r^{2}""", "x,y,z,a,b,c,r", "Represent a sphere."),
            item("Vector angle", """\cos\theta=\frac{\mathbf{a}\cdot\mathbf{b}}{\left\lVert\mathbf{a}\right\rVert\left\lVert\mathbf{b}\right\rVert}""", "theta,a,b", "Find angle between vectors.", KnowledgeLevel.UG),
            item("Plane distance", """d=\frac{\left|\mathbf{n}\cdot\mathbf{p}+D\right|}{\left\lVert\mathbf{n}\right\rVert}""", "d,n,p,D", "Find distance from point to plane.", KnowledgeLevel.UG),
            item("Cylinder surface area", """S=2\pi r\left(r+h\right)""", "S,r,h", "Find total surface area."),
            item("Cone volume", """V=\frac{1}{3}\pi r^{2}h""", "V,r,h", "Find cone volume."),
            item("Torus volume", """V=2\pi^{2}Rr^{2}""", "V,R,r", "Find torus volume.", KnowledgeLevel.UG),
            item("Tangent plane", """z-z_{0}=f_{x}\left(x_{0},y_{0}\right)\left(x-x_{0}\right)+f_{y}\left(x_{0},y_{0}\right)\left(y-y_{0}\right)""", "z,x,y,f", "Approximate a surface locally.", KnowledgeLevel.UG),
        )),
        FormulaGroup(FormulaCategory.Probability, listOf(
            item("Complement rule", """P\left(A^{c}\right)=1-P\left(A\right)""", "A", "Find probability of not A."),
            item("Addition rule", """P\left(A\cup B\right)=P\left(A\right)+P\left(B\right)-P\left(A\cap B\right)""", "A,B", "Combine overlapping events."),
            item("Conditional probability", """P\left(A\mid B\right)=\frac{P\left(A\cap B\right)}{P\left(B\right)}""", "A,B", "Condition on evidence."),
            item("Multiplication rule", """P\left(A\cap B\right)=P\left(A\mid B\right)P\left(B\right)""", "A,B", "Find joint probability."),
            item("Bayes theorem", """P\left(A\mid B\right)=\frac{P\left(B\mid A\right)P\left(A\right)}{P\left(B\right)}""", "A,B", "Update probability after evidence.", KnowledgeLevel.UG, listOf("conditional probability", "posterior")),
            item("Total probability", """P\left(B\right)=\sum_{i}P\left(B\mid A_{i}\right)P\left(A_{i}\right)""", "A_i,B", "Partition sample space.", KnowledgeLevel.UG),
            item("Independence", """P\left(A\cap B\right)=P\left(A\right)P\left(B\right)""", "A,B", "Test independent events."),
            item("Expected value", """E\left(X\right)=\sum_{x}xP\left(X=x\right)""", "X,x", "Average random outcomes."),
            item("Variance", """\operatorname{Var}\left(X\right)=E\left(X^{2}\right)-\left(E\left(X\right)\right)^{2}""", "X", "Measure spread of a random variable."),
            item("Odds", """\operatorname{odds}\left(A\right)=\frac{P\left(A\right)}{1-P\left(A\right)}""", "A", "Convert probability to odds."),
            item("Union bound", """P\left(\bigcup_{i=1}^{n}A_{i}\right)\leq\sum_{i=1}^{n}P\left(A_{i}\right)""", "A_i,n", "Bound probability of any event.", KnowledgeLevel.UG),
            item("Markov inequality", """P\left(X\geq a\right)\leq\frac{E\left(X\right)}{a}""", "X,a", "Bound tail probability.", KnowledgeLevel.PG),
        )),
        FormulaGroup(FormulaCategory.Statistics, listOf(
            item("Mean", """\bar{x}=\frac{1}{n}\sum_{i=1}^{n}x_{i}""", "x,n,i", "Find arithmetic average."),
            item("Sample variance", """s^{2}=\frac{1}{n-1}\sum_{i=1}^{n}\left(x_{i}-\bar{x}\right)^{2}""", "s,x,n,i", "Estimate variance from a sample."),
            item("Population variance", """\sigma^{2}=\frac{1}{N}\sum_{i=1}^{N}\left(x_{i}-\mu\right)^{2}""", "sigma,N,x,mu", "Measure population spread."),
            item("Standard score", """z=\frac{x-\mu}{\sigma}""", "z,x,mu,sigma", "Standardize a value.", related = listOf("normal distribution", "z score")),
            item("Standard error", """SE=\frac{s}{\sqrt{n}}""", "SE,s,n", "Estimate spread of sample mean."),
            item("Confidence interval", """\bar{x}\pm t_{\frac{\alpha}{2},n-1}\frac{s}{\sqrt{n}}""", "x,t,alpha,s,n", "Estimate population mean.", KnowledgeLevel.UG),
            item("Pearson correlation", """r=\frac{\sum\left(x_{i}-\bar{x}\right)\left(y_{i}-\bar{y}\right)}{\sqrt{\sum\left(x_{i}-\bar{x}\right)^{2}\sum\left(y_{i}-\bar{y}\right)^{2}}}""", "r,x,y,i", "Measure linear association.", KnowledgeLevel.UG),
            item("Regression slope", """b_{1}=\frac{\sum\left(x_{i}-\bar{x}\right)\left(y_{i}-\bar{y}\right)}{\sum\left(x_{i}-\bar{x}\right)^{2}}""", "b_1,x,y", "Fit simple linear regression.", KnowledgeLevel.UG),
            item("Regression intercept", """b_{0}=\bar{y}-b_{1}\bar{x}""", "b_0,b_1,x,y", "Complete regression line.", KnowledgeLevel.UG),
            item("Chi-square statistic", """\chi^{2}=\sum\frac{\left(O-E\right)^{2}}{E}""", "chi,O,E", "Compare observed and expected counts.", KnowledgeLevel.UG),
            item("Coefficient of variation", """CV=\frac{s}{\bar{x}}\times100\%""", "CV,s,x", "Compare relative variability.", KnowledgeLevel.UG),
            item("Interquartile range", """IQR=Q_{3}-Q_{1}""", "IQR,Q_1,Q_3", "Measure middle spread."),
        )),
        FormulaGroup(FormulaCategory.Distributions, listOf(
            item("Binomial PMF", """P\left(X=k\right)=\binom{n}{k}p^{k}\left(1-p\right)^{n-k}""", "X,k,n,p", "Model fixed Bernoulli trials."),
            item("Binomial mean", """E\left(X\right)=np""", "X,n,p", "Find binomial expectation."),
            item("Binomial variance", """\operatorname{Var}\left(X\right)=np\left(1-p\right)""", "X,n,p", "Find binomial spread."),
            item("Poisson PMF", """P\left(X=k\right)=e^{-\lambda}\frac{\lambda^{k}}{k!}""", "X,k,lambda", "Model event counts."),
            item("Poisson mean", """E\left(X\right)=\lambda""", "X,lambda", "Find Poisson expectation."),
            item("Normal PDF", """f\left(x\right)=\frac{1}{\sigma\sqrt{2\pi}}e^{-\frac{1}{2}\left(\frac{x-\mu}{\sigma}\right)^{2}}""", "f,x,mu,sigma", "Model continuous bell curves."),
            item("Normal standardization", """Z=\frac{X-\mu}{\sigma}""", "Z,X,mu,sigma", "Convert normal variable to standard normal."),
            item("Exponential PDF", """f\left(x\right)=\lambda e^{-\lambda x}""", "f,x,lambda", "Model waiting time."),
            item("Exponential CDF", """F\left(x\right)=1-e^{-\lambda x}""", "F,x,lambda", "Find waiting-time probability."),
            item("Uniform PDF", """f\left(x\right)=\frac{1}{b-a}""", "f,x,a,b", "Model equal density interval."),
            item("Gamma PDF", """f\left(x\right)=\frac{\beta^{\alpha}}{\Gamma\left(\alpha\right)}x^{\alpha-1}e^{-\beta x}""", "f,x,alpha,beta", "Model waiting-time families.", KnowledgeLevel.PG),
            item("Student t statistic", """t=\frac{\bar{x}-\mu_{0}}{\frac{s}{\sqrt{n}}}""", "t,x,mu,s,n", "Test a mean with unknown variance.", KnowledgeLevel.UG),
        )),
        FormulaGroup(FormulaCategory.NumberTheory, listOf(
            item("Euclidean division", """a=bq+r,\quad0\leq r<b""", "a,b,q,r", "Divide integers with remainder."),
            item("Greatest common divisor", """\gcd\left(a,b\right)=\gcd\left(b,a\bmod b\right)""", "a,b", "Use Euclidean algorithm."),
            item("Bezout identity", """ax+by=\gcd\left(a,b\right)""", "a,b,x,y", "Represent gcd as a linear combination.", KnowledgeLevel.UG),
            item("Least common multiple", """\operatorname{lcm}\left(a,b\right)=\frac{\left|ab\right|}{\gcd\left(a,b\right)}""", "a,b", "Connect gcd and lcm."),
            item("Modular congruence", """a\equiv b\pmod{m}""", "a,b,m", "Express equal remainders."),
            item("Fermat little theorem", """a^{p-1}\equiv1\pmod{p}""", "a,p", "Simplify powers modulo primes.", KnowledgeLevel.UG),
            item("Euler theorem", """a^{\varphi\left(n\right)}\equiv1\pmod{n}""", "a,n", "Generalize Fermat theorem.", KnowledgeLevel.UG),
            item("Prime counting estimate", """\pi\left(x\right)\sim\frac{x}{\ln x}""", "pi,x", "Estimate number of primes.", KnowledgeLevel.PG),
            item("Sum of first n integers", """1+2+\cdots+n=\frac{n\left(n+1\right)}{2}""", "n", "Sum consecutive integers."),
            item("Sum of squares", """1^{2}+2^{2}+\cdots+n^{2}=\frac{n\left(n+1\right)\left(2n+1\right)}{6}""", "n", "Sum square numbers."),
            item("Sum of cubes", """1^{3}+2^{3}+\cdots+n^{3}=\left(\frac{n\left(n+1\right)}{2}\right)^{2}""", "n", "Sum cube numbers."),
            item("Chinese remainder", """x\equiv a_{i}\pmod{m_{i}}""", "x,a_i,m_i", "Solve compatible modular systems.", KnowledgeLevel.PG),
        )),
        FormulaGroup(FormulaCategory.Combinatorics, listOf(
            item("Factorial", """n!=n\left(n-1\right)\left(n-2\right)\cdots1""", "n", "Count arrangements."),
            item("Permutation", """{}^{n}P_{r}=\frac{n!}{\left(n-r\right)!}""", "n,r", "Count ordered selections."),
            item("Combination", """\binom{n}{r}=\frac{n!}{r!\left(n-r\right)!}""", "n,r", "Count unordered selections."),
            item("Binomial theorem", """\left(a+b\right)^{n}=\sum_{r=0}^{n}\binom{n}{r}a^{n-r}b^{r}""", "a,b,n,r", "Expand powers and derive combinations.", related = listOf("combination", "pascal triangle")),
            item("Pascal identity", """\binom{n}{r}=\binom{n-1}{r-1}+\binom{n-1}{r}""", "n,r", "Build Pascal triangle."),
            item("Stars and bars", """\binom{n+k-1}{k-1}""", "n,k", "Count nonnegative integer solutions.", KnowledgeLevel.UG),
            item("Inclusion-exclusion", """\left|A\cup B\right|=\left|A\right|+\left|B\right|-\left|A\cap B\right|""", "A,B", "Count overlapping sets."),
            item("Pigeonhole principle", """n>km\Rightarrow\text{some box has at least }k+1\text{ objects}""", "n,k,m", "Prove unavoidable repetition."),
            item("Catalan number", """C_{n}=\frac{1}{n+1}\binom{2n}{n}""", "C,n", "Count balanced structures.", KnowledgeLevel.UG),
            item("Derangements", """!n=n!\sum_{k=0}^{n}\frac{\left(-1\right)^{k}}{k!}""", "n,k", "Count permutations with no fixed points.", KnowledgeLevel.UG),
            item("Multinomial coefficient", """\binom{n}{k_{1},k_{2},\ldots,k_{m}}=\frac{n!}{k_{1}!k_{2}!\cdots k_{m}!}""", "n,k,m", "Count grouped arrangements.", KnowledgeLevel.UG),
            item("Handshake lemma", """\sum_{v\in V}\deg\left(v\right)=2\left|E\right|""", "v,V,E", "Relate graph degrees and edges.", KnowledgeLevel.UG),
        )),
        FormulaGroup(FormulaCategory.ComplexNumbers, listOf(
            item("Complex number", """z=a+b\mathrm{i}""", "z,a,b", "Represent real and imaginary parts."),
            item("Modulus", """\left|z\right|=\sqrt{a^{2}+b^{2}}""", "z,a,b", "Find complex magnitude."),
            item("Argument", """\theta=\arg z=\tan^{-1}\left(\frac{b}{a}\right)""", "theta,z,a,b", "Find complex angle."),
            item("Conjugate", """\overline{z}=a-bi""", "z,a,b", "Reflect across real axis."),
            item("Product with conjugate", """z\overline{z}=\left|z\right|^{2}""", "z", "Simplify divisions."),
            item("Polar form", """z=r\left(\cos\theta+i\sin\theta\right)""", "z,r,theta", "Represent complex numbers geometrically."),
            item("Euler form", """z=re^{i\theta}""", "z,r,theta", "Use exponential complex form.", KnowledgeLevel.UG),
            item("Euler identity", """e^{i\theta}=\cos\theta+i\sin\theta""", "theta", "Connect trig and exponential.", KnowledgeLevel.UG),
            item("De Moivre", """\left(\cos\theta+i\sin\theta\right)^{n}=\cos n\theta+i\sin n\theta""", "theta,n", "Raise complex numbers to powers.", KnowledgeLevel.UG),
            item("Complex roots", """z_{k}=r^{\frac{1}{n}}e^{i\frac{\theta+2\pi k}{n}}""", "z,r,n,theta,k", "Find all nth roots.", KnowledgeLevel.UG),
            item("Real part", """\operatorname{Re}\left(z\right)=\frac{z+\overline{z}}{2}""", "z", "Extract real component."),
            item("Imaginary part", """\operatorname{Im}\left(z\right)=\frac{z-\overline{z}}{2i}""", "z", "Extract imaginary component."),
        )),
        FormulaGroup(FormulaCategory.NumericalMethods, listOf(
            item("Newton method", """x_{n+1}=x_{n}-\frac{f\left(x_{n}\right)}{f'\left(x_{n}\right)}""", "x,n,f", "Approximate roots.", KnowledgeLevel.UG),
            item("Bisection midpoint", """c=\frac{a+b}{2}""", "c,a,b", "Bisect root bracket."),
            item("Secant method", """x_{n+1}=x_{n}-f\left(x_{n}\right)\frac{x_{n}-x_{n-1}}{f\left(x_{n}\right)-f\left(x_{n-1}\right)}""", "x,n,f", "Approximate roots without derivative.", KnowledgeLevel.UG),
            item("Forward difference", """f'\left(x\right)\approx\frac{f\left(x+h\right)-f\left(x\right)}{h}""", "f,x,h", "Approximate derivative."),
            item("Central difference", """f'\left(x\right)\approx\frac{f\left(x+h\right)-f\left(x-h\right)}{2h}""", "f,x,h", "Approximate derivative accurately."),
            item("Trapezoidal rule", """\int_{a}^{b}f\left(x\right)\,dx\approx\frac{h}{2}\left[y_{0}+2\sum_{i=1}^{n-1}y_{i}+y_{n}\right]""", "a,b,h,y,n", "Approximate area.", KnowledgeLevel.UG),
            item("Simpson rule", """\int_{a}^{b}f\left(x\right)\,dx\approx\frac{h}{3}\left[y_{0}+4\sum y_{2i-1}+2\sum y_{2i}+y_{n}\right]""", "a,b,h,y,n", "Approximate area with parabolas.", KnowledgeLevel.UG),
            item("Linear interpolation", """y=y_{0}+\left(x-x_{0}\right)\frac{y_{1}-y_{0}}{x_{1}-x_{0}}""", "x,y,x_0,x_1,y_0,y_1", "Estimate between known points."),
            item("Lagrange interpolation", """P\left(x\right)=\sum_{j=0}^{n}y_{j}\prod_{m\ne j}\frac{x-x_{m}}{x_{j}-x_{m}}""", "P,x,y,j,m,n", "Build polynomial through points.", KnowledgeLevel.PG),
            item("Absolute error", """E_{a}=\left|x_{\text{true}}-x_{\text{approx}}\right|""", "E,x", "Measure approximation error."),
            item("Relative error", """E_{r}=\frac{\left|x_{\text{true}}-x_{\text{approx}}\right|}{\left|x_{\text{true}}\right|}""", "E,x", "Measure scale-aware error."),
            item("Fixed point iteration", """x_{n+1}=g\left(x_{n}\right)""", "x,n,g", "Iterate toward a fixed point.", KnowledgeLevel.UG),
        )),
    )

    private fun additionalFormulaGroups() = listOf(
        FormulaGroup(FormulaCategory.Algebra, listOf(
            item("Logarithm product", """\log_{b}\left(xy\right)=\log_{b}x+\log_{b}y""", "b,x,y", "Split logarithm of a product."),
            item("Logarithm power", """\log_{b}\left(x^{r}\right)=r\log_{b}x""", "b,x,r", "Bring powers down in logarithms."),
            item("Change of base", """\log_{b}x=\frac{\ln x}{\ln b}""", "b,x", "Evaluate logs using another base."),
            item("Exponential inverse", """b^{\log_{b}x}=x""", "b,x", "Undo logarithms and exponentials."),
            item("Remainder theorem", """f\left(a\right)=\operatorname{rem}\left(f\left(x\right),x-a\right)""", "f,a,x", "Find polynomial remainders.", KnowledgeLevel.UG),
        )),
        FormulaGroup(FormulaCategory.Geometry, listOf(
            item("Cone surface area", """S=\pi r\left(r+\ell\right)""", "S,r,ell", "Find total surface area of a cone."),
            item("Cone slant height", """\ell=\sqrt{r^{2}+h^{2}}""", "ell,r,h", "Find cone slant height."),
            item("Frustum volume", """V=\frac{1}{3}\pi h\left(R^{2}+Rr+r^{2}\right)""", "V,h,R,r", "Find volume of a conical frustum.", KnowledgeLevel.UG),
            item("Regular polygon area", """A=\frac{1}{2}aP""", "A,a,P", "Find area from apothem and perimeter."),
            item("Interior angle sum", """S=\left(n-2\right)180^{\circ}""", "S,n", "Find total interior angle measure."),
        )),
        FormulaGroup(FormulaCategory.Trigonometry, listOf(
            item("Half-angle sine", """\sin^{2}\frac{\theta}{2}=\frac{1-\cos\theta}{2}""", "theta", "Use half-angle identities.", KnowledgeLevel.UG),
            item("Half-angle cosine", """\cos^{2}\frac{\theta}{2}=\frac{1+\cos\theta}{2}""", "theta", "Use half-angle identities.", KnowledgeLevel.UG),
            item("Product to sum sine cosine", """\sin A\cos B=\frac{1}{2}\left[\sin\left(A+B\right)+\sin\left(A-B\right)\right]""", "A,B", "Convert products to sums.", KnowledgeLevel.UG),
            item("Sum to product sine", """\sin A+\sin B=2\sin\frac{A+B}{2}\cos\frac{A-B}{2}""", "A,B", "Convert sums to products.", KnowledgeLevel.UG),
            item("Radians and degrees", """\theta_{\mathrm{rad}}=\frac{\pi}{180^{\circ}}\theta_{\mathrm{deg}}""", "theta", "Convert degrees to radians."),
        )),
        FormulaGroup(FormulaCategory.Calculus, listOf(
            item("Mean value theorem", """f'\left(c\right)=\frac{f\left(b\right)-f\left(a\right)}{b-a}""", "f,a,b,c", "Relate average and instantaneous rate.", KnowledgeLevel.UG),
            item("L'Hopital rule", """\lim_{x\to a}\frac{f\left(x\right)}{g\left(x\right)}=\lim_{x\to a}\frac{f'\left(x\right)}{g'\left(x\right)}""", "f,g,x,a", "Evaluate indeterminate limits.", KnowledgeLevel.UG),
            item("Arc length", """L=\int_{a}^{b}\sqrt{1+\left(f'\left(x\right)\right)^{2}}\,dx""", "L,a,b,f,x", "Find curve length.", KnowledgeLevel.UG),
            item("Surface of revolution", """S=2\pi\int_{a}^{b}f\left(x\right)\sqrt{1+\left(f'\left(x\right)\right)^{2}}\,dx""", "S,a,b,f,x", "Find surface area of revolution.", KnowledgeLevel.UG),
            item("Directional derivative", """D_{\mathbf{u}}f=\nabla f\cdot\mathbf{u}""", "D,u,f", "Find rate in a direction.", KnowledgeLevel.UG),
        )),
        FormulaGroup(FormulaCategory.DifferentialEquations, listOf(
            item("Homogeneous linear solution", """y=C_{1}e^{r_{1}x}+C_{2}e^{r_{2}x}""", "y,C,r,x", "Solve distinct-root second-order ODEs.", KnowledgeLevel.UG),
            item("Repeated-root solution", """y=\left(C_{1}+C_{2}x\right)e^{rx}""", "y,C,x,r", "Solve repeated-root second-order ODEs.", KnowledgeLevel.UG),
            item("Complex-root solution", """y=e^{\alpha x}\left(C_{1}\cos\beta x+C_{2}\sin\beta x\right)""", "y,alpha,beta,C,x", "Solve oscillatory ODEs.", KnowledgeLevel.UG),
            item("Laplace derivative", """\mathcal{L}\left\{f'\left(t\right)\right\}=sF\left(s\right)-f\left(0\right)""", "L,f,t,s,F", "Transform derivatives.", KnowledgeLevel.PG),
            item("Convolution theorem", """\mathcal{L}\left\{f*g\right\}=F\left(s\right)G\left(s\right)""", "L,f,g,F,G,s", "Solve forced linear systems.", KnowledgeLevel.PG),
        )),
        FormulaGroup(FormulaCategory.LinearAlgebra, listOf(
            item("Cramer's rule", """x_{i}=\frac{\det A_{i}}{\det A}""", "x,A,i", "Solve square systems.", KnowledgeLevel.UG),
            item("Cauchy-Schwarz", """\left|\mathbf{u}\cdot\mathbf{v}\right|\leq\left\lVert\mathbf{u}\right\rVert\left\lVert\mathbf{v}\right\rVert""", "u,v", "Bound dot products.", KnowledgeLevel.UG),
            item("Gram matrix", """G=X^{T}X""", "G,X", "Store pairwise inner products.", KnowledgeLevel.UG),
            item("QR factorization", """\mathbf{A}=\mathbf{Q}\mathbf{R}""", "A,Q,R", "Decompose into orthogonal and triangular factors.", KnowledgeLevel.PG),
            item("Singular value decomposition", """\mathbf{A}=\mathbf{U}\Sigma\mathbf{V}^{T}""", "A,U,Sigma,V", "Analyze matrix geometry.", KnowledgeLevel.PG),
        )),
        FormulaGroup(FormulaCategory.CoordinateGeometry, listOf(
            item("Parametric line 2D", """\left(x,y\right)=\left(x_{0},y_{0}\right)+t\left(a,b\right)""", "x,y,x_0,y_0,t,a,b", "Represent a 2D line parametrically.", KnowledgeLevel.UG),
            item("Line intercept form", """\frac{x}{a}+\frac{y}{b}=1""", "x,y,a,b", "Represent a line by intercepts."),
            item("Pair of lines", """ax^{2}+2hxy+by^{2}=0""", "a,h,b,x,y", "Represent homogeneous pair of lines.", KnowledgeLevel.UG),
            item("Eccentricity", """e=\frac{c}{a}""", "e,c,a", "Classify conic shape."),
            item("Polar conic", """r=\frac{\ell}{1+e\cos\theta}""", "r,ell,e,theta", "Represent conics in polar form.", KnowledgeLevel.UG),
        )),
        FormulaGroup(FormulaCategory.Vectors3D, listOf(
            item("Vector projection scalar", """\operatorname{comp}_{\mathbf{b}}\mathbf{a}=\frac{\mathbf{a}\cdot\mathbf{b}}{\left\lVert\mathbf{b}\right\rVert}""", "a,b", "Find scalar projection.", KnowledgeLevel.UG),
            item("Area parallelogram", """A=\left\lVert\mathbf{a}\times\mathbf{b}\right\rVert""", "A,a,b", "Find area from cross product.", KnowledgeLevel.UG),
            item("Area triangle 3D", """A=\frac{1}{2}\left\lVert\mathbf{a}\times\mathbf{b}\right\rVert""", "A,a,b", "Find triangle area in space.", KnowledgeLevel.UG),
            item("Line-plane intersection", """t=\frac{\mathbf{n}\cdot\left(\mathbf{p}_{0}-\mathbf{r}_{0}\right)}{\mathbf{n}\cdot\mathbf{v}}""", "t,n,p_0,r_0,v", "Intersect a parametric line with a plane.", KnowledgeLevel.UG),
            item("Dihedral angle", """\cos\theta=\frac{\mathbf{n}_{1}\cdot\mathbf{n}_{2}}{\left\lVert\mathbf{n}_{1}\right\rVert\left\lVert\mathbf{n}_{2}\right\rVert}""", "theta,n_1,n_2", "Find angle between planes.", KnowledgeLevel.UG),
        )),
        FormulaGroup(FormulaCategory.Probability, listOf(
            item("Chebyshev inequality", """P\left(\left|X-\mu\right|\geq k\sigma\right)\leq\frac{1}{k^{2}}""", "X,mu,k,sigma", "Bound probability far from mean.", KnowledgeLevel.UG),
            item("Moment generating function", """M_{X}\left(t\right)=E\left(e^{tX}\right)""", "M,X,t", "Encode moments.", KnowledgeLevel.PG),
            item("Covariance", """\operatorname{Cov}\left(X,Y\right)=E\left(XY\right)-E\left(X\right)E\left(Y\right)""", "X,Y", "Measure joint variation.", KnowledgeLevel.UG),
            item("Correlation", """\rho_{X,Y}=\frac{\operatorname{Cov}\left(X,Y\right)}{\sigma_{X}\sigma_{Y}}""", "rho,X,Y,sigma", "Normalize covariance.", KnowledgeLevel.UG),
            item("Law of total expectation", """E\left(X\right)=E\left(E\left(X\mid Y\right)\right)""", "X,Y", "Average conditional expectations.", KnowledgeLevel.PG),
        )),
        FormulaGroup(FormulaCategory.Statistics, listOf(
            item("Pooled variance", """s_{p}^{2}=\frac{\left(n_{1}-1\right)s_{1}^{2}+\left(n_{2}-1\right)s_{2}^{2}}{n_{1}+n_{2}-2}""", "s,n", "Combine two sample variances.", KnowledgeLevel.UG),
            item("Welch t statistic", """t=\frac{\bar{x}_{1}-\bar{x}_{2}}{\sqrt{\frac{s_{1}^{2}}{n_{1}}+\frac{s_{2}^{2}}{n_{2}}}}""", "t,x,s,n", "Compare two means without equal variance.", KnowledgeLevel.UG),
            item("ANOVA F statistic", """F=\frac{MS_{\text{between}}}{MS_{\text{within}}}""", "F,MS", "Compare group means.", KnowledgeLevel.UG),
            item("Logistic regression", """\log\frac{p}{1-p}=\beta_{0}+\beta_{1}x""", "p,beta,x", "Model binary response.", KnowledgeLevel.PG),
            item("AIC", """AIC=2k-2\ln\left(\hat{L}\right)""", "AIC,k,L", "Compare statistical models.", KnowledgeLevel.PG),
        )),
        FormulaGroup(FormulaCategory.Distributions, listOf(
            item("Beta PDF", """f\left(x\right)=\frac{x^{\alpha-1}\left(1-x\right)^{\beta-1}}{B\left(\alpha,\beta\right)}""", "f,x,alpha,beta", "Model probabilities and proportions.", KnowledgeLevel.PG),
            item("Geometric PMF", """P\left(X=k\right)=\left(1-p\right)^{k-1}p""", "X,k,p", "Model first success trial."),
            item("Negative binomial PMF", """P\left(X=k\right)=\binom{k-1}{r-1}p^{r}\left(1-p\right)^{k-r}""", "X,k,r,p", "Model trials until r successes.", KnowledgeLevel.UG),
            item("Chi-square PDF", """f\left(x\right)=\frac{1}{2^{\frac{k}{2}}\Gamma\left(\frac{k}{2}\right)}x^{\frac{k}{2}-1}e^{-\frac{x}{2}}""", "f,x,k", "Model variance-related statistics.", KnowledgeLevel.UG),
            item("F statistic", """F=\frac{\frac{U_{1}}{d_{1}}}{\frac{U_{2}}{d_{2}}}""", "F,U,d", "Model ratio of scaled chi-square variables.", KnowledgeLevel.PG),
        )),
        FormulaGroup(FormulaCategory.NumberTheory, listOf(
            item("Mobius inversion", """g\left(n\right)=\sum_{d\mid n}f\left(d\right)\Rightarrow f\left(n\right)=\sum_{d\mid n}\mu\left(d\right)g\left(\frac{n}{d}\right)""", "f,g,n,d,mu", "Invert divisor sums.", KnowledgeLevel.PG),
            item("Euler product", """\zeta\left(s\right)=\prod_{p}\frac{1}{1-p^{-s}}""", "zeta,s,p", "Connect primes and zeta function.", KnowledgeLevel.PG),
            item("Wilson theorem", """\left(p-1\right)!\equiv-1\pmod{p}""", "p", "Characterize primes.", KnowledgeLevel.UG),
            item("Divisor count", """d\left(n\right)=\prod_{i=1}^{k}\left(a_{i}+1\right)""", "d,n,a,i,k", "Count positive divisors."),
            item("Divisor sum", """\sigma\left(n\right)=\prod_{i=1}^{k}\frac{p_{i}^{a_{i}+1}-1}{p_{i}-1}""", "sigma,n,p,a,i,k", "Sum positive divisors.", KnowledgeLevel.UG),
        )),
        FormulaGroup(FormulaCategory.Combinatorics, listOf(
            item("Bell recurrence", """B_{n+1}=\sum_{k=0}^{n}\binom{n}{k}B_{k}""", "B,n,k", "Count set partitions.", KnowledgeLevel.PG),
            item("Stirling second kind", """S\left(n,k\right)=kS\left(n-1,k\right)+S\left(n-1,k-1\right)""", "S,n,k", "Count partitions into k blocks.", KnowledgeLevel.UG),
            item("Vandermonde identity", """\sum_{k}\binom{r}{k}\binom{s}{n-k}=\binom{r+s}{n}""", "r,s,n,k", "Combine binomial choices.", KnowledgeLevel.UG),
            item("Burnside lemma", """\operatorname{orbits}_{G}\left(X\right)=\frac{1}{\left|G\right|}\sum_{g\in G}\left|X^{g}\right|""", "X,G,g", "Count up to symmetry.", KnowledgeLevel.PG),
            item("Euler graph formula", """\left|V\right|-\left|E\right|+\left|F\right|=2""", "V,E,F", "Relate planar graph faces.", KnowledgeLevel.UG),
        )),
        FormulaGroup(FormulaCategory.ComplexNumbers, listOf(
            item("Complex division", """\frac{a+b\mathrm{i}}{c+d\mathrm{i}}=\frac{\left(a+b\mathrm{i}\right)\left(c-d\mathrm{i}\right)}{c^{2}+d^{2}}""", "a,b,c,d", "Divide complex numbers."),
            item("Root of unity", """\omega_{k}=e^{\frac{2\pi\mathrm{i}k}{n}}""", "omega,k,n", "Find nth roots of unity.", KnowledgeLevel.UG),
            item("Magnitude product", """\left|zw\right|=\left|z\right|\left|w\right|""", "z,w", "Multiply magnitudes."),
            item("Argument product", """\arg\left(zw\right)=\arg z+\arg w""", "z,w", "Add angles under multiplication."),
            item("Cauchy-Riemann", """u_{x}=v_{y},\quad u_{y}=-v_{x}""", "u,v,x,y", "Test complex differentiability.", KnowledgeLevel.PG),
        )),
        FormulaGroup(FormulaCategory.NumericalMethods, listOf(
            item("Newton error", """e_{n+1}\approx\frac{f''\left(\alpha\right)}{2f'\left(\alpha\right)}e_{n}^{2}""", "e,n,f,alpha", "Describe quadratic convergence.", KnowledgeLevel.PG),
            item("Composite midpoint rule", """\int_{a}^{b}f\left(x\right)\,dx\approx h\sum_{i=1}^{n}f\left(\frac{x_{i-1}+x_{i}}{2}\right)""", "a,b,h,f,x,i,n", "Approximate area with midpoints.", KnowledgeLevel.UG),
            item("Newton interpolation", """P\left(x\right)=a_{0}+a_{1}\left(x-x_{0}\right)+\cdots+a_{n}\prod_{j=0}^{n-1}\left(x-x_{j}\right)""", "P,x,a,j,n", "Build divided-difference interpolants.", KnowledgeLevel.PG),
            item("Condition number", """\kappa\left(A\right)=\left\lVert A\right\rVert\left\lVert A^{-1}\right\rVert""", "kappa,A", "Measure sensitivity of linear systems.", KnowledgeLevel.PG),
            item("Gradient descent", """\mathbf{x}_{k+1}=\mathbf{x}_{k}-\eta\nabla f\left(\mathbf{x}_{k}\right)""", "x,k,eta,f", "Minimize functions iteratively.", KnowledgeLevel.PG),
        )),
    )

    private data class McqSeed(
        val prompt: String,
        val choices: List<String>,
        val answerIndex: Int,
        val explanation: String,
        val category: String,
    )

    private fun generatedMcqs(): List<McqQuestion> {
        val seeds = listOf(
            QuizSubject.Maths to listOf(
                QuizLevel.Basic to listOf(
                    mcq("What is the value of 7 × 8?", listOf("54", "56", "58", "64"), 1, "7 groups of 8 make 56.", "Arithmetic"),
                    mcq("Which number is prime?", listOf("21", "27", "29", "39"), 2, "29 has no positive divisors except 1 and 29.", "Number Theory"),
                    mcq("The area of a rectangle is:", listOf("length + width", "2(length + width)", "length × width", "length ÷ width"), 2, "Area counts square units across length and width.", "Geometry"),
                    mcq("If x + 5 = 12, x equals:", listOf("5", "7", "12", "17"), 1, "Subtract 5 from both sides.", "Algebra"),
                    mcq("A right angle measures:", listOf("45°", "60°", "90°", "180°"), 2, "A right angle is exactly 90 degrees.", "Geometry"),
                    mcq("The median of 2, 5, 9 is:", listOf("2", "5", "7", "9"), 1, "The middle ordered value is 5.", "Statistics"),
                ),
                QuizLevel.Intermediate to listOf(
                    mcq("The derivative of x² is:", listOf("x", "2x", "x³", "2"), 1, "Use the power rule d(x^n)/dx = nx^(n-1).", "Calculus"),
                    mcq("For a geometric sequence with a=3 and r=2, a₄ is:", listOf("12", "18", "24", "48"), 2, "a4 = 3 × 2^3 = 24.", "Sequences"),
                    mcq("sin²θ + cos²θ equals:", listOf("0", "1", "tan θ", "sec θ"), 1, "This is the main Pythagorean trigonometric identity.", "Trigonometry"),
                    mcq("The roots of x² - 5x + 6 are:", listOf("1 and 6", "2 and 3", "-2 and -3", "0 and 6"), 1, "x² - 5x + 6 factors as (x - 2)(x - 3).", "Algebra"),
                    mcq("The probability of an impossible event is:", listOf("0", "0.5", "1", "Undefined"), 0, "Impossible events never occur, so their probability is 0.", "Probability"),
                    mcq("The slope of a horizontal line is:", listOf("0", "1", "Undefined", "Negative"), 0, "Horizontal lines have no vertical change.", "Coordinate Geometry"),
                ),
                QuizLevel.Advanced to listOf(
                    mcq("If det(A)=0, matrix A is:", listOf("Invertible", "Singular", "Orthogonal", "Identity"), 1, "A square matrix has an inverse only when its determinant is nonzero.", "Linear Algebra"),
                    mcq("The gradient points in the direction of:", listOf("Least decrease", "Steepest increase", "Zero curvature", "Constant area"), 1, "The gradient is the direction of maximum directional derivative.", "Vector Calculus"),
                    mcq("Bayes theorem computes:", listOf("Prior only", "Posterior probability", "Derivative", "Eigenvalue"), 1, "Bayes theorem updates a probability after observing evidence.", "Probability"),
                    mcq("A Taylor series uses derivatives at:", listOf("Only infinity", "A chosen center", "Only zero", "Random points"), 1, "Taylor polynomials are built from derivatives at a center point.", "Calculus"),
                    mcq("Rank-nullity applies to:", listOf("Matrices / linear maps", "Triangles only", "Chemical bonds", "Planet orbits"), 0, "It relates rank and nullity of a linear map.", "Linear Algebra"),
                    mcq("A complex number z=a+bi has modulus:", listOf("a+b", "a-b", "sqrt(a²+b²)", "ab"), 2, "The modulus is Euclidean distance from the origin.", "Complex Numbers"),
                ),
            ),
            QuizSubject.Physics to listOf(
                QuizLevel.Basic to listOf(
                    mcq("SI unit of force is:", listOf("Joule", "Newton", "Watt", "Pascal"), 1, "Force is measured in newtons.", "Mechanics"),
                    mcq("Speed equals:", listOf("distance × time", "distance ÷ time", "time ÷ distance", "mass × acceleration"), 1, "Speed is distance traveled per unit time.", "Mechanics"),
                    mcq("The unit of electric current is:", listOf("Volt", "Ohm", "Ampere", "Coulomb"), 2, "Current is measured in amperes.", "Electricity"),
                    mcq("Light travels fastest in:", listOf("Water", "Glass", "Vacuum", "Air only"), 2, "The speed of light is maximum in vacuum.", "Optics"),
                    mcq("A push or pull is called:", listOf("Energy", "Power", "Force", "Pressure"), 2, "Force is a push or pull.", "Mechanics"),
                    mcq("The unit of energy is:", listOf("Joule", "Newton", "Tesla", "Hertz"), 0, "Energy is measured in joules.", "Energy"),
                ),
                QuizLevel.Intermediate to listOf(
                    mcq("Newton's second law is:", listOf("F=ma", "E=mc²", "V=IR", "pV=nRT"), 0, "Net force equals mass times acceleration.", "Mechanics"),
                    mcq("Ohm's law is:", listOf("V=IR", "F=ma", "P=mv", "Q=mcΔT"), 0, "Voltage equals current times resistance.", "Electricity"),
                    mcq("Momentum is:", listOf("mv", "ma", "mgh", "IR"), 0, "Linear momentum is mass times velocity.", "Mechanics"),
                    mcq("Frequency and period satisfy:", listOf("f=T", "f=1/T", "f=T²", "f=2T"), 1, "Frequency is reciprocal of period.", "Waves"),
                    mcq("Kinetic energy is:", listOf("mgh", "1/2 mv²", "mcΔT", "VIt"), 1, "Classical kinetic energy is one half mass times speed squared.", "Energy"),
                    mcq("Pressure equals:", listOf("force/area", "area/force", "mass × area", "power/time"), 0, "Pressure is normal force per unit area.", "Fluids"),
                ),
                QuizLevel.Advanced to listOf(
                    mcq("de Broglie wavelength is:", listOf("h/p", "hp", "pc", "mc²"), 0, "Matter wavelength equals Planck's constant divided by momentum.", "Quantum"),
                    mcq("Lorentz factor is:", listOf("1/sqrt(1-v²/c²)", "sqrt(1-v/c)", "v/c", "mc²"), 0, "Special relativity uses gamma = 1/sqrt(1-v²/c²).", "Relativity"),
                    mcq("Gauss law relates electric flux to:", listOf("enclosed charge", "mass", "temperature", "frequency"), 0, "Electric flux through a closed surface depends on enclosed charge.", "Electromagnetism"),
                    mcq("Escape velocity varies as:", listOf("sqrt(2GM/R)", "GM/R", "R/GM", "2πR/T"), 0, "Escape speed from a spherical body is sqrt(2GM/R).", "Gravitation"),
                    mcq("The Schrödinger equation evolves:", listOf("wavefunction", "resistance", "pressure", "entropy only"), 0, "Quantum states are represented by wavefunctions.", "Quantum"),
                    mcq("In SHM, acceleration is proportional to:", listOf("displacement opposite direction", "velocity", "mass only", "time squared"), 0, "SHM satisfies a = -ω²x.", "Oscillations"),
                ),
            ),
            QuizSubject.Chemistry to listOf(
                QuizLevel.Basic to listOf(
                    mcq("Atomic number equals number of:", listOf("Neutrons", "Protons", "Electrons plus neutrons", "Moles"), 1, "Atomic number is the proton count.", "Atoms"),
                    mcq("Water formula is:", listOf("CO₂", "H₂O", "O₂", "NaCl"), 1, "Water contains two hydrogen atoms and one oxygen atom.", "Compounds"),
                    mcq("pH less than 7 is:", listOf("Acidic", "Neutral", "Basic", "Metallic"), 0, "Acids have pH below 7.", "Acids and Bases"),
                    mcq("NaCl is commonly called:", listOf("Sugar", "Salt", "Lime", "Alcohol"), 1, "Sodium chloride is table salt.", "Compounds"),
                    mcq("Electrons have charge:", listOf("Positive", "Negative", "Neutral", "Variable only"), 1, "Electrons are negatively charged.", "Atoms"),
                    mcq("The center of an atom is the:", listOf("Shell", "Nucleus", "Orbital", "Bond"), 1, "Protons and neutrons are in the nucleus.", "Atoms"),
                ),
                QuizLevel.Intermediate to listOf(
                    mcq("Avogadro number is approximately:", listOf("6.02×10²³", "3.14", "9.8", "1.6×10⁻¹⁹"), 0, "One mole contains about 6.02×10^23 particles.", "Moles"),
                    mcq("Ionic bond forms by:", listOf("electron transfer", "electron sharing only", "proton sharing", "neutron decay"), 0, "Ionic bonding involves transfer of electrons.", "Bonding"),
                    mcq("Covalent bond forms by:", listOf("electron sharing", "neutron sharing", "mass transfer", "heat flow"), 0, "Covalent bonding shares electron pairs.", "Bonding"),
                    mcq("Oxidation is:", listOf("gain of electrons", "loss of electrons", "gain of neutrons", "loss of protons"), 1, "Oxidation is loss of electrons.", "Redox"),
                    mcq("Molarity is:", listOf("moles/liter", "grams/liter only", "liters/mole", "moles × liters"), 0, "Molarity is amount of solute per liter of solution.", "Solutions"),
                    mcq("Ideal gas law is:", listOf("PV=nRT", "F=ma", "V=IR", "E=hf"), 0, "PV=nRT relates pressure, volume, moles and temperature.", "Gases"),
                ),
                QuizLevel.Advanced to listOf(
                    mcq("Rate constant units depend on:", listOf("reaction order", "color", "atomic mass only", "container shape"), 0, "Rate-law units change with overall order.", "Kinetics"),
                    mcq("Le Chatelier principle predicts response to:", listOf("stress on equilibrium", "atomic number", "nuclear spin", "period only"), 0, "Equilibria shift to oppose imposed changes.", "Equilibrium"),
                    mcq("Gibbs spontaneity criterion at constant T,P is:", listOf("ΔG<0", "ΔG>0", "ΔH=0", "pH=7"), 0, "Negative Gibbs free energy indicates spontaneous change.", "Thermodynamics"),
                    mcq("Henderson-Hasselbalch equation uses:", listOf("pH and pKa", "force and mass", "voltage and current", "genes and alleles"), 0, "It relates pH, pKa, acid and conjugate base ratio.", "Acids and Bases"),
                    mcq("A catalyst changes:", listOf("activation energy", "equilibrium constant always", "atomic number", "mass conservation"), 0, "Catalysts lower activation energy without being consumed.", "Kinetics"),
                    mcq("Crystal field splitting is used for:", listOf("transition metal complexes", "ideal gases", "carbohydrates only", "nuclear fission only"), 0, "Crystal field theory explains d-orbital splitting.", "Inorganic"),
                ),
            ),
            QuizSubject.Biology to listOf(
                QuizLevel.Basic to listOf(
                    mcq("Basic unit of life is:", listOf("Atom", "Cell", "Organ", "Tissue only"), 1, "Cells are the smallest living units.", "Cells"),
                    mcq("DNA stores:", listOf("genetic information", "oxygen only", "glucose only", "water only"), 0, "DNA stores hereditary instructions.", "Genetics"),
                    mcq("Photosynthesis occurs mainly in:", listOf("chloroplasts", "mitochondria", "nucleus", "ribosomes"), 0, "Chloroplasts contain chlorophyll for photosynthesis.", "Plants"),
                    mcq("Humans breathe in mostly:", listOf("oxygen for respiration", "helium", "chlorine", "ozone only"), 0, "Oxygen is used in cellular respiration.", "Respiration"),
                    mcq("Proteins are made of:", listOf("amino acids", "fatty acids only", "nucleotides only", "salts"), 0, "Amino acids are protein monomers.", "Biochemistry"),
                    mcq("Blood is pumped by the:", listOf("heart", "lung", "kidney", "liver"), 0, "The heart drives blood circulation.", "Human Biology"),
                ),
                QuizLevel.Intermediate to listOf(
                    mcq("Mitosis produces:", listOf("two identical cells", "four gametes", "only sperm", "no nuclei"), 0, "Mitosis creates genetically identical daughter cells.", "Cell Division"),
                    mcq("Meiosis reduces chromosome number by:", listOf("half", "double", "zero", "triple"), 0, "Meiosis forms haploid gametes.", "Cell Division"),
                    mcq("mRNA is translated at:", listOf("ribosome", "chloroplast only", "cell wall", "vacuole"), 0, "Ribosomes synthesize proteins from mRNA.", "Genetics"),
                    mcq("Enzymes are mostly:", listOf("biological catalysts", "inert gases", "minerals only", "lipids only"), 0, "Enzymes speed reactions in living systems.", "Biochemistry"),
                    mcq("Natural selection acts on:", listOf("heritable variation", "only learned habits", "rocks", "water"), 0, "Selection changes frequencies of inherited traits.", "Evolution"),
                    mcq("Xylem transports mainly:", listOf("water", "sugars", "proteins", "DNA"), 0, "Xylem carries water and minerals.", "Plants"),
                ),
                QuizLevel.Advanced to listOf(
                    mcq("PCR amplifies:", listOf("DNA", "lipids", "starch", "ions only"), 0, "Polymerase chain reaction copies DNA segments.", "Biotechnology"),
                    mcq("Hardy-Weinberg equilibrium assumes:", listOf("no selection", "rapid mutation only", "tiny population", "nonrandom mating"), 0, "No selection is one of the equilibrium assumptions.", "Evolution"),
                    mcq("ATP synthase is driven by:", listOf("proton gradient", "DNA gradient", "lipid gradient", "oxygen gradient only"), 0, "Chemiosmosis powers ATP synthase.", "Cellular Respiration"),
                    mcq("CRISPR-Cas systems are used for:", listOf("genome editing", "protein digestion only", "blood clotting", "photosynthesis only"), 0, "CRISPR can target and edit DNA sequences.", "Biotechnology"),
                    mcq("Allosteric regulation changes enzyme activity by:", listOf("binding away from active site", "destroying DNA", "removing water only", "changing gravity"), 0, "Allosteric ligands bind regulatory sites.", "Biochemistry"),
                    mcq("A phylogenetic tree represents:", listOf("evolutionary relationships", "blood pressure", "pH scale", "cell size only"), 0, "Phylogenies model shared ancestry.", "Evolution"),
                ),
            ),
            QuizSubject.AstroPhysics to listOf(
                QuizLevel.Basic to listOf(
                    mcq("The Sun is a:", listOf("star", "planet", "comet", "galaxy"), 0, "The Sun is our nearest star.", "Stars"),
                    mcq("Earth orbits the:", listOf("Moon", "Sun", "Mars", "Milky Way center only"), 1, "Earth is a planet orbiting the Sun.", "Solar System"),
                    mcq("A galaxy contains many:", listOf("stars", "cells", "atoms only", "enzymes"), 0, "Galaxies are large systems of stars, gas and dust.", "Galaxies"),
                    mcq("The Moon is Earth's:", listOf("natural satellite", "star", "galaxy", "asteroid belt"), 0, "The Moon orbits Earth naturally.", "Solar System"),
                    mcq("A light-year measures:", listOf("distance", "time only", "mass", "temperature"), 0, "A light-year is the distance light travels in one year.", "Cosmic Scale"),
                    mcq("Gravity causes planets to:", listOf("orbit", "photosynthesize", "ionize always", "divide cells"), 0, "Gravity provides centripetal attraction for orbits.", "Gravity"),
                ),
                QuizLevel.Intermediate to listOf(
                    mcq("Hubble law relates velocity to:", listOf("distance", "mass", "temperature only", "charge"), 0, "More distant galaxies recede faster on average.", "Cosmology"),
                    mcq("A black hole escape speed is:", listOf("greater than light at event horizon", "zero", "same as sound", "unrelated to gravity"), 0, "At the event horizon, escape requires light speed.", "Black Holes"),
                    mcq("Stellar parallax measures:", listOf("distance", "mass only", "composition only", "age only"), 0, "Parallax uses apparent shift from Earth's orbit.", "Observation"),
                    mcq("Main sequence stars fuse:", listOf("hydrogen", "iron", "silicon only", "carbon dioxide"), 0, "Hydrogen fusion powers main-sequence stars.", "Stars"),
                    mcq("Redshift means wavelength is:", listOf("stretched", "shortened", "zero", "unchanged always"), 0, "Redshift shifts light toward longer wavelengths.", "Cosmology"),
                    mcq("Kepler's third law connects period with:", listOf("orbital size", "color", "pH", "charge only"), 0, "Orbital period depends on semi-major axis.", "Orbits"),
                ),
                QuizLevel.Advanced to listOf(
                    mcq("Schwarzschild radius is proportional to:", listOf("mass", "temperature only", "luminosity only", "metallicity only"), 0, "Rs = 2GM/c^2 grows with mass.", "Black Holes"),
                    mcq("The CMB is evidence for:", listOf("Big Bang", "plate tectonics", "photosynthesis", "chemical bonding"), 0, "Cosmic microwave background is relic radiation.", "Cosmology"),
                    mcq("Hydrostatic equilibrium balances gravity with:", listOf("pressure gradient", "electric current only", "DNA repair", "pH"), 0, "Stars balance inward gravity with outward pressure.", "Stars"),
                    mcq("Type Ia supernovae are useful as:", listOf("standard candles", "amino acids", "catalysts", "enzymes"), 0, "Their predictable luminosity helps measure distances.", "Cosmology"),
                    mcq("Dark matter is inferred mainly from:", listOf("gravitational effects", "taste", "pH", "atomic number"), 0, "Rotation curves and lensing reveal unseen mass.", "Cosmology"),
                    mcq("Gravitational lensing bends:", listOf("light", "sound in air only", "chemical bonds", "DNA"), 0, "Mass curves spacetime and deflects light.", "Relativity"),
                ),
            ),
            QuizSubject.IQLabs to listOf(
                QuizLevel.Basic to listOf(
                    mcq("Next number: 2, 4, 6, 8, ?", listOf("9", "10", "12", "16"), 1, "The pattern adds 2 each time.", "Sequences"),
                    mcq("Odd one out:", listOf("Square", "Triangle", "Circle", "Apple"), 3, "Apple is not a geometric shape.", "Classification"),
                    mcq("If all cats are animals, a cat is:", listOf("animal", "plant", "mineral", "number"), 0, "The statement directly includes cats in animals.", "Logic"),
                    mcq("Mirror of left is:", listOf("right", "up", "down", "same always"), 0, "Left and right swap under mirror reflection.", "Spatial"),
                    mcq("Which is largest?", listOf("0.2", "0.05", "0.5", "0.11"), 2, "0.5 is the largest decimal.", "Numeracy"),
                    mcq("A pair is how many?", listOf("1", "2", "3", "4"), 1, "A pair means two.", "Verbal"),
                ),
                QuizLevel.Intermediate to listOf(
                    mcq("Next number: 3, 6, 12, 24, ?", listOf("30", "36", "48", "60"), 2, "The pattern doubles.", "Sequences"),
                    mcq("Book is to reading as fork is to:", listOf("writing", "eating", "driving", "sleeping"), 1, "A fork is used for eating.", "Analogies"),
                    mcq("If A=1, C=3, Z=26, then DOG sums to:", listOf("24", "26", "28", "30"), 1, "D=4, O=15, G=7, total 26.", "Coding"),
                    mcq("Complete: AB, CD, EF, ?", listOf("GH", "FG", "HI", "JK"), 0, "Pairs advance by two letters.", "Patterns"),
                    mcq("Which number is missing: 1, 4, 9, 16, ?", listOf("20", "24", "25", "36"), 2, "These are square numbers.", "Sequences"),
                    mcq("Opposite of scarce is:", listOf("rare", "many", "abundant", "empty"), 2, "Abundant means plentiful.", "Verbal"),
                ),
                QuizLevel.Advanced to listOf(
                    mcq("Next number: 1, 1, 2, 3, 5, 8, ?", listOf("11", "12", "13", "15"), 2, "This is the Fibonacci pattern.", "Sequences"),
                    mcq("If some A are B and all B are C, then:", listOf("some A are C", "all A are C", "no A are C", "all C are A"), 0, "The A that are B must also be C.", "Logic"),
                    mcq("Next number: 2, 6, 12, 20, 30, ?", listOf("36", "40", "42", "44"), 2, "Differences are 4, 6, 8, 10, so next is 42.", "Sequences"),
                    mcq("A cube has how many faces?", listOf("4", "6", "8", "12"), 1, "A cube has six square faces.", "Spatial"),
                    mcq("If CODE becomes DPEF, then MATH becomes:", listOf("NBUG", "NBUI", "LZSG", "MZUI"), 1, "Each letter shifts forward by one.", "Coding"),
                    mcq("Choose the necessary condition for a square:", listOf("four equal sides and right angles", "three sides", "circle center", "one parallel side"), 0, "A square has four equal sides and four right angles.", "Logic"),
                ),
            ),
        )
        return seeds.flatMap { (subject, levelGroups) ->
            levelGroups.flatMap { (quizLevel, questions) ->
                questions.mapIndexed { index, seed ->
                    McqQuestion(
                        id = "mcq-${subject.name}-${quizLevel.name}-${seed.category}-${index}".slug(),
                        topic = when (subject) {
                            QuizSubject.Maths -> when (seed.category) {
                                "Calculus" -> KnowledgeTopic.Calculus
                                "Geometry", "Coordinate Geometry", "Trigonometry", "Vector Calculus", "Complex Numbers" -> KnowledgeTopic.Geometry
                                "Statistics" -> KnowledgeTopic.Statistics
                                "Probability" -> KnowledgeTopic.Probability
                                else -> KnowledgeTopic.Algebra
                            }
                            else -> KnowledgeTopic.Algebra
                        },
                        level = when (quizLevel) {
                            QuizLevel.Basic -> KnowledgeLevel.School
                            QuizLevel.Intermediate -> KnowledgeLevel.UG
                            QuizLevel.Advanced -> KnowledgeLevel.PG
                        },
                        prompt = seed.prompt,
                        choices = seed.choices,
                        answerIndex = seed.answerIndex,
                        explanation = seed.explanation,
                        difficulty = quizLevel.difficulty,
                        subject = subject,
                        quizLevel = quizLevel,
                        category = seed.category,
                    )
                }
            }
        }
    }

    private fun mcq(prompt: String, choices: List<String>, answerIndex: Int, explanation: String, category: String) =
        McqSeed(prompt, choices, answerIndex, explanation, category)
}

private fun String.slug(): String = lowercase()
    .replace(Regex("[^a-z0-9]+"), "-")
    .trim('-')
