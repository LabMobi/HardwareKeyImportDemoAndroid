package mobi.lab.keyimportdemo.infrastructure.crypto

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import android.security.keystore.WrappedKeyEntry
import androidx.annotation.RequiresApi
import com.nimbusds.jose.JWEObject
import com.nimbusds.jose.crypto.DirectDecrypter
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.jwk.RSAKey
import mobi.lab.keyimportdemo.domain.entities.DomainException
import mobi.lab.keyimportdemo.domain.gateway.CryptoClientGateway
import java.security.Key
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
import java.security.spec.MGF1ParameterSpec
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource
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
    override fun getSecretKeySecurityLevel(keyStoreKeyAlias: String): CryptoClientGateway.KeySecurityLevel {
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
    override fun getPrivateKeySecurityLevel(keyStoreKeyAlias: String): CryptoClientGateway.KeySecurityLevel {
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
    private fun getSecretKeySecurityLevelFromApi(key: SecretKey): CryptoClientGateway.KeySecurityLevel {
        val factory: SecretKeyFactory = SecretKeyFactory.getInstance(key.algorithm, KEY_STORE_PROVIDER_ANDROID_KEYSTORE)
        return when ((factory.getKeySpec(key, KeyInfo::class.java) as KeyInfo).securityLevel) {
            KeyProperties.SECURITY_LEVEL_STRONGBOX -> {
                CryptoClientGateway.KeySecurityLevel.TeeStrongbox
            }
            KeyProperties.SECURITY_LEVEL_TRUSTED_ENVIRONMENT, KeyProperties.SECURITY_LEVEL_UNKNOWN_SECURE -> {
                CryptoClientGateway.KeySecurityLevel.TeeHardwareNoStrongbox
            }
            KeyProperties.SECURITY_LEVEL_SOFTWARE -> {
                CryptoClientGateway.KeySecurityLevel.TeeSoftware
            }
            else -> {
                CryptoClientGateway.KeySecurityLevel.Unknown
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun getPrivateKeySecurityLevelFromApi(key: PrivateKey): CryptoClientGateway.KeySecurityLevel {
        val factory: KeyFactory = KeyFactory.getInstance(key.algorithm, KEY_STORE_PROVIDER_ANDROID_KEYSTORE)
        return when ((factory.getKeySpec(key, KeyInfo::class.java) as KeyInfo).securityLevel) {
            KeyProperties.SECURITY_LEVEL_STRONGBOX -> {
                CryptoClientGateway.KeySecurityLevel.TeeStrongbox
            }
            KeyProperties.SECURITY_LEVEL_TRUSTED_ENVIRONMENT, KeyProperties.SECURITY_LEVEL_UNKNOWN_SECURE -> {
                CryptoClientGateway.KeySecurityLevel.TeeHardwareNoStrongbox
            }
            KeyProperties.SECURITY_LEVEL_SOFTWARE -> {
                CryptoClientGateway.KeySecurityLevel.TeeSoftware
            }
            else -> {
                CryptoClientGateway.KeySecurityLevel.Unknown
            }
        }
    }

    private fun isSecureKeyInsideSecureHardwareCompat(key: SecretKey): CryptoClientGateway.KeySecurityLevel {
        val factory: SecretKeyFactory = SecretKeyFactory.getInstance(key.algorithm, KEY_STORE_PROVIDER_ANDROID_KEYSTORE)
        @Suppress("DEPRECATION")
        return if ((factory.getKeySpec(key, KeyInfo::class.java) as KeyInfo).isInsideSecureHardware) {
            CryptoClientGateway.KeySecurityLevel.TeeHardwareNoStrongbox
        } else {
            CryptoClientGateway.KeySecurityLevel.TeeSoftware
        }
    }

    private fun isPrivateKeyInsideSecureHardwareCompat(key: PrivateKey): CryptoClientGateway.KeySecurityLevel {
        val factory: KeyFactory = KeyFactory.getInstance(key.algorithm, KEY_STORE_PROVIDER_ANDROID_KEYSTORE)
        @Suppress("DEPRECATION")
        return if ((factory.getKeySpec(key, KeyInfo::class.java) as KeyInfo).isInsideSecureHardware) {
            CryptoClientGateway.KeySecurityLevel.TeeHardwareNoStrongbox
        } else {
            CryptoClientGateway.KeySecurityLevel.TeeSoftware
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
    override fun decryptTextWithImportedKey(keyStoreKeyAlias: String, messageTekEncryptedAtClient: ByteArray): String {
        val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null, null)
        val key: SecretKey = keyStore.getKey(keyStoreKeyAlias, null) as SecretKey

        val c: Cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
        // First 16 byte are iv
        val ivPart: ByteArray = messageTekEncryptedAtClient.copyOfRange(0, 16)
        val messagePart: ByteArray = messageTekEncryptedAtClient.copyOfRange(16, messageTekEncryptedAtClient.size)
        val ivParamSpec = IvParameterSpec(ivPart)
        c.init(Cipher.DECRYPT_MODE, key, ivParamSpec)
        return String(c.doFinal(messagePart))
    }

    override fun encryptTextWithWrappingKey(keyStoreKeyAlias: String, secretMessage: String): ByteArray {
        val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null, null)

        val privateKeyEntry: KeyStore.PrivateKeyEntry? = keyStore.getEntry(keyStoreKeyAlias, null) as KeyStore.PrivateKeyEntry?
        if (privateKeyEntry == null) {
            throw DomainException.noSuchImportedKeyFound(keyStoreKeyAlias)
        }

        val cipher = Cipher.getInstance("RSA/ECB/OAEPPadding")
        cipher.init(
            Cipher.ENCRYPT_MODE,
            privateKeyEntry.certificate.publicKey,
            OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT)
        )
        return cipher.doFinal(secretMessage.toByteArray())
    }

    override fun decryptTextWithWrappingKey(keyStoreKeyAlias: String, messageTekEncryptedAtClient: ByteArray): String {
        val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null, null)

        val privateKeyEntry: KeyStore.PrivateKeyEntry? = keyStore.getEntry(keyStoreKeyAlias, null) as KeyStore.PrivateKeyEntry?
        if (privateKeyEntry == null) {
            throw DomainException.noSuchImportedKeyFound(keyStoreKeyAlias)
        }

        val cipher = Cipher.getInstance("RSA/ECB/OAEPPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            privateKeyEntry.privateKey,
            OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT)
        )
        return String(cipher.doFinal(messageTekEncryptedAtClient))
    }

    override fun encryptTextWithImportedKey(keyStoreKeyAlias: String, secretMessage: String): ByteArray {
        val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null, null)
        val rawKey: Key? = keyStore.getKey(keyStoreKeyAlias, null)
        if (rawKey == null) {
            throw DomainException.noSuchImportedKeyFound(keyStoreKeyAlias)
        }
        val key: SecretKey = rawKey as SecretKey

        val c: Cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
        c.init(Cipher.ENCRYPT_MODE, key)
        return c.iv + c.doFinal(secretMessage.toByteArray())
    }

    @Suppress("MagicNumber")
    override fun decryptJWEWithImportedWrappedKey(keyStoreKeyAlias: String, messageWrappedTekEncryptedJWE: String): String {
        val keyStore: KeyStore = KeyStore.getInstance(KEY_STORE_PROVIDER_ANDROID_KEYSTORE)
        keyStore.load(null, null)
        val secretKeyEntry = keyStore.getEntry(keyStoreKeyAlias, null) as KeyStore.SecretKeyEntry

        val decrypter = DirectDecrypter(secretKeyEntry.secretKey)
        decrypter.jcaContext.provider = keyStore.provider

        val jweObject = JWEObject.parse(messageWrappedTekEncryptedJWE)
        jweObject.decrypt(decrypter)
        return jweObject.payload.toString()
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
