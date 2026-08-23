package com.indianservers.aiexplorer.ar3dgraph.ar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

class PlacementMathTest {
    private val identity = floatArrayOf(
        1f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f,
        0f, 0f, 1f, 0f,
        0f, 0f, 0f, 1f,
    )

    @Test fun centreTapProducesNormalizedForwardRayInPortraitAndLandscape() {
        listOf(1080 to 1920, 1920 to 1080, 800 to 800).forEach { (width, height) ->
            val ray = PlacementMath.screenRay(
                width / 2f, height / 2f, width, height,
                WorldVector3(0f, 0f, 0f), identity, identity,
            ).getOrThrow()
            assertEquals(0f, ray.direction.x, 1e-6f)
            assertEquals(0f, ray.direction.y, 1e-6f)
            assertEquals(1f, ray.direction.z, 1e-6f)
            assertEquals(1f, length(ray.direction), 1e-6f)
        }
    }

    @Test fun cornersAndEdgesProduceDistinctNormalizedRays() {
        val points = listOf(0f to 0f, 1000f to 0f, 0f to 600f, 1000f to 600f, 0f to 300f, 1000f to 300f)
        val rays = points.map { (x, y) ->
            PlacementMath.screenRay(x, y, 1000, 600, WorldVector3(0f, 0f, 0f), identity, identity).getOrThrow()
        }
        assertEquals(points.size, rays.map { it.direction }.distinct().size)
        assertTrue(rays.all { abs(length(it.direction) - 1f) < 1e-6f })
    }

    @Test fun nineCanonicalTapDirectionsHaveCorrectScreenSigns() {
        val width = 1000
        val height = 600
        fun ray(x: Float, y: Float) = PlacementMath.screenRay(
            x, y, width, height, WorldVector3(0f, 0f, 0f), identity, identity,
        ).getOrThrow().direction
        assertTrue(ray(0f, 300f).x < 0f)
        assertTrue(ray(1000f, 300f).x > 0f)
        assertTrue(ray(500f, 0f).y > 0f)
        assertTrue(ray(500f, 600f).y < 0f)
        assertTrue(ray(0f, 0f).x < 0f && ray(0f, 0f).y > 0f)
        assertTrue(ray(1000f, 0f).x > 0f && ray(1000f, 0f).y > 0f)
        assertTrue(ray(0f, 600f).x < 0f && ray(0f, 600f).y < 0f)
        assertTrue(ray(1000f, 600f).x > 0f && ray(1000f, 600f).y < 0f)
        assertEquals(1f, ray(500f, 300f).z, 1e-6f)
    }

    @Test fun oneHundredDeterministicViewportTapsStayFiniteNormalizedAndInFront() {
        repeat(100) { index ->
            val x = ((index * 379) % 1081).toFloat()
            val y = ((index * 613) % 2401).toFloat()
            val ray = PlacementMath.screenRay(x, y, 1080, 2400, WorldVector3(0f, 0f, 0f), identity, identity).getOrThrow()
            assertTrue(ray.origin.finite && ray.direction.finite)
            assertEquals(1f, length(ray.direction), 1e-6f)
            assertTrue(ray.direction.z > 0f)
            assertEquals(1.5f, length(PlacementMath.placementPose(ray, ray.origin).getOrThrow().position - ray.origin), 1e-5f)
        }
    }

    @Test fun differentInvertibleProjectionMatrixIsHonoured() {
        val projection = identity.copyOf().apply { this[0] = 2f; this[5] = 3f }
        val ray = PlacementMath.screenRay(1000f, 0f, 1000, 600, WorldVector3(0f, 0f, 0f), identity, projection).getOrThrow()
        assertTrue(ray.direction.x > 0f)
        assertTrue(ray.direction.y > 0f)
        assertEquals(1f, length(ray.direction), 1e-6f)
    }

    @Test fun translatedCameraAndViewportInsetsPreserveLocalRayCoordinates() {
        val camera = WorldVector3(3f, -2f, 5f)
        val translatedView = identity.copyOf().apply {
            this[12] = -camera.x
            this[13] = -camera.y
            this[14] = -camera.z
        }
        val viewportLeft = 24f
        val viewportTop = 120f
        val fullScreenTapX = viewportLeft + 500f
        val fullScreenTapY = viewportTop + 300f
        val ray = PlacementMath.screenRay(
            fullScreenTapX - viewportLeft,
            fullScreenTapY - viewportTop,
            1000,
            600,
            camera,
            translatedView,
            identity,
        ).getOrThrow()
        assertEquals(camera, ray.origin)
        assertEquals(0f, ray.direction.x, 1e-6f)
        assertEquals(0f, ray.direction.y, 1e-6f)
        assertEquals(1f, ray.direction.z, 1e-6f)
    }

    @Test fun invalidViewportAndSingularMatrixFailWithoutEscaping() {
        assertTrue(PlacementMath.screenRay(0f, 0f, 0, 100, WorldVector3(0f, 0f, 0f), identity, identity).isFailure)
        assertTrue(PlacementMath.screenRay(0f, 0f, 100, -1, WorldVector3(0f, 0f, 0f), identity, identity).isFailure)
        assertTrue(PlacementMath.screenRay(0f, 0f, 100, 100, WorldVector3(0f, 0f, 0f), identity, FloatArray(16)).isFailure)
        listOf(-1f to 50f, 101f to 50f, 50f to -1f, 50f to 101f).forEach { (x, y) ->
            assertTrue(PlacementMath.screenRay(x, y, 100, 100, WorldVector3(0f, 0f, 0f), identity, identity).isFailure)
        }
    }

