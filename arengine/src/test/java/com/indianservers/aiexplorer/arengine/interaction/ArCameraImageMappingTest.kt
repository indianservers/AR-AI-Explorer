package com.indianservers.aiexplorer.arengine.interaction

import com.indianservers.aiexplorer.arengine.contract.*
import org.junit.Assert.assertEquals
import org.junit.Test

class ArCameraImageMappingTest {
    private fun map(corners: List<ArVector2>, p: ArVector2) = ArCameraImage(1, 2, 2, emptyList(), corners).imagePointToView(p)
    @Test fun portraitRotationAndCropAlignWithCamera() {
        val p = map(listOf(ArVector2(1.2f, 0f), ArVector2(1.2f, 1f), ArVector2(-.2f, 0f)), ArVector2(.3f, .5f))
        assertEquals(.5f, p.x, .0001f); assertEquals(.3f, p.y, .0001f)
    }
    @Test fun landscapeIdentity() {
        assertEquals(ArVector2(.2f, .8f), map(listOf(ArVector2(0f,0f), ArVector2(1f,0f), ArVector2(0f,1f)), ArVector2(.2f,.8f)))
    }
}
