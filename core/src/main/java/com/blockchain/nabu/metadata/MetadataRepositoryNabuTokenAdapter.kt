package com.blockchain.nabu.metadata

import com.blockchain.exceptions.MetadataNotFoundException
import com.blockchain.nabu.CreateNabuToken
import com.blockchain.nabu.NabuToken
import com.blockchain.nabu.models.responses.tokenresponse.NabuOfflineTokenResponse
import com.blockchain.nabu.models.responses.tokenresponse.mapFromMetadata
import com.blockchain.rx.maybeCache
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single

class MetadataRepositoryNabuTokenAdapter(
    private val accountCredentialsMetadata: AccountCredentialsMetadata,
    private val createNabuToken: CreateNabuToken,
) : NabuToken {

    private fun createMetaData(): Single<CredentialMetadata> = Single.defer {
        createNabuToken.createNabuOfflineToken()
            .flatMap {
                accountCredentialsMetadata.saveAndReturn(it)
            }
    }

    private val defer = Maybe.defer {
        accountCredentialsMetadata.load()
    }.maybeCache()
        .filter { it.isValid() }.switchIfEmpty(createMetaData())
        .map { metadata ->
            if (!metadata.isValid()) {
                throw MetadataNotFoundException("Nabu Token is empty")
            }
            metadata.mapFromMetadata()
        }

    override fun fetchNabuToken(): Single<NabuOfflineTokenResponse> = defer
}
