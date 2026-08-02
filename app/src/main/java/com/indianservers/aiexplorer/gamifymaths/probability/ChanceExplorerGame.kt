package com.indianservers.aiexplorer.gamifymaths.probability

import androidx.compose.ui.graphics.Color

internal object ChanceExplorerCatalog {
    val game = ArcadeMiniGame("chance-explorer", "Chance Explorer", "Find probabilities from equally likely outcomes.", Color(0xFF8D55D7), ArcadeVisual.Bag, listOf(
        ArcadeChallenge("A bag has 3 red, 2 blue and 5 green balls. P(red)?", listOf("1/10", "2/10", "3/10", "5/10"), "3/10", "Favourable outcomes divided by total outcomes.", "There are 3 red balls among 10 total."),
        ArcadeChallenge("Using the same bag, what is P(blue)?", listOf("1/5", "1/3", "1/2", "2/5"), "1/5", "Simplify 2/10.", "Two blue balls out of ten gives 2/10 = 1/5."),
        ArcadeChallenge("Using the same bag, what is P(not green)?", listOf("1/5", "3/10", "1/2", "7/10"), "1/2", "Count red and blue outcomes together.", "Five of ten balls are not green, so the probability is 1/2."),
    ))
}
