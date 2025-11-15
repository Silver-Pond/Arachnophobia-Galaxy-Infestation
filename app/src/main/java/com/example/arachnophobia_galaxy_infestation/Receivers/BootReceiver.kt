package com.example.arachnophobia_galaxy_infestation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            // Schedule notifications after boot (Android Developers, 2025)
            Log.d("BootReceiver", "BOOT_COMPLETED received, checking prefs")
            val prefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
            val scheduled = prefs.getBoolean("scheduled_notifications", false)
            if (scheduled) {
                NotificationReceiver.scheduleNextAlarm(context)
                Log.d("BootReceiver", "Rescheduled notifications after boot")
            }
        }
    }
}
/*
* Reference List
*
* Android Developers, 2025. AlarmManager. [online]. Available at:
* https://developer.android.com/reference/android/app/AlarmManager
* [Accessed: 6 October 2025].
*
* Android Developers, 2025. NotificationChannel. [online]. Available at:
* https://developer.android.com/reference/android/app/NotificationChannel
* [Accessed: 6 October 2025].
*
* Android Developers, 2025. BroadcastReceiver. [online]. Available at:
* https://developer.android.com/reference/android/content/BroadcastReceiver
* [Accessed: 6 October 2025].
*
* Android Developers, 2025. PendingIntent. [online]. Available at:
* https://developer.android.com/reference/android/app/PendingIntent
* [Accessed: 6 October 2025].
*
* Android Developers, 2025. Manifest.permission.SCHEDULE_EXACT_ALARM. [online]. Available at:
* https://developer.android.com/reference/android/Manifest.permission#SCHEDULE_EXACT_ALARM
* [Accessed: 6 October 2025].
*
* Android Developers, 2025. Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM. [online]. Available at:
* https://developer.android.com/reference/android/provider/Settings#ACTION_REQUEST_SCHEDULE_EXACT_ALARM
* [Accessed: 6 October 2025].
*
* Android Developers, 2025. Application lifecycle overview. [online]. Available at:
* https://developer.android.com/guide/components/activities/activity-lifecycle
* [Accessed: 6 October 2025].
*/