package com.blockchain.walletconnect.data

import app.cash.turbine.test
import com.blockchain.coincore.Asset
import com.blockchain.coincore.Coincore
import com.blockchain.core.chains.ethereum.EthDataManager
import com.blockchain.testutils.CoroutineTestRule
import com.blockchain.walletconnect.ui.networks.NetworkInfo
import com.blockchain.walletconnect.ui.networks.SelectNetworkIntents
import com.blockchain.walletconnect.ui.networks.SelectNetworkViewModel
import info.blockchain.balance.CoinNetwork
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.NetworkType
import io.mockk.every
import io.mockk.mockk
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SelectNetworkViewModelTest {

    @ExperimentalCoroutinesApi
    @get:Rule
    var coroutineTestRule = CoroutineTestRule()

    private val coincore: Coincore = mockk()
    private val ethDataManager: EthDataManager = mockk()

    private val asset: Asset = mockk {
        every { currency } returns evmAsset
    }

    private lateinit var subject: SelectNetworkViewModel

    @Before
    fun setUp() {
        subject = SelectNetworkViewModel(
            coincore = coincore,
            ethDataManager = ethDataManager
        )
    }

    @Test
    fun `load supported networks successfully and selects preselected`() = runTest {
        val ethNetworkInfoNoLogo = NetworkInfo(
            networkTicker = ethEvmNetwork.networkTicker,
            name = ethEvmNetwork.name,
            chainId = ethEvmNetwork.chainId!!
        )

        val ethNetworkInfo = NetworkInfo(
            networkTicker = ethEvmNetwork.networkTicker,
            name = ethEvmNetwork.name,
            chainId = ethEvmNetwork.chainId!!,
            logo = "logo"
        )

        every { ethDataManager.supportedNetworks } returns Single.just(listOf(ethEvmNetwork))
        every { coincore[ethEvmNetwork.networkTicker] } returns asset

        subject.viewState.test {
            awaitItem()
            subject.onIntent(SelectNetworkIntents.LoadSupportedNetworks(ETH_CHAIN_ID))
            awaitItem().run {
                networks shouldBeEqualTo listOf(ethNetworkInfoNoLogo)
                selectedNetwork shouldBeEqualTo ethNetworkInfoNoLogo
            }
            awaitItem().run {
                networks shouldBeEqualTo listOf(ethNetworkInfo)
                selectedNetwork shouldBeEqualTo ethNetworkInfo
            }
        }
    }

    @Test
    fun `select specified network`() = runTest {
        val ethNetworkInfoNoLogo = NetworkInfo(
            networkTicker = ethEvmNetwork.networkTicker,
            name = ethEvmNetwork.name,
            chainId = ethEvmNetwork.chainId!!
        )
        val ethNetworkInfo = NetworkInfo(
            networkTicker = ethEvmNetwork.networkTicker,
            name = ethEvmNetwork.name,
            chainId = ethEvmNetwork.chainId!!,
            logo = "logo"
        )
        val otherEvmNetworkInfoNoLogo = NetworkInfo(
            networkTicker = otherEvmNetwork.networkTicker,
            name = otherEvmNetwork.name,
            chainId = otherEvmNetwork.chainId!!
        )
        val otherEvmNetworkInfo = NetworkInfo(
            networkTicker = otherEvmNetwork.networkTicker,
            name = otherEvmNetwork.name,
            chainId = otherEvmNetwork.chainId!!,
            logo = "logo"
        )

        every { ethDataManager.supportedNetworks } returns Single.just(listOf(ethEvmNetwork, otherEvmNetwork))
        every { coincore[ethEvmNetwork.networkTicker] } returns asset
        every { coincore[otherEvmNetwork.networkTicker] } returns asset

        subject.viewState.test {
            awaitItem()
            subject.onIntent(SelectNetworkIntents.LoadSupportedNetworks(ETH_CHAIN_ID))
            awaitItem().run {
                networks shouldBeEqualTo listOf(ethNetworkInfoNoLogo, otherEvmNetworkInfoNoLogo)
                selectedNetwork shouldBeEqualTo ethNetworkInfoNoLogo
            }
            awaitItem().run {
                networks shouldBeEqualTo listOf(ethNetworkInfo, otherEvmNetworkInfo)
                selectedNetwork shouldBeEqualTo ethNetworkInfo
            }
            subject.onIntent(SelectNetworkIntents.SelectNetwork(EVM_CHAIN_ID))
            awaitItem().run {
                networks shouldBeEqualTo listOf(ethNetworkInfo, otherEvmNetworkInfo)
                selectedNetwork shouldBeEqualTo otherEvmNetworkInfo
            }
        }
    }

    companion object {
        private const val ETH_CHAIN_ID = 1
        private val ethEvmNetwork = CoinNetwork(
            explorerUrl = "https://eth.io/transactions",
            nativeAssetTicker = "ETH",
            networkTicker = "ETH",
            name = "Ethereum",
            shortName = "Ethereum",
            isMemoSupported = false,
            type = NetworkType.EVM,
            chainId = 1,
            feeCurrencies = listOf("native"),
            nodeUrls = listOf("sfasdsa")
        )
        private val evmAsset = CryptoCurrency(
            displayTicker = "EVM",
            networkTicker = "EVM",
            name = "Evm Network",
            categories = emptySet(),
            precisionDp = 1,
            requiredConfirmations = 1,
            colour = "",
            logo = "logo"
        )
        private const val EVM_CHAIN_ID = 2
        private val otherEvmNetwork = CoinNetwork(
            explorerUrl = "https://eth.io/transactions",
            nativeAssetTicker = "OTHER",
            networkTicker = "OTHER",
            name = "Other",
            shortName = "Other",
            isMemoSupported = false,
            type = NetworkType.EVM,
            chainId = 2,
            feeCurrencies = listOf("native"),
            nodeUrls = listOf("sfasdsa")
        )
    }
}
