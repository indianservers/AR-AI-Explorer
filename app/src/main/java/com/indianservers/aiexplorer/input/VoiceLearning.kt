package com.indianservers.aiexplorer.input

import com.indianservers.aiexplorer.assistant.privacy.AssistantConsent

enum class VoiceCommandType { SET_VALUE, PAUSE, PLAY, SHOW_LAYER, EXPLAIN_VIEW, EXPLAIN_MISTAKE, NEXT_STEP, REPEAT_QUESTION, RECORD_OBSERVATION, UNKNOWN }
data class VoiceCommand(val type: VoiceCommandType, val targetId: String? = null, val value: Double? = null, val unit: String? = null, val requiresConfirmation: Boolean = false, val valid: Boolean = true, val issue: String? = null)

class LocalVoiceCommandParser {
    fun parse(transcript: String, allowedTargets: Set<String> = emptySet(), validRanges: Map<String, ClosedFloatingPointRange<Double>> = emptyMap()): VoiceCommand {
        val text = transcript.trim().lowercase().replaceNumberWords()
        val simple = when {
            text == "pause simulation" || text == "pause" -> VoiceCommand(VoiceCommandType.PAUSE)
            text == "play simulation" || text == "resume simulation" -> VoiceCommand(VoiceCommandType.PLAY)
            text.startsWith("explain this") || text == "explain this graph" -> VoiceCommand(VoiceCommandType.EXPLAIN_VIEW)
            "what did i do wrong" in text -> VoiceCommand(VoiceCommandType.EXPLAIN_MISTAKE)
            text == "read the next step" -> VoiceCommand(VoiceCommandType.NEXT_STEP)
            text == "repeat the question" -> VoiceCommand(VoiceCommandType.REPEAT_QUESTION)
            text == "record observation" -> VoiceCommand(VoiceCommandType.RECORD_OBSERVATION)
            text.startsWith("show ") -> VoiceCommand(VoiceCommandType.SHOW_LAYER, targetId = text.removePrefix("show ").replace(' ', '-'), requiresConfirmation = true)
            else -> null
        }
        if (simple != null) return validate(simple, allowedTargets, validRanges)
        val set = Regex("(?:increase|decrease|set) ([a-z ]+?) (?:to )?(-?\\d+(?:\\.\\d+)?)\\s*([a-z]+)?").find(text)
        if (set != null) return validate(VoiceCommand(VoiceCommandType.SET_VALUE, set.groupValues[1].trim().replace(' ', '-'), set.groupValues[2].toDouble(), set.groupValues[3].ifBlank { null }), allowedTargets, validRanges)
        return VoiceCommand(VoiceCommandType.UNKNOWN, valid = false, issue = "Command was not recognised; no action was applied.")
    }
    fun cloudAllowed(consent: AssistantConsent, userConfirmedTranscript: Boolean) = consent.cloudEnabled && consent.voiceMayBeSent && userConfirmedTranscript
    private fun validate(command: VoiceCommand, targets: Set<String>, ranges: Map<String, ClosedFloatingPointRange<Double>>): VoiceCommand {
        if (command.targetId != null && targets.isNotEmpty() && command.targetId !in targets) return command.copy(valid = false, issue = "Target is not available in the active view.")
        val range = command.targetId?.let(ranges::get)
        if (range != null && command.value != null && command.value !in range) return command.copy(valid = false, issue = "Value is outside the model's valid range.")
        return command
    }
    private fun String.replaceNumberWords():String{var result=this;mapOf("zero" to "0","one" to "1","two" to "2","three" to "3","four" to "4","five" to "5","six" to "6","seven" to "7","eight" to "8","nine" to "9","ten" to "10").forEach{(word,value)->result=result.replace(Regex("\\b$word\\b"),value)};return result}
}
