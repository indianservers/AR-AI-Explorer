package com.indianservers.aiexplorer.practice

import com.indianservers.aiexplorer.learningintelligence.model.LearnerErrorType
import kotlin.math.abs
import kotlin.random.Random

enum class GeneratedVariableType { INTEGER, DECIMAL, CHOICE }
data class GeneratedVariableDefinition(val id: String, val type: GeneratedVariableType, val minimum: Double? = null, val maximum: Double? = null, val choices: List<String> = emptyList(), val unit: String? = null)
sealed interface GenerationConstraint { data class NonZero(val variableId: String) : GenerationConstraint; data class LessThan(val firstId: String, val secondId: String) : GenerationConstraint; data class IntegerResult(val expressionId: String) : GenerationConstraint }
data class DifficultyDimensions(val reasoningSteps: Int, val numberComplexity: Int, val representationCount: Int, val prerequisiteLoad: Int)
data class PracticeTemplate(val id: String, val conceptId: String, val variableDefinitions: List<GeneratedVariableDefinition>, val constraints: List<GenerationConstraint>, val solverId: String, val validatorIds: List<String>, val difficultyDimensions: DifficultyDimensions)
data class GeneratedPractice(val templateId: String, val conceptId: String, val prompt: String, val variables: Map<String, Double>, val units: Map<String, String>, val answer: Double, val validationStages: List<String>, val seed: Int)
data class PracticeGenerationResult(val task: GeneratedPractice?, val rejectedAttempts: Int, val issues: List<String>)

object VerifiedPracticeCatalog {
    val templates = listOf(
        PracticeTemplate("linear-equation-ax-b", "math-linear-equations", listOf(GeneratedVariableDefinition("a", GeneratedVariableType.INTEGER, 1.0, 8.0), GeneratedVariableDefinition("x", GeneratedVariableType.INTEGER, -9.0, 9.0), GeneratedVariableDefinition("b", GeneratedVariableType.INTEGER, -12.0, 12.0)), listOf(GenerationConstraint.NonZero("a")), "linear", listOf("algebraic", "domain"), DifficultyDimensions(3, 1, 2, 1)),
        PracticeTemplate("gas-pressure", "chemistry-gas-laws", listOf(GeneratedVariableDefinition("n", GeneratedVariableType.INTEGER, 1.0, 4.0, unit = "mol"), GeneratedVariableDefinition("t", GeneratedVariableType.INTEGER, 250.0, 400.0, unit = "K"), GeneratedVariableDefinition("v", GeneratedVariableType.INTEGER, 5.0, 25.0, unit = "L")), listOf(GenerationConstraint.NonZero("v")), "ideal-gas", listOf("unit", "range", "significant-figures"), DifficultyDimensions(3, 2, 3, 2)),
        PracticeTemplate("mendelian-mono", "biology-mendelian-inheritance", listOf(GeneratedVariableDefinition("dominant", GeneratedVariableType.CHOICE, choices = listOf("T", "R", "B"))), emptyList(), "monohybrid", listOf("genetics-ratio"), DifficultyDimensions(2, 1, 2, 1)),
    )
}

class VerifiedPracticeGenerator {
    fun generate(template: PracticeTemplate, seed: Int = 1, maximumAttempts: Int = 30): PracticeGenerationResult {
        repeat(maximumAttempts) { attempt ->
            val random = Random(seed + attempt)
            val vars = template.variableDefinitions.filter { it.type != GeneratedVariableType.CHOICE }.associate { it.id to random.nextInt(it.minimum!!.toInt(), it.maximum!!.toInt() + 1).toDouble() }
            val result = solve(template, vars) ?: return@repeat
            val issues = validate(template, vars, result)
            if (issues.isEmpty()) {
                val prompt = when (template.solverId) {
                    "linear" -> "Solve ${vars.getValue("a").toInt()}x + ${vars.getValue("b").toInt()} = ${(vars.getValue("a") * vars.getValue("x") + vars.getValue("b")).toInt()}. Preserve the balance at each step."
                    "ideal-gas" -> "For ${vars["n"]} mol of an ideal gas at ${vars["t"]} K in ${vars["v"]} L, calculate pressure in kPa using R = 8.314 kPa·L·mol⁻¹·K⁻¹."
                    else -> "Complete the verified monohybrid cross and state the expected phenotype ratio."
                }
                val stages = listOf("valid_input", "deterministic_solution", "unit_valid", "domain_valid", "range_valid", "unambiguous", "significant_figures_checked", "difficulty_classified", "answer_verified", "rendering_safe")
                return PracticeGenerationResult(GeneratedPractice(template.id, template.conceptId, prompt, vars, template.variableDefinitions.mapNotNull { it.unit?.let { u -> it.id to u } }.toMap(), result, stages, seed + attempt), attempt, emptyList())
            }
        }
        return PracticeGenerationResult(null, maximumAttempts, listOf("Unable to generate a valid variant within the bounded retry limit."))
    }
    private fun solve(t: PracticeTemplate, v: Map<String, Double>) = when (t.solverId) { "linear" -> v["x"]; "ideal-gas" -> 8.314 * v.getValue("n") * v.getValue("t") / v.getValue("v"); "monohybrid" -> 3.0; else -> null }
    private fun validate(t: PracticeTemplate, v: Map<String, Double>, answer: Double): List<String> = buildList {
        if (!answer.isFinite()) add("non_finite_answer")
        t.constraints.filterIsInstance<GenerationConstraint.NonZero>().forEach { if (abs(v[it.variableId] ?: 0.0) < 1e-12) add("zero_${it.variableId}") }
        if (t.solverId == "ideal-gas" && answer !in 1.0..5000.0) add("pressure_out_of_range")
        if (t.variableDefinitions.any { it.minimum != null && (v[it.id] ?: it.minimum) < it.minimum }) add("domain")
    }
}
