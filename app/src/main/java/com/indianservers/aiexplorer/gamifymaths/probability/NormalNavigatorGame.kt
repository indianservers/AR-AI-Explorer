package com.indianservers.aiexplorer.gamifymaths.probability

import androidx.compose.ui.graphics.Color

internal object NormalNavigatorCatalog {
    val game = ArcadeMiniGame("normal-navigator", "Normal Navigator", "Estimate areas under a normal curve.", Color(0xFF2C8DD4), ArcadeVisual.Normal, listOf(
        ArcadeChallenge("Mean 100, SD 15. Approximate area left of 115.", listOf("50%", "68%", "84%", "95%"), "84%", "115 is one standard deviation above the mean.", "About 84% lies below μ + 1σ."),
        ArcadeChallenge("What percent lies within one SD of the mean?", listOf("50%", "68%", "95%", "99.7%"), "68%", "Use the empirical rule.", "About 68% lies within one standard deviation."),
        ArcadeChallenge("For a symmetric normal curve, area above the mean?", listOf("25%", "50%", "68%", "100%"), "50%", "The mean splits the symmetric curve.", "Half the area lies above the mean."),
    ))
}
