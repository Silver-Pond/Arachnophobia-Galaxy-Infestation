package com.example.arachnophobia_galaxy_infestation

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.widget.Toast
import android.os.Handler
import android.os.Looper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

object CurrencyManager {

    private var isSyncing = false
    private var networkCallbackRegistered = false

    fun saveInGameCurrency(appContext: Context, score: Int) {
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: return

        val username = auth.currentUser?.displayName
        if (username.isNullOrBlank() || username.equals("Guest", ignoreCase = true)) return

        val prefs = appContext.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)

        // Calculate silk earned (Android Developers, 2025; ChatGPT-4, 2025)
        val gainedSilk = (score * 0.5) / 100.0   // double

        // Fast modern network check
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = cm.getNetworkCapabilities(cm.activeNetwork)
        val isOnline = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        // --- Offline -> store pending silk --- (Android Developers, 2025; Firebase, 2025)
        if (!isOnline) {
            val pending = prefs.getFloat("pending_silk", 0f)
            prefs.edit().putFloat("pending_silk", (pending + gainedSilk).toFloat()).apply()

            Handler(Looper.getMainLooper()).post {
                Toast.makeText(appContext, "No internet. Silk saved locally.", Toast.LENGTH_SHORT).show()
            }
            return
        }

        // --- Online -> merge pending + gained silk --- (Android Developers, 2025; Firebase, 2025)
        val pendingSilk = prefs.getFloat("pending_silk", 0f).toDouble()
        val silkToAdd = pendingSilk + gainedSilk

        if (silkToAdd <= 0) return

        val dbRef = FirebaseDatabase.getInstance().getReference("players").child(uid).child("spider_silk")

        // Use a Firebase transaction (fast, atomic, no read needed)
        dbRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val existing = when (val v = currentData.value) {
                    is Long -> v.toDouble()
                    is Int -> v.toDouble()
                    is Double -> v
                    is Float -> v.toDouble()
                    else -> 0.0
                }

                val newAmount = (existing + silkToAdd).coerceAtMost(100_000.0)

                currentData.value = newAmount
                return Transaction.success(currentData)
            }

            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                snapshot: DataSnapshot?
            ) {
                if (error == null && committed) {
                    prefs.edit().remove("pending_silk").apply()

                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(
                            appContext,
                            "You earned ${"%.2f".format(silkToAdd)} silk!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(appContext, "Failed to update silk.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    /**
     * Auto-sync pending silk when internet returns.
     */
    fun registerNetworkCallback(appContext: Context) {
        if (networkCallbackRegistered) return
        networkCallbackRegistered = true

        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                if (isSyncing) return
                isSyncing = true

                val prefs = appContext.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
                val pending = prefs.getFloat("pending_silk", 0f)

                if (pending > 0f) {
                    saveInGameCurrency(appContext, 0) // triggers sync
                }

                isSyncing = false
            }
        }

        // Instant monitoring
        cm.registerDefaultNetworkCallback(callback)
    }
}
/*
* Reference List
*
* Android Developers, 2025. SharedPreferences. [online]. Available at:
* https://developer.android.com/reference/android/content/SharedPreferences
* [Accessed: 7 October 2025].
*
* Android Developers, 2025. Toast. [online]. Available at:
* https://developer.android.com/reference/android/widget/Toast
* [Accessed: 7 October 2025].
*
* Android Developers, 2025. Context. [online]. Available at:
* https://developer.android.com/reference/android/content/Context
* [Accessed: 7 October 2025].
*
* Firebase, 2025. FirebaseAuth. [online]. Available at:
* https://firebase.google.com/docs/reference/android/com/google/firebase/auth/FirebaseAuth
* [Accessed: 7 October 2025].
*
* Firebase, 2025. FirebaseDatabase. [online]. Available at:
* https://firebase.google.com/docs/reference/android/com/google/firebase/database/FirebaseDatabase
* [Accessed: 7 October 2025].
*
* Firebase, 2025. DatabaseReference. [online]. Available at:
* https://firebase.google.com/docs/reference/android/com/google/firebase/database/DatabaseReference
* [Accessed: 7 October 2025].
*
* ChatGPT-4, 2025. OpenAI. [online]. Available at:
* https://chatgpt.com/?model=auto
* [Accessed: 10 November 2025].
*/