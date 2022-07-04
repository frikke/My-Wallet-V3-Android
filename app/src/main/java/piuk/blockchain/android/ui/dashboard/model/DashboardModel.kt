package piuk.blockchain.android.ui.dashboard.model

import com.blockchain.coincore.AssetAction
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
    private val appRatingService: AppRatingService,
) : MviModel<DashboardState, DashboardIntent>(
    initialState,
    mainScheduler,
    environmentConfig,
    remoteLogger
) {
    override fun performAction(
        previousState: DashboardState,
        intent: DashboardIntent,
    ): Disposable? {
        Timber.d("***> performAction: ${intent.javaClass.simpleName}")
        return when (intent) {
            DashboardIntent.VerifyAppRating -> {
                rxSingle { appRatingService.shouldShowRating() }.subscribe { showRating ->
                    if (showRating) process(DashboardIntent.ShowAppRating)
                }
            }
            DashboardIntent.ShowAppRating -> null
            DashboardIntent.JoinNftWaitlist -> {
                interactor.joinNftWaitlist()
            }
            is DashboardIntent.GetActiveAssets -> interactor.fetchActiveAssets(this)
            is DashboardIntent.UpdateActiveAssets -> interactor.fetchAccounts(
                intent.assetList,
                this
            )
            is DashboardIntent.UpdateAllAssetsAndBalances -> {
                process(DashboardIntent.LoadFundsLocked)
                process(DashboardIntent.RefreshAllBalancesIntent(false))
                null
            }
            is DashboardIntent.GetAssetPrice -> interactor.fetchAssetPrice(this, intent.asset)
            is DashboardIntent.RefreshAllBalancesIntent -> {
                if (previousState.activeAssets.isNotEmpty()) {
                    interactor.refreshBalances(this, previousState)
                } else {
                    process(DashboardIntent.GetActiveAssets)
                    null
                }
            }
            is DashboardIntent.BalanceUpdate -> {
                process(DashboardIntent.RefreshPrices(previousState[intent.asset]))
                null
            }
            is DashboardIntent.RefreshPrices -> interactor.refreshPrices(this, intent.asset)
            is DashboardIntent.AssetPriceWithDeltaUpdate ->
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
            is DashboardIntent.FetchReferralSuccess -> interactor.checkReferralSuccess(this)
            is DashboardIntent.DismissReferralSuccess -> {
                interactor.dismissReferralSuccess()
            }
            is DashboardIntent.ShowReferralSuccess,
            is DashboardIntent.FiatBalanceUpdate,
            is DashboardIntent.BalanceUpdateError,
            is DashboardIntent.PriceHistoryUpdate,
            is DashboardIntent.ClearAnnouncement,
            is DashboardIntent.ShowAnnouncement,
            is DashboardIntent.AssetPriceUpdate,
            is DashboardIntent.ShowFiatAssetDetails,
            is DashboardIntent.ShowBankLinkingSheet,
            is DashboardIntent.ShowPortfolioSheet,
            is DashboardIntent.ClearActiveFlow,
            is DashboardIntent.UpdateSelectedCryptoAccount,
            is DashboardIntent.ShowBackupSheet,
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
            is DashboardIntent.UpdateNavigationAction,
            -> null
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
            AssetAction.FiatWithdraw -> {
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
