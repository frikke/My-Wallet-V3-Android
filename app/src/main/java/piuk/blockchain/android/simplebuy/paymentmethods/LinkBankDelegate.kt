package piuk.blockchain.android.simplebuy.paymentmethods

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.nabu.datamanagers.PaymentMethod
import piuk.blockchain.android.databinding.LinkBankLayoutBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.util.context

class LinkBankDelegate : AdapterDelegate<PaymentMethodItem> {

    override fun isForViewType(items: List<PaymentMethodItem>, position: Int): Boolean =
        items[position].paymentMethod is PaymentMethod.UndefinedBankTransfer

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        LinkBankViewHolder(
            LinkBankLayoutBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(items: List<PaymentMethodItem>, position: Int, holder: RecyclerView.ViewHolder) {
        (holder as LinkBankViewHolder).bind(items[position])
    }

    private class LinkBankViewHolder(private val binding: LinkBankLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(paymentMethodItem: PaymentMethodItem) {
            with(binding) {
                paymentMethodRoot.setOnClickListener { paymentMethodItem.clickAction() }
            }
        }
    }
}
