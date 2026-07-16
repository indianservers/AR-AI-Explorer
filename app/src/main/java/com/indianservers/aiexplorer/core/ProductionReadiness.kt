package com.indianservers.aiexplorer.core

import com.indianservers.aiexplorer.spatial.ARPrivacySafetyChecklist
import com.indianservers.aiexplorer.spatial.RenderQuality

enum class PerformanceStatus { Pass, Degraded, Fail }
data class ProductPerformanceBudget(
    val targetFrameMillis: Double = 16.67,
    val maximumInteractiveFrameMillis: Double = 33.34,
    val maximumGraphEvaluations: Int = 50_000,
    val maximumSceneObjects: Int = 2_000,
    val maximumArchiveBytes: Long = 8_000_000,
    val maximumWorkingMemoryBytes: Long = 256_000_000,
)
data class ProductPerformanceSnapshot(
    val frameMillisP95: Double,
    val graphEvaluations: Int,
    val sceneObjects: Int,
    val archiveBytes: Long,
    val estimatedWorkingMemoryBytes: Long,
)
data class PerformanceAssessment(val status: PerformanceStatus, val quality: RenderQuality, val messages: List<String>, val score: Int)

object ProductPerformanceManager {
    fun assess(snapshot: ProductPerformanceSnapshot, budget: ProductPerformanceBudget = ProductPerformanceBudget()): PerformanceAssessment {
        val messages = mutableListOf<String>(); var penalties = 0
        if (snapshot.frameMillisP95 > budget.maximumInteractiveFrameMillis) { messages += "Frame p95 exceeds the interactive limit."; penalties += 35 }
        else if (snapshot.frameMillisP95 > budget.targetFrameMillis) { messages += "Frame p95 misses 60 fps; reduce sampling or renderer quality."; penalties += 12 }
        if (snapshot.graphEvaluations > budget.maximumGraphEvaluations) { messages += "Graph evaluation budget exceeded."; penalties += 20 }
        if (snapshot.sceneObjects > budget.maximumSceneObjects) { messages += "Scene object budget exceeded."; penalties += 20 }
        if (snapshot.archiveBytes > budget.maximumArchiveBytes) { messages += "Project archive exceeds the safe sharing budget."; penalties += 25 }
        if (snapshot.estimatedWorkingMemoryBytes > budget.maximumWorkingMemoryBytes) { messages += "Estimated working memory exceeds the mobile budget."; penalties += 30 }
        if (messages.isEmpty()) messages += "All measured product budgets pass."
        val score = (100 - penalties).coerceIn(0, 100)
        val status = when { score >= 90 -> PerformanceStatus.Pass; score >= 60 -> PerformanceStatus.Degraded; else -> PerformanceStatus.Fail }
        val quality = when { score >= 95 -> RenderQuality.Ultra; score >= 85 -> RenderQuality.High; score >= 65 -> RenderQuality.Balanced; score >= 40 -> RenderQuality.Low; else -> RenderQuality.Safety }
        return PerformanceAssessment(status, quality, messages, score)
    }
}

enum class DeviceCapabilityTier { FullSpatial, Advanced, Standard, Safety }
data class DeviceCapabilityProfile(
    val androidApi: Int,
    val cpuCores: Int,
    val memoryClassMb: Int,
    val openGlEsMajor: Int,
    val arCoreSupported: Boolean,
    val depthSupported: Boolean,
    val cameraPermission: Boolean,
    val lowRam: Boolean,
    val tv: Boolean = false,
)
data class DeviceCapabilityAssessment(val tier: DeviceCapabilityTier, val enabled: Set<String>, val disabled: Map<String, String>, val recommendedSurfaceDensity: Int)

