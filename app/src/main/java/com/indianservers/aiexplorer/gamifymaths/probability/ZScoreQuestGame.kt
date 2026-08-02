package com.indianservers.aiexplorer.gamifymaths.probability

import androidx.compose.ui.graphics.Color

internal object ZScoreQuestCatalog {
    val game = ArcadeMiniGame("z-score-quest", "Z-Score Quest", "Standardise values and compare positions.", Color(0xFFD28417), ArcadeVisual.ZScore, listOf(
        ArcadeChallenge("Value 135, mean 100, SD 10. Find z.", listOf("2.5", "3", "3.5", "4"), "3.5", "z = (value − mean)/SD.", "(135 − 100)/10 = 3.5."),
        ArcadeChallenge("Value 85, mean 100, SD 5. Find z.", listOf("-3", "-2", "2", "3"), "-3", "Subtract the mean before dividing.", "(85 − 100)/5 = -3."),
        ArcadeChallenge("Value 120, mean 100, SD 10. Find z.", listOf("1", "2", "10", "20"), "2", "The value is 20 above the mean.", "20/10 = 2."),
    ))
}
