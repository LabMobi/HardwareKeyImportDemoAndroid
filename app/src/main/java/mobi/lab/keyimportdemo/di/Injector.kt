package mobi.lab.keyimportdemo.di

import android.app.Application
import mobi.lab.keyimportdemo.App
import mobi.lab.keyimportdemo.domain.di.UseCaseModule
import mobi.lab.keyimportdemo.infrastructure.di.GatewayModule
import mobi.lab.keyimportdemo.infrastructure.di.MapperModule
import mobi.lab.keyimportdemo.infrastructure.di.PlatformModule
import mobi.lab.keyimportdemo.infrastructure.di.ResourceModule
import mobi.lab.keyimportdemo.infrastructure.di.StorageModule
import mobi.lab.keyimportdemo.main.MainFragment
import mobi.lab.keyimportdemo.prototype.PrototypeActivity
import mobi.lab.keyimportdemo.splash.SplashActivity

object Injector : BaseAppComponent {

    private lateinit var appComponent: AppComponent

    /**
     * UseCaseModule uses constructor injection for providing UseCases. Gateway and Storage modules are only used by UseCaseModule.
     * Since Dagger can resolve UseCaseModule on its own, specifically providing said modules is deprecated (a no-op in reality).
     *
     * We'll leave these module definitions here to have a better overview of what's provided and used.
     */
    @Suppress("DEPRECATION")
    fun buildGraph(application: Application) {
        appComponent = DaggerAppComponent
            .builder()
            .resourceModule(ResourceModule)
            .mapperModule(MapperModule)
            .appModule(AppModule(application))
            .platformModule(PlatformModule)
            .useCaseModule(UseCaseModule)
            .gatewayModule(GatewayModule)
            .storageModule(StorageModule)
            .build()
    }

    override fun inject(target: App) {
        appComponent.inject(target)
    }

    override fun inject(target: SplashActivity) {
        appComponent.inject(target)
    }

    override fun inject(target: MainFragment) {
        appComponent.inject(target)
    }

    override fun inject(target: PrototypeActivity) {
        appComponent.inject(target)
    }
}
