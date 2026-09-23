package com.indianservers.aiexplorer

import org.junit.Assert.assertEquals
import org.junit.Test

class ActionIconMappingTest {
    @Test
    fun decreaseControlsNeverUseTheDeleteIcon() {
        assertEquals("subtract", smartIconKey("-", "Size -"))
        assertEquals("subtract", smartIconKey("subtract", "Decrease size"))
        // TransparentIcon resolves the selected icon again while drawing it.
        assertEquals("subtract", smartIconKey("subtract", "subtract"))
    }

    @Test
    fun deletionRetainsItsTrashIconIncludingLegacyMinusAliases() {
        assertEquals("delete", smartIconKey("-", "Delete selected object"))
        assertEquals("delete", smartIconKey("×", "Delete"))
        assertEquals("delete", smartIconKey("delete", "delete"))
    }
}
