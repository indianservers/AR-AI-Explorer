package com.indianservers.aiexplorer.gamifymaths.probability

import androidx.compose.ui.graphics.Color

internal object CardPickerCatalog {
    val game = ArcadeMiniGame("card-picker", "Card Picker", "Calculate probabilities from a standard deck.", Color(0xFF3478CF), ArcadeVisual.Cards, listOf(
        ArcadeChallenge("Pick one card from 52. What is P(heart)?", listOf("1/2", "1/4", "1/13", "13/4"), "1/4", "There are 13 hearts in 52 cards.", "13/52 simplifies to 1/4."),
        ArcadeChallenge("Pick one card. What is P(ace)?", listOf("1/4", "1/13", "4/13", "1/52"), "1/13", "There are four aces.", "4/52 simplifies to 1/13."),
        ArcadeChallenge("Pick one card. P(J, Q or K)?", listOf("3/13", "1/4", "1/13", "12/13"), "3/13", "There are 12 face cards.", "12/52 simplifies to 3/13."),
    ))
}
