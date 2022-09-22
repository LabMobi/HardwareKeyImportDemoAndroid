package mobi.lab.keyimportdemo.domain.usecases.crypto

import dagger.Reusable
import io.reactivex.rxjava3.core.Single
import mobi.lab.keyimportdemo.domain.DomainConstants
import mobi.lab.keyimportdemo.domain.entities.DomainException
import mobi.lab.keyimportdemo.domain.entities.ErrorCode
import mobi.lab.keyimportdemo.domain.entities.KeyUsageTestResult
import mobi.lab.keyimportdemo.domain.gateway.CryptoClientGateway
import mobi.lab.keyimportdemo.domain.gateway.CryptoServerGateway
import mobi.lab.keyimportdemo.domain.gateway.LoggerGateway
import mobi.lab.keyimportdemo.domain.usecases.UseCase
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject

@Suppress("UnusedPrivateMember")
@Reusable
class ImportedKeyTwoWayUsageUseCase @Inject constructor(
    private val client: CryptoClientGateway,
    private val server: CryptoServerGateway,
    private val loadServerSecretKeyUseCase: LoadServerSecretKeyUseCase,
    private val log: LoggerGateway,
) : UseCase() {
    fun execute(serverToClientSecretMessage: String, clientToServerSecretMessage: String): Single<KeyUsageTestResult> {
        return Single.fromCallable { runTest(serverToClientSecretMessage, clientToServerSecretMessage) }
    }

    private fun runTest(serverToClientSecretMessage: String, clientToServerSecretMessage: String): KeyUsageTestResult {
        log.d("Usage test started")

        return try {

            log.d("Testing client -> server message flow ..")
            val clientToServerSecretMessageResult = runTestClientEncryptCryptAndServerDecryptJweWithTek(clientToServerSecretMessage)
            log.d("Testing client -> server message flow test finished")

            log.d("Testing server -> client message flow ..")
            val serverToClientSecretMessageResult = runTestServerEncryptCryptAndClientDecryptJweWithTek(serverToClientSecretMessage)
            log.d("Testing server -> client message flow test finished")



            KeyUsageTestResult.UsageSuccess(
                client.getPrivateKeySecurityLevel(DomainConstants.DEVICE_TEE_IMPORT_KEY_ALIAS),
                serverToClientSecretMessageResult,
                clientToServerSecretMessageResult
            )
        } catch (e: DomainException) {
            log.e(e, "runTest")
            when (e.code) {
                ErrorCode.NO_SUCH_IMPORTED_KEY_FOUND,
                ErrorCode.NO_SUCH_SERVER_KEY_FOUND -> {
                    KeyUsageTestResult.UsageFailedNoSuchKey
                }
                else -> {
                    KeyUsageTestResult.UsageFailedGeneric(e)
                }
            }
        } catch (t: Throwable) {
            log.e(t, "runTest")
            KeyUsageTestResult.UsageFailedGeneric(t)
        }
    }

    private fun runTestServerEncryptCryptAndClientDecryptJweWithTek(secretMessage: String): String {
        val keyBytes = loadServerSecretKeyUseCase.execute()
        if (keyBytes == null) {
            throw DomainException.noSuchServerKeyFound(DomainConstants.DEVICE_TEE_IMPORT_KEY_ALIAS)
        }
        val keySpec = SecretKeySpec(keyBytes, "AES")
        val encryptedSecretMessage = server.encryptMessageWithTekToJWE(secretMessage, keySpec)
        val resultDecryptedMessage = client.decryptJWEWithImportedKey(DomainConstants.DEVICE_TEE_IMPORT_KEY_ALIAS, encryptedSecretMessage)
        assertDecryptedMessageEqualsOriginalMessage(secretMessage, resultDecryptedMessage)
        return resultDecryptedMessage
    }

    private fun runTestClientEncryptCryptAndServerDecryptJweWithTek(secretMessage: String): String {
        val keyBytes = loadServerSecretKeyUseCase.execute()
        if (keyBytes == null) {
            throw DomainException.noSuchServerKeyFound(DomainConstants.DEVICE_TEE_IMPORT_KEY_ALIAS)
        }
        val keySpec = SecretKeySpec(keyBytes, "AES")
        val encryptedSecretMessage = client.encryptMessageWithTekToJWE(secretMessage, DomainConstants.DEVICE_TEE_IMPORT_KEY_ALIAS)
        val resultDecryptedMessage = server.decryptJWEWithImportedKey(encryptedSecretMessage, keySpec)
        assertDecryptedMessageEqualsOriginalMessage(secretMessage, resultDecryptedMessage)
        return resultDecryptedMessage
    }

    private fun assertDecryptedMessageEqualsOriginalMessage(originalMessage: String, decryptedMessage: String) {
        if (originalMessage != decryptedMessage) {
            throw DomainException.decryptedMessageDoesNotMatchTheOriginalMessage("Original: $originalMessage\nDecrypted: $decryptedMessage")
        }
    }
}
