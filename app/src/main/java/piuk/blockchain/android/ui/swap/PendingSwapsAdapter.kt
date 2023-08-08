package piuk.blockchain.android.ui.swap

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.nabu.datamanagers.CustodialOrder
import com.blockchain.utils.toFormattedDate
import info.blockchain.balance.Money
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.SwapPendingItemLayoutBinding
import piuk.blockchain.android.util.context
import piuk.blockchain.android.util.setAssetIconColoursWithTint

class PendingSwapsAdapter(
    private val orders: List<CustodialOrder>,
    private val toFiat: (Money) -> Money
) :
    RecyclerView.Adapter<PendingSwapsAdapter.PendingSwapViewHolder>() {

    override fun getItemCount(): Int = orders.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PendingSwapViewHolder =
        PendingSwapViewHolder(
            SwapPendingItemLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(holder: PendingSwapViewHolder, position: Int) {
        holder.bind(orders[position], toFiat)
    }

    class PendingSwapViewHolder(private val binding: SwapPendingItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(
            custodialOrder: CustodialOrder,
            toFiat: (Money) -> Money
        ) {
            with(binding) {
                title.text = context.resources.getString(
                    com.blockchain.stringResources.R.string.swap_direction,
                    custodialOrder.inputMoney.currency.displayTicker,
                    custodialOrder.outputMoney.currency.displayTicker
                )
                subtitle.text = custodialOrder.createdAt.toFormattedDate()
                fiatvalue.text = toFiat(custodialOrder.inputMoney).toStringWithSymbol()
                cryptovalue.text = custodialOrder.inputMoney.toStringWithSymbol()
                val asset = custodialOrder.inputMoney.currency
                icon.setAssetIconColoursWithTint(asset)
            }
        }
    }
}
