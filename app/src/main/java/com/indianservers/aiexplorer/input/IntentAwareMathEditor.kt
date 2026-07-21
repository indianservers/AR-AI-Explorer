package com.indianservers.aiexplorer.input

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.indianservers.aiexplorer.core.MathInputAnalysis
import com.indianservers.aiexplorer.core.MathInputIntelligence
import com.indianservers.aiexplorer.core.MathInputTokenKind

object IntentMathPalette {
    val Ink = Color(0xFFF2F7FF)
    val Muted = Color(0xFF92A5BE)
    val Command = Color(0xFFA878FF)
    val Function = Color(0xFFFF67D4)
    val Number = Color(0xFF24DCFF)
    val Variable = Color(0xFF50E7A5)
    val Constant = Color(0xFFFFC857)
    val Unit = Color(0xFF2DE2C5)
    val Operator = Color(0xFFFFF1A8)
    val Relation = Color(0xFFFF8A70)
    val Keyword = Color(0xFF73A7FF)
    val Separator = Color(0xFFB4C3D8)
    val Error = Color(0xFFFF5E73)
    val Brackets = listOf(Color(0xFF24DCFF), Color(0xFFA878FF), Color(0xFF50E7A5), Color(0xFFFFC857))

    fun color(kind: MathInputTokenKind, depth: Int = 0): Color = when (kind) {
        MathInputTokenKind.Command -> Command
        MathInputTokenKind.Function -> Function
        MathInputTokenKind.Number -> Number
        MathInputTokenKind.Variable -> Variable
        MathInputTokenKind.Constant -> Constant
        MathInputTokenKind.Unit -> Unit
        MathInputTokenKind.Operator -> Operator
        MathInputTokenKind.Relation -> Relation
        MathInputTokenKind.Bracket -> Brackets[(depth - 1).coerceAtLeast(0) % Brackets.size]
        MathInputTokenKind.Separator -> Separator
        MathInputTokenKind.Keyword -> Keyword
        MathInputTokenKind.Text -> Muted
        MathInputTokenKind.Error -> Error
    }
}

class IntentAwareMathVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val builder = AnnotatedString.Builder(text.text)
        MathInputIntelligence.analyze(text.text).tokens.forEach { token ->
            val style = SpanStyle(
                color = IntentMathPalette.color(token.kind, token.depth),
                background = if (token.kind == MathInputTokenKind.Error) IntentMathPalette.Error.copy(alpha = .20f) else Color.Transparent,
                fontWeight = when (token.kind) {
                    MathInputTokenKind.Command, MathInputTokenKind.Function, MathInputTokenKind.Variable,
                    MathInputTokenKind.Constant, MathInputTokenKind.Unit, MathInputTokenKind.Bracket, MathInputTokenKind.Error -> FontWeight.Bold
                    MathInputTokenKind.Number, MathInputTokenKind.Relation -> FontWeight.SemiBold
                    else -> FontWeight.Normal
                },
            )
            builder.addStyle(style, token.start, token.end)
        }
        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}

@Composable
fun IntentAwareMathField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "Type naturally or enter exact notation",
    singleLine: Boolean = true,
    minLines: Int = 1,
    imeAction: ImeAction = ImeAction.Done,
    onDone: (() -> Unit)? = null,
    showLegend: Boolean = true,
) {
    val analysis = remember(value) { MathInputIntelligence.analyze(value) }
    val transformation = remember { IntentAwareMathVisualTransformation() }
    val healthy = analysis.validBrackets && !analysis.hasErrors
    val accent = when {
        !healthy -> IntentMathPalette.Error
        analysis.confidence >= .85 -> IntentMathPalette.Variable
        else -> IntentMathPalette.Command
    }
    Column(
        modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(listOf(Color(0xEE091522), Color(0xF20D1020), accent.copy(alpha = .10f))),
                RoundedCornerShape(18.dp),
            )
            .border(1.dp, accent.copy(alpha = .62f), RoundedCornerShape(18.dp))
            .padding(8.dp)
            .semantics { contentDescription = "$label. ${analysis.accessibleSummary}" },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(accent, RoundedCornerShape(8.dp)))
                Text(analysis.intent.label.uppercase(), color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = .7.sp)
            }
            Text("${(analysis.confidence * 100).toInt()}% understood", color = IntentMathPalette.Muted, fontSize = 9.sp)
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            placeholder = { Text(placeholder, color = IntentMathPalette.Muted) },
            visualTransformation = transformation,
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium),
            singleLine = singleLine,
            minLines = minLines,
            keyboardOptions = KeyboardOptions(imeAction = imeAction),
            keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
            isError = !healthy,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = IntentMathPalette.Ink,
                unfocusedTextColor = IntentMathPalette.Ink,
                focusedBorderColor = accent,
                unfocusedBorderColor = accent.copy(alpha = .35f),
                cursorColor = IntentMathPalette.Number,
                errorCursorColor = IntentMathPalette.Error,
                focusedLabelColor = accent,
                unfocusedLabelColor = IntentMathPalette.Muted,
                focusedContainerColor = Color(0x77101B2A),
                unfocusedContainerColor = Color(0x44101B2A),
            ),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(analysis.message, color = if (healthy) IntentMathPalette.Muted else IntentMathPalette.Error, fontSize = 10.sp, modifier = Modifier.weight(1f))
            if (analysis.variables.isNotEmpty()) Text("vars ${analysis.variables.joinToString()}", color = IntentMathPalette.Variable, fontSize = 10.sp)
        }
        if (showLegend) TokenLegend(analysis)
        analysis.suggestions.firstOrNull()?.let { suggestion -> Text("TIP  $suggestion", color = IntentMathPalette.Constant, fontSize = 9.sp) }
    }
}

