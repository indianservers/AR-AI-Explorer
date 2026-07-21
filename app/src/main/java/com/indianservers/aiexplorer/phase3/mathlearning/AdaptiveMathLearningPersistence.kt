package com.indianservers.aiexplorer.phase3.mathlearning

import android.content.Context
import com.indianservers.aiexplorer.learningintelligence.learner.LocalLearningIntelligenceService
import com.indianservers.aiexplorer.learningintelligence.model.*
import com.indianservers.aiexplorer.learningintelligence.persistence.DataStoreLearningPersistence
import com.indianservers.aiexplorer.learningintelligence.persistence.LearningSnapshotMapper
import com.indianservers.aiexplorer.learningintelligence.reference.LearningIntelligenceCatalog
import com.indianservers.aiexplorer.learningintelligence.repository.InMemoryLearningIntelligenceRepository
import java.time.Instant

/** Reuses the app-wide learning_intelligence DataStore and repository contracts. */
class AdaptiveMathLearningStore(context: Context) {
    private val persistence = DataStoreLearningPersistence(context.applicationContext)

    suspend fun load(): LocalLearningIntelligenceService {
        val snapshot = persistence.load()
        val repository = InMemoryLearningIntelligenceRepository(LearningIntelligenceCatalog.graph, LearningIntelligenceCatalog.misconceptions, LearningIntelligenceCatalog.hints)
        LearningSnapshotMapper.restoreStates(snapshot).forEach(repository::save)
        snapshot.records.forEach { record ->
            val f = record.fields
            runCatching {
                when (record.type) {
                    "mastery-evidence" -> repository.appendMastery(MasteryEvidence(record.id, f.getValue("concept"), MasteryEvidenceType.valueOf(f.getValue("type")), f.getValue("score").toDouble(), f.getValue("independence").toDouble(), f.getValue("hintPenalty").toDouble(), f["confidence"]?.takeIf(String::isNotBlank)?.let(LearnerConfidence::valueOf), Instant.parse(f.getValue("at")), f["source"]?.takeIf(String::isNotBlank)))
                    "misconception-evidence" -> repository.appendMisconception(MisconceptionEvidence(f.getValue("misconception"), f.getValue("concept"), null, f.getValue("weight").toDouble(), f["confidence"]?.takeIf(String::isNotBlank)?.let(LearnerConfidence::valueOf), Instant.parse(f.getValue("at")), f["resolved"].toBoolean()))
                    "scheduled-review" -> repository.save(ScheduledReview(record.id, Instant.parse(f.getValue("at")), ReviewUrgency.valueOf(f.getValue("urgency")), f.getValue("reason"), MasteryEvidenceType.valueOf(f.getValue("type"))))
                    "error-book" -> repository.save(ErrorBookEntry(record.id, f.getValue("concept"), f.getValue("curriculum"), f["question"]?.takeIf(String::isNotBlank), f.getValue("answer"), decodeSteps(f["steps"].orEmpty()), f["firstInvalid"]?.toIntOrNull(), LearnerErrorType.valueOf(f.getValue("error")), f["misconception"]?.takeIf(String::isNotBlank), f["confidence"]?.takeIf(String::isNotBlank)?.let(LearnerConfidence::valueOf), f["hintDepth"]?.takeIf(String::isNotBlank)?.let(HintLevel::valueOf), f["corrected"].orEmpty().split('\u001f').filter(String::isNotBlank), f["remediation"]?.takeIf(String::isNotBlank), f["retry"]?.takeIf(String::isNotBlank)?.let(Instant::parse), ErrorBookStatus.valueOf(f.getValue("status"))))
                }
            }
        }
        // Merge durable evidence into state objects because mastery derives from state-local windows.
        repository.all().forEach { state -> repository.save(state.copy(masteryEvidence = repository.masteryEvidence(state.conceptId), misconceptionEvidence = repository.misconceptionEvidence(state.conceptId), nextReviewAt = repository.review(state.conceptId)?.scheduledAt ?: state.nextReviewAt)) }
        return LocalLearningIntelligenceService(repository)
    }

    suspend fun save(service: LocalLearningIntelligenceService) {
        val repository = service.repository
        val states = repository.all()
        persistence.save(
            LearningSnapshotMapper.snapshot(
                states,
                emptyList(),
                emptyList(),
                states.flatMap { repository.masteryEvidence(it.conceptId) }.distinctBy { it.id },
                states.flatMap { repository.misconceptionEvidence(it.conceptId) }.distinctBy { "${it.misconceptionId}-${it.occurredAt}" },
                emptyList(),
                states.mapNotNull { repository.review(it.conceptId) },
                repository.current(),
                repository.entries(),
            ),
        )
    }

    private fun decodeSteps(raw: String) = raw.split('\u001e').filter(String::isNotBlank).map { encoded ->
        val parts = encoded.split('\u001f', limit = 2)
        LearnerAnswerStep(parts[0], parts.getOrNull(1)?.takeIf(String::isNotBlank))
    }
}
