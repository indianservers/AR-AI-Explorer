package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.accessibility.GraphSonificationPcm
import com.indianservers.aiexplorer.core.GraphAudioNote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GraphSonificationPcmTest {
    @Test fun stereoPcmIsDeterministicNonSilentAndRespectsPan() {
        val notes = listOf(GraphAudioNote(0.0, 69.0, -1.0, false, "left"), GraphAudioNote(1.0, 81.0, 1.0, true, "right"))
        val first = GraphSonificationPcm.render(notes, sampleRate = 1_000, durationSeconds = 1.0)
        val second = GraphSonificationPcm.render(notes, sampleRate = 1_000, durationSeconds = 1.0)

        assertEquals(2_000, first.size)
        assertTrue(first.any { it.toInt() != 0 })
        assertTrue(first.contentEquals(second))
        assertTrue(first.filterIndexed { index, _ -> index % 2 == 0 }.any { it.toInt() != 0 })
        assertTrue(first.filterIndexed { index, _ -> index % 2 == 1 }.any { it.toInt() != 0 })
    }
}
