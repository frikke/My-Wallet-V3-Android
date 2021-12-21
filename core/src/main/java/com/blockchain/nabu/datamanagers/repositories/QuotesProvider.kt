package com.blockchain.nabu.datamanagers.repositories

import com.blockchain.nabu.Authenticator
import com.blockchain.nabu.datamanagers.CurrencyPair
import com.blockchain.nabu.datamanagers.PriceTier
import com.blockchain.nabu.datamanagers.TransferDirection
import com.blockchain.nabu.datamanagers.TransferQuote
import com.blockchain.nabu.models.responses.swap.QuoteRequest
import com.blockchain.nabu.service.NabuService
import com.blockchain.utils.fromIso8601ToUtc
import com.blockchain.utils.toLocalTime
import info.blockchain.balance.Money
import java.util.Date

class QuotesProvider(
    private val nabuService: NabuService,
    private val authenticator: Authenticator
) {

    fun fetchQuote(product: String = "BROKERAGE", direction: TransferDirection, pair: CurrencyPair) =
        authenticator.authenticate { sessionToken ->
            nabuService.fetchQuote(
                sessionToken,
                QuoteRequest(
                    product = product,
                    direction = direction.toString(),
                    pair = pair.rawValue
                )
            ).map {
                TransferQuote(
                    id = it.id,
                    prices = it.quote.priceTiers.map { price ->
                        PriceTier(
                            volume = Money.fromMinor(pair.source, price.volume.toBigInteger()),
                            price = Money.fromMinor(pair.destination, price.price.toBigInteger())
                        )
                    },
                    staticFee = Money.fromMinor(pair.source, it.staticFee.toBigInteger()),
                    networkFee = Money.fromMinor(pair.destination, it.networkFee.toBigInteger()),
                    sampleDepositAddress = it.sampleDepositAddress,
                    expirationDate = it.expiresAt.fromIso8601ToUtc()?.toLocalTime() ?: Date(),
                    creationDate = it.createdAt.fromIso8601ToUtc()?.toLocalTime() ?: Date()
                )
            }
        }
}
