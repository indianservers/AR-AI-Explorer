package com.indianservers.aiexplorer.gamifymaths

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle

internal enum class MathTokenCategory(val label: String, val ink: Color, val fill: Color) {
    Number("Number",Color(0xFF1657B5),Color(0xFFE8F2FF)),
    Function("Function",Color(0xFF7030AF),Color(0xFFF2E7FF)),
    Operator("Operator",Color(0xFF895006),Color(0xFFFFF1D2)),
    Bracket("Bracket",Color(0xFF006D63),Color(0xFFDCF5ED)),
    Constant("Constant",Color(0xFF99268B),Color(0xFFFBE7F8)),
    Symbol("Symbol",Color(0xFFAA254B),Color(0xFFFFE9EF)),
}

internal fun mathTokenCategory(token: String): MathTokenCategory? = when(token.trim()) {
    "(",")","[","]","{","}"->MathTokenCategory.Bracket
    "sin","cos","tan","√","sqrt","log","ln"->MathTokenCategory.Function
    "+","−","-","×","*","÷","/","^","=","²"->MathTokenCategory.Operator
    "π","e"->MathTokenCategory.Constant
    "C","P","!","%","°"->MathTokenCategory.Symbol
    else->if(token.trim().toDoubleOrNull()?.isFinite()==true || token.matches(Regex("-?\\d+/\\d+")))MathTokenCategory.Number else null
}

private val mathTextTokens=Regex("(?<![A-Za-z])(?:sin|cos|tan|sqrt|log|ln|π|e)(?![A-Za-z])|\\d+(?:\\.\\d+)?|[()\\[\\]{}+−×÷^√=%!°²³]")

/** Highlight mathematical content without changing the readable question or its semantics. */
internal fun coloredMathText(text:String):AnnotatedString = AnnotatedString.Builder(text).apply {
    mathTextTokens.findAll(text).forEach{match->
        mathTokenCategory(match.value)?.let{addStyle(SpanStyle(color=it.ink),match.range.first,match.range.last+1)}
    }
}.toAnnotatedString()
