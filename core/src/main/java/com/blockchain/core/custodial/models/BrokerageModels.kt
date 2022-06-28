package com.blockchain.core.custodial.models

import com.blockchain.nabu.datamanagers.BuySellOrder
import info.blockchain.balance.Money
import java.lang.Long.min
import java.time.Duration
import java.time.ZonedDateTime

data class BrokerageQuote(
    val id: String?,
    val price: Money,
    val quoteMargin: Double?,
    val availability: Availability?,
    val feeDetails: QuoteFee,
    val createdAt: ZonedDateTime,
    val expiresAt: ZonedDateTime,
) {

    fun millisToExpire(): Long {
        return Duration.between(
            now,
            expiresAt
        ).toMillis()
    }

    val secondsToExpire: Float
        get() = millisToExpire().div(1000f)

    private val now: ZonedDateTime
        get() = ZonedDateTime.now(expiresAt.zone)

    var totalDurationUI = min(
        Duration.between(
            now,
            expiresAt
        ).seconds,
        MIN_QUOTE_REFRESH
    )

    private var expireTimeUI = now.plusSeconds(totalDurationUI)

    fun secondsToExpireUI(): Long {
        val duration = Duration.between(
            now,
            expireTimeUI
        ).seconds
        if (duration == 0L) initializeTimerValues()
        return duration
    }

    private fun initializeTimerValues() {
        totalDurationUI = min(
            Duration.between(
                now,
                expiresAt
            ).seconds,
            MIN_QUOTE_REFRESH
        )
        expireTimeUI = now.plusSeconds(totalDurationUI)
    }

    companion object {
        const val MIN_QUOTE_REFRESH = 30L
    }
}

data class BuyOrderAndQuote(
    val buyOrder: BuySellOrder,
    val quote: BrokerageQuote,
)

data class QuoteFee(
    val fee: Money,
    val feeBeforePromo: Money,
    val promo: Promo,
)

enum class Promo {
    NEW_USER, NO_PROMO
}

enum class Availability {
    INSTANT, REGULAR, UNAVAILABLE
}
