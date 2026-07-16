package com.indianservers.aiexplorer.core

import com.indianservers.aiexplorer.spatial.ThermalLevel
import com.indianservers.aiexplorer.spatial.TrackingQuality
import java.security.MessageDigest
import kotlin.math.abs
import kotlin.math.sqrt

enum class BenchmarkTopic { Arithmetic, Algebra, Calculus, Trigonometry, LinearAlgebra, Statistics, Units, NumberTheory, Series, ODE }
enum class BenchmarkExpectation { ExactAnswer, AnswerContains, EquivalentExpression, NumericValue, SafeRefusal }
data class MathBenchmarkCase(val id: String, val topic: BenchmarkTopic, val question: String, val expectation: BenchmarkExpectation, val expected: String, val tolerance: Double = 1e-8)
data class MathBenchmarkResult(val id: String, val topic: BenchmarkTopic, val passed: Boolean, val actual: String, val expected: String, val evidence: String, val durationNanos: Long)
data class MathBenchmarkReport(val results: List<MathBenchmarkResult>) {
    val passed: Int get() = results.count { it.passed }
    val failed: Int get() = results.size - passed
    val passRate: Double get() = if (results.isEmpty()) 0.0 else passed.toDouble() / results.size
    val topicCoverage: Map<BenchmarkTopic, Pair<Int, Int>> get() = results.groupBy { it.topic }.mapValues { (_, rows) -> rows.count { it.passed } to rows.size }
}

class DeterministicMathBenchmarkRunner(
    private val solver: MathSolverTutor = MathSolverTutor(),
    private val trusted: TrustedMathKernel = TrustedMathKernel(),
) {
    fun run(cases: List<MathBenchmarkCase>, maximumCases: Int = 500): MathBenchmarkReport {
        require(cases.size <= maximumCases && cases.map { it.id }.distinct().size == cases.size)
        return MathBenchmarkReport(cases.map { case ->
            val start = System.nanoTime(); val solved = solver.solve(case.question).solution
            val comparison = when (case.expectation) {
                BenchmarkExpectation.ExactAnswer -> solved.supported && solved.answer.normalized() == case.expected.normalized()
                BenchmarkExpectation.AnswerContains -> solved.supported && case.expected.normalized() in solved.answer.normalized()
                BenchmarkExpectation.SafeRefusal -> !solved.supported
                BenchmarkExpectation.NumericValue -> {
                    val actual = Regex("-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?").findAll(solved.answer).lastOrNull()?.value?.toDoubleOrNull()
                    val expected = case.expected.toDoubleOrNull(); solved.supported && actual != null && expected != null && abs(actual - expected) <= case.tolerance * maxOf(1.0, abs(expected))
                }
                BenchmarkExpectation.EquivalentExpression -> {
                    val actual = solved.answer.substringAfter('=', solved.answer).trim().removePrefix("≈").trim()
                    solved.supported && trusted.equivalence(actual, case.expected, tolerance = case.tolerance).equivalent
                }
            }
            MathBenchmarkResult(case.id, case.topic, comparison, solved.answer, case.expected, solved.verification, System.nanoTime() - start)
        })
    }
    private fun String.normalized() = lowercase().replace(" ", "").replace("−", "-").replace("×", "*")
}

