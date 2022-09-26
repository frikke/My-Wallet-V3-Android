package com.blockchain.nfts.collection

import androidx.lifecycle.viewModelScope
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.CryptoAsset
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.data.DataResource
import com.blockchain.nfts.OPENSEA_URL
import com.blockchain.nfts.collection.navigation.NftCollectionNavigationEvent
import com.blockchain.nfts.domain.service.NftService
import info.blockchain.balance.CryptoCurrency
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.await
import kotlinx.coroutines.rx3.awaitSingle

class NftCollectionViewModel(
    private val coincore: Coincore,
    private val nftService: NftService
) : MviViewModel<NftCollectionIntent,
    NftCollectionViewState,
    NftCollectionModelState,
    NftCollectionNavigationEvent,
    ModelConfigArgs.NoArgs>(
    initialState = NftCollectionModelState()
) {
    override fun viewCreated(args: ModelConfigArgs.NoArgs) {
        viewModelScope.launch {
            loadAccount()
            onIntent(NftCollectionIntent.LoadData)
        }
    }

    override fun reduce(state: NftCollectionModelState): NftCollectionViewState = state.run {
        NftCollectionViewState(
            collection = collection
        )
    }

    override suspend fun handleIntent(modelState: NftCollectionModelState, intent: NftCollectionIntent) {
        when (intent) {
            NftCollectionIntent.LoadData -> {
                check(modelState.account != null) { "account not initialized" }
                loadNftCollection(modelState.account)
            }

            NftCollectionIntent.ExternalShop -> {
                navigate(NftCollectionNavigationEvent.ShopExternal(OPENSEA_URL))
            }

            NftCollectionIntent.ShowReceiveAddress -> {
                check(modelState.account != null) { "account not initialized" }
                navigate(NftCollectionNavigationEvent.ShowReceiveAddress(modelState.account))
            }

            is NftCollectionIntent.ShowDetail -> {
                navigate(NftCollectionNavigationEvent.ShowDetail(intent.nftId))
            }
        }
    }

    private suspend fun loadAccount() {
        (coincore[CryptoCurrency.ETHER.networkTicker] as? CryptoAsset)?.let { asset ->
            asset.accountGroup(AssetFilter.NonCustodial).awaitSingle()
                .accounts.firstOrNull()?.let { account ->
                    updateState {
                        it.copy(account = account)
                    }
                } ?: error("account ${CryptoCurrency.ETHER.networkTicker} not found")
        } ?: error("asset ${CryptoCurrency.ETHER.networkTicker} not found")
    }

    private fun loadNftCollection(account: BlockchainAccount) {
        viewModelScope.launch {
            val address = account.receiveAddress.await().address
            nftService.getNftCollectionForAddress(
                address = address
            ).collectLatest { dataResource ->
                updateState {
                    it.copy(
                        collection = if (dataResource is DataResource.Loading && it.collection is DataResource.Data) {
                            // if data is present already - don't show loading
                            it.collection
                        } else {
                            dataResource
                        }
                    )
                }
            }
        }
    }
}
