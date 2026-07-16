package com.indianservers.aiexplorer.learning

import com.indianservers.aiexplorer.core.GraphSegment
import com.indianservers.aiexplorer.core.Vec2
import com.indianservers.aiexplorer.workspace.MathModule
import java.security.MessageDigest
import kotlin.math.abs

data class ActivityAssignment(
    val id: String,
    val title: String,
    val activityIds: List<String>,
    val learnerAliases: Set<String>,
    val dueAt: Long? = null,
    val allowHints: Boolean = true,
    val attemptLimit: Int? = null,
)
enum class ClassroomEventKind { Started, Attempted, HintUsed, BlockPassed, Completed }
data class ClassroomEvent(val id: String, val assignmentId: String, val learnerAlias: String, val activityId: String, val blockId: String?, val kind: ClassroomEventKind, val timestamp: Long, val score: Int? = null)
data class LearnerActivitySummary(val learnerAlias: String, val completed: Int, val attempts: Int, val hints: Int, val averageScore: Double, val needsAttention: Boolean)
data class ClassroomActivitySummary(val assignmentId: String, val learners: List<LearnerActivitySummary>, val completionRate: Double, val commonStruggleBlocks: List<String>)

object ClassroomActivityEngine {
    fun summarize(assignment: ActivityAssignment, events: List<ClassroomEvent>): ClassroomActivitySummary {
        val scoped = events.filter { it.assignmentId == assignment.id && it.activityId in assignment.activityIds && it.learnerAlias in assignment.learnerAliases }.distinctBy { it.id }
        val learners = assignment.learnerAliases.sorted().map { alias ->
            val own = scoped.filter { it.learnerAlias == alias }
            val completed = own.filter { it.kind == ClassroomEventKind.Completed }.map { it.activityId }.distinct().size
            val attempts = own.count { it.kind == ClassroomEventKind.Attempted }
            val hints = own.count { it.kind == ClassroomEventKind.HintUsed }
            val scores = own.mapNotNull { it.score }
            LearnerActivitySummary(alias, completed, attempts, hints, scores.average().takeIf { !it.isNaN() } ?: 0.0, completed < assignment.activityIds.size && (attempts >= 3 || hints >= 2))
        }
        val total = (assignment.learnerAliases.size * assignment.activityIds.size).coerceAtLeast(1)
        val completion = learners.sumOf { it.completed }.toDouble() / total
        val struggles = scoped.filter { it.kind == ClassroomEventKind.Attempted && it.blockId != null }.groupingBy { it.blockId!! }.eachCount().entries.sortedByDescending { it.value }.filter { it.value >= 2 }.take(5).map { it.key }
        return ClassroomActivitySummary(assignment.id, learners, completion, struggles)
    }
}

enum class ExamAction { OpenModule, UseTool, RequestHint, OpenNetwork, UseClipboard, ImportFile, ExportFile, StartAR }
data class ExamPolicy(
    val id: String,
    val title: String,
    val allowedModules: Set<MathModule>,
    val allowedTools: Set<String>,
    val durationMinutes: Int,
    val hintsAllowed: Boolean = false,
    val networkAllowed: Boolean = false,
    val clipboardAllowed: Boolean = false,
    val fileTransferAllowed: Boolean = false,
    val arAllowed: Boolean = false,
)
data class ExamDecision(val allowed: Boolean, val reason: String)
data class ExamAuditEvent(val sequence: Int, val timestamp: Long, val action: ExamAction, val detail: String, val allowed: Boolean, val previousHash: String, val hash: String)

