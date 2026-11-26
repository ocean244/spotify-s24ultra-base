package com.s24ultra.spotifybase

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build

data class AudioRouteDiagnostics(
    val activeOutputs: List<String>,
    val spatialAudioAvailable: Boolean,
    val directPlaybackSupported: Boolean,
    val sessionHintsSummary: String
)

class EnhancedAudioSessionRepository(private val context: Context) {

    fun inspectAudioRoutes(): AudioRouteDiagnostics {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val outputs = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            for (dev in devices) {
                val label = buildString {
                    append(dev.productName?.toString() ?: "AudioDevice")
                    append(" [type=")
                    append(dev.type)
                    append(", id=")
                    append(dev.id)
                    append("]")
                }
                outputs.add(label)
            }
        }

        val spatialOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            audioManager.spatializer.isAvailable &&
                audioManager.spatializer.canBeSpatialized(attrs, audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).firstOrNull()?.let {
                    android.media.AudioFormat.Builder()
                        .setSampleRate(48000)
                        .setEncoding(android.media.AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(android.media.AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                } ?: android.media.AudioFormat.Builder()
                    .setSampleRate(48000)
                    .setEncoding(android.media.AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(android.media.AudioFormat.CHANNEL_OUT_STEREO)
                    .build())
        } else {
            false
        }

        val directOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            val fmt = android.media.AudioFormat.Builder()
                .setSampleRate(48000)
                .setEncoding(android.media.AudioFormat.ENCODING_PCM_24BIT_PACKED)
                .setChannelMask(android.media.AudioFormat.CHANNEL_OUT_STEREO)
                .build()
            AudioManager.getDirectPlaybackSupport(fmt, attrs) != AudioManager.DIRECT_PLAYBACK_NOT_SUPPORTED
        } else {
            false
        }

        return AudioRouteDiagnostics(
            activeOutputs = if (outputs.isEmpty()) listOf("DEFAULT_MEDIA_OUT") else outputs,
            spatialAudioAvailable = spatialOk,
            directPlaybackSupported = directOk,
            sessionHintsSummary = "e3q-arm64-v8a-api36-low-latency-ready"
        )
    }
}
