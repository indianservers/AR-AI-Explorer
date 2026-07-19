package com.indianservers.aiexplorer.chemistry.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.indianservers.aiexplorer.chemistry.data.BundledElementData
import com.indianservers.aiexplorer.chemistry.domain.ChemistryValidationReport
import com.indianservers.aiexplorer.chemistry.model.ChemicalElement
import com.indianservers.aiexplorer.chemistry.repository.ChemistryRepository
import com.indianservers.aiexplorer.chemistry.repository.OfflineChemistryRepository

data class ChemistryHomeUiState(
    val query: String = "",
    val results: List<ChemicalElement> = emptyList(),
    val selectedElement: ChemicalElement? = null,
    val validation: ChemistryValidationReport = ChemistryValidationReport(0, emptyList(), emptyList()),
    val datasetVersion: String = BundledElementData.DATASET_VERSION,
)

class ChemistryHomeViewModel(
    private val repository: ChemistryRepository = OfflineChemistryRepository(),
) : ViewModel() {
    var state by mutableStateOf(
        ChemistryHomeUiState(
            results = repository.getAllElements().take(12),
            validation = repository.validate(),
        ),
    )
        private set

    fun search(query: String) {
        state = state.copy(query = query, results = repository.searchElements(query).take(24))
    }

    fun select(element: ChemicalElement) {
        state = state.copy(selectedElement = element)
    }

    fun clearSelection() {
        state = state.copy(selectedElement = null)
    }
}
