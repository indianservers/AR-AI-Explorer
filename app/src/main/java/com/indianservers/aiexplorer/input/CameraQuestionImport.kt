package com.indianservers.aiexplorer.input

import com.indianservers.aiexplorer.assistant.privacy.AssistantConsent
import com.indianservers.aiexplorer.curriculum.SchoolSubject
import com.indianservers.aiexplorer.learningintelligence.reference.LearningIntelligenceCatalog

enum class ProcessingLocation { LOCAL, REMOTE_WITH_ONE_TIME_CONSENT }
data class SubjectCandidate(val subject: SchoolSubject, val confidence: Double)
data class ConceptCandidate(val conceptId: String, val confidence: Double)
data class ParsedQuantity(val label: String?, val value: Double, val unit: String?)
data class ImportedQuestion(
    val rawRecognisedText: String,
    val confirmedText: String,
    val subjectCandidates: List<SubjectCandidate>,
    val conceptCandidates: List<ConceptCandidate>,
    val parsedQuantities: List<ParsedQuantity>,
    val diagramDetected: Boolean,
    val confidence: Double,
    val processingLocation: ProcessingLocation,
)
enum class ImportedQuestionAction { EXPLAIN_CONCEPT, OPEN_FORMULA, OPEN_VISUALISATION, SOLVE_WITH_HINTS, ATTEMPT_INDEPENDENTLY }

fun interface LocalTextRecognizer { fun recognise(preprocessedBytes: ByteArray): String }
class CameraQuestionImporter(private val recognizer: LocalTextRecognizer) {
    fun import(croppedImage: ByteArray, confirmedText: String? = null): ImportedQuestion {
        require(croppedImage.isNotEmpty())
        val local = recognizer.recognise(croppedImage).trim()
        val confirmed = confirmedText?.trim()?.takeIf { it.isNotEmpty() } ?: local
        val subjects = classify(confirmed)
        val concepts = mapConcepts(confirmed, subjects.firstOrNull()?.subject)
        return ImportedQuestion(local, confirmed, subjects, concepts, quantities(confirmed), Regex("graph|diagram|figure|triangle|circuit", RegexOption.IGNORE_CASE).containsMatchIn(confirmed), if (local.isBlank()) 0.0 else .78, ProcessingLocation.LOCAL)
    }
    fun mayUploadForInterpretation(consent: AssistantConsent, learnerConfirmedTranscription: Boolean, oneTimeConfirmation: Boolean) = consent.cloudEnabled && consent.cameraMayBeSent && learnerConfirmedTranscription && oneTimeConfirmation
    private fun classify(text: String): List<SubjectCandidate> {
        val scores = mapOf(
            SchoolSubject.MATHEMATICS to listOf("equation", "triangle", "probability", "graph", "solve"),
            SchoolSubject.PHYSICS to listOf("force", "velocity", "circuit", "wave", "motion"),
            SchoolSubject.CHEMISTRY to listOf("mole", "acid", "atom", "gas", "titration"),
            SchoolSubject.BIOLOGY to listOf("cell", "blood", "allele", "photosynthesis", "membrane"),
        ).mapValues { (_, words) -> words.count { text.contains(it, true) } }
        val maximum = scores.values.maxOrNull()?.coerceAtLeast(1) ?: 1
        return scores.entries.filter { it.value > 0 }.sortedByDescending { it.value }.map { SubjectCandidate(it.key, it.value.toDouble() / maximum) }
    }
    private fun mapConcepts(text: String, subject: SchoolSubject?): List<ConceptCandidate> = LearningIntelligenceCatalog.concepts
        .asSequence().filter { subject == null || it.subject == subject }
        .map { concept -> concept to concept.conceptId.removePrefix("${concept.subject.name.lowercase()}-").split('-').count { text.contains(it, true) } }
        .filter { it.second > 0 }.sortedByDescending { it.second }.take(3).map { ConceptCandidate(it.first.conceptId, (.5 + it.second * .15).coerceAtMost(.95)) }.toList()
    private fun quantities(text: String) = Regex("(-?\\d+(?:\\.\\d+)?)\\s*([a-zA-Z°/%]+)?").findAll(text).mapNotNull { match -> match.groupValues[1].toDoubleOrNull()?.let { ParsedQuantity(null, it, match.groupValues.getOrNull(2)?.takeIf(String::isNotBlank)) } }.toList()
}
