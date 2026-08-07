package com.indianservers.aiexplorer.input

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MathEditorReferenceSuiteTest {
    @Test
    fun allEightyReferenceExpressionsRenderAndRoundTripWithoutLeakingEditorCommands() {
        assertTrue(referenceExpressions.size == 80)
        referenceExpressions.forEachIndexed { index, source ->
            val rendered = StructuredMathVisualLayout.render(source)
            val parserText = StructuredMathCodec.toParser(
                androidx.compose.ui.text.input.TextFieldValue(source),
            ).text

            assertTrue("Expression ${index + 1} rendered empty", rendered.text.isNotBlank())
            assertTrue("Expression ${index + 1} serialized empty", parserText.isNotBlank())
            forbiddenVisualCommands.forEach { command ->
                assertFalse(
                    "Expression ${index + 1} leaked $command: ${rendered.text}",
                    rendered.text.contains(command),
                )
            }
            for (offset in 0..source.length) {
                assertTrue(
                    rendered.offsetMapping.originalToTransformed(offset) in 0..rendered.text.length,
                )
            }
            for (offset in 0..rendered.text.length) {
                assertTrue(
                    rendered.offsetMapping.transformedToOriginal(offset) in 0..source.length,
                )
            }
        }
    }

    private val forbiddenVisualCommands = listOf(
        "sqrt(",
        "cbrt(",
        "nthroot(",
        "logbase(",
        "derivative(",
        "partial(",
        "integral(",
        "limit(",
        "sum(",
        "product(",
        "det(",
        "exp(",
        "[[",
    )

    private val referenceExpressions = listOf(
        "600+(a)/(b)",
        "(3*x^(2)-2*x+1)^(3)",
        "(2*x^(3)-5*x^(2)+4*x-7)/(x^(2)-1)",
        "sqrt(3*x^(2)+2*x-5)",
        "logbase(10,600+(a)/(b))",
        "ln(exp(2*x)+1)",
        "sin(x+(pi)/(4))+cos(x-(pi)/(3))",
        "tan(x)^(2)+sec(x)^(2)",
        "derivative(x^(3)*sin(x),x)",
        "integral(600+(a)/(b),x)",
        "limit((x^(2)-4)/(x-2),x,2)",
        "sum(2*k^(2)+3*k+1,k,1,n)",
        "product(1+(1)/(k),k,1,5)",
        "i^(7)+i^(19)",
        "derivative(y,x,2)+4*y=sin(2*x)",
        "integral(x*sin(x),x,0,pi)",
        "limit((sin(x))/(x),x,0)",
        "(1)/(x+1)+(1)/(x-1)",
        "derivative(exp(3*x)*cos(x),x)",
        "integral((2*x+1)/(x^(2)+3*x+2),x)",
        "cbrt(x^(2)+8)",
        "((x-1)/(x+2))^(5)",
        "logbase(2,(x^(2)+1)/(x-1))",
        "600^(2)+((a)/(b))^(2)+2*600*(a)/(b)",
        "sin(x)^(2)+cos(x)^(2)",
        "tan((pi)/(4)+x)",
        "derivative(ln(x^(2)+1),x)",
        "integral(exp(2*x),x)",
        "limit((1+(1)/(x))^(x),x,infinity)",
        "sum((1)/(n*(n+1)),n,1,infinity)",
        "[[1,2,3],[0,-1,4],[2,1,0]]",
        "[[a,b],[c,d]][[2,1],[3,4]]",
        "det([[2,-1,3],[0,4,5],[1,2,-2]])",
        "derivative(y,x,3)-3*derivative(y,x)+2*y=exp(x)",
        "integral(3*x^(2)+2*x+1,x,1,2)",
        "(1)/(1-x)=sum(x^(n),n,0,infinity)",
        "derivative((sin(x))/(x),x)",
        "integral((1)/(x*ln(x)),x)",
        "(600+(a)/(b))/(1-(a)/(b))",
        "sqrt(600^(2)+((a)/(b))^(2))",
        "600+(a)/(b)+(1)/(b^(2))",
        "(3*x^(2)+2*x-5)/(x-3)",
        "((x^(2)-1)/(x+1))^(2)",
        "logbase(10,(600+b)/(a-b))",
        "ln((x^(2)+2*x+1)/(x-1))",
        "sin(x)^(2)-cos(x)^(2)",
        "derivative(y,x,2)+600*y=exp(x)",
        "integral((600+(a)/(b))/(x^(2)+1),x)",
        "limit((1+(600)/(x))^(x),x,infinity)",
        "sum((1)/(n^(2)+600),n,1,infinity)",
        "(600^(x)+a^(x))/(b^(x)-1)",
        "sqrt(600+(a)/(b))",
        "(1)/(600)+(1)/(601)+(1)/(602)+(1)/(699)",
        "derivative(ln(600+x^(2)),x)",
        "integral((1)/(600^(2)+x^(2)),x)",
        "600!+a!",
        "det([[600,a],[b,1]])",
        "partial(z,x)+600*partial(z,y)=z",
        "integral((1)/(1+600*sin(x)^(2)),x,0,(pi)/(2))",
        "limit((exp(600*x)-1)/(x),x,0)",
        "(600+x)^(n)",
        "sum((n!)/(k!*(n-k)!)*600^(k)*a^(n-k),k,0,n)",
        "(601)/(600+(a)/(b))",
        "atan((x)/(600))",
        "logbase(a,(600+x)/(b))",
        "derivative((600+(a)/(b))/(x),x)",
        "600^(1/2)+((a)/(b))^(1/2)",
        "integral(exp(-600*x),x)",
        "(x^(3)-600*x+a)/(x^(2)+b)",
        "ln(600*x+a)-ln(x+b)",
        "derivative(exp(600*x)*sin(x),x)",
        "limit((ln(x))/(600+x),x,infinity)",
        "sum(((600*x)^(n))/(n!),n,0,infinity)",
        "integral((600*x+a)/(x^(2)+b*x+c),x)",
        "sqrt(600^(2)-((a)/(b))^(2))",
        "(1)/(600)*derivative(y,x,3)+derivative(y,x)=sin(x)",
        "integral((1)/(sqrt(x)),x,1,600)",
        "[[600,2*a,-1],[0,b,3],[1,-2,600]]",
        "((600+(a)/(b))/(600-(a)/(b)))^(1/2)",
        "derivative((sin(600*x))/(x),x)",
    )
}
