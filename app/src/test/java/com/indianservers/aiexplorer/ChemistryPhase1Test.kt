package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.chemistry.data.BundledElementData
import com.indianservers.aiexplorer.chemistry.domain.ChemistryDataValidator
import com.indianservers.aiexplorer.chemistry.domain.ElectronConfigurationEngine
import com.indianservers.aiexplorer.chemistry.model.ElementCategory
import com.indianservers.aiexplorer.chemistry.model.PeriodicBlock
import com.indianservers.aiexplorer.chemistry.model.PeriodicTrendProperty
import com.indianservers.aiexplorer.chemistry.navigation.ChemistryRoute
import com.indianservers.aiexplorer.chemistry.repository.OfflineChemistryRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChemistryPhase1Test {
    private val repository = OfflineChemistryRepository()

    @Test fun loadsAll118OfficialElements() {
        assertEquals(118, repository.getAllElements().size)
        assertEquals((1..118).toList(), repository.getAllElements().map { it.atomicNumber })
    }

    @Test fun atomicNumbersAndSymbolsAreUnique() {
        val elements = repository.getAllElements()
        assertEquals(118, elements.map { it.atomicNumber }.toSet().size)
        assertEquals(118, elements.map { it.symbol }.toSet().size)
    }

    @Test fun periodicPositionsIncludeSeparatedFBlock() {
        val hydrogen = repository.getElementByAtomicNumber(1)!!
        val helium = repository.getElementByAtomicNumber(2)!!
        val cerium = repository.getElementByAtomicNumber(58)!!
        val oganesson = repository.getElementByAtomicNumber(118)!!
        assertEquals(1, hydrogen.group)
        assertEquals(18, helium.group)
        assertNull(cerium.group)
        assertEquals(6, cerium.period)
        assertEquals(PeriodicBlock.F, cerium.block)
        assertEquals(18, oganesson.group)
        assertEquals(7, oganesson.period)
    }

    @Test fun searchesByNameSymbolAndAtomicNumber() {
        assertEquals("O", repository.searchElements("Oxygen").single().symbol)
        assertEquals("O", repository.searchElements("o").single().symbol)
        assertEquals("O", repository.searchElements("8").single().symbol)
    }

    @Test fun categoryGroupAndPeriodFiltersWork() {
        assertTrue(repository.getElementsByCategory(ElementCategory.NobleGas).any { it.symbol == "Ne" })
        assertEquals(listOf("H", "Li", "Na", "K", "Rb", "Cs", "Fr"), repository.getElementsByGroup(1).map { it.symbol })
        assertEquals(18, repository.getElementsByPeriod(4).size)
    }

    @Test fun everyNeutralAtomHasCorrectElectronAndShellTotals() {
        repository.getAllElements().forEach { element ->
            assertEquals(element.atomicNumber, element.neutralElectronCount)
            assertEquals(element.atomicNumber, element.electronConfiguration.electronCount)
            assertEquals(element.atomicNumber, element.electronConfiguration.shellDistribution.sum())
            assertEquals(element.atomicNumber, element.electronConfiguration.orbitals.sumOf { it.electrons })
            assertTrue(element.electronConfiguration.orbitals.all { it.electrons <= it.capacity })
        }
    }

    @Test fun ionElectronCountsChangeWithoutChangingProtons() {
        val sodium = repository.getElementByAtomicNumber(11)!!
        val chloride = repository.getElementByAtomicNumber(17)!!
        assertEquals(10, sodium.electronCountForCharge(1))
        assertEquals(18, chloride.electronCountForCharge(-1))
        assertEquals(11, sodium.protonCount)
        assertEquals(10, ElectronConfigurationEngine.ion(11, 1).electronCount)
    }

    @Test fun chromiumAndCopperGroundStateExceptionsAreCorrect() {
        val chromium = repository.getElementByAtomicNumber(24)!!.electronConfiguration
        val copper = repository.getElementByAtomicNumber(29)!!.electronConfiguration
        assertTrue(chromium.nobleGasShorthand.contains("3d⁵"))
        assertTrue(chromium.nobleGasShorthand.contains("4s¹"))
        assertTrue(copper.nobleGasShorthand.contains("3d¹⁰"))
        assertTrue(copper.nobleGasShorthand.contains("4s¹"))
    }

    @Test fun representativeIsotopesAreExplicitlyQualified() {
        repository.getAllElements().forEach { element ->
            assertTrue(element.representativeIsotope.massNumber >= element.atomicNumber)
            assertTrue(element.representativeIsotope.note.contains("representative", ignoreCase = true))
            assertEquals(element.representativeIsotope.massNumber - element.atomicNumber, element.representativeNeutronCount)
        }
    }

    @Test fun missingScientificPropertiesRemainMissingNotZero() {
        val oganesson = repository.getElementByAtomicNumber(118)!!
        assertNull(oganesson.physicalProperties.electronegativityPauling)
        assertNull(oganesson.physicalProperties.densityGcm3)
    }

    @Test fun curatedTrendDataLoadsWithoutInventedMissingValues() {
        val electronegativity = repository.getTrendData(PeriodicTrendProperty.Electronegativity)
        assertEquals(3.98, electronegativity[9]!!, 1e-12)
        assertFalse(118 in electronegativity)
        assertTrue(repository.getTrendData(PeriodicTrendProperty.FirstIonisationEnergy).size >= 20)
    }

    @Test fun comparesBetweenTwoAndFourElements() {
        val compared = repository.compareElements(listOf(1, 6, 8, 26))
        assertEquals(listOf("H", "C", "O", "Fe"), compared.map { it.symbol })
    }

    @Test fun completeDatasetPassesScientificValidation() {
        val report = ChemistryDataValidator.validate(BundledElementData.elements)
        assertTrue(report.errors.joinToString(), report.valid)
        assertEquals(118, report.elementCount)
    }

    @Test fun chemistryRoutesRemainFeatureScoped() {
        assertEquals("chemistry/home", ChemistryRoute.ChemistryHome.route)
        assertEquals("chemistry/element/8", ChemistryRoute.ElementDetails(8).route)
        assertTrue(ChemistryRoute.PeriodicTable.route.startsWith("chemistry/"))
        assertNotNull(ChemistryRoute.ElementQuiz)
    }
}
