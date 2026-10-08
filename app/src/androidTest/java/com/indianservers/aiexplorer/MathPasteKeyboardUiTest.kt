package com.indianservers.aiexplorer

import androidx.activity.compose.setContent
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.indianservers.aiexplorer.input.AdaptiveMathKeyboard
import com.indianservers.aiexplorer.input.MathKeyboardContext
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File

@Suppress("DEPRECATION")
class MathPasteKeyboardUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val value = mutableStateOf(TextFieldValue("a+old+b", TextRange(2, 5)))
    private val visible = mutableStateOf(true)
    private lateinit var clipboard: ClipboardManager

    private fun screenshot(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        checkNotNull(bitmap)
        File(instrumentation.targetContext.externalCacheDir, name).outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
    }

    private fun launch(text: String) {
        compose.runOnUiThread {
            compose.activity.setContent {
                MaterialTheme {
                    clipboard = LocalClipboardManager.current
                    if (visible.value) Box(Modifier.width(320.dp)) {
                        AdaptiveMathKeyboard(value.value, { value.value = it }, MathKeyboardContext.GENERAL,
                            onDone = { visible.value = false }, onDismiss = { visible.value = false })
                    }
                }
            }
        }
        compose.runOnIdle { clipboard.setText(AnnotatedString(text)) }
        compose.onNodeWithContentDescription("Keyboard display and clipboard tools").performClick()
        compose.onNodeWithText("Paste", substring = false).performClick()
    }

    @Test fun plainPasteIsImmediateAndUndoable() {
        launch("x+1")
        compose.onNodeWithText("Preview math paste").assertDoesNotExist()
        compose.runOnIdle { assertEquals(TextFieldValue("a+x+1+b", TextRange(5)), value.value) }
        compose.onNodeWithContentDescription("Undo last edit").performClick()
        compose.runOnIdle { assertEquals(TextFieldValue("a+old+b", TextRange(2, 5)), value.value) }
    }

    @Test fun emptyClipboardIsNoOpAndPlainPasteAtCursorKeepsSurroundingText() {
        value.value = TextFieldValue("ab", TextRange(1))
        launch("")
        compose.runOnIdle { assertEquals(TextFieldValue("ab", TextRange(1)), value.value) }
        compose.runOnIdle { clipboard.setText(AnnotatedString("x+1")) }
        compose.onNodeWithText("Paste", substring = false).performClick()
        compose.runOnIdle { assertEquals(TextFieldValue("ax+1b", TextRange(4)), value.value) }
    }

    @Test fun convertedPastePreservesSelectionAndSupportsUndoRedo() {
        launch("x² + π")
        compose.onNodeWithTag("math.paste.original").assertTextEquals("x² + π")
        compose.onNodeWithTag("math.paste.converted").assertTextEquals("x^(2) + pi")
        compose.runOnIdle { assertEquals("a+old+b", value.value.text) }
        screenshot("math-paste-preview.png")
        compose.onNodeWithText("Insert converted").performClick()
        val pasted = TextFieldValue("a+x^(2) + pi+b", TextRange(12))
        compose.runOnIdle { assertEquals(pasted, value.value) }
        compose.onNodeWithContentDescription("Undo last edit").performClick()
        compose.runOnIdle { assertEquals(TextFieldValue("a+old+b", TextRange(2, 5)), value.value) }
        compose.onNodeWithContentDescription("Redo last edit").performClick()
        compose.runOnIdle { assertEquals(pasted, value.value) }
    }

    @Test fun unsupportedLatexAllowsOriginalOnly() {
        launch("\\unknown{x}")
        compose.onNodeWithText("Insert converted").assertIsNotEnabled()
        compose.onNodeWithText("Paste original").performClick()
        compose.runOnIdle { assertEquals("a+\\unknown{x}+b", value.value.text) }
    }

    @Test fun cancelAndExternalEditLeaveNoPendingInsertion() {
        launch("x²")
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle { assertEquals("a+old+b", value.value.text) }
        compose.onNodeWithText("Paste", substring = false).performClick()
        compose.runOnIdle { value.value = TextFieldValue("changed", TextRange(7)) }
        compose.onNodeWithText("Preview math paste").assertDoesNotExist()
        compose.runOnIdle { assertEquals("changed", value.value.text) }
    }

    @Test fun longPreviewScrollsOnNarrowKeyboardAndClosingCancels() {
        launch("x² + π\n".repeat(100))
        compose.onNodeWithTag("math.paste.converted").performScrollTo()
        screenshot("math-paste-long-preview.png")
        compose.onNodeWithText("Cancel").assertIsDisplayed().performClick()
        compose.onNodeWithText("Paste", substring = false).performClick()
        compose.runOnIdle { visible.value = false }
        compose.onNodeWithText("Preview math paste").assertDoesNotExist()
        compose.runOnIdle { assertEquals("a+old+b", value.value.text) }
    }
}
