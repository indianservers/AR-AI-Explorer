package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.FormalMathDestination
import com.indianservers.aiexplorer.core.ManipulativeEngine
import com.indianservers.aiexplorer.core.ManipulativeItem
import com.indianservers.aiexplorer.core.ManipulativeKind
import com.indianservers.aiexplorer.core.ManipulativeScene
import com.indianservers.aiexplorer.core.Vec2
import com.indianservers.aiexplorer.core.VisualProofCatalog
import com.indianservers.aiexplorer.core.VisualProofEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase5ManipulativesTest {
    @Test
    fun objectsCreateMoveSnapCloneGroupTransformLockAndSerialize() {
        val engine = ManipulativeEngine()
        var scene = ManipulativeScene(snapSize = .5)
        scene = engine.create(scene, ManipulativeKind.AlgebraX, Vec2(.26, .74), "x")
        scene = engine.create(scene, ManipulativeKind.AlgebraUnit, Vec2(2.1, 1.1), "1")
        assertEquals(Vec2(.5, .5), scene.items.first().position)

        scene = engine.group(scene, scene.items.map { it.id }.toSet())
        val group = scene.items.first().groupId!!
        val before = scene.items.associate { it.id to it.position }
        scene = engine.move(scene, scene.items.first().id, Vec2(1.0, 1.0))
        assertTrue(scene.items.all { it.position != before.getValue(it.id) })
        scene = engine.ungroup(scene, group)
        scene = engine.duplicate(scene, scene.items.first().id)
        assertEquals(3, scene.items.size)
        val selected = scene.items.last()
        scene = engine.transform(scene, selected.id, rotationDelta = 45.0, scaleFactor = 1.5)
        assertEquals(45.0, scene.items.last().rotationDegrees, 1e-9)
        assertEquals(1.5, scene.items.last().scale, 1e-9)
        scene = engine.setLocked(scene, selected.id, true)
        val lockedPosition = scene.items.last().position
        scene = engine.move(scene, selected.id, Vec2(9.0, 9.0))
        assertEquals(lockedPosition, scene.items.last().position)
        val json = engine.serialize(scene)
        assertTrue(json.contains("\"schemaVersion\":1"))
        assertTrue(json.contains("\"locked\":true"))
        assertTrue(json.contains("AlgebraX"))
    }

    @Test
    fun manipulativesGenerateEquationGraphAndNotebookMath() {
        val engine = ManipulativeEngine()
        val scene = ManipulativeScene(
            items = listOf(
                ManipulativeItem("x1", ManipulativeKind.AlgebraX, Vec2(0.0, 0.0), value = 2.0),
                ManipulativeItem("u1", ManipulativeKind.AlgebraUnit, Vec2(1.0, 0.0), value = -3.0),
                ManipulativeItem("f1", ManipulativeKind.FractionBar, Vec2(0.0, 2.0), numerator = 1, denominator = 2),
                ManipulativeItem("f2", ManipulativeKind.FractionBar, Vec2(0.0, 3.0), numerator = 1, denominator = 3),
                ManipulativeItem("g1", ManipulativeKind.GeometricTile, Vec2(3.0, 3.0), width = 3.0, height = 2.0),
            ),
        )
        val links = engine.links(scene)

        assertTrue(links.any { it.destination == FormalMathDestination.Equation && it.content == "2x - 3 = 0" })
        assertTrue(links.any { it.destination == FormalMathDestination.Graph && it.content == "2x - 3" })
        assertTrue(links.any { it.destination == FormalMathDestination.Notebook && it.content == "5/6" })
        assertTrue(links.any { it.destination == FormalMathDestination.Notebook && it.content.contains("area = 6") })
    }

    @Test
    fun tenVisualProofsRunWithPlaybackParametersAndInvariants() {
        val engine = VisualProofEngine()
        assertTrue(VisualProofCatalog.labs.size >= 10)
        assertEquals(VisualProofCatalog.labs.size, VisualProofCatalog.labs.map { it.id }.distinct().size)

        VisualProofCatalog.labs.forEach { lab ->
            var playback = engine.start(lab.id)
            assertEquals(0, playback.frame.step)
            lab.parameters.forEach { parameter ->
                playback = engine.setParameter(playback, parameter.name, (parameter.minimum + parameter.maximum) / 2)
            }
            assertTrue(playback.frame.measurements.isNotEmpty())
            assertTrue(playback.frame.invariant.isNotBlank())
            assertTrue(playback.frame.residual.isFinite())
            assertTrue(playback.frame.holds)
            val next = engine.next(playback)
            assertTrue(next.frame.step >= playback.frame.step)
            val revealed = engine.reveal(playback)
            assertEquals(lab.steps.lastIndex, revealed.frame.step)
            assertFalse(revealed.playing)
        }
    }

    @Test
    fun proofParametersActuallyChangeMeasurementsWhileInvariantRemains() {
        val engine = VisualProofEngine()
        val initial = engine.start("pythagorean")
        val changed = engine.setParameter(initial, "a", 5.0)

        assertNotEquals(initial.frame.measurements["a²"], changed.frame.measurements["a²"])
        assertEquals(0.0, changed.frame.residual, 1e-12)
        assertTrue(changed.frame.holds)
    }
}
