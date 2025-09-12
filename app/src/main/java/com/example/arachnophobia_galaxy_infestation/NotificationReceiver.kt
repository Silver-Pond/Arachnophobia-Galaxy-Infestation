package com.example.arachnophobia_galaxy_infestation

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        Log.d(TAG, "onReceive() — showing notification")

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "app_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "App Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Tap → open MainActivity
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.spider_maroon)
            .setContentTitle("Come back and play!")
            .setContentText("We miss you! Tap to continue your journey 🚀")
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()

        val id = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        notificationManager.notify(id, notification)

        // Schedule the next exact alarm
        scheduleNextAlarm(context)

        Log.d(TAG, "Notification shown and next alarm scheduled")
    }

    companion object {
        private const val TAG = "NotificationReceiver"
        // 30 minutes:
        private const val INTERVAL_MS = 30 * 60 * 1000L
        // For quick testing you can temporarily set INTERVAL_MS = 60_000L

        fun scheduleNextAlarm(context: Context) {
            val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, NotificationReceiver::class.java)
            val pending = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val triggerAt = System.currentTimeMillis() + INTERVAL_MS

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // On Android 12+ check if app can schedule exact alarms
                    if (alarm.canScheduleExactAlarms()) {
                        alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
                    } else {
                        // Fallback: use inexact repeating alarm
                        alarm.set(AlarmManager.RTC_WAKEUP, triggerAt, pending)
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    alarm.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pending)
                } else {
                    alarm.set(AlarmManager.RTC_WAKEUP, triggerAt, pending)
                }
            } catch (se: SecurityException) {
                // App doesn’t have permission → fallback gracefully
                alarm.set(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            }

            context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
                .edit().putBoolean("scheduled_notifications", true).apply()
        }

        fun cancelAlarm(context: Context) {
            val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, NotificationReceiver::class.java)
            val pending = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarm.cancel(pending)

            context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
                .edit().putBoolean("scheduled_notifications", false).apply()

            Log.d(TAG, "cancelAlarm() called")
        }
    }
}