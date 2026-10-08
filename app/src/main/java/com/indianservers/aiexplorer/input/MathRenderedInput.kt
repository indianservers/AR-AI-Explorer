package com.indianservers.aiexplorer.input

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Read-only rendered text with explicit source-coordinate editing and owned scrolling. */
@Composable
internal fun MathRenderedInput(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    label: String,
    placeholder: String,
    singleLine: Boolean,
    minLines: Int,
    minimumHeight: Dp,
    scale: Float,
    pan: Offset,
    transformation: VisualTransformation,
    onFocusChange: (Boolean) -> Unit,
    editorTestTag: String?,
) {
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var focused by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    val textScroll = rememberScrollState()
    val latest = rememberUpdatedState(value)
    val emit = rememberUpdatedState(onValueChange)
    val transformed = transformation.filter(AnnotatedString(value.text))
    val visual = rememberUpdatedState(transformed)
    val selection = value.selection
    val selectedText = remember(transformed, selection) {
        AnnotatedString.Builder(transformed.text).apply {
            if (!selection.collapsed) {
                val start = transformed.offsetMapping.originalToTransformed(selection.min)
                val end = transformed.offsetMapping.originalToTransformed(selection.max)
                if (start < end) addStyle(SpanStyle(background = IntentMathPalette.Command.copy(alpha = .4f)), start, end)
            }
        }.toAnnotatedString()
    }
    fun sourceAt(position: Offset): Int {
        val offset = layout?.getOffsetForPosition(position) ?: 0
        return mathSourceOffsetAtVisual(latest.value.text, visual.value, offset)
    }
    BoxWithConstraints(Modifier.fillMaxWidth().heightIn(min = minimumHeight)
        .graphicsLayer { scaleX = scale; scaleY = scale; translationX = pan.x; translationY = pan.y; transformOrigin = TransformOrigin.Center }) {
        val viewportWidth = maxWidth
        val viewportPixels = with(LocalDensity.current) { viewportWidth.toPx() }
        LaunchedEffect(value.selection, layout, focused) {
            if (focused && singleLine) layout?.let { textLayout ->
                val offset = if (value.text.isEmpty()) 0 else transformed.offsetMapping.originalToTransformed(value.selection.end)
                val caret = textLayout.getCursorRect(offset)
                val target = when {
                    caret.right > textScroll.value + viewportPixels -> (caret.right - viewportPixels + 12).toInt()
                    caret.left < textScroll.value -> (caret.left - 12).toInt()
                    else -> textScroll.value
                }
                textScroll.scrollTo(target.coerceIn(0, textScroll.maxValue))
            }
        }
        Box(if (singleLine) Modifier.horizontalScroll(textScroll) else Modifier) {
            Text(
                text = if (value.text.isEmpty()) AnnotatedString(placeholder) else selectedText,
                modifier = Modifier.widthIn(min = viewportWidth).heightIn(min = minimumHeight)
                    .focusRequester(focus)
                    .onFocusChanged { focused = it.isFocused; onFocusChange(it.isFocused) }
                    .focusable()
                    .testTag(editorTestTag ?: "math.rendered.editor")
                    .semantics {
                        contentDescription = "Editable $label. ${mathSlotLabel(StructuredMathEditing.modeAt(value.text, value.selection.end))}"
                        editableText = AnnotatedString(value.text)
                        textSelectionRange = value.selection
                        setSelection { start, end, _ ->
                            emit.value(latest.value.copy(selection = TextRange(start.coerceIn(0, latest.value.text.length), end.coerceIn(0, latest.value.text.length))))
                            true
                        }
                        onClick { focus.requestFocus(); true }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = { point ->
                                focus.requestFocus()
                                emit.value(latest.value.copy(selection = TextRange(sourceAt(point))))
                                tryAwaitRelease()
                            },
                            onDoubleTap = { point ->
                                focus.requestFocus()
                                val offset = sourceAt(point)
                                val slot = StructuredMathEditing.activeSlot(latest.value.text, offset)
                                emit.value(latest.value.copy(selection = if (slot?.isPlaceholder == true) TextRange(offset) else mathTermSelection(latest.value.text, offset)))
                            },
                        )
                    }
                    .pointerInput(Unit) {
                        var anchor = 0
                        detectDragGesturesAfterLongPress(
                            onDragStart = { point -> focus.requestFocus(); anchor = sourceAt(point); emit.value(latest.value.copy(selection = TextRange(anchor))) },
                            onDrag = { change, _ -> change.consume(); emit.value(latest.value.copy(selection = TextRange(anchor, sourceAt(change.position)))) },
                        )
                    }
                    .drawWithContent {
                        val textLayout = layout
                        if (focused && textLayout != null && value.text.isNotEmpty()) {
                            val slot = StructuredMathEditing.activeSlot(value.text, value.selection.end)
                            if (slot != null && selection.collapsed) {
                                val start = transformed.offsetMapping.originalToTransformed(slot.contentStart)
                                val end = if (slot.isPlaceholder) (start + 1).coerceAtMost(transformed.text.length)
                                    else transformed.offsetMapping.originalToTransformed(slot.contentEnd)
                                if (start < end) drawPath(textLayout.getPathForRange(start, end), IntentMathPalette.Number.copy(alpha = .22f))
                            }
                            if (selection.collapsed) matchingMathBrackets(value.text, selection.end)?.let { pair ->
                                listOf(pair.first, pair.second).forEach { original ->
                                    val offset = transformed.offsetMapping.originalToTransformed(original)
                                    if (transformed.text.getOrNull(offset) == value.text[original]) {
                                        drawPath(textLayout.getPathForRange(offset, offset + 1), IntentMathPalette.Constant.copy(alpha = .32f))
                                    }
                                }
                            }
                        }
                        drawContent()
                        if (focused && selection.collapsed && textLayout != null) {
                            val offset = if (value.text.isEmpty()) 0 else transformed.offsetMapping.originalToTransformed(selection.end)
                            val caret = textLayout.getCursorRect(offset)
                            drawLine(IntentMathPalette.Number, caret.topCenter, caret.bottomCenter, 2.dp.toPx())
                        }
                    },
                color = if (value.text.isEmpty()) IntentMathPalette.Muted else IntentMathPalette.Ink,
                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium),
                minLines = minLines.coerceAtLeast(1),
                onTextLayout = { layout = it },
                softWrap = !singleLine,
            )
        }
    }
}
