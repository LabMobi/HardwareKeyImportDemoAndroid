package mobi.lab.keyimportdemo.infrastructure.crypto

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import android.security.keystore.WrappedKeyEntry
import androidx.annotation.RequiresApi
import com.nimbusds.jose.EncryptionMethod
import com.nimbusds.jose.JWEAlgorithm
import com.nimbusds.jose.JWEHeader
import com.nimbusds.jose.JWEObject
import com.nimbusds.jose.Payload
import com.nimbusds.jose.crypto.DirectDecrypter
import com.nimbusds.jose.crypto.DirectEncrypter
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.jwk.RSAKey
import mobi.lab.keyimportdemo.domain.gateway.CryptoClientGateway
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException
import java.security.PrivateKey
import java.security.PublicKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.AlgorithmParameterSpec
import java.security.spec.InvalidKeySpecException
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import kotlin.random.Random

@Suppress("EmptyClassBlock")
class CryptoClient @Inject constructor() : CryptoClientGateway {

    @RequiresApi(Build.VERSION_CODES.P)
    override fun generateRsaKeyPairInDeviceTee(alias: String, isStrongBoxBacked: Boolean): PublicKey {
        val kpg: KeyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, KEY_STORE_PROVIDER_ANDROID_KEYSTORE)
        // Note: The KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT are here only for the extra tests, not needed for the import!
        val builder =
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_WRAP_KEY or KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
                .setBlockModes(KeyProperties.BLOCK_MODE_ECB)

