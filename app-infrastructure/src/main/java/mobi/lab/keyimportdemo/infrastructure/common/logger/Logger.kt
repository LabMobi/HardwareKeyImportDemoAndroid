package mobi.lab.keyimportdemo.infrastructure.common.logger

import android.os.Handler
import android.os.Looper
import android.util.Log
import mobi.lab.keyimportdemo.domain.gateway.LoggerGateway
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Logger @Inject constructor() : LoggerGateway {
    private var listener: LoggerGateway.LoggerGatewayListener? = null
    private var messages: ArrayList<CharSequence> = ArrayList()
    private val uiHandler = Handler(Looper.getMainLooper())

    init {
        messages.ensureCapacity(MAX_LOG_LINE_COUNT)
    }

    override fun d(message: String) {
        logToBuffer(message)
        Timber.d(message)
    }

    override fun e(error: Throwable, message: String) {
        logToBuffer(message + "\n" + Log.getStackTraceString(error))
        Timber.e(error, message)
    }

    @Suppress("MagicNumber", "UnusedPrivateMember")
    private fun logToBuffer(text: CharSequence) {
        synchronized(messages) {
            val futureSize: Int = messages.size + 1
            if (futureSize > MAX_LOG_LINE_COUNT) {
                val itemsToRemove: Int = futureSize - MAX_LOG_LINE_COUNT + (MAX_LOG_LINE_COUNT / 10).coerceAtMost(500)
                val iterator: MutableIterator<CharSequence> = messages.iterator()
                for (i in 0 until itemsToRemove) {
                    iterator.next()
                    iterator.remove()
                }
            }
            messages.add(text)
            uiHandler.post {
                this.listener?.onLogLinesUpdated(getLogLines())
            }
        }
    }

    override fun getLogLines(): ArrayList<CharSequence> {
        return messages
    }

    override fun setLogLinesListener(listener: LoggerGateway.LoggerGatewayListener?) {
        this.listener = listener
    }

    override fun clearLog() {
        synchronized(messages) {
            messages.clear()
        }
    }

    companion object {
        var MAX_LOG_LINE_COUNT = 6000
    }
}
