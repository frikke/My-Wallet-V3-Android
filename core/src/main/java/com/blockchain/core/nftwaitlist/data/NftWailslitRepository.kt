package com.blockchain.core.nftwaitlist.data

import com.blockchain.api.adapters.ApiError
import com.blockchain.api.nftwaitlist.data.model.NftWaitlistDto
import com.blockchain.api.services.NftWaitlistApiService
import com.blockchain.core.nftwaitlist.domain.NftWaitlistService
import com.blockchain.nabu.UserIdentity
import com.blockchain.outcome.Outcome
import kotlinx.coroutines.rx3.await

class NftWailslitRepository(
    private val nftWaitlistApiService: NftWaitlistApiService,
    private val userIdentity: UserIdentity,
) : NftWaitlistService {
    override suspend fun joinWaitlist(): Outcome<ApiError, Unit> {
        val body = NftWaitlistDto.build(
            email = userIdentity.getBasicProfileInformation().await().email
        )

        return nftWaitlistApiService.joinNftWaitlist(
            nftWaitlistDto = body
        )
    }
}