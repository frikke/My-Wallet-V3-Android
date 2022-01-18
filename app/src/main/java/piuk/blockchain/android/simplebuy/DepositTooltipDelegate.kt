package piuk.blockchain.android.simplebuy

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.nabu.datamanagers.PaymentMethod
import piuk.blockchain.android.databinding.DepositTooltipLayoutBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate


class DepositTooltipDelegate : AdapterDelegate<PaymentMethodItem> {

    override fun isForViewType(items: List<PaymentMethodItem>, position: Int): Boolean =
        items[position].paymentMethod is PaymentMethod.UndefinedBankAccount

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        ViewHolder(
            DepositTooltipLayoutBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(items: List<PaymentMethodItem>, position: Int, holder: RecyclerView.ViewHolder) {
        val headerViewHolder = holder as ViewHolder
        headerViewHolder.bind(items[position])
    }

    private class ViewHolder(
        private val binding: DepositTooltipLayoutBinding
    ) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(paymentMethodItem: PaymentMethodItem) {
            require(paymentMethodItem.paymentMethod is PaymentMethod.UndefinedBankAccount)
            binding.apply {
                paymentMethodRoot.setOnClickListener { paymentMethodItem.clickAction() }
            }
        }
    }
}
