package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.*
import org.junit.Assert.*
import org.junit.Test

class MobileMathInputRegressionTest {
    @Test fun squaredYEquationsUseImplicitRendering() {
        for (source in listOf("y=x+y^2", "x²+y²=4", "Y^2=X")) {
            assertTrue(TypedGraphExpressionParser.parse(source) is TypedGraphExpression.Implicit)
            assertEquals(GraphDefinitionKind.Implicit, GraphAnalysis().definitionKind(source))
            assertEquals(AdvancedGraphKind.Implicit, AdvancedGraphEngine().classify(source))
        }
        assertEquals(GraphDefinitionKind.Explicit, GraphAnalysis().definitionKind("y=x^2"))
    }

    @Test fun missingOpeningBracketRepairPreservesEquationSides() {
        val source = "y=x+1)"
        val repair = MathInputIntelligence.assist(source).actions.first { it.label == "Add (" }
        val corrected = MathInputIntelligence.apply(source, repair).first
        assertEquals("y=(x+1)", corrected)
        assertTrue(MathInputIntelligence.analyze(corrected).validBrackets)
    }

    @Test fun selfReferentialZIsImplicitAndDuplicateCoordinatesAreRejected() {
        assertEquals(SpatialSurfaceKind.Implicit, SurfaceInputInterpreter.interpret("z=x^2+y^2+z^2").getOrThrow().kind)
        assertTrue(SurfaceInputInterpreter.interpret("x=u;x=v;y=u;z=v").isFailure)
    }

    @Test fun nestedBracketPairsShareDepthAndDifferentPairsHaveDifferentDepths() {
        val brackets = MathInputIntelligence.analyze("((x+1)*(y+2))").tokens
            .filter { it.kind == MathInputTokenKind.Bracket }
        assertEquals(listOf(1, 2, 2, 2, 2, 1), brackets.map { it.depth })
    }
}
