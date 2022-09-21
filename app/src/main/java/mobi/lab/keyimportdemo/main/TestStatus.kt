package mobi.lab.keyimportdemo.main

sealed class TestStatus {
    object NotStated : TestStatus() {
        override fun toString(): String {
            return "NotStated"
        }
    }

    object InProgress : TestStatus() {
        override fun toString(): String {
            return "InProgress"
        }
    }

    object FailedKeyImportNotSupportedOnThisApiLevel : TestStatus() {
        override fun toString(): String {
            return "FailedKeyImportNotSupportedOnThisApiLevel"
        }
    }

    object FailedKeyImportNotAvailableOnThisDevice : TestStatus() {
        override fun toString(): String {
            return "FailedKeyImportNotAvailableOnThisDevice"
        }
    }

    object FailedTestDecryptionResultDifferentThanInput : TestStatus() {
        override fun toString(): String {
            return "FailedTestDecryptionResultDifferentThanInput"
        }
    }

    data class FailedGeneric(val error: Throwable) : TestStatus() {
        override fun toString(): String {
            return "FailedGeneric"
        }
    }

    data class SuccessStrongboxTee(val message: String) : TestStatus() {
        override fun toString(): String {
            return "SuccessStrongboxTee"
        }
    }

    data class SuccessHardwareTeeNoStrongbox(val message: String) : TestStatus() {
        override fun toString(): String {
            return "SuccessHardwareTeeNoStrongbox"
        }
    }

    data class SuccessSoftwareTeeOnly(val message: String) : TestStatus() {
        override fun toString(): String {
            return "SuccessSoftwareTeeOnly"
        }
    }

    data class SuccessUnknownTeeOnly(val message: String) : TestStatus() {
        override fun toString(): String {
            return "SuccessUnknownTeeOnly"
        }
    }

    data class SuccessLocalKeyUsage(val message: String) : TestStatus() {
        override fun toString(): String {
            return "SuccessLocalKeyUsage"
        }
    }

    object FailedLocalKeyUsageNoSuchKey : TestStatus() {
        override fun toString(): String {
            return "FailedLocalKeyUsageNoSuchKey"
        }
    }
}
