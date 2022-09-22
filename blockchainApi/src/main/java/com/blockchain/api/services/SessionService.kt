package com.blockchain.api.services

import com.blockchain.api.session.SessionApi
import com.blockchain.api.session.data.GenerateSessionResponse
import com.blockchain.outcome.Outcome

class SessionService internal constructor(
    private val api: SessionApi
) {
    suspend fun getSessionId(): Outcome<Exception, GenerateSessionResponse> =
        api.getSessionId()
}
