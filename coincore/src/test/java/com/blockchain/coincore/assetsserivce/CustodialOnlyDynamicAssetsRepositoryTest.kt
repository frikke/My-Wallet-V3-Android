package com.blockchain.coincore.assetsserivce

import com.blockchain.api.services.AssetDiscoveryApiService
import com.blockchain.api.services.DynamicAsset
import com.blockchain.api.services.DynamicAssetProducts
import com.blockchain.coincore.loader.CustodialOnlyDynamicAssetsRepository
import com.blockchain.coincore.loader.NonCustodialDynamicAssetRepository
import com.blockchain.coincore.loader.NonCustodialL2sDynamicAssetRepository
import com.blockchain.coincore.loader.toAssetInfo
import com.blockchain.coincore.testutil.CoincoreTestBase.Companion.TEST_ASSET
import com.blockchain.coincore.testutil.CoincoreTestBase.Companion.TEST_ASSET_NC
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Single
import kotlin.random.Random
import org.junit.Test

class CustodialOnlyDynamicAssetsRepositoryTest {
    private val discoveryService: AssetDiscoveryApiService = mock()
    private val subject = CustodialOnlyDynamicAssetsRepository(discoveryService)

    @Test
    fun `When Custodial and NonCustodial assets returned Result should contain only custodial`() {
        val dynamicAssets = listOf(
            dynamicAssetWithCategories(
                DynamicAssetProducts.DynamicSelfCustody, DynamicAssetProducts.PrivateKey
            ),
            dynamicAssetWithCategories(
                DynamicAssetProducts.CustodialWalletBalance, DynamicAssetProducts.PrivateKey
            ),
            dynamicAssetWithCategories(
                DynamicAssetProducts.CustodialWalletBalance, DynamicAssetProducts.InterestBalance
            ),
            dynamicAssetWithCategories(
                DynamicAssetProducts.CustodialWalletBalance
            ),
        )
        whenever(discoveryService.getCustodialAssets()).thenReturn(
            Single.just(
                dynamicAssets
            )
        )

        val test = subject.availableCryptoAssets().test()
        test.assertValue {
            it == listOf(
                dynamicAssets[1].copy(products = setOf(DynamicAssetProducts.CustodialWalletBalance))
                    .toAssetInfo(),
                dynamicAssets[2].copy(
                    products = setOf(DynamicAssetProducts.CustodialWalletBalance, DynamicAssetProducts.InterestBalance)
                ).toAssetInfo(),
                dynamicAssets[3].toAssetInfo()
            )
        }
    }
}

class NonCustodialOnlyDynamicAssetsRepositoryTest {
    private val discoveryService: AssetDiscoveryApiService = mock()
    private val fixedAssets = setOf(TEST_ASSET_NC)
    private val l2sDynamicAssetRepository = mock<NonCustodialL2sDynamicAssetRepository>()
    private val subject = NonCustodialDynamicAssetRepository(discoveryService, fixedAssets, l2sDynamicAssetRepository)

    @Test
    fun `When Custodial and NonCustodial assets returned Result should contain only non custodial+any fixed assets`() {
        val dynamicAssets = listOf(
            dynamicAssetWithCategories(
                DynamicAssetProducts.DynamicSelfCustody, DynamicAssetProducts.PrivateKey
            ),
            dynamicAssetWithCategories(
                DynamicAssetProducts.CustodialWalletBalance, DynamicAssetProducts.PrivateKey
            ),
            dynamicAssetWithCategories(
                DynamicAssetProducts.CustodialWalletBalance, DynamicAssetProducts.InterestBalance
            ),
            dynamicAssetWithCategories(
                DynamicAssetProducts.CustodialWalletBalance
            ),
        )
        whenever(discoveryService.getErc20Assets()).thenReturn(
            Single.just(
                dynamicAssets
            )
        )

        whenever(l2sDynamicAssetRepository.availableL2s()).thenReturn(Single.just(emptyList()))

        val test = subject.availableCryptoAssets().test()
        test.assertValue {
            it == listOf(
                dynamicAssets[0].copy(
                    products = setOf(DynamicAssetProducts.DynamicSelfCustody, DynamicAssetProducts.PrivateKey)
                ).toAssetInfo(),
                dynamicAssets[1].copy(
                    products = setOf(DynamicAssetProducts.PrivateKey)
                ).toAssetInfo(),
                TEST_ASSET_NC
            )
        }
    }

    @Test
    fun `When Fixed asset is returned then there should be no duplicate`() {
        whenever(discoveryService.getErc20Assets()).thenReturn(
            Single.just(
                listOf(
                    DynamicAsset(
                        assetName = TEST_ASSET_NC.name,
                        networkTicker = TEST_ASSET_NC.networkTicker,
                        displayTicker = TEST_ASSET_NC.displayTicker,
                        isFiat = false,
                        precision = TEST_ASSET_NC.precisionDp,
                        products = setOf(DynamicAssetProducts.PrivateKey)
                    )
                )
            )
        )
        whenever(l2sDynamicAssetRepository.availableL2s()).thenReturn(Single.just(emptyList()))

        val test = subject.availableCryptoAssets().test()
        test.assertValue {
            it == listOf(
                TEST_ASSET
            )
        }
    }
}

internal fun dynamicAssetWithCategories(vararg products: DynamicAssetProducts): DynamicAsset =
    DynamicAsset(
        assetName = "Test ${Random.nextInt()}",
        networkTicker = "networkTicker ${Random.nextInt()}",
        displayTicker = "displayTicker ${Random.nextInt()}",
        isFiat = false,
        precision = Random.nextInt(),
        products = products.toSet()
    )
