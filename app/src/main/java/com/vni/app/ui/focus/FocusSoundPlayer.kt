package com.vni.app.ui.focus

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.sin

/**
 * Native synthesizer for 3 epic Iron Man sci-fi sound effects:
 * 1. Start: Iron Man repulsor / arc reactor power-up sweep.
 * 2. Pause / Break: Powering down hum with gentle steam release.
 * 3. Stop: Multi-tone reactor shutdown chime.
 * Works 100% offline without requiring any external mp3 files.
 */
object FocusSoundPlayer {
    private const val SAMPLE_RATE = 44100

    fun playStartSound() {
        thread(start = true, isDaemon = true) {
            try {
                val durationMs = 1100
                val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
                val buffer = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / numSamples // 0.0 to 1.0
                    // Upward sweep from 130 Hz to 860 Hz with accelerating curve
                    val freq = 130.0 + 730.0 * (t * t)
                    val phase = 2.0 * PI * freq * (i.toDouble() / SAMPLE_RATE)
                    // Core sine + first & second harmonics for rich repulsor sound
                    val sample = 0.55 * sin(phase) + 0.30 * sin(phase * 2.0) + 0.15 * sin(phase * 3.0)
                    // Smooth envelope
                    val envelope = when {
                        t < 0.08 -> t / 0.08
                        t > 0.82 -> (1.0 - t) / 0.18
                        else -> 1.0
                    }
                    buffer[i] = (sample * envelope * Short.MAX_VALUE * 0.72).toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
                playBuffer(buffer)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playPauseBreakSound() {
        thread(start = true, isDaemon = true) {
            try {
                val durationMs = 850
                val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
                val buffer = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / numSamples
                    // Descending sweep from 680 Hz to 110 Hz
                    val freq = 680.0 * (1.0 - t * 0.84)
                    val phase = 2.0 * PI * freq * (i.toDouble() / SAMPLE_RATE)
                    // Add steam valve hiss
                    val noise = (Math.random() * 2.0 - 1.0) * 0.16 * (1.0 - t)
                    val sample = 0.58 * sin(phase) + 0.26 * sin(phase * 2.0) + noise
                    val envelope = (1.0 - t)
                    buffer[i] = (sample * envelope * Short.MAX_VALUE * 0.68).toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
                playBuffer(buffer)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playStopSound() {
        thread(start = true, isDaemon = true) {
            try {
                val durationMs = 950
                val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
                val buffer = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / numSamples
                    // Three-stage power down chord: 660 Hz -> 440 Hz -> 220 Hz
                    val freq = when {
                        t < 0.28 -> 660.0
                        t < 0.60 -> 440.0
                        else -> 220.0
                    }
                    val phase = 2.0 * PI * freq * (i.toDouble() / SAMPLE_RATE)
                    val sample = 0.65 * sin(phase) + 0.25 * sin(phase * 2.0)
                    val envelope = (1.0 - t * t)
                    buffer[i] = (sample * envelope * Short.MAX_VALUE * 0.70).toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
                playBuffer(buffer)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun playBuffer(buffer: ShortArray) {
        var track: AudioTrack? = null
        try {
            track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(buffer, 0, buffer.size)
            track.play()
            Thread.sleep((buffer.size.toDouble() / SAMPLE_RATE * 1000).toLong() + 40)
            track.stop()
        } finally {
            track?.release()
        }
    }
}
