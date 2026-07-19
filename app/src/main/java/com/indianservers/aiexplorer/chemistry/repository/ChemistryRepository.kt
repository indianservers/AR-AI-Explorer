package com.indianservers.aiexplorer.chemistry.repository

import com.indianservers.aiexplorer.chemistry.data.BundledElementData
import com.indianservers.aiexplorer.chemistry.domain.ChemistryDataValidator
import com.indianservers.aiexplorer.chemistry.domain.ChemistryValidationReport
import com.indianservers.aiexplorer.chemistry.model.ChemicalElement
import com.indianservers.aiexplorer.chemistry.model.ElementCategory
import com.indianservers.aiexplorer.chemistry.model.PeriodicTrendProperty

interface ChemistryRepository {
    fun getAllElements(): List<ChemicalElement>
    fun getElementByAtomicNumber(atomicNumber: Int): ChemicalElement?
    fun searchElements(query: String): List<ChemicalElement>
    fun getElementsByCategory(category: ElementCategory): List<ChemicalElement>
    fun getElementsByGroup(group: Int): List<ChemicalElement>
    fun getElementsByPeriod(period: Int): List<ChemicalElement>
    fun getTrendData(property: PeriodicTrendProperty): Map<Int, Double>
    fun compareElements(atomicNumbers: List<Int>): List<ChemicalElement>
    fun validate(): ChemistryValidationReport
}

class OfflineChemistryRepository(
    private val elements: List<ChemicalElement> = BundledElementData.elements,
) : ChemistryRepository {
    private val byAtomicNumber = elements.associateBy { it.atomicNumber }
    private val normalizedIndex = elements.associateBy { it.symbol.lowercase() }

    init {
        val report = ChemistryDataValidator.validate(elements)
        require(report.valid) { report.errors.joinToString("; ") }
    }

    override fun getAllElements(): List<ChemicalElement> = elements
    override fun getElementByAtomicNumber(atomicNumber: Int): ChemicalElement? = byAtomicNumber[atomicNumber]

    override fun searchElements(query: String): List<ChemicalElement> {
        val normalized = query.trim().lowercase()
        if (normalized.isEmpty()) return elements
        normalized.toIntOrNull()?.let { number -> return listOfNotNull(byAtomicNumber[number]) }
        normalizedIndex[normalized]?.let { exact -> return listOf(exact) }
        return elements.filter { it.name.lowercase().contains(normalized) || it.symbol.lowercase().startsWith(normalized) }
    }

    override fun getElementsByCategory(category: ElementCategory): List<ChemicalElement> = elements.filter { it.category == category }
    override fun getElementsByGroup(group: Int): List<ChemicalElement> = elements.filter { it.group == group }
    override fun getElementsByPeriod(period: Int): List<ChemicalElement> = elements.filter { it.period == period }

    override fun getTrendData(property: PeriodicTrendProperty): Map<Int, Double> = elements.mapNotNull { element ->
        val value = when (property) {
            PeriodicTrendProperty.AtomicRadius -> element.physicalProperties.atomicRadiusPm
            PeriodicTrendProperty.Electronegativity -> element.physicalProperties.electronegativityPauling
            PeriodicTrendProperty.FirstIonisationEnergy -> element.physicalProperties.firstIonisationEnergyKjMol
            PeriodicTrendProperty.Density -> element.physicalProperties.densityGcm3
            PeriodicTrendProperty.MeltingPoint -> element.physicalProperties.meltingPointK
            PeriodicTrendProperty.BoilingPoint -> element.physicalProperties.boilingPointK
        }
        value?.let { element.atomicNumber to it }
    }.toMap()

    override fun compareElements(atomicNumbers: List<Int>): List<ChemicalElement> {
        require(atomicNumbers.size in 2..4) { "Compare between two and four elements." }
        return atomicNumbers.distinct().mapNotNull(byAtomicNumber::get)
    }

    override fun validate(): ChemistryValidationReport = ChemistryDataValidator.validate(elements)
}
