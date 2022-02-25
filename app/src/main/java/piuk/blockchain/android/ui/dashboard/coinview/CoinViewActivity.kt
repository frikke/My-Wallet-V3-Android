package piuk.blockchain.android.ui.dashboard.coinview

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.CryptoAccount
import com.blockchain.commonarch.presentation.mvi.MviActivity
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.charts.PercentageChangeData
import com.blockchain.componentlib.databinding.ToolbarGeneralBinding
import com.blockchain.core.price.HistoricalTimeSpan
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.models.data.RecurringBuy
import com.blockchain.notifications.analytics.LaunchOrigin
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.AssetInfo
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ActivityCoinviewBinding
import piuk.blockchain.android.simplebuy.CustodialBalanceClicked
import piuk.blockchain.android.ui.customviews.BlockchainListDividerDecor
import piuk.blockchain.android.ui.customviews.BlockchainSnackbar
import piuk.blockchain.android.ui.customviews.account.PendingBalanceAccountDecorator
import piuk.blockchain.android.ui.dashboard.assetdetails.AssetDetailsItemNew
import piuk.blockchain.android.ui.dashboard.coinview.accounts.AccountsAdapterDelegate
import piuk.blockchain.android.ui.recurringbuy.RecurringBuyAnalytics
import piuk.blockchain.android.ui.resources.AssetResources

class CoinViewActivity : MviActivity<CoinViewModel, CoinViewIntent, CoinViewState, ActivityCoinviewBinding>() {

    override val alwaysDisableScreenshots: Boolean
        get() = false

    override val toolbarBinding: ToolbarGeneralBinding
        get() = binding.toolbar

    override val model: CoinViewModel by scopedInject()

    private val assetTicker: String by lazy {
        intent.getStringExtra(ASSET_TICKER) ?: ""
    }

    private val assetName: String by lazy {
        intent.getStringExtra(ASSET_NAME) ?: ""
    }

    private val labels: DefaultLabels by inject()
    private val assetResources: AssetResources by inject()
    private val listItems = mutableListOf<AssetDetailsItemNew>()

    override fun initBinding(): ActivityCoinviewBinding = ActivityCoinviewBinding.inflate(layoutInflater)

