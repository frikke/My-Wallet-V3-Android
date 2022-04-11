package piuk.blockchain.android.ui.dashboard

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.UiThread
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.SingleAccount
import com.blockchain.coincore.impl.CustodialTradingAccount
import com.blockchain.componentlib.viewextensions.configureWithPinnedView
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.extensions.exhaustive
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.BlockedReason
import com.blockchain.nabu.FeatureAccess
import com.blockchain.notifications.analytics.AnalyticsEvents
import com.blockchain.notifications.analytics.LaunchOrigin
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.DashboardPrefs
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.campaign.blockstackCampaignName
import piuk.blockchain.android.databinding.FragmentPortfolioBinding
import piuk.blockchain.android.domain.usecases.CompletableDashboardOnboardingStep
import piuk.blockchain.android.domain.usecases.DashboardOnboardingStep
import piuk.blockchain.android.domain.usecases.DashboardOnboardingStepState
import piuk.blockchain.android.simplebuy.BuySellClicked
import piuk.blockchain.android.simplebuy.SimpleBuyAnalytics
import piuk.blockchain.android.simplebuy.sheets.BuyPendingOrdersBottomSheet
import piuk.blockchain.android.simplebuy.sheets.SimpleBuyCancelOrderBottomSheet
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
import piuk.blockchain.android.ui.dashboard.assetdetails.FullScreenCoinViewFlow
import piuk.blockchain.android.ui.dashboard.assetdetails.assetActionEvent
import piuk.blockchain.android.ui.dashboard.assetdetails.fiatAssetAction
import piuk.blockchain.android.ui.dashboard.coinview.CoinViewActivity
import piuk.blockchain.android.ui.dashboard.model.CryptoAssetState
import piuk.blockchain.android.ui.dashboard.model.DashboardIntent
import piuk.blockchain.android.ui.dashboard.model.DashboardItem
import piuk.blockchain.android.ui.dashboard.model.DashboardModel
import piuk.blockchain.android.ui.dashboard.model.DashboardOnboardingState
import piuk.blockchain.android.ui.dashboard.model.DashboardState
import piuk.blockchain.android.ui.dashboard.model.LinkablePaymentMethodsForAction
import piuk.blockchain.android.ui.dashboard.model.Locks
import piuk.blockchain.android.ui.dashboard.navigation.DashboardNavigationAction
import piuk.blockchain.android.ui.dashboard.onboarding.DashboardOnboardingActivity
import piuk.blockchain.android.ui.dashboard.onboarding.DashboardOnboardingAnalytics
import piuk.blockchain.android.ui.dashboard.onboarding.toCurrentStepIndex
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
import piuk.blockchain.android.ui.settings.v2.BankLinkingHost
import piuk.blockchain.android.ui.transactionflow.DialogFlow
import piuk.blockchain.android.ui.transactionflow.TransactionFlow
import piuk.blockchain.android.ui.transactionflow.analytics.SwapAnalyticsEvents
import piuk.blockchain.android.ui.transactionflow.flow.TransactionFlowActivity
import piuk.blockchain.android.ui.transfer.analytics.TransferAnalyticsEvent
import piuk.blockchain.android.util.getAccount
import piuk.blockchain.android.util.launchUrlInBrowser
import piuk.blockchain.androidcore.data.events.ActionEvent
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import timber.log.Timber

class EmptyDashboardItem : DashboardItem

