package piuk.blockchain.android.ui.interest

import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.CryptoAccount
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.componentlib.utils.openUrl
import com.blockchain.domain.common.model.BuySellViewType
import com.blockchain.earn.R
import com.blockchain.earn.activeRewards.ActiveRewardsSummaryBottomSheet
import com.blockchain.earn.dashboard.EarnAccessBlockedBottomSheet
import com.blockchain.earn.dashboard.viewmodel.EarnDashboardNavigationEvent
import com.blockchain.earn.dashboard.viewmodel.EarnType
import com.blockchain.earn.interest.InterestSummarySheet
import com.blockchain.earn.navigation.EarnNavigation
import com.blockchain.earn.staking.StakingSummaryBottomSheet
import com.blockchain.home.presentation.navigation.AssetActionsNavigation
import com.blockchain.home.presentation.navigation.HomeLaunch
import com.blockchain.presentation.sheets.NoBalanceActionBottomSheet
import info.blockchain.balance.AssetInfo
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity

class EarnNavigationImpl(
    private val activity: BlockchainActivity?,
    private val assetActionsNavigation: AssetActionsNavigation
) : EarnNavigation {
    override fun route(navigationEvent: EarnDashboardNavigationEvent) {
        when (navigationEvent) {
            is EarnDashboardNavigationEvent.OpenRewardsSummarySheet -> openInterestSummarySheet(
                account = navigationEvent.account
            )
            is EarnDashboardNavigationEvent.OpenStakingSummarySheet -> openStakingSummarySheet(
                assetTicker = navigationEvent.assetTicker
            )
            is EarnDashboardNavigationEvent.OpenActiveRewardsSummarySheet -> openActiveRewardsSummarySheet(
                assetTicker = navigationEvent.assetTicker
            )
            is EarnDashboardNavigationEvent.OpenBlockedForRegionSheet -> {
                activity?.let {
                    showBlockedAccessSheet(
                        title = activity.getString(R.string.earn_access_blocked_region_title),
                        paragraph = activity.getString(
                            R.string.earn_access_blocked_region_paragraph,
                            when (navigationEvent.earnType) {
                                EarnType.Passive -> activity.getString(R.string.earn_rewards_label_passive)
                                EarnType.Staking -> activity.getString(R.string.earn_rewards_label_staking)
                                EarnType.Active -> activity.getString(R.string.earn_rewards_label_active)
                            }
                        )
                    )
                }
            }
            is EarnDashboardNavigationEvent.OpenUrl -> openExternalUrl(url = navigationEvent.url)
            is EarnDashboardNavigationEvent.OpenBuyOrReceiveSheet -> showBuyUpsellSheet(
                account = navigationEvent.account,
                action = navigationEvent.assetAction,
                canBuy = navigationEvent.availableToBuy
            )
            is EarnDashboardNavigationEvent.OpenBuy -> launchBuySell(
                BuySellViewType.TYPE_BUY, navigationEvent.assetInfo, false
            )
            is EarnDashboardNavigationEvent.OpenReceive -> launchReceive(navigationEvent.networkTicker)
            EarnDashboardNavigationEvent.OpenKyc -> startKycClicked()
        }
    }

    override fun openInterestSummarySheet(account: CryptoAccount) {
        activity?.showBottomSheet(InterestSummarySheet.newInstance(account))
    }

    override fun openStakingSummarySheet(assetTicker: String) {
        activity?.showBottomSheet(StakingSummaryBottomSheet.newInstance(assetTicker))
    }

    override fun openActiveRewardsSummarySheet(assetTicker: String) {
        activity?.showBottomSheet(ActiveRewardsSummaryBottomSheet.newInstance(assetTicker))
    }

    override fun showBlockedAccessSheet(title: String, paragraph: String) {
        activity?.showBottomSheet(EarnAccessBlockedBottomSheet.newInstance(title, paragraph))
    }

    override fun openExternalUrl(url: String) {
        activity?.openUrl(url)
    }

    override fun showBuyUpsellSheet(account: BlockchainAccount, action: AssetAction, canBuy: Boolean) {
        activity?.showBottomSheet(
            NoBalanceActionBottomSheet.newInstance(
                account, action, canBuy
            )
        )
    }

    override fun launchBuySell(viewType: BuySellViewType, asset: AssetInfo?, reload: Boolean) {
        assetActionsNavigation.buyCrypto(
            currency = asset!!,
            amount = null
        )
    }

    override fun launchReceive(cryptoTicker: String) {
        assetActionsNavigation.receive(cryptoTicker)
    }

    override fun startKycClicked() {
        if (activity != null) {
            KycNavHostActivity.startForResult(activity, CampaignType.None, HomeLaunch.KYC_STARTED)
        }
    }
}
