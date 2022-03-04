package piuk.blockchain.android.data

import com.blockchain.api.trade.data.AccumulatedInPeriod
import com.blockchain.api.trade.data.NextPaymentRecurringBuy
import com.blockchain.api.trade.data.RecurringBuyResponse
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import com.blockchain.nabu.datamanagers.custodialwalletimpl.toPaymentMethodType
import com.blockchain.nabu.models.data.EligibleAndNextPaymentRecurringBuy
import com.blockchain.nabu.models.data.RecurringBuy
import com.blockchain.nabu.models.data.RecurringBuyState
import com.blockchain.nabu.models.responses.simplebuy.toRecurringBuyFrequency
import com.blockchain.utils.fromIso8601ToUtc
import com.blockchain.utils.toLocalTime
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.Money
import java.util.Date

class GetAccumulatedInPeriodToIsFirstTimeBuyerMapper : Mapper<List<AccumulatedInPeriod>, Boolean> {
    override fun map(type: List<AccumulatedInPeriod>): Boolean =
        type.first { it.termType == AccumulatedInPeriod.ALL }.amount.value.toDouble() == 0.0
}

class GetNextPaymentDateListToFrequencyDateMapper :
    Mapper<List<NextPaymentRecurringBuy>, List<EligibleAndNextPaymentRecurringBuy>> {
    override fun map(type: List<NextPaymentRecurringBuy>): List<EligibleAndNextPaymentRecurringBuy> {
        return type.map {
            EligibleAndNextPaymentRecurringBuy(
                frequency = it.period.toRecurringBuyFrequency(),
                nextPaymentDate = it.nextPayment,
                eligibleMethods = mapStringToPaymentMethod(it.eligibleMethods)
            )
        }.toList()
    }

    private fun mapStringToPaymentMethod(eligibleMethods: List<String>): List<PaymentMethodType> {
        return eligibleMethods.map {
            when (it) {
                NextPaymentRecurringBuy.BANK_TRANSFER -> PaymentMethodType.BANK_TRANSFER
                NextPaymentRecurringBuy.PAYMENT_CARD -> PaymentMethodType.PAYMENT_CARD
                NextPaymentRecurringBuy.FUNDS -> PaymentMethodType.FUNDS
                else -> PaymentMethodType.UNKNOWN
            }
        }.toList()
    }
}

class RecurringBuyResponseToRecurringBuyMapper(
    private val assetCatalogue: AssetCatalogue
) : Mapper<List<RecurringBuyResponse>, List<RecurringBuy>> {
    override fun map(type: List<RecurringBuyResponse>): List<RecurringBuy> {
        return type.mapNotNull {
            val asset = assetCatalogue.assetInfoFromNetworkTicker(it.destinationCurrency) ?: return@mapNotNull null
            val fiatCurrency = assetCatalogue.fiatFromNetworkTicker(it.inputCurrency) ?: return@mapNotNull null
            return@mapNotNull RecurringBuy(
                id = it.id,
                state = it.state.toRecurringBuyState(),
                recurringBuyFrequency = it.period.toRecurringBuyFrequency(),
                nextPaymentDate = it.nextPayment.fromIso8601ToUtc()?.toLocalTime() ?: Date(),
                paymentMethodType = it.paymentMethod.toPaymentMethodType(),
                amount = Money.fromMinor(fiatCurrency, it.inputValue.toBigInteger()),
                asset = asset,
                createDate = it.insertedAt.fromIso8601ToUtc()?.toLocalTime() ?: Date(),
                paymentMethodId = it.paymentMethodId
            )
        }
    }

    private fun String.toRecurringBuyState() =
        when (this) {
            com.blockchain.nabu.models.responses.simplebuy.RecurringBuyResponse.ACTIVE -> RecurringBuyState.ACTIVE
            com.blockchain.nabu.models.responses.simplebuy.RecurringBuyResponse.INACTIVE -> RecurringBuyState.INACTIVE
            else -> throw IllegalStateException("Unsupported recurring state")
        }
}
