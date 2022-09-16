package piuk.blockchain.android.ui.settings.v2

import com.blockchain.api.NabuApiException
import com.blockchain.api.NabuErrorCodes.MaxPaymentBankAccountLinkAttempts
import com.blockchain.api.NabuErrorCodes.MaxPaymentBankAccounts
import com.blockchain.commonarch.presentation.mvi.MviModel
import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.extensions.exhaustive
import com.blockchain.logging.RemoteLogger
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.kotlin.zipWith
import timber.log.Timber

class SettingsModel(
    initialState: SettingsState,
    mainScheduler: Scheduler,
    private val interactor: SettingsInteractor,
    environmentConfig: EnvironmentConfig,
    remoteLogger: RemoteLogger
) : MviModel<SettingsState, SettingsIntent>(
    initialState,
    mainScheduler,
    environmentConfig,
    remoteLogger
) {

    override fun performAction(
        previousState: SettingsState,
        intent: SettingsIntent
    ): Disposable? =
        when (intent) {
            is SettingsIntent.InitializeFeatureFlags -> {
                interactor.initializeFeatureFlags().subscribeBy(
                    onSuccess = {
                        process(SettingsIntent.UpdateFeatureFlags(it))
                    },
                    onError = {
                        process(SettingsIntent.UpdateFeatureFlags(FeatureFlagsSet()))
                    }
                )
            }
            is SettingsIntent.LoadHeaderInformation -> {
                interactor.getSupportEligibilityAndBasicInfo()
                    .subscribeBy(
                        onSuccess = { userDetails ->
                            process(
                                SettingsIntent.UpdateContactSupportEligibility(
                                    tier = userDetails.kycTier,
                                    userInformation = userDetails.userInfo,
                                    referralInfo = userDetails.referralInfo
                                )
                            )
                        }, onError = {
                        process(SettingsIntent.UpdateContactSupportEligibility(tier = KycTier.BRONZE))
                    }
                    )
            }
            is SettingsIntent.LoadPaymentMethods ->
                interactor.getExistingPaymentMethods().zipWith(interactor.canPayWithBind())
                    .subscribeBy(
                        onSuccess = { (paymentMethodInfo, canPayWithBind) ->
                            process(
                                SettingsIntent.UpdatePaymentMethodsInfo(
                                    paymentMethodInfo = paymentMethodInfo,
                                    canPayWithBind = canPayWithBind
                                )
                            )
                        },
                        onError = {
                            process(SettingsIntent.UpdateErrorState(SettingsError.PaymentMethodsLoadFail))
                        }
                    )
            is SettingsIntent.AddLinkBankSelected -> interactor.getBankLinkingInfo()
                .subscribeBy(
                    onSuccess = { bankTransferInfo ->
                        process(SettingsIntent.UpdateViewToLaunch(ViewToLaunch.BankTransfer(bankTransferInfo)))
                    }, onError = {
                    when ((it as? NabuApiException)?.getErrorCode()) {
                        MaxPaymentBankAccounts ->
                            process(SettingsIntent.UpdateErrorState(SettingsError.BankLinkMaxAccountsReached(it)))
                        MaxPaymentBankAccountLinkAttempts ->
                            process(SettingsIntent.UpdateErrorState(SettingsError.BankLinkMaxAttemptsReached(it)))
                        else ->
                            process(SettingsIntent.UpdateErrorState(SettingsError.BankLinkStartFail))
                    }
                }
                )
            is SettingsIntent.Logout -> interactor.unpairWallet()
                .subscribeBy(
                    onComplete = {
                        process(SettingsIntent.UserLoggedOut)
                    },
                    onError = {
                        Timber.e("Unpair wallet failed $it")
                        process(SettingsIntent.UpdateErrorState(SettingsError.UnpairFailed))
                    }
                )
            is SettingsIntent.OnCardRemoved ->
                interactor.getAvailablePaymentMethodsTypes()
                    .subscribeBy(
                        onSuccess = { available ->
                            process(SettingsIntent.UpdateAvailablePaymentMethods(available))
                        }, onError = {
                        process(SettingsIntent.UpdateErrorState(SettingsError.PaymentMethodsLoadFail))
                    }
                    )
            is SettingsIntent.UserLoggedOut,
            is SettingsIntent.UpdateViewToLaunch,
            is SettingsIntent.ResetViewState,
            is SettingsIntent.UpdateContactSupportEligibility,
            is SettingsIntent.UpdatePaymentMethodsInfo,
            is SettingsIntent.OnBankRemoved,
            is SettingsIntent.ResetErrorState,
            is SettingsIntent.UpdateAvailablePaymentMethods,
            is SettingsIntent.UpdateErrorState,
            is SettingsIntent.UpdateFeatureFlags -> null
        }.exhaustive
}