    private val adapterDelegate by lazy {
        AccountsAdapterDelegate(
            onAccountSelected = ::onAccountSelected,
            labels = labels,
            onCardClicked = ::openOnboardingForRecurringBuy,
            onRecurringBuyClicked = ::onRecurringBuyClicked,
            assetDetailsDecorator = {
                PendingBalanceAccountDecorator(it.account)
            },
            assetResources = assetResources
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        updateToolbar(toolbarTitle = assetName, backAction = { onBackPressed() })
        with(binding) {
            assetList.apply {
                adapter = adapterDelegate
                addItemDecoration(BlockchainListDividerDecor(this@CoinViewActivity))
            }

            // TODO update these in relevant story - placeholder texts
            assetAboutTitle.apply {
                text = getString(R.string.coinview_about_asset, assetName)
                textColor = ComposeColors.Title
                style = ComposeTypographies.Body2
            }

            assetAboutBlurb.apply {
                text =
                    "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut" +
                    " labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco " +
                    "laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in" +
                    " voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat " +
                    "cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."
                textColor = ComposeColors.Title
                style = ComposeTypographies.Paragraph1
            }

            assetChartViewSwitcher.apply {
                displayedChild = CHART_LOADING
            }

            assetAccountsViewSwitcher.apply {
                displayedChild = ACCOUNTS_LOADING
            }

            assetChart.isChartLive = false

            chartControls.apply {
                items = listOf(
                    getString(R.string.coinview_chart_tab_day),
                    getString(R.string.coinview_chart_tab_week),
                    getString(R.string.coinview_chart_tab_month),
                    getString(R.string.coinview_chart_tab_year),
                    getString(R.string.coinview_chart_tab_all)
                )
                onItemSelected = {
                    model.process(CoinViewIntent.LoadNewChartPeriod(HistoricalTimeSpan.fromInt(it)))
                }
                selectedItemIndex = 0
                showLiveIndicator = false
            }

            // TODO load through interactor
            assetPrice.apply {
                title = "Current $assetTicker Price"
                price = "1000000.00€"
                percentageChangeData = PercentageChangeData("500%", 500.0, "Last Hour")
                endIcon = ImageResource.Local(R.drawable.ic_blockchain)
            }

            // TODO load through interactor
            assetBalance.apply {
                primaryText = "15,000.00€"
                secondaryText = "0.1 BTC"
                onIconClick = {
                    // TODO change icon type and update watchlist endpoint
                }
            }

            // TODO update these in another story based on accounts logic
            primaryCta.apply {
                text = "Buy"
                icon = ImageResource.Local(
                    R.drawable.ic_bottom_nav_buy,
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                        Color(ContextCompat.getColor(this@CoinViewActivity, R.color.white))
                    )
                )
            }

            secondaryCta.apply {
                text = "Sell"
                icon = ImageResource.Local(R.drawable.ic_fiat_notes)
            }

            // TODO in upcoming stories
            assetWebsite.apply {
                text = "Visit Website ->"
                textColor = ComposeColors.Primary
                style = ComposeTypographies.Paragraph2
                onClick = {
                    BlockchainSnackbar.make(binding.root, "Website link clicked").show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        model.process(CoinViewIntent.LoadAsset(assetTicker))
    }

    override fun render(newState: CoinViewState) {
        newState.asset?.let {
            with(binding) {
                assetAboutTitle.text = getString(R.string.coinview_about_asset, it.assetInfo.name)
                assetPrice.endIcon = ImageResource.Remote(it.assetInfo.logo)
            }
        }

        if (newState.viewState != CoinViewViewState.None) {
            when (val state = newState.viewState) {
                CoinViewViewState.LoadingWallets -> {
                    binding.assetAccountsViewSwitcher.apply {
                        displayedChild = ACCOUNTS_LOADING
                    }
                }
                is CoinViewViewState.ShowAccountInfo -> {
                    renderAccountsDetails(state.displayList)
                    binding.assetAccountsViewSwitcher.apply {
                        displayedChild = ACCOUNTS_LIST
                    }
                }
                CoinViewViewState.LoadingChart -> {
                    binding.assetChartViewSwitcher.apply {
                        displayedChild = CHART_LOADING
                    }
                }
                is CoinViewViewState.ShowChartInfo -> {
                    with(binding) {
                        assetChartViewSwitcher.apply {
                            displayedChild = CHART_VIEW
                        }
                        assetChart.setData(state.entries)
                    }
                }
                CoinViewViewState.None -> {
                    // do nothing
                }
            }

            model.process(CoinViewIntent.ResetViewState)
        }
    }

    private fun renderAccountsDetails(assetDetails: List<AssetDisplayInfo>) {
        val itemList = mutableListOf<AssetDetailsItemNew>()

        assetDetails.map {
            itemList.add(
                AssetDetailsItemNew.CryptoDetailsInfo(
                    assetFilter = it.filter,
                    account = it.account,
                    balance = it.amount,
                    fiatBalance = it.fiatValue,
                    actions = it.actions,
                    interestRate = it.interestRate
                )
            )
        }

        listItems.addAll(0, itemList)
        updateList()
    }

    private fun onAccountSelected(account: BlockchainAccount, assetFilter: AssetFilter) {
        clearList()

        if (account is CryptoAccount && assetFilter == AssetFilter.Custodial) {
            analytics.logEvent(CustodialBalanceClicked(account.currency))
        }

        //        model.process(
        //            ShowAssetActionsIntent(account)
        //        )
    }

    private fun openOnboardingForRecurringBuy() {
        analytics.logEvent(RecurringBuyAnalytics.RecurringBuyLearnMoreClicked(LaunchOrigin.CURRENCY_PAGE))
        //        startActivity(
        //            RecurringBuyOnboardingActivity.newInstance(
        //                context = this,
        //                fromCoinView = true,
        //                asset = asset
        //            )
        //        )
        //        dismiss()
    }

    private fun onRecurringBuyClicked(recurringBuy: RecurringBuy) {
        clearList()

        recurringBuy.asset.let {
            analytics.logEvent(
                RecurringBuyAnalytics.RecurringBuyDetailsClicked(
                    LaunchOrigin.CURRENCY_PAGE,
                    it.networkTicker
                )
            )
        }
        //        model.process(ShowRecurringBuySheet(recurringBuy))
    }

    private fun clearList() {
        listItems.clear()
        updateList()
    }

    private fun updateList() {
        adapterDelegate.items = listItems
        adapterDelegate.notifyDataSetChanged()
    }

    companion object {
        private const val CHART_LOADING = 0
        private const val CHART_VIEW = 1
        private const val ACCOUNTS_LOADING = 0
        private const val ACCOUNTS_LIST = 1
        private const val ASSET_TICKER = "ASSET_TICKER"
        private const val ASSET_NAME = "ASSET_NAME"

        fun newIntent(context: Context, asset: AssetInfo): Intent =
            Intent(context, CoinViewActivity::class.java).apply {
                putExtra(ASSET_TICKER, asset.networkTicker)
                putExtra(ASSET_NAME, asset.name)
            }
    }
}
