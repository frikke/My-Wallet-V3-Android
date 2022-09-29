package com.blockchain.api.blockchainCard.data

import kotlinx.serialization.Serializable

@Serializable
data class ProductDto(
    val productCode: String,
    val price: PriceDto,
    val brand: String,
    val type: String
)

@Serializable
data class CardDto(
    val id: String,
    val type: String,
    val last4: String,
    val expiry: String,
    val brand: String,
    val status: String,
    val orderStatus: String?,
    val createdAt: String
)

@Serializable
data class PriceDto(
    val symbol: String,
    val value: String,
)

@Serializable
data class CardCreationRequestBodyDto(
    val productCode: String,
    val ssn: String
)

@Serializable
data class CardWidgetTokenDto(
    val token: String
)

@Serializable
data class CardAccountDto(
    val balance: PriceDto
)

@Serializable
data class CardAccountLinkDto(
    val accountCurrency: String
)

@Serializable
data class ResidentialAddressRequestDto(
    val userId: String,
    val address: ResidentialAddressDto
)

@Serializable
data class ResidentialAddressUpdateDto(
    val address: ResidentialAddressDto
)

@Serializable
data class ResidentialAddressDto(
    val line1: String,
    val line2: String = "",
    val postCode: String,
    val city: String,
    val state: String,
    val country: String
)

@Serializable
data class BlockchainCardTransactionDto(
    val id: String,
    val cardId: String,
    val type: String,
    val state: String,
    val originalAmount: PriceDto,
    val fundingAmount: PriceDto,
    val reversedAmount: PriceDto,
    val counterAmount: PriceDto?,
    val clearedFundingAmount: PriceDto,
    val userTransactionTime: String,
    val merchantName: String,
    val networkConversionRate: Float?,
    val declineReason: String?,
    val fee: PriceDto,
)

@Serializable
data class BlockchainCardLegalDocumentDto(
    val name: String,
    val displayName: String,
    val url: String,
    val version: String,
    val acceptedVersion: String?,
    val required: Boolean
)

@Serializable
data class BlockchainCardAcceptedDocsFormDto(
    val legalPolicies: List<BlockchainCardAcceptedDocumentDto>
)

@Serializable
data class BlockchainCardAcceptedDocumentDto(
    val name: String,
    val acceptedVersion: String
)
