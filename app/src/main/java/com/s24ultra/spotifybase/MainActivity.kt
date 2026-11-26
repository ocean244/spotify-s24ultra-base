package com.s24ultra.spotifybase

import android.os.Build
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val statusView = TextView(this).apply {
            text = buildString {
                appendLine("Spotify S24 Ultra Base")
                appendLine("Model: ${Build.MODEL} (${Build.DEVICE})")
                appendLine("SDK: ${Build.VERSION.SDK_INT}")
                appendLine("ABIs: ${Build.SUPPORTED_ABIS.joinToString()}")
            }
            textSize = 16f
            setPadding(48, 96, 48, 48)
        }
        setContentView(statusView)
    }
}
