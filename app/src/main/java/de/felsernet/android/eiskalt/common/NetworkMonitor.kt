package de.felsernet.android.eiskalt.common

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import de.felsernet.android.eiskalt.log.AppLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Whether the device is online, from Android's network state.
 * Online means a network that Android has checked to have a working internet connection.
 */
object NetworkMonitor {
    private const val TAG = "network"

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    fun initialize(context: Context) {
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
        _isOnline.value = hasInternet(connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork))
        AppLog.i(TAG, if (_isOnline.value) "Online" else "Offline")

        // called on a background thread, the StateFlow can be set from any thread
        connectivityManager.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                update(hasInternet(capabilities))
            }

            override fun onLost(network: Network) {
                update(false)
            }
        })
    }

    private fun update(online: Boolean) {
        if (_isOnline.value == online)
            return
        _isOnline.value = online
        AppLog.i(TAG, if (online) "Online" else "Offline")
    }

    private fun hasInternet(capabilities: NetworkCapabilities?) = capabilities != null &&
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
