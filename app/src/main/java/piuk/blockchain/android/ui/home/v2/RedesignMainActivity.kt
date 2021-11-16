package piuk.blockchain.android.ui.home.v2

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutManager
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.NullCryptoAccount
import com.blockchain.componentlib.navigation.BottomNavigationState
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
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.databinding.ActivityRedesignMainBinding
import piuk.blockchain.android.databinding.ToolbarGeneralBinding
import piuk.blockchain.android.simplebuy.SimpleBuyActivity
import piuk.blockchain.android.simplebuy.SmallSimpleBuyNavigator
import piuk.blockchain.android.ui.FeatureFlagsHandlingActivity
import piuk.blockchain.android.ui.activity.ActivitiesFragment
import piuk.blockchain.android.ui.auth.AccountWalletLinkAlertSheet
import piuk.blockchain.android.ui.auth.newlogin.AuthNewLoginSheet
import piuk.blockchain.android.ui.backup.BackupWalletActivity
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.ui.base.mvi.MviActivity
import piuk.blockchain.android.ui.base.showFragment
import piuk.blockchain.android.ui.customviews.ToastCustom
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
import piuk.blockchain.android.ui.sell.BuySellFragment
import piuk.blockchain.android.ui.settings.SettingsActivity
import piuk.blockchain.android.ui.thepit.PitLaunchBottomDialog
import piuk.blockchain.android.ui.thepit.PitPermissionsActivity
import piuk.blockchain.android.ui.transactionflow.analytics.InterestAnalytics
import piuk.blockchain.android.ui.transactionflow.flow.TransactionFlowActivity
import piuk.blockchain.android.ui.transfer.receive.detail.ReceiveDetailSheet
import piuk.blockchain.android.ui.upsell.KycUpgradePromptManager
import piuk.blockchain.android.ui.upsell.UpsellHost
import piuk.blockchain.android.util.AndroidUtils
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

    private val toolbar: Toolbar by lazy {
        ToolbarGeneralBinding.bind(binding.root).toolbarGeneral
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        launchDashboard()
        setupNavigation()

        if (intent.hasExtra(INTENT_FROM_NOTIFICATION) &&
            intent.getBooleanExtra(INTENT_FROM_NOTIFICATION, false)
        ) {
            analytics.logEvent(NotificationAppOpened)
        }

        if (savedInstanceState == null) {
            model.process(RedesignIntent.CheckForPendingLinks(intent))
        }

        with(toolbar) {
            title = ""
            setSupportActionBar(this)
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

    private fun setupNavigation() {
        binding.bottomNavigation.apply {
            onNavigationItemClick = {
                selectedNavigationItem = it
                supportFragmentManager.showFragment(
                    fragment = when (it) {
                        NavigationItem.Home -> {
                            PortfolioFragment.newInstance(true)
                        }
                        NavigationItem.Prices -> {
                            PricesFragment.newInstance()
                        }
                        NavigationItem.BuyAndSell -> {
                            BuySellFragment.newInstance()
                        }
                        NavigationItem.Activity -> {
                            ActivitiesFragment.newInstance()
                        }
                        else -> throw IllegalStateException("Illegal navigation state - unknown item $it")
                    },
                    loadingView = binding.progress
                )
            }
            onMiddleButtonClick = {
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_redesign_main_activity, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_qr_main -> {
                QrScanActivity.start(this, QrExpected.MAIN_ACTIVITY_QR)
                analytics.logEvent(SendAnalytics.QRButtonClicked)
                true
            }
            R.id.action_account_main -> {
                // TODO remove this - stopgap to access flags on this activity
                startActivity(Intent(this, FeatureFlagsHandlingActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

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
            is ViewToLaunch.LaunchOpenBankingApprovalError -> {
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
            ViewToLaunch.LaunchOpenBankingBuyApprovalError -> {
                ToastCustom.makeText(
                    this, getString(R.string.simple_buy_confirmation_error), Toast.LENGTH_LONG, ToastCustom.TYPE_ERROR
                )
            }
            is ViewToLaunch.LaunchOpenBankingDepositError -> {
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
            ViewToLaunch.LaunchReceive -> {
                launchReceive()
            }
            ViewToLaunch.LaunchSend -> {
                launchSend()
            }
            ViewToLaunch.LaunchSetupBiometricLogin -> {
                launchSetupFingerprintLogin()
            }
            is ViewToLaunch.LaunchSimpleBuy -> launchSimpleBuy(view.asset)
            ViewToLaunch.LaunchSimpleBuyFromDeepLinkApproval -> launchSimpleBuyFromDeepLinkApproval()
            ViewToLaunch.LaunchSwap -> launchSwap()
            ViewToLaunch.LaunchTwoFaSetup -> launchSetup2Fa()
            ViewToLaunch.LaunchVerifyEmail -> launchVerifyEmail()
            ViewToLaunch.ShowOpenBankingError ->
                ToastCustom.makeText(
                    this, getString(R.string.open_banking_deeplink_error), Toast.LENGTH_LONG, ToastCustom.TYPE_ERROR
                )
            ViewToLaunch.None -> {
                // do nothing
            }
        }
    }

    private fun launchAssetAction(
        action: AssetAction,
        account: BlockchainAccount?
    ) = when (action) {
        AssetAction.Receive -> replaceBottomSheet(ReceiveDetailSheet.newInstance(account as CryptoAccount))
        AssetAction.Swap -> launchSwap(sourceAccount = account as CryptoAccount)
        AssetAction.ViewActivity -> startActivitiesFragment(account)
        else -> {
        }
    }

    private fun startActivitiesFragment(account: BlockchainAccount? = null, reload: Boolean = true) {
        supportFragmentManager.showFragment(
            fragment = ActivitiesFragment.newInstance(account),
            loadingView = binding.progress
        )
        binding.bottomNavigation.selectedNavigationItem = NavigationItem.Activity
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

    override fun launchDashboard() {
        binding.bottomNavigation.selectedNavigationItem = NavigationItem.Home
        supportFragmentManager.showFragment(
            fragment = PortfolioFragment.newInstance(true),
            loadingView = binding.progress
        )
    }

    override fun launchSwap(sourceAccount: CryptoAccount?, targetAccount: CryptoAccount?) {
        if (sourceAccount == null && targetAccount == null) {
            toolbar.title = getString(R.string.common_swap)
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
        supportFragmentManager.showFragment(
            fragment = BuySellFragment.newInstance(
                viewType = viewType,
                asset = asset
            ),
            loadingView = binding.progress
        )
        binding.bottomNavigation.selectedNavigationItem = NavigationItem.BuyAndSell
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
        supportFragmentManager.showFragment(
            fragment = PortfolioFragment.newInstance(true, AssetAction.FiatDeposit, currency),
            loadingView = binding.progress
        )
    }

    override fun launchTransfer() {
        // delete
    }

    override fun launchOpenBankingLinking(bankLinkingInfo: BankLinkingInfo) {
        startActivityForResult(
            BankAuthActivity.newInstance(bankLinkingInfo.linkingId, bankLinkingInfo.bankAuthSource, this),
            when (bankLinkingInfo.bankAuthSource) {
                BankAuthSource.SIMPLE_BUY -> MainActivity.BANK_DEEP_LINK_SIMPLE_BUY
                BankAuthSource.SETTINGS -> MainActivity.BANK_DEEP_LINK_SETTINGS
                BankAuthSource.DEPOSIT -> MainActivity.BANK_DEEP_LINK_DEPOSIT
                BankAuthSource.WITHDRAW -> MainActivity.BANK_DEEP_LINK_WITHDRAW
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
        // TODO model.process()
        // presenter.validateAccountAction(action, account)
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

        fun newInstance(context: Context, shouldShowSwap: Boolean, shouldBeNewTask: Boolean): Intent =
            Intent(context, RedesignMainActivity::class.java).apply {
                putExtra(SHOW_SWAP, shouldShowSwap)
                if (shouldBeNewTask) {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                context.startActivity(this)
            }

        fun newInstance(
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

        fun newInstance(context: Context, intentFromNotification: Boolean): Intent =
            Intent(context, RedesignMainActivity::class.java).apply {
                putExtra(INTENT_FROM_NOTIFICATION, intentFromNotification)
            }

        fun newInstanceAsNewTask(context: Context): Intent =
            Intent(context, RedesignMainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            }

        fun newInstance(
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
