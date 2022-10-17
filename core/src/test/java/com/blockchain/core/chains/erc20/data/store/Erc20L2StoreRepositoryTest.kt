package com.blockchain.core.chains.erc20.data.store

import com.blockchain.api.ethereum.evm.BalancesResponse
import com.blockchain.api.ethereum.evm.EvmAddressResponse
import com.blockchain.api.ethereum.evm.EvmBalanceResponse
import com.blockchain.core.chains.erc20.data.Erc20L2StoreRepository
import com.blockchain.core.chains.erc20.domain.Erc20L2StoreService
import com.blockchain.core.chains.erc20.domain.model.Erc20Balance
import com.blockchain.core.chains.ethereum.EthDataManager
import com.blockchain.data.DataResource
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetCategory
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class Erc20L2StoreRepositoryTest {
    private val assetCatalogue = mockk<AssetCatalogue>()
    private val ethDataManager = mockk<EthDataManager>()
    private val erc20L2DataSource = mockk<Erc20L2DataSource>()

    private val erc20L2StoreService: Erc20L2StoreService = Erc20L2StoreRepository(
        assetCatalogue = assetCatalogue,
        ethDataManager = ethDataManager,
        erc20L2DataSource = erc20L2DataSource
    )

    private val cryptoCurrencyNative = object : CryptoCurrency(
        displayTicker = "CRYPTO_NATIVE",
        networkTicker = "CRYPTO_NATIVE",
        name = "Crypto_1",
        categories = setOf(AssetCategory.CUSTODIAL, AssetCategory.NON_CUSTODIAL),
        precisionDp = 8,
        requiredConfirmations = 5,
        colour = "#123456",
        isErc20 = true
    ) {}

    private val cryptoCurrency = object : CryptoCurrency(
        displayTicker = "CRYPTO2",
        networkTicker = "CRYPTO2",
        name = "Crypto_2",
        categories = setOf(AssetCategory.CUSTODIAL, AssetCategory.NON_CUSTODIAL),
        precisionDp = 8,
        requiredConfirmations = 5,
        colour = "#678912",
        isErc20 = true
    ) {}

    private val evmBalanceResponseNative = EvmBalanceResponse(
        contractAddress = "native",
        amount = 3.toBigInteger()
    )

    private val evmBalanceResponse = EvmBalanceResponse(
        contractAddress = "contractAddress",
        amount = 2.toBigInteger()
    )

    private val evmAddressResponse = EvmAddressResponse(
        address = "accountHash",
        balances = listOf(evmBalanceResponseNative, evmBalanceResponse)
    )

    private val balancesResponse = BalancesResponse(
        addresses = listOf(evmAddressResponse)
    )

    private val erc20BalanceNative = Erc20Balance(
        balance = CryptoValue.fromMinor(cryptoCurrencyNative, 3.toBigInteger()),
        hasTransactions = true
    )

    private val erc20Balance = Erc20Balance(
        balance = CryptoValue.fromMinor(cryptoCurrency, 2.toBigInteger()),
        hasTransactions = true
    )

    private val data = mapOf(cryptoCurrencyNative to erc20BalanceNative, cryptoCurrency to erc20Balance)

    @Before
    fun setUp() {
        every { erc20L2DataSource.streamData(any()) } returns flowOf(DataResource.Data(balancesResponse))
        every { erc20L2DataSource.invalidate(any()) } just Runs
        every { assetCatalogue.assetFromL1ChainByContractAddress(l1chain = "CRYPTO_NATIVE", any()) } returns cryptoCurrency
        every { assetCatalogue.assetInfoFromNetworkTicker(symbol = "CRYPTO_NATIVE") } returns cryptoCurrencyNative
        every { ethDataManager.accountAddress } returns "accountHash"
    }

    @Test
    fun `WHEN getBalances is called, THEN data should be returned`() {
        erc20L2StoreService.getBalances(networkTicker = "CRYPTO_NATIVE")
            .test()
            .await()
            .assertValue {
                it == data
            }

        verify(exactly = 1) {
            assetCatalogue.assetFromL1ChainByContractAddress(
                l1chain = "CRYPTO_NATIVE",
                contractAddress = "contractAddress"
            )
        }

        verify(exactly = 1) {
            assetCatalogue.assetInfoFromNetworkTicker(symbol = "CRYPTO_NATIVE")
        }
    }

    @Test
    fun `GIVEN asset included, WHEN getBalanceFor is called, THEN erc20Balance should be returned`() {
        erc20L2StoreService.getBalanceFor(
            networkTicker = "CRYPTO_NATIVE",
            asset = cryptoCurrency
        )
            .test()
            .await()
            .assertValue {
                it == erc20Balance
            }
    }

    @Test
    fun `GIVEN asset not included, WHEN getBalanceFor is called, THEN Erc20Balance-zero should be returned`() {
        val asset = CryptoCurrency.BTC

        erc20L2StoreService.getBalanceFor(
            networkTicker = "CRYPTO2",
            asset = asset
        )
            .test()
            .await()
            .assertValue {
                it == Erc20Balance.zero(asset)
            }
    }

    @Test
    fun `WHEN getActiveAssets is called, THEN data-keys should be returned`() = runTest {
        val result = erc20L2StoreService.getActiveAssets(networkTicker = "CRYPTO_NATIVE").last()
        assertEquals(data.keys, result)
    }
}
