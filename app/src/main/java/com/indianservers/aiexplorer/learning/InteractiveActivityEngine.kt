package com.indianservers.aiexplorer.learning

import com.indianservers.aiexplorer.core.EquivalenceStatus
import com.indianservers.aiexplorer.core.FormalMathDestination
import com.indianservers.aiexplorer.core.ManipulativeEngine
import com.indianservers.aiexplorer.core.ManipulativeScene
import com.indianservers.aiexplorer.core.TrustedMathKernel
import com.indianservers.aiexplorer.workspace.MathModule
import com.indianservers.aiexplorer.workspace.WorkspaceState
import java.security.MessageDigest
import kotlin.math.max

sealed interface ActivityBlock {
    val id: String
    val title: String
    val skills: Set<String>
    val nextOnPass: String?
    val nextOnFail: String?

    data class Instruction(override val id: String, override val title: String, val body: String, override val skills: Set<String> = emptySet(), override val nextOnPass: String? = null, override val nextOnFail: String? = nextOnPass) : ActivityBlock
    data class MathResponse(override val id: String, override val title: String, val prompt: String, val expected: String, override val skills: Set<String>, val tolerance: Double = 1e-9, override val nextOnPass: String? = null, override val nextOnFail: String? = id) : ActivityBlock
    data class MultipleChoice(override val id: String, override val title: String, val prompt: String, val choices: List<String>, val correctIndex: Int, val explanation: String, override val skills: Set<String>, override val nextOnPass: String? = null, override val nextOnFail: String? = id) : ActivityBlock { init { require(choices.size in 2..8 && correctIndex in choices.indices) } }
    data class WorkspaceCheck(override val id: String, override val title: String, val instruction: String, val rule: CheckpointRule, override val skills: Set<String>, override val nextOnPass: String? = null, override val nextOnFail: String? = id) : ActivityBlock
    data class ManipulativeCheck(override val id: String, override val title: String, val destination: FormalMathDestination, val expectedFragment: String, override val skills: Set<String>, override val nextOnPass: String? = null, override val nextOnFail: String? = id) : ActivityBlock
    data class ProofCheck(override val id: String, override val title: String, val invariant: String, val maximumResidual: Double, override val skills: Set<String>, override val nextOnPass: String? = null, override val nextOnFail: String? = id) : ActivityBlock
    data class Reflection(override val id: String, override val title: String, val prompt: String, val minimumWords: Int = 5, override val skills: Set<String> = emptySet(), override val nextOnPass: String? = null, override val nextOnFail: String? = id) : ActivityBlock
    data class Branch(override val id: String, override val title: String, val skill: String, val threshold: Double, val masteredTarget: String, val practiceTarget: String, override val skills: Set<String> = setOf(skill), override val nextOnPass: String? = masteredTarget, override val nextOnFail: String? = practiceTarget) : ActivityBlock
}

data class InteractiveActivityDocument(
    val id: String,
    val title: String,
    val objective: String,
    val module: MathModule,
    val startBlockId: String,
    val blocks: List<ActivityBlock>,
    val author: String = "Local teacher",
    val schemaVersion: Int = 1,
    val revision: Int = 0,
    val tags: Set<String> = emptySet(),
)

sealed interface ActivityAnswer {
    data object Continue : ActivityAnswer
    data class Text(val value: String) : ActivityAnswer
    data class Choice(val index: Int) : ActivityAnswer
    data object Workspace : ActivityAnswer
    data object Manipulative : ActivityAnswer
    data class ProofResidual(val value: Double) : ActivityAnswer
}

data class ActivityBlockResult(val blockId: String, val passed: Boolean, val feedback: String, val evidence: String, val attempt: Int)
enum class MasteryBand { Beginning, Developing, Secure, Mastered }
data class SkillMastery(val skill: String, val successes: Double = 1.0, val opportunities: Double = 2.0, val evidenceCount: Int = 0, val nextReviewAt: Long = 0L) {
    val score: Double get() = (successes / opportunities).coerceIn(0.0, 1.0)
    val band: MasteryBand get() = when { evidenceCount >= 5 && score >= .9 -> MasteryBand.Mastered; evidenceCount >= 3 && score >= .75 -> MasteryBand.Secure; score >= .5 -> MasteryBand.Developing; else -> MasteryBand.Beginning }
}
data class ActivityRunState(
    val activityId: String,
    val currentBlockId: String?,
    val results: List<ActivityBlockResult> = emptyList(),
    val mastery: Map<String, SkillMastery> = emptyMap(),
    val visited: List<String> = emptyList(),
    val completed: Boolean = false,
) {
    val score: Int get() { val checks = results.filter { it.evidence != "instruction" && it.evidence != "branch" }; return if (checks.isEmpty()) 0 else checks.count { it.passed } * 100 / checks.size }
}
data class ActivityEvaluationContext(
    val workspace: WorkspaceState = WorkspaceState(),
    val manipulatives: ManipulativeScene = ManipulativeScene(),
    val now: Long = 0L,
    val hintsUsed: Int = 0,
)

