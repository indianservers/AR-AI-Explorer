package com.indianservers.aiexplorer.gamifymaths.probability

import androidx.compose.ui.graphics.Color

internal enum class ArcadeVisual {
    Bag, Spinner, Coin, Cards, Tree, Permutation, Combination, Binomial, Normal, ZScore,
    Sampling, Mean, Median, StandardDeviation, MixedStatistics,
}

internal data class ArcadeChallenge(
    val prompt: String,
    val choices: List<String>,
    val answer: String,
    val hint: String,
    val explanation: String,
    val values: List<Double> = emptyList(),
)

internal data class ArcadeMiniGame(
    val id: String,
    val title: String,
    val goal: String,
    val accent: Color,
    val visual: ArcadeVisual,
    val levels: List<ArcadeChallenge>,
)

internal val ProbabilityArcadeGames: List<ArcadeMiniGame>
    get() = listOf(
        ChanceExplorerCatalog.game,
        SpinAndWinCatalog.game,
        CoinFlipperCatalog.game,
        CardPickerCatalog.game,
        TreeBuilderCatalog.game,
        PermutationsProCatalog.game,
        CombinationsChampCatalog.game,
        BinomialBoostCatalog.game,
        NormalNavigatorCatalog.game,
        ZScoreQuestCatalog.game,
        SamplingSafariCatalog.game,
        MeanMachineCatalog.game,
        MedianMissionCatalog.game,
        StdDevDetectiveCatalog.game,
        StatisticsChallengeCatalog.game,
    )
