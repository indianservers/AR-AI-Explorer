package com.indianservers.aiexplorer.assistant.operations

import com.indianservers.aiexplorer.assistant.contracts.AssistantCapability
import com.indianservers.aiexplorer.assistant.contracts.GroundedAssistantRequest
import com.indianservers.aiexplorer.assistant.contracts.LearningAssistantProvider
import com.indianservers.aiexplorer.assistant.privacy.AssistantConsent
import com.indianservers.aiexplorer.assistant.privacy.PrivacyGate
import com.indianservers.aiexplorer.assistant.privacy.SharedDataCategory
import com.indianservers.aiexplorer.assistant.routing.AssistanceNeed
import com.indianservers.aiexplorer.learningintelligence.learner.LocalLearningIntelligenceService
import com.indianservers.aiexplorer.learningintelligence.reference.LearningIntelligenceCatalog
import java.security.MessageDigest
import java.time.Instant

enum class AssistantOfflineFeatureStatus { READY, LIMITED, BLOCKED }
enum class AssistantRouteTarget { LOCAL, CLOUD, FALLBACK }

data class AssistantOfflineFeature(
    val id: String,
    val label: String,
    val status: AssistantOfflineFeatureStatus,
    val reason: String,
)

data class AssistantOfflineHealthReport(
    val generatedAt: Instant,
    val readyCount: Int,
    val limitedCount: Int,
    val blockedCount: Int,
    val features: List<AssistantOfflineFeature>,
    val summary: String,
)

data class AssistantSharePayload(
    val categories: Set<SharedDataCategory>,
    val fields: Map<String, String>,
    val redactedFields: Set<String>,
)

data class AssistantRouteDecision(
    val target: AssistantRouteTarget,
    val providerId: String?,
    val reason: String,
    val shareableCategories: Set<SharedDataCategory>,
)

data class AssistantAuditEvent(
    val sequence: Int,
    val timestampMs: Long,
    val action: String,
    val detail: String,
    val previousHash: String,
    val hash: String,
)

object AssistantOfflineHealthEngine {
    fun assess(
        local: LearningAssistantProvider,
        service: LocalLearningIntelligenceService,
        consent: AssistantConsent = AssistantConsent(),
        providers: Map<String, LearningAssistantProvider> = emptyMap(),
        now: Instant = Instant.now(),
    ): AssistantOfflineHealthReport {
        val conceptCount = LearningIntelligenceCatalog.concepts.size
        val features = listOf(
            feature("local-chat", "Local reviewed assistant", AssistantCapability.SOCRATIC_TUTORING in local.capabilities, "Local Socratic tutoring is available.", "Local tutoring capability is missing."),
            feature("step-check", "Step analysis", AssistantCapability.STEP_ANALYSIS in local.capabilities, "Local step feedback can run offline.", "Step feedback capability is missing."),
            feature("practice", "Practice wording", AssistantCapability.PRACTICE_WORDING in local.capabilities, "Practice prompts can be generated from local templates.", "Practice prompt capability is missing."),
            AssistantOfflineFeature(
                "knowledge",
                "Reviewed knowledge catalog",
                if (conceptCount > 0) AssistantOfflineFeatureStatus.READY else AssistantOfflineFeatureStatus.BLOCKED,
                if (conceptCount > 0) "$conceptCount reviewed concept path(s) are indexed." else "No reviewed concepts are indexed.",
            ),
            AssistantOfflineFeature(
                "continuity",
                "Learning continuity",
                if (service.repository.all().isNotEmpty()) AssistantOfflineFeatureStatus.READY else AssistantOfflineFeatureStatus.LIMITED,
                if (service.repository.all().isNotEmpty()) "Local learner state is available for recaps and next actions." else "No learner state is available yet.",
            ),
            AssistantOfflineFeature(
                "cloud",
                "Optional cloud handoff",
                when {
                    !consent.cloudEnabled -> AssistantOfflineFeatureStatus.BLOCKED
                    providers.isEmpty() -> AssistantOfflineFeatureStatus.LIMITED
                    else -> AssistantOfflineFeatureStatus.READY
                },
                when {
                    !consent.cloudEnabled -> "Cloud sharing is disabled by consent."
                    providers.isEmpty() -> "Cloud is allowed, but no provider is configured."
                    else -> "Cloud handoff is available for non-routine requests after local verification."
                },
            ),
        )
        val ready = features.count { it.status == AssistantOfflineFeatureStatus.READY }
        val limited = features.count { it.status == AssistantOfflineFeatureStatus.LIMITED }
        val blocked = features.count { it.status == AssistantOfflineFeatureStatus.BLOCKED }
        return AssistantOfflineHealthReport(
            generatedAt = now,
            readyCount = ready,
            limitedCount = limited,
            blockedCount = blocked,
            features = features,
            summary = "$ready ready, $limited limited, $blocked blocked offline assistant feature(s).",
        )
    }

    private fun feature(id: String, label: String, ready: Boolean, readyReason: String, blockedReason: String): AssistantOfflineFeature =
        AssistantOfflineFeature(id, label, if (ready) AssistantOfflineFeatureStatus.READY else AssistantOfflineFeatureStatus.BLOCKED, if (ready) readyReason else blockedReason)
}

