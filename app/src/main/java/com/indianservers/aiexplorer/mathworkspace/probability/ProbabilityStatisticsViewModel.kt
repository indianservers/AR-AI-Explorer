package com.indianservers.aiexplorer.mathworkspace.probability

import androidx.compose.runtime.mutableStateOf
import kotlin.math.*
import kotlin.random.Random

class ProbabilityStatisticsViewModel {
    val kind = mutableStateOf(DistributionKind.Normal)
    val mode = mutableStateOf(ProbabilityMode.Distributions)
    val first = mutableStateOf("0")
    val second = mutableStateOf("1")
    val trials = mutableStateOf("10")
    val probability = mutableStateOf("0.5")
    val lower = mutableStateOf("-1")
    val upper = mutableStateOf("1")
    val sampleSize = mutableStateOf("1000")
    val sheet = mutableStateOf(com.indianservers.aiexplorer.mathworkspace.components.WorkspaceSheetStop.Peek)
    val viewport = mutableStateOf(DistributionViewport())
    val inspectedX = mutableStateOf<Double?>(null)
    val samples = mutableStateOf<List<Double>>(emptyList())
    val simulationMeans = mutableStateOf<List<Double>>(emptyList())
    val seed = mutableStateOf("42")

    fun parameters() = DistributionParameters(first.value.toDoubleOrNull() ?: Double.NaN, second.value.toDoubleOrNull() ?: Double.NaN, trials.value.toIntOrNull() ?: -1, probability.value.toDoubleOrNull() ?: Double.NaN)
    fun resetView() { viewport.value = DistributionViewport() }
    fun sample(repeated: Boolean = false) {
        val n = (sampleSize.value.toIntOrNull() ?: 1000).coerceIn(1, 200_000)
        val p = parameters(); if (!ProbabilityMath.valid(kind.value, p)) return
        val random = Random(seed.value.toIntOrNull() ?: 42)
        fun one(): Double = when (kind.value) {
            DistributionKind.Normal -> p.first + p.second * sqrt(-2 * ln(random.nextDouble().coerceAtLeast(1e-15))) * cos(2 * Math.PI * random.nextDouble())
            DistributionKind.Uniform -> p.first + random.nextDouble() * (p.second - p.first)
            DistributionKind.Exponential -> -ln(random.nextDouble().coerceAtLeast(1e-15)) / p.first
            DistributionKind.Binomial -> if(p.trials<=5000) (0 until p.trials).count { random.nextDouble() < p.probability }.toDouble() else {
                val m=p.trials*p.probability;val sd=sqrt(p.trials*p.probability*(1-p.probability));(m+sd*sqrt(-2*ln(random.nextDouble().coerceAtLeast(1e-15)))*cos(2*Math.PI*random.nextDouble())).roundToInt().coerceIn(0,p.trials).toDouble()
            }
            DistributionKind.Poisson -> if(p.first<30){val l=exp(-p.first);var k=0;var product=1.0;do{k++;product*=random.nextDouble().coerceAtLeast(1e-15)}while(product>l);(k-1).toDouble()}else poissonLarge(p.first,random)
        }
        if (!repeated) { samples.value = List(n) { one() }; simulationMeans.value = emptyList() }
        else {
            val count = 200
            simulationMeans.value = List(count) { (0 until min(n, 5000)).sumOf { one() } / min(n, 5000) }
            samples.value = emptyList()
        }
    }

    private fun poissonLarge(lambda:Double,random:Random):Double {
        val root=sqrt(lambda);val b=.931+2.53*root;val a=-.059+.02483*b;val invAlpha=1.1239+1.1328/(b-3.4);val vr=.9277-3.6224/(b-2)
        while(true){val u=random.nextDouble()-.5;val v=random.nextDouble();val us=.5-abs(u);if(us<=0)continue
            val k=floor((2*a/us+b)*u+lambda+.43);if(us>=.07&&v<=vr)return k
            if(k<0||(us<.013&&v>us))continue
            val lhs=ln(v*invAlpha/(a/(us*us)+b));val rhs=-lambda+k*ln(lambda)-logFactorial(k)
            if(lhs<=rhs)return k
        }
    }
    private fun logFactorial(k:Double):Double {if(k<2)return 0.0;val x=k+1;return (x-.5)*ln(x)-x+.5*ln(2*Math.PI)+1/(12*x)-1/(360*x*x*x)}
}
