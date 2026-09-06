package com.indianservers.aiexplorer

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.indianservers.aiexplorer.learnall.MathsLearnAllRepository
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BundledLessonsSeedTest {
    @Test
    fun plainJsonLessonsAreSeededAndReadable() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = MathsLearnAllRepository(context)
        val bundled = JSONArray(context.assets.open("maths_learn_all_lessons.json")
            .bufferedReader(Charsets.UTF_8).use { it.readText() })

        assertEquals(1987, bundled.length())
        assertEquals(bundled.length(), repository.seedBundledLessons())
        assertEquals(bundled.length(), repository.stats().lessonCount)
        assertEquals(29, repository.concepts().size)
        assertEquals(20, repository.classes().size)
        for (index in 0 until bundled.length()) {
            val expected = bundled.getJSONObject(index)
            val lesson = requireNotNull(repository.lesson(expected.getString("id")))
            assertEquals(expected.getString("subtopic"), lesson.subtopic)
            assertTrue("Unreadable lesson: ${lesson.id}", lesson.detailedExplanation.isNotBlank())
        }
        // Reopening the bundled catalog must not duplicate or lose lessons.
        assertEquals(bundled.length(), repository.seedBundledLessons())
        assertEquals(bundled.length(), repository.stats().lessonCount)
    }
}
