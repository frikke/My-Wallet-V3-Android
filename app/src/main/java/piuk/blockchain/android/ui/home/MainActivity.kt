package piuk.blockchain.android.ui.home

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ShortcutManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
import com.blockchain.analytics.NotificationAppOpened
import com.blockchain.analytics.events.AnalyticsEvents
import com.blockchain.analytics.events.LaunchOrigin
import com.blockchain.analytics.events.SendAnalytics
import com.blockchain.analytics.events.activityShown
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.CryptoTarget
import com.blockchain.coincore.NullCryptoAccount
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import com.blockchain.commonarch.presentation.mvi.MviActivity
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.databinding.ToolbarGeneralBinding
import com.blockchain.componentlib.navigation.BottomNavigationState
import com.blockchain.componentlib.navigation.NavigationBarButton
import com.blockchain.componentlib.navigation.NavigationItem
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.deeplinking.navigation.Destination
import com.blockchain.deeplinking.navigation.DestinationArgs
import com.blockchain.extensions.exhaustive
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.koin.deeplinkingFeatureFlag
import com.blockchain.koin.scopedInject
import com.blockchain.notifications.analytics.NotificationAnalyticsEvents
import com.blockchain.preferences.DashboardPrefs
import com.blockchain.walletconnect.domain.WalletConnectAnalytics
import com.blockchain.walletconnect.domain.WalletConnectSession
import com.blockchain.walletconnect.ui.sessionapproval.WCApproveSessionBottomSheet
import com.blockchain.walletconnect.ui.sessionapproval.WCSessionUpdatedBottomSheet
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import java.io.Serializable
import java.net.URLDecoder
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.databinding.ActivityMainBinding
import piuk.blockchain.android.databinding.DialogEntitySwitchSilverBinding
import piuk.blockchain.android.scan.QrScanError
import piuk.blockchain.android.scan.QrScanResultProcessor
import piuk.blockchain.android.simplebuy.SimpleBuyActivity
import piuk.blockchain.android.simplebuy.SmallSimpleBuyNavigator
import piuk.blockchain.android.simplebuy.sheets.BuyPendingOrdersBottomSheet
import piuk.blockchain.android.ui.activity.ActivitiesFragment
import piuk.blockchain.android.ui.addresses.AddressesActivity
import piuk.blockchain.android.ui.airdrops.AirdropCentreActivity
import piuk.blockchain.android.ui.auth.AccountWalletLinkAlertSheet
import piuk.blockchain.android.ui.auth.newlogin.AuthNewLoginSheet
import piuk.blockchain.android.ui.backup.BackupWalletActivity
import piuk.blockchain.android.ui.base.showFragment
import piuk.blockchain.android.ui.customviews.BlockchainSnackbar
import piuk.blockchain.android.ui.dashboard.PortfolioFragment
import piuk.blockchain.android.ui.dashboard.PricesFragment
import piuk.blockchain.android.ui.dashboard.coinview.CoinViewActivity
import piuk.blockchain.android.ui.dashboard.sheets.KycUpgradeNowSheet
import piuk.blockchain.android.ui.home.analytics.EntitySwitchSilverKycUpsellCtaClicked
import piuk.blockchain.android.ui.home.analytics.EntitySwitchSilverKycUpsellDismissed
import piuk.blockchain.android.ui.home.analytics.EntitySwitchSilverKycUpsellViewed
import piuk.blockchain.android.ui.home.models.MainIntent
import piuk.blockchain.android.ui.home.models.MainModel
import piuk.blockchain.android.ui.home.models.MainState
import piuk.blockchain.android.ui.home.models.ViewToLaunch
import piuk.blockchain.android.ui.home.ui_tour.UiTourView
import piuk.blockchain.android.ui.interest.InterestDashboardActivity
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity
import piuk.blockchain.android.ui.kyc.status.KycStatusActivity
import piuk.blockchain.android.ui.linkbank.BankAuthActivity
import piuk.blockchain.android.ui.linkbank.BankAuthSource
import piuk.blockchain.android.ui.linkbank.BankLinkingInfo
import piuk.blockchain.android.ui.linkbank.FiatTransactionState
import piuk.blockchain.android.ui.linkbank.yapily.FiatTransactionBottomSheet
import piuk.blockchain.android.ui.onboarding.OnboardingActivity
import piuk.blockchain.android.ui.scan.CameraAnalytics
import piuk.blockchain.android.ui.scan.QrExpected
import piuk.blockchain.android.ui.scan.QrScanActivity
import piuk.blockchain.android.ui.scan.QrScanActivity.Companion.getRawScanData
import piuk.blockchain.android.ui.scan.ScanAndConnectBottomSheet
import piuk.blockchain.android.ui.sell.BuySellFragment
import piuk.blockchain.android.ui.settings.v2.SettingsActivity
import piuk.blockchain.android.ui.thepit.ExchangeConnectionSheet
import piuk.blockchain.android.ui.thepit.PitPermissionsActivity
import piuk.blockchain.android.ui.transactionflow.analytics.InterestAnalytics
import piuk.blockchain.android.ui.transactionflow.flow.TransactionFlowActivity
import piuk.blockchain.android.ui.transfer.receive.detail.ReceiveDetailSheet
import piuk.blockchain.android.ui.upsell.KycUpgradePromptManager
import piuk.blockchain.android.util.AndroidUtils
import piuk.blockchain.android.util.getAccount
import timber.log.Timber

