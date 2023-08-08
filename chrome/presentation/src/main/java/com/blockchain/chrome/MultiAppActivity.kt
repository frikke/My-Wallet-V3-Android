package com.blockchain.chrome

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.blockchain.analytics.events.LaunchOrigin
import com.blockchain.chrome.navigation.AssetActionsNavigation
import com.blockchain.chrome.navigation.DefiBackupNavigation
import com.blockchain.chrome.navigation.MultiAppNavHost
import com.blockchain.chrome.navigation.RecurringBuyNavigation
import com.blockchain.chrome.navigation.SettingsDestination
import com.blockchain.chrome.navigation.SettingsNavigation
import com.blockchain.chrome.navigation.SupportNavigation
import com.blockchain.chrome.navigation.TransactionFlowNavigation
import com.blockchain.chrome.navigation.WalletLinkAndOpenBankingNavigation
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.EarnRewardsAccount
import com.blockchain.coincore.OneTimeAccountPersistenceService
import com.blockchain.coincore.SingleAccount
import com.blockchain.coincore.TransactionTarget
import com.blockchain.coincore.impl.CustodialTradingAccount
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.commonarch.presentation.base.setContent
import com.blockchain.componentlib.alert.BlockchainSnackbar
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.navigation.ModeBackgroundColor
import com.blockchain.componentlib.utils.checkValidUrlAndOpen
import com.blockchain.componentlib.utils.openUrl
import com.blockchain.deeplinking.navigation.Destination
import com.blockchain.deeplinking.navigation.DestinationArgs
import com.blockchain.deeplinking.processor.DeepLinkResult
import com.blockchain.deeplinking.processor.DeeplinkProcessorV2
import com.blockchain.domain.auth.SecureChannelService
import com.blockchain.domain.paymentmethods.model.LINKED_BANK_ID_KEY
import com.blockchain.earn.activeRewards.ActiveRewardsSummaryBottomSheet
import com.blockchain.earn.activeRewards.viewmodel.ActiveRewardsError
import com.blockchain.earn.interest.InterestSummaryBottomSheet
import com.blockchain.earn.interest.viewmodel.InterestError
import com.blockchain.earn.navigation.EarnNavigation
import com.blockchain.earn.staking.StakingSummaryBottomSheet
import com.blockchain.earn.staking.viewmodel.StakingError
import com.blockchain.fiatActions.BankLinkingHost
import com.blockchain.fiatActions.QuestionnaireSheetHost
import com.blockchain.fiatActions.fiatactions.FiatActionsNavigation
import com.blockchain.fiatActions.fiatactions.KycBenefitsSheetHost
import com.blockchain.fiatActions.fiatactions.models.LinkablePaymentMethodsForAction
import com.blockchain.home.presentation.fiat.actions.FiatActionRequest
import com.blockchain.home.presentation.fiat.actions.FiatActionsNavEvent
import com.blockchain.home.presentation.fiat.actions.FiatActionsNavigator
import com.blockchain.home.presentation.navigation.AccountWalletLinkAlertSheetHost
import com.blockchain.home.presentation.navigation.AuthNavigation
import com.blockchain.home.presentation.navigation.AuthNavigationHost
import com.blockchain.home.presentation.navigation.HomeLaunch
import com.blockchain.home.presentation.navigation.HomeLaunch.PENDING_DESTINATION
import com.blockchain.home.presentation.navigation.QrScanNavigation
import com.blockchain.home.presentation.navigation.WCSessionIntent
import com.blockchain.koin.payloadScope
import com.blockchain.koin.scopedInject
import com.blockchain.presentation.customviews.kyc.KycUpgradeNowSheet
import com.blockchain.presentation.sheets.NoBalanceActionBottomSheet
import com.blockchain.transactions.receive.detail.ReceiveAccountDetailFragment
import com.blockchain.walletconnect.domain.WalletConnectSession
import com.blockchain.walletconnect.ui.networks.NetworkInfo
import com.blockchain.walletconnect.ui.networks.SelectNetworkBottomSheet
import com.blockchain.walletconnect.ui.sessionapproval.WCApproveSessionBottomSheet
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.kotlin.subscribeBy
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.await
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.KoinScopeComponent
import org.koin.core.parameter.parametersOf
import org.koin.core.scope.Scope
import piuk.blockchain.android.rating.presentaion.AppRatingFragment
import piuk.blockchain.android.rating.presentaion.AppRatingTriggerSource
import timber.log.Timber

