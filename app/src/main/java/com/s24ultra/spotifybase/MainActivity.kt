package com.s24ultra.spotifybase

import android.content.Intent
import android.os.Bundle
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var telemetryRepo: HardwareTelemetryRepository
    private lateinit var outputView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        telemetryRepo = HardwareTelemetryRepository(this)
        val scrollView = ScrollView(this)
        outputView = TextView(this).apply {
            textSize = 15f
            setPadding(48, 96, 48, 96)
        }
        scrollView.addView(outputView)
        setContentView(scrollView)

        val serviceIntent = Intent(this, AudioSessionMonitorService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)

        renderTelemetry()
    }

    override fun onResume() {
        super.onResume()
        renderTelemetry()
    }

    private fun renderTelemetry() {
        val snap = telemetryRepo.collectSnapshot()
        outputView.text = buildString {
            appendLine("=== SPOTIFY S24 ULTRA BASE (v1.1.0-s24ultra-e3q) ===")
            appendLine("Model / Device : ${snap.model} (${snap.device})")
            appendLine("Android SDK    : ${snap.sdkInt}")
            appendLine("ABIs           : ${snap.abis}")
            appendLine("Bateria        : ${snap.batteryPercent}% (ładowanie: ${snap.isCharging})")
            appendLine("RAM dostępny   : ${snap.availableRamMb} / ${snap.totalRamMb} MB")
            appendLine("Wolne /data    : ${snap.freeStorageMb} MB")
            appendLine("Sieć aktywna   : ${snap.activeNetworkType}")
            appendLine("Głośność media : ${snap.mediaVolumePercent}%")
        }
    }
}