object DeviceCapabilityManager {
    fun assess(profile: DeviceCapabilityProfile): DeviceCapabilityAssessment {
        val enabled = mutableSetOf("2D maths", "CAS", "solver", "activities", "simulated AR")
        val disabled = linkedMapOf<String, String>()
        if (profile.openGlEsMajor >= 3 && !profile.lowRam) enabled += setOf("GPU 3D", "surface graphs") else disabled["GPU 3D"] = "OpenGL ES 3 and a non-low-RAM device are required."
        if (profile.arCoreSupported && profile.cameraPermission && !profile.tv) enabled += "live AR" else disabled["live AR"] = when { profile.tv -> "TV devices use the spatial simulator."; !profile.arCoreSupported -> "ARCore is not supported."; else -> "Camera permission is not granted." }
        if ("live AR" in enabled && profile.depthSupported) enabled += "depth occlusion" else disabled["depth occlusion"] = "Depth support is unavailable; plane placement remains available."
        val tier = when {
            "depth occlusion" in enabled && profile.memoryClassMb >= 384 && profile.cpuCores >= 6 -> DeviceCapabilityTier.FullSpatial
            "GPU 3D" in enabled -> DeviceCapabilityTier.Advanced
            !profile.lowRam -> DeviceCapabilityTier.Standard
            else -> DeviceCapabilityTier.Safety
        }
        val density = when (tier) { DeviceCapabilityTier.FullSpatial -> 48; DeviceCapabilityTier.Advanced -> 36; DeviceCapabilityTier.Standard -> 24; DeviceCapabilityTier.Safety -> 12 }
        return DeviceCapabilityAssessment(tier, enabled, disabled, density)
    }
}

enum class ReleaseGateStatus { Pass, Conditional, Blocked }
data class ReleaseEvidence(
    val unitTestsPassed: Int,
    val unitTestsFailed: Int,
    val lintErrors: Int,
    val archiveRecoveryPassed: Boolean,
    val migrationPassed: Boolean,
    val offlineMergePassed: Boolean,
    val interoperabilityRoundTripPassed: Boolean,
    val privacyChecklistCompletedIds: Set<String>,
    val physicalArDeviceChecksPassed: Boolean,
    val performance: PerformanceAssessment,
)
data class ReleaseReadinessReport(val status: ReleaseGateStatus, val score: Int, val passed: List<String>, val blockers: List<String>, val conditions: List<String>)

object ReleaseReadinessEngine {
    fun assess(evidence: ReleaseEvidence): ReleaseReadinessReport {
        val passed = mutableListOf<String>(); val blockers = mutableListOf<String>(); val conditions = mutableListOf<String>()
        if (evidence.unitTestsFailed == 0 && evidence.unitTestsPassed > 0) passed += "${evidence.unitTestsPassed} automated tests pass." else blockers += "Automated test failures remain."
        if (evidence.lintErrors == 0) passed += "Android lint has no errors." else blockers += "Android lint errors remain."
        if (evidence.archiveRecoveryPassed && evidence.migrationPassed) passed += "Project recovery and migration pass." else blockers += "Project recovery/migration gate failed."
        if (evidence.offlineMergePassed) passed += "Offline merge invariants pass." else blockers += "Offline merge is not verified."
        if (evidence.interoperabilityRoundTripPassed) passed += "Supported exchange round trips pass." else conditions += "Interoperability remains foundation-level; publish the coverage matrix."
        val mandatoryPrivacy = ARPrivacySafetyChecklist.items.filter { it.mandatory }.map { it.id }.toSet()
        val missingPrivacy = mandatoryPrivacy - evidence.privacyChecklistCompletedIds
        if (missingPrivacy.isEmpty()) passed += "AR privacy/safety checklist is complete." else blockers += "Missing mandatory privacy checks: ${missingPrivacy.sorted().joinToString()}."
        if (evidence.physicalArDeviceChecksPassed) passed += "Physical AR device checks pass." else conditions += "Live AR remains beta until physical-device drift, thermal and depth QA passes."
        when (evidence.performance.status) { PerformanceStatus.Pass -> passed += "Performance budgets pass."; PerformanceStatus.Degraded -> conditions += "Performance requires adaptive quality: ${evidence.performance.messages.joinToString()}"; PerformanceStatus.Fail -> blockers += "Performance budget gate failed." }
        val score = (100 - blockers.size * 25 - conditions.size * 8).coerceIn(0, 100)
        val status = when { blockers.isNotEmpty() -> ReleaseGateStatus.Blocked; conditions.isNotEmpty() -> ReleaseGateStatus.Conditional; else -> ReleaseGateStatus.Pass }
        return ReleaseReadinessReport(status, score, passed, blockers, conditions)
    }
}
