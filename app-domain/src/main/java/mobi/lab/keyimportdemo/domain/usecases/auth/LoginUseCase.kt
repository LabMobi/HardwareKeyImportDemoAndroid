package mobi.lab.keyimportdemo.domain.usecases.auth

import dagger.Reusable
import io.reactivex.rxjava3.core.Single
import mobi.lab.keyimportdemo.app.common.isStringEmpty
import mobi.lab.keyimportdemo.domain.entities.DomainException
import mobi.lab.keyimportdemo.domain.entities.ErrorCode
import mobi.lab.keyimportdemo.domain.entities.Session
import mobi.lab.keyimportdemo.domain.gateway.AuthGateway
import mobi.lab.keyimportdemo.domain.usecases.UseCase
import javax.inject.Inject

@Reusable
class LoginUseCase @Inject constructor(
    private val gw: AuthGateway,
    private val saveSessionUseCase: SaveSessionUseCase
) : UseCase() {
    fun execute(email: String, password: String): Single<Session> {
        if (isStringEmpty(email) || isStringEmpty(password)) {
            return Single.error(DomainException(ErrorCode.LOCAL_INVALID_CREDENTIALS))
        }
        return gw.login("eve.holt@reqres.in", "cityslicka").doOnSuccess { saveSessionUseCase.execute(it) }
    }
}