class InteractiveActivityEngine(
    private val maths: TrustedMathKernel = TrustedMathKernel(),
    private val manipulatives: ManipulativeEngine = ManipulativeEngine(),
) {
    fun start(document: InteractiveActivityDocument) = ActivityRunState(document.id, document.startBlockId, visited = listOf(document.startBlockId))

    fun submit(document: InteractiveActivityDocument, state: ActivityRunState, answer: ActivityAnswer, context: ActivityEvaluationContext = ActivityEvaluationContext()): ActivityRunState {
        require(!state.completed && state.activityId == document.id)
        val block = document.blocks.firstOrNull { it.id == state.currentBlockId } ?: error("Unknown activity block '${state.currentBlockId}'.")
        val previousAttempts = state.results.count { it.blockId == block.id }
        val result = evaluate(document, block, answer, state, context).copy(attempt = previousAttempts + 1)
        val mastery = updateMastery(state.mastery, block.skills, result.passed, previousAttempts + 1, context.hintsUsed, context.now)
        val target = when (block) {
            is ActivityBlock.Branch -> if ((mastery[block.skill]?.score ?: 0.0) >= block.threshold) block.masteredTarget else block.practiceTarget
            else -> if (result.passed) block.nextOnPass else block.nextOnFail
        }
        require(target == null || document.blocks.any { it.id == target }) { "Activity points to missing block '$target'." }
        return state.copy(
            currentBlockId = target,
            results = state.results + result,
            mastery = mastery,
            visited = if (target == null) state.visited else state.visited + target,
            completed = target == null && result.passed,
        )
    }

    private fun evaluate(document: InteractiveActivityDocument, block: ActivityBlock, answer: ActivityAnswer, state: ActivityRunState, context: ActivityEvaluationContext): ActivityBlockResult = when (block) {
        is ActivityBlock.Instruction -> ActivityBlockResult(block.id, answer is ActivityAnswer.Continue, "Continue when ready.", "instruction", 0)
        is ActivityBlock.MathResponse -> {
            val source = (answer as? ActivityAnswer.Text)?.value.orEmpty()
            val evidence = maths.equivalence(source, block.expected, tolerance = block.tolerance)
            ActivityBlockResult(block.id, evidence.equivalent, if (evidence.equivalent) "Equivalent expression verified." else evidence.explanation, "${evidence.status}: ${evidence.exactDifference ?: "numeric evidence"}", 0)
        }
        is ActivityBlock.MultipleChoice -> {
            val selected = (answer as? ActivityAnswer.Choice)?.index
            ActivityBlockResult(block.id, selected == block.correctIndex, if (selected == block.correctIndex) block.explanation else "Revisit the representation, then try again.", "choice=${selected ?: "none"}", 0)
        }
        is ActivityBlock.WorkspaceCheck -> {
            val checkpoint = LearningCheckpoint(block.id, block.title, block.instruction, block.rule, "Workspace condition verified.", "The required construction is not present yet.", listOf(block.instruction))
            val activity = LearningActivity("${document.id}-${block.id}", block.title, document.module, document.objective, listOf(checkpoint), "Interactive activity check")
            val checked = LearningEvaluator.evaluate(activity, context.workspace).checkpoints.single()
            ActivityBlockResult(block.id, checked.passed, checked.message, "workspace objects=${context.workspace.points.size + context.workspace.shapes.size + context.workspace.functions.size}", 0)
        }
        is ActivityBlock.ManipulativeCheck -> {
            val links = manipulatives.links(context.manipulatives).filter { it.destination == block.destination }
            val matched = links.firstOrNull { block.expectedFragment.replace(" ", "") in it.content.replace(" ", "") }
            ActivityBlockResult(block.id, matched != null, matched?.explanation ?: "Build a manipulative representation that generates the requested formal maths.", matched?.content ?: "no matching formal link", 0)
        }
        is ActivityBlock.ProofCheck -> {
            val residual = (answer as? ActivityAnswer.ProofResidual)?.value
            val passed = residual != null && residual.isFinite() && residual <= block.maximumResidual
            ActivityBlockResult(block.id, passed, if (passed) "Invariant verified while manipulating the scene." else "Residual is above ${block.maximumResidual}; test the invariant again.", "${block.invariant}; residual=${residual ?: "missing"}", 0)
        }
        is ActivityBlock.Reflection -> {
            val text = (answer as? ActivityAnswer.Text)?.value.orEmpty().trim(); val words = text.split(Regex("\\s+")).count { it.isNotBlank() }
            ActivityBlockResult(block.id, words >= block.minimumWords, if (words >= block.minimumWords) "Reflection recorded locally." else "Explain your reasoning in at least ${block.minimumWords} words.", "words=$words", 0)
        }
        is ActivityBlock.Branch -> {
            val score = state.mastery[block.skill]?.score ?: 0.0
            ActivityBlockResult(block.id, score >= block.threshold, "Adaptive path selected from prior evidence only.", "branch skill=${block.skill}; score=$score", 0)
        }
    }

    private fun updateMastery(current: Map<String, SkillMastery>, skills: Set<String>, passed: Boolean, attempt: Int, hints: Int, now: Long): Map<String, SkillMastery> {
        if (skills.isEmpty()) return current
        return current + skills.associateWith { skill ->
            val old = current[skill] ?: SkillMastery(skill)
            val weight = (1.0 / max(1, attempt)) * (1.0 / (1 + hints * .25))
            val successes = old.successes + if (passed) weight else 0.0
            val opportunities = old.opportunities + weight
            val score = successes / opportunities
            val reviewDays = when { score >= .9 -> 14; score >= .75 -> 7; score >= .5 -> 2; else -> 1 }
            old.copy(successes = successes, opportunities = opportunities, evidenceCount = old.evidenceCount + 1, nextReviewAt = now + reviewDays * 86_400_000L)
        }
    }
}