/** Cursor/selection-preserving variant for calculator and solver editors. */
@Composable
fun IntentAwareMathValueField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "Ask in words or enter exact notation",
    singleLine: Boolean = false,
    minLines: Int = 2,
    showLegend: Boolean = true,
) {
    val analysis = remember(value.text) { MathInputIntelligence.analyze(value.text) }
    val transformation = remember { IntentAwareMathVisualTransformation() }
    val healthy = analysis.validBrackets && !analysis.hasErrors
    val accent = when { !healthy -> IntentMathPalette.Error; analysis.confidence >= .85 -> IntentMathPalette.Variable; else -> IntentMathPalette.Command }
    Column(
        modifier.fillMaxWidth()
            .background(Brush.linearGradient(listOf(Color(0xEE091522), Color(0xF20D1020), accent.copy(.10f))), RoundedCornerShape(18.dp))
            .border(1.dp, accent.copy(.62f), RoundedCornerShape(18.dp)).padding(8.dp)
            .semantics { contentDescription = "$label. ${analysis.accessibleSummary}" },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(accent, RoundedCornerShape(8.dp)))
                Text(analysis.intent.label.uppercase(), color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = .7.sp)
            }
            Text("${(analysis.confidence * 100).toInt()}% understood", color = IntentMathPalette.Muted, fontSize = 9.sp)
        }
        OutlinedTextField(
            value = value, onValueChange = onValueChange, modifier = Modifier.fillMaxWidth(), label = { Text(label) },
            placeholder = { Text(placeholder, color = IntentMathPalette.Muted) }, visualTransformation = transformation,
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium),
            singleLine = singleLine, minLines = minLines, isError = !healthy,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = IntentMathPalette.Ink, unfocusedTextColor = IntentMathPalette.Ink,
                focusedBorderColor = accent, unfocusedBorderColor = accent.copy(.35f), cursorColor = IntentMathPalette.Number,
                errorCursorColor = IntentMathPalette.Error, focusedLabelColor = accent, unfocusedLabelColor = IntentMathPalette.Muted,
                focusedContainerColor = Color(0x77101B2A), unfocusedContainerColor = Color(0x44101B2A),
            ),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(analysis.message, color = if (healthy) IntentMathPalette.Muted else IntentMathPalette.Error, fontSize = 10.sp, modifier = Modifier.weight(1f))
            if (analysis.variables.isNotEmpty()) Text("vars ${analysis.variables.joinToString()}", color = IntentMathPalette.Variable, fontSize = 10.sp)
        }
        if (showLegend) TokenLegend(analysis)
        analysis.suggestions.firstOrNull()?.let { Text("TIP  $it", color = IntentMathPalette.Constant, fontSize = 9.sp) }
    }
}

@Composable
private fun TokenLegend(analysis: MathInputAnalysis) {
    val kinds = analysis.tokens.map { it.kind }.filterNot { it in setOf(MathInputTokenKind.Text, MathInputTokenKind.Error) }.distinct()
    if (kinds.isEmpty()) return
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        kinds.forEach { kind ->
            val color = IntentMathPalette.color(kind)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).background(color, RoundedCornerShape(6.dp)))
                Text(kind.name.lowercase(), color = color, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}
