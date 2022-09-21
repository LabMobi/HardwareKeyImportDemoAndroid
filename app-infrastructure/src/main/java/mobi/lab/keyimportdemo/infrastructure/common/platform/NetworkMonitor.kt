package mobi.lab.keyimportdemo.infrastructure.common.platform

import android.content.Context
import mobi.lab.keyimportdemo.app.common.isNetworkConnected

class NetworkMonitor(private val context: Context) {
    fun isConnected(): Boolean {
        return isNetworkConnected(context)
    }
}
