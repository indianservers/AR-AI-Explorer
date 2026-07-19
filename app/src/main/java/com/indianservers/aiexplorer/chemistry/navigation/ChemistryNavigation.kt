package com.indianservers.aiexplorer.chemistry.navigation

sealed class ChemistryRoute(val route: String) {
    data object ChemistryHome : ChemistryRoute("chemistry/home")
    data object PeriodicTable : ChemistryRoute("chemistry/periodic-table")
    data class ElementDetails(val atomicNumber: Int) : ChemistryRoute("chemistry/element/$atomicNumber")
    data class AtomViewer(val atomicNumber: Int) : ChemistryRoute("chemistry/atom/$atomicNumber")
    data class ElectronConfiguration(val atomicNumber: Int) : ChemistryRoute("chemistry/electron-configuration/$atomicNumber")
    data object CompareElements : ChemistryRoute("chemistry/compare")
    data object PeriodicTrends : ChemistryRoute("chemistry/trends")
    data object ElementQuiz : ChemistryRoute("chemistry/quiz")
    data object FormulaLibrary : ChemistryRoute("chemistry/formulas/home")
}
