package com.indianservers.aiexplorer.learning

import com.indianservers.aiexplorer.core.SolidType
import com.indianservers.aiexplorer.core.stripEquation
import com.indianservers.aiexplorer.workspace.MathModule
import com.indianservers.aiexplorer.workspace.Shape2DType
import com.indianservers.aiexplorer.workspace.WorkspaceJson
import com.indianservers.aiexplorer.workspace.WorkspaceState

enum class LearningRole { Learner, Teacher }
enum class ProgressStatus { NotStarted, InProgress, Completed }

sealed interface CheckpointRule {
    data class MinimumPoints(val count: Int, val minimumSeparation: Double = 0.0) : CheckpointRule
    data class HasShape(val type: Shape2DType) : CheckpointRule
    data class ExpressionContains(val fragments: List<String>) : CheckpointRule
    data class ModuleOpen(val module: MathModule) : CheckpointRule
    data class SolidFamilies(val types: Set<SolidType>, val minimumCount: Int = types.size) : CheckpointRule
    data class SurfaceExpression(val accepted: Set<String>) : CheckpointRule
}

data class LearningCheckpoint(
    val id: String,
    val title: String,
    val instruction: String,
    val rule: CheckpointRule,
    val successMessage: String,
    val misconception: String,
    val hints: List<String>,
    val weight: Int = 1,
)

data class LearningActivity(
    val id: String,
    val title: String,
    val module: MathModule,
    val objective: String,
    val checkpoints: List<LearningCheckpoint>,
    val explanation: String,
    val allowedTools: Set<String> = emptySet(),
    val accessibilityLabel: String = title,
    val tags: Set<String> = emptySet(),
) {
    val target: String get() = checkpoints.firstOrNull()?.instruction.orEmpty()
    val hint: String get() = checkpoints.firstOrNull()?.hints?.firstOrNull().orEmpty()
    val proof: String get() = explanation
}

data class CheckpointResult(
    val checkpointId: String,
    val passed: Boolean,
    val message: String,
    val misconception: String? = null,
)

data class LearningValidation(
    val passed: Boolean,
    val message: String,
    val checkpoints: List<CheckpointResult> = emptyList(),
)

data class LearnerProgress(
    val lessonId: String,
    val status: ProgressStatus = ProgressStatus.NotStarted,
    val completedCheckpointIds: Set<String> = emptySet(),
    val attempts: Int = 0,
    val hintsUsed: Int = 0,
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val updatedAt: Long = 0L,
) {
    fun percent(lesson: LearningActivity): Int = if (lesson.checkpoints.isEmpty()) 100 else {
        (completedCheckpointIds.size * 100 / lesson.checkpoints.size).coerceIn(0, 100)
    }
}

data class Assignment(
    val id: String,
    val title: String,
    val lessonIds: List<String>,
    val dueAt: Long? = null,
    val teacherNote: String = "",
)

data class TeacherSummary(
    val assignedLessons: Int,
    val completedLessons: Int,
    val checkpointsCompleted: Int,
    val attempts: Int,
    val hintsUsed: Int,
    val needsAttention: List<String>,
)

object LearningEvaluator {
    fun evaluate(activity: LearningActivity, state: WorkspaceState): LearningValidation {
        val results = activity.checkpoints.map { checkpoint -> evaluate(checkpoint, state) }
        val firstFailure = results.firstOrNull { !it.passed }
        return LearningValidation(
            passed = firstFailure == null,
            message = firstFailure?.message ?: "All ${results.size} checkpoints passed.",
            checkpoints = results,
        )
    }

