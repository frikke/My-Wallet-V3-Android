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
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.analytics.events.AnalyticsEvents
import com.blockchain.analytics.events.LaunchOrigin
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.SingleAccount
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.card.CustomBackgroundCard
import com.blockchain.componentlib.card.CustomBackgroundCardView
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.viewextensions.configureWithPinnedView
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.domain.common.model.PromotionStyleInfo
import com.blockchain.domain.referral.model.ReferralInfo
import com.blockchain.extensions.minus
import com.blockchain.koin.scopedInject
import com.blockchain.logging.MomentEvent
import com.blockchain.logging.MomentLogger
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.DashboardPrefs
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.disposables.CompositeDisposable
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.databinding.FragmentPortfolioBinding
import piuk.blockchain.android.domain.usecases.CompletableDashboardOnboardingStep
import piuk.blockchain.android.domain.usecases.DashboardOnboardingStep
import piuk.blockchain.android.domain.usecases.DashboardOnboardingStepState
import piuk.blockchain.android.rating.presentaion.AppRatingFragment
import piuk.blockchain.android.rating.presentaion.AppRatingTriggerSource
import piuk.blockchain.android.simplebuy.BuySellClicked
import piuk.blockchain.android.simplebuy.SimpleBuyAnalytics
import piuk.blockchain.android.simplebuy.sheets.BuyPendingOrdersBottomSheet
import piuk.blockchain.android.simplebuy.sheets.SimpleBuyCancelOrderBottomSheet
import piuk.blockchain.android.ui.cowboys.CowboysAnalytics
import piuk.blockchain.android.ui.cowboys.CowboysFlowActivity
import piuk.blockchain.android.ui.cowboys.FlowStep
import piuk.blockchain.android.ui.customviews.BlockchainListDividerDecor
import piuk.blockchain.android.ui.customviews.BlockedDueToSanctionsSheet
import piuk.blockchain.android.ui.customviews.KycBenefitsBottomSheet
import piuk.blockchain.android.ui.customviews.VerifyIdentityNumericBenefitItem
import piuk.blockchain.android.ui.dashboard.adapter.PortfolioDelegateAdapter
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementCard
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementHost
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementList
import piuk.blockchain.android.ui.dashboard.assetdetails.AssetDetailsAnalytics
import piuk.blockchain.android.ui.dashboard.assetdetails.assetActionEvent
import piuk.blockchain.android.ui.dashboard.assetdetails.fiatAssetAction
import piuk.blockchain.android.ui.dashboard.coinview.CoinViewActivity
import piuk.blockchain.android.ui.dashboard.model.BrokearageFiatAsset
import piuk.blockchain.android.ui.dashboard.model.DashboardAsset
import piuk.blockchain.android.ui.dashboard.model.DashboardCowboysState
import piuk.blockchain.android.ui.dashboard.model.DashboardIntent
import piuk.blockchain.android.ui.dashboard.model.DashboardModel
import piuk.blockchain.android.ui.dashboard.model.DashboardOnboardingState
import piuk.blockchain.android.ui.dashboard.model.DashboardState
import piuk.blockchain.android.ui.dashboard.model.DashboardUIState
import piuk.blockchain.android.ui.dashboard.model.FiatBalanceInfo
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
import piuk.blockchain.android.ui.dataremediation.QuestionnaireSheet
import piuk.blockchain.android.ui.home.HomeScreenMviFragment
import piuk.blockchain.android.ui.home.MainActivity
import piuk.blockchain.android.ui.home.WalletClientAnalytics
import piuk.blockchain.android.ui.interest.InterestSummarySheet
import piuk.blockchain.android.ui.linkbank.BankAuthActivity
import piuk.blockchain.android.ui.linkbank.BankAuthSource
import piuk.blockchain.android.ui.linkbank.alias.BankAliasLinkContract
import piuk.blockchain.android.ui.locks.LocksDetailsActivity
import piuk.blockchain.android.ui.recurringbuy.onboarding.RecurringBuyOnboardingActivity
import piuk.blockchain.android.ui.referral.presentation.ReferralSheet
import piuk.blockchain.android.ui.resources.AssetResources
import piuk.blockchain.android.ui.sell.BuySellFragment
import piuk.blockchain.android.ui.settings.v2.BankLinkingHost
import piuk.blockchain.android.ui.transactionflow.analytics.SwapAnalyticsEvents
import piuk.blockchain.android.ui.transactionflow.flow.TransactionFlowActivity
import piuk.blockchain.android.ui.transfer.analytics.TransferAnalyticsEvent
import piuk.blockchain.android.util.getAccount
import piuk.blockchain.android.util.launchUrlInBrowser
import piuk.blockchain.androidcore.data.events.ActionEvent
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import timber.log.Timber

