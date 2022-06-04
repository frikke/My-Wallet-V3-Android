package com.blockchain.core.nftwaitlist.data

import com.blockchain.api.adapters.ApiError
import com.blockchain.api.nftwaitlist.data.model.NftWaitlistDto
import com.blockchain.api.services.NftWaitlistApiService
import com.blockchain.core.nftwaitlist.domain.NftWaitlistService
import com.blockchain.nabu.Authenticator
import com.blockchain.outcome.Outcome

class NftWailslitRepository(
    private val nftWaitlistApiService: NftWaitlistApiService
) : NftWaitlistService {
    override suspend fun addToWaitlist(userEmail: String): Outcome<ApiError, Unit> {
        val body = NftWaitlistDto.build(email = userEmail)

        return nftWaitlistApiService.subscribeToNftWaitlist(
            nftWaitlistDto = body
        )
    }
}