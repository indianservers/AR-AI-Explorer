package com.indianservers.aiexplorer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MathConceptCatalogTest {
    @Test
    fun catalogContainsAllTwentySevenConceptAreas() {
        assertEquals(27, MathConceptCatalog.concepts.size)
        assertEquals(27, MathConceptCatalog.concepts.map { it.title }.distinct().size)
    }

    @Test
    fun coordinateGeometryOpensDirectSubConcepts() {
        val coordinateGeometry = MathConceptCatalog.find("Coordinate Geometry")

        assertEquals(
            listOf("Cartesian plane", "Distance & midpoint", "Straight lines", "Circles & conics"),
            coordinateGeometry?.subtopics,
        )
    }

    @Test
    fun classWiseCoverageRunsFromSixToPostgraduate() {
        MathClassBand.entries.forEach { band ->
            assertTrue("${band.label} should have concept coverage", MathConceptCatalog.search("", band).isNotEmpty())
        }
    }

    @Test
    fun searchFindsSubConceptsNotOnlySubjectTitles() {
        val matches = MathConceptCatalog.search("midpoint", null)

        assertTrue(matches.any { it.title == "Coordinate Geometry" })
    }

    @Test
    fun everyConceptExposesDistinctSearchableSubConcepts() {
        assertTrue(MathConceptCatalog.concepts.all { it.subtopics.isNotEmpty() })
        MathConceptCatalog.concepts.forEach { concept ->
            assertEquals(concept.subtopics.size, concept.subtopics.distinct().size)
            concept.subtopics.forEach { subConcept ->
                assertTrue(
                    "$subConcept should resolve to ${concept.title}",
                    MathConceptCatalog.search(subConcept, null).any { it.title == concept.title },
                )
            }
        }
    }
}
