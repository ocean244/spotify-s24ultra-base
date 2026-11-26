package com.s24ultra.spotifybase

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs

data class DeviceTelemetrySnapshot(
    val model: String,
    val device: String,
    val sdkInt: Int,
    val abis: String,
    val batteryPercent: Int,
    val isCharging: Boolean,
    val availableRamMb: Long,
    val totalRamMb: Long,
    val freeStorageMb: Long,
    val activeNetworkType: String,
    val mediaVolumePercent: Int
)

class HardwareTelemetryRepository(private val context: Context) {

    fun collectSnapshot(): DeviceTelemetrySnapshot {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val batteryPercent = if (level >= 0 && scale > 0) (level * 100) / scale else -1
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        val actMgr = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actMgr.getMemoryInfo(memInfo)
        val availRamMb = memInfo.availMem / (1024L * 1024L)
        val totalRamMb = memInfo.totalMem / (1024L * 1024L)

        val statFs = StatFs(Environment.getDataDirectory().path)
        val freeStorageMb = statFs.availableBytes / (1024L * 1024L)

        val connMgr = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNet = connMgr.activeNetwork
        val caps = if (activeNet != null) connMgr.getNetworkCapabilities(activeNet) else null
        val netType = when {
            caps == null -> "OFFLINE"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
            else -> "OTHER"
        }

        val audioMgr = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val curVol = audioMgr.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVol = audioMgr.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val volPercent = (curVol * 100) / maxVol

        return DeviceTelemetrySnapshot(
            model = Build.MODEL ?: "UNKNOWN",
            device = Build.DEVICE ?: "UNKNOWN",
            sdkInt = Build.VERSION.SDK_INT,
            abis = Build.SUPPORTED_ABIS.joinToString(", "),
            batteryPercent = batteryPercent,
            isCharging = isCharging,
            availableRamMb = availRamMb,
            totalRamMb = totalRamMb,
            freeStorageMb = freeStorageMb,
            activeNetworkType = netType,
            mediaVolumePercent = volPercent
        )
    }
}
