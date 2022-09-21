package mobi.lab.keyimportdemo.domain.gateway

import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException
import java.security.PublicKey
import java.security.spec.InvalidKeySpecException

@Suppress("EmptyClassBlock")
interface CryptoClientGateway {
    @Throws(Exception::class)
    fun generateRsaKeyPairInDeviceTee(alias: String, isStrongBoxBacked: Boolean): PublicKey

    @Throws(NoSuchAlgorithmException::class, NoSuchProviderException::class, InvalidKeySpecException::class)
    fun getSecretKeySecurityLevel(keyStoreKeyAlias: String): KeySecurityLevel

    @Throws(NoSuchAlgorithmException::class, NoSuchProviderException::class, InvalidKeySpecException::class)
    fun getPrivateKeySecurityLevel(keyStoreKeyAlias: String): CryptoClientGateway.KeySecurityLevel

    @Throws(Exception::class)
    fun encodeRsaPublicKeyAsJwk(alias: String, publicKey: PublicKey): String

    @Throws(Exception::class)
    fun importWrappedKeyFromServer(asn1DerEncodedWrappedKey: ByteArray, wrappingKeyAliasInKeysStore: String, wrappedKeyAlias: String)

    @Throws(Exception::class)
    fun decryptJWEWithImportedWrappedKey(keyStoreKeyAlias: String, messageWrappedTekEncryptedJWE: String): String

    @Throws(Exception::class)
    fun encryptTextWithImportedKey(keyStoreKeyAlias: String, secretMessage: String): ByteArray

    @Throws(Exception::class)
    fun decryptTextWithImportedKey(keyStoreKeyAlias: String, messageTekEncryptedAtClient: ByteArray): String

    @Throws(Exception::class)
    fun encryptTextWithWrappingKey(keyStoreKeyAlias: String, secretMessage: String): ByteArray

    @Throws(Exception::class)
    fun decryptTextWithWrappingKey(keyStoreKeyAlias: String, messageTekEncryptedAtClient: ByteArray): String

    sealed class KeySecurityLevel {
        object TeeStrongbox : KeySecurityLevel() {
            override fun toString(): String {
                return "KeySecurityLevel.TeeStrongbox"
            }
        }

        object TeeHardwareNoStrongbox : KeySecurityLevel() {
            override fun toString(): String {
                return "KeySecurityLevel.TeeHardwareNoStrongbox"
            }
        }

        object TeeSoftware : KeySecurityLevel() {
            override fun toString(): String {
                return "KeySecurityLevel.TeeSoftware"
            }
        }

        object Unknown : KeySecurityLevel() {
            override fun toString(): String {
                return "KeySecurityLevel.Unknown"
            }
        }
    }
}
