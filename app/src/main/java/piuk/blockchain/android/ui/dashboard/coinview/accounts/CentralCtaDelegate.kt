package piuk.blockchain.android.ui.dashboard.coinview.accounts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.componentlib.basic.ImageResource
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ViewCoinviewCentralCtasBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.dashboard.coinview.AssetDetailsItem

class CentralCtaDelegate(
    private val swapOnClick: (BlockchainAccount) -> Unit
) : AdapterDelegate<AssetDetailsItem> {
    override fun isForViewType(items: List<AssetDetailsItem>, position: Int): Boolean =
        items[position] is AssetDetailsItem.CentralCta

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        CentralCtaViewHolder(
            ViewCoinviewCentralCtasBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            swapOnClick = swapOnClick,
        )

    override fun onBindViewHolder(
        items: List<AssetDetailsItem>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as CentralCtaViewHolder).bind(
        items[position] as AssetDetailsItem.CentralCta
    )
}

private class CentralCtaViewHolder(
    private val binding: ViewCoinviewCentralCtasBinding,
    private val swapOnClick: (BlockchainAccount) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: AssetDetailsItem.CentralCta) {
        with(binding) {
            swapCta.apply {
                text = context.getString(R.string.common_swap)
                icon = ImageResource.Local(R.drawable.ic_swap)
                onClick = { swapOnClick(item.account) }
            }
        }
    }
}
