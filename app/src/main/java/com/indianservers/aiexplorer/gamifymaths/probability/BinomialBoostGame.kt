package com.indianservers.aiexplorer.gamifymaths.probability

import androidx.compose.ui.graphics.Color

internal object BinomialBoostCatalog {
    val game = ArcadeMiniGame("binomial-boost", "Binomial Boost", "Calculate exact-success probabilities.", Color(0xFF64B72C), ArcadeVisual.Binomial, listOf(
        ArcadeChallenge("Fair coin flipped 6 times. P(exactly 4 heads)?", listOf("5/32", "15/64", "3/8", "1/2"), "15/64", "Use 6C4(1/2)^6.", "6C4 = 15, so the probability is 15/64.", listOf(1.0, 6.0, 15.0, 20.0, 15.0, 6.0, 1.0)),
        ArcadeChallenge("Fair coin flipped 4 times. P(exactly 2 heads)?", listOf("1/4", "3/8", "1/2", "5/8"), "3/8", "Use 4C2/16.", "6/16 simplifies to 3/8.", listOf(1.0, 4.0, 6.0, 4.0, 1.0)),
        ArcadeChallenge("Fair coin flipped 5 times. P(no heads)?", listOf("1/32", "1/16", "1/8", "1/5"), "1/32", "Only TTTTT works.", "(1/2)^5 = 1/32.", listOf(1.0, 5.0, 10.0, 10.0, 5.0, 1.0)),
    ))
}
