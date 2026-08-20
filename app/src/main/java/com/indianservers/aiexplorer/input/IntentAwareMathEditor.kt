package com.indianservers.aiexplorer.input

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import com.indianservers.aiexplorer.core.MathInputAnalysis
import com.indianservers.aiexplorer.core.MathInputIntelligence
import com.indianservers.aiexplorer.core.MathInputTokenKind

internal data class MathEditorViewport(
    val scale: Float = 1f,
    val pan: Offset = Offset.Zero,
)

internal fun transformMathEditorViewport(
    viewport: MathEditorViewport,
    editorSize: IntSize,
    centroid: Offset,
    panChange: Offset,
    zoomChange: Float,
): MathEditorViewport {
    if (editorSize.width <= 0 || editorSize.height <= 0) return viewport
    val nextScale = (viewport.scale * zoomChange).coerceIn(.75f, 4.5f)
    if (nextScale <= 1f) return MathEditorViewport(scale = nextScale)
    val center = Offset(editorSize.width / 2f, editorSize.height / 2f)
    val effectiveZoom = nextScale / viewport.scale.coerceAtLeast(.01f)
    val anchoredPan = viewport.pan + panChange + (centroid - center) * (1f - effectiveZoom)
    val maxX = editorSize.width * (nextScale - 1f) / 2f
    val maxY = editorSize.height * (nextScale - 1f) / 2f
    return MathEditorViewport(
        scale = nextScale,
        pan = Offset(
            anchoredPan.x.coerceIn(-maxX, maxX),
            anchoredPan.y.coerceIn(-maxY, maxY),
        ),
    )
}

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
    private val structured = StructuredMathVisualTransformation()

    override fun filter(text: AnnotatedString): TransformedText = structured.filter(text)
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
    keyboardContext: MathKeyboardContext = inferMathKeyboardContext(label),
    useMathKeyboard: Boolean = isMathematicalInputLabel(label),
    onFocusChange: (Boolean) -> Unit = {},
) {
    var editorValue by remember {
        mutableStateOf(StructuredMathCodec.fromParser(TextFieldValue(value, TextRange(value.length))))
    }
    var keyboardVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val systemKeyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(value) {
        if (StructuredMathCodec.toParser(editorValue).text != value) {
            editorValue = StructuredMathCodec.fromParser(TextFieldValue(value, TextRange(value.length)))
        }
    }
    val emitStructured: (TextFieldValue) -> Unit = {
        editorValue = it
        onValueChange(StructuredMathCodec.toParser(it).text)
    }
    val analysis = remember(value) { MathInputIntelligence.analyze(value) }
    val assistance = remember(value, editorValue.selection.end, keyboardContext) {
        val parserValue = StructuredMathCodec.toParser(editorValue)
        MathInputIntelligence.assist(value, parserValue.selection.end, keyboardContext.toInputContext())
    }
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
        if (useMathKeyboard) {
            MathKeyboardOnlyTextField(
                value = editorValue,
                onValueChange = emitStructured,
                label = label,
                placeholder = placeholder,
                singleLine = singleLine,
                minLines = minLines,
                accent = accent,
                healthy = healthy,
                transformation = transformation,
                onFocusChange = {
                    keyboardVisible = it
                    if (it) systemKeyboard?.hide()
                    onFocusChange(it)
                },
            )
        } else {
            OutlinedTextField(
                value = editorValue,
                onValueChange = emitStructured,
                modifier = Modifier.fillMaxWidth().onFocusChanged { onFocusChange(it.isFocused) },
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
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(analysis.message, color = if (healthy) IntentMathPalette.Muted else IntentMathPalette.Error, fontSize = 10.sp, modifier = Modifier.weight(1f))
            if (analysis.variables.isNotEmpty()) Text("vars ${analysis.variables.joinToString()}", color = IntentMathPalette.Variable, fontSize = 10.sp)
        }
        if (showLegend) TokenLegend(analysis)
        (assistance.primaryMessage ?: analysis.suggestions.firstOrNull())?.let { suggestion ->
            Text("TIP  $suggestion", color = IntentMathPalette.Constant, fontSize = 9.sp, maxLines = 1)
        }
    }
    if (keyboardVisible) {
        AdaptiveMathKeyboardPopup(
            value = editorValue,
            onValueChange = emitStructured,
            context = keyboardContext,
            onDone = {
                onDone?.invoke()
                keyboardVisible = false
                focusManager.clearFocus()
            },
            onDismiss = {
                keyboardVisible = false
                focusManager.clearFocus()
            },
        )
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
    imeAction: ImeAction = ImeAction.Default,
    onDone: (() -> Unit)? = null,
    keyboardContext: MathKeyboardContext = inferMathKeyboardContext(label),
    useMathKeyboard: Boolean = isMathematicalInputLabel(label),
    onFocusChange: (Boolean) -> Unit = {},
    compactChrome: Boolean = false,
) {
    var keyboardVisible by remember { mutableStateOf(false) }
    var structuredValue by remember { mutableStateOf(StructuredMathCodec.fromParser(value)) }
    val focusManager = LocalFocusManager.current
    val systemKeyboard = LocalSoftwareKeyboardController.current
    val analysis = remember(value.text) { MathInputIntelligence.analyze(value.text) }
    val assistance = remember(value.text, value.selection.end, keyboardContext) {
        MathInputIntelligence.assist(value.text, value.selection.end, keyboardContext.toInputContext())
    }
    val transformation = remember { IntentAwareMathVisualTransformation() }
    val healthy = analysis.validBrackets && !analysis.hasErrors
    val accent = when { !healthy -> IntentMathPalette.Error; analysis.confidence >= .85 -> IntentMathPalette.Variable; else -> IntentMathPalette.Command }
    LaunchedEffect(value.text) {
        if (StructuredMathCodec.toParser(structuredValue).text != value.text) {
            structuredValue = StructuredMathCodec.fromParser(value)
        }
    }
    val emitStructured: (TextFieldValue) -> Unit = { next ->
        structuredValue = next
        onValueChange(StructuredMathCodec.toParser(next))
    }
    LaunchedEffect(keyboardVisible, useMathKeyboard) {
        if (keyboardVisible && useMathKeyboard) systemKeyboard?.hide()
    }
    Column(
        modifier.fillMaxWidth()
            .background(Brush.linearGradient(listOf(Color(0xEE091522), Color(0xF20D1020), accent.copy(.10f))), RoundedCornerShape(18.dp))
            .border(1.dp, accent.copy(.62f), RoundedCornerShape(18.dp))
            .padding(if (compactChrome) 4.dp else 8.dp)
            .semantics { contentDescription = "$label. ${analysis.accessibleSummary}" },
        verticalArrangement = Arrangement.spacedBy(if (compactChrome) 3.dp else 6.dp),
    ) {
        if (!compactChrome) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).background(accent, RoundedCornerShape(8.dp)))
                    Text(analysis.intent.label.uppercase(), color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = .7.sp)
                }
                Text("${(analysis.confidence * 100).toInt()}% understood", color = IntentMathPalette.Muted, fontSize = 9.sp)
            }
        }
        if (useMathKeyboard) {
            MathKeyboardOnlyTextField(
                value = structuredValue,
                onValueChange = emitStructured,
                label = label,
                placeholder = placeholder,
                singleLine = singleLine,
                minLines = minLines,
                accent = accent,
                healthy = healthy,
                transformation = transformation,
                onFocusChange = {
                    keyboardVisible = it
                    if (it) systemKeyboard?.hide()
                    onFocusChange(it)
                },
            )
        } else {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth().onFocusChanged { onFocusChange(it.isFocused) },
                label = { Text(label) },
                placeholder = { Text(placeholder, color = IntentMathPalette.Muted) },
                visualTransformation = transformation,
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium),
                singleLine = singleLine,
                minLines = minLines,
                isError = !healthy,
                keyboardOptions = KeyboardOptions(imeAction = imeAction),
                keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = IntentMathPalette.Ink, unfocusedTextColor = IntentMathPalette.Ink,
                    focusedBorderColor = accent, unfocusedBorderColor = accent.copy(.35f), cursorColor = IntentMathPalette.Number,
                    errorCursorColor = IntentMathPalette.Error, focusedLabelColor = accent, unfocusedLabelColor = IntentMathPalette.Muted,
                    focusedContainerColor = Color(0x77101B2A), unfocusedContainerColor = Color(0x44101B2A),
                ),
            )
        }
        if (!compactChrome) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(analysis.message, color = if (healthy) IntentMathPalette.Muted else IntentMathPalette.Error, fontSize = 10.sp, modifier = Modifier.weight(1f))
                if (analysis.variables.isNotEmpty()) Text("vars ${analysis.variables.joinToString()}", color = IntentMathPalette.Variable, fontSize = 10.sp)
            }
            if (showLegend) TokenLegend(analysis)
            (assistance.primaryMessage ?: analysis.suggestions.firstOrNull())?.let {
                Text("TIP  $it", color = IntentMathPalette.Constant, fontSize = 9.sp, maxLines = 1)
            }
        }
    }
    if (keyboardVisible) {
        AdaptiveMathKeyboardPopup(
            value = structuredValue,
            onValueChange = emitStructured,
            context = keyboardContext,
            onDone = {
                onDone?.invoke()
                keyboardVisible = false
                focusManager.clearFocus()
            },
            onDismiss = {
                keyboardVisible = false
                focusManager.clearFocus()
            },
        )
    }
}

