package com.indianservers.aiexplorer.arengine.arcore

import com.google.ar.core.Anchor
import com.google.ar.core.HitResult
import com.indianservers.aiexplorer.arengine.contract.ArAnchorHandle
import java.util.UUID

internal class ArCoreAnchorRegistry {
    private data class NativeAnchor(val anchor: Anchor, val createdAtMillis: Long)
    private val anchors = linkedMapOf<String, NativeAnchor>()

    fun create(hit: HitResult, nowMillis: Long): ArAnchorHandle {
        require(nowMillis >= 0L)
        val native = hit.createAnchor()
        val id = "ar-anchor-${UUID.randomUUID()}"
        anchors[id] = NativeAnchor(native, nowMillis)
        return native.toHandle(id, nowMillis, nowMillis)
    }

    fun snapshots(nowMillis: Long): List<ArAnchorHandle> = anchors.map { (id, value) ->
        value.anchor.toHandle(id, value.createdAtMillis, nowMillis.coerceAtLeast(value.createdAtMillis))
    }

    fun detach(id: String): Boolean {
        val removed = anchors.remove(id) ?: return false
        runCatching { removed.anchor.detach() }
        return true
    }

    fun clear() {
        anchors.values.forEach { runCatching { it.anchor.detach() } }
        anchors.clear()
    }

    private fun Anchor.toHandle(id: String, createdAtMillis: Long, updatedAtMillis: Long) = ArAnchorHandle(
        id = id,
        pose = ArCorePoseMapper.map(pose),
        trackingState = ArCoreStateMapper.anchorTracking(trackingState),
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis,
    )
}
