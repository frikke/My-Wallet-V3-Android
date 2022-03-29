package piuk.blockchain.android.ui.dashboard

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.InterestAccount
import com.blockchain.coincore.SingleAccount
import com.blockchain.coincore.impl.CustodialTradingAccount
import com.blockchain.core.price.Prices24HrWithDelta
import com.blockchain.extensions.exhaustive
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.BlockedReason
import com.blockchain.nabu.FeatureAccess
import com.blockchain.notifications.analytics.LaunchOrigin
import com.blockchain.preferences.CurrencyPrefs
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.disposables.CompositeDisposable
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.campaign.blockstackCampaignName
import piuk.blockchain.android.databinding.FragmentPricesBinding
import piuk.blockchain.android.simplebuy.SimpleBuyAnalytics
import piuk.blockchain.android.simplebuy.sheets.BuyPendingOrdersBottomSheet
import piuk.blockchain.android.simplebuy.sheets.SimpleBuyCancelOrderBottomSheet
import piuk.blockchain.android.ui.airdrops.AirdropStatusSheet
import piuk.blockchain.android.ui.customviews.BlockchainListDividerDecor
import piuk.blockchain.android.ui.customviews.KycBenefitsBottomSheet
import piuk.blockchain.android.ui.customviews.VerifyIdentityNumericBenefitItem
import piuk.blockchain.android.ui.dashboard.adapter.PricesDelegateAdapter
import piuk.blockchain.android.ui.dashboard.assetdetails.AssetDetailsAnalytics
import piuk.blockchain.android.ui.dashboard.assetdetails.AssetDetailsFlow
import piuk.blockchain.android.ui.dashboard.assetdetails.FullScreenCoinViewFlow
import piuk.blockchain.android.ui.dashboard.assetdetails.assetActionEvent
import piuk.blockchain.android.ui.dashboard.coinview.CoinViewActivity
import piuk.blockchain.android.ui.dashboard.model.AssetPriceState
import piuk.blockchain.android.ui.dashboard.model.CryptoAssetState
import piuk.blockchain.android.ui.dashboard.model.DashboardIntent
import piuk.blockchain.android.ui.dashboard.model.DashboardModel
import piuk.blockchain.android.ui.dashboard.model.DashboardState
import piuk.blockchain.android.ui.dashboard.model.LinkablePaymentMethodsForAction
import piuk.blockchain.android.ui.dashboard.navigation.DashboardNavigationAction
import piuk.blockchain.android.ui.dashboard.sheets.FiatFundsDetailSheet
import piuk.blockchain.android.ui.dashboard.sheets.ForceBackupForSendSheet
import piuk.blockchain.android.ui.dashboard.sheets.LinkBankMethodChooserBottomSheet
import piuk.blockchain.android.ui.dashboard.sheets.WireTransferAccountDetailsBottomSheet
import piuk.blockchain.android.ui.home.HomeScreenMviFragment
import piuk.blockchain.android.ui.home.MainActivity
import piuk.blockchain.android.ui.interest.InterestSummarySheet
import piuk.blockchain.android.ui.linkbank.BankAuthActivity
import piuk.blockchain.android.ui.linkbank.BankAuthSource
import piuk.blockchain.android.ui.resources.AssetResources
import piuk.blockchain.android.ui.sell.BuySellFragment
import piuk.blockchain.android.ui.settings.v2.BankLinkingHost
import piuk.blockchain.android.ui.transactionflow.DialogFlow
import piuk.blockchain.android.ui.transactionflow.TransactionFlow
import piuk.blockchain.android.ui.transactionflow.flow.TransactionFlowActivity
import piuk.blockchain.android.util.AfterTextChangedWatcher
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import timber.log.Timber

data class PricesItem(
    val asset: AssetInfo,
    val priceWithDelta: Prices24HrWithDelta? = null
) {
    val assetName = asset.name
}

