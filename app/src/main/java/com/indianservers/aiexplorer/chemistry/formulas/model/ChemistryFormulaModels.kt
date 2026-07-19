package com.indianservers.aiexplorer.chemistry.formulas.model

enum class ChemistryFormulaLevel(val label: String, val rank: Int) { Foundation("Foundation",0), Class7("Class 7",1), Class8("Class 8",2), Class9("Class 9",3), Class10("Class 10",4), Class11("Class 11",5), Class12("Class 12",6), Undergraduate("Undergraduate",7), Postgraduate("Postgraduate",8) }
data class ChemistryFormulaCategory(val id: String, val title: String, val description: String, val subcategoryIds: List<String>)
data class ChemistryFormulaSubcategory(val id: String, val categoryId: String, val title: String, val description: String)
data class ChemistryFormulaVariable(val id: String, val symbol: String, val spokenName: String, val meaning: String, val siUnit: String?, val dimension: String?, val positiveOnly: Boolean = false)
data class ChemistryFormulaForm(val label: String, val equation: String)
data class ChemistryDerivationStep(val equation: String, val explanation: String)
data class ChemistryWorkedExample(val question: String, val substitution: String, val answer: String, val unitCheck: String)
data class ChemistryCalculatorDefinition(val targetVariableIds: Set<String>, val requiredVariableIds: Set<String>, val internalUnitSystem: String = "SI-compatible chemistry units")
data class ChemistryFormula(
    val id: String, val categoryId: String, val subcategoryId: String, val title: String, val equation: String, val searchableEquation: String,
    val spokenEquation: String, val description: String, val minimumLevel: ChemistryFormulaLevel, val variables: List<ChemistryFormulaVariable>,
    val assumptions: List<String>, val limitations: List<String>, val alternativeForms: List<ChemistryFormulaForm>, val derivationSteps: List<ChemistryDerivationStep>,
    val workedExamples: List<ChemistryWorkedExample>, val unitCheck: String, val calculator: ChemistryCalculatorDefinition?, val relatedFormulaIds: List<String>,
    val relatedConceptIds: List<String>, val keywords: Set<String>, val featured: Boolean = false,
)
data class ChemistryFormulaCatalogue(val schemaVersion: Int, val categories: List<ChemistryFormulaCategory>, val subcategories: List<ChemistryFormulaSubcategory>, val formulas: List<ChemistryFormula>)
data class ChemistryFormulaFilters(val level: ChemistryFormulaLevel = ChemistryFormulaLevel.Class10, val categoryId: String? = null, val subcategoryId: String? = null, val calculatorOnly: Boolean = false, val derivationOnly: Boolean = false, val bookmarkedOnly: Boolean = false, val recentOnly: Boolean = false)
data class ChemistryFormulaValidationReport(val errors: List<String>, val warnings: List<String>) { val valid get() = errors.isEmpty() }

