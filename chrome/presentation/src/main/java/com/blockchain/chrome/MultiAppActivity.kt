package com.blockchain.chrome

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
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
import com.blockchain.fiatActions.BankLinkingHost
import com.blockchain.fiatActions.QuestionnaireSheetHost
import com.blockchain.fiatActions.fiatactions.FiatActionsNavigation
import com.blockchain.fiatActions.fiatactions.models.LinkablePaymentMethodsForAction
import com.blockchain.home.presentation.fiat.actions.FiatActionRequest
import com.blockchain.home.presentation.fiat.actions.FiatActionsNavEvent
import com.blockchain.home.presentation.fiat.actions.FiatActionsNavigator
import com.blockchain.home.presentation.navigation.AssetActionsNavigation
import com.blockchain.koin.payloadScope
import com.blockchain.prices.navigation.PricesNavigation
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.android.material.snackbar.Snackbar
import info.blockchain.balance.FiatCurrency
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf

class MultiAppActivity :
    BlockchainActivity(),
    InterestSummarySheet.Host,
    StakingSummaryBottomSheet.Host,
    QuestionnaireSheetHost,
    BankLinkingHost {

    private val fiatActionsNavigator: FiatActionsNavigator = payloadScope.get {
        parametersOf(lifecycleScope)
    }

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // allow to draw on status and navigation bars
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val systemUiController = rememberSystemUiController()
            systemUiController.setStatusBarColor(Color.Transparent)

            MultiAppNavHost(
                assetActionsNavigation = assetActionsNavigation,
                pricesNavigation = pricesNavigation
            )
        }

        handleFiatActionsNav()
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

    private fun handleFiatActionsNav() {
        lifecycleScope.launch {
            fiatActionsNavigator.navigator.flowWithLifecycle(lifecycle).collectLatest {
                when (it) {
                    is FiatActionsNavEvent.BlockedDueToSanctions -> {
                        fiatActionsNavigation.blockedDueToSanctions(
                            reason = it.reason
                        )
                    }
                    is FiatActionsNavEvent.DepositQuestionnaire -> {
                        fiatActionsNavigation.depositQuestionnaire(
                            questionnaire = it.questionnaire
                        )
                    }
                    is FiatActionsNavEvent.LinkBankMethod -> {
                        fiatActionsNavigation.linkBankMethod(
                            paymentMethodsForAction = it.paymentMethodsForAction
                        )
                    }
                    is FiatActionsNavEvent.TransactionFlow -> {
                        fiatActionsNavigation.transactionFlow(
                            sourceAccount = it.sourceAccount,
                            target = it.target,
                            action = it.action
                        )
                    }
                    is FiatActionsNavEvent.WireTransferAccountDetails -> {
                        fiatActionsNavigation.wireTransferDetail(
                            account = it.account
                        )
                    }
                    is FiatActionsNavEvent.BankLinkFlow -> {
                        fiatActionsNavigation.bankLinkFlow(
                            launcher = activityResultLinkBank,
                            linkBankTransfer = it.linkBankTransfer,
                            fiatAccount = it.fiatAccount,
                            assetAction = it.assetAction
                        )
                    }
                }
            }
        }
    }

    // //////////////////////////////////
    // QuestionnaireSheetHost
    override fun questionnaireSubmittedSuccessfully() {
        println("--------- questionnaireSubmittedSuccessfully")
    }

    override fun questionnaireSkipped() {
        println("--------- questionnaireSkipped")
    }

    // //////////////////////////////////
    // BankLinkingHost
    override fun onBankWireTransferSelected(currency: FiatCurrency) {
        fiatActionsNavigator.performAction(FiatActionRequest.WireTransferAccountDetails)
    }

    override fun onLinkBankSelected(paymentMethodForAction: LinkablePaymentMethodsForAction) {
        if (paymentMethodForAction is LinkablePaymentMethodsForAction.LinkablePaymentMethodsForDeposit) {
            fiatActionsNavigator.performAction(
                FiatActionRequest.RestartDeposit(
                    action = AssetAction.FiatDeposit,
                    shouldLaunchBankLinkTransfer = false
                )
            )
        } else if (paymentMethodForAction is LinkablePaymentMethodsForAction.LinkablePaymentMethodsForWithdraw) {
            //                    model.process(DashboardIntent.LaunchBankTransferFlow(it, AssetAction.FiatWithdraw, true))
        }
    }

    // //////////////////////////////////
    // link bank
    private val activityResultLinkBank = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            fiatActionsNavigator.performAction(
                FiatActionRequest.RestartDeposit(
                    shouldLaunchBankLinkTransfer = false
                )
            )
        }
    }

    override fun onSheetClosed() {
    }
}
