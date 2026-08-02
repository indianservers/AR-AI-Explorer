package com.indianservers.aiexplorer.gamifymaths.probability

import androidx.compose.ui.graphics.Color

internal object CombinationsChampCatalog {
    val game = ArcadeMiniGame("combinations-champ", "Combinations Champ", "Choose groups when order does not matter.", Color(0xFF7451C8), ArcadeVisual.Combination, listOf(
        ArcadeChallenge("Choose 2 students from 5. How many groups?", listOf("5", "10", "15", "20"), "10", "Use 5 choose 2.", "5C2 = 10."),
        ArcadeChallenge("Choose 3 toppings from 6. How many sets?", listOf("12", "18", "20", "24"), "20", "Use 6 choose 3.", "6C3 = 20."),
        ArcadeChallenge("Choose 2 finalists from 8. How many pairs?", listOf("16", "24", "28", "32"), "28", "Use 8 choose 2.", "8C2 = 28."),
    ))
}
