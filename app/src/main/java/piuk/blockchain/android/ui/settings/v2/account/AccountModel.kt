package piuk.blockchain.android.ui.settings.v2.account

import com.blockchain.commonarch.presentation.mvi.MviModel
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.extensions.exhaustive
import com.blockchain.logging.RemoteLogger
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import piuk.blockchain.androidcore.utils.extensions.rxSingleOutcome
import timber.log.Timber

class AccountModel(
    initialState: AccountState,
    mainScheduler: Scheduler,
    private val interactor: AccountInteractor,
    environmentConfig: EnvironmentConfig,
    remoteLogger: RemoteLogger
) : MviModel<AccountState, AccountIntent>(
    initialState,
    mainScheduler,
    environmentConfig,
    remoteLogger
) {

    override fun performAction(
        previousState: AccountState,
        intent: AccountIntent
    ): Disposable? =
        when (intent) {
            is AccountIntent.LoadAccountInformation -> interactor.getWalletInfo()
                .subscribeBy(
                    onSuccess = {
                        process(AccountIntent.UpdateAccountInformation(it))
                    },
                    onError = {
                        process(AccountIntent.UpdateErrorState(AccountError.ACCOUNT_INFO_FAIL))
                    }
                )
            is AccountIntent.LoadBCDebitCardInformation -> {
                rxSingleOutcome {
                    interactor.getDebitCardState()
                }.subscribeBy(
                    onSuccess = {
                        process(AccountIntent.UpdateBlockchainCardOrderState(it))
                    },
                    onError = {
                        Timber.e(it)
                        process(AccountIntent.UpdateErrorState(AccountError.BLOCKCHAIN_CARD_LOAD_FAIL))
                    }
                )
            }
            is AccountIntent.LoadDisplayCurrencies -> interactor.getAvailableDisplayCurrencies()
                .subscribeBy(
                    onSuccess = { list ->
                        previousState.accountInformation?.displayCurrency?.let { currentSelection ->
                            process(
                                AccountIntent.UpdateViewToLaunch(
                                    ViewToLaunch.DisplayCurrencySelection(currentSelection, list)
                                )
                            )
                        }
                    },
                    onError = {
                        process(AccountIntent.UpdateErrorState(AccountError.FIAT_LIST_FAIL))
                    }
                )
            is AccountIntent.UpdateSelectedDisplayCurrency -> interactor.updateSelectedDisplayCurrency(
                intent.updatedCurrency
            )
                .subscribeBy(
                    onComplete = {
                        previousState.accountInformation?.let { previousInfo ->
                            process(
                                AccountIntent.UpdateAccountInformation(
                                    previousInfo.copy(
                                        displayCurrency = intent.updatedCurrency
                                    )
                                )
                            )
                        }
                    },
                    onError = {
                        process(AccountIntent.UpdateErrorState(AccountError.ACCOUNT_FIAT_UPDATE_FAIL))
                    }
                )
            is AccountIntent.LoadTradingCurrencies -> interactor.getAvailableTradingCurrencies()
                .subscribeBy(
                    onSuccess = { list ->
                        previousState.accountInformation?.tradingCurrency?.let { currentSelection ->
                            process(
                                AccountIntent.UpdateViewToLaunch(
                                    ViewToLaunch.TradingCurrencySelection(currentSelection, list)
                                )
                            )
                        }
                    },
                    onError = {
                        process(AccountIntent.UpdateErrorState(AccountError.FIAT_LIST_FAIL))
                    }
                )
            is AccountIntent.UpdateSelectedTradingCurrency -> interactor.updateSelectedTradingCurrency(
                intent.updatedCurrency
            )
                .subscribeBy(
                    onComplete = {
                        previousState.accountInformation?.let { previousInfo ->
                            process(
                                AccountIntent.UpdateAccountInformation(
                                    previousInfo.copy(
                                        tradingCurrency = intent.updatedCurrency
                                    )
                                )
                            )
                        }
                    },
                    onError = {
                        process(AccountIntent.UpdateErrorState(AccountError.ACCOUNT_FIAT_UPDATE_FAIL))
                    }
                )
            AccountIntent.ToggleChartVibration -> {
                previousState.accountInformation?.let { info ->
                    interactor.toggleChartVibration(
                        info.isChartVibrationEnabled
                    ).subscribeBy(
                        onSuccess = { enabled ->
                            process(AccountIntent.UpdateChartVibration(enabled))
                        },
                        onError = {
                            Timber.e("Error updating chart toggle")
                        }
                    )
                }
            }
            is AccountIntent.UpdateChartVibration,
            is AccountIntent.UpdateAccountInformation,
            is AccountIntent.UpdateErrorState,
            is AccountIntent.ResetViewState,
            is AccountIntent.UpdateViewToLaunch,
            is AccountIntent.UpdateBlockchainCardOrderState -> null
        }.exhaustive
}
