package com.indianservers.aiexplorer.ar3dgraph.ar

import com.indianservers.aiexplorer.ar3dgraph.presentation.ARCameraPermissionState

object ARCameraPermissionManager {
    fun classify(
        granted: Boolean,
        shouldShowRationale: Boolean,
        hasRequested: Boolean,
    ): ARCameraPermissionState = when {
        granted -> ARCameraPermissionState.Granted
        shouldShowRationale -> ARCameraPermissionState.PermissionDenied
        hasRequested -> ARCameraPermissionState.PermissionPermanentlyDenied
        else -> ARCameraPermissionState.PermissionRequired
    }
}
