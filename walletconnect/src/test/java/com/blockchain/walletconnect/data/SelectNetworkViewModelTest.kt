package com.blockchain.walletconnect.data

import app.cash.turbine.test
import com.blockchain.coincore.Asset
import com.blockchain.coincore.Coincore
import com.blockchain.core.chains.EvmNetwork
import com.blockchain.core.chains.ethereum.EthDataManager
import com.blockchain.testutils.CoroutineTestRule
import com.blockchain.walletconnect.ui.networks.NetworkInfo
import com.blockchain.walletconnect.ui.networks.SelectNetworkIntents
import com.blockchain.walletconnect.ui.networks.SelectNetworkViewModel
import info.blockchain.balance.CryptoCurrency
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
            name = ethEvmNetwork.networkName,
            chainId = ethEvmNetwork.chainId,
        )

        val ethNetworkInfo = NetworkInfo(
            networkTicker = ethEvmNetwork.networkTicker,
            name = ethEvmNetwork.networkName,
            chainId = ethEvmNetwork.chainId,
            logo = "logo"
        )

        every { ethDataManager.supportedNetworks } returns Single.just(setOf(ethEvmNetwork))
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
            name = ethEvmNetwork.networkName,
            chainId = ethEvmNetwork.chainId,
        )
        val ethNetworkInfo = NetworkInfo(
            networkTicker = ethEvmNetwork.networkTicker,
            name = ethEvmNetwork.networkName,
            chainId = ethEvmNetwork.chainId,
            logo = "logo"
        )
        val otherEvmNetworkInfoNoLogo = NetworkInfo(
            networkTicker = otherEvmNetwork.networkTicker,
            name = otherEvmNetwork.networkName,
            chainId = otherEvmNetwork.chainId,
        )
        val otherEvmNetworkInfo = NetworkInfo(
            networkTicker = otherEvmNetwork.networkTicker,
            name = otherEvmNetwork.networkName,
            chainId = otherEvmNetwork.chainId,
            logo = "logo"
        )

        every { ethDataManager.supportedNetworks } returns Single.just(setOf(ethEvmNetwork, otherEvmNetwork))
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
        private val ethEvmNetwork = EvmNetwork(
            "ETH",
            "Ethereum",
            "Ethereum",
            ETH_CHAIN_ID,
            "",
            ""
        )
        private val evmAsset = CryptoCurrency(
            displayTicker = "EVM",
            networkTicker = "EVM",
            name = "Evm Network",
            categories = emptySet(),
            precisionDp = 1,
            requiredConfirmations = 1,
            colour = "",
            logo = "logo",
            isErc20 = true
        )
        private const val EVM_CHAIN_ID = 2
        private val otherEvmNetwork = EvmNetwork(
            "OTHER",
            "Other",
            "Other",
            EVM_CHAIN_ID,
            "",
            ""
        )
    }
}
