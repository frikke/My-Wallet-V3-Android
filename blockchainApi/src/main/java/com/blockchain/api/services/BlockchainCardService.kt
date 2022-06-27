package com.blockchain.api.services

import com.blockchain.api.adapters.ApiError
import com.blockchain.api.blockchainCard.BlockchainCardApi
import com.blockchain.api.blockchainCard.WalletHelperUrl
import com.blockchain.api.blockchainCard.data.BlockchainCardTransactionDto
import com.blockchain.api.blockchainCard.data.CardAccountDto
import com.blockchain.api.blockchainCard.data.CardAccountLinkDto
import com.blockchain.api.blockchainCard.data.CardCreationRequestBodyDto
import com.blockchain.api.blockchainCard.data.CardDto
import com.blockchain.api.blockchainCard.data.CardWidgetTokenDto
import com.blockchain.api.blockchainCard.data.ProductDto
import com.blockchain.api.blockchainCard.data.ResidentialAddressDto
import com.blockchain.api.blockchainCard.data.ResidentialAddressRequestDto
import com.blockchain.api.blockchainCard.data.ResidentialAddressUpdateDto
import com.blockchain.outcome.Outcome

class BlockchainCardService internal constructor(
    private val api: BlockchainCardApi,
    private val walletHelperUrl: WalletHelperUrl
) {
    suspend fun getProducts(authHeader: String): Outcome<ApiError, List<ProductDto>> =
        api.getProducts(authHeader)

    suspend fun getCards(authHeader: String): Outcome<ApiError, List<CardDto>> =
        api.getCards(authHeader)

    suspend fun createCard(
        authHeader: String,
        productCode: String,
        ssn: String
    ): Outcome<ApiError, CardDto> = api.createCard(
        authorization = authHeader,
        cardCreationRequest = CardCreationRequestBodyDto(
            productCode = productCode,
            ssn = ssn
        )
    )

    suspend fun deleteCard(
        authHeader: String,
        cardId: String
    ): Outcome<ApiError, CardDto> = api.deleteCard(
        authorization = authHeader,
        cardId = cardId
    )

    suspend fun getCardWidgetToken(
        authHeader: String,
        cardId: String
    ): Outcome<ApiError, CardWidgetTokenDto> = api.getCardWidgetToken(
        authorization = authHeader,
        cardId = cardId
    )

    fun getCardWidgetUrl(
        widgetToken: String,
        last4Digits: String
    ): Outcome<ApiError, String> = Outcome.Success(buildCardWidgetUrl(widgetToken, last4Digits))

    private fun buildCardWidgetUrl(
        widgetToken: String,
        last4Digits: String
    ): String = "${walletHelperUrl.url}wallet-helper/marqeta-card/#/$widgetToken/$last4Digits"

    suspend fun getEligibleAccounts(
        authHeader: String,
        cardId: String
    ): Outcome<ApiError, List<CardAccountDto>> = api.getEligibleAccounts(
        authorization = authHeader,
        cardId = cardId
    )

    suspend fun linkCardAccount(
        authHeader: String,
        cardId: String,
        accountCurrency: String
    ): Outcome<ApiError, CardAccountLinkDto> = api.linkCardAccount(
        authorization = authHeader,
        cardId = cardId,
        cardAccountLinkDto = CardAccountLinkDto(
            accountCurrency = accountCurrency
        )
    )

    suspend fun getCardLinkedAccount(
        authHeader: String,
        cardId: String
    ): Outcome<ApiError, CardAccountLinkDto> = api.getCardLinkedAccount(
        authorization = authHeader,
        cardId = cardId
    )

    suspend fun lockCard(
        authHeader: String,
        cardId: String
    ): Outcome<ApiError, CardDto> = api.lockCard(
        authorization = authHeader,
        cardId = cardId
    )

    suspend fun unlockCard(
        authHeader: String,
        cardId: String
    ): Outcome<ApiError, CardDto> = api.unlockCard(
        authorization = authHeader,
        cardId = cardId
    )

    suspend fun getResidentialAddress(
        authHeader: String,
    ): Outcome<ApiError, ResidentialAddressRequestDto> = api.getResidentialAddress(
        authorization = authHeader
    )

    suspend fun updateResidentialAddress(
        authHeader: String,
        residentialAddress: ResidentialAddressDto
    ): Outcome<ApiError, ResidentialAddressRequestDto> = api.updateResidentialAddress(
        authorization = authHeader,
        residentialAddress = ResidentialAddressUpdateDto(address = residentialAddress)
    )

    suspend fun getTransactions(
        authHeader: String,
        cardId: String? = null,
        types: List<String>? = null,
        from: String? = null,
        to: String? = null,
        toId: String? = null,
        fromId: String? = null,
        limit: Int? = null,
    ): Outcome<ApiError, List<BlockchainCardTransactionDto>> = api.getTransactions(
        authorization = authHeader,
        cardId = cardId,
        types = types,
        from = from,
        to = to,
        toId = toId,
        fromId = fromId,
        limit = limit
    )
}
