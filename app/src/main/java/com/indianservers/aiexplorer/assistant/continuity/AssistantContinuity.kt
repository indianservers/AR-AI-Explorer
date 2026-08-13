package com.indianservers.aiexplorer.assistant.continuity

import com.indianservers.aiexplorer.learningintelligence.learner.LocalLearningIntelligenceService
import com.indianservers.aiexplorer.learningintelligence.model.ConceptMasteryState
import com.indianservers.aiexplorer.learningintelligence.model.ErrorBookEntry
import com.indianservers.aiexplorer.learningintelligence.model.ErrorBookStatus
import com.indianservers.aiexplorer.learningintelligence.model.LearnerConceptState
import com.indianservers.aiexplorer.learningintelligence.model.LearningRecommendation
import com.indianservers.aiexplorer.learningintelligence.model.MasteryEvidenceType
import com.indianservers.aiexplorer.learningintelligence.model.RecommendationReason
import com.indianservers.aiexplorer.learningintelligence.model.ReviewUrgency
import com.indianservers.aiexplorer.learningintelligence.model.ScheduledReview
import com.indianservers.aiexplorer.learningintelligence.reference.LearningIntelligenceCatalog
import java.time.Duration
import java.time.Instant
import java.util.Locale

enum class AssistantContinuityActionType { REVIEW_NOW, RETRY_MISTAKE, CONTINUE_LESSON, PRACTICE_VARIANT, REPAIR_PREREQUISITE, CELEBRATE_PROGRESS }
enum class AssistantContinuityPriority { LOW, MEDIUM, HIGH, URGENT }

data class AssistantReviewCard(
    val conceptId: String,
    val title: String,
    val dueAt: Instant,
    val urgency: ReviewUrgency,
    val prompt: String,
    val minutes: Int,
)

data class AssistantRetryCard(
    val errorId: String,
    val conceptId: String,
    val title: String,
    val learnerAnswer: String,
    val repairPrompt: String,
    val retryAt: Instant?,
    val priority: AssistantContinuityPriority,
)

data class AssistantNextAction(
    val type: AssistantContinuityActionType,
    val conceptId: String,
    val title: String,
    val reason: String,
    val minutes: Int,
    val priority: AssistantContinuityPriority,
    val activityId: String? = null,
)

data class AssistantSessionRecap(
    val title: String,
    val masteredCount: Int,
    val learningCount: Int,
    val dueReviewCount: Int,
    val openMistakeCount: Int,
    val highlights: List<String>,
    val nextAction: AssistantNextAction?,
)

object AssistantContinuityPlanner {
    fun reviewCards(service: LocalLearningIntelligenceService, now: Instant = Instant.now()): List<AssistantReviewCard> {
        val scheduled = service.repository.all().mapNotNull { state ->
            val review = service.repository.review(state.conceptId)
            when {
                review != null && (review.urgency != ReviewUrgency.NOT_DUE || !review.scheduledAt.isAfter(now)) -> review
                state.nextReviewAt != null && !state.nextReviewAt.isAfter(now) -> ScheduledReview(
                    conceptId = state.conceptId,
                    scheduledAt = state.nextReviewAt,
                    urgency = urgencyFor(state.nextReviewAt, now),
                    reason = "Evidence-based review is due.",
                    preferredEvidenceType = MasteryEvidenceType.DELAYED_RETRIEVAL,
                )
                else -> null
            }
        }
        return scheduled.sortedWith(compareByDescending<ScheduledReview> { priorityScore(it.urgency) }.thenBy { it.scheduledAt })
            .map { review ->
                AssistantReviewCard(
                    conceptId = review.conceptId,
                    title = title(review.conceptId),
                    dueAt = review.scheduledAt,
                    urgency = review.urgency,
                    prompt = promptForReview(review, now),
                    minutes = if (review.urgency == ReviewUrgency.URGENT_REPAIR) 8 else 5,
                )
            }
    }

