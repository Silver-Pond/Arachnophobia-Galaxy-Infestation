package com.example.arachnophobia_galaxy_infestation

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

object CurrencyManager {

    fun saveInGameCurrency(appContext: Context, score: Int) {
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: return

        val username = auth.currentUser?.displayName
        if (username.isNullOrBlank() || username == "Guest") {
            // Do NOT save silk for guest accounts
            return
        }

        val prefs = appContext.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)

        // Calculate silk earned
        val gainedSilk = (score * 0.5) / 100.0   // double

        // Check network
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val netInfo = cm.activeNetworkInfo
        val isOnline = netInfo != null && netInfo.isConnected

        // --- Offline → store pending silk --- (Android Developers, 2025; ChatGPT-4, 2025)
        if (!isOnline) {
            val pending = prefs.getFloat("pending_silk", 0f)
            prefs.edit().putFloat("pending_silk", (pending + gainedSilk).toFloat()).apply()

            Handler(Looper.getMainLooper()).post {
                Toast.makeText(appContext, "No internet. Silk saved locally.", Toast.LENGTH_SHORT).show()
            }
            return
        }

        // --- Online → sync pending + current silk --- (Android Developers, 2025; ChatGPT-4, 2025)
        val pendingSilk = prefs.getFloat("pending_silk", 0f).toDouble()
        val silkToAdd = pendingSilk + gainedSilk

        if (silkToAdd <= 0) return

        val dbRef = FirebaseDatabase.getInstance().getReference("players").child(uid)

        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                // Read spider_silk safely regardless of Firebase's type (Android Developers, 2025; ChatGPT-4, 2025)
                val silkSnapshot = snapshot.child("spider_silk")
                val currentSilk: Double = when (val value = silkSnapshot.value) {
                    is Long -> value.toDouble()
                    is Int -> value.toDouble()
                    is Double -> value
                    is Float -> value.toDouble()
                    else -> 0.0
                }

                val updatedAmount = (currentSilk + silkToAdd).coerceAtMost(100_000.0)

                dbRef.child("spider_silk").setValue(updatedAmount)
                    .addOnSuccessListener {
                        prefs.edit().remove("pending_silk").apply()

                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(
                                appContext,
                                "You earned ${"%.2f".format(silkToAdd)} silk!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    .addOnFailureListener {
                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(appContext, "Failed to update silk.", Toast.LENGTH_SHORT).show()
                        }
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(appContext, "Database error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }


    /**
     * Automatically syncs pending silk when the device reconnects.
     */
    fun registerNetworkCallback(appContext: Context) {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)

                val prefs = appContext.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
                val pendingSilk = prefs.getFloat("pending_silk", 0f)

                if (pendingSilk > 0f) {
                    saveInGameCurrency(appContext, 0) // triggers sync to Firebase
                }
            }
        }

        val request = NetworkRequest.Builder().build()
        cm.registerNetworkCallback(request, callback)
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