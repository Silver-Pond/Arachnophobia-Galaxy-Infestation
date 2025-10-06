package com.example.arachnophobia_galaxy_infestation

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.MockedStatic
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

class BootReceiverTest {

    private lateinit var receiver: BootReceiver
    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var mockedStatic: MockedStatic<NotificationReceiver.Companion>
    private lateinit var mockIntent: Intent

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        receiver = BootReceiver()

        // Mock dependencies
        context = mock(Context::class.java)
        prefs = mock(SharedPreferences::class.java)
        mockIntent = mock(Intent::class.java)

        `when`(context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)).thenReturn(prefs)

        // Mock static NotificationReceiver
        mockedStatic = mockStatic(NotificationReceiver.Companion::class.java)
    }

    @After
    fun tearDown() {
        mockedStatic.close()
    }

    @Test
    fun `onReceive does not schedule notifications if disabled`() {
        `when`(mockIntent.action).thenReturn(Intent.ACTION_BOOT_COMPLETED)
        `when`(prefs.getBoolean("scheduled_notifications", false)).thenReturn(false)

        receiver.onReceive(context, mockIntent)

        mockedStatic.verifyNoInteractions()
    }

    @Test
    fun `onReceive ignores other actions`() {
        `when`(mockIntent.action).thenReturn("OTHER_ACTION")

        receiver.onReceive(context, mockIntent)

        mockedStatic.verifyNoInteractions()
    }
}