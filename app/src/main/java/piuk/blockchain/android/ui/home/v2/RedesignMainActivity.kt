package piuk.blockchain.android.ui.home.v2

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.NullCryptoAccount
import com.blockchain.componentlib.navigation.NavigationItem
import com.blockchain.koin.scopedInject
import com.blockchain.notifications.analytics.LaunchOrigin
import com.blockchain.notifications.analytics.NotificationAppOpened
import com.blockchain.notifications.analytics.SendAnalytics
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import info.blockchain.balance.AssetInfo
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.databinding.ActivityRedesignMainBinding
import piuk.blockchain.android.databinding.ToolbarGeneralBinding
import piuk.blockchain.android.simplebuy.SmallSimpleBuyNavigator
import piuk.blockchain.android.ui.FeatureFlagsHandlingActivity
import piuk.blockchain.android.ui.activity.ActivitiesFragment
import piuk.blockchain.android.ui.auth.AccountWalletLinkAlertSheet
import piuk.blockchain.android.ui.auth.newlogin.AuthNewLoginSheet
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.ui.base.mvi.MviActivity
import piuk.blockchain.android.ui.dashboard.PortfolioFragment
import piuk.blockchain.android.ui.dashboard.PricesFragment
import piuk.blockchain.android.ui.home.HomeNavigator
import piuk.blockchain.android.ui.home.v2.FragmentTransitions.Companion.showFragment
import piuk.blockchain.android.ui.linkbank.BankLinkingInfo
import piuk.blockchain.android.ui.scan.QrExpected
import piuk.blockchain.android.ui.scan.QrScanActivity
import piuk.blockchain.android.ui.sell.BuySellFragment
import piuk.blockchain.android.ui.swap.SwapFragment
import piuk.blockchain.android.ui.transactionflow.flow.TransactionFlowActivity
import piuk.blockchain.android.ui.upsell.UpsellHost
import timber.log.Timber

