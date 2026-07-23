package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.EquivalenceStatus
import com.indianservers.aiexplorer.core.KernelRegressionCorpus
import com.indianservers.aiexplorer.core.MathAssumptionSet
import com.indianservers.aiexplorer.core.MathNumberDomain
import com.indianservers.aiexplorer.core.TrustedMathKernel
import com.indianservers.aiexplorer.core.VariableAssumption
import com.indianservers.aiexplorer.core.FunctionDefinition
import com.indianservers.aiexplorer.workspace.UniversalDocumentRecovery
import com.indianservers.aiexplorer.workspace.UniversalMathDocument
import com.indianservers.aiexplorer.workspace.UniversalMathDocumentCodec
import com.indianservers.aiexplorer.workspace.UniversalMathDocumentEngine
import com.indianservers.aiexplorer.workspace.UniversalMathKind
import com.indianservers.aiexplorer.workspace.UniversalMathObject
import com.indianservers.aiexplorer.workspace.UniversalMathPayload
import com.indianservers.aiexplorer.workspace.UniversalMathObjectFactory
import com.indianservers.aiexplorer.workspace.UniversalMutationResult
import com.indianservers.aiexplorer.workspace.UniversalWorkspaceBridge
import com.indianservers.aiexplorer.workspace.WorkspaceJson
import com.indianservers.aiexplorer.workspace.WorkspaceState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase1TrustedKernelTest {
    private val kernel = TrustedMathKernel()

    @Test fun workspaceBecomesOneTypedDocumentAcrossAllCurrentViews() {
        val document = UniversalWorkspaceBridge.fromWorkspace(WorkspaceState())
        val kinds = document.objects.values.map { it.kind }.toSet()
        assertTrue(kinds.containsAll(setOf(
            UniversalMathKind.Function, UniversalMathKind.Point2D, UniversalMathKind.Segment,
            UniversalMathKind.Solid, UniversalMathKind.Vector, UniversalMathKind.Surface, UniversalMathKind.SpatialScene,
        )))
        val validation = UniversalMathDocumentEngine().validate(document)
        assertTrue(validation.diagnostics.joinToString(), validation.valid)
        assertEquals(document.objects.size, validation.topologicalOrder.size)
    }

    @Test fun typedFactoriesCoverCalculatorMatrixDataProbabilityUnitsAndFutureViews() {
        val objects = listOf(
            UniversalMathObjectFactory.scalar("s", "one third", com.indianservers.aiexplorer.core.ExactRational.parse("1/3")),
            UniversalMathObjectFactory.symbolic("eq", UniversalMathKind.Equation, "equation", "x+1"),
            UniversalMathObjectFactory.point3D("p3", "P", 1.0, 2.0, 3.0),
            UniversalMathObjectFactory.vector("v", "v", listOf(1.0, 2.0, 3.0)),
            UniversalMathObjectFactory.matrix("m", "A", listOf(listOf(1.0, 2.0), listOf(3.0, 4.0))),
            UniversalMathObjectFactory.dataList("data", "sample", listOf(1.0, Double.NaN, 3.0), setOf(1)),
            UniversalMathObjectFactory.probability("normal", "Normal", "Normal", mapOf("mean" to 0.0, "sd" to 1.0)),
            UniversalMathObjectFactory.unitMeasurement("length", "length", com.indianservers.aiexplorer.core.ExactRational.parse("3/2"), "m", .01),
        )
        assertTrue(objects.map { it.kind }.containsAll(setOf(
            UniversalMathKind.Scalar, UniversalMathKind.Equation, UniversalMathKind.Point3D, UniversalMathKind.Vector,
            UniversalMathKind.Matrix, UniversalMathKind.DataList, UniversalMathKind.ProbabilityModel, UniversalMathKind.UnitMeasurement,
        )))
    }

    @Test fun symbolicEditInvalidatesDependentsAndRoundTripsToWorkspace() {
        val state = WorkspaceState(functions = listOf(
            FunctionDefinition("f", "f(x)", "x^2", "cyan"),
            FunctionDefinition("g", "g(x)", "f(x)+1", "violet"),
        ))
        val engine = UniversalMathDocumentEngine()
        val document = UniversalWorkspaceBridge.fromWorkspace(state)
        assertTrue("f" in document.objects.getValue("g").dependencies)
        val result = engine.editSymbolic(document, "f", "x^3")
        assertTrue(result is UniversalMutationResult.Applied)
        result as UniversalMutationResult.Applied
        assertTrue(result.affectedObjects.containsAll(setOf("f", "g")))
        val updated = UniversalWorkspaceBridge.applyToWorkspace(result.document, state)
        assertEquals("x^3", updated.functions.first().expression)
    }

    @Test fun staleRevisionAndCyclesAreRejected() {
        val engine = UniversalMathDocumentEngine()
        val document = UniversalWorkspaceBridge.fromWorkspace(WorkspaceState())
        val function = document.objects.getValue("f")
        assertTrue(engine.upsert(document, function, expectedRevision = document.revision - 1) is UniversalMutationResult.Conflict)

        val a = UniversalMathObject("a", UniversalMathKind.Expression, "a", UniversalMathPayload.Properties(emptyMap()), setOf("b"))
        val b = UniversalMathObject("b", UniversalMathKind.Expression, "b", UniversalMathPayload.Properties(emptyMap()), setOf("a"))
        val invalid = UniversalMathDocument(objects = mapOf("a" to a, "b" to b))
        val validation = engine.validate(invalid)
        assertFalse(validation.valid)
        assertTrue(validation.cycles.isNotEmpty())
    }

    @Test fun exactNumericAndDomainEvidenceRemainDistinct() {
        val exact = kernel.equivalence("(x+1)^2", "x^2+2*x+1")
        assertEquals(EquivalenceStatus.Exact, exact.status)
        assertEquals("0", exact.exactDifference)

        val mismatch = kernel.equivalence("x*x^-1", "1")
        assertEquals(EquivalenceStatus.DomainMismatch, mismatch.status)
        assertTrue(mismatch.leftDomain.description.contains("≠ 0"))

        val counterexample = kernel.equivalence("sqrt(x^2)", "x")
        assertEquals(EquivalenceStatus.NotEquivalent, counterexample.status)
        assertTrue(counterexample.samples.any { it.residual > 0.1 })
    }

    @Test fun assumptionsConstrainVerificationAndEvaluation() {
        val positiveX = MathAssumptionSet().with(VariableAssumption("x", MathNumberDomain.Real, positive = true, nonZero = true))
        val evidence = kernel.equivalence("sqrt(x^2)", "x", positiveX)
        assertTrue(evidence.status == EquivalenceStatus.VerifiedNumerically || evidence.status == EquivalenceStatus.Exact)
        assertTrue(evidence.samples.all { it.variables.getValue("x") > 0 })
        val value = kernel.evaluate("1/3 + x", mapOf("x" to 2.0), positiveX).getOrThrow()
        assertTrue(value.verified)
        assertEquals(7.0 / 3.0, value.decimal, 1e-12)
    }

    @Test fun versionedCodecRoundTripsAndRecoversChecksumDamage() {
        val document = UniversalWorkspaceBridge.fromWorkspace(WorkspaceState())
        val encoded = UniversalMathDocumentCodec.encode(document)
        val decoded = UniversalMathDocumentCodec.decode(encoded)
        assertTrue(decoded.checksumValid)
        assertFalse(decoded.recovered)
        assertEquals(document.objects.keys, decoded.document?.objects?.keys)
        assertEquals(document.objects.getValue("f").payload, decoded.document?.objects?.get("f")?.payload)

        val damagedChecksum = encoded.replaceFirst(Regex("\\\"checksum\\\":\\\"."), "\"checksum\":\"0")
        val recovered = UniversalMathDocumentCodec.decode(damagedChecksum, recover = true)
        assertFalse(recovered.checksumValid)
        assertTrue(recovered.recovered)
        assertNotNull(recovered.document)
        assertEquals(document.objects.size, recovered.document?.objects?.size)
    }

    @Test fun schemaOneDocumentsMigrateAndWorkspaceExportEmbedsAuthority() {
        val document = UniversalWorkspaceBridge.fromWorkspace(WorkspaceState())
        val legacy = UniversalMathDocumentCodec.encode(document)
            .replaceFirst("\"schemaVersion\":2", "\"schemaVersion\":1")
            .replace(Regex("\\s*\\\"checksum\\\":\\\"[a-f0-9]+\\\",?"), "")
        val migrated: UniversalDocumentRecovery = UniversalMathDocumentCodec.decode(legacy)
        assertTrue(migrated.recovered)
        assertEquals(UniversalMathDocument.CURRENT_SCHEMA, migrated.document?.schemaVersion)
        val workspaceJson = WorkspaceJson.export(WorkspaceState())
        assertTrue(workspaceJson.contains("\"universalMathDocument\""))
        val embedded = WorkspaceJson.recoverMathDocument(workspaceJson)
        assertTrue(embedded.checksumValid)
        assertNotNull(embedded.document)
        assertTrue(WorkspaceJson.applyRecoveredMathDocument(workspaceJson).isSuccess)
    }

    @Test fun generatedTenThousandCaseCorpusPassesDeterministically() {
        val corpus = KernelRegressionCorpus.generate(10_000)
        assertEquals(10_000, corpus.size)
        corpus.forEach { case -> assertTrue(case.label, case.invariantHolds()) }
        corpus.filterIndexed { index, _ -> index % 100 == 0 }.forEach { case ->
            val evidence = kernel.equivalence(case.left, case.right, requestedSamples = 5)
            assertTrue("${case.label}: ${evidence.explanation} (${evidence.exactDifference})", evidence.equivalent)
        }
    }
}
