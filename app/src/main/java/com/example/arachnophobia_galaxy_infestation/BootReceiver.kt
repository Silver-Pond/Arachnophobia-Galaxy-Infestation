package com.example.arachnophobia_galaxy_infestation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
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
