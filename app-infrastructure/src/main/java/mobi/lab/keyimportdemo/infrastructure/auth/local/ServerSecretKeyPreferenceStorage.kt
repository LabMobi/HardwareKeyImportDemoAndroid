package mobi.lab.keyimportdemo.infrastructure.auth.local

import mobi.lab.keyimportdemo.domain.storage.ServerSecretKeyStorage
import mobi.lab.keyimportdemo.infrastructure.common.local.SharedPreferenceStorage
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ServerSecretKeyPreferenceStorage @Inject constructor(
    private val sharedPrefs: SharedPreferenceStorage,
) : ServerSecretKeyStorage {

    override fun save(keyBytes: ByteArray) {
        try {
            saveToPreferences(keyBytes)
        } catch (error: Exception) {
            Timber.w(error, "save")
        }
    }

    override fun load(): ByteArray? {
        return try {
            Timber.d("load STORAGE this=$this")
            loadFromPreferences()
        } catch (error: Exception) {
            Timber.w(error, "load")
            null
        }
    }

    private fun loadFromPreferences(): ByteArray? {
        return sharedPrefs.getObject(KEY)
    }

    private fun saveToPreferences(keyBytes: ByteArray?) {
        sharedPrefs.putObject(KEY, keyBytes)
    }

    override fun clear() {
        sharedPrefs.putObject(KEY, null)
    }

    companion object {
        private const val KEY = "KEY_SERVER_KEY_BYTES"
    }
}
