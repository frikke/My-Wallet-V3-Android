package com.blockchain.coincore.impl

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.mock
import info.blockchain.balance.CryptoCurrency
import io.reactivex.rxjava3.core.Completable
import org.amshove.kluent.`should be`
import org.junit.Before
import org.junit.Test
import com.blockchain.coincore.loader.AssetCatalogueImpl
import com.blockchain.coincore.loader.AssetRemoteFeatureLookup
import com.blockchain.coincore.loader.RemoteFeature
import com.blockchain.coincore.testutil.CoincoreTestBase
import com.blockchain.core.dynamicassets.DynamicAssetsDataManager
import com.blockchain.remoteconfig.FeatureFlag
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.androidcore.utils.extensions.emptySubscribe

class AssetCatalogueTest : CoincoreTestBase() {

    private val featureFlag: FeatureFlag = mock {
        on { enabled }.thenReturn(Single.just(true))
    }

    private val featureConfig: AssetRemoteFeatureLookup = mock {
        on { init(any()) }.thenReturn(Completable.complete())
        on { featuresFor(anyOrNull()) }.thenReturn(setOf(RemoteFeature.Balance))
    }

    private val assetList = listOf(
        TEST_ASSET,
        SECONDARY_TEST_ASSET
        )

    private val assetsManager: DynamicAssetsDataManager = mock {
        on { availableCryptoAssets() }.thenReturn(Single.just(assetList))
    }

    private val subject = AssetCatalogueImpl(
        fixedAssets = setOf(
            CryptoCurrency.BTC,
            CryptoCurrency.BCH,
            CryptoCurrency.ETHER,
            CryptoCurrency.XLM
        ),
        featureFlag = featureFlag,
        assetsDataManager = assetsManager,
        featureConfig = featureConfig
    )

    @Before
    fun before() {
        subject.initialise().emptySubscribe()
    }

    @Test
    fun `lowercase btc`() {
        subject.fromNetworkTicker("btc") `should be` CryptoCurrency.BTC
    }

    @Test
    fun `uppercase BTC`() {
        subject.fromNetworkTicker("BTC") `should be` CryptoCurrency.BTC
    }

    @Test
    fun `lowercase bch`() {
        subject.fromNetworkTicker("btc") `should be` CryptoCurrency.BTC
    }

    @Test
    fun `uppercase BCH`() {
        subject.fromNetworkTicker("BCH") `should be` CryptoCurrency.BCH
    }

    @Test
    fun `lowercase eth`() {
        subject.fromNetworkTicker("eth") `should be` CryptoCurrency.ETHER
    }

    @Test
    fun `uppercase ETH`() {
        subject.fromNetworkTicker("ETH") `should be` CryptoCurrency.ETHER
    }

    @Test
    fun `uppercase XLM`() {
        subject.fromNetworkTicker("XLM") `should be` CryptoCurrency.XLM
    }

    @Test
    fun `lowercase xlm`() {
        subject.fromNetworkTicker("xlm") `should be` CryptoCurrency.XLM
    }

    @Test
    fun `uppercase Dynamic`() {
        subject.fromNetworkTicker("NOPE") `should be` TEST_ASSET
    }

    @Test
    fun `lowercase Dynamci`() {
        subject.fromNetworkTicker("nope") `should be` TEST_ASSET
    }

    @Test
    fun `empty should return null`() {
        subject.fromNetworkTicker("") `should be` null
    }

    @Test
    fun `not recognised should return null`() {
        subject.fromNetworkTicker("NONE") `should be` null
    }
}
