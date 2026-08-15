package com.indianservers.aiexplorer.solver.domain.input

data class RecognitionCorpusCase(
    val id: String,
    val input: String,
    val expectedTopic: String,
    val expectedCanonical: String? = null,
    val expectsAmbiguity: Boolean = false,
)

data class RecognitionQualityReport(
    val total: Int,
    val topicAccuracy: Double,
    val canonicalAccuracy: Double,
    val ambiguityRecall: Double,
    val failures: List<String>,
) {
    val releaseReady: Boolean get() = total >= 20 && topicAccuracy >= .90 && canonicalAccuracy >= .95 && ambiguityRecall == 1.0 && failures.isEmpty()
}

object SchoolRecognitionEvaluationCorpus {
    val cases = listOf(
        RecognitionCorpusCase("pct-en", "What is 25 percent of 80?", "Percentage", "25% of 80"),
        RecognitionCorpusCase("pct-hi-latin", "80 ka 25 pratishat", "Percentage", "25% of 80"),
        RecognitionCorpusCase("pct-hi", "80 का 25 प्रतिशत", "Percentage", "25% of 80"),
        RecognitionCorpusCase("eq-en", "Solve for x: 3x plus 5 equals 20", "Equation", "3x + 5 = 20"),
        RecognitionCorpusCase("eq-hinglish", "3x plus 5 barabar 20 hal karo", "Equation", "3x + 5 = 20"),
        RecognitionCorpusCase("eq-hi", "3x जमा 5 बराबर 20 हल करो", "Equation", "3x + 5 = 20"),
        RecognitionCorpusCase("derivative", "Find the derivative of x^3", "Calculus", "differentiate x^3"),
        RecognitionCorpusCase("integral", "Find the integral of x^2", "Calculus", "integrate x^2"),
        RecognitionCorpusCase("mean", "Calculate the mean of 2, 4, 6, 8", "Statistics", "mean(2, 4, 6, 8)"),
        RecognitionCorpusCase("quadratic", "2x^2 - 7x + 3 = 0", "Quadratic equation"),
        RecognitionCorpusCase("system", "2x+y=7; x-y=2", "System of equations"),
        RecognitionCorpusCase("matrix", "det([[1,2],[3,4]])", "Matrices"),
        RecognitionCorpusCase("stats", "stats 2,3,5,8", "Probability & statistics"),
        RecognitionCorpusCase("calculus", "limit sin(x)/x as x -> 0", "Calculus"),
        RecognitionCorpusCase("number", "gcd(84,30)", "Number theory"),
        RecognitionCorpusCase("arithmetic", "12 + 7 * 3", "Arithmetic"),
        RecognitionCorpusCase("trig-amb", "sin 30", "Trigonometry", expectsAmbiguity = true),
        RecognitionCorpusCase("log-amb", "log 8", "Logarithms", expectsAmbiguity = true),
        RecognitionCorpusCase("fraction-amb", "2/3x", "Algebra", expectsAmbiguity = true),
        RecognitionCorpusCase("trig-clear", "sin(30deg)", "Trigonometry"),
        RecognitionCorpusCase("inequality", "3x + 4 <= 19", "Algebra"),
        RecognitionCorpusCase("regression", "regression x:1,2,3; y:2,4,5", "Probability & statistics"),
    )

    fun evaluate(): RecognitionQualityReport {
        var topics = 0; var canonicals = 0; var canonicalTotal = 0; var ambiguities = 0; var ambiguityTotal = 0
        val failures = mutableListOf<String>()
        cases.forEach { case ->
            val result = SchoolMathInputRecognizer.recognize(case.input)
            if (result?.topic == case.expectedTopic) topics++ else failures += "${case.id}: topic ${result?.topic}"
            case.expectedCanonical?.let { expected -> canonicalTotal++; if (result?.canonicalInput == expected) canonicals++ else failures += "${case.id}: canonical ${result?.canonicalInput}" }
            if (case.expectsAmbiguity) { ambiguityTotal++; if (result?.choices?.size ?: 0 >= 2) ambiguities++ else failures += "${case.id}: ambiguity not surfaced" }
        }
        return RecognitionQualityReport(cases.size, topics.toDouble()/cases.size, if(canonicalTotal==0) 1.0 else canonicals.toDouble()/canonicalTotal, if(ambiguityTotal==0) 1.0 else ambiguities.toDouble()/ambiguityTotal, failures)
    }
}

data class RecognitionDeviceQaCase(val device: String, val inputMethod: String, val expectedFallback: String, val minimumTargetDp: Int)

object SchoolRecognitionDeviceQa {
    val matrix = listOf(
        RecognitionDeviceQaCase("Compact phone", "Camera, touch, keyboard", "Editable text", 48),
        RecognitionDeviceQaCase("Large phone", "Camera, stylus, keyboard", "Editable text", 48),
        RecognitionDeviceQaCase("Tablet", "Camera, stylus, hardware keyboard", "Split-safe scrolling", 48),
        RecognitionDeviceQaCase("Foldable", "Camera, touch, hardware keyboard", "Constraint-responsive scrolling", 48),
        RecognitionDeviceQaCase("Television", "Remote and hardware keyboard", "Camera controls hidden or permission-safe", 48),
    )
}
