package com.indianservers.aiexplorer

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.indianservers.aiexplorer.learnall.MathsLearnAllRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BundledLessonsSeedTest {
    @Test
    fun emptyLessonPackPreservesConceptCatalog() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = MathsLearnAllRepository(context)

        assertEquals(0, repository.seedBundledLessons())
        assertEquals(0, repository.stats().lessonCount)
        assertEquals(29, repository.concepts().size)
        assertTrue(repository.classes().isEmpty())
    }
}
