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
import com.blockchain.chrome.tbr.FiatActionsIntents
import com.blockchain.chrome.tbr.FiatActionsNavEvent
import com.blockchain.chrome.tbr.FiatActionsViewModel
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
import com.blockchain.tempsheetinterfaces.BankLinkingHost
import com.blockchain.tempsheetinterfaces.QuestionnaireSheetHost
import com.blockchain.tempsheetinterfaces.fiatactions.FiatActionsNavigation
import com.blockchain.tempsheetinterfaces.fiatactions.models.LinkablePaymentMethodsForAction
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.android.material.snackbar.Snackbar
import info.blockchain.balance.FiatCurrency
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.KoinScopeComponent
import org.koin.core.parameter.parametersOf
import org.koin.core.scope.Scope

class MultiAppActivity :
    BlockchainActivity(),
    InterestSummarySheet.Host,
    StakingSummaryBottomSheet.Host,
    QuestionnaireSheetHost,
    BankLinkingHost,
    KoinScopeComponent {

    override val scope: Scope = payloadScope
    private val fiatActionsViewModel: FiatActionsViewModel by viewModel()

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
                fiatActionsNavigation = fiatActionsNavigation,
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
            fiatActionsViewModel.navigationEventFlow.flowWithLifecycle(lifecycle).collectLatest {
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
        fiatActionsViewModel.onIntent(FiatActionsIntents.WireTransferAccountDetails)
    }

    override fun onLinkBankSelected(paymentMethodForAction: LinkablePaymentMethodsForAction) {
        if (paymentMethodForAction is LinkablePaymentMethodsForAction.LinkablePaymentMethodsForDeposit) {
            fiatActionsViewModel.onIntent(
                FiatActionsIntents.RestartDeposit(
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
            fiatActionsViewModel.onIntent(
                FiatActionsIntents.RestartDeposit(
                    shouldLaunchBankLinkTransfer = false
                )
            )
        }
    }

    override fun onSheetClosed() {
    }
}
