package com.blockchain.coincore.impl

import com.blockchain.featureflag.FeatureFlag
import com.blockchain.preferences.AuthPrefs
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.api.WalletApi
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.rxjava3.core.Observable
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody
import org.junit.Before
import org.junit.Test

class BackendNotificationUpdaterTest {

    private val walletApi = mockk<WalletApi>()
    private val prefs = mockk<AuthPrefs>()
    private val json = Json
    private val replaceGsonKtxFF = mockk<FeatureFlag>()

    private val responseBody = mockk<ResponseBody>()

    private val backendNotificationUpdater = BackendNotificationUpdater(
        walletApi = walletApi,
        prefs = prefs,
        json = json,
        replaceGsonKtxFF = replaceGsonKtxFF
    )

    private val jsonString =
        """[{"coin":"BTC","addresses":[]},{"coin":"BCH","addresses":[]},{"coin":"ETH","addresses":[]}]"""

    @Before
    fun setUp() {
        every { prefs.walletGuid } returns "walletGuid"
        every { prefs.sharedKey } returns "sharedKey"
        every { walletApi.submitCoinReceiveAddresses(any(), any(), any()) } returns Observable.just(responseBody)
    }

    @Test
    fun `GIVEN ktx, WHEN updateNotificationBackend is called, THEN walletApi-submitCoinReceiveAddresses should be called with correct data`() {
        every { replaceGsonKtxFF.isEnabled } returns true

        backendNotificationUpdater.updateNotificationBackend(
            item = NotificationAddresses(
                assetTicker = CryptoCurrency.BTC.networkTicker,
                addressList = listOf()
            )
        )

        backendNotificationUpdater.updateNotificationBackend(
            item = NotificationAddresses(
                assetTicker = CryptoCurrency.BCH.networkTicker,
                addressList = listOf()
            )
        )

        backendNotificationUpdater.updateNotificationBackend(
            item = NotificationAddresses(
                assetTicker = CryptoCurrency.ETHER.networkTicker,
                addressList = listOf()
            )
        )

        verify(exactly = 1) {
            walletApi.submitCoinReceiveAddresses(
                guid = "walletGuid",
                sharedKey = "sharedKey",
                coinAddresses = jsonString
            )
        }
    }

    @Test
    fun `GIVEN gson, WHEN updateNotificationBackend is called, THEN walletApi-submitCoinReceiveAddresses should be called with correct data`() {
        every { replaceGsonKtxFF.isEnabled } returns false

        backendNotificationUpdater.updateNotificationBackend(
            item = NotificationAddresses(
                assetTicker = CryptoCurrency.BTC.networkTicker,
                addressList = listOf()
            )
        )

        backendNotificationUpdater.updateNotificationBackend(
            item = NotificationAddresses(
                assetTicker = CryptoCurrency.BCH.networkTicker,
                addressList = listOf()
            )
        )

        backendNotificationUpdater.updateNotificationBackend(
            item = NotificationAddresses(
                assetTicker = CryptoCurrency.ETHER.networkTicker,
                addressList = listOf()
            )
        )

        verify(exactly = 1) {
            walletApi.submitCoinReceiveAddresses(
                guid = "walletGuid",
                sharedKey = "sharedKey",
                coinAddresses = jsonString
            )
        }
    }
}
