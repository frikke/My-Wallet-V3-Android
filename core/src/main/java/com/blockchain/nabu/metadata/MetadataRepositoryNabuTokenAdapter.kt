package com.blockchain.nabu.metadata

import com.blockchain.exceptions.MetadataNotFoundException
import com.blockchain.nabu.CreateNabuToken
import com.blockchain.nabu.NabuToken
import com.blockchain.nabu.models.responses.tokenresponse.NabuOfflineToken
import com.blockchain.nabu.models.responses.tokenresponse.toNabuOfflineToken
import com.blockchain.rx.maybeCache
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.androidcore.utils.extensions.thenSingle

class MetadataRepositoryNabuTokenAdapter(
    private val accountCredentialsMetadata: AccountCredentialsMetadata,
    private val createNabuToken: CreateNabuToken,
) : NabuToken {

    private fun createUserAndMetaData(): Single<NabuOfflineToken> = Single.defer {
        createNabuToken.createNabuOfflineToken()
            .flatMap { tokenResponse ->
                val nabuToken = tokenResponse.toNabuOfflineToken()
                if (tokenResponse.created) {
                    accountCredentialsMetadata.save(nabuToken).thenSingle {
                        Single.just(nabuToken)
                    }
                } else {
                    Single.just(nabuToken)
                }
            }
    }

    private val defer = Maybe.defer {
        accountCredentialsMetadata.load()
    }.maybeCache()
        .filter { it.isValid() }.map { metadata -> metadata.toNabuOfflineToken() }
        .switchIfEmpty(createUserAndMetaData())

    override fun fetchNabuToken(): Single<NabuOfflineToken> = defer.map {
        if (!it.isValid())
            throw MetadataNotFoundException("Nabu Token is empty")
        it
    }
}
