package piuk.blockchain.android.ui.dashboard.adapter

import androidx.recyclerview.widget.DiffUtil
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.AssetInfo
import piuk.blockchain.android.ui.adapters.AdapterDelegatesManager
import piuk.blockchain.android.ui.adapters.DelegationAdapter
import piuk.blockchain.android.ui.dashboard.PricesItem
import piuk.blockchain.android.ui.resources.AssetResources

class PricesDelegateAdapter(
    prefs: CurrencyPrefs,
    onPriceRequest: (AssetInfo) -> Unit,
    onCardClicked: (AssetInfo) -> Unit,
    assetResources: AssetResources
) : DelegationAdapter<PricesItem>(AdapterDelegatesManager(), emptyList()) {

    override var items: List<PricesItem> = emptyList()
        set(value) {
            val diffResult =
                DiffUtil.calculateDiff(PriceItemsDiffUtil(this.items, value))
            field = value
            diffResult.dispatchUpdatesTo(this)
        }

    init {
        with(delegatesManager) {
            addAdapterDelegate(
                PriceCardDelegate(
                    prefs,
                    assetResources,
                    onPriceRequest,
                    onCardClicked
                )
            )
        }
    }
}

class PriceItemsDiffUtil(
    private val oldPrices: List<PricesItem>,
    private val newPrices: List<PricesItem>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldPrices.size

    override fun getNewListSize(): Int = newPrices.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        oldPrices[oldItemPosition].asset == newPrices[newItemPosition].asset

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldPrices[oldItemPosition]
        val newItem = newPrices[newItemPosition]
        return oldItem.asset == newItem.asset &&
            oldItem.priceWithDelta == newItem.priceWithDelta
    }
}
