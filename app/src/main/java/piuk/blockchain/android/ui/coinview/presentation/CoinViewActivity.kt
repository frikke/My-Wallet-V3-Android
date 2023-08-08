package piuk.blockchain.android.ui.coinview.presentation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.blockchain.analytics.events.LaunchOrigin
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.EarnRewardsAccount
import com.blockchain.coincore.OneTimeAccountPersistenceService
import com.blockchain.coincore.SingleAccount
import com.blockchain.coincore.StateAwareAction
import com.blockchain.coincore.TransactionTarget
import com.blockchain.coincore.impl.CustodialTradingAccount
import com.blockchain.commonarch.presentation.base.HostedBottomSheet
import com.blockchain.commonarch.presentation.base.setContent
import com.blockchain.commonarch.presentation.mvi_v2.MVIActivity
import com.blockchain.commonarch.presentation.mvi_v2.NavigationRouter
import com.blockchain.commonarch.presentation.mvi_v2.bindViewModel
import com.blockchain.componentlib.alert.BlockchainSnackbar
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.utils.openUrl
import com.blockchain.domain.swap.SwapOption
import com.blockchain.earn.activeRewards.ActiveRewardsSummaryBottomSheet
import com.blockchain.earn.activeRewards.viewmodel.ActiveRewardsError
import com.blockchain.earn.interest.InterestSummaryBottomSheet
import com.blockchain.earn.interest.viewmodel.InterestError
import com.blockchain.earn.staking.StakingSummaryBottomSheet
import com.blockchain.earn.staking.viewmodel.StakingError
import com.blockchain.extensions.enumValueOfOrNull
import com.blockchain.home.presentation.recurringbuy.detail.RecurringBuyDetailsSheet
import com.blockchain.koin.payloadScope
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.BlockedReason
import com.blockchain.presentation.customviews.kyc.KycUpgradeNowSheet
import com.blockchain.presentation.extensions.putAccount
import com.blockchain.presentation.sheets.NoBalanceActionBottomSheet
import com.blockchain.transactions.receive.detail.ReceiveAccountDetailFragment
import com.google.android.material.snackbar.Snackbar
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Money
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.KoinScopeComponent
import org.koin.core.scope.Scope
import piuk.blockchain.android.simplebuy.SimpleBuyActivity
import piuk.blockchain.android.support.SupportCentreActivity
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAccount
import piuk.blockchain.android.ui.coinview.presentation.composable.Coinview
import piuk.blockchain.android.ui.coinview.presentation.interstitials.AccountActionsBottomSheet
import piuk.blockchain.android.ui.coinview.presentation.interstitials.AccountExplainerBottomSheet
import piuk.blockchain.android.ui.customviews.BlockedDueToSanctionsSheet
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity
import piuk.blockchain.android.ui.kyc.navhost.models.KycEntryPoint
import piuk.blockchain.android.ui.recurringbuy.onboarding.RecurringBuyOnboardingActivity
import piuk.blockchain.android.ui.swap.SwapSelectorSheet
import piuk.blockchain.android.ui.transactionflow.analytics.CoinViewSellClickedEvent
import piuk.blockchain.android.ui.transactionflow.analytics.SwapAnalyticsEvents
import piuk.blockchain.android.ui.transactionflow.flow.TransactionFlowActivity

