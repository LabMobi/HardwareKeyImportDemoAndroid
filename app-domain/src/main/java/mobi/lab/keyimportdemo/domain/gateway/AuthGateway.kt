package mobi.lab.keyimportdemo.domain.gateway

import io.reactivex.rxjava3.core.Single
import mobi.lab.keyimportdemo.domain.entities.Session

interface AuthGateway {
    fun login(username: String, password: String): Single<Session>
}
