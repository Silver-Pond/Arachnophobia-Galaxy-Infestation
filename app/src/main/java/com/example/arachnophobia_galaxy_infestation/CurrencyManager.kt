package com.example.arachnophobia_galaxy_infestation

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

object CurrencyManager {

    fun saveInGameCurrency(appContext: Context, score: Int) {
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: return

        val prefs = appContext.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)

        // Calculate silk earned
        val gainedSilk = (score * 0.5) / 100

        // Check network
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val netInfo = cm.activeNetworkInfo
        val isOnline = netInfo != null && netInfo.isConnected

        // Offline → store pending silk
        if (!isOnline) {
            val pending = prefs.getFloat("pending_silk", 0f)
            prefs.edit().putFloat("pending_silk", (pending + gainedSilk).toFloat()).apply()
            Toast.makeText(appContext, "No internet. Silk saved locally.", Toast.LENGTH_SHORT).show()
            return
        }

        // Online → sync pending + current silk
        val pendingSilk = prefs.getFloat("pending_silk", 0f)
        val silkToAdd = pendingSilk + gainedSilk

        val dbRef = FirebaseDatabase.getInstance().getReference("players").child(uid)

        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                // Safely read spider_silk regardless of type
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
                        Toast.makeText(
                            appContext,
                            "You earned $gainedSilk silk!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(appContext, "Failed to update silk.", Toast.LENGTH_SHORT).show()
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(appContext, "Database error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    /**
     * Registers network callback for auto-syncing pending silk
     */
    fun registerNetworkCallback(appContext: Context) {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)

                val prefs = appContext.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
                val pendingSilk = prefs.getFloat("pending_silk", 0f)

                if (pendingSilk > 0f) {
                    // sync pending silk; score is 0 because we're using pendingSilk
                    saveInGameCurrency(appContext, 0)
                }
            }
        }

        val request = NetworkRequest.Builder().build()
        cm.registerNetworkCallback(request, callback)
    }
}
