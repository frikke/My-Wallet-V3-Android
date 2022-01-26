package piuk.blockchain.android.simplebuy.paymentmethods

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.nabu.datamanagers.PaymentMethod
import piuk.blockchain.android.databinding.GooglePayLayoutBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate

class GooglePayDelegate : AdapterDelegate<PaymentMethodItem> {

    override fun isForViewType(items: List<PaymentMethodItem>, position: Int): Boolean =
        items[position].paymentMethod is PaymentMethod.GooglePay

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder = ViewHolder(
        GooglePayLayoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
    )

    override fun onBindViewHolder(items: List<PaymentMethodItem>, position: Int, holder: RecyclerView.ViewHolder) {
        val headerViewHolder = holder as ViewHolder
        headerViewHolder.bind(items[position])
    }

    private class ViewHolder(binding: GooglePayLayoutBinding) : RecyclerView.ViewHolder(binding.root) {

        val root: ViewGroup = binding.paymentMethodRoot

        fun bind(paymentMethodItem: PaymentMethodItem) {
            root.setOnClickListener { paymentMethodItem.clickAction() }
        }
    }
}
