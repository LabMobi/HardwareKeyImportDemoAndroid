package mobi.lab.keyimportdemo.di

import android.app.Application
import android.content.Context
import dagger.Module
import dagger.Provides
import mobi.lab.keyimportdemo.Env
import mobi.lab.keyimportdemo.common.rx.AndroidSchedulerProvider
import mobi.lab.keyimportdemo.common.rx.SchedulerProvider
import mobi.lab.keyimportdemo.common.util.isDebugBuild
import mobi.lab.keyimportdemo.infrastructure.common.platform.AppEnvironment
import javax.inject.Singleton

@Module(includes = [ViewModelModule::class, BuildVariantModule::class])
class AppModule(private val application: Application) {

    @Provides
    @Singleton
    fun provideContext(): Context = application.applicationContext

    @Provides
    @Singleton
    fun provideAppEnvironment(): AppEnvironment = AppEnvironment(Env.URL_BASE, isDebugBuild())

    @Provides
    @Singleton
    fun provideSchedulerProvider(): SchedulerProvider = AndroidSchedulerProvider()
}