    private fun evaluate(checkpoint: LearningCheckpoint, state: WorkspaceState): CheckpointResult {
        val passed = when (val rule = checkpoint.rule) {
            is CheckpointRule.MinimumPoints -> state.points.size >= rule.count &&
                (rule.minimumSeparation <= 0.0 || state.points.take(rule.count).zipWithNext().all { (a, b) -> a.distanceTo(b) >= rule.minimumSeparation })
            is CheckpointRule.HasShape -> state.shapes.any { it.type == rule.type }
            is CheckpointRule.ExpressionContains -> {
                val expression = state.functions.firstOrNull()?.expression?.replace(" ", "").orEmpty()
                rule.fragments.all { it.replace(" ", "") in expression }
            }
            is CheckpointRule.ModuleOpen -> state.module == rule.module
            is CheckpointRule.SolidFamilies -> state.solids.size >= rule.minimumCount && rule.types.all { type -> state.solids.any { it.type == type } }
            is CheckpointRule.SurfaceExpression -> stripEquation(state.surfaceExpression).replace(" ", "") in rule.accepted
        }
        return CheckpointResult(
            checkpointId = checkpoint.id,
            passed = passed,
            message = if (passed) checkpoint.successMessage else checkpoint.instruction,
            misconception = if (passed) null else checkpoint.misconception,
        )
    }

    fun recordAttempt(activity: LearningActivity, previous: LearnerProgress?, validation: LearningValidation, now: Long): LearnerProgress {
        val completed = validation.checkpoints.filter { it.passed }.mapTo(mutableSetOf()) { it.checkpointId }
        completed += previous?.completedCheckpointIds.orEmpty()
        val done = activity.checkpoints.all { it.id in completed }
        return LearnerProgress(
            lessonId = activity.id,
            status = if (done) ProgressStatus.Completed else ProgressStatus.InProgress,
            completedCheckpointIds = completed,
            attempts = (previous?.attempts ?: 0) + 1,
            hintsUsed = previous?.hintsUsed ?: 0,
            startedAt = previous?.startedAt ?: now,
            completedAt = if (done) previous?.completedAt ?: now else null,
            updatedAt = now,
        )
    }

    fun nextHint(activity: LearningActivity, validation: LearningValidation, hintsUsed: Int): String {
        val failedId = validation.checkpoints.firstOrNull { !it.passed }?.checkpointId
        val checkpoint = activity.checkpoints.firstOrNull { it.id == failedId } ?: activity.checkpoints.firstOrNull()
            ?: return "Explore the construction and check again."
        return checkpoint.hints.getOrElse(hintsUsed.coerceAtLeast(0)) { checkpoint.hints.lastOrNull() ?: checkpoint.instruction }
    }
}

object ClassroomEngine {
    fun summarize(assignment: Assignment, lessons: List<LearningActivity>, progress: Map<String, LearnerProgress>): TeacherSummary {
        val assigned = assignment.lessonIds.mapNotNull { id -> lessons.firstOrNull { it.id == id } }
        val records = assigned.mapNotNull { progress[it.id] }
        val attention = assigned.filter { lesson ->
            val record = progress[lesson.id]
            record != null && record.status != ProgressStatus.Completed && (record.attempts >= 2 || record.hintsUsed >= 2)
        }.map { it.title }
        return TeacherSummary(
            assignedLessons = assigned.size,
            completedLessons = records.count { it.status == ProgressStatus.Completed },
            checkpointsCompleted = records.sumOf { it.completedCheckpointIds.size },
            attempts = records.sumOf { it.attempts },
            hintsUsed = records.sumOf { it.hintsUsed },
            needsAttention = attention,
        )
    }
}

enum class LearningOperationType { Attempt, HintUsed, Complete }
data class LearningOperation(val id: String, val lessonId: String, val type: LearningOperationType, val timestamp: Long)

/** Offline-first operation queue. Operations are idempotent and safe to replay after reconnection. */
class OfflineLearningQueue {
    private val pending = linkedMapOf<String, LearningOperation>()
    fun enqueue(operation: LearningOperation) { pending.putIfAbsent(operation.id, operation) }
    fun pending(): List<LearningOperation> = pending.values.sortedWith(compareBy(LearningOperation::timestamp, LearningOperation::id))
    fun acknowledge(ids: Set<String>) { ids.forEach(pending::remove) }
}

data class PackageValidation(val valid: Boolean, val message: String, val schemaVersion: Int? = null)

object LearningPackage {
    const val schemaVersion = 3
    private const val maxPackageChars = 2_000_000

