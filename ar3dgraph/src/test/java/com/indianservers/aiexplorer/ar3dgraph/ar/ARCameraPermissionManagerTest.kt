package com.indianservers.aiexplorer.ar3dgraph.ar

import com.indianservers.aiexplorer.ar3dgraph.presentation.ARCameraPermissionState
import org.junit.Assert.assertEquals
import org.junit.Test

class ARCameraPermissionManagerTest {
    @Test fun grantedMapsToGranted() = assertEquals(ARCameraPermissionState.Granted, ARCameraPermissionManager.classify(true, false, true))
    @Test fun firstVisitRequiresPermission() = assertEquals(ARCameraPermissionState.PermissionRequired, ARCameraPermissionManager.classify(false, false, false))
    @Test fun denialWithRationaleIsRetryable() = assertEquals(ARCameraPermissionState.PermissionDenied, ARCameraPermissionManager.classify(false, true, true))
    @Test fun denialWithoutRationaleAfterRequestIsPermanent() = assertEquals(ARCameraPermissionState.PermissionPermanentlyDenied, ARCameraPermissionManager.classify(false, false, true))
}
