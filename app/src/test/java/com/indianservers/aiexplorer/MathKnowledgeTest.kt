package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.learning.KnowledgeLevel
import com.indianservers.aiexplorer.learning.KnowledgeTopic
import com.indianservers.aiexplorer.learning.FormulaCategory
import com.indianservers.aiexplorer.learning.DictionaryClassBand
import com.indianservers.aiexplorer.learning.DictionaryDifficulty
import com.indianservers.aiexplorer.learning.MathDictionaryCatalog
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
    fun visualDictionarySupportsAlphabetClassDifficultyAndTeachingExamples() {
        val result = MathDictionaryCatalog.search(
            query = "",
            topic = null,
            level = null,
            initial = 'D',
            classBand = DictionaryClassBand.CLASS_11_12,
            difficulty = DictionaryDifficulty.ADVANCED,
        )

        assertEquals(listOf("Derivative"), result.map { it.term })
        assertTrue(result.single().example.isNotBlank())
        assertTrue(result.single().nonExample.isNotBlank())
    }

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
    fun formulaLibraryHasMainCategoriesWithTaggedLatexFormulas() {
        assertTrue(FormulaCategory.entries.size in 10..14)
        assertTrue("Expanded formula catalog should contain at least 399 formulas", MathKnowledgeCatalog.formulas.size >= 399)
        assertEquals(
            "Formula identifiers must remain unique",
            MathKnowledgeCatalog.formulas.size,
            MathKnowledgeCatalog.formulas.map { it.id }.distinct().size,
        )
        FormulaCategory.entries.forEach { category ->
            val formulas = MathKnowledgeCatalog.formulas.filter { it.category == category }
            assertTrue("${category.label} should have at least 29 formulas", formulas.size >= 29)
            category.subcategories.forEach { subcategory ->
                assertTrue(
                    "${category.label} / $subcategory should contain formulas",
                    formulas.any { it.subcategory == subcategory },
                )
            }
            formulas.forEach { formula ->
                assertTrue(
                    "${formula.title} has an undeclared subcategory ${formula.subcategory}",
                    formula.subcategory in category.subcategories,
                )
                assertFalse("${formula.title} should not use plain slash division", "/" in formula.expression)
                assertFalse("${formula.title} should not use unbraced power notation", Regex("\\^[A-Za-z0-9]").containsMatchIn(formula.expression))
                assertTrue("${formula.title} should use KaTeX-style notation", "\\" in formula.expression || "_{" in formula.expression || "^{" in formula.expression)
                assertTrue("${formula.title} should have a basic introduction", formula.introduction.length >= 80)
                assertTrue("${formula.title} introduction should explain use", formula.introduction.contains("used in", ignoreCase = true))
                assertTrue("${formula.title} introduction should explain when to use it", formula.introduction.contains("Use it when", ignoreCase = true))
            }
        }
        assertTrue(MathKnowledgeCatalog.formulas.any { "area" in it.tags })
        assertTrue(MathKnowledgeCatalog.formulas.any { "perimeter" in it.tags })
        assertTrue(MathKnowledgeCatalog.formulas.any { "volume" in it.tags })
        assertTrue(MathKnowledgeCatalog.formulas.any { "angle" in it.tags })
        assertTrue(MathKnowledgeCatalog.search("navigation").formulas.any { it.category == FormulaCategory.Trigonometry })
        MathKnowledgeCatalog.formulas.forEach { formula ->
            val display = displayLatexFormula(formula.expression)
            assertFalse("${formula.title} display should not use raw slash division", "/" in display)
            assertFalse("${formula.title} display should not use raw caret powers", "^" in display)
        }
    }

    @Test
    fun formulaLibraryCoversEssentialSchoolToPgFormulaGaps() {
        val required = listOf(
            "Percentage change",
            "Simple interest",
            "Compound amount annual",
            "Quadratic formula",
            "Arithmetic progression sum",
            "Triangle area base height",
            "Heron formula",
            "Circle circumference",
            "Sector area",
            "Slope formula",
            "Section formula",
            "Coordinate triangle area",
            "Pythagorean identity sine cosine",
            "Law of sines",
            "Law of cosines",
            "Product rule",
            "Chain rule",
            "Integration by parts",
            "Two by two inverse",
            "Eigenvalue equation",
            "Singular value decomposition",
            "Conditional probability",
            "Binomial probability",
            "Normal density",
            "Arithmetic mean",
            "One sample t statistic",
            "HCF LCM product",
            "Fermat little theorem",
            "Gradient descent update",
            "KKT stationarity",
        )
        val titles = MathKnowledgeCatalog.formulas.map { it.title }.toSet()
        required.forEach { title -> assertTrue("Missing essential formula: $title", title in titles) }
    }

    @Test
    fun theoremLibraryCoversClassSixThroughPgAcrossExpandedCategories() {
        assertEquals(20, com.indianservers.aiexplorer.learning.theoremCategories.size)
        assertTrue(MathKnowledgeCatalog.theorems.size >= 100)
        com.indianservers.aiexplorer.learning.theoremCategories.forEach { category ->
            assertTrue("$category should have at least five important theorems", MathKnowledgeCatalog.theorems.count { it.category == category } >= 5)
        }
        com.indianservers.aiexplorer.learning.TheoremBand.entries.forEach { band ->
            assertTrue("$band should be represented", MathKnowledgeCatalog.theorems.any { it.band == band })
        }
        assertEquals(MathKnowledgeCatalog.theorems.size, MathKnowledgeCatalog.theorems.map { it.id }.distinct().size)
        assertTrue(MathKnowledgeCatalog.theorems.all { it.tags.isNotEmpty() })
        assertTrue(MathKnowledgeCatalog.theorems.all { it.proofSketch.isNotEmpty() })
        assertTrue(MathKnowledgeCatalog.theorems.all { it.conditions.isNotEmpty() })
        assertTrue(MathKnowledgeCatalog.search("area").theorems.isNotEmpty())
        listOf("pythagoras", "bayes-rule", "spectral", "ode-existence", "residue", "max-flow-min-cut", "kkt").forEach { id ->
            assertTrue("$id should be in the theorem library", MathKnowledgeCatalog.theorems.any { it.id == id })
        }
    }

    @Test
    fun formulasCanBeFilteredByCategory() {
        val result = MathKnowledgeCatalog.search("", formulaCategory = FormulaCategory.Trigonometry)

        assertTrue(result.formulas.size >= 12)
        assertTrue(result.formulas.all { it.category == FormulaCategory.Trigonometry })
        assertTrue(result.formulas.any { it.title == "Law of cosines" })
        assertFalse(result.formulas.any { it.title == "Sector area" })
        val geometry = MathKnowledgeCatalog.search("", formulaCategory = FormulaCategory.GeometryMensuration)
        assertTrue(geometry.formulas.any { it.title == "Sector area" })
        assertFalse(geometry.formulas.any { it.title == "Law of cosines" })
    }
}