    fun retryCards(service: LocalLearningIntelligenceService, now: Instant = Instant.now()): List<AssistantRetryCard> =
        service.repository.entries()
            .filter { it.status in setOf(ErrorBookStatus.OPEN, ErrorBookStatus.RETRY_SCHEDULED, ErrorBookStatus.IMPROVING) }
            .sortedWith(compareBy<ErrorBookEntry> { it.retryAt ?: Instant.MAX }.thenBy { it.id })
            .map { entry ->
                val due = entry.retryAt?.let { !it.isAfter(now) } ?: true
                AssistantRetryCard(
                    errorId = entry.id,
                    conceptId = entry.conceptId,
                    title = entry.misconceptionId?.let { it.readable() } ?: entry.errorType.name.readable(),
                    learnerAnswer = entry.learnerAnswer,
                    repairPrompt = entry.correctedMethod.firstOrNull() ?: "Retry the same idea with one smaller justified step.",
                    retryAt = entry.retryAt,
                    priority = when {
                        due && entry.confidence?.name == "VERY_SURE" -> AssistantContinuityPriority.URGENT
                        due -> AssistantContinuityPriority.HIGH
                        entry.status == ErrorBookStatus.IMPROVING -> AssistantContinuityPriority.MEDIUM
                        else -> AssistantContinuityPriority.LOW
                    },
                )
            }

    fun nextAction(service: LocalLearningIntelligenceService, timeMinutes: Int = 10, now: Instant = Instant.now()): AssistantNextAction? {
        retryCards(service, now).firstOrNull { it.priority in setOf(AssistantContinuityPriority.URGENT, AssistantContinuityPriority.HIGH) }?.let {
            return AssistantNextAction(
                type = AssistantContinuityActionType.RETRY_MISTAKE,
                conceptId = it.conceptId,
                title = "Retry ${it.title}",
                reason = "A stored mistake is ready for a short repair attempt.",
                minutes = 5,
                priority = it.priority,
            )
        }
        reviewCards(service, now).firstOrNull()?.let {
            return AssistantNextAction(
                type = AssistantContinuityActionType.REVIEW_NOW,
                conceptId = it.conceptId,
                title = "Review ${it.title}",
                reason = it.prompt,
                minutes = it.minutes,
                priority = priorityFor(it.urgency),
            )
        }
        val recommendation = service.recommend(timeMinutes = timeMinutes, now = now) ?: return progressCelebration(service)
        return recommendation.toAction()
    }

    fun recap(service: LocalLearningIntelligenceService, now: Instant = Instant.now()): AssistantSessionRecap {
        val states = service.repository.all()
        val mastered = states.count { it.masteryState == ConceptMasteryState.MASTERED }
        val learning = states.count { it.masteryState in setOf(ConceptMasteryState.INTRODUCED, ConceptMasteryState.LEARNING, ConceptMasteryState.PRACTISING, ConceptMasteryState.PROFICIENT, ConceptMasteryState.NEEDS_REVIEW) }
        val due = reviewCards(service, now)
        val retries = retryCards(service, now)
        val highlights = buildList {
            if (mastered > 0) add("$mastered concept(s) have mastery evidence.")
            states.maxByOrNull { it.masteryEvidence.size }?.takeIf { it.masteryEvidence.isNotEmpty() }?.let { add("${title(it.conceptId)} has the most recent evidence.") }
            due.firstOrNull()?.let { add("${it.title} is ready for spaced review.") }
            retries.firstOrNull()?.let { add("${it.title} has a saved retry path.") }
            if (isEmpty()) add("Start with one short local practice task to create progress evidence.")
        }
        return AssistantSessionRecap(
            title = "Offline learning recap",
            masteredCount = mastered,
            learningCount = learning,
            dueReviewCount = due.size,
            openMistakeCount = retries.size,
            highlights = highlights.take(4),
            nextAction = nextAction(service, now = now),
        )
    }

