package com.blockchain.coincore.assetsserivce

import com.blockchain.api.services.DynamicAsset
import com.blockchain.api.services.DynamicAssetProducts
import com.blockchain.coincore.loader.toAssetInfo
import info.blockchain.balance.AssetCategory
import info.blockchain.balance.CryptoCurrency
import org.junit.Test

class DynamicAssetServiceTest {

    @Test
    fun `Private Key Dynamic Asset should be mapped properly to Asset Info`() {
        val dynamicAsset = DynamicAsset(
            assetName = "assetName",
            networkTicker = "networkTicker",
            displayTicker = "displayTicker",
            isFiat = false,
            precision = 12,
            minConfirmations = 1,
            products = setOf(DynamicAssetProducts.PrivateKey, DynamicAssetProducts.DynamicSelfCustody),
        )

        val assetInfo = dynamicAsset.toAssetInfo()
        assert(
            assetInfo == CryptoCurrency(
                displayTicker = "displayTicker",
                networkTicker = "networkTicker",
                name = "assetName",
                precisionDp = 12,
                requiredConfirmations = 1,
                colour = "#0C6CF2",
                categories = setOf(AssetCategory.NON_CUSTODIAL)
            )
        )
    }

    @Test
    fun `Custodial Dynamic Asset should be mapped properly to Asset Info`() {
        val dynamicAsset = DynamicAsset(
            assetName = "CustodialName",
            networkTicker = "CustodialNetworkTicker",
            displayTicker = "CustodialDisplayTicker",
            isFiat = false,
            precision = 12,
            minConfirmations = 1,
            products = setOf(DynamicAssetProducts.CustodialWalletBalance),
        )

        val assetInfo = dynamicAsset.toAssetInfo()
        assert(
            assetInfo == CryptoCurrency(
                displayTicker = "CustodialDisplayTicker",
                networkTicker = "CustodialNetworkTicker",
                name = "CustodialName",
                precisionDp = 12,
                requiredConfirmations = 1,
                colour = "#0C6CF2",
                categories = setOf(AssetCategory.CUSTODIAL)
            )
        )
    }
}
