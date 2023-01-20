package com.blockchain.earn.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.StakingAccount
import com.blockchain.commonarch.presentation.mvi_v2.MVIFragment
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.NavigationRouter
import com.blockchain.commonarch.presentation.mvi_v2.bindViewModel
import com.blockchain.componentlib.alert.BlockchainSnackbar
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.utils.openUrl
import com.blockchain.domain.common.model.BuySellViewType
import com.blockchain.earn.R
import com.blockchain.earn.dashboard.viewmodel.EarnDashboardIntent
import com.blockchain.earn.dashboard.viewmodel.EarnDashboardNavigationEvent
import com.blockchain.earn.dashboard.viewmodel.EarnDashboardViewModel
import com.blockchain.earn.dashboard.viewmodel.EarnDashboardViewState
import com.blockchain.earn.dashboard.viewmodel.EarnType
import com.blockchain.earn.interest.InterestSummarySheet
import com.blockchain.earn.staking.StakingSummaryBottomSheet
import com.blockchain.earn.staking.viewmodel.StakingError
import com.blockchain.koin.payloadScope
import com.blockchain.presentation.sheets.NoBalanceActionBottomSheet
import com.google.android.material.snackbar.Snackbar
import info.blockchain.balance.AssetInfo
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.KoinScopeComponent
import org.koin.core.scope.Scope

class EarnDashboardFragment :
    MVIFragment<EarnDashboardViewState>(),
    KoinScopeComponent,
    NavigationRouter<EarnDashboardNavigationEvent>,
    InterestSummarySheet.Host,
    StakingSummaryBottomSheet.Host,
    NoBalanceActionBottomSheet.Host {

    interface Host {
        fun goToInterestDeposit(toAccount: BlockchainAccount)
        fun goToInterestWithdraw(fromAccount: BlockchainAccount)
        fun launchStakingWithdrawal(account: StakingAccount)
        fun launchStakingDeposit(account: StakingAccount)
        fun startKycClicked()
        fun launchReceive(cryptoTicker: String?)
        fun launchBuySell(viewType: BuySellViewType, asset: AssetInfo?, reload: Boolean)
    }

    private val host: Host by lazy {
        activity as? Host ?: error("Parent activity is not an EarnDashboardFragment.Host")
    }

    override val scope: Scope
        get() = payloadScope

    private val viewModel by viewModel<EarnDashboardViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                bindViewModel(viewModel, this@EarnDashboardFragment, ModelConfigArgs.NoArgs)

                EarnDashboardScreen(viewModel, childFragmentManager)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onIntent(EarnDashboardIntent.LoadSilently)
    }

    override fun onStateUpdated(state: EarnDashboardViewState) {
    }

    override fun route(navigationEvent: EarnDashboardNavigationEvent) =
        when (navigationEvent) {
            is EarnDashboardNavigationEvent.OpenRewardsSummarySheet -> openInterestSummarySheet(
                account = navigationEvent.account
            )
            is EarnDashboardNavigationEvent.OpenStakingSummarySheet -> openStakingSummarySheet(
                assetTicker = navigationEvent.assetTicker
            )
            is EarnDashboardNavigationEvent.OpenBlockedForRegionSheet -> showBlockedAccessSheet(
                title = getString(R.string.earn_access_blocked_region_title),
                paragraph = getString(
                    R.string.earn_access_blocked_region_paragraph,
                    when (navigationEvent.earnType) {
                        EarnType.Passive -> getString(R.string.earn_rewards_label_passive)
                        EarnType.Staking -> getString(R.string.earn_rewards_label_staking)
                    }
                )
            )
            is EarnDashboardNavigationEvent.OpenUrl -> openExternalUrl(url = navigationEvent.url)
            is EarnDashboardNavigationEvent.OpenBuyOrReceiveSheet -> showBuyUpsellSheet(
                account = navigationEvent.account,
                action = navigationEvent.assetAction,
                canBuy = navigationEvent.availableToBuy
            )
            is EarnDashboardNavigationEvent.OpenBuy -> host.launchBuySell(
                BuySellViewType.TYPE_BUY, navigationEvent.assetInfo, false
            )
            is EarnDashboardNavigationEvent.OpenReceive -> host.launchReceive(navigationEvent.networkTicker)
            EarnDashboardNavigationEvent.OpenKyc -> host.startKycClicked()
        }

    private fun showBuyUpsellSheet(account: BlockchainAccount, action: AssetAction, canBuy: Boolean) {
        showBottomSheet(
            NoBalanceActionBottomSheet.newInstance(
                account, action, canBuy
            )
        )
    }

    private fun showBlockedAccessSheet(title: String, paragraph: String) {
        showBottomSheet(EarnAccessBlockedBottomSheet.newInstance(title, paragraph))
    }

    private fun openInterestSummarySheet(account: CryptoAccount) {
        showBottomSheet(InterestSummarySheet.newInstance(account))
    }

    private fun openStakingSummarySheet(assetTicker: String) {
        showBottomSheet(StakingSummaryBottomSheet.newInstance(assetTicker))
    }

    override fun navigateToAction(action: AssetAction, selectedAccount: BlockchainAccount, assetInfo: AssetInfo) {
        viewModel.onIntent(
            EarnDashboardIntent.OnNavigateToAction(action, assetInfo)
        )
    }

    override fun goToInterestDeposit(toAccount: BlockchainAccount) {
        host.goToInterestDeposit(toAccount)
    }

    override fun goToInterestWithdraw(fromAccount: BlockchainAccount) {
        host.goToInterestWithdraw(fromAccount)
    }

    override fun openExternalUrl(url: String) {
        requireContext().openUrl(url)
    }

    override fun launchStakingWithdrawal(account: StakingAccount) {
        host.launchStakingWithdrawal(account)
    }

    override fun launchStakingDeposit(account: StakingAccount) {
        host.launchStakingDeposit(account)
    }

    override fun showStakingLoadingError(error: StakingError) {
        view?.let {
            BlockchainSnackbar.make(
                view = it,
                message = when (error) {
                    is StakingError.UnknownAsset -> getString(
                        R.string.staking_summary_sheet_error_unknown_asset, error.assetTicker
                    )
                    StakingError.Other -> getString(R.string.staking_summary_sheet_error_other)
                    StakingError.None -> getString(R.string.empty)
                },
                duration = Snackbar.LENGTH_SHORT,
                type = SnackbarType.Error
            ).show()
        }
    }

    override fun onSheetClosed() {
        // do nothing
    }

    companion object {
        fun newInstance() = EarnDashboardFragment()
    }
}
