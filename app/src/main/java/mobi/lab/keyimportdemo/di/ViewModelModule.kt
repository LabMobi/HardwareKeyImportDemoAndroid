package mobi.lab.keyimportdemo.di

import androidx.lifecycle.ViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import mobi.lab.keyimportdemo.di.annotations.ViewModelKey
import mobi.lab.keyimportdemo.main.MainViewModel
import mobi.lab.keyimportdemo.splash.SplashViewModel

@Module(includes = [ViewModelModule.Definitions::class])
object ViewModelModule {

    @Module
    internal interface Definitions {
        @Binds
        @IntoMap
        @ViewModelKey(SplashViewModel::class)
        fun bindSplashViewModel(viewModel: SplashViewModel): ViewModel

        @Binds
        @IntoMap
        @ViewModelKey(MainViewModel::class)
        fun bindMainViewModel(viewModel: MainViewModel): ViewModel
    }
}
