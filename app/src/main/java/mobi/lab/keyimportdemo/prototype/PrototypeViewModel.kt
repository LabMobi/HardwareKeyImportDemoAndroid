package mobi.lab.keyimportdemo.prototype

import androidx.lifecycle.MutableLiveData
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import mobi.lab.mvvm.SingleEvent
import mobi.lab.keyimportdemo.common.BaseViewModel
import mobi.lab.keyimportdemo.common.util.asLiveData
import timber.log.Timber

/**
 * An example ViewModel that uses AssistedInjection to get argument values at runtime.
 * prototypeUrl is provided at runtime when the ViewModel is injected.
 *
 * Other dependencies can be injected in a regular manner by adding them as constructor arguments.
 *
 * NB! Note @AssistedInject annotation for the constructor and @Assisted annotation for the assisted argument.
 */
class PrototypeViewModel @AssistedInject constructor(
    @Assisted private val prototypeUrl: String
) : BaseViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(prototypeUrl: String): PrototypeViewModel
    }

    private val _action = MutableLiveData<SingleEvent<Action>>()
    val action = _action.asLiveData()

    init {
        Timber.d("PrototypeViewModel init prototypeUrl=$prototypeUrl")
        _action.value = if (prototypeUrl.isBlank()) {
            SingleEvent(Action.Close)
        } else {
            SingleEvent(Action.OpenWebLinkAndClose(prototypeUrl))
        }
    }

    sealed class Action {
        data class OpenWebLinkAndClose(val url: String) : Action()
        object Close : Action()
    }
}
