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
    val createdAt: String
)

@Serializable
data class PriceDto(
    val symbol: String,
    val value: String,
)

@Serializable
class CardCreationRequestBodyDto(
    val productCode: String,
    val ssn: String
)

@Serializable
class CardWidgetTokenDto(
    val token: String
)

@Serializable
class CardAccountDto(
    val balance: PriceDto
)

@Serializable
class EligibleAccountsDto(
    val accounts: List<CardAccountDto>
)

@Serializable
class LinkedAccountsDto(
    val accounts: List<CardAccountDto>
)

@Serializable
class CardAccountLinkDto(
    val accountCurrency: String
)

@Serializable
class ResidentialAddressRequestDto(
    val userId: String,
    val address: ResidentialAddressDto
)

@Serializable
class ResidentialAddressUpdateDto(
    val address: ResidentialAddressDto
)

@Serializable
class ResidentialAddressDto(
    val line1: String,
    val line2: String = "",
    val postCode: String,
    val city: String,
    val state: String,
    val country: String
)

@Serializable
class BlockchainCardTransactionDto(
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
    val networkConversionRate: Int?,
    val declineReason: String?,
    val fee: PriceDto,
)
