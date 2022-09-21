/*
 * nimbus-jose-jwt
 *
 * Copyright 2012-2016, Connect2id Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the
 * License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package mobi.lab.keyimportdemo.infrastructure.crypto;


import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.nimbusds.jose.crypto.impl.AAD;
import com.nimbusds.jose.crypto.impl.AESCBC;
import com.nimbusds.jose.crypto.impl.AlgorithmSupportMessage;
import com.nimbusds.jose.crypto.impl.AuthenticatedCipherText;
import com.nimbusds.jose.crypto.impl.CipherHelper;
import com.nimbusds.jose.crypto.impl.CompositeKey;
import com.nimbusds.jose.crypto.impl.DeflateHelper;
import com.nimbusds.jose.crypto.impl.DirectCryptoProvider;
import net.jcip.annotations.ThreadSafe;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.impl.HMAC;
import com.nimbusds.jose.jca.JWEJCAContext;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jose.util.ByteUtils;
import com.nimbusds.jose.util.IntegerOverflowException;

import java.nio.ByteBuffer;
import java.security.Provider;
import java.util.Arrays;


/**
 * Direct encrypter of {@link com.nimbusds.jose.JWEObject JWE objects} with a
 * shared symmetric key.
 *
 * <p>See RFC 7518
 * <a href="https://tools.ietf.org/html/rfc7518#section-4.5">section 4.5</a>
 * for more information.</p>
 *
 * <p>This class is thread-safe.
 *
 * <p>Supports the following key management algorithms:
 *
 * <ul>
 *     <li>{@link com.nimbusds.jose.JWEAlgorithm#DIR}
 * </ul>
 *
 * <p>Supports the following content encryption algorithms:
 *
 * <ul>
 *     <li>{@link com.nimbusds.jose.EncryptionMethod#A128CBC_HS256} (requires 256 bit key)
 *     <li>{@link com.nimbusds.jose.EncryptionMethod#A192CBC_HS384} (requires 384 bit key)
 *     <li>{@link com.nimbusds.jose.EncryptionMethod#A256CBC_HS512} (requires 512 bit key)
 *     <li>{@link com.nimbusds.jose.EncryptionMethod#A128GCM} (requires 128 bit key)
 *     <li>{@link com.nimbusds.jose.EncryptionMethod#A192GCM} (requires 192 bit key)
 *     <li>{@link com.nimbusds.jose.EncryptionMethod#A256GCM} (requires 256 bit key)
 *     <li>{@link com.nimbusds.jose.EncryptionMethod#A128CBC_HS256_DEPRECATED} (requires 256 bit key)
 *     <li>{@link com.nimbusds.jose.EncryptionMethod#A256CBC_HS512_DEPRECATED} (requires 512 bit key)
 *     <li>{@link com.nimbusds.jose.EncryptionMethod#XC20P} (requires 256 bit key)
 * </ul>
 *
 * @author Vladimir Dzhuvinov
 * @version 2017-06-01
 */
@ThreadSafe
public class TeeDirectEncrypter extends DirectCryptoProvider implements JWEEncrypter {


    /**
     * Creates a new direct encrypter.
     *
     * @param key The symmetric key. Its algorithm should be "AES". Must be
     *            128 bits (16 bytes), 192 bits (24 bytes), 256 bits (32
     *            bytes), 384 bits (48 bytes) or 512 bits (64 bytes) long.
     *            Must not be {@code null}.
     *
     * @throws KeyLengthException If the symmetric key length is not
     *                            compatible.
     */
    public TeeDirectEncrypter(final SecretKey key)
            throws KeyLengthException {

        super(key);
    }


    /**
     * Creates a new direct encrypter.
     *
     * @param keyBytes The symmetric key, as a byte array. Must be 128 bits
     *                 (16 bytes), 192 bits (24 bytes), 256 bits (32
     *                 bytes), 384 bits (48 bytes) or 512 bits (64 bytes)
     *                 long. Must not be {@code null}.
     *
     * @throws KeyLengthException If the symmetric key length is not
     *                            compatible.
     */
    public TeeDirectEncrypter(final byte[] keyBytes)
            throws KeyLengthException {

        this(new SecretKeySpec(keyBytes, "AES"));
    }


    /**
     * Creates a new direct encrypter.
     *
     * @param octJWK The symmetric key, as a JWK. Must be 128 bits (16
     *               bytes), 192 bits (24 bytes), 256 bits (32 bytes), 384
     *               bits (48 bytes) or 512 bits (64 bytes) long. Must not
     *               be {@code null}.
     *
     * @throws KeyLengthException If the symmetric key length is not
     *                            compatible.
     */
    public TeeDirectEncrypter(final OctetSequenceKey octJWK)
            throws KeyLengthException {

        this(octJWK.toSecretKey("AES"));
    }


