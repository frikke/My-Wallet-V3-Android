package piuk.blockchain.android.ui.dashboard

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.InterestAccount
import com.blockchain.coincore.SingleAccount
import com.blockchain.coincore.impl.CustodialTradingAccount
import com.blockchain.koin.scopedInject
import com.blockchain.notifications.analytics.AnalyticsEvents
import com.blockchain.notifications.analytics.LaunchOrigin
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.DashboardPrefs
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.campaign.blockstackCampaignName
import piuk.blockchain.android.databinding.FragmentPortfolioBinding
import piuk.blockchain.android.simplebuy.BuySellClicked
import piuk.blockchain.android.simplebuy.BuySellType
import piuk.blockchain.android.simplebuy.SimpleBuyAnalytics
import piuk.blockchain.android.simplebuy.SimpleBuyCancelOrderBottomSheet
import piuk.blockchain.android.ui.airdrops.AirdropStatusSheet
import piuk.blockchain.android.ui.customviews.BlockchainListDividerDecor
import piuk.blockchain.android.ui.customviews.KycBenefitsBottomSheet
import piuk.blockchain.android.ui.customviews.VerifyIdentityNumericBenefitItem
import piuk.blockchain.android.ui.dashboard.adapter.PortfolioDelegateAdapter
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementCard
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementHost
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementList
import piuk.blockchain.android.ui.dashboard.assetdetails.AssetDetailsAnalytics
import piuk.blockchain.android.ui.dashboard.assetdetails.AssetDetailsFlow
import piuk.blockchain.android.ui.dashboard.assetdetails.assetActionEvent
import piuk.blockchain.android.ui.dashboard.assetdetails.fiatAssetAction
import piuk.blockchain.android.ui.dashboard.model.CryptoAssetState
import piuk.blockchain.android.ui.dashboard.model.DashboardIntent
import piuk.blockchain.android.ui.dashboard.model.DashboardItem
import piuk.blockchain.android.ui.dashboard.model.DashboardModel
import piuk.blockchain.android.ui.dashboard.model.DashboardState
import piuk.blockchain.android.ui.dashboard.model.LinkablePaymentMethodsForAction
import piuk.blockchain.android.ui.dashboard.model.Locks
import piuk.blockchain.android.ui.dashboard.navigation.DashboardNavigationAction
import piuk.blockchain.android.ui.dashboard.navigation.LinkBankNavigationAction
import piuk.blockchain.android.ui.dashboard.sheets.FiatFundsDetailSheet
import piuk.blockchain.android.ui.dashboard.sheets.ForceBackupForSendSheet
import piuk.blockchain.android.ui.dashboard.sheets.LinkBankMethodChooserBottomSheet
import piuk.blockchain.android.ui.dashboard.sheets.WireTransferAccountDetailsBottomSheet
import piuk.blockchain.android.ui.home.HomeScreenMviFragment
import piuk.blockchain.android.ui.home.MainActivity
import piuk.blockchain.android.ui.interest.InterestSummarySheet
import piuk.blockchain.android.ui.linkbank.BankAuthActivity
import piuk.blockchain.android.ui.linkbank.BankAuthSource
import piuk.blockchain.android.ui.locks.LocksDetailsActivity
import piuk.blockchain.android.ui.recurringbuy.onboarding.RecurringBuyOnboardingActivity
import piuk.blockchain.android.ui.resources.AssetResources
import piuk.blockchain.android.ui.sell.BuySellFragment
import piuk.blockchain.android.ui.settings.BankLinkingHost
import piuk.blockchain.android.ui.transactionflow.DialogFlow
import piuk.blockchain.android.ui.transactionflow.TransactionFlow
import piuk.blockchain.android.ui.transactionflow.analytics.SwapAnalyticsEvents
import piuk.blockchain.android.ui.transactionflow.flow.TransactionFlowActivity
import piuk.blockchain.android.ui.transfer.analytics.TransferAnalyticsEvent
import piuk.blockchain.android.util.launchUrlInBrowser
import piuk.blockchain.android.util.visibleIf
import piuk.blockchain.androidcore.data.events.ActionEvent
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import timber.log.Timber

