package com.s24ultra.spotifybase

import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var telemetryView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 84, 48, 48)
        }

        val refreshBtn = Button(this).apply {
            text = "Odswiez telemetrie i stan srodowiska"
            setOnClickListener {
                updateTelemetry(intent)
            }
        }

        val shareBtn = Button(this).apply {
            text = "Udostepnij raport diagnostyczny"
            setOnClickListener {
                val reportText = telemetryView.text?.toString() ?: ""
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "SpotifyS24UltraBase Telemetry")
                    putExtra(Intent.EXTRA_TEXT, reportText)
                }
                startActivity(Intent.createChooser(sendIntent, "Udostepnij raport"))
            }
        }

        telemetryView = TextView(this).apply {
            textSize = 15f
            setPadding(0, 32, 0, 0)
        }

        root.addView(refreshBtn)
        root.addView(shareBtn)
        root.addView(telemetryView)

        val scroll = ScrollView(this).apply {
            addView(root)
        }

        setContentView(scroll)
        updateTelemetry(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        updateTelemetry(intent)
    }

    private fun updateTelemetry(activeIntent: Intent?) {
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val stat = StatFs(Environment.getDataDirectory().path)
        val freeBytes = stat.availableBytes
        val totalBytes = stat.totalBytes
        val freeGb = freeBytes / (1024.0 * 1024.0 * 1024.0)
        val totalGb = totalBytes / (1024.0 * 1024.0 * 1024.0)

        val netSummary = resolveNetworkState()
        val intentAction = activeIntent?.action ?: "NONE"
        val intentData = activeIntent?.dataString ?: "NONE"
        val intentExtraText = activeIntent?.getStringExtra(Intent.EXTRA_TEXT) ?: "NONE"

        telemetryView.text = buildString {
            appendLine("=== SPOTIFY S24 ULTRA BASE v1.1.0 ===")
            appendLine("Timestamp: $now")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})")
            appendLine("Android SDK: ${Build.VERSION.SDK_INT} | ABI: ${Build.SUPPORTED_ABIS.joinToString()}")
            appendLine("Storage free: ${String.format(Locale.US, "%.2f", freeGb)} GB / ${String.format(Locale.US, "%.2f", totalGb)} GB")
            appendLine("Network: $netSummary")
            appendLine("Intent action: $intentAction")
            appendLine("Intent data: $intentData")
            appendLine("Intent text: $intentExtraText")
            appendLine("Pipeline status: Signed release/debug APK ready")
        }
    }

    private fun resolveNetworkState(): String {
        val cm = getSystemService(ConnectivityManager::class.java) ?: return "UNAVAILABLE"
        val activeNetwork = cm.activeNetwork ?: return "DISCONNECTED"
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return "NO_CAPABILITIES"
        val transports = buildList {
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) add("WIFI")
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) add("CELLULAR")
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) add("ETHERNET")
        }
        val validated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        val transportLabel = if (transports.isEmpty()) "OTHER" else transports.joinToString("+")
        return "CONNECTED ($transportLabel, validated=$validated)"
    }
}
