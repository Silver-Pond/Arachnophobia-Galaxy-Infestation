package com.example.arachnophobia_galaxy_infestation;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.util.Log;

public class NetworkMonitor {

    public interface NetworkListener {
        void onNetworkAvailable();
        void onNetworkLost();
    }

    private final ConnectivityManager connectivityManager;
    private final NetworkListener listener;
    private final ConnectivityManager.NetworkCallback networkCallback;

    public NetworkMonitor(Context context, NetworkListener listener) {
        this.listener = listener;
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        // Create a network callback (Android Developers, 2025; ChatGPT-4, 2025)
        this.networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                super.onAvailable(network);
                Log.d("NetworkMonitor", "Online");
                listener.onNetworkAvailable();
            }

            @Override
            public void onLost(Network network) {
                super.onLost(network);
                Log.d("NetworkMonitor", "Offline");
                listener.onNetworkLost();
            }
        };
    }

    // Register and unregister network callback (Android Developers, 2025; ChatGPT-4, 2025)
    public void register() {
        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
        connectivityManager.registerNetworkCallback(request, networkCallback);
    }

    public void unregister() {
        connectivityManager.unregisterNetworkCallback(networkCallback);
    }
}
/*
 * Reference List
 *
 * Android Developers, 2025. Developer centers. [online]. Available at:
 * https://developer.android.com/
 * [Accessed: 6 October 2025].
 *
 * Android Developers, 2025. Fragment transactions. [online]. Available at:
 * https://developer.android.com/guide/fragments/transactions
 * [Accessed: 6 October 2025].
 *
 * ChatGPT-4, 2025. OpenAI. [online]. Available at:
 * https://chatgpt.com/?model=auto
 * [Accessed: 6 October 2025].
 */