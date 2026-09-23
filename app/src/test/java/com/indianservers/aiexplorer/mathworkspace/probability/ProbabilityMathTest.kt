package com.indianservers.aiexplorer.mathworkspace.probability

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.exp

class ProbabilityMathTest {
    @Test fun standardNormalIntervalAndLargeMeanScale() {
        val standard = DistributionParameters(0.0,1.0)
        assertEquals(.682689492,ProbabilityMath.interval(DistributionKind.Normal,-1.0,1.0,standard),2e-8)
        val shifted=DistributionParameters(1000.0,.01)
        assertTrue(ProbabilityMath.valid(DistributionKind.Normal,shifted))
        assertEquals(.682689492,ProbabilityMath.interval(DistributionKind.Normal,999.99,1000.01,shifted),2e-8)
        val (a,b)=ProbabilityMath.support(DistributionKind.Normal,shifted)
        assertTrue(a>900&&b>1000)
    }
    @Test fun discreteMomentsAndQueries() {
        val binomial=DistributionParameters(trials=10,probability=.5)
        assertEquals(5.0,ProbabilityMath.mean(DistributionKind.Binomial,binomial),0.0)
        assertEquals(2.5,ProbabilityMath.variance(DistributionKind.Binomial,binomial),0.0)
        assertEquals(1.0,ProbabilityMath.interval(DistributionKind.Binomial,0.0,10.0,binomial),1e-12)
        assertEquals(0.24609375,ProbabilityMath.pmf(DistributionKind.Binomial,5.0,binomial),1e-12)
        val poisson=DistributionParameters(first=4.0)
        assertEquals(4.0,ProbabilityMath.mean(DistributionKind.Poisson,poisson),0.0)
        assertEquals(4.0,ProbabilityMath.variance(DistributionKind.Poisson,poisson),0.0)
        assertEquals(1.0,ProbabilityMath.interval(DistributionKind.Poisson,0.0,100.0,poisson),1e-12)
    }
    @Test fun otherContinuousDistributionsAndValidation() {
        val uniform=DistributionParameters(-2.0,2.0)
        assertEquals(.5,ProbabilityMath.interval(DistributionKind.Uniform,-1.0,1.0,uniform),1e-12)
        assertFalse(ProbabilityMath.valid(DistributionKind.Uniform,DistributionParameters(2.0,1.0)))
        val exponential=DistributionParameters(first=2.0)
        assertEquals(1-exp(-2.0),ProbabilityMath.cdf(DistributionKind.Exponential,1.0,exponential),1e-12)
        assertFalse(ProbabilityMath.valid(DistributionKind.Normal,DistributionParameters(0.0,0.0)))
    }
    @Test fun largeDiscreteRangesRemainComputedAndPlottable() {
        val binomial=DistributionParameters(trials=100_000,probability=.5)
        assertEquals(.5,ProbabilityMath.cdf(DistributionKind.Binomial,49_999.0,binomial),.01)
        val (lo,hi)=ProbabilityMath.support(DistributionKind.Binomial,binomial)
        assertTrue(hi-lo<100_000&&lo>0)
        val poisson=DistributionParameters(first=1_000_000.0)
        assertEquals(.5,ProbabilityMath.cdf(DistributionKind.Poisson,999_999.0,poisson),.01)
    }
}
