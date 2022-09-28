package mobi.lab.keyimportdemo.infrastructure.crypto

import com.nimbusds.jose.JWEAlgorithm
import com.nimbusds.jose.JWEHeader
import com.nimbusds.jose.JWEObject
import com.nimbusds.jose.Payload
import com.nimbusds.jose.crypto.DirectDecrypter
import com.nimbusds.jose.crypto.DirectEncrypter
import com.nimbusds.jose.jwk.JWK
import mobi.lab.keyimportdemo.domain.gateway.CryptoServerGateway
import mobi.lab.keyimportdemo.infrastructure.crypto.CryptoUtil.toBitsFromBytes
import mobi.lab.keyimportdemo.infrastructure.crypto.CryptoUtil.toBytesFromBits
import org.bouncycastle.asn1.ASN1Encodable
import org.bouncycastle.asn1.ASN1EncodableVector
import org.bouncycastle.asn1.DERInteger
import org.bouncycastle.asn1.DERNull
import org.bouncycastle.asn1.DEROctetString
import org.bouncycastle.asn1.DERSequence
import org.bouncycastle.asn1.DERSet
import org.bouncycastle.asn1.DERTaggedObject
import java.security.PublicKey
import java.security.spec.MGF1ParameterSpec
import java.util.Arrays
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import kotlin.random.Random.Default.nextBytes

@Suppress("EmptyClassBlock")
class CryptoServer @Inject constructor() : CryptoServerGateway {
    override fun decodeRsaPublicKeyFromJWKString(jwkString: String): PublicKey {
        return JWK.parse(jwkString).toRSAKey().toRSAPublicKey()
    }

    @Suppress("MagicNumber")
    override fun generateAesTek(keySizeBits: Int): SecretKeySpec {
        // Generate keySize bit AES key for TEK
        val keySizeBytes = keySizeBits.toBytesFromBits()
        val aesKeyBytes = ByteArray(keySizeBytes)
        nextBytes(aesKeyBytes)
        return SecretKeySpec(aesKeyBytes, "AES")
    }

    @Suppress("MagicNumber")
    private fun makeSecureKeyAuthorizationList(keySizeBits: Int, algorithmInt: Int): DERSequence {
        // Make an AuthorizationList to describe the secure key
        // https://developer.android.com/training/articles/security-key-attestation.html#verifying
        val allPurposes = ASN1EncodableVector()
        allPurposes.add(DERInteger(KM_PURPOSE_ENCRYPT))
        allPurposes.add(DERInteger(KM_PURPOSE_DECRYPT))
        val purposeSet = DERSet(allPurposes)
        val purpose = DERTaggedObject(true, 1, purposeSet)
        val algorithm = DERTaggedObject(true, 2, DERInteger(algorithmInt))
        // Size in bits https://source.android.com/docs/security/features/keystore/tags#key_size
        val keySize = DERTaggedObject(true, 3, DERInteger(keySizeBits))
        val allBlockModes = ASN1EncodableVector()
        allBlockModes.add(DERInteger(KM_MODE_ECB))
        allBlockModes.add(DERInteger(KM_MODE_CBC))
        val blockModeSet = DERSet(allBlockModes)
        val blockMode = DERTaggedObject(true, 4, blockModeSet)
        val allPaddings = ASN1EncodableVector()
        allPaddings.add(DERInteger(KM_PAD_PKCS7))
        allPaddings.add(DERInteger(KM_PAD_NONE))
        val paddingSet = DERSet(allPaddings)
        val padding = DERTaggedObject(true, 6, paddingSet)
        val noAuthRequired = DERTaggedObject(true, 503, DERNull.INSTANCE)
        // Build sequence
        val allItems = ASN1EncodableVector()
        allItems.add(purpose)
        allItems.add(algorithm)
        allItems.add(keySize)
        allItems.add(blockMode)
        allItems.add(padding)
        allItems.add(noAuthRequired)
        return DERSequence(allItems)
    }

    override fun generateTekImportMetadata(keySizeBits: Int, keyMasterAlgorithm: Int): DERSequence {
        val authorizationList = makeSecureKeyAuthorizationList(keySizeBits, keyMasterAlgorithm)
        // Build description
        val descriptionItems = ASN1EncodableVector()
        descriptionItems.add(DERInteger(KM_KEY_FORMAT_RAW))
        descriptionItems.add(authorizationList)
        return DERSequence(descriptionItems)
    }

    @Suppress("MagicNumber")
    override fun generateAesCek(cekKeySizeBits: Int): SecretKeySpec {
        // Generate an AES key. This is the ephemeral key used to encrypt the secure key.
        val aesKeyBytes = ByteArray(cekKeySizeBits.toBytesFromBits())
        nextBytes(aesKeyBytes)
        return SecretKeySpec(aesKeyBytes, "AES")
    }

