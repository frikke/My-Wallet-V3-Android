package piuk.blockchain.android.ui.dashboard.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.componentlib.tag.TagType
import com.blockchain.componentlib.tag.TagViewState
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.setOnClickListenerDebounced
import com.blockchain.componentlib.viewextensions.visible
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ItemDashboardDefiAssetCardBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.dashboard.model.DashboardItem
import piuk.blockchain.android.ui.dashboard.model.DefiAsset
import piuk.blockchain.android.ui.resources.AssetResources
import piuk.blockchain.android.util.context

class DefiCardDelegate(
    private val assetResources: AssetResources,
    private val assetCatalogue: AssetCatalogue,
    private val onCardClicked: (AssetInfo) -> Unit
) : AdapterDelegate<DashboardItem> {

    override fun isForViewType(items: List<DashboardItem>, position: Int): Boolean =
        items[position] is DefiAsset

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        DefiAssetCardViewHolder(
            ItemDashboardDefiAssetCardBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            assetResources, assetCatalogue = assetCatalogue
        )

    override fun onBindViewHolder(
        items: List<DashboardItem>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as DefiAssetCardViewHolder).bind(
        items[position] as DefiAsset,
        onCardClicked
    )
}

private class DefiAssetCardViewHolder(
    private val binding: ItemDashboardDefiAssetCardBinding,
    private val assetResources: AssetResources,
    private val assetCatalogue: AssetCatalogue
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(defiAsset: DefiAsset, onCardClicked: (AssetInfo) -> Unit) {
        with(binding) {
            root.contentDescription = "${ASSET_CARD_ID}${defiAsset.currency.networkTicker}"
            fiatBalance.contentDescription = "${FIAT_BALANCE_ID}${defiAsset.currency.networkTicker}"
            cryptoBalance.contentDescription = "${CRYPTO_BALANCE_ID}${defiAsset.currency.networkTicker}"
            val l1Parent = defiAsset.currency.l1chainTicker?.let {
                assetCatalogue.fromNetworkTicker(it)
            }
            l1Netwok.tags = listOfNotNull(
                l1Parent?.let {
                    TagViewState(
                        it.name, TagType.Default()
                    )
                }
            )
            assetResources.loadAssetIcon(icon, defiAsset.currency)
            currency.text = defiAsset.currency.displayTicker
            currencyName.text = defiAsset.currency.name
        }

        when {
            defiAsset.hasBalanceError -> renderError(defiAsset)
            defiAsset.isUILoading -> renderLoading()
            else -> renderLoaded(defiAsset, onCardClicked)
        }
    }

    private fun renderLoaded(
        defiAsset: DefiAsset,
        onCardClicked: (AssetInfo) -> Unit
    ) {
        with(binding) {
            cardLayout.isEnabled = true
            root.setOnClickListenerDebounced { onCardClicked(defiAsset.currency) }
            showContent()
            fiatBalance.text = defiAsset.fiatBalance(useDisplayBalance = false)?.toStringWithSymbol()
            cryptoBalance.text = defiAsset.accountBalance?.total?.toStringWithSymbol()
        }
    }

    private fun renderLoading() {
        with(binding) {
            cardLayout.isEnabled = false
            root.setOnClickListener { }
            showContent()
        }
    }

    private fun renderError(state: DefiAsset) {
        showError()

        with(binding) {
            cardLayout.isEnabled = false
            root.setOnClickListener { }
            errorMsg.text = context.resources.getString(R.string.dashboard_asset_error, state.currency.displayTicker)
        }
    }

    private fun showError() {
        with(binding) {
            fiatBalance.gone()
            cryptoBalance.gone()
            errorMsg.visible()
            currencyName.gone()
        }
    }

    private fun showContent() {
        with(binding) {
            fiatBalance.visible()
            currencyName.visible()
            cryptoBalance.visible()
            errorMsg.gone()
        }
    }
}
