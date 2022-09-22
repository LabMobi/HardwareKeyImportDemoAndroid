package mobi.lab.keyimportdemo.main

sealed class UiTestStatus {
    object NotStated : UiTestStatus() {
        override fun toString(): String {
            return "NotStated"
        }
    }

    object InProgress : UiTestStatus() {
        override fun toString(): String {
            return "InProgress"
        }
    }

    object FailedKeyImportNotSupportedOnThisApiLevel : UiTestStatus() {
        override fun toString(): String {
            return "FailedKeyImportNotSupportedOnThisApiLevel"
        }
    }

    object FailedKeyImportNotAvailableOnThisDevice : UiTestStatus() {
        override fun toString(): String {
            return "FailedKeyImportNotAvailableOnThisDevice"
        }
    }

    object FailedTestDecryptionResultDifferentThanInput : UiTestStatus() {
        override fun toString(): String {
            return "FailedTestDecryptionResultDifferentThanInput"
        }
    }

    data class FailedImportGeneric(val error: Throwable) : UiTestStatus() {
        override fun toString(): String {
            return "FailedImportGeneric"
        }
    }

    data class FailedUsageGeneric(val error: Throwable) : UiTestStatus() {
        override fun toString(): String {
            return "FailedUsageGeneric"
        }
    }

    data class SuccessKeyImportAndUsage(val keyUsageStatus: UiTestStatus) : UiTestStatus() {
        override fun toString(): String {
            return "SuccessKeyImportAndUsage(keyUsageStatus=$keyUsageStatus)"
        }
    }

    data class SuccessKeyUsage(val keyTeeLevel: UIKeyTeeSecurityLevel, val serverMessage: String, val clientMessage: String) : UiTestStatus() {
        override fun toString(): String {
            return "SuccessKeyUsage(keyTeeLevel=$keyTeeLevel, serverMessage='$serverMessage', clientMessage='$clientMessage')"
        }
    }

    object FailedKeyUsageNoSuchKey : UiTestStatus() {
        override fun toString(): String {
            return "FailedKeyUsageNoSuchKey"
        }
    }
}
