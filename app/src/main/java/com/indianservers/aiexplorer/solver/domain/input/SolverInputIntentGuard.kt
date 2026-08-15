package com.indianservers.aiexplorer.solver.domain.input

/**
 * Prevents unknown prose from falling through to symbolic implicit multiplication.
 * This is intentionally conservative: recognized commands continue to the parser,
 * while unsupported word problems fail closed instead of producing plausible noise.
 */
object SolverInputIntentGuard {
    private val supportedCommand = Regex(
        """(?i)^\s*(?:differentiate|derivative\s+of|partial\s+derivative\b|directional\s+derivative\b|gradient\b|divergence\b|curl\b|tangent\s+plane\b|integrate|integral\s+of|limit\b|continuity\b|""" +
            """tangent\b|normal\b|ode\b|complex\b|partial\s+fractions\b|domain\b|""" +
            """matrix(?:add|subtract|multiply)\s*\(|matmul\s*\(|distance\s*\(|midpoint\s*\(|""" +
            """gradient\s*\(|sin\s*\(|cos\s*\(|tan\s*\(|f\s*\(|gcd\s*\(|hcf\s*\(|lcm\s*\(|""" +
            """primefactors?\s*\(|mod\s*\(|base\s*\(|arithmetic(?:term|sum)\s*\(|""" +
            """geometric(?:term|sum)\s*\(|infinitegeometricsum\s*\(|convert|rank\s*\(|""" +
            """transpose\s*\(|rref\s*\(|inverse\s*\(|det\s*\(|mean\s*\(|median\s*\(|""" +
            """standard\s+deviation\s*\(|stats\b|binomial\s+(?:pmf|cdf)\b|normal\s+pdf\b|""" +
            """regression\s+x\s*:|ncr\s*\(|npr\s*\(|combinations\s*\(|permutations\s*\(|log\s+base\b)""",
    )

    private val supportedNaturalLanguage = listOf(
        Regex("""(?i)^\s*find\s+the\s+(?:hcf|gcd)\s+of\s+-?\d+\s+and\s+-?\d+[.?!]?\s*$"""),
    )

    private val supportedConversion = Regex(
        """(?i)^\s*[+-]?\d+(?:/\d+)?\s*(?:deg|degree|degrees|rad|radian|radians)\s*""" +
            """(?:to)?\s*(?:deg|degree|degrees|rad|radian|radians)\s*$""",
    )

    private val unsupportedTopic = Regex(
        """(?i)\b(?:probability|normal\s+probability|expected\s+value|variance|confidence\s+interval|t\s*test|""" +
            """chi\s*square|anova|regression|principal\s+component|markov|moment\s+generating|""" +
            """poisson|bernoulli|hypergeometric|laplace\s+transform|inverse\s+laplace|""" +
            """fourier\s+transform|partial\s+differential|pde\b|contour\s+integral|residue\b|""" +
            """homology|finite\s+integral\s+domain|jordan\s+form|diagonalize|eigenvalues?\b|""" +
            """gram\s+schmidt|line\s+integral|flux\b)\b""",
    )

    fun rejectionReason(source: String): String? {
        val text = source.trim()
        if (text.isBlank()) return null
        if (supportedNaturalLanguage.any { it.matches(text) }) return null
        if (supportedConversion.matches(text)) return null
        if (supportedCommand.containsMatchIn(text)) return null
        if (unsupportedTopic.containsMatchIn(text)) {
            return "This topic or wording has no verified offline Solver strategy yet. Use a supported symbolic command."
        }
        val words = Regex("""[A-Za-z]+""").findAll(text).map { it.value }.toList()
        val startsLikeProse = Regex(
            """(?i)^\s*(?:what|find|solve|prove|evaluate|calculate|show|a|an|the|if|two)\b""",
        ).containsMatchIn(text)
        val containsSentencePunctuation = text.any { it == '?' || it == '!' }
        val unknownPhrase = words.size >= 3 &&
            !Regex("""[=<>+\-*/^%]""").containsMatchIn(text) &&
            !Regex("""\w+\s*\(""").containsMatchIn(text)

        return if (startsLikeProse || containsSentencePunctuation || unknownPhrase) {
            "Natural-language interpretation is not verified for this question. Rewrite it with supported mathematical notation."
        } else {
            null
        }
    }
}
