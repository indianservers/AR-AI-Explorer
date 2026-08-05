package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.Geometry3D
import com.indianservers.aiexplorer.core.Solid
import com.indianservers.aiexplorer.core.SolidMeshFactory
import com.indianservers.aiexplorer.core.SolidType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SolidCatalogTest {
    @Test fun catalogHasTwentyRenderableMeasuredShapesWithFormulas() {
        assertEquals(20, SolidType.entries.size)
        SolidType.entries.forEach { type ->
            val solid = Solid(type, width = 2.0, height = 2.0, depth = 2.0, radius = .8, topRadius = .4)
            val mesh = SolidMeshFactory.create(solid, segments = 16)
            val measurements = Geometry3D.measure(solid)
            assertTrue("$type has vertices", mesh.vertices.isNotEmpty())
            assertTrue("$type has faces", mesh.faces.isNotEmpty())
            assertTrue("$type has a positive volume", measurements.volume > 0.0)
            assertTrue("$type has a positive area", measurements.surfaceArea > 0.0)
            assertTrue("$type has a formula", Geometry3D.formula(type).isNotBlank())
            assertTrue("$type exposes multiple formulas or properties", Geometry3D.formulas(type).size >= 2)
            assertEquals(
                "$type formula labels are unique",
                Geometry3D.formulas(type).size,
                Geometry3D.formulas(type).map { it.first }.distinct().size,
            )
        }
    }
}
