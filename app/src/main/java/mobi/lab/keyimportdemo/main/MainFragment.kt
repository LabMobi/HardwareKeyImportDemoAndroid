package mobi.lab.keyimportdemo.main

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import mobi.lab.keyimportdemo.BuildConfig
import mobi.lab.keyimportdemo.R
import mobi.lab.keyimportdemo.app.common.exhaustive
import mobi.lab.keyimportdemo.common.BaseMvvmFragment
import mobi.lab.keyimportdemo.common.FragmentBindingHolder
import mobi.lab.keyimportdemo.common.ViewBindingHolder
import mobi.lab.keyimportdemo.common.ViewModelFactory
import mobi.lab.keyimportdemo.common.debug.DebugActions
import mobi.lab.keyimportdemo.common.util.NavUtil
import mobi.lab.keyimportdemo.databinding.FragmentMainBinding
import mobi.lab.keyimportdemo.di.Injector
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject

class MainFragment : BaseMvvmFragment(), ViewBindingHolder<FragmentMainBinding> by FragmentBindingHolder() {

    @Inject
    lateinit var debugActions: DebugActions

    @Inject
    lateinit var factory: ViewModelFactory

    override val viewModel: MainViewModel by viewModels { factory }

    init {
        Injector.inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return createBinding(FragmentMainBinding.inflate(inflater), this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireBinding {
            initToolbar(this)
            buttonStartTest.setOnClickListener { viewModel.onRunKeyImportTestClicked() }
            buttonStartTestImportKeyUsage.setOnClickListener { viewModel.onRunImportKeyUsageTestClicked() }
        }

        initViewModel()
    }

    private fun initViewModel() {
        /**
         * Init ViewModel in onViewCreated as they are connected to View's lifecycle.
         */
        initViewModelActions()
        initViewModelState()
    }

    private fun initViewModelState() {
        viewModel.state.onEachNotNull { state ->
            when (state.status) {
                is UiTestStatus.FailedImportGeneric -> onKeyImportFailedGeneric(state.status)
                is UiTestStatus.FailedUsageGeneric -> onKeyUsageFailedGeneric(state.status)
                UiTestStatus.InProgress -> onTestStarted()
                UiTestStatus.NotStated -> onTestEnded()
                is UiTestStatus.SuccessKeyImportAndUsage -> onKeyImportSuccess(state.status)
                UiTestStatus.FailedKeyImportNotSupportedOnThisApiLevel -> onKeyImportFailedNotSupportedOnThisApiLevel(state.status)
                UiTestStatus.FailedKeyImportNotAvailableOnThisDevice -> onKeyImportFailedNotAvailableOnThisDevice(state.status)
                is UiTestStatus.FailedTestDecryptionResultDifferentThanInput -> onDecryptFailed(state.status)
                UiTestStatus.FailedKeyUsageNoSuchKey -> onLocalKeyUsageNoSuchKey(state.status)
                is UiTestStatus.SuccessKeyUsage -> onKeyUsageSuccess(state.status)
            }.exhaustive
            onLogLinesUpdated(state.logLines)
        }
    }

    private fun initViewModelActions() {
        viewModel.action.onEachEvent { event ->
            when (event) {
                is MainViewModel.Action.RestartApplication -> restartApplication()
                is MainViewModel.Action.OpenDebug -> openDebug()
                is MainViewModel.Action.ShareResultAndLog -> shareResultAndLog(event.state)
                is MainViewModel.Action.ShareResultOnly -> shareResult(event.state)
            }.exhaustive
        }
    }

    private fun shareResult(state: MainViewModel.State) {
        val text = getResultAsText(state.status) + "\n" + getDeviceInfoAsText()
        shareResultAsText(text)
    }

    private fun shareResultAndLog(state: MainViewModel.State) {
        val text = getResultAsText(state.status) + "\n" + getDeviceInfoAsText() + "\n\n" + getLogAsText(state.logLines)
        shareResultAsText(text)
    }

    private fun getDeviceInfoAsText(): String {
        return "${Build.BRAND.replaceFirstChar { it.uppercase(Locale.ENGLISH) }} ${Build.MODEL} API level ${Build.VERSION.SDK_INT}"
    }

    private fun getLogAsText(logLines: List<CharSequence>): String {
        return logLines.joinToString(separator = "\n")
    }

    private fun getResultAsText(status: UiTestStatus): String {
        return when (status) {
            is UiTestStatus.FailedImportGeneric ->
                getString(R.string.text_status_key_import_failed)
            is UiTestStatus.FailedUsageGeneric ->
                getString(R.string.text_status_key_usage_failed)
            UiTestStatus.FailedKeyImportNotAvailableOnThisDevice ->
                getString(R.string.text_status_failed_no_key_import_support_available_on_this_device)
            UiTestStatus.FailedKeyImportNotSupportedOnThisApiLevel ->
                getString(R.string.text_status_failed_no_key_import_support_on_this_api_level)
            UiTestStatus.FailedTestDecryptionResultDifferentThanInput ->
                getString(R.string.text_status_decrypt_failed)
            UiTestStatus.InProgress ->
                getString(R.string.text_status_test_in_progress)
            UiTestStatus.NotStated ->
                getString(R.string.text_status_test_not_started)
            is UiTestStatus.SuccessKeyUsage ->
                getString(R.string.text_status_success_key_usage) + " " + getResultAsText(status.keyTeeLevel)
            is UiTestStatus.SuccessKeyImportAndUsage ->
                getString(R.string.text_status_key_import_success) + " " + getResultAsText(status.keyUsageStatus)
            UiTestStatus.FailedKeyUsageNoSuchKey ->
                getString(R.string.text_status_local_key_usage_failed_no_such_key)
        }.exhaustive
    }

    private fun getResultAsText(teeLevel: UIKeyTeeSecurityLevel): String {
        return when (teeLevel) {
            UIKeyTeeSecurityLevel.TeeHardwareNoStrongbox -> getString(R.string.text_key_level_hardware_tee_no_strongbox)
            UIKeyTeeSecurityLevel.TeeSoftware -> getString(R.string.text_key_level_software_tee_only)
            UIKeyTeeSecurityLevel.TeeStrongbox -> getString(R.string.text_key_level_hardware_strongboc_tee)
            UIKeyTeeSecurityLevel.Unknown -> getString(R.string.text_key_level_unknown_tee)
        }.exhaustive
    }

    private fun shareResultAsText(text: String) {
        // Log it also so it is in logcat
        Timber.d("shareResultAsText:\n$text")
        val intent = Intent()
        intent.action = Intent.ACTION_SEND
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.text_title_share_results))
        intent.putExtra(Intent.EXTRA_TEXT, text)
        try {
            requireActivity().startActivity(Intent.createChooser(intent, getString(R.string.text_title_share_results)))
        } catch (e: ActivityNotFoundException) {
            Timber.e("Share activity not found", e)
        }
    }

    private fun onTestStarted() {
        requireBinding {
            textTitle.text = getString(R.string.text_status_running)
            textTitle.background = null
            buttonStartTest.showProgress()
        }
    }

    private fun onTestEnded() {
        requireBinding {
            buttonStartTest.hideProgress()
        }
    }

    private fun onLogLinesUpdated(logLines: List<CharSequence>) {
        requireBinding {
            textLog.text = getLogAsText(logLines)
            scrollview.post {
                scrollview.smoothScrollTo(0, textLog.bottom)
            }
        }
    }

    private fun initToolbar(binding: FragmentMainBinding) {
        if (BuildConfig.DEBUG) {
            binding.toolbar.inflateMenu(R.menu.debug_toolbar)
            binding.toolbar.setOnMenuItemClickListener { item ->
                if (item.itemId == R.id.button_debug) {
                    viewModel.onDebugClicked()
                    return@setOnMenuItemClickListener true
                } else if (item.itemId == R.id.button_share) {
                    viewModel.onShareClicked()
                    return@setOnMenuItemClickListener true
                }
                return@setOnMenuItemClickListener false
            }
        } else {
            binding.toolbar.inflateMenu(R.menu.main_toolbar)
            binding.toolbar.setOnMenuItemClickListener { item ->
                if (item.itemId == R.id.button_share) {
                    viewModel.onShareClicked()
                    return@setOnMenuItemClickListener true
                }
                return@setOnMenuItemClickListener false
            }
        }
    }

    private fun onKeyImportSuccess(status: UiTestStatus) {
        if (status is UiTestStatus.SuccessKeyImportAndUsage) {
            if (status.keyUsageStatus !is UiTestStatus.SuccessKeyUsage) {
                displayTestFailed(status)
                return
            }
        }
        displayTestFullSuccess(status)
    }

    private fun onKeyUsageSuccess(status: UiTestStatus) {
        displayTestFullSuccess(status)
    }

    private fun onLocalKeyUsageNoSuchKey(status: UiTestStatus) {
        displayTestFailed(status)
    }

    private fun onKeyImportFailedGeneric(status: UiTestStatus) {
        displayTestFailed(status)
    }

    private fun onKeyUsageFailedGeneric(status: UiTestStatus) {
        displayTestFailed(status)
    }

    private fun onDecryptFailed(status: UiTestStatus) {
        displayTestFailed(status)
    }

    private fun onKeyImportFailedNotSupportedOnThisApiLevel(status: UiTestStatus) {
        displayTestFailed(status)
    }

    private fun onKeyImportFailedNotAvailableOnThisDevice(status: UiTestStatus) {
        displayTestFailed(status)
    }

    private fun displayTestFullSuccess(status: UiTestStatus) {
        onTestEnded()
        requireBinding {
            textTitle.text = getResultAsText(status)
            textTitle.setBackgroundColor(resources.getColor(R.color.green, requireContext().theme))
        }
    }

    private fun displayTestFailed(status: UiTestStatus) {
        onTestEnded()
        requireBinding {
            textTitle.text = getResultAsText(status)
            textTitle.setBackgroundColor(resources.getColor(R.color.red, requireContext().theme))
        }
    }

    private fun restartApplication() {
        context?.let { NavUtil.restartApplication(it) }
    }

    private fun openDebug() {
        // Open DebugActivity
        context?.let { debugActions.launchDebugActivity(it) }
    }

    companion object {
        fun newInstance(): MainFragment {
            return MainFragment()
        }
    }
}
