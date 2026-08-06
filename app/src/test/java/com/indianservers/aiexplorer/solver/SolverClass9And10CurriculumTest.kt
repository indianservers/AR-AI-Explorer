package com.indianservers.aiexplorer.solver

import com.indianservers.aiexplorer.solver.domain.engine.Phase3SolverEngine
import com.indianservers.aiexplorer.solver.domain.model.VerificationStatus
import com.indianservers.aiexplorer.solver.domain.steps.SolverRuleRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SolverClass9And10CurriculumTest {
    private val engine = Phase3SolverEngine()

    @Test
    fun allEightyScreenshotQuestionsProduceCheckedStepsOrPreciseMissingDataGuidance() {
        val cases = class9Cases() + class10Cases()
        assertEquals(80, cases.size)

        val failures = mutableListOf<String>()
        cases.forEach { case ->
            val solution = engine.solve("${case.number}. ${case.question}")
            val valid = if (case.missingData) {
                !solution.supported &&
                    solution.verification.status == VerificationStatus.Inconclusive &&
                    solution.steps.size >= 3 &&
                    solution.message.contains("Insufficient information", true)
            } else {
                solution.supported &&
                    solution.canPresentAsCorrect &&
                    solution.verification.status == VerificationStatus.Verified &&
                    solution.finalAnswer.orEmpty().normalize().contains(case.answerSnippet.normalize()) &&
                    solution.steps.size >= 3 &&
                    solution.steps.all { step ->
                        step.explanation.isNotBlank() &&
                            step.affectedTerms.isNotEmpty() &&
                            runCatching { SolverRuleRegistry.get(step.ruleId) }.isSuccess
                    }
            }
            if (!valid) {
                failures += "${case.grade}.${case.number} ${case.question}\n" +
                    " expected=${case.answerSnippet}, missing=${case.missingData}\n" +
                    " supported=${solution.supported}, answer=${solution.finalAnswer}, " +
                    "verification=${solution.verification.status}, steps=${solution.steps.size}, message=${solution.message}"
            }
        }
        assertTrue(failures.joinToString("\n\n"), failures.isEmpty())
    }

    private fun class9Cases() = listOf(
        c(9, 1, "Classify √7 as rational or irrational", "irrational"),
        c(9, 2, "Represent √5 on the number line", "distance√5"),
        c(9, 3, "Rationalise: 5/(√3+1)", "5(√3-1)/2"),
        c(9, 4, "Simplify: (3 + √2)(3 - √2)", "7"),
        c(9, 5, "Express 0.272727... as a rational number", "3/11"),
        c(9, 6, "Find p(2) for p(x) = x³ - 4x + 7", "7"),
        c(9, 7, "Find the zero of p(x) = 5x - 20", "x=4"),
        c(9, 8, "Factorise: x² - 9x + 20", "(x-4)(x-5)"),
        c(9, 9, "Factorise: 8a³ - 27b³", "(2a-3b)(4a²+6ab+9b²)"),
        c(9, 10, "Expand: (2x - 3y)³", "8x³-36x²y+54xy²-27y³"),
        c(9, 11, "Solve: 2x + 3y = 12 when x = 3", "y=2"),
        c(9, 12, "Write three solutions of x + 2y = 8", "(0,4),(2,3),(4,2)"),
        c(9, 13, "Plot the graph of 2x + y = 6", "linethrough(0,6)and(3,0)"),
        c(9, 14, "Find the coordinates of a point lying on the y-axis and 5 units below the origin", "(0,-5)"),
        c(9, 15, "Find the distance of the point (-4, 7) from the x-axis and the y-axis", "x-axis=7"),
        c(9, 16, "State Euclid's fifth postulate", "linesmeet"),
        c(9, 17, "Prove that vertically opposite angles are equal", "verticallyoppositeanglesareequal"),
        c(9, 18, "Two supplementary angles are in the ratio 4 : 5. Find them", "80°and100°"),
        c(9, 19, "Prove that the angles opposite equal sides of a triangle are equal", "anglesoppositeequalsidesareequal"),
        c(9, 20, "In △ABC, AB = AC and ∠B = 55°. Find ∠A", "∠A=70°"),
        c(9, 21, "State and prove the angle-sum property of a triangle", "180°"),
        c(9, 22, "Prove that a diagonal of a parallelogram divides it into two congruent triangles", "congruenttriangles"),
        c(9, 23, "The diagonals of a rhombus are 24 cm and 10 cm. Find its area", "120cm²"),
        c(9, 24, "Prove that equal chords of a circle subtend equal angles at the centre", "equalanglesatthecentre"),
        c(9, 25, "A chord of a circle is 16 cm long and is 6 cm from the centre. Find the radius", "10cm"),
        c(9, 26, "Construct a triangle with sides 5 cm, 6 cm and 7 cm", "AB=7cm"),
        c(9, 27, "Find the area of a triangle with sides 13 cm, 14 cm and 15 cm using Heron's formula", "84cm²"),
        c(9, 28, "Find the area of an equilateral triangle of side 12 cm using Heron's formula", "36√3cm²"),
        c(9, 29, "Find the curved surface area of a cylinder of radius 7 cm and height 12 cm", "168πcm²"),
        c(9, 30, "Find the total surface area of a cone of radius 5 cm and slant height 13 cm", "90πcm²"),
        c(9, 31, "Find the volume of a sphere of radius 6 cm", "288πcm³"),
        c(9, 32, "Find the volume of a hemispherical bowl of radius 7 cm", "686π/3cm³"),
        c(9, 33, "Find the mean of the first ten natural numbers", "5.5"),
        c(9, 34, "Calculate the median of 15, 18, 12, 20, 14, 16 and 19", "16"),
        c(9, 35, "Prepare a frequency distribution for: 4, 6, 5, 7, 4, 8, 6, 5, 4, 7", "4:3,5:2,6:2,7:2,8:1"),
        c(9, 36, "Draw a histogram for the intervals 0-10, 10-20, 20-30 and 30-40 with frequencies 5, 8, 12 and 7", "0-10→5"),
        c(9, 37, "A coin is tossed 200 times and heads occurs 112 times. Find the empirical probability of heads", "14/25"),
        c(9, 38, "A die is thrown 300 times and 6 occurs 48 times. Find its empirical probability", "4/25"),
        c(9, 39, "Explain why every terminating decimal is a rational number", "terminatingdecimalisrational"),
        c(9, 40, "Prove that √3 is irrational", "√3isirrational"),
    )

    private fun class10Cases() = listOf(
        c(10, 1, "Use Euclid's division algorithm to find the HCF of 867 and 255", "HCF=51"),
        c(10, 2, "Find the HCF and LCM of 144 and 180 using prime factorisation", "HCF=36;LCM=720"),
        c(10, 3, "Prove that √5 is irrational", "√5isirrational"),
        c(10, 4, "Determine whether 13/3125 has a terminating decimal expansion", "terminating"),
        c(10, 5, "Find the zeroes of x² - 7x + 12", "x=3orx=4"),
        c(10, 6, "Form a quadratic polynomial whose zeroes are 3 and -5", "x²+2x-15"),
        c(10, 7, "Find the relationship between the zeroes and coefficients of 2x² - 5x - 3", "sum=5/2"),
        c(10, 8, "Divide 2x³ + 3x² - 11x - 6 by x - 2", "quotient=2x²+7x+3"),
        c(10, 9, "Solve: 2x + 3y = 13 and 3x - 2y = 4", "x=38/13;y=31/13"),
        c(10, 10, "Determine whether 4x + 6y = 8 and 2x + 3y = 9 are consistent", "inconsistent"),
        c(10, 11, "Solve x² - 9x + 20 = 0 by factorisation", "x=4orx=5"),
        c(10, 12, "Solve 3x² - 5x - 2 = 0 using the quadratic formula", "x=2orx=-1/3"),
        c(10, 13, "Find the nature of the roots of 2x² + 4x + 5 = 0", "non-realcomplexconjugate"),
        c(10, 14, "Find the 25th term of the AP 7, 11, 15, ...", "103"),
        c(10, 15, "Find the sum of the first 30 terms of the AP 5, 9, 13, ...", "1890"),
        c(10, 16, "Which term of the AP 3, 8, 13, ... is 78?", "16thterm"),
        c(10, 17, "Prove the Basic Proportionality Theorem", "AD/DB=AE/EC"),
        c(10, 18, "In two similar triangles, corresponding sides are in the ratio 3 : 5. Find the ratio of their areas", "9:25"),
        c(10, 19, "Find the distance between (2, -3) and (8, 5)", "10units"),
        c(10, 20, "Find the coordinates of the midpoint of (-4, 7) and (6, -3)", "(1,2)"),
        c(10, 21, "Find the point dividing the segment joining (2, 3) and (8, 15) internally in the ratio 1 : 2", "(4,7)"),
        c(10, 22, "Find the area of the triangle with vertices (1, 2), (5, 6) and (7, 2)", "12squareunits"),
        c(10, 23, "If tan θ = 3/4, find sin θ and cos θ", "sinθ=3/5"),
        c(10, 24, "Prove that sec² θ - tan² θ = 1", "sec²θ-tan²θ=1"),
        c(10, 25, "Evaluate: sin² 30° + cos² 60°", "1/2"),
        c(10, 26, "From a point 20 m away from a tower, the angle of elevation is 45°. Find the tower's height", "20m"),
        c(10, 27, "Prove that the tangent at any point of a circle is perpendicular to the radius through the point of contact", "perpendicular"),
        c(10, 28, "From an external point, two tangents to a circle are drawn. Prove that their lengths are equal", "PA=PB"),
        c(10, 29, "Construct a tangent to a circle of radius 4 cm from a point 7 cm from its centre", "twotangents"),
        c(10, 30, "Find the area of a sector of radius 14 cm and angle 90°", "49πcm²"),
        c(10, 31, "Find the area of the minor segment of a circle of radius 7 cm subtending 90° at the centre", "49(π-2)/4cm²"),
        c(10, 32, "A solid consists of a cone mounted on a hemisphere. Find its volume when both have radius 3 cm and the cone has height 4 cm", "30πcm³"),
        c(10, 33, "A sphere of radius 6 cm is melted and recast into spheres of radius 2 cm. Find their number", "27spheres"),
        c(10, 34, "Find the mean of grouped data using the assumed-mean method", "missing", true),
        c(10, 35, "Find the median for classes 0-10, 10-20, 20-30 and 30-40 with frequencies 5, 9, 12 and 4", "125/6"),
        c(10, 36, "Find the mode for grouped data using the mode formula", "missing", true),
        c(10, 37, "Draw a less-than ogive and use it to estimate the median", "missing", true),
        c(10, 38, "Two dice are thrown. Find the probability that the sum is 9", "1/9"),
        c(10, 39, "A card is drawn from a standard deck. Find the probability of obtaining a red face card", "3/26"),
        c(10, 40, "A bag contains 5 red, 7 blue and 8 green balls. Find the probability of drawing a ball that is not blue", "13/20"),
    )

    private fun c(grade: Int, number: Int, question: String, answer: String, missing: Boolean = false) =
        Case(grade, number, question, answer, missing)

    private fun String.normalize() =
        lowercase()
            .replace(" ", "")
            .replace(".", "")
            .replace("°", "deg")
            .replace("⇒", "")

    private data class Case(
        val grade: Int,
        val number: Int,
        val question: String,
        val answerSnippet: String,
        val missingData: Boolean,
    )
}
