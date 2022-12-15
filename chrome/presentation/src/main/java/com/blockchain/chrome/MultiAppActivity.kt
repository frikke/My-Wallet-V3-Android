package com.blockchain.chrome

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.blockchain.analytics.events.LaunchOrigin
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
import com.blockchain.deeplinking.navigation.Destination
import com.blockchain.deeplinking.navigation.DestinationArgs
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
import com.blockchain.home.presentation.navigation.AuthNavigation
import com.blockchain.home.presentation.navigation.AuthNavigationHost
import com.blockchain.home.presentation.navigation.HomeLaunch.LAUNCH_AUTH_FLOW
import com.blockchain.home.presentation.navigation.HomeLaunch.PENDING_DESTINATION
import com.blockchain.home.presentation.navigation.SettingsDestination
import com.blockchain.home.presentation.navigation.SettingsNavigation
import com.blockchain.koin.payloadScope
import com.blockchain.koin.scopedInject
import com.blockchain.prices.navigation.PricesNavigation
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.kotlin.subscribeBy
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.core.component.KoinScopeComponent
import org.koin.core.parameter.parametersOf
import org.koin.core.scope.Scope
import timber.log.Timber

class MultiAppActivity :
    BlockchainActivity(),
    InterestSummarySheet.Host,
    StakingSummaryBottomSheet.Host,
    QuestionnaireSheetHost,
    AuthNavigationHost,
    BankLinkingHost,
    KoinScopeComponent {

    override val scope: Scope = payloadScope

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

    private val settingsNavigation: SettingsNavigation = payloadScope.get {
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

    private val authNavigation: AuthNavigation = payloadScope.get {
        parametersOf(
            this
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        // allow to draw on status and navigation bars
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val systemUiController = rememberSystemUiController()
            systemUiController.setStatusBarColor(Color.Transparent)

            MultiAppNavHost(
                assetActionsNavigation = assetActionsNavigation,
                fiatActionsNavigation = fiatActionsNavigation,
                settingsNavigation = settingsNavigation,
                pricesNavigation = pricesNavigation
            )
        }

        handleFiatActionsNav()
    }

    private fun handleIntent(intent: Intent) {
        if (intent.hasExtra(LAUNCH_AUTH_FLOW) &&
            intent.getBooleanExtra(LAUNCH_AUTH_FLOW, false)
        ) {
            intent.extras?.let {
                authNavigation.launchAuth(it)
            }
        } else if (intent.hasExtra(PENDING_DESTINATION)) {
            intent.getParcelableExtra<Destination>(PENDING_DESTINATION)?.let { destination ->
                navigateToDeeplinkDestination(destination)
            }
        }
    }

    private val destinationArgs: DestinationArgs by scopedInject()

    private fun navigateToDeeplinkDestination(destination: Destination) {
        when (destination) {
            is Destination.AssetViewDestination -> {
                destinationArgs.getAssetInfo(destination.networkTicker)?.let { assetInfo ->
                    assetActionsNavigation.coinview(
                        asset = assetInfo,
                        recurringBuyId = destination.recurringBuyId,
                        originScreen = LaunchOrigin.DEEPLINK.name
                    )
                }
            }
            is Destination.AssetBuyDestination -> {
                destinationArgs.getAssetInfo(destination.networkTicker)?.let { assetInfo ->
                    assetActionsNavigation.buyCrypto(
                        currency = assetInfo, amount = destination.amount,
                        preselectedFiatTicker = destination.fiatTicker
                    )
                } ?: run {
                    Timber.e("Unable to start SimpleBuyActivity from deeplink. AssetInfo is null")
                }
            }
            is Destination.AssetSendDestination -> {
                destinationArgs.getAssetInfo(destination.networkTicker)?.let { assetInfo ->
                    destinationArgs.getSendSourceCryptoAccount(assetInfo, destination.accountAddress).subscribeBy(
                        onSuccess = { account ->
                            transactionFlowNavigation.startTransactionFlow(
                                sourceAccount = account,
                                action = AssetAction.Send
                            )
                        },
                        onError = {
                            Timber.e(it)
                        }
                    )
                } ?: run {
                    Timber.e("Unable to start Send flow from deeplink. AssetInfo is null")
                }
            }
            is Destination.AssetEnterAmountDestination -> {
                destinationArgs.getAssetInfo(destination.networkTicker)?.let { assetInfo ->
                    assetActionsNavigation.buyCrypto(currency = assetInfo)
                } ?: run {
                    Timber.e("Unable to start SimpleBuyActivity from deeplink. AssetInfo is null")
                }
            }
            is Destination.AssetEnterAmountLinkCardDestination -> {
                destinationArgs.getAssetInfo(destination.networkTicker)?.let { assetInfo ->
                    assetActionsNavigation.buyCrypto(currency = assetInfo, launchLinkCard = true)
                } ?: run {
                    destinationArgs.getFiatAssetInfo(destination.networkTicker)?.let { _ ->
                        settingsNavigation.settings(SettingsDestination.CardLinking)
                    } ?: Timber.e(
                        "Unable to start CardLinking from deeplink. Ticker not found ${destination.networkTicker}"
                    )
                }
            }
            is Destination.AssetEnterAmountNewMethodDestination -> {
                destinationArgs.getAssetInfo(destination.networkTicker)?.let { assetInfo ->
                    assetActionsNavigation.buyCrypto(
                        currency = assetInfo,
                        launchNewPaymentMethodSelection = true
                    )
                } ?: run {
                    Timber.e(
                        "Unable to start SimpleBuyActivity from deeplink. Ticker not found ${destination.networkTicker}"
                    )
                }
            }
            is Destination.CustomerSupportDestination -> settingsNavigation.launchSupportCenter()
            is Destination.StartKycDestination -> assetActionsNavigation.startKyc()
            is Destination.AssetReceiveDestination -> assetActionsNavigation.receive(destination.networkTicker)
            Destination.SettingsAddCardDestination -> settingsNavigation.settings(SettingsDestination.CardLinking)
            Destination.SettingsAddBankDestination -> settingsNavigation.settings(SettingsDestination.BankLinking)
            Destination.DashboardDestination,
            is Destination.ActivityDestination -> {
            }
            /**
             * TODO WE NEED Viwetolaucnh integration to do those.
             */
            is Destination.AssetSellDestination -> TODO()
            is Destination.AssetSwapDestination -> TODO()
            is Destination.FiatDepositDestination -> TODO()
            Destination.ReferralDestination -> TODO()
            is Destination.RewardsDepositDestination -> TODO()
            is Destination.RewardsSummaryDestination -> TODO()
            is Destination.WalletConnectDestination -> TODO()
        }
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

    override fun navigateToBottomSheet(bottomSheet: BottomSheetDialogFragment) =
        showBottomSheet(bottomSheet)

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
