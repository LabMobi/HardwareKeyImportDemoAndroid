package mobi.lab.keyimportdemo.infrastructure.crypto

import com.nimbusds.jose.EncryptionMethod

object CryptoUtil {
    @Suppress("MagicNumber")
    fun Int.toBitsFromBytes(): Int {
        return this * 8
    }

    @Suppress("MagicNumber")
    fun Int.toBytesFromBits(): Int {
        return this / 8
    }

    @Suppress("MagicNumber")
    fun getEncryptionMethodForKeySize(keySizeBits: Int): EncryptionMethod {
        val enc = when (keySizeBits) {
            128 -> {
                EncryptionMethod.A128GCM
            }
            192 -> {
                EncryptionMethod.A192GCM
            }
            256 -> {
                EncryptionMethod.A256GCM
            }
            else -> {
                throw IllegalArgumentException("Unsupported key size $keySizeBits!")
            }
        }
        return enc
    }
}
