package mobi.lab.keyimportdemo.splash

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import androidx.activity.viewModels
import mobi.lab.keyimportdemo.R
import mobi.lab.keyimportdemo.common.BaseMvvmActivity
import mobi.lab.keyimportdemo.common.ViewModelFactory
import mobi.lab.keyimportdemo.di.Injector
import mobi.lab.keyimportdemo.main.MainActivity
import javax.inject.Inject

class SplashActivity : BaseMvvmActivity(R.layout.activity_splash) {

    @Inject lateinit var factory: ViewModelFactory

    override val viewModel: SplashViewModel by viewModels { factory }

    init {
        Injector.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /**
         * Android launcher bug workaround. Previous task is not resumed, but a new one is opened.
         * See:
         * https://issuetracker.google.com/issues/64108432 A new bug report
         * https://issuetracker.google.com/issues/36907463 Old, closed bug report
         */
        if (!isTaskRoot) {
            val intent = intent
            if (intent.hasCategory(Intent.CATEGORY_LAUNCHER) && TextUtils.equals(Intent.ACTION_MAIN, intent.action)) {
                finish()
                return
            }
        }
        setContentView(R.layout.activity_splash)
        initViewModel()
    }

    private fun initViewModel() {
        viewModel.action.onEachEvent { action ->
            val intent = when (action) {
                SplashViewModel.Action.LaunchApplication -> MainActivity.getIntent(this)
            }
            startActivity(intent)
            finish()
        }
    }
}
