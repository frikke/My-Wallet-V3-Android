package piuk.blockchain.android.ui.dashboard.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.invisible
import com.blockchain.componentlib.viewextensions.setOnClickListenerDebounced
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.preferences.CurrencyPrefs
import com.robinhood.spark.SparkAdapter
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.FiatCurrency
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ItemDashboardAssetCardBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.dashboard.asDeltaPercent
import piuk.blockchain.android.ui.dashboard.format
import piuk.blockchain.android.ui.dashboard.model.BrokerageCryptoAsset
import piuk.blockchain.android.ui.dashboard.model.DashboardItem
import piuk.blockchain.android.ui.dashboard.showLoading
import piuk.blockchain.android.ui.resources.AssetResources
import piuk.blockchain.android.util.context

// Uses sparkline lib from here: https://github.com/robinhood/spark

class BrokerageCardDelegate(
    private val prefs: CurrencyPrefs,
    private val assetResources: AssetResources,
    private val onCardClicked: (AssetInfo) -> Unit
) : AdapterDelegate<DashboardItem> {

    override fun isForViewType(items: List<DashboardItem>, position: Int): Boolean =
        items[position] is BrokerageCryptoAsset

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        AssetCardViewHolder(ItemDashboardAssetCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(
        items: List<DashboardItem>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as AssetCardViewHolder).bind(
        items[position] as BrokerageCryptoAsset,
        prefs.selectedFiatCurrency,
        assetResources,
        onCardClicked
    )
}

private class AssetCardViewHolder(
    private val binding: ItemDashboardAssetCardBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(
        state: BrokerageCryptoAsset,
        fiatSymbol: FiatCurrency,
        assetResources: AssetResources,
        onCardClicked: (AssetInfo) -> Unit
    ) {
        with(binding) {
            fiatBalance.contentDescription = "$FIAT_BALANCE_ID${state.currency.networkTicker}"
            cryptoBalance.contentDescription = "$CRYPTO_BALANCE_ID${state.currency.networkTicker}"

            assetResources.loadAssetIcon(icon, state.currency)
            currency.text = state.currency.name
        }

        when {
            state.hasBalanceError -> renderError(state)
            state.isUILoading -> renderLoading()
            else -> renderLoaded(state, fiatSymbol, assetResources, onCardClicked)
        }
    }

    private fun renderLoading() {
        with(binding) {
            cardLayout.isEnabled = false
            root.setOnClickListener { }

            showContent()

            fiatBalance.showLoading()
            cryptoBalance.showLoading()
            price.showLoading()
            priceDelta.showLoading()
            priceDeltaInterval.showLoading()
            sparkview.invisible()
        }
    }

    private fun renderLoaded(
        state: BrokerageCryptoAsset,
        fiatCurrency: FiatCurrency,
        assetResources: AssetResources,
        onCardClicked: (AssetInfo) -> Unit
    ) {
        with(binding) {
            cardLayout.isEnabled = true
            root.setOnClickListenerDebounced { onCardClicked(state.currency) }

            showContent()

            fiatBalance.text =
                state.fiatBalance(useDisplayBalance = state.assetDisplayBalanceFFEnabled).format(fiatCurrency)
            cryptoBalance.text = if (state.assetDisplayBalanceFFEnabled) {
                state.accountBalance?.dashboardDisplay.format(state.currency)
            } else {
                state.accountBalance?.total.format(state.currency)
            }

            price.text = state.accountBalance?.exchangeRate?.price.format(fiatCurrency)

            priceDelta.asDeltaPercent(state.priceDelta)
            priceDeltaInterval.text = context.getString(R.string.asset_card_rate_period)

            if (state.priceTrend.isNotEmpty()) {
                sparkview.lineColor = assetResources.assetColor(state.currency)
                sparkview.adapter = PriceAdapter(state.priceTrend.toFloatArray())
                sparkview.visible()
            } else {
                sparkview.gone()
            }

            root.contentDescription =
                "${context.getString(R.string.accessibility_asset_name)} ${currency.text}." +
                "${context.getString(R.string.accessibility_total_balance)}: " +
                "${fiatBalance.text}, ${cryptoBalance.text}." +
                "${context.getString(R.string.accessibility_current_market_price)}: ${price.text}. " +
                "${context.getString(R.string.accessibility_24h_change)}: ${priceDelta.text}"
        }
    }

    private fun renderError(state: BrokerageCryptoAsset) {
        showError()

        with(binding) {
            cardLayout.isEnabled = false
            root.setOnClickListener { }

            errorMsg.text = context.resources.getString(R.string.dashboard_asset_error, state.currency.displayTicker)
            root.contentDescription = errorMsg.text
        }
    }

    private fun showContent() {
        with(binding) {
            fiatBalance.visible()
            cryptoBalance.visible()
            sparkview.visible()
            separator.visible()
            price.visible()
            priceDelta.visible()
            priceDeltaInterval.visible()
            errorMsg.invisible()
        }
    }

    private fun showError() {
        with(binding) {
            fiatBalance.invisible()
            cryptoBalance.invisible()
            sparkview.invisible()
            separator.invisible()
            price.invisible()
            priceDelta.invisible()
            priceDeltaInterval.invisible()
            errorMsg.visible()
        }
    }
}

const val FIAT_BALANCE_ID = "DashboardAssetFiatBalance_"
const val CRYPTO_BALANCE_ID = "DashboardAssetCryptoBalance_"
const val ASSET_CARD_ID = "DashboardAssetCard_"

private class PriceAdapter(private val yData: FloatArray) : SparkAdapter() {
    override fun getCount(): Int = yData.size
    override fun getItem(index: Int): Any = yData[index]
    override fun getY(index: Int): Float = yData[index]
}
