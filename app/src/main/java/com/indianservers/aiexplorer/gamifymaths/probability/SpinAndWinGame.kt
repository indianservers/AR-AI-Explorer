package com.indianservers.aiexplorer.gamifymaths.probability

import androidx.compose.ui.graphics.Color

internal object SpinAndWinCatalog {
    val game = ArcadeMiniGame("spin-win", "Spin & Win", "Predict spinner outcomes and compare trials.", Color(0xFF20A7A7), ArcadeVisual.Spinner, listOf(
        ArcadeChallenge("A fair spinner has 6 equal sectors and 1 is yellow. P(yellow)?", listOf("1/2", "1/3", "1/6", "5/6"), "1/6", "One favourable sector out of six.", "The spinner has 1 yellow sector among 6 equal sectors."),
        ArcadeChallenge("Two of 6 equal sectors are blue. P(blue)?", listOf("1/6", "1/3", "1/2", "2/3"), "1/3", "Simplify 2/6.", "Two blue sectors out of six gives 2/6 = 1/3."),
        ArcadeChallenge("Only one sector is red. P(not red)?", listOf("1/6", "1/3", "2/3", "5/6"), "5/6", "Use the complement of P(red).", "Five of the six sectors are not red."),
    ))
}
