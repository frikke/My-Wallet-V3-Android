package com.blockchain.api.services

import com.blockchain.api.adapters.ApiError
import com.blockchain.api.blockchainCard.BlockchainCardApi
import com.blockchain.api.blockchainCard.WalletHelperUrl
import com.blockchain.api.blockchainCard.data.CardAccount
import com.blockchain.api.blockchainCard.data.CardAccountLinkRequest
import com.blockchain.api.blockchainCard.data.CardAccountLinkResponse
import com.blockchain.api.blockchainCard.data.CardCreationRequestBody
import com.blockchain.api.blockchainCard.data.CardLinkedAccountResponse
import com.blockchain.api.blockchainCard.data.CardWidgetTokenResponse
import com.blockchain.api.blockchainCard.data.CardsResponse
import com.blockchain.api.blockchainCard.data.ProductsResponse
import com.blockchain.outcome.Outcome

class BlockchainCardService internal constructor(
    private val api: BlockchainCardApi,
    private val walletHelperUrl: WalletHelperUrl
) {
    suspend fun getProducts(authHeader: String): Outcome<ApiError, List<ProductsResponse>> =
        api.getProducts(authHeader)

    suspend fun getCards(authHeader: String): Outcome<ApiError, List<CardsResponse>> =
        api.getCards(authHeader)

    suspend fun createCard(
        authHeader: String,
        productCode: String,
        ssn: String
    ): Outcome<ApiError, CardsResponse> = api.createCard(
        authorization = authHeader,
        cardCreationRequest = CardCreationRequestBody(
            productCode = productCode,
            ssn = ssn
        )
    )

    suspend fun deleteCard(
        authHeader: String,
        cardId: String
    ): Outcome<ApiError, CardsResponse> = api.deleteCard(
        authorization = authHeader,
        cardId = cardId
    )

    suspend fun getCardWidgetToken(
        authHeader: String,
        cardId: String
    ): Outcome<ApiError, CardWidgetTokenResponse> = api.getCardWidgetToken(
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
    ): Outcome<ApiError, List<CardAccount>> = api.getEligibleAccounts(
        authorization = authHeader,
        cardId = cardId
    )

    suspend fun linkCardAccount(
        authHeader: String,
        cardId: String,
        accountCurrency: String
    ): Outcome<ApiError, CardAccountLinkResponse> = api.linkCardAccount(
        authorization = authHeader,
        cardId = cardId,
        cardAccountLinkRequest = CardAccountLinkRequest(
            accountCurrency = accountCurrency
        )
    )

    suspend fun getCardLinkedAccount(
        authHeader: String,
        cardId: String
    ): Outcome<ApiError, CardLinkedAccountResponse> = api.getCardLinkedAccount(
        authorization = authHeader,
        cardId = cardId
    )

    suspend fun lockCard(
        authHeader: String,
        cardId: String
    ): Outcome<ApiError, CardsResponse> = api.lockCard(
        authorization = authHeader,
        cardId = cardId
    )

    suspend fun unlockCard(
        authHeader: String,
        cardId: String
    ): Outcome<ApiError, CardsResponse> = api.unlockCard(
        authorization = authHeader,
        cardId = cardId
    )
}
