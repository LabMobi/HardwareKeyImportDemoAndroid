package mobi.lab.keyimportdemo.domain.usecases.crypto

import dagger.Reusable
import mobi.lab.keyimportdemo.domain.storage.ServerSecretKeyStorage
import mobi.lab.keyimportdemo.domain.usecases.UseCase
import javax.inject.Inject

@Reusable
class SaveServerSecretKeyUseCase @Inject constructor(private val serverSecretKeyStorage: ServerSecretKeyStorage) : UseCase() {

    fun execute(keyBytes: ByteArray) {
        serverSecretKeyStorage.save(keyBytes)
    }
}
