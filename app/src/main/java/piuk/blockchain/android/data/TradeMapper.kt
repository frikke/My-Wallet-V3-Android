package piuk.blockchain.android.data

import com.blockchain.api.trade.data.NextPaymentRecurringBuy
import com.blockchain.api.trade.data.NextPaymentRecurringBuyResponse
import com.blockchain.api.trade.data.RecurringBuyResponse
import com.blockchain.core.recurringbuy.domain.EligibleAndNextPaymentRecurringBuy
import com.blockchain.core.recurringbuy.domain.RecurringBuy
import com.blockchain.core.recurringbuy.domain.RecurringBuyState
import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.nabu.datamanagers.custodialwalletimpl.toPaymentMethodType
import com.blockchain.nabu.models.responses.simplebuy.toRecurringBuyFrequency
import com.blockchain.utils.fromIso8601ToUtc
import com.blockchain.utils.toLocalTime
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.Money
import java.util.Date

fun NextPaymentRecurringBuyResponse.toDomain(): List<EligibleAndNextPaymentRecurringBuy> = this.nextPayments.map {
    EligibleAndNextPaymentRecurringBuy(
        frequency = it.period.toRecurringBuyFrequency(),
        nextPaymentDate = it.nextPayment,
        eligibleMethods = it.eligibleMethods.map(String::toPaymentMethodTypeDomain)
    )
}

private fun String.toPaymentMethodTypeDomain(): PaymentMethodType = when (this) {
    NextPaymentRecurringBuy.BANK_TRANSFER -> PaymentMethodType.BANK_TRANSFER
    NextPaymentRecurringBuy.PAYMENT_CARD -> PaymentMethodType.PAYMENT_CARD
    NextPaymentRecurringBuy.FUNDS -> PaymentMethodType.FUNDS
    else -> PaymentMethodType.UNKNOWN
}

fun RecurringBuyResponse.toDomain(assetCatalogue: AssetCatalogue): RecurringBuy? {
    val asset = assetCatalogue.assetInfoFromNetworkTicker(destinationCurrency) ?: return null
    val fiatCurrency = assetCatalogue.fiatFromNetworkTicker(inputCurrency) ?: return null
    return RecurringBuy(
        id = id,
        state = state.toRecurringBuyStateDomain(),
        recurringBuyFrequency = period.toRecurringBuyFrequency(),
        nextPaymentDate = nextPayment.fromIso8601ToUtc()?.toLocalTime() ?: Date(),
        paymentMethodType = paymentMethod.toPaymentMethodType(),
        amount = Money.fromMinor(fiatCurrency, inputValue.toBigInteger()),
        asset = asset,
        createDate = insertedAt.fromIso8601ToUtc()?.toLocalTime() ?: Date(),
        paymentMethodId = paymentMethodId
    )
}

private fun String.toRecurringBuyStateDomain() =
    when (this) {
        RecurringBuyResponse.ACTIVE -> RecurringBuyState.ACTIVE
        RecurringBuyResponse.INACTIVE -> RecurringBuyState.INACTIVE
        else -> throw IllegalStateException("Unsupported recurring state")
    }
