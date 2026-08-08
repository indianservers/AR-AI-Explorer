package com.indianservers.aiexplorer.solver

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class SolverNavigationBoundaryTest {
    @Test
    fun additiveSolverRouteKeepsLegacyToolsAndWorkspacesPresent() {
        val source = File("src/main/java/com/indianservers/aiexplorer/MainActivity.kt").readText()
        listOf(
            "\"Solver\" -> vm.openSolver()",
            "\"Problem Solver\" -> vm.openProblemSolver()",
            "\"Math Camera\" -> vm.openMathCamera()",
            "MathModule.Geometry2D -> Geometry2DScreen",
            "MathModule.Geometry3D -> Geometry3DScreen",
            "MathModule.Graph2D -> Graph2DScreen",
            "MathModule.Graph3D -> Graph3DScreen",
        ).forEach { route -> assertTrue("Missing non-regression route: $route", route in source) }
    }

    @Test
    fun homeCategoriesOpenPrimaryDestinationsWithoutIntermediateToolPanels() {
        val source = File("src/main/java/com/indianservers/aiexplorer/MainActivity.kt").readText()
        listOf(
            "\"Solve & Calculate\" -> \"Solver\"",
            "\"Data & Probability\" -> \"Probability & Statistics\"",
            "\"Formulas & Proofs\" -> \"Formulas\"",
            "\"Reference & Logic\" -> \"Visual Dictionary\"",
            "allTools.firstOrNull { it.title == primaryToolTitle }?.let(::openOption)",
        ).forEach { route -> assertTrue("Missing direct home category route: $route", route in source) }
    }
}
