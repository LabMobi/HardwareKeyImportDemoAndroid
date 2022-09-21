package mobi.lab.keyimportdemo.app.common

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Check if we have a network connection
 */
@SuppressWarnings("MissingPermission")
fun isNetworkConnected(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager? ?: return true
    val network = connectivityManager.activeNetwork
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return true
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
