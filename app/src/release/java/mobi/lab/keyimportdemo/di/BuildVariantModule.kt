package mobi.lab.keyimportdemo.di

import dagger.Module
import dagger.Provides
import mobi.lab.keyimportdemo.common.debug.DebugActions
import mobi.lab.keyimportdemo.debug.ReleaseDebugActions
import javax.inject.Singleton

@Module
object BuildVariantModule {

    @Provides
    @Singleton
    fun provideDebugActions(): DebugActions {
        return ReleaseDebugActions()
    }
}
