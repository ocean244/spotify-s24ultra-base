package com.s24ultra.spotifybase

import android.content.Intent
import android.os.Bundle
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var telemetryRepo: HardwareTelemetryRepository
    private lateinit var audioRepo: EnhancedAudioSessionRepository
    private lateinit var outputView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        telemetryRepo = HardwareTelemetryRepository(this)
        audioRepo = EnhancedAudioSessionRepository(this)

        val scrollView = ScrollView(this)
        outputView = TextView(this).apply {
            textSize = 14f
            setPadding(48, 96, 48, 96)
        }
        scrollView.addView(outputView)
        setContentView(scrollView)

        val serviceIntent = Intent(this, AudioSessionMonitorService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)

        renderDiagnostics()
    }

    override fun onResume() {
        super.onResume()
        renderDiagnostics()
    }

    private fun renderDiagnostics() {
        val snap = telemetryRepo.collectSnapshot()
        val audio = audioRepo.inspectAudioRoutes()
        outputView.text = buildString {
            appendLine("=== SPOTIFY S24 ULTRA BASE (v1.2.0-s24ultra-e3q) ===")
            appendLine("Model / Device  : ${snap.model} (${snap.device})")
            appendLine("Android SDK     : ${snap.sdkInt}")
            appendLine("ABIs            : ${snap.abis}")
            appendLine("Bateria         : ${snap.batteryPercent}% (ładowanie: ${snap.isCharging})")
            appendLine("RAM dostępny    : ${snap.availableRamMb} / ${snap.totalRamMb} MB")
            appendLine("Wolne /data     : ${snap.freeStorageMb} MB")
            appendLine("Sieć aktywna    : ${snap.activeNetworkType}")
            appendLine("Głośność media  : ${snap.mediaVolumePercent}%")
            appendLine("Spatial Audio   : ${audio.spatialAudioAvailable}")
            appendLine("Direct Playback : ${audio.directPlaybackSupported}")
            appendLine("Profil sesji    : ${audio.sessionHintsSummary}")
            appendLine("Wyjścia audio   : ${audio.activeOutputs.joinToString(" | ")}")
        }
    }
}
