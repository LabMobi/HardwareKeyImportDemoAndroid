package mobi.lab.keyimportdemo.splash

import androidx.lifecycle.MutableLiveData
import io.reactivex.rxjava3.disposables.Disposable
import mobi.lab.mvvm.SingleEvent
import mobi.lab.keyimportdemo.common.BaseViewModel
import mobi.lab.keyimportdemo.common.util.asLiveData
import mobi.lab.keyimportdemo.common.util.dispose
import javax.inject.Inject

class SplashViewModel @Inject constructor() : BaseViewModel() {

    private val _action = MutableLiveData<SingleEvent<Action>>()
    val action = _action.asLiveData()

    private var disposable: Disposable? = null

    init {
        startMain()
    }

    private fun startMain() {
        _action.value = SingleEvent(Action.LaunchApplication)
    }

    override fun onCleared() {
        super.onCleared()
        dispose(disposable)
    }

    sealed class Action {
        object LaunchApplication : Action()
    }
}
