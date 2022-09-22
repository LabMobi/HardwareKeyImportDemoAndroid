package mobi.lab.keyimportdemo.domain.entities

sealed class KeyImportTestResult {
    data class Success(val keyTestResult: KeyUsageTestResult) : KeyImportTestResult()
    object FailedKeyImportNotSupportedOnThisApiLevel : KeyImportTestResult()
    object FailedKeyImportNotAvailableOnThisDevice : KeyImportTestResult()
    object FailedTestDecryptionResultDifferentThanInput : KeyImportTestResult()
}
