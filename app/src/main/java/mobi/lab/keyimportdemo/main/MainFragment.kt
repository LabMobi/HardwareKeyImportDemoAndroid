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
            buttonStartTestWrappingKeyUsage.setOnClickListener { viewModel.onRunWrappingKeyUsageTestClicked() }
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
                is TestStatus.FailedGeneric -> onKeyImportFailedGeneric(state.status)
                TestStatus.InProgress -> onTestStarted()
                TestStatus.NotStated -> onTestEnded()
                is TestStatus.SuccessStrongboxTee -> onKeyImportSuccessStongboxTee(state.status)
                is TestStatus.SuccessHardwareTeeNoStrongbox -> onKeyImportSuccessHardwareTeeNoStrongbox(state.status)
                is TestStatus.SuccessSoftwareTeeOnly -> onKeyImportSuccessSoftwareTeeOnly(state.status)
                TestStatus.FailedKeyImportNotSupportedOnThisApiLevel -> onKeyImportFailedNotSupportedOnThisApiLevel(state.status)
                TestStatus.FailedKeyImportNotAvailableOnThisDevice -> onKeyImportFailedNotAvailableOnThisDevice(state.status)
                is TestStatus.FailedTestDecryptionResultDifferentThanInput -> onDecryptFailed(state.status)
                is TestStatus.SuccessUnknownTeeOnly -> onKeyImportSuccessUnknownTee(state.status)
                TestStatus.FailedLocalKeyUsageNoSuchKey -> onLocalKeyUsageNoSuchKey(state.status)
                is TestStatus.SuccessLocalKeyUsage -> onLocalKeyUsageSuccess(state.status)
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

    private fun getResultAsText(status: TestStatus): String {
        return when (status) {
            is TestStatus.FailedGeneric ->
                getString(R.string.text_status_failed)
            TestStatus.FailedKeyImportNotAvailableOnThisDevice ->
                getString(R.string.text_status_failed_no_key_import_support_available_on_this_device)
            TestStatus.FailedKeyImportNotSupportedOnThisApiLevel ->
                getString(R.string.text_status_failed_no_key_import_support_on_this_api_level)
            TestStatus.FailedTestDecryptionResultDifferentThanInput ->
                getString(R.string.text_status_decrypt_failed)
            TestStatus.InProgress ->
                getString(R.string.text_status_test_in_progress)
            TestStatus.NotStated ->
                getString(R.string.text_status_test_not_started)
            is TestStatus.SuccessHardwareTeeNoStrongbox ->
                getString(R.string.text_status_success_hardware_tee_no_strongbox)
            is TestStatus.SuccessSoftwareTeeOnly ->
                getString(R.string.text_status_success_software_tee_only)
            is TestStatus.SuccessStrongboxTee ->
                getString(R.string.text_status_success_strongbox_tee)
            is TestStatus.SuccessUnknownTeeOnly ->
                getString(R.string.text_status_success_unknown_tee)
            TestStatus.FailedLocalKeyUsageNoSuchKey ->
                getString(R.string.text_status_local_key_usage_failed_no_such_key)
            is TestStatus.SuccessLocalKeyUsage ->
                getString(R.string.text_status_success_local_key_usage)
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

    private fun onKeyImportSuccessStongboxTee(status: TestStatus) {
        displayTestFullSuccess(status)
    }

    private fun onKeyImportSuccessHardwareTeeNoStrongbox(status: TestStatus) {
        displayTestFullSuccess(status)
    }

    private fun onKeyImportSuccessSoftwareTeeOnly(status: TestStatus) {
        displayTestPartialSuccess(status)
    }

    private fun onKeyImportSuccessUnknownTee(status: TestStatus) {
        displayTestPartialSuccess(status)
    }

    private fun onLocalKeyUsageSuccess(status: TestStatus) {
        displayTestFullSuccess(status)
    }

    private fun onLocalKeyUsageNoSuchKey(status: TestStatus) {
        displayTestFailed(status)
    }

    private fun onKeyImportFailedGeneric(status: TestStatus) {
        displayTestFailed(status)
    }

    private fun onDecryptFailed(status: TestStatus) {
        displayTestFailed(status)
    }

    private fun onKeyImportFailedNotSupportedOnThisApiLevel(status: TestStatus) {
        displayTestFailed(status)
    }

    private fun onKeyImportFailedNotAvailableOnThisDevice(status: TestStatus) {
        displayTestFailed(status)
    }

    private fun displayTestPartialSuccess(status: TestStatus) {
        onTestEnded()
        requireBinding {
            textTitle.text = getResultAsText(status)
            textTitle.setBackgroundColor(resources.getColor(R.color.yellow, requireContext().theme))
        }
    }

    private fun displayTestFullSuccess(status: TestStatus) {
        onTestEnded()
        requireBinding {
            textTitle.text = getResultAsText(status)
            textTitle.setBackgroundColor(resources.getColor(R.color.green, requireContext().theme))
        }
    }

    private fun displayTestFailed(status: TestStatus) {
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
