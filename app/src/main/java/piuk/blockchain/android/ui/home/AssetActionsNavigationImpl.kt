package piuk.blockchain.android.ui.home

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.lifecycleScope
import com.blockchain.analytics.events.LaunchOrigin
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.impl.CryptoNonCustodialAccount
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.domain.onboarding.CompletableDashboardOnboardingStep
import com.blockchain.domain.paymentmethods.model.FundsLocks
import com.blockchain.earn.activeRewards.ActiveRewardsSummaryBottomSheet
import com.blockchain.earn.interest.InterestSummaryBottomSheet
import com.blockchain.earn.staking.StakingSummaryBottomSheet
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.home.presentation.navigation.AssetActionsNavigation
import com.blockchain.koin.newSellFlowFeatureFlag
import com.blockchain.koin.newSwapFlowFeatureFlag
import com.blockchain.prices.navigation.PricesNavigation
import info.blockchain.balance.AssetInfo
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.simplebuy.SimpleBuyActivity
import piuk.blockchain.android.ui.coinview.presentation.CoinViewActivity
import piuk.blockchain.android.ui.dashboard.onboarding.DashboardOnboardingActivity
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity
import piuk.blockchain.android.ui.locks.LocksDetailsActivity
import piuk.blockchain.android.ui.settings.SettingsActivity
import piuk.blockchain.android.ui.transactionflow.flow.TransactionFlowActivity
import piuk.blockchain.android.ui.transfer.receive.detail.ReceiveDetailActivity

class AssetActionsNavigationImpl(private val activity: BlockchainActivity?) : AssetActionsNavigation, PricesNavigation {

    private val newSwapFlowFF: FeatureFlag?
        get() = activity?.run {
            get(newSwapFlowFeatureFlag)
        }
    private val newSellFlowFF: FeatureFlag?
        get() = activity?.run {
            get(newSellFlowFeatureFlag)
        }

    private val actionsResultContract =
        activity?.registerForActivityResult(ActionActivity.BlockchainActivityResultContract()) {
            when (it) {
                ActionActivity.ActivityResult.StartKyc -> launchKyc()
                is ActionActivity.ActivityResult.StartReceive -> launchReceive()
                ActionActivity.ActivityResult.StartBuyIntro -> launchBuy()
                ActionActivity.ActivityResult.ViewActivity -> launchViewActivity()
                null -> {
                }
            }
        }

    override fun unregister() {
        actionsResultContract?.unregister()
    }

    private val activityResultDashboardOnboarding =
        activity?.registerForActivityResult(DashboardOnboardingActivity.BlockchainActivityResultContract()) { result ->
            when (result) {
                DashboardOnboardingActivity.ActivityResult.LaunchBuyFlow -> Handler(Looper.getMainLooper()).post {
                    launchBuy()
                }
                null -> {
                }
            }
        }

    private fun launchBuy() {
        actionsResultContract!!.launch(ActionActivity.ActivityArgs(action = AssetAction.Buy, null))
    }

    private fun launchReceive() {
        actionsResultContract!!.launch(ActionActivity.ActivityArgs(AssetAction.Receive))
    }

    private fun launchKyc() {
        KycNavHostActivity.start(activity!!, campaignType = CampaignType.None)
    }

    private fun launchViewActivity() {
        actionsResultContract!!.launch(ActionActivity.ActivityArgs(AssetAction.ViewActivity))
    }

    override fun navigate(assetAction: AssetAction) {
        return actionsResultContract!!.launch(ActionActivity.ActivityArgs(action = assetAction, null))
    }

    override fun buyCryptoWithRecurringBuy() {
        return actionsResultContract!!.launch(
            ActionActivity.ActivityArgs(action = AssetAction.Buy, fromRecurringBuy = true)
        )
    }

    override fun receive(currency: String) {
        actionsResultContract!!.launch(ActionActivity.ActivityArgs(AssetAction.Receive, cryptoTicker = currency))
    }
    override fun receive(account: CryptoNonCustodialAccount) {
        activity?.startActivity(
            ReceiveDetailActivity.newIntent(
                context = activity,
                account = account
            )
        )
    }

    override fun buyCrypto(
        currency: AssetInfo,
        amount: String?,
        preselectedFiatTicker: String?,
        launchLinkCard: Boolean,
        launchNewPaymentMethodSelection: Boolean
    ) {
        activity!!.startActivity(
            SimpleBuyActivity.newIntent(
                context = activity,
                asset = currency,
                preselectedAmount = amount,
                preselectedFiatTicker = preselectedFiatTicker,
                launchLinkCard = launchLinkCard,
                launchNewPaymentMethodSelection = launchNewPaymentMethodSelection
            )
        )
    }

    override fun buyWithPreselectedMethod(paymentMethodId: String?) {
        activity!!.startActivity(
            SimpleBuyActivity.newIntent(
                context = activity,
                preselectedPaymentMethodId = paymentMethodId
            )
        )
    }

    override fun settings() {
        activity?.startActivity(SettingsActivity.newIntent(activity))
    }

    override fun interestSummary(account: CryptoAccount) {
        activity?.showBottomSheet(InterestSummaryBottomSheet.newInstance(account.currency.networkTicker))
    }

    override fun stakingSummary(networkTicker: String) {
        activity?.showBottomSheet(StakingSummaryBottomSheet.newInstance(networkTicker))
    }

    override fun activeRewardsSummary(networkTicker: String) {
        activity?.showBottomSheet(ActiveRewardsSummaryBottomSheet.newInstance(networkTicker))
    }

    override fun fundsLocksDetail(fundsLocks: FundsLocks) {
        activity?.let { LocksDetailsActivity.start(activity, fundsLocks) }
    }

    override fun coinview(asset: AssetInfo, recurringBuyId: String?, originScreen: String) {
        activity!!.startActivity(
            CoinViewActivity.newIntent(
                context = activity,
                asset = asset,
                recurringBuyId = recurringBuyId,
                originScreen = originScreen
            )
        )
    }

    override fun startKyc() {
        activity!!.startActivity(KycNavHostActivity.newIntent(activity, CampaignType.None))
    }

    override fun coinview(
        asset: AssetInfo
    ) {
        activity!!.startActivity(
            CoinViewActivity.newIntent(
                context = activity,
                asset = asset,
                originScreen = LaunchOrigin.HOME.name
            )
        )
    }

    override fun onBoardingNavigation(initialSteps: List<CompletableDashboardOnboardingStep>) {
        activityResultDashboardOnboarding?.launch(
            DashboardOnboardingActivity.ActivityArgs(initialSteps = initialSteps, isSuperappDesignEnabled = true)
        )
    }

    // TODO(aromano): Funky code, but was the least intrusive way of feature flagging this,
    //                since TransactionFlowActivity is used in dozens of places it would have required FFing everywhere
    override fun initNewTxFlowFFs() {
        activity?.lifecycleScope?.launch {
            TransactionFlowActivity.newSwapFlowFFEnabled = newSwapFlowFF?.coEnabled() ?: false
            TransactionFlowActivity.newSellFlowFFEnabled = newSellFlowFF?.coEnabled() ?: false
        }
    }
}
