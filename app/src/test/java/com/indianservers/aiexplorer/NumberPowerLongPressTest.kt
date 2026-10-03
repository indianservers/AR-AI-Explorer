package com.indianservers.aiexplorer

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.indianservers.aiexplorer.input.*
import org.junit.Assert.*
import org.junit.Test

class NumberPowerLongPressTest {
    @Test fun everyDigitHasDirectPowerLongPress() {
        for (digit in '0'..'9') {
            val key = numberPowerLongPressKey(MathKey(digit.toString()))!!
            assertEquals(MathKeyAction.INSERT_POWER_DIGIT, key.action)
            val result = MathTextEditing.insertPowerDigit(TextFieldValue("y", TextRange(1)), key.insertion)
            assertEquals("y^($digit)", result.text)
            assertEquals(TextRange(result.text.length), result.selection)
        }
        assertNull(numberPowerLongPressKey(MathKey("+")))
        assertNull(numberPowerLongPressKey(MathKey("√", "sqrt()")))
    }

    @Test fun selectedExpressionBecomesTheBase() {
        val result = MathTextEditing.insertPowerDigit(TextFieldValue("x+1", TextRange(0, 3)), "2")
        assertEquals("(x+1)^(2)", result.text)
    }

    @Test fun existingExponentReceivesDigitWithoutNestedPower() {
        val result = MathTextEditing.insertPowerDigit(TextFieldValue("x^(1)", TextRange(4)), "2")
        assertEquals("x^(12)", result.text)
    }
}
