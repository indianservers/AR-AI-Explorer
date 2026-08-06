package com.indianservers.aiexplorer.solver

import com.indianservers.aiexplorer.solver.domain.engine.Phase3SolverEngine
import com.indianservers.aiexplorer.solver.domain.model.VerificationStatus
import com.indianservers.aiexplorer.solver.domain.steps.SolverRuleRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SolverClass11CurriculumTest {
    private val engine = Phase3SolverEngine()

    @Test
    fun allTwentyOneVisibleClass11QuestionsAreSolvedOfflineWithVerifiedSteps() {
        val cases = listOf(
            c("Write the set {x : x ∈ N, x² < 30} in roster form", "{1,2,3,4,5}"),
            c("Prove that A - (B ∪ C) = (A - B) ∩ (A - C)", "A-(B∪C)=(A-B)∩(A-C)"),
            c("In a class of 60 students, 35 study Mathematics and 28 study Physics, while 15 study both. Find how many study at least one subject", "48students"),
            c("Find the domain and range of f(x) = √(9 - x²)", "Domain=[-3,3];Range=[0,3]"),
            c("Determine whether f(x) = x³ - x is one-one", "notone-one"),
            c("Find f ∘ g and g ∘ f when f(x) = 2x + 1 and g(x) = x²", "(f∘g)(x)=2x²+1"),
            c("Solve: sin x = 1/2, for 0 ≤ x ≤ 2π", "x=π/6orx=5π/6"),
            c("Prove that (1 - cos 2x)/(sin 2x) = tan x", "=tanx"),
            c("Find the general solution of tan x = √3", "x=nπ+π/3,n∈Z"),
            c("Find the modulus and argument of z = -1 + √3i", "|z|=2;principalargument=2π/3"),
            c("Simplify: (3 + 4i)/(2 - i)", "2/5+(11/5)i"),
            c("Solve x² + 4x + 13 = 0 in the complex-number system", "x=-2±3i"),
            c("Solve the inequality 3x - 7 < 2x + 5", "x<12"),
            c("Solve (x - 2)/(x + 3) ≥ 0", "(-∞,-3)∪[2,∞)"),
            c("How many five-digit numbers can be formed using the digits 1, 2, 3, 4, 5 without repetition?", "120"),
            c("Find the number of ways of selecting a committee of 4 from 10 people", "210"),
            c("Find the middle term in the expansion of (x + 2)⁸", "1120x⁴"),
            c("Find the coefficient of x⁵ in (2 + x)⁹", "2016"),
            c("Find the 20th term of the sequence 3, 7, 11, ...", "79"),
            c("Find the sum 1 + 3 + 5 + ... + 99", "2500"),
            c("Insert three geometric means between 2 and 162", "6,18,54"),
        )
        assertEquals(21, cases.size)

        val failures = mutableListOf<String>()
        cases.forEachIndexed { index, case ->
            val solution = engine.solve("${index + 1}. ${case.question}")
            val valid = solution.supported &&
                solution.canPresentAsCorrect &&
                solution.verification.status == VerificationStatus.Verified &&
                solution.finalAnswer.orEmpty().normalize().contains(case.answer.normalize()) &&
                solution.steps.size >= 3 &&
                solution.steps.all { step ->
                    step.explanation.isNotBlank() &&
                        step.affectedTerms.isNotEmpty() &&
                        runCatching { SolverRuleRegistry.get(step.ruleId) }.isSuccess
                }
            if (!valid) {
                failures += "${index + 1}. ${case.question}\nexpected=${case.answer}; " +
                    "answer=${solution.finalAnswer}; supported=${solution.supported}; " +
                    "verification=${solution.verification.status}; steps=${solution.steps.size}; message=${solution.message}"
            }
        }
        assertTrue(failures.joinToString("\n\n"), failures.isEmpty())
    }

    private fun c(question: String, answer: String) = Case(question, answer)

    private fun String.normalize() =
        lowercase().replace(" ", "").replace(".", "").replace("where", "")

    private data class Case(val question: String, val answer: String)
}
