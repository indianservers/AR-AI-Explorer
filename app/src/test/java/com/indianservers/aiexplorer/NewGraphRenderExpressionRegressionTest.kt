package com.indianservers.aiexplorer

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import com.indianservers.aiexplorer.core.GraphTransformKind

class NewGraphRenderExpressionRegressionTest {
    @Test fun transformationPreviewRequiresOpenControlsAndAnExplicitFunction() {
        assertNull(graphTransformationPreview("x^2+y^2=1", true, GraphTransformKind.TranslateX, .5))
        assertNull(graphTransformationPreview("y=x^2", false, GraphTransformKind.TranslateX, .5))
        assertNotNull(graphTransformationPreview("y=x^2", true, GraphTransformKind.TranslateX, .5))
    }
    @Test fun constantRightHandSideDoesNotReplaceAnImplicitCurve() {
        assertEquals("x^2/3+y^2=1", graphRenderExpression("x^2/3+y^2=1", "1", 1.0))
        assertEquals("x=y^2+4", graphRenderExpression("x=y^2+4", "y^2+4", 1.0))
        assertEquals("y=x+y^2", graphRenderExpression("y=x+y^2", "x+y^2", 1.0))
    }

    @Test fun nonExplicitModesKeepTheirSourceAndExplicitParametersResolve() {
        assertEquals("r=2*sin(theta)", graphRenderExpression("r=a*sin(theta)", "sin(theta)", 2.0))
        assertEquals("x(t)=t;y(t)=t^2", graphRenderExpression("x(t)=t;y(t)=t^2", "t^2", 1.0))
        assertEquals("y>sqrt(x^2+1)", graphRenderExpression("y>sqrt(x^2+1)", "1", 1.0))
        assertEquals("2*x+1", graphRenderExpression("y=a*x+1", "2*x+1", 2.0))
    }
}
