package com.indianservers.aiexplorer.learning.ncert

import com.indianservers.aiexplorer.curriculum.*
import com.indianservers.aiexplorer.curriculum.production.*

object MathematicsNcertProductionContent {
    private const val chapterId = "cbse-2026-c9-mathematics-c3"
    private const val topicId = "$chapterId-core"
    private fun q(id: String, type: CurriculumQuestionType, competency: CompetencyType, prompt: String, answer: String, explanation: String, error: String, section: String = "patterns") = CurriculumQuestion("math-seq-$id", SchoolSubject.MATHEMATICS, SchoolClassLevel.CLASS_9, chapterId, listOf(topicId), competency, QuestionDifficulty.STANDARD, type, prompt, answer = AnswerDefinition(answer), explanation = explanation, commonErrorTags = listOf(error), remediationSectionId = section)

    val sequencesAndProgressions = SubjectOwnedCurriculumChapter(
        id = "math-ncert-c9-sequences-production", curriculumNodeId = chapterId, subject = SchoolSubject.MATHEMATICS, classLevel = SchoolClassLevel.CLASS_9,
        officialChapterTitle = "Sequences and Progressions", unitTitle = "Algebra", estimatedMinutes = 95, route = "mathematics/ncert/9/sequences-and-progressions",
        topicIds = listOf(topicId),
        learningObjectives = listOf("Distinguish a sequence from an unordered collection.", "Describe term-to-term and position-to-term rules.", "Recognise an arithmetic progression and calculate an indicated term.", "Use tables, graphs and context to justify a pattern rule."),
        prerequisites = listOf("Integer operations", "Substitution in algebraic expressions", "Plotting ordered pairs"),
        explanationSections = linkedMapOf(
            "patterns" to "A sequence is an ordered list. Each term has both a value and a position. A term-to-term rule explains how to move to the next value, while a position-to-term rule calculates a value directly from its term number.",
            "arithmetic" to "An arithmetic progression has a constant difference d between consecutive terms. Starting from first term a, reaching term n requires n−1 equal moves, giving aₙ = a + (n−1)d. A negative difference creates a decreasing progression.",
            "representations" to "A table pairs n with aₙ. Plotting these pairs gives discrete points; for an arithmetic progression they lie on a straight-line pattern, but values between integer term numbers are not sequence terms.",
            "applications" to "Arithmetic progressions model quantities changing by the same amount per step, such as rows of seats or a fixed weekly saving increase. Always identify what position 1 represents before using the formula."
        ),
        keyTerms = linkedMapOf("sequence" to "An ordered list of terms.", "term" to "A value at a specified position.", "common difference" to "The constant change aₙ₊₁−aₙ in an arithmetic progression.", "nth term" to "A position-to-term expression for aₙ."),
        workedExamples = listOf(
            WorkedExample("Find term 18 of 7, 11, 15, …", listOf("a = 7 and d = 4.", "Use aₙ = a + (n−1)d.", "a₁₈ = 7 + 17×4 = 75."), "75", "Check: term 2 is 7+4=11, so position counting is consistent."),
            WorkedExample("Is 42 a term of 6, 10, 14, …?", listOf("Solve 42 = 6 + (n−1)4.", "36 = 4(n−1), so n−1 = 9.", "n = 10, a positive integer position."), "Yes, it is term 10.", "Substitution gives 6+9×4=42.")
        ),
        diagrams = listOf(CurriculumDiagramSpecification("math-c9-sequence-table-graph", SchoolSubject.MATHEMATICS, SchoolClassLevel.CLASS_9, topicId, "Arithmetic sequence: table and discrete graph", DiagramType.GRAPH, listOf(DiagramLabelRequirement("axis-n", "term number n", "horizontal discrete position"), DiagramLabelRequirement("axis-an", "term value aₙ", "vertical value"), DiagramLabelRequirement("difference", "equal rise d", "show invariant change")), "Connect symbolic, tabular and graphical representations.", listOf("Plot only integer n values.", "Axes begin with a visible break if zero is omitted.", "Do not imply that joining points creates additional sequence terms."), DiagramInteractionMode.LABEL_HIDE_AND_QUIZ, "A table and coordinate plot for terms 3, 7, 11, 15; equal vertical changes of four are marked between successive integer positions.", ScientificReviewStatus.VERIFIED)),
        formulaLinks = listOf(CurriculumFormulaLink("algebra-arithmetic-sequence", "Required nth-term relation", mapOf("aₙ" to "term at position n", "a" to "first term", "d" to "common difference", "n" to "positive integer term number"), mapOf("aₙ" to "same unit as term values", "a" to "same unit as term values", "d" to "change per term", "n" to "dimensionless"), listOf("The difference between consecutive terms is constant.", "n is a positive integer."), listOf("d=(aₙ−a)/(n−1)", "a=aₙ−(n−1)d"), listOf("Using nd instead of (n−1)d.", "Reversing the sign of a negative difference."), "mathematics/formulas/algebra-arithmetic-sequence")),
        activities = listOf("Build a staircase with counters using a fixed number of new counters per step; record step number and total, then compare two proposed nth-term rules.", "Use the simple-case check n=1 and n=2 to test any proposed formula before generalising."),
        practicals = emptyList(),
        misconceptions = linkedMapOf("Every visible pattern is arithmetic." to "Arithmetic specifically requires one constant consecutive difference.", "The graph contains every point on the line." to "A sequence is defined only at integer term positions.", "Term n uses n jumps." to "From term 1 to term n there are n−1 jumps."),
        questions = listOf(
            q("mcq", CurriculumQuestionType.MULTIPLE_CHOICE, CompetencyType.UNDERSTANDING, "Which list is arithmetic? A: 2,4,8,16; B: 9,6,3,0; C: 1,4,9,16", "B", "Its consecutive difference is always −3.", "property misunderstanding"),
            q("numeric", CurriculumQuestionType.NUMERIC_INPUT, CompetencyType.APPLYING, "Find term 25 of 12, 17, 22, …", "132", "12+24×5=132.", "off-by-one", "arithmetic"),
            q("table", CurriculumQuestionType.TABLE_INTERPRETATION, CompetencyType.ANALYSING, "A table gives a₁=20, a₂=17, a₃=14. Predict a₁₂ and justify the invariant.", "-13", "The common difference is −3; 20+11(−3)=−13.", "sign error", "representations"),
            q("case", CurriculumQuestionType.CASE_BASED, CompetencyType.APPLYING, "A hall has 18 seats in row 1 and four more in every next row. How many seats are in row 15?", "74", "18+14×4=74.", "formula selection", "applications"),
            q("proof", CurriculumQuestionType.PROOF_DERIVATION, CompetencyType.ANALYSING, "Explain why the difference of corresponding terms of two arithmetic progressions with the same d is constant.", "Subtracting [a+(n−1)d]−[b+(n−1)d] leaves a−b.", "The identical variable parts cancel for every n.", "algebraic manipulation", "arithmetic")
        ),
        revision = CurriculumRevisionResource(listOf("Order and position are essential.", "Arithmetic means constant consecutive difference.", "Term n is a+(n−1)d.", "Use n=1 as a fast formula check."), listOf("aₙ=a+(n−1)d"), listOf("Discrete (n,aₙ) points form a straight-line pattern for an arithmetic progression."), listOf("term number versus term value", "n jumps versus n−1 jumps", "negative term versus negative difference"), listOf("math-seq-mcq", "math-seq-numeric", "math-seq-table")),
        relationships = listOf("Introduction to Polynomials", "Linear Equations in Two Variables", "Class X Arithmetic Progressions"),
        requirements = CurriculumCompletionRequirements(true, true, true, true, true, false, true, true, true), reviewStatus = ScientificReviewStatus.VERIFIED
    )

    val chapters = listOf(sequencesAndProgressions)
}
