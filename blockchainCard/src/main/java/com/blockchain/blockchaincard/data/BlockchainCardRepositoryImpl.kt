package com.blockchain.blockchaincard.data

import com.blockchain.api.NabuApiException
import com.blockchain.api.blockchainCard.data.BlockchainCardAcceptedDocsFormDto
import com.blockchain.api.blockchainCard.data.BlockchainCardAcceptedDocumentDto
import com.blockchain.api.blockchainCard.data.BlockchainCardLegalDocumentDto
import com.blockchain.api.blockchainCard.data.BlockchainCardTransactionDto
import com.blockchain.api.blockchainCard.data.CardDto
import com.blockchain.api.blockchainCard.data.ProductDto
import com.blockchain.api.blockchainCard.data.ResidentialAddressDto
import com.blockchain.api.eligibility.data.StateResponse
import com.blockchain.api.services.BlockchainCardService
import com.blockchain.api.services.EligibilityApiService
import com.blockchain.blockchaincard.domain.BlockchainCardRepository
import com.blockchain.blockchaincard.domain.models.BlockchainCard
import com.blockchain.blockchaincard.domain.models.BlockchainCardAddress
import com.blockchain.blockchaincard.domain.models.BlockchainCardBrand
import com.blockchain.blockchaincard.domain.models.BlockchainCardError
import com.blockchain.blockchaincard.domain.models.BlockchainCardLegalDocument
import com.blockchain.blockchaincard.domain.models.BlockchainCardProduct
import com.blockchain.blockchaincard.domain.models.BlockchainCardStatus
import com.blockchain.blockchaincard.domain.models.BlockchainCardTransaction
import com.blockchain.blockchaincard.domain.models.BlockchainCardTransactionState
import com.blockchain.blockchaincard.domain.models.BlockchainCardType
import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.TradingAccount
import com.blockchain.coincore.fiat.FiatCustodialAccount
import com.blockchain.coincore.impl.CustodialTradingAccount
import com.blockchain.core.eligibility.mapper.toDomain
import com.blockchain.domain.eligibility.model.Region
import com.blockchain.nabu.Authenticator
import com.blockchain.nabu.UserIdentity
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.flatMap
import com.blockchain.outcome.map
import com.blockchain.outcome.mapError
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.FiatValue
import java.math.BigDecimal
import piuk.blockchain.androidcore.utils.extensions.awaitOutcome

