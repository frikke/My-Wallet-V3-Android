package com.blockchain.earn.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.CryptoAccount
import com.blockchain.commonarch.presentation.mvi_v2.MVIFragment
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.NavigationRouter
import com.blockchain.commonarch.presentation.mvi_v2.bindViewModel
import com.blockchain.componentlib.alert.BlockchainSnackbar
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.utils.openUrl
import com.blockchain.earn.R
import com.blockchain.earn.dashboard.viewmodel.EarnDashboardIntent
import com.blockchain.earn.dashboard.viewmodel.EarnDashboardNavigationEvent
import com.blockchain.earn.dashboard.viewmodel.EarnDashboardViewModel
import com.blockchain.earn.dashboard.viewmodel.EarnDashboardViewState
import com.blockchain.earn.interest.InterestSummarySheet
import com.blockchain.earn.staking.StakingSummaryBottomSheet
import com.blockchain.earn.staking.viewmodel.StakingError
import com.blockchain.koin.payloadScope
import com.blockchain.presentation.customviews.kyc.KycUpgradeNowSheet
import com.google.android.material.snackbar.Snackbar
import info.blockchain.balance.Currency
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.KoinScopeComponent
import org.koin.core.scope.Scope

class EarnDashboardFragment :
    MVIFragment<EarnDashboardViewState>(),
    KoinScopeComponent,
    NavigationRouter<EarnDashboardNavigationEvent>,
    InterestSummarySheet.Host,
    StakingSummaryBottomSheet.Host,
    KycUpgradeNowSheet.Host {

    interface Host {
        fun goToActivityFor(account: BlockchainAccount)
        fun goToInterestDeposit(toAccount: BlockchainAccount)
        fun goToInterestWithdraw(fromAccount: BlockchainAccount)
        fun launchStakingWithdrawal(currency: Currency)
        fun launchStakingDeposit(currency: Currency)
        fun goToStakingActivity(currency: Currency)
        fun startKycClicked()
    }

    private val host: Host by lazy {
        activity as? Host ?: error("Parent activity is not an EarnDashboardFragment.Host")
    }

    override val scope: Scope
        get() = payloadScope

    private val viewModel by viewModel<EarnDashboardViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
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
            is EarnDashboardNavigationEvent.OpenRewardsSummarySheet -> {
                openInterestSummarySheet(navigationEvent.account)
            }

            is EarnDashboardNavigationEvent.OpenStakingSummarySheet -> {
                openStakingSummarySheet(navigationEvent.assetTicker)
            }
        }

    private fun openInterestSummarySheet(account: CryptoAccount) {
        showBottomSheet(InterestSummarySheet.newInstance(account))
    }

    private fun openStakingSummarySheet(assetTicker: String) {
        showBottomSheet(StakingSummaryBottomSheet.newInstance(assetTicker))
    }

    companion object {
        fun newInstance() = EarnDashboardFragment()
    }

    override fun goToActivityFor(account: BlockchainAccount) {
        host.goToActivityFor(account)
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

    override fun launchStakingWithdrawal(currency: Currency) {
        host.launchStakingWithdrawal(currency)
    }

    override fun launchStakingDeposit(currency: Currency) {
        host.launchStakingDeposit(currency)
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

    override fun goToStakingAccountActivity(currency: Currency) {
        host.goToStakingActivity(currency)
    }

    override fun startKycClicked() {
        host.startKycClicked()
    }

    override fun onSheetClosed() {
        // do nothing
    }
}