class RedesignMainActivity :
    MviActivity<RedesignModel, RedesignIntent, RedesignState, ActivityRedesignMainBinding>(),
    HomeNavigator,
    SlidingModalBottomDialog.Host,
    UpsellHost,
    AuthNewLoginSheet.Host,
    AccountWalletLinkAlertSheet.Host,
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
            startSwapFlow()
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
                showFragment(
                    fragmentManager = supportFragmentManager,
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
            is ViewToLaunch.LaunchAssetAction -> {
                // TODO
            }
            is ViewToLaunch.LaunchBuySell -> {
                // TODO
            }
            is ViewToLaunch.LaunchExchange -> {
                // TODO
            }
            is ViewToLaunch.LaunchInterestDashboard -> {
                // TODO
            }
            is ViewToLaunch.LaunchKyc -> {
                // TODO
            }
            is ViewToLaunch.LaunchOpenBankingApprovalDepositComplete -> {
                // TODO
            }
            is ViewToLaunch.LaunchOpenBankingApprovalDepositInProgress -> {
                // TODO
            }
            is ViewToLaunch.LaunchOpenBankingApprovalError -> {
                // TODO
            }
            is ViewToLaunch.LaunchOpenBankingApprovalTimeout -> {
                // TODO
            }
            ViewToLaunch.LaunchOpenBankingBuyApprovalError -> {
                // TODO
            }
            is ViewToLaunch.LaunchOpenBankingDepositError -> {
                // TODO
            }
            is ViewToLaunch.LaunchOpenBankingLinking -> {
                // TODO
            }
            is ViewToLaunch.LaunchPaymentForCancelledOrder -> {
                // TODO
            }
            ViewToLaunch.LaunchReceive -> {
                // TODO
            }
            ViewToLaunch.LaunchSend -> {
                // TODO
            }
            ViewToLaunch.LaunchSetupBiometricLogin -> {
                // TODO
            }
            is ViewToLaunch.LaunchSimpleBuy -> {
                // TODO
            }
            ViewToLaunch.LaunchSimpleBuyFromDeepLinkApproval -> {
                // TODO
            }
            ViewToLaunch.LaunchSwap -> {
                // TODO
            }
            ViewToLaunch.LaunchTwoFaSetup -> {
                // TODO
            }
            ViewToLaunch.LaunchVerifyEmail -> {
                // TODO
            }
            ViewToLaunch.ShowOpenBankingError -> {
                // TODO
            }
            ViewToLaunch.None -> {
                // do nothing
            }
        }
    }

    override fun exitSimpleBuyFlow() {
        // TODO not yet implemented
    }

    override fun logout() {
        // TODO not yet implemented
    }

    override fun startUpsellKyc() {
        // TODO not yet implemented
    }

    override fun onSheetClosed() {
        // un rotate
        // binding.bottomNavigation.onMiddleButtonClick.
        Timber.d("On closed")
    }

    override fun navigateToBottomSheet(bottomSheet: BottomSheetDialogFragment) {
        // TODO not yet implemented
    }

    override fun launchDashboard() {
        // TODO not yet implemented
    }

    override fun launchSwap(sourceAccount: CryptoAccount?, targetAccount: CryptoAccount?) {
        // TODO not yet implemented
    }

    private fun startSwapFlow(
        sourceAccount: CryptoAccount? = null,
        destinationAccount: CryptoAccount? = null,
        reload: Boolean = true
    ) {
        if (sourceAccount == null && destinationAccount == null) {
            // TODO setCurrentTabItem(R.id.nav_swap)
            toolbar.title = getString(R.string.common_swap)
            val swapFragment = SwapFragment.newInstance()
            // TODO showFragment(swapFragment, reload)
        } else if (sourceAccount != null) {
            startActivity(
                TransactionFlowActivity.newInstance(
                    context = this,
                    sourceAccount = sourceAccount,
                    target = destinationAccount ?: NullCryptoAccount(),
                    action = AssetAction.Swap
                )
            )
        }
    }

    override fun launchKyc(campaignType: CampaignType) {
        // TODO not yet implemented
    }

    override fun launchThePitLinking(linkId: String) {
        // TODO not yet implemented
    }

    override fun launchThePit() {
        // TODO not yet implemented
    }

    override fun launchBackupFunds(fragment: Fragment?, requestCode: Int) {
        // TODO not yet implemented
    }

    override fun launchSetup2Fa() {
        // TODO not yet implemented
    }

    override fun launchVerifyEmail() {
        // TODO not yet implemented
    }

    override fun launchSetupFingerprintLogin() {
        // TODO not yet implemented
    }

    override fun launchReceive() {
        // TODO not yet implemented
    }

    override fun launchSend() {
        // TODO not yet implemented
    }

    override fun launchBuySell(viewType: BuySellFragment.BuySellViewType, asset: AssetInfo?) {
        showFragment(
            fragmentManager = supportFragmentManager,
            fragment = BuySellFragment.newInstance(
                viewType = viewType,
                asset = asset
            ),
            loadingView = binding.progress
        )
        binding.bottomNavigation.selectedNavigationItem = NavigationItem.BuyAndSell
    }

    override fun launchSimpleBuy(asset: AssetInfo) {
        // TODO not yet implemented
    }

    override fun launchInterestDashboard(origin: LaunchOrigin) {
        // TODO not yet implemented
    }

    override fun launchFiatDeposit(currency: String) {
        // TODO not yet implemented
    }

    override fun launchTransfer() {
        // TODO not yet implemented
    }

    override fun launchOpenBankingLinking(bankLinkingInfo: BankLinkingInfo) {
        // TODO not yet implemented
    }

    override fun launchSimpleBuyFromDeepLinkApproval() {
        // TODO not yet implemented
    }

    override fun launchPendingVerificationScreen(campaignType: CampaignType) {
        // TODO not yet implemented
    }

    override fun performAssetActionFor(action: AssetAction, account: BlockchainAccount) {
        // TODO not yet implemented
    }

    override fun resumeSimpleBuyKyc() {
        // TODO not yet implemented
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
