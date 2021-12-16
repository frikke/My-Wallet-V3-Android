package piuk.blockchain.android.ui.settings.v2

import com.blockchain.extensions.exhaustive
import com.blockchain.logging.CrashLogger
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import piuk.blockchain.android.ui.base.mvi.MviModel
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import timber.log.Timber

class SettingsModel(
    initialState: SettingsState,
    mainScheduler: Scheduler,
    private val interactor: SettingsInteractor,
    environmentConfig: EnvironmentConfig,
    crashLogger: CrashLogger
) : MviModel<SettingsState, SettingsIntent>(
    initialState,
    mainScheduler,
    environmentConfig,
    crashLogger
) {

    override fun performAction(
        previousState: SettingsState,
        intent: SettingsIntent
    ): Disposable? =
        when (intent) {
            is SettingsIntent.LoadInitialInformation -> {
                interactor.getSupportEligibilityAndBasicInfo()
                    .subscribeBy(
                        onSuccess = { (isEligibleForChat, userInformation) ->
                            process(
                                SettingsIntent.UpdateContactSupportEligibility(
                                    isSupportChatEnabled = isEligibleForChat,
                                    userInformation = userInformation
                                )
                            )
                        }, onError = {
                        process(
                            SettingsIntent.UpdateContactSupportEligibility(
                                isSupportChatEnabled = false
                            )
                        )
                    }
                    )
            }
            is SettingsIntent.UnpairWallet -> interactor.unpairWallet().subscribeBy(
                onComplete = {
                    process(SettingsIntent.UserLoggedOut)
                },
                onError = {
                    Timber.e("Unpair wallet failed")
                }
            )
            is SettingsIntent.UserLoggedOut,
            is SettingsIntent.UpdateViewToLaunch,
            is SettingsIntent.ResetViewState,
            is SettingsIntent.UpdateContactSupportEligibility -> null
        }.exhaustive
}
