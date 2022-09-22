package mobi.lab.keyimportdemo.domain.usecases.crypto

import android.os.Build
import android.security.keystore.SecureKeyImportUnavailableException
import androidx.annotation.RequiresApi
import dagger.Reusable
import io.reactivex.rxjava3.core.Single
import mobi.lab.keyimportdemo.app.common.exhaustive
import mobi.lab.keyimportdemo.domain.DomainConstants
import mobi.lab.keyimportdemo.domain.entities.KeyImportTestResult
import mobi.lab.keyimportdemo.domain.gateway.CryptoClientGateway
import mobi.lab.keyimportdemo.domain.gateway.CryptoServerGateway
import mobi.lab.keyimportdemo.domain.gateway.LoggerGateway
import mobi.lab.keyimportdemo.domain.usecases.UseCase
import org.bouncycastle.util.encoders.Base64
import java.nio.charset.Charset
import java.security.PublicKey
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject

@Suppress("UnusedPrivateMember")
@Reusable
class KeyImportUseCase @Inject constructor(
    private val client: CryptoClientGateway,
    private val server: CryptoServerGateway,
    private val log: LoggerGateway,
) : UseCase() {
    fun execute(inputMessage: String): Single<KeyImportTestResult> {
        return Single.fromCallable { runTest(inputMessage) }
    }

    /**
     * This method plays both the client and server side in this key exchange.
     * The client creates a RSA key pair in the Android TEE (Strongbox hardware, hardware or software)
     * and sends the public key to server.
     * Server uses the public key to do a key exchange: uses the client's public key to encrypt and share an AES key
     * to the client.
     * Client uses the Android TEE to decrypt and import the AES key resulting in a shared key at both server and client.
     *
     * Finally, the key exchange result is tested by the server encrypting a message and the client decrypting it.
     */
    @Suppress("LongMethod", "ReturnCount")
    private fun runTest(serverSecretMessageAtServer: String): KeyImportTestResult {
        log.d("Import started")

        // Storage accessible for "client".
        val clientStorage = Storage.ClientSide()

        // Storage accessible for "server"
        // In real word would be in the server instance.
        val serverStorage = Storage.ServerSide(serverSecretMessageAtServer = serverSecretMessageAtServer)

        // Client: Client key init phase in Android TEE
        runTestClientKeyInitPhase(clientStorage)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return KeyImportTestResult.FailedKeyImportNotSupportedOnThisApiLevel
        }

        // Handoff: RSA public key generated in device TEE is transferred from client to server
        log.d("HANDOFF: Wrapping RSA key given from client to server ..")
        serverStorage.wrappingRsaKeyPairAsJwk = clientStorage.wrappingRsaKeyPairAsJwk
        log.d(serverStorage.wrappingRsaKeyPairAsJwk)
        log.d("HANDOFF: Wrapping RSA key given from client to server - success")

        // Server: Server key generation and wrapping phase
        runTestServerKeyGenerationPhase(serverStorage)

        // Handoff: Server key generation phase 8. Server outputs DER encoded ASN.1 structure as Base64 encoded string.
        log.d("HANDOFF: Server outputs DER encoded ASN.1 structure as Base64 encoded string ..")
        clientStorage.asn1DerBase64EncodedTekAndCekAtServer = serverStorage.asn1DerBase64EncodedTekAndCekAtServer
        log.d("String: ${clientStorage.asn1DerBase64EncodedTekAndCekAtServer}")
        log.d("HANDOFF: Server outputs DER encoded ASN.1 structure as Base64 encoded string - success")

        // Client: Client TEK wrapped key import phase
        try {
            runTestClientKeyImportPhase(clientStorage)
        } catch (e: SecureKeyImportUnavailableException) {
            log.e(e, "CLIENT: Key import not supported on this device!")
            return KeyImportTestResult.FailedKeyImportNotAvailableOnThisDevice
        }

        // Test 1
        // Test 1: Encrypt a message at server with TEK and return it as Base64 with the iv appended at the front (first 16 byes) of the cryptogram
        runTestCryptTextWithTekAtServer(serverStorage)

        // Test 1: Handoff client -> server
        log.d("HANDOFF: Server outputs the TEK encrypted message as Base64 encoded string ..")
        clientStorage.messageTekEncryptedEncodedBase64 = serverStorage.messageTekEncryptedEncodedBase64
        log.d("String: ${clientStorage.messageTekEncryptedEncodedBase64}")
        log.d("HANDOFF: Server outputs the TEK encrypted message as Base64 encoded string - success")

        // Test 1: Decrypt at client with imported key
        runTestDecryptTextWithImportedKeyAtClient(clientStorage)

        // Test 1: compare the input and output
        if (serverStorage.serverSecretMessageAtServer != clientStorage.decryptedServerTextMessage) {
            log.d(
                "Test 1 decryption result \"${clientStorage.decryptedServerTextMessage}\" different " +
                    "than the server string \"${clientStorage.decryptedServerTextMessage}\"."
            )
            return KeyImportTestResult.FailedTestDecryptionResultDifferentThanInput
        }

        // Encrypt a message at server with TEK using JWE format and encode as Base64
        runTestCryptJweWithTekAtServer(serverStorage)

        // Handoff
        log.d("HANDOFF: Server outputs the TEK JWE encrypted message as Base64 encoded string ..")
        clientStorage.messageTekEncryptedJweEncodedBase64 = serverStorage.messageTekEncryptedJweEncodedBase64
        log.d("String: ${clientStorage.messageTekEncryptedJweEncodedBase64}")
        log.d("HANDOFF: Server outputs the TEK JWE encrypted message as Base64 encoded string - success")

        // Decrypt at client with imported key
        runTestDecryptJweWithImportedKeyAtClient(clientStorage)

        // Test 2: compare the input and output
        if (serverStorage.serverSecretMessageAtServer != clientStorage.decryptedServerJweMessage) {
            log.d(
                "Test 2 decryption result \"${clientStorage.decryptedServerTextMessage}\" different " +
                    "than the server string \"${clientStorage.decryptedServerJweMessage}\"."
            )
            return KeyImportTestResult.FailedTestDecryptionResultDifferentThanInput
        }

        log.d("Import finished")
        return getKeyFinalType(clientStorage)
    }

    private fun runTestCryptJweWithTekAtServer(serverStorage: Storage.ServerSide) {
        // Server key generation phase 9. Server uses TEK key to create JWE message with payload of “Hello world”
        // and outputs it as Base64 encoded string.
        log.d("SERVER: Encrypting the message with TEK to JWE ..")
        val messageTekEncryptedJWEAtServer =
            server.encryptMessageWithTekToJWE(serverStorage.serverSecretMessageAtServer, serverStorage.tekAesKeyAtServer)
        log.d("SERVER: JWE: $messageTekEncryptedJWEAtServer")
        log.d("SERVER: Encrypting the message with TEK to JWE - success")

        // Encode to Base64
        serverStorage.messageTekEncryptedJweEncodedBase64 =
            String(
                Base64.encode(messageTekEncryptedJWEAtServer.toByteArray(Charset.forName(BASE64_ENCODING_CHARSET_NAME))),
                Charset.forName(BASE64_ENCODING_CHARSET_NAME)
            )
    }

    private fun runTestDecryptJweWithImportedKeyAtClient(clientStorage: Storage.ClientSide) {
        // Android key import phase 2. Android app loads the server encrypted JWE message.
        log.d("CLIENT: Decode the server encrypted text message from Base64 ..")
        val messageTekEncryptedJweString = String(
            Base64.decode(clientStorage.messageTekEncryptedJweEncodedBase64), Charset.forName(BASE64_ENCODING_CHARSET_NAME)
        )
        log.d("CLIENT: JWE: $messageTekEncryptedJweString")
        log.d("CLIENT: Decode the server encrypted text message from Base64 - success")

        // Android key import phase 5. Android app uses the StrongBox protected key to decrypt the JWE message from server and outputs the payload.
        log.d("CLIENT: Decrypt the Server's JWE message with the imported key ..")
        clientStorage.decryptedServerJweMessage =
            client.decryptJWEWithImportedWrappedKey(DomainConstants.DEVICE_TEE_IMPORT_KEY_ALIAS, messageTekEncryptedJweString)
        log.d("CLIENT: Decrypt the Server's JWE message with the imported key - SKIPPED")
    }

    private fun runTestClientKeyInitPhase(clientStorage: Storage.ClientSide) {
        // Client key init phase 1. App generates RSA key pair inside the StrongBox protected KeyStore
        // (example code at line 422 of
        // https://android.googlesource.com/platform/cts/+/master/tests/tests/keystore/src/android/keystore/cts/ImportWrappedKeyTest.java)
        clientStorage.wasGeneratedInStrongbox = try {
            log.d("CLIENT: Generating a wrapping RSA key in Device StrongBox TEE ..")
            clientStorage.wrappingRsaKeyPairPublicKeyAtClient =
                client.generateRsaKeyPairInDeviceTee(DomainConstants.DEVICE_TEE_WRAPPING_KEY_ALIAS, isStrongBoxBacked = true)
            log.d("CLIENT: Generating a wrapping RSA key in Device StrongBox TEE - success")
            true
        } catch (t: Throwable) {
            log.e(t, "CLIENT: Strongbox not supported!")
            // Try TEE without Strongbox
            log.d("CLIENT: Generating a wrapping RSA key in Device TEE (hardware or  software) with no Strongbox ..")
            clientStorage.wrappingRsaKeyPairPublicKeyAtClient =
                client.generateRsaKeyPairInDeviceTee(DomainConstants.DEVICE_TEE_WRAPPING_KEY_ALIAS, isStrongBoxBacked = false)
            log.d("CLIENT: Generating a wrapping RSA key in Device TEE (hardware or  software) with no Strongbox -- success")
            false
        }

        // Client key init phase 2. (Optional: app could also create Google attestation of the RSA key pair.
        // But it seems that server doesn’t need this for cryptography, so, we skip it at the moment)
        // SKIPPED

        // Client key init phase 3. App exports public key of the RSA key pair in the JWK format.
        log.d("CLIENT: Encoding the wrapping RSA key to JWK for export ..")
        clientStorage.wrappingRsaKeyPairAsJwk =
            client.encodeRsaPublicKeyAsJwk(DomainConstants.DEVICE_TEE_WRAPPING_KEY_ALIAS, clientStorage.wrappingRsaKeyPairPublicKeyAtClient)
        log.d("CLIENT: Encoding the wrapping RSA key to JWK for export - success")
    }

    private fun runTestServerKeyGenerationPhase(serverStorage: Storage.ServerSide) {
        // Server key generation phase 1. Server imports the app’s public key from the JWK format.
        log.d("SERVER: Decoding the wrapping RSA key from JWK for import ..")
        val wrappingRsaKeyPairPublicKeyAtServer = server.decodeRsaPublicKeyFromJWKString(serverStorage.wrappingRsaKeyPairAsJwk)
        log.d("SERVER: Decoding the wrapping RSA key from JWK for import - success")

        // Server key generation phase 2. Server generates long-term AES secret key.
        // This will be the TEK key, which will be retained at the server side and which will be imported into the Android KeyStore.

        log.d("SERVER: Generating AES TEK for Android import ..")
        serverStorage.tekAesKeyAtServer = server.generateAesTek(TEK_KEY_SIZE_BYTES)
        log.d("SERVER: Generating AES TEK for Android import - success")

        // Server key generation phase 3. Server generates TEK key metadata, which will be used by StrongBox to import the TEK key
        log.d("SERVER: Generating TEK import metadata ..")
        val tekImportMetadata = server.generateTekImportMetadata(TEK_KEY_SIZE_BYTES, KEY_MASTER_ALGORITHM_AES)
        log.d("SERVER: Metadata = $tekImportMetadata")
        log.d("SERVER: Generating TEK import metadata - success")

        // Server key generation phase 4. Server generates ephemeral AES secret key.
        // This will be CEK (Content Encryption Key), which will be temporarily used to wrap the TEK and additional metadata.
        log.d("SERVER: Generating AES CEK to wrap the TEK and Metadata ..")
        val cekAesKeyAtServer = server.generateAesCek(CEK_KEY_SIZE_BYTES)
        log.d("SERVER: Generating AES CEK to wrap the TEK and Metadata - success")

        // Server key generation phase 5. Server encrypts the CEK to the RSA public key with “RSA/ECB/OAEPPadding” encryption.
        // (Example code at line 338 of
        // https://android.googlesource.com/platform/cts/+/master/tests/tests/keystore/src/android/keystore/cts/ImportWrappedKeyTest.java)
        log.d("SERVER: Encrypt CEK with the RSA public key using RSA/ECB/OAEPPadding ..")
        val cekAesKeyEncryptedWithRsaPublicKeyAtServer = server.encryptCekWithRsaPublicKey(cekAesKeyAtServer, wrappingRsaKeyPairPublicKeyAtServer)
        log.d("SERVER: Encrypt CEK with the RSA public key using RSA/ECB/OAEPPadding - success")

        // Server key generation phase 6. Server uses CEK with “AES/GCM/NoPadding” encryption to encrypt TEK key
        log.d("SERVER: Encrypt TEK with CEK using “AES/GCM/NoPadding” ..")
        val tekEncryptedWithCekAtServer = server.encryptTekWithCek(serverStorage.tekAesKeyAtServer, tekImportMetadata, cekAesKeyAtServer)
        log.d("SERVER: Encrypt TEK with CEK using “AES/GCM/NoPadding” - success")

        // Server key generation phase 7. Server encodes encrypted information in ASN.1 format, as required in Android keystore system
        // (example code at line 361 of
        // https://android.googlesource.com/platform/cts/+/master/tests/tests/keystore/src/android/keystore/cts/ImportWrappedKeyTest.java)
        log.d("SERVER: Encoding encrypted TEK and CEK to ASN.1 DER format ..")
        val asn1DerEncodedTekAndCekAtServer =
            server.encodeTekAndCetToAsn1Der(tekEncryptedWithCekAtServer, cekAesKeyEncryptedWithRsaPublicKeyAtServer, tekImportMetadata)
        log.d("SERVER: Encoding encrypted TEK and CEK to ASN.1 DER format - success")

        // Encode to Base64
        serverStorage.asn1DerBase64EncodedTekAndCekAtServer =
            String(Base64.encode(asn1DerEncodedTekAndCekAtServer), Charset.forName(BASE64_ENCODING_CHARSET_NAME))
    }

    @RequiresApi(Build.VERSION_CODES.P)
    @Throws(SecureKeyImportUnavailableException::class)
    private fun runTestClientKeyImportPhase(clientStorage: Storage.ClientSide) {
        // Android key import phase 1. Android app loads the DER encoded ASN.1 structure
        log.d("CLIENT: Loads the DER encoded ASN.1 structure ..")
        val asn1DerEncodedTekAndCekAtClient = Base64.decode(clientStorage.asn1DerBase64EncodedTekAndCekAtServer)!!
        log.d("CLIENT: Loads the DER encoded ASN.1 structure - success")

        // Android key import phase 4. Android app imports the wrapped key
        log.d("CLIENT: Importing the server wrapped key to device TEE keystore ..")
        client.importWrappedKeyFromServer(
            asn1DerEncodedTekAndCekAtClient,
            DomainConstants.DEVICE_TEE_WRAPPING_KEY_ALIAS,
            DomainConstants.DEVICE_TEE_IMPORT_KEY_ALIAS
        )
        log.d("CLIENT: Importing the server wrapped key to device TEE keystore - success")
    }

    private fun runTestCryptTextWithTekAtServer(serverStorage: Storage.ServerSide) {
        log.d("SERVER: Encrypting the message with TEK ..")
        val messageTekEncryptedAtServer = server.encryptMessageWithTek(serverStorage.serverSecretMessageAtServer, serverStorage.tekAesKeyAtServer)
        log.d("SERVER: Encrypting the message with TEK - success")

        // Encode to base64
        serverStorage.messageTekEncryptedEncodedBase64 = String(
            Base64.encode(messageTekEncryptedAtServer), Charset.forName(BASE64_ENCODING_CHARSET_NAME)
        )
    }

    private fun runTestDecryptTextWithImportedKeyAtClient(clientStorage: Storage.ClientSide) {
        log.d("CLIENT: Decode the server encrypted text message from Base64 ..")
        val messageTekEncryptedAtClient = Base64.decode(clientStorage.messageTekEncryptedEncodedBase64)
        log.d("CLIENT: Decode the server encrypted text message from Base64 - success")

        log.d("CLIENT: Decrypt the Server's text message with the imported key ..")
        clientStorage.decryptedServerTextMessage =
            client.decryptTextWithImportedKey(DomainConstants.DEVICE_TEE_IMPORT_KEY_ALIAS, messageTekEncryptedAtClient)
        log.d("CLIENT: Decrypt the Server's text message with the imported key - success")
    }

    private fun getKeyFinalType(clientStorage: Storage.ClientSide): KeyImportTestResult {
        val text = "Test 1 message: ${clientStorage.decryptedServerTextMessage}"
        return when (client.getSecretKeySecurityLevel(DomainConstants.DEVICE_TEE_IMPORT_KEY_ALIAS)) {
            CryptoClientGateway.KeySecurityLevel.TeeStrongbox -> KeyImportTestResult.SuccessHardwareTeeStrongBox(text)
            CryptoClientGateway.KeySecurityLevel.TeeHardwareNoStrongbox -> KeyImportTestResult.SuccessHardwareTeeNoStrongbox(text)
            CryptoClientGateway.KeySecurityLevel.TeeSoftware -> KeyImportTestResult.SuccessSoftwareTeeOnly(text)
            CryptoClientGateway.KeySecurityLevel.Unknown -> KeyImportTestResult.SuccessTeeUnknown(text)
        }.exhaustive
    }

    sealed class Storage {
        class ClientSide : Storage() {
            lateinit var wrappingRsaKeyPairPublicKeyAtClient: PublicKey
            lateinit var wrappingRsaKeyPairAsJwk: String
            var wasGeneratedInStrongbox: Boolean = false
            lateinit var asn1DerBase64EncodedTekAndCekAtServer: String
            lateinit var messageTekEncryptedEncodedBase64: String
            lateinit var messageTekEncryptedJweEncodedBase64: String
            lateinit var decryptedServerTextMessage: String
            lateinit var decryptedServerJweMessage: String
        }

        class ServerSide(val serverSecretMessageAtServer: String) : Storage() {
            lateinit var asn1DerBase64EncodedTekAndCekAtServer: String
            lateinit var tekAesKeyAtServer: SecretKeySpec
            lateinit var wrappingRsaKeyPairAsJwk: String
            lateinit var messageTekEncryptedEncodedBase64: String
            lateinit var messageTekEncryptedJweEncodedBase64: String
        }
    }

    companion object {
        private const val TEK_KEY_SIZE_BYTES = 256
        private const val CEK_KEY_SIZE_BYTES = 256
        private const val KEY_MASTER_ALGORITHM_AES = 32
        private const val BASE64_ENCODING_CHARSET_NAME = "UTF-8"
    }
}
