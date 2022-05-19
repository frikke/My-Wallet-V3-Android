package com.blockchain.coincore.xlm

import com.blockchain.android.testutils.rxInit
import com.blockchain.core.custodial.TradingBalanceDataManager
import com.blockchain.core.interest.InterestBalanceDataManager
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.logging.RemoteLogger
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.WalletStatus
import com.blockchain.sunriver.XlmDataManager
import com.blockchain.sunriver.XlmFeesFetcher
import com.blockchain.wallet.DefaultLabels
import com.nhaarman.mockitokotlin2.mock
import exchange.ExchangeLinking
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.walletoptions.WalletOptionsDataManager

class XlmAssetUriParseTest {

    @get:Rule
    val rxSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
        computationTrampoline()
    }

    private val payloadManager: PayloadDataManager = mock()
    private val xlmDataManager: XlmDataManager = mock()
    private val xlmFeesFetcher: XlmFeesFetcher = mock()
    private val walletOptionsDataManager: WalletOptionsDataManager = mock()
    private val custodialManager: CustodialWalletManager = mock()
    private val exchangeRates: ExchangeRatesDataManager = mock()
    private val tradingBalances: TradingBalanceDataManager = mock()
    private val interestBalances: InterestBalanceDataManager = mock()
    private val currencyPrefs: CurrencyPrefs = mock()
    private val labels: DefaultLabels = mock()
    private val exchangeLinking: ExchangeLinking = mock()
    private val remoteLogger: RemoteLogger = mock()
    private val walletPreferences: WalletStatus = mock()

    private val subject = XlmAsset(
        payloadManager = payloadManager,
        xlmDataManager = xlmDataManager,
        xlmFeesFetcher = xlmFeesFetcher,
        walletOptionsDataManager = walletOptionsDataManager,
        custodialManager = custodialManager,
        tradingBalances = tradingBalances,
        interestBalances = interestBalances,
        exchangeRates = exchangeRates,
        currencyPrefs = currencyPrefs,
        labels = labels,
        exchangeLinking = exchangeLinking,
        remoteLogger = remoteLogger,
        walletPreferences = walletPreferences,
        identity = mock(),
        addressResolver = mock()
    )

    @Test
    fun parseValidAddress() {

        val expectedResult = XlmAddress(
            _address = VALID_SCAN_URI,
            _label = VALID_SCAN_URI
        )

        subject.parseAddress(VALID_SCAN_URI)
            .test()
            .assertNoErrors()
            .assertResult(expectedResult)
    }

    @Test
    fun parseInvalidAddress() {

        subject.parseAddress(INVALID_SCAN_URI)
            .test()
            .assertNoErrors()
            .assertComplete()
    }

    companion object {
        private const val VALID_SCAN_URI = "GDYULVJK2T6G7HFUC76LIBKZEMXPKGINSG6566EPWJKCLXTYVWJ7XPY4"
        private const val INVALID_SCAN_URI = "bitcoin:GDY6LIBKZEMXPKGINSG6566EPWJKCLXTYVWJ7XPY4"
    }
}
