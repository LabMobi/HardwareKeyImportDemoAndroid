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
import mobi.lab.keyimportdemo.domain.entities.KeyUsageTestResult
import mobi.lab.keyimportdemo.domain.gateway.CryptoClientGateway
import mobi.lab.keyimportdemo.domain.gateway.LoggerGateway
import mobi.lab.keyimportdemo.domain.usecases.crypto.ImportedKeyTwoWayUsageUseCase
import mobi.lab.keyimportdemo.domain.usecases.crypto.KeyImportUseCase
import mobi.lab.mvvm.SingleEvent
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class MainViewModel @Inject constructor(
    private val keyImportUseCase: KeyImportUseCase,
    private val importedKeyTwoWayUsageUseCase: ImportedKeyTwoWayUsageUseCase,
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
        updateState { it.copy(status = UiTestStatus.InProgress) }
        dispose(disposable)
        disposable = keyImportUseCase
            .execute(createServerMessage(), createClientMessage())
            .compose(schedulers.single(Schedulers.computation(), AndroidSchedulers.mainThread()))
            .subscribe(::onKeyImportTestSuccess, ::onKeyImportTestFailed)
    }

    private fun onKeyImportTestFailed(error: Throwable) {
        logger.d("onKeyImportTestFailed: ${Log.getStackTraceString(error)}")
        updateState { it.copy(status = UiTestStatus.FailedImportGeneric(error)) }
    }

    private fun onKeyImportTestSuccess(result: KeyImportTestResult) {
        logger.d("onKeyImportTestSuccess: $result")
        updateState { it.copy(status = mapResult(result)) }
    }

    fun onRunImportKeyUsageTestClicked() {
        logger.clearLog()
        updateState { it.copy(status = UiTestStatus.InProgress) }
        dispose(disposable)
        disposable = importedKeyTwoWayUsageUseCase
            .execute(createServerMessage(), createClientMessage())
            .compose(schedulers.single(Schedulers.computation(), AndroidSchedulers.mainThread()))
            .subscribe(::onKeyLocalUsageTestSuccess, ::onKeyLocalUsageTestFailed)
    }

    private fun createServerMessage() = "Hello from Server! The date and time is ${formatCurrentTime()}"

    private fun createClientMessage() = "Hello from Client! The date and time is ${formatCurrentTime()}"

    private fun onKeyLocalUsageTestFailed(error: Throwable) {
        logger.d("onKeyLocalUsageTestFailed: ${Log.getStackTraceString(error)}")
        updateState { it.copy(status = UiTestStatus.FailedUsageGeneric(error)) }
    }

    private fun onKeyLocalUsageTestSuccess(result: KeyUsageTestResult) {
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
        val status = state.value?.status ?: UiTestStatus.NotStated
        when (status) {
            is UiTestStatus.FailedImportGeneric,
            is UiTestStatus.FailedUsageGeneric,
            UiTestStatus.FailedKeyImportNotAvailableOnThisDevice,
            UiTestStatus.FailedKeyImportNotSupportedOnThisApiLevel,
            UiTestStatus.FailedKeyUsageNoSuchKey,
            UiTestStatus.FailedTestDecryptionResultDifferentThanInput -> {
                shareResultAndLog(state.value ?: defaultState())
            }
            UiTestStatus.InProgress,
            UiTestStatus.NotStated,
            is UiTestStatus.SuccessKeyUsage -> {
                shareResultOnly(state.value ?: defaultState())
            }
            is UiTestStatus.SuccessKeyImportAndUsage -> {
                if (status.keyUsageStatus !is UiTestStatus.SuccessKeyUsage) {
                    shareResultAndLog(state.value ?: defaultState())
                    return
                }
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
        return State(UiTestStatus.NotStated, ArrayList())
    }

    private fun updateState(function: (State) -> State) {
        _state.value = function.invoke(currentStateOrDefault())
    }

    private fun currentStateOrDefault(): State {
        return _state.value ?: defaultState()
    }

    private fun mapResult(result: KeyImportTestResult): UiTestStatus {
        return when (result) {
            KeyImportTestResult.FailedKeyImportNotSupportedOnThisApiLevel -> UiTestStatus.FailedKeyImportNotSupportedOnThisApiLevel
            KeyImportTestResult.FailedKeyImportNotAvailableOnThisDevice -> UiTestStatus.FailedKeyImportNotAvailableOnThisDevice
            KeyImportTestResult.FailedTestDecryptionResultDifferentThanInput -> UiTestStatus.FailedTestDecryptionResultDifferentThanInput
            is KeyImportTestResult.Success -> UiTestStatus.SuccessKeyImportAndUsage(mapResult(result.keyTestResult))
        }.exhaustive
    }

    private fun mapResult(result: KeyUsageTestResult): UiTestStatus {
        return when (result) {
            is KeyUsageTestResult.UsageFailedGeneric -> UiTestStatus.FailedUsageGeneric(result.throwable)
            KeyUsageTestResult.UsageFailedNoSuchKey -> UiTestStatus.FailedKeyUsageNoSuchKey
            is KeyUsageTestResult.UsageSuccess -> UiTestStatus.SuccessKeyUsage(
                mapResult(result.keyLevel),
                result.serverToClientMessage,
                result.clientToServerMessage
            )
        }.exhaustive
    }

    private fun mapResult(keyLevel: CryptoClientGateway.KeyTeeSecurityLevel): UIKeyTeeSecurityLevel {
        return when (keyLevel) {
            CryptoClientGateway.KeyTeeSecurityLevel.TeeHardwareNoStrongbox -> UIKeyTeeSecurityLevel.TeeHardwareNoStrongbox
            CryptoClientGateway.KeyTeeSecurityLevel.TeeSoftware -> UIKeyTeeSecurityLevel.TeeSoftware
            CryptoClientGateway.KeyTeeSecurityLevel.TeeStrongbox -> UIKeyTeeSecurityLevel.TeeStrongbox
            CryptoClientGateway.KeyTeeSecurityLevel.Unknown -> UIKeyTeeSecurityLevel.Unknown
        }.exhaustive
    }

    sealed class Action {
        object RestartApplication : Action()
        object OpenDebug : Action()
        data class ShareResultOnly(val state: State) : Action()
        data class ShareResultAndLog(val state: State) : Action()
    }

    data class State(val status: UiTestStatus, val logLines: List<CharSequence>)

    override fun onLogLinesUpdated(logLines: List<CharSequence>) {
        updateState { it.copy(logLines = ArrayList(logLines)) }
    }
}
