package com.vitals.mobile.feature.consultations.video

import android.os.SystemClock
import org.webrtc.AudioTrack
import org.webrtc.AudioTrackSink
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

class SpeakingMonitor(
    private val onSpeaking: (Boolean) -> Unit,
) : AudioTrackSink {
    @Volatile private var lastEmitted = false
    private var lastSpokeAt = 0L

    override fun onData(
        audioData: ByteBuffer,
        bitsPerSample: Int,
        sampleRate: Int,
        numberOfChannels: Int,
        numberOfFrames: Int,
        absoluteCaptureTimestampMs: Long,
    ) {
        if (bitsPerSample != 16) return
        val copy = audioData.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        var sum = 0.0
        var samples = 0
        while (copy.remaining() >= 2) {
            val sample = copy.short.toInt()
            sum += sample * sample
            samples += 1
        }
        if (samples == 0) return
        val rms = sqrt(sum / samples)
        val now = SystemClock.elapsedRealtime()
        val speaking = if (rms > 650) {
            lastSpokeAt = now
            true
        } else {
            now - lastSpokeAt < 220L
        }
        if (speaking != lastEmitted) {
            lastEmitted = speaking
            onSpeaking(speaking)
        }
    }

    fun attach(track: AudioTrack?) {
        track?.addSink(this)
    }

    fun detach(track: AudioTrack?) {
        runCatching { track?.removeSink(this) }
        reset()
    }

    fun reset() {
        if (lastEmitted) {
            lastEmitted = false
            onSpeaking(false)
        }
        lastSpokeAt = 0L
    }
}
