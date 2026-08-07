package com.indianservers.aiexplorer.input

import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MathEditorReferencePart3And4Test {
    @Test
    fun referenceExpressionsEightyOneThroughTwoHundredRenderAndRoundTrip() {
        assertTrue(referenceExpressions.size == 120)
        referenceExpressions.forEachIndexed { offset, source ->
            val number = offset + 81
            val rendered = StructuredMathVisualLayout.render(source)
            val parserText = StructuredMathCodec.toParser(TextFieldValue(source)).text

            assertTrue("Expression $number rendered empty", rendered.text.isNotBlank())
            assertTrue("Expression $number serialized empty", parserText.isNotBlank())
            forbiddenVisualCommands.forEach { command ->
                assertFalse(
                    "Expression $number leaked $command: ${rendered.text}",
                    rendered.text.contains(command),
                )
            }
            for (index in 0..source.length) {
                assertTrue(
                    "Expression $number original offset $index",
                    rendered.offsetMapping.originalToTransformed(index) in 0..rendered.text.length,
                )
            }
            for (index in 0..rendered.text.length) {
                assertTrue(
                    "Expression $number transformed offset $index",
                    rendered.offsetMapping.transformedToOriginal(index) in 0..source.length,
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
        "600+(a)/(b)-(b)/(a)",
        "((x^(2)+600*x+1)/(x-600))^(2)",
        "logbase(10,(600*x+a)/(b-600))",
        "ln(exp(600*x)+1)",
        "sin(600*x)+cos((x)/(600))",
        "derivative(x^(600)+600*x,x)",
        "integral(600*x^(2)+(a)/(x)+b,x)",
        "limit((x^(2)-600^(2))/(x-600),x,600)",
        "sum((1)/(k^(2)),k,1,600)",
        "product(1+(1)/(k),k,1,600)",
        "i^(600)+i^(601)",
        "cbrt(600+(a)/(b))",
        "(600^(2)+1)/(600-x)",
        "tan(600*x)-sec((x)/(600))",
        "derivative(y,x,2)-600*derivative(y,x)+600*y=exp(x)",
        "integral((1)/(1+x^(2)),x,0,600)",
        "(1+(1)/(600))^(600)",
        "logbase(2,1+(600)/(x))",
        "derivative(600^(x),x)",
        "integral((600)/(x*(x+600)),x)",
        "[[600,1],[2,-600]]",
        "[[a,600],[-600,b]]",
        "det([[600,a,1],[0,1,600],[a,600,0]])",
        "integral(600+3*sin(x),x,0,2*pi)",
        "sum((600^(n))/(n!),n,0,infinity)",
        "limit((1+(600)/(x))^(x),x,infinity)",
        "derivative(y,x,3)+6*derivative(y,x,2)+600*derivative(y,x)+y=0",
        "integral((exp(600*x))/(1+exp(600*x)),x)",
        "(600*x^(3)-a*x+b)/(x^(2)+600*x+1)",
        "ln(x+600)+ln(x-600)",
        "derivative(ln(x+600)-ln(x-600),x)",
        "sin(600*x)^(2)+cos(600*x)^(2)",
        "atan((x)/(600))",
        "integral(exp(-600*x),x)",
        "logbase(600,x)",
        "sqrt(600^(2)-x^(2))",
        "(1)/(600)+(1)/(600^(2))+(1)/(600^(n))",
        "(x+(1)/(x))^(600)",
        "derivative((1)/(1+600*x),x)",
        "integral((1)/(x^(2)-600^(2)),x)",
        "limit((sqrt(x)-sqrt(600))/(x-600),x,600)",
        "(x^(600)-1)/(x-1)",
        "sum(k^(3),k,1,600)",
        "product(1-(1)/(k),k,1,600)",
        "derivative(y,x,4)-600^(2)*derivative(y,x,2)+y=sin(x)",
        "integral(x^(2),x,-600,600)",
        "[[600,0,1],[0,600,2],[1,2,600]]",
        "det([[600,1,0],[1,600,1],[0,1,600]])",
        "limit((1-(1)/(600*n))^(600*n),n,infinity)",
        "logbase(a,600^(x))",
        "exp(600)+exp(-600)",
        "(sin(600*x))/(600*x)",
        "integral((600*x)/((x^(2)+600)^(2)),x)",
        "ln((x+600)/(x-600))",
        "derivative(exp(600*x)*sin(x),x)",
        "sum((-1)^(n)*n,n,1,600)",
        "(x-600)/(x^(2)+600*x+1)",
        "tan(pi-600*x)",
        "integral(600*exp(-600*x),x,0,infinity)",
        "sqrt(600^(2)+((a)/(b))^(2))",
        "600+(a)/(b)+(a^(2))/(b^(2))",
        "(3*x^(3)+600*x^(2)-2*x+1)/(x^(2)-600)",
        "(600*x-(1)/(x))^(2)",
        "logbase(10,(x+600)/(x-600))",
        "ln((x^(2)+600)/(x-600))",
        "sin(600*x)-cos((x)/(600))",
        "derivative(exp(600*x)*cos(x),x)",
        "integral(600*x+(a)/(x)+600,x)",
        "limit((x^(2)-600^(2))/(x-600),x,600)",
        "sum(2*k-1,k,1,600)",
        "product(1+(1)/(k),k,1,600)",
        "cbrt(600^(3)+a^(3))",
        "exp(600*x)+exp(-600*x)",
        "tan(600*x)+sec((x)/(600))",
        "derivative(y,x,2)+600*derivative(y,x)+600^(2)*y=exp(x)",
        "integral((x^(2))/(1+x^(2)),x,0,600)",
        "(1-(1)/(600))^(-600)",
        "logbase(2,1+(600)/(x))",
        "derivative((600^(x))/(x),x)",
        "integral((600)/(x^(2)+600^(2)),x)",
        "(600^(x)+600^(-x))/(2)",
        "sinh(600*x)+cosh(600*x)",
        "(x^(3)-600*x^(2)+a)/(x^(2)+1)",
        "limit((sin(600*x))/(x),x,0)",
        "integral(sin(600*x),x,0,pi)",
        "(1)/(x-600)+(1)/(x+600)",
        "(x+(600)/(x))^(3)",
        "derivative(y,x,3)-600*derivative(y,x,2)+600*derivative(y,x)+y=0",
        "integral((exp(600*x))/(1+exp(600*x)),x)",
        "sum((600^(n)*x^(n))/(n!),n,0,infinity)",
        "det([[600,1,0],[0,600,1],[0,0,600]])",
        "[[600,a],[b,600]]*[[1,2],[3,4]]",
        "limit((1+(600)/(x))^(x),x,infinity)",
        "ln(x+600)-ln(x-600)",
        "derivative(ln(x^(2)+600*x+1),x)",
        "integral((1)/(x^(2)-600^(2)),x)",
        "atan((x)/(600))",
        "sqrt(600^(2)-x^(2))",
        "(1)/(600)+(1)/(600^(2))+(1)/(600^(n))",
        "derivative((sin(600*x))/(x),x)",
        "limit((1+(600)/(n))^(n),n,infinity)",
        "(1)/(1-(x)/(600))=sum(((x)/(600))^(n),n,0,infinity)",
        "integral(exp(-600*x)*sin(x),x)",
        "600^(1/2)+600^(1/3)+600^(-1/4)",
        "((600*x+1)^(5))/(x-600)",
        "logbase(a,(600*x+a)/(b))",
        "sum((1)/(n*(n+1)),n,1,600)",
        "det([[600,2,3],[4,600,5],[6,7,600]])",
        "([[600,0,-1],[1,600,0],[0,-1,600]])^(-1)",
        "derivative(exp(600*x)*sin(600*x),x)",
        "integral((1)/(1+x^(2)),x,-600,600)",
        "((600+(a)/(b))/(600-(a)/(b)))^(2)",
        "limit((sqrt(x)-sqrt(600))/(x-600),x,600)",
        "sum(k^(2),k,1,600)",
        "integral((600*x)/(x^(2)+600^(2)),x)",
        "logbase(600,x^(2)+1)",
        "i^(600)+i^(-600)",
        "(exp(600)-exp(-600))/(exp(600)+exp(-600))",
        "(x^(4)-600^(4))/(x^(2)+600^(2))",
        "integral(600*exp(-600*x),x,0,infinity)",
    )
}
