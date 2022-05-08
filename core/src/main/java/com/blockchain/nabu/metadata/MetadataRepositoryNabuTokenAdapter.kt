package com.blockchain.nabu.metadata

import com.blockchain.exceptions.MetadataNotFoundException
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.metadata.MetadataRepository
import com.blockchain.metadata.load
import com.blockchain.metadata.save
import com.blockchain.nabu.CreateNabuToken
import com.blockchain.nabu.NabuToken
import com.blockchain.nabu.models.responses.tokenresponse.NabuOfflineTokenResponse
import com.blockchain.nabu.models.responses.tokenresponse.mapFromMetadata
import com.blockchain.nabu.models.responses.tokenresponse.mapToNabuAccountMetadata
import com.blockchain.nabu.models.responses.tokenresponse.mapToNabuUserMetadata
import com.blockchain.rx.maybeCache
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.zipWith
import kotlinx.serialization.InternalSerializationApi

@OptIn(InternalSerializationApi::class)
class MetadataRepositoryNabuTokenAdapter(
    private val metadataRepository: MetadataRepository,
    private val createNabuToken: CreateNabuToken,
    private val accountMetadataMigrationFF: FeatureFlag
) : NabuToken {

    private fun createMetaData(currency: String?, action: String?): Maybe<CredentialMetadata> = Maybe.defer {
        createNabuToken.createNabuOfflineToken(currency, action)
            .zipWith(accountMetadataMigrationFF.enabled)
            .map { (tokenResponse, enabled) ->
                if (enabled) {
                    tokenResponse.mapToNabuAccountMetadata()
                } else {
                    tokenResponse.mapToNabuUserMetadata()
                }
            }
            .flatMapMaybe { credentialsMetadata ->
                when (credentialsMetadata) {
                    is NabuAccountCredentialsMetadata -> {
                        metadataRepository.save(
                            credentialsMetadata,
                            NabuAccountCredentialsMetadata.ACCOUNT_CREDENTIALS_METADATA_NODE
                        ).andThen(Maybe.just(credentialsMetadata))
                    }
                    is NabuUserCredentialsMetadata -> {
                        metadataRepository.save(
                            credentialsMetadata,
                            NabuUserCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
                        ).andThen(Maybe.just(credentialsMetadata))
                    }
                }
            }
    }

    private val defer = Maybe.defer {
        metadataRepository.load<NabuAccountCredentialsMetadata>(
            NabuAccountCredentialsMetadata.ACCOUNT_CREDENTIALS_METADATA_NODE,
        ).switchIfEmpty(
            Maybe.just(NabuAccountCredentialsMetadata())
        ).flatMap {
            if (it.isValid()) {
                Maybe.just(it)
            } else {
                metadataRepository.load<NabuUserCredentialsMetadata>(
                    NabuUserCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
                )
            }
        }.onErrorResumeNext {
            metadataRepository.load<NabuUserCredentialsMetadata>(
                NabuUserCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
            )
        }
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
            .toSingle()
}
