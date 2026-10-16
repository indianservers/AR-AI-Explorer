package com.indianservers.aiexplorer.assistant.workspace

import java.util.Locale

object RuhiMathLanguage {
    private val numbers=mapOf("zero" to 0,"one" to 1,"two" to 2,"three" to 3,"four" to 4,"five" to 5,"six" to 6,"seven" to 7,"eight" to 8,"nine" to 9,"ten" to 10,"eleven" to 11,"twelve" to 12,"fifteen" to 15,"twenty" to 20,"thirty" to 30,"forty five" to 45,"ninety" to 90)
    fun normalize(source:String):String {
        require(source.length<=2000) { "Keep instructions under 2000 characters." }
        var text=source.trim().lowercase(Locale.US).replace(Regex("^ruhi[,:]?\\s+"),"").replace(Regex("^(?:please|can you|could you)\\s+"),"")
        text=text.replace("first one","first object").replace("second one","second object")
        numbers.entries.sortedByDescending{it.key.length}.forEach{(word,value)->text=text.replace(Regex("\\b${Regex.escape(word)}\\b"),value.toString())}
        text=text.replace(Regex("([a-z]) (?:to the power of|raised to(?: the power of)?) ([0-9]+)"),"$1^$2")
            .replace(Regex("([a-z]) squared\\b"),"$1^2").replace(Regex("([a-z]) cubed\\b"),"$1^3")
            .replace(Regex("\\bequals?\\b"),"=").replace(Regex("\\bplus\\b"),"+").replace(Regex("\\bminus\\b"),"-")
            .replace(Regex("\\bcomma\\b"),",").replace(Regex("\\s+")," ").trimEnd('.', '?')
        return text
    }
    fun isStudioInstruction(source:String):Boolean {
        val text=runCatching{normalize(source)}.getOrDefault("")
        if(text.startsWith("explain ")) return text in setOf("explain this","explain the selected object","explain what i am currently looking at","explain the remaining circle")
        return GraphAssistant.isGraphInstruction(text) || Regex("^(?:in (?:the )?(?:2d|3d) (?:graph|geometry)|(?:2d|3d) (?:graph|geometry):|calculate (?:the )?(?:area|volume|perimeter|surface area)|measure |explain (?:this|the selected|the remaining|what i am)|stop (?:the )?(?:rotation|animation))\\b").containsMatchIn(text)
    }
}