internal class PricesFragment :
    HomeScreenMviFragment<DashboardModel, DashboardIntent, DashboardState, FragmentPricesBinding>(),
    ForceBackupForSendSheet.Host,
    FiatFundsDetailSheet.Host,
    KycBenefitsBottomSheet.Host,
    DialogFlow.FlowHost,
    AssetDetailsFlow.AssetDetailsHost,
    InterestSummarySheet.Host,
    BuyPendingOrdersBottomSheet.Host,
    BankLinkingHost {

    override val model: DashboardModel by scopedInject()
    private val currencyPrefs: CurrencyPrefs by inject()
    private val assetResources: AssetResources by inject()

    private val theAdapter: PricesDelegateAdapter by lazy {
        PricesDelegateAdapter(
            prefs = currencyPrefs,
            onPriceRequest = { onGetAssetPrice(it) },
            onCardClicked = { onAssetClicked(it) },
            assetResources = assetResources
        )
    }

    private val theLayoutManager: RecyclerView.LayoutManager by unsafeLazy {
        SafeLayoutManager(requireContext())
    }

    private val compositeDisposable = CompositeDisposable()

    // Hold the 'current' display state, to enable optimising of state updates
    private var state: DashboardState? = null

    @UiThread
    override fun render(newState: DashboardState) {
        try {
            doRender(newState)
        } catch (e: Throwable) {
            Timber.e("Error rendering: $e")
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
            state?.activeFlow?.let {
                clearBottomSheet()
            }

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
                        startActivity(CoinViewActivity.newIntent(requireContext(), it.asset))
                        model.process(DashboardIntent.ClearActiveFlow)
                    }
                    else -> {
                        it.startFlow(childFragmentManager, this)
                    }
                }
            }
        }
        this.state = newState
    }

    private fun updateDisplayList(newState: DashboardState) {
        // Get the active assets sorted by balance
        val activeAssets = newState.activeAssets.values.sortedWith(
            compareByDescending<CryptoAssetState> { it.fiatBalance?.toBigInteger() }
                .thenBy { it.currency.name }
        ).map { it.toAssetPriceState() }
        // Get all the available assets
        val availableAssets = newState.availablePrices.values
        // Merge active and available, maintaining the order - active assets with biggest balances first
        val sortedAssets = activeAssets.toSet().plus(availableAssets).filter { assetPriceState ->
            newState.filterBy.isBlank() ||
                assetPriceState.assetInfo.name.contains(newState.filterBy, ignoreCase = true) ||
                assetPriceState.assetInfo.displayTicker.contains(newState.filterBy, ignoreCase = true)
        }

        binding.searchBoxLayout.apply {
            updateResults(resultCount = sortedAssets.size.toString(), shouldShow = newState.filterBy.isNotEmpty())
            updateLayoutState()
        }

        theAdapter.items = sortedAssets.toList().map {
            PricesItem(
                asset = it.assetInfo,
                priceWithDelta = it.prices
            )
        }
    }

    override fun onBackPressed(): Boolean = false

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentPricesBinding =
        FragmentPricesBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSwipeRefresh()
        setupRecycler()
        setupSearchBox()
    }

    private fun setupRecycler() {
        binding.recyclerView.apply {
            layoutManager = theLayoutManager
            adapter = theAdapter

            addItemDecoration(BlockchainListDividerDecor(requireContext()))
        }
    }

    private fun setupSwipeRefresh() {
        with(binding) {
            swipe.setOnRefreshListener {
                model.process(
                    DashboardIntent.GetAvailableAssets
                )
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

    private fun setupSearchBox() {
        binding.searchBoxLayout.setDetails(
            hint = R.string.search_coins_hint,
            textWatcher = object : AfterTextChangedWatcher() {
                override fun afterTextChanged(s: Editable?) {
                    s?.let { editable ->
                        model.process(DashboardIntent.FilterAssets(editable.toString()))
                    }
                }
            }
        )
    }

    override fun onResume() {
        super.onResume()
        if (isHidden) return
        initOrUpdateAssets()
    }

    private fun initOrUpdateAssets() {
        model.process(DashboardIntent.GetAvailableAssets)
    }

    override fun onPause() {
        compositeDisposable.clear()
        super.onPause()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            MainActivity.SETTINGS_EDIT,
            PortfolioFragment.BACKUP_FUNDS_REQUEST_CODE -> {
                state?.backupSheetDetails?.let {
                    model.process(DashboardIntent.CheckBackupStatus(it.account, it.action))
                }
            }
            BankAuthActivity.LINK_BANK_REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK) {
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

    private fun onGetAssetPrice(asset: AssetInfo) {
        model.process(DashboardIntent.GetAssetPrice(asset))
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

    private fun handleStateNavigation(navigationAction: DashboardNavigationAction) {
        when {
            navigationAction is DashboardNavigationAction.BottomSheet -> {
                handleBottomSheet(navigationAction)
                model.process(DashboardIntent.ResetDashboardNavigation)
            }
            navigationAction is DashboardNavigationAction.LinkBankWithPartner -> {
                startBankLinking(navigationAction)
            }
        }
    }

    private fun startBankLinking(action: DashboardNavigationAction.LinkBankWithPartner) {
        startActivityForResult(
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
            ),
            BankAuthActivity.LINK_BANK_REQUEST_CODE
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

    override fun startActivityRequested() {
        navigator().performAssetActionFor(AssetAction.ViewActivity)
    }

    // DialogBottomSheet.Host
    override fun onSheetClosed() {
        model.process(DashboardIntent.ClearActiveFlow)
    }

    // DialogFlow.FlowHost
    override fun onFlowFinished() {
        model.process(DashboardIntent.ClearActiveFlow)
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

    // ForceBackupForSendSheet.Host
    override fun startBackupForTransfer() {
        navigator().launchBackupFunds(this, PortfolioFragment.BACKUP_FUNDS_REQUEST_CODE)
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
        fun newInstance() = PricesFragment()
    }

    private fun CryptoAssetState.toAssetPriceState() =
        AssetPriceState(
            assetInfo = currency,
            prices = prices24HrWithDelta
        )
}
