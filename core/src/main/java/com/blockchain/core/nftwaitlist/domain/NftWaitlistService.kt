package com.blockchain.core.nftwaitlist.domain

import com.blockchain.api.adapters.ApiException
import com.blockchain.outcome.Outcome

interface NftWaitlistService {
    suspend fun joinWaitlist(): Outcome<ApiException, Unit>
}
