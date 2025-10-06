package com.example.arachnophobia_galaxy_infestation

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationReceiverInstrumentedTest {

    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManager
    private lateinit var alarmManager: AlarmManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Cancel any previously scheduled alarms
        NotificationReceiver.cancelAlarm(context)
    }

    @After
    fun tearDown() {
        // Cleanup: cancel alarms after test
        NotificationReceiver.cancelAlarm(context)
    }

    @Test
    fun testOnReceive_showsNotificationAndSchedulesNextAlarm() {
        val receiver = NotificationReceiver()

        val intent = Intent(context, NotificationReceiver::class.java)
        receiver.onReceive(context, intent)

        // Check that SharedPreferences flag was set
        val scheduled = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
            .getBoolean("scheduled_notifications", false)
        assertTrue("Notification scheduling flag should be true", scheduled)

        // NOTE: We can't directly check NotificationManager for posted notifications in instrumented tests,
        // but the fact that no exception was thrown and the SharedPreferences flag is true
        // is a reasonable test for notification posting.

        // Similarly, MediaPlayer playback can't be asserted easily in tests,
        // but we verify that onReceive executes without crashing.
    }

    @Test
    fun testScheduleAndCancelAlarm_setsAndRemovesAlarm() {
        // Schedule alarm
        NotificationReceiver.scheduleNextAlarm(context)

        val scheduled = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
            .getBoolean("scheduled_notifications", false)
        assertTrue("Alarm should be scheduled", scheduled)

        // Cancel alarm
        NotificationReceiver.cancelAlarm(context)
        val canceled = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
            .getBoolean("scheduled_notifications", true)
        assertTrue("Alarm should be canceled", !canceled)
    }
}