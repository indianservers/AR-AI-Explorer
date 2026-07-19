package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.chemistry.formulas.data.BundledChemistryFormulaData
import com.indianservers.aiexplorer.chemistry.formulas.domain.ChemistryFormulaValidator
import com.indianservers.aiexplorer.chemistry.formulas.model.ChemistryFormulaFilters
import com.indianservers.aiexplorer.chemistry.formulas.model.ChemistryFormulaLevel
import com.indianservers.aiexplorer.chemistry.formulas.navigation.ChemistryFormulaRoute
import com.indianservers.aiexplorer.chemistry.formulas.navigation.ChemistryFormulaViewModel
import com.indianservers.aiexplorer.chemistry.formulas.repository.OfflineChemistryFormulaRepository
import com.indianservers.aiexplorer.chemistry.formulas.units.ChemistryUnitSystem
import com.indianservers.aiexplorer.physics.formulas.data.BundledPhysicsFormulaData
import com.indianservers.aiexplorer.physics.formulas.domain.PhysicsFormulaValidator
import com.indianservers.aiexplorer.physics.formulas.model.PhysicsFormulaFilters
import com.indianservers.aiexplorer.physics.formulas.model.PhysicsFormulaLevel
import com.indianservers.aiexplorer.physics.formulas.navigation.PhysicsFormulaRoute
import com.indianservers.aiexplorer.physics.formulas.navigation.PhysicsFormulaViewModel
import com.indianservers.aiexplorer.physics.formulas.repository.OfflinePhysicsFormulaRepository
import com.indianservers.aiexplorer.physics.formulas.units.PhysicsUnitSystem
import org.junit.Assert.*
import org.junit.Test

class PhysicsChemistryFormulaPhase1And2Test {
    private val physics = OfflinePhysicsFormulaRepository()
    private val chemistry = OfflineChemistryFormulaRepository()

    @Test fun physicsCatalogueHasCompleteRequestedHierarchy() {
        assertEquals(25, physics.getCategories().size)
        assertTrue(physics.getCategories().all { it.subcategoryIds.isNotEmpty() })
        physics.getCategories().forEach { category -> assertEquals(category.subcategoryIds, physics.getSubcategories(category.id).map { it.id }) }
        assertTrue(BundledPhysicsFormulaData.catalogue.subcategories.size >= 200)
    }

    @Test fun chemistryCatalogueHasCompleteRequestedHierarchy() {
        assertEquals(26, chemistry.getCategories().size)
        assertTrue(chemistry.getCategories().all { it.subcategoryIds.isNotEmpty() })
        chemistry.getCategories().forEach { category -> assertEquals(category.subcategoryIds, chemistry.getSubcategories(category.id).map { it.id }) }
        assertTrue(BundledChemistryFormulaData.catalogue.subcategories.size >= 200)
    }

    @Test fun repositoriesRemainSubjectIsolated() {
        assertNull(physics.getFormula("chem-ph")); assertNull(chemistry.getFormula("physics-ohm"))
        assertTrue(physics.search("Nernst", PhysicsFormulaFilters(level = PhysicsFormulaLevel.Postgraduate)).isEmpty())
        assertTrue(chemistry.search("Newton second law", ChemistryFormulaFilters(level = ChemistryFormulaLevel.Postgraduate)).isEmpty())
    }

    @Test fun physicsSearchNormalisesNamesEquationsAndSymbols() {
        assertEquals("physics-newton-second-law", physics.search("F = ma", PhysicsFormulaFilters(level = PhysicsFormulaLevel.Postgraduate)).first().id)
        assertTrue(physics.search("lens equation", PhysicsFormulaFilters(level = PhysicsFormulaLevel.Postgraduate)).any { it.id == "physics-thin-lens" })
        assertTrue(physics.search("acceleration", PhysicsFormulaFilters(level = PhysicsFormulaLevel.Postgraduate)).any { it.id == "physics-final-velocity" })
    }

    @Test fun chemistrySearchNormalisesNamesAndCommonNotation() {
        assertTrue(chemistry.search("PV = nRT", ChemistryFormulaFilters(level = ChemistryFormulaLevel.Postgraduate)).any { it.id == "chem-ideal-gas" })
        assertTrue(chemistry.search("pH", ChemistryFormulaFilters(level = ChemistryFormulaLevel.Postgraduate)).any { it.id == "chem-ph" })
        assertTrue(chemistry.search("Nernst", ChemistryFormulaFilters(level = ChemistryFormulaLevel.Postgraduate)).any { it.id == "chem-nernst" })
    }

    @Test fun learningLevelsHideAdvancedFormulasByDefault() {
        assertFalse(physics.search("Lagrangian", PhysicsFormulaFilters(level = PhysicsFormulaLevel.Class12)).any { it.id == "physics-lagrangian" })
        assertTrue(physics.search("Lagrangian", PhysicsFormulaFilters(level = PhysicsFormulaLevel.Undergraduate)).any { it.id == "physics-lagrangian" })
        assertFalse(chemistry.search("chemical potential", ChemistryFormulaFilters(level = ChemistryFormulaLevel.Class12)).any { it.id == "chem-chemical-potential" })
    }

