package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.learning.KnowledgeLevel
import com.indianservers.aiexplorer.learning.KnowledgeTopic
import com.indianservers.aiexplorer.learning.FormulaCategory
import com.indianservers.aiexplorer.learning.MathKnowledgeCatalog
import com.indianservers.aiexplorer.learning.QuizEngine
import com.indianservers.aiexplorer.learning.QuizLevel
import com.indianservers.aiexplorer.learning.QuizSubject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MathKnowledgeTest {
    @Test
    fun searchFindsContentAcrossKnowledgeTypes() {
        val result = MathKnowledgeCatalog.search("Bayes")

        assertTrue(result.formulas.any { it.title == "Bayes theorem" })
        assertTrue(result.theorems.any { it.id == "bayes-rule" })
        assertTrue(result.dictionary.any { it.term == "Posterior" })
        assertTrue(result.mcqs.any { it.id == "mcq-bayes" })
        assertTrue(result.total >= 4)
    }

    @Test
    fun searchCanFilterByTopicAndLevel() {
        val result = MathKnowledgeCatalog.search("", topic = KnowledgeTopic.Algebra, level = KnowledgeLevel.PG)

        assertTrue(result.formulas.all { it.topic == KnowledgeTopic.Algebra && it.level == KnowledgeLevel.PG })
        assertTrue(result.theorems.all { it.topic == KnowledgeTopic.Algebra && it.level == KnowledgeLevel.PG })
        assertTrue(result.dictionary.all { it.topic == KnowledgeTopic.Algebra && it.level == KnowledgeLevel.PG })
        assertFalse(result.total == 0)
    }

    @Test
    fun mcqCheckingProducesFeedbackAndAdaptiveDifficulty() {
        val question = MathKnowledgeCatalog.mcqs.first { it.id == "mcq-product-rule" }
        val wrong = question.check(0)
        val right = question.check(question.answerIndex)

        assertFalse(wrong.correct)
        assertEquals(1, wrong.nextDifficulty)
        assertTrue(wrong.message.contains("Review"))
        assertTrue(right.correct)
        assertEquals(3, right.nextDifficulty)
    }

    @Test
    fun recommendationsPreferNearestDifficulty() {
        val recommended = MathKnowledgeCatalog.recommendedMcqs(KnowledgeTopic.Probability, KnowledgeLevel.UG, targetDifficulty = 4)

        assertEquals("mcq-bayes", recommended.first().id)
    }

    @Test
    fun sharedMcqBankCoversSubjectsAndLevelsWithAtLeastHundredQuestions() {
        assertTrue(MathKnowledgeCatalog.mcqs.size >= 100)
        QuizSubject.entries.forEach { subject ->
            assertTrue("$subject should have questions", MathKnowledgeCatalog.mcqs.any { it.subject == subject })
            QuizLevel.entries.forEach { level ->
                assertTrue("$subject $level should have questions", MathKnowledgeCatalog.mcqs.count { it.subject == subject && it.quizLevel == level } >= 6)
            }
        }
    }

    @Test
    fun quizEngineCreatesFifteenQuestionSessionAndScoresAnswers() {
        val session = QuizEngine.start(MathKnowledgeCatalog.mcqs, QuizSubject.Maths, QuizLevel.Basic)

        assertEquals(15, session.questions.size)
        assertEquals(0, session.score)
        val answered = QuizEngine.answer(session, session.currentQuestion!!.answerIndex)
        assertEquals(1, answered.answers.size)
        assertEquals(1, answered.score)
    }

    @Test
    fun formulaLibraryHasFifteenCategoriesWithAtLeastTwelveLatexFormulasEach() {
        assertEquals(15, FormulaCategory.entries.size)
        FormulaCategory.entries.forEach { category ->
            val formulas = MathKnowledgeCatalog.formulas.filter { it.category == category }
            assertTrue("${category.label} should have at least 17 formulas", formulas.size >= 17)
            formulas.forEach { formula ->
                assertFalse("${formula.title} should not use plain slash division", "/" in formula.expression)
                assertFalse("${formula.title} should not use unbraced power notation", Regex("\\^[A-Za-z0-9]").containsMatchIn(formula.expression))
                assertTrue("${formula.title} should use KaTeX-style notation", "\\" in formula.expression || "_{" in formula.expression || "^{" in formula.expression)
            }
        }
    }

    @Test
    fun formulasCanBeFilteredByCategory() {
        val result = MathKnowledgeCatalog.search("", formulaCategory = FormulaCategory.Trigonometry)

        assertTrue(result.formulas.size >= 12)
        assertTrue(result.formulas.all { it.category == FormulaCategory.Trigonometry })
        assertTrue(result.formulas.any { it.title == "Law of cosines" })
    }
}
