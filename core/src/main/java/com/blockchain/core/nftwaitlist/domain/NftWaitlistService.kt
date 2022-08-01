package com.blockchain.core.nftwaitlist.domain

import com.blockchain.outcome.Outcome

interface NftWaitlistService {
    suspend fun joinWaitlist(): Outcome<Exception, Unit>
}
