package mobi.lab.keyimportdemo.di

import mobi.lab.keyimportdemo.App
import mobi.lab.keyimportdemo.main.MainFragment
import mobi.lab.keyimportdemo.prototype.PrototypeActivity
import mobi.lab.keyimportdemo.splash.SplashActivity

interface BaseAppComponent {
    fun inject(target: App)
    fun inject(target: SplashActivity)
    fun inject(target: PrototypeActivity)
    fun inject(target: MainFragment)
}
