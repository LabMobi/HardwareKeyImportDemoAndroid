package mobi.lab.keyimportdemo.domain.usecases.auth

import dagger.Reusable
import mobi.lab.keyimportdemo.domain.entities.Session
import mobi.lab.keyimportdemo.domain.storage.SessionStorage
import mobi.lab.keyimportdemo.domain.usecases.UseCase
import javax.inject.Inject

@Reusable
class SaveSessionUseCase @Inject constructor(private val sessionStorage: SessionStorage) : UseCase() {

    fun execute(session: Session) {
        sessionStorage.save(session)
    }
}
