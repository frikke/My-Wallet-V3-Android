package com.blockchain.api.nfts.api

import com.blockchain.api.nfts.data.NftAssetsResponse
import io.reactivex.rxjava3.core.Single
import retrofit2.http.GET
import retrofit2.http.Path

interface NftApi {
    @GET("/nft-market-api/nft/account_assets/{address}")
    fun getAssetsForAddress(
        @Path("address") ownerAddress: String,
        //  @Body requestBody: NftAssetRequestBody
    ): Single<NftAssetsResponse>
}
