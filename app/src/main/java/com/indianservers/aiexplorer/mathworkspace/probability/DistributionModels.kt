package com.indianservers.aiexplorer.mathworkspace.probability

enum class DistributionKind(val title: String, val discrete: Boolean) {
    Normal("Normal", false), Uniform("Uniform", false), Exponential("Exponential", false), Binomial("Binomial", true), Poisson("Poisson", true)
}
enum class ProbabilityMode(val title: String) { Distributions("Distributions"), Sampling("Sampling"), Simulation("Simulation") }
data class DistributionParameters(val first: Double = 0.0, val second: Double = 1.0, val trials: Int = 10, val probability: Double = .5)
data class DistributionViewport(val centerX: Double = 0.0, val centerY: Double = 0.0, val scaleX: Double = 1.0, val scaleY: Double = 1.0)
