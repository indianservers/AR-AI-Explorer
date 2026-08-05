package com.indianservers.aiexplorer.features.probabilitystatistics.models

enum class StatisticsLearningLevel(val label: String, val audience: String) {
    Foundation("Foundation", "Class 6-8"),
    School("School Statistics", "Class 9-10"),
    SeniorSecondary("Senior Secondary", "Class 11-12"),
    Undergraduate("Undergraduate", "College foundation"),
    AdvancedUndergraduate("Advanced Undergraduate", "Mathematical statistics"),
    Postgraduate("Postgraduate", "Advanced inference"),
}

enum class StatisticsDifficulty { Starter, Intermediate, Advanced }

enum class MasteryState {
    NotStarted, Exploring, Learning, Practising, Proficient, Mastered
}

enum class TopicWorkspaceMode(val label: String) {
    Overview("Overview"),
    Learn("Learn"),
    Visualize("Visualize"),
    Explore("Explore"),
    Simulate("Simulate"),
    Formula("Formula"),
    Examples("Examples"),
    Practice("Practice"),
    Applications("Applications"),
    Assessment("Assessment"),
}

data class StatisticsCategory(
    val id: String,
    val title: String,
    val icon: String,
    val description: String,
    val difficulty: StatisticsDifficulty,
    val topicIds: List<String>,
)

data class FormulaContent(
    val expression: String,
    val spoken: String,
    val terms: List<Pair<String, String>>,
)

data class WorkedExample(
    val question: String,
    val steps: List<String>,
    val answer: String,
)

data class PracticeQuestion(
    val prompt: String,
    val options: List<String>,
    val answerIndex: Int,
    val explanation: String,
)

data class StatisticsTopic(
    val id: String,
    val categoryId: String,
    val title: String,
    val shortDescription: String,
    val aliases: Set<String>,
    val minimumLevel: StatisticsLearningLevel,
    val difficulty: StatisticsDifficulty,
    val prerequisites: List<String>,
    val learningOutcomes: List<String>,
    val lessonSteps: List<String>,
    val formulas: List<FormulaContent>,
    val examples: List<WorkedExample>,
    val practice: List<PracticeQuestion>,
    val applications: List<String>,
    val relatedTopics: List<String>,
    val estimatedMinutes: Int = 15,
)

data class TopicProgress(
    val topicId: String,
    val mastery: MasteryState = MasteryState.NotStarted,
    val completedModes: Set<TopicWorkspaceMode> = emptySet(),
    val correctAnswers: Int = 0,
    val attempts: Int = 0,
) {
    val percent: Int
        get() = ((completedModes.size / TopicWorkspaceMode.entries.size.toDouble()) * 100).toInt()
}

enum class AnalysisObjective(val label: String) {
    Compare("Compare groups"),
    Associate("Study association"),
    Predict("Predict an outcome"),
    Fit("Check goodness of fit"),
}

enum class OutcomeType(val label: String) {
    Quantitative("Quantitative"),
    Binary("Binary"),
    Categorical("Categorical"),
    TimeSeries("Time series"),
}

enum class GroupStructure(val label: String) {
    One("One sample"),
    TwoIndependent("Two independent groups"),
    TwoPaired("Two paired measurements"),
    Many("More than two groups"),
}

data class StatisticalTestRecommendation(
    val method: String,
    val reason: String,
    val assumptions: List<String>,
    val alternative: String,
    val topicId: String,
    val caution: String = "This guide supports method selection; it does not guarantee a valid scientific analysis.",
)

data class BundledDataset(
    val id: String,
    val title: String,
    val description: String,
    val values: List<Double>,
)