@Composable
private fun MathKeyboardOnlyTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    label: String,
    placeholder: String,
    singleLine: Boolean,
    minLines: Int,
    accent: Color,
    healthy: Boolean,
    transformation: VisualTransformation,
    onFocusChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var editorScale by remember { mutableStateOf(1f) }
    var editorPan by remember { mutableStateOf(Offset.Zero) }
    var editorSize by remember { mutableStateOf(IntSize.Zero) }
    val minimumHeight = if (singleLine) 52.dp else (52 + (minLines.coerceAtLeast(1) - 1) * 22).dp
    val updateViewport: (Offset, Offset, Float) -> Unit = { centroid, pan, zoom ->
        val next = transformMathEditorViewport(
            viewport = MathEditorViewport(editorScale, editorPan),
            editorSize = editorSize,
            centroid = centroid,
            panChange = pan,
            zoomChange = zoom,
        )
        editorScale = next.scale
        editorPan = next.pan
    }
    Column(
        modifier
            .fillMaxWidth()
            .background(Color(0x77101B2A), RoundedCornerShape(8.dp))
            .border(
                width = if (focused) 1.5.dp else 1.dp,
                color = if (!healthy) IntentMathPalette.Error else if (focused) accent else accent.copy(alpha = .38f),
                shape = RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                label,
                color = if (focused) accent else IntentMathPalette.Muted,
                fontSize = 10.sp,
                modifier = Modifier.weight(1f),
            )
            if (value.text.isNotEmpty()) {
                EditorZoomControl("−", "Zoom out") {
                    updateViewport(Offset(editorSize.width / 2f, editorSize.height / 2f), Offset.Zero, .8f)
                }
                EditorZoomControl("${(editorScale * 100).toInt()}%", "Reset zoom", wide = true) {
                    editorScale = 1f
                    editorPan = Offset.Zero
                }
                EditorZoomControl("+", "Zoom in") {
                    updateViewport(Offset(editorSize.width / 2f, editorSize.height / 2f), Offset.Zero, 1.25f)
                }
                Box(
                    Modifier
                        .size(22.dp)
                        .semantics {
                            contentDescription = "Clear complete input"
                            role = Role.Button
                        }
                        .clickable { onValueChange(TextFieldValue("")) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("×", color = IntentMathPalette.Error, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minimumHeight)
                .clipToBounds()
                .onSizeChanged { editorSize = it }
                .pointerInput(editorSize) {
                    detectTransformGestures(panZoomLock = false) { centroid, pan, zoom, _ ->
                        updateViewport(centroid, pan, zoom)
                    }
                },
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = minimumHeight)
                    .graphicsLayer {
                        scaleX = editorScale
                        scaleY = editorScale
                        translationX = editorPan.x
                        translationY = editorPan.y
                        transformOrigin = TransformOrigin.Center
                    }
                    .onFocusChanged {
                        focused = it.isFocused
                        onFocusChange(it.isFocused)
                    }
                    .drawWithContent {
                        drawContent()
                        val transformed = transformation.filter(AnnotatedString(value.text))
                        val activeSlot = StructuredMathEditing.activeSlot(value.text, value.selection.end)
                        if (focused && activeSlot != null) {
                            val visualStart = transformed.offsetMapping.originalToTransformed(activeSlot.contentStart)
                            val mappedEnd = transformed.offsetMapping.originalToTransformed(activeSlot.contentEnd)
                            val visualEnd = if (activeSlot.isPlaceholder) visualStart + 1 else maxOf(visualStart + 1, mappedEnd)
                            val layout = textLayout
                            if (layout != null && transformed.text.isNotEmpty()) {
                                var bounds: Rect? = null
                                val last = (visualEnd - 1).coerceAtMost(transformed.text.lastIndex)
                                val first = visualStart.coerceAtLeast(0)
                                if (first <= last) {
                                    for (offset in first..last) {
                                        if (transformed.text[offset] == '\u0305') continue
                                        val glyph = layout.getBoundingBox(offset)
                                        bounds = bounds?.let { existing ->
                                            Rect(
                                                minOf(existing.left, glyph.left),
                                                minOf(existing.top, glyph.top),
                                                maxOf(existing.right, glyph.right),
                                                maxOf(existing.bottom, glyph.bottom),
                                            )
                                        } ?: glyph
                                    }
                                }
                                bounds?.let { slotBounds ->
                                    val padding = 2.dp.toPx()
                                    val topLeft = Offset(slotBounds.left - padding, slotBounds.top - padding)
                                    val size = Size(slotBounds.width + padding * 2, slotBounds.height + padding * 2)
                                    drawRoundRect(
                                        color = IntentMathPalette.Number.copy(alpha = .09f),
                                        topLeft = topLeft,
                                        size = size,
                                        cornerRadius = CornerRadius(4.dp.toPx()),
                                    )
                                    drawRoundRect(
                                        color = IntentMathPalette.Number.copy(alpha = .72f),
                                        topLeft = topLeft,
                                        size = size,
                                        cornerRadius = CornerRadius(4.dp.toPx()),
                                        style = Stroke(width = 1.dp.toPx()),
                                    )
                                }
                            }
                        }
                        if (focused && value.selection.collapsed) {
                            val transformedOffset = transformed.offsetMapping.originalToTransformed(
                                value.selection.start.coerceIn(0, value.text.length),
                            )
                            val cursor = textLayout?.getCursorRect(transformedOffset)
                            if (cursor != null) {
                                drawLine(
                                    color = IntentMathPalette.Number.copy(alpha = .28f),
                                    start = cursor.topCenter,
                                    end = cursor.bottomCenter,
                                    strokeWidth = 5.dp.toPx(),
                                )
                                drawLine(
                                    color = IntentMathPalette.Number,
                                    start = cursor.topCenter,
                                    end = cursor.bottomCenter,
                                    strokeWidth = 1.6.dp.toPx(),
                                )
                            }
                        }
                    },
                readOnly = true,
                singleLine = singleLine,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = IntentMathPalette.Ink,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                ),
                cursorBrush = SolidColor(Color.Transparent),
                visualTransformation = transformation,
                onTextLayout = { textLayout = it },
                keyboardOptions = KeyboardOptions(showKeyboardOnFocus = false),
                decorationBox = { innerTextField ->
                    Box {
                        if (value.text.isBlank()) {
                            Text(placeholder, color = IntentMathPalette.Muted, fontSize = 13.sp)
                        }
                        innerTextField()
                    }
                }
            )
        }
        if (focused) {
            Text(
                "Cursor ${value.selection.end + 1} of ${value.text.length + 1}",
                color = IntentMathPalette.Number,
                fontSize = 8.sp,
            )
        }
    }
}