class MultiAppActivity :
    BlockchainActivity(),
    InterestSummaryBottomSheet.Host,
    StakingSummaryBottomSheet.Host,
    ActiveRewardsSummaryBottomSheet.Host,
    QuestionnaireSheetHost,
    AuthNavigationHost,
    BankLinkingHost,
    AccountWalletLinkAlertSheetHost,
    WCApproveSessionBottomSheet.Host,
    KycBenefitsSheetHost,
    SelectNetworkBottomSheet.Host,
    NoBalanceActionBottomSheet.Host,
    KycUpgradeNowSheet.Host,
    MultiAppActions,
    KoinScopeComponent {

    override val statusbarColor: ModeBackgroundColor = ModeBackgroundColor.None

    override val scope: Scope = payloadScope
    private val deeplinkNavigationHandler: DeeplinkNavigationHandler by viewModel()
    private val walletModeService: WalletModeService by scopedInject()
    private val secureChannelService: SecureChannelService by scopedInject()

    private val fiatActionsNavigator: FiatActionsNavigator = payloadScope.get {
        parametersOf(lifecycleScope)
    }

    private val walletLinkAndOpenBankingNavigation: WalletLinkAndOpenBankingNavigation = payloadScope.get {
        parametersOf(this)
    }

    override val alwaysDisableScreenshots: Boolean
        get() = false

    private val defiBackupNavigation: DefiBackupNavigation = payloadScope.get {
        parametersOf(
            this
        )
    }

    private val assetActionsNavigation: AssetActionsNavigation = payloadScope.get {
        parametersOf(
            this
        )
    }

    private val recurringBuyNavigation: RecurringBuyNavigation = payloadScope.get {
        parametersOf(
            this
        )
    }

    private val deeplinkProcessor: DeeplinkProcessorV2 by scopedInject()

    private lateinit var qrScanNavigation: QrScanNavigation
    private lateinit var settingsNavigation: SettingsNavigation
    private lateinit var fiatActionsNavigation: FiatActionsNavigation
    private lateinit var supportNavigation: SupportNavigation
    private lateinit var transactionFlowNavigation: TransactionFlowNavigation
    private lateinit var authNavigation: AuthNavigation

    private val earnNavigation: EarnNavigation = payloadScope.get {
        parametersOf(
            this,
            assetActionsNavigation
        )
    }

    private var openDex: MutableState<Boolean> = mutableStateOf(false)

    private val oneTimeAccountPersistenceService: OneTimeAccountPersistenceService by scopedInject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // allow to draw on status and navigation bars
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val systemUiController = rememberSystemUiController()
            systemUiController.setStatusBarColor(Color.Transparent)

            MultiAppNavHost(
                startPhraseRecovery = ::handlePhraseRecovery,
                assetActionsNavigation = assetActionsNavigation,
                recurringBuyNavigation = recurringBuyNavigation,
                showAppRating = ::showAppRating,
                settingsNavigation = settingsNavigation,
                qrScanNavigation = qrScanNavigation,
                supportNavigation = supportNavigation,
                earnNavigation = earnNavigation,
                defiBackupNavigation = defiBackupNavigation,
                openExternalUrl = ::openExternalUrl,
                processAnnouncementUrl = ::processAnnouncementUrl,
                openDex = openDex
            )
        }

        qrScanNavigation = payloadScope.get {
            parametersOf(
                this
            )
        }

        settingsNavigation = payloadScope.get {
            parametersOf(
                this
            )
        }
        fiatActionsNavigation = payloadScope.get {
            parametersOf(
                this
            )
        }

        supportNavigation = payloadScope.get {
            parametersOf(
                this
            )
        }
        transactionFlowNavigation = payloadScope.get {
            parametersOf(
                this
            )
        }

        authNavigation = payloadScope.get {
            parametersOf(
                this
            )
        }

        handleIntent(intent)
        subscribeForSecurityChannelLogin()
        handleFiatActionsNav()
        if (savedInstanceState == null) {
            lifecycleScope.launch {
                deeplinkNavigationHandler.step.collect {
                    navigate(it)
                }
            }

            lifecycleScope.launch {
                deeplinkNavigationHandler.checkDeeplinkDestination(intent)
            }
        }
    }

    private fun processAnnouncementUrl(url: String) {
        lifecycleScope.launch {
            deeplinkProcessor.process(url.toUri())
                .onErrorReturn { DeepLinkResult.DeepLinkResultUnknownLink() }
                .await()
                .let { result ->
                    when (result) {
                        is DeepLinkResult.DeepLinkResultSuccess -> {
                            navigateToDeeplinkDestination(result.destination)
                        }

                        is DeepLinkResult.DeepLinkResultUnknownLink -> {
                            result.uri?.let { uri ->
                                checkValidUrlAndOpen(uri)
                            }
                        }
                    }
                }
        }
    }

    // //////////////////////////////////
    // app rating
    private fun showAppRating() {
        AppRatingFragment.newInstance(AppRatingTriggerSource.DASHBOARD)
            .show(supportFragmentManager, AppRatingFragment.TAG)
    }

    // ////////////////////////////////
    // defi onboarding
    private val activityResultDefiOnboarding = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            lifecycleScope.launch {
                walletModeService.updateEnabledWalletMode(WalletMode.NON_CUSTODIAL)
            }
        }
    }

    private fun handlePhraseRecovery() {
        defiBackupNavigation.startPhraseRecovery(
            launcher = activityResultDefiOnboarding
        )
    }

    // //////////////////////////////////
    // deep link
    private fun navigate(step: DeeplinkNavigationStep) {
        when (step) {
            is DeeplinkNavigationStep.AccountWalletLinkAlert ->
                walletLinkAndOpenBankingNavigation.walletLinkError(
                    step.walletIdHint
                )

            is DeeplinkNavigationStep.OpenBankingApprovalDepositComplete ->
                walletLinkAndOpenBankingNavigation.depositComplete(
                    step.amount,
                    step.estimationTime
                )

            is DeeplinkNavigationStep.OpenBankingApprovalDepositInProgress ->
                walletLinkAndOpenBankingNavigation.depositInProgress(
                    step.orderValue
                )

            is DeeplinkNavigationStep.OpenBankingApprovalTimeout ->
                walletLinkAndOpenBankingNavigation.openBankingTimeout(
                    step.currency
                )

            DeeplinkNavigationStep.OpenBankingBuyApprovalError -> walletLinkAndOpenBankingNavigation.approvalError()
            DeeplinkNavigationStep.OpenBankingError -> walletLinkAndOpenBankingNavigation.openBankingError()
            is DeeplinkNavigationStep.OpenBankingErrorWithCurrency ->
                walletLinkAndOpenBankingNavigation.openBankingError(
                    step.currency
                )

            is DeeplinkNavigationStep.OpenBankingLinking -> walletLinkAndOpenBankingNavigation.launchOpenBankingLinking(
                step.bankLinkingInfo
            )

            is DeeplinkNavigationStep.PaymentForCancelledOrder ->
                walletLinkAndOpenBankingNavigation.paymentForCancelledOrder(
                    step.currency
                )

            DeeplinkNavigationStep.SimpleBuyFromDeepLinkApproval ->
                walletLinkAndOpenBankingNavigation.launchSimpleBuyFromLinkApproval()
        }
    }

    private fun handleIntent(intent: Intent) {
        if (intent.hasExtra(PENDING_DESTINATION)) {
            intent.getParcelableExtra<Destination>(PENDING_DESTINATION)?.let { destination ->
                navigateToDeeplinkDestination(destination)
            }
        }
    }

    private fun subscribeForSecurityChannelLogin() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                secureChannelService.secureLoginAttempted.collectLatest {
                    authNavigation.launchAuth(it)
                }
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
                        currency = assetInfo,
                        amount = destination.amount,
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
                                action = AssetAction.Send,
                                origin = "NavigateToDeeplinkDestination --- ${assetInfo.networkTicker}",
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
            is Destination.AssetReceiveDestination -> {
                // todo we only have ticker here - open receiveaccountdetail
            }
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

    override fun launchInterestDeposit(account: EarnRewardsAccount.Interest) {
        transactionFlowNavigation.startTransactionFlow(
            action = AssetAction.InterestDeposit,
            target = account as TransactionTarget,
            origin = "",
        )
    }

    override fun launchInterestWithdrawal(sourceAccount: BlockchainAccount) {
        transactionFlowNavigation.startTransactionFlow(
            action = AssetAction.InterestWithdraw,
            sourceAccount = sourceAccount,
            origin = "",
        )
    }

    override fun openExternalUrl(url: String) {
        openUrl(url)
    }

    override fun showInterestLoadingError(error: InterestError) {
        BlockchainSnackbar.make(
            view = window.decorView.rootView,
            message = when (error) {
                is InterestError.UnknownAsset -> getString(
                    com.blockchain.stringResources.R.string.earn_summary_sheet_error_unknown_asset,
                    error.assetTicker
                )

                InterestError.Other -> getString(com.blockchain.stringResources.R.string.earn_summary_sheet_error_other)
                InterestError.None -> getString(com.blockchain.stringResources.R.string.empty)
            },
            duration = Snackbar.LENGTH_SHORT,
            type = SnackbarType.Error
        ).show()
    }

    override fun launchStakingWithdrawal(
        sourceAccount: BlockchainAccount,
        targetAccount: CustodialTradingAccount
    ) {
        transactionFlowNavigation.startTransactionFlow(
            action = AssetAction.StakingWithdraw,
            sourceAccount = sourceAccount,
            target = targetAccount as TransactionTarget,
            origin = "",
        )
    }

    override fun launchStakingDeposit(account: EarnRewardsAccount.Staking) {
        transactionFlowNavigation.startTransactionFlow(
            action = AssetAction.StakingDeposit,
            target = account as TransactionTarget,
            origin = "",
        )
    }

    override fun showStakingLoadingError(error: StakingError) {
        BlockchainSnackbar.make(
            view = window.decorView.rootView,
            message = when (error) {
                is StakingError.UnknownAsset -> getString(
                    com.blockchain.stringResources.R.string.earn_summary_sheet_error_unknown_asset,
                    error.assetTicker
                )

                StakingError.Other -> getString(com.blockchain.stringResources.R.string.earn_summary_sheet_error_other)
                StakingError.None -> getString(com.blockchain.stringResources.R.string.empty)
            },
            duration = Snackbar.LENGTH_SHORT,
            type = SnackbarType.Error
        ).show()
    }

    override fun launchActiveRewardsDeposit(account: EarnRewardsAccount.Active) {
        transactionFlowNavigation.startTransactionFlow(
            action = AssetAction.ActiveRewardsDeposit,
            target = account as TransactionTarget,
            origin = "",
        )
    }

    override fun launchActiveRewardsWithdrawal(
        sourceAccount: BlockchainAccount,
        targetAccount: CustodialTradingAccount
    ) {
        transactionFlowNavigation.startTransactionFlow(
            action = AssetAction.ActiveRewardsWithdraw,
            sourceAccount = sourceAccount,
            origin = "",
            target = targetAccount as TransactionTarget
        )
    }

    override fun showActiveRewardsLoadingError(error: ActiveRewardsError) {
        BlockchainSnackbar.make(
            view = window.decorView.rootView,
            message = when (error) {
                is ActiveRewardsError.UnknownAsset -> getString(
                    com.blockchain.stringResources.R.string.earn_summary_sheet_error_unknown_asset,
                    error.assetTicker
                )

                ActiveRewardsError.Other -> getString(
                    com.blockchain.stringResources.R.string.earn_summary_sheet_error_other
                )

                ActiveRewardsError.None -> getString(com.blockchain.stringResources.R.string.empty)
            },
            duration = Snackbar.LENGTH_SHORT,
            type = SnackbarType.Error
        ).show()
    }

    override fun navigateToBottomSheet(bottomSheet: BottomSheetDialogFragment) {
        clearBottomSheet()
        showBottomSheet(bottomSheet)
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
                            account = it.account,
                            target = it.target,
                            action = it.action
                        )
                    }

                    is FiatActionsNavEvent.WireTransferAccountDetails -> {
                        fiatActionsNavigation.wireTransferDetail(
                            account = it.account,
                            accountIsFunded = it.accountIsFunded
                        )
                    }

                    is FiatActionsNavEvent.BankLinkFlow -> {
                        fiatActionsNavigation.bankLinkFlow(
                            launcher = activityResultLinkBank,
                            linkBankTransfer = it.linkBankTransfer,
                            fiatAccount = it.account,
                            assetAction = it.action
                        )
                    }

                    is FiatActionsNavEvent.LinkBankWithAlias -> {
                        fiatActionsNavigation.bankLinkWithAlias(
                            launcher = activityResultLinkBankWithAlias,
                            fiatAccount = it.account
                        )
                    }

                    is FiatActionsNavEvent.KycCashBenefits -> {
                        fiatActionsNavigation.kycCashBenefits(
                            currency = it.currency
                        )
                    }

                    is FiatActionsNavEvent.Failure -> {
                        fiatActionsNavigation.failure(
                            action = it.action,
                            error = it.error
                        )
                    }
                }
            }
        }
    }

    // //////////////////////////////////
    // QuestionnaireSheetHost
    override fun questionnaireSubmittedSuccessfully() {
        fiatActionsNavigator.performAction(
            FiatActionRequest.Restart(
                shouldLaunchBankLinkTransfer = false,
                shouldSkipQuestionnaire = true
            )
        )
    }

    override fun questionnaireSkipped() {
        fiatActionsNavigator.performAction(
            FiatActionRequest.Restart(
                shouldLaunchBankLinkTransfer = false,
                shouldSkipQuestionnaire = true
            )
        )
    }

    // //////////////////////////////////
    // BankLinkingHost
    override fun onBankWireTransferSelected(currency: FiatCurrency) {
        fiatActionsNavigator.performAction(FiatActionRequest.WireTransferAccountDetails)
    }

    override fun onLinkBankSelected(paymentMethodForAction: LinkablePaymentMethodsForAction) {
        fiatActionsNavigator.performAction(
            FiatActionRequest.Restart(
                shouldLaunchBankLinkTransfer = true
            )
        )
    }

    // //////////////////////////////////
    // link bank
    private val activityResultLinkBank = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            fiatActionsNavigator.performAction(
                FiatActionRequest.Restart(
                    shouldLaunchBankLinkTransfer = false
                )
            )
        }
    }

    // //////////////////////////////////
    // link bank with alias
    companion object {
        const val ALIAS_LINK_SUCCESS = "ALIAS_LINK_SUCCESS"
    }

    private val activityResultLinkBankWithAlias = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.data?.getBooleanExtra(ALIAS_LINK_SUCCESS, false) == true) {
            fiatActionsNavigator.performAction(
                FiatActionRequest.Restart(
                    shouldLaunchBankLinkTransfer = false
                )
            )
        }
    }

    override fun onNetworkSelected(session: WalletConnectSession, networkInfo: NetworkInfo) {
        showBottomSheet(
            WCApproveSessionBottomSheet.newInstance(
                session,
                networkInfo
            )
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        assetActionsNavigation.unregister()
        qrScanNavigation.unregister()
        settingsNavigation.unregister()
    }

    override fun onSelectNetworkClicked(session: WalletConnectSession) {
        showBottomSheet(SelectNetworkBottomSheet.newInstance(session))
    }

    override fun onSessionApproved(session: WalletConnectSession) {
        qrScanNavigation.updateWalletConnectSession(WCSessionIntent.ApproveWCSession(session))
    }

    override fun onSessionRejected(session: WalletConnectSession) {
        qrScanNavigation.updateWalletConnectSession(WCSessionIntent.RejectWCSession(session))
    }

    override fun startKycClicked() {
        earnNavigation.startKycClicked()
    }

    override fun navigateToAction(action: AssetAction, selectedAccount: BlockchainAccount, assetInfo: AssetInfo) {
        when (action) {
            AssetAction.Buy -> {
                assetActionsNavigation.buyCrypto(
                    currency = assetInfo,
                    amount = null
                )
            }

            AssetAction.Receive -> {
                oneTimeAccountPersistenceService.saveAccount(selectedAccount as SingleAccount)
                showBottomSheet(ReceiveAccountDetailFragment.newInstance())
            }
            else -> throw IllegalStateException("Earn dashboard: ${intent.action} not valid for navigation")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            HomeLaunch.BANK_DEEP_LINK_SIMPLE_BUY -> {
                if (resultCode == RESULT_OK) {
                    assetActionsNavigation.buyWithPreselectedMethod(
                        paymentMethodId = data?.getStringExtra(LINKED_BANK_ID_KEY)
                    )
                }
            }

            HomeLaunch.BANK_DEEP_LINK_SETTINGS -> {
                if (resultCode == RESULT_OK) {
                    settingsNavigation.settings()
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun logout() {
        authNavigation.logout()
    }

    override fun verificationCtaClicked() {
        assetActionsNavigation.startKyc()
    }

    override fun onSheetClosed() {
    }

    override fun navigateToDex() {
        openDex.value = true
    }
}

interface MultiAppActions {
    fun navigateToDex()
}
