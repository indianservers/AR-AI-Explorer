package com.indianservers.aiexplorer.core

data class ComparisonAttribute(
    val label: String,
    val value: String,
)

data class ComparisonItem(
    val id: String,
    val title: String,
    val primary: String,
    val attributes: List<ComparisonAttribute>,
)

data class ComparisonRow(
    val label: String,
    val left: String,
    val right: String,
    val matches: Boolean,
)

data class ComparisonReport(
    val left: ComparisonItem,
    val right: ComparisonItem,
    val rows: List<ComparisonRow>,
) {
    val sharedCount: Int get() = rows.count(ComparisonRow::matches)
    val differenceCount: Int get() = rows.size - sharedCount
}

object CompareModeEngine {
    fun compare(left: ComparisonItem, right: ComparisonItem): ComparisonReport {
        require(left.id != right.id) { "Compare mode needs two different items." }
        val labels = (left.attributes.map { it.label } + right.attributes.map { it.label }).distinct()
        val leftValues = left.attributes.associate { it.label to it.value }
        val rightValues = right.attributes.associate { it.label to it.value }
        val rows = labels.map { label ->
            val leftValue = leftValues[label].orEmpty().ifBlank { "Not applicable" }
            val rightValue = rightValues[label].orEmpty().ifBlank { "Not applicable" }
            ComparisonRow(label, leftValue, rightValue, leftValue.equals(rightValue, ignoreCase = true))
        }
        return ComparisonReport(left, right, rows)
    }

    fun proof(lab: VisualProofLab): ComparisonItem {
        val certificate = VisualProofCatalog.certificateFor(lab.id)
        return ComparisonItem(
            id = lab.id,
            title = lab.title,
            primary = lab.formalResult,
            attributes = listOf(
                ComparisonAttribute("Topic", lab.topic),
                ComparisonAttribute("Method", certificate.method),
                ComparisonAttribute("Steps", lab.steps.size.toString()),
                ComparisonAttribute("Controls", lab.parameters.size.toString()),
                ComparisonAttribute("Invariant", lab.invariantPrompt),
                ComparisonAttribute("Assumptions", certificate.assumptions.joinToString()),
            ),
        )
    }

    fun distribution(distribution: ProbabilityDistribution): ComparisonItem {
        val summary = distribution.summary
        return ComparisonItem(
            id = summary.kind.name,
            title = summary.kind.name,
            primary = summary.parameters.entries.joinToString(", ") { "${it.key}=${format(it.value)}" },
            attributes = listOf(
                ComparisonAttribute("Type", summary.domain.name),
                ComparisonAttribute("Mean", format(summary.mean)),
                ComparisonAttribute("Variance", format(summary.variance)),
                ComparisonAttribute("Std dev", format(summary.standardDeviation)),
                ComparisonAttribute("Median", format(distribution.quantile(.5))),
                ComparisonAttribute("90th percentile", format(distribution.quantile(.9))),
            ),
        )
    }

    private fun format(value: Double): String = when {
        !value.isFinite() -> value.toString()
        kotlin.math.abs(value) >= 1_000 || (value != 0.0 && kotlin.math.abs(value) < .001) ->
            "%.3e".format(java.util.Locale.US, value)
        else -> "%.4f".format(java.util.Locale.US, value).trimEnd('0').trimEnd('.')
    }
}
