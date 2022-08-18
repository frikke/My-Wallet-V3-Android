package com.blockchain.api.nfts.api

import com.blockchain.api.nfts.data.NftAssetRequestBody
import com.blockchain.api.nfts.data.NftAssetsResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Path

interface NftApi {
    @GET("/nft-market-api/nft/account_assets/{address}")
    suspend fun getAssetsForAddress(
        @Path("address") ownerAddress: String,
      //  @Body requestBody: NftAssetRequestBody
    ): NftAssetsResponse
}
