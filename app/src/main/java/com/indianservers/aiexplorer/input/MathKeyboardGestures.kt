package com.indianservers.aiexplorer.input

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.indianservers.aiexplorer.adaptive.LocalAdaptiveDeviceProfile
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

internal fun backspaceRepeatInterval(elapsedMillis: Long): Long = if (elapsedMillis >= 2_000) 60 else 100

@Composable
internal fun MathSpaceKey(modifier: Modifier, onSpace: () -> Unit, onMove: (Int) -> Unit) {
    val space = rememberUpdatedState(onSpace)
    val move = rememberUpdatedState(onMove)
    MathGestureKey("Space", "Space. Drag horizontally to move cursor", "math.space", modifier.pointerInput(Unit) {
        val step = 12.dp.toPx()
        awaitEachGesture {
            val down = awaitFirstDown()
            var remainder = 0f
            var dragged = false
            var cancelled = false
            do {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (event.changes.count { it.pressed } > 1 || abs(change.position.y - down.position.y) > viewConfiguration.touchSlop) cancelled = true
                if (!cancelled) {
                    remainder += change.positionChange().x
                    val steps = (remainder / step).toInt()
                    if (steps != 0) {
                        dragged = true
                        move.value(steps)
                        remainder -= steps * step
                    }
                    if (dragged) change.consume()
                }
                if (!change.pressed) {
                    if (!dragged && !cancelled && !change.isConsumed) space.value()
                    break
                }
                if (change.isConsumed && !dragged) cancelled = true
            } while (true)
        }
    }, onClick = onSpace)
}

@Composable
internal fun MathBackspaceKey(modifier: Modifier, onDelete: (Boolean) -> Boolean, onTap: () -> Unit) {
    val delete = rememberUpdatedState(onDelete)
    val tap = rememberUpdatedState(onTap)
    MathGestureKey("⌫", "Backspace", "math.backspace", modifier.pointerInput(Unit) {
        coroutineScope {
            val scope = this
            awaitEachGesture {
                val down = awaitFirstDown()
                var repeated = false
                var cancelled = false
                val repeat = scope.launch {
                    delay(400)
                    var elapsed = 400L
                    var first = true
                    while (true) {
                        repeated = true
                        if (!delete.value(first)) break
                        first = false
                        val interval = backspaceRepeatInterval(elapsed)
                        delay(interval)
                        elapsed += interval
                    }
                }
                try {
                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        val outside = change.position.x !in 0f..size.width.toFloat() || change.position.y !in 0f..size.height.toFloat()
                        if (outside || change.isConsumed || event.changes.count { it.pressed } > 1) { cancelled = true; break }
                        change.consume()
                        if (!change.pressed) {
                            if (!repeated && !cancelled) tap.value()
                            break
                        }
                    } while (true)
                } finally { repeat.cancel() }
            }
        }
    }, onClick = onTap)
}

@Composable
private fun MathGestureKey(label: String, description: String, tag: String, modifier: Modifier, onClick: () -> Unit) {
    val appearance = MathKeyboardPreferences.keySize
    Box(
        modifier.height(mathKeyboardActionHeight(appearance, LocalAdaptiveDeviceProfile.current.minimumTargetSize, true))
            .testTag(tag)
            .background(SmartInputStyle.key(IntentMathPalette.Variable, true, MathKeyboardPreferences.highContrast), RoundedCornerShape(7.dp))
            .border(if (MathKeyboardPreferences.highContrast) 2.dp else 1.dp, if (MathKeyboardPreferences.highContrast) Color.White else IntentMathPalette.Variable, RoundedCornerShape(7.dp))
            .semantics { contentDescription = description; role = Role.Button; onClick { onClick(); true } },
        contentAlignment = Alignment.Center,
    ) { Text(label, color = IntentMathPalette.Ink, fontSize = (14 * appearance.fontScale).sp) }
}
