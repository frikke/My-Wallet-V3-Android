package com.blockchain.core.nftwaitlist.domain

import com.blockchain.api.adapters.ApiError
import com.blockchain.outcome.Outcome

interface NftWaitlistService {
    suspend fun joinWaitlist(): Outcome<ApiError, Unit>
}