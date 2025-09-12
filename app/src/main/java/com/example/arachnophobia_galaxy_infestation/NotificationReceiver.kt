package com.example.arachnophobia_galaxy_infestation

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create channel for Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "app_channel",
                "App Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500) // wait, vibrate, pause, vibrate
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Build the notification with vibration
        val notification = NotificationCompat.Builder(context, "app_channel")
            .setSmallIcon(R.drawable.spider_maroon)
            .setContentTitle("Come back and play!")
            .setContentText("We miss you! Open the app and continue your journey 🚀")
            .setPriority(NotificationCompat.PRIORITY_HIGH) // high priority to ensure vibration
            .setVibrate(longArrayOf(0, 500, 200, 500)) // works pre-Android O
            .setAutoCancel(true)
            .build()

        // Show the notification
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}