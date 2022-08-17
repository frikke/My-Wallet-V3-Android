package piuk.blockchain.android.ui.dashboard.coinview

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.blockchain.analytics.events.LaunchOrigin
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.InterestAccount
import com.blockchain.coincore.NonCustodialAccount
import com.blockchain.coincore.StateAwareAction
import com.blockchain.coincore.TradingAccount
import com.blockchain.coincore.TransactionTarget
import com.blockchain.commonarch.presentation.mvi.MviActivity
import com.blockchain.componentlib.alert.AlertType
import com.blockchain.componentlib.alert.BlockchainSnackbar
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.charts.PercentageChangeData
import com.blockchain.componentlib.databinding.ToolbarGeneralBinding
import com.blockchain.componentlib.expandables.ExpandableItem
import com.blockchain.componentlib.sectionheader.BalanceSectionHeaderView
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.setMargins
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.core.price.HistoricalRateList
import com.blockchain.core.price.HistoricalTimeSpan
import com.blockchain.core.price.Prices24HrWithDelta
import com.blockchain.extensions.enumValueOfOrNull
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.BlockedReason
import com.blockchain.nabu.models.data.RecurringBuy
import com.blockchain.preferences.LocalSettingsPrefs
import com.blockchain.wallet.DefaultLabels
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import com.github.mikephil.charting.data.Entry
import com.google.android.material.snackbar.Snackbar
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.databinding.ActivityCoinviewBinding
import piuk.blockchain.android.simplebuy.CustodialBalanceClicked
import piuk.blockchain.android.simplebuy.SimpleBuyActivity
import piuk.blockchain.android.simplebuy.SimpleBuySyncFactory
import piuk.blockchain.android.support.SupportCentreActivity
import piuk.blockchain.android.ui.customviews.BlockchainListDividerDecor
import piuk.blockchain.android.ui.customviews.BlockedDueToSanctionsSheet
import piuk.blockchain.android.ui.dashboard.coinview.accounts.AccountsAdapterDelegate
import piuk.blockchain.android.ui.dashboard.coinview.interstitials.AccountActionsBottomSheet
import piuk.blockchain.android.ui.dashboard.coinview.interstitials.AccountExplainerBottomSheet
import piuk.blockchain.android.ui.dashboard.coinview.interstitials.NoBalanceActionBottomSheet
import piuk.blockchain.android.ui.dashboard.coinview.recurringbuy.RecurringBuyDetailsSheet
import piuk.blockchain.android.ui.dashboard.sheets.KycUpgradeNowSheet
import piuk.blockchain.android.ui.interest.InterestSummarySheet
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity
import piuk.blockchain.android.ui.recurringbuy.RecurringBuyAnalytics
import piuk.blockchain.android.ui.recurringbuy.onboarding.RecurringBuyOnboardingActivity
import piuk.blockchain.android.ui.resources.AssetResources
import piuk.blockchain.android.ui.transactionflow.flow.TransactionFlowActivity
import piuk.blockchain.android.ui.transfer.receive.detail.ReceiveDetailSheet
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.putAccount

