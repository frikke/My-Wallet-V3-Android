package piuk.blockchain.android.ui.interest

import com.blockchain.chrome.navigation.AssetActionsNavigation
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.componentlib.utils.openUrl
import com.blockchain.earn.R
import com.blockchain.earn.activeRewards.ActiveRewardsSummaryBottomSheet
import com.blockchain.earn.dashboard.EarnAccessBlockedBottomSheet
import com.blockchain.earn.dashboard.EarnProductComparatorBottomSheet
import com.blockchain.earn.dashboard.viewmodel.EarnDashboardNavigationEvent
import com.blockchain.earn.dashboard.viewmodel.EarnType
import com.blockchain.earn.interest.InterestSummaryBottomSheet
import com.blockchain.earn.navigation.EarnNavigation
import com.blockchain.earn.staking.StakingSummaryBottomSheet
import com.blockchain.home.presentation.navigation.HomeLaunch
import com.blockchain.presentation.customviews.kyc.KycUpgradeNowSheet
import com.blockchain.presentation.sheets.NoBalanceActionBottomSheet
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity
import piuk.blockchain.android.ui.kyc.navhost.models.KycEntryPoint

class EarnNavigationImpl(
    private val activity: BlockchainActivity?,
    private val assetActionsNavigation: AssetActionsNavigation
) : EarnNavigation {
    override fun route(navigationEvent: EarnDashboardNavigationEvent) {
        when (navigationEvent) {
            is EarnDashboardNavigationEvent.OpenInterestSummarySheet -> openInterestSummarySheet(
                assetTicker = navigationEvent.assetTicker
            )

            is EarnDashboardNavigationEvent.OpenStakingSummarySheet -> openStakingSummarySheet(
                assetTicker = navigationEvent.assetTicker
            )

            is EarnDashboardNavigationEvent.OpenActiveRewardsSummarySheet -> openActiveRewardsSummarySheet(
                assetTicker = navigationEvent.assetTicker
            )

            is EarnDashboardNavigationEvent.OpenKycUpgradeNowSheet ->
                activity?.showBottomSheet(KycUpgradeNowSheet.newInstance())

            is EarnDashboardNavigationEvent.OpenBlockedForRegionSheet -> {
                activity?.let {
                    showBlockedAccessSheet(
                        title = activity.getString(
                            com.blockchain.stringResources.R.string.earn_access_blocked_region_title
                        ),
                        paragraph = activity.getString(
                            com.blockchain.stringResources.R.string.earn_access_blocked_region_paragraph,
                            when (navigationEvent.earnType) {
                                EarnType.Passive -> activity.getString(
                                    com.blockchain.stringResources.R.string.earn_rewards_label_passive
                                )

                                EarnType.Staking -> activity.getString(
                                    com.blockchain.stringResources.R.string.earn_rewards_label_staking
                                )

                                EarnType.Active -> activity.getString(
                                    com.blockchain.stringResources.R.string.earn_rewards_label_active
                                )
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

            EarnDashboardNavigationEvent.OpenKyc -> startKycClicked()

            is EarnDashboardNavigationEvent.OpenProductComparator ->
                openProductComparatorBottomSheet(earnProducts = navigationEvent.earnProducts)
        }
    }

    override fun openInterestSummarySheet(assetTicker: String) {
        activity?.showBottomSheet(InterestSummaryBottomSheet.newInstance(assetTicker))
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

    override fun openProductComparatorBottomSheet(earnProducts: Map<EarnType, Double>) {
        activity?.showBottomSheet(EarnProductComparatorBottomSheet.newInstance(earnProducts))
    }

    override fun openExternalUrl(url: String) {
        activity?.openUrl(url)
    }

    override fun showBuyUpsellSheet(account: BlockchainAccount, action: AssetAction, canBuy: Boolean) {
        activity?.showBottomSheet(
            NoBalanceActionBottomSheet.newInstance(
                account,
                action,
                canBuy
            )
        )
    }

    override fun startKycClicked() {
        if (activity != null) {
            KycNavHostActivity.startForResult(activity, KycEntryPoint.Other, HomeLaunch.KYC_STARTED)
        }
    }
}
