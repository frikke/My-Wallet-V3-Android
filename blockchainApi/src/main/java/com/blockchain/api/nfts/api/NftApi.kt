package com.blockchain.api.nfts.api

import com.blockchain.api.nfts.data.NftAssetsDto
import io.reactivex.rxjava3.core.Single
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface NftApi {
    @GET("/nft-market-api/nft/account_assets/{address}")
    fun getAssetsForAddress(
        @Path("address") ownerAddress: String,
        @Query("cursor") pageKey: String? = null
    ): Single<NftAssetsDto>
}
