package mobi.lab.keyimportdemo.infrastructure.common.http

interface ErrorTransformer {
    fun transform(error: Throwable): Throwable
}
