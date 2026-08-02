package com.indianservers.aiexplorer.gamifymaths.probability

import androidx.compose.ui.graphics.Color

internal object StdDevDetectiveCatalog {
    val game = ArcadeMiniGame("sd-detective", "Std Dev Detective", "Measure and compare the spread of data.", Color(0xFF7651C9), ArcadeVisual.StandardDeviation, listOf(
        ArcadeChallenge("What is the standard deviation of 2, 2, 2?", listOf("0", "1", "2", "3"), "0", "No value differs from the mean.", "With no spread, the standard deviation is zero."),
        ArcadeChallenge("Population SD of 1 and 3?", listOf("0", "1", "2", "4"), "1", "The mean is 2; deviations are -1 and 1.", "Mean squared deviation is 1, so SD = 1."),
        ArcadeChallenge("Which data has greater spread?", listOf("5,5,5", "1,5,9", "4,5,6", "All equal"), "1,5,9", "Compare distances from each mean.", "1,5,9 is spread farthest around its mean."),
    ))
}
