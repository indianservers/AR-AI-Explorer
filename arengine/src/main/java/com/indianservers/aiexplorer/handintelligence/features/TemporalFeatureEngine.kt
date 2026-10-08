package com.indianservers.aiexplorer.handintelligence.features
import com.indianservers.aiexplorer.handintelligence.*
import com.indianservers.aiexplorer.handintelligence.filtering.*
import kotlin.math.*

/** Ninety retained samples per identity, never an unbounded landmark history. */
class TemporalFeatureEngine(private val capacity:Int=60) {
    private class History(capacity:Int) { val positions=arrayOfNulls<Vec3>(capacity); val angles=DoubleArray(capacity); val pinches=DoubleArray(capacity); val times=LongArray(capacity); var cursor=0; var count=0; var last:TemporalFeatures=TemporalFeatures(); val filter=AdaptiveHandFilter() }
    private val histories=LinkedHashMap<String,History>()
    fun process(features:List<HandPoseFeatures>,time:Long,precision:Boolean):List<IntelligentHand> {
        histories.entries.removeAll { (_,h) -> h.count>0 && time-h.times[(h.cursor-1+h.times.size)%h.times.size]>1000 }
        return features.map { p ->
            val h=histories.getOrPut(p.id) { History(capacity) }; val prev=(h.cursor-1+capacity)%capacity
            val dt=if(h.count==0) 0.0 else (time-h.times[prev])/1000.0
            if(dt<=0 || dt>.35) { h.count=0; h.last=TemporalFeatures(); h.filter.reset() }
            val v=if(h.count==0) Vec3.Zero else (p.pinch-h.positions[prev]!!)*(1.0/dt)
            val a=if(h.count<2) Vec3.Zero else (v-h.last.velocity)*(1.0/dt)
            val jerk=if(h.count<3) Vec3.Zero else (a-h.last.acceleration)*(1.0/dt)
            val consistency=if(v.magnitude()<.015 || h.last.velocity.magnitude()<.015) .8f else clamp((v.unit().dot(h.last.velocity.unit())+1)*.5)
            val stability=clamp(1-v.magnitude()*1.5-a.magnitude()*.02)
            val motion=TemporalFeatures(v,a,jerk,if(h.count==0) 0f else (angleDelta(p.angle,h.angles[prev])/dt).toFloat(),1-consistency,if(h.count==0) 0f else ((p.pinchDistance-h.pinches[prev])/dt).toFloat(),stability,consistency,if(h.count==0) 0 else time-h.times[(h.cursor-h.count+capacity)%capacity],h.count+1)
            h.positions[h.cursor]=p.pinch; h.times[h.cursor]=time; h.angles[h.cursor]=p.angle; h.pinches[h.cursor]=p.pinchDistance; h.cursor=(h.cursor+1)%capacity; h.count=(h.count+1).coerceAtMost(capacity); h.last=motion
            IntelligentHand(p,motion,h.filter.filter(p.pinch,time,precision,p.quality))
        }
    }
    fun reset()=histories.clear()
}