    @Override
    public JWECryptoParts encrypt(final JWEHeader header, final byte[] clearText)
            throws JOSEException {

        JWEAlgorithm alg = header.getAlgorithm();

        if (! alg.equals(JWEAlgorithm.DIR)) {
            throw new JOSEException(AlgorithmSupportMessage.unsupportedJWEAlgorithm(alg, SUPPORTED_ALGORITHMS));
        }

        // Check key length matches encryption method
        EncryptionMethod enc = header.getEncryptionMethod();

        if (enc.cekBitLength() != ByteUtils.safeBitLength(getKey().getEncoded())) {
            throw new KeyLengthException(enc.cekBitLength(), enc);
        }

        final Base64URL encryptedKey = null; // The second JWE part

        return encrypt(header, clearText, getKey(), encryptedKey, getJCAContext());
    }

    public static JWECryptoParts encrypt(final JWEHeader header,
                                         final byte[] clearText,
                                         final SecretKey cek,
                                         final Base64URL encryptedKey,
                                         final JWEJCAContext jcaProvider)
            throws JOSEException {

        checkCEKLength(cek, header.getEncryptionMethod());

        // Apply compression if instructed
        final byte[] plainText = DeflateHelper.applyCompression(header, clearText);

        // Compose the AAD
        final byte[] aad = AAD.compute(header);

        // Encrypt the plain text according to the JWE enc
        final byte[] iv;
        final AuthenticatedCipherText authCipherText;

        if (    header.getEncryptionMethod().equals(EncryptionMethod.A128CBC_HS256) ||
                header.getEncryptionMethod().equals(EncryptionMethod.A192CBC_HS384) ||
                header.getEncryptionMethod().equals(EncryptionMethod.A256CBC_HS512)    ) {

            iv = AESCBC.generateIV(jcaProvider.getSecureRandom());

            authCipherText = AESCBC.encryptAuthenticated(
                    cek, iv, plainText, aad,
                    jcaProvider.getContentEncryptionProvider(),
                    jcaProvider.getMACProvider());

        } else {

            throw new JOSEException(AlgorithmSupportMessage.unsupportedEncryptionMethod(
                    header.getEncryptionMethod(),
                    SUPPORTED_ENCRYPTION_METHODS));
        }

        return new JWECryptoParts(
                header,
                encryptedKey,
                Base64URL.encode(iv),
                Base64URL.encode(authCipherText.getCipherText()),
                Base64URL.encode(authCipherText.getAuthenticationTag()));
    }

    private static void checkCEKLength(final SecretKey cek, final EncryptionMethod enc)
            throws KeyLengthException {

        try {
            if (enc.cekBitLength() != ByteUtils.safeBitLength(cek.getEncoded())) {
                throw new KeyLengthException("The Content Encryption Key (CEK) length for " + enc + " must be " + enc.cekBitLength() + " bits");
            }
        } catch (IntegerOverflowException e) {
            throw new KeyLengthException("The Content Encryption Key (CEK) is too long: " + e.getMessage());
        }
    }

    public static AuthenticatedCipherText encryptAuthenticated(final SecretKey secretKey,
                                                               final byte[] iv,
                                                               final byte[] plainText,
                                                               final byte[] aad,
                                                               final Provider ceProvider,
                                                               final Provider macProvider)
            throws JOSEException {

        // Extract MAC + AES/CBC keys from input secret key
        CompositeKey compositeKey = new CompositeKey(secretKey);

        // Encrypt plain text
        byte[] cipherText = encrypt(compositeKey.getAESKey(), iv, plainText, ceProvider);

        // AAD length to 8 byte array
        byte[] al = AAD.computeLength(aad);

        // Do MAC
        int hmacInputLength = aad.length + iv.length + cipherText.length + al.length;
        byte[] hmacInput = ByteBuffer.allocate(hmacInputLength).put(aad).put(iv).put(cipherText).put(al).array();
        byte[] hmac = HMAC.compute(compositeKey.getMACKey(), hmacInput, macProvider);
        byte[] authTag = Arrays.copyOf(hmac, compositeKey.getTruncatedMACByteLength());

        return new AuthenticatedCipherText(cipherText, authTag);
    }

    public static byte[] encrypt(final SecretKey secretKey,
                                 final byte[] iv,
                                 final byte[] plainText,
                                 final Provider provider)
            throws JOSEException {

        Cipher cipher = createAESCBCCipher(secretKey, true, iv, provider);

        try {
            return cipher.doFinal(plainText);

        } catch (Exception e) {

            throw new JOSEException(e.getMessage(), e);
        }
    }

    private static Cipher createAESCBCCipher(final SecretKey secretKey,
                                             final boolean forEncryption,
                                             final byte[] iv,
                                             final Provider provider)
            throws JOSEException {

        Cipher cipher;

        try {
            cipher = CipherHelper.getInstance("AES/CBC/PKCS7Padding", provider);

            SecretKeySpec keyspec = new SecretKeySpec(secretKey.getEncoded(), "AES");

            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            if (forEncryption) {

                cipher.init(Cipher.ENCRYPT_MODE, keyspec, ivSpec);

            } else {

                cipher.init(Cipher.DECRYPT_MODE, keyspec, ivSpec);
            }

        } catch (Exception e) {

            throw new JOSEException(e.getMessage(), e);
        }

        return cipher;
    }
}
