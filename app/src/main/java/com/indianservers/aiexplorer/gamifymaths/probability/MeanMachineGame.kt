package com.indianservers.aiexplorer.gamifymaths.probability

import androidx.compose.ui.graphics.Color

internal object MeanMachineCatalog {
    val game = ArcadeMiniGame("mean-machine", "Mean Machine", "Calculate and interpret arithmetic means.", Color(0xFF22A5A9), ArcadeVisual.Mean, listOf(
        ArcadeChallenge("Find the mean of 4, 7, 2, 9, 6, 2.", listOf("4", "5", "6", "7"), "5", "Add all values and divide by 6.", "The total is 30; 30/6 = 5.", listOf(4.0, 7.0, 2.0, 9.0, 6.0, 2.0)),
        ArcadeChallenge("Find the mean of 8, 10, 12, 14.", listOf("10", "11", "12", "13"), "11", "Divide the total by 4.", "44/4 = 11.", listOf(8.0, 10.0, 12.0, 14.0)),
        ArcadeChallenge("Mean of 3, 5, 7, 9, 11?", listOf("6", "7", "8", "9"), "7", "The values are symmetric around 7.", "35/5 = 7.", listOf(3.0, 5.0, 7.0, 9.0, 11.0)),
    ))
}
