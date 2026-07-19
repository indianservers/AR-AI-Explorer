package com.indianservers.aiexplorer.physics.formulas.calculator

data class PhysicsCalculationInput(val value: Double, val unit: String)
data class PhysicsCalculationStep(val expression: String, val explanation: String)
data class PhysicsCalculationResult(val value: Double, val unit: String, val steps: List<PhysicsCalculationStep>)
data class PhysicsFormulaCalculationState(val formulaId: String, val targetVariableId: String? = null, val inputs: Map<String, PhysicsCalculationInput> = emptyMap(), val validationErrors: Map<String, String> = emptyMap(), val result: PhysicsCalculationResult? = null)

/** Phase-1 contract. Subject-specific solvers are registered incrementally in the calculator phase. */
fun interface PhysicsFormulaSolver {
    fun solve(targetVariableId: String, siValues: Map<String, Double>): PhysicsCalculationResult
}

