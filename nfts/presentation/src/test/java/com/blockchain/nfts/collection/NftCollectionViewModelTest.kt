package com.blockchain.nfts.collection

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.blockchain.coincore.AccountGroup
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.CryptoAsset
import com.blockchain.coincore.OneTimeAccountPersistenceService
import com.blockchain.coincore.ReceiveAddress
import com.blockchain.data.DataResource
import com.blockchain.data.map
import com.blockchain.nfts.OPENSEA_URL
import com.blockchain.nfts.collection.navigation.NftCollectionNavigationEvent
import com.blockchain.nfts.domain.models.NftAsset
import com.blockchain.nfts.domain.models.NftAssetsPage
import com.blockchain.nfts.domain.service.NftService
import com.blockchain.testutils.CoroutineTestRule
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class NftCollectionViewModelTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @ExperimentalCoroutinesApi
    @get:Rule
    var coroutineTestRule = CoroutineTestRule()

    private val coincore = mockk<Coincore>()
    private val nftService = mockk<NftService>()
    private val oneTimeAccountPersistenceService = mockk<OneTimeAccountPersistenceService>()

    private lateinit var viewModel: NftCollectionViewModel

    private val asset = mockk<CryptoAsset>()
    private val accountGroup = mockk<AccountGroup>()
    private val singleAccount = mockk<CryptoAccount>()
    private val receiveAddress = mockk<ReceiveAddress>()
    private val address = "address"

    private val nftAsset = mockk<NftAsset>()
    private val nextPageKey = "nextPageKey"
    private val nftAssetsPage = mockk<NftAssetsPage>()

    @Before
    fun setUp() {
        viewModel = NftCollectionViewModel(coincore, nftService, oneTimeAccountPersistenceService)

        every { coincore[any<String>()] } returns asset
        every { oneTimeAccountPersistenceService.saveAccount(any()) } just Runs
        every { asset.accountGroup(any()) } returns Maybe.just(accountGroup)
        every { accountGroup.accounts } returns listOf(singleAccount)
        every { singleAccount.receiveAddress } returns Single.just(receiveAddress)
        every { receiveAddress.address } returns address
        every { nftAssetsPage.assets } returns listOf(nftAsset)
        every { nftAssetsPage.nextPageKey } returns nextPageKey
    }

    @Test
    fun `GIVEN success, WHEN viewCreated is called, THEN collection should be returned`() = runTest {
        val dataResource = MutableSharedFlow<DataResource<NftAssetsPage>>()

        coEvery { nftService.getNftCollectionForAddress(any(), any(), any()) } returns dataResource

        viewModel.onIntent(NftCollectionIntent.LoadData())

        viewModel.viewState.test {
            // first loading - should be loading
            dataResource.emit(DataResource.Loading)
            awaitItem().run {
                assertEquals(DataResource.Loading, collection)
            }

            // data - should be data
            dataResource.emit(DataResource.Data(nftAssetsPage))
            awaitItem().run {
                assertEquals(DataResource.Data(nftAssetsPage).map { it.assets }, collection)
            }

            // following loading - should be data
            dataResource.emit(DataResource.Loading)
            // trying to emit viewState with same data will not emit anything since the new object equals the old
            awaitItem().run {
                assertEquals(DataResource.Data(nftAssetsPage).map { it.assets }, collection)
            }
        }
    }

    @Test
    fun `WHEN ExternalShop is called, THEN ShopExternal nav should be called`() = runTest {
        viewModel.navigationEventFlow.test {
            viewModel.onIntent(NftCollectionIntent.ExternalShop)
            awaitItem().run {
                assertEquals(NftCollectionNavigationEvent.ShopExternal(OPENSEA_URL), this)
            }
        }
    }

    @Test
    fun `WHEN ShowReceiveAddress is called, THEN ShowReceiveAddress nav should be called`() = runTest {
        viewModel.onIntent(NftCollectionIntent.LoadData())

        viewModel.navigationEventFlow.test {
            viewModel.onIntent(NftCollectionIntent.ShowReceiveAddress)
            awaitItem().run {
                assertEquals(NftCollectionNavigationEvent.ShowReceiveAddress, this)
            }
        }
    }

    @Test
    fun `WHEN ShowHelp is called, THEN ShowHelp nav should be called`() = runTest {
        viewModel.navigationEventFlow.test {
            viewModel.onIntent(NftCollectionIntent.ShowHelp)
            awaitItem().run {
                assertEquals(NftCollectionNavigationEvent.ShowHelp, this)
            }
        }
    }

    @Test
    fun `WHEN ShowDetail is called, THEN ShowDetail nav should be called`() = runTest {
        val nftId = "nftId"
        viewModel.onIntent(NftCollectionIntent.LoadData())

        viewModel.navigationEventFlow.test {
            viewModel.onIntent(NftCollectionIntent.ShowDetail(nftId, nextPageKey))
            awaitItem().run {
                assertEquals(NftCollectionNavigationEvent.ShowDetail(nftId, nextPageKey, address), this)
            }
        }
    }
}
