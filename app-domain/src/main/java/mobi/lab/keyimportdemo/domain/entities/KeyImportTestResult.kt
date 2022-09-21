package mobi.lab.keyimportdemo.domain.entities

sealed class KeyImportTestResult {
    data class SuccessHardwareTeeStrongBox(val message: String) : KeyImportTestResult()
    data class SuccessHardwareTeeNoStrongbox(val message: String) : KeyImportTestResult()
    data class SuccessSoftwareTeeOnly(val message: String) : KeyImportTestResult()
    data class SuccessTeeUnknown(val message: String) : KeyImportTestResult()
    object FailedKeyImportNotSupportedOnThisApiLevel : KeyImportTestResult()
    object FailedKeyImportNotAvailableOnThisDevice : KeyImportTestResult()
    object FailedTestDecryptionResultDifferentThanInput : KeyImportTestResult()
}