class PortfolioFragment :
    HomeScreenMviFragment<DashboardModel, DashboardIntent, DashboardState, FragmentPortfolioBinding>(),
    ForceBackupForSendSheet.Host,
    FiatFundsDetailSheet.Host,
    KycBenefitsBottomSheet.Host,
    BuyPendingOrdersBottomSheet.Host,
    QuestionnaireSheet.Host,
    BankLinkingHost {

    override val model: DashboardModel by scopedInject()

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
            assetCatalogue = get(),
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

    private var questionnaireCallbackIntent: DashboardIntent.LaunchBankTransferFlow? = null

    private val activityResultsContract = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            (it.data?.getAccount(CoinViewActivity.ACCOUNT_FOR_ACTIVITY))?.let { account ->
                goToActivityFor(account)
            }
        }
    }

    private val bankAliasLinkLauncher = registerForActivityResult(BankAliasLinkContract()) { linkSuccess ->
        if (linkSuccess) {
            (state?.dashboardNavigationAction as? DashboardNavigationAction.LinkWithAlias)?.let { action ->
                action.fiatAccount?.let {
                    model.process(
                        DashboardIntent.LaunchBankTransferFlow(it, AssetAction.FiatWithdraw, false)
                    )
                }
            }
        }
    }

    private val momentLogger: MomentLogger by inject()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        analytics.logEvent(AnalyticsEvents.Dashboard)
        analytics.logEvent(WalletClientAnalytics.WalletHomeViewed)

        model.process(DashboardIntent.UpdateDepositButton)
        setupSwipeRefresh()
        setupRecycler()

        if (flowToLaunch != null && flowCurrency != null) {
            when (flowToLaunch) {
                AssetAction.FiatDeposit,
                AssetAction.FiatWithdraw -> model.process(
                    DashboardIntent.StartBankTransferFlow(
                        action = AssetAction.FiatWithdraw
                    )
                )
                else -> throw IllegalStateException("Unsupported flow launch for action $flowToLaunch")
            }
        }
    }

    private fun isDashboardLoading(state: DashboardState): Boolean {
        val atLeastOneAssetIsLoading = state.activeAssets.values.any { it.isUILoading }
        val dashboardLoading = state.isLoadingAssets
        return dashboardLoading || atLeastOneAssetIsLoading
    }

    @UiThread
    override fun render(newState: DashboardState) {
        binding.swipe.isRefreshing = newState.isSwipingToRefresh
        updateDisplayList(newState)
        verifyAppRating(newState)

        if (this.state?.dashboardNavigationAction != newState.dashboardNavigationAction) {
            newState.dashboardNavigationAction?.let { dashboardNavigationAction ->
                handleStateNavigation(dashboardNavigationAction)
            }
        }

        // Update/show announcement
        if (this.state?.announcement != newState.announcement) {
            showAnnouncement(newState.announcement)
        }

        updateAnalytics(this.state, newState)
        updateOnboarding(newState.onboardingState)
        newState.referralSuccessData?.let {
            showReferralSuccess(it)
        }

        renderCowboysFlow(newState.dashboardCowboysState)

        this.state = newState
    }

    private fun updateDisplayList(newState: DashboardState) {
        val items = listOfNotNull(
            newState.dashboardBalance,
            newState.locks.fundsLocks?.let {
                newState.locks
            },
            newState.fiatDashboardAssets.takeIf { it.isNotEmpty() }?.let { FiatBalanceInfo(it) }
        )

        val cryptoAssets = newState.displayableAssets.filterNot { it is BrokearageFiatAsset }.sortedWith(
            compareByDescending<DashboardAsset> { it.fiatBalance?.toBigInteger() }
                .thenBy { it.currency.name }
        )

        renderState(newState.uiState)
        setupCtaButtons(newState)
        theAdapter.items = theAdapter.items.filterIsInstance<AnnouncementCard>().plus(items).plus(cryptoAssets)
    }

    private fun renderState(uiState: DashboardUIState) {
        Timber.i("Rendering state $uiState")
        with(binding) {
            when (uiState) {
                DashboardUIState.ASSETS -> {
                    portfolioRecyclerView.visible()
                    dashboardProgress.gone()
                    emptyPortfolioGroup.gone()
                    momentLogger.endEvent(MomentEvent.PIN_TO_DASHBOARD)
                }
                DashboardUIState.EMPTY -> {
                    portfolioRecyclerView.gone()
                    emptyPortfolioGroup.visible()
                    dashboardProgress.gone()
                    momentLogger.endEvent(MomentEvent.PIN_TO_DASHBOARD)
                }
                DashboardUIState.LOADING -> {
                    portfolioRecyclerView.gone()
                    emptyPortfolioGroup.gone()
                    dashboardProgress.visible()
                }
            }
        }
    }

    /**
     * Once the dashboard is fully loaded, and there is money on account
     * -> verify app rating
     */
    private fun verifyAppRating(state: DashboardState) {
        if (isDashboardLoading(state).not() && state.dashboardBalance?.fiatBalance?.isPositive == true) {
            model.process(DashboardIntent.VerifyAppRating)
        }
    }

    private fun handleStateNavigation(navigationAction: DashboardNavigationAction) {
        when (navigationAction) {
            DashboardNavigationAction.AppRating -> {
                showAppRating()
            }
            is DashboardNavigationAction.BottomSheet -> {
                handleBottomSheet(navigationAction)
                model.process(DashboardIntent.ResetNavigation)
            }
            is DashboardNavigationAction.LinkBankWithPartner -> {
                startBankLinking(navigationAction)
            }
            is DashboardNavigationAction.DashboardOnboarding -> {
                launchDashboardOnboarding(navigationAction.initialSteps)
                model.process(DashboardIntent.ResetNavigation)
            }
            is DashboardNavigationAction.TransactionFlow -> {
                startActivity(
                    TransactionFlowActivity.newIntent(
                        context = requireActivity(),
                        sourceAccount = navigationAction.sourceAccount,
                        target = navigationAction.target,
                        action = navigationAction.action
                    )
                )
                model.process(DashboardIntent.ResetNavigation)
            }
            is DashboardNavigationAction.Coinview -> {
                activityResultsContract.launch(
                    CoinViewActivity.newIntent(
                        context = requireContext(),
                        asset = navigationAction.asset,
                        originScreen = LaunchOrigin.HOME.name,
                    )
                )
                model.process(DashboardIntent.ResetNavigation)
            }
            is DashboardNavigationAction.LinkWithAlias -> {
                bankAliasLinkLauncher.launch(
                    navigationAction.fiatAccount?.currency?.networkTicker
                        ?: currencyPrefs.selectedFiatCurrency.networkTicker
                )
            }
            is DashboardNavigationAction.BackUpBeforeSend,
            is DashboardNavigationAction.FiatDepositOrWithdrawalBlockedDueToSanctions,
            is DashboardNavigationAction.FiatFundsDetails,
            DashboardNavigationAction.FiatFundsNoKyc,
            is DashboardNavigationAction.InterestSummary,
            is DashboardNavigationAction.LinkOrDeposit,
            is DashboardNavigationAction.PaymentMethods,
            DashboardNavigationAction.SimpleBuyCancelOrder,
            is DashboardNavigationAction.DepositQuestionnaire ->
                Timber.e("Unhandled navigation event $navigationAction")
        }
    }

    private fun showAppRating() {
        AppRatingFragment.newInstance(AppRatingTriggerSource.DASHBOARD)
            .show(childFragmentManager, AppRatingFragment.TAG)
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
                    AssetAction.FiatWithdraw -> {
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
                is DashboardNavigationAction.FiatDepositOrWithdrawalBlockedDueToSanctions ->
                    BlockedDueToSanctionsSheet.newInstance(navigationAction.reason)
                is DashboardNavigationAction.DepositQuestionnaire -> {
                    questionnaireCallbackIntent = navigationAction.callbackIntent
                    QuestionnaireSheet.newInstance(navigationAction.questionnaire, true)
                }
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
        card?.let {
            theAdapter.items = theAdapter.items.plus(it)
        } ?: kotlin.run {
            theAdapter.items = theAdapter.items.minus { it is AnnouncementCard }
        }
    }

    private fun updateAnalytics(oldState: DashboardState?, newState: DashboardState) {
        analyticsReporter.updateFiatTotal(newState.dashboardBalance?.fiatBalance)

        newState.activeAssets.forEach { (asset, state) ->
            val newBalance = state.accountBalance?.total
            if (newBalance != null && newBalance != oldState?.activeAssets?.getOrNull(asset)?.accountBalance?.total) {
                // If we have the full set, this will fire
                (asset as? AssetInfo)?.let {
                    analyticsReporter.gotAssetBalance(asset, newBalance, newState.activeAssets.size)
                }
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

    private fun renderCowboysFlow(cowboysState: DashboardCowboysState) {
        with(binding.cardCowboys) {
            when (cowboysState) {
                is DashboardCowboysState.CowboyWelcomeCard ->
                    showCowboysCard(
                        cardInfo = cowboysState.cardInfo,
                        onClick = {
                            analytics.logEvent(CowboysAnalytics.VerifyEmailAnnouncementClicked)
                            startActivity(
                                CowboysFlowActivity.newIntent(requireContext(), FlowStep.Welcome)
                            )
                        }
                    )
                is DashboardCowboysState.CowboyRaffleCard ->
                    showCowboysCard(
                        cardInfo = cowboysState.cardInfo,
                        onClick = {
                            analytics.logEvent(CowboysAnalytics.CompleteSignupAnnouncementClicked)
                            startActivity(
                                CowboysFlowActivity.newIntent(requireContext(), FlowStep.Welcome)
                            )
                        }
                    )
                is DashboardCowboysState.CowboyIdentityCard ->
                    showCowboysCard(
                        cardInfo = cowboysState.cardInfo,
                        onClick = {
                            analytics.logEvent(CowboysAnalytics.VerifyIdAnnouncementClicked)
                            startActivity(
                                CowboysFlowActivity.newIntent(requireContext(), FlowStep.Verify)
                            )
                        }
                    )
                is DashboardCowboysState.CowboyReferFriendsCard ->
                    showCowboysCard(
                        cardInfo = cowboysState.cardInfo,
                        onClick = {
                            if (cowboysState.referralData is ReferralInfo.Data) {
                                analytics.logEvent(CowboysAnalytics.ReferFriendAnnouncementClicked)
                                showBottomSheet(ReferralSheet.newInstance(cowboysState.referralData))
                            }
                        },
                        isDismissable = true,
                        onDismiss = {
                            model.process(DashboardIntent.CowboysReferralCardClosed)
                            gone()
                        }
                    )
                is DashboardCowboysState.Hidden -> gone()
            }
        }
    }

    private fun CustomBackgroundCardView.showCowboysCard(
        cardInfo: PromotionStyleInfo,
        onClick: () -> Unit,
        isDismissable: Boolean = false,
        onDismiss: () -> Unit = {}
    ) {
        visible()
        title = cardInfo.title
        subtitle = cardInfo.message
        backgroundResource = ImageResource.Remote(cardInfo.backgroundUrl)
        iconResource = ImageResource.Remote(cardInfo.iconUrl)
        isCloseable = isDismissable
        this.onClick = onClick
        onClose = onDismiss
    }

    private fun setupCtaButtons(state: DashboardState) {
        with(binding) {
            buyCryptoButton.setOnClickListener { navigator().launchBuySell() }
            receiveDepositButton.apply {
                visibleIf { state.uiState == DashboardUIState.EMPTY && state.canPotentiallyTransactWithBanks }
                leftButton.setOnClickListener { navigator().launchReceive() }
                rightButton.setOnClickListener {
                    model.process(DashboardIntent.StartBankTransferFlow(action = AssetAction.FiatDeposit))
                }
            }
            receiveButton.apply {
                visibleIf { state.uiState == DashboardUIState.EMPTY && !state.canPotentiallyTransactWithBanks }
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
    }

    private fun setupSwipeRefresh() {
        with(binding) {
            swipe.setOnRefreshListener {
                model.process(DashboardIntent.OnSwipeToRefresh)
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

        announcements.checkLatest(announcementHost, compositeDisposable)
        model.process(DashboardIntent.FetchOnboardingSteps)
        model.process(DashboardIntent.CheckCowboysFlow)
        model.process(DashboardIntent.GetActiveAssets(loadSilently = true))
        model.process(DashboardIntent.FetchReferralSuccess)
    }

    // This method doesn't get called when we use the split portfolio/prices dashboard.
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            model.process(
                DashboardIntent.GetActiveAssets(
                    loadSilently = activeFiat == currencyPrefs.selectedFiatCurrency
                )
            )
            activeFiat = currencyPrefs.selectedFiatCurrency
            model.process(DashboardIntent.FetchOnboardingSteps)
        }
    }

    override fun onPause() {
        // Save the sort order for use elsewhere, so that other asset lists can have the same
        // ordering. Storing this through prefs is a bit of a hack, um, "optimisation" - we don't
        // want to be getting all the balances every time we want to display assets in balance order.
        // TODO This UI is due for a re-write soon, at which point this ordering should be managed better
        dashboardPrefs.dashboardAssetOrder = theAdapter.items.filterIsInstance<DashboardAsset>()
            .map { it.currency.displayTicker }
        compositeDisposable.clear()
        rxBus.unregister(ActionEvent::class.java, actionEvent)
        super.onPause()
    }

    override fun questionnaireSubmittedSuccessfully() {
        questionnaireCallbackIntent?.let {
            model.process(it)
        }
    }

    override fun questionnaireSkipped() {
        questionnaireCallbackIntent?.let {
            model.process(it)
        }
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
            model.process(DashboardIntent.ResetNavigation)
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
        }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            MainActivity.SETTINGS_EDIT,
            MainActivity.ACCOUNT_EDIT,
            -> model.process(DashboardIntent.GetActiveAssets(false))
            BACKUP_FUNDS_REQUEST_CODE -> {
                state?.backupSheetDetails?.let {
                    model.process(DashboardIntent.CheckBackupStatus(it.account, it.action))
                }
            }
        }

        model.process(DashboardIntent.ResetNavigation)
    }

    private fun onAssetClicked(asset: AssetInfo) {
        analytics.logEvent(assetActionEvent(AssetDetailsAnalytics.WALLET_DETAILS, asset))
        model.process(

            DashboardIntent.UpdateNavigationAction(
                DashboardNavigationAction.Coinview(
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

    private fun showReferralSuccess(successData: Pair<String, String>) {
        binding.referralSuccess.apply {
            title = successData.first
            subtitle = successData.second
            onClose = {
                model.process(DashboardIntent.DismissReferralSuccess)
                gone()
            }
            visible()
        }
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

        override fun finishSimpleBuySignup() {
            navigator().resumeSimpleBuyKyc()
        }

        override fun startSimpleBuy(asset: AssetInfo, paymentMethodId: String?) {
            navigator().launchSimpleBuy(asset, paymentMethodId)
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

        override fun joinNftWaitlist() {
            model.process(DashboardIntent.JoinNftWaitlist)
        }
    }

    // DialogBottomSheet.Host
    override fun onSheetClosed() {
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
                model.process(DashboardIntent.LaunchBankTransferFlow(it, AssetAction.FiatWithdraw, true))
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
        model.process(DashboardIntent.LaunchBankTransferFlow(fiatAccount, AssetAction.FiatWithdraw, false))
    }

    override fun startDepositFlow(fiatAccount: FiatAccount) {
        model.process(DashboardIntent.LaunchBankTransferFlow(fiatAccount, AssetAction.FiatDeposit, false))
    }

    // KycBenefitsBottomSheet.Host
    override fun verificationCtaClicked() {
        navigator().launchKyc(CampaignType.FiatFunds)
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
                DashboardIntent.UpdateNavigationAction(
                    DashboardNavigationAction.TransactionFlow(
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
            fiatCurrency: String? = null,
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

        const val BACKUP_FUNDS_REQUEST_CODE = 8265
    }
}

internal class SafeLayoutManager(context: Context) : LinearLayoutManager(context) {
    override fun supportsPredictiveItemAnimations() = false
}

@Preview
@Composable
fun CustomBackgroundCard_Basic() {
    AppTheme {
        AppSurface {
            CustomBackgroundCard(
                title = "Title",
                subtitle = "Subtitle",
                iconResource = ImageResource.Local(R.drawable.ic_temp_cowboys_icon),
                backgroundResource = ImageResource.Local(R.drawable.ic_temp_cowboys_header),
                isCloseable = false
            )
        }
    }
}
