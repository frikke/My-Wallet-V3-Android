package piuk.blockchain.android.ui.sell

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.BuyCryptoItemLayoutBinding
import piuk.blockchain.android.ui.dashboard.asDeltaPercent
import piuk.blockchain.android.ui.dashboard.setContentDescriptionSuffix
import piuk.blockchain.android.ui.resources.AssetResources

class BuyCryptoCurrenciesAdapter(
    private val assetResources: AssetResources,
    private val onItemClick: (BuyCryptoItem) -> Unit
) : RecyclerView.Adapter<BuyCryptoCurrenciesAdapter.ViewHolder>() {

    var items: List<BuyCryptoItem> = emptyList()
        set(value) {
            val diffResult =
                DiffUtil.calculateDiff(BuyCryptoDiffUtil(this.items, value))
            field = value
            diffResult.dispatchUpdatesTo(this)
        }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            BuyCryptoItemLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false), assetResources
        )
    }

    class ViewHolder(binding: BuyCryptoItemLayoutBinding, val assetResources: AssetResources) :
        RecyclerView.ViewHolder(binding.root) {
        val iconView: AppCompatImageView = binding.icon
        val currency: AppCompatTextView = binding.currency
        val container: View = binding.container
        val priceDelta: TextView = binding.priceDelta
        val price: AppCompatTextView = binding.price
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        with(holder) {
            assetResources.loadAssetIcon(iconView, item.asset)

            currency.text = item.asset.name
            currency.setContentDescriptionSuffix(R.string.accessibility_asset_name)

            price.text = item.price.toStringWithSymbol()
            price.setContentDescriptionSuffix(R.string.accessibility_current_market_price)

            priceDelta.asDeltaPercent(item.percentageDelta)
            priceDelta.setContentDescriptionSuffix(R.string.accessibility_24h_change)

            container.setOnClickListener {
                onItemClick(item)
            }
        }
    }
}

class BuyCryptoDiffUtil(private val oldItems: List<BuyCryptoItem>, private val newItems: List<BuyCryptoItem>) :
    DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldItems.size

    override fun getNewListSize(): Int = newItems.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        oldItems[oldItemPosition].asset.networkTicker == newItems[newItemPosition].asset.networkTicker

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldItems[oldItemPosition]
        val newItem = newItems[newItemPosition]
        return oldItem.asset.networkTicker == newItem.asset.networkTicker &&
            oldItem.price == newItem.price &&
            oldItem.percentageDelta == newItem.percentageDelta
    }
}
