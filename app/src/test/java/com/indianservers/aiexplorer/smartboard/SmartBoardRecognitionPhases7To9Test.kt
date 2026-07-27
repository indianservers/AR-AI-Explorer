package com.indianservers.aiexplorer.smartboard

import com.indianservers.aiexplorer.smartboard.models.MathExpressionType
import com.indianservers.aiexplorer.smartboard.models.MathRecognitionAlternative
import com.indianservers.aiexplorer.smartboard.models.MathRecognitionResult
import com.indianservers.aiexplorer.smartboard.models.RecognitionQualityTier
import com.indianservers.aiexplorer.smartboard.models.SmartBoardSubject
import com.indianservers.aiexplorer.smartboard.recognition.BoundedRecognitionDiagnostics
import com.indianservers.aiexplorer.smartboard.recognition.RecognitionBenchmarkMetrics
import com.indianservers.aiexplorer.smartboard.recognition.RecognitionBenchmarkTargets
import com.indianservers.aiexplorer.smartboard.recognition.RecognitionCandidateSource
import com.indianservers.aiexplorer.smartboard.recognition.RecognitionConfidenceBucket
import com.indianservers.aiexplorer.smartboard.recognition.RecognitionContext
import com.indianservers.aiexplorer.smartboard.recognition.RecognitionContextEvidenceType
import com.indianservers.aiexplorer.smartboard.recognition.RecognitionDiagnosticEvent
import com.indianservers.aiexplorer.smartboard.recognition.RecognitionDiagnosticInput
import com.indianservers.aiexplorer.smartboard.recognition.RecognitionLatencyBucket
import com.indianservers.aiexplorer.smartboard.recognition.RecognitionLatticeCandidate
import com.indianservers.aiexplorer.smartboard.recognition.RecognitionModelManifest
import com.indianservers.aiexplorer.smartboard.recognition.RecognitionPersonalizationProfile
import com.indianservers.aiexplorer.smartboard.recognition.RecognitionPersonalizationProfileCodec
import com.indianservers.aiexplorer.smartboard.recognition.SmartBoardContextualRecognitionReranker
import com.indianservers.aiexplorer.smartboard.recognition.SmartBoardRecognitionPersonalizer
import com.indianservers.aiexplorer.smartboard.recognition.SmartBoardRecognitionQualityGate
import com.indianservers.aiexplorer.smartboard.recognition.SmartBoardRecognitionReleaseController
import com.indianservers.aiexplorer.smartboard.recognition.StreamingRecognitionSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartBoardRecognitionPhases7To9Test {
    @Test
    fun contextualRerankingOnlyReordersExistingCandidatesAndExplainsWhy() {
        val original = snapshot(
            candidate("q+1", .70f),
            candidate("x+1", .68f),
        )
        val context = RecognitionContext(
            boardSubject = SmartBoardSubject.MATHEMATICS,
            recentExpressions = listOf("x+2"),
            nearbyLabels = listOf("solve for x"),
            activeVariables = setOf("x"),
            conceptNames = setOf("linear equation"),
            qualityTier = RecognitionQualityTier.ACCURATE,
        )
        val outcome = SmartBoardContextualRecognitionReranker.rerank(original, context)
        assertEquals("x+1", outcome.snapshot.result.latex)
        assertEquals(original.candidates.map { it.text }.toSet(), outcome.snapshot.candidates.map { it.text }.toSet())
        assertTrue(outcome.primaryEvidence.any { it.type == RecognitionContextEvidenceType.SHARED_VARIABLE })
        assertTrue(outcome.primaryEvidence.any { it.type == RecognitionContextEvidenceType.NEARBY_LABEL })
    }

    @Test
    fun personalizationStoresOnlyBoundedCorrectionPairsAndRoundTripsLocally() {
        var profile = RecognitionPersonalizationProfile.Empty
        repeat(300) { index ->
            profile = SmartBoardRecognitionPersonalizer.recordCorrection(profile, "x+$index", "x+${index + 1}", index.toLong())
        }
        assertEquals(256, profile.corrections.size)
        assertEquals(300, profile.totalConfirmedCorrections)
        val restored = RecognitionPersonalizationProfileCodec.decode(RecognitionPersonalizationProfileCodec.encode(profile))
        assertEquals(profile, restored)
        assertTrue(restored.corrections.all { it.recognized.length < 512 && it.confirmed.length < 512 })
    }

    @Test
    fun qualityGateBlocksRegressionAndReleaseControllerKeepsActiveModel() {
        val baseline = metrics(semantic = .98, p95 = 300)
        val regressed = metrics(semantic = .95, p95 = 620)
        val gate = SmartBoardRecognitionQualityGate.evaluate(regressed, RecognitionBenchmarkTargets(), baseline)
        assertFalse(gate.passed)
        assertTrue(gate.blockers.any { it.contains("regressed", ignoreCase = true) })
        val active = manifest("builtin", "1", "0")
        val controller = SmartBoardRecognitionReleaseController(active)
        val result = controller.activate(manifest("candidate", "2", "1"), gate, supportedSchemaVersion = 1)
        assertTrue(result.isFailure)
        assertEquals(active, controller.activeManifest)
    }

    @Test
    fun contentFreeDiagnosticsRecommendRollbackOnSlowAndCorrectedRuntime() {
        val diagnostics = BoundedRecognitionDiagnostics(20)
        repeat(20) { index ->
            diagnostics.record(
                RecognitionDiagnosticEvent(
                    RecognitionDiagnosticInput.FUSED,
                    if (index < 8) RecognitionLatencyBucket.OVER_500_MS else RecognitionLatencyBucket.UNDER_150_MS,
                    RecognitionConfidenceBucket.MEDIUM,
                    candidateCount = 3,
                    selectedRank = 2,
                    corrected = index < 10,
                    occurredAt = index.toLong(),
                ),
            )
        }
        val health = diagnostics.health()
        assertEquals(20, health.sampleCount)
        assertTrue(health.rollbackRecommended)
        assertEquals(20, diagnostics.snapshot().size)
    }

    private fun candidate(text: String, confidence: Float) = RecognitionLatticeCandidate(
        text,
        text,
        confidence,
        setOf(RecognitionCandidateSource.DIGITAL_INK, RecognitionCandidateSource.PARSER),
        parserVerified = true,
        detectedType = MathExpressionType.ALGEBRAIC_EXPRESSION,
    )

    private fun snapshot(vararg candidates: RecognitionLatticeCandidate): StreamingRecognitionSnapshot {
        val first = candidates.first()
        return StreamingRecognitionSnapshot(
            "fingerprint",
            candidates.toList(),
            null,
            .5f,
            50,
            MathRecognitionResult(
                first.text,
                first.normalizedExpression,
                first.text,
                first.confidence,
                candidates.drop(1).map { MathRecognitionAlternative(it.text, it.confidence) },
                first.detectedType,
                emptyList(),
            ),
        )
    }

    private fun metrics(semantic: Double, p95: Long) = RecognitionBenchmarkMetrics(
        caseCount = 2_000,
        exactLatexAccuracy = semantic,
        semanticAccuracy = semantic,
        meanSymbolAccuracy = semantic,
        topThreeRecall = .995,
        confidenceBrierScore = .02,
        medianLatencyMillis = 100,
        p95LatencyMillis = p95,
        medianCorrectionActions = 1,
    )

    private fun manifest(id: String, version: String, digit: String) = RecognitionModelManifest(
        id,
        version,
        digit.repeat(64),
        1,
        setOf(RecognitionDiagnosticInput.DIGITAL_INK, RecognitionDiagnosticInput.FUSED),
        RecognitionQualityTier.BALANCED,
        verified = true,
    )
}
