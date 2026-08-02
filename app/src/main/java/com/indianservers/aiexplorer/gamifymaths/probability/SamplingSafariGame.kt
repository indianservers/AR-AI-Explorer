package com.indianservers.aiexplorer.gamifymaths.probability

import androidx.compose.ui.graphics.Color

internal object SamplingSafariCatalog {
    val game = ArcadeMiniGame("sampling-safari", "Sampling Safari", "Identify and compare sampling methods.", Color(0xFFC44C65), ArcadeVisual.Sampling, listOf(
        ArcadeChallenge("Every animal has an equal chance of selection. Which method?", listOf("Convenience", "Simple random", "Cluster", "Voluntary"), "Simple random", "Equal selection chance is the key.", "This is a simple random sample."),
        ArcadeChallenge("Sample animals from every habitat group. Which method?", listOf("Systematic", "Stratified", "Convenience", "Voluntary"), "Stratified", "Divide into groups, then sample each group.", "Sampling from every habitat is stratified sampling."),
        ArcadeChallenge("Select every 10th visitor. Which method?", listOf("Cluster", "Systematic", "Simple random", "Convenience"), "Systematic", "Look for a fixed interval.", "Choosing every kth member is systematic sampling."),
    ))
}
