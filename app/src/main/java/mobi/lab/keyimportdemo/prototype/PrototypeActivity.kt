package mobi.lab.keyimportdemo.prototype

import android.content.Context
import android.content.Intent
import android.os.Bundle
import mobi.lab.keyimportdemo.common.BaseMvvmActivity
import mobi.lab.keyimportdemo.common.util.NavUtil
import mobi.lab.keyimportdemo.common.util.assistedViewModels
import mobi.lab.keyimportdemo.di.Injector
import javax.inject.Inject

class PrototypeActivity : BaseMvvmActivity() {

    // Not the usual ViewModelFactory. We use assisted injection here and need to reference the assisted factory instead
    @Inject
    lateinit var factory: PrototypeViewModel.Factory

    override val viewModel: PrototypeViewModel by assistedViewModels {
        factory.create(intent.getStringExtra(EXTRA_PROTOTYPE_URL) ?: "")
    }

    init {
        Injector.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initViewModel()
    }

    private fun initViewModel() {
        viewModel.action.onEachEvent { action ->
            when (action) {
                is PrototypeViewModel.Action.OpenWebLinkAndClose -> openBrowserAndClose(action.url)
                PrototypeViewModel.Action.Close -> close()
            }
        }
    }

    private fun openBrowserAndClose(url: String) {
        NavUtil.openBrowser(this, url)
        close()
    }

    private fun close() {
        finish()
    }

    companion object {
        private const val EXTRA_PROTOTYPE_URL = "prototype_url"

        fun getIntent(context: Context, prototypeUrl: String): Intent {
            return Intent(context, PrototypeActivity::class.java).apply {
                putExtra(EXTRA_PROTOTYPE_URL, prototypeUrl)
            }
        }
    }
}
