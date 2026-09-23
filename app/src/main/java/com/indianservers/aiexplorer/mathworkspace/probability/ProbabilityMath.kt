package com.indianservers.aiexplorer.mathworkspace.probability

import kotlin.math.*

object ProbabilityMath {
    fun valid(kind: DistributionKind, p: DistributionParameters): Boolean = when (kind) {
        DistributionKind.Normal -> p.second > 0.0 && p.first.isFinite() && p.second.isFinite()
        DistributionKind.Uniform -> p.second > p.first && p.first.isFinite() && p.second.isFinite()
        DistributionKind.Exponential -> p.first > 0.0 && p.first.isFinite()
        DistributionKind.Binomial -> p.trials >= 0 && p.probability in 0.0..1.0
        DistributionKind.Poisson -> p.first > 0.0 && p.first.isFinite()
    }

    fun pdf(kind: DistributionKind, x: Double, p: DistributionParameters): Double {
        if (!valid(kind, p) || !x.isFinite()) return Double.NaN
        return when (kind) {
            DistributionKind.Normal -> exp(-.5 * ((x - p.first) / p.second).pow(2)) / (p.second * sqrt(2 * PI))
            DistributionKind.Uniform -> if (x in p.first..p.second) 1.0 / (p.second - p.first) else 0.0
            DistributionKind.Exponential -> if (x < 0) 0.0 else p.first * exp(-p.first * x)
            else -> Double.NaN
        }
    }

    fun cdf(kind: DistributionKind, x: Double, p: DistributionParameters): Double {
        if (!valid(kind, p) || x.isNaN()) return Double.NaN
        return when (kind) {
            DistributionKind.Normal -> .5 * erfc(-((x - p.first) / (p.second * sqrt(2.0))))
            DistributionKind.Uniform -> when { x <= p.first -> 0.0; x >= p.second -> 1.0; else -> (x - p.first) / (p.second - p.first) }
            DistributionKind.Exponential -> if (x <= 0) 0.0 else -Math.expm1(-p.first * x)
            DistributionKind.Binomial -> if (x < 0) 0.0 else if (x >= p.trials) 1.0 else binomialCdf(floor(x).toInt(),p.trials,p.probability)
            DistributionKind.Poisson -> if (x < 0) 0.0 else poissonCdf(floor(x),p.first)
        }
    }

    fun pmf(kind: DistributionKind, x: Double, p: DistributionParameters): Double {
        if (!valid(kind, p) || x < 0 || x != floor(x)) return 0.0
        val k = x.toInt()
        return when (kind) {
            DistributionKind.Binomial -> {
                if (k > p.trials) 0.0 else if (p.probability == 0.0) if (k == 0) 1.0 else 0.0
                else if (p.probability == 1.0) if (k == p.trials) 1.0 else 0.0
                else exp(logChoose(p.trials, k) + k * ln(p.probability) + (p.trials - k) * ln1p(-p.probability))
            }
            DistributionKind.Poisson -> exp(-p.first + x * ln(p.first) - logGamma(x + 1.0))
            else -> Double.NaN
        }
    }

    fun interval(kind: DistributionKind, a: Double, b: Double, p: DistributionParameters): Double {
        if (!valid(kind, p) || !a.isFinite() || !b.isFinite()) return Double.NaN
        val lo = min(a, b); val hi = max(a, b)
        return when (kind) {
            DistributionKind.Binomial -> { val first=ceil(lo).toInt();val last=floor(hi).toInt();if(last<first)0.0 else (cdf(kind,last.toDouble(),p)-cdf(kind,first-1.0,p)).coerceIn(0.0,1.0) }
            DistributionKind.Poisson -> {val first=ceil(lo);val last=floor(hi);if(last<first)0.0 else (cdf(kind,last,p)-cdf(kind,first-1,p)).coerceIn(0.0,1.0)}
            else -> (cdf(kind, hi, p) - cdf(kind, lo, p)).coerceIn(0.0, 1.0)
        }
    }

    fun mean(kind: DistributionKind, p: DistributionParameters): Double = when (kind) {
        DistributionKind.Normal -> p.first; DistributionKind.Uniform -> (p.first + p.second) / 2; DistributionKind.Exponential -> 1 / p.first
        DistributionKind.Binomial -> p.trials * p.probability; DistributionKind.Poisson -> p.first
    }
    fun variance(kind: DistributionKind, p: DistributionParameters): Double = when (kind) {
        DistributionKind.Normal -> p.second * p.second; DistributionKind.Uniform -> (p.second - p.first).pow(2) / 12; DistributionKind.Exponential -> 1 / p.first.pow(2)
        DistributionKind.Binomial -> p.trials * p.probability * (1 - p.probability); DistributionKind.Poisson -> p.first
    }
    fun support(kind: DistributionKind, p: DistributionParameters): Pair<Double, Double> = when (kind) {
        DistributionKind.Normal -> p.first - 4.5 * p.second to p.first + 4.5 * p.second
        DistributionKind.Uniform -> p.first to p.second
        DistributionKind.Exponential -> 0.0 to max(8 / p.first, 1e-9)
        DistributionKind.Binomial -> if(p.trials<=500) 0.0 to p.trials.toDouble() else max(0.0,mean(kind,p)-5*sqrt(variance(kind,p))) to min(p.trials.toDouble(),mean(kind,p)+5*sqrt(variance(kind,p)))
        DistributionKind.Poisson -> max(0.0,p.first-5*sqrt(p.first)) to max(p.first+5*sqrt(p.first),1.0)
    }

