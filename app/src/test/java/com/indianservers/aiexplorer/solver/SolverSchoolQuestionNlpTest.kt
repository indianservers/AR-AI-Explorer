package com.indianservers.aiexplorer.solver

import com.indianservers.aiexplorer.solver.domain.engine.Phase3SolverEngine
import com.indianservers.aiexplorer.solver.domain.model.VerificationStatus
import com.indianservers.aiexplorer.solver.domain.steps.SolverRuleRegistry
import com.indianservers.aiexplorer.solver.domain.visualisation.VisualisationData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SolverSchoolQuestionNlpTest {
    private val engine = Phase3SolverEngine()

    @Test
    fun allFortySuppliedSchoolQuestionsAreUnderstoodSolvedAndExplained() {
        val cases = listOf(
            Case("Simplify: 5x + 3x - 7 + 12", setOf("8x+5", "5+8*x")),
            Case("Solve: 3x + 7 = 25", setOf("x=6")),
            Case("Solve: 5(x - 2) = 3x + 10", setOf("x=10")),
            Case("Find the value of 2a^2 - 3a + 5 when a = -2", setOf("19")),
            Case("Expand: (x + 5)(x + 3)", setOf("x^2+8x+15", "15+8*x+x^2")),
            Case("Factorise: 6x + 18", setOf("6(x+3)", "6*(x+3)")),
            Case("Factorise: x^2 + 7x + 12", setOf("(x+3)(x+4)", "(x+3)*(x+4)", "(x+4)*(x+3)")),
            Case("Find the additive inverse of -7/9", setOf("7/9")),
            Case("Simplify: 3/4 + 5/8 - 1/2", setOf("7/8")),
            Case("Evaluate: (2/3)^3", setOf("8/27")),
            Case("Express 0.00056 in standard form", setOf("5.6*10^-4")),
            Case("Find the square root of 1764", setOf("42")),
            Case("Find the cube root of 2197", setOf("13")),
            Case("Find the smallest number by which 432 must be multiplied to make it a perfect square", setOf("3")),
            Case("A shirt marked at INR 1,200 is sold at a discount of 15%. Find its selling price", setOf("INR1020")),
            Case("Find the simple interest on INR 8,000 at 7.5% per annum for 2 years", setOf("INR1200")),
            Case("A population increases from 20,000 to 23,000. Find the percentage increase", setOf("15%")),
            Case("Find the compound interest on INR 10,000 at 10% per annum for 2 years", setOf("INR2100")),
            Case("If 8 notebooks cost INR 360, find the cost of 15 notebooks", setOf("INR675")),
            Case("Twelve workers complete a job in 18 days. How many days will 27 workers take?", setOf("8days")),
            Case("Divide INR 3,600 in the ratio 5 : 7", setOf("INR1500andINR2100")),
            Case("Find the fourth proportional to 6, 9 and 12", setOf("18")),
            Case("The angles of a quadrilateral are in the ratio 2 : 3 : 4 : 6. Find the angles", setOf("48deg,72deg,96deg,144deg")),
            Case("Find the exterior angle of a regular 15-sided polygon", setOf("24deg")),
            Case("Find the sum of the interior angles of a decagon", setOf("1440deg")),
            Case("A rectangle has length 18 cm and breadth 12 cm. Find its area and perimeter", setOf("Area=216cm^2;Perimeter=60cm")),
            Case("Find the area of a trapezium with parallel sides 14 cm and 20 cm and height 9 cm", setOf("153cm^2")),
            Case("Find the area of a rhombus whose diagonals are 16 cm and 12 cm", setOf("96cm^2")),
            Case("Find the total surface area of a cube of side 7 cm", setOf("294cm^2")),
            Case("Find the volume of a cuboid measuring 15 cm x 8 cm x 6 cm", setOf("720cm^3")),
            Case("Find the volume of a cylinder of radius 7 cm and height 10 cm", setOf("490picm^3")),
            Case("Plot the points A(2,3), B(-2,3), C(-2,-1) and D(2,-1). Identify the figure formed", setOf("Rectangle")),
            Case("Reflect the point (4, -3) in the x-axis", setOf("(4,3)")),
            Case("Find the mean of 12, 18, 15, 20, 25 and 24", setOf("19")),
            Case("Find the median of 7, 12, 9, 15, 8, 20 and 11", setOf("11")),
            Case("Find the mode of 4, 7, 6, 4, 8, 4, 9, 6", setOf("4")),
            Case("A die is rolled once. Find the probability of obtaining a prime number", setOf("1/2")),
            Case("Two coins are tossed simultaneously. List the sample space", setOf("{HH,HT,TH,TT}")),
            Case("Draw a bar graph for the marks 35, 42, 28, 46 and 39 obtained in five subjects", setOf("Subject1=35,Subject2=42,Subject3=28,Subject4=46,Subject5=39")),
            Case("A number is selected from 1 to 20. Find the probability that it is divisible by 3", setOf("3/10")),
        )
        assertEquals(40, cases.size)

        val failures = mutableListOf<String>()
        cases.forEachIndexed { index, case ->
            val solution = engine.solve("${index + 1}. ${case.question}")
            val answer = solution.finalAnswer
            val normalized = answer?.normalize()
            val expected = case.answers.map { it.normalize() }.toSet()
            val valid = solution.supported &&
                solution.canPresentAsCorrect &&
                solution.verification.status == VerificationStatus.Verified &&
                normalized in expected &&
                solution.steps.size >= 2 &&
                solution.steps.all { step ->
                    step.explanation.isNotBlank() &&
                        step.affectedTerms.isNotEmpty() &&
                        runCatching { SolverRuleRegistry.get(step.ruleId) }.isSuccess
                }
            if (!valid) {
                failures += "${index + 1}. ${case.question}: answer=$answer expected=${case.answers}; " +
                    "supported=${solution.supported}; verification=${solution.verification.status}; " +
                    "steps=${solution.steps.size}; message=${solution.message}"
            }
            if (index == 38) {
                assertTrue(
                    "The bar-graph question must include a rendered data specification",
                    solution.visualisations.any { it.mathematicalData is VisualisationData.BarChart },
                )
            }
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    private fun String.normalize(): String =
        replace(" ", "")
            .replace("×", "*")
            .replace("₹", "INR")
            .replace("°", "deg")
            .trimEnd('.')

    private data class Case(
        val question: String,
        val answers: Set<String>,
    )
}
