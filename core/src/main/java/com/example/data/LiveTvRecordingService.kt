package com.example.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.content.ContentValues
import android.provider.MediaStore
import android.net.Uri
import java.io.OutputStream
import androidx.core.app.NotificationCompat
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class LiveTvRecordingService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var recordingJob: Job? = null
    private var isRecording = false
    private var outputFile: File? = null
    private var outputUri: Uri? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == "STOP_RECORDING") {
            val customName = intent.getStringExtra("CUSTOM_NAME")
            stopRecording(customName)
            stopSelf()
            return START_NOT_STICKY
        }

        val streamUrl = intent?.getStringExtra("STREAM_URL")
        val channelName = intent?.getStringExtra("CHANNEL_NAME") ?: "Live TV"
        val channelId = intent?.getStringExtra("CHANNEL_ID")
        
        if (streamUrl != null && !isRecording) {
            if (channelId != null) {
                setRecordingChannelId(channelId)
            }
            startRecording(streamUrl, channelName)
        }

        return START_STICKY
    }

    private fun startRecording(streamUrl: String, channelName: String) {
        isRecording = true
        
        // Foreground notification
        val stopIntent = Intent(this, LiveTvRecordingService::class.java).apply {
            this.action = "STOP_RECORDING"
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, "TV_RECORDING_CHANNEL")
            .setContentTitle("Recording Live TV")
            .setContentText("Recording $channelName in high quality...")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_media_ff, "Stop Recording", stopPendingIntent)
            .build()

        startForeground(9988, notification)

        recordingJob = serviceScope.launch {
            try {
                val isHls = streamUrl.contains(".m3u8")
                val ext = if (isHls) "ts" else "mp4"
                val mimeType = if (isHls) "video/mp2t" else "video/mp4"
                val filename = "Record_${channelName.replace(" ", "_")}_${System.currentTimeMillis()}.$ext"
                var os: OutputStream? = null
                outputUri = null
                outputFile = null

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        val contentValues = ContentValues().apply {
                            put(MediaStore.Video.Media.DISPLAY_NAME, filename)
                            put(MediaStore.Video.Media.MIME_TYPE, mimeType)
                            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Swapnoflix")
                            put(MediaStore.Video.Media.IS_PENDING, 1)
                        }
                        val resolver = contentResolver
                        val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                        outputUri = resolver.insert(collection, contentValues)
                        if (outputUri != null) {
                            os = resolver.openOutputStream(outputUri!!)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("RecordingService", "Failed to create MediaStore entry", e)
                    }
                }

                if (os == null) {
                    // Fallback to legacy file writing in Swapnoflix folder in Downloads
                    var directory = File(
                        android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS),
                        "Swapnoflix"
                    )
                    try {
                        if (!directory.exists()) {
                            directory.mkdirs()
                        }
                        if (!directory.exists()) {
                            directory = File(getExternalFilesDir(null), "Swapnoflix")
                            if (!directory.exists()) directory.mkdirs()
                        }
                    } catch (e: Exception) {
                        directory = File(getExternalFilesDir(null), "Swapnoflix")
                        if (!directory.exists()) directory.mkdirs()
                    }
                    val fallbackFile = File(directory, filename)
                    outputFile = fallbackFile
                    os = FileOutputStream(fallbackFile)
                }

                if (os == null) {
                    throw java.io.IOException("Unable to create output stream for recording")
                }

                if (streamUrl.contains(".m3u8")) {
                    // HLS Live Stream Segment Downloader
                    val urlWithoutParams = streamUrl.substringBefore("?")
                    val params = if (streamUrl.contains("?")) streamUrl.substringAfter("?") else ""
                    val baseUri = urlWithoutParams.substringBeforeLast("/") + "/"
                    val downloadedSegments = mutableSetOf<String>()
                    val outputStream = os

                    while (isActive && isRecording) {
                        try {
                            val manifestConn = URL(streamUrl).openConnection() as HttpURLConnection
                            manifestConn.connectTimeout = 5000
                            manifestConn.readTimeout = 5000
                            val manifestText = manifestConn.inputStream.bufferedReader().use { it.readText() }
                            
                            val lines = manifestText.split("\n")
                            val newSegments = mutableListOf<String>()
                            
                            for (line in lines) {
                                val trimmed = line.trim()
                                if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                                    val fullUrl = if (trimmed.startsWith("http")) {
                                        trimmed
                                    } else {
                                        baseUri + trimmed + (if (params.isNotEmpty()) "?$params" else "")
                                    }
                                    if (!downloadedSegments.contains(fullUrl)) {
                                        newSegments.add(fullUrl)
                                    }
                                }
                            }
                            
                            for (segmentUrl in newSegments) {
                                if (!isActive || !isRecording) break
                                try {
                                    val segConn = URL(segmentUrl).openConnection() as HttpURLConnection
                                    segConn.connectTimeout = 5000
                                    segConn.readTimeout = 5000
                                    segConn.inputStream.use { input ->
                                        input.copyTo(outputStream)
                                    }
                                    downloadedSegments.add(segmentUrl)
                                    if (downloadedSegments.size > 200) {
                                        downloadedSegments.remove(downloadedSegments.first())
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("RecordingService", "Failed to download segment: $segmentUrl", e)
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("RecordingService", "Failed to read HLS manifest", e)
                        }
                        
                        kotlinx.coroutines.delay(4000)
                    }
                    outputStream.close()
                } else {
                    // Progressive Stream Downloader fallback
                    val connection = URL(streamUrl).openConnection() as HttpURLConnection
                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000
                    connection.connect()

                    if (connection.responseCode in 200..299) {
                        val inputStream: InputStream = connection.inputStream
                        val outputStream = os
                        val buffer = ByteArray(8192)
                        var bytesRead: Int

                        while (isActive && isRecording) {
                            bytesRead = inputStream.read(buffer)
                            if (bytesRead == -1) break
                            outputStream.write(buffer, 0, bytesRead)
                        }
                        outputStream.close()
                        inputStream.close()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("RecordingService", "Error during recording", e)
            } finally {
                withContext(Dispatchers.Main) {
                    stopSelf()
                }
            }
        }
    }

    private fun stopRecording(customName: String? = null) {
        isRecording = false
        recordingJob?.cancel()
        setRecordingChannelId(null)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && outputUri != null) {
            try {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Video.Media.IS_PENDING, 0)
                    if (!customName.isNullOrBlank()) {
                        val extension = if (outputUri.toString().contains(".ts")) "ts" else "mp4" // rough fallback
                        val sanitizedName = if (customName.endsWith(".$extension")) customName else "$customName.$extension"
                        put(MediaStore.Video.Media.DISPLAY_NAME, sanitizedName)
                    }
                }
                contentResolver.update(outputUri!!, contentValues, null, null)
                android.util.Log.d("RecordingService", "MediaStore recording stopped. Uri: $outputUri")
            } catch (e: Exception) {
                android.util.Log.e("RecordingService", "Failed to update MediaStore entry on stop", e)
            }
        }
        
        if (!customName.isNullOrBlank() && outputFile != null && outputFile!!.exists()) {
            try {
                val extension = outputFile!!.name.substringAfterLast(".", "mp4")
                val sanitizedName = if (customName.endsWith(".$extension")) customName else "$customName.$extension"
                val newFile = File(outputFile!!.parentFile, sanitizedName)
                val success = outputFile!!.renameTo(newFile)
                if (success) {
                    outputFile = newFile
                }
            } catch (e: Exception) {
                android.util.Log.e("RecordingService", "Failed to rename file", e)
            }
        }
        android.util.Log.d("RecordingService", "Recording stopped. File saved to: ${outputFile?.absolutePath}")
    }

    override fun onDestroy() {
        stopRecording()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "TV_RECORDING_CHANNEL",
                "Live TV Recording",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        private val _recordingChannelId = MutableStateFlow<String?>(null)
        val recordingChannelId = _recordingChannelId.asStateFlow()
        
        fun setRecordingChannelId(id: String?) {
            _recordingChannelId.value = id
        }
    }
}