data class ActivityDocumentValidation(val valid: Boolean, val errors: List<String>, val reachableBlockIds: Set<String>)

object InteractiveActivityAuthoring {
    fun add(document: InteractiveActivityDocument, block: ActivityBlock) = document.copy(blocks = document.blocks + block, revision = document.revision + 1)
    fun replace(document: InteractiveActivityDocument, block: ActivityBlock) = document.copy(blocks = document.blocks.map { if (it.id == block.id) block else it }, revision = document.revision + 1)
    fun remove(document: InteractiveActivityDocument, blockId: String) = document.copy(blocks = document.blocks.filterNot { it.id == blockId }, revision = document.revision + 1)

    fun validate(document: InteractiveActivityDocument): ActivityDocumentValidation {
        val errors = mutableListOf<String>(); val ids = document.blocks.map { it.id }
        if (ids.size != ids.distinct().size) errors += "Block ids must be unique."
        if (document.startBlockId !in ids) errors += "Start block does not exist."
        document.blocks.forEach { block -> listOf(block.nextOnPass, block.nextOnFail).filterNotNull().forEach { if (it !in ids) errors += "${block.id} points to missing block $it." } }
        val byId = document.blocks.associateBy { it.id }; val reached = mutableSetOf<String>(); val queue = ArrayDeque<String>(); if (document.startBlockId in byId) queue += document.startBlockId
        while (queue.isNotEmpty()) { val id = queue.removeFirst(); if (!reached.add(id)) continue; val block = byId[id] ?: continue; listOf(block.nextOnPass, block.nextOnFail).filterNotNull().filter { it !in reached }.forEach(queue::addLast) }
        val unreachable = ids.toSet() - reached; if (unreachable.isNotEmpty()) errors += "Unreachable blocks: ${unreachable.sorted().joinToString()}."
        if (document.blocks.none { it.nextOnPass == null }) errors += "Activity has no successful terminal block."
        return ActivityDocumentValidation(errors.isEmpty(), errors, reached)
    }

    fun serialize(document: InteractiveActivityDocument): String {
        val body = buildString {
            append("{\"schemaVersion\":${document.schemaVersion},\"id\":\"").append(document.id.escape()).append("\",\"title\":\"").append(document.title.escape())
            append("\",\"objective\":\"").append(document.objective.escape()).append("\",\"module\":\"").append(document.module.name).append("\",\"revision\":").append(document.revision)
            append(",\"start\":\"").append(document.startBlockId.escape()).append("\",\"blocks\":[")
            append(document.blocks.joinToString { "{\"id\":\"${it.id.escape()}\",\"type\":\"${it.javaClass.simpleName}\",\"title\":\"${it.title.escape()}\",\"pass\":${it.nextOnPass?.let { v -> "\"${v.escape()}\"" } ?: "null"},\"fail\":${it.nextOnFail?.let { v -> "\"${v.escape()}\"" } ?: "null"}}" })
            append("]}")
        }
        val checksum = MessageDigest.getInstance("SHA-256").digest(body.toByteArray()).joinToString("") { "%02x".format(it) }
        return "{\"checksum\":\"$checksum\",\"activity\":$body}"
    }
}

object InteractiveActivityCatalog {
    val unitCircle = InteractiveActivityDocument(
        "interactive-trig-mastery", "Interactive Unit Circle Mastery", "Connect the circle, exact values, identities and explanation.", MathModule.Trigonometry, "welcome",
        listOf(
            ActivityBlock.Instruction("welcome", "Explore", "Drag the unit-circle point and watch the linked wave.", nextOnPass = "identity"),
            ActivityBlock.MathResponse("identity", "Verify an identity", "Simplify sin(x)^2+cos(x)^2.", "1", setOf("trig-identities"), nextOnPass = "quadrant", nextOnFail = "identity"),
            ActivityBlock.MultipleChoice("quadrant", "Reason about signs", "In quadrant II, which signs are correct?", listOf("sin +, cos -", "sin -, cos +", "both +", "both -"), 0, "Quadrant II has positive y and negative x.", setOf("unit-circle"), nextOnPass = "reflect"),
            ActivityBlock.Reflection("reflect", "Explain", "Explain why sin²θ+cos²θ stays equal to one while θ changes.", 8, setOf("mathematical-reasoning")),
        ), tags = setOf("trigonometry", "interactive", "proof"),
    )
}

private fun String.escape() = replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
