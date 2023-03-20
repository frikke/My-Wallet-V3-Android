package com.dex.data

import com.blockchain.api.dex.DexQuotesApiService
import com.blockchain.api.dex.FromCurrency
import com.blockchain.api.dex.ToCurrency
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.Coincore
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.flatMap
import com.blockchain.outcome.map
import com.blockchain.utils.awaitOutcome
import com.dex.domain.DexAccount
import com.dex.domain.DexQuote
import com.dex.domain.DexQuoteParams
import com.dex.domain.DexQuotesService
import com.dex.domain.OutputAmount
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.Money
import info.blockchain.balance.isLayer2Token

class DexQuotesRepository(
    private val dexQuotesApiService: DexQuotesApiService,
    private val coincore: Coincore,
    private val assetCatalogue: AssetCatalogue
) : DexQuotesService {
    override suspend fun quote(
        dexQuoteParams: DexQuoteParams
    ): Outcome<Exception, DexQuote> {
        val address = dexQuoteParams.sourceAccount.receiveAddress()
        return address.flatMap {
            dexQuotesApiService.quote(
                fromCurrency = FromCurrency(
                    chainId = dexQuoteParams.sourceAccount.chainId,
                    symbol = dexQuoteParams.sourceAccount.currency.networkTicker,
                    address = dexQuoteParams.sourceAccount.contractAddress,
                    amount = dexQuoteParams.amount.toBigInteger().toString()
                ),
                toCurrency = ToCurrency(
                    chainId = dexQuoteParams.destinationAccount.chainId,
                    symbol = dexQuoteParams.destinationAccount.currency.networkTicker,
                    address = dexQuoteParams.destinationAccount.contractAddress,
                ),
                slippage = dexQuoteParams.slippage,
                address = it
            ).map { resp ->
                DexQuote(
                    amount = dexQuoteParams.amount,
                    outputAmount = OutputAmount(
                        expectedOutput = Money.fromMinor(
                            currency = dexQuoteParams.destinationAccount.currency,
                            value = resp.quote.buyAmount.amount.toBigInteger()
                        ),
                        minOutputAmount = Money.fromMinor(
                            currency = dexQuoteParams.destinationAccount.currency,
                            value = resp.quote.buyAmount.minAmount?.toBigInteger()
                                ?: resp.quote.buyAmount.amount.toBigInteger()
                        )
                    )
                )
            }
        }
    }

    private suspend fun DexAccount.receiveAddress(): Outcome<Exception, String> {
        val nativeAssetTicker =
            currency.takeIf { it.isLayer2Token }?.coinNetwork?.nativeAssetTicker ?: currency.networkTicker
        val currency = assetCatalogue.fromNetworkTicker(nativeAssetTicker) ?: return Outcome.Failure(
            IllegalStateException("Unknown currency")
        )
        return coincore[currency].defaultAccount(AssetFilter.NonCustodial).flatMap { acc ->
            acc.receiveAddress.map { receiveAddress ->
                receiveAddress.address
            }
        }.awaitOutcome()
    }
}