@Composable
private fun EditorZoomControl(
    label: String,
    description: String,
    wide: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(width = if (wide) 42.dp else 26.dp, height = 26.dp)
            .background(IntentMathPalette.Command.copy(alpha = .13f), RoundedCornerShape(6.dp))
            .border(1.dp, IntentMathPalette.Command.copy(alpha = .38f), RoundedCornerShape(6.dp))
            .semantics {
                contentDescription = description
                role = Role.Button
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = IntentMathPalette.Ink,
            fontSize = if (wide) 8.sp else 15.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun inferMathKeyboardContext(label: String): MathKeyboardContext {
    val normalized = label.lowercase()
    return when {
        "3d" in normalized || "surface" in normalized -> MathKeyboardContext.GRAPH_3D
        "graph" in normalized || "expression" in normalized -> MathKeyboardContext.GRAPH_2D
        "matrix" in normalized || "determinant" in normalized -> MathKeyboardContext.MATRIX
        normalized.startsWith("set") || "set expression" in normalized -> MathKeyboardContext.SETS
        "stat" in normalized || "probab" in normalized || "data" in normalized -> MathKeyboardContext.STATISTICS
        "derivative" in normalized || "integral" in normalized || "limit" in normalized -> MathKeyboardContext.CALCULUS
        "physics" in normalized || "chemistry" in normalized || "unit" in normalized -> MathKeyboardContext.SCIENCE
        else -> MathKeyboardContext.GENERAL
    }
}

private fun isMathematicalInputLabel(label: String): Boolean {
    val normalized = label.lowercase()
    return listOf("search", "note", "caption", "folder", "name", "annotation", "voice", "transcript").none { it in normalized }
}

@Composable
fun CompactMathField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardContext: MathKeyboardContext = MathKeyboardContext.GENERAL,
    onDone: (() -> Unit)? = null,
) {
    var editorValue by remember {
        mutableStateOf(StructuredMathCodec.fromParser(TextFieldValue(value, TextRange(value.length))))
    }
    var keyboardVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val systemKeyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(value) {
        if (value != StructuredMathCodec.toParser(editorValue).text) {
            editorValue = StructuredMathCodec.fromParser(TextFieldValue(value, TextRange(value.length)))
        }
    }
    val emitStructured: (TextFieldValue) -> Unit = {
        editorValue = it
        onValueChange(StructuredMathCodec.toParser(it).text)
    }
    MathKeyboardOnlyTextField(
        value = editorValue,
        onValueChange = emitStructured,
        label = label,
        placeholder = "Enter mathematics",
        singleLine = true,
        minLines = 1,
        accent = IntentMathPalette.Number,
        healthy = true,
        transformation = remember { IntentAwareMathVisualTransformation() },
        onFocusChange = {
            keyboardVisible = it
            if (it) systemKeyboard?.hide()
        },
        modifier = modifier,
    )
    if (keyboardVisible) {
        AdaptiveMathKeyboardPopup(
            value = editorValue,
            onValueChange = emitStructured,
            context = keyboardContext,
            onDone = {
                onDone?.invoke()
                keyboardVisible = false
                focusManager.clearFocus()
            },
            onDismiss = {
                keyboardVisible = false
                focusManager.clearFocus()
            },
        )
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
