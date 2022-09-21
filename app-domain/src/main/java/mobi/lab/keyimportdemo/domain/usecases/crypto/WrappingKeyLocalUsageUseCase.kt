package mobi.lab.keyimportdemo.domain.usecases.crypto

import dagger.Reusable
import io.reactivex.rxjava3.core.Single
import mobi.lab.keyimportdemo.domain.DomainConstants
import mobi.lab.keyimportdemo.domain.entities.DomainException
import mobi.lab.keyimportdemo.domain.entities.ErrorCode
import mobi.lab.keyimportdemo.domain.entities.KeyLocalUsageTestResult
import mobi.lab.keyimportdemo.domain.gateway.CryptoClientGateway
import mobi.lab.keyimportdemo.domain.gateway.LoggerGateway
import mobi.lab.keyimportdemo.domain.usecases.UseCase
import javax.inject.Inject

@Suppress("UnusedPrivateMember")
@Reusable
class WrappingKeyLocalUsageUseCase @Inject constructor(
    private val client: CryptoClientGateway,
    private val log: LoggerGateway,
) : UseCase() {
    fun execute(inputMessage: String): Single<KeyLocalUsageTestResult> {
        return Single.fromCallable { runTest(inputMessage) }
    }

    private fun runTest(secretMessage: String): KeyLocalUsageTestResult {
        log.d("Local usage test started")

        // Test 1
        // Use the local RSA key to encrypt and decrypt a message
        return try {
            val result = runTestCryptAndEncryptTextWithWrappingKey(secretMessage)
            log.d("Local usage test finished")
            KeyLocalUsageTestResult.UsageSuccess(result)
        } catch (e: DomainException) {
            log.e(e, "runTest")
            when (e.code) {
                ErrorCode.NO_SUCH_IMPORTED_KEY_FOUND -> {
                    KeyLocalUsageTestResult.UsageFailedNoSuchKey
                }
                else -> {
                    KeyLocalUsageTestResult.UsageFailedGeneric(e)
                }
            }
        } catch (t: Throwable) {
            log.e(t, "runTest")
            KeyLocalUsageTestResult.UsageFailedGeneric(t)
        }
    }

    private fun runTestCryptAndEncryptTextWithWrappingKey(secretMessage: String): String {
        val encryptedSecretMessage = client.encryptTextWithWrappingKey(DomainConstants.DEVICE_TEE_WRAPPING_KEY_ALIAS, secretMessage)
        val availableHardware = client.getPrivateKeySecurityLevel(DomainConstants.DEVICE_TEE_WRAPPING_KEY_ALIAS)
        log.d("runTestCryptAndEncryptTextWithWrappingKey: Key in $availableHardware")
        return client.decryptTextWithWrappingKey(DomainConstants.DEVICE_TEE_WRAPPING_KEY_ALIAS, encryptedSecretMessage)
    }
}
