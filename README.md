## Mobi Lab

# Key Import Android Demo README

Demo app for Android hardware TEE KeyStore key import. Shows how to import a key generated at backend server to Android hardware TEE (Trusted Execution Environment ) securely.

NOTE: For ease of the Demo there is no separate server component here in the project. But the "server-side" is written in such way that it is separated from the client code and has explicit handoffs between server and client. So it will be easy to move that part to an actual server and communicate via HTTPS.

# Running it

There are the following options

## A. Take a build from GitHub releases

1. Take the latest debug build from GitHub releases.
   1. Releases: https://github.com/LabMobi/HardwareKeyImportDemoAndroid/releases
      1. You can direct-download or scan a download QR there.

2. Install it
3. Run it via "Run full key import test" button

## B1. Clone and compile yourself via Android Studio

1. Clone the repository
2. Switch to master branch
3. Compile and run the debug build variant
4. Run it via `Run full key import test` button

## B2. Clone and compile from command line

1. Clone the repository

2. Switch to master branch

3. Install via Gradle:

   ```bash
   .\gradlew installDebug
   ```

4. Run it via "Run full key import test" button

# Main demo

Usable via button `Run full key import test` in the demo

## 1. Android app key initiation phase

1. App generates RSA key pair inside the TEE protected KeyStore
   1. Preferably to a Strongbox TEE, but a normal TEE is used as fallback

2. App exports public key of the RSA key pair in the JWK format.

See the `mobi.lab.keyimportdemo.infrastructure.crypto.CryptoClient` class for the implementation details.

## 2. Server key generation and wrapping phase

1. Server imports the app’s public key from the JWK format. 
2. Server generates long-term AES secret key. 
   1. This will be the TEK key, which will be retained at the server side and which will be imported into the Android KeyStore.
3. Server generates TEK key metadata, which will be used by StrongBox to import the TEK key
4. Server generates ephemeral AES secret key. 
   1. This will be CEK (Content Encryption Key), which will be temporarily used to wrap the TEK and additional metadata. 
5. Server encrypts the CEK to the RSA public key with “RSA/ECB/OAEPPadding” encryption
6. Server uses CEK with “AES/GCM/NoPadding” encryption to encrypt TEK key
7. Server encodes encrypted information in ASN.1 format, as required in https://developer.android.com/training/articles/keystore#ImportingEncryptedKeys
8. Server outputs DER encoded ASN.1 structure as Base64 encoded string. 
9. Server uses TEK key to create an encrypted message with content of “Hello world” and outputs it as Base64 encoded string. 
   1. This can be later used to test the key import


See the `mobi.lab.keyimportdemo.infrastructure.crypto.CryptoServer` class for the implementation details.

## 3. Android key import phase

1. Android app loads the DER encoded ASN.1 structure received from server
2. Android app imports the wrapped key to Android TEE hardware
3. Android app uses the TEE protected key to decrypt the message from server and outputs the text. 

See the `mobi.lab.keyimportdemo.infrastructure.crypto.CryptoClient` class for the implementation details.

# Secondary demo

Usable via buttons `Use imported key` and `Use wrapping key` in the demo.

## Optional: Test the usage of the imported (AES) key again after it been imported and tested once

Useful to validate access to the imported key in different situations. For apps with different identities (no access) or after a cycle of install-import-uninstall-reinstall (key gets removed during uninstall).

## Optional: Test the usage of the wrapping (RSA) key again after it been imported and tested once

Useful to validate access to the wrapping key in different situations. For apps with different identities (no access) or after a cycle of install-import-uninstall-reinstall (key gets removed during uninstall).

# Sharing results

To share the results together with the device info use the share icon from the Toolbar. It will use the Android's built-in sharing function to share the result as text.

In case of a successful import it will share the result plus the device info. For example,

```
Import success: Hardware Strongbox TEE
Google Pixel 3 API level 31 
```

In case of a failure it includes the info above and the log content.

NOTE: The full shared data is also written to Android Logcat after the share button is pressed. This simplifies copying the results when the device is connected to USB debugging via Android Studio.

# Reading code

- Test use case that orchestrates everything: **app-domain/**src/main/java/mobi/lab/keyimportdemo/domain/usecases/crypto/KeyImportUseCase.kt
- Client part: **app-infrastructure/**src/main/java/mobi/lab/keyimportdemo/infrastructure/crypto/CryptoClient.kt
- Server part: **app-infrastructure/**src/main/java/mobi/lab/keyimportdemo/infrastructure/crypto/CryptoServer.kt



# Building code

Full build can be done via:

```bash
.\gradlew buildAllRelease
```

This builds all variants and runs all linters.

Code linters (Detekt, ktlint) can be also ran separately via:

```
.\gradlew checkCode
```

NOTE: This skips the Android Lint as that takes a long time to run and rarely has something to say.

# Contact

## Mobi Lab

Email: hello@lab.mobi

Twitter: https://mobile.twitter.com/LabMobi

Web: https://lab.mobi/
