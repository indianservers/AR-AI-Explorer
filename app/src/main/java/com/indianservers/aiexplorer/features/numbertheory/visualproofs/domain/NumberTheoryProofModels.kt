package com.indianservers.aiexplorer.features.numbertheory.visualproofs.domain

enum class NumberTheoryProofLevel(val label: String) {
    Foundation("Foundation"),
    School("School"),
    SeniorSecondary("Senior Secondary"),
    Undergraduate("Undergraduate"),
}

enum class NumberTheoryProofCategory(val title: String, val icon: String, val description: String) {
    Patterns("Number Patterns", "1,2,3", "See sums and sequences become shapes."),
    Divisibility("Divisibility", "÷", "Understand place-value divisibility tests."),
    Factors("Factors and Multiples", "×", "Build factors, GCD and LCM with arrays."),
    Primes("Prime Numbers", "P", "Explore sieves and unique prime building blocks."),
    Modular("Remainders and Modular Arithmetic", "mod", "Walk through remainder classes and clock arithmetic."),
    Powers("Powers and Exponents", "aⁿ", "See exponent laws through factor chains and areas."),
    Special("Special Numbers", "★", "Investigate figurate, perfect and digital-root patterns."),
    GcdLcm("GCD and LCM", "G/L", "Compare equal grouping, cycles and Euclid's algorithm."),
    Representation("Integer Representation", "ℤ", "Represent integers with chips, bases and sums."),
    Classical("Classical Theorems", "QED", "Build formal arguments from visual invariants."),
}

enum class NumberTheoryVisualModel {
    Staircase, TileGrid, BarStaircase, PlaceValueBlocks, FactorRectangle,
    CycleTrack, EuclideanRectangle, PrimeFactorTree, SieveGrid, ModularClock,
    NumberLine, RemainderBuckets, ExponentChain, DivisorMap,
}

enum class NumberTheoryWorkspaceSection(val label: String) {
    Discover("Discover"),
    VisualProof("Visual Proof"),
    Steps("Step-by-Step"),
    Formula("Formula"),
    TryValues("Try Values"),
    PatternTable("Pattern Table"),
    Why("Why It Works"),
    Mistake("Common Mistake"),
    Practice("Practice"),
    Challenge("Challenge"),
}

enum class NumberTheoryProofMode(val label: String) {
    Guided("Guided"), Explore("Free Explore"), AutoPlay("Auto-play"), Challenge("Challenge"), Explanation("Explanation"),
}

data class NumberTheoryParameter(
    val key: String,
    val label: String,
    val minimum: Int,
    val maximum: Int,
    val initial: Int,
)

data class NumberTheoryProofStep(
    val id: String,
    val instruction: String,
    val observation: String,
    val expression: String,
    val spokenExpression: String,
)

data class NumberTheoryPractice(
    val prompt: String,
    val options: List<String>,
    val answerIndex: Int,
    val explanation: String,
)

data class NumberTheoryVisualProofTopic(
    val id: String,
    val category: NumberTheoryProofCategory,
    val title: String,
    val aliases: Set<String>,
    val level: NumberTheoryProofLevel,
    val visualModel: NumberTheoryVisualModel,
    val statement: String,
    val spokenStatement: String,
    val parameters: List<NumberTheoryParameter>,
    val steps: List<NumberTheoryProofStep>,
    val discoveryQuestion: String,
    val whyItWorks: List<String>,
    val commonMistake: String,
    val practice: NumberTheoryPractice,
    val completedInPhase: Int,
)

data class EuclideanDivisionStep(val dividend: Int, val divisor: Int, val quotient: Int, val remainder: Int)

data class NumberTheoryProofEvidence(
    val values: Map<String, Long>,
    val labels: Map<String, String>,
    val sequence: List<Long> = emptyList(),
    val factorPairs: List<Pair<Int, Int>> = emptyList(),
    val primeFactors: Map<Int, Int> = emptyMap(),
    val euclideanSteps: List<EuclideanDivisionStep> = emptyList(),
    val primes: List<Int> = emptyList(),
    val crossed: Set<Int> = emptySet(),
    val holds: Boolean,
    val accessibilityDescription: String,
)

sealed interface NumberTheoryProofState {
    data class Ready(
        val topic: NumberTheoryVisualProofTopic,
        val stepIndex: Int,
        val parameters: Map<String, Int>,
        val evidence: NumberTheoryProofEvidence,
        val section: NumberTheoryWorkspaceSection = NumberTheoryWorkspaceSection.Discover,
        val mode: NumberTheoryProofMode = NumberTheoryProofMode.Guided,
        val formulaRevealed: Boolean = false,
        val prediction: String = "",
        val predictionCorrect: Boolean? = null,
        val playing: Boolean = false,
        val reducedMotion: Boolean = false,
        val replayTarget: Int? = null,
        val completed: Boolean = false,
    ) : NumberTheoryProofState

    data class Error(val message: String) : NumberTheoryProofState
}

sealed interface NumberTheoryProofAction {
    data object Next : NumberTheoryProofAction
    data object Previous : NumberTheoryProofAction
    data object Reset : NumberTheoryProofAction
    data object RevealFormula : NumberTheoryProofAction
    data object TogglePlaying : NumberTheoryProofAction
    data object ReplayStep : NumberTheoryProofAction
    data class UpdateParameter(val key: String, val value: Int) : NumberTheoryProofAction
    data class SelectSection(val section: NumberTheoryWorkspaceSection) : NumberTheoryProofAction
    data class SelectMode(val mode: NumberTheoryProofMode) : NumberTheoryProofAction
    data class SubmitPrediction(val answer: String) : NumberTheoryProofAction
    data class SetReducedMotion(val enabled: Boolean) : NumberTheoryProofAction
}
