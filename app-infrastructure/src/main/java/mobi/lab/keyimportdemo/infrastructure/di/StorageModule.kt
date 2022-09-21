package mobi.lab.keyimportdemo.infrastructure.di

import dagger.Binds
import dagger.Module
import mobi.lab.keyimportdemo.domain.storage.SessionStorage
import mobi.lab.keyimportdemo.infrastructure.auth.local.SessionPreferenceStorage

/**
 * Storage implementation are provided by constructor injection from db package
 */
@Module(includes = [StorageModule.Definitions::class])
object StorageModule {

    @Module
    internal interface Definitions {
        @Binds fun bindSessionStorage(impl: SessionPreferenceStorage): SessionStorage
    }
}
