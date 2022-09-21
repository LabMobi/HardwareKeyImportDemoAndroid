package mobi.lab.keyimportdemo.main

import android.util.Log
import androidx.lifecycle.MutableLiveData
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import mobi.lab.keyimportdemo.app.common.exhaustive
import mobi.lab.keyimportdemo.common.BaseViewModel
import mobi.lab.keyimportdemo.common.rx.SchedulerProvider
import mobi.lab.keyimportdemo.common.util.asLiveData
import mobi.lab.keyimportdemo.common.util.dispose
import mobi.lab.keyimportdemo.domain.entities.KeyImportTestResult
import mobi.lab.keyimportdemo.domain.entities.KeyLocalUsageTestResult
import mobi.lab.keyimportdemo.domain.gateway.LoggerGateway
import mobi.lab.keyimportdemo.domain.usecases.crypto.KeyImportUseCase
import mobi.lab.keyimportdemo.domain.usecases.crypto.ImportedKeyLocalUsageUseCase
import mobi.lab.keyimportdemo.domain.usecases.crypto.WrappingKeyLocalUsageUseCase
import mobi.lab.mvvm.SingleEvent
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class MainViewModel @Inject constructor(
    private val keyImportUseCase: KeyImportUseCase,
    private val importedKeyLocalUsageUseCase: ImportedKeyLocalUsageUseCase,
    private val wrappingKeyLocalUsageUseCase: WrappingKeyLocalUsageUseCase,
    private val schedulers: SchedulerProvider,
    private val logger: LoggerGateway
) : BaseViewModel(), LoggerGateway.LoggerGatewayListener {

    private val _action = MutableLiveData<SingleEvent<Action>>()
    val action = _action.asLiveData()

    private val _state = MutableLiveData(defaultState())
    val state = _state.asLiveData()

    private var disposable: Disposable? = null

    init {
        logger.setLogLinesListener(this)
    }

    override fun onCleared() {
        super.onCleared()
        dispose(disposable)
        logger.setLogLinesListener(null)
    }

    fun onRunKeyImportTestClicked() {
        logger.clearLog()
        updateState { it.copy(status = TestStatus.InProgress) }
        dispose(disposable)
        disposable = keyImportUseCase
            .execute("Hello world! The date and time is ${formatCurrentTime()}")
            .compose(schedulers.single(Schedulers.computation(), AndroidSchedulers.mainThread()))
            .subscribe(::onKeyImportTestSuccess, ::onKeyImportTestFailed)
    }

    private fun onKeyImportTestFailed(error: Throwable) {
        logger.d("onKeyImportTestFailed: ${Log.getStackTraceString(error)}")
        updateState { it.copy(status = TestStatus.FailedGeneric(error)) }
    }

    private fun onKeyImportTestSuccess(result: KeyImportTestResult) {
        logger.d("onKeyImportTestSuccess: $result")
        updateState { it.copy(status = mapResult(result)) }
    }

    fun onRunImportKeyUsageTestClicked() {
        logger.clearLog()
        updateState { it.copy(status = TestStatus.InProgress) }
        dispose(disposable)
        disposable = importedKeyLocalUsageUseCase
            .execute("Hello world! The date and time is ${formatCurrentTime()}")
            .compose(schedulers.single(Schedulers.computation(), AndroidSchedulers.mainThread()))
            .subscribe(::onKeyLocalUsageTestSuccess, ::onKeyLocalUsageTestFailed)
    }

    fun onRunWrappingKeyUsageTestClicked() {
        logger.clearLog()
        updateState { it.copy(status = TestStatus.InProgress) }
        dispose(disposable)
        disposable = wrappingKeyLocalUsageUseCase
            .execute("Hello world! The date and time is ${formatCurrentTime()}")
            .compose(schedulers.single(Schedulers.computation(), AndroidSchedulers.mainThread()))
            .subscribe(::onKeyLocalUsageTestSuccess, ::onKeyLocalUsageTestFailed)
    }

    private fun onKeyLocalUsageTestFailed(error: Throwable) {
        logger.d("onKeyLocalUsageTestFailed: ${Log.getStackTraceString(error)}")
        updateState { it.copy(status = TestStatus.FailedGeneric(error)) }
    }

    private fun onKeyLocalUsageTestSuccess(result: KeyLocalUsageTestResult) {
        logger.d("onKeyLocalUsageTestSuccess: $result")
        updateState { it.copy(status = mapResult(result)) }
    }

    private fun formatCurrentTime(): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        return LocalDateTime.now().format(formatter)
    }

    fun onDebugClicked() {
        _action.value = SingleEvent(Action.OpenDebug)
    }

    fun onShareClicked() {
        startShare()
    }

    private fun startShare() {
        when (state.value?.status ?: TestStatus.NotStated) {
            is TestStatus.FailedGeneric,
            TestStatus.FailedKeyImportNotAvailableOnThisDevice,
            TestStatus.FailedKeyImportNotSupportedOnThisApiLevel,
            TestStatus.FailedLocalKeyUsageNoSuchKey,
            TestStatus.FailedTestDecryptionResultDifferentThanInput -> {
                shareResultAndLog(state.value ?: defaultState())
            }
            TestStatus.InProgress,
            TestStatus.NotStated,
            is TestStatus.SuccessHardwareTeeNoStrongbox,
            is TestStatus.SuccessSoftwareTeeOnly,
            is TestStatus.SuccessStrongboxTee,
            is TestStatus.SuccessLocalKeyUsage,
            is TestStatus.SuccessUnknownTeeOnly -> {
                shareResultOnly(state.value ?: defaultState())
            }
        }.exhaustive
    }

    private fun shareResultAndLog(state: State) {
        _action.value = SingleEvent(Action.ShareResultAndLog(state))
    }

    private fun shareResultOnly(state: State) {
        _action.value = SingleEvent(Action.ShareResultOnly(state))
    }

    private fun defaultState(): State {
        return State(TestStatus.NotStated, ArrayList())
    }

    private fun updateState(function: (State) -> State) {
        _state.value = function.invoke(currentStateOrDefault())
    }

    private fun currentStateOrDefault(): State {
        return _state.value ?: defaultState()
    }

    private fun mapResult(result: KeyImportTestResult): TestStatus {
        return when (result) {
            KeyImportTestResult.FailedKeyImportNotSupportedOnThisApiLevel -> TestStatus.FailedKeyImportNotSupportedOnThisApiLevel
            KeyImportTestResult.FailedKeyImportNotAvailableOnThisDevice -> TestStatus.FailedKeyImportNotAvailableOnThisDevice
            KeyImportTestResult.FailedTestDecryptionResultDifferentThanInput -> TestStatus.FailedTestDecryptionResultDifferentThanInput
            is KeyImportTestResult.SuccessHardwareTeeStrongBox -> TestStatus.SuccessStrongboxTee(result.message)
            is KeyImportTestResult.SuccessHardwareTeeNoStrongbox -> TestStatus.SuccessHardwareTeeNoStrongbox(result.message)
            is KeyImportTestResult.SuccessSoftwareTeeOnly -> TestStatus.SuccessSoftwareTeeOnly(result.message)
            is KeyImportTestResult.SuccessTeeUnknown -> TestStatus.SuccessUnknownTeeOnly(result.message)
        }.exhaustive
    }

    private fun mapResult(result: KeyLocalUsageTestResult): TestStatus {
        return when (result) {
            is KeyLocalUsageTestResult.UsageFailedGeneric -> TestStatus.FailedGeneric(result.throwable)
            KeyLocalUsageTestResult.UsageFailedNoSuchKey -> TestStatus.FailedLocalKeyUsageNoSuchKey
            is KeyLocalUsageTestResult.UsageSuccess -> TestStatus.SuccessLocalKeyUsage(result.message)
        }.exhaustive
    }

    sealed class Action {
        object RestartApplication : Action()
        object OpenDebug : Action()
        data class ShareResultOnly(val state: State) : Action()
        data class ShareResultAndLog(val state: State) : Action()
    }

    data class State(val status: TestStatus, val logLines: List<CharSequence>)

    override fun onLogLinesUpdated(logLines: List<CharSequence>) {
        updateState { it.copy(logLines = ArrayList(logLines)) }
    }
}