        if (isStrongBoxBacked) {
            // Only call this if we need to set it true.
            // Otherwise, for devices that do not support it calling the method will throw
            builder.setIsStrongBoxBacked(true)
        }
        kpg.initialize(builder.build())
        return kpg.generateKeyPair().public
    }

    @Throws(NoSuchAlgorithmException::class, NoSuchProviderException::class, InvalidKeySpecException::class)
    override fun getSecretKeySecurityLevel(keyStoreKeyAlias: String): CryptoClientGateway.KeyTeeSecurityLevel {
        val keyStore: KeyStore = KeyStore.getInstance(KEY_STORE_PROVIDER_ANDROID_KEYSTORE)
        keyStore.load(null, null)
        val key: SecretKey = keyStore.getKey(keyStoreKeyAlias, null) as SecretKey
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSecretKeySecurityLevelFromApi(key)
        } else {
            isSecureKeyInsideSecureHardwareCompat(key)
        }
    }

    @Throws(NoSuchAlgorithmException::class, NoSuchProviderException::class, InvalidKeySpecException::class)
    override fun getPrivateKeySecurityLevel(keyStoreKeyAlias: String): CryptoClientGateway.KeyTeeSecurityLevel {
        val keyStore: KeyStore = KeyStore.getInstance(KEY_STORE_PROVIDER_ANDROID_KEYSTORE)
        keyStore.load(null, null)
        val privateKeyEntry = keyStore.getEntry(keyStoreKeyAlias, null) as KeyStore.PrivateKeyEntry
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getPrivateKeySecurityLevelFromApi(privateKeyEntry.privateKey)
        } else {
            isPrivateKeyInsideSecureHardwareCompat(privateKeyEntry.privateKey)
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun getSecretKeySecurityLevelFromApi(key: SecretKey): CryptoClientGateway.KeyTeeSecurityLevel {
        val factory: SecretKeyFactory = SecretKeyFactory.getInstance(key.algorithm, KEY_STORE_PROVIDER_ANDROID_KEYSTORE)
        return when ((factory.getKeySpec(key, KeyInfo::class.java) as KeyInfo).securityLevel) {
            KeyProperties.SECURITY_LEVEL_STRONGBOX -> {
                CryptoClientGateway.KeyTeeSecurityLevel.TeeStrongbox
            }
            KeyProperties.SECURITY_LEVEL_TRUSTED_ENVIRONMENT, KeyProperties.SECURITY_LEVEL_UNKNOWN_SECURE -> {
                CryptoClientGateway.KeyTeeSecurityLevel.TeeHardwareNoStrongbox
            }
            KeyProperties.SECURITY_LEVEL_SOFTWARE -> {
                CryptoClientGateway.KeyTeeSecurityLevel.TeeSoftware
            }
            else -> {
                CryptoClientGateway.KeyTeeSecurityLevel.Unknown
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun getPrivateKeySecurityLevelFromApi(key: PrivateKey): CryptoClientGateway.KeyTeeSecurityLevel {
        val factory: KeyFactory = KeyFactory.getInstance(key.algorithm, KEY_STORE_PROVIDER_ANDROID_KEYSTORE)
        return when ((factory.getKeySpec(key, KeyInfo::class.java) as KeyInfo).securityLevel) {
            KeyProperties.SECURITY_LEVEL_STRONGBOX -> {
                CryptoClientGateway.KeyTeeSecurityLevel.TeeStrongbox
            }
            KeyProperties.SECURITY_LEVEL_TRUSTED_ENVIRONMENT, KeyProperties.SECURITY_LEVEL_UNKNOWN_SECURE -> {
                CryptoClientGateway.KeyTeeSecurityLevel.TeeHardwareNoStrongbox
            }
            KeyProperties.SECURITY_LEVEL_SOFTWARE -> {
                CryptoClientGateway.KeyTeeSecurityLevel.TeeSoftware
            }
            else -> {
                CryptoClientGateway.KeyTeeSecurityLevel.Unknown
            }
        }
    }

    private fun isSecureKeyInsideSecureHardwareCompat(key: SecretKey): CryptoClientGateway.KeyTeeSecurityLevel {
        val factory: SecretKeyFactory = SecretKeyFactory.getInstance(key.algorithm, KEY_STORE_PROVIDER_ANDROID_KEYSTORE)
        @Suppress("DEPRECATION")
        return if ((factory.getKeySpec(key, KeyInfo::class.java) as KeyInfo).isInsideSecureHardware) {
            CryptoClientGateway.KeyTeeSecurityLevel.TeeHardwareNoStrongbox
        } else {
            CryptoClientGateway.KeyTeeSecurityLevel.TeeSoftware
        }
    }

    private fun isPrivateKeyInsideSecureHardwareCompat(key: PrivateKey): CryptoClientGateway.KeyTeeSecurityLevel {
        val factory: KeyFactory = KeyFactory.getInstance(key.algorithm, KEY_STORE_PROVIDER_ANDROID_KEYSTORE)
        @Suppress("DEPRECATION")
        return if ((factory.getKeySpec(key, KeyInfo::class.java) as KeyInfo).isInsideSecureHardware) {
            CryptoClientGateway.KeyTeeSecurityLevel.TeeHardwareNoStrongbox
        } else {
            CryptoClientGateway.KeyTeeSecurityLevel.TeeSoftware
        }
    }

    override fun encodeRsaPublicKeyAsJwk(alias: String, publicKey: PublicKey): String {
        val jwk: JWK = RSAKey.Builder(publicKey as RSAPublicKey)
            .keyID(alias)
            .build()
        return jwk.toJSONString()
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun importWrappedKeyFromServer(asn1DerEncodedWrappedKey: ByteArray, wrappingKeyAliasInKeysStore: String, wrappedKeyAlias: String) {
        val keyStore: KeyStore = KeyStore.getInstance(KEY_STORE_PROVIDER_ANDROID_KEYSTORE)
        keyStore.load(null, null)
        val spec: AlgorithmParameterSpec = KeyGenParameterSpec.Builder(wrappingKeyAliasInKeysStore, KeyProperties.PURPOSE_WRAP_KEY)
            .setDigests(KeyProperties.DIGEST_SHA256)
            .build()
        val wrappedKeyEntry = WrappedKeyEntry(asn1DerEncodedWrappedKey, wrappingKeyAliasInKeysStore, "RSA/ECB/OAEPPadding", spec)
        keyStore.setEntry(wrappedKeyAlias, wrappedKeyEntry, null)
    }

    @Suppress("MagicNumber")
    override fun decryptJWEWithImportedKey(keyStoreKeyAlias: String, messageWrappedTekEncryptedJWE: String): String {
        val keyStore: KeyStore = KeyStore.getInstance(KEY_STORE_PROVIDER_ANDROID_KEYSTORE)
        keyStore.load(null, null)
        val secretKeyEntry = keyStore.getEntry(keyStoreKeyAlias, null) as KeyStore.SecretKeyEntry

        val decrypter = DirectDecrypter(secretKeyEntry.secretKey)
        decrypter.jcaContext.provider = keyStore.provider

        val jweObject = JWEObject.parse(messageWrappedTekEncryptedJWE)
        jweObject.decrypt(decrypter)
        return jweObject.payload.toString()
    }

    override fun encryptMessageWithTekToJWE(message: String, keyStoreKeyAlias: String): String {
        val keyStore: KeyStore = KeyStore.getInstance(KEY_STORE_PROVIDER_ANDROID_KEYSTORE)
        keyStore.load(null, null)
        val secretKeyEntry = keyStore.getEntry(keyStoreKeyAlias, null) as KeyStore.SecretKeyEntry

        val header = JWEHeader(JWEAlgorithm.DIR, EncryptionMethod.A256GCM)
        // Set the message as payload plain text
        val payload = Payload(message)

        // Create the JWE object and encrypt it
        val jweObject = JWEObject(header, payload)
        val encrypter = DirectEncrypter(secretKeyEntry.secretKey)
        encrypter.jcaContext.provider = keyStore.provider

        jweObject.encrypt(encrypter)

        // Serialise to compact JOSE form
        return jweObject.serialize()
    }

    @Suppress("MagicNumber", "unused")
    private fun generateFakeDecryptionKey(@Suppress("SameParameterValue") keySize: Int): SecretKey {
        val arraySize = keySize / 8
        val aesKeyBytes = ByteArray(arraySize)
        Random.nextBytes(aesKeyBytes)
        return SecretKeySpec(aesKeyBytes, "AES")
    }

    companion object {
        private const val KEY_STORE_PROVIDER_ANDROID_KEYSTORE = "AndroidKeyStore"
    }
}
