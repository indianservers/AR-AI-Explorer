package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.ProfessionalGraphTableEngine
import com.indianservers.aiexplorer.core.Vec2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DynamicDataTablesTest {
    private val engine = ProfessionalGraphTableEngine()

    @Test
    fun generatesAscendingAndDescendingFunctionInputs() {
        assertEquals(listOf(-1.0, -.5, 0.0, .5, 1.0), engine.functionInputs(-1.0, 1.0, .5))
        assertEquals(listOf(2.0, 1.0, 0.0, -1.0), engine.functionInputs(2.0, -1.0, -1.0))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsRangesThatExceedTheVisibleTableLimit() {
        engine.functionInputs(0.0, 1_000.0, 1.0, maximumRows = 100)
    }

    @Test
    fun importsQuotedCsvAndPlotsOnlyCompleteNumericRows() {
        val table = engine.paste(
            """
            time,"measured value",note
            0,2.5,"start"
            1,3.5,"middle"
            2,,"missing"
            3,8.5,"end"
            """.trimIndent(),
        )

        assertEquals(listOf("time", "measured_value", "note"), table.columns.map { it.name })
        assertNull(table.columns[1].values[2])
        assertEquals(
            listOf(Vec2(0.0, 2.5), Vec2(1.0, 3.5), Vec2(3.0, 8.5)),
            engine.series(table, "time", "measured_value"),
        )
    }

    @Test
    fun supportsHeaderlessTsvData() {
        val table = engine.paste("1\t10\n2\t20", hasHeader = false)

        assertEquals(listOf("c1", "c2"), table.columns.map { it.name })
        assertEquals(listOf(Vec2(1.0, 10.0), Vec2(2.0, 20.0)), engine.series(table, "c1", "c2"))
    }
}
