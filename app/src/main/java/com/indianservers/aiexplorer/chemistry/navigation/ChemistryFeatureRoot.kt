package com.indianservers.aiexplorer.chemistry.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.indianservers.aiexplorer.chemistry.formulas.navigation.ChemistryFormulaFeatureRoot
import com.indianservers.aiexplorer.chemistry.home.ChemistryHomeScreen
import com.indianservers.aiexplorer.chemistry.learning.ChemistryConnectedLearningFeature

/** Isolated Chemistry destination host. Later phases can add internal routes without changing the app root. */
@Composable
fun ChemistryFeatureRoot(onExit: () -> Unit) {
    var showFormulaLibrary by remember { mutableStateOf(false) }
    var showConnectedJourney by remember { mutableStateOf(false) }
    if (showConnectedJourney) {
        ChemistryConnectedLearningFeature(onExit = { showConnectedJourney = false })
    } else if (showFormulaLibrary) {
        ChemistryFormulaFeatureRoot(onExit = { showFormulaLibrary = false })
    } else {
        ChemistryHomeScreen(onBack = onExit, onOpenFormulas = { showFormulaLibrary = true }, onOpenConnectedJourney = { showConnectedJourney = true })
    }
}
