package com.indianservers.aiexplorer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.indianservers.aiexplorer.curriculum.production.CurriculumLearningScreen
import com.indianservers.aiexplorer.curriculum.production.CurriculumProductionBatchPlanner
import org.junit.Rule
import org.junit.Test

class CurriculumProductionUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun mathematicsChapterRenders() = verifyChapter(0)
    @Test fun physicsChapterRenders() = verifyChapter(1)
    @Test fun chemistryChapterRenders() = verifyChapter(2)
    @Test fun biologyChapterRenders() = verifyChapter(3)
    @Test fun realNumbersChapterRenders() = verifyChapter(4)
    @Test fun electricityChapterRenders() = verifyChapter(5)
    @Test fun acidsBasesSaltsChapterRenders() = verifyChapter(6)
    @Test fun controlCoordinationChapterRenders() = verifyChapter(7)

    @Test fun learnerCanOpenAnswersAndMarkLessonComplete() {
        var complete = false
        compose.setContent { CurriculumLearningScreen(CurriculumProductionBatchPlanner.chapters.first(), 0, false, true, {}, { complete = true }) }
        compose.onNodeWithText("Check answers").performScrollTo().performClick()
        compose.onNodeWithText("Mark lesson complete").performScrollTo().performClick()
        assert(complete)
    }

    private fun verifyChapter(index: Int) {
        val chapter = CurriculumProductionBatchPlanner.chapters[index]
        compose.setContent { CurriculumLearningScreen(chapter, 0, false, true, {}, {}) }
        compose.onNodeWithText(chapter.officialChapterTitle).assertIsDisplayed()
        compose.onNodeWithText("Learning objectives").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Prerequisites").performScrollTo().assertIsDisplayed()
    }
}
