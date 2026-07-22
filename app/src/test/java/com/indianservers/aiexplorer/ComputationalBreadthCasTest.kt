package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.CasDomainBranchAnalyzer
import com.indianservers.aiexplorer.core.SymbolicCasEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ComputationalBreadthCasTest {
    private val cas = SymbolicCasEngine()

    @Test fun computesSeriesAndAsymptoticsWithExplicitRemainders() {
        val series = cas.casRow("exp(x),x,0,4", "series")
        assertTrue(series.supported)
        assertTrue(series.exact.contains("1/24*x^4"))
        assertTrue(series.exact.contains("O(x^5)"))
        assertTrue(cas.casRow("1/(x+1)", "asymptotic").exact.contains("O(1/x^4)"))
    }

    @Test fun evaluatesExactFiniteSumsAndProducts() {
        assertEquals("14", cas.casRow("sum(k^2,k,1,3)", "sum").exact)
        assertEquals("24", cas.casRow("product(k,k,1,4)", "product").exact)
        assertEquals("n*(n+1)/2", cas.casRow("sum(k,k,1,n)", "sum").exact)
        assertEquals("factorial(n)", cas.casRow("product(k,k,1,n)", "product").exact)
    }

    @Test fun solvesFactoredPolynomialAndRationalInequalitiesByExactSignCharts() {
        val polynomial = cas.solveInequalities(listOf("(x-1)*(x+2)>=0"))
        assertTrue(polynomial.supported)
        assertEquals("(-infinity, -2] union [1, infinity)", polynomial.exact)
        val rational = cas.solveInequalities(listOf("(x-1)/(x+3)<0"))
        assertEquals("(-3, 1)", rational.exact)
        assertTrue(rational.assumptions.contains("x != -3"))
    }

    @Test fun supportsFourierPairsSpecialFunctionsAndNumberTheory() {
        assertEquals("2/(1+w^2)", cas.casRow("exp(-abs(x))", "fourier").exact)
        assertEquals("120", cas.casRow("gamma(6)", "special functions").exact)
        assertEquals("6", cas.casRow("gcd(54,24)", "number theory").exact)
        assertEquals("2^2 * 3 * 5", cas.casRow("factorInteger(60)", "number theory").exact)
        assertEquals("1", cas.casRow("residue(1/(z-1),z,1)", "residue").exact)
        assertTrue(cas.casRow("residue(1/(z-1),z,1)", "contour integral").exact.contains("2*pi*i"))
    }

    @Test fun solvesRecurrencesOptimizationAndClassifiesPdes() {
        assertEquals("a(n) = -3 + (4)*(2)^n", cas.casRow("a(n)=2*a(n-1)+3, a(0)=1", "recurrence").exact)
        assertEquals("minimum -1 at x=1", cas.casRow("minimize(2*x^2-4*x+1)", "optimization").exact)
        assertEquals("minimum 2 at (x=1, y=1)", cas.casRow("minimize(x^2+y^2) subject to 1*x+1*y=2", "optimization").exact)
        assertEquals("elliptic PDE", cas.casRow("u_xx+u_yy=0", "pde").exact)
        assertEquals("hyperbolic PDE", cas.casRow("u_xx-u_yy=0", "pde").exact)
    }

    @Test fun exposesFiniteAlgebraAndExactAlgebraicRootObjects() {
        val ring = cas.casRow("Z/8Z", "finite algebra")
        assertTrue(ring.exact.contains("units={1, 3, 5, 7}"))
        val roots = cas.casRow("x^5-x+1", "exact roots")
        assertTrue(roots.supported)
        assertEquals(5, Regex("RootOf").findAll(roots.exact).count())
    }

    @Test fun matrixBreadthIncludesEigenvectorsJordanAndSvd() {
        assertTrue(cas.casRow("[[2,0,0],[0,2,0],[0,0,3]]", "eigenvectors").exact.contains("span{e1, e2}"))
        assertTrue(cas.casRow("[[2,1],[0,2]]", "jordan form").supported)
        val svd = cas.casRow("[[3,0],[0,4]]", "svd")
        assertTrue(svd.supported)
        assertTrue(svd.exact.contains("S=[4.000000000, 3.000000000]"))
        assertTrue(svd.decimal!!.toDouble() < 1e-8)
        assertEquals("[[-2, 1], [3/2, -1/2]]", cas.casRow("[[1,2],[3,4]]", "matrix inverse").exact)
        assertEquals("2", cas.casRow("[[1,2,3],[2,4,6],[0,1,0]]", "rank").exact)
        assertTrue(cas.casRow("[[1,2,3],[2,4,6]]", "nullspace").exact.startsWith("span"))
        assertTrue(cas.casRow("[[2,1],[0,3]]; lambda=2", "eigenvectors").exact.contains("span{(1, 0)}"))
    }

    @Test fun domainAndBranchAnalysisFollowsNotebookExpressions() {
        val report = CasDomainBranchAnalyzer.analyze("sqrt(x-1)/(x-2) + ln(y)")
        assertTrue(report.domain.contains("x-1 >= 0"))
        assertTrue(report.domain.contains("y > 0"))
        assertTrue(report.excluded.contains("x-2 = 0"))
        assertFalse(report.branches.isEmpty())
    }

    @Test fun broadensIntegrationLimitsAndDifferentialEquationsWithVerificationSteps() {
        assertEquals("exp(x)*(x - 1) + C", cas.integral("x*exp(x)").exact)
        assertEquals("1", cas.limit("sin(x)/x", approaching = "0").exact)
        cas.solveOde("y'=2*y^2").let { assertTrue(it.exact, it.supported && it.exact.contains("y =")) }
        val third = cas.solveOde("y'''-6*y''+11*y'-6*y=0")
        assertTrue(third.supported)
        assertTrue(third.exact.contains("C3"))
        assertTrue(third.steps.any { it.title == "Verify" })
    }

    @Test fun solvesMultiVariableNonlinearSystemsThroughDependencyElimination() {
        val row = cas.solveSystem(listOf("y=x^2", "z=y+1", "x+z=7"), listOf("x", "y", "z"))
        assertTrue(row.supported)
        assertTrue(row.exact.contains("x = 2, y = 4, z = 5"))
        assertTrue(row.exact.contains("x = -3, y = 9, z = 10"))
        assertTrue(row.steps.any { it.title == "Dependency ordering" })
    }
}
