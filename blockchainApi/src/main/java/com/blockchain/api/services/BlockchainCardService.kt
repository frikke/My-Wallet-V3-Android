package com.blockchain.api.services

import com.blockchain.api.blockchainCard.BlockchainCardApi
import com.blockchain.api.blockchainCard.WalletHelperUrl
import com.blockchain.api.blockchainCard.data.BlockchainCardAcceptedDocsFormDto
import com.blockchain.api.blockchainCard.data.BlockchainCardActivationUrlResponseDto
import com.blockchain.api.blockchainCard.data.BlockchainCardGoogleWalletProvisionRequestDto
import com.blockchain.api.blockchainCard.data.BlockchainCardGoogleWalletProvisionResponseDto
import com.blockchain.api.blockchainCard.data.BlockchainCardKycStatusDto
import com.blockchain.api.blockchainCard.data.BlockchainCardKycUpdateRequestDto
import com.blockchain.api.blockchainCard.data.BlockchainCardLegalDocumentDto
import com.blockchain.api.blockchainCard.data.BlockchainCardOrderStateResponseDto
import com.blockchain.api.blockchainCard.data.BlockchainCardSetPinURLResponseDto
import com.blockchain.api.blockchainCard.data.BlockchainCardStatementUrlResponseDto
import com.blockchain.api.blockchainCard.data.BlockchainCardStatementsResponseDto
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
    suspend fun getProducts(): Outcome<Exception, List<ProductDto>> =
        api.getProducts()

    suspend fun getCards(): Outcome<Exception, List<CardDto>> =
        api.getCards()

    suspend fun createCard(
        productCode: String,
        shippingAddress: ResidentialAddressDto?
    ): Outcome<Exception, CardDto> = api.createCard(
        cardCreationRequest = CardCreationRequestBodyDto(
            productCode = productCode,
            shippingAddress = shippingAddress
        )
    )

    suspend fun getCard(
        cardId: String
    ): Outcome<Exception, CardDto> = api.getCard(
        cardId = cardId
    )

    suspend fun deleteCard(
        cardId: String
    ): Outcome<Exception, CardDto> = api.deleteCard(
        cardId = cardId
    )

    suspend fun getCardWidgetToken(
        cardId: String
    ): Outcome<Exception, CardWidgetTokenDto> = api.getCardWidgetToken(
        cardId = cardId
    )

    fun getCardWidgetUrl(
        widgetToken: String,
        last4Digits: String,
        userFullName: String,
        cardType: String
    ): Outcome<Exception, String> = Outcome.Success(
        buildCardWidgetUrl(widgetToken, last4Digits, userFullName, cardType)
    )

    private fun buildCardWidgetUrl(
        widgetToken: String,
        last4Digits: String,
        userFullName: String,
        cardType: String
    ): String =
        "${walletHelperUrl.url}wallet-helper/marqeta-card/#/" +
            "?token=$widgetToken" +
            "&last4=$last4Digits" +
            "&fullName=$userFullName" +
            "&cardType=$cardType"

    suspend fun getEligibleAccounts(
        cardId: String
    ): Outcome<Exception, List<CardAccountDto>> = api.getEligibleAccounts(
        cardId = cardId
    )

    suspend fun linkCardAccount(
        cardId: String,
        accountCurrency: String
    ): Outcome<Exception, CardAccountLinkDto> = api.linkCardAccount(
        cardId = cardId,
        cardAccountLinkDto = CardAccountLinkDto(
            accountCurrency = accountCurrency
        )
    )

    suspend fun getCardLinkedAccount(
        cardId: String
    ): Outcome<Exception, CardAccountLinkDto> = api.getCardLinkedAccount(
        cardId = cardId
    )

    suspend fun lockCard(
        cardId: String
    ): Outcome<Exception, CardDto> = api.lockCard(
        cardId = cardId
    )

    suspend fun unlockCard(
        cardId: String
    ): Outcome<Exception, CardDto> = api.unlockCard(
        cardId = cardId
    )

    suspend fun getResidentialAddress(): Outcome<Exception, ResidentialAddressRequestDto> = api.getResidentialAddress()

    suspend fun updateResidentialAddress(
        residentialAddress: ResidentialAddressDto
    ): Outcome<Exception, ResidentialAddressRequestDto> = api.updateResidentialAddress(
        residentialAddress = ResidentialAddressUpdateDto(address = residentialAddress)
    )

    suspend fun getTransactions(
        cardId: String? = null,
        types: List<String>? = null,
        from: String? = null,
        to: String? = null,
        toId: String? = null,
        fromId: String? = null,
        limit: Int? = null,
    ): Outcome<Exception, List<BlockchainCardTransactionDto>> = api.getTransactions(
        cardId = cardId,
        types = types,
        from = from,
        to = to,
        toId = toId,
        fromId = fromId,
        limit = limit
    )

    suspend fun getLegalDocuments(): Outcome<Exception, List<BlockchainCardLegalDocumentDto>> = api.getLegalDocuments()

    suspend fun acceptLegalDocuments(
        acceptedDocumentsForm: BlockchainCardAcceptedDocsFormDto
    ): Outcome<Exception, List<BlockchainCardLegalDocumentDto>> = api.acceptLegalDocuments(
        acceptedDocumentsForm = acceptedDocumentsForm
    )

    suspend fun provisionGoogleWalletCard(
        cardId: String,
        provisionRequest: BlockchainCardGoogleWalletProvisionRequestDto
    ): Outcome<Exception, BlockchainCardGoogleWalletProvisionResponseDto> = api.provisionGoogleWalletCard(
        cardId = cardId,
        provisionRequest = provisionRequest
    )

    suspend fun getCardOrderState(
        cardId: String
    ): Outcome<Exception, BlockchainCardOrderStateResponseDto> = api.getCardOrderState(
        cardId = cardId
    )

    suspend fun getCardActivationUrl(): Outcome<Exception, BlockchainCardActivationUrlResponseDto> =
        api.getCardActivationUrl()

    suspend fun getCardStatements(): Outcome<Exception, List<BlockchainCardStatementsResponseDto>> =
        api.getCardStatements()

    suspend fun getCardStatementUrl(statementId: String): Outcome<Exception, BlockchainCardStatementUrlResponseDto> =
        api.getCardStatementUrl(statementId = statementId)

    suspend fun getKycStatus(): Outcome<Exception, BlockchainCardKycStatusDto> =
        api.getKycStatus()

    suspend fun updateKycStatus(
        kycUpdateRequest: BlockchainCardKycUpdateRequestDto
    ): Outcome<Exception, BlockchainCardKycStatusDto> = api.updateKyc(
        kycUpdateRequest = kycUpdateRequest
    )

    suspend fun getSetPinUrl(
        cardId: String
    ): Outcome<Exception, BlockchainCardSetPinURLResponseDto> = api.getSetPinUrl(
        cardId = cardId
    )
}
