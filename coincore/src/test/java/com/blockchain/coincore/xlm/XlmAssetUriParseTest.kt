package com.blockchain.coincore.xlm

import com.blockchain.android.testutils.rxInit
import com.blockchain.core.walletoptions.WalletOptionsDataManager
import com.blockchain.preferences.WalletStatusPrefs
import com.blockchain.sunriver.XlmDataManager
import com.blockchain.sunriver.XlmFeesFetcher
import com.nhaarman.mockitokotlin2.mock
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.androidcore.data.payload.PayloadDataManager

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
    private val walletPreferences: WalletStatusPrefs = mock()

    private val subject = XlmAsset(
        payloadManager = payloadManager,
        xlmDataManager = xlmDataManager,
        xlmFeesFetcher = xlmFeesFetcher,
        walletOptionsDataManager = walletOptionsDataManager,
        walletPreferences = walletPreferences,
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
