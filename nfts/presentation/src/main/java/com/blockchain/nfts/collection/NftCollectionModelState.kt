package com.blockchain.nfts.collection

import com.blockchain.coincore.BlockchainAccount
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.data.DataResource
import com.blockchain.nfts.domain.models.NftAsset

data class NftCollectionModelState(
    val account: BlockchainAccount? = null,
    val isRefreshing: Boolean = false,
    val collection: DataResource<List<NftAsset>> = DataResource.Loading
) : ModelState
