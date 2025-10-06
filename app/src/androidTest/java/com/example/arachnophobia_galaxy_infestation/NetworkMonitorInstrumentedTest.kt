package com.example.arachnophobia_galaxy_infestation

import android.content.Context
import android.net.ConnectivityManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.arachnophobia_galaxy_infestation.NetworkMonitor.NetworkListener
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class NetworkMonitorInstrumentedTest {
    private var networkMonitor: NetworkMonitor? = null
    private var connectivityManager: ConnectivityManager? = null
    private var networkAvailableLatch: CountDownLatch? = null
    private var networkLostLatch: CountDownLatch? = null

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context?>()
        connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?

        networkAvailableLatch = CountDownLatch(1)
        networkLostLatch = CountDownLatch(1)

        networkMonitor = NetworkMonitor(context, object : NetworkListener {
            override fun onNetworkAvailable() {
                networkAvailableLatch!!.countDown()
            }

            override fun onNetworkLost() {
                networkLostLatch!!.countDown()
            }
        })

        networkMonitor!!.register()
    }

    @After
    fun tearDown() {
        networkMonitor!!.unregister()
    }

    @Test
    @Throws(InterruptedException::class)
    fun testNetworkAvailableCallback() {
        // Wait up to 5 seconds for the callback
        val triggered = networkAvailableLatch!!.await(5, TimeUnit.SECONDS)
        Assert.assertTrue("Network available callback was not triggered", triggered)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testNetworkLostCallback() {
        // NOTE: To actually trigger this, you need to disable network on the device/emulator manually.
        // Wait up to 10 seconds for the callback
        val triggered = networkLostLatch!!.await(10, TimeUnit.SECONDS)
        Assert.assertTrue("Network lost callback was not triggered", triggered)
    }
}