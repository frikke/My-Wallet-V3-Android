package piuk.blockchain.android.ui.dashboard.model

import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.SingleAccount
import com.blockchain.commonarch.presentation.mvi.MviModel
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.extensions.exhaustive
import com.blockchain.logging.RemoteLogger
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import kotlinx.coroutines.rx3.rxSingle
import piuk.blockchain.android.rating.domain.service.AppRatingService
import piuk.blockchain.android.ui.dashboard.navigation.DashboardNavigationAction
import timber.log.Timber

class DashboardModel(
    initialState: DashboardState,
    mainScheduler: Scheduler,
    private val interactor: DashboardActionInteractor,
    environmentConfig: EnvironmentConfig,
    remoteLogger: RemoteLogger,
    private val appRatingService: AppRatingService
) : MviModel<DashboardState, DashboardIntent>(
    initialState,
    mainScheduler,
    environmentConfig,
    remoteLogger
) {
    override fun performAction(
        previousState: DashboardState,
        intent: DashboardIntent
    ): Disposable? {
        Timber.d("***> performAction: ${intent.javaClass.simpleName}")
        return when (intent) {
            DashboardIntent.VerifyAppRating -> {
                rxSingle { appRatingService.shouldShowRating() }.subscribe { showRating ->
                    if (showRating) process(DashboardIntent.ShowAppRating)
                }
            }
            DashboardIntent.ShowAppRating -> null
            is DashboardIntent.GetActiveAssets -> interactor.fetchActiveAssets(this)
            is DashboardIntent.GetAvailableAssets -> interactor.fetchAvailableAssets(this)
            is DashboardIntent.UpdateAllAssetsAndBalances -> {
                process(DashboardIntent.RefreshAllBalancesIntent(false))
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
            is DashboardIntent.AssetPriceUpdate ->
                if (intent.shouldFetchDayHistoricalPrices) interactor.refreshPriceHistory(this, intent.asset)
                else null
            is DashboardIntent.CheckBackupStatus -> checkBackupStatus(intent.account, intent.action)
            is DashboardIntent.CancelSimpleBuyOrder -> interactor.cancelSimpleBuyOrder(intent.orderId)
            is DashboardIntent.LaunchBankTransferFlow -> processBankTransferFlow(intent)
            is DashboardIntent.StartBankTransferFlow ->
                interactor.launchBankTransferFlow(this, intent.currency, intent.action)
            is DashboardIntent.UpdateDepositButton -> userCanDeposit()
            is DashboardIntent.LoadFundsLocked -> interactor.loadWithdrawalLocks(this)
            is DashboardIntent.FetchOnboardingSteps -> interactor.getOnboardingSteps(this)
            is DashboardIntent.RefreshFiatBalances -> interactor.refreshFiatBalances(intent.fiatAccounts, this)
            is DashboardIntent.FiatBalanceUpdate,
            is DashboardIntent.BalanceUpdateError,
            is DashboardIntent.PriceHistoryUpdate,
            is DashboardIntent.ClearAnnouncement,
            is DashboardIntent.ShowAnnouncement,
            is DashboardIntent.ShowFiatAssetDetails,
            is DashboardIntent.ShowBankLinkingSheet,
            is DashboardIntent.ShowPortfolioSheet,
            is DashboardIntent.ClearActiveFlow,
            is DashboardIntent.UpdateSelectedCryptoAccount,
            is DashboardIntent.ShowBackupSheet,
            is DashboardIntent.AssetListUpdate,
            is DashboardIntent.LaunchBankLinkFlow,
            is DashboardIntent.ResetNavigation,
            is DashboardIntent.ShowLinkablePaymentMethodsSheet,
            is DashboardIntent.LongCallStarted,
            is DashboardIntent.LongCallEnded,
            is DashboardIntent.FilterAssets,
            is DashboardIntent.FundsLocksLoaded,
            is DashboardIntent.FetchOnboardingStepsSuccess,
            is DashboardIntent.LaunchDashboardOnboarding,
            is DashboardIntent.SetDepositVisibility,
            DashboardIntent.ResetDashboardAssets,
            is DashboardIntent.UpdateNavigationAction -> null
        }.exhaustive
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

    private fun userCanDeposit(): Disposable =
        interactor.canDeposit().subscribeBy(
            onSuccess = { canDeposit ->
                process(DashboardIntent.SetDepositVisibility(canDeposit))
            },
            onError = {
                Timber.e(it)
            }
        )

    private fun checkBackupStatus(account: SingleAccount, action: AssetAction): Disposable =
        interactor.hasUserBackedUp()
            .subscribeBy(
                onSuccess = { isBackedUp ->
                    if (isBackedUp) {
                        process(
                            DashboardIntent.UpdateNavigationAction(
                                DashboardNavigationAction.TransactionFlow(
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
}
