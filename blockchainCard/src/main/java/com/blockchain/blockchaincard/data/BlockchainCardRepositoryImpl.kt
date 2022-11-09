package com.blockchain.blockchaincard.data

import com.blockchain.api.NabuApiException
import com.blockchain.api.blockchainCard.data.BlockchainCardAcceptedDocsFormDto
import com.blockchain.api.blockchainCard.data.BlockchainCardAcceptedDocumentDto
import com.blockchain.api.blockchainCard.data.BlockchainCardGoogleWalletProvisionRequestDto
import com.blockchain.api.blockchainCard.data.BlockchainCardGoogleWalletProvisionResponseDto
import com.blockchain.api.blockchainCard.data.BlockchainCardGoogleWalletUserAddressDto
import com.blockchain.api.blockchainCard.data.BlockchainCardKycStatusDto
import com.blockchain.api.blockchainCard.data.BlockchainCardKycUpdateRequestDto
import com.blockchain.api.blockchainCard.data.BlockchainCardLegalDocumentDto
import com.blockchain.api.blockchainCard.data.BlockchainCardOrderStateResponseDto
import com.blockchain.api.blockchainCard.data.BlockchainCardStatementsResponseDto
import com.blockchain.api.blockchainCard.data.BlockchainCardTransactionDto
import com.blockchain.api.blockchainCard.data.BlockchainCardWebViewPostMessage
import com.blockchain.api.blockchainCard.data.CardDto
import com.blockchain.api.blockchainCard.data.ProductDto
import com.blockchain.api.blockchainCard.data.ResidentialAddressDto
import com.blockchain.api.eligibility.data.StateResponse
import com.blockchain.api.services.BlockchainCardService
import com.blockchain.api.services.EligibilityApiService
import com.blockchain.blockchaincard.domain.BlockchainCardRepository
import com.blockchain.blockchaincard.domain.models.BlockchainCard
import com.blockchain.blockchaincard.domain.models.BlockchainCardAddress
import com.blockchain.blockchaincard.domain.models.BlockchainCardAddressType
import com.blockchain.blockchaincard.domain.models.BlockchainCardBrand
import com.blockchain.blockchaincard.domain.models.BlockchainCardError
import com.blockchain.blockchaincard.domain.models.BlockchainCardGoogleWalletData
import com.blockchain.blockchaincard.domain.models.BlockchainCardGoogleWalletPushTokenizeData
import com.blockchain.blockchaincard.domain.models.BlockchainCardGoogleWalletUserAddress
import com.blockchain.blockchaincard.domain.models.BlockchainCardKycErrorField
import com.blockchain.blockchaincard.domain.models.BlockchainCardKycState
import com.blockchain.blockchaincard.domain.models.BlockchainCardKycStatus
import com.blockchain.blockchaincard.domain.models.BlockchainCardKycUpdate
import com.blockchain.blockchaincard.domain.models.BlockchainCardLegalDocument
import com.blockchain.blockchaincard.domain.models.BlockchainCardOrderState
import com.blockchain.blockchaincard.domain.models.BlockchainCardOrderStatus
import com.blockchain.blockchaincard.domain.models.BlockchainCardPostMessageType
import com.blockchain.blockchaincard.domain.models.BlockchainCardProduct
import com.blockchain.blockchaincard.domain.models.BlockchainCardStatement
import com.blockchain.blockchaincard.domain.models.BlockchainCardStatus
import com.blockchain.blockchaincard.domain.models.BlockchainCardTransaction
import com.blockchain.blockchaincard.domain.models.BlockchainCardTransactionState
import com.blockchain.blockchaincard.domain.models.BlockchainCardTransactionType
import com.blockchain.blockchaincard.domain.models.BlockchainCardType
import com.blockchain.blockchaincard.googlewallet.manager.GoogleWalletManager
import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.TradingAccount
import com.blockchain.coincore.fiat.FiatCustodialAccount
import com.blockchain.coincore.impl.CustodialTradingAccount
import com.blockchain.core.eligibility.mapper.toDomain
import com.blockchain.domain.eligibility.model.Region
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.nabu.UserIdentity
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.flatMap
import com.blockchain.outcome.map
import com.blockchain.outcome.mapError
import com.blockchain.preferences.BlockchainCardPrefs
import com.blockchain.utils.awaitOutcome
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.FiatValue
import java.math.BigDecimal
import kotlinx.coroutines.rx3.rxSingle
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private const val DEFAULT_CARD_ID = "DEFAULT_CARD_ID"

