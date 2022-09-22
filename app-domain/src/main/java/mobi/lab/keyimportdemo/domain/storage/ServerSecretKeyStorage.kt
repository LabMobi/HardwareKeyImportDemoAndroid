package mobi.lab.keyimportdemo.domain.storage

interface ServerSecretKeyStorage {
    fun save(keyBytes: ByteArray)
    fun load(): ByteArray?
    fun clear()
}
