package com.blockchain.nabu.models.responses.nabu

import kotlinx.serialization.Serializable

@Serializable
internal data class SupportedDocumentsResponse(
    val countryCode: String,
    val documentTypes: List<SupportedDocuments>
)

@Serializable
enum class SupportedDocuments {
    PASSPORT,
    DRIVING_LICENCE,
    NATIONAL_IDENTITY_CARD,
    RESIDENCE_PERMIT
}