class ExamSessionGuard(val policy: ExamPolicy, val startedAt: Long) {
    private val audit = mutableListOf<ExamAuditEvent>()
    fun check(action: ExamAction, detail: String, now: Long): ExamDecision {
        val expired = now > startedAt + policy.durationMinutes * 60_000L
        val decision = when {
            expired -> ExamDecision(false, "Exam time has ended.")
            action == ExamAction.OpenModule -> ExamDecision(runCatching { MathModule.valueOf(detail) }.getOrNull() in policy.allowedModules, "Module policy")
            action == ExamAction.UseTool -> ExamDecision(detail in policy.allowedTools, "Tool policy")
            action == ExamAction.RequestHint -> ExamDecision(policy.hintsAllowed, "Hints are ${if (policy.hintsAllowed) "enabled" else "disabled"}.")
            action == ExamAction.OpenNetwork -> ExamDecision(policy.networkAllowed, "Network is ${if (policy.networkAllowed) "enabled" else "disabled"}.")
            action == ExamAction.UseClipboard -> ExamDecision(policy.clipboardAllowed, "Clipboard policy")
            action in setOf(ExamAction.ImportFile, ExamAction.ExportFile) -> ExamDecision(policy.fileTransferAllowed, "File-transfer policy")
            action == ExamAction.StartAR -> ExamDecision(policy.arAllowed, "AR policy")
            else -> ExamDecision(false, "Action is not covered by the exam policy.")
        }
        appendAudit(now, action, detail, decision.allowed)
        return decision
    }
    fun auditTrail(): List<ExamAuditEvent> = audit.toList()
    fun verifyAuditChain(): Boolean = audit.indices.all { index ->
        val item = audit[index]; val previous = if (index == 0) "GENESIS" else audit[index - 1].hash
        item.previousHash == previous && item.hash == hash(item.sequence, item.timestamp, item.action, item.detail, item.allowed, previous)
    }
    private fun appendAudit(now: Long, action: ExamAction, detail: String, allowed: Boolean) {
        val previous = audit.lastOrNull()?.hash ?: "GENESIS"; val sequence = audit.size + 1
        audit += ExamAuditEvent(sequence, now, action, detail, allowed, previous, hash(sequence, now, action, detail, allowed, previous))
    }
    private fun hash(sequence: Int, timestamp: Long, action: ExamAction, detail: String, allowed: Boolean, previous: String): String =
        MessageDigest.getInstance("SHA-256").digest("$sequence|$timestamp|$action|$detail|$allowed|$previous".toByteArray()).joinToString("") { "%02x".format(it) }
}

data class SonificationNote(val time: Double, val pitchHz: Double, val pan: Double, val duration: Double, val emphasis: Boolean, val description: String)
data class AccessibleGraphSummary(val description: String, val notes: List<SonificationNote>, val landmarks: List<String>)

/** Renderer-neutral graph audio plan. Android audio, haptics or a screen reader can consume the same notes. */
object GraphAccessibilityEngine {
    fun sonify(segments: List<GraphSegment>, minimumPitch: Double = 220.0, maximumPitch: Double = 880.0): AccessibleGraphSummary {
        val points = segments.flatMap { it.points }
        if (points.isEmpty()) return AccessibleGraphSummary("No finite graph points are available.", emptyList(), emptyList())
        val minX = points.minOf { it.x }; val maxX = points.maxOf { it.x }.takeIf { it > minX } ?: minX + 1
        val minY = points.minOf { it.y }; val maxY = points.maxOf { it.y }.takeIf { it > minY } ?: minY + 1
        val roots = mutableListOf<Vec2>(); val extrema = mutableListOf<Vec2>()
        segments.forEach { segment ->
            segment.points.zipWithNext().forEach { (a, b) -> if (a.y == 0.0 || a.y * b.y < 0) roots += if (abs(a.y) <= abs(b.y)) a else b }
            segment.points.windowed(3).forEach { (a, b, c) -> if ((b.y - a.y) * (c.y - b.y) <= 0) extrema += b }
        }
        val notes = points.mapIndexed { index, point ->
            val xRatio = (point.x - minX) / (maxX - minX); val yRatio = ((point.y - minY) / (maxY - minY)).coerceIn(0.0, 1.0)
            val landmark = roots.any { it.distanceTo(point) < (maxX - minX) / points.size.coerceAtLeast(1) } || extrema.any { it == point }
            SonificationNote(xRatio * 4.0, minimumPitch + yRatio * (maximumPitch - minimumPitch), xRatio * 2 - 1, if (landmark) .12 else .045, landmark, "x=${format(point.x)}, y=${format(point.y)}")
        }
        val landmarks = roots.distinctBy { format(it.x) }.take(8).map { "Root near x=${format(it.x)}" } + extrema.distinct().take(8).map { "Turning point near (${format(it.x)}, ${format(it.y)})" }
        return AccessibleGraphSummary("Graph has ${segments.size} continuous segment${if (segments.size == 1) "" else "s"}, x from ${format(minX)} to ${format(maxX)}, and y from ${format(minY)} to ${format(maxY)}.", notes, landmarks)
    }
    private fun format(value: Double) = String.format(java.util.Locale.US, "%.4g", value)
}

enum class AccessibleMathLandmarkKind { Heading, Input, Action, Result, Warning, GraphPoint }
data class AccessibleMathLandmark(val id: String, val kind: AccessibleMathLandmarkKind, val label: String, val value: String = "", val keyboardAction: String? = null)
class AccessibleMathNavigator(landmarks: List<AccessibleMathLandmark>) {
    private val items = landmarks.distinctBy { it.id }
    fun first() = items.firstOrNull()
    fun next(currentId: String) = items.getOrNull((items.indexOfFirst { it.id == currentId } + 1).coerceAtMost(items.lastIndex))
    fun previous(currentId: String) = items.getOrNull((items.indexOfFirst { it.id == currentId } - 1).coerceAtLeast(0))
    fun byKind(kind: AccessibleMathLandmarkKind) = items.filter { it.kind == kind }
}
