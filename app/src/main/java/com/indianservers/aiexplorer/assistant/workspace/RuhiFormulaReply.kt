package com.indianservers.aiexplorer.assistant.workspace

/** Shared language boundary for a formula requested by the active analysis conversation. */
internal object RuhiFormulaReply {
    private fun body(source:String,family:String):String {
        require(family in setOf("derivative","integral"))
        var text=source.trim().replace(Regex("(?i)^use $family formula\\s+"),"")
        text=RuhiMathLanguage.normalize(text)
        return text.replace(Regex("^(?:use|let|try)\\s+"),"")
    }
    fun recognizes(source:String,family:String):Boolean {
        if(Regex("(?i)^use $family formula\\s+").containsMatchIn(source.trim()))return true
        val text=runCatching{body(source,family)}.getOrNull() ?: return false
        val named=Regex("^(?:(?:y|[a-z_][a-z_0-9]*\\(x\\))\\s*=|x\\b|(?:sin|cos|tan|exp|ln|sqrt|abs)\\s*\\(|square root of\\b|e to the x\\b)")
        val coefficient=Regex("^[-+]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:e[-+]?[0-9]+)?(?=\\s|x\\b|\\(|[+*/^\\-]|$)")
        return named.containsMatchIn(text)||coefficient.containsMatchIn(text)
    }
    fun native(source:String,family:String):String {
        val expression=RuhiFunctionLanguage.expression(body(source,family))
        return if('=' in expression)expression else "y=$expression"
    }
}
