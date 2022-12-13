package com.blockchain.chrome

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.blockchain.chrome.navigation.MultiAppNavHost
import com.blockchain.chrome.navigation.TransactionFlowNavigation
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.FiatAccount
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
import com.blockchain.tempsheetinterfaces.BankLinkingHost
import com.blockchain.tempsheetinterfaces.QuestionnaireSheetHost
import com.blockchain.tempsheetinterfaces.fiatactions.FiatActionsNavigation
import com.blockchain.tempsheetinterfaces.fiatactions.FiatActionsUseCase
import com.blockchain.tempsheetinterfaces.fiatactions.models.FiatActionsResult
import com.blockchain.tempsheetinterfaces.fiatactions.models.LinkablePaymentMethodsForAction
import com.blockchain.utils.filterList
import com.blockchain.walletmode.WalletMode
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.android.material.snackbar.Snackbar
import info.blockchain.balance.FiatCurrency
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf

class MultiAppActivity : BlockchainActivity(),
    InterestSummarySheet.Host,
    StakingSummaryBottomSheet.Host,
    QuestionnaireSheetHost,
    BankLinkingHost {
    override val alwaysDisableScreenshots: Boolean
        get() = false

    private val fiatActionsUseCase: FiatActionsUseCase = payloadScope.get()

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

    private val fiatActionsNavigation: FiatActionsNavigation = payloadScope.get {
        parametersOf(
            this
        )
    }

    private val transactionFlowNavigation: TransactionFlowNavigation = payloadScope.get {
        parametersOf(
            this
        )
    }

    private val coincore: Coincore = payloadScope.get()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // allow to draw on status and navigation bars
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val systemUiController = rememberSystemUiController()
            systemUiController.setStatusBarColor(Color.Transparent)

            MultiAppNavHost(
                assetActionsNavigation = assetActionsNavigation,
                fiatActionsNavigation = fiatActionsNavigation,
                pricesNavigation = pricesNavigation
            )
        }

        handleFiatActionResult()
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

    private fun handleFiatActionResult() {
        lifecycleScope.launch {
            fiatActionsUseCase.result.collectLatest {
                when (it) {
                    is FiatActionsResult.BlockedDueToSanctions -> {
                        fiatActionsNavigation.blockedDueToSanctions(
                            reason = it.reason
                        )
                    }
                    is FiatActionsResult.DepositQuestionnaire -> {
                        fiatActionsNavigation.depositQuestionnaire(
                            questionnaire = it.questionnaire
                        )
                    }
                    is FiatActionsResult.LinkBankMethod -> {
                        fiatActionsNavigation.linkBankMethod(
                            paymentMethodsForAction = it.paymentMethodsForAction
                        )
                    }
                    is FiatActionsResult.TransactionFlow -> {
                        fiatActionsNavigation.transactionFlow(
                            sourceAccount = it.sourceAccount,
                            target = it.target,
                            action = it.action
                        )
                    }
                    is FiatActionsResult.WireTransferAccountDetails -> {
                        fiatActionsNavigation.wireTransferDetail(
                            account = it.account
                        )
                    }
                }
            }
        }
    }

    ////////////////////////////////////
    // QuestionnaireSheetHost
    override fun questionnaireSubmittedSuccessfully() {
        println("--------- questionnaireSubmittedSuccessfully")
    }

    override fun questionnaireSkipped() {
        println("--------- questionnaireSkipped")
    }

    ////////////////////////////////////
    // BankLinkingHost
    override fun onBankWireTransferSelected(currency: FiatCurrency) {
        lifecycleScope.launch {
            val account = coincore.activeWalletsInMode(WalletMode.CUSTODIAL_ONLY).map { it.accounts }
                .map { it.filterIsInstance<FiatAccount>() }
                .filterList { it.currency.networkTicker == currency.networkTicker }
                .firstOrNull()?.firstOrNull()

            account?.let {
                fiatActionsNavigation.wireTransferDetail(account)
            }
        }
    }

    override fun onLinkBankSelected(paymentMethodForAction: LinkablePaymentMethodsForAction) {
        lifecycleScope.launch {
            val account = coincore.activeWalletsInMode(WalletMode.CUSTODIAL_ONLY).map { it.accounts }
                .map { it.filterIsInstance<FiatAccount>() }
                .filterList {
                    it.currency.networkTicker == paymentMethodForAction.linkablePaymentMethods.currency.networkTicker
                }
                .firstOrNull()?.firstOrNull()

            account?.let {
                if (paymentMethodForAction is LinkablePaymentMethodsForAction.LinkablePaymentMethodsForDeposit) {
                    fiatActionsUseCase.deposit(
                        account = it,
                        action = AssetAction.FiatDeposit,
                        shouldLaunchBankLinkTransfer = false
                    )
                } else if (paymentMethodForAction is LinkablePaymentMethodsForAction.LinkablePaymentMethodsForWithdraw) {
                    //                    model.process(DashboardIntent.LaunchBankTransferFlow(it, AssetAction.FiatWithdraw, true))
                }
            }
        }
    }
    ////////////////////////////////////

    override fun onSheetClosed() {
    }
}
