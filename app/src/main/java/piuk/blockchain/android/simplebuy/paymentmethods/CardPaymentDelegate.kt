package piuk.blockchain.android.simplebuy.paymentmethods

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.ui.text.buildAnnotatedString
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.componentlib.basic.ImageResource
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

class CardPaymentDelegate(
    private val onCardTagClicked: (cardInfo: CardRejectionState) -> Unit
) : AdapterDelegate<PaymentMethodItem> {

    override fun isForViewType(items: List<PaymentMethodItem>, position: Int): Boolean =
        items[position].paymentMethod is PaymentMethod.Card

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        CardPaymentViewHolder(
            CardPaymentMethodLayoutBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ),
            onCardTagClicked
        )

    override fun onBindViewHolder(items: List<PaymentMethodItem>, position: Int, holder: RecyclerView.ViewHolder) {
        (holder as CardPaymentViewHolder).bind(items[position])
    }

    private class CardPaymentViewHolder(
        private val binding: CardPaymentMethodLayoutBinding,
        private val onCardTagClicked: (cardInfo: CardRejectionState) -> Unit
    ) :
        RecyclerView.ViewHolder(binding.root), KoinComponent {

        private val cardRejectionFF: FeatureFlag by inject(cardRejectionCheckFeatureFlag)

        fun bind(paymentMethodItem: PaymentMethodItem) {
            with(binding) {
                (paymentMethodItem.paymentMethod as? PaymentMethod.Card)?.let {
                    cardPayment.apply {
                        titleStart = buildAnnotatedString { append(it.uiLabel()) }
                        titleEnd = buildAnnotatedString { append(it.dottedEndDigits()) }
                        startImageResource = ImageResource.Local(
                            id = it.cardType.toCardType().icon(),
                        )
                        bodyStart = buildAnnotatedString {
                            append(
                                context.getString(
                                    R.string.common_spaced_strings, it.limits.max.toStringWithSymbol(),
                                    context.getString(R.string.deposit_enter_amount_limit_title)
                                )
                            )
                        }
                        bodyEnd = buildAnnotatedString {
                            append(
                                context.getString(R.string.card_expiry_date, it.expireDate.formatted())
                            )
                        }
                        onClick = {
                            paymentMethodItem.clickAction()
                        }
                        cardRejectionFF.enabled.map { isEnabled ->
                            if (isEnabled) {
                                tags = when (val cardState = it.cardRejectionState) {
                                    is CardRejectionState.AlwaysRejected -> {
                                        listOf(
                                            TagViewState(
                                                value = cardState.title ?: context.getString(
                                                    R.string.card_issuer_always_rejects_title
                                                ),
                                                type = TagType.Error(),
                                                onClick = {
                                                    onCardTagClicked(cardState)
                                                }
                                            )
                                        )
                                    }
                                    is CardRejectionState.MaybeRejected -> {
                                        listOf(
                                            TagViewState(
                                                value = cardState.title ?: context.getString(
                                                    R.string.card_issuer_sometimes_rejects_title
                                                ),
                                                type = TagType.Warning(),
                                                onClick = {
                                                    onCardTagClicked(cardState)
                                                }
                                            )
                                        )
                                    }
                                    else -> null
                                }
                            }
                        }
                    }
                }
            }
        }

        private fun Date.formatted(): String =
            SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(this)
    }
}
