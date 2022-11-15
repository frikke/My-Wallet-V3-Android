package com.blockchain.domain.session

import io.reactivex.rxjava3.core.Single

interface SessionIdService {
    fun sessionId(): Single<String>
}
