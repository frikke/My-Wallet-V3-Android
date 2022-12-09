package piuk.blockchain.android.ui.home

import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import com.blockchain.analytics.events.LaunchOrigin
import com.blockchain.coincore.AssetAction
import com.blockchain.domain.onboarding.CompletableDashboardOnboardingStep
import com.blockchain.home.presentation.navigation.AssetActionsNavigation
import com.blockchain.prices.navigation.PricesNavigation
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Money
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.simplebuy.SimpleBuyActivity
import piuk.blockchain.android.ui.coinview.presentation.CoinViewActivityV2
import piuk.blockchain.android.ui.dashboard.onboarding.DashboardOnboardingActivity
import piuk.blockchain.android.ui.interest.InterestDashboardActivity
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity

class AssetActionsNavigationImpl(
    private val activity: ComponentActivity?
) : AssetActionsNavigation, PricesNavigation {
    private val actionsResultContract =
        activity?.registerForActivityResult(ActionActivity.BlockchainActivityResultContract()) {
            when (it) {
                ActionActivity.ActivityResult.StartKyc -> launchKyc()
                is ActionActivity.ActivityResult.StartReceive -> launchReceive()
                ActionActivity.ActivityResult.StartBuyIntro -> launchBuy()
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

    override fun navigate(assetAction: AssetAction) {
        return actionsResultContract!!.launch(ActionActivity.ActivityArgs(action = assetAction, null))
    }

    override fun buyCrypto(currency: AssetInfo, amount: Money?) {
        activity!!.startActivity(
            SimpleBuyActivity.newIntent(
                context = activity,
                asset = currency,
                preselectedAmount = amount?.toBigDecimal().toString()
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
