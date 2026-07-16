package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.spatial.ThermalLevel
import com.indianservers.aiexplorer.spatial.TrackingQuality
import com.indianservers.aiexplorer.workspace.*
import org.junit.Assert.*
import org.junit.Test

class Phase6ReleaseQualityTest {
    @Test fun deterministicSmokeBenchmarkIsReproducible() {
        val first = DeterministicMathBenchmarkRunner().run(ReleaseMathBenchmarkCatalog.smoke)
        val second = DeterministicMathBenchmarkRunner().run(ReleaseMathBenchmarkCatalog.smoke)
        assertEquals(first.results.map { it.actual }, second.results.map { it.actual })
        assertEquals(first.results.filterNot { it.passed }.joinToString { "${it.id}: ${it.actual}" }, first.results.size, first.passed)
    }

    @Test fun signedPackagesRejectTamperingUnknownKeysAndExpiry() {
        val key = ByteArray(32) { (it + 7).toByte() }
        val signed = TrustedPackageSigner.sign("project-data", "classroom-1", key, issuedAt = 1_000, expiresAt = 4_000)
        val verified = TrustedPackageSigner.verify(signed, mapOf("classroom-1" to key), now = 2_000)
        assertTrue(verified.diagnostics.joinToString(), verified.trusted)
        assertEquals("project-data", verified.payload)
        assertFalse(TrustedPackageSigner.verify(signed, emptyMap(), 2_000).trusted)
        assertFalse(TrustedPackageSigner.verify(signed, mapOf("classroom-1" to key), 5_000).trusted)
        val payloadField = signed.split('|')[5]
        val tampered = signed.replace(payloadField, payloadField.dropLast(1) + if (payloadField.last() == 'A') 'B' else 'A')
        assertFalse(TrustedPackageSigner.verify(tampered, mapOf("classroom-1" to key), 2_000).trusted)
    }

    @Test fun securityAuditBlocksUnsafeTransportAndCameraHandling() {
        val safe = AppSecurityAuditEngine.audit(AppSecurityConfiguration(
            permissions = setOf("android.permission.CAMERA"), exportedComponents = mapOf("MainActivity" to true),
            cleartextTrafficAllowed = false, backupAllowed = false, cameraFramesPersisted = false,
            cameraFramesUploaded = false, secretsInSource = false, networkTransportsAttached = emptySet(),
        ))
        assertTrue(safe.findings.joinToString(), safe.passed)
        val unsafe = AppSecurityAuditEngine.audit(AppSecurityConfiguration(
            permissions = emptySet(), exportedComponents = mapOf("DebugReceiver" to true),
            cleartextTrafficAllowed = true, backupAllowed = true, cameraFramesPersisted = true,
            cameraFramesUploaded = true, secretsInSource = true, networkTransportsAttached = setOf("http"),
        ))
        assertFalse(unsafe.passed)
        assertTrue(unsafe.findings.any { it.severity == SecuritySeverity.Critical })
    }

    @Test fun physicalArEvidenceRequiresDurationAndMeetsNumericalThresholds() {
        val samples = (0..30).map { index ->
            val expected = Vec3(index / 100.0, .2, -.4)
            ArQaSample(index * 400L, expected, expected + Vec3(.005, -.004, .003), 1.5, 1.51, 15.5,
                TrackingQuality.Tracking, ThermalLevel.Nominal)
        }
        val passing = ArDeviceQaEngine.assess(samples)
        assertTrue(passing.diagnostics.joinToString(), passing.validEvidence)
        assertTrue(passing.diagnostics.joinToString(), passing.passed)
        assertFalse(ArDeviceQaEngine.assess(samples.take(5)).validEvidence)
        val drifting = samples.map { it.copy(observedPosition = it.expectedPosition + Vec3(.2, 0.0, 0.0)) }
        assertFalse(ArDeviceQaEngine.assess(drifting).passed)
    }

    @Test fun accessibilityContractFindsEveryReleaseBlockingClass() {
        val passing = AccessibilityQaEngine.audit(listOf(AccessibilityNodeEvidence("solve", "Solve", "Button", 48.0, 48.0, true, 7.0)))
        assertTrue(passing.passed)
        val failing = AccessibilityQaEngine.audit(listOf(AccessibilityNodeEvidence("bad", null, null, 32.0, 44.0, false, 2.0, true, false)))
        assertFalse(failing.passed)
        assertTrue(failing.findings.size >= 5)
    }

    @Test fun qaEvidenceIsChecksummedAndTamperEvident() {
        val bundle = ReleaseQaEvidenceBundle("debug-1", "emulator", 42, listOf(
            QaEvidenceSection("maths", "PASS", listOf("12/12 deterministic cases")),
            QaEvidenceSection("ar", "NOT_RECORDED", listOf("Physical device run required")),
        ))
        val encoded = ReleaseQaEvidenceCodec.encode(bundle)
        assertTrue(ReleaseQaEvidenceCodec.verify(encoded))
        val lines = encoded.lines().toMutableList()
        lines[1] = lines[1].dropLast(1) + if (lines[1].last() == 'A') 'B' else 'A'
        assertFalse(ReleaseQaEvidenceCodec.verify(lines.joinToString("\n")))
    }
}
