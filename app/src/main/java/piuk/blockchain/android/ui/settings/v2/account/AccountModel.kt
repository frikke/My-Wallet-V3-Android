package piuk.blockchain.android.ui.settings.v2.account

import com.blockchain.commonarch.presentation.mvi.MviModel
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.extensions.exhaustive
import com.blockchain.logging.CrashLogger
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy

class AccountModel(
    initialState: AccountState,
    mainScheduler: Scheduler,
    private val interactor: AccountInteractor,
    environmentConfig: EnvironmentConfig,
    crashLogger: CrashLogger
) : MviModel<AccountState, AccountIntent>(
    initialState,
    mainScheduler,
    environmentConfig,
    crashLogger
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
            is AccountIntent.LoadExchangeInformation -> interactor.getExchangeState()
                .subscribeBy(
                    onSuccess = {
                        process(AccountIntent.UpdateExchangeInformation(it))
                    },
                    onError = {
                        process(AccountIntent.UpdateErrorState(AccountError.EXCHANGE_INFO_FAIL))
                    }
                )
            is AccountIntent.LoadExchange -> {
                interactor.getExchangeState()
                    .subscribeBy(
                        onSuccess = {
                            process(AccountIntent.UpdateViewToLaunch(ViewToLaunch.ExchangeLink(it)))
                        },
                        onError = {
                            process(AccountIntent.UpdateErrorState(AccountError.EXCHANGE_LOAD_FAIL))
                        }
                    )
            }
            is AccountIntent.LoadFiatList -> interactor.getAvailableFiatList()
                .subscribeBy(
                    onSuccess = { list ->
                        previousState.accountInformation?.userCurrency?.let { currentSelection ->
                            process(
                                AccountIntent.UpdateViewToLaunch(ViewToLaunch.CurrencySelection(currentSelection, list))
                            )
                        }
                    },
                    onError = {
                        process(AccountIntent.UpdateErrorState(AccountError.FIAT_LIST_FAIL))
                    }
                )
            is AccountIntent.UpdateFiatCurrency -> interactor.updateSelectedCurrency(intent.updatedCurrency)
                .subscribeBy(
                    onComplete = {
                        previousState.accountInformation?.let { previousInfo ->
                            process(
                                AccountIntent.UpdateAccountInformation(
                                    previousInfo.copy(
                                        userCurrency = intent.updatedCurrency
                                    )
                                )
                            )
                        }
                    },
                    onError = {
                        process(AccountIntent.UpdateErrorState(AccountError.ACCOUNT_FIAT_UPDATE_FAIL))
                    }
                )
            is AccountIntent.UpdateErrorState,
            is AccountIntent.ResetViewState,
            is AccountIntent.UpdateViewToLaunch,
            is AccountIntent.UpdateAccountInformation,
            is AccountIntent.UpdateExchangeInformation -> null
        }.exhaustive
}