object AssistantPrivacyPayloadBuilder {
    fun build(
        request: GroundedAssistantRequest,
        consent: AssistantConsent,
        hasCameraCrop: Boolean = false,
        hasVoiceTranscript: Boolean = false,
    ): AssistantSharePayload {
        val categories = PrivacyGate.shareableCategories(
            requestHasCamera = hasCameraCrop,
            requestHasVoice = hasVoiceTranscript,
            requestHasSteps = request.learnerSteps.isNotEmpty(),
            consent = consent,
        )
        val fields = linkedMapOf<String, String>()
        val redacted = mutableSetOf<String>()

        putOrRedact(fields, redacted, "question", request.learnerQuestion, SharedDataCategory.CONFIRMED_QUESTION_TEXT in categories)
        putOrRedact(fields, redacted, "concept", request.conceptId, SharedDataCategory.CONCEPT_AND_LEVEL in categories)
        putOrRedact(fields, redacted, "level", request.learningLevel?.name.orEmpty(), SharedDataCategory.CONCEPT_AND_LEVEL in categories)
        putOrRedact(fields, redacted, "visualState", request.currentSimulationState?.id.orEmpty(), SharedDataCategory.SELECTED_VISUAL_STATE in categories)
        putOrRedact(fields, redacted, "learnerSteps", request.learnerSteps.joinToString("\n") { it.expression }, SharedDataCategory.SELECTED_LEARNER_STEPS in categories)
        putOrRedact(fields, redacted, "cameraCrop", if (hasCameraCrop) "selected-crop" else "", SharedDataCategory.SELECTED_CAMERA_CROP in categories)
        putOrRedact(fields, redacted, "voiceTranscript", if (hasVoiceTranscript) request.learnerQuestion else "", SharedDataCategory.CONFIRMED_VOICE_TRANSCRIPT in categories)
        fields["groundingReferenceIds"] = request.verifiedContent.filter { it.reviewed }.joinToString(",") { it.id }

        return AssistantSharePayload(categories, fields, redacted)
    }

    private fun putOrRedact(fields: MutableMap<String, String>, redacted: MutableSet<String>, key: String, value: String, allowed: Boolean) {
        if (allowed && value.isNotBlank()) fields[key] = value else {
            fields[key] = "[redacted]"
            redacted += key
        }
    }
}

object AssistantRouteDecisionEngine {
    fun decide(
        need: AssistanceNeed,
        selectedProviderId: String?,
        consent: AssistantConsent,
        providers: Map<String, LearningAssistantProvider> = emptyMap(),
        requestHasCamera: Boolean = false,
        requestHasVoice: Boolean = false,
        requestHasSteps: Boolean = false,
    ): AssistantRouteDecision {
        val categories = PrivacyGate.shareableCategories(requestHasCamera, requestHasVoice, requestHasSteps, consent)
        return when {
            need == AssistanceNeed.ROUTINE -> AssistantRouteDecision(AssistantRouteTarget.LOCAL, null, "Routine help stays on the reviewed local assistant.", emptySet())
            !consent.cloudEnabled -> AssistantRouteDecision(AssistantRouteTarget.LOCAL, null, "Cloud handoff is disabled, so the local assistant handles the request.", emptySet())
            selectedProviderId == null -> AssistantRouteDecision(AssistantRouteTarget.LOCAL, null, "No provider was selected for the optional handoff.", emptySet())
            selectedProviderId !in providers -> AssistantRouteDecision(AssistantRouteTarget.FALLBACK, null, "Selected provider is unavailable; use local reviewed fallback.", categories)
            else -> AssistantRouteDecision(AssistantRouteTarget.CLOUD, selectedProviderId, "Optional provider may answer, then local verification must approve the response.", categories)
        }
    }
}

class AssistantAuditTrail {
    private val events = mutableListOf<AssistantAuditEvent>()

    fun record(timestampMs: Long, action: String, detail: String): AssistantAuditEvent {
        val sequence = events.size + 1
        val previous = events.lastOrNull()?.hash ?: "root"
        val hash = hash(sequence, timestampMs, action, detail, previous)
        val event = AssistantAuditEvent(sequence, timestampMs, action, detail, previous, hash)
        events += event
        return event
    }

    fun events(): List<AssistantAuditEvent> = events.toList()

    fun verify(input: List<AssistantAuditEvent> = events): Boolean = input.indices.all { index ->
        val event = input[index]
        val previous = if (index == 0) "root" else input[index - 1].hash
        event.sequence == index + 1 && event.previousHash == previous && event.hash == hash(event.sequence, event.timestampMs, event.action, event.detail, event.previousHash)
    }

    fun exportText(): String = events.joinToString("\n") { event ->
        "${event.sequence},${event.timestampMs},${event.action.csv()},${event.detail.csv()},${event.hash}"
    }

    private fun String.csv(): String = "\"" + replace("\"", "\"\"") + "\""

    private fun hash(sequence: Int, timestampMs: Long, action: String, detail: String, previousHash: String): String {
        val raw = "$sequence|$timestampMs|$action|$detail|$previousHash"
        return MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }
}
