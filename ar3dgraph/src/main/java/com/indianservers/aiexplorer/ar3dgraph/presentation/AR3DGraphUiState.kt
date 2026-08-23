package com.indianservers.aiexplorer.ar3dgraph.presentation

import com.indianservers.aiexplorer.ar3dgraph.gesture.ARGraphTransformState

enum class ARCapabilityState {
    Checking,
    Supported,
    Unsupported,
    ARCoreNotInstalled,
    ARCoreUpdateRequired,
    InstallationRequested,
    Error,
}

enum class ARCameraPermissionState {
    PermissionRequired,
    Granted,
    PermissionDenied,
    PermissionPermanentlyDenied,
}

enum class ARSessionState {
    Idle,
    SessionInitializing,
    Ready,
    SessionRunning,
    SessionPaused,
    SessionError,
    Closed,
}

enum class ARGraphPlacementState {
    NoGraph,
    GeneratingGraph,
    GraphReadyForPlacement,
    WaitingForTracking,
    Placing,
    Placed,
    Repositioning,
    PlacementFailed,
    Cleared,
}

data class AR3DGraphUiState(
    val capability: ARCapabilityState = ARCapabilityState.Checking,
    val cameraPermission: ARCameraPermissionState = ARCameraPermissionState.PermissionRequired,
    val session: ARSessionState = ARSessionState.Idle,
    val message: String = "Checking ARCore availability…",
    val hasRequestedCamera: Boolean = false,
    val equations: String = "z = x^2 + y^2",
    val domainMinimum: String = "-3",
    val domainMaximum: String = "3",
    val density: String = "26",
    val graphMessage: String = "Enter an equation and tap Plot.",
    val placement: ARGraphPlacementState = ARGraphPlacementState.NoGraph,
    val tracking: com.indianservers.aiexplorer.ar3dgraph.ar.ARTrackingState =
        com.indianservers.aiexplorer.ar3dgraph.ar.ARTrackingState.Stopped,
    val renderData: com.indianservers.aiexplorer.ar3dgraph.integration.ARGraphRenderData? = null,
    val userTransform: ARGraphTransformState = ARGraphTransformState(),
)