internal class BlockchainCardRepositoryImpl(
    private val blockchainCardService: BlockchainCardService,
    private val eligibilityApiService: EligibilityApiService,
    private val authenticator: Authenticator,
    private val coincore: Coincore,
    private val assetCatalogue: AssetCatalogue,
    private val userIdentity: UserIdentity
) : BlockchainCardRepository {

    override suspend fun getProducts(): Outcome<BlockchainCardError, List<BlockchainCardProduct>> =
        authenticator.getAuthHeader().awaitOutcome()
            .flatMap { tokenResponse ->
                blockchainCardService.getProducts(
                    tokenResponse
                )
            }.map { productList ->
                productList.map { product ->
                    product.toDomainModel()
                }
            }.wrapBlockchainCardError()

    override suspend fun getCards(): Outcome<BlockchainCardError, List<BlockchainCard>> =
        authenticator.getAuthHeader().awaitOutcome()
            .flatMap { tokenResponse ->
                blockchainCardService.getCards(
                    tokenResponse
                )
            }.map { response ->
                response.map {
                    it.toDomainModel()
                }
            }.wrapBlockchainCardError()

    override suspend fun createCard(
        productCode: String,
        ssn: String
    ): Outcome<BlockchainCardError, BlockchainCard> =
        authenticator.getAuthHeader().awaitOutcome()
            .flatMap { tokenResponse ->
                blockchainCardService.createCard(
                    authHeader = tokenResponse,
                    productCode = productCode,
                    ssn = ssn
                )
            }.map { card ->
                card.toDomainModel()
            }.wrapBlockchainCardError()

    override suspend fun deleteCard(cardId: String): Outcome<BlockchainCardError, BlockchainCard> =
        authenticator.getAuthHeader().awaitOutcome()
            .flatMap { tokenResponse ->
                blockchainCardService.deleteCard(
                    authHeader = tokenResponse,
                    cardId = cardId
                )
            }.map { card ->
                card.toDomainModel()
            }.wrapBlockchainCardError()

    override suspend fun lockCard(cardId: String): Outcome<BlockchainCardError, BlockchainCard> =
        authenticator.getAuthHeader().awaitOutcome()
            .flatMap { tokenResponse ->
                blockchainCardService.lockCard(
                    authHeader = tokenResponse,
                    cardId = cardId
                )
            }.map { card ->
                card.toDomainModel()
            }.wrapBlockchainCardError()

    override suspend fun unlockCard(cardId: String): Outcome<BlockchainCardError, BlockchainCard> =
        authenticator.getAuthHeader().awaitOutcome()
            .flatMap { tokenResponse ->
                blockchainCardService.unlockCard(
                    authHeader = tokenResponse,
                    cardId = cardId
                )
            }.map { card ->
                card.toDomainModel()
            }.wrapBlockchainCardError()

    override suspend fun getCardWidgetUrl(
        cardId: String,
        last4Digits: String,
        userFullName: String
    ): Outcome<BlockchainCardError, String> =
        authenticator.getAuthHeader().awaitOutcome()
            .flatMap { tokenResponse ->
                blockchainCardService.getCardWidgetToken(
                    authHeader = tokenResponse,
                    cardId = cardId,
                )
            }.flatMap { widgetToken ->
                blockchainCardService.getCardWidgetUrl(widgetToken.token, last4Digits, userFullName)
            }.wrapBlockchainCardError()

    override suspend fun getEligibleTradingAccounts(
        cardId: String
    ): Outcome<BlockchainCardError, List<TradingAccount>> =
        authenticator.getAuthHeader().awaitOutcome()
            .flatMap { tokenResponse ->
                blockchainCardService.getEligibleAccounts(
                    authHeader = tokenResponse,
                    cardId = cardId
                )
            }.flatMap { eligibleAccountsList ->
                val eligibleCurrencies = eligibleAccountsList.map { cardAccount ->
                    cardAccount.balance.symbol
                }

                coincore.allWallets().awaitOutcome().map { accountGroup ->
                    accountGroup.accounts.filterIsInstance<TradingAccount>().filter { tradingAccount ->
                        eligibleCurrencies.any {
                            when (tradingAccount) {
                                is FiatCustodialAccount ->
                                    tradingAccount.currency.networkTicker == it
                                is CustodialTradingAccount ->
                                    tradingAccount.currency.networkTicker == it
                                else ->
                                    throw IllegalStateException(
                                        "Account is not a FiatCustodialAccount nor CustodialTradingAccount"
                                    )
                            }
                        }
                    }
                }
            }.wrapBlockchainCardError()

    override suspend fun linkCardAccount(
        cardId: String,
        accountCurrency: String
    ): Outcome<BlockchainCardError, String> =
        authenticator.getAuthHeader().awaitOutcome()
            .flatMap { tokenResponse ->
                blockchainCardService.linkCardAccount(
                    authHeader = tokenResponse,
                    cardId = cardId,
                    accountCurrency = accountCurrency
                )
            }.map { cardAccountLinkResponse ->
                cardAccountLinkResponse.accountCurrency
            }.wrapBlockchainCardError()

    override suspend fun getCardLinkedAccount(
        cardId: String
    ): Outcome<BlockchainCardError, TradingAccount> =
        authenticator.getAuthHeader().awaitOutcome()
            .flatMap { tokenResponse ->
                blockchainCardService.getCardLinkedAccount(
                    authHeader = tokenResponse,
                    cardId = cardId
                )
            }.flatMap { cardLinkedAccountResponse ->
                coincore.allWallets().awaitOutcome()
                    .map { accountGroup ->
                        accountGroup.accounts.filterIsInstance<TradingAccount>().first { tradingAccount ->
                            when (tradingAccount) {
                                is FiatCustodialAccount ->
                                    tradingAccount
                                        .currency.networkTicker == cardLinkedAccountResponse.accountCurrency
                                is CustodialTradingAccount ->
                                    tradingAccount
                                        .currency.networkTicker == cardLinkedAccountResponse.accountCurrency
                                else ->
                                    throw IllegalStateException(
                                        "Account is not a FiatCustodialAccount nor CustodialTradingAccount"
                                    )
                            }
                        }
                    }.wrapBlockchainCardError()
            }.wrapBlockchainCardError()

    override suspend fun loadAccountBalance(
        tradingAccount: BlockchainAccount
    ): Outcome<BlockchainCardError, AccountBalance> =
        tradingAccount.balance.firstOrError().awaitOutcome().wrapBlockchainCardError()

    override suspend fun getAsset(networkTicker: String): Outcome<BlockchainCardError, AssetInfo> =
        assetCatalogue.assetInfoFromNetworkTicker(networkTicker)?.let { asset ->
            Outcome.Success(asset)
        } ?: Outcome.Failure(BlockchainCardError.LocalCopyBlockchainCardError)

    override suspend fun getFiatAccount(networkTicker: String): Outcome<BlockchainCardError, FiatAccount> =
        coincore.allWallets().awaitOutcome()
            .map { accountGroup ->
                accountGroup.accounts.filterIsInstance<FiatAccount>().first { tradingAccount ->
                    tradingAccount.currency.networkTicker == networkTicker
                }
            }.wrapBlockchainCardError()

    override suspend fun getResidentialAddress(): Outcome<BlockchainCardError, BlockchainCardAddress> =
        authenticator.getAuthHeader().awaitOutcome()
            .flatMap { tokenResponse ->
                blockchainCardService.getResidentialAddress(
                    authHeader = tokenResponse
                )
            }.map { response ->
                response.address.toDomainModel()
            }.wrapBlockchainCardError()

    override suspend fun updateResidentialAddress(
        address: BlockchainCardAddress
    ): Outcome<BlockchainCardError, BlockchainCardAddress> =
        authenticator.getAuthHeader().awaitOutcome()
            .flatMap { tokenResponse ->
                blockchainCardService.updateResidentialAddress(
                    authHeader = tokenResponse,
                    residentialAddress = address.toDto(address)
                )
            }.map { response ->
                response.address.toDomainModel()
            }.wrapBlockchainCardError()

    override suspend fun getUserFirstAndLastName(): Outcome<BlockchainCardError, String> =
        userIdentity.getBasicProfileInformation().awaitOutcome()
            .map { response ->
                response.firstName + " " + response.lastName
            }.wrapBlockchainCardError()

    override suspend fun getTransactions(): Outcome<BlockchainCardError, List<BlockchainCardTransaction>> =
        authenticator.getAuthHeader().awaitOutcome()
            .flatMap { tokenResponse ->
                blockchainCardService.getTransactions(tokenResponse)
            }.map { response ->
                response.map { it.toDomainModel() }
            }.wrapBlockchainCardError()

    override suspend fun getStatesList(countryCode: String): Outcome<BlockchainCardError, List<Region.State>> =
        eligibilityApiService.getStatesList(countryCode)
            .map { states -> states.map(StateResponse::toDomain) }
            .wrapBlockchainCardError()

    override suspend fun getLegalDocuments(): Outcome<BlockchainCardError, List<BlockchainCardLegalDocument>> =
        authenticator.getAuthHeader().awaitOutcome()
            .flatMap { tokenResponse ->
                blockchainCardService.getLegalDocuments(
                    authHeader = tokenResponse
                )
            }.map { response ->
                response.map { it.toDomainModel() }
            }.wrapBlockchainCardError()

    override suspend fun acceptLegalDocuments(
        acceptedLegalDocuments: List<BlockchainCardLegalDocument>
    ): Outcome<BlockchainCardError, List<BlockchainCardLegalDocument>> =
        authenticator.getAuthHeader().awaitOutcome()
            .flatMap { tokenResponse ->
                blockchainCardService.acceptLegalDocuments(
                    authHeader = tokenResponse,
                    acceptedDocumentsForm = acceptedLegalDocuments.toAcceptedLegalDocForm()
                )
            }.map { response ->
                response.map { it.toDomainModel() }
            }.wrapBlockchainCardError()

    //
    // Domain Model Conversion
    //
    private fun ProductDto.toDomainModel(): BlockchainCardProduct =
        BlockchainCardProduct(
            productCode = productCode,
            price = FiatValue.fromMajor(
                fiatCurrency = FiatCurrency.fromCurrencyCode(price.symbol),
                major = BigDecimal(price.value)
            ),
            brand = BlockchainCardBrand.valueOf(brand),
            type = BlockchainCardType.valueOf(type)
        )

    private fun CardDto.toDomainModel(): BlockchainCard =
        BlockchainCard(
            id = id,
            type = BlockchainCardType.valueOf(type),
            last4 = last4,
            expiry = expiry,
            brand = BlockchainCardBrand.valueOf(brand),
            status = BlockchainCardStatus.valueOf(status),
            createdAt = createdAt
        )

    private fun ResidentialAddressDto.toDomainModel(): BlockchainCardAddress =
        BlockchainCardAddress(
            line1 = line1,
            line2 = line2,
            postCode = postCode,
            city = city,
            state = state,
            country = country
        )

    private fun BlockchainCardAddress.toDto(address: BlockchainCardAddress): ResidentialAddressDto =
        ResidentialAddressDto(
            line1 = address.line1,
            line2 = address.line2,
            postCode = address.postCode,
            city = address.city,
            state = address.state,
            country = address.country
        )

    private fun BlockchainCardTransactionDto.toDomainModel(): BlockchainCardTransaction =
        BlockchainCardTransaction(
            id = id,
            cardId = cardId,
            type = type,
            state = BlockchainCardTransactionState.valueOf(state),
            originalAmount = FiatValue.fromMajor(
                fiatCurrency = FiatCurrency.fromCurrencyCode(originalAmount.symbol),
                major = BigDecimal(originalAmount.value)
            ),
            fundingAmount = FiatValue.fromMajor(
                fiatCurrency = FiatCurrency.fromCurrencyCode(fundingAmount.symbol),
                major = BigDecimal(fundingAmount.value)
            ),
            reversedAmount = FiatValue.fromMajor(
                fiatCurrency = FiatCurrency.fromCurrencyCode(reversedAmount.symbol),
                major = BigDecimal(reversedAmount.value)
            ),
            counterAmount = counterAmount?.let {
                FiatValue.fromMajor(
                    fiatCurrency = FiatCurrency.fromCurrencyCode(it.symbol),
                    major = BigDecimal(it.value)
                )
            },
            clearedFundingAmount = FiatValue.fromMajor(
                fiatCurrency = FiatCurrency.fromCurrencyCode(clearedFundingAmount.symbol),
                major = BigDecimal(clearedFundingAmount.value)
            ),
            userTransactionTime = userTransactionTime,
            merchantName = merchantName,
            networkConversionRate = networkConversionRate,
            declineReason = declineReason,
            fee = FiatValue.fromMajor(
                fiatCurrency = FiatCurrency.fromCurrencyCode(fee.symbol),
                major = BigDecimal(fee.value)
            ),
        )

    private fun BlockchainCardLegalDocumentDto.toDomainModel(): BlockchainCardLegalDocument =
        BlockchainCardLegalDocument(
            name = name,
            displayName = displayName,
            url = url,
            version = version,
            acceptedVersion = acceptedVersion,
            seen = false

        )

    private fun List<BlockchainCardLegalDocument>.toAcceptedLegalDocForm(): BlockchainCardAcceptedDocsFormDto =
        BlockchainCardAcceptedDocsFormDto(
            legalPolicies = this.map { BlockchainCardAcceptedDocumentDto(it.name, it.version) }
        )

    private fun NabuApiException.toBlockchainCardError(): BlockchainCardError =
        this.getServerSideErrorInfo()?.let {
            BlockchainCardError.UXBlockchainCardError(it)
        } ?: BlockchainCardError.LocalCopyBlockchainCardError

    private fun <Exception, R> Outcome<Exception, R>.wrapBlockchainCardError(): Outcome<BlockchainCardError, R> =
        mapError {
            if (it is NabuApiException) it.toBlockchainCardError()
            else BlockchainCardError.LocalCopyBlockchainCardError
        }
}
