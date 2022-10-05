package mobi.lab.keyimportdemo.domain.entities

import mobi.lab.keyimportdemo.domain.gateway.CryptoClientGateway

sealed class KeyUsageTestResult {
    data class UsageSuccess(
        val keyLevel: CryptoClientGateway.KeyTeeSecurityLevel,
        val serverToClientMessage: String,
        val clientToServerMessage: String
    ) : KeyUsageTestResult()

    object UsageFailedNoSuchKey : KeyUsageTestResult()
    data class UsageFailedGeneric(val throwable: Throwable) : KeyUsageTestResult()
}
