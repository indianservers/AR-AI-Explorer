package com.indianservers.aiexplorer.core

import kotlin.math.hypot
import kotlin.math.max

/** Deterministic, size-aware placement for solids inserted from the 3D shape library. */
object SmartSolidPlacementEngine {
    fun next(existing: List<Solid>, incoming: Solid): Vec3 {
        if (existing.isEmpty()) return Vec3(0.0, 0.0, 0.0)
        val incomingRadius = footprintRadius(incoming)
        val gridStep = max(2.2, incomingRadius * 2.0 + .55)
        val candidates = buildList {
            add(0 to 0)
            for (ring in 1..12) {
                for (z in -ring..ring) for (x in -ring..ring) {
                    if (max(kotlin.math.abs(x), kotlin.math.abs(z)) == ring) add(x to z)
                }
            }
        }
        return candidates
            .map { (x, z) -> Vec3(x * gridStep, 0.0, z * gridStep) }
            .firstOrNull { candidate ->
                existing.all { solid ->
                    val requiredClearance = incomingRadius + footprintRadius(solid) + .35
                    hypot(candidate.x - solid.position.x, candidate.z - solid.position.z) >= requiredClearance
                }
            }
            ?: Vec3((existing.size + 1) * gridStep, 0.0, 0.0)
    }

    private fun footprintRadius(solid: Solid): Double =
        maxOf(solid.width / 2.0, solid.depth / 2.0, solid.radius, solid.topRadius, .45)
}
