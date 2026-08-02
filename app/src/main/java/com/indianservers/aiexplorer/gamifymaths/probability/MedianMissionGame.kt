package com.indianservers.aiexplorer.gamifymaths.probability

import androidx.compose.ui.graphics.Color

internal object MedianMissionCatalog {
    val game = ArcadeMiniGame("median-mission", "Median Mission", "Order data and locate the middle.", Color(0xFFD8891C), ArcadeVisual.Median, listOf(
        ArcadeChallenge("Find the median of 12, 7, 5, 9, 3, 11, 8.", listOf("7", "8", "9", "11"), "8", "Sort the seven values.", "Sorted: 3,5,7,8,9,11,12. The middle is 8.", listOf(12.0, 7.0, 5.0, 9.0, 3.0, 11.0, 8.0)),
        ArcadeChallenge("Median of 2, 8, 4, 10?", listOf("4", "5", "6", "7"), "6", "Average the two middle values.", "Sorted: 2,4,8,10. Median = (4+8)/2 = 6.", listOf(2.0, 8.0, 4.0, 10.0)),
        ArcadeChallenge("Median of 15, 6, 9, 12, 3?", listOf("6", "8", "9", "12"), "9", "Sort first.", "Sorted: 3,6,9,12,15. The middle is 9.", listOf(15.0, 6.0, 9.0, 12.0, 3.0)),
    ))
}
