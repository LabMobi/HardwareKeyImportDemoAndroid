package mobi.lab.keyimportdemo.infrastructure.auth.remote

import mobi.lab.keyimportdemo.domain.entities.Session
import javax.inject.Inject

internal class ApiSessionMapper @Inject constructor() {

    fun toEntity(item: ApiSession): Session {
        return Session(item.token)
    }
}
