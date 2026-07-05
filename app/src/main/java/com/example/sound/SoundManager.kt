package com.example.sound

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

class SoundManager(private val isSoundEnabledProvider: () -> Boolean) {

    private val scope = CoroutineScope(Dispatchers.Default)

    fun playClick() {
        if (!isSoundEnabledProvider()) return
        scope.launch {
            generateAndPlayTone(950.0, 40)
        }
    }

    fun playSuccess() {
        if (!isSoundEnabledProvider()) return
        scope.launch {
            generateAndPlayTone(600.0, 80)
            generateAndPlayTone(900.0, 100)
        }
    }

    fun playFailure() {
        if (!isSoundEnabledProvider()) return
        scope.launch {
            generateAndPlayTone(200.0, 200, squareWave = true)
        }
    }

    fun playLevelUp() {
        if (!isSoundEnabledProvider()) return
        scope.launch {
            generateAndPlayTone(523.25, 80)  // C5
            generateAndPlayTone(659.25, 80)  // E5
            generateAndPlayTone(783.99, 80)  // G5
            generateAndPlayTone(1046.50, 180) // C6
        }
    }

    private fun generateAndPlayTone(frequency: Double, durationMs: Int, squareWave: Boolean = false) {
        val sampleRate = 22050 // Lower sample rate to conserve memory and speed up generation
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val generatedSnd = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val angle = 2.0 * Math.PI * frequency * t
            val value = if (squareWave) {
                if (sin(angle) >= 0) 3000 else -3000
            } else {
                (sin(angle) * 7000).toInt()
            }
            generatedSnd[i] = value.toShort()
        }

        try {
            val audioTrack = AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                numSamples * 2,
                AudioTrack.MODE_STATIC
            )
            audioTrack.write(generatedSnd, 0, numSamples)
            audioTrack.play()
            Thread.sleep(durationMs.toLong() + 30L)
            audioTrack.stop()
            audioTrack.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
