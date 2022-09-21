package mobi.lab.keyimportdemo.domain.usecases.auth

import dagger.Reusable
import mobi.lab.keyimportdemo.domain.storage.SessionStorage
import mobi.lab.keyimportdemo.domain.usecases.UseCase
import javax.inject.Inject

@Reusable
class DeleteSessionUseCase @Inject constructor(private val sessionStorage: SessionStorage) : UseCase() {

    fun execute() {
        sessionStorage.clear()
    }
}
