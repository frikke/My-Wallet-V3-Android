package piuk.blockchain.android.ui.home

import androidx.activity.result.contract.ActivityResultContracts
import com.blockchain.analytics.events.LaunchOrigin
import com.blockchain.chrome.MultiAppActions
import com.blockchain.chrome.navigation.AssetActionsNavigation
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.impl.CustodialInterestAccount
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.domain.paymentmethods.model.FundsLocks
import com.blockchain.earn.activeRewards.ActiveRewardsSummaryBottomSheet
import com.blockchain.earn.interest.InterestSummaryBottomSheet
import com.blockchain.earn.staking.StakingSummaryBottomSheet
import info.blockchain.balance.AssetInfo
import piuk.blockchain.android.simplebuy.SimpleBuyActivity
import piuk.blockchain.android.ui.coinview.presentation.CoinViewActivity
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity
import piuk.blockchain.android.ui.kyc.navhost.models.KycEntryPoint
import piuk.blockchain.android.ui.locks.LocksDetailsActivity
import piuk.blockchain.android.ui.settings.SettingsActivity
import piuk.blockchain.android.ui.transactionflow.flow.TransactionFlowActivity

class AssetActionsNavigationImpl(private val activity: BlockchainActivity?) : AssetActionsNavigation {

    private val actionsResultContract =
        activity?.registerForActivityResult(ActionActivity.BlockchainActivityResultContract()) {
            when (it) {
                ActionActivity.ActivityResult.StartKyc -> launchKyc()
                ActionActivity.ActivityResult.StartBuyIntro -> launchBuy()
                ActionActivity.ActivityResult.ViewActivity -> launchViewActivity()
                null -> {
                }
            }
        }

    private val activityResultCoinview = activity?.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == CoinViewActivity.RESULT_DEX) {
            (activity as? MultiAppActions)?.navigateToDex()
        }
    }

    override fun unregister() {
        actionsResultContract?.unregister()
    }

    private fun launchBuy() {
        actionsResultContract!!.launch(ActionActivity.ActivityArgs(action = AssetAction.Buy, null))
    }

    private fun launchKyc() {
        KycNavHostActivity.start(activity!!, KycEntryPoint.Other)
    }

    private fun launchViewActivity() {
        actionsResultContract!!.launch(ActionActivity.ActivityArgs(AssetAction.ViewActivity))
    }

    override fun navigate(assetAction: AssetAction) {
        return actionsResultContract!!.launch(ActionActivity.ActivityArgs(action = assetAction))
    }

    override fun buyCryptoWithRecurringBuy() {
        return actionsResultContract!!.launch(
            ActionActivity.ActivityArgs(action = AssetAction.Buy, fromRecurringBuy = true)
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

    override fun interestDeposit(source: CryptoAccount, target: CustodialInterestAccount) {
        activity!!.startActivity(
            TransactionFlowActivity.newIntent(
                context = activity,
                action = AssetAction.InterestDeposit,
                sourceAccount = source,
                target = target
            )
        )
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
        activity?.let {
            activityResultCoinview?.launch(
                CoinViewActivity.newIntent(
                    context = activity,
                    asset = asset,
                    recurringBuyId = recurringBuyId,
                    originScreen = originScreen
                )
            )
        }
    }

    override fun startKyc() {
        activity!!.startActivity(KycNavHostActivity.newIntent(activity, KycEntryPoint.Other))
    }

    override fun coinview(
        asset: AssetInfo
    ) {
        activity?.let {
            activityResultCoinview?.launch(
                CoinViewActivity.newIntent(
                    context = activity,
                    asset = asset,
                    originScreen = LaunchOrigin.HOME.name
                )
            )
        }
    }
}
