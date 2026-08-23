package com.indianservers.aiexplorer.ar3dgraph.presentation

import org.junit.Assert.assertEquals
import org.junit.Test

class AR3DGraphRouteTest {
    @Test fun routeAndTitleAreStable() {
        assertEquals("math/ar-3d-graph", AR3DGraphRoute.route)
        assertEquals("AR 3D Graph", AR3DGraphRoute.title)
    }
}