    @Test fun defaultPoseIsExactlyOnePointFiveMetresAndFacesCameraWithoutRoll() {
        val camera = WorldVector3(0f, 0f, 0f)
        val ray = WorldRay(camera, WorldVector3(0f, 0f, 1f))
        val pose = PlacementMath.placementPose(ray, camera).getOrThrow()
        assertEquals(PlacementMath.DEFAULT_PLACEMENT_DISTANCE_METRES, length(pose.position - camera), 1e-6f)
        assertEquals(0f, pose.rotation.x, 0f)
        assertEquals(0f, pose.rotation.z, 0f)
        assertTrue(pose.finite)
        val quaternionLength = sqrt(
            pose.rotation.x * pose.rotation.x + pose.rotation.y * pose.rotation.y +
                pose.rotation.z * pose.rotation.z + pose.rotation.w * pose.rotation.w,
        )
        assertEquals(1f, quaternionLength, 1e-6f)
    }

    @Test fun placementFollowsOffCentreRayAndRejectsInvalidDistance() {
        val ray = WorldRay(WorldVector3(1f, 2f, 3f), WorldVector3(1f, 1f, 1f).normalized())
        val pose = PlacementMath.placementPose(ray, ray.origin).getOrThrow()
        assertEquals(1.5f, length(pose.position - ray.origin), 1e-6f)
        assertFalse(PlacementMath.placementPose(ray, ray.origin, Float.NaN).isSuccess)
    }

    @Test fun variedCameraPositionsAndPitchedRaysRemainOnePointFiveMetresUprightAndFinite() {
        val cameras = listOf(
            WorldVector3(0f, 0f, 0f),
            WorldVector3(4f, -3f, 2f),
            WorldVector3(-7f, 5f, -1f),
        )
        val directions = listOf(
            WorldVector3(0f, 0f, 1f),
            WorldVector3(1f, 0f, 1f),
            WorldVector3(-1f, 0f, 1f),
            WorldVector3(0f, 1f, 1f),
            WorldVector3(0f, -1f, 1f),
        )
        cameras.forEach { camera ->
            directions.forEach { direction ->
                val pose = PlacementMath.placementPose(WorldRay(camera, direction), camera).getOrThrow()
                assertEquals(1.5f, length(pose.position - camera), 1e-5f)
                assertEquals(0f, pose.rotation.x, 0f)
                assertEquals(0f, pose.rotation.z, 0f)
                assertTrue(pose.finite)
            }
        }
    }

    @Test fun nineHundredMatrixViewportCameraPlacementCasesHaveZeroFormulaDeviation() {
        val screenFractions = listOf(
            0f to 0f, .5f to 0f, 1f to 0f,
            0f to .5f, .5f to .5f, 1f to .5f,
            0f to 1f, .5f to 1f, 1f to 1f,
        )
        val viewports = listOf(1080 to 2400, 2400 to 1080, 1600 to 2560, 2560 to 1600)
        val projectionScales = listOf(.75f, 1f, 1.5f, 2f, 2.5f)
        val cameraTranslations = listOf(
            WorldVector3(-4f, 1f, 2f), WorldVector3(-2f, .5f, 1f), WorldVector3(0f, 0f, 0f),
            WorldVector3(2f, -.5f, -1f), WorldVector3(4f, -1f, -2f),
        )
        var cases = 0
        viewports.forEachIndexed { viewportIndex, (width, height) ->
            cameraTranslations.forEachIndexed { cameraIndex, camera ->
                val translatedView = identity.copyOf().apply {
                    this[12] = -camera.x
                    this[13] = -camera.y
                    this[14] = -camera.z
                }
                projectionScales.forEachIndexed { projectionIndex, scale ->
                    val projection = identity.copyOf().apply {
                        this[0] = scale
                        this[5] = projectionScales[(projectionIndex + cameraIndex) % projectionScales.size]
                    }
                    screenFractions.forEach { (xFraction, yFraction) ->
                        val insetX = (viewportIndex + 1) * 7f
                        val insetY = (cameraIndex + 1) * 11f
                        val tapX = insetX + width * xFraction
                        val tapY = insetY + height * yFraction
                        val ray = PlacementMath.screenRay(
                            tapX - insetX, tapY - insetY, width, height, camera, translatedView, projection,
                        ).getOrThrow()
                        val placed = PlacementMath.placementPose(ray, camera).getOrThrow()
                        val expected = camera + ray.direction.normalized() * PlacementMath.DEFAULT_PLACEMENT_DISTANCE_METRES
                        assertEquals(expected.x, placed.position.x, 0f)
                        assertEquals(expected.y, placed.position.y, 0f)
                        assertEquals(expected.z, placed.position.z, 0f)
                        assertEquals(1.5f, length(placed.position - camera), 1e-5f)
                        assertTrue(ray.direction.z > 0f)
                        assertTrue(placed.finite)
                        cases++
                    }
                }
            }
        }
        assertEquals(900, cases)
    }

    private fun length(value: WorldVector3) = sqrt(value.x * value.x + value.y * value.y + value.z * value.z)
}
