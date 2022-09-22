package mobi.lab.keyimportdemo.domain.entities

class DomainException(
    val code: ErrorCode,
    cause: Throwable? = null,
    message: String? = "DomainException: errorCode=$code",
) : Exception(message) {

    constructor(code: ErrorCode, cause: Throwable) : this(code = code, cause = cause, message = null)
    constructor(code: ErrorCode, message: String) : this(code = code, cause = null, message = message)

    init {
        if (cause != null) {
            this.initCause(cause)
        }
    }

    fun isFor(errorCode: ErrorCode): Boolean {
        return errorCode == code
    }

    companion object {
        fun unknown(): DomainException {
            return DomainException(ErrorCode.UNKNOWN)
        }

        fun unauthorized(cause: Throwable? = null): DomainException {
            return DomainException(ErrorCode.LOCAL_UNAUTHORIZED, cause)
        }

        fun noSuchImportedKeyFound(keyAlias: String): DomainException {
            return DomainException(ErrorCode.NO_SUCH_IMPORTED_KEY_FOUND, keyAlias)
        }

        fun noSuchServerKeyFound(keyAlias: String): DomainException {
            return DomainException(ErrorCode.NO_SUCH_SERVER_KEY_FOUND, keyAlias)
        }

        fun decryptedMessageDoesNotMatchTheOriginalMessage(message: String): DomainException {
            return DomainException(ErrorCode.DECRYPTED_MESSAGE_DOES_NOT_MATCH_THE_ORIGINAL_MESSAGE, message)
        }
    }
}
