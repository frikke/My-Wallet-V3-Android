package com.blockchain.api.nfts.api

import com.blockchain.api.nfts.data.NftAssetRequestBody
import com.blockchain.api.nfts.data.NftAssetResponse
import retrofit2.http.Body
import retrofit2.http.GET

interface NftApi {
    @GET("/currency/evm/nft/balance")
    suspend fun getAssetsForAddress(
        @Body requestBody: NftAssetRequestBody
    ): NftAssetResponse
}
