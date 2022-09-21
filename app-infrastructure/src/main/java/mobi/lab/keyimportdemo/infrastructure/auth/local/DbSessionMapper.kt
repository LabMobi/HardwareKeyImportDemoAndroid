package mobi.lab.keyimportdemo.infrastructure.auth.local

import mobi.lab.keyimportdemo.domain.entities.Session
import javax.inject.Inject

internal class DbSessionMapper @Inject constructor() {

    fun toEntity(item: DbSession): Session {
        return Session(item.token)
    }

    fun toDb(item: Session): DbSession {
        return DbSession(item.token)
    }
}
