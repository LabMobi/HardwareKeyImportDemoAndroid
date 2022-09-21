package mobi.lab.keyimportdemo.common

import android.os.Bundle
import androidx.annotation.LayoutRes
import androidx.lifecycle.LifecycleOwner
import mobi.lab.mvvm.MvvmLiveDataExtensions

abstract class BaseMvvmActivity : BaseActivity, MvvmLiveDataExtensions {

    constructor() : super()
    constructor(@LayoutRes contentLayoutId: Int) : super(contentLayoutId)

    abstract val viewModel: BaseViewModel

    /**
     * Activity Lifecycle is also connected to it's View lifecycle so we want to return
     * the lifecycle of the Activity itself here. See [BaseMvvmFragment] for why you'd want something different.
     */
    override fun getLifecycleOwner(): LifecycleOwner = this

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        viewModel.finish.onEachNotNull { finish ->
            if (finish) {
                finish()
            }
        }
    }
}
