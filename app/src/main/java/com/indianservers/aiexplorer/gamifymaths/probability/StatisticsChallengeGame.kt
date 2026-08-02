package com.indianservers.aiexplorer.gamifymaths.probability

import androidx.compose.ui.graphics.Color

internal object StatisticsChallengeCatalog {
    val game = ArcadeMiniGame("statistics-challenge", "Statistics Challenge", "Combine centre, spread and probability skills.", Color(0xFF27A99E), ArcadeVisual.MixedStatistics, listOf(
        ArcadeChallenge("For 3,7,8,7,5,8,7,6, what is the mode?", listOf("6", "7", "8", "No mode"), "7", "Count how often each value appears.", "Seven appears three times, more than any other value.", listOf(3.0, 7.0, 8.0, 7.0, 5.0, 8.0, 7.0, 6.0)),
        ArcadeChallenge("For 2,4,6,8,10, find the range.", listOf("5", "6", "8", "10"), "8", "Maximum minus minimum.", "10 − 2 = 8.", listOf(2.0, 4.0, 6.0, 8.0, 10.0)),
        ArcadeChallenge("Data 4,6,6,8: which pair is correct?", listOf("Mean 6, median 6", "Mean 5, median 6", "Mean 6, median 7", "Mean 7, median 6"), "Mean 6, median 6", "Calculate both measures separately.", "The total is 24, so mean is 6; the middle pair is 6 and 6.", listOf(4.0, 6.0, 6.0, 8.0)),
    ))
}