class EmptyDashboardItem : DashboardItem

class PortfolioFragment :
    HomeScreenMviFragment<
        DashboardModel,
        DashboardIntent,
        DashboardState,
        FragmentPortfolioBinding
        >(),
    ForceBackupForSendSheet.Host,
    FiatFundsDetailSheet.Host,
    KycBenefitsBottomSheet.Host,
    DialogFlow.FlowHost,
    AssetDetailsFlow.AssetDetailsHost,
    InterestSummarySheet.Host,
    BankLinkingHost {

    override val model: DashboardModel by scopedInject()
    private val announcements: AnnouncementList by scopedInject()
    private val analyticsReporter: BalanceAnalyticsReporter by scopedInject()
    private val dashboardPrefs: DashboardPrefs by inject()
    private val assetResources: AssetResources by inject()
    private val currencyPrefs: CurrencyPrefs by inject()

    private val theAdapter: PortfolioDelegateAdapter by lazy {
        PortfolioDelegateAdapter(
            prefs = get(),
            onCardClicked = { onAssetClicked(it) },
            analytics = get(),
            onFundsItemClicked = { onFundsClicked(it) },
            assetResources = assetResources,
            onHoldAmountClicked = { onHoldAmountClicked(it) }
        )
    }

    private val theLayoutManager: RecyclerView.LayoutManager by unsafeLazy {
        SafeLayoutManager(requireContext())
    }

    private val displayList = mutableListOf<DashboardItem>()

    private val compositeDisposable = CompositeDisposable()
    private val rxBus: RxBus by inject()

    private val actionEvent by unsafeLazy {
        rxBus.register(ActionEvent::class.java)
    }

    private val flowToLaunch: AssetAction? by unsafeLazy {
        arguments?.getSerializable(FLOW_TO_LAUNCH) as? AssetAction
    }

    private val flowCurrency: String? by unsafeLazy {
        arguments?.getString(FLOW_FIAT_CURRENCY)
    }

    private val useDynamicAssets: Boolean by unsafeLazy {
        arguments?.getBoolean(USE_DYNAMIC_ASSETS) ?: false
    }

    private var state: DashboardState? =
        null // Hold the 'current' display state, to enable optimising of state updates

    @UiThread
    override fun render(newState: DashboardState) {
        try {
            doRender(newState)
        } catch (e: Throwable) {
            Timber.e(e)
        }
    }

    @UiThread
    private fun doRender(newState: DashboardState) {

        binding.swipe.isRefreshing = false
        updateDisplayList(newState)

        if (this.state?.dashboardNavigationAction != newState.dashboardNavigationAction) {
            newState.dashboardNavigationAction?.let { dashboardNavigationAction ->
                handleStateNavigation(dashboardNavigationAction)
            }
        }

        // Update/show dialog flow
        if (state?.activeFlow != newState.activeFlow) {
            state?.activeFlow?.let { clearBottomSheet() }

            newState.activeFlow?.let {
                if (it is TransactionFlow) {
                    startActivity(
                        TransactionFlowActivity.newInstance(
                            context = requireActivity(),
                            sourceAccount = it.txSource,
                            target = it.txTarget,
                            action = it.txAction
                        )
                    )
                } else {
                    it.startFlow(childFragmentManager, this)
                }
            }
        }

        // Update/show announcement
        if (this.state?.announcement != newState.announcement) {
            showAnnouncement(newState.announcement)
        }

        updateAnalytics(this.state, newState)

        binding.dashboardProgress.visibleIf { newState.hasLongCallInProgress }

        this.state = newState
    }

    private fun updateDisplayList(newState: DashboardState) {
        with(displayList) {
            val newMap = if (isEmpty()) {
                mapOf(
                    IDX_CARD_ANNOUNCE to EmptyDashboardItem(),
                    IDX_CARD_BALANCE to newState,
                    IDX_WITHDRAWAL_LOCKS to newState.locks,
                    IDX_FUNDS_BALANCE to EmptyDashboardItem() // Placeholder for funds
                )
            } else {
                mapOf(
                    IDX_CARD_ANNOUNCE to get(IDX_CARD_ANNOUNCE),
                    IDX_CARD_BALANCE to newState,
                    IDX_WITHDRAWAL_LOCKS to newState.locks,
                    IDX_FUNDS_BALANCE to if (newState.fiatAssets.fiatAccounts.isNotEmpty()) {
                        newState.fiatAssets
                    } else {
                        get(IDX_FUNDS_BALANCE)
                    }
                )
            }

            // Add assets, sorted by fiat balance then alphabetically
            val assets = newState.activeAssets.values.sortedWith(
                compareByDescending<CryptoAssetState> { it.fiatBalance?.toBigInteger() }
                    .thenBy { it.currency.name }
            )
            if (useDynamicAssets) {
                val hasBalanceOrIsLoading = newState.isLoadingAssets ||
                    assets.any { it.fiatBalance?.isPositive == true }
                binding.portfolioLayoutGroup.visibleIf { hasBalanceOrIsLoading }
                binding.emptyPortfolioGroup.visibleIf { !hasBalanceOrIsLoading }
            }
            clear()
            addAll(newMap.values + assets)
        }
        theAdapter.notifyDataSetChanged()
    }

    private fun handleStateNavigation(navigationAction: DashboardNavigationAction) {
        when {
            navigationAction.isBottomSheet() -> {
                handleBottomSheet(navigationAction)
                model.process(DashboardIntent.ResetDashboardNavigation)
            }
            navigationAction is LinkBankNavigationAction -> {
                startBankLinking(navigationAction)
            }
        }
    }

    private fun startBankLinking(action: DashboardNavigationAction) {
        (action as? DashboardNavigationAction.LinkBankWithPartner)?.let {
            startActivityForResult(
                BankAuthActivity.newInstance(
                    action.linkBankTransfer,
                    when (it.assetAction) {
                        AssetAction.FiatDeposit -> {
                            BankAuthSource.DEPOSIT
                        }
                        AssetAction.Withdraw -> {
                            BankAuthSource.WITHDRAW
                        }
                        else -> {
                            throw IllegalStateException("Attempting to link from an unsupported action")
                        }
                    },
                    requireContext()
                ),
                BankAuthActivity.LINK_BANK_REQUEST_CODE
            )
        }
    }

    private fun handleBottomSheet(navigationAction: DashboardNavigationAction) {
        showBottomSheet(
            when (navigationAction) {
                DashboardNavigationAction.StxAirdropComplete -> AirdropStatusSheet.newInstance(
                    blockstackCampaignName
                )
                is DashboardNavigationAction.BackUpBeforeSend -> ForceBackupForSendSheet.newInstance(
                    navigationAction.backupSheetDetails
                )
                DashboardNavigationAction.SimpleBuyCancelOrder -> {
                    analytics.logEvent(SimpleBuyAnalytics.BANK_DETAILS_CANCEL_PROMPT)
                    SimpleBuyCancelOrderBottomSheet.newInstance(true)
                }
                is DashboardNavigationAction.FiatFundsDetails -> FiatFundsDetailSheet.newInstance(
                    navigationAction.fiatAccount
                )
                is DashboardNavigationAction.LinkOrDeposit -> {
                    navigationAction.fiatAccount?.let {
                        WireTransferAccountDetailsBottomSheet.newInstance(it)
                    } ?: WireTransferAccountDetailsBottomSheet.newInstance()
                }
                is DashboardNavigationAction.PaymentMethods -> {
                    LinkBankMethodChooserBottomSheet.newInstance(
                        navigationAction.paymentMethodsForAction
                    )
                }
                DashboardNavigationAction.FiatFundsNoKyc -> showFiatFundsKyc()
                is DashboardNavigationAction.InterestSummary -> InterestSummarySheet.newInstance(
                    navigationAction.account,
                    navigationAction.asset
                )
                else -> null
            }
        )
    }

    private fun showFiatFundsKyc(): BottomSheetDialogFragment {
        val currencyIcon = when (currencyPrefs.selectedFiatCurrency) {
            "EUR" -> R.drawable.ic_funds_euro
            "GBP" -> R.drawable.ic_funds_gbp
            else -> R.drawable.ic_funds_usd // show dollar if currency isn't selected
        }
        return KycBenefitsBottomSheet.newInstance(
            KycBenefitsBottomSheet.BenefitsDetails(
                title = getString(R.string.fiat_funds_no_kyc_announcement_title),
                description = getString(R.string.fiat_funds_no_kyc_announcement_description),
                listOfBenefits = listOf(
                    VerifyIdentityNumericBenefitItem(
                        getString(R.string.fiat_funds_no_kyc_step_1_title),
                        getString(R.string.fiat_funds_no_kyc_step_1_description)
                    ),
                    VerifyIdentityNumericBenefitItem(
                        getString(R.string.fiat_funds_no_kyc_step_2_title),
                        getString(R.string.fiat_funds_no_kyc_step_2_description)
                    ),
                    VerifyIdentityNumericBenefitItem(
                        getString(R.string.fiat_funds_no_kyc_step_3_title),
                        getString(R.string.fiat_funds_no_kyc_step_3_description)
                    )
                ),
                icon = currencyIcon
            )
        )
    }

    private fun showAnnouncement(card: AnnouncementCard?) {
        displayList[IDX_CARD_ANNOUNCE] = card ?: EmptyDashboardItem()
        theAdapter.notifyItemChanged(IDX_CARD_ANNOUNCE)
    }

    private fun updateAnalytics(oldState: DashboardState?, newState: DashboardState) {
        analyticsReporter.updateFiatTotal(newState.fiatBalance)

        newState.activeAssets.forEach { (asset, state) ->
            val newBalance = state.accountBalance?.total
            if (newBalance != null && newBalance != oldState?.activeAssets?.get(asset)?.accountBalance?.total) {
                // If we have the full set, this will fire
                analyticsReporter.gotAssetBalance(asset, newBalance)
            }
        }
    }

    override fun onBackPressed(): Boolean = false

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentPortfolioBinding =
        FragmentPortfolioBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        analytics.logEvent(AnalyticsEvents.Dashboard)

        setupSwipeRefresh()
        setupRecycler()
        setupCtaButtons()

        model.process(DashboardIntent.LoadFundsLocked)

        if (flowToLaunch != null && flowCurrency != null) {
            when (flowToLaunch) {
                AssetAction.FiatDeposit,
                AssetAction.Withdraw -> model.process(
                    DashboardIntent.StartBankTransferFlow(
                        action = AssetAction.Withdraw
                    )
                )
                else -> throw IllegalStateException("Unsupported flow launch for action $flowToLaunch")
            }
        }
    }

    private fun setupCtaButtons() {
        with(binding) {
            buyCryptoButton.setOnClickListener { navigator().launchBuySell() }
            receiveDepositButton.apply {
                leftButton.setOnClickListener { navigator().launchReceive() }
                rightButton.setOnClickListener {
                    model.process(DashboardIntent.StartBankTransferFlow(action = AssetAction.FiatDeposit))
                }
            }
        }
    }

    private fun setupRecycler() {
        binding.recyclerView.apply {
            layoutManager = theLayoutManager
            adapter = theAdapter

            addItemDecoration(BlockchainListDividerDecor(requireContext()))
        }
        theAdapter.items = displayList
    }

    private fun setupSwipeRefresh() {
        with(binding) {
            swipe.setOnRefreshListener { model.process(DashboardIntent.RefreshAllBalancesIntent) }

            // Configure the refreshing colors
            swipe.setColorSchemeResources(
                R.color.blue_800,
                R.color.blue_600,
                R.color.blue_400,
                R.color.blue_200
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (isHidden) return
        compositeDisposable += actionEvent.subscribe {
            initOrUpdateAssets()
        }

        (activity as? MainActivity)?.let {
            compositeDisposable += it.refreshAnnouncements.observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (announcements.enable()) {
                        announcements.checkLatest(announcementHost, compositeDisposable)
                    }
                }
        }

        announcements.checkLatest(announcementHost, compositeDisposable)

        initOrUpdateAssets()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            model.process(DashboardIntent.RefreshAllBalancesIntent)
        }
    }

    private fun initOrUpdateAssets() {
        if (displayList.isEmpty()) {
            model.process(DashboardIntent.GetActiveAssets)
        } else {
            model.process(DashboardIntent.RefreshAllBalancesIntent)
        }
    }

    override fun onPause() {
        // Save the sort order for use elsewhere, so that other asset lists can have the same
        // ordering. Storing this through prefs is a bit of a hack, um, "optimisation" - we don't
        // want to be getting all the balances every time we want to display assets in balance order.
        // TODO This UI is due for a re-write soon, at which point this ordering should be managed better
        dashboardPrefs.dashboardAssetOrder = displayList.filterIsInstance<CryptoAssetState>()
            .map { it.currency.displayTicker }

        compositeDisposable.clear()
        rxBus.unregister(ActionEvent::class.java, actionEvent)
        super.onPause()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            MainActivity.SETTINGS_EDIT,
            MainActivity.ACCOUNT_EDIT -> model.process(DashboardIntent.RefreshAllBalancesIntent)
            BACKUP_FUNDS_REQUEST_CODE -> {
                state?.backupSheetDetails?.let {
                    model.process(DashboardIntent.CheckBackupStatus(it.account, it.action))
                }
            }
            BankAuthActivity.LINK_BANK_REQUEST_CODE -> {
                if (resultCode == RESULT_OK) {
                    (state?.dashboardNavigationAction as? DashboardNavigationAction.LinkBankWithPartner)?.let {
                        model.process(
                            DashboardIntent.LaunchBankTransferFlow(
                                it.fiatAccount,
                                it.assetAction,
                                true
                            )
                        )
                    }
                }
            }
        }

        model.process(DashboardIntent.ResetDashboardNavigation)
    }

    private fun onAssetClicked(asset: AssetInfo) {
        analytics.logEvent(assetActionEvent(AssetDetailsAnalytics.WALLET_DETAILS, asset))
        model.process(
            DashboardIntent.UpdateLaunchDetailsFlow(
                AssetDetailsFlow(
                    asset = asset
                )
            )
        )
    }

    private fun onFundsClicked(fiatAccount: FiatAccount) {
        analytics.logEvent(fiatAssetAction(AssetDetailsAnalytics.FIAT_DETAIL_CLICKED, fiatAccount.fiatCurrency))
        model.process(DashboardIntent.ShowFiatAssetDetails(fiatAccount))
    }

    private fun onHoldAmountClicked(locks: Locks) {
        require(locks.fundsLocks != null) { "funds are null" }
        LocksDetailsActivity.start(requireContext(), locks.fundsLocks)
    }

    private val announcementHost = object : AnnouncementHost {

        override val disposables: CompositeDisposable
            get() = compositeDisposable

        override fun showAnnouncementCard(card: AnnouncementCard) {
            model.process(DashboardIntent.ShowAnnouncement(card))
        }

        override fun dismissAnnouncementCard() {
            model.process(DashboardIntent.ClearAnnouncement)
        }

        override fun startKyc(campaignType: CampaignType) = navigator().launchKyc(campaignType)

        override fun startSwap() {
            analytics.logEvent(SwapAnalyticsEvents.SwapClickedEvent(LaunchOrigin.DASHBOARD_PROMO))
            navigator().launchSwap()
        }

        override fun startPitLinking() = navigator().launchThePitLinking()

        override fun startFundsBackup() = navigator().launchBackupFunds()

        override fun startSetup2Fa() = navigator().launchSetup2Fa()

        override fun startVerifyEmail() = navigator().launchVerifyEmail()

        override fun startEnableFingerprintLogin() = navigator().launchSetupFingerprintLogin()

        override fun startTransferCrypto() {
            analytics.logEvent(
                TransferAnalyticsEvent.TransferClicked(
                    origin = LaunchOrigin.DASHBOARD_PROMO, type = TransferAnalyticsEvent.AnalyticsTransferType.RECEIVE
                )
            )
            navigator().launchReceive()
        }

        override fun startStxReceivedDetail() =
            model.process(DashboardIntent.ShowPortfolioSheet(DashboardNavigationAction.StxAirdropComplete))

        override fun finishSimpleBuySignup() {
            navigator().resumeSimpleBuyKyc()
        }

        override fun startSimpleBuy(asset: AssetInfo) {
            navigator().launchSimpleBuy(asset)
        }

        override fun startBuy() {
            analytics.logEvent(
                BuySellClicked(
                    origin = LaunchOrigin.DASHBOARD_PROMO, type = BuySellType.BUY
                )
            )
            navigator().launchBuySell()
        }

        override fun startSell() {
            analytics.logEvent(
                BuySellClicked(
                    origin = LaunchOrigin.DASHBOARD_PROMO, type = BuySellType.SELL
                )
            )
            navigator().launchBuySell(BuySellFragment.BuySellViewType.TYPE_SELL)
        }

        override fun startSend() {
            analytics.logEvent(
                TransferAnalyticsEvent.TransferClicked(
                    origin = LaunchOrigin.DASHBOARD_PROMO, type = TransferAnalyticsEvent.AnalyticsTransferType.SEND
                )
            )
            navigator().launchSend()
        }

        override fun startInterestDashboard() {
            navigator().launchInterestDashboard(LaunchOrigin.DASHBOARD_PROMO)
        }

        override fun showFiatFundsKyc() {
            model.process(DashboardIntent.ShowPortfolioSheet(DashboardNavigationAction.FiatFundsNoKyc))
        }

        override fun showBankLinking() =
            model.process(DashboardIntent.ShowBankLinkingSheet())

        override fun openBrowserLink(url: String) =
            requireContext().launchUrlInBrowser(url)

        override fun startRecurringBuyUpsell() {
            startActivity(RecurringBuyOnboardingActivity.newInstance(requireActivity(), false))
        }
    }

    // DialogBottomSheet.Host
    override fun onSheetClosed() {
        model.process(DashboardIntent.ClearBottomSheet)
    }

    // DialogFlow.FlowHost
    override fun onFlowFinished() {
        model.process(DashboardIntent.ClearBottomSheet)
    }

    // BankLinkingHost
    override fun onBankWireTransferSelected(currency: String) {
        state?.selectedFiatAccount?.let {
            model.process(DashboardIntent.ShowBankLinkingSheet(it))
        }
    }

    override fun onLinkBankSelected(paymentMethodForAction: LinkablePaymentMethodsForAction) {
        state?.selectedFiatAccount?.let {
            if (paymentMethodForAction is LinkablePaymentMethodsForAction.LinkablePaymentMethodsForDeposit) {
                model.process(DashboardIntent.LaunchBankTransferFlow(it, AssetAction.FiatDeposit, true))
            } else if (paymentMethodForAction is LinkablePaymentMethodsForAction.LinkablePaymentMethodsForWithdraw) {
                model.process(DashboardIntent.LaunchBankTransferFlow(it, AssetAction.Withdraw, true))
            }
        }
    }

    // FiatFundsDetailSheet.Host
    override fun goToActivityFor(account: BlockchainAccount) =
        navigator().performAssetActionFor(AssetAction.ViewActivity, account)

    override fun showFundsKyc() {
        model.process(DashboardIntent.ShowPortfolioSheet(DashboardNavigationAction.FiatFundsNoKyc))
    }

    override fun startBankTransferWithdrawal(fiatAccount: FiatAccount) {
        model.process(DashboardIntent.LaunchBankTransferFlow(fiatAccount, AssetAction.Withdraw, false))
    }

    override fun startDepositFlow(fiatAccount: FiatAccount) {
        model.process(DashboardIntent.LaunchBankTransferFlow(fiatAccount, AssetAction.FiatDeposit, false))
    }

    // KycBenefitsBottomSheet.Host
    override fun verificationCtaClicked() {
        navigator().launchKyc(CampaignType.FiatFunds)
    }

    private fun launchSendFor(account: SingleAccount, action: AssetAction) {
        if (account is CustodialTradingAccount) {
            model.process(DashboardIntent.CheckBackupStatus(account, action))
        } else if (account is CryptoAccount) {
            model.process(
                DashboardIntent.UpdateLaunchDialogFlow(
                    TransactionFlow(
                        sourceAccount = account,
                        action = action
                    )
                )
            )
        }
    }

    // AssetDetailsHost
    override fun performAssetActionFor(action: AssetAction, account: BlockchainAccount) {
        clearBottomSheet()
        when (action) {
            AssetAction.Send -> launchSendFor(account as SingleAccount, action)
            else -> navigator().performAssetActionFor(action, account)
        }
    }

    override fun goToSellFrom(account: CryptoAccount) =
        startActivity(
            TransactionFlowActivity.newInstance(
                context = requireActivity(),
                sourceAccount = account,
                action = AssetAction.Sell
            )
        )

    override fun goToInterestDeposit(toAccount: InterestAccount) {
        if (toAccount is CryptoAccount) {
            model.process(
                DashboardIntent.UpdateLaunchDialogFlow(
                    TransactionFlow(
                        target = toAccount,
                        action = AssetAction.InterestDeposit
                    )
                )
            )
        }
    }

    override fun goToInterestWithdraw(fromAccount: InterestAccount) {
        if (fromAccount is CryptoAccount) {
            model.process(
                DashboardIntent.UpdateLaunchDialogFlow(
                    TransactionFlow(
                        sourceAccount = fromAccount,
                        action = AssetAction.InterestWithdraw
                    )
                )
            )
        }
    }

    override fun goToInterestDashboard() {
        navigator().launchInterestDashboard(LaunchOrigin.CURRENCY_PAGE)
    }

    override fun goToSummary(account: SingleAccount, asset: AssetInfo) {
        model.process(
            DashboardIntent.UpdateSelectedCryptoAccount(
                account,
                asset
            )
        )
        model.process(
            DashboardIntent.ShowPortfolioSheet(
                DashboardNavigationAction.InterestSummary(
                    account,
                    asset
                )
            )
        )
    }

    override fun goToBuy(asset: AssetInfo) {
        navigator().launchBuySell(BuySellFragment.BuySellViewType.TYPE_BUY, asset)
    }

    // ForceBackupForSendSheet.Host
    override fun startBackupForTransfer() {
        navigator().launchBackupFunds(this, BACKUP_FUNDS_REQUEST_CODE)
    }

    override fun startTransferFunds(account: SingleAccount, action: AssetAction) {
        if (account is CryptoAccount) {
            model.process(
                DashboardIntent.UpdateLaunchDialogFlow(
                    TransactionFlow(
                        sourceAccount = account,
                        action = action
                    )
                )
            )
        }
    }

    companion object {
        fun newInstance(
            useDynamicAssets: Boolean,
            flowToLaunch: AssetAction? = null,
            fiatCurrency: String? = null
        ) = PortfolioFragment().apply {
            arguments = Bundle().apply {
                putBoolean(USE_DYNAMIC_ASSETS, useDynamicAssets)
                if (flowToLaunch != null && fiatCurrency != null) {
                    putSerializable(FLOW_TO_LAUNCH, flowToLaunch)
                    putString(FLOW_FIAT_CURRENCY, fiatCurrency)
                }
            }
        }

        internal const val USE_DYNAMIC_ASSETS = "USE_DYNAMIC_ASSETS"
        internal const val FLOW_TO_LAUNCH = "FLOW_TO_LAUNCH"
        internal const val FLOW_FIAT_CURRENCY = "FLOW_FIAT_CURRENCY"

        private const val IDX_CARD_ANNOUNCE = 0
        private const val IDX_CARD_BALANCE = 1
        private const val IDX_WITHDRAWAL_LOCKS = 2
        private const val IDX_FUNDS_BALANCE = 3

        const val BACKUP_FUNDS_REQUEST_CODE = 8265
    }
}

/**
 * supportsPredictiveItemAnimations = false to avoid crashes when computing changes.
 */
internal class SafeLayoutManager(context: Context) : LinearLayoutManager(context) {
    override fun supportsPredictiveItemAnimations() = false
}
