package com.indianservers.aiexplorer.solver

import com.indianservers.aiexplorer.core.ExpressionEngine
import com.indianservers.aiexplorer.solver.domain.engine.Phase3SolverEngine
import com.indianservers.aiexplorer.solver.domain.model.VerificationStatus
import java.io.File
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SolverCasStaticGoldenDatasetTest {
    private val engine = Phase3SolverEngine()
    private val expressions = ExpressionEngine()

    @Test
    fun allTwelveHundredStaticGoldenCasesExecuteAgainstProductionEngine() {
        val dataset = locateDataset()
        val lines = dataset.readLines().filter(String::isNotBlank)
        assertEquals("The static corpus must contain exactly 1,200 JSONL records", 1_200, lines.size)

        val cases = lines.mapIndexed { index, line -> parse(index + 1, line) }
        assertEquals("Every case ID must be unique", 1_200, cases.map(GoldenCase::id).distinct().size)
        assertEquals("Every input must be unique", 1_200, cases.map(GoldenCase::input).distinct().size)
        assertEquals(
            linkedMapOf(
                "arithmetic" to 200,
                "algebra_symbolic" to 300,
                "equations_inequalities" to 200,
                "calculus" to 200,
                "matrices" to 100,
                "vectors_complex" to 100,
                "invalid_undefined_ambiguous_unsupported" to 100,
            ),
            cases.groupingBy(GoldenCase::category).eachCount(),
        )

        val outcomes = cases.map { execute(it) }
        writeExecutionEvidence(outcomes)
        val failures = outcomes.filterNot(Outcome::passed)
        assertTrue(
            buildString {
                appendLine("${failures.size} of ${outcomes.size} static Solver/CAS golden cases failed:")
                failures.take(100).forEach { appendLine("${it.case.id} ${it.case.input}: ${it.detail}") }
                if (failures.size > 100) appendLine("... ${failures.size - 100} additional failures are in the CSV evidence.")
            },
            failures.isEmpty(),
        )
    }

    private fun execute(case: GoldenCase): Outcome {
        val solution = runCatching { engine.solve(case.input) }.getOrElse { error ->
            return Outcome(case, false, "production engine threw ${error::class.simpleName}: ${error.message}", "THREW", "")
        }
        if (!case.expectedSupported) {
            val passed = !solution.canPresentAsCorrect && solution.finalAnswer == null && solution.message.isNotBlank()
            return Outcome(
                case,
                passed,
                if (passed) "Safely rejected: ${solution.message}" else
                    "expected fail-closed; supported=${solution.supported}; answer=${solution.finalAnswer}; verification=${solution.verification.status}",
                solution.verification.status.name,
                solution.finalAnswer.orEmpty(),
            )
        }

        val actual = solution.exactAnswer ?: solution.finalAnswer
        val answerMatches = actual != null && when (case.comparison) {
            "exact_compact" -> compact(actual) == compact(case.expected)
            "symbolic_numeric" -> numericallyEquivalent(actual, case.expected, removeConstant = false)
            "antiderivative_numeric" -> numericallyEquivalent(actual, case.expected, removeConstant = true)
            "numeric_tolerance" -> runCatching {
                abs(actual.toDouble() - case.approximate.toDouble()) <= case.tolerance
            }.getOrDefault(false)
            else -> false
        }
        val passed = solution.supported && solution.canPresentAsCorrect &&
            solution.verification.status == VerificationStatus.Verified &&
            solution.steps.isNotEmpty() && solution.steps.all { it.ruleId.isNotBlank() && it.explanation.isNotBlank() } &&
            answerMatches
        return Outcome(
            case,
            passed,
            if (passed) "Exact expectation and independent contract checks passed" else
                "supported=${solution.supported}; answer=$actual; expected=${case.expected}; comparison=${case.comparison}; " +
                    "verification=${solution.verification.status}; steps=${solution.steps.size}; message=${solution.message}",
            solution.verification.status.name,
            actual.orEmpty(),
        )
    }

    private fun numericallyEquivalent(actual: String, expected: String, removeConstant: Boolean): Boolean = runCatching {
        fun prepared(value: String): String {
            val compact = value.replace(" ", "")
            return if (removeConstant) compact.removeSuffix("+C").removeSuffix("-C") else compact
        }
        val left = expressions.compile(prepared(actual))
        val right = expressions.compile(prepared(expected))
        listOf(-3.0, -1.25, -0.5, 0.0, 0.75, 2.0, 4.0).all { x ->
            val a = left.eval(mapOf("x" to x))
            val b = right.eval(mapOf("x" to x))
            a.isFinite() && b.isFinite() && abs(a - b) <= 1e-9 * maxOf(1.0, abs(a), abs(b))
        }
    }.getOrDefault(false)

    private fun parse(lineNumber: Int, line: String): GoldenCase {
        assertTrue("Line $lineNumber is not a JSON object", line.startsWith('{') && line.endsWith('}'))
        val fields = FIELD.findAll(line).associate { it.groupValues[1] to unescape(it.groupValues[2]) }
        val missing = REQUIRED_FIELDS.filter { fields[it].isNullOrBlank() }
        assertTrue("Line $lineNumber is missing metadata: $missing", missing.isEmpty())
        return GoldenCase(
            id = fields.getValue("case_id"),
            category = fields.getValue("category"),
            input = fields.getValue("input"),
            expected = fields.getValue("expected_exact_result"),
            approximate = fields.getValue("expected_approximate_result"),
            tolerance = fields.getValue("tolerance").toDouble(),
            expectedSupported = fields.getValue("expected_supported").toBooleanStrict(),
            comparison = fields.getValue("comparison"),
        )
    }

    private fun locateDataset(): File = listOf(
        File("../SOLVER_CAS_GOLDEN_DATASET.jsonl"),
        File("SOLVER_CAS_GOLDEN_DATASET.jsonl"),
    ).firstOrNull(File::isFile) ?: error("SOLVER_CAS_GOLDEN_DATASET.jsonl was not found from ${File(".").absolutePath}")

    private fun writeExecutionEvidence(outcomes: List<Outcome>) {
        val directory = File("build/reports/solver-cas-golden").apply { mkdirs() }
        File(directory, "SOLVER_CAS_GOLDEN_RESULTS.csv").writeText(
            buildString {
                appendLine("case_id,category,status,verification,input,expected,actual,detail")
                outcomes.forEach { outcome ->
                    appendLine(
                        listOf(
                            outcome.case.id,
                            outcome.case.category,
                            if (outcome.passed) "PASS" else "FAIL",
                            outcome.verification,
                            outcome.case.input,
                            outcome.case.expected,
                            outcome.actual,
                            outcome.detail,
                        ).joinToString(",") { csv(it) },
                    )
                }
            },
        )
    }

    private fun compact(value: String): String = value.replace(Regex("\\s+"), "")
    private fun csv(value: String): String = "\"${value.replace("\"", "\"\"")}\""
    private fun unescape(value: String): String {
        val unicodeDecoded = UNICODE_ESCAPE.replace(value) { match ->
            match.groupValues[1].toInt(16).toChar().toString()
        }
        return unicodeDecoded.replace("\\\"", "\"").replace("\\\\", "\\")
    }

    private data class GoldenCase(
        val id: String,
        val category: String,
        val input: String,
        val expected: String,
        val approximate: String,
        val tolerance: Double,
        val expectedSupported: Boolean,
        val comparison: String,
    )

    private data class Outcome(
        val case: GoldenCase,
        val passed: Boolean,
        val detail: String,
        val verification: String,
        val actual: String,
    )

    private companion object {
        val FIELD = Regex("\\\"([a-z_]+)\\\":\\\"((?:\\\\.|[^\\\"])*)\\\"")
        val UNICODE_ESCAPE = Regex("\\\\u([0-9a-fA-F]{4})")
        val REQUIRED_FIELDS = listOf(
            "case_id", "category", "subcategory", "difficulty", "input", "normalized_input",
            "expected_exact_result", "expected_approximate_result", "tolerance", "expected_solution_set",
            "assumptions", "domain_constraints", "units", "angle_mode", "expected_warnings",
            "expected_error_classification", "expected_step_summary", "expected_verification",
            "independent_derivation", "tags", "regression_id", "expected_supported", "comparison",
        )
    }
}
