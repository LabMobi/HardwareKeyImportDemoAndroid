package mobi.lab.keyimportdemo.domain.usecases.auth

import dagger.Reusable
import io.reactivex.rxjava3.core.Completable
import mobi.lab.keyimportdemo.domain.entities.DomainException
import mobi.lab.keyimportdemo.domain.entities.ErrorCode
import mobi.lab.keyimportdemo.domain.storage.SessionStorage
import mobi.lab.keyimportdemo.domain.usecases.UseCase
import javax.inject.Inject

@Reusable
class HasValidSessionUseCase @Inject constructor(private val sessionStorage: SessionStorage) : UseCase() {

    fun execute(): Completable {
        return Completable.fromSupplier {
            val session = sessionStorage.load()
            if (session == null || !session.isValid()) {
                throw DomainException(ErrorCode.LOCAL_UNAUTHORIZED)
            }
        }
    }
}
