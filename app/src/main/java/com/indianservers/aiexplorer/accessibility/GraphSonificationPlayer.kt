package com.indianservers.aiexplorer.accessibility

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.indianservers.aiexplorer.core.GraphAudioNote
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

/** Deterministic stereo PCM: pitch follows y, pan follows x, and POIs receive a longer accent. */
object GraphSonificationPcm {
    fun render(notes: List<GraphAudioNote>, sampleRate: Int = 22_050, durationSeconds: Double = 4.0): ShortArray {
        if (notes.isEmpty()) return ShortArray(0)
        val frames = (sampleRate * durationSeconds).toInt().coerceAtLeast(1)
        val output = ShortArray(frames * 2)
        notes.forEach { note ->
            val toneFrames = (sampleRate * if (note.emphasis) .105 else .045).toInt()
            val start = (note.time.coerceIn(0.0, 1.0) * (frames - toneFrames.coerceAtMost(frames))).toInt()
            val frequency = 440.0 * 2.0.pow((note.pitch - 69.0) / 12.0)
            val leftGain = ((1.0 - note.pan.coerceIn(-1.0, 1.0)) * .5)
            val rightGain = ((1.0 + note.pan.coerceIn(-1.0, 1.0)) * .5)
            repeat(toneFrames.coerceAtMost(frames - start)) { offset ->
                val envelope = 1.0 - offset.toDouble() / toneFrames.coerceAtLeast(1)
                val sample = sin(2.0 * PI * frequency * offset / sampleRate) * envelope * if (note.emphasis) .8 else .55
                val frame = (start + offset) * 2
                output[frame] = saturatingAdd(output[frame], sample * leftGain)
                output[frame + 1] = saturatingAdd(output[frame + 1], sample * rightGain)
            }
        }
        return output
    }

    private fun saturatingAdd(current: Short, value: Double): Short =
        (current.toInt() + (value * Short.MAX_VALUE).toInt()).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
}

class GraphSonificationPlayer : AutoCloseable {
    @Volatile private var active: AudioTrack? = null

    @Synchronized
    fun play(notes: List<GraphAudioNote>) {
        stop()
        val pcm = GraphSonificationPcm.render(notes)
        if (pcm.isEmpty()) return
        val track = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(22_050).setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build())
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(pcm.size * 2)
            .build()
        track.write(pcm, 0, pcm.size)
        active = track
        track.play()
    }

    @Synchronized
    fun stop() {
        active?.let { runCatching { it.stop() }; it.release() }
        active = null
    }

    override fun close() = stop()
}
