package com.blockchain.chrome

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.blockchain.chrome.navigation.MultiAppNavHost
import com.blockchain.chrome.navigation.TransactionFlowNavigation
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.StakingAccount
import com.blockchain.coincore.TransactionTarget
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.componentlib.alert.BlockchainSnackbar
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.utils.openUrl
import com.blockchain.earn.interest.InterestSummarySheet
import com.blockchain.earn.staking.StakingSummaryBottomSheet
import com.blockchain.earn.staking.viewmodel.StakingError
import com.blockchain.home.presentation.navigation.AssetActionsNavigation
import com.blockchain.koin.payloadScope
import com.blockchain.prices.navigation.PricesNavigation
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.android.material.snackbar.Snackbar
import org.koin.core.parameter.parametersOf

class MultiAppActivity : BlockchainActivity(), InterestSummarySheet.Host, StakingSummaryBottomSheet.Host {
    override val alwaysDisableScreenshots: Boolean
        get() = false

    private val pricesNavigation: PricesNavigation = payloadScope.get {
        parametersOf(
            this
        )
    }

    private val assetActionsNavigation: AssetActionsNavigation = payloadScope.get {
        parametersOf(
            this
        )
    }

    private val transactionFlowNavigation: TransactionFlowNavigation = payloadScope.get {
        parametersOf(
            this
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // allow to draw on status and navigation bars
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val systemUiController = rememberSystemUiController()
            systemUiController.setStatusBarColor(Color.Transparent)

            MultiAppNavHost(
                navController = rememberNavController(),
                assetActionsNavigation = assetActionsNavigation,
                pricesNavigation = pricesNavigation
            )
        }
    }

    companion object {
        fun newIntent(
            context: Context,
        ): Intent =
            Intent(context, MultiAppActivity::class.java)
    }

    override fun goToInterestDeposit(toAccount: BlockchainAccount) {
        transactionFlowNavigation.startTransactionFlow(
            action = AssetAction.InterestDeposit,
            target = toAccount as TransactionTarget
        )
    }

    override fun goToInterestWithdraw(fromAccount: BlockchainAccount) {
        transactionFlowNavigation.startTransactionFlow(
            action = AssetAction.InterestWithdraw,
            sourceAccount = fromAccount
        )
    }

    override fun openExternalUrl(url: String) {
        openUrl(url)
    }

    override fun launchStakingWithdrawal(account: StakingAccount) {
    }

    override fun launchStakingDeposit(account: StakingAccount) {
        transactionFlowNavigation.startTransactionFlow(
            action = AssetAction.StakingDeposit,
            target = account as TransactionTarget
        )
    }

    override fun showStakingLoadingError(error: StakingError) {
        BlockchainSnackbar.make(
            view = window.decorView.rootView,
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

    override fun goToStakingAccountActivity(account: StakingAccount) {
        // Do nothing not supported
    }

    override fun onSheetClosed() {
    }
}
