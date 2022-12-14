package com.blockchain.nabu.models.responses.simplebuy

import com.blockchain.core.recurringbuy.domain.RecurringBuy
import com.blockchain.core.recurringbuy.domain.RecurringBuyFrequency
import com.blockchain.core.recurringbuy.domain.RecurringBuyState
import com.blockchain.nabu.datamanagers.RecurringBuyOrder
import com.blockchain.nabu.datamanagers.custodialwalletimpl.toPaymentMethodType
import com.blockchain.utils.fromIso8601ToUtc
import com.blockchain.utils.toLocalTime
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.Money
import java.util.Date
import kotlinx.serialization.Serializable

// TODO(dserrano-bc): remove this in favour of the one defined in the blockchainApi module
@Serializable
data class RecurringBuyResponse(
    val id: String,
    val userId: String,
    val inputCurrency: String,
    val inputValue: String,
    val destinationCurrency: String,
    val paymentMethod: String,
    val paymentMethodId: String? = null,
    val period: String,
    val nextPayment: String,
    val state: String,
    val insertedAt: String,
    val updatedAt: String
) {
    companion object {
        const val ACTIVE = "ACTIVE"
        const val INACTIVE = "INACTIVE"
        const val DAILY = "DAILY"
        const val WEEKLY = "WEEKLY"
        const val BI_WEEKLY = "BI_WEEKLY"
        const val MONTHLY = "MONTHLY"
    }
}

fun RecurringBuyResponse.toRecurringBuy(assetCatalogue: AssetCatalogue): RecurringBuy? {
    val asset = assetCatalogue.assetInfoFromNetworkTicker(destinationCurrency) ?: return null
    val fiatCurrency = assetCatalogue.fiatFromNetworkTicker(inputCurrency) ?: return null
    return RecurringBuy(
        id = id,
        state = state.toRecurringBuyState(),
        recurringBuyFrequency = period.toRecurringBuyFrequency(),
        nextPaymentDate = nextPayment.fromIso8601ToUtc()?.toLocalTime() ?: Date(),
        paymentMethodType = paymentMethod.toPaymentMethodType(),
        amount = Money.fromMinor(fiatCurrency, inputValue.toBigInteger()),
        asset = asset,
        createDate = insertedAt.fromIso8601ToUtc()?.toLocalTime() ?: Date(),
        paymentMethodId = paymentMethodId
    )
}

private fun String.toRecurringBuyState() =
    when (this) {
        RecurringBuyResponse.ACTIVE -> RecurringBuyState.ACTIVE
        RecurringBuyResponse.INACTIVE -> RecurringBuyState.INACTIVE
        else -> throw IllegalStateException("Unsupported recurring state")
    }

fun RecurringBuyResponse.toRecurringBuyOrder(): RecurringBuyOrder =
    RecurringBuyOrder(
        id = this.id,
        state = this.state.toRecurringBuyState()
    )

fun String.toRecurringBuyFrequency(): RecurringBuyFrequency =
    when (this) {
        RecurringBuyResponse.DAILY -> RecurringBuyFrequency.DAILY
        RecurringBuyResponse.WEEKLY -> RecurringBuyFrequency.WEEKLY
        RecurringBuyResponse.BI_WEEKLY -> RecurringBuyFrequency.BI_WEEKLY
        RecurringBuyResponse.MONTHLY -> RecurringBuyFrequency.MONTHLY
        else -> RecurringBuyFrequency.UNKNOWN
    }