    override fun encryptCekWithRsaPublicKey(cekAesKey: SecretKeySpec, rsaKeyPairPublicKey: PublicKey): ByteArray {
        // Encrypt ephemeral keys
        val spec = OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT)
        val pkCipher: Cipher = Cipher.getInstance("RSA/ECB/OAEPPadding")
        pkCipher.init(Cipher.ENCRYPT_MODE, rsaKeyPairPublicKey, spec)
        return pkCipher.doFinal(cekAesKey.encoded)
    }

    @Suppress("MagicNumber")
    override fun encryptTekWithCek(
        tekAesWrappedKey: SecretKeySpec,
        wrappedKeyDescription: DERSequence,
        cekAesKey: SecretKeySpec
    ): CryptoServerGateway.EncryptedTekWrapper {
        // Generate 12 byte initialization vector
        val initializationVector = ByteArray(12)
        nextBytes(initializationVector)
        // Encrypt secure key
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmParameterSpec = GCMParameterSpec(GCM_TAG_SIZE_BITS, initializationVector)
        cipher.init(Cipher.ENCRYPT_MODE, cekAesKey, gcmParameterSpec)
        val aad: ByteArray = wrappedKeyDescription.encoded
        cipher.updateAAD(aad)
        var encryptedSecureKey = cipher.doFinal(tekAesWrappedKey.encoded)
        // Get GCM tag. Java puts the tag at the end of the ciphertext data :(
        val len = encryptedSecureKey.size
        val tagSizeBytes: Int = GCM_TAG_SIZE_BITS.toBytesFromBits()
        val tag: ByteArray = Arrays.copyOfRange(encryptedSecureKey, len - tagSizeBytes, len)
        // Remove GCM tag from end of output
        encryptedSecureKey = Arrays.copyOfRange(encryptedSecureKey, 0, len - tagSizeBytes)
        // Return both
        return CryptoServerGateway.EncryptedTekWrapper(encryptedSecureKey, tag, initializationVector)
    }

    override fun encodeTekAndCetToAsn1Der(
        tekEncryptedWrapper: CryptoServerGateway.EncryptedTekWrapper,
        cekEncrypted: ByteArray,
        tekImportMetadata: DERSequence
    ): ByteArray {
        // Build ASN.1 DER encoded sequence WrappedKeyWrapper
        val items = ASN1EncodableVector()
        items.add(DERInteger(WRAPPED_FORMAT_VERSION))
        items.add(DEROctetString(cekEncrypted))
        items.add(DEROctetString(tekEncryptedWrapper.initializationVector))
        items.add(tekImportMetadata)
        items.add(DEROctetString(tekEncryptedWrapper.encryptedTek))
        items.add(DEROctetString(tekEncryptedWrapper.tag))
        return DERSequence(items).getEncoded(ASN1Encodable.DER)
    }

    override fun encryptMessageWithTekToJWE(message: String, tekAesKeyAtServer: SecretKeySpec): String {
        // Create the header
        // Uses GCM for now,
        // for more info see https://bitbucket.org/connect2id/nimbus-jose-jwt/issues/490/jwe-with-shared-key-support-for-android
        val enc = CryptoUtil.getEncryptionMethodForKeySize(tekAesKeyAtServer.encoded.size.toBitsFromBytes())
        val header = JWEHeader(JWEAlgorithm.DIR, enc)
        // Set the message as payload plain text
        val payload = Payload(message)

        // Create the JWE object and encrypt it
        val jweObject = JWEObject(header, payload)
        jweObject.encrypt(DirectEncrypter(tekAesKeyAtServer))

        // Serialise to compact JOSE form...
        return jweObject.serialize()
    }

    override fun decryptJWEWithImportedKey(messageWrappedTekEncryptedJWE: String, tekAesKeyAtServer: SecretKeySpec): String {
        val decrypter = DirectDecrypter(tekAesKeyAtServer)

        val jweObject = JWEObject.parse(messageWrappedTekEncryptedJWE)
        jweObject.decrypt(decrypter)
        return jweObject.payload.toString()
    }

    companion object {
        // These values afaik do not have public value available
        // See https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/security/keymaster/KeymasterDefs.java
        private const val KM_PURPOSE_ENCRYPT = 0
        private const val KM_PURPOSE_DECRYPT = 1
        private const val KM_MODE_ECB = 1
        private const val KM_MODE_CBC = 2
        private const val KM_PAD_NONE = 1
        private const val KM_PAD_PKCS7 = 64
        private const val KM_KEY_FORMAT_RAW = 3
        private const val GCM_TAG_SIZE_BITS = 128
        private const val WRAPPED_FORMAT_VERSION = 0
    }
}
