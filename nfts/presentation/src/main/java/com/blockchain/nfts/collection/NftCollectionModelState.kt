package com.blockchain.nfts.collection

import com.blockchain.coincore.BlockchainAccount
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.data.DataResource
import com.blockchain.nfts.domain.models.NftAsset

data class NftCollectionModelState(
    val account: BlockchainAccount? = null,
    val isPullToRefreshLoading: Boolean = false,
    val isNextPageLoading: Boolean = false,
    val allPreviousPagesData: List<NftAsset> = emptyList(),
    val nextPageKey: String? = null,
    val collection: DataResource<List<NftAsset>> = DataResource.Loading,
    val displayType: DisplayType = DisplayType.Grid
) : ModelState
