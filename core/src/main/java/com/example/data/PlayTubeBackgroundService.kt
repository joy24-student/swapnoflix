package com.example.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class PlayTubeBackgroundService : Service() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        // Call startForeground immediately to satisfy Android system requirements and avoid startup crashes
        val notification = NotificationCompat.Builder(this, "PLAYTUBE_PLAYBACK_CHANNEL")
            .setContentTitle("PlayTube Background Playback")
            .setContentText("Initializing...")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .build()
        try {
            startForeground(9977, notification)
        } catch (e: Exception) {
            android.util.Log.e("PlayTubeService", "Failed to startForeground in onCreate", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        
        when (action) {
            "START_PLAYER_NOTIFICATION" -> {
                val videoId = intent.getStringExtra("VIDEO_ID") ?: ""
                val videoTitle = intent.getStringExtra("VIDEO_TITLE") ?: "YouTube Video"
                val channelName = intent.getStringExtra("CHANNEL_NAME") ?: "PlayTube"
                showNotification(videoId, videoTitle, channelName)
            }
            "STOP_PLAYER_NOTIFICATION" -> {
                stopSelf()
            }
            "STOP_ACTION" -> {
                postAction("STOP")
                stopSelf()
            }
            "NEXT_ACTION" -> {
                postAction("NEXT")
            }
        }
        return START_NOT_STICKY
    }

    private fun showNotification(videoId: String, videoTitle: String, channelName: String) {
        val stopIntent = Intent(this, PlayTubeBackgroundService::class.java).apply {
            this.action = "STOP_ACTION"
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextIntent = Intent(this, PlayTubeBackgroundService::class.java).apply {
            this.action = "NEXT_ACTION"
        }
        val nextPendingIntent = PendingIntent.getService(
            this, 2, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent to open video directly in app
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, Class.forName("com.example.MainActivity")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("PLAY_TUBE_VIDEO_ID", videoId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, "PLAYTUBE_PLAYBACK_CHANNEL")
            .setContentTitle(videoTitle)
            .setContentText(channelName)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopPendingIntent)
            .addAction(android.R.drawable.ic_media_next, "Next", nextPendingIntent)
            .build()

        startForeground(9977, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "PLAYTUBE_PLAYBACK_CHANNEL",
                "PlayTube Background Playback",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        private val _serviceActions = MutableSharedFlow<String>(extraBufferCapacity = 10)
        val serviceActions = _serviceActions.asSharedFlow()
        
        fun postAction(action: String) {
            _serviceActions.tryEmit(action)
        }
    }
}
