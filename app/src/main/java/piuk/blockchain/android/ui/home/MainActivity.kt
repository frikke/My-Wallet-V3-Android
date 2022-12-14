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
import com.blockchain.analytics.events.transactionsShown
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.CryptoTarget
import com.blockchain.coincore.NullCryptoAccount
import com.blockchain.coincore.StakingAccount
import com.blockchain.coincore.TransactionTarget
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import com.blockchain.commonarch.presentation.mvi.MviActivity
import com.blockchain.commonarch.presentation.mvi_v2.NavigationRouter
import com.blockchain.componentlib.alert.BlockchainSnackbar
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.databinding.ToolbarGeneralBinding
import com.blockchain.componentlib.navigation.BottomNavigationState
import com.blockchain.componentlib.navigation.NavigationBarButton
import com.blockchain.componentlib.navigation.NavigationItem
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.deeplinking.navigation.Destination
import com.blockchain.deeplinking.navigation.DestinationArgs
import com.blockchain.domain.common.model.BuySellViewType
import com.blockchain.domain.referral.model.ReferralInfo
import com.blockchain.earn.EarnAnalytics
import com.blockchain.earn.dashboard.EarnDashboardFragment
import com.blockchain.earn.interest.InterestSummarySheet
import com.blockchain.extensions.exhaustive
import com.blockchain.nfts.NftHost
import com.blockchain.nfts.collection.NftCollectionFragment
import com.blockchain.nfts.detail.NftDetailFragment
import com.blockchain.nfts.help.NftHelpFragment
import com.blockchain.notifications.analytics.NotificationAnalyticsEvents
import com.blockchain.notifications.analytics.NotificationAnalyticsEvents.Companion.createCampaignPayload
import com.blockchain.preferences.DashboardPrefs
import com.blockchain.preferences.SuperAppMvpPrefs
import com.blockchain.presentation.customviews.kyc.KycUpgradeNowSheet
import com.blockchain.presentation.extensions.getAccount
import com.blockchain.presentation.koin.scopedInject
import com.blockchain.presentation.openUrl
import com.blockchain.utils.emptySubscribe
import com.blockchain.walletconnect.domain.WalletConnectAnalytics
import com.blockchain.walletconnect.domain.WalletConnectSession
import com.blockchain.walletconnect.ui.networks.NetworkInfo
import com.blockchain.walletconnect.ui.networks.SelectNetworkBottomSheet
import com.blockchain.walletconnect.ui.sessionapproval.WCApproveSessionBottomSheet
import com.blockchain.walletconnect.ui.sessionapproval.WCSessionUpdatedBottomSheet
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import java.net.URLDecoder
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.databinding.ActivityMainBinding
import piuk.blockchain.android.fraud.domain.service.FraudService
import piuk.blockchain.android.scan.QrScanError
import piuk.blockchain.android.scan.QrScanResultProcessor
import piuk.blockchain.android.simplebuy.SimpleBuyActivity
import piuk.blockchain.android.simplebuy.SimpleBuySyncFactory
import piuk.blockchain.android.simplebuy.SmallSimpleBuyNavigator
import piuk.blockchain.android.simplebuy.sheets.BuyPendingOrdersBottomSheet
import piuk.blockchain.android.support.SupportCentreActivity
import piuk.blockchain.android.ui.activity.ActivitiesFragment
import piuk.blockchain.android.ui.addresses.AddressesActivity
import piuk.blockchain.android.ui.airdrops.AirdropCentreActivity
import piuk.blockchain.android.ui.auth.AccountWalletLinkAlertSheet
import piuk.blockchain.android.ui.auth.newlogin.presentation.AuthNewLoginSheet
import piuk.blockchain.android.ui.auth.newlogin.presentation.SecureChannelBrowserMessageArg
import piuk.blockchain.android.ui.backup.BackupWalletActivity
import piuk.blockchain.android.ui.base.showFragment
import piuk.blockchain.android.ui.brokerage.BuySellFragment
import piuk.blockchain.android.ui.coinview.presentation.CoinViewActivityV2
import piuk.blockchain.android.ui.coinview.presentation.CoinViewActivityV2.Companion.ACCOUNT_FOR_ACTIVITY
import piuk.blockchain.android.ui.dashboard.PortfolioFragment
import piuk.blockchain.android.ui.dashboard.walletmode.WalletModeSelectionBottomSheet
import piuk.blockchain.android.ui.dashboard.walletmode.icon
import piuk.blockchain.android.ui.dashboard.walletmode.title
import piuk.blockchain.android.ui.home.analytics.BuyDefiAnalyticsEvents
import piuk.blockchain.android.ui.home.models.LaunchFlowForAccount
import piuk.blockchain.android.ui.home.models.MainIntent
import piuk.blockchain.android.ui.home.models.MainModel
import piuk.blockchain.android.ui.home.models.MainState
import piuk.blockchain.android.ui.home.models.ReferralState
import piuk.blockchain.android.ui.home.models.ViewToLaunch
import piuk.blockchain.android.ui.home.ui_tour.UiTourAnalytics
import piuk.blockchain.android.ui.home.ui_tour.UiTourView
import piuk.blockchain.android.ui.interest.InterestDashboardActivity
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity
import piuk.blockchain.android.ui.kyc.status.KycStatusActivity
import piuk.blockchain.android.ui.linkbank.BankAuthActivity
import piuk.blockchain.android.ui.linkbank.BankAuthSource
import piuk.blockchain.android.ui.linkbank.BankLinkingInfo
import piuk.blockchain.android.ui.linkbank.FiatTransactionState
import piuk.blockchain.android.ui.linkbank.yapily.FiatTransactionBottomSheet
import piuk.blockchain.android.ui.membership.MembershipActivity
import piuk.blockchain.android.ui.onboarding.OnboardingActivity
import piuk.blockchain.android.ui.prices.presentation.PricesFragment
import piuk.blockchain.android.ui.prices.presentation.PricesNavigationEvent
import piuk.blockchain.android.ui.referral.presentation.Origin
import piuk.blockchain.android.ui.referral.presentation.ReferralAnalyticsEvents
import piuk.blockchain.android.ui.referral.presentation.ReferralSheet
import piuk.blockchain.android.ui.scan.CameraAnalytics
import piuk.blockchain.android.ui.scan.QrExpected
import piuk.blockchain.android.ui.scan.QrScanActivity
import piuk.blockchain.android.ui.scan.QrScanActivity.Companion.getRawScanData
import piuk.blockchain.android.ui.scan.ScanAndConnectBottomSheet
import piuk.blockchain.android.ui.settings.SettingsActivity
import piuk.blockchain.android.ui.settings.SettingsActivity.Companion.SettingsDestination
import piuk.blockchain.android.ui.transactionflow.flow.TransactionFlowActivity
import piuk.blockchain.android.ui.transfer.receive.detail.ReceiveDetailActivity
import piuk.blockchain.android.ui.upsell.KycUpgradePromptManager
import piuk.blockchain.android.util.AndroidUtils
import timber.log.Timber

