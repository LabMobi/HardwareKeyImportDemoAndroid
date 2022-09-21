package mobi.lab.keyimportdemo.common

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import mobi.lab.keyimportdemo.common.util.asLiveData

abstract class BaseViewModel : ViewModel() {
    private val _finish = MutableLiveData(false)
    val finish = _finish.asLiveData()

    protected open fun finish() {
        _finish.value = true
    }
}
