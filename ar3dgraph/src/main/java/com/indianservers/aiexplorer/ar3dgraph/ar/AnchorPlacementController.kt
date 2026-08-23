package com.indianservers.aiexplorer.ar3dgraph.ar

interface PlacementAnchor {
    fun detach()
}

fun interface WorldAnchorFactory {
    fun create(pose: WorldPlacementPose): PlacementAnchor
}

sealed interface AnchorPlacementResult {
    data object Placed : AnchorPlacementResult
    data object Replaced : AnchorPlacementResult
    data class Failed(val message: String) : AnchorPlacementResult
    data object TrackingPaused : AnchorPlacementResult
}

/**
 * Transactional single-anchor owner. A replacement is created before the old anchor is detached, so
 * a failed replacement leaves the previous placement recoverable.
 */
class AnchorPlacementController(
    private val attach: (PlacementAnchor) -> Unit = {},
    private val factory: WorldAnchorFactory,
) : AutoCloseable {
    private var active: PlacementAnchor? = null
    val activeAnchorCount: Int get() = if (active == null) 0 else 1

    @Synchronized
    fun place(pose: WorldPlacementPose, tracking: Boolean): AnchorPlacementResult {
        if (!tracking) return AnchorPlacementResult.TrackingPaused
        if (!pose.finite) return AnchorPlacementResult.Failed("Placement pose is invalid.")
        val replacement = runCatching { factory.create(pose) }
            .getOrElse { return AnchorPlacementResult.Failed(it.message ?: "Unable to create an AR anchor.") }
        runCatching { attach(replacement) }.getOrElse { error ->
            runCatching { replacement.detach() }
            return AnchorPlacementResult.Failed(error.message ?: "Unable to attach the graph to the new AR anchor.")
        }
        val previous = active
        active = replacement
        runCatching { previous?.detach() }
        return if (previous == null) AnchorPlacementResult.Placed else AnchorPlacementResult.Replaced
    }

    @Synchronized
    fun reset() {
        runCatching { active?.detach() }
        active = null
    }

    fun clear() = reset()
    override fun close() = reset()
}