    private fun logChoose(n: Int, k: Int) = logGamma(n + 1.0) - logGamma(k + 1.0) - logGamma(n - k + 1.0)
    private fun binomialCdf(k:Int,n:Int,p:Double):Double {
        if(p==0.0)return 1.0;if(p==1.0)return if(k>=n)1.0 else 0.0
        return regularizedBeta(1-p,n-k.toDouble(),k+1.0)
    }

    private fun regularizedBeta(x:Double,a:Double,b:Double):Double {
        if(x<=0)return 0.0;if(x>=1)return 1.0
        val front=exp(logGamma(a+b)-logGamma(a)-logGamma(b)+a*ln(x)+b*ln1p(-x))
        return if(x<(a+1)/(a+b+2)) (front*betaFraction(a,b,x)/a).coerceIn(0.0,1.0)
        else (1-front*betaFraction(b,a,1-x)/b).coerceIn(0.0,1.0)
    }
    private fun betaFraction(a:Double,b:Double,x:Double):Double {
        val tiny=1e-300;val eps=2e-14;val qab=a+b;val qap=a+1;val qam=a-1
        var c=1.0;var d=1-qab*x/qap;if(abs(d)<tiny)d=tiny;d=1/d;var h=d
        for(m in 1..10000){val m2=2*m;var aa=m*(b-m)*x/((qam+m2)*(a+m2));d=1+aa*d;if(abs(d)<tiny)d=tiny;c=1+aa/c;if(abs(c)<tiny)c=tiny;d=1/d;h*=d*c
            aa=-(a+m)*(qab+m)*x/((a+m2)*(qap+m2));d=1+aa*d;if(abs(d)<tiny)d=tiny;c=1+aa/c;if(abs(c)<tiny)c=tiny;d=1/d;val delta=d*c;h*=delta;if(abs(delta-1)<eps)return h}
        return Double.NaN
    }
    private fun poissonCdf(k:Double,lambda:Double):Double = regularizedGammaQ(k+1.0,lambda).coerceIn(0.0,1.0)

    // Regularized upper incomplete gamma; continued fraction/series give fast tails even for large λ.
    private fun regularizedGammaQ(a:Double,x:Double):Double {
        if(x<=0)return 1.0
        val gln=logGamma(a);val eps=2e-14;val tiny=1e-300
        if(x<a+1){var ap=a;var sum=1/a;var delta=sum;repeat(100000){ap++;delta*=x/ap;sum+=delta;if(abs(delta)<abs(sum)*eps)return (1-sum*exp(-x+a*ln(x)-gln)).coerceIn(0.0,1.0)};return Double.NaN}
        var b=x+1-a;var c=1/tiny;var d=1/b;var h=d
        for(i in 1..100000){val an=-i*(i-a);b+=2;d=an*d+b;if(abs(d)<tiny)d=tiny;c=b+an/c;if(abs(c)<tiny)c=tiny;d=1/d;val delta=d*c;h*=delta;if(abs(delta-1)<eps)return (exp(-x+a*ln(x)-gln)*h).coerceIn(0.0,1.0)}
        return Double.NaN
    }
    private fun logGamma(z: Double): Double {
        val c = doubleArrayOf(676.5203681218851, -1259.1392167224028, 771.32342877765313, -176.61502916214059, 12.507343278686905, -0.13857109526572012, 9.9843695780195716e-6, 1.5056327351493116e-7)
        if (z < .5) return ln(PI) - ln(abs(sin(PI * z))) - logGamma(1 - z)
        val q = z - 1; var x = .99999999999980993
        c.forEachIndexed { i, v -> x += v / (q + i + 1) }
        val t = q + 7.5
        return .5 * ln(2 * PI) + (q + .5) * ln(t) - t + ln(x)
    }

    // High-accuracy complementary error function (Numerical Recipes rational approximation).
    private fun erfc(x: Double): Double {
        val z = abs(x); val t = 1 / (1 + .5 * z)
        val ans = t * exp(-z*z - 1.26551223 + t*(1.00002368 + t*(.37409196 + t*(.09678418 + t*(-.18628806 + t*(.27886807 + t*(-1.13520398 + t*(1.48851587 + t*(-.82215223 + t*.17087277)))))))))
        return if (x >= 0) ans else 2 - ans
    }
}
