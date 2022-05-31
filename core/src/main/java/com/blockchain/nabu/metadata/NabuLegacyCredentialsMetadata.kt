package com.blockchain.nabu.metadata

import com.blockchain.serialization.JsonSerializable
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Back in version 202205.1.0 we had a feature flag for disabling moshi in favour of kotlinx.
 * The below objects though were created only to support kotlinx serialisation and de-serialisation.
 * This caused those objects to be saved in the Metadata with the wrong fields
 * when moshi serialization was used, so instead of saving `user_id`,
 * 'userId' was saved etc as moshi couldn't find the right serialisation field.
 * In order to fix that we have to add those customs serializers that have the logic of map both the right and
 * the corrupted fields. Also, a boolean to specify if the object is corrupted or not is also defined, in order to
 * know if we need to re-sync the corresponding metadata payloads or not.
 * TL;DR We should never remove this logic and the custom serializers for Metadata entries 10 and 14.
 *
 */

@Serializable(with = RootNabuLegacyAccountSerializer::class)
data class NabuLegacyCredentialsMetadata(
    override val userId: String,
    override val lifetimeToken: String,
    @Transient
    val isCorrupted: Boolean = false
) : CredentialMetadata {

    companion object {
        fun invalid(): NabuLegacyCredentialsMetadata = NabuLegacyCredentialsMetadata("", "")
    }
}

@Serializable(with = RootBlockchainAccountCredentialsMetadataSerializer::class)
data class BlockchainAccountCredentialsMetadata(
    override val userId: String? = null,
    override val lifetimeToken: String? = null,
    val exchangeUserId: String? = null,
    val exchangeLifetimeToken: String? = null,
    @Transient
    val isCorrupted: Boolean = false
) : CredentialMetadata {

    companion object {

        fun invalid(): BlockchainAccountCredentialsMetadata = BlockchainAccountCredentialsMetadata()
    }
}

sealed interface CredentialMetadata : JsonSerializable {
    val userId: String?
    val lifetimeToken: String?

    fun isValid() = userId.isNullOrEmpty().not() && lifetimeToken.isNullOrEmpty().not()
}
