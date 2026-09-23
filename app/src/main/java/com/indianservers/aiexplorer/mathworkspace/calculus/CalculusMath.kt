package com.indianservers.aiexplorer.mathworkspace.calculus

import kotlin.math.*

object CalculusMath {
    fun derivative(f: (Double)->Double, x:Double):Double {
        val h=max(abs(x)*1e-4,1e-6)
        val f1=f(x-h);val f2=f(x+h);val f0=f(x);val fm2=f(x-2*h);val fp2=f(x+2*h)
        if(listOf(f1,f2,f0,fm2,fp2).any{!it.isFinite()})return Double.NaN
        val d5=(fm2-8*f1+8*f2-fp2)/(12*h)
        val d3=(f2-f1)/(2*h)
        return if(d5.isFinite() && abs(d5-d3) <= 1e-3*(1+abs(d5))) d5 else d3
    }

    fun integrate(f:(Double)->Double,a:Double,b:Double,tolerance:Double=1e-9):Double {
        if(!a.isFinite()||!b.isFinite())return Double.NaN
        if(a==b)return 0.0
        val sign=if(b>=a)1.0 else -1.0;val lo=min(a,b);val hi=max(a,b)
        fun eval(x:Double)=f(x).takeIf{it.isFinite()}
        val fa=eval(lo)?:return Double.NaN;val fb=eval(hi)?:return Double.NaN;val m=(lo+hi)/2;val fm=eval(m)?:return Double.NaN
        val whole=(hi-lo)*(fa+4*fm+fb)/6
        fun recurse(l:Double,r:Double,fl:Double,fc:Double,fr:Double,est:Double,tol:Double,depth:Int):Double {
            val c=(l+r)/2;val lm=(l+c)/2;val rm=(c+r)/2;val fLm=eval(lm)?:return Double.NaN;val fRm=eval(rm)?:return Double.NaN
            val left=(c-l)*(fl+4*fLm+fc)/6;val right=(r-c)*(fc+4*fRm+fr)/6;val delta=left+right-est
            if(depth<=0)return if(abs(delta)<tol*50) left+right+delta/15 else Double.NaN
            if(abs(delta)<=15*tol)return left+right+delta/15
            val lval=recurse(l,c,fl,fLm,fc,left,tol/2,depth-1);val rval=recurse(c,r,fc,fRm,fr,right,tol/2,depth-1)
            return if(lval.isFinite()&&rval.isFinite())lval+rval else Double.NaN
        }
        return sign*recurse(lo,hi,fa,fm,fb,whole,tolerance*(1+abs(whole)),22)
    }

    fun limit(f:(Double)->Double, at:Double):LimitEstimate {
        if(!at.isFinite())return LimitEstimate(Double.NaN,Double.NaN,null,false)
        val scales=doubleArrayOf(1e-2,1e-3,1e-4,1e-5,1e-6,1e-7,1e-8)
        fun side(sign:Double):Double {
            val values=scales.map { e -> f(at+sign*e*max(1.0,abs(at))) }
            val tail=values.takeLast(3)
            if(tail.any{!it.isFinite()})return tail.last()
            return if(abs(tail[2]-tail[1])<abs(tail[1]-tail[0])) tail.average() else tail.last()
        }
        val left=side(-1.0);val right=side(1.0)
        val finite=left.isFinite()&&right.isFinite()
        val agrees=finite&&abs(left-right)<=1e-4*(1+max(abs(left),abs(right)))
        return LimitEstimate(left,right,if(agrees)(left+right)/2 else null,agrees)
    }
}
