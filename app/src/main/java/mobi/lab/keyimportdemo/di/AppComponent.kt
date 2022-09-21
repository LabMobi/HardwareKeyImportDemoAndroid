package mobi.lab.keyimportdemo.di

import dagger.Component
import mobi.lab.keyimportdemo.domain.di.UseCaseModule
import mobi.lab.keyimportdemo.infrastructure.di.GatewayModule
import mobi.lab.keyimportdemo.infrastructure.di.MapperModule
import mobi.lab.keyimportdemo.infrastructure.di.PlatformModule
import mobi.lab.keyimportdemo.infrastructure.di.ResourceModule
import mobi.lab.keyimportdemo.infrastructure.di.StorageModule
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        ResourceModule::class,
        UseCaseModule::class,
        GatewayModule::class,
        MapperModule::class,
        StorageModule::class,
        AppModule::class,
        PlatformModule::class
    ]
)
interface AppComponent : BaseAppComponent
/**
 * DO NOT ADD METHODS HERE. Add methods to [BaseAppComponent].
 */
