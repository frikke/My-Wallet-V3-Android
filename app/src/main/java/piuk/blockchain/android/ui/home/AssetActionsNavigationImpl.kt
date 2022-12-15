package piuk.blockchain.android.ui.home

import android.os.Handler
import android.os.Looper
import com.blockchain.analytics.events.LaunchOrigin
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.CryptoAccount
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.domain.onboarding.CompletableDashboardOnboardingStep
import com.blockchain.earn.interest.InterestSummarySheet
import com.blockchain.earn.staking.StakingSummaryBottomSheet
import com.blockchain.home.presentation.navigation.AssetActionsNavigation
import com.blockchain.prices.navigation.PricesNavigation
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Currency
import info.blockchain.balance.Money
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.simplebuy.SimpleBuyActivity
import piuk.blockchain.android.ui.coinview.presentation.CoinViewActivityV2
import piuk.blockchain.android.ui.dashboard.onboarding.DashboardOnboardingActivity
import piuk.blockchain.android.ui.interest.InterestDashboardActivity
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity
import piuk.blockchain.android.ui.settings.SettingsActivity

class AssetActionsNavigationImpl(private val activity: BlockchainActivity?) : AssetActionsNavigation, PricesNavigation {

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

    override fun receive(currency: String) {
        actionsResultContract!!.launch(ActionActivity.ActivityArgs(AssetAction.Receive, cryptoTicker = currency))
    }

    override fun buyCrypto(currency: AssetInfo, amount: Money) {
        activity!!.startActivity(
            SimpleBuyActivity.newIntent(
                context = activity,
                asset = currency,
                preselectedAmount = amount.toBigDecimal().toString(),
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
                launchNewPaymentMethodSelection = launchNewPaymentMethodSelection,
            )
        )
    }

    override fun earnRewards() {
        activity!!.startActivity(
            InterestDashboardActivity.newInstance(
                activity
            )
        )
    }

    override fun settings() {
        activity?.startActivity(SettingsActivity.newIntent(activity))
    }

    override fun interestSummary(account: CryptoAccount) {
        activity?.showBottomSheet(InterestSummarySheet.newInstance(account))
    }

    override fun stakingSummary(currency: Currency) {
        activity?.showBottomSheet(StakingSummaryBottomSheet.newInstance(currency.networkTicker, false))
    }

    override fun coinview(asset: AssetInfo, recurringBuyId: String?, originScreen: String) {
        activity!!.startActivity(
            CoinViewActivityV2.newIntent(
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
            CoinViewActivityV2.newIntent(
                context = activity,
                asset = asset,
                originScreen = LaunchOrigin.HOME.name,
            )
        )
    }

    override fun onBoardingNavigation(initialSteps: List<CompletableDashboardOnboardingStep>) {
        activityResultDashboardOnboarding?.launch(
            DashboardOnboardingActivity.ActivityArgs(initialSteps = initialSteps)
        )
    }
}
