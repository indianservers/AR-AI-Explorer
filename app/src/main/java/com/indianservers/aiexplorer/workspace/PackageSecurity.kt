package com.indianservers.aiexplorer.workspace

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

data class SignedPackageEnvelope(val keyId: String, val issuedAt: Long, val expiresAt: Long?, val payload: String, val signature: String, val algorithm: String = "HmacSHA256")
data class PackageSignatureVerification(val trusted: Boolean, val payload: String?, val keyId: String?, val diagnostics: List<String>)

/** Local/classroom authenticity envelope. Keys are supplied by a keystore owner and never persisted in the package. */
object TrustedPackageSigner {
    const val maximumPayloadChars = 8_000_000
    fun sign(payload: String, keyId: String, key: ByteArray, issuedAt: Long, expiresAt: Long? = null): String {
        require(payload.length <= maximumPayloadChars && keyId.matches(Regex("[A-Za-z0-9._-]{1,64}")) && key.size >= 32)
        require(expiresAt == null || expiresAt > issuedAt)
        val encoded = pack(payload); val canonical = "$keyId|$issuedAt|${expiresAt ?: ""}|$encoded"
        return "AIEXPLORER_SIGNED|1|$canonical|${hmac(canonical, key)}"
    }

    fun verify(source: String, trustedKeys: Map<String, ByteArray>, now: Long): PackageSignatureVerification {
        if (source.length > maximumPayloadChars * 2) return PackageSignatureVerification(false, null, null, listOf("Signed package exceeds the safety limit."))
        val fields = source.split('|'); if (fields.size != 7 || fields[0] != "AIEXPLORER_SIGNED" || fields[1] != "1") return PackageSignatureVerification(false, null, null, listOf("Unsupported signed-package envelope."))
        val keyId = fields[2]; val issued = fields[3].toLongOrNull(); val expires = fields[4].toLongOrNull(); val payload64 = fields[5]; val provided = fields[6]
        // A payload cannot contain '|' because it is base64url; the exact field count protects parser confusion.
        if (issued == null) return PackageSignatureVerification(false, null, keyId, listOf("Invalid issue time."))
        val key = trustedKeys[keyId] ?: return PackageSignatureVerification(false, null, keyId, listOf("Signing key '$keyId' is not trusted on this device."))
        val canonical = "$keyId|$issued|${fields[4]}|$payload64"; val expected = hmac(canonical, key)
        if (!MessageDigest.isEqual(provided.toByteArray(), expected.toByteArray())) return PackageSignatureVerification(false, null, keyId, listOf("Package signature does not match the payload."))
        if (issued > now + 300_000) return PackageSignatureVerification(false, null, keyId, listOf("Package issue time is in the future."))
        if (expires != null && now > expires) return PackageSignatureVerification(false, null, keyId, listOf("Package signature has expired."))
        val payload = runCatching { unpack(payload64) }.getOrElse { return PackageSignatureVerification(false, null, keyId, listOf("Signed payload is not valid base64url.")) }
        if (payload.length > maximumPayloadChars) return PackageSignatureVerification(false, null, keyId, listOf("Decoded payload exceeds the safety limit."))
        return PackageSignatureVerification(true, payload, keyId, listOf("HMAC-SHA-256 authenticity verified with trusted key '$keyId'."))
    }

    private fun hmac(value: String, key: ByteArray): String {
        val mac = Mac.getInstance("HmacSHA256"); mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(value.toByteArray(StandardCharsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }
    private fun pack(value: String) = Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(StandardCharsets.UTF_8))
    private fun unpack(value: String) = String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)
}

enum class SecuritySeverity { Info, Warning, Error, Critical }
data class AppSecurityConfiguration(
    val permissions: Set<String>,
    val exportedComponents: Map<String, Boolean>,
    val cleartextTrafficAllowed: Boolean,
    val backupAllowed: Boolean,
    val cameraFramesPersisted: Boolean,
    val cameraFramesUploaded: Boolean,
    val secretsInSource: Boolean,
    val networkTransportsAttached: Set<String>,
)
data class SecurityFinding(val id: String, val severity: SecuritySeverity, val message: String, val remediation: String)
data class SecurityAuditReport(val passed: Boolean, val findings: List<SecurityFinding>)

object AppSecurityAuditEngine {
    private val sensitivePermissions = setOf("android.permission.RECORD_AUDIO", "android.permission.ACCESS_FINE_LOCATION", "android.permission.READ_CONTACTS", "android.permission.READ_MEDIA_IMAGES")
    fun audit(config: AppSecurityConfiguration): SecurityAuditReport {
        val findings = buildList {
            (config.permissions intersect sensitivePermissions).forEach { add(SecurityFinding("permission-$it", SecuritySeverity.Warning, "Sensitive permission declared: $it", "Remove it unless a documented feature requires it.")) }
            config.exportedComponents.filterValues { it }.keys.filterNot { it == "MainActivity" }.forEach { add(SecurityFinding("exported-$it", SecuritySeverity.Error, "$it is externally exported.", "Disable export or require a signature-level permission.")) }
            if (config.cleartextTrafficAllowed) add(SecurityFinding("cleartext", SecuritySeverity.Critical, "Cleartext network traffic is allowed.", "Disable cleartext traffic in the network security configuration."))
            if (config.cameraFramesPersisted) add(SecurityFinding("camera-storage", SecuritySeverity.Critical, "Camera frames may be persisted.", "Keep AR frames in GPU/session memory only."))
            if (config.cameraFramesUploaded) add(SecurityFinding("camera-upload", SecuritySeverity.Critical, "Camera frames may be uploaded.", "Remove upload paths or require explicit reviewed consent."))
            if (config.secretsInSource) add(SecurityFinding("embedded-secret", SecuritySeverity.Critical, "A signing/API secret is embedded in source.", "Move secrets to Android Keystore or CI secret storage."))
            if (config.backupAllowed) add(SecurityFinding("backup", SecuritySeverity.Warning, "Application backup is enabled.", "Confirm project/audit data exclusions before production."))
            if (config.networkTransportsAttached.isEmpty()) add(SecurityFinding("offline", SecuritySeverity.Info, "No classroom/AR network transport is attached.", "Keep future sharing opt-in and consent-gated."))
        }
        return SecurityAuditReport(findings.none { it.severity >= SecuritySeverity.Error }, findings)
    }
}
