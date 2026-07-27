package com.indianservers.aiexplorer.smartboard

import com.indianservers.aiexplorer.smartboard.models.SmartBoardInputMode
import com.indianservers.aiexplorer.smartboard.models.SmartBoardPreferences
import com.indianservers.aiexplorer.smartboard.models.SmartBoardRecognitionMode
import com.indianservers.aiexplorer.smartboard.models.SmartBoardTool
import com.indianservers.aiexplorer.smartboard.models.afterSelecting
import org.junit.Assert.assertEquals
import org.junit.Test

class SmartBoardInputPolicyTest {
    @Test
    fun boardDefaultsToTouchAndStylusDrawing() {
        assertEquals(SmartBoardInputMode.DRAW_WITH_FINGER, SmartBoardPreferences().inputMode)
        assertEquals(SmartBoardRecognitionMode.SUGGEST_AFTER_PAUSE, SmartBoardPreferences().recognitionMode)
    }

    @Test
    fun selectingDirectToolMakesFingerInputActOnTheBoard() {
        listOf(
            SmartBoardTool.PEN,
            SmartBoardTool.PENCIL,
            SmartBoardTool.HIGHLIGHTER,
            SmartBoardTool.ERASER,
            SmartBoardTool.LASSO,
            SmartBoardTool.RECTANGLE_SELECT,
        ).forEach { tool ->
            assertEquals(
                SmartBoardInputMode.DRAW_WITH_FINGER,
                SmartBoardInputMode.FINGER_PANS.afterSelecting(tool),
            )
        }
    }

    @Test
    fun explicitStylusOnlyModeAndPanToolArePreserved() {
        assertEquals(
            SmartBoardInputMode.STYLUS_ONLY,
            SmartBoardInputMode.STYLUS_ONLY.afterSelecting(SmartBoardTool.PEN),
        )
        assertEquals(
            SmartBoardInputMode.FINGER_PANS,
            SmartBoardInputMode.FINGER_PANS.afterSelecting(SmartBoardTool.PAN),
        )
    }
}
