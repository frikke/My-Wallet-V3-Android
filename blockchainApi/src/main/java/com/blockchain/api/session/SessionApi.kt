package com.blockchain.api.session

import com.blockchain.api.session.data.GenerateSessionResponse
import com.blockchain.outcome.Outcome
import retrofit2.http.PUT

internal interface SessionApi {
    @PUT("generate-session")
    suspend fun getSessionId(): Outcome<Exception, GenerateSessionResponse>
}