    fun export(
        state: WorkspaceState,
        activeLessonId: String,
        progress: Map<String, LearnerProgress>,
        assignments: List<Assignment>,
    ): String = buildString {
        appendLine("{")
        appendLine("  \"packageType\": \"ai-explorer-learning\",")
        appendLine("  \"schemaVersion\": $schemaVersion,")
        appendLine("  \"workspace\": ${WorkspaceJson.export(state).prependIndent("  ").trim()},")
        appendLine("  \"learning\": {")
        appendLine("    \"activeLessonId\": \"${activeLessonId.jsonEscaped()}\",")
        appendLine("    \"progress\": [${progress.values.joinToString { it.toJson() }}],")
        appendLine("    \"assignments\": [${assignments.joinToString { it.toJson() }}]")
        appendLine("  }")
        appendLine("}")
    }

    fun validate(source: String): PackageValidation {
        if (source.length > maxPackageChars) return PackageValidation(false, "Package exceeds the 2 MB safety limit.")
        if (!source.trimStart().startsWith("{") || !source.trimEnd().endsWith("}")) return PackageValidation(false, "Package must be a JSON object.")
        if (!balanced(source)) return PackageValidation(false, "Package has unbalanced JSON delimiters.")
        if (!Regex("\"packageType\"\\s*:\\s*\"ai-explorer-learning\"").containsMatchIn(source)) return PackageValidation(false, "Unsupported package type.")
        val version = Regex("\"schemaVersion\"\\s*:\\s*(\\d+)").find(source)?.groupValues?.get(1)?.toIntOrNull()
            ?: return PackageValidation(false, "Missing schema version.")
        if (version !in 2..schemaVersion) return PackageValidation(false, "Unsupported schema version $version.", version)
        if (!Regex("\"workspace\"\\s*:").containsMatchIn(source)) return PackageValidation(false, "Package has no workspace.", version)
        if (!Regex("\"learning\"\\s*:").containsMatchIn(source)) return PackageValidation(false, "Package has no learning metadata.", version)
        return PackageValidation(true, "Validated AI Explorer learning package v$version.", version)
    }

    private fun balanced(source: String): Boolean {
        val stack = ArrayDeque<Char>(); var quoted = false; var escaped = false
        source.forEach { char ->
            if (escaped) { escaped = false; return@forEach }
            if (char == '\\' && quoted) { escaped = true; return@forEach }
            if (char == '"') { quoted = !quoted; return@forEach }
            if (!quoted) when (char) {
                '{', '[' -> stack.addLast(char)
                '}' -> if (stack.removeLastOrNull() != '{') return false
                ']' -> if (stack.removeLastOrNull() != '[') return false
            }
        }
        return !quoted && stack.isEmpty()
    }
}

private fun LearnerProgress.toJson() = "{\"lessonId\":\"${lessonId.jsonEscaped()}\",\"status\":\"$status\",\"checkpoints\":[${completedCheckpointIds.joinToString { "\"${it.jsonEscaped()}\"" }}],\"attempts\":$attempts,\"hintsUsed\":$hintsUsed,\"updatedAt\":$updatedAt}"
private fun Assignment.toJson() = "{\"id\":\"${id.jsonEscaped()}\",\"title\":\"${title.jsonEscaped()}\",\"lessonIds\":[${lessonIds.joinToString { "\"${it.jsonEscaped()}\"" }}],\"teacherNote\":\"${teacherNote.jsonEscaped()}\"}"
private fun String.jsonEscaped(): String = buildString {
    this@jsonEscaped.forEach { char -> append(when (char) { '\\' -> "\\\\"; '"' -> "\\\""; '\n' -> "\\n"; '\r' -> "\\r"; '\t' -> "\\t"; else -> char }) }
}

