package com.example.arachnophobia_galaxy_infestation

import android.app.*
import android.content.Context
import android.content.Intent
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.eq

class NotificationReceiverTest {

    @Mock lateinit var mockContext: Context
    @Mock lateinit var mockNotificationManager: NotificationManager
    @Mock lateinit var mockAlarmManager: AlarmManager
    @Mock lateinit var mockPrefs: android.content.SharedPreferences
    @Mock lateinit var mockEditor: android.content.SharedPreferences.Editor

    private lateinit var receiver: NotificationReceiver

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        receiver = NotificationReceiver()

        `when`(mockContext.getSystemService(Context.NOTIFICATION_SERVICE))
            .thenReturn(mockNotificationManager)
        `when`(mockContext.getSystemService(Context.ALARM_SERVICE))
            .thenReturn(mockAlarmManager)
        `when`(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockPrefs)
        `when`(mockPrefs.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putBoolean(anyString(), anyBoolean())).thenReturn(mockEditor)

        // Stub Android static methods that crash in JVM tests (ChatGPT-4, 2025)
        mockStatic(PendingIntent::class.java).use { pendingIntentMock ->
            pendingIntentMock.`when`<Any> {
                PendingIntent.getBroadcast(any(), anyInt(), any(), anyInt())
            }.thenReturn(mock(PendingIntent::class.java))

            mockStatic(Intent::class.java).use { _ ->
                // No need to mock Intent methods for this one (ChatGPT-4, 2025)
            }
        }
    }

    @Test
    fun `scheduleNextAlarm saves scheduled state`() {
        mockStatic(PendingIntent::class.java).use {
            `when`(PendingIntent.getBroadcast(any(), anyInt(), any(), anyInt()))
                .thenReturn(mock(PendingIntent::class.java))

            NotificationReceiver.scheduleNextAlarm(mockContext)
        }

        verify(mockPrefs).edit()
        verify(mockEditor).putBoolean(eq("scheduled_notifications"), eq(true))
        verify(mockEditor).apply()
    }

    @Test
    fun `cancelAlarm cancels alarm and updates shared prefs`() {
        mockStatic(PendingIntent::class.java).use {
            `when`(PendingIntent.getBroadcast(any(), anyInt(), any(), anyInt()))
                .thenReturn(mock(PendingIntent::class.java))

            NotificationReceiver.cancelAlarm(mockContext)
        }

        verify(mockAlarmManager).cancel(any(PendingIntent::class.java))
        verify(mockEditor).putBoolean(eq("scheduled_notifications"), eq(false))
        verify(mockEditor).apply()
    }
}
/*
* Reference List
*
* ChatGPT-4, 2025. OpenAI. [online]. Available at:
* https://chatgpt.com/?model=auto
* [Accessed: 10 November 2025].
*/