    private fun LearningRecommendation.toAction(): AssistantNextAction {
        val type = when (reason) {
            RecommendationReason.MISSING_PREREQUISITE -> AssistantContinuityActionType.REPAIR_PREREQUISITE
            RecommendationReason.REVIEW_DUE, RecommendationReason.LOW_RETENTION, RecommendationReason.CONFIDENTLY_WRONG, RecommendationReason.MISCONCEPTION_REPAIR -> AssistantContinuityActionType.REVIEW_NOW
            RecommendationReason.PRACTICE_VARIETY, RecommendationReason.LOW_APPLICATION_EVIDENCE -> AssistantContinuityActionType.PRACTICE_VARIANT
            else -> AssistantContinuityActionType.CONTINUE_LESSON
        }
        return AssistantNextAction(
            type = type,
            conceptId = conceptId,
            title = title(conceptId),
            reason = explanation,
            minutes = expectedDurationMinutes,
            priority = when {
                priorityScore >= 95.0 -> AssistantContinuityPriority.URGENT
                priorityScore >= 80.0 -> AssistantContinuityPriority.HIGH
                priorityScore >= 60.0 -> AssistantContinuityPriority.MEDIUM
                else -> AssistantContinuityPriority.LOW
            },
            activityId = activityId,
        )
    }

    private fun progressCelebration(service: LocalLearningIntelligenceService): AssistantNextAction? {
        val state = service.repository.all().maxByOrNull { it.masteryEvidence.size } ?: return null
        return AssistantNextAction(
            type = AssistantContinuityActionType.CELEBRATE_PROGRESS,
            conceptId = state.conceptId,
            title = title(state.conceptId),
            reason = "No urgent review or retry is waiting. Keep momentum with a short mixed practice item.",
            minutes = 5,
            priority = AssistantContinuityPriority.LOW,
            activityId = LearningIntelligenceCatalog.activities[state.conceptId],
        )
    }

    private fun promptForReview(review: ScheduledReview, now: Instant): String {
        val days = Duration.between(review.scheduledAt, now).toDays()
        return when {
            review.urgency == ReviewUrgency.URGENT_REPAIR -> "Repair the confidently wrong retrieval before continuing."
            days > 0 -> "Review is $days day(s) overdue; answer first, then request a hint only if stuck."
            else -> "Do a quick delayed retrieval without looking at the worked example first."
        }
    }

    private fun urgencyFor(dueAt: Instant, now: Instant): ReviewUrgency {
        val overdueDays = Duration.between(dueAt, now).toDays()
        return when {
            overdueDays >= 7 -> ReviewUrgency.OVERDUE
            overdueDays >= 1 -> ReviewUrgency.DUE_TODAY
            else -> ReviewUrgency.DUE_SOON
        }
    }

    private fun priorityFor(urgency: ReviewUrgency): AssistantContinuityPriority = when (urgency) {
        ReviewUrgency.URGENT_REPAIR -> AssistantContinuityPriority.URGENT
        ReviewUrgency.OVERDUE, ReviewUrgency.DUE_TODAY -> AssistantContinuityPriority.HIGH
        ReviewUrgency.DUE_SOON -> AssistantContinuityPriority.MEDIUM
        ReviewUrgency.NOT_DUE -> AssistantContinuityPriority.LOW
    }

    private fun priorityScore(urgency: ReviewUrgency): Int = when (urgency) {
        ReviewUrgency.URGENT_REPAIR -> 4
        ReviewUrgency.OVERDUE -> 3
        ReviewUrgency.DUE_TODAY -> 2
        ReviewUrgency.DUE_SOON -> 1
        ReviewUrgency.NOT_DUE -> 0
    }

    private fun title(conceptId: String): String = conceptId.readable()

    private fun String.readable(): String = replace('-', ' ')
        .replace('_', ' ')
        .lowercase(Locale.US)
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
}