object LearningCatalog {
    val lessons = listOf(
        lesson("midpoint-distance", "Midpoint And Distance", MathModule.Geometry2D, "Explain midpoint and distance from one construction.", "Two separated points", CheckpointRule.MinimumPoints(2, .1), "Two movable points are ready.", "Coincident points do not define a useful direction.", listOf("Drag A or B away from the other point.", "The midpoint averages the two x- and y-coordinates."), "The midpoint formula and Pythagorean theorem explain the construction."),
        LearningActivity("triangle-angle-sum", "Triangle Angle Sum", MathModule.Geometry2D, "Construct and deform a triangle while preserving its angle sum.", listOf(
            checkpoint("triangle-points", "Place vertices", "Place at least three points.", CheckpointRule.MinimumPoints(3, .05), "Three vertices are available.", "A triangle needs three distinct vertices.", "Tap three separated locations."),
            checkpoint("triangle-shape", "Construct triangle", "Use the Triangle tool to connect the vertices.", CheckpointRule.HasShape(Shape2DType.Triangle), "Triangle constructed.", "Three points alone are not yet a triangle object.", "Choose Triangle, then tap three vertices."),
        ), "A parallel through one vertex turns the interior angles into a straight angle.", setOf("Triangle", "Select", "Measure"), "Triangle angle-sum guided activity", setOf("geometry", "proof")),
        lesson("quadratic-roots", "Quadratic Roots And Vertex", MathModule.Graph2D, "Connect roots and vertex to the same quadratic.", "Set f(x)=x^2-4x+3.", CheckpointRule.ExpressionContains(listOf("x^2", "-4", "+3")), "Quadratic target detected.", "Roots and vertex must be read from the same expression.", listOf("Use x^2 - 4*x + 3.", "Factor it as (x-1)(x-3)."), "Completing the square reveals the vertex; factoring reveals the roots."),
        lesson("unit-circle", "Unit Circle Trigonometry", MathModule.Trigonometry, "Connect radians, sine, cosine and tangent.", "Open the trigonometry workspace.", CheckpointRule.ModuleOpen(MathModule.Trigonometry), "Unit-circle workspace ready.", "Angles need a common coordinate model.", listOf("Open Trig from the bottom navigation.", "The point is (cos(theta), sin(theta))."), "The identity follows from x^2+y^2=1."),
        LearningActivity("solid-volume", "3D Volume Lab", MathModule.Geometry3D, "Compare solid volume and surface area.", listOf(
            checkpoint("solid-variety", "Build comparison", "Add a cube, cylinder, cone and sphere.", CheckpointRule.SolidFamilies(setOf(SolidType.Cube, SolidType.Cylinder, SolidType.Cone, SolidType.Sphere), 4), "Four solid families are ready.", "Comparing only one family hides the base-area relationship.", "Use the solid buttons across the top."),
        ), "Prisms use base area times height; related cones and pyramids use one third.", setOf("Move", "Scale", "Cube", "Cylinder", "Cone", "Sphere"), "3D volume comparison activity", setOf("3d", "measurement")),
        lesson("paraboloid-slices", "Paraboloid Slices", MathModule.Graph3D, "Relate a surface to its circular level curves.", "Set z=x^2+y^2.", CheckpointRule.SurfaceExpression(setOf("x^2+y^2", "x*x+y*y")), "Paraboloid target detected.", "A slice is meaningful only when the surface definition matches.", listOf("Choose the Paraboloid preset.", "Fix z and rearrange to x^2+y^2=z."), "Every positive horizontal slice is a circle of radius sqrt(z)."),
    )

    val defaultAssignments = listOf(Assignment("foundations", "Visual Mathematics Foundations", lessons.map { it.id }, teacherNote = "Complete each lab and use hints only after an independent attempt."))

    private fun lesson(id: String, title: String, module: MathModule, objective: String, instruction: String, rule: CheckpointRule, success: String, misconception: String, hints: List<String>, explanation: String) =
        LearningActivity(id, title, module, objective, listOf(LearningCheckpoint("$id-check", title, instruction, rule, success, misconception, hints)), explanation, accessibilityLabel = "$title guided activity")

    private fun checkpoint(id: String, title: String, instruction: String, rule: CheckpointRule, success: String, misconception: String, hint: String) =
        LearningCheckpoint(id, title, instruction, rule, success, misconception, listOf(hint))
}
