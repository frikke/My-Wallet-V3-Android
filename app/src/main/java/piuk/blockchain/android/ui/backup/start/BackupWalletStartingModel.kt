package piuk.blockchain.android.ui.backup.start

import com.blockchain.commonarch.presentation.mvi.MviModel
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.logging.RemoteLogger
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy

class BackupWalletStartingModel(
    initialState: BackupWalletStartingState,
    mainScheduler: Scheduler,
    environmentConfig: EnvironmentConfig,
    remoteLogger: RemoteLogger,
    private val interactor: BackupWalletStartingInteractor
) : MviModel<BackupWalletStartingState, BackupWalletStartingIntents>(
    initialState,
    mainScheduler,
    environmentConfig,
    remoteLogger
) {
    override fun performAction(
        previousState: BackupWalletStartingState,
        intent: BackupWalletStartingIntents
    ): Disposable? {
        return when (intent) {
            BackupWalletStartingIntents.TriggerEmailAlert -> triggerEmailAlert()
            else -> null
        }
    }

    private fun triggerEmailAlert() =
        interactor.triggerSeedPhraseAlert()
            .subscribeBy(
                onComplete = { process(BackupWalletStartingIntents.UpdateStatus(BackupWalletStartingStatus.COMPLETE)) },
                onError = { process(BackupWalletStartingIntents.UpdateStatus(BackupWalletStartingStatus.COMPLETE)) }
            )
}
