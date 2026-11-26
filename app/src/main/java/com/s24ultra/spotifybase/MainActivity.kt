package com.s24ultra.spotifybase

import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.StatFs
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var streamInput: EditText
    private lateinit var statusView: TextView
    private lateinit var seekBar: SeekBar
    private lateinit var telemetryView: TextView

    private var mediaPlayer: MediaPlayer? = null
    private var isPrepared = false
    private val uiHandler = Handler(Looper.getMainLooper())

    private val progressRunnable = object : Runnable {
        override fun run() {
            val mp = mediaPlayer
            if (mp != null && isPrepared) {
                try {
                    if (mp.isPlaying) {
                        val dur = mp.duration.coerceAtLeast(1)
                        val pos = mp.currentPosition.coerceIn(0, dur)
                        seekBar.max = dur
                        seekBar.progress = pos
                        statusView.text = "Odtwarzanie: ${formatMs(pos)} / ${formatMs(dur)}"
                    }
                } catch (_: IllegalStateException) {
                }
            }
            uiHandler.postDelayed(this, 500L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 84, 48, 48)
        }

        val playerHeader = TextView(this).apply {
            text = "SPOTIFY S24 ULTRA — AUDIO & STREAM PLAYER"
            textSize = 16f
            setPadding(0, 0, 0, 16)
        }

        streamInput = EditText(this).apply {
            hint = "URL strumienia audio (.mp3/.aac/.m3u8) lub link Spotify"
            setText("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3")
            textSize = 14f
        }

        val controlsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 16, 0, 8)
        }

        val playBtn = Button(this).apply {
            text = "PLAY"
            setOnClickListener { startStream(streamInput.text?.toString()?.trim().orEmpty()) }
        }

        val pauseBtn = Button(this).apply {
            text = "PAUSE / RESUME"
            setOnClickListener { togglePause() }
        }

        val stopBtn = Button(this).apply {
            text = "STOP"
            setOnClickListener { stopStream() }
        }

        controlsRow.addView(playBtn)
        controlsRow.addView(pauseBtn)
        controlsRow.addView(stopBtn)

        seekBar = SeekBar(this).apply {
            setPadding(0, 12, 0, 12)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser && isPrepared) {
                        try {
                            mediaPlayer?.seekTo(progress)
                        } catch (_: IllegalStateException) {
                        }
                    }
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }

        statusView = TextView(this).apply {
            text = "Status odtwarzacza: GOTOWY"
            textSize = 14f
            setPadding(0, 0, 0, 24)
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
            textSize = 14f
            setPadding(0, 28, 0, 0)
        }

        root.addView(playerHeader)
        root.addView(streamInput)
        root.addView(controlsRow)
        root.addView(seekBar)
        root.addView(statusView)
        root.addView(refreshBtn)
        root.addView(shareBtn)
        root.addView(telemetryView)

        val scroll = ScrollView(this).apply {
            addView(root)
        }

        setContentView(scroll)
        handleIncomingIntent(intent)
        updateTelemetry(intent)
        uiHandler.post(progressRunnable)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
        updateTelemetry(intent)
    }

    private fun handleIncomingIntent(activeIntent: Intent?) {
        val dataStr = activeIntent?.dataString
        val extraText = activeIntent?.getStringExtra(Intent.EXTRA_TEXT)
        val candidate = when {
            !dataStr.isNullOrBlank() -> dataStr
            !extraText.isNullOrBlank() -> extraText
            else -> null
        }
        if (candidate != null) {
            streamInput.setText(candidate)
            if (candidate.startsWith("http://") || candidate.startsWith("https://") || candidate.startsWith("content://") || candidate.startsWith("file://")) {
                startStream(candidate)
            } else {
                statusView.text = "Odebrano link/tekst: $candidate (wymaga bezposredniego strumienia audio lub klienta Spotify)"
            }
        }
    }

    private fun startStream(rawUrl: String) {
        if (rawUrl.isEmpty()) {
            statusView.text = "Blad: pusty adres URL strumienia"
            return
        }
        if (rawUrl.contains("open.spotify.com") || rawUrl.startsWith("spotify:")) {
            statusView.text = "Wykryto link Spotify ($rawUrl): bezposredni strumien audio wymaga URL pliku/streamu audio lub oficjalnego SDK Spotify"
            return
        }
        releasePlayer()
        statusView.text = "Buforowanie strumienia: $rawUrl"
        try {
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(this@MainActivity, Uri.parse(rawUrl))
                setOnPreparedListener { player ->
                    isPrepared = true
                    seekBar.max = player.duration.coerceAtLeast(1)
                    player.start()
                    statusView.text = "Odtwarzanie uruchomione (${formatMs(player.duration)})"
                }
                setOnCompletionListener {
                    statusView.text = "Odtwarzanie zakonczone"
                    seekBar.progress = 0
                }
                setOnErrorListener { _, what, extra ->
                    isPrepared = false
                    statusView.text = "Blad MediaPlayer (what=$what, extra=$extra)"
                    true
                }
                prepareAsync()
            }
            mediaPlayer = mp
        } catch (e: Exception) {
            isPrepared = false
            statusView.text = "Wyjatek uruchamiania strumienia: ${e.message}"
        }
    }

    private fun togglePause() {
        val mp = mediaPlayer ?: run {
            statusView.text = "Brak aktywnego strumienia — kliknij PLAY"
            return
        }
        if (!isPrepared) {
            statusView.text = "Strumien jeszcze nie gotowy"
            return
        }
        try {
            if (mp.isPlaying) {
                mp.pause()
                statusView.text = "Wstrzymano (PAUSE)"
            } else {
                mp.start()
                statusView.text = "Wznowiono odtwarzanie"
            }
        } catch (e: IllegalStateException) {
            statusView.text = "Blad stanu odtwarzacza: ${e.message}"
        }
    }

    private fun stopStream() {
        releasePlayer()
        seekBar.progress = 0
        statusView.text = "Zatrzymano (STOP)"
    }

    private fun releasePlayer() {
        isPrepared = false
        mediaPlayer?.let {
            try {
                if (it.isPlaying) it.stop()
            } catch (_: IllegalStateException) {
            }
            it.release()
        }
        mediaPlayer = null
    }

    override fun onDestroy() {
        uiHandler.removeCallbacks(progressRunnable)
        releasePlayer()
        super.onDestroy()
    }

    private fun formatMs(ms: Int): String {
        val totalSec = (ms / 1000).coerceAtLeast(0)
        val min = totalSec / 60
        val sec = totalSec % 60
        return String.format(Locale.US, "%02d:%02d", min, sec)
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
            appendLine("=== SPOTIFY S24 ULTRA PLAYER v1.2.0 ===")
            appendLine("Timestamp: $now")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})")
            appendLine("Android SDK: ${Build.VERSION.SDK_INT} | ABI: ${Build.SUPPORTED_ABIS.joinToString()}")
            appendLine("Storage free: ${String.format(Locale.US, "%.2f", freeGb)} GB / ${String.format(Locale.US, "%.2f", totalGb)} GB")
            appendLine("Network: $netSummary")
            appendLine("Intent action: $intentAction")
            appendLine("Intent data: $intentData")
            appendLine("Intent text: $intentExtraText")
            appendLine("Player active: ${mediaPlayer != null} | prepared: $isPrepared")
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
