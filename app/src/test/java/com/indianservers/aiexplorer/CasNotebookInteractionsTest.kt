package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.*
import org.junit.Assert.*
import org.junit.Test

class CasNotebookInteractionsTest {
    private val engine = CasNotebookInteractionEngine()

    @Test fun naturalLanguageAndAmbiguityProduceSelectableInterpretations() {
        val derivative = CasInterpretationResolver.options("what is the derivative of x^3 with respect to x")
        assertEquals("derivative", derivative.first().operation)
        assertEquals("x^3", derivative.first().expression)
        assertTrue(CasInterpretationResolver.options("sin x^2").map { it.expression }.containsAll(listOf("sin(x)^2", "sin(x^2)")))
        assertEquals(3, CasInterpretationResolver.options("2/3x").size)
    }

    @Test fun voiceHandwritingAndPhotoCandidatesRemainReviewable() {
        val capture = CasCaptureNormalizer.normalize(CasInputModality.Voice, listOf("x squared plus 2" to .94, "x squared plus z" to .52))
        assertEquals("x ^2 + 2", capture.recognizedText)
        assertEquals(1, capture.alternatives.size)
        assertThrows(IllegalArgumentException::class.java) { CasCaptureNormalizer.normalize(CasInputModality.Keyboard, listOf("x" to 1.0)) }
    }

    @Test fun csvAndTsvImportsCreateScopedMatrixVariables() {
        val data = CasStructuredImportEngine.csv("scores.csv", "x,y\n1,2\n3,4")
        assertEquals("[[1,2],[3,4]]", data.matrixExpression)
        assertEquals(listOf("x", "y"), data.headers)
        val session = engine.import(CasNotebookSession(), data)
        assertEquals(data.matrixExpression, session.visibleVariables()["scores"])
    }

    @Test fun structuredFilesSupportJsonWhitespaceAndMatrixMarketWithPreviews() {
        val json = CasStructuredImportEngine.structured("matrix.json", "[[1,2],[3,4]]", "application/json")
        assertEquals("JSON matrix", json.format)
        assertEquals("[[1,2],[3,4]]", json.data.matrixExpression)
        val text = CasStructuredImportEngine.structured("matrix.txt", "1 2 3\n4 5 6", "text/plain")
        assertEquals(2, text.data.rows)
        assertEquals(3, text.data.columns)
        val market = CasStructuredImportEngine.structured("sparse.mtx", "%%MatrixMarket matrix coordinate real general\n2 3 2\n1 1 5\n2 3 7")
        assertEquals("[[5,0,0],[0,0,7]]", market.data.matrixExpression)
        assertEquals(2, market.preview.size)
    }

    @Test fun unitCheckingRejectsInvalidAdditiveDimensions() {
        assertFalse(CasDimensionalAnalyzer.analyze("3 m + 2 s").valid)
        assertTrue(CasDimensionalAnalyzer.analyze("3 m + 20 cm").valid)
        assertEquals("mass", CasDimensionalAnalyzer.analyze("4 kg").dimension)
    }

    @Test fun evaluationBuildsInteractivePodsHintsWarningsAndScopedRows() {
        var session = CasNotebookSession(assumptions = MathAssumptionSet().with(VariableAssumption("x", nonNegative = true)))
        session = engine.define(session, CasScopedVariable("a", "2", CasVariableScope.Notebook))
        val option = CasInterpretationOption("one", "simplify", "sqrt(x^2)+a", "simplify")
        session = engine.evaluate(session, option.expression, option, CasSolutionMethod.Direct)
        val row = session.rows.single()
        assertTrue(row.result.exact.contains("2"))
        assertTrue(row.pods.map { it.kind }.containsAll(listOf(CasPodKind.Exact, CasPodKind.Domain, CasPodKind.Verification, CasPodKind.Plot)))
        assertEquals(row.result.steps.size, row.hints.size)
        assertTrue(row.assumptionWarnings.isNotEmpty())
    }

    @Test fun subexpressionsSupportTargetedActionsAndRowTransfer() {
        val targets = engine.subexpressions("(x^2+2*x)+1")
        val inner = targets.first { it.text == "x^2+2*x" }
        val changed = engine.applyTarget("(x^2+2*x)+1", inner, CasTargetAction.Differentiate)
        assertTrue(changed.contains("2 + 2*x") || changed.contains("2*x + 2"))
        var session = engine.evaluate(CasNotebookSession(), "x+1", CasInterpretationOption("a", "literal", "x+1", "simplify"), CasSolutionMethod.Direct, "r1")
        assertEquals("x+1", engine.transfer(session, CasExpressionTransfer("r1", "x+1"), null))
        session = engine.pin(session, "r1")
        session = engine.compare(session, "r1")
        session = engine.annotate(session, "r1", "Baseline")
        assertTrue(session.rows.single().pinned)
        assertEquals(setOf("r1"), session.compareRowIds)
        assertEquals("Baseline", session.rows.single().annotation)
    }

    @Test fun everyResultExportsPortableAndStructuredFormats() {
        val session = engine.evaluate(CasNotebookSession(), "(x+1)^2", CasInterpretationOption("a", "expand", "(x+1)^2", "expand"), CasSolutionMethod.Direct)
        val row = session.rows.single()
        CasExportFormat.entries.forEach { assertTrue(CasExportEngine.export(row, it).isNotBlank()) }
        assertTrue(CasExportEngine.export(row, CasExportFormat.MathMl).startsWith("<math"))
        assertTrue(CasExportEngine.export(row, CasExportFormat.Svg).contains("<svg"))
        assertTrue(CasExportEngine.export(row, CasExportFormat.Json).contains("\"exact\""))
    }

    @Test fun everyIntermediateStepHasProgressiveHintRuleWorkedLineAndCheck() {
        val row = engine.evaluate(CasNotebookSession(), "(x+1)^2", CasInterpretationOption("a", "expand", "(x+1)^2", "expand"), CasSolutionMethod.Direct).rows.single()
        row.result.steps.forEachIndexed { index, step ->
            val disclosures = CasStepDisclosureEngine.disclosures(step, index, row.result.steps.size, row.result.assumptions)
            assertEquals(CasStepDisclosureLevel.entries.toList(), disclosures.map { it.level })
            assertFalse(disclosures.first().revealsIntermediate)
            assertTrue(disclosures.single { it.level == CasStepDisclosureLevel.Worked }.revealsIntermediate)
            assertEquals(step.expression, disclosures.single { it.level == CasStepDisclosureLevel.Worked }.content)
        }
    }
}