class MainActivity :
    MviActivity<MainModel, MainIntent, MainState, ActivityMainBinding>(),
    HomeNavigator,
    SlidingModalBottomDialog.Host,
    AuthNewLoginSheet.Host,
    AccountWalletLinkAlertSheet.Host,
    SelectNetworkBottomSheet.Host,
    WCApproveSessionBottomSheet.Host,
    BuyDefiBottomSheet.Host,
    ActionBottomSheetHost,
    SmallSimpleBuyNavigator,
    BuyPendingOrdersBottomSheet.Host,
    ScanAndConnectBottomSheet.Host,
    UiTourView.Host,
    KycUpgradeNowSheet.Host,
    NftHost,
    InterestSummarySheet.Host,
    NavigationRouter<PricesNavigationEvent>,
    EarnDashboardFragment.Host {

    override val alwaysDisableScreenshots: Boolean
        get() = false

    override val model: MainModel by scopedInject()

    override fun initBinding(): ActivityMainBinding = ActivityMainBinding.inflate(layoutInflater)

    override val toolbarBinding: ToolbarGeneralBinding
        get() = binding.mainToolbar

    private val dashboardPrefs: DashboardPrefs by scopedInject()
    private val walletModeService: WalletModeService by scopedInject()
    private val mvpPrefs: SuperAppMvpPrefs by inject()

    @Deprecated("Use MVI loop instead")
    private val compositeDisposable = CompositeDisposable()

    @Deprecated("Use MVI loop instead")
    private val qrProcessor: QrScanResultProcessor by scopedInject()

    private val destinationArgs: DestinationArgs by scopedInject()

    private val simpleBuySyncFactory: SimpleBuySyncFactory by scopedInject()

    private val fraudService: FraudService by inject()

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
            is ActionActivity.ActivityResult.StartReceive -> launchReceive(cryptoTicker = it.cryptoTicker)
            ActionActivity.ActivityResult.StartBuyIntro -> launchBuySell(BuySellViewType.TYPE_BUY)
            ActionActivity.ActivityResult.ViewActivity -> performAssetActionFor(AssetAction.ViewActivity)
            null -> {
            }
        }
    }

    private val activityResultsContract = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            (it.data?.getAccount(ACCOUNT_FOR_ACTIVITY))?.let { account ->
                startActivitiesFragment(account)
            }
        }
    }

    private var isStakingAccountEnabled: Boolean = false
    private var isEarnOnNavBarEnabled: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        fraudService.updateAuthenticatedUserFlows()

        launchPortfolio()

        setupToolbarAndNavigation()

        if (intent.hasExtra(INTENT_FROM_NOTIFICATION) &&
            intent.getBooleanExtra(INTENT_FROM_NOTIFICATION, false)
        ) {
            analytics.logEvent(NotificationAppOpened)
            val payload = createCampaignPayload(intent.extras)
            analytics.logEvent(NotificationAnalyticsEvents.PushNotificationTapped(payload))
        }

        val startUiTour = intent.getBooleanExtra(
            START_UI_TOUR_KEY, false
        )
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
                        message = it.getParcelable(AuthNewLoginSheet.MESSAGE),
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

        if (savedInstanceState == null) {
            model.process(MainIntent.PerformInitialChecks(intent))
            model.process(MainIntent.CheckReferralCode)

            if (startUiTour) {
                showUiTourIfCustodial()
            }
        }

        model.process(MainIntent.LoadFeatureFlags)
    }

    override fun onResume() {
        super.onResume()
        simpleBuySyncFactory.cancelAnyPendingConfirmationBuy()
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
            SettingsActivity.Companion.SettingsAction.Airdrops ->
                startActivity(AirdropCentreActivity.newIntent(this))
            SettingsActivity.Companion.SettingsAction.WebLogin ->
                QrScanActivity.start(this, QrExpected.MAIN_ACTIVITY_QR)
            SettingsActivity.Companion.SettingsAction.Logout -> showLogoutDialog()
        }.also {
            hideLoading()
        }
    }

    private val qrButton = NavigationBarButton.Icon(
        drawable = R.drawable.ic_qr_scan,
        contentDescription = R.string.accessibility_qr_code_scanner
    ) {
        tryToLaunchQrScan()
        analytics.logEvent(CameraAnalytics.QrCodeClicked())
    }

    private val settingsButton = NavigationBarButton.Icon(
        drawable = R.drawable.ic_bank_user,
        contentDescription = R.string.accessibility_user_settings
    ) {
        showLoading()
        settingsResultContract.launch(SettingsActivity.newIntent(this))
    }

    private fun setupMenuWithPresentButton(referralState: ReferralState) {
        val presentButton = if (referralState.referralInfo is ReferralInfo.Data) {
            NavigationBarButton.Icon(
                drawable = if (referralState.hasReferralBeenClicked) {
                    R.drawable.ic_present
                } else {
                    R.drawable.ic_present_dot
                },
                contentDescription = R.string.accessibility_referral,
                color = null
            ) {
                if (referralState.areMembershipsEnabled) {
                    startActivity(MembershipActivity.newIntent(this))
                } else {
                    model.process(MainIntent.ReferralIconClicked)
                    showReferralBottomSheet(referralState.referralInfo)
                    analytics.logEvent(ReferralAnalyticsEvents.ReferralProgramClicked(Origin.Portfolio))
                }
            }
        } else {
            null
        }
        if (presentButton != null) {
            updateToolbarMenuItems(listOf(qrButton, presentButton, settingsButton))
        }
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

    private fun showReferralBottomSheet(info: ReferralInfo) {
        if (info is ReferralInfo.Data) {
            showBottomSheet(
                ReferralSheet.newInstance()
            )
        }
    }

    private fun setupToolbarAndNavigation() {
        walletModeService.walletModeSingle.subscribeBy { walletMode ->
            binding.bottomNavigation.apply {
                if (!dashboardPrefs.hasTappedFabButton) {
                    isPulseAnimationEnabled = true
                }
                onNavigationItemClick = {
                    it.launch()
                }
                onMiddleButtonClick = {
                    dashboardPrefs.hasTappedFabButton = true
                    binding.bottomNavigation.isPulseAnimationEnabled = false
                    showBottomSheet(
                        middleButtonBottomSheetLaunch(walletMode)
                    )
                }
            }
            updateToolbarMenuItems(
                listOf(qrButton, settingsButton)
            )
            updateToolbarStartItem(
                NavigationBarButton.DropdownIndicator(
                    dropDownClicked = {
                        mvpPrefs.shouldHighlightModeSwitch = false
                        showBottomSheet(WalletModeSelectionBottomSheet.newInstance())
                    },
                    isHighlighted = mvpPrefs.shouldHighlightModeSwitch,
                    text = getString(walletMode.title()),
                    rightIcon = walletMode.icon(),
                    contentDescription = walletMode.name
                )
            )
        }
    }

    private fun NavigationItem.launch() {
        when (this) {
            NavigationItem.Home -> launchPortfolio()
            NavigationItem.Prices -> launchPrices()
            NavigationItem.BuyAndSell -> launchBuySell()
            NavigationItem.Nfts -> launchNfts()
            NavigationItem.Activity -> startActivitiesFragment()
            NavigationItem.Earn -> launchEarn()
        }.exhaustive
    }

    private fun launchEarn() {
        homeToolbarTitle()
        updateSelectedNavigationItem(NavigationItem.Earn)

        supportFragmentManager.showFragment(
            fragment = EarnDashboardFragment.newInstance(),
            reloadFragment = false
        )
    }

    private fun launchNfts() {
        homeToolbarTitle()
        updateSelectedNavigationItem(NavigationItem.Nfts)

        supportFragmentManager.showFragment(
            fragment = NftCollectionFragment(),
            reloadFragment = false
        )
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
                    goToSettings()
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
                        AssetAction.FiatWithdraw,
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
                        BlockchainSnackbar.make(
                            binding.root,
                            getString(R.string.scan_no_account_selected, targetAddress.asset.displayTicker)
                        ).show()
                    },
                    onError = {
                        Timber.e("Unable to select source account for scan")
                        BlockchainSnackbar.make(
                            binding.root,
                            getString(R.string.scan_no_available_account, targetAddress.asset.displayTicker)
                        ).show()
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

    override fun render(newState: MainState) {
        isStakingAccountEnabled = newState.isStakingEnabled
        isEarnOnNavBarEnabled = newState.isEarnOnNavEnabled

        when (val view = newState.viewToLaunch) {
            is ViewToLaunch.DisplayAlertDialog -> displayDialog(view.dialogTitle, view.dialogMessage)
            is ViewToLaunch.LaunchAssetAction -> launchAssetAction(view.action, view.account)
            is ViewToLaunch.LaunchBuySell -> launchBuySell(view.type, view.asset)
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
            is ViewToLaunch.LaunchServerDrivenOpenBankingError -> {
                replaceBottomSheet(
                    FiatTransactionBottomSheet.newInstance(
                        view.currencyCode,
                        view.title,
                        view.description,
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
            is ViewToLaunch.LaunchReceive -> launchReceive(cryptoTicker = null)
            is ViewToLaunch.LaunchSend -> launchSend()
            is ViewToLaunch.LaunchSetupBiometricLogin -> launchSetupFingerprintLogin()
            is ViewToLaunch.LaunchSimpleBuy -> launchSimpleBuy(view.asset)
            is ViewToLaunch.LaunchSimpleBuyFromDeepLinkApproval -> launchSimpleBuyFromDeepLinkApproval()
            is ViewToLaunch.LaunchSwap -> launchSwap()
            is ViewToLaunch.LaunchTwoFaSetup -> launchSetup2Fa()
            is ViewToLaunch.LaunchVerifyEmail -> launchOpenExternalEmailApp()
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
            is ViewToLaunch.LaunchWalletConnectSessionNetworkSelection ->
                launchWalletConnectSessionSelectNetwork(view.walletConnectSession)
            is ViewToLaunch.LaunchWalletConnectSessionApproval ->
                launchWalletConnectSessionApproval(view.walletConnectSession)
            is ViewToLaunch.LaunchWalletConnectSessionApprovalWithNetwork ->
                launchWalletConnectSessionApprovalWithNetwork(
                    view.walletConnectSession,
                    view.networkInfo
                )
            is ViewToLaunch.LaunchWalletConnectSessionApproved -> launchWalletConnectSessionApproved(
                view.walletConnectSession
            )
            is ViewToLaunch.LaunchWalletConnectSessionRejected -> launchWalletConnectSessionRejected(
                view.walletConnectSession
            )
            is ViewToLaunch.ShowReferralSheet -> {
                analytics.logEvent(ReferralAnalyticsEvents.ReferralProgramClicked(Origin.Deeplink))
                showReferralBottomSheet(newState.referral.referralInfo)
            }
            is ViewToLaunch.LaunchTxFlowWithAccountForAction -> {
                when (view.account) {
                    is LaunchFlowForAccount.SourceAndTargetAccount ->
                        startActivity(
                            TransactionFlowActivity.newIntent(
                                this,
                                action = view.action,
                                sourceAccount = view.account.sourceAccount,
                                target = view.account.targetAccount
                            )
                        )
                    is LaunchFlowForAccount.SourceAccount ->
                        startActivity(
                            TransactionFlowActivity.newIntent(
                                this,
                                action = view.action,
                                sourceAccount = view.account.source
                            )
                        )
                    is LaunchFlowForAccount.TargetAccount ->
                        startActivity(
                            TransactionFlowActivity.newIntent(
                                this,
                                action = view.action,
                                target = view.account.target
                            )
                        )
                    is LaunchFlowForAccount.NoAccount ->
                        startActivity(
                            TransactionFlowActivity.newIntent(
                                this,
                                action = view.action
                            )
                        )
                }
            }
            is ViewToLaunch.GoToActivityForAccount -> goToActivityFor(view.account)
            is ViewToLaunch.LaunchRewardsSummaryFromDeepLink -> {
                if (view.account is LaunchFlowForAccount.SourceAccount) {
                    showBottomSheet(
                        InterestSummarySheet.newInstance(
                            singleAccount = view.account.source as CryptoAccount
                        )
                    )
                } else {
                    launchInterestDashboard(LaunchOrigin.DASHBOARD)
                }
            }
        }.exhaustive

        // once we've completed a loop of render with a view to launch
        // ensure we reset the UI state to avoid duplication of screens on navigating back
        if (newState.viewToLaunch != ViewToLaunch.None) {
            model.process(MainIntent.ResetViewState)
        }
        if (newState.referral.referralInfo != ReferralInfo.NotAvailable) {
            setupMenuWithPresentButton(newState.referral)
        }

        renderTabs(newState.tabs, newState.currentTab)
        renderMode(newState.walletMode)
    }

    private fun middleButtonBottomSheetLaunch(walletMode: WalletMode): BottomSheetDialogFragment = when (walletMode) {
        WalletMode.CUSTODIAL_ONLY,
        WalletMode.NON_CUSTODIAL_ONLY -> SuperAppActionsBottomSheet.newInstance(
            walletMode = walletMode,
            isEarnOnNavBarEnabled = isEarnOnNavBarEnabled
        )
        WalletMode.UNIVERSAL -> BrokerageActionsBottomSheet.newInstance(isEarnOnNavBarEnabled)
    }

    private fun renderMode(walletMode: WalletMode) {
        if (walletMode == WalletMode.UNIVERSAL)
            return
        val updatedDropdownIndicator =
            (toolbarBinding.navigationToolbar.startNavigationButton as? NavigationBarButton.DropdownIndicator)?.copy(
                text = getString(walletMode.title()),
                rightIcon = walletMode.icon(),
            )
        updatedDropdownIndicator?.let {
            updateToolbarStartItem(it)
        }
    }

    private fun renderTabs(tabs: List<NavigationItem>, currentTab: NavigationItem) {
        binding.bottomNavigation.apply {
            this.navigationItems = tabs
            this.hasMiddleButton = true
        }

        if (binding.bottomNavigation.selectedNavigationItem != currentTab) {
            binding.bottomNavigation.selectedNavigationItem = currentTab
            currentTab.launch()
        }
    }

    private fun navigateToDeeplinkDestination(destination: Destination) {
        when (destination) {
            is Destination.AssetViewDestination -> {
                destinationArgs.getAssetInfo(destination.networkTicker)?.let { assetInfo ->
                    activityResultsContract.launch(
                        CoinViewActivityV2.newIntent(
                            context = this,
                            asset = assetInfo,
                            recurringBuyId = destination.recurringBuyId,
                            originScreen = LaunchOrigin.DEEPLINK.name
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
                            preselectedAmount = destination.amount,
                            preselectedFiatTicker = destination.fiatTicker
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
            is Destination.ActivityDestination -> startActivitiesFragment()
            is Destination.AssetEnterAmountDestination -> {
                destinationArgs.getAssetInfo(destination.networkTicker)?.let { assetInfo ->
                    startActivity(
                        SimpleBuyActivity.newIntent(
                            context = this,
                            asset = assetInfo
                        )
                    )
                } ?: run {
                    Timber.e("Unable to start SimpleBuyActivity from deeplink. AssetInfo is null")
                }
            }
            is Destination.AssetEnterAmountLinkCardDestination -> {
                destinationArgs.getAssetInfo(destination.networkTicker)?.let { assetInfo ->
                    startActivity(
                        SimpleBuyActivity.newIntent(
                            context = this,
                            asset = assetInfo,
                            launchLinkCard = true
                        )
                    )
                } ?: run {
                    destinationArgs.getFiatAssetInfo(destination.networkTicker)?.let { _ ->
                        goToSettings(SettingsDestination.CardLinking)
                    } ?: Timber.e(
                        "Unable to start CardLinking from deeplink. Ticker not found ${destination.networkTicker}"
                    )
                }
            }
            is Destination.AssetEnterAmountNewMethodDestination -> {
                destinationArgs.getAssetInfo(destination.networkTicker)?.let { assetInfo ->
                    startActivity(
                        SimpleBuyActivity.newIntent(
                            context = this,
                            asset = assetInfo,
                            launchNewPaymentMethodSelection = true
                        )
                    )
                } ?: run {
                    Timber.e(
                        "Unable to start SimpleBuyActivity from deeplink. Ticker not found ${destination.networkTicker}"
                    )
                }
            }
            Destination.CustomerSupportDestination -> startActivity(SupportCentreActivity.newIntent(this))
            Destination.StartKycDestination ->
                startActivity(KycNavHostActivity.newIntent(this, CampaignType.None))
            Destination.ReferralDestination -> model.process(MainIntent.ShowReferralWhenAvailable)
            is Destination.DashboardDestination -> launchPortfolio(reload = true)
            is Destination.WalletConnectDestination -> model.process(MainIntent.StartWCSession(destination.url))
            is Destination.AssetReceiveDestination -> launchReceive(destination.networkTicker)
            is Destination.AssetSellDestination ->
                model.process(
                    MainIntent.LaunchTransactionFlowFromDeepLink(
                        networkTicker = destination.networkTicker,
                        action = AssetAction.Sell
                    )
                )
            is Destination.AssetSwapDestination ->
                model.process(
                    MainIntent.LaunchTransactionFlowFromDeepLink(
                        networkTicker = destination.networkTicker,
                        action = AssetAction.Swap
                    )
                )
            is Destination.RewardsDepositDestination ->
                model.process(
                    MainIntent.LaunchTransactionFlowFromDeepLink(
                        networkTicker = destination.networkTicker,
                        action = AssetAction.InterestDeposit
                    )
                )
            is Destination.RewardsSummaryDestination -> {
                model.process(
                    MainIntent.SelectRewardsAccountForAsset(
                        cryptoTicker = destination.networkTicker
                    )
                )
            }
            is Destination.FiatDepositDestination -> {
                model.process(
                    MainIntent.LaunchTransactionFlowFromDeepLink(
                        networkTicker = destination.fiatTicker,
                        action = AssetAction.FiatDeposit
                    )
                )
            }
            Destination.SettingsAddCardDestination -> goToSettings(SettingsDestination.CardLinking)
            Destination.SettingsAddBankDestination -> goToSettings(SettingsDestination.BankLinking)
        }.exhaustive

        model.process(MainIntent.ClearDeepLinkResult)
    }

    private fun goToSettings(destination: SettingsDestination = SettingsDestination.Home) =
        startActivity(SettingsActivity.newIntent(this, destination))

    private fun launchWalletConnectSessionSelectNetwork(walletConnectSession: WalletConnectSession) {
        showBottomSheet(
            SelectNetworkBottomSheet.newInstance(walletConnectSession)
        )
    }

    private fun launchWalletConnectSessionApproval(walletConnectSession: WalletConnectSession) {
        showBottomSheet(
            WCApproveSessionBottomSheet.newInstance(walletConnectSession)
        )
    }

    private fun launchWalletConnectSessionApprovalWithNetwork(
        walletConnectSession: WalletConnectSession,
        networkInfo: NetworkInfo
    ) {
        showBottomSheet(
            WCApproveSessionBottomSheet.newInstance(
                walletConnectSession,
                networkInfo
            )
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
                    QrScanError.ErrorCode.ScanUnrecognized -> R.string.error_scan_unrecognized
                    QrScanError.ErrorCode.ScanFailed -> R.string.error_scan_failed_general
                    QrScanError.ErrorCode.BitPayScanFailed -> R.string.error_scan_failed_bitpay
                }
            ),
            type = SnackbarType.Error
        ).show()
    }

    private fun launchAssetAction(
        action: AssetAction,
        account: BlockchainAccount?,
    ) = when (action) {
        AssetAction.Receive -> startActivity(ReceiveDetailActivity.newIntent(this, account as CryptoAccount))
        AssetAction.Swap -> launchSwap(sourceAccount = account as CryptoAccount)
        AssetAction.ViewActivity -> startActivitiesFragment(account)
        else -> {
            // do nothing
        }
    }

    private fun startActivitiesFragment(
        account: BlockchainAccount? = null,
        reload: Boolean = false,
    ) {
        homeToolbarTitle()
        updateSelectedNavigationItem(NavigationItem.Activity)
        supportFragmentManager.showFragment(
            fragment = ActivitiesFragment.newInstance(account),
            reloadFragment = reload
        )
        analytics.logEvent(transactionsShown(account?.label ?: "All Wallets"))
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
            launchBuySell(BuySellViewType.TYPE_BUY)
        })
    }

    override fun dismiss() {
        hideUiTour()
    }

    override fun startKycClicked() {
        launchKyc(CampaignType.None)
    }

    private fun showUiTourIfCustodial() {
        walletModeService.walletModeSingle.doOnSuccess {
            if (it == WalletMode.CUSTODIAL_ONLY) {
                analytics.logEvent(UiTourAnalytics.Viewed)
                binding.uiTour.apply {
                    host = this@MainActivity
                    alpha = 0f
                    bringToFront()
                    visible()
                    animate()
                        .alpha(1f)
                        .setStartDelay(500L).duration =
                        resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()
                }
            }
        }.emptySubscribe()
    }

    private fun hideUiTour(onAnimationEnd: (() -> Unit)? = null) {
        binding.uiTour.apply {
            logHideUi(this@MainActivity)

            animate()
                .alpha(0f)
                .setDuration(resources.getInteger(android.R.integer.config_shortAnimTime).toLong())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        gone()
                        onAnimationEnd?.invoke()
                    }
                })
        }
    }

    private fun updateSelectedNavigationItem(navigationItem: NavigationItem) {
        model.process(MainIntent.UpdateCurrentTab(navigationItem))
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

    override fun logout() {
        analytics.logEvent(AnalyticsEvents.Logout)
        model.process(MainIntent.UnpairWallet)
        if (AndroidUtils.is25orHigher()) {
            getSystemService(ShortcutManager::class.java).removeAllDynamicShortcuts()
        }
    }

    override fun onSelectNetworkClicked(session: WalletConnectSession) {
        model.process(
            MainIntent.UpdateViewToLaunch(ViewToLaunch.LaunchWalletConnectSessionNetworkSelection(session))
        )
    }

    override fun onNetworkSelected(session: WalletConnectSession, networkInfo: NetworkInfo) {
        model.process(
            MainIntent.UpdateViewToLaunch(
                ViewToLaunch.LaunchWalletConnectSessionApprovalWithNetwork(
                    session,
                    networkInfo
                )
            )
        )
    }

    override fun onSessionApproved(session: WalletConnectSession) {
        model.process(MainIntent.ApproveWCSession(session))
        analytics.logEvent(
            WalletConnectAnalytics.DappConnectionConfirmed
        )
    }

    override fun onSessionRejected(session: WalletConnectSession) {
        model.process(MainIntent.RejectWCSession(session))
        analytics.logEvent(
            WalletConnectAnalytics.DappConnectionRejected
        )
    }

    override fun onCameraAccessAllowed() {
        launchQrScan()
    }

    override fun goToTrading() {
        analytics.logEvent(BuyDefiAnalyticsEvents.SwitchedToTrading)
        model.process(MainIntent.SwitchWalletMode(WalletMode.CUSTODIAL_ONLY))
        startBuy()
    }

    override fun goToActivityFor(account: BlockchainAccount) {
        startActivitiesFragment(account)
    }

    override fun goToInterestDeposit(toAccount: BlockchainAccount) {
        model.process(
            MainIntent.UpdateViewToLaunch(
                ViewToLaunch.LaunchTxFlowWithAccountForAction(
                    LaunchFlowForAccount.TargetAccount(toAccount as TransactionTarget), AssetAction.InterestDeposit
                )
            )
        )
    }

    override fun goToInterestWithdraw(fromAccount: BlockchainAccount) {
        model.process(
            MainIntent.UpdateViewToLaunch(
                ViewToLaunch.LaunchTxFlowWithAccountForAction(
                    LaunchFlowForAccount.SourceAccount(fromAccount), AssetAction.InterestWithdraw
                )
            )
        )
    }

    override fun launchStakingWithdrawal(account: StakingAccount) {
        // TODO(dserrano) - STAKING - not yet implemented
    }

    override fun launchStakingDeposit(account: StakingAccount) {
        model.process(
            MainIntent.SelectStakingAccountForAction(account, AssetAction.StakingDeposit)
        )
    }

    override fun goToStakingActivity(account: StakingAccount) {
        model.process(
            MainIntent.SelectStakingAccountForAction(account, AssetAction.ViewActivity)
        )
    }

    override fun onSheetClosed() {
        Timber.d("On closed")
        binding.bottomNavigation.bottomNavigationState = BottomNavigationState.Add
    }

    override fun navigateToBottomSheet(bottomSheet: BottomSheetDialogFragment) {
        clearBottomSheet()
        showBottomSheet(bottomSheet)
    }

    private fun launchPortfolio(
        action: AssetAction? = null,
        fiatCurrency: String? = null,
        reload: Boolean = false,
    ) {
        homeToolbarTitle()
        updateSelectedNavigationItem(NavigationItem.Home)

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
        targetAccount: CryptoAccount?,
    ) {
        if (sourceAccount == null && targetAccount == null) {
            actionsResultContract.launch(ActionActivity.ActivityArgs(AssetAction.Swap, null))
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

    override fun launchBackupFunds(fragment: Fragment?, requestCode: Int) {
        fragment?.let {
            BackupWalletActivity.startForResult(it, requestCode)
        } ?: BackupWalletActivity.start(this)
    }

    override fun launchSetup2Fa() {
        goToSettings(destination = SettingsDestination.Security)
    }

    override fun launchOpenExternalEmailApp() {
        Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_EMAIL)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(Intent.createChooser(this, getString(R.string.security_centre_email_check)))
        }
    }

    override fun launchSetupFingerprintLogin() {
        OnboardingActivity.launchForFingerprints(this)
    }

    override fun launchReceive(cryptoTicker: String?) {
        actionsResultContract.launch(ActionActivity.ActivityArgs(AssetAction.Receive, cryptoTicker = cryptoTicker))
    }

    override fun launchSend() {
        actionsResultContract.launch(ActionActivity.ActivityArgs(AssetAction.Send))
    }

    override fun launchBuy() {
        launchBuySell(BuySellViewType.TYPE_BUY)
    }

    override fun launchBuyForDefi() {
        analytics.logEvent(BuyDefiAnalyticsEvents.BuySelected)
        showBottomSheet(BuyDefiBottomSheet.newInstance())
    }

    override fun launchSell() {
        launchBuySell(BuySellViewType.TYPE_SELL)
    }

    override fun launchBuySell(
        viewType: BuySellViewType,
        asset: AssetInfo?,
        reload: Boolean,
    ) {
        homeToolbarTitle()

        if (isEarnOnNavBarEnabled) {
            actionsResultContract.launch(
                ActionActivity.ActivityArgs(
                    action = when (viewType) {
                        BuySellViewType.TYPE_BUY -> AssetAction.Buy
                        BuySellViewType.TYPE_SELL -> AssetAction.Sell
                    },
                    asset?.networkTicker
                )
            )
        } else {
            updateSelectedNavigationItem(NavigationItem.BuyAndSell)
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
    }

    private fun homeToolbarTitle() {
        updateToolbarTitle(title = "")
    }

    private fun launchPrices(reload: Boolean = false) {
        updateSelectedNavigationItem(NavigationItem.Prices)
        homeToolbarTitle()
        supportFragmentManager.showFragment(
            fragment = PricesFragment.newInstance(),
            reloadFragment = reload
        )
    }

    override fun launchSimpleBuy(asset: AssetInfo, paymentMethodId: String?) {
        startActivity(
            SimpleBuyActivity.newIntent(
                context = this,
                launchFromNavigationBar = true,
                preselectedPaymentMethodId = paymentMethodId,
                asset = asset
            )
        )
    }

    override fun launchInterestDashboard(origin: LaunchOrigin) {
        startActivityForResult(
            InterestDashboardActivity.newInstance(this), INTEREST_DASHBOARD
        )
        analytics.logEvent(EarnAnalytics.InterestClicked(origin = LaunchOrigin.DASHBOARD_PROMO))
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

    override fun showReceiveSheet(account: BlockchainAccount) {
        launchAssetAction(AssetAction.Receive, account)
    }

    override fun showNftDetail(nftId: String, pageKey: String?, address: String) {
        replaceBottomSheet(NftDetailFragment.newInstance(nftId = nftId, pageKey = pageKey, address = address))
    }

    override fun showNftHelp() {
        replaceBottomSheet(NftHelpFragment.newInstance())
    }

    override fun openExternalUrl(url: String) {
        openUrl(url)
    }

    companion object {
        private const val START_UI_TOUR_KEY = "START_UI_TOUR_KEY"
        private const val SHOW_SWAP = "SHOW_SWAP"
        private const val LAUNCH_AUTH_FLOW = "LAUNCH_AUTH_FLOW"
        private const val INTENT_FROM_NOTIFICATION = "INTENT_FROM_NOTIFICATION"
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
            message: SecureChannelBrowserMessageArg,
            originIp: String?,
            originLocation: String?,
            originBrowser: String?,
            forcePin: Boolean,
            shouldBeNewTask: Boolean,
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
            notificationAnalyticsPayload: Map<String, String>? = null,
        ): Intent =
            Intent(context, MainActivity::class.java).apply {
                putExtra(INTENT_FROM_NOTIFICATION, intentFromNotification)
                notificationAnalyticsPayload?.keys?.forEach { key ->
                    notificationAnalyticsPayload[key]?.let { value ->
                        putExtra(key, value)
                    }
                }
            }

        fun newIntent(context: Context, pendingDestination: Destination): Intent =
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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
            shouldBeNewTask: Boolean,
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

    override fun route(navigationEvent: PricesNavigationEvent) {
        when (navigationEvent) {
            is PricesNavigationEvent.CoinView -> {
                activityResultsContract.launch(
                    CoinViewActivityV2.newIntent(
                        context = this,
                        asset = navigationEvent.assetInfo,
                        originScreen = LaunchOrigin.PRICES.name,
                    )
                )
            }
        }.exhaustive
    }
}
