package com.blockchain.nfts.collection

import androidx.lifecycle.viewModelScope
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.CryptoAsset
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.map
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
            onIntent(NftCollectionIntent.LoadData(isFromPullToRefresh = false))
        }
    }

    override fun reduce(state: NftCollectionModelState): NftCollectionViewState = state.run {
        NftCollectionViewState(
            isPullToRefreshLoading = isPullToRefreshLoading,
            showNextPageLoading = isNextPageLoading,
            collection = collection.map { it.distinct() }
        )
    }

    override suspend fun handleIntent(modelState: NftCollectionModelState, intent: NftCollectionIntent) {
        when (intent) {
            is NftCollectionIntent.LoadData -> {
                check(modelState.account != null) { "account not initialized" }
                loadNftCollection(
                    account = modelState.account,
                    pageKey = null,
                    isFromPullToRefresh = intent.isFromPullToRefresh
                )
            }

            is NftCollectionIntent.LoadNextPage -> {
                check(modelState.account != null) { "account not initialized" }
                loadNftCollection(
                    account = modelState.account,
                    pageKey = modelState.nextPageKey,
                    isFromPullToRefresh = false
                )
            }

            NftCollectionIntent.ExternalShop -> {
                navigate(NftCollectionNavigationEvent.ShopExternal(OPENSEA_URL))
            }

            NftCollectionIntent.ShowReceiveAddress -> {
                check(modelState.account != null) { "account not initialized" }
                navigate(NftCollectionNavigationEvent.ShowReceiveAddress(modelState.account))
            }

            NftCollectionIntent.ShowHelp -> {
                navigate(NftCollectionNavigationEvent.ShowHelp)
            }

            is NftCollectionIntent.ShowDetail -> {
                check(modelState.account != null) { "account not initialized" }

                viewModelScope.launch {
                    val address = modelState.account.receiveAddress.await().address
                    navigate(NftCollectionNavigationEvent.ShowDetail(nftId = intent.nftId, address = address))
                }
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

    private fun loadNftCollection(account: BlockchainAccount, pageKey: String?, isFromPullToRefresh: Boolean) {
        viewModelScope.launch {
            val address = account.receiveAddress.await().address
            nftService.getNftCollectionForAddress(
                freshnessStrategy = if (isFromPullToRefresh) {
                    FreshnessStrategy.Fresh
                } else {
                    FreshnessStrategy.Cached(forceRefresh = true)
                },
                address = address,
                pageKey = pageKey
            ).collectLatest { dataResource ->
                when (dataResource) {
                    is DataResource.Loading -> {
                        updateState {
                            it.copy(
                                isPullToRefreshLoading = isFromPullToRefresh,
                                isNextPageLoading = it.nextPageKey != null,
                                collection = if (it.collection is DataResource.Data) {
                                    // if data is present already - don't show loading
                                    it.collection
                                } else {
                                    dataResource
                                }
                            )
                        }
                    }

                    is DataResource.Error -> {
                        updateState {
                            it.copy(
                                isPullToRefreshLoading = false,
                                isNextPageLoading = false,
                                collection = dataResource // error or old data if available
                            )
                        }
                    }

                    is DataResource.Data -> {
                        updateState {
                            val allPreviousPagesData = if (isFromPullToRefresh) emptyList() else it.allPreviousPagesData

                            it.copy(
                                isPullToRefreshLoading = false,
                                isNextPageLoading = false,
                                nextPageKey = dataResource.data.nextPageKey,
                                allPreviousPagesData = allPreviousPagesData + dataResource.data.assets,
                                // combine current page and new page items
                                collection = dataResource.map { data -> allPreviousPagesData + data.assets }
                            )
                        }
                    }
                }
            }
        }
    }
}