class MainActivity :
    MviActivity<MainModel, MainIntent, MainState, ActivityMainBinding>(),
    HomeNavigator,
    SlidingModalBottomDialog.Host,
    AuthNewLoginSheet.Host,
    AccountWalletLinkAlertSheet.Host,
    WCApproveSessionBottomSheet.Host,
    RedesignActionsBottomSheet.Host,
    SmallSimpleBuyNavigator,
    BuyPendingOrdersBottomSheet.Host,
    ScanAndConnectBottomSheet.Host,
    UiTourView.Host,
    KycUpgradeNowSheet.Host {

    override val alwaysDisableScreenshots: Boolean
        get() = false

    override val model: MainModel by scopedInject()

    override fun initBinding(): ActivityMainBinding = ActivityMainBinding.inflate(layoutInflater)

    override val toolbarBinding: ToolbarGeneralBinding
        get() = binding.mainToolbar

    private val dashboardPrefs: DashboardPrefs by scopedInject()

    @Deprecated("Use MVI loop instead")
    private val compositeDisposable = CompositeDisposable()

    @Deprecated("Use MVI loop instead")
    private val qrProcessor: QrScanResultProcessor by scopedInject()

    private val deeplinkingV2FF: FeatureFlag by scopedInject(deeplinkingFeatureFlag)

    private val destinationArgs: DestinationArgs by scopedInject()

    private val settingsResultContract = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            (
                it.data?.getSerializableExtra(SettingsActivity.SETTINGS_RESULT_DATA)
                    as? SettingsActivity.Companion.SettingsAction
                )?.let { action ->
                startSettingsAction(action)
            }
        }
    }

    private val actionsResultContract = registerForActivityResult(ActionActivity.BlockchainActivityResultContract()) {
        when (it) {
            ActionActivity.ActivityResult.StartKyc -> launchKyc(CampaignType.None)
            ActionActivity.ActivityResult.StartReceive -> launchReceive()
            ActionActivity.ActivityResult.StartBuyIntro -> launchBuySell(BuySellFragment.BuySellViewType.TYPE_BUY)
            null -> {
            }
        }
    }

    private val activityResultsContract = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            (it.data?.getAccount(CoinViewActivity.ACCOUNT_FOR_ACTIVITY))?.let { account ->
                startActivitiesFragment(account)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        launchPortfolio()

        setupToolbar()
        setupNavigation()

        if (intent.hasExtra(INTENT_FROM_NOTIFICATION) &&
            intent.getBooleanExtra(INTENT_FROM_NOTIFICATION, false)
        ) {
            analytics.logEvent(NotificationAppOpened)
        }

        if (intent.hasExtra(INTENT_FROM_NOTIFICATION_ANALYTICS_PAYLOAD)) {
            val analyticsPayload = intent.getSerializableExtra(INTENT_FROM_NOTIFICATION_ANALYTICS_PAYLOAD)
            analytics.logEvent(NotificationAnalyticsEvents.PushNotificationTapped(analyticsPayload))
        }

        if (savedInstanceState == null) {
            deeplinkingV2FF.enabled.subscribeBy(
                onSuccess = { isEnabled ->
                    if (isEnabled) {
                        model.process(MainIntent.SaveDeeplinkIntent(intent))
                    } else {
                        model.process(MainIntent.CheckForPendingLinks(intent))
                    }
                }
            )
        }

        val startUiTour = intent.getBooleanExtra(START_UI_TOUR_KEY, false)
        intent.removeExtra(START_UI_TOUR_KEY)

        if (intent.hasExtra(SHOW_SWAP) &&
            intent.getBooleanExtra(SHOW_SWAP, false)
        ) {
            launchSwap()
        } else if (intent.hasExtra(LAUNCH_AUTH_FLOW) &&
            intent.getBooleanExtra(LAUNCH_AUTH_FLOW, false)
        ) {
            intent.extras?.let {
                showBottomSheet(
                    AuthNewLoginSheet.newInstance(
                        pubKeyHash = it.getString(AuthNewLoginSheet.PUB_KEY_HASH),
                        messageInJson = it.getString(AuthNewLoginSheet.MESSAGE),
                        forcePin = it.getBoolean(AuthNewLoginSheet.FORCE_PIN),
                        originIP = it.getString(AuthNewLoginSheet.ORIGIN_IP),
                        originLocation = it.getString(AuthNewLoginSheet.ORIGIN_LOCATION),
                        originBrowser = it.getString(AuthNewLoginSheet.ORIGIN_BROWSER)
                    )
                )
            }
        } else if (intent.hasExtra(PENDING_DESTINATION)) {
            intent.getParcelableExtra<Destination>(PENDING_DESTINATION)?.let { destination ->
                navigateToDeeplinkDestination(destination)
            }
        }
        model.process(MainIntent.CheckForInitialDialogs(startUiTour))
        model.process(MainIntent.PerformInitialChecks)
    }

    override fun onResume() {
        super.onResume()
        model.process(MainIntent.CancelAnyPendingConfirmationBuy)
    }

    override fun onDestroy() {
        compositeDisposable.clear()
        // stopgap to be able to clear separate calls on Rx on the model
        model.clearDisposables()
        // TODO consider invoking the DataWiper here
        super.onDestroy()
    }

    private fun startSettingsAction(action: SettingsActivity.Companion.SettingsAction) {
        when (action) {
            SettingsActivity.Companion.SettingsAction.Addresses ->
                startActivityForResult(AddressesActivity.newIntent(this), ACCOUNT_EDIT)
            SettingsActivity.Companion.SettingsAction.Exchange ->
                model.process(MainIntent.LaunchExchange)
            SettingsActivity.Companion.SettingsAction.Airdrops ->
                startActivity(AirdropCentreActivity.newIntent(this))
            SettingsActivity.Companion.SettingsAction.WebLogin ->
                QrScanActivity.start(this, QrExpected.MAIN_ACTIVITY_QR)
            SettingsActivity.Companion.SettingsAction.Logout -> showLogoutDialog()
        }.also {
            hideLoading()
        }
    }

    private fun setupToolbar() {
        updateToolbarMenuItems(
            listOf(
                NavigationBarButton.Icon(R.drawable.ic_qr_scan) {
                    tryToLaunchQrScan()
                    analytics.logEvent(CameraAnalytics.QrCodeClicked())
                },
                NavigationBarButton.Icon(R.drawable.ic_bank_user) {
                    showLoading()
                    settingsResultContract.launch(SettingsActivity.newIntent(this))
                }
            )
        )
    }

    private fun tryToLaunchQrScan() {
        if (!isCameraPermissionGranted()) {
            showScanAndConnectBottomSheet()
        } else {
            launchQrScan()
        }
        analytics.logEvent(SendAnalytics.QRButtonClicked)
    }

    private fun isCameraPermissionGranted(): Boolean {
        return (
            checkSelfPermission(Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
            ).also {
            analytics.logEvent(CameraAnalytics.CameraPermissionChecked(it))
        }
    }

    private fun launchQrScan() {
        QrScanActivity.start(this, QrExpected.MAIN_ACTIVITY_QR)
    }

    private fun showScanAndConnectBottomSheet() {
        showBottomSheet(ScanAndConnectBottomSheet.newInstance(showCta = true))
    }

    private fun setupNavigation() {
        binding.bottomNavigation.apply {
            if (!dashboardPrefs.hasTappedFabButton) {
                isPulseAnimationEnabled = true
            }
            onNavigationItemClick = {
                selectedNavigationItem = it
                when (it) {
                    NavigationItem.Home -> {
                        launchPortfolio()
                    }
                    NavigationItem.Prices -> {
                        launchPrices()
                    }
                    NavigationItem.BuyAndSell -> {
                        launchBuySell()
                    }
                    NavigationItem.Activity -> {
                        startActivitiesFragment()
                    }
                    else -> throw IllegalStateException("Illegal navigation state - unknown item $it")
                }
            }
            onMiddleButtonClick = {
                dashboardPrefs.hasTappedFabButton = true
                isPulseAnimationEnabled = false
                showBottomSheet(
                    RedesignActionsBottomSheet.newInstance()
                )
            }
        }
    }

    override fun showLoading() {
        binding.progress.visible()
        binding.progress.playAnimation()
    }

    override fun hideLoading() {
        binding.progress.gone()
        binding.progress.pauseAnimation()
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setTitle(R.string.logout_wallet)
            .setMessage(R.string.ask_you_sure_logout)
            .setPositiveButton(R.string.btn_logout) { _, _ ->
                logout()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    // TODO this is deprecated, should be replaced with ActivityResult.contract
    // some consideration needs to be paid to QR scanning and how it deals with the results
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            QrScanActivity.SCAN_URI_RESULT -> {
                data.getRawScanData()?.let {
                    val decodedData = URLDecoder.decode(it, "UTF-8")
                    if (resultCode == RESULT_OK) {
                        model.process(MainIntent.ProcessScanResult(decodedData))
                    }
                }
            }
            SETTINGS_EDIT,
            ACCOUNT_EDIT,
            KYC_STARTED -> {
                // Reset state in case of changing currency etc
                launchPortfolio()

                // Pass this result to balance fragment
                for (fragment in supportFragmentManager.fragments) {
                    fragment.onActivityResult(requestCode, resultCode, data)
                }
            }
            INTEREST_DASHBOARD -> {
                if (resultCode == RESULT_FIRST_USER) {
                    data?.let { intent ->
                        val account = intent.extras?.getAccount(InterestDashboardActivity.ACTIVITY_ACCOUNT)
                        startActivitiesFragment(account)
                    }
                }
            }
            BANK_DEEP_LINK_SIMPLE_BUY -> {
                if (resultCode == RESULT_OK) {
                    startActivity(
                        SimpleBuyActivity.newIntent(
                            context = this,
                            preselectedPaymentMethodId = data?.getStringExtra(BankAuthActivity.LINKED_BANK_ID_KEY)
                        )
                    )
                }
            }
            BANK_DEEP_LINK_SETTINGS -> {
                if (resultCode == RESULT_OK) {
                    startActivity(SettingsActivity.newIntent(this))
                }
            }
            BANK_DEEP_LINK_DEPOSIT -> {
                if (resultCode == RESULT_OK) {
                    launchPortfolio(
                        AssetAction.FiatDeposit,
                        data?.getStringExtra(
                            BankAuthActivity.LINKED_BANK_CURRENCY
                        )
                    )
                }
            }
            BANK_DEEP_LINK_WITHDRAW -> {
                if (resultCode == RESULT_OK) {
                    launchPortfolio(
                        AssetAction.Withdraw,
                        data?.getStringExtra(
                            BankAuthActivity.LINKED_BANK_CURRENCY
                        )
                    )
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun startTxFlowWithTargets(targets: Collection<CryptoTarget>) {
        if (targets.size > 1) {
            disambiguateSendScan(targets)
        } else if (targets.size == 1) {
            val targetAddress = targets.first()
            // FIXME selecting a source account shows UI, refactor so this can be called from the interactor
            compositeDisposable += qrProcessor.selectSourceAccount(this, targetAddress)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = { sourceAccount ->
                        startActivity(
                            TransactionFlowActivity.newIntent(
                                context = this,
                                sourceAccount = sourceAccount,
                                target = targetAddress,
                                action = AssetAction.Send
                            )
                        )
                    },
                    onComplete = {
                        Timber.d("No source accounts available for scan target")
                        showNoAccountFromScanSnackbar(targetAddress.asset)
                    },
                    onError = {
                        Timber.e("Unable to select source account for scan")
                        showNoAccountFromScanSnackbar(targetAddress.asset)
                    }
                )
        }
    }

    private fun disambiguateSendScan(targets: Collection<CryptoTarget>) {
        compositeDisposable += qrProcessor.disambiguateScan(this, targets)
            .subscribeBy(
                onSuccess = {
                    startTxFlowWithTargets(listOf(it))
                },
                onError = {
                    Timber.e("Failed to disambiguate scan: $it")
                }
            )
    }

    private fun showNoAccountFromScanSnackbar(asset: AssetInfo) =
        BlockchainSnackbar.make(
            binding.root, getString(R.string.scan_no_available_account, asset.displayTicker)
        ).show()

    override fun render(newState: MainState) {

        when (val view = newState.viewToLaunch) {
            is ViewToLaunch.DisplayAlertDialog -> displayDialog(view.dialogTitle, view.dialogMessage)
            is ViewToLaunch.LaunchAssetAction -> launchAssetAction(view.action, view.account)
            is ViewToLaunch.LaunchBuySell -> launchBuySell(view.type, view.asset)
            is ViewToLaunch.LaunchExchange -> {
                view.linkId?.let {
                    launchThePitLinking(it)
                } ?: kotlin.run { launchThePit() }
            }
            is ViewToLaunch.LaunchInterestDashboard -> launchInterestDashboard(view.origin)
            is ViewToLaunch.LaunchKyc -> KycNavHostActivity.startForResult(
                this, view.campaignType, KYC_STARTED
            )
            is ViewToLaunch.LaunchUpsellAssetAction -> replaceBottomSheet(
                KycUpgradePromptManager.getUpsellSheet(view.upsell)
            )
            is ViewToLaunch.LaunchOpenBankingApprovalDepositComplete -> {
                val currencyCode = view.amount.currencyCode
                val amountWithSymbol = view.amount.toStringWithSymbol()
                supportFragmentManager.findFragmentByTag(PortfolioFragment.javaClass.simpleName)
                    ?.let { portfolioFragment ->
                        (portfolioFragment as PortfolioFragment).refreshFiatAssets()
                    }

                replaceBottomSheet(
                    FiatTransactionBottomSheet.newInstance(
                        currencyCode,
                        getString(R.string.deposit_confirmation_success_title, amountWithSymbol),
                        getString(
                            R.string.yapily_fiat_deposit_success_subtitle, amountWithSymbol,
                            currencyCode,
                            view.estimatedDepositCompletionTime
                        ),
                        FiatTransactionState.SUCCESS
                    )
                )
            }
            is ViewToLaunch.LaunchOpenBankingApprovalDepositInProgress -> {
                replaceBottomSheet(
                    FiatTransactionBottomSheet.newInstance(
                        view.value.currencyCode,
                        getString(R.string.deposit_confirmation_pending_title),
                        getString(
                            R.string.deposit_confirmation_pending_subtitle
                        ),
                        FiatTransactionState.PENDING
                    )
                )
            }
            is ViewToLaunch.LaunchOpenBankingApprovalTimeout -> {
                replaceBottomSheet(
                    FiatTransactionBottomSheet.newInstance(
                        view.currencyCode,
                        getString(R.string.deposit_confirmation_pending_title),
                        getString(
                            R.string.deposit_confirmation_pending_subtitle
                        ),
                        FiatTransactionState.ERROR
                    )
                )
            }
            is ViewToLaunch.LaunchOpenBankingBuyApprovalError -> {
                BlockchainSnackbar.make(
                    binding.root,
                    getString(R.string.simple_buy_confirmation_error),
                    type = SnackbarType.Error
                ).show()
            }
            is ViewToLaunch.LaunchOpenBankingError -> {
                replaceBottomSheet(
                    FiatTransactionBottomSheet.newInstance(
                        view.currencyCode,
                        getString(R.string.deposit_confirmation_error_title),
                        getString(
                            R.string.deposit_confirmation_error_subtitle
                        ),
                        FiatTransactionState.ERROR
                    )
                )
            }
            is ViewToLaunch.LaunchOpenBankingLinking -> {
                launchOpenBankingLinking(view.bankLinkingInfo)
            }
            is ViewToLaunch.LaunchPaymentForCancelledOrder -> {
                replaceBottomSheet(
                    FiatTransactionBottomSheet.newInstance(
                        view.state.fiatCurrency.displayTicker,
                        getString(R.string.yapily_payment_to_fiat_wallet_title, view.state.fiatCurrency.displayTicker),
                        getString(
                            R.string.yapily_payment_to_fiat_wallet_subtitle,
                            view.state.selectedCryptoAsset?.displayTicker ?: getString(
                                R.string.yapily_payment_to_fiat_wallet_default
                            ),
                            view.state.fiatCurrency.displayTicker
                        ),
                        FiatTransactionState.SUCCESS
                    )
                )
            }
            is ViewToLaunch.LaunchReceive -> launchReceive()
            is ViewToLaunch.LaunchSend -> launchSend()
            is ViewToLaunch.LaunchSetupBiometricLogin -> launchSetupFingerprintLogin()
            is ViewToLaunch.LaunchSimpleBuy -> launchSimpleBuy(view.asset)
            is ViewToLaunch.LaunchSimpleBuyFromDeepLinkApproval -> launchSimpleBuyFromDeepLinkApproval()
            is ViewToLaunch.LaunchSwap -> launchSwap()
            is ViewToLaunch.LaunchTwoFaSetup -> launchSetup2Fa()
            is ViewToLaunch.LaunchVerifyEmail -> launchVerifyEmail()
            is ViewToLaunch.ShowOpenBankingError ->
                BlockchainSnackbar.make(
                    binding.root,
                    getString(R.string.open_banking_deeplink_error),
                    type = SnackbarType.Error
                ).show()
            is ViewToLaunch.CheckForAccountWalletLinkErrors -> showBottomSheet(
                AccountWalletLinkAlertSheet.newInstance(view.walletIdHint)
            )
            is ViewToLaunch.LaunchTransactionFlowWithTargets -> startTxFlowWithTargets(view.targets)
            is ViewToLaunch.ShowTargetScanError -> showTargetScanError(view.error)
            is ViewToLaunch.None -> {
                // do nothing
            }
            is ViewToLaunch.LaunchWalletConnectSessionApproval -> launchWalletConnectSessionApproval(
                view.walletConnectSession
            )
            is ViewToLaunch.LaunchWalletConnectSessionApproved -> launchWalletConnectSessionApproved(
                view.walletConnectSession
            )
            is ViewToLaunch.LaunchWalletConnectSessionRejected -> launchWalletConnectSessionRejected(
                view.walletConnectSession
            )
            ViewToLaunch.ShowEntitySwitchSilverKycUpsell -> {
                var ctaClicked = false
                var alertDialog: AlertDialog? = null
                val dialog = MaterialAlertDialogBuilder(this, R.style.RoundedCornersDialog)
                val contentViewBinding = DialogEntitySwitchSilverBinding.inflate(layoutInflater).apply {
                    closeButton.setOnClickListener {
                        alertDialog?.dismiss()
                        alertDialog = null
                    }
                    verifyNowButton.text = getString(R.string.entity_switch_silver_dialog_verify_now)
                    verifyNowButton.setOnClickListener {
                        analytics.logEvent(EntitySwitchSilverKycUpsellCtaClicked)
                        ctaClicked = true
                        alertDialog?.dismiss()
                        showBottomSheet(KycUpgradeNowSheet.newInstance())
                    }
                }
                dialog.setView(contentViewBinding.root)
                dialog.setOnDismissListener {
                    alertDialog = null
                    if (!ctaClicked) analytics.logEvent(EntitySwitchSilverKycUpsellDismissed)
                }
                analytics.logEvent(EntitySwitchSilverKycUpsellViewed)
                alertDialog = dialog.show()
            }
            ViewToLaunch.ShowUiTour -> {
                binding.uiTour.host = this
                showUiTour()
            }
        }.exhaustive

        // once we've completed a loop of render with a view to launch
        // ensure we reset the UI state to avoid duplication of screens on navigating back
        if (newState.viewToLaunch != ViewToLaunch.None) {
            model.process(MainIntent.ResetViewState)
        }
    }

    private fun navigateToDeeplinkDestination(destination: Destination) {
        when (destination) {
            is Destination.AssetViewDestination -> {
                destinationArgs.getAssetInfo(destination.networkTicker)?.let { assetInfo ->
                    activityResultsContract.launch(
                        CoinViewActivity.newIntent(
                            context = this,
                            asset = assetInfo
                        )
                    )
                } ?: run {
                    Timber.e("Unable to start CoinViewActivity from deeplink. AssetInfo is null")
                }
            }

            is Destination.AssetBuyDestination -> {
                destinationArgs.getAssetInfo(destination.networkTicker)?.let { assetInfo ->
                    startActivity(
                        SimpleBuyActivity.newIntent(
                            context = this,
                            asset = assetInfo,
                            preselectedAmount = destination.amount
                        )
                    )
                } ?: run {
                    Timber.e("Unable to start SimpleBuyActivity from deeplink. AssetInfo is null")
                }
            }

            is Destination.AssetSendDestination -> {
                destinationArgs.getAssetInfo(destination.networkTicker)?.let { assetInfo ->
                    destinationArgs.getSendSourceCryptoAccount(assetInfo, destination.accountAddress).subscribeBy(
                        onSuccess = { account ->
                            startActivity(
                                TransactionFlowActivity.newIntent(
                                    context = application,
                                    sourceAccount = account,
                                    action = AssetAction.Send
                                )
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

            is Destination.ActivityDestination -> {
                startActivitiesFragment()
            }
        }.exhaustive

        model.process(MainIntent.ClearDeepLinkResult)
    }

    private fun launchWalletConnectSessionApproval(walletConnectSession: WalletConnectSession) {
        showBottomSheet(
            WCApproveSessionBottomSheet.newInstance(walletConnectSession)
        )
    }

    private fun launchWalletConnectSessionApproved(walletConnectSession: WalletConnectSession) {
        showBottomSheet(
            WCSessionUpdatedBottomSheet.newInstance(session = walletConnectSession, approved = true)
        )
    }

    private fun launchWalletConnectSessionRejected(walletConnectSession: WalletConnectSession) {
        showBottomSheet(
            WCSessionUpdatedBottomSheet.newInstance(session = walletConnectSession, approved = false)
        )
    }

    private fun showTargetScanError(error: QrScanError) {
        BlockchainSnackbar.make(
            binding.root,
            getString(
                when (error.errorCode) {
                    QrScanError.ErrorCode.ScanFailed -> R.string.error_scan_failed_general
                    QrScanError.ErrorCode.BitPayScanFailed -> R.string.error_scan_failed_bitpay
                }
            ),
            type = SnackbarType.Error
        ).show()
    }

    private fun launchAssetAction(
        action: AssetAction,
        account: BlockchainAccount?
    ) = when (action) {
        AssetAction.Receive -> replaceBottomSheet(ReceiveDetailSheet.newInstance(account as CryptoAccount))
        AssetAction.Swap -> launchSwap(sourceAccount = account as CryptoAccount)
        AssetAction.ViewActivity -> startActivitiesFragment(account)
        else -> {
            // do nothing
        }
    }

    private fun startActivitiesFragment(
        account: BlockchainAccount? = null,
        reload: Boolean = false
    ) {
        updateToolbarTitle(title = getString(R.string.main_toolbar_activity))
        binding.bottomNavigation.selectedNavigationItem = NavigationItem.Activity
        supportFragmentManager.showFragment(
            fragment = ActivitiesFragment.newInstance(account),
            reloadFragment = reload
        )
        analytics.logEvent(activityShown(account?.label ?: "All Wallets"))
    }

    override fun startDashboardOnboarding() {
        hideUiTour(onAnimationEnd = {
            supportFragmentManager.findFragmentByTag(PortfolioFragment::class.java.simpleName)
                ?.let {
                    (it as PortfolioFragment).launchNewUserDashboardOnboarding()
                }
        })
    }

    override fun startBuy() {
        hideUiTour(onAnimationEnd = {
            launchBuySell(BuySellFragment.BuySellViewType.TYPE_BUY)
        })
    }

    override fun dismiss() {
        hideUiTour()
    }

    override fun startKycClicked() {
        launchKyc(CampaignType.None)
    }

    private fun showUiTour() {
        binding.uiTour.apply {
            alpha = 0f
            visible()
            animate()
                .alpha(1f)
                .setStartDelay(500L)
                .setDuration(resources.getInteger(android.R.integer.config_mediumAnimTime).toLong())
        }
    }

    private fun hideUiTour(onAnimationEnd: (() -> Unit)? = null) {
        binding.uiTour.apply {
            animate()
                .alpha(0f)
                .setDuration(resources.getInteger(android.R.integer.config_shortAnimTime).toLong())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator?) {
                        gone()
                        onAnimationEnd?.invoke()
                    }
                })
        }
    }

    override fun exitSimpleBuyFlow() {
        launchBuySell()
    }

    override fun popFragmentsInStackUntilFind(fragmentName: String, popInclusive: Boolean) {
        supportFragmentManager.popBackStack(
            fragmentName,
            if (popInclusive) POP_BACK_STACK_INCLUSIVE else 0
        )
    }

    override fun showErrorInBottomSheet(title: String, description: String, button: String?) {
        // do nothing
    }

    override fun logout() {
        analytics.logEvent(AnalyticsEvents.Logout)
        model.process(MainIntent.UnpairWallet)
        if (AndroidUtils.is25orHigher()) {
            getSystemService(ShortcutManager::class.java).removeAllDynamicShortcuts()
        }
    }

    override fun onSessionApproved(session: WalletConnectSession) {
        model.process(MainIntent.ApproveWCSession(session))
        analytics.logEvent(
            WalletConnectAnalytics.DappConectionActioned(
                action = WalletConnectAnalytics.DappConnectionAction.CONFIRM,
                appName = session.dAppInfo.peerMeta.name
            )
        )
    }

    override fun onSessionRejected(session: WalletConnectSession) {
        model.process(MainIntent.RejectWCSession(session))
        analytics.logEvent(
            WalletConnectAnalytics.DappConectionActioned(
                action = WalletConnectAnalytics.DappConnectionAction.CANCEL,
                appName = session.dAppInfo.peerMeta.name
            )
        )
    }

    override fun onCameraAccessAllowed() {
        launchQrScan()
    }

    override fun onSheetClosed() {
        binding.bottomNavigation.bottomNavigationState = BottomNavigationState.Add
        Timber.d("On closed")
    }

    override fun navigateToBottomSheet(bottomSheet: BottomSheetDialogFragment) {
        clearBottomSheet()
        showBottomSheet(bottomSheet)
    }

    private fun launchPortfolio(
        action: AssetAction? = null,
        fiatCurrency: String? = null,
        reload: Boolean = false
    ) {
        updateToolbarTitle(title = getString(R.string.main_toolbar_home))
        binding.bottomNavigation.selectedNavigationItem = NavigationItem.Home
        supportFragmentManager.showFragment(
            fragment = PortfolioFragment.newInstance(action, fiatCurrency),
            reloadFragment = reload
        )
    }

    override fun launchSwapScreen() {
        launchSwap()
    }

    override fun launchSwap(
        sourceAccount: CryptoAccount?,
        targetAccount: CryptoAccount?
    ) {
        if (sourceAccount == null && targetAccount == null) {
            actionsResultContract.launch(ActionActivity.ActivityArgs(AssetAction.Swap))
        } else if (sourceAccount != null) {
            startActivity(
                TransactionFlowActivity.newIntent(
                    context = this,
                    sourceAccount = sourceAccount,
                    target = targetAccount ?: NullCryptoAccount(),
                    action = AssetAction.Swap
                )
            )
        }
    }

    override fun launchTooManyPendingBuys(maxTransactions: Int) =
        showBottomSheet(BuyPendingOrdersBottomSheet.newInstance(maxTransactions))

    override fun startActivityRequested() {
        launchAssetAction(AssetAction.ViewActivity, null)
    }

    override fun launchKyc(campaignType: CampaignType) {
        KycNavHostActivity.startForResult(this, campaignType, KYC_STARTED)
    }

    override fun launchThePitLinking(linkId: String) {
        PitPermissionsActivity.start(this, linkId)
    }

    override fun launchThePit() {
        ExchangeConnectionSheet.launch(this)
    }

    override fun launchBackupFunds(fragment: Fragment?, requestCode: Int) {
        fragment?.let {
            BackupWalletActivity.startForResult(it, requestCode)
        } ?: BackupWalletActivity.start(this)
    }

    override fun launchSetup2Fa() {
        startActivity(SettingsActivity.newIntent(this, true))
    }

    override fun launchVerifyEmail() {
        Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_EMAIL)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(Intent.createChooser(this, getString(R.string.security_centre_email_check)))
        }
    }

    override fun launchSetupFingerprintLogin() {
        OnboardingActivity.launchForFingerprints(this)
    }

    override fun launchReceive() {
        actionsResultContract.launch(ActionActivity.ActivityArgs(AssetAction.Receive))
    }

    override fun launchSend() {
        actionsResultContract.launch(ActionActivity.ActivityArgs(AssetAction.Send))
    }

    override fun launchBuy() {
        launchBuySell(BuySellFragment.BuySellViewType.TYPE_BUY)
    }

    override fun launchSell() {
        launchBuySell(BuySellFragment.BuySellViewType.TYPE_SELL)
    }

    override fun launchBuySell(
        viewType: BuySellFragment.BuySellViewType,
        asset: AssetInfo?,
        reload: Boolean
    ) {
        updateToolbarTitle(title = getString(R.string.main_toolbar_buy_sell))
        binding.bottomNavigation.selectedNavigationItem = NavigationItem.BuyAndSell

        val buySellFragment = BuySellFragment.newInstance(
            viewType = viewType,
            asset = asset
        )

        if (!reload) {
            val currentBuySell: BuySellFragment? =
                supportFragmentManager.findFragmentByTag(buySellFragment.javaClass.simpleName)
                    as? BuySellFragment
            currentBuySell?.goToPage(viewType.ordinal)
        }

        supportFragmentManager.showFragment(
            fragment = buySellFragment,
            reloadFragment = reload
        )
    }

    private fun launchPrices(reload: Boolean = false) {
        updateToolbarTitle(title = getString(R.string.main_toolbar_prices))
        supportFragmentManager.showFragment(
            fragment = PricesFragment.newInstance(),
            reloadFragment = reload
        )
    }

    override fun launchSimpleBuy(asset: AssetInfo) {
        startActivity(
            SimpleBuyActivity.newIntent(
                context = this,
                launchFromNavigationBar = true,
                asset = asset
            )
        )
    }

    override fun launchInterestDashboard(origin: LaunchOrigin) {
        startActivityForResult(
            InterestDashboardActivity.newInstance(this), INTEREST_DASHBOARD
        )
        analytics.logEvent(InterestAnalytics.InterestClicked(origin = LaunchOrigin.DASHBOARD_PROMO))
    }

    override fun launchFiatDeposit(currency: String) {
        launchPortfolio(AssetAction.FiatDeposit, currency)
    }

    override fun launchTransfer() {
        // do nothing, transfer fragment is not used in the new designs
    }

    override fun launchOpenBankingLinking(bankLinkingInfo: BankLinkingInfo) {
        startActivityForResult(
            BankAuthActivity.newInstance(bankLinkingInfo.linkingId, bankLinkingInfo.bankAuthSource, this),
            when (bankLinkingInfo.bankAuthSource) {
                BankAuthSource.SIMPLE_BUY -> BANK_DEEP_LINK_SIMPLE_BUY
                BankAuthSource.SETTINGS -> BANK_DEEP_LINK_SETTINGS
                BankAuthSource.DEPOSIT -> BANK_DEEP_LINK_DEPOSIT
                BankAuthSource.WITHDRAW -> BANK_DEEP_LINK_WITHDRAW
            }.exhaustive
        )
    }

    override fun launchSimpleBuyFromDeepLinkApproval() {
        startActivity(SimpleBuyActivity.newIntent(this, launchFromApprovalDeepLink = true))
    }

    override fun launchPendingVerificationScreen(campaignType: CampaignType) {
        KycStatusActivity.start(this, campaignType)
    }

    override fun performAssetActionFor(action: AssetAction, account: BlockchainAccount?) {
        model.process(MainIntent.ValidateAccountAction(action, account))
    }

    override fun resumeSimpleBuyKyc() {
        startActivity(
            SimpleBuyActivity.newIntent(
                context = this,
                launchKycResume = true
            )
        )
    }

    private fun displayDialog(title: Int, message: Int) {
        AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    companion object {
        private const val START_UI_TOUR_KEY = "START_UI_TOUR_KEY"
        private const val SHOW_SWAP = "SHOW_SWAP"
        private const val LAUNCH_AUTH_FLOW = "LAUNCH_AUTH_FLOW"
        private const val INTENT_FROM_NOTIFICATION = "INTENT_FROM_NOTIFICATION"
        private const val INTENT_FROM_NOTIFICATION_ANALYTICS_PAYLOAD = "INTENT_FROM_NOTIFICATION_ANALYTICS_PAYLOAD"
        private const val PENDING_DESTINATION = "PENDING_DESTINATION"
        const val ACCOUNT_EDIT = 2008
        const val SETTINGS_EDIT = 2009
        const val KYC_STARTED = 2011
        const val INTEREST_DASHBOARD = 2012
        const val BANK_DEEP_LINK_SIMPLE_BUY = 2013
        const val BANK_DEEP_LINK_SETTINGS = 2014
        const val BANK_DEEP_LINK_DEPOSIT = 2015
        const val BANK_DEEP_LINK_WITHDRAW = 2021

        fun newIntent(context: Context, shouldShowSwap: Boolean, shouldBeNewTask: Boolean): Intent =
            Intent(context, MainActivity::class.java).apply {
                putExtra(SHOW_SWAP, shouldShowSwap)
                if (shouldBeNewTask) {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                context.startActivity(this)
            }

        fun newIntent(
            context: Context,
            launchAuthFlow: Boolean,
            pubKeyHash: String,
            message: String,
            originIp: String?,
            originLocation: String?,
            originBrowser: String?,
            forcePin: Boolean,
            shouldBeNewTask: Boolean
        ): Intent = Intent(context, MainActivity::class.java).apply {
            putExtra(LAUNCH_AUTH_FLOW, launchAuthFlow)
            putExtra(AuthNewLoginSheet.PUB_KEY_HASH, pubKeyHash)
            putExtra(AuthNewLoginSheet.MESSAGE, message)
            putExtra(AuthNewLoginSheet.ORIGIN_IP, originIp)
            putExtra(AuthNewLoginSheet.ORIGIN_LOCATION, originLocation)
            putExtra(AuthNewLoginSheet.ORIGIN_BROWSER, originBrowser)
            putExtra(AuthNewLoginSheet.FORCE_PIN, forcePin)

            if (shouldBeNewTask) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        fun newIntent(
            context: Context,
            intentFromNotification: Boolean,
            notificationAnalyticsPayload: Serializable?
        ): Intent =
            Intent(context, MainActivity::class.java).apply {
                putExtra(INTENT_FROM_NOTIFICATION, intentFromNotification)
                putExtra(INTENT_FROM_NOTIFICATION_ANALYTICS_PAYLOAD, notificationAnalyticsPayload)
            }

        fun newIntent(context: Context, pendingDestination: Destination): Intent =
            Intent(context, MainActivity::class.java).apply {
                putExtra(PENDING_DESTINATION, pendingDestination)
            }

        fun newIntentAsNewTask(context: Context): Intent =
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            }

        fun newIntent(
            context: Context,
            intentData: String?,
            shouldLaunchUiTour: Boolean,
            shouldBeNewTask: Boolean
        ): Intent = Intent(context, MainActivity::class.java).apply {
            if (intentData != null) {
                data = Uri.parse(intentData)
            }

            if (shouldBeNewTask) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            putExtra(START_UI_TOUR_KEY, shouldLaunchUiTour)
        }
    }
}
