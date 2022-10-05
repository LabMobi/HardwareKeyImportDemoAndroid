package mobi.lab.keyimportdemo.domain.entities

enum class ErrorCode(val code: String) {
    // Local errors
    UNKNOWN("local-unknown"),
    LOCAL_UNAUTHORIZED("local-unauthorized"),
    LOCAL_NO_NETWORK("local-no-network"),
    NO_SUCH_IMPORTED_KEY_FOUND("no-such-imported-key-found"),
    NO_SUCH_SERVER_KEY_FOUND("no-such-server-key-found"),
    DECRYPTED_MESSAGE_DOES_NOT_MATCH_THE_ORIGINAL_MESSAGE("decrypted-message-does-not-match-the-original-message"),
    LOCAL_INVALID_CREDENTIALS("local-invalid-credentials");

    companion object {
        fun parse(code: String?): ErrorCode {
            if (code == null) {
                return UNKNOWN
            }
            for (domainCode in values()) {
                if (domainCode.code == code) {
                    return domainCode
                }
            }
            return UNKNOWN
        }
    }
}
