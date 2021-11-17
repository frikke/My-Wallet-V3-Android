package piuk.blockchain.android.ui.home.v2

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.CryptoTarget
import com.blockchain.coincore.NullCryptoAccount
import com.blockchain.componentlib.navigation.BottomNavigationState
import com.blockchain.componentlib.navigation.NavigationBarButton
import com.blockchain.componentlib.navigation.NavigationItem
import com.blockchain.extensions.exhaustive
import com.blockchain.koin.scopedInject
import com.blockchain.notifications.analytics.AnalyticsEvents
import com.blockchain.notifications.analytics.LaunchOrigin
import com.blockchain.notifications.analytics.NotificationAppOpened
import com.blockchain.notifications.analytics.SendAnalytics
import com.blockchain.notifications.analytics.activityShown
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import java.net.URLDecoder
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.databinding.ActivityRedesignMainBinding
import piuk.blockchain.android.scan.QrScanError
import piuk.blockchain.android.scan.QrScanResultProcessor
import piuk.blockchain.android.simplebuy.SimpleBuyActivity
import piuk.blockchain.android.simplebuy.SmallSimpleBuyNavigator
import piuk.blockchain.android.ui.activity.ActivitiesFragment
import piuk.blockchain.android.ui.addresses.AccountActivity
import piuk.blockchain.android.ui.airdrops.AirdropCentreActivity
import piuk.blockchain.android.ui.auth.AccountWalletLinkAlertSheet
import piuk.blockchain.android.ui.auth.newlogin.AuthNewLoginSheet
import piuk.blockchain.android.ui.backup.BackupWalletActivity
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.ui.base.mvi.MviActivity
import piuk.blockchain.android.ui.base.showFragment
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.customviews.toast
import piuk.blockchain.android.ui.dashboard.PortfolioFragment
import piuk.blockchain.android.ui.dashboard.PricesFragment
import piuk.blockchain.android.ui.home.HomeNavigator
import piuk.blockchain.android.ui.home.MainActivity
import piuk.blockchain.android.ui.interest.InterestDashboardActivity
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity
import piuk.blockchain.android.ui.kyc.status.KycStatusActivity
import piuk.blockchain.android.ui.linkbank.BankAuthActivity
import piuk.blockchain.android.ui.linkbank.BankAuthSource
import piuk.blockchain.android.ui.linkbank.BankLinkingInfo
import piuk.blockchain.android.ui.linkbank.FiatTransactionState
import piuk.blockchain.android.ui.linkbank.yapily.FiatTransactionBottomSheet
import piuk.blockchain.android.ui.onboarding.OnboardingActivity
import piuk.blockchain.android.ui.scan.QrExpected
import piuk.blockchain.android.ui.scan.QrScanActivity
import piuk.blockchain.android.ui.scan.QrScanActivity.Companion.getRawScanData
import piuk.blockchain.android.ui.sell.BuySellFragment
import piuk.blockchain.android.ui.settings.SettingsActivity
import piuk.blockchain.android.ui.settings.v2.RedesignSettingsActivity
import piuk.blockchain.android.ui.thepit.PitLaunchBottomDialog
import piuk.blockchain.android.ui.thepit.PitPermissionsActivity
import piuk.blockchain.android.ui.transactionflow.analytics.InterestAnalytics
import piuk.blockchain.android.ui.transactionflow.flow.TransactionFlowActivity
import piuk.blockchain.android.ui.transfer.receive.detail.ReceiveDetailSheet
import piuk.blockchain.android.ui.upsell.KycUpgradePromptManager
import piuk.blockchain.android.ui.upsell.UpsellHost
import piuk.blockchain.android.util.AndroidUtils
import piuk.blockchain.android.util.getAccount
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.visible
import timber.log.Timber