object ReleaseMathBenchmarkCatalog {
    val smoke = listOf(
        MathBenchmarkCase("fraction", BenchmarkTopic.Arithmetic, "Calculate 1/3 + 1/6", BenchmarkExpectation.ExactAnswer, "1/2"),
        MathBenchmarkCase("linear", BenchmarkTopic.Algebra, "Solve 2x + 3 = 11", BenchmarkExpectation.ExactAnswer, "x = 4"),
        MathBenchmarkCase("quadratic", BenchmarkTopic.Algebra, "x^2 - 5x + 6 = 0", BenchmarkExpectation.ExactAnswer, "x = 3 or x = 2"),
        MathBenchmarkCase("derivative", BenchmarkTopic.Calculus, "Differentiate x^3", BenchmarkExpectation.AnswerContains, "3*x^2"),
        MathBenchmarkCase("integral", BenchmarkTopic.Calculus, "Integrate sin(x) with respect to x", BenchmarkExpectation.AnswerContains, "-cos(x)"),
        MathBenchmarkCase("limit", BenchmarkTopic.Calculus, "Limit (x^2 - 9)/(x - 3) as x -> 3", BenchmarkExpectation.NumericValue, "6"),
        MathBenchmarkCase("trigonometry", BenchmarkTopic.Trigonometry, "Evaluate sin(pi/2) + cos(0)", BenchmarkExpectation.ExactAnswer, "2"),
        MathBenchmarkCase("matrix", BenchmarkTopic.LinearAlgebra, "Determinant [[1,2],[3,4]]", BenchmarkExpectation.ExactAnswer, "-2"),
        MathBenchmarkCase("statistics", BenchmarkTopic.Statistics, "Mean of 4, 7, 7, 10", BenchmarkExpectation.ExactAnswer, "Mean = 7"),
        MathBenchmarkCase("units", BenchmarkTopic.Units, "Convert 5 km to m", BenchmarkExpectation.AnswerContains, "5000 m"),
        MathBenchmarkCase("gcd", BenchmarkTopic.NumberTheory, "gcd 84 and 30", BenchmarkExpectation.ExactAnswer, "6"),
        MathBenchmarkCase("series", BenchmarkTopic.Series, "Maclaurin series of cos(x) through order 4", BenchmarkExpectation.AnswerContains, "O(x^5)"),
        MathBenchmarkCase("ode", BenchmarkTopic.ODE, "Solve differential equation dy/dx = 2y + 4, y(0)=3", BenchmarkExpectation.AnswerContains, "5*exp(2*x)"),
        MathBenchmarkCase("divergent", BenchmarkTopic.Calculus, "Limit (1/x) as x -> 0", BenchmarkExpectation.SafeRefusal, "unsupported"),
    )
}

data class ArQaSample(
    val timestampMillis: Long,
    val expectedPosition: Vec3,
    val observedPosition: Vec3,
    val depthExpectedMeters: Double?,
    val depthObservedMeters: Double?,
    val frameMillis: Double,
    val tracking: TrackingQuality,
    val thermal: ThermalLevel,
)
data class ArQaThresholds(val maximumRmsDriftMeters: Double = .04, val maximumP95FrameMillis: Double = 33.34, val maximumMeanDepthErrorMeters: Double = .05, val minimumSamples: Int = 30, val minimumDurationMillis: Long = 10_000)
data class ArQaReport(val validEvidence: Boolean, val passed: Boolean, val rmsDriftMeters: Double, val p95FrameMillis: Double, val meanDepthErrorMeters: Double?, val trackingLossSamples: Int, val peakThermal: ThermalLevel, val diagnostics: List<String>)

object ArDeviceQaEngine {
    fun assess(samples: List<ArQaSample>, thresholds: ArQaThresholds = ArQaThresholds()): ArQaReport {
        val diagnostics = mutableListOf<String>(); val duration = (samples.maxOfOrNull { it.timestampMillis } ?: 0) - (samples.minOfOrNull { it.timestampMillis } ?: 0)
        val valid = samples.size >= thresholds.minimumSamples && duration >= thresholds.minimumDurationMillis
        if (!valid) diagnostics += "Physical evidence requires at least ${thresholds.minimumSamples} samples across ${thresholds.minimumDurationMillis / 1000} seconds."
        val drift = samples.map { (it.expectedPosition - it.observedPosition).magnitude() }
        val rms = if (drift.isEmpty()) Double.NaN else sqrt(drift.sumOf { value -> value * value } / drift.size)
        val sortedFrames = samples.map { it.frameMillis }.sorted(); val p95 = percentile(sortedFrames, .95)
        val depthErrors = samples.mapNotNull { row -> row.depthExpectedMeters?.let { expected -> row.depthObservedMeters?.let { abs(expected - it) } } }
        val meanDepth = depthErrors.average().takeUnless(Double::isNaN)
        val trackingLoss = samples.count { it.tracking in setOf(TrackingQuality.Lost, TrackingQuality.Stopped, TrackingQuality.Limited) }
        val thermal = samples.maxOfOrNull { it.thermal } ?: ThermalLevel.Nominal
        if (rms.isFinite() && rms > thresholds.maximumRmsDriftMeters) diagnostics += "RMS anchor drift exceeds ${thresholds.maximumRmsDriftMeters} m."
        if (p95.isFinite() && p95 > thresholds.maximumP95FrameMillis) diagnostics += "Frame p95 exceeds ${thresholds.maximumP95FrameMillis} ms."
        if (meanDepth != null && meanDepth > thresholds.maximumMeanDepthErrorMeters) diagnostics += "Mean depth error exceeds ${thresholds.maximumMeanDepthErrorMeters} m."
        if (trackingLoss > samples.size / 20) diagnostics += "Tracking was poor/lost in more than 5% of samples."
        if (thermal >= ThermalLevel.Severe) diagnostics += "Device reached severe or critical thermal state."
        val pass = valid && diagnostics.isEmpty()
        return ArQaReport(valid, pass, rms, p95, meanDepth, trackingLoss, thermal, diagnostics)
    }
    private fun percentile(sorted: List<Double>, p: Double): Double = if (sorted.isEmpty()) Double.NaN else sorted[((sorted.size - 1) * p).toInt().coerceIn(sorted.indices)]
}

