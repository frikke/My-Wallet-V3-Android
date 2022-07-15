package piuk.blockchain.android.simplebuy.paymentmethods

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.componentlib.tag.TagType
import com.blockchain.componentlib.tag.TagViewState
import com.blockchain.core.payments.toCardType
import com.blockchain.domain.paymentmethods.model.CardRejectionState
import com.blockchain.domain.paymentmethods.model.PaymentMethod
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.koin.cardRejectionCheckFeatureFlag
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.cards.icon
import piuk.blockchain.android.databinding.CardPaymentMethodLayoutBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.util.context

class CardPaymentDelegate : AdapterDelegate<PaymentMethodItem> {

    override fun isForViewType(items: List<PaymentMethodItem>, position: Int): Boolean =
        items[position].paymentMethod is PaymentMethod.Card

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        CardPaymentViewHolder(
            CardPaymentMethodLayoutBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(items: List<PaymentMethodItem>, position: Int, holder: RecyclerView.ViewHolder) {
        (holder as CardPaymentViewHolder).bind(items[position])
    }

    private class CardPaymentViewHolder(private val binding: CardPaymentMethodLayoutBinding) :
        RecyclerView.ViewHolder(binding.root), KoinComponent {

        private val cardRejectionFF: FeatureFlag by inject(cardRejectionCheckFeatureFlag)

        fun bind(paymentMethodItem: PaymentMethodItem) {
            with(binding) {
                (paymentMethodItem.paymentMethod as? PaymentMethod.Card)?.let {
                    paymentMethodIcon.setImageResource(it.cardType.toCardType().icon())
                    paymentMethodLimit.text =
                        context.getString(
                            R.string.payment_method_limit,
                            paymentMethodItem.paymentMethod.limits.max.toStringWithSymbol()
                        )
                    paymentMethodTitle.text = it.uiLabel()
                    cardNumber.text = it.dottedEndDigits()
                    expDate.text = context.getString(R.string.card_expiry_date, it.expireDate.formatted())
                }
                paymentMethodRoot.setOnClickListener { paymentMethodItem.clickAction() }

                if (cardRejectionFF.isEnabled) {
                    paymentMethodTagRow.apply {
                        tags = when (
                            val cardState =
                                (paymentMethodItem.paymentMethod as PaymentMethod.Card).cardRejectionState
                        ) {
                            is CardRejectionState.AlwaysRejected -> {
                                listOf(
                                    TagViewState(
                                        cardState.title ?: context.getString(R.string.card_issuer_always_rejects_title),
                                        TagType.Error()
                                    )
                                )
                            }
                            is CardRejectionState.MaybeRejected -> {
                                listOf(
                                    TagViewState(
                                        cardState.title ?: context.getString(
                                            R.string.card_issuer_sometimes_rejects_title
                                        ),
                                        TagType.Warning()
                                    )
                                )
                            }
                            else -> emptyList()
                        }
                    }
                }
            }
        }

        private fun Date.formatted(): String =
            SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(this)
    }
}
