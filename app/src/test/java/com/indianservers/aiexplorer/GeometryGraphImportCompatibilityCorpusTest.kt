package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.workspace.GeoGebraExchange
import com.indianservers.aiexplorer.workspace.GeoGebraPackageExchange
import com.indianservers.aiexplorer.workspace.Shape2DType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class GeometryGraphImportCompatibilityCorpusTest {
    private val corpus = listOf(
        """<geogebra format="5.0"><construction><element type="point" label="A"><coords x="2" y="4" z="2"/></element><element type="point" label="B"><coords x="3" y="1" z="1"/></element><command name="Segment"><input a0="A" a1="B"/><output a0="s"/></command></construction></geogebra>""",
        """<geogebra format="5.2"><construction><element type="point" label="A"><coords x="0" y="0" z="1"/></element><element type="point" label="B"><coords x="1" y="0" z="1"/></element><element type="point" label="C"><coords x="0" y="1" z="1"/></element><command name="Circle"><input a0="A" a1="B"/><output a0="c"/></command><command name="Polygon"><input a0="A" a1="B" a2="C"/><output a0="tri"/></command></construction></geogebra>""",
        """<geogebra format="6.0"><construction><expression label="f" exp="f(x)=x^2+1"/><expression label="g" exp="g(x)=sin(x)"/><element type="numeric" label="a"><value val="2"/></element></construction></geogebra>""",
        """<geogebra format="5.0"><construction><element type="point" label="A"><coords x="0" y="0" z="1"/></element><command name="Spline"><input a0="A"/><output a0="unsupported"/></command></construction></geogebra>""",
    )

    @Test fun xmlCorpusImportsSupportedFamiliesAndReportsUnsupportedObjects() {
        val results = corpus.map(GeoGebraExchange::importXml)

        assertEquals(1.0, results[0].workspace.points[0].x, 0.0)
        assertTrue(results[0].workspace.shapes.any { it.type == Shape2DType.Segment })
        assertTrue(results[1].workspace.shapes.map { it.type }.containsAll(setOf(Shape2DType.Circle, Shape2DType.Polygon)))
        assertEquals(setOf("f", "g"), results[2].workspace.functions.map { it.name }.toSet())
        assertTrue(results[2].coverage.skipped.contains("element:numeric"))
        assertTrue(results[3].coverage.skipped.any { it.contains("Spline") })
    }

    @Test fun realGgbPackageBoundaryReadsTheSameCorpusAndRejectsUnsafePaths() {
        val imported = GeoGebraPackageExchange.import(packageWith("geogebra.xml", corpus[1]))
        assertEquals(3, imported.workspace.points.size)
        assertTrue(runCatching { GeoGebraPackageExchange.import(packageWith("../geogebra.xml", corpus[0])) }.isFailure)
    }

    private fun packageWith(name: String, xml: String): ByteArray = ByteArrayOutputStream().use { bytes ->
        ZipOutputStream(bytes).use { zip -> zip.putNextEntry(ZipEntry(name)); zip.write(xml.toByteArray()); zip.closeEntry() }
        bytes.toByteArray()
    }
}
