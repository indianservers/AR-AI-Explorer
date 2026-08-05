package com.indianservers.aiexplorer.learning

internal val theoremCategories = listOf(
    "Foundations & Arithmetic",
    "Algebra",
    "Plane Geometry",
    "Coordinate Geometry",
    "Trigonometry",
    "Linear Algebra",
    "Abstract Algebra",
    "Number Theory",
    "Combinatorics",
    "Graph Theory",
    "Calculus",
    "Real Analysis",
    "Complex Analysis",
    "Differential Equations",
    "Numerical Analysis",
    "Probability",
    "Statistics",
    "Discrete Mathematics",
    "Topology",
    "Optimization",
)

internal fun theoremEntry(
    id: String,
    title: String,
    category: String,
    topic: KnowledgeTopic,
    level: KnowledgeLevel,
    band: TheoremBand,
    statement: String,
    applications: String,
    conditions: String = "standard hypotheses",
    proof: String = "Apply the definitions and reduce the claim to the stated invariant.",
): TheoremCard = TheoremCard(
    id = id,
    title = title,
    topic = topic,
    level = level,
    statement = statement,
    conditions = conditions.split('|').map(String::trim).filter(String::isNotBlank),
    applications = applications.split('|').map(String::trim).filter(String::isNotBlank),
    proofSketch = proof.split('|').map(String::trim).filter(String::isNotBlank),
    category = category,
    tags = (applications.split('|') + category).map(String::trim).filter(String::isNotBlank).distinct(),
    band = band,
)

internal fun expandedTheoremCatalog(): List<TheoremCard> = (
    schoolAndGeometryTheorems() +
        algebraAndDiscreteTheorems() +
        analysisTheorems() +
        probabilityAndAdvancedTheorems()
).also { catalog ->
    require(catalog.map(TheoremCard::id).distinct().size == catalog.size) { "Theorem IDs must be unique." }
    require(catalog.all { it.category in theoremCategories }) { "Every theorem must use a registered category." }
}
