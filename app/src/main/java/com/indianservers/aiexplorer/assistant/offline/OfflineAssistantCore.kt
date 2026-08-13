package com.indianservers.aiexplorer.assistant.offline

import com.indianservers.aiexplorer.assistant.contracts.AssistantConversationMemory
import com.indianservers.aiexplorer.assistant.contracts.AssistantIntent
import com.indianservers.aiexplorer.assistant.contracts.AssistantKnowledgeKind
import com.indianservers.aiexplorer.assistant.contracts.AssistantKnowledgeSearchResult
import com.indianservers.aiexplorer.assistant.contracts.AssistantMemoryEvent
import com.indianservers.aiexplorer.assistant.contracts.AssistantMemoryEventType
import com.indianservers.aiexplorer.curriculum.SchoolClassLevel
import com.indianservers.aiexplorer.curriculum.SchoolSubject
import com.indianservers.aiexplorer.learningintelligence.reference.LearningIntelligenceCatalog
import java.util.Locale
import kotlin.math.min

object OfflineAssistantIntentClassifier {
    private val rules = listOf(
        AssistantIntent.OCR_REPAIR to listOf("ocr", "camera", "photo", "scan", "wrong reading", "read this"),
        AssistantIntent.WORKSPACE_ACTION to listOf("delete", "clear", "add", "rotate", "resize", "move", "select", "undo", "draw", "construct"),
        AssistantIntent.VIVA to listOf("viva", "oral", "ask me", "interview", "practice speaking"),
        AssistantIntent.QUIZ to listOf("quiz", "test me", "mcq", "question me", "practice question"),
        AssistantIntent.HINT to listOf("hint", "clue", "next step", "stuck", "help me continue"),
        AssistantIntent.SOLVE to listOf("solve", "calculate", "answer", "simplify", "factor", "evaluate", "find"),
        AssistantIntent.FORMULA to listOf("formula", "equation for", "identity", "derive"),
        AssistantIntent.DIAGRAM_HELP to listOf("diagram", "graph", "visual", "figure", "shape", "3d", "2d"),
        AssistantIntent.DEFINITION to listOf("define", "definition", "meaning", "what is"),
        AssistantIntent.SEARCH to listOf("search", "find lesson", "show topic", "where is"),
        AssistantIntent.EXPLAIN to listOf("explain", "why", "how", "concept", "understand"),
    )

    fun classify(text: String): AssistantIntent {
        val normalized = text.lowercase(Locale.US)
        if (normalized.isBlank()) return AssistantIntent.UNKNOWN
        return rules
            .map { (intent, keywords) -> intent to keywords.count { it in normalized } }
            .filter { it.second > 0 }
            .maxWithOrNull(compareBy<Pair<AssistantIntent, Int>> { it.second }.thenByDescending { priority(it.first) })
            ?.first ?: AssistantIntent.UNKNOWN
    }

    private fun priority(intent: AssistantIntent): Int = when (intent) {
        AssistantIntent.OCR_REPAIR -> 11
        AssistantIntent.WORKSPACE_ACTION -> 10
        AssistantIntent.VIVA -> 9
        AssistantIntent.QUIZ -> 8
        AssistantIntent.HINT -> 7
        AssistantIntent.SOLVE -> 6
        AssistantIntent.FORMULA -> 5
        AssistantIntent.DIAGRAM_HELP -> 4
        AssistantIntent.DEFINITION -> 3
        AssistantIntent.SEARCH -> 2
        AssistantIntent.EXPLAIN -> 1
        AssistantIntent.UNKNOWN -> 0
    }
}

data class AssistantKnowledgeEntry(
    val id: String,
    val kind: AssistantKnowledgeKind,
    val title: String,
    val text: String,
    val conceptId: String? = null,
    val subject: SchoolSubject? = null,
)

