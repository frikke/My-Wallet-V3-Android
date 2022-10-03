package piuk.blockchain.android.simplebuy.paymentmethods

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.ui.text.buildAnnotatedString
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.domain.paymentmethods.model.PaymentMethod
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.BankPaymentMethodLayoutBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate

class BankPaymentDelegate : AdapterDelegate<PaymentMethodItem> {

    override fun isForViewType(items: List<PaymentMethodItem>, position: Int): Boolean =
        items[position].paymentMethod is PaymentMethod.Bank

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        BankPaymentViewHolder(
            BankPaymentMethodLayoutBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(items: List<PaymentMethodItem>, position: Int, holder: RecyclerView.ViewHolder) {
        (holder as BankPaymentViewHolder).bind(items[position])
    }

    private class BankPaymentViewHolder(private val binding: BankPaymentMethodLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(paymentMethodItem: PaymentMethodItem) {
            with(binding) {
                (paymentMethodItem.paymentMethod as? PaymentMethod.Bank)?.let {
                    bankPayment.apply {
                        titleStart = buildAnnotatedString { append(it.bankName) }
                        titleEnd = buildAnnotatedString {
                            append(context.getString(R.string.dotted_suffixed_string, it.accountEnding))
                        }
                        startImageResource = if (it.iconUrl.isEmpty()) {
                            ImageResource.Local(R.drawable.ic_bank_icon, null)
                        } else {
                            ImageResource.Remote(url = it.iconUrl, null)
                        }
                        bodyStart = buildAnnotatedString {
                            append(
                                context.getString(
                                    R.string.common_spaced_strings, it.limits.max.toStringWithSymbol(),
                                    context.getString(R.string.deposit_enter_amount_limit_title)
                                )
                            )
                        }
                        bodyEnd = buildAnnotatedString {
                            append(it.uiAccountType)
                        }
                        onClick = {
                            paymentMethodItem.clickAction()
                        }
                    }
                }
            }
        }
    }
}
