package com.blockchain.nfts.domain.service

import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.nfts.domain.models.NftAsset
import com.blockchain.nfts.domain.models.NftAssetsPage
import kotlinx.coroutines.flow.Flow

interface NftService {
    suspend fun getNftCollectionForAddress(
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale),
        address: String,
        pageKey: String?
    ): Flow<DataResource<NftAssetsPage>>

    suspend fun getNftAsset(
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh),
        address: String,
        nftId: String,
        pageKey: String?
    ): Flow<DataResource<NftAsset?>>
}