data class AccessibilityNodeEvidence(
    val id: String,
    val label: String?,
    val role: String?,
    val touchWidthDp: Double,
    val touchHeightDp: Double,
    val keyboardReachable: Boolean,
    val contrastRatio: Double,
    val animated: Boolean = false,
    val reducedMotionAlternative: Boolean = true,
)
data class AccessibilityFinding(val nodeId: String, val severity: String, val message: String)
data class AccessibilityQaReport(val passed: Boolean, val findings: List<AccessibilityFinding>, val nodesChecked: Int)

object AccessibilityQaEngine {
    fun audit(nodes: List<AccessibilityNodeEvidence>): AccessibilityQaReport {
        val findings = buildList {
            nodes.forEach { node ->
                if (node.label.isNullOrBlank()) add(AccessibilityFinding(node.id, "Error", "Interactive node has no spoken label."))
                if (node.role.isNullOrBlank()) add(AccessibilityFinding(node.id, "Warning", "Semantic role is not declared."))
                if (node.touchWidthDp < 48 || node.touchHeightDp < 48) add(AccessibilityFinding(node.id, "Error", "Touch target is smaller than 48 dp."))
                if (!node.keyboardReachable) add(AccessibilityFinding(node.id, "Error", "Node is not keyboard/switch reachable."))
                if (node.contrastRatio < 4.5) add(AccessibilityFinding(node.id, "Error", "Text contrast is below 4.5:1."))
                if (node.animated && !node.reducedMotionAlternative) add(AccessibilityFinding(node.id, "Error", "Animation has no reduced-motion alternative."))
            }
        }
        return AccessibilityQaReport(findings.none { it.severity == "Error" }, findings, nodes.size)
    }
}

data class QaEvidenceSection(val id: String, val status: String, val details: List<String>)
data class ReleaseQaEvidenceBundle(val buildLabel: String, val deviceLabel: String, val createdAt: Long, val sections: List<QaEvidenceSection>)
object ReleaseQaEvidenceCodec {
    fun encode(bundle: ReleaseQaEvidenceBundle): String {
        val records = bundle.sections.sortedBy { it.id }.joinToString("\n") { section -> "${pack(section.id)}|${pack(section.status)}|${pack(section.details.joinToString("\u001f"))}" }
        val body = "AIEXPLORER_QA|1|${pack(bundle.buildLabel)}|${pack(bundle.deviceLabel)}|${bundle.createdAt}\n$records"
        return "$body\nSHA256|${sha256(body)}"
    }
    fun verify(source: String): Boolean { val lines = source.lines(); val signature = lines.lastOrNull()?.takeIf { it.startsWith("SHA256|") }?.substringAfter('|') ?: return false; return MessageDigest.isEqual(signature.toByteArray(), sha256(lines.dropLast(1).joinToString("\n")).toByteArray()) }
    private fun pack(value: String) = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray())
    private fun sha256(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
}
