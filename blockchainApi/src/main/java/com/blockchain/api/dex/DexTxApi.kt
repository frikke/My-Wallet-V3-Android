package com.blockchain.api.dex

import com.blockchain.outcome.Outcome
import retrofit2.http.Body
import retrofit2.http.POST

interface DexTxApi {
    @POST("currency/evm/allowance")
    suspend fun allowance(
        @Body request: AllowanceBodyRequest
    ): Outcome<Exception, TokenAllowanceResponse>
}
