package com.blockchain.nabu

import com.blockchain.nabu.models.responses.tokenresponse.NabuOfflineToken
import io.reactivex.rxjava3.core.Single

interface NabuToken {

    /**
     * Find or creates the token
     */
    fun fetchNabuToken(): Single<NabuOfflineToken>
}
