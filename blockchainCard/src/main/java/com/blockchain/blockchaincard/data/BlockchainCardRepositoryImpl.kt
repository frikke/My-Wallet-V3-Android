package com.blockchain.blockchaincard.data

import com.blockchain.api.blockchainCard.data.CardDto
import com.blockchain.api.blockchainCard.data.ProductDto
import com.blockchain.api.blockchainCard.data.ResidentialAddressDto
import com.blockchain.api.services.BlockchainCardService
import com.blockchain.blockchaincard.domain.BlockchainCardRepository
import com.blockchain.blockchaincard.domain.models.BlockchainCard
import com.blockchain.blockchaincard.domain.models.BlockchainCardAddress
import com.blockchain.blockchaincard.domain.models.BlockchainCardBrand
import com.blockchain.blockchaincard.domain.models.BlockchainCardError
import com.blockchain.blockchaincard.domain.models.BlockchainCardProduct
import com.blockchain.blockchaincard.domain.models.BlockchainCardStatus
import com.blockchain.blockchaincard.domain.models.BlockchainCardType
import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.TradingAccount
import com.blockchain.coincore.fiat.FiatCustodialAccount
import com.blockchain.coincore.impl.CustodialTradingAccount
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
    private val authenticator: Authenticator,
    private val coincore: Coincore,
    private val assetCatalogue: AssetCatalogue,
    private val userIdentity: UserIdentity
) : BlockchainCardRepository {

    override suspend fun getProducts(): Outcome<BlockchainCardError, List<BlockchainCardProduct>> =
        authenticator.getAuthHeader().awaitOutcome()
            .mapError { BlockchainCardError.GetAuthFailed }
            .flatMap { tokenResponse ->
                blockchainCardService.getProducts(
                    tokenResponse
                ).mapError {
                    BlockchainCardError.GetProductsRequestFailed
                }.map { response ->
                    response.map {
                        it.toDomainModel()
                    }
                }
            }

    override suspend fun getCards(): Outcome<BlockchainCardError, List<BlockchainCard>> =
        authenticator.getAuthHeader().awaitOutcome()
            .mapError { BlockchainCardError.GetAuthFailed }
            .flatMap { tokenResponse ->
                blockchainCardService.getCards(
                    tokenResponse
                ).mapError { BlockchainCardError.GetCardsRequestFailed }.map { response ->
                    response.map {
                        it.toDomainModel()
                    }
                }
            }

    override suspend fun createCard(
        productCode: String,
        ssn: String
    ): Outcome<BlockchainCardError, BlockchainCard> =
        authenticator.getAuthHeader().awaitOutcome()
            .mapError { BlockchainCardError.GetAuthFailed }
            .flatMap { tokenResponse ->
                blockchainCardService.createCard(
                    authHeader = tokenResponse,
                    productCode = productCode,
                    ssn = ssn
                ).mapError { BlockchainCardError.CreateCardRequestFailed }.map { card ->
                    card.toDomainModel()
                }
            }

    override suspend fun deleteCard(cardId: String): Outcome<BlockchainCardError, BlockchainCard> =
        authenticator.getAuthHeader().awaitOutcome()
            .mapError { BlockchainCardError.GetAuthFailed }
            .flatMap { tokenResponse ->
                blockchainCardService.deleteCard(
                    authHeader = tokenResponse,
                    cardId = cardId
                ).mapError {
                    BlockchainCardError.DeleteCardRequestFailed
                }.map { card ->
                    card.toDomainModel()
                }
            }

    override suspend fun lockCard(cardId: String): Outcome<BlockchainCardError, BlockchainCard> =
        authenticator.getAuthHeader().awaitOutcome()
            .mapError { BlockchainCardError.GetAuthFailed }
            .flatMap { tokenResponse ->
                blockchainCardService.lockCard(
                    authHeader = tokenResponse,
                    cardId = cardId
                ).mapError {
                    BlockchainCardError.LockCardRequestFailed
                }.map { card ->
                    card.toDomainModel()
                }
            }

    override suspend fun unlockCard(cardId: String): Outcome<BlockchainCardError, BlockchainCard> =
        authenticator.getAuthHeader().awaitOutcome()
            .mapError { BlockchainCardError.GetAuthFailed }
            .flatMap { tokenResponse ->
                blockchainCardService.unlockCard(
                    authHeader = tokenResponse,
                    cardId = cardId
                ).mapError {
                    BlockchainCardError.UnlockCardRequestFailed
                }.map { card ->
                    card.toDomainModel()
                }
            }

    override suspend fun getCardWidgetToken(cardId: String): Outcome<BlockchainCardError, String> =
        authenticator.getAuthHeader().awaitOutcome()
            .mapError { BlockchainCardError.GetAuthFailed }
            .flatMap { tokenResponse ->
                blockchainCardService.getCardWidgetToken(
                    authHeader = tokenResponse,
                    cardId = cardId
                ).mapError {
                    BlockchainCardError.GetCardWidgetTokenRequestFailed
                }.map { widgetToken ->
                    widgetToken.token
                }
            }

    override suspend fun getCardWidgetUrl(cardId: String, last4Digits: String): Outcome<BlockchainCardError, String> =
        authenticator.getAuthHeader().awaitOutcome()
            .mapError { BlockchainCardError.GetAuthFailed }
            .flatMap { tokenResponse ->
                blockchainCardService.getCardWidgetToken(
                    authHeader = tokenResponse,
                    cardId = cardId,
                ).mapError {
                    BlockchainCardError.GetCardWidgetTokenRequestFailed
                }.flatMap { widgetToken ->
                    blockchainCardService.getCardWidgetUrl(widgetToken.token, last4Digits)
                }.mapError {
                    BlockchainCardError.GetCardWidgetRequestFailed
                }
            }

    override suspend fun getEligibleTradingAccounts(
        cardId: String
    ): Outcome<BlockchainCardError, List<TradingAccount>> =
        authenticator.getAuthHeader().awaitOutcome()
            .mapError { BlockchainCardError.GetAuthFailed }
            .flatMap { tokenResponse ->
                blockchainCardService.getEligibleAccounts(
                    authHeader = tokenResponse,
                    cardId = cardId
                ).mapError {
                    BlockchainCardError.GetEligibleCardAccountsRequestFailed
                }.flatMap { eligibleAccountsList ->
                    val eligibleCurrencies = eligibleAccountsList.map { cardAccount ->
                        cardAccount.balance.symbol
                    }

                    coincore.allWallets().awaitOutcome().mapError {
                        BlockchainCardError.LoadAllWalletsFailed
                    }.map { accountGroup ->
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
                }
            }

    override suspend fun linkCardAccount(
        cardId: String,
        accountCurrency: String
    ): Outcome<BlockchainCardError, String> =
        authenticator.getAuthHeader().awaitOutcome()
            .mapError {
                BlockchainCardError.GetAuthFailed
            }
            .flatMap { tokenResponse ->
                blockchainCardService.linkCardAccount(
                    authHeader = tokenResponse,
                    cardId = cardId,
                    accountCurrency = accountCurrency
                ).mapError {
                    BlockchainCardError.LinkCardAccountFailed
                }.map { cardAccountLinkResponse ->
                    cardAccountLinkResponse.accountCurrency
                }
            }

    override suspend fun getCardLinkedAccount(
        cardId: String
    ): Outcome<BlockchainCardError, TradingAccount> =
        authenticator.getAuthHeader().awaitOutcome()
            .mapError {
                BlockchainCardError.GetAuthFailed
            }
            .flatMap { tokenResponse ->
                blockchainCardService.getCardLinkedAccount(
                    authHeader = tokenResponse,
                    cardId = cardId
                ).mapError {
                    BlockchainCardError.GetCardLinkedAccountFailed
                }.flatMap { cardLinkedAccountResponse ->
                    coincore.allWallets().awaitOutcome()
                        .mapError {
                            BlockchainCardError.LoadAllWalletsFailed
                        }.map { accountGroup ->
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
                        }
                }
            }

    override suspend fun loadAccountBalance(
        tradingAccount: BlockchainAccount
    ): Outcome<BlockchainCardError, AccountBalance> =
        tradingAccount.balance.firstOrError().awaitOutcome().mapError {
            BlockchainCardError.GetAccountBalanceFailed
        }

    override suspend fun getAsset(networkTicker: String): Outcome<BlockchainCardError, AssetInfo> =
        assetCatalogue.assetInfoFromNetworkTicker(networkTicker)?.let { asset ->
            Outcome.Success(asset)
        }
            ?: Outcome.Failure(BlockchainCardError.GetAssetFailed)

    override suspend fun getFiatAccount(networkTicker: String): Outcome<BlockchainCardError, FiatAccount> =
        coincore.allWallets().awaitOutcome()
            .mapError {
                BlockchainCardError.LoadAllWalletsFailed
            }.map { accountGroup ->
                accountGroup.accounts.filterIsInstance<FiatAccount>().first { tradingAccount ->
                    tradingAccount.currency.networkTicker == networkTicker
                }
            }

    override suspend fun getResidentialAddress(): Outcome<BlockchainCardError, BlockchainCardAddress> =
        authenticator.getAuthHeader().awaitOutcome()
            .mapError {
                BlockchainCardError.GetAuthFailed
            }
            .flatMap { tokenResponse ->
                blockchainCardService.getResidentialAddress(
                    authHeader = tokenResponse
                ).mapError {
                    BlockchainCardError.GetResidentialAddressFailed
                }.map { response ->
                    response.address.toDomainModel()
                }
            }

    override suspend fun updateResidentialAddress(
        address: BlockchainCardAddress
    ): Outcome<BlockchainCardError, BlockchainCardAddress> =
        authenticator.getAuthHeader().awaitOutcome()
            .mapError {
                BlockchainCardError.GetAuthFailed
            }
            .flatMap { tokenResponse ->
                blockchainCardService.updateResidentialAddress(
                    authHeader = tokenResponse,
                    residentialAddress = address.toDto(address)
                ).mapError {
                    BlockchainCardError.UpdateResidentialAddressFailed
                }.map { response ->
                    response.address.toDomainModel()
                }
            }

    override suspend fun getUserFirstAndLastName(): Outcome<BlockchainCardError, String> =
        userIdentity.getBasicProfileInformation().awaitOutcome()
            .mapError {
                BlockchainCardError.GetUserProfileFailed
            }.map { response ->
                response.firstName + " " + response.lastName
            }

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
}
