package mobi.lab.keyimportdemo.domain.usecases.crypto

import android.os.Build
import android.security.keystore.SecureKeyImportUnavailableException
import androidx.annotation.RequiresApi
import dagger.Reusable
import io.reactivex.rxjava3.core.Single
import mobi.lab.keyimportdemo.domain.DomainConstants
import mobi.lab.keyimportdemo.domain.entities.KeyImportTestResult
import mobi.lab.keyimportdemo.domain.entities.KeyUsageTestResult
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
    private val saveServerSecretKeyUseCase: SaveServerSecretKeyUseCase,
    private val importedKeyTwoWayUsageUseCase: ImportedKeyTwoWayUsageUseCase
) : UseCase() {
    fun execute(serverSecretMessage: String, clientSecretMessage: String): Single<KeyImportTestResult> {
        return Single.fromCallable { runTest(serverSecretMessage, clientSecretMessage) }
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
    private fun runTest(serverSecretMessage: String, clientSecretMessage: String): KeyImportTestResult {
        log.d("Import started")

        // Storage accessible for "client".
        val clientStorage = Storage.ClientSide()

        // Storage accessible for "server"
        // In real word would be in the server instance.
        val serverStorage = Storage.ServerSide()

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

        // Test messaging between the client and server both ways using the imported AES TEK
        val keyTestResult = importedKeyTwoWayUsageUseCase.execute(serverSecretMessage, clientSecretMessage).blockingGet()

        log.d("Import finished")
        return getKeyFinalType(keyTestResult)
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

        // Save the key locally for additional tests (the other button in the UI)
        saveServerSecretKeyUseCase.execute(serverStorage.tekAesKeyAtServer.encoded)

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
            asn1DerEncodedTekAndCekAtClient, DomainConstants.DEVICE_TEE_WRAPPING_KEY_ALIAS, DomainConstants.DEVICE_TEE_IMPORT_KEY_ALIAS
        )
        log.d("CLIENT: Importing the server wrapped key to device TEE keystore - success")
    }

    private fun getKeyFinalType(keyTestResult: KeyUsageTestResult): KeyImportTestResult {
        return KeyImportTestResult.Success(keyTestResult)
    }

    sealed class Storage {
        class ClientSide : Storage() {
            lateinit var wrappingRsaKeyPairPublicKeyAtClient: PublicKey
            lateinit var wrappingRsaKeyPairAsJwk: String
            var wasGeneratedInStrongbox: Boolean = false
            lateinit var asn1DerBase64EncodedTekAndCekAtServer: String
        }

        class ServerSide : Storage() {
            lateinit var asn1DerBase64EncodedTekAndCekAtServer: String
            lateinit var tekAesKeyAtServer: SecretKeySpec
            lateinit var wrappingRsaKeyPairAsJwk: String
        }
    }

    companion object {
        private const val TEK_KEY_SIZE_BYTES = 256
        private const val CEK_KEY_SIZE_BYTES = 256
        private const val KEY_MASTER_ALGORITHM_AES = 32
        private const val BASE64_ENCODING_CHARSET_NAME = "UTF-8"
    }
}
