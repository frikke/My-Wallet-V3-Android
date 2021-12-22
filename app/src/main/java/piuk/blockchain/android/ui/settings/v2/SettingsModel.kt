package piuk.blockchain.android.ui.settings.v2

import com.blockchain.extensions.exhaustive
import com.blockchain.logging.CrashLogger
import com.blockchain.nabu.Tier
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
            is SettingsIntent.LoadHeaderInformation -> {
                interactor.getSupportEligibilityAndBasicInfo()
                    .subscribeBy(
                        onSuccess = { userDetails ->
                            process(
                                SettingsIntent.UpdateContactSupportEligibility(
                                    tier = userDetails.userTier,
                                    userInformation = userDetails.userInfo
                                )
                            )
                        }, onError = {
                        process(SettingsIntent.UpdateContactSupportEligibility(tier = Tier.BRONZE))
                    }
                    )
            }
            is SettingsIntent.LoadPaymentMethods ->
                interactor.getExistingPaymentMethods()
                    .subscribeBy(
                        onSuccess = { paymentMethodInfo ->
                            process(SettingsIntent.UpdatePaymentMethodsInfo(paymentMethodInfo = paymentMethodInfo))
                        }, onError = {
                        process(SettingsIntent.UpdateErrorState(SettingsError.PAYMENT_METHODS_LOAD_FAIL))
                    }
                    )
            is SettingsIntent.AddBankTransferSelected -> interactor.getBankLinkingInfo()
                .subscribeBy(
                    onSuccess = { bankTransferInfo ->
                        process(SettingsIntent.UpdateViewToLaunch(ViewToLaunch.BankTransfer(bankTransferInfo)))
                    }, onError = {
                    process(SettingsIntent.UpdateErrorState(SettingsError.BANK_LINK_START_FAIL))
                }
                )
            is SettingsIntent.AddBankAccountSelected -> {
                process(SettingsIntent.UpdateViewToLaunch(ViewToLaunch.BankAccount(interactor.getUserFiat())))
                null
            }
            is SettingsIntent.Logout -> interactor.unpairWallet()
                .subscribeBy(
                    onComplete = {
                        process(SettingsIntent.UserLoggedOut)
                    },
                    onError = {
                        Timber.e("Unpair wallet failed $it")
                        process(SettingsIntent.UpdateErrorState(SettingsError.UNPAIR_FAILED))
                    }
                )
            is SettingsIntent.UserLoggedOut,
            is SettingsIntent.UpdateViewToLaunch,
            is SettingsIntent.ResetViewState,
            is SettingsIntent.UpdateContactSupportEligibility,
            is SettingsIntent.UpdatePaymentMethodsInfo,
            is SettingsIntent.OnCardRemoved,
            is SettingsIntent.OnBankRemoved,
            is SettingsIntent.ResetErrorState,
            is SettingsIntent.UpdateErrorState -> null
        }.exhaustive
}
