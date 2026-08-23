package com.indianservers.aiexplorer.ar3dgraph.rendering

data class ARGraphScene(
    val graphGeometryAttached: Boolean = false,
    val renderedMeshes: Int = 0,
    val activeAnchors: Int = 0,
    val anchorsCreated: Int = 0,
    val planeFindingEnabled: Boolean = false,
    val userScale: Float = 1f,
    val userYawDegrees: Float = 0f,
    val userPitchDegrees: Float = 0f,
)
