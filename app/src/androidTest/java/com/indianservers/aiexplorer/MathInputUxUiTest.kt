package com.indianservers.aiexplorer

import androidx.activity.compose.setContent
import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.view.MotionEvent
import android.view.inspector.WindowInspector
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.indianservers.aiexplorer.input.*
import com.indianservers.aiexplorer.persistence.DurableMathStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

class MathInputUxUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val value = mutableStateOf(TextFieldValue("abcdef", TextRange(3)))
    private val visible = mutableStateOf(true)
    private val size = mutableStateOf(MathKeyboardKeySize.COMPACT)
    private val width = mutableStateOf(320.dp)
    private val height = mutableStateOf(620.dp)
    private val scale = mutableStateOf(1f)
    private val context = mutableStateOf(MathKeyboardContext.GENERAL)
    private lateinit var store: DurableMathStore
    private var savedFavorites: List<String>? = null

    @Before fun setup() = runBlocking {
        store = DurableMathStore(InstrumentationRegistry.getInstrumentation().targetContext)
        savedFavorites = store.loadMathKeyboardFavorites()
        store.saveMathKeyboardFavorites(defaultMathFavoriteIds)
    }

    @After fun restore() = runBlocking {
        store.saveMathKeyboardFavorites(savedFavorites ?: defaultMathFavoriteIds)
        compose.runOnIdle { MathKeyboardPreferences.keySize = MathKeyboardKeySize.COMPACT }
    }

    private fun launch() {
        compose.runOnUiThread {
            compose.activity.setContent {
                MaterialTheme {
                    MathKeyboardPreferences.keySize = size.value
                    Column(Modifier.width(width.value).height(height.value)) {
                        MathRenderedInput(value.value, { value.value = it }, "Math", "Enter math", true, 1,
                            48.dp, scale.value, Offset.Zero, IntentAwareMathVisualTransformation(), {}, "math.ux.editor")
                        if (visible.value) AdaptiveMathKeyboard(value.value, { value.value = it }, context.value,
                            onDone = { visible.value = false }, onDismiss = { visible.value = false })
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        val file = File(checkNotNull(instrumentation.targetContext.externalCacheDir), name)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand("cp ${file.absolutePath} /sdcard/Download/$name")).use { it.readBytes() }
    }

    // A non-focusable Popup has its own screen origin; inject in screen coordinates.
    private fun touchPopup(node: SemanticsNodeInteraction, popup: Boolean = true) {
        compose.waitForIdle()
        val origin = IntArray(2)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            WindowInspector.getGlobalWindowViews().last { it.javaClass.name.contains("PopupLayout") == popup }.getLocationOnScreen(origin)
        }
        val point = node.fetchSemanticsNode().boundsInWindow.center + Offset(origin[0].toFloat(), origin[1].toFloat())
        val time = SystemClock.uptimeMillis()
        MotionEvent.obtain(time, time, MotionEvent.ACTION_DOWN, point.x, point.y, 0).also { instrumentation.sendPointerSync(it); it.recycle() }
        MotionEvent.obtain(time, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, point.x, point.y, 0).also { instrumentation.sendPointerSync(it); it.recycle() }
        instrumentation.waitForIdleSync()
        compose.waitForIdle()
    }

    @Test fun spaceTapAndDragAreDifferentAndCursorMovesTwelveDpPerStep() {
        launch()
        compose.onNodeWithTag("math.space").performTouchInput { click() }
        compose.runOnIdle { assertEquals(TextFieldValue("abc def", TextRange(4)), value.value) }
        compose.onNodeWithContentDescription("Undo last edit").performClick()
        compose.onNodeWithTag("math.space").performTouchInput {
            down(center)
            moveBy(Offset(24.dp.toPx(), 0f), delayMillis = 100)
            up()
        }
        compose.runOnIdle { assertEquals(TextFieldValue("abcdef", TextRange(5)), value.value) }
        compose.onNodeWithTag("math.space").performTouchInput {
            down(center)
            moveBy(Offset(-12.dp.toPx(), 0f), delayMillis = 100)
            up()
        }
        compose.runOnIdle { assertEquals(TextRange(4), value.value.selection) }
    }

    @Test fun heldBackspaceRepeatsAndOneUndoRestoresWholeHold() {
        value.value = TextFieldValue("12345678901234567890", TextRange(20))
        launch()
        compose.onNodeWithTag("math.backspace").performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(750)
        compose.waitForIdle()
        compose.onNodeWithTag("math.backspace").performTouchInput { up() }
        compose.runOnIdle { assertTrue(value.value.text.length in 14..17) }
        val after = value.value
        compose.mainClock.advanceTimeBy(600)
        compose.runOnIdle { assertEquals(after, value.value) }
        compose.onNodeWithContentDescription("Undo last edit").performClick()
        compose.runOnIdle { assertEquals(TextFieldValue("12345678901234567890", TextRange(20)), value.value) }
        compose.onNodeWithContentDescription("Redo last edit").performClick()
        compose.runOnIdle { assertEquals(after, value.value) }
    }

    @Test fun backspaceCancellationAndDismissalStopRepeat() {
        value.value = TextFieldValue("1234567890", TextRange(10))
        launch()
        compose.onNodeWithTag("math.backspace").performTouchInput { down(center); cancel() }
        compose.mainClock.advanceTimeBy(600)
        compose.runOnIdle { assertEquals("1234567890", value.value.text) }
        compose.onNodeWithTag("math.backspace").performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(450)
        compose.runOnIdle { visible.value = false }
        val after = value.value
        compose.mainClock.advanceTimeBy(600)
        compose.runOnIdle { assertEquals(after, value.value) }
    }

    @Test fun nextExitAndSelectionActionsAreUndoable() {
        value.value = TextFieldValue("()/()", TextRange(1))
        launch()
        compose.onNodeWithContentDescription("Next slot").performClick()
        compose.runOnIdle { assertEquals(TextRange(4), value.value.selection) }
        compose.onNodeWithContentDescription("Exit structure").performClick()
        compose.runOnIdle { assertEquals(TextRange(5), value.value.selection) }
        compose.onNodeWithContentDescription("Exit structure").assertIsNotEnabled()
        compose.runOnIdle { value.value = TextFieldValue("x+1", TextRange(3, 0)) }
        compose.onNodeWithTag("math.selection.SQUARE").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("(x+1)^(2)", value.value.text) }
        compose.onNodeWithContentDescription("Undo last edit").performClick()
        compose.runOnIdle { assertEquals(TextFieldValue("x+1", TextRange(3, 0)), value.value) }
    }

    @Test fun renderedDoubleTapAndLongDragSelectSourceTextIncludingZoom() {
        value.value = TextFieldValue("alpha+beta", TextRange(0))
        launch()
        compose.onNodeWithTag("math.ux.editor").performTouchInput { doubleClick(Offset(14.dp.toPx(), 12.dp.toPx())) }
        compose.runOnIdle { assertEquals(TextRange(0, 5), value.value.selection) }
        compose.onNodeWithTag("math.ux.editor").performTouchInput {
            down(Offset(4.dp.toPx(), 12.dp.toPx()))
            advanceEventTime(650)
            moveTo(Offset(75.dp.toPx(), 12.dp.toPx()), delayMillis = 100)
            up()
        }
        compose.runOnIdle { assertFalse(value.value.selection.collapsed) }
        compose.runOnIdle { scale.value = 1.5f; value.value = TextFieldValue("x^(12)+1", TextRange(0)) }
        compose.onNodeWithTag("math.ux.editor").performTouchInput { click(center) }
        compose.runOnIdle { assertTrue(value.value.selection.end in 0..value.value.text.length) }
    }

    @Test fun sharedEditorPinchZoomAndResetPreserveExpression() {
        value.value = TextFieldValue("x^(2)+sqrt(x+1)", TextRange(0))
        compose.runOnUiThread {
            compose.activity.setContent {
                MaterialTheme {
                    IntentAwareMathValueField(value.value, { value.value = it }, "Math", singleLine = true,
                        minLines = 1, showLegend = false, compactChrome = true, editorTestTag = "math.ux.pinch")
                }
            }
        }
        compose.onNodeWithTag("math.ux.pinch").performTouchInput {
            down(0, Offset(60.dp.toPx(), 10.dp.toPx()))
            down(1, Offset(180.dp.toPx(), 10.dp.toPx()))
            moveTo(0, Offset(30.dp.toPx(), 10.dp.toPx()), delayMillis = 100)
            moveTo(1, Offset(210.dp.toPx(), 10.dp.toPx()), delayMillis = 100)
            up(0); up(1)
        }
        compose.onNodeWithText("100%", substring = false).assertDoesNotExist()
        compose.runOnIdle { assertEquals("x^(2)+sqrt(x+1)", value.value.text) }
        compose.onNodeWithContentDescription("Reset zoom").performClick()
        compose.onNodeWithText("100%", substring = false).assertExists()
    }

    @Test fun ordinarySwipeScrollsWithoutSelectingOrChangingText() {
        val source = "x+".repeat(60) + "1"
        value.value = TextFieldValue(source, TextRange(0))
        launch()
        compose.onNodeWithTag("math.ux.editor").performTouchInput {
            down(Offset(200.dp.toPx(), 12.dp.toPx()))
            moveTo(Offset(40.dp.toPx(), 12.dp.toPx()), delayMillis = 100)
            up()
        }
        compose.runOnIdle { assertEquals(source, value.value.text); assertTrue(value.value.selection.collapsed) }
    }

    @Test fun placeholderTapsMapToEmptySlots() {
        value.value = TextFieldValue("()/()", TextRange(0))
        launch()
        compose.onNodeWithTag("math.ux.editor").performTouchInput { click(Offset(4.dp.toPx(), 10.dp.toPx())) }
        compose.runOnIdle { assertEquals(MathInputMode.NUMERATOR, StructuredMathEditing.modeAt(value.value.text, value.value.selection.end)) }
        compose.runOnIdle { assertFalse('□' in value.value.text) }
    }

    @Test fun sharedFieldPopupKeepsSelectionAndSurvivesFavoritesDialog() {
        value.value = TextFieldValue("sqrt(x)", TextRange(0))
        compose.runOnUiThread {
            compose.activity.setContent {
                MaterialTheme {
                    IntentAwareMathValueField(value.value, { value.value = it }, "Math", singleLine = true,
                        minLines = 1, showLegend = false, compactChrome = true, editorTestTag = "math.ux.field")
                }
            }
        }
        compose.onNodeWithTag("math.ux.field").performTouchInput { click(Offset(15.dp.toPx(), 10.dp.toPx())) }
        compose.onNodeWithTag("math.space").assertIsDisplayed()
        compose.runOnIdle { assertEquals(MathInputMode.RADICAND, StructuredMathEditing.modeAt(value.value.text, value.value.selection.end)) }
        touchPopup(compose.onNodeWithContentDescription("Keyboard display and clipboard tools").performScrollTo())
        touchPopup(compose.onNodeWithContentDescription("Manage favorite math keys").performScrollTo())
        compose.onNodeWithText("Done", substring = false).performClick()
        compose.onNodeWithTag("math.space").assertIsDisplayed()
        touchPopup(compose.onNodeWithContentDescription("Exit structure"))
        compose.runOnIdle { assertEquals(TextRange(7), value.value.selection) }
        compose.waitUntil(5000) { compose.activity.hasWindowFocus() }
        compose.runOnIdle {
            val clipboard = compose.activity.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("math", "π"))
        }
        touchPopup(compose.onNodeWithText("Paste", substring = false).performScrollTo())
        screenshot("math-ux-paste-integration.png")
        compose.waitUntil(5000) { compose.onAllNodesWithText("Insert converted").fetchSemanticsNodes().isNotEmpty() }
        touchPopup(compose.onNodeWithText("Insert converted"), popup = false)
        compose.runOnIdle { assertEquals(TextFieldValue("sqrt(x)pi", TextRange(9)), value.value) }
        compose.onNodeWithTag("math.space").assertIsDisplayed()
        screenshot("math-ux-editor.png")
    }

    @Test fun favoritesRoundTripAndEmptyRowStaysHidden() {
        launch()
        compose.onNodeWithContentDescription("Keyboard display and clipboard tools").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Manage favorite math keys").performScrollTo().performClick()
        compose.onNodeWithTag("math.favorite.down.variable.x").performClick()
        compose.waitUntil(5000) { runBlocking { store.loadMathKeyboardFavorites()?.first() == "constant.pi" } }
        defaultMathFavoriteIds.forEach { id -> compose.onNodeWithTag("math.favorite.remove.$id").performScrollTo().performClick() }
        compose.onNodeWithText("Done", substring = false).performClick()
        compose.waitUntil(5000) { runBlocking { store.loadMathKeyboardFavorites()?.isEmpty() == true } }
        compose.onNodeWithTag("math.favorites").assertDoesNotExist()
        compose.runOnIdle { visible.value = false }
        compose.runOnIdle { visible.value = true }
        compose.waitForIdle()
        compose.onNodeWithTag("math.favorites").assertDoesNotExist()
    }

    @Test fun controlsRemainVisibleInNarrowShortAndLargeKeyLayouts() {
        height.value = 280.dp
        launch()
        compose.onNodeWithTag("math.space").assertIsDisplayed()
        compose.onNodeWithTag("math.backspace").assertIsDisplayed()
        compose.onNodeWithContentDescription("Next slot").assertIsDisplayed()
        screenshot("math-ux-narrow.png")
        compose.onNodeWithTag("math.key.7", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        compose.runOnIdle { width.value = 600.dp; height.value = 360.dp; size.value = MathKeyboardKeySize.LARGE; context.value = MathKeyboardContext.GRAPH_3D }
        compose.onNodeWithTag("math.space").assertIsDisplayed()
        compose.onNodeWithContentDescription("Exit structure").assertIsDisplayed()
        compose.onNodeWithTag("math.key.7", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        screenshot("math-ux-large-short.png")
    }

    @Test fun favoritesSearchAddsKeysAndEnforcesEightKeyLimit() {
        launch()
        compose.onNodeWithContentDescription("Keyboard display and clipboard tools").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Manage favorite math keys").performScrollTo().performClick()
        listOf("tan", "cos", "sin").forEach { name ->
            compose.onNodeWithTag("math.favorite.search").performScrollTo().performTextClearance()
            compose.onNodeWithTag("math.favorite.search").performTextInput(name)
            val id = mathFavoriteCatalog.entries.first { it.value.insertion == "$name()" }.key
            compose.onNodeWithTag("math.favorite.add.$id").performScrollTo().performClick()
        }
        compose.waitUntil(5000) { runBlocking { store.loadMathKeyboardFavorites()?.size == 8 } }
        compose.onNodeWithTag("math.favorite.search").performScrollTo().performTextClearance()
        compose.onNodeWithTag("math.favorite.search").performTextInput("log")
        val logId = mathFavoriteCatalog.entries.first { it.value.insertion == "log()" }.key
        compose.onNodeWithTag("math.favorite.add.$logId").performScrollTo().assertIsNotEnabled()
    }
}
