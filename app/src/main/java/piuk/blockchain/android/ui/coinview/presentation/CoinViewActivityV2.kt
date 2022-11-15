package piuk.blockchain.android.ui.coinview.presentation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import com.blockchain.analytics.events.LaunchOrigin
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.StateAwareAction
import com.blockchain.coincore.TransactionTarget
import com.blockchain.commonarch.presentation.base.HostedBottomSheet
import com.blockchain.commonarch.presentation.mvi_v2.MVIActivity
import com.blockchain.commonarch.presentation.mvi_v2.NavigationRouter
import com.blockchain.commonarch.presentation.mvi_v2.bindViewModel
import com.blockchain.extensions.enumValueOfOrNull
import com.blockchain.koin.payloadScope
import com.blockchain.nabu.BlockedReason
import info.blockchain.balance.AssetInfo
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.KoinScopeComponent
import org.koin.core.scope.Scope
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.simplebuy.SimpleBuyActivity
import piuk.blockchain.android.support.SupportCentreActivity
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAccount
import piuk.blockchain.android.ui.coinview.presentation.composable.Coinview
import piuk.blockchain.android.ui.coinview.presentation.composable.StakingAccountSheet
import piuk.blockchain.android.ui.customviews.BlockedDueToSanctionsSheet
import piuk.blockchain.android.ui.dashboard.coinview.CoinViewAnalytics
import piuk.blockchain.android.ui.dashboard.coinview.interstitials.AccountActionsBottomSheet
import piuk.blockchain.android.ui.dashboard.coinview.interstitials.AccountExplainerBottomSheet
import piuk.blockchain.android.ui.dashboard.coinview.interstitials.NoBalanceActionBottomSheet
import piuk.blockchain.android.ui.dashboard.coinview.recurringbuy.RecurringBuyDetailsSheet
import piuk.blockchain.android.ui.dashboard.sheets.KycUpgradeNowSheet
import piuk.blockchain.android.ui.interest.InterestSummarySheet
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity
import piuk.blockchain.android.ui.recurringbuy.onboarding.RecurringBuyOnboardingActivity
import piuk.blockchain.android.ui.transactionflow.analytics.CoinViewSellClickedEvent
import piuk.blockchain.android.ui.transactionflow.analytics.SwapAnalyticsEvents
import piuk.blockchain.android.ui.transactionflow.flow.TransactionFlowActivity
import piuk.blockchain.android.ui.transfer.receive.detail.ReceiveDetailActivity
import piuk.blockchain.android.urllinks.STAKING_LEARN_MORE
import piuk.blockchain.android.urllinks.STAKING_WEB_APP
import piuk.blockchain.android.util.openUrl
import piuk.blockchain.android.util.putAccount