internal class BlockchainCardRepositoryImpl(
    private val blockchainCardService: BlockchainCardService,
    private val eligibilityApiService: EligibilityApiService,
    private val coincore: Coincore,
    private val assetCatalogue: AssetCatalogue,
    private val userIdentity: UserIdentity,
    private val googleWalletManager: GoogleWalletManager,
    private val blockchainCardPrefs: BlockchainCardPrefs,
    private val googleWalletFeatureFlag: FeatureFlag,
) : BlockchainCardRepository {

    override suspend fun getProducts(): Outcome<BlockchainCardError, List<BlockchainCardProduct>> =
        blockchainCardService.getProducts()
            .map { productList ->
                productList.map { product ->
                    product.toDomainModel()
                }
            }.wrapBlockchainCardError()

    override suspend fun getCards(): Outcome<BlockchainCardError, List<BlockchainCard>> =
        blockchainCardService.getCards()
            .map { response ->
                response.map {
                    it.toDomainModel()
                }
            }.wrapBlockchainCardError()

    override suspend fun createCard(
        productCode: String,
        shippingAddress: BlockchainCardAddress?
    ): Outcome<BlockchainCardError, BlockchainCard> =
        blockchainCardService.createCard(
            productCode = productCode,
            shippingAddress = shippingAddress?.toDto()
        ).map { card ->
            card.toDomainModel()
        }.wrapBlockchainCardError()

    override suspend fun getCard(cardId: String): Outcome<BlockchainCardError, BlockchainCard> =
        blockchainCardService.getCard(
            cardId = cardId
        ).map { card ->
            card.toDomainModel()
        }.wrapBlockchainCardError()

    override suspend fun deleteCard(cardId: String): Outcome<BlockchainCardError, BlockchainCard> =
        blockchainCardService.deleteCard(
            cardId = cardId
        ).map { card ->
            card.toDomainModel()
        }.wrapBlockchainCardError()

    override suspend fun lockCard(cardId: String): Outcome<BlockchainCardError, BlockchainCard> =
        blockchainCardService.lockCard(
            cardId = cardId
        ).map { card ->
            card.toDomainModel()
        }.wrapBlockchainCardError()

    override suspend fun unlockCard(cardId: String): Outcome<BlockchainCardError, BlockchainCard> =
        blockchainCardService.unlockCard(
            cardId = cardId
        ).map { card ->
            card.toDomainModel()
        }.wrapBlockchainCardError()

    override suspend fun getCardWidgetUrl(
        cardId: String,
        last4Digits: String,
        userFullName: String
    ): Outcome<BlockchainCardError, String> =
        blockchainCardService.getCardWidgetToken(
            cardId = cardId,
        ).flatMap { widgetToken ->
            blockchainCardService.getCardWidgetUrl(widgetToken.token, last4Digits, userFullName)
        }.wrapBlockchainCardError()

    override suspend fun getEligibleTradingAccounts(
        cardId: String
    ): Outcome<BlockchainCardError, List<TradingAccount>> =
        blockchainCardService.getEligibleAccounts(
            cardId = cardId
        ).flatMap { eligibleAccountsList ->
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
        blockchainCardService.linkCardAccount(
            cardId = cardId,
            accountCurrency = accountCurrency
        ).map { cardAccountLinkResponse ->
            cardAccountLinkResponse.accountCurrency
        }.wrapBlockchainCardError()

    override suspend fun getCardLinkedAccount(
        cardId: String
    ): Outcome<BlockchainCardError, TradingAccount> =
        blockchainCardService.getCardLinkedAccount(
            cardId = cardId
        ).flatMap { cardLinkedAccountResponse ->
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
        tradingAccount.balanceRx.firstOrError().awaitOutcome().wrapBlockchainCardError()

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
        blockchainCardService.getResidentialAddress().map { response ->
            response.address.toDomainModel()
        }.wrapBlockchainCardError()

    override suspend fun updateResidentialAddress(
        address: BlockchainCardAddress
    ): Outcome<BlockchainCardError, BlockchainCardAddress> =
        blockchainCardService.updateResidentialAddress(
            residentialAddress = address.toDto()
        ).map { response ->
            response.address.toDomainModel()
        }.wrapBlockchainCardError()

    override suspend fun getUserFirstAndLastName(): Outcome<BlockchainCardError, String> =
        userIdentity.getBasicProfileInformation().awaitOutcome()
            .map { response ->
                response.firstName + " " + response.lastName
            }.wrapBlockchainCardError()

    override suspend fun getTransactions(
        limit: Int?,
        toId: String?
    ): Outcome<BlockchainCardError, List<BlockchainCardTransaction>> =
        blockchainCardService.getTransactions(limit = limit, toId = toId).map { response ->
            response.map { it.toDomainModel() }
        }.wrapBlockchainCardError()

    override suspend fun getStatesList(countryCode: String): Outcome<BlockchainCardError, List<Region.State>> =
        eligibilityApiService.getStatesList(countryCode)
            .map { states -> states.map(StateResponse::toDomain) }
            .wrapBlockchainCardError()

    override suspend fun getLegalDocuments(): Outcome<BlockchainCardError, List<BlockchainCardLegalDocument>> =
        blockchainCardService.getLegalDocuments().map { response ->
            response.map { it.toDomainModel() }
        }.wrapBlockchainCardError()

    override suspend fun acceptLegalDocuments(
        acceptedLegalDocuments: List<BlockchainCardLegalDocument>
    ): Outcome<BlockchainCardError, List<BlockchainCardLegalDocument>> =
        blockchainCardService.acceptLegalDocuments(
            acceptedDocumentsForm = acceptedLegalDocuments.toAcceptedLegalDocForm()
        ).map { response ->
            response.map { it.toDomainModel() }
        }.wrapBlockchainCardError()

    override suspend fun provisionGoogleWalletCard(
        cardId: String,
        provisionRequest: BlockchainCardGoogleWalletData
    ): Outcome<BlockchainCardError, BlockchainCardGoogleWalletPushTokenizeData> =
        blockchainCardService.provisionGoogleWalletCard(
            cardId = cardId,
            provisionRequest = provisionRequest.toDto()
        ).map { response ->
            response.toDomainModel()
        }.wrapBlockchainCardError()

    override suspend fun getGoogleWalletId(): Outcome<BlockchainCardError, String> =
        rxSingle {
            googleWalletManager.getWalletId()
        }.awaitOutcome().wrapBlockchainCardError()

    override suspend fun getGoogleWalletStableHardwareId(): Outcome<BlockchainCardError, String> =
        rxSingle {
            googleWalletManager.getStableHardwareId()
        }.awaitOutcome().wrapBlockchainCardError()

    override suspend fun getGoogleWalletTokenizationStatus(last4Digits: String): Outcome<BlockchainCardError, Boolean> =
        if (googleWalletFeatureFlag.coEnabled()) {
            rxSingle {
                googleWalletManager.getTokenizationStatus(last4Digits)
            }.awaitOutcome().wrapBlockchainCardError()
        } else {
            Outcome.Success(true)
        }

    override fun getDefaultCard(): String =
        blockchainCardPrefs.defaultCardId

    override fun saveCardAsDefault(cardId: String) {
        blockchainCardPrefs.defaultCardId = cardId
    }

    override suspend fun getCardOrderState(cardId: String): Outcome<BlockchainCardError, BlockchainCardOrderState> =
        blockchainCardService.getCardOrderState(cardId).map { response ->
            response.toDomainModel()
        }.wrapBlockchainCardError()

    override suspend fun getCardActivationUrl(): Outcome<BlockchainCardError, String> =
        blockchainCardService.getCardActivationUrl().map {
            it.url
        }.wrapBlockchainCardError()

    override suspend fun getCardStatements(): Outcome<BlockchainCardError, List<BlockchainCardStatement>> =
        blockchainCardService.getCardStatements().map { response ->
            response.map {
                it.toDomainModel()
            }
        }.wrapBlockchainCardError()

    override suspend fun getCardStatementUrl(statementId: String): Outcome<BlockchainCardError, String> =
        blockchainCardService.getCardStatementUrl(statementId).map { response ->
            response.url
        }.wrapBlockchainCardError()

    override suspend fun decodePostMessageType(
        postMessage: String
    ): Outcome<BlockchainCardError, BlockchainCardPostMessageType> {
        return try {
            val message = Json.decodeFromString<BlockchainCardWebViewPostMessage>(postMessage)
            Outcome.Success(BlockchainCardPostMessageType.valueOf(message.type))
        } catch (exception: Exception) {
            Outcome.Failure(BlockchainCardError.LocalCopyBlockchainCardError)
        }
    }

    override suspend fun getKycStatus(): Outcome<BlockchainCardError, BlockchainCardKycStatus> =
        blockchainCardService.getKycStatus().map { response ->
            response.toDomainModel()
        }.wrapBlockchainCardError()

    override suspend fun updateKyc(
        kycUpdate: BlockchainCardKycUpdate
    ): Outcome<BlockchainCardError, BlockchainCardKycStatus> =
        blockchainCardService.updateKycStatus(
            kycUpdate.toDto()
        ).map { response ->
            response.toDomainModel()
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
            orderStatus = orderStatus?.let { BlockchainCardOrderStatus.valueOf(it) },
            createdAt = createdAt
        )

    private fun ResidentialAddressDto.toDomainModel(): BlockchainCardAddress =
        BlockchainCardAddress(
            line1 = line1,
            line2 = line2,
            postCode = postCode,
            city = city,
            state = state,
            country = country,
            addressType = BlockchainCardAddressType.BILLING
        )

    private fun BlockchainCardAddress.toDto(): ResidentialAddressDto =
        ResidentialAddressDto(
            line1 = this.line1,
            line2 = this.line2,
            postCode = this.postCode,
            city = this.city,
            state = this.state,
            country = this.country
        )

    private fun BlockchainCardTransactionDto.toDomainModel(): BlockchainCardTransaction =
        BlockchainCardTransaction(
            id = id,
            cardId = cardId,
            type = BlockchainCardTransactionType.valueOf(type),
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
            required = required,
            seen = false,
        )

    private fun List<BlockchainCardLegalDocument>.toAcceptedLegalDocForm(): BlockchainCardAcceptedDocsFormDto =
        BlockchainCardAcceptedDocsFormDto(
            legalPolicies = this.map { BlockchainCardAcceptedDocumentDto(it.name, it.version) }
        )

    private fun BlockchainCardGoogleWalletData.toDto(): BlockchainCardGoogleWalletProvisionRequestDto =
        BlockchainCardGoogleWalletProvisionRequestDto(
            deviceId = deviceId,
            deviceType = deviceType,
            provisioningAppVersion = provisioningAppVersion,
            walletAccountId = walletAccountId
        )

    private fun BlockchainCardGoogleWalletProvisionResponseDto.toDomainModel():
        BlockchainCardGoogleWalletPushTokenizeData =
        BlockchainCardGoogleWalletPushTokenizeData(
            cardType = cardType,
            displayName = displayName,
            opaquePaymentCard = opaquePaymentCard,
            last4 = last4,
            network = network,
            tokenServiceProvider = tokenServiceProvider,
            googleWalletUserAddress = userAddress.toDomainModel()
        )

    private fun BlockchainCardGoogleWalletUserAddressDto.toDomainModel(): BlockchainCardGoogleWalletUserAddress =
        BlockchainCardGoogleWalletUserAddress(
            name = name,
            address1 = address1,
            address2 = address2,
            city = city,
            stateCode = stateCode,
            postalCode = postalCode,
            countryCode = countryCode,
            phone = phone
        )

    private fun BlockchainCardOrderStateResponseDto.toDomainModel(): BlockchainCardOrderState =
        BlockchainCardOrderState(
            status = BlockchainCardOrderStatus.valueOf(status),
            address = address?.let {
                BlockchainCardAddress(
                    line1 = it.line1,
                    line2 = it.line2,
                    postCode = it.postCode,
                    city = it.city,
                    state = it.state,
                    country = it.country,
                    addressType = BlockchainCardAddressType.SHIPPING
                )
            }
        )

    private fun BlockchainCardStatementsResponseDto.toDomainModel(): BlockchainCardStatement =
        BlockchainCardStatement(
            id = statementId,
            date = "$month/$year"
        )

    private fun BlockchainCardKycStatusDto.toDomainModel(): BlockchainCardKycStatus =
        BlockchainCardKycStatus(
            state = BlockchainCardKycState.valueOf(status),
            errorFields = errorFields?.map { BlockchainCardKycErrorField.valueOf(it) }
        )

    private fun BlockchainCardKycUpdate.toDto(): BlockchainCardKycUpdateRequestDto =
        BlockchainCardKycUpdateRequestDto(
            address = address?.toDto(),
            ssn = ssn
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
