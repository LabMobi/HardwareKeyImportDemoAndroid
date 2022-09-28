package mobi.lab.keyimportdemo.domain.gateway

import java.security.PublicKey

@Suppress("EmptyClassBlock")
interface CryptoClientGateway {
    fun generateRsaKeyPairInDeviceTee(alias: String, isStrongBoxBacked: Boolean): PublicKey
    fun getSecretKeySecurityLevel(keyStoreKeyAlias: String): KeyTeeSecurityLevel
    fun getPrivateKeySecurityLevel(keyStoreKeyAlias: String): KeyTeeSecurityLevel
    fun encodeRsaPublicKeyAsJwk(alias: String, publicKey: PublicKey): String
    fun importWrappedKeyFromServer(asn1DerEncodedWrappedKey: ByteArray, wrappingKeyAliasInKeysStore: String, wrappedKeyAlias: String)
    fun decryptJWEWithImportedKey(keyStoreKeyAlias: String, messageWrappedTekEncryptedJWE: String): String
    fun encryptMessageWithTekToJWE(message: String, keyStoreKeyAlias: String, keySizeBits: Int): String

    sealed class KeyTeeSecurityLevel {
        object TeeStrongbox : KeyTeeSecurityLevel() {
            override fun toString(): String {
                return "KeySecurityLevel.TeeStrongbox"
            }
        }

        object TeeHardwareNoStrongbox : KeyTeeSecurityLevel() {
            override fun toString(): String {
                return "KeySecurityLevel.TeeHardwareNoStrongbox"
            }
        }

        object TeeSoftware : KeyTeeSecurityLevel() {
            override fun toString(): String {
                return "KeySecurityLevel.TeeSoftware"
            }
        }

        object Unknown : KeyTeeSecurityLevel() {
            override fun toString(): String {
                return "KeySecurityLevel.Unknown"
            }
        }
    }
}