class CoinViewActivity :
    MVIActivity<CoinviewViewState>(),
    KoinScopeComponent,
    NavigationRouter<CoinviewNavigationEvent>,
    HostedBottomSheet.Host,
    AccountExplainerBottomSheet.Host,
    NoBalanceActionBottomSheet.Host,
    AccountActionsBottomSheet.Host,
    RecurringBuyDetailsSheet.Host,
    KycUpgradeNowSheet.Host,
    InterestSummaryBottomSheet.Host,
    StakingSummaryBottomSheet.Host,
    ActiveRewardsSummaryBottomSheet.Host,
    SwapSelectorSheet.Host {

    override val alwaysDisableScreenshots: Boolean
        get() = false

    override val scope: Scope = payloadScope
    private val viewModel: CoinviewViewModel by viewModel()

    private val oneTimeAccountPersistenceService: OneTimeAccountPersistenceService by scopedInject()

    private val originName: LaunchOrigin? by lazy {
        enumValueOfOrNull<LaunchOrigin>(intent.getStringExtra(ORIGIN_NAME).orEmpty())
    }

    @Suppress("IMPLICIT_NOTHING_TYPE_ARGUMENT_IN_RETURN_POSITION")
    val args: CoinviewArgs by lazy {
        intent.getParcelableExtra<CoinviewArgs>(CoinviewArgs.ARGS_KEY) ?: error("missing CoinviewArgs")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bindViewModel(
            viewModel = viewModel,
            navigator = this,
            args = args
        )

        setContent {
            Coinview(
                viewModel = viewModel,
                backOnClick = {
                    analytics.logEvent(
                        CoinViewAnalytics
                            .CoinViewClosed(
                                closingMethod = CoinViewAnalytics.Companion.ClosingMethod.BACK_BUTTON,
                                currency = args.networkTicker
                            )
                    )
                    onBackPressedDispatcher.onBackPressed()
                }
            )
        }

        originName?.let {
            analytics.logEvent(
                CoinViewAnalytics.CoinViewOpen(
                    origin = it,
                    currency = args.networkTicker
                )
            )
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onIntent(CoinviewIntent.LoadAllData)
    }

    override fun onStateUpdated(state: CoinviewViewState) {
    }

    override fun route(navigationEvent: CoinviewNavigationEvent) {
        when (navigationEvent) {
            is CoinviewNavigationEvent.ShowAccountExplainer -> {
                navigateToAccountExplainer(
                    cvAccount = navigationEvent.cvAccount,
                    networkTicker = navigationEvent.networkTicker,
                    interestRate = navigationEvent.interestRate,
                    stakingRate = navigationEvent.stakingRate,
                    activeRewardsRate = navigationEvent.activeRewardsRate,
                    actions = navigationEvent.actions
                )
            }

            is CoinviewNavigationEvent.ShowAccountActions -> {
                navigateToAccountActions(
                    cvAccount = navigationEvent.cvAccount,
                    interestRate = navigationEvent.interestRate,
                    stakingRate = navigationEvent.stakingRate,
                    activeRewardsRate = navigationEvent.activeRewardsRate,
                    actions = navigationEvent.actions,
                    balanceCrypto = navigationEvent.cryptoBalance,
                    fiatBalance = navigationEvent.fiatBalance
                )
            }

            is CoinviewNavigationEvent.NavigateToBuy -> {
                analytics.logEvent(CoinViewAnalytics.CoinViewBuyClickedEvent)

                startActivity(
                    SimpleBuyActivity.newIntent(
                        context = this,
                        asset = navigationEvent.asset.currency,
                        fromRecurringBuy = navigationEvent.fromRecurringBuy
                    )
                )
            }

            is CoinviewNavigationEvent.NavigateToSell -> {
                analytics.logEvent(
                    CoinViewAnalytics.BuySellClicked(
                        origin = LaunchOrigin.COIN_VIEW,
                        currency = args.networkTicker,
                        type = CoinViewAnalytics.Companion.Type.SELL
                    )
                )
                analytics.logEvent(CoinViewSellClickedEvent)

                startActivity(
                    TransactionFlowActivity.newIntent(
                        context = this,
                        action = AssetAction.Sell,
                        sourceAccount = navigationEvent.cvAccount.account
                    )
                )
            }

            is CoinviewNavigationEvent.NavigateToSend -> {
                analytics.logEvent(
                    CoinViewAnalytics.SendReceiveClicked(
                        origin = LaunchOrigin.COIN_VIEW,
                        currency = args.networkTicker,
                        type = CoinViewAnalytics.Companion.Type.SEND
                    )
                )

                startActivity(
                    TransactionFlowActivity.newIntent(
                        context = this,
                        action = AssetAction.Send,
                        origin = "CoinviewActivity",
                        sourceAccount = navigationEvent.cvAccount.account
                    )
                )
            }

            is CoinviewNavigationEvent.NavigateToReceive -> {
                if (navigationEvent.isBuyReceive) {
                    analytics.logEvent(
                        CoinViewAnalytics.BuyReceiveClicked(
                            origin = LaunchOrigin.COIN_VIEW,
                            currency = args.networkTicker,
                            type = CoinViewAnalytics.Companion.Type.RECEIVE
                        )
                    )
                } else if (navigationEvent.isSendReceive) {
                    analytics.logEvent(
                        CoinViewAnalytics.SendReceiveClicked(
                            origin = LaunchOrigin.COIN_VIEW,
                            currency = args.networkTicker,
                            type = CoinViewAnalytics.Companion.Type.RECEIVE
                        )
                    )
                }

                oneTimeAccountPersistenceService.saveAccount(navigationEvent.cvAccount.account as SingleAccount)
                showBottomSheet(ReceiveAccountDetailFragment.newInstance())
            }

            is CoinviewNavigationEvent.NavigateToSwap -> {
                analytics.logEvent(SwapAnalyticsEvents.CoinViewSwapClickedEvent)
                when (navigationEvent.swapOption) {
                    SwapOption.BcdcSwap -> {
                        startActivity(
                            TransactionFlowActivity.newIntent(
                                context = this,
                                action = AssetAction.Swap,
                                sourceAccount = navigationEvent.cvAccount.account
                            )
                        )
                    }

                    SwapOption.Dex -> {
                        closeWithDexResult()
                    }

                    SwapOption.Multiple -> {
                        showBottomSheet(
                            SwapSelectorSheet.newInstance(navigationEvent.cvAccount.account as CryptoAccount)
                        )
                    }
                }
            }

            is CoinviewNavigationEvent.NavigateToActivity -> {
                goToActivityFor(navigationEvent.cvAccount.account)
            }

            is CoinviewNavigationEvent.NavigateToInterestStatement ->
                showBottomSheet(
                    InterestSummaryBottomSheet.newInstance(
                        (navigationEvent.cvAccount.account as CryptoAccount).currency.networkTicker
                    )
                )

            is CoinviewNavigationEvent.NavigateToStakingStatement ->
                showBottomSheet(
                    StakingSummaryBottomSheet.newInstance(
                        (navigationEvent.cvAccount.account as CryptoAccount).currency.networkTicker
                    )
                )

            is CoinviewNavigationEvent.NavigateToActiveRewardsStatement ->
                showBottomSheet(
                    ActiveRewardsSummaryBottomSheet.newInstance(
                        (navigationEvent.cvAccount.account as CryptoAccount).currency.networkTicker
                    )
                )

            is CoinviewNavigationEvent.ShowNoBalanceUpsell -> {
                showBottomSheet(
                    NoBalanceActionBottomSheet.newInstance(
                        selectedAccount = navigationEvent.cvAccount.account,
                        action = navigationEvent.action,
                        canBuy = navigationEvent.canBuy
                    )
                )
            }

            CoinviewNavigationEvent.ShowKycUpgrade -> {
                showBottomSheet(KycUpgradeNowSheet.newInstance())
            }

            is CoinviewNavigationEvent.ShowRecurringBuyInfo -> {
                showBottomSheet(RecurringBuyDetailsSheet.newInstance(navigationEvent.recurringBuyId))
            }

            is CoinviewNavigationEvent.NavigateToRecurringBuyUpsell -> {
                startActivity(
                    RecurringBuyOnboardingActivity.newIntent(
                        context = this,
                        assetTicker = navigationEvent.asset.currency.networkTicker
                    )
                )
            }

            CoinviewNavigationEvent.NavigateToSupport -> {
                startActivity(SupportCentreActivity.newIntent(this, SUPPORT_SUBJECT_NO_ASSET))
                finish()
            }

            is CoinviewNavigationEvent.ShowRecurringBuySheet -> {
                showBottomSheet(RecurringBuyDetailsSheet.newInstance(navigationEvent.recurringBuyId))
            }

            is CoinviewNavigationEvent.OpenAssetWebsite -> {
                openUrl(navigationEvent.website)
            }

            is CoinviewNavigationEvent.NavigateToStakingDeposit -> {
                startActivity(
                    TransactionFlowActivity.newIntent(
                        context = this,
                        action = AssetAction.StakingDeposit,
                        target = navigationEvent.cvAccount.account as TransactionTarget
                    )
                )
            }

            is CoinviewNavigationEvent.NavigateToStakingWithdraw -> {
                startActivity(
                    TransactionFlowActivity.newIntent(
                        context = this,
                        action = AssetAction.StakingWithdraw,
                        sourceAccount = navigationEvent.cvSourceStakingAccount.account,
                        target = navigationEvent.cvTargetCustodialTradingAccount.account as TransactionTarget
                    )
                )
            }

            is CoinviewNavigationEvent.NavigateToActiveRewardsDeposit -> {
                startActivity(
                    TransactionFlowActivity.newIntent(
                        context = this,
                        action = AssetAction.ActiveRewardsDeposit,
                        target = navigationEvent.cvAccount.account as TransactionTarget
                    )
                )
            }

            is CoinviewNavigationEvent.NavigateToActiveRewardsWithdraw -> {
                startActivity(
                    TransactionFlowActivity.newIntent(
                        context = this,
                        action = AssetAction.ActiveRewardsWithdraw,
                        sourceAccount = navigationEvent.cvSourceActiveRewardsAccount.account,
                        target = navigationEvent.cvTargetCustodialTradingAccount.account as TransactionTarget
                    )
                )
            }
        }
    }

    private fun navigateToAccountExplainer(
        cvAccount: CoinviewAccount,
        networkTicker: String,
        interestRate: Double,
        stakingRate: Double,
        activeRewardsRate: Double,
        actions: List<StateAwareAction>
    ) {
        showBottomSheet(
            AccountExplainerBottomSheet.newInstance(
                selectedAccount = cvAccount.account,
                networkTicker = networkTicker,
                interestRate = interestRate,
                stakingRate = stakingRate,
                activeRewardsRate = activeRewardsRate,
                stateAwareActions = actions.toTypedArray()
            )
        )
    }

    private fun navigateToAccountActions(
        cvAccount: CoinviewAccount,
        interestRate: Double,
        stakingRate: Double,
        activeRewardsRate: Double,
        fiatBalance: Money?,
        balanceCrypto: Money,
        actions: List<StateAwareAction>
    ) {
        showBottomSheet(
            AccountActionsBottomSheet.newInstance(
                selectedAccount = cvAccount.account,
                balanceFiat = fiatBalance,
                balanceCrypto = balanceCrypto,
                interestRate = interestRate,
                stakingRate = stakingRate,
                activeRewardsRate = activeRewardsRate,
                stateAwareActions = actions.toTypedArray()
            )
        )
    }

    // host calls
    override fun navigateToActionSheet(actions: Array<StateAwareAction>, account: BlockchainAccount) {
        viewModel.onIntent(
            CoinviewIntent.AccountExplainerAcknowledged(
                account = account,
                actions = actions.toList()
            )
        )
    }

    override fun navigateToAction(action: AssetAction, selectedAccount: BlockchainAccount, assetInfo: AssetInfo) {
        viewModel.onIntent(
            CoinviewIntent.AccountActionSelected(
                account = selectedAccount,
                action = action
            )
        )
    }

    override fun showBalanceUpsellSheet(item: AccountActionsBottomSheet.AssetActionItem) {
        item.account?.let {
            viewModel.onIntent(
                CoinviewIntent.NoBalanceUpsell(
                    account = item.account,
                    action = item.action.action
                )
            )
        }
    }

    override fun showSanctionsSheet(reason: BlockedReason.Sanctions) {
        showBottomSheet(BlockedDueToSanctionsSheet.newInstance(reason))
    }

    override fun showUpgradeKycSheet() {
        showBottomSheet(KycUpgradeNowSheet.newInstance())
    }

    fun goToActivityFor(account: BlockchainAccount) {
        val intent = Intent().apply {
            putAccount(ACCOUNT_FOR_ACTIVITY, account)
        }

        setResult(RESULT_OK, intent)
        finish()
    }

    override fun startKycClicked() {
        KycNavHostActivity.startForResult(this, KycEntryPoint.Buy, SimpleBuyActivity.KYC_STARTED)
    }

    override fun onRecurringBuyDeleted() {
        viewModel.onIntent(CoinviewIntent.RecurringBuyDeleted)
    }

    override fun openExternalUrl(url: String) {
        openUrl(url)
    }

    override fun launchInterestDeposit(account: EarnRewardsAccount.Interest) {
        analytics.logEvent(
            CoinViewAnalytics.RewardsWithdrawOrAddClicked(
                origin = LaunchOrigin.COIN_VIEW,
                currency = (account as CryptoAccount).currency.networkTicker,
                type = CoinViewAnalytics.Companion.Type.ADD
            )
        )

        startActivity(
            TransactionFlowActivity.newIntent(
                context = this,
                action = AssetAction.InterestDeposit,
                target = account as TransactionTarget,
                origin = "",
            )
        )
    }

    override fun launchInterestWithdrawal(sourceAccount: BlockchainAccount) {
        analytics.logEvent(
            CoinViewAnalytics.RewardsWithdrawOrAddClicked(
                origin = LaunchOrigin.COIN_VIEW,
                currency = (sourceAccount as CryptoAccount).currency.networkTicker,
                type = CoinViewAnalytics.Companion.Type.WITHDRAW
            )
        )

        startActivity(
            TransactionFlowActivity.newIntent(
                context = this,
                action = AssetAction.InterestWithdraw,
                sourceAccount = sourceAccount,
                origin = "",
            )
        )
    }

    override fun showInterestLoadingError(error: InterestError) {
        BlockchainSnackbar.make(
            view = window.decorView.rootView,
            message = when (error) {
                is InterestError.UnknownAsset -> getString(
                    com.blockchain.stringResources.R.string.earn_summary_sheet_error_unknown_asset,
                    error.assetTicker
                )

                InterestError.Other -> getString(com.blockchain.stringResources.R.string.earn_summary_sheet_error_other)
                InterestError.None -> getString(com.blockchain.stringResources.R.string.empty)
            },
            duration = Snackbar.LENGTH_SHORT,
            type = SnackbarType.Error
        ).show()
    }

    override fun launchStakingDeposit(account: EarnRewardsAccount.Staking) {
        analytics.logEvent(
            CoinViewAnalytics.RewardsWithdrawOrAddClicked(
                origin = LaunchOrigin.COIN_VIEW,
                currency = (account as CryptoAccount).currency.networkTicker,
                type = CoinViewAnalytics.Companion.Type.ADD
            )
        )

        viewModel.onIntent(CoinviewIntent.LaunchStakingDepositFlow(account))
    }

    override fun launchStakingWithdrawal(
        sourceAccount: BlockchainAccount,
        targetAccount: CustodialTradingAccount
    ) {
        analytics.logEvent(
            CoinViewAnalytics.RewardsWithdrawOrAddClicked(
                origin = LaunchOrigin.COIN_VIEW,
                currency = (sourceAccount as CryptoAccount).currency.networkTicker,
                type = CoinViewAnalytics.Companion.Type.WITHDRAW
            )
        )

        viewModel.onIntent(CoinviewIntent.LaunchStakingWithdrawFlow)
    }

    override fun launchActiveRewardsDeposit(account: EarnRewardsAccount.Active) {
        analytics.logEvent(
            CoinViewAnalytics.RewardsWithdrawOrAddClicked(
                origin = LaunchOrigin.COIN_VIEW,
                currency = (account as CryptoAccount).currency.networkTicker,
                type = CoinViewAnalytics.Companion.Type.ADD
            )
        )

        viewModel.onIntent(CoinviewIntent.LaunchActiveRewardsDepositFlow)
    }

    override fun launchActiveRewardsWithdrawal(
        sourceAccount: BlockchainAccount,
        targetAccount: CustodialTradingAccount
    ) {
        analytics.logEvent(
            CoinViewAnalytics.RewardsWithdrawOrAddClicked(
                origin = LaunchOrigin.COIN_VIEW,
                currency = (sourceAccount as CryptoAccount).currency.networkTicker,
                type = CoinViewAnalytics.Companion.Type.WITHDRAW
            )
        )

        viewModel.onIntent(CoinviewIntent.LaunchActiveRewardsWithdrawFlow)
    }

    override fun showStakingLoadingError(error: StakingError) =
        BlockchainSnackbar.make(
            view = window.decorView.rootView,
            message = when (error) {
                is StakingError.UnknownAsset -> getString(
                    com.blockchain.stringResources.R.string.earn_summary_sheet_error_unknown_asset,
                    error.assetTicker
                )

                StakingError.Other -> getString(com.blockchain.stringResources.R.string.earn_summary_sheet_error_other)
                StakingError.None -> getString(com.blockchain.stringResources.R.string.empty)
            },
            duration = Snackbar.LENGTH_SHORT,
            type = SnackbarType.Error
        ).show()

    override fun showActiveRewardsLoadingError(error: ActiveRewardsError) =
        BlockchainSnackbar.make(
            view = window.decorView.rootView,
            message = when (error) {
                is ActiveRewardsError.UnknownAsset -> getString(
                    com.blockchain.stringResources.R.string.earn_summary_sheet_error_unknown_asset,
                    error.assetTicker
                )

                ActiveRewardsError.Other -> getString(
                    com.blockchain.stringResources.R.string.earn_summary_sheet_error_other
                )

                ActiveRewardsError.None -> getString(com.blockchain.stringResources.R.string.empty)
            },
            duration = Snackbar.LENGTH_SHORT,
            type = SnackbarType.Error
        ).show()

    override fun onSwapSelectorOpenSwap(account: CryptoAccount) {
        startActivity(
            TransactionFlowActivity.newIntent(
                context = this,
                action = AssetAction.Swap,
                sourceAccount = account
            )
        )
    }

    override fun onSwapSelectorOpenDex() {
        closeWithDexResult()
    }

    private fun closeWithDexResult() {
        setResult(RESULT_DEX)
        finish()
    }

    override fun onSheetClosed() {}
    // host calls/

    companion object {
        private const val ORIGIN_NAME = "ORIGIN_NAME"
        const val ACCOUNT_FOR_ACTIVITY = "ACCOUNT_FOR_ACTIVITY"

        const val RESULT_DEX = 100

        fun newIntent(
            context: Context,
            asset: AssetInfo,
            recurringBuyId: String? = null,
            originScreen: String
        ): Intent {
            return Intent(context, CoinViewActivity::class.java).apply {
                putExtra(
                    CoinviewArgs.ARGS_KEY,
                    CoinviewArgs(
                        networkTicker = asset.networkTicker,
                        recurringBuyId = recurringBuyId
                    )
                )
                putExtra(ORIGIN_NAME, originScreen)
            }
        }

        private const val SUPPORT_SUBJECT_NO_ASSET = "UNKNOWN ASSET"
    }
}
