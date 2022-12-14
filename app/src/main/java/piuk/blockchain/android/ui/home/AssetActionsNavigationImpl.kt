package piuk.blockchain.android.ui.home

import androidx.activity.ComponentActivity
import com.blockchain.coincore.AssetAction
import com.blockchain.home.presentation.navigation.AssetActionsNavigation
import org.koin.androidx.compose.get
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity

class AssetActionsNavigationImpl(private val activity: ComponentActivity?) : AssetActionsNavigation {
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
}
