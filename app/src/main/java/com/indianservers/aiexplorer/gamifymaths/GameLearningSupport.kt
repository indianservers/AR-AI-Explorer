package com.indianservers.aiexplorer.gamifymaths

internal enum class GameLearningPhase(val label: String, val goal: String) {
    Explore("LEARN", "Use the model to identify the mathematical idea."),
    Practise("PRACTISE", "Choose a method, calculate, and explain the key step."),
    Master("MASTER", "Solve independently, then verify using a second method."),
}

internal data class GameLearningGuidance(
    val phase: GameLearningPhase,
    val objective: String,
    val hint: String,
    val retrySteps: List<String>,
    val reflection: String,
)

internal object GameLearningCoach {
    fun phase(level: Int): GameLearningPhase = when ((level.coerceAtLeast(1) - 1) % 3) {
        0 -> GameLearningPhase.Explore
        1 -> GameLearningPhase.Practise
        else -> GameLearningPhase.Master
    }

    fun guidance(level: Int, prompt: String, explanation: String = ""): GameLearningGuidance {
        val phase = phase(level)
        val normalized = "$prompt $explanation".lowercase()
        val (objective, hint, check) = when {
            "quadrant" in normalized || "coordinate" in normalized ->
                Triple("Connect coordinate signs to position on the plane.", "Read x first, then y. Quadrant II has x < 0 and y > 0.", "Check the sign pair against (+,+), (−,+), (−,−), (+,−).")
            "slope" in normalized ->
                Triple("Interpret slope as vertical change per horizontal change.", "Mark rise and run separately before dividing.", "Substitute both points into (y₂−y₁)/(x₂−x₁).")
            "mean" in normalized || "average" in normalized ->
                Triple("Redistribute the total equally across all values.", "Add every value once, then count how many values there are.", "Multiply your mean by the count; it should recover the original total.")
            "median" in normalized ->
                Triple("Find the centre of an ordered data set.", "Sort first. For an even count, average the two middle values.", "Count equally inward from both ends of the ordered list.")
            "range" in normalized ->
                Triple("Measure the spread from the smallest to largest value.", "Identify the maximum and minimum before subtracting.", "Your range cannot exceed the maximum or be negative.")
            "probab" in normalized || "chance" in normalized || "coin" in normalized || "die" in normalized || "card" in normalized ->
                Triple("Compare favourable outcomes with all equally likely outcomes.", "Write the sample space, then mark favourable outcomes.", "Probability = favourable ÷ total; confirm it lies from 0 to 1.")
            "fraction" in normalized || "/" in prompt ->
                Triple("Connect fraction symbols to equal-sized visual parts.", "Use a common denominator or scale numerator and denominator by the same factor.", "Check equivalence by cross multiplication or with the fraction model.")
            "ratio" in normalized || "rate" in normalized || "proportion" in normalized ->
                Triple("Preserve a multiplicative relationship between quantities.", "Find the scale factor or first calculate the value for one unit.", "Divide corresponding quantities; the ratios should agree.")
            "area" in normalized ->
                Triple("Connect dimensions to the number of square units covering a shape.", "Identify the perpendicular dimensions and the shape’s area model.", "Estimate first, then verify the unit is squared.")
            "volume" in normalized ->
                Triple("Connect base layers and height to cubic units.", "Find one layer’s area, then multiply by the number of layers.", "Verify the answer uses cubic units and grows with every dimension.")
            "angle" in normalized || "triangle" in normalized ->
                Triple("Use invariant angle relationships instead of guessing from appearance.", "Label known angles and write the relevant total before subtracting.", "Substitute your result back into the angle sum.")
            "equation" in normalized || " x" in normalized || "x " in normalized ->
                Triple("Preserve equality while isolating the unknown.", "Apply the inverse operation to both sides of the balance.", "Substitute your solution into the original equation.")
            "prime" in normalized || "factor" in normalized || "multiple" in normalized ->
                Triple("Use divisibility and factor structure to classify the number.", "Test small prime divisors systematically.", "List factor pairs; a prime has exactly 1 and itself.")
            "sequence" in normalized || "pattern" in normalized ->
                Triple("Identify how one term changes into the next.", "Compare consecutive differences, then ratios.", "Apply the same rule to at least two earlier transitions.")
            else ->
                Triple("Translate the mission into known quantities, a relationship, and an unknown.", "Underline what is given and what must be found; choose one matching rule.", "Estimate and reverse the operation to verify your result.")
        }
        return GameLearningGuidance(
            phase = phase,
            objective = objective,
            hint = hint,
            retrySteps = listOf("Name the given information.", "Choose the relationship before calculating.", check),
            reflection = when (phase) {
                GameLearningPhase.Explore -> "What feature of the visual helped you decide?"
                GameLearningPhase.Practise -> "Which step did the mathematical work?"
                GameLearningPhase.Master -> "How could you verify this without repeating the same method?"
            },
        )
    }
}
