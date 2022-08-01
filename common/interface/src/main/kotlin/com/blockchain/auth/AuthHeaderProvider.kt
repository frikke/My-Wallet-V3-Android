package com.blockchain.auth

import io.reactivex.rxjava3.core.Single

// FLAG_AUTH_REMOVAL
interface AuthHeaderProvider {
    @Deprecated(message = "Use Authenticator.authenticate {} instead, this doesn't refresh the token in case of 401")
    fun getAuthHeader(): Single<String>
}
