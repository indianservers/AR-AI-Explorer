package com.indianservers.aiexplorer.biology.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.indianservers.aiexplorer.biology.learning.BiologyConnectedLearningFeature
import com.indianservers.aiexplorer.biology.ui.*

@Composable
fun BiologyFeatureRoot(onExit: () -> Unit, vm: BiologyFeatureViewModel = viewModel()) {
    var showConnectedJourney by remember { mutableStateOf(false) }
    if (showConnectedJourney) {
        BiologyConnectedLearningFeature(onExit = { showConnectedJourney = false })
        return
    }
    BackHandler { if (!vm.back()) onExit() }
    when (val route = vm.state.route) {
        BiologyRoute.Home -> BiologyHomePage(vm, onExit, onOpenConnectedJourney = { showConnectedJourney = true })
        BiologyRoute.Catalogue -> BiologyCataloguePage(vm)
        is BiologyRoute.Domain -> BiologyDomainPage(vm, route.domainId)
        is BiologyRoute.Unit -> BiologyUnitPage(vm, route.unitId)
        is BiologyRoute.Chapter -> BiologyChapterPage(vm, route.chapterId)
        is BiologyRoute.Topic -> BiologyTopicPage(vm, route.topicId)
        is BiologyRoute.Concept -> BiologyConceptPage(vm, route.conceptId)
        BiologyRoute.Search -> BiologySearchPage(vm)
        BiologyRoute.Glossary -> BiologyGlossaryPage(vm)
        BiologyRoute.Bookmarks -> BiologyBookmarksPage(vm)
        BiologyRoute.Progress -> BiologyProgressPage(vm)
        is BiologyRoute.Future3D -> PlannedBiologyPage(vm, "Interactive 3D model planned for a future update.")
        is BiologyRoute.Quiz -> PlannedBiologyPage(vm, "Quiz foundation is registered; question sessions arrive in Phase 6.")
        is BiologyRoute.Revision -> PlannedBiologyPage(vm, "Revision route is ready; curated revision sets arrive in Phase 6.")
        is BiologyRoute.Diagram -> PlannedBiologyPage(vm, "The labelled 2D diagram viewer arrives in Phase 5.")
    }
}
