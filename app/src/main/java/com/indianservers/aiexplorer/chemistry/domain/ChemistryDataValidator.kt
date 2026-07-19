package com.indianservers.aiexplorer.chemistry.domain

import com.indianservers.aiexplorer.chemistry.model.ChemicalElement
import com.indianservers.aiexplorer.chemistry.model.PeriodicBlock

data class ChemistryValidationReport(
    val elementCount: Int,
    val errors: List<String>,
    val warnings: List<String>,
) {
    val valid: Boolean get() = errors.isEmpty()
}

object ChemistryDataValidator {
    fun validate(elements: List<ChemicalElement>): ChemistryValidationReport {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        if (elements.size != 118) errors += "Expected 118 elements, found ${elements.size}."
        if (elements.map { it.atomicNumber }.toSet() != (1..118).toSet()) errors += "Atomic numbers must be unique and continuous from 1 to 118."
        if (elements.map { it.symbol }.toSet().size != elements.size) errors += "Element symbols must be unique."
        elements.forEach { element ->
            val prefix = "${element.atomicNumber} ${element.symbol}"
            if (element.period !in 1..7) errors += "$prefix has invalid period ${element.period}."
            if (element.group != null && element.group !in 1..18) errors += "$prefix has invalid group ${element.group}."
            if (element.neutralElectronCount != element.atomicNumber) errors += "$prefix neutral electron count is invalid."
            if (element.electronConfiguration.electronCount != element.atomicNumber) errors += "$prefix configuration electron total is invalid."
            if (element.electronConfiguration.shellDistribution.sum() != element.atomicNumber) errors += "$prefix shell total is invalid."
            if (element.electronConfiguration.orbitals.any { it.electrons !in 0..it.capacity }) errors += "$prefix exceeds an orbital capacity."
            if (element.representativeIsotope.massNumber < element.atomicNumber) errors += "$prefix representative isotope has fewer nucleons than protons."
            if (element.representativeIsotope.note.isBlank()) errors += "$prefix representative isotope needs an explanatory note."
            if (element.block == PeriodicBlock.F && element.atomicNumber !in 58..71 && element.atomicNumber !in 90..103) errors += "$prefix has invalid f-block placement."
            if (element.atomicNumber in 58..71 || element.atomicNumber in 90..103) {
                if (element.group != null || element.block != PeriodicBlock.F) errors += "$prefix must use separated f-block placement."
            }
            if (element.physicalProperties.electronegativityPauling == null) warnings += "$prefix has no standard Pauling electronegativity value in the Phase 1 core dataset."
        }
        return ChemistryValidationReport(elements.size, errors, warnings)
    }
}
