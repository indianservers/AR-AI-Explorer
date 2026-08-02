package com.indianservers.aiexplorer.gamifymaths.probability

import androidx.compose.ui.graphics.Color

internal object TreeBuilderCatalog {
    val game = ArcadeMiniGame("tree-builder", "Tree Builder", "Multiply probabilities along paths and add paths.", Color(0xFF54AE2C), ArcadeVisual.Tree, listOf(
        ArcadeChallenge("Two fair coin flips: P(H then H)?", listOf("1/2", "1/3", "1/4", "3/4"), "1/4", "Multiply probabilities along one path.", "1/2 × 1/2 = 1/4."),
        ArcadeChallenge("Bag: 3 red, 2 blue. Without replacement, P(red then blue)?", listOf("1/5", "3/10", "2/5", "3/5"), "3/10", "The second draw has four balls remaining.", "3/5 × 2/4 = 6/20 = 3/10."),
        ArcadeChallenge("Two coin flips: P(at least one head)?", listOf("1/4", "1/2", "3/4", "1"), "3/4", "Use 1 − P(no heads).", "Only TT has no heads, so 3 of 4 outcomes work."),
    ))
}