class RedesignMainActivity :
    MviActivity<RedesignModel, RedesignIntent, RedesignState, ActivityRedesignMainBinding>(),
    HomeNavigator,
    SlidingModalBottomDialog.Host,
    UpsellHost,
    AuthNewLoginSheet.Host,
    AccountWalletLinkAlertSheet.Host,
    RedesignActionsBottomSheet.Host,
    SmallSimpleBuyNavigator {

    override val alwaysDisableScreenshots: Boolean
        get() = false

    override val model: RedesignModel by scopedInject()

    override fun initBinding(): ActivityRedesignMainBinding = ActivityRedesignMainBinding.inflate(layoutInflater)

    private var activityResultAction: () -> Unit = {}
    private var handlingResult = false

    @Deprecated("Use MVI loop instead")
    private val compositeDisposable = CompositeDisposable()

    @Deprecated("Use MVI loop instead")
    private val qrProcessor: QrScanResultProcessor by scopedInject()

    private val settingsResultContract = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            (
                it.data?.getSerializableExtra(RedesignSettingsActivity.SETTINGS_RESULT_DATA)
                    as? RedesignSettingsActivity.Companion.SettingsAction
                )?.let { action ->
                startSettingsAction(action)
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

        if (savedInstanceState == null) {
            model.process(RedesignIntent.CheckForPendingLinks(intent))
        }

        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(false)
            it.setHomeButtonEnabled(false)
        }

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
        }
        model.process(RedesignIntent.PerformInitialChecks)
    }

    override fun onResume() {
        super.onResume()
        activityResultAction().also {
            activityResultAction = {}
        }

        model.process(RedesignIntent.CancelAnyPendingConfirmationBuy)

        handlingResult = false
    }

    override fun onDestroy() {
        compositeDisposable.clear()
        // stopgap to be able to clear separate calls on Rx on the model
        model.clearDisposables()
        super.onDestroy()
    }

    private fun startSettingsAction(action: RedesignSettingsActivity.Companion.SettingsAction) {
        when (action) {
            RedesignSettingsActivity.Companion.SettingsAction.Addresses ->
                startActivityForResult(AccountActivity.newIntent(this), ACCOUNT_EDIT)
            RedesignSettingsActivity.Companion.SettingsAction.Exchange ->
                model.process(RedesignIntent.LaunchExchange)
            RedesignSettingsActivity.Companion.SettingsAction.Airdrops ->
                AirdropCentreActivity.start(this)
            RedesignSettingsActivity.Companion.SettingsAction.WebLogin ->
                QrScanActivity.start(this, QrExpected.MAIN_ACTIVITY_QR)
            RedesignSettingsActivity.Companion.SettingsAction.Logout -> showLogoutDialog()
        }
    }

    private fun setupToolbar() {
        binding.mainToolbar.apply {
            navigationBarButtons = listOf(
                NavigationBarButton.Icon(R.drawable.ic_qr_scan) {
                    launchQrScan()
                },
                NavigationBarButton.Icon(R.drawable.ic_bank_user) {
                    settingsResultContract.launch(RedesignSettingsActivity.newIntent(this@RedesignMainActivity))
                }
            )
        }
    }

    private fun updateToolbarTitle(title: String) {
        binding.mainToolbar.title = title
    }

    private fun launchQrScan() {
        QrScanActivity.start(this, QrExpected.MAIN_ACTIVITY_QR)
        analytics.logEvent(SendAnalytics.QRButtonClicked)
    }

    private fun setupNavigation() {
        binding.bottomNavigation.apply {
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
                showBottomSheet(
                    RedesignActionsBottomSheet.newInstance()
                )
            }
        }
    }

    // TODO in these methods in MainActivity, we show a dialog, check whether it should be blocking or not
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
        handlingResult = true
        // We create a lambda so we handle the result after the view is attached to the presenter (onResume)
        activityResultAction = {
            when (requestCode) {
                QrScanActivity.SCAN_URI_RESULT -> {
                    data.getRawScanData()?.let {
                        val decodedData = URLDecoder.decode(it, "UTF-8")
                        if (resultCode == RESULT_OK) {
                            model.process(RedesignIntent.ProcessScanResult(decodedData))
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
                            SimpleBuyActivity.newInstance(
                                context = this,
                                preselectedPaymentMethodId = data?.getStringExtra(BankAuthActivity.LINKED_BANK_ID_KEY)
                            )
                        )
                    }
                }
                BANK_DEEP_LINK_SETTINGS -> {
                    if (resultCode == RESULT_OK) {
                        startActivity(Intent(this, SettingsActivity::class.java))
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
    }

    private fun startTxFlowWithTargets(targets: Collection<CryptoTarget>) {
        if (targets.size > 1) {
            disambiguateSendScan(targets)
        } else {
            val targetAddress = targets.first()
            // FIXME selecting a source account shows UI, refactor so this can be called from the interactor
            compositeDisposable += qrProcessor.selectSourceAccount(this, targetAddress)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = { sourceAccount ->
                        startActivity(
                            TransactionFlowActivity.newInstance(
                                context = this,
                                sourceAccount = sourceAccount,
                                target = targetAddress,
                                action = AssetAction.Send
                            )
                        )
                    },
                    onComplete = {
                        Timber.d("No source accounts available for scan target")
                        showNoAccountFromScanToast(targetAddress.asset)
                    },
                    onError = {
                        Timber.e("Unable to select source account for scan")
                        showNoAccountFromScanToast(targetAddress.asset)
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

    private fun showNoAccountFromScanToast(asset: AssetInfo) =
        toast(getString(R.string.scan_no_available_account, asset.displayTicker))

    override fun render(newState: RedesignState) {
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
                this, view.campaignType, MainActivity.KYC_STARTED
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
                ToastCustom.makeText(
                    this, getString(R.string.simple_buy_confirmation_error), Toast.LENGTH_LONG, ToastCustom.TYPE_ERROR
                )
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
                val currencyCode = view.state.fiatCurrency
                replaceBottomSheet(
                    FiatTransactionBottomSheet.newInstance(
                        currencyCode, getString(R.string.yapily_payment_to_fiat_wallet_title, currencyCode),
                        getString(
                            R.string.yapily_payment_to_fiat_wallet_subtitle,
                            view.state.selectedCryptoAsset?.displayTicker ?: getString(
                                R.string.yapily_payment_to_fiat_wallet_default
                            ),
                            currencyCode
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
                ToastCustom.makeText(
                    context = this,
                    msg = getString(R.string.open_banking_deeplink_error),
                    duration = Toast.LENGTH_LONG,
                    type = ToastCustom.TYPE_ERROR
                )
            is ViewToLaunch.CheckForAccountWalletLinkErrors -> showBottomSheet(
                AccountWalletLinkAlertSheet.newInstance(view.walletIdHint)
            )
            is ViewToLaunch.LaunchTransactionFlowWithTargets -> startTxFlowWithTargets(view.targets)
            is ViewToLaunch.ShowTargetScanError -> showTargetScanError(view.error)
            is ViewToLaunch.None -> {
                // do nothing
            }
        }.exhaustive
    }

    private fun showTargetScanError(error: QrScanError) {
        ToastCustom.makeText(
            this,
            getString(
                when (error.errorCode) {
                    QrScanError.ErrorCode.ScanFailed -> R.string.error_scan_failed_general
                    QrScanError.ErrorCode.BitPayScanFailed -> R.string.error_scan_failed_bitpay
                }
            ),
            ToastCustom.LENGTH_LONG,
            ToastCustom.TYPE_ERROR
        )
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

    private fun startActivitiesFragment(account: BlockchainAccount? = null, reload: Boolean = true) {
        updateToolbarTitle(title = getString(R.string.main_toolbar_activity))
        binding.bottomNavigation.selectedNavigationItem = NavigationItem.Activity

        supportFragmentManager.showFragment(
            fragment = ActivitiesFragment.newInstance(account),
            loadingView = binding.progress
        )

        analytics.logEvent(activityShown(account?.label ?: "All Wallets"))
    }

    override fun exitSimpleBuyFlow() {
        launchBuySell()
    }

    override fun logout() {
        analytics.logEvent(AnalyticsEvents.Logout)
        model.process(RedesignIntent.UnpairWallet)
        if (AndroidUtils.is25orHigher()) {
            getSystemService(ShortcutManager::class.java).removeAllDynamicShortcuts()
        }
    }

    override fun startUpsellKyc() {
        launchKyc(CampaignType.None)
    }

    override fun onSheetClosed() {
        binding.bottomNavigation.bottomNavigationState = BottomNavigationState.Add
        Timber.d("On closed")
    }

    override fun navigateToBottomSheet(bottomSheet: BottomSheetDialogFragment) {
        clearBottomSheet()
        showBottomSheet(bottomSheet)
    }

    private fun launchPortfolio(action: AssetAction? = null, fiatCurrency: String? = null) {
        updateToolbarTitle(title = getString(R.string.main_toolbar_home))
        binding.bottomNavigation.selectedNavigationItem = NavigationItem.Home
        supportFragmentManager.showFragment(
            fragment = PortfolioFragment.newInstance(true, action, fiatCurrency),
            loadingView = binding.progress
        )
    }

    override fun launchSwap(sourceAccount: CryptoAccount?, targetAccount: CryptoAccount?) {
        if (sourceAccount == null && targetAccount == null) {
            ActionActivity.start(this, AssetAction.Swap)
        } else if (sourceAccount != null) {
            startActivity(
                TransactionFlowActivity.newInstance(
                    context = this,
                    sourceAccount = sourceAccount,
                    target = targetAccount ?: NullCryptoAccount(),
                    action = AssetAction.Swap
                )
            )
        }
    }

    override fun launchKyc(campaignType: CampaignType) {
        KycNavHostActivity.startForResult(this, campaignType, MainActivity.KYC_STARTED)
    }

    override fun launchThePitLinking(linkId: String) {
        PitPermissionsActivity.start(this, linkId)
    }

    override fun launchThePit() {
        PitLaunchBottomDialog.launch(this)
    }

    override fun launchBackupFunds(fragment: Fragment?, requestCode: Int) {
        fragment?.let {
            BackupWalletActivity.startForResult(it, requestCode)
        } ?: BackupWalletActivity.start(this)
    }

    override fun launchSetup2Fa() {
        SettingsActivity.startFor2Fa(this)
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

    override fun launchReceive() = ActionActivity.start(this, AssetAction.Receive)

    override fun launchSend() = ActionActivity.start(this, AssetAction.Send)

    override fun launchBuySell(viewType: BuySellFragment.BuySellViewType, asset: AssetInfo?) {
        updateToolbarTitle(title = getString(R.string.main_toolbar_buy_sell))
        binding.bottomNavigation.selectedNavigationItem = NavigationItem.BuyAndSell
        supportFragmentManager.showFragment(
            fragment = BuySellFragment.newInstance(
                viewType = viewType,
                asset = asset
            ),
            loadingView = binding.progress
        )
    }

    private fun launchPrices() {
        updateToolbarTitle(title = getString(R.string.main_toolbar_prices))
        supportFragmentManager.showFragment(
            PricesFragment.newInstance(), loadingView = binding.progress
        )
    }

    override fun launchSimpleBuy(asset: AssetInfo) {
        startActivity(
            SimpleBuyActivity.newInstance(
                context = this,
                launchFromNavigationBar = true,
                asset = asset
            )
        )
    }

    override fun launchInterestDashboard(origin: LaunchOrigin) {
        startActivityForResult(
            InterestDashboardActivity.newInstance(this), MainActivity.INTEREST_DASHBOARD
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
        startActivity(SimpleBuyActivity.newInstance(this, launchFromApprovalDeepLink = true))
    }

    override fun launchPendingVerificationScreen(campaignType: CampaignType) {
        KycStatusActivity.start(this, campaignType)
    }

    override fun performAssetActionFor(action: AssetAction, account: BlockchainAccount) {
        model.process(RedesignIntent.ValidateAccountAction(action, account))
    }

    override fun resumeSimpleBuyKyc() {
        startActivity(
            SimpleBuyActivity.newInstance(
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
        private const val START_BUY_SELL_INTRO_KEY = "START_BUY_SELL_INTRO_KEY"
        private const val SHOW_SWAP = "SHOW_SWAP"
        private const val LAUNCH_AUTH_FLOW = "LAUNCH_AUTH_FLOW"
        private const val INTENT_FROM_NOTIFICATION = "INTENT_FROM_NOTIFICATION"
        const val ACCOUNT_EDIT = 2008
        const val SETTINGS_EDIT = 2009
        const val KYC_STARTED = 2011
        const val INTEREST_DASHBOARD = 2012
        const val BANK_DEEP_LINK_SIMPLE_BUY = 2013
        const val BANK_DEEP_LINK_SETTINGS = 2014
        const val BANK_DEEP_LINK_DEPOSIT = 2015
        const val BANK_DEEP_LINK_WITHDRAW = 2021

        fun newIntent(context: Context, shouldShowSwap: Boolean, shouldBeNewTask: Boolean): Intent =
            Intent(context, RedesignMainActivity::class.java).apply {
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
        ): Intent = Intent(context, RedesignMainActivity::class.java).apply {
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

        fun newIntent(context: Context, intentFromNotification: Boolean): Intent =
            Intent(context, RedesignMainActivity::class.java).apply {
                putExtra(INTENT_FROM_NOTIFICATION, intentFromNotification)
            }

        fun newIntentAsNewTask(context: Context): Intent =
            Intent(context, RedesignMainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            }

        fun newIntent(
            context: Context,
            intentData: String?,
            shouldLaunchBuySellIntro: Boolean,
            shouldBeNewTask: Boolean
        ): Intent = Intent(context, RedesignMainActivity::class.java).apply {
            if (intentData != null) {
                data = Uri.parse(intentData)
            }

            if (shouldBeNewTask) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            putExtra(START_BUY_SELL_INTRO_KEY, shouldLaunchBuySellIntro)
        }
    }
}
