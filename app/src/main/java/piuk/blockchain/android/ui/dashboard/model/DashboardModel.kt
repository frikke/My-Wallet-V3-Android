package piuk.blockchain.android.ui.dashboard.model

import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.SingleAccount
import com.blockchain.core.custodial.TradingBalanceDataManager
import com.blockchain.core.payments.PaymentsDataManager
import com.blockchain.logging.CrashLogger
import com.blockchain.preferences.CurrencyPrefs
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import piuk.blockchain.android.ui.base.mvi.MviModel
import piuk.blockchain.android.ui.transactionflow.TransactionFlow
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import timber.log.Timber

class DashboardModel(
    initialState: DashboardState,
    mainScheduler: Scheduler,
    private val interactor: DashboardActionAdapter,
    environmentConfig: EnvironmentConfig,
    crashLogger: CrashLogger,
    private val paymentsDataManager: PaymentsDataManager,
    private val tradingBalanceDataManager: TradingBalanceDataManager,
    private val currencyPrefs: CurrencyPrefs
) : MviModel<DashboardState, DashboardIntent>(
    initialState,
    mainScheduler,
    environmentConfig,
    crashLogger
) {
    override fun performAction(
        previousState: DashboardState,
        intent: DashboardIntent
    ): Disposable? {
        Timber.d("***> performAction: ${intent.javaClass.simpleName}")
        return when (intent) {
            is DashboardIntent.GetActiveAssets -> interactor.fetchActiveAssets(this)
            is DashboardIntent.GetAvailableAssets -> interactor.fetchAvailableAssets(this)
            is DashboardIntent.UpdateAllAssetsAndBalances -> {
                process(DashboardIntent.RefreshAllBalancesIntent)
                null
            }
            is DashboardIntent.GetAssetPrice -> interactor.fetchAssetPrice(this, intent.asset)
            is DashboardIntent.RefreshAllBalancesIntent ->
                interactor.refreshBalances(this, AssetFilter.All, previousState)
            is DashboardIntent.BalanceUpdate -> {
                process(DashboardIntent.CheckForCustodialBalanceIntent(intent.asset))
                null
            }
            is DashboardIntent.CheckForCustodialBalanceIntent -> interactor.checkForCustodialBalance(
                this,
                intent.asset
            )
            is DashboardIntent.UpdateHasCustodialBalanceIntent -> {
                process(DashboardIntent.RefreshPrices(intent.asset))
                null
            }
            is DashboardIntent.RefreshPrices -> interactor.refreshPrices(this, intent.asset)
            is DashboardIntent.AssetPriceUpdate -> interactor.refreshPriceHistory(this, intent.asset)
            is DashboardIntent.CheckBackupStatus -> checkBackupStatus(intent.account, intent.action)
            is DashboardIntent.CancelSimpleBuyOrder -> interactor.cancelSimpleBuyOrder(intent.orderId)
            is DashboardIntent.LaunchBankTransferFlow -> processBankTransferFlow(intent)
            is DashboardIntent.StartBankTransferFlow ->
                interactor.launchBankTransferFlow(this, intent.currency, intent.action)
            is DashboardIntent.LoadWithdrawalLocks -> loadWithdrawalLocks(currencyPrefs.selectedFiatCurrency)
            is DashboardIntent.FiatBalanceUpdate,
            is DashboardIntent.BalanceUpdateError,
            is DashboardIntent.PriceHistoryUpdate,
            is DashboardIntent.ClearAnnouncement,
            is DashboardIntent.ShowAnnouncement,
            is DashboardIntent.ShowFiatAssetDetails,
            is DashboardIntent.ShowBankLinkingSheet,
            is DashboardIntent.ShowPortfolioSheet,
            is DashboardIntent.UpdateLaunchDialogFlow,
            is DashboardIntent.ClearBottomSheet,
            is DashboardIntent.UpdateSelectedCryptoAccount,
            is DashboardIntent.ShowBackupSheet,
            is DashboardIntent.AssetListUpdate,
            is DashboardIntent.LaunchBankLinkFlow,
            is DashboardIntent.ResetDashboardNavigation,
            is DashboardIntent.ShowLinkablePaymentMethodsSheet,
            is DashboardIntent.LongCallStarted,
            is DashboardIntent.LongCallEnded,
            is DashboardIntent.FilterAssets,
            is DashboardIntent.UpdateLaunchDetailsFlow,
            is DashboardIntent.WithdrawalLocksLoaded -> null
        }
    }

    private fun processBankTransferFlow(intent: DashboardIntent.LaunchBankTransferFlow) =
        when (intent.action) {
            AssetAction.FiatDeposit -> {
                interactor.getBankDepositFlow(
                    this,
                    intent.account,
                    intent.action,
                    intent.shouldLaunchBankLinkTransfer
                )
            }
            AssetAction.Withdraw -> {
                interactor.getBankWithdrawalFlow(
                    this,
                    intent.account,
                    intent.action,
                    intent.shouldLaunchBankLinkTransfer
                )
            }
            else -> {
                null
            }
        }

    private fun checkBackupStatus(account: SingleAccount, action: AssetAction): Disposable =
        interactor.hasUserBackedUp()
            .subscribeBy(
                onSuccess = { isBackedUp ->
                    if (isBackedUp) {
                        process(
                            DashboardIntent.UpdateLaunchDialogFlow(
                                TransactionFlow(
                                    sourceAccount = account,
                                    action = action
                                )
                            )
                        )
                    } else {
                        process(DashboardIntent.ShowBackupSheet(account, action))
                    }
                },
                onError = {
                    Timber.e(it)
                }
            )

    override fun distinctIntentFilter(
        previousIntent: DashboardIntent,
        nextIntent: DashboardIntent
    ): Boolean {
        return when {
            previousIntent is DashboardIntent.UpdateLaunchDialogFlow &&
                nextIntent is DashboardIntent.ClearBottomSheet -> true
            else -> super.distinctIntentFilter(previousIntent, nextIntent)
        }
    }

    private fun loadWithdrawalLocks(localCurrency: String): Disposable =
        Single.zip(
            paymentsDataManager.getWithdrawalLocks(localCurrency),
            tradingBalanceDataManager.getBalanceForFiat(localCurrency).singleOrError()
        ) { locks, balance ->
            process(DashboardIntent.WithdrawalLocksLoaded(locks, balance.actionable))
        }.subscribe()
}