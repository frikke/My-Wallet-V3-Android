package com.blockchain.nfts.detail

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.blockchain.data.DataResource
import com.blockchain.nfts.NFT_NETWORK
import com.blockchain.nfts.OPENSEA_ASSET_URL
import com.blockchain.nfts.OPENSEA_URL
import com.blockchain.nfts.collection.NftCollectionIntent
import com.blockchain.nfts.collection.navigation.NftCollectionNavigationEvent
import com.blockchain.nfts.detail.navigation.NftDetailNavigationEvent
import com.blockchain.nfts.domain.models.NftAsset
import com.blockchain.nfts.domain.models.NftContract
import com.blockchain.nfts.domain.service.NftService
import com.blockchain.testutils.CoroutineTestRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class NftDetailViewModelTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    @ExperimentalCoroutinesApi
    @get:Rule
    var coroutineTestRule = CoroutineTestRule()

    private val nftService = mockk<NftService>()

    private lateinit var viewModel: NftDetailViewModel

    private val asset = mockk<NftAsset>()
    private val contract = mockk<NftContract>()
    private val tokenId = "tokenId"
    private val address = "address"

    private val nftDetailNavArgs = NftDetailNavArgs(
        nftId  = "nftId",
        address = "address"
    )

    @Before
    fun setUp() {
        viewModel = NftDetailViewModel(nftService)

        every { asset.contract } returns contract
        every { asset.tokenId } returns tokenId
        every { contract.address } returns address
    }

    @Test
    fun `GIVEN success, WHEN viewCreated is called, THEN nftAsset should be returned`() = runTest {
        val dataResource = MutableSharedFlow<DataResource<NftAsset>>()

        coEvery { nftService.getNftAsset(any(), any(), any()) } returns dataResource

        viewModel.viewState.test {
            viewModel.viewCreated(nftDetailNavArgs)

            // first loading - should be loading
            dataResource.emit(DataResource.Loading)
            awaitItem().run {
                assertEquals(DataResource.Loading, nftAsset)
            }

            // data - should be data
            dataResource.emit(DataResource.Data(asset))
            awaitItem().run {
                assertEquals(DataResource.Data(asset), nftAsset)
            }

            // following loading - should be data
            dataResource.emit(DataResource.Loading)
            // trying to emit viewState with same data will not emit anything since the new object equals the old
            expectNoEvents()
        }
    }

    @Test
    fun `WHEN ExternalViewRequested is called, THEN ExternalView nav should be called`() = runTest {
        val url = String.format(OPENSEA_ASSET_URL, NFT_NETWORK, address, tokenId)

        viewModel.navigationEventFlow.test {
            viewModel.onIntent(NftDetailIntent.ExternalViewRequested(asset))
            awaitItem().run {
                assertEquals(NftDetailNavigationEvent.ExternalView(url), this)
            }
        }
    }
}
