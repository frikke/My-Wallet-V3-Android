package piuk.blockchain.android.ui.settings.v2

import com.blockchain.logging.CrashLogger
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.CompositeDisposable
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

    private val compositeDisposable = CompositeDisposable()

    fun clearDisposables() = compositeDisposable.clear()

    override fun performAction(
        previousState: SettingsState,
        intent: SettingsIntent
    ): Disposable? =
        when (intent) {
            is SettingsIntent.LoadContactSupportEligibility -> {
                if (!previousState.contactSupportLoaded) {
                    interactor.checkContactSupportEligibility()
                        .subscribeBy(
                            onSuccess = { (isSimpleBuyEligible, userInformation) ->
                                if (isSimpleBuyEligible) {
                                    process(
                                        SettingsIntent
                                            .LoadedContactSupportEligibility(
                                                contactSupportLoaded = true,
                                                userInformation = userInformation
                                            )
                                    )
                                } else {
                                    process(
                                        SettingsIntent
                                            .LoadedContactSupportEligibility(
                                                contactSupportLoaded = true
                                            )
                                    )
                                }
                            }, onError = {
                            Timber.e("LoadContactSupportEligibility failure")
                            process(
                                SettingsIntent
                                    .LoadedContactSupportEligibility(
                                        contactSupportLoaded = false
                                    )
                            )
                        }
                        )
                } else {
                    null
                }
            }
            is SettingsIntent.LoadedContactSupportEligibility -> null
        }
}
