package com.blockchain.api.services

import com.blockchain.api.auth.AuthApiInterface
import com.blockchain.api.auth.data.SendEmailRequest
import com.blockchain.domain.session.SessionIdService
import com.blockchain.utils.withBearerPrefix
import io.reactivex.rxjava3.core.Completable

class AuthApiService internal constructor(
    private val api: AuthApiInterface,
    private val apiCode: String,
    private val sessionIdService: SessionIdService,
    private val captchaSiteKey: String
) {
    fun sendEmailForAuthentication(email: String, captcha: String): Completable {
        return sessionIdService.sessionId().flatMapCompletable {
            api.sendEmailForAuthentication(
                it.withBearerPrefix(),
                SendEmailRequest(
                    apiCode,
                    email,
                    captcha,
                    PRODUCT_WALLET,
                    captchaSiteKey
                )
            )
        }
    }

    companion object {
        private const val PRODUCT_WALLET = "WALLET"
    }
}
