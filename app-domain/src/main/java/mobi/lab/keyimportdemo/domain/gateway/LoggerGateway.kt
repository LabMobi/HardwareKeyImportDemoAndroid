package mobi.lab.keyimportdemo.domain.gateway

interface LoggerGateway {
    fun d(message: String)
    fun e(error: Throwable, message: String)
    fun getLogLines(): ArrayList<CharSequence>
    fun setLogLinesListener(listener: LoggerGatewayListener?)
    fun clearLog()

    interface LoggerGatewayListener {
        fun onLogLinesUpdated(logLines: List<CharSequence>)
    }
}
