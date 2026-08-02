package com.indianservers.aiexplorer.gamifymaths.probability

import androidx.compose.ui.graphics.Color

internal object PermutationsProCatalog {
    val game = ArcadeMiniGame("permutations-pro", "Permutations Pro", "Arrange objects when order matters.", Color(0xFF2E91D7), ArcadeVisual.Permutation, listOf(
        ArcadeChallenge("How many arrangements of A, B and C?", listOf("3", "6", "8", "9"), "6", "Use 3 factorial.", "3! = 3 × 2 × 1 = 6."),
        ArcadeChallenge("How many ways can 4 different books be ordered?", listOf("12", "16", "20", "24"), "24", "Use 4 factorial.", "4! = 24."),
        ArcadeChallenge("How many arrangements of the distinct letters MATH?", listOf("4", "12", "16", "24"), "24", "All four letters are different.", "4! = 24 arrangements."),
    ))
}
