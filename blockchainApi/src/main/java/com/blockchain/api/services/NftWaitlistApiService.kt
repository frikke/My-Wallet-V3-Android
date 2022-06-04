package com.blockchain.api.services

import com.blockchain.api.adapters.ApiError
import com.blockchain.api.nftwaitlist.data.api.NftWaitlistApi
import com.blockchain.api.nftwaitlist.data.model.NftWaitlistDto
import com.blockchain.outcome.Outcome

class NftWaitlistApiService internal constructor(
    private val nftWaitlistApi: NftWaitlistApi
) {
    suspend fun subscribeToNftWaitlist(
        nftWaitlistDto: NftWaitlistDto
    ): Outcome<ApiError, Unit> =
        nftWaitlistApi.subscribeToNftWaitlist(
            nftWaitlistDto = nftWaitlistDto
        )
}
