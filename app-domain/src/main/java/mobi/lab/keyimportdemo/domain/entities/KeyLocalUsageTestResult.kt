package mobi.lab.keyimportdemo.domain.entities

sealed class KeyLocalUsageTestResult {
    data class UsageSuccess(val message: String) : KeyLocalUsageTestResult()
    object UsageFailedNoSuchKey : KeyLocalUsageTestResult()
    data class UsageFailedGeneric(val throwable: Throwable) : KeyLocalUsageTestResult()
}
