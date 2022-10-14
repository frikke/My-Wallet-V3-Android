package piuk.blockchain.android.cards.mapper

import androidx.annotation.DrawableRes
import com.blockchain.domain.paymentmethods.model.CardType
import piuk.blockchain.android.cards.icon

@DrawableRes
fun CardType.icon(): Int = when (this) {
    CardType.VISA -> com.braintreepayments.cardform.utils.CardType.VISA.icon()
    CardType.MASTERCARD -> com.braintreepayments.cardform.utils.CardType.MASTERCARD.icon()
    CardType.DISCOVER -> com.braintreepayments.cardform.utils.CardType.DISCOVER.icon()
    CardType.AMEX -> com.braintreepayments.cardform.utils.CardType.AMEX.icon()
    CardType.DINERS_CLUB -> com.braintreepayments.cardform.utils.CardType.DINERS_CLUB.icon()
    CardType.JCB -> com.braintreepayments.cardform.utils.CardType.JCB.icon()
    CardType.MAESTRO -> com.braintreepayments.cardform.utils.CardType.MAESTRO.icon()
    CardType.UNIONPAY -> com.braintreepayments.cardform.utils.CardType.UNIONPAY.icon()
    CardType.HIPER -> com.braintreepayments.cardform.utils.CardType.HIPER.icon()
    CardType.HIPERCARD -> com.braintreepayments.cardform.utils.CardType.HIPERCARD.icon()
    CardType.UNKNOWN -> com.braintreepayments.cardform.utils.CardType.UNKNOWN.icon()
    CardType.EMPTY -> com.braintreepayments.cardform.utils.CardType.EMPTY.icon()
}

fun CardType.isEquals(other: com.braintreepayments.cardform.utils.CardType?): Boolean =
    this.name == other?.name

fun com.braintreepayments.cardform.utils.CardType.isEquals(other: CardType?): Boolean =
    this.name == other?.name
