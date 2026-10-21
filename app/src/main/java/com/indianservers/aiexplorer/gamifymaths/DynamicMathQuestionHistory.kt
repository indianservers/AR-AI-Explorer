package com.indianservers.aiexplorer.gamifymaths

import java.security.MessageDigest
import kotlin.random.Random

// Bound storage while retaining recent content and template identities across visits.
internal const val RecentMathQuestionCount = 128

internal fun mathQuestionKey(content: String): String = MessageDigest.getInstance("SHA-256")
    .digest(content.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it.toInt() and 255) }

internal fun workshopQuestionHistoryKey(round:WorkshopChallenge):String = "${round.templateId}|${mathQuestionKey(round.signature)}"

internal fun rememberMathQuestion(history: List<String>, key: String): ArrayList<String> =
    ArrayList((history.filterNot { it == key } + key).takeLast(RecentMathQuestionCount))

internal data class FreshWorkshopQuestion(val seed: Long, val challenge: WorkshopChallenge)

/** Choose content and its answer together. Retaining the seed also makes restoration exact. */
internal fun freshWorkshopQuestion(
    id: String, level: Int, difficulty: Int, focus: Int, recentKeys: List<String>,
    random: Random = Random.Default,
): FreshWorkshopQuestion {
    var oldest: FreshWorkshopQuestion? = null
    var oldestIndex = Int.MAX_VALUE
    repeat(512) {
        val seed = random.nextLong()
        val candidate = generateWorkshopChallenge(id, level, difficulty, seed, focus)
        val contentKey=mathQuestionKey(candidate.signature)
        val key=workshopQuestionHistoryKey(candidate)
        val index=recentKeys.indexOfFirst{it.substringAfterLast('|')==contentKey}
        val recentTemplates=recentKeys.takeLast(3).map{it.substringBefore('|',"")}
        if(index<0 && candidate.templateId !in recentTemplates)return FreshWorkshopQuestion(seed,candidate)
        if(index<0 && oldest==null){oldest=FreshWorkshopQuestion(seed,candidate);oldestIndex=-1}
        if (index < oldestIndex) {
            oldest = FreshWorkshopQuestion(seed, candidate)
            oldestIndex = index
        }
    }
    // A deliberately constrained topic may exhaust its pool; prefer its least recent question.
    return checkNotNull(oldest)
}
