package com.blockchain.api.services

import com.blockchain.api.nfts.api.NftApi
import com.blockchain.api.nfts.data.NftAssetsDto
import io.reactivex.rxjava3.core.Single

class NftApiService internal constructor(
    private val nftApi: NftApi
) {
    fun getNftCollection(
        address: String,
        pageKey: String? = null
    ): Single<NftAssetsDto> =
        nftApi.getAssetsForAddress(
            ownerAddress = address,
            pageKey = pageKey
        )
}
