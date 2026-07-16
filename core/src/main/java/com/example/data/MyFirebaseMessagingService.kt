package com.example.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM Token generated: $token")
        FirebaseManager.saveFcmToken(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM Message Received from: ${remoteMessage.from}")

        var title = "New Stream Update"
        var body = "Check out the latest live content on SwapnoFlix!"
        var updateVersion = ""
        var updateDesc = ""
        var updateUrl = ""

        // Handle notification payload
        remoteMessage.notification?.let {
            title = it.title ?: title
            body = it.body ?: body
        }

        // Handle data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "FCM Message Data payload: ${remoteMessage.data}")
            title = remoteMessage.data["title"] ?: title
            body = remoteMessage.data["body"] ?: body
            
            // Check if this is an app update alert
            if (remoteMessage.data["type"] == "app_update" || remoteMessage.data.containsKey("new_version")) {
                updateVersion = remoteMessage.data["new_version"] ?: remoteMessage.data["version"] ?: ""
                updateDesc = remoteMessage.data["description"] ?: remoteMessage.data["update_desc"] ?: ""
                updateUrl = remoteMessage.data["download_url"] ?: remoteMessage.data["url"] ?: ""
            }
        }

        // Post to the application live notifier
        FirebaseManager.postFcmNotification(title, body, if (updateVersion.isNotEmpty()) "SYSTEM" else "REMINDER")

        // Trigger dynamic pop-up on the home screen if update fields are provided
        if (updateVersion.isNotEmpty()) {
            FirebaseManager.triggerSimulatedUpdate(
                version = updateVersion,
                description = updateDesc.ifEmpty { body },
                downloadUrl = updateUrl.ifEmpty { "https://cdn.swapnoflix.tv/downloads/swapnoflix-v$updateVersion.apk" }
            )
        }

        // Show a standard system status bar notification
        sendSystemNotification(title, body)
    }

    private fun sendSystemNotification(title: String, messageBody: String) {
        val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        } ?: Intent()
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "swapnoflix_fcm_channel"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "SwapnoFlix Broadcast Pushes",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "FCM push notifications from SwapnoFlix administration"
            }
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    companion object {
        private const val TAG = "SwapnoFlixFCM"
    }
}