    @Test fun filtersSupportCalculatorDerivationBookmarkAndRecentState() {
        val id = "physics-speed"
        assertEquals(listOf(id), physics.search("speed", PhysicsFormulaFilters(level = PhysicsFormulaLevel.Postgraduate, bookmarkedOnly = true), setOf(id)).map { it.id })
        assertTrue(physics.search("", PhysicsFormulaFilters(level = PhysicsFormulaLevel.Postgraduate, calculatorOnly = true)).all { it.calculator != null })
        assertTrue(chemistry.search("", ChemistryFormulaFilters(level = ChemistryFormulaLevel.Postgraduate, derivationOnly = true)).all { it.derivationSteps.isNotEmpty() })
        assertEquals(listOf("chem-ph"), chemistry.search("", ChemistryFormulaFilters(level = ChemistryFormulaLevel.Postgraduate, recentOnly = true), recent = listOf("chem-ph")).map { it.id })
    }

    @Test fun physicsUnitsConvertScaleAndRejectDimensionMismatch() {
        assertEquals(10.0, PhysicsUnitSystem.convert(36.0, "km/h", "m/s"), 1e-10)
        assertEquals(1.0, PhysicsUnitSystem.convert(100.0, "cm", "m"), 1e-10)
        assertThrows(IllegalArgumentException::class.java) { PhysicsUnitSystem.convert(1.0, "m", "s") }
    }

    @Test fun temperatureConversionHandlesOffsetsCorrectly() {
        assertEquals(273.15, PhysicsUnitSystem.convert(0.0, "°C", "K"), 1e-10)
        assertEquals(298.15, ChemistryUnitSystem.convert(25.0, "°C", "K"), 1e-10)
        assertEquals(25.0, ChemistryUnitSystem.convert(298.15, "K", "°C"), 1e-10)
    }

    @Test fun chemistryUnitsSupportPressureVolumeAndConcentration() {
        assertEquals(101325.0, ChemistryUnitSystem.convert(1.0, "atm", "Pa"), 1e-8)
        assertEquals(.001, ChemistryUnitSystem.convert(1.0, "L", "m³"), 1e-12)
        assertEquals(1000.0, ChemistryUnitSystem.convert(1.0, "mol/L", "mol/m³"), 1e-8)
    }

    @Test fun calculatorContractsReferenceOnlyDeclaredVariables() {
        BundledPhysicsFormulaData.catalogue.formulas.forEach { formula -> formula.calculator?.let { assertTrue((it.targetVariableIds + it.requiredVariableIds).all { id -> formula.variables.any { variable -> variable.id == id } }) } }
        BundledChemistryFormulaData.catalogue.formulas.forEach { formula -> formula.calculator?.let { assertTrue((it.targetVariableIds + it.requiredVariableIds).all { id -> formula.variables.any { variable -> variable.id == id } }) } }
    }

    @Test fun formulaMetadataProvidesAccessibleDetailFoundation() {
        BundledPhysicsFormulaData.catalogue.formulas.forEach { formula ->
            assertTrue(formula.equation.isNotBlank()); assertTrue(formula.spokenEquation.isNotBlank()); assertTrue(formula.variables.isNotEmpty()); assertTrue(formula.workedExamples.isNotEmpty()); assertTrue(formula.unitCheck.isNotBlank())
        }
        BundledChemistryFormulaData.catalogue.formulas.forEach { formula ->
            assertTrue(formula.equation.isNotBlank()); assertTrue(formula.spokenEquation.isNotBlank()); assertTrue(formula.variables.isNotEmpty()); assertTrue(formula.workedExamples.isNotEmpty()); assertTrue(formula.unitCheck.isNotBlank())
        }
    }

    @Test fun bothCompleteCataloguesPassValidation() {
        assertTrue(PhysicsFormulaValidator.validate(BundledPhysicsFormulaData.catalogue).errors.joinToString(), physics.validate().valid)
        assertTrue(ChemistryFormulaValidator.validate(BundledChemistryFormulaData.catalogue).errors.joinToString(), chemistry.validate().valid)
    }

    @Test fun validatorsRejectDuplicateIdsAndBrokenHierarchy() {
        val invalidPhysics = BundledPhysicsFormulaData.catalogue.copy(categories = BundledPhysicsFormulaData.catalogue.categories + BundledPhysicsFormulaData.catalogue.categories.first())
        val invalidChemistry = BundledChemistryFormulaData.catalogue.copy(formulas = BundledChemistryFormulaData.catalogue.formulas.mapIndexed { index, formula -> if (index == 0) formula.copy(subcategoryId = "missing") else formula })
        assertFalse(PhysicsFormulaValidator.validate(invalidPhysics).valid); assertFalse(ChemistryFormulaValidator.validate(invalidChemistry).valid)
    }

    @Test fun navigationStacksAndPersonalStateStayIndependent() {
        val physicsVm = PhysicsFormulaViewModel(physics); val chemistryVm = ChemistryFormulaViewModel(chemistry)
        physicsVm.navigate(PhysicsFormulaRoute.Detail("physics-speed")); physicsVm.toggleBookmark("physics-speed")
        chemistryVm.navigate(ChemistryFormulaRoute.Detail("chem-ph")); chemistryVm.toggleBookmark("chem-ph")
        assertEquals(setOf("physics-speed"), physicsVm.state.bookmarks); assertEquals(setOf("chem-ph"), chemistryVm.state.bookmarks)
        assertTrue(physicsVm.back()); assertTrue(chemistryVm.back())
    }
}
