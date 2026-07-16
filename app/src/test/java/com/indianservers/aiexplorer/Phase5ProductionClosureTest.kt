package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.spatial.ARPrivacySafetyChecklist
import com.indianservers.aiexplorer.workspace.*
import java.util.Base64
import org.junit.Assert.*
import org.junit.Test

class Phase5ProductionClosureTest {
    private fun maths(id: String = "project", value: String = "x^2", revision: Long = 0, modified: Long = 1): UniversalMathDocument {
        val item = UniversalMathObjectFactory.symbolic("f", UniversalMathKind.Function, "f", value).copy(objectRevision = revision)
        return UniversalMathDocument(id = id, revision = revision, objects = mapOf("f" to item), modifiedAt = modified)
    }

    @Test fun projectArchiveRoundTripsAllChecksummedSections() {
        val project = AIExplorerProjectArchive.create("project", maths(), 1, 2, listOf(
            ProjectSection(ProjectSectionKind.Notebook, "cell one\ncell two", 2),
            ProjectSection(ProjectSectionKind.Settings, "{\"contrast\":true}", 1),
        ))
        val encoded = AIExplorerProjectArchive.encode(project)
        val decoded = AIExplorerProjectArchive.decode(encoded)
        assertTrue(decoded.diagnostics.joinToString(), decoded.checksumValid)
        assertFalse(decoded.recovered)
        assertEquals(3, decoded.project?.sections?.size)
        assertEquals("cell one\ncell two", decoded.project?.sections?.first { it.kind == ProjectSectionKind.Notebook }?.content)
    }

    @Test fun damagedOptionalSectionIsSkippedWhileMathsRecovers() {
        val encoded = AIExplorerProjectArchive.encode(AIExplorerProjectArchive.create("project", maths(), 1, 2, listOf(ProjectSection(ProjectSectionKind.Settings, "safe-settings"))))
        val lines = encoded.lines().toMutableList(); val index = lines.indexOfFirst { it.startsWith("S|Settings|") }
        lines[index] = lines[index].dropLast(1) + if (lines[index].last() == 'A') "B" else "A"
        val recovered = AIExplorerProjectArchive.decode(lines.joinToString("\n"), recover = true)
        assertNotNull(recovered.project)
        assertTrue(recovered.recovered)
        assertEquals(listOf(ProjectSectionKind.Settings), recovered.skippedSections)
        assertTrue(recovered.project?.sections?.any { it.kind == ProjectSectionKind.Mathematics } == true)
    }

