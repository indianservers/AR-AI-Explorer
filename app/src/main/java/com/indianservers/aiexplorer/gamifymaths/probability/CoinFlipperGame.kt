package com.indianservers.aiexplorer.gamifymaths.probability

import androidx.compose.ui.graphics.Color

internal object CoinFlipperCatalog {
    val game = ArcadeMiniGame("coin-flipper", "Coin Flipper", "Compare theoretical and experimental coin probability.", Color(0xFFD98712), ArcadeVisual.Coin, listOf(
        ArcadeChallenge("For one fair coin flip, what is P(heads)?", listOf("0", "1/4", "1/2", "1"), "1/2", "Heads and tails are equally likely.", "One of the two outcomes is heads."),
        ArcadeChallenge("Flip two fair coins. What is P(two heads)?", listOf("1/2", "1/3", "1/4", "3/4"), "1/4", "List HH, HT, TH and TT.", "Only HH gives two heads, so 1 of 4 outcomes works."),
        ArcadeChallenge("Flip two fair coins. P(exactly one head)?", listOf("1/4", "1/2", "2/3", "3/4"), "1/2", "HT and TH both work.", "Two of four outcomes have exactly one head."),
    ))
}
