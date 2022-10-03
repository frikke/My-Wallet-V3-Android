package piuk.blockchain.android.simplebuy.paymentmethods

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.domain.paymentmethods.model.PaymentMethod
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.AddNewCardLayoutBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate

class AddCardDelegate(
    private val canUseCreditCards: Boolean
) : AdapterDelegate<PaymentMethodItem> {

    override fun isForViewType(items: List<PaymentMethodItem>, position: Int): Boolean =
        items[position].paymentMethod is PaymentMethod.UndefinedCard

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        ViewHolder(
            AddNewCardLayoutBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ),
            canUseCreditCards
        )

    override fun onBindViewHolder(items: List<PaymentMethodItem>, position: Int, holder: RecyclerView.ViewHolder) {
        val headerViewHolder = holder as ViewHolder
        headerViewHolder.bind(items[position])
    }

    private class ViewHolder(
        binding: AddNewCardLayoutBinding,
        private val canUseCreditCards: Boolean
    ) : RecyclerView.ViewHolder(binding.root) {

        val root: ViewGroup = binding.paymentMethodRoot
        val title: TextView = binding.paymentMethodTitle

        fun bind(paymentMethodItem: PaymentMethodItem) {
            root.setOnClickListener { paymentMethodItem.clickAction() }
            title.text = if (canUseCreditCards)
                title.context.getString(R.string.add_credit_or_debit_card_1)
            else
                title.context.getString(R.string.add_debit_card)
        }
    }
}
