package com.indianservers.aiexplorer.assistant.workspace

/** Model advice is limited to an explicit grammar; it never supplies coordinates or executes code. */
data class GraphIntentPrediction(val intent: String, val confidence: Float, val margin: Float)
fun interface GraphIntentClassifier { fun predict(text: String): GraphIntentPrediction }
object GraphLanguageRouter {
    @Volatile var classifier: GraphIntentClassifier? = null
    fun canonical(text: String): String? {
        if (!Regex("^(?:relocate|reposition|enlarge|shrink|turn|cluster|erase|dissolve|break apart|combine)\\b").containsMatchIn(text)) return null
        val prediction=classifier?.predict(text) ?: return null
        if(prediction.confidence<.85f || prediction.margin<.35f || !prediction.confidence.isFinite() || !prediction.margin.isFinite()) return null
        return when(prediction.intent) {
            "MOVE" -> Regex("^(?:relocate|reposition) (.+)").matchEntire(text)?.let { "move ${it.groupValues[1]}" }
            "SCALE" -> Regex("(enlarge|shrink) (.+?) by (?:a )?factor (?:of )?([0-9]+(?:\\.[0-9]+)?)").matchEntire(text)?.let {
                val n=it.groupValues[3].toDouble();require(n.isFinite() && n>0 && n<=1000) { "Use a positive finite scale factor up to 1000." }
                "scale ${it.groupValues[2]} by ${if(it.groupValues[1]=="shrink") 1/n else n}"
            }
            "ROTATE" -> Regex("turn (.+?) by (.+)").matchEntire(text)?.let { "rotate ${it.groupValues[1]} by ${it.groupValues[2]}" }
            "GROUP" -> Regex("(?:cluster (.+)|combine (.+) into a group)").matchEntire(text)?.let { "group ${it.groupValues[1].ifEmpty { it.groupValues[2] }}" }
            "UNGROUP" -> if(text in setOf("dissolve the group","break apart the group")) "ungroup the group" else null
            "DELETE" -> Regex("erase (.+)").matchEntire(text)?.let { "delete ${it.groupValues[1]}" }
            else -> null
        }
    }
}