// TODO (dserrano) - STAKING - rename this when staking FF is removed & old [CoinViewActivity] is obsolete
class CoinViewActivityV2 :
    MVIActivity<CoinviewViewState>(),
    KoinScopeComponent,
    NavigationRouter<CoinviewNavigationEvent>,
    HostedBottomSheet.Host,
    AccountExplainerBottomSheet.Host,
    NoBalanceActionBottomSheet.Host,
    AccountActionsBottomSheet.Host,
    InterestSummarySheet.Host,
    RecurringBuyDetailsSheet.Host,
    KycUpgradeNowSheet.Host,
    StakingAccountSheet.Host {

    override val alwaysDisableScreenshots: Boolean
        get() = false

    override val scope: Scope = payloadScope
    private val viewModel: CoinviewViewModel by viewModel()

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
                    currency = args.networkTicker,
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
                    actions = navigationEvent.actions
                )
            }

            is CoinviewNavigationEvent.ShowAccountActions -> {
                navigateToAccountActions(
                    cvAccount = navigationEvent.cvAccount,
                    interestRate = navigationEvent.interestRate,
                    actions = navigationEvent.actions
                )
            }

            is CoinviewNavigationEvent.NavigateToBuy -> {
                analytics.logEvent(CoinViewAnalytics.CoinViewBuyClickedEvent)

                startActivity(
                    SimpleBuyActivity.newIntent(
                        context = this,
                        asset = navigationEvent.asset.currency
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

                startActivity(
                    ReceiveDetailActivity.newIntent(
                        context = this, account = navigationEvent.cvAccount.account as CryptoAccount
                    )
                )
            }

            is CoinviewNavigationEvent.NavigateToSwap -> {
                analytics.logEvent(SwapAnalyticsEvents.CoinViewSwapClickedEvent)

                startActivity(
                    TransactionFlowActivity.newIntent(
                        context = this,
                        action = AssetAction.Swap,
                        sourceAccount = navigationEvent.cvAccount.account
                    )
                )
            }

            is CoinviewNavigationEvent.NavigateToActivity -> {
                goToActivityFor(navigationEvent.cvAccount.account)
            }

            is CoinviewNavigationEvent.NavigateToInterestStatement -> {
                showBottomSheet(InterestSummarySheet.newInstance(navigationEvent.cvAccount.account as CryptoAccount))
            }

            is CoinviewNavigationEvent.NavigateToInterestDeposit -> {
                goToInterestDeposit(navigationEvent.cvAccount.account)
            }

            is CoinviewNavigationEvent.NavigateToInterestWithdraw -> {
                goToInterestWithdraw(navigationEvent.cvAccount.account)
            }

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
                        fromCoinView = true,
                        assetTicker = navigationEvent.asset.currency.networkTicker
                    )
                )
            }
            is CoinviewNavigationEvent.ShowStakingAccountInterstitial -> {
                showBottomSheet(StakingAccountSheet.newInstance(navigationEvent.assetIconUrl))
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
        }
    }

    private fun navigateToAccountExplainer(
        cvAccount: CoinviewAccount,
        networkTicker: String,
        interestRate: Double,
        stakingRate: Double,
        actions: List<StateAwareAction>
    ) {
        showBottomSheet(
            AccountExplainerBottomSheet.newInstance(
                selectedAccount = cvAccount.account,
                networkTicker = networkTicker,
                interestRate = interestRate,
                stakingRate = stakingRate,
                stateAwareActions = actions.toTypedArray()
            )
        )
    }

    private fun navigateToAccountActions(
        cvAccount: CoinviewAccount,
        interestRate: Double,
        actions: List<StateAwareAction>
    ) {
        showBottomSheet(
            AccountActionsBottomSheet.newInstance(
                selectedAccount = cvAccount.account,
                balanceFiat = cvAccount.fiatBalance,
                balanceCrypto = cvAccount.cryptoBalance,
                interestRate = interestRate,
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

    override fun goToActivityFor(account: BlockchainAccount) {
        val intent = Intent().apply {
            putAccount(ACCOUNT_FOR_ACTIVITY, account)
        }

        setResult(RESULT_OK, intent)
        finish()
    }

    override fun goToInterestDeposit(toAccount: BlockchainAccount) {
        startActivity(
            TransactionFlowActivity.newIntent(
                context = this,
                action = AssetAction.InterestDeposit,
                target = toAccount as TransactionTarget
            )
        )
    }

    override fun goToInterestWithdraw(fromAccount: BlockchainAccount) {
        startActivity(
            TransactionFlowActivity.newIntent(
                context = this,
                action = AssetAction.InterestWithdraw,
                sourceAccount = fromAccount
            )
        )
    }

    override fun startKycClicked() {
        KycNavHostActivity.startForResult(this, CampaignType.SimpleBuy, SimpleBuyActivity.KYC_STARTED)
    }

    override fun onRecurringBuyDeleted(asset: AssetInfo) {
        viewModel.onIntent(CoinviewIntent.LoadRecurringBuysData)
    }

    override fun learnMoreClicked() {
        openUrl(STAKING_LEARN_MORE)
    }

    override fun goToWebAppClicked() {
        openUrl(STAKING_WEB_APP)
    }

    override fun onSheetClosed() {}
    // host calls/

    companion object {
        private const val ORIGIN_NAME = "ORIGIN_NAME"
        const val ACCOUNT_FOR_ACTIVITY = "ACCOUNT_FOR_ACTIVITY"

        fun newIntent(
            context: Context,
            asset: AssetInfo,
            recurringBuyId: String? = null,
            originScreen: String
        ): Intent {
            return Intent(context, CoinViewActivityV2::class.java).apply {
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