class PortfolioFragment :
    HomeScreenMviFragment<DashboardModel, DashboardIntent, DashboardState, FragmentPortfolioBinding>(),
    ForceBackupForSendSheet.Host,
    FiatFundsDetailSheet.Host,
    KycBenefitsBottomSheet.Host,
    DialogFlow.FlowHost,
    AssetDetailsFlow.AssetDetailsHost,
    InterestSummarySheet.Host,
    BuyPendingOrdersBottomSheet.Host,
    BankLinkingHost {

    override val model: DashboardModel by scopedInject()

    override fun onBackPressed(): Boolean = false

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentPortfolioBinding =
        FragmentPortfolioBinding.inflate(inflater, container, false)

    private val announcements: AnnouncementList by scopedInject()
    private val analyticsReporter: BalanceAnalyticsReporter by scopedInject()
    private val dashboardPrefs: DashboardPrefs by inject()
    private val assetResources: AssetResources by inject()
    private val currencyPrefs: CurrencyPrefs by inject()
    private var activeFiat = currencyPrefs.selectedFiatCurrency

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

    private var state: DashboardState? =
        null // Hold the 'current' display state, to enable optimising of state updates

    private val activityResultsContract = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            (it.data?.getAccount(CoinViewActivity.ACCOUNT_FOR_ACTIVITY))?.let { account ->
                goToActivityFor(account)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        analytics.logEvent(AnalyticsEvents.Dashboard)

        model.process(DashboardIntent.UpdateDepositButton)
        model.process(DashboardIntent.LoadFundsLocked)

        setupSwipeRefresh()
        setupRecycler()

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
                when (it) {
                    is TransactionFlow -> {
                        startActivity(
                            TransactionFlowActivity.newIntent(
                                context = requireActivity(),
                                sourceAccount = it.txSource,
                                target = it.txTarget,
                                action = it.txAction
                            )
                        )
                    }
                    is FullScreenCoinViewFlow -> {
                        activityResultsContract.launch(CoinViewActivity.newIntent(requireContext(), it.asset))
                        model.process(DashboardIntent.ClearActiveFlow)
                    }
                    else -> {
                        it.startFlow(childFragmentManager, this)
                    }
                }
            }
        }

        // Update/show announcement
        if (this.state?.announcement != newState.announcement) {
            showAnnouncement(newState.announcement)
        }

        updateAnalytics(this.state, newState)
        updateOnboarding(newState.onboardingState)

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
            val cryptoAssets = newState.activeAssets.values.sortedWith(
                compareByDescending<CryptoAssetState> { it.fiatBalance?.toBigInteger() }
                    .thenBy { it.currency.name }
            )
            val fiatAssets = newState.fiatAssets.fiatAccounts

            val dashboardLoading = newState.isLoadingAssets
            val atLeastOneAssetIsLoading = cryptoAssets.any { it.isLoading }
            val isLoading = dashboardLoading || atLeastOneAssetIsLoading

            val atLeastOneCryptoAssetHasBalancePositive =
                cryptoAssets.any { it.accountBalance?.total?.isPositive == true }

            val atLeastOneFiatAssetHasBalancePositive =
                fiatAssets.any { it.value.availableBalance?.isPositive == true }

            val showPortfolio = atLeastOneCryptoAssetHasBalancePositive || atLeastOneFiatAssetHasBalancePositive

            manageLoadingState(isLoading, showPortfolio, newState.canPotentiallyTransactWithBanks)
            clear()
            addAll(newMap.values + cryptoAssets)
        }
        theAdapter.notifyDataSetChanged()
    }

    private fun manageLoadingState(isLoading: Boolean, showPortfolio: Boolean, showDepositButton: Boolean) {
        with(binding) {
            when {
                isLoading && showPortfolio -> {
                    portfolioRecyclerView.visible()
                    dashboardProgress.gone()
                }
                isLoading -> {
                    portfolioRecyclerView.gone()
                    emptyPortfolioGroup.gone()
                    dashboardProgress.visible()
                }
                else -> {
                    portfolioRecyclerView.visibleIf { showPortfolio }
                    emptyPortfolioGroup.visibleIf { !showPortfolio }
                    setupCtaButtons(showDepositButton, showPortfolio)
                    dashboardProgress.gone()
                }
            }
        }
    }

    private fun handleStateNavigation(navigationAction: DashboardNavigationAction) {
        when {
            navigationAction is DashboardNavigationAction.BottomSheet -> {
                handleBottomSheet(navigationAction)
                model.process(DashboardIntent.ResetDashboardNavigation)
            }
            navigationAction is DashboardNavigationAction.LinkBankWithPartner -> {
                startBankLinking(navigationAction)
            }
            navigationAction is DashboardNavigationAction.DashboardOnboarding -> {
                launchDashboardOnboarding(navigationAction.initialSteps)
                model.process(DashboardIntent.ResetDashboardNavigation)
            }
        }
    }

    fun launchNewUserDashboardOnboarding() {
        val steps = DashboardOnboardingStep.values().map { step ->
            CompletableDashboardOnboardingStep(step, DashboardOnboardingStepState.INCOMPLETE)
        }
        launchDashboardOnboarding(steps)
    }

    private fun launchDashboardOnboarding(initialSteps: List<CompletableDashboardOnboardingStep>) {
        activityResultDashboardOnboarding.launch(DashboardOnboardingActivity.ActivityArgs(initialSteps = initialSteps))
    }

    private fun startBankLinking(action: DashboardNavigationAction.LinkBankWithPartner) {
        activityResultLinkBank.launch(
            BankAuthActivity.newInstance(
                action.linkBankTransfer,
                when (action.assetAction) {
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
            )
        )
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
                    navigationAction.account
                )
                else -> null
            }
        )
    }

    private fun showFiatFundsKyc(): BottomSheetDialogFragment {
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
                icon = currencyPrefs.selectedFiatCurrency.logo
            )
        )
    }

    private fun showAnnouncement(card: AnnouncementCard?) {
        displayList[IDX_CARD_ANNOUNCE] = card ?: EmptyDashboardItem()
        theAdapter.notifyItemChanged(IDX_CARD_ANNOUNCE)
        card?.let {
            binding.portfolioRecyclerView.smoothScrollToPosition(IDX_CARD_ANNOUNCE)
        }
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

    private fun updateOnboarding(newState: DashboardOnboardingState) {
        with(binding.cardOnboarding) {
            binding.portfolioRecyclerView.configureWithPinnedView(this, newState is DashboardOnboardingState.Visible)
            if (newState is DashboardOnboardingState.Visible) {
                val totalSteps = newState.steps.size
                val completeSteps = newState.steps.count { it.isCompleted }
                setTotalSteps(totalSteps)
                setCompleteSteps(completeSteps)
                setOnClickListener {
                    model.process(DashboardIntent.LaunchDashboardOnboarding(newState.steps))
                    newState.steps.toCurrentStepIndex()?.let {
                        analytics.logEvent(DashboardOnboardingAnalytics.CardClicked(it))
                    }
                }
            }
        }
    }

    private fun setupCtaButtons(showDepositButton: Boolean, showPortfolio: Boolean) {
        with(binding) {
            buyCryptoButton.setOnClickListener { navigator().launchBuySell() }
            receiveDepositButton.apply {
                visibleIf { showDepositButton && !showPortfolio }
                leftButton.setOnClickListener { navigator().launchReceive() }
                rightButton.setOnClickListener {
                    model.process(DashboardIntent.StartBankTransferFlow(action = AssetAction.FiatDeposit))
                }
            }
            receiveButton.apply {
                visibleIf { !showDepositButton && !showPortfolio }
                setOnClickListener { navigator().launchReceive() }
            }
        }
    }

    private fun setupRecycler() {
        binding.portfolioRecyclerView.apply {
            layoutManager = theLayoutManager
            adapter = theAdapter

            addItemDecoration(BlockchainListDividerDecor(requireContext()))
        }
        theAdapter.items = displayList
    }

    private fun setupSwipeRefresh() {
        with(binding) {
            swipe.setOnRefreshListener {
                model.process(DashboardIntent.RefreshAllBalancesIntent(false))
                model.process(DashboardIntent.LoadFundsLocked)
            }

            // Configure the refreshing colors
            swipe.setColorSchemeResources(
                R.color.blue_800,
                R.color.blue_600,
                R.color.blue_400,
                R.color.blue_200
            )
        }
    }

    // For the split dashboard, this onResume is called only once. When the fragment is created.
    // To fix that we need to use a different PagerAdapter (FragmentStateAdapter) with the corresponding behavior
    override fun onResume() {
        super.onResume()
        if (activeFiat != currencyPrefs.selectedFiatCurrency) {
            activeFiat = currencyPrefs.selectedFiatCurrency
            model.process(DashboardIntent.ResetDashboardAssets)
        }
        if (isHidden) return
        compositeDisposable += actionEvent.subscribe {
            initOrUpdateAssets()
        }

        announcements.checkLatest(announcementHost, compositeDisposable)
        model.process(DashboardIntent.FetchOnboardingSteps)
        initOrUpdateAssets()
    }

    // This method doesn't get called when we use the split portfolio/prices dashboard.
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            model.process(
                DashboardIntent.RefreshAllBalancesIntent(
                    loadSilently = activeFiat == currencyPrefs.selectedFiatCurrency
                )
            )
            activeFiat = currencyPrefs.selectedFiatCurrency
            model.process(DashboardIntent.FetchOnboardingSteps)
        }
    }

    private fun initOrUpdateAssets() {
        if (displayList.isEmpty()) {
            model.process(DashboardIntent.GetActiveAssets)
        } else {
            model.process(DashboardIntent.RefreshAllBalancesIntent(false))
        }
    }

    fun refreshFiatAssets() {
        state?.fiatAssets?.let {
            model.process(DashboardIntent.RefreshFiatBalances(it.fiatAccounts))
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

    private val activityResultLinkBank =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
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
            model.process(DashboardIntent.ResetDashboardNavigation)
        }

    private val activityResultDashboardOnboarding =
        registerForActivityResult(DashboardOnboardingActivity.BlockchainActivityResultContract()) { result ->
            when (result) {
                // Without Handler this fails with FragmentManager is already executing transactions, investigated but came up with nothing
                DashboardOnboardingActivity.ActivityResult.LaunchBuyFlow -> Handler(Looper.getMainLooper()).post {
                    navigator().launchBuySell(BuySellFragment.BuySellViewType.TYPE_BUY)
                }
                null -> {
                }
            }
            model.process(DashboardIntent.ResetDashboardNavigation)
        }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            MainActivity.SETTINGS_EDIT,
            MainActivity.ACCOUNT_EDIT -> model.process(DashboardIntent.RefreshAllBalancesIntent(false))
            BACKUP_FUNDS_REQUEST_CODE -> {
                state?.backupSheetDetails?.let {
                    model.process(DashboardIntent.CheckBackupStatus(it.account, it.action))
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
        analytics.logEvent(
            fiatAssetAction(AssetDetailsAnalytics.FIAT_DETAIL_CLICKED, fiatAccount.currency.networkTicker)
        )
        model.process(DashboardIntent.ShowFiatAssetDetails(fiatAccount))
    }

    private fun onHoldAmountClicked(locks: Locks) {
        require(locks.fundsLocks != null) { "funds are null" }
        LocksDetailsActivity.start(requireContext(), locks.fundsLocks)
    }

    private val announcementHost = object : AnnouncementHost {

        override val disposables: CompositeDisposable
            get() = compositeDisposable

        override val context: Context?
            get() = this@PortfolioFragment.context

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
                    origin = LaunchOrigin.DASHBOARD_PROMO,
                    type = BuySellFragment.BuySellViewType.TYPE_BUY
                )
            )
            navigator().launchBuySell()
        }

        override fun startSell() {
            analytics.logEvent(
                BuySellClicked(
                    origin = LaunchOrigin.DASHBOARD_PROMO, type = BuySellFragment.BuySellViewType.TYPE_SELL
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
        model.process(DashboardIntent.ClearActiveFlow)
    }

    // DialogFlow.FlowHost
    override fun onFlowFinished() {
        model.process(DashboardIntent.ClearActiveFlow)
    }

    // BankLinkingHost
    override fun onBankWireTransferSelected(currency: FiatCurrency) {
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
            TransactionFlowActivity.newIntent(
                context = requireActivity(),
                sourceAccount = account,
                action = AssetAction.Sell
            )
        )

    override fun goToInterestDeposit(toAccount: BlockchainAccount) {
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

    override fun goToInterestWithdraw(fromAccount: BlockchainAccount) {
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

    override fun goToSummary(account: CryptoAccount) {
        model.process(
            DashboardIntent.UpdateSelectedCryptoAccount(
                account
            )
        )
        model.process(
            DashboardIntent.ShowPortfolioSheet(
                DashboardNavigationAction.InterestSummary(
                    account
                )
            )
        )
    }

    override fun goToKyc() {
        navigator().launchKyc(CampaignType.None)
    }

    override fun tryToLaunchBuy(asset: AssetInfo, buyAccess: FeatureAccess) {
        val blockedState = buyAccess as? FeatureAccess.Blocked
        blockedState?.let {
            when (val reason = it.reason) {
                is BlockedReason.TooManyInFlightTransactions -> showPendingBuysBottomSheet(reason.maxTransactions)
                BlockedReason.NotEligible -> throw IllegalStateException("Buy should not be accessible")
                BlockedReason.InsufficientTier -> throw IllegalStateException("Not used in Feature.SimpleBuy")
            }.exhaustive
        } ?: run {
            navigator().launchBuySell(BuySellFragment.BuySellViewType.TYPE_BUY, asset, true)
        }
    }

    private fun showPendingBuysBottomSheet(pendingBuys: Int) {
        BuyPendingOrdersBottomSheet.newInstance(pendingBuys).show(
            childFragmentManager,
            BuyPendingOrdersBottomSheet.TAG
        )
    }

    override fun startActivityRequested() {
        navigator().performAssetActionFor(AssetAction.ViewActivity)
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
            flowToLaunch: AssetAction? = null,
            fiatCurrency: String? = null
        ) = PortfolioFragment().apply {
            arguments = Bundle().apply {
                if (flowToLaunch != null && fiatCurrency != null) {
                    putSerializable(FLOW_TO_LAUNCH, flowToLaunch)
                    putString(FLOW_FIAT_CURRENCY, fiatCurrency)
                }
            }
        }

        internal const val FLOW_TO_LAUNCH = "FLOW_TO_LAUNCH"
        internal const val FLOW_FIAT_CURRENCY = "FLOW_FIAT_CURRENCY"

        private const val IDX_CARD_ANNOUNCE = 0
        private const val IDX_CARD_BALANCE = 1
        private const val IDX_WITHDRAWAL_LOCKS = 2
        private const val IDX_FUNDS_BALANCE = 3

        const val BACKUP_FUNDS_REQUEST_CODE = 8265
    }
}

internal class SafeLayoutManager(context: Context) : LinearLayoutManager(context) {
    override fun supportsPredictiveItemAnimations() = false
}
