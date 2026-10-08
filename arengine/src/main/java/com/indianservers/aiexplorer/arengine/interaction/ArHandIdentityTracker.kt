package com.indianservers.aiexplorer.arengine.interaction
import com.indianservers.aiexplorer.arengine.contract.ArVector2
import kotlin.math.hypot

/** Global assignment against predicted trajectories survives detector ordering and short occlusion. */
class ArHandIdentityTracker {
    private data class Track(val hand:ArTrackedHand,val velocity:ArVector2,val time:Long)
    private val tracks=linkedMapOf<String,Track>(); private var nextId=0
    fun assign(hands:List<ArTrackedHand>,time:Long):List<ArTrackedHand> {
        tracks.entries.removeAll { time-it.value.time>350 }
        val input=hands.take(2); val old=tracks.values.toList()
        fun cost(hand:ArTrackedHand,track:Track):Double {
            val p=hand.landmarks.firstOrNull() ?: return 100.0; val q=track.hand.landmarks.firstOrNull() ?: return 100.0
            val dt=((time-track.time)/1000f).coerceIn(0f,.2f)
            val distance=hypot((p.x-q.x-track.velocity.x*dt).toDouble(),(p.y-q.y-track.velocity.y*dt).toDouble())
            return if(distance>.35) 100.0 else distance+if(hand.handedness!=track.hand.handedness) .025 else 0.0
        }
        var best=Double.MAX_VALUE; var assignment=List(input.size) { -1 }
        fun search(index:Int,used:Set<Int>,chosen:List<Int>,total:Double) {
            if(index==input.size) { if(total<best) { best=total; assignment=chosen }; return }
            search(index+1,used,chosen+(-1),total+.4)
            old.indices.filter { it !in used }.forEach { j -> search(index+1,used+j,chosen+j,total+cost(input[index],old[j])) }
        }
        search(0,emptySet(),emptyList(),0.0)
        return input.mapIndexed { i,h ->
            val previous=assignment[i].takeIf { it>=0 }?.let(old::get)
            val id=previous?.hand?.id ?: "tracked-${nextId++}"; val assigned=h.copy(id=id)
            val p=h.landmarks.firstOrNull();val q=previous?.hand?.landmarks?.firstOrNull();val dt=previous?.let { (time-it.time)/1000f } ?: 0f
            val velocity=if(p!=null && q!=null && dt>0) ArVector2((p.x-q.x)/dt,(p.y-q.y)/dt) else ArVector2(0f,0f)
            tracks[id]=Track(assigned,velocity,time); assigned
        }
    }
}
