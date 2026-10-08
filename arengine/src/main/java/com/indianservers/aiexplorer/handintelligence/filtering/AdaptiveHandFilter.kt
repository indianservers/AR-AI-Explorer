package com.indianservers.aiexplorer.handintelligence.filtering
import com.indianservers.aiexplorer.handintelligence.*
import kotlin.math.*

class OneEuroFilter(private val minimumCutoff:Double=1.8,private val beta:Double=8.0) {
    private var time:Long=-1; private var value=0.0; private var derivative=0.0; private var raw=0.0
    fun filter(input:Double,timestamp:Long,precision:Boolean=false,quality:Float=1f):Double {
        if(time<0 || timestamp-time>350) { time=timestamp; value=input; raw=input; derivative=0.0; return input }
        if(timestamp<=time) return value
        val dt=(timestamp-time)/1000.0
        fun alpha(cutoff:Double)=1.0/(1.0+1.0/(2*PI*cutoff*dt))
        derivative+=alpha(1.0)*((input-raw)/dt-derivative)
        val cutoff=(if(precision) minimumCutoff*.65 else minimumCutoff)*(.6+.4*quality)+beta*abs(derivative)
        value+=alpha(cutoff)*(input-value); raw=input; time=timestamp; return value
    }
    fun reset() { time=-1 }
}
class AdaptiveHandFilter {
    private val axes=Array(3) { OneEuroFilter() }
    fun filter(p:Vec3,t:Long,precision:Boolean,quality:Float)=Vec3(axes[0].filter(p.x,t,precision,quality),axes[1].filter(p.y,t,precision,quality),axes[2].filter(p.z,t,precision,quality))
    fun reset()=axes.forEach { it.reset() }
}
enum class JitterKind { TRACKING_JITTER, PRECISION_MOVEMENT, NORMAL, FAST }
class JitterClassifier {
    fun classify(m:TemporalFeatures,quality:Float):JitterKind=when {
        m.velocity.magnitude()>.8 -> JitterKind.FAST
        m.velocity.magnitude()<.15 && m.trajectoryCurvature>.6 && m.motionConsistency<.3 && quality<.8 -> JitterKind.TRACKING_JITTER
        m.velocity.magnitude()<.15 && m.motionConsistency>.6 -> JitterKind.PRECISION_MOVEMENT
        else -> JitterKind.NORMAL
    }
}
