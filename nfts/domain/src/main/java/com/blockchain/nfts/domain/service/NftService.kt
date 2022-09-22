package com.blockchain.nfts.domain.service

import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.nfts.domain.models.NftAsset
import kotlinx.coroutines.flow.Flow

interface NftService {
    suspend fun getNftCollectionForAddress(
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Fresh/*Cached(forceRefresh = true)*/,
        network: String = "ETH",
        address: String
    ): Flow<DataResource<List<NftAsset>>>

    suspend fun getNftAsset(
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Fresh,
        network: String = "ETH",
        address: String,
        nftId: String
    ): Flow<DataResource<NftAsset?>>
}
