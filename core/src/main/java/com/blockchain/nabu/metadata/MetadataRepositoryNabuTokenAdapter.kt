package com.blockchain.nabu.metadata

import com.blockchain.exceptions.MetadataNotFoundException
import com.blockchain.nabu.CreateNabuToken
import com.blockchain.nabu.NabuToken
import com.blockchain.nabu.models.responses.tokenresponse.NabuOfflineTokenResponse
import com.blockchain.nabu.models.responses.tokenresponse.mapFromMetadata
import com.blockchain.rx.maybeCache
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import kotlinx.serialization.InternalSerializationApi

@OptIn(InternalSerializationApi::class)
class MetadataRepositoryNabuTokenAdapter(
    private val accountCredentialsMetadata: AccountCredentialsMetadata,
    private val createNabuToken: CreateNabuToken,
) : NabuToken {

    private fun createMetaData(currency: String?, action: String?): Single<CredentialMetadata> = Single.defer {
        createNabuToken.createNabuOfflineToken(currency, action)
            .flatMap {
                accountCredentialsMetadata.saveAndReturn(it)
            }
    }

    private val defer = Maybe.defer {
        accountCredentialsMetadata.load()
    }.maybeCache()
        .filter { it.isValid() }

    override fun fetchNabuToken(currency: String?, action: String?): Single<NabuOfflineTokenResponse> =
        defer
            .switchIfEmpty(createMetaData(currency, action))
            .map { metadata ->
                if (!metadata.isValid()) {
                    throw MetadataNotFoundException("Nabu Token is empty")
                }
                metadata.mapFromMetadata()
            }
}
