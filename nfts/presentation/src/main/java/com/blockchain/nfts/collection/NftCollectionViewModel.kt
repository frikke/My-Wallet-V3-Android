package com.blockchain.nfts.collection

import androidx.lifecycle.viewModelScope
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.CryptoAsset
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.data.DataResource
import com.blockchain.data.RefreshStrategy
import com.blockchain.data.dataOrElse
import com.blockchain.data.map
import com.blockchain.nfts.OPENSEA_URL
import com.blockchain.nfts.collection.navigation.NftCollectionNavigationEvent
import com.blockchain.nfts.domain.service.NftService
import com.blockchain.outcome.getOrNull
import com.blockchain.presentation.pulltorefresh.PullToRefresh
import com.blockchain.utils.CurrentTimeProvider
import com.blockchain.utils.awaitOutcome
import info.blockchain.balance.CryptoCurrency
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
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
    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}

    override fun reduce(state: NftCollectionModelState): NftCollectionViewState = state.run {
        NftCollectionViewState(
            isPullToRefreshLoading = isPullToRefreshLoading,
            showNextPageLoading = isNextPageLoading,
            collection = collection.map { it.distinct() },
            displayType = displayType
        )
    }

    override suspend fun handleIntent(modelState: NftCollectionModelState, intent: NftCollectionIntent) {
        when (intent) {
            is NftCollectionIntent.LoadData -> {
                val account = loadAccount()

                updateState {
                    it.copy(account = account)
                }

                loadNftCollection(
                    account = account,
                    pageKey = null,
                    forceRefresh = intent.isFromPullToRefresh
                )
            }

            is NftCollectionIntent.ChangeDisplayType -> {
                updateState {
                    it.copy(
                        displayType = intent.displayType
                    )
                }
            }

            is NftCollectionIntent.LoadNextPage -> {
                check(modelState.account != null) { "account not initialized" }
                loadNftCollection(
                    account = modelState.account,
                    pageKey = modelState.nextPageKey,
                    forceRefresh = false
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
                    modelState.account.receiveAddress.awaitOutcome().getOrNull()?.address?.let {
                        navigate(
                            NftCollectionNavigationEvent.ShowDetail(
                                nftId = intent.nftId,
                                pageKey = intent.pageKey,
                                address = it
                            )
                        )
                    }
                }
            }

            NftCollectionIntent.Refresh -> {
                updateState {
                    it.copy(lastFreshDataTime = CurrentTimeProvider.currentTimeMillis())
                }

                check(modelState.account != null)
                loadNftCollection(
                    account = modelState.account,
                    pageKey = null,
                    forceRefresh = true
                )
            }
        }
    }

    private suspend fun loadAccount(): CryptoAccount {
        (coincore[CryptoCurrency.ETHER.networkTicker] as? CryptoAsset)?.let { asset ->
            asset.accountGroup(AssetFilter.NonCustodial).awaitSingle()
                .accounts.firstOrNull()?.let { account ->
                    return account as CryptoAccount
                } ?: error("account ${CryptoCurrency.ETHER.networkTicker} not found")
        } ?: error("asset ${CryptoCurrency.ETHER.networkTicker} not found")
    }

    private fun loadNftCollection(
        account: BlockchainAccount,
        pageKey: String?,
        forceRefresh: Boolean
    ) {
        viewModelScope.launch {
            val address = account.receiveAddress.awaitOutcome().getOrNull()?.address ?: return@launch
            nftService.getNftCollectionForAddress(
                freshnessStrategy = PullToRefresh.freshnessStrategy(
                    shouldGetFresh = forceRefresh,
                    cacheStrategy = RefreshStrategy.RefreshIfStale
                ),
                address = address,
                pageKey = pageKey
            ).collectLatest { dataResource ->
                when (dataResource) {
                    is DataResource.Loading -> {
                        updateState {
                            it.copy(
                                isPullToRefreshLoading = forceRefresh,
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
                            val allPreviousPagesData = if (forceRefresh) emptyList() else it.allPreviousPagesData
                            val allCollection = dataResource.map { data -> allPreviousPagesData + data.assets }

                            it.copy(
                                isPullToRefreshLoading = false,
                                isNextPageLoading = false,
                                nextPageKey = dataResource.data.nextPageKey,
                                allPreviousPagesData = allPreviousPagesData + dataResource.data.assets,
                                // combine current page and new page items
                                collection = allCollection.map { it.toSet().toList() },
                                displayType = allCollection.map { it.size == 1 }
                                    .dataOrElse(false).let { isOneItem ->
                                        if (isOneItem) DisplayType.List
                                        else it.displayType
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}
