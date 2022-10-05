package mobi.lab.keyimportdemo.domain.gateway

import org.bouncycastle.asn1.DERSequence
import java.security.PublicKey
import javax.crypto.spec.SecretKeySpec

@Suppress("EmptyClassBlock")
interface CryptoServerGateway {

    @Throws(Exception::class)
    fun decodeRsaPublicKeyFromJWKString(jwkString: String): PublicKey
    fun generateAesTek(keySizeBits: Int): SecretKeySpec
    fun generateTekImportMetadata(keySizeBits: Int, keyMasterAlgorithm: Int): DERSequence
    fun generateAesCek(cekKeySizeBits: Int): SecretKeySpec
    fun encryptCekWithRsaPublicKey(cekAesKey: SecretKeySpec, rsaKeyPairPublicKey: PublicKey): ByteArray
    fun encryptTekWithCek(tekAesWrappedKey: SecretKeySpec, wrappedKeyDescription: DERSequence, cekAesKey: SecretKeySpec): EncryptedTekWrapper
    fun encodeTekAndCetToAsn1Der(tekEncryptedWrapper: EncryptedTekWrapper, cekEncrypted: ByteArray, tekImportMetadata: DERSequence): ByteArray
    fun encryptMessageWithTekToJWE(message: String, tekAesKeyAtServer: SecretKeySpec): String
    fun decryptJWEWithImportedKey(messageWrappedTekEncryptedJWE: String, tekAesKeyAtServer: SecretKeySpec): String

    @Suppress("ArrayInDataClass")
    data class EncryptedTekWrapper(val encryptedTek: ByteArray, val tag: ByteArray, val initializationVector: ByteArray)
}