    @Test fun legacyArchiveMigratesWithoutPretendingChecksumValidity() {
        fun pack(value: String) = Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray())
        val legacy = "AIEXPLORER_PROJECT|1|${pack("legacy")}|1|2\nS|Mathematics|true|0|${pack(UniversalMathDocumentCodec.encode(maths("legacy")))}"
        val migrated = AIExplorerProjectArchive.decode(legacy, recover = true)
        assertNotNull(migrated.project)
        assertTrue(migrated.recovered)
        assertFalse(migrated.checksumValid)
        assertEquals(AIExplorerProject.CURRENT_SCHEMA, migrated.project?.schemaVersion)
    }

    @Test fun threeWayMergeKeepsIndependentChangesAndReportsConflicts() {
        val base = maths(value = "x", revision = 0, modified = 1)
        val localF = UniversalMathObjectFactory.symbolic("f", UniversalMathKind.Function, "f", "x+1").copy(objectRevision = 1)
        val localG = UniversalMathObjectFactory.symbolic("g", UniversalMathKind.Function, "g", "2*x").copy(objectRevision = 0)
        val local = base.copy(objects = mapOf("f" to localF, "g" to localG), revision = 2, modifiedAt = 5)
        val remoteF = UniversalMathObjectFactory.symbolic("f", UniversalMathKind.Function, "f", "x-1").copy(objectRevision = 2)
        val remote = base.copy(objects = mapOf("f" to remoteF), revision = 3, modifiedAt = 6)
        val merged = ProjectMergeEngine.merge(base, local, remote, 10)
        assertEquals(setOf("f", "g"), merged.document.objects.keys)
        assertEquals(1, merged.conflicts.size)
        assertEquals(2, merged.document.objects.getValue("f").objectRevision)
        assertEquals(4, merged.document.revision)
    }

    @Test fun geogebraFoundationExportsAndImportsDeclaredCoverage() {
        val workspace = WorkspaceState(
            points = listOf(Vec2(0.0, 0.0), Vec2(2.0, 1.0), Vec2(1.0, 2.0)),
            shapes = listOf(Shape2D("s", Shape2DType.Segment, listOf(0, 1)), Shape2D("arc", Shape2DType.Arc, listOf(0, 1, 2))),
            functions = listOf(FunctionDefinition("f", "f", "x^2", "cyan")), solids = emptyList(), vectors3D = emptyList(),
        )
        val exported = GeoGebraExchange.exportXml(workspace)
        assertTrue(exported.xml.contains("<geogebra"))
        assertEquals(listOf("arc:Arc"), exported.coverage.skipped)
        val imported = GeoGebraExchange.importXml(exported.xml)
        assertEquals(3, imported.workspace.points.size)
        assertEquals("x^2", imported.workspace.functions.single().expression)
        assertEquals(4, imported.coverage.imported)
    }

    @Test fun svgAndCsvExportsAreBoundedAndReportDiscontinuities() {
        val workspace = WorkspaceState(points = listOf(Vec2(0.0, 0.0), Vec2(2.0, 0.0)), shapes = listOf(Shape2D("line", Shape2DType.Segment, listOf(0, 1))), solids = emptyList(), vectors3D = emptyList())
        val (svg, svgCoverage) = OpenMathExports.geometrySvg(workspace)
        assertTrue(svg.startsWith("<svg")); assertEquals(1, svgCoverage.exported)
        val (csv, csvCoverage) = OpenMathExports.functionCsv("1/x", -1.0, 1.0, 101)
        assertTrue(csv.startsWith("x,y")); assertTrue(csvCoverage.skipped.isNotEmpty())
    }

    @Test fun performanceManagerSelectsAdaptiveQualityFromBudgets() {
        val passing = ProductPerformanceManager.assess(ProductPerformanceSnapshot(14.0, 20_000, 300, 1_000_000, 80_000_000))
        val degraded = ProductPerformanceManager.assess(ProductPerformanceSnapshot(25.0, 70_000, 300, 1_000_000, 80_000_000))
        val failing = ProductPerformanceManager.assess(ProductPerformanceSnapshot(60.0, 100_000, 4_000, 10_000_000, 400_000_000))
        assertEquals(PerformanceStatus.Pass, passing.status)
        assertEquals(PerformanceStatus.Degraded, degraded.status)
        assertEquals(PerformanceStatus.Fail, failing.status)
        assertTrue(failing.quality in setOf(com.indianservers.aiexplorer.spatial.RenderQuality.Low, com.indianservers.aiexplorer.spatial.RenderQuality.Safety))
    }

    @Test fun deviceCapabilitiesKeepFullFallbackWithoutArcore() {
        val standard = DeviceCapabilityManager.assess(DeviceCapabilityProfile(36, 8, 512, 3, false, false, false, false))
        assertTrue("2D maths" in standard.enabled)
        assertTrue("simulated AR" in standard.enabled)
        assertFalse("live AR" in standard.enabled)
        assertTrue(standard.disabled.getValue("live AR").contains("ARCore"))
        val full = DeviceCapabilityManager.assess(DeviceCapabilityProfile(36, 8, 512, 3, true, true, true, false))
        assertEquals(DeviceCapabilityTier.FullSpatial, full.tier)
    }

    @Test fun releaseReportCannotPassWithoutPrivacyAndPhysicalEvidence() {
        val performance = ProductPerformanceManager.assess(ProductPerformanceSnapshot(12.0, 10_000, 100, 1_000_000, 50_000_000))
        val conditional = ReleaseReadinessEngine.assess(ReleaseEvidence(150, 0, 0, true, true, true, true, ARPrivacySafetyChecklist.items.map { it.id }.toSet(), false, performance))
        assertEquals(ReleaseGateStatus.Conditional, conditional.status)
        assertTrue(conditional.conditions.any { it.contains("AR") })
        val blocked = ReleaseReadinessEngine.assess(ReleaseEvidence(150, 1, 0, true, true, true, true, emptySet(), false, performance))
        assertEquals(ReleaseGateStatus.Blocked, blocked.status)
        assertTrue(blocked.blockers.any { it.contains("privacy") })
    }
}