class CoinViewActivity :
    MviActivity<CoinViewModel, CoinViewIntent, CoinViewState, ActivityCoinviewBinding>(),
    RecurringBuyDetailsSheet.Host,
    AccountExplainerBottomSheet.Host,
    AccountActionsBottomSheet.Host,
    InterestSummarySheet.Host,
    NoBalanceActionBottomSheet.Host,
    KycUpgradeNowSheet.Host {

    override val alwaysDisableScreenshots: Boolean
        get() = false

    override val toolbarBinding: ToolbarGeneralBinding
        get() = binding.toolbar

    override val model: CoinViewModel by scopedInject()

    private val assetTicker: String by lazy {
        intent.getStringExtra(ASSET_TICKER).orEmpty()
    }

    private val assetName: String by lazy {
        intent.getStringExtra(ASSET_NAME).orEmpty()
    }

    private val originName: LaunchOrigin? by lazy {
        enumValueOfOrNull<LaunchOrigin>(intent.getStringExtra(ORIGIN_NAME).orEmpty())
    }

    private val labels: DefaultLabels by inject()
    private val assetResources: AssetResources by inject()
    private val localSettingsPrefs: LocalSettingsPrefs by inject()

    private val listItems = mutableListOf<AssetDetailsItem>()
    private lateinit var historicalGraphData: HistoricalRateList
    private lateinit var prices24Hr: Prices24HrWithDelta
    private lateinit var selectedFiat: FiatCurrency

    private var ctaActions: List<QuickActionCta> = emptyList()

    private val simpleBuySyncFactory: SimpleBuySyncFactory by scopedInject()

    private val walletMode = get<WalletModeService>().enabledWalletMode()

    override fun initBinding(): ActivityCoinviewBinding = ActivityCoinviewBinding.inflate(layoutInflater)

    private val adapterDelegate by lazy {
        AccountsAdapterDelegate(
            onAccountSelected = ::onAccountSelected,
            onLockedAccountSelected = ::showUpgradeKycSheet,
            labels = labels,
            swapOnClick = ::startSwap,
            onCardClicked = ::openOnboardingForRecurringBuy,
            onRecurringBuyClicked = ::onRecurringBuyClicked,
            assetResources = assetResources
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        updateToolbar(toolbarTitle = assetName, backAction = {
            analytics.logEvent(
                CoinViewAnalytics
                    .CoinViewClosed(
                        closingMethod = CoinViewAnalytics.Companion.ClosingMethod.BACK_BUTTON,
                        currency = assetTicker
                    )
            )
            onBackPressedDispatcher.onBackPressed()
        })
        originName?.let {
            analytics.logEvent(
                CoinViewAnalytics.CoinViewOpen(
                    origin = it,
                    currency = assetTicker,
                )
            )
        }
        initUI()
    }

    private fun initUI() {
        with(binding) {
            assetList.apply {

                if (walletMode == WalletMode.NON_CUSTODIAL_ONLY) {
                    foreground = getDrawable(R.drawable.rounded_view_grey_100_border_16_radius)
                    setMargins(
                        start = resources.getDimensionPixelSize(R.dimen.small_margin),
                        top = resources.getDimensionPixelSize(R.dimen.small_margin),
                        end = resources.getDimensionPixelSize(R.dimen.small_margin),
                        bottom = resources.getDimensionPixelSize(R.dimen.standard_margin),
                    )
                }

                adapter = adapterDelegate
                addItemDecoration(BlockchainListDividerDecor(this@CoinViewActivity))
            }

            binding.swapCta.apply {
                text = context.getString(R.string.common_swap)
                icon = ImageResource.Local(R.drawable.ic_cta_swap, size = 24.dp)
            }

            assetPricesLoading.showIconLoader = false
            assetBalancesLoading.showIconLoader = false
            assetInfoLoading.showIconLoader = false

            // not showing asset balance on defi
            if (walletMode == WalletMode.NON_CUSTODIAL_ONLY) {
                assetBalancesSwitcher.gone()
            } else {
                assetBalance.apply {
                    shouldShowIcon = true
                    onIconClick = {
                        model.process(CoinViewIntent.ToggleWatchlist)
                    }
                }
            }

            assetAboutTitle.apply {
                textColor = ComposeColors.Title
                style = ComposeTypographies.Body2
            }

            initNoAssetError()
            initAssetChart()
            renderViewsLoading()
            showLoadingCtas()
        }
    }

    private fun initNoAssetError() {
        with(binding) {
            noAssetTitle.apply {
                text = getString(R.string.coinview_no_asset_title)
                style = ComposeTypographies.Title3
                textColor = ComposeColors.Title
                gravity = ComposeGravities.Centre
            }

            noAssetBlurb.apply {
                movementMethod = LinkMovementMethod.getInstance()
                setText(
                    StringUtils.getStringWithMappedAnnotations(
                        this@CoinViewActivity,
                        R.string.coinview_no_asset_blurb,
                        emptyMap()
                    ) {
                        startActivity(SupportCentreActivity.newIntent(this@CoinViewActivity, SUPPORT_SUBJECT_NO_ASSET))
                        this@CoinViewActivity.finish()
                    },
                    TextView.BufferType.SPANNABLE
                )
            }
        }
    }

    private fun initAssetChart() {
        with(binding) {
            assetChartLoading.loadingText = getString(R.string.coinview_chart_loading)

            assetChart.apply {
                isChartLive = false
                onEntryHighlighted = { entry ->
                    updateScrubPriceInformation(entry)
                }
                onActionPressDown = {
                    analytics.logEvent(
                        CoinViewAnalytics.ChartEngaged(
                            origin = LaunchOrigin.COIN_VIEW,
                            currency = assetTicker,
                            timeInterval = stringPositionToTimeInterval(binding.chartControls.selectedItemIndex)
                        )
                    )
                }
                onScrubRelease = {
                    analytics.logEvent(
                        CoinViewAnalytics.ChartDisengaged(
                            origin = LaunchOrigin.COIN_VIEW,
                            currency = assetTicker,
                            timeInterval = stringPositionToTimeInterval(binding.chartControls.selectedItemIndex)
                        )
                    )
                    renderPriceInformation(
                        prices24Hr,
                        historicalGraphData,
                        selectedFiat
                    )
                }
                shouldVibrate = localSettingsPrefs.isChartVibrationEnabled
            }

            chartControls.apply {
                items = getTimeIntervalItems()
                onItemSelected = {
                    analytics.logEvent(
                        CoinViewAnalytics.ChartTimeIntervalSelected(
                            origin = LaunchOrigin.COIN_VIEW,
                            currency = assetTicker,
                            timeInterval = stringPositionToTimeInterval(it)
                        )
                    )
                    model.process(CoinViewIntent.LoadNewChartPeriod(HistoricalTimeSpan.fromInt(it)))
                }
                selectedItemIndex = 0
                showLiveIndicator = false
            }

            assetChartError.apply {
                isBordered = true
                title = getString(R.string.coinview_chart_load_error_title)
                subtitle = getString(R.string.coinview_chart_load_error_subtitle)
                alertType = AlertType.Warning
                isDismissable = false
            }
        }
    }

    private fun ActivityCoinviewBinding.renderViewsLoading() {
        assetChartViewSwitcher.displayedChild = CHART_LOADING
        assetAccountsViewSwitcher.displayedChild = ACCOUNTS_LOADING
        assetPricesSwitcher.displayedChild = PRICES_LOADING
        assetBalancesSwitcher.displayedChild = BALANCES_LOADING
        assetInformationSwitcher.displayedChild = INFO_LOADING
    }

    private fun getTimeIntervalItems(): List<String> =
        listOf(
            getString(R.string.coinview_chart_tab_day),
            getString(R.string.coinview_chart_tab_week),
            getString(R.string.coinview_chart_tab_month),
            getString(R.string.coinview_chart_tab_year),
            getString(R.string.coinview_chart_tab_all)
        )

    private fun stringPositionToTimeInterval(position: Int): CoinViewAnalytics.Companion.TimeInterval =
        when (HistoricalTimeSpan.fromInt(position)) {
            HistoricalTimeSpan.DAY -> CoinViewAnalytics.Companion.TimeInterval.DAY
            HistoricalTimeSpan.WEEK -> CoinViewAnalytics.Companion.TimeInterval.WEEK
            HistoricalTimeSpan.MONTH -> CoinViewAnalytics.Companion.TimeInterval.MONTH
            HistoricalTimeSpan.YEAR -> CoinViewAnalytics.Companion.TimeInterval.YEAR
            HistoricalTimeSpan.ALL_TIME -> CoinViewAnalytics.Companion.TimeInterval.ALL_TIME
            else -> CoinViewAnalytics.Companion.TimeInterval.LIVE
        }

    private fun ActivityCoinviewBinding.updateScrubPriceInformation(entry: Entry) {
        val dataForEntry = historicalGraphData.firstOrNull {
            it.timestamp.toFloat() == entry.x
        }

        dataForEntry?.let { historicalRate ->
            val firstForPeriod = historicalGraphData.first()
            val difference = historicalRate.rate - firstForPeriod.rate

            val percentChange = (difference / firstForPeriod.rate) * 100

            val changeDifference = Money.fromMajor(selectedFiat, difference.toBigDecimal()).toStringWithSymbol()

            assetPrice.apply {
                price = Money.fromMajor(selectedFiat, dataForEntry.rate.toBigDecimal()).toStringWithSymbol()
                percentageChangeData = PercentageChangeData(
                    priceChange = changeDifference,
                    percentChange = percentChange / 100,
                    interval = ""
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        simpleBuySyncFactory.cancelAnyPendingConfirmationBuy()
        model.process(CoinViewIntent.LoadAsset(assetTicker))
    }

    override fun render(newState: CoinViewState) {
        newState.asset?.let { cryptoAsset ->
            with(binding) {
                assetAboutTitle.text = getString(R.string.coinview_about_asset, cryptoAsset.currency.name)
                assetPrice.endIcon = ImageResource.Remote(url = cryptoAsset.currency.logo, shape = CircleShape)
                assetBalance.updateIcon(newState.isAddedToWatchlist)
            }
        }

        if (newState.viewState != CoinViewViewState.None) {
            renderUiState(newState)
        }

        if (newState.error != CoinViewError.None) {
            handleErrors(newState)
            model.process(CoinViewIntent.ResetErrorState)
        }
    }

    private fun handleErrors(newState: CoinViewState) =
        when (newState.error) {
            CoinViewError.UnknownAsset -> binding.noAssetError.visible()
            CoinViewError.WalletLoadError -> {
                if (!listItems.contains(AssetDetailsItem.AccountError) &&
                    listItems.none { it is AssetDetailsItem.CryptoDetailsInfo }
                ) {
                    addListError(AssetDetailsItem.AccountError)
                }
                BlockchainSnackbar.make(
                    binding.root, getString(R.string.coinview_wallet_load_error), type = SnackbarType.Error
                ).show()
            }
            CoinViewError.ChartLoadError -> {
                binding.assetChartViewSwitcher.displayedChild = CHART_ERROR
                BlockchainSnackbar.make(
                    binding.root, getString(R.string.coinview_chart_load_error), type = SnackbarType.Warning
                ).show()
            }
            CoinViewError.RecurringBuysLoadError -> {
                if (!listItems.contains(AssetDetailsItem.RecurringBuyError) &&
                    listItems.none { it is AssetDetailsItem.RecurringBuyInfo }
                ) {
                    addListError(AssetDetailsItem.RecurringBuyError)
                }
                BlockchainSnackbar.make(
                    binding.root, getString(R.string.coinview_recurring_buy_load_error), type = SnackbarType.Warning
                ).show()
            }
            CoinViewError.QuickActionsFailed -> {
                with(binding) {
                    ctasDivider.gone()
                    startCta.gone()
                    endCta.gone()
                }
            }
            CoinViewError.MissingSelectedFiat -> {
                binding.noAssetError.visible()
                BlockchainSnackbar.make(
                    binding.root, getString(R.string.coinview_fiat_missing), type = SnackbarType.Warning
                ).show()
            }
            CoinViewError.MissingAssetPrices -> {
                binding.noAssetError.visible()
                BlockchainSnackbar.make(
                    binding.root, getString(R.string.coinview_prices_missing), type = SnackbarType.Warning
                ).show()
            }
            CoinViewError.WatchlistUpdateFailed -> BlockchainSnackbar.make(
                binding.root, getString(R.string.coinview_watchlist_toggle_fail), type = SnackbarType.Warning
            ).show()
            CoinViewError.ActionsLoadError -> BlockchainSnackbar.make(
                binding.root, getString(R.string.coinview_actions_error), type = SnackbarType.Warning
            ).show()
            CoinViewError.None -> {
                // do nothing
            }
            CoinViewError.AssetDetailsLoadError -> binding.assetInformationSwitcher.gone()
        }

    private fun addListError(errorType: AssetDetailsItem) {
        listItems.add(errorType)
        updateList()
        binding.assetAccountsViewSwitcher.displayedChild = ACCOUNTS_LIST
    }

    private fun renderUiState(newState: CoinViewState) {
        when (val state = newState.viewState) {
            CoinViewViewState.LoadingWallets,
            CoinViewViewState.LoadingRecurringBuys,
            -> {
                binding.assetAccountsViewSwitcher.displayedChild = ACCOUNTS_LOADING
            }
            CoinViewViewState.LoadingChart -> binding.assetChartViewSwitcher.displayedChild = CHART_LOADING
            CoinViewViewState.LoadingQuickActions -> showLoadingCtas()
            CoinViewViewState.LoadingAssetDetails -> binding.assetInformationSwitcher.displayedChild = INFO_LOADING
            is CoinViewViewState.ShowAccountInfo -> {
                renderAccountsDetails(state.assetDetails)
                renderBalanceInformation(
                    state.totalCryptoBalance, state.totalFiatBalance, state.isAddedToWatchlist
                )
                binding.assetAccountsViewSwitcher.displayedChild = ACCOUNTS_LIST
            }
            is CoinViewViewState.ShowNonTradeableAccount -> renderNonTradeableAsset(newState, state.isAddedToWatchlist)
            is CoinViewViewState.ShowAssetInfo -> {
                with(binding) {
                    assetChartViewSwitcher.displayedChild = CHART_VIEW
                    assetChart.apply {
                        datePattern = HistoricalTimeSpan.fromInt(chartControls.selectedItemIndex).toDatePattern()
                        fiatSymbol = state.selectedFiat.symbol
                        setData(state.entries)
                    }
                }
                renderPriceInformation(state.prices, state.historicalRateList, state.selectedFiat)
            }
            is CoinViewViewState.ShowRecurringBuys -> {
                renderRecurringBuys(state.recurringBuys, state.shouldShowUpsell)
                binding.assetAccountsViewSwitcher.displayedChild = ACCOUNTS_LIST
            }
            is CoinViewViewState.QuickActionsLoaded -> {
                newState.asset?.let { asset ->
                    renderQuickActions(asset.currency, state.actionableAccount, state.startAction, state.endAction)
                }
            }
            is CoinViewViewState.UpdatedWatchlist -> renderWatchlistIcon(state.addedToWatchlist)
            is CoinViewViewState.ShowAccountExplainerSheet -> {
                require(newState.selectedCryptoAccount != null)
                with(newState.selectedCryptoAccount) {
                    showBottomSheet(
                        AccountExplainerBottomSheet.newInstance(
                            selectedAccount = account,
                            networkTicker = assetTicker,
                            interestRate = interestRate,
                            stateAwareActions = state.actions
                        )
                    )
                }
            }
            is CoinViewViewState.ShowAccountActionSheet -> {
                require(newState.selectedCryptoAccount != null)
                with(newState.selectedCryptoAccount) {
                    showBottomSheet(
                        AccountActionsBottomSheet.newInstance(
                            selectedAccount = account,
                            balanceFiat = fiatBalance,
                            balanceCrypto = balance,
                            interestRate = interestRate,
                            stateAwareActions = state.actions,
                            hasWarning = newState.hasActionBuyWarning
                        )
                    )
                }
            }
            is CoinViewViewState.ShowAssetDetails -> renderAssetInfo(state)
            CoinViewViewState.None -> {
                // do nothing
            }
        }

        model.process(CoinViewIntent.ResetViewState)
    }

    private fun renderAssetInfo(state: CoinViewViewState.ShowAssetDetails) {
        with(binding) {
            assetAboutTitle.apply {
                text = getString(R.string.coinview_about_asset, assetName)
                textColor = ComposeColors.Title
                style = ComposeTypographies.Body2
                visible()
            }

            with(state.details) {
                setWebsiteLink(website)
                setExpandableDescription(description)
            }

            assetInformationSwitcher.displayedChild = INFO_VIEW
        }
    }

    private fun setWebsiteLink(website: String) {
        binding.assetWebsite.apply {
            if (website.isNotEmpty()) {
                text = getString(R.string.coinview_asset_info_cta)
                onClick = { goToAssetWebsite(website) }
                visible()
            } else {
                gone()
            }
        }
    }

    private fun setExpandableDescription(description: String) {
        binding.assetAboutBlurb.setContent {
            AppTheme {
                if (description.isEmpty()) {
                    SimpleText(
                        text = getString(R.string.coinview_no_asset_description),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        style = ComposeTypographies.Paragraph1,
                        color = ComposeColors.Body,
                        gravity = ComposeGravities.Start
                    )
                } else {
                    ExpandableItem(
                        text = description,
                        numLinesVisible = VISIBLE_LINES_DESCRIPTION,
                        textButtonToExpand = getString(R.string.coinview_expandable_button),
                        textButtonToCollapse = getString(R.string.coinview_collapsable_button)
                    )
                }
            }
        }
    }

    private fun goToAssetWebsite(url: String) {
        analytics.logEvent(
            CoinViewAnalytics.HyperlinkClicked(
                origin = LaunchOrigin.COIN_VIEW,
                currency = assetTicker,
                selection = CoinViewAnalytics.Companion.Selection.LEARN_MORE
            )
        )

        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    private fun renderWatchlistIcon(addedToWatchlist: Boolean) {
        sendWatchlistAnalytics(addedToWatchlist)
        showSnackBarWatchlist(addedToWatchlist)
    }

    private fun showSnackBarWatchlist(addedToWatchlist: Boolean) {
        BlockchainSnackbar.make(
            binding.root,
            if (addedToWatchlist) {
                getString(R.string.coinview_added_watchlist)
            } else {
                getString(R.string.coinview_removed_watchlist)
            },
            type = SnackbarType.Success,
            duration = Snackbar.LENGTH_SHORT
        ).show()
    }

    private fun sendWatchlistAnalytics(addedToWatchlist: Boolean) {
        if (addedToWatchlist) {
            analytics.logEvent(
                CoinViewAnalytics.CoinAddedFromWatchlist(
                    origin = LaunchOrigin.COIN_VIEW,
                    currency = assetTicker
                )
            )
        } else {
            analytics.logEvent(
                CoinViewAnalytics.CoinRemovedFromWatchlist(
                    origin = LaunchOrigin.COIN_VIEW,
                    currency = assetTicker
                )
            )
        }
    }

    private fun renderNonTradeableAsset(newState: CoinViewState, isAddedToWatchlist: Boolean) {
        with(binding) {
            assetAccountsViewSwitcher.gone()
            ctasDivider.gone()
            startCta.gone()
            endCta.gone()
            nonTradeableCard.apply {
                visible()
                isDismissable = false
                title = getString(R.string.coinview_not_tradeable_title, assetName, assetTicker)
                subtitle = getString(R.string.coinview_not_tradeable_subtitle, assetName)
            }

            newState.asset?.currency?.let { assetInfo ->
                newState.selectedFiat?.let { selectedFiat ->
                    renderBalanceInformation(
                        totalCryptoBalance = hashMapOf(AssetFilter.All to CryptoValue.zero(assetInfo)),
                        totalFiatBalance = FiatValue.zero(selectedFiat),
                        isInWatchList = isAddedToWatchlist
                    )
                }
            }
        }
    }

    private fun showLoadingCtas() {
        with(binding) {
            startCta.buttonState = ButtonState.Loading
            endCta.buttonState = ButtonState.Loading
        }
    }

    private fun renderQuickActions(
        asset: AssetInfo,
        highestBalanceWallet: BlockchainAccount,
        startAction: QuickActionCta,
        endAction: QuickActionCta,
    ) {
        with(binding) {
            when {
                startAction == QuickActionCta.None && endAction == QuickActionCta.None -> {
                    startCta.gone()
                    ctasDivider.gone()
                    endCta.gone()
                }
                endAction == QuickActionCta.None -> {
                    startCta.gone()
                    updateEndCta(asset, highestBalanceWallet, startAction)
                }
                startAction == QuickActionCta.None -> {
                    startCta.gone()
                    updateEndCta(asset, highestBalanceWallet, endAction)
                }
                else -> {
                    updateEndCta(asset, highestBalanceWallet, endAction)
                    updateStartCta(asset, highestBalanceWallet, startAction)
                    ctaActions = listOf(startAction, endAction)
                }
            }
        }
    }

    private fun ActivityCoinviewBinding.updateStartCta(
        asset: AssetInfo,
        highestBalanceWallet: BlockchainAccount,
        startAction: QuickActionCta,
    ) {
        val startButtonResources = getQuickActionUi(asset, highestBalanceWallet, startAction)
        startCta.apply {
            buttonState = if (startButtonResources.isEnabled) ButtonState.Enabled else ButtonState.Disabled
            text = startButtonResources.name
            icon = startButtonResources.icon
            onClick = {
                startButtonResources.onClick()
            }
        }
    }

    private fun ActivityCoinviewBinding.updateEndCta(
        asset: AssetInfo,
        highestBalanceWallet: BlockchainAccount,
        endAction: QuickActionCta,
    ) {
        val endButtonResources = getQuickActionUi(asset, highestBalanceWallet, endAction)
        endCta.apply {
            buttonState = if (endButtonResources.isEnabled) ButtonState.Enabled else ButtonState.Disabled
            text = endButtonResources.name
            icon = endButtonResources.icon
            onClick = {
                endButtonResources.onClick()
            }
        }
    }

    private fun startBuy(asset: AssetInfo) {
        startActivity(
            SimpleBuyActivity.newIntent(
                context = this,
                asset = asset
            )
        )
    }

    private fun startSell(account: BlockchainAccount) {
        startActivity(
            TransactionFlowActivity.newIntent(
                context = this,
                action = AssetAction.Sell,
                sourceAccount = account
            )
        )
    }

    private fun startSend(account: BlockchainAccount) {
        startActivity(
            TransactionFlowActivity.newIntent(
                context = this,
                action = AssetAction.Send,
                sourceAccount = account
            )
        )
    }

    private fun startSwap(account: BlockchainAccount) {
        startActivity(
            TransactionFlowActivity.newIntent(
                context = this,
                action = AssetAction.Swap,
                sourceAccount = account
            )
        )
    }

    private fun startReceive(account: BlockchainAccount) {
        showBottomSheet(ReceiveDetailSheet.newInstance(account as CryptoAccount))
    }

    private fun startViewSummary(account: BlockchainAccount) {
        showBottomSheet(InterestSummarySheet.newInstance(account as CryptoAccount))
    }

    private fun logBuyEvent() {
        val isBuySell = ctaActions.contains(QuickActionCta.Sell(true))
        val isBuyReceive = ctaActions.contains(QuickActionCta.Receive(true))
        if (isBuySell) {
            analytics.logEvent(
                CoinViewAnalytics.BuySellClicked(
                    origin = LaunchOrigin.COIN_VIEW,
                    currency = assetTicker,
                    type = CoinViewAnalytics.Companion.Type.BUY
                )
            )
        } else if (isBuyReceive) {
            analytics.logEvent(
                CoinViewAnalytics.BuyReceiveClicked(
                    origin = LaunchOrigin.COIN_VIEW,
                    currency = assetTicker,
                    type = CoinViewAnalytics.Companion.Type.BUY
                )
            )
        }
    }

    private fun logReceiveEvent() {
        val isBuyReceive = ctaActions.contains(QuickActionCta.Buy(true))
        val isSendReceive = ctaActions.contains(QuickActionCta.Send(true))
        if (isBuyReceive) {
            analytics.logEvent(
                CoinViewAnalytics.BuyReceiveClicked(
                    origin = LaunchOrigin.COIN_VIEW,
                    currency = assetTicker,
                    type = CoinViewAnalytics.Companion.Type.RECEIVE
                )
            )
        } else if (isSendReceive) {
            analytics.logEvent(
                CoinViewAnalytics.SendReceiveClicked(
                    origin = LaunchOrigin.COIN_VIEW,
                    currency = assetTicker,
                    type = CoinViewAnalytics.Companion.Type.RECEIVE
                )
            )
        }
    }

    private fun getQuickActionUi(
        asset: AssetInfo,
        highestBalanceWallet: BlockchainAccount,
        action: QuickActionCta,
    ): QuickAction =
        when (action) {
            is QuickActionCta.Buy -> QuickAction(
                name = getString(R.string.common_buy),
                icon = ImageResource.Local(
                    R.drawable.ic_cta_buy,
                    colorFilter = ColorFilter.tint(
                        Color(ContextCompat.getColor(this@CoinViewActivity, R.color.white))
                    ),
                    size = 24.dp
                ),
                isEnabled = action.enabled
            ) {
                logBuyEvent()
                startBuy(asset)
            }
            is QuickActionCta.Sell -> QuickAction(
                name = getString(R.string.common_sell),
                icon = ImageResource.Local(
                    R.drawable.ic_cta_sell,
                    colorFilter = ColorFilter.tint(
                        Color(ContextCompat.getColor(this@CoinViewActivity, R.color.white))
                    ),
                    size = 24.dp
                ),
                isEnabled = action.enabled
            ) {
                analytics.logEvent(
                    CoinViewAnalytics.BuySellClicked(
                        origin = LaunchOrigin.COIN_VIEW,
                        currency = assetTicker,
                        type = CoinViewAnalytics.Companion.Type.SELL
                    )
                )
                startSell(highestBalanceWallet)
            }
            is QuickActionCta.Send -> QuickAction(
                name = getString(R.string.common_send),
                icon = ImageResource.Local(
                    R.drawable.ic_cta_sell,
                    colorFilter = ColorFilter.tint(
                        Color(ContextCompat.getColor(this@CoinViewActivity, R.color.white))
                    ),
                    size = 24.dp
                ),
                isEnabled = action.enabled
            ) {
                analytics.logEvent(
                    CoinViewAnalytics.SendReceiveClicked(
                        origin = LaunchOrigin.COIN_VIEW,
                        currency = assetTicker,
                        type = CoinViewAnalytics.Companion.Type.SEND
                    )
                )
                startSend(highestBalanceWallet)
            }
            is QuickActionCta.Receive -> QuickAction(
                name = getString(R.string.common_receive),
                icon = ImageResource.Local(
                    R.drawable.ic_cta_receive,
                    colorFilter = ColorFilter.tint(
                        Color(ContextCompat.getColor(this@CoinViewActivity, R.color.white))
                    ),
                    size = 24.dp
                ),
                isEnabled = action.enabled
            ) {
                logReceiveEvent()
                startReceive(highestBalanceWallet)
            }
            QuickActionCta.None -> {
                // do nothing
                QuickAction(getString(R.string.empty), ImageResource.None, false, {})
            }
        }

    private fun renderRecurringBuys(recurringBuys: List<RecurringBuy>, shouldShowUpsell: Boolean) {
        when {
            recurringBuys.isNotEmpty() -> {
                listItems.removeIf { details ->
                    details is AssetDetailsItem.RecurringBuyInfo
                }
                val recurringBuysItems = recurringBuys.map {
                    AssetDetailsItem.RecurringBuyInfo(it)
                }
                listItems.addAll(recurringBuysItems)
            }
            shouldShowUpsell -> {
                listItems.removeIf { details ->
                    details is AssetDetailsItem.RecurringBuyBanner
                }
                listItems.add(AssetDetailsItem.RecurringBuyBanner)
            }
        }
        updateList()
    }

    private fun renderBalanceInformation(
        totalCryptoBalance: Map<AssetFilter, Money>,
        totalFiatBalance: Money,
        isInWatchList: Boolean,
    ) {
        // not showing this view in defi
        if (walletMode == WalletMode.NON_CUSTODIAL_ONLY) return

        totalCryptoBalance[AssetFilter.All]?.let { cryptoBalance ->
            with(binding) {
                assetBalance.apply {
                    labelText = getString(R.string.coinview_balance_label, assetTicker)
                    primaryText = totalFiatBalance.toStringWithSymbol()
                    secondaryText = cryptoBalance.toStringWithSymbol()
                    updateIcon(isInWatchList)
                }
                assetBalancesSwitcher.displayedChild = BALANCES_VIEW
            }
        }
    }

    private fun BalanceSectionHeaderView.updateIcon(isInWatchList: Boolean) {
        iconResource = ImageResource.Local(if (isInWatchList) R.drawable.ic_star_filled else R.drawable.ic_star)
    }

    private fun renderPriceInformation(
        prices: Prices24HrWithDelta,
        historicalRateList: HistoricalRateList,
        selectedFiat: FiatCurrency,
    ) {
        prices24Hr = prices
        historicalGraphData = historicalRateList
        this.selectedFiat = selectedFiat

        val currentPrice = prices.currentRate.price.toStringWithSymbol()
        // We have filtered out nulls by here, so we can 'safely' default to zeros for the price
        val firstPrice: Double = historicalRateList.firstOrNull()?.rate ?: 0.0
        val lastPrice: Double = historicalRateList.lastOrNull()?.rate ?: 0.0
        val difference = lastPrice - firstPrice

        with(binding) {
            val percentChange =
                if (chartControls.selectedItemIndex == HistoricalTimeSpan.DAY.ordinal) {
                    prices.delta24h
                } else {
                    (difference / firstPrice) * 100
                }

            val changeDifference = Money.fromMajor(selectedFiat, difference.toBigDecimal()).toStringWithSymbol()

            assetPrice.apply {
                price = currentPrice
                percentageChangeData = PercentageChangeData(
                    changeDifference, percentChange / 100,
                    when (chartControls.selectedItemIndex) {
                        HistoricalTimeSpan.DAY.ordinal -> getString(R.string.coinview_price_day)
                        HistoricalTimeSpan.WEEK.ordinal -> getString(R.string.coinview_price_week)
                        HistoricalTimeSpan.MONTH.ordinal -> getString(R.string.coinview_price_month)
                        HistoricalTimeSpan.YEAR.ordinal -> getString(R.string.coinview_price_year)
                        HistoricalTimeSpan.ALL_TIME.ordinal -> getString(R.string.coinview_price_all)
                        else -> getString(R.string.empty)
                    }
                )
                title = getString(R.string.coinview_price_label, assetName)
            }

            assetPricesSwitcher.displayedChild = PRICES_VIEW
        }
    }

    private fun renderAccountsDetails(
        assetDetails: List<AssetDetailsItem.CryptoDetailsInfo>
    ) {
        val account = assetDetails.firstOrNull { it.balance.isPositive }?.account
        when (walletMode) {
            WalletMode.UNIVERSAL, WalletMode.CUSTODIAL_ONLY -> {
                listItems.removeIf { details ->
                    details is AssetDetailsItem.CentralCta
                }

                account?.let {
                    listItems.add(0, AssetDetailsItem.CentralCta(it))
                }
            }

            WalletMode.NON_CUSTODIAL_ONLY -> {
                binding.swapCta.apply {
                    account?.let {
                        visible()
                        onClick = { startSwap(it) }
                    } ?: kotlin.run {
                        gone()
                    }
                }
            }
        }

        listItems.removeIf { details ->
            details is AssetDetailsItem.CryptoDetailsInfo
        }
        listItems.addAll(0, assetDetails)

        updateList()
    }

    private fun onAccountSelected(
        accountDetails: AssetDetailsItem.CryptoDetailsInfo,
    ) {
        accountDetails.account.let { account ->
            if (account is CryptoAccount && account is TradingAccount) {
                analytics.logEvent(CustodialBalanceClicked(account.currency))
            }
        }

        analytics.logEvent(
            CoinViewAnalytics.WalletsAccountsClicked(
                origin = LaunchOrigin.COIN_VIEW,
                currency = assetTicker,
                accountType = when (accountDetails.account) {
                    is TradingAccount -> CoinViewAnalytics.Companion.AccountType.CUSTODIAL
                    is NonCustodialAccount -> CoinViewAnalytics.Companion.AccountType.USERKEY
                    is InterestAccount -> CoinViewAnalytics.Companion.AccountType.REWARDS_ACCOUNT
                    else -> CoinViewAnalytics.Companion.AccountType.EXCHANGE_ACCOUNT
                }
            )
        )
        model.process(CoinViewIntent.CheckScreenToOpen(accountDetails))
    }

    private fun openOnboardingForRecurringBuy() {
        analytics.logEvent(RecurringBuyAnalytics.RecurringBuyLearnMoreClicked(LaunchOrigin.CURRENCY_PAGE))
        startActivity(
            RecurringBuyOnboardingActivity.newIntent(
                context = this,
                fromCoinView = true,
                assetTicker = assetTicker
            )
        )
    }

    private fun onRecurringBuyClicked(recurringBuy: RecurringBuy) {
        analytics.logEvent(
            RecurringBuyAnalytics.RecurringBuyDetailsClicked(
                LaunchOrigin.CURRENCY_PAGE,
                recurringBuy.asset.networkTicker
            )
        )

        showBottomSheet(RecurringBuyDetailsSheet.newInstance(recurringBuy.id))
    }

    override fun onRecurringBuyDeleted(asset: AssetInfo) {
        model.process(CoinViewIntent.LoadRecurringBuys(asset))
    }

    private fun updateList() {
        adapterDelegate.items = listItems
        adapterDelegate.notifyDataSetChanged()
    }

    override fun navigateToActionSheet(actions: Array<StateAwareAction>) {
        model.process(CoinViewIntent.UpdateViewState(CoinViewViewState.ShowAccountActionSheet(actions)))
    }

    override fun showUpgradeKycSheet() {
        showBottomSheet(KycUpgradeNowSheet.newInstance())
    }

    override fun showSanctionsSheet(reason: BlockedReason.Sanctions) {
        showBottomSheet(BlockedDueToSanctionsSheet.newInstance(reason))
    }

    override fun startKycClicked() {
        KycNavHostActivity.startForResult(this, CampaignType.SimpleBuy, SimpleBuyActivity.KYC_STARTED)
    }

    override fun navigateToAction(
        action: AssetAction,
        selectedAccount: BlockchainAccount,
        assetInfo: AssetInfo,
    ) {
        when (action) {
            AssetAction.Send -> startSend(selectedAccount)
            AssetAction.Receive -> startReceive(selectedAccount)
            AssetAction.Swap -> startSwap(selectedAccount)
            AssetAction.Sell -> startSell(selectedAccount)
            AssetAction.ViewStatement -> startViewSummary(selectedAccount)
            AssetAction.ViewActivity -> goToActivityFor(selectedAccount)
            AssetAction.Buy -> startBuy(assetInfo)
            AssetAction.InterestDeposit -> goToInterestDeposit(selectedAccount)
            AssetAction.InterestWithdraw -> goToInterestWithdraw(selectedAccount)
            else -> throw IllegalStateException("Action $action is not supported in this flow")
        }
    }

    override fun showBalanceUpsellSheet(item: AccountActionsBottomSheet.AssetActionItem) {
        item.account?.let {
            showBottomSheet(NoBalanceActionBottomSheet.newInstance(it, item.action.action))
        }
    }

    override fun goToActivityFor(account: BlockchainAccount) {
        val intent = Intent().apply {
            putAccount(ACCOUNT_FOR_ACTIVITY, account)
        }

        setResult(RESULT_OK, intent)
        finish()
    }

    override fun goToInterestDeposit(toAccount: BlockchainAccount) {
        startActivity(
            TransactionFlowActivity.newIntent(
                context = this,
                action = AssetAction.InterestDeposit,
                target = toAccount as TransactionTarget
            )
        )
    }

    override fun goToInterestWithdraw(fromAccount: BlockchainAccount) {
        startActivity(
            TransactionFlowActivity.newIntent(
                context = this,
                action = AssetAction.InterestWithdraw,
                sourceAccount = fromAccount
            )
        )
    }

    override fun onSheetClosed() {
        // do nothing
    }

    companion object {
        private const val CHART_LOADING = 0
        private const val CHART_VIEW = 1
        private const val CHART_ERROR = 2
        private const val ACCOUNTS_LOADING = 0
        private const val ACCOUNTS_LIST = 1
        private const val PRICES_LOADING = 0
        private const val PRICES_VIEW = 1
        private const val BALANCES_LOADING = 0
        private const val BALANCES_VIEW = 1
        private const val INFO_LOADING = 0
        private const val INFO_VIEW = 1
        private const val ASSET_TICKER = "ASSET_TICKER"
        private const val ASSET_NAME = "ASSET_NAME"
        private const val ORIGIN_NAME = "ORIGIN_NAME"
        private const val PATTERN_HOURS = "HH:mm"
        private const val PATTERN_DAY_HOUR = "HH:mm, EEE"
        private const val PATTERN_DAY_HOUR_MONTH = "HH:mm d, MMM"
        private const val PATTERN_DAY_MONTH_YEAR = "d MMM YYYY"
        private const val SUPPORT_SUBJECT_NO_ASSET = "UNKNOWN ASSET"
        const val ACCOUNT_FOR_ACTIVITY = "ACCOUNT_FOR_ACTIVITY"
        private const val VISIBLE_LINES_DESCRIPTION = 6

        fun newIntent(context: Context, asset: AssetInfo, originScreen: String): Intent =
            Intent(context, CoinViewActivity::class.java).apply {
                putExtra(ASSET_TICKER, asset.networkTicker)
                putExtra(ASSET_NAME, asset.name)
                putExtra(ORIGIN_NAME, originScreen)
            }
    }

    private fun HistoricalTimeSpan.toDatePattern(): String =
        when (this) {
            HistoricalTimeSpan.DAY -> PATTERN_HOURS
            HistoricalTimeSpan.WEEK -> PATTERN_DAY_HOUR
            HistoricalTimeSpan.MONTH -> PATTERN_DAY_HOUR_MONTH
            HistoricalTimeSpan.YEAR,
            HistoricalTimeSpan.ALL_TIME,
            -> PATTERN_DAY_MONTH_YEAR
        }
}

private data class QuickAction(
    val name: String,
    val icon: ImageResource,
    val isEnabled: Boolean,
    val onClick: () -> Unit
)
