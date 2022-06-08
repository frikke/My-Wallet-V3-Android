package com.blockchain.core.custodial

import com.blockchain.android.testutils.rxInit
import com.blockchain.api.services.CustodialBalanceService
import com.blockchain.api.services.TradingBalance
import com.blockchain.nabu.FakeAuthenticator
import com.blockchain.nabu.GBP
import com.blockchain.nabu.USD
import com.blockchain.nabu.util.fakefactory.nabu.FakeNabuSessionTokenFactory
import com.blockchain.testutils.waitForCompletionWithoutErrors
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetCategory
import info.blockchain.balance.CryptoCurrency
import io.reactivex.rxjava3.core.Single
import org.junit.Rule
import org.junit.Test

class TradingBalanceCallCacheTest {

    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        computationTrampoline()
        ioTrampoline()
    }

    private val authenticator = FakeAuthenticator(FakeNabuSessionTokenFactory.any)

    private val assetCatalogue: AssetCatalogue = mock {
        on { fromNetworkTicker(CRYPTO_TICKER_1) }.thenReturn(CRYPTO_ASSET_1)
        on { fromNetworkTicker(CRYPTO_TICKER_2) }.thenReturn(CRYPTO_ASSET_2)
        on { fromNetworkTicker(FIAT_TICKER_1) }.thenReturn(FIAT_CURRENCY_1)
        on { fromNetworkTicker(FIAT_TICKER_2) }.thenReturn(FIAT_CURRENCY_2)
        on { fromNetworkTicker(UNKNOWN_TICKER) }.thenReturn(null)
    }

    private val balanceService: CustodialBalanceService = mock()

    private val subject = TradingBalanceCallCache(
        balanceService,
        assetCatalogue,
        authenticator
    )

    @Test
    fun `cache is transformed correctly`() {
        givenATradingBalanceFor(
            CRYPTO_TICKER_1,
            CRYPTO_TICKER_2,
            FIAT_TICKER_1,
            FIAT_TICKER_2,
            UNKNOWN_TICKER
        )

        subject.getTradingBalances()
            .test()
            .waitForCompletionWithoutErrors()
            .assertValue {
                it.balances.keys.size == 4
            }.assertValue {
                it.balances.keys.containsAll(setOf(CRYPTO_ASSET_1, CRYPTO_ASSET_2, FIAT_CURRENCY_1, FIAT_CURRENCY_2))
            }
    }

    private fun givenATradingBalanceFor(vararg assetCode: String) {
        val mapEntries = assetCode.map { makeTradingBalanceFor(it) }
        whenever(
            balanceService.getTradingBalanceForAllAssets(any())
        ).thenReturn(
            Single.just(mapEntries)
        )
    }

    private fun makeTradingBalanceFor(symbol: String) =
        TradingBalance(
            assetTicker = symbol,
            pending = 2.toBigInteger(),
            total = 10.toBigInteger(),
            withdrawable = 3.toBigInteger()
        )

    companion object {
        private const val CRYPTO_TICKER_1 = "CRYPTO1"
        private const val CRYPTO_TICKER_2 = "CRYPTO2"
        private const val FIAT_TICKER_1 = "USD"
        private const val FIAT_TICKER_2 = "GBP"
        private const val UNKNOWN_TICKER = "NOPE!"

        private val CRYPTO_ASSET_1 = object : CryptoCurrency(
            networkTicker = CRYPTO_TICKER_1,
            displayTicker = CRYPTO_TICKER_1,
            name = "Crypto_1",
            categories = setOf(AssetCategory.CUSTODIAL, AssetCategory.NON_CUSTODIAL),
            precisionDp = 8,
            requiredConfirmations = 5,
            colour = "#123456"
        ) {}

        private val CRYPTO_ASSET_2 = object : CryptoCurrency(
            networkTicker = CRYPTO_TICKER_2,
            displayTicker = CRYPTO_TICKER_2,
            name = "Crypto_2",
            categories = setOf(AssetCategory.CUSTODIAL),
            precisionDp = 8,
            requiredConfirmations = 5,
            colour = "#123456"
        ) {}

        private val FIAT_CURRENCY_1 = USD
        private val FIAT_CURRENCY_2 = GBP
    }
}
