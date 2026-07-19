package com.indianservers.aiexplorer.chemistry.formulas.calculator

data class ChemistryCalculationInput(val value: Double, val unit: String)
data class ChemistryCalculationStep(val expression: String, val explanation: String)
data class ChemistryCalculationResult(val value: Double, val unit: String, val steps: List<ChemistryCalculationStep>)
data class ChemistryFormulaCalculationState(val formulaId: String, val targetVariableId: String? = null, val inputs: Map<String, ChemistryCalculationInput> = emptyMap(), val validationErrors: Map<String,String> = emptyMap(), val result: ChemistryCalculationResult? = null)
fun interface ChemistryFormulaSolver { fun solve(targetVariableId: String, siValues: Map<String, Double>): ChemistryCalculationResult }