class LocalAssistantKnowledgeIndex(
    private val entries: List<AssistantKnowledgeEntry> = fromLearningIntelligence(),
) {
    fun search(query: String, limit: Int = 6, conceptId: String? = null): List<AssistantKnowledgeSearchResult> {
        val tokens = tokenize(query)
        if (tokens.isEmpty()) return emptyList()
        return entries.asSequence()
            .filter { conceptId == null || it.conceptId == conceptId }
            .mapNotNull { entry ->
                val haystack = "${entry.title} ${entry.text}".lowercase(Locale.US)
                val title = entry.title.lowercase(Locale.US)
                val score = tokens.sumOf { token ->
                    when {
                        token in title -> 3.0
                        token in haystack -> 1.0
                        else -> 0.0
                    }
                } + if (conceptId != null && entry.conceptId == conceptId) .75 else 0.0
                if (score <= 0.0) null else AssistantKnowledgeSearchResult(
                    id = entry.id,
                    kind = entry.kind,
                    title = entry.title,
                    snippet = entry.text.take(180),
                    conceptId = entry.conceptId,
                    subject = entry.subject,
                    score = score,
                )
            }
            .sortedWith(compareByDescending<AssistantKnowledgeSearchResult> { it.score }.thenBy { it.title })
            .take(limit.coerceIn(1, 20))
            .toList()
    }

    companion object {
        fun fromLearningIntelligence(): List<AssistantKnowledgeEntry> = buildList {
            LearningIntelligenceCatalog.concepts.forEach { concept ->
                val title = concept.conceptId.readableId()
                add(AssistantKnowledgeEntry(concept.conceptId, AssistantKnowledgeKind.CONCEPT, title, concept.diagnosticQuestions.firstOrNull()?.prompt.orEmpty(), concept.conceptId, concept.subject))
                concept.diagnosticQuestions.forEach { question ->
                    add(AssistantKnowledgeEntry(question.id, AssistantKnowledgeKind.DIAGNOSTIC_QUESTION, question.prompt, question.options.joinToString(" "), concept.conceptId, concept.subject))
                }
                concept.hints.forEach { hint ->
                    add(AssistantKnowledgeEntry(hint.id, AssistantKnowledgeKind.HINT, "${title} ${hint.level.name.readableEnum()}", hint.text, concept.conceptId, concept.subject))
                }
                concept.misconceptions.forEach { misconception ->
                    add(AssistantKnowledgeEntry(misconception.id, AssistantKnowledgeKind.MISCONCEPTION, misconception.title, misconception.description, concept.conceptId, concept.subject))
                }
                add(AssistantKnowledgeEntry(concept.workedExample.id, AssistantKnowledgeKind.WORKED_EXAMPLE, "$title worked example", concept.workedExample.steps.joinToString(" ") { it.instruction + " " + it.explanation }, concept.conceptId, concept.subject))
                concept.workedExample.steps.forEach { step ->
                    add(AssistantKnowledgeEntry(step.id, AssistantKnowledgeKind.WORKED_STEP, step.instruction, step.explanation + " " + step.expression.orEmpty(), concept.conceptId, concept.subject))
                }
            }
        }
    }
}

object AssistantConversationMemoryReducer {
    fun reduce(memory: AssistantConversationMemory, event: AssistantMemoryEvent): AssistantConversationMemory = when (event.type) {
        AssistantMemoryEventType.QUESTION_ASKED -> memory.copy(
            currentConceptId = event.conceptId ?: memory.currentConceptId,
            recentQuestions = (memory.recentQuestions + event.text).filter { it.isNotBlank() }.takeLast(6),
        )
        AssistantMemoryEventType.RESPONSE_GIVEN -> memory
        AssistantMemoryEventType.CONCEPT_OPENED -> memory.copy(currentConceptId = event.conceptId, currentTopic = event.text.ifBlank { event.conceptId?.readableId() })
        AssistantMemoryEventType.OBJECT_SELECTED -> memory.copy(selectedObjectId = event.selectedObjectId, currentTopic = event.text.ifBlank { memory.currentTopic })
        AssistantMemoryEventType.MISTAKE_DETECTED -> memory.copy(
            currentConceptId = event.conceptId ?: memory.currentConceptId,
            recentMistakeIds = (memory.recentMistakeIds + event.text).filter { it.isNotBlank() }.distinct().takeLast(8),
        )
        AssistantMemoryEventType.HINT_USED -> memory.copy(currentConceptId = event.conceptId ?: memory.currentConceptId)
        AssistantMemoryEventType.LEVEL_CHANGED -> memory.copy(preferredLevel = event.text.toClassLevel() ?: memory.preferredLevel)
    }
}

private fun tokenize(query: String): List<String> = query
    .lowercase(Locale.US)
    .split(Regex("[^a-z0-9]+"))
    .filter { it.length >= 3 }
    .distinct()

private fun String.readableId(): String = split('-', '_').joinToString(" ") { part ->
    part.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
}

private fun String.readableEnum(): String = lowercase(Locale.US).split('_').joinToString(" ") { part ->
    part.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
}

private fun String.toClassLevel(): SchoolClassLevel? {
    val number = Regex("\\d+").find(this)?.value?.toIntOrNull() ?: return null
    return SchoolClassLevel.entries.firstOrNull { it.number == min(number, 12) }
}
