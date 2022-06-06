package com.blockchain.blockchaincard.data

import com.blockchain.api.blockchainCard.data.CardsResponse
import com.blockchain.api.blockchainCard.data.ProductsResponse
import com.blockchain.api.services.BlockchainCardService
import com.blockchain.blockchaincard.domain.BlockchainCardRepository
import com.blockchain.blockchaincard.domain.models.BlockchainCard
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
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.flatMap
import com.blockchain.outcome.map
import com.blockchain.outcome.mapLeft
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
    private val assetCatalogue: AssetCatalogue
) : BlockchainCardRepository {

    override suspend fun getProducts(): Outcome<BlockchainCardError, List<BlockchainCardProduct>> =
        authenticator.getAuthHeader().awaitOutcome()
            .mapLeft { BlockchainCardError.GetAuthFailed }
            .flatMap { tokenResponse ->
                blockchainCardService.getProducts(
                    tokenResponse
                ).mapLeft {
                    BlockchainCardError.GetProductsRequestFailed
                }.map { response ->
                    response.map {
                        it.toDomainModel()
                    }
                }
            }

    override suspend fun getCards(): Outcome<BlockchainCardError, List<BlockchainCard>> =
        authenticator.getAuthHeader().awaitOutcome()
            .mapLeft { BlockchainCardError.GetAuthFailed }
            .flatMap { tokenResponse ->
                blockchainCardService.getCards(
                    tokenResponse
                ).mapLeft { BlockchainCardError.GetCardsRequestFailed }.map { response ->
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
            .mapLeft { BlockchainCardError.GetAuthFailed }
            .flatMap { tokenResponse ->
                blockchainCardService.createCard(
                    authHeader = tokenResponse,
                    productCode = productCode,
                    ssn = ssn
                ).mapLeft { BlockchainCardError.CreateCardRequestFailed }.map { card ->
                    card.toDomainModel()
                }
            }

    override suspend fun deleteCard(cardId: String): Outcome<BlockchainCardError, BlockchainCard> =
        authenticator.getAuthHeader().awaitOutcome()
            .mapLeft { BlockchainCardError.GetAuthFailed }
            .flatMap { tokenResponse ->
                blockchainCardService.deleteCard(
                    authHeader = tokenResponse,
                    cardId = cardId
                ).mapLeft {
                    BlockchainCardError.DeleteCardRequestFailed
                }.map { card ->
                    card.toDomainModel()
                }
            }

    override suspend fun lockCard(cardId: String): Outcome<BlockchainCardError, BlockchainCard> =
        authenticator.getAuthHeader().awaitOutcome()
            .mapLeft { BlockchainCardError.GetAuthFailed }
            .flatMap { tokenResponse ->
                blockchainCardService.lockCard(
                    authHeader = tokenResponse,
                    cardId = cardId
                ).mapLeft {
                    BlockchainCardError.LockCardRequestFailed
                }.map { card ->
                    card.toDomainModel()
                }
            }

    override suspend fun unlockCard(cardId: String): Outcome<BlockchainCardError, BlockchainCard> =
        authenticator.getAuthHeader().awaitOutcome()
            .mapLeft { BlockchainCardError.GetAuthFailed }
            .flatMap { tokenResponse ->
                blockchainCardService.unlockCard(
                    authHeader = tokenResponse,
                    cardId = cardId
                ).mapLeft {
                    BlockchainCardError.UnlockCardRequestFailed
                }.map { card ->
                    card.toDomainModel()
                }
            }

    override suspend fun getCardWidgetToken(cardId: String): Outcome<BlockchainCardError, String> =
        authenticator.getAuthHeader().awaitOutcome()
            .mapLeft { BlockchainCardError.GetAuthFailed }
            .flatMap { tokenResponse ->
                blockchainCardService.getCardWidgetToken(
                    authHeader = tokenResponse,
                    cardId = cardId
                ).mapLeft {
                    BlockchainCardError.GetCardWidgetTokenRequestFailed
                }.map { widgetToken ->
                    widgetToken.token
                }
            }

    override suspend fun getCardWidgetUrl(cardId: String, last4Digits: String): Outcome<BlockchainCardError, String> =
        authenticator.getAuthHeader().awaitOutcome()
            .mapLeft { BlockchainCardError.GetAuthFailed }
            .flatMap { tokenResponse ->
                blockchainCardService.getCardWidgetToken(
                    authHeader = tokenResponse,
                    cardId = cardId,
                ).mapLeft {
                    BlockchainCardError.GetCardWidgetTokenRequestFailed
                }.flatMap { widgetToken ->
                    blockchainCardService.getCardWidgetUrl(widgetToken.token, last4Digits)
                }.mapLeft {
                    BlockchainCardError.GetCardWidgetRequestFailed
                }
            }

    override suspend fun getEligibleTradingAccounts(
        cardId: String
    ): Outcome<BlockchainCardError, List<TradingAccount>> =
        authenticator.getAuthHeader().awaitOutcome()
            .mapLeft { BlockchainCardError.GetAuthFailed }
            .flatMap { tokenResponse ->
                blockchainCardService.getEligibleAccounts(
                    authHeader = tokenResponse,
                    cardId = cardId
                ).mapLeft {
                    BlockchainCardError.GetEligibleCardAccountsRequestFailed
                }.flatMap { eligibleAccountsList ->
                    val eligibleCurrencies = eligibleAccountsList.map { cardAccount ->
                        cardAccount.balance.symbol
                    }

                    coincore.allWallets().awaitOutcome().mapLeft {
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
            .mapLeft {
                BlockchainCardError.GetAuthFailed
            }
            .flatMap { tokenResponse ->
                blockchainCardService.linkCardAccount(
                    authHeader = tokenResponse,
                    cardId = cardId,
                    accountCurrency = accountCurrency
                ).mapLeft {
                    BlockchainCardError.LinkCardAccountFailed
                }.map { cardAccountLinkResponse ->
                    cardAccountLinkResponse.accountCurrency
                }
            }

    override suspend fun getCardLinkedAccount(
        cardId: String
    ): Outcome<BlockchainCardError, TradingAccount> =
        authenticator.getAuthHeader().awaitOutcome()
            .mapLeft {
                BlockchainCardError.GetAuthFailed
            }
            .flatMap { tokenResponse ->
                blockchainCardService.getCardLinkedAccount(
                    authHeader = tokenResponse,
                    cardId = cardId
                ).mapLeft {
                    BlockchainCardError.GetCardLinkedAccountFailed
                }.flatMap { cardLinkedAccountResponse ->
                    coincore.allWallets().awaitOutcome()
                        .mapLeft {
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
        tradingAccount.balance.firstOrError().awaitOutcome().mapLeft {
            BlockchainCardError.GetAccountBalanceFailed
        }

    override suspend fun getAsset(networkTicker: String): Outcome<BlockchainCardError, AssetInfo> =
        assetCatalogue.assetInfoFromNetworkTicker(networkTicker)?.let { asset ->
            Outcome.Success(asset)
        }
            ?: Outcome.Failure(BlockchainCardError.GetAssetFailed)

    override suspend fun getFiatAccount(networkTicker: String): Outcome<BlockchainCardError, FiatAccount> =
        coincore.allWallets().awaitOutcome()
            .mapLeft {
                BlockchainCardError.LoadAllWalletsFailed
            }.map { accountGroup ->
                accountGroup.accounts.filterIsInstance<FiatAccount>().first { tradingAccount ->
                    tradingAccount.currency.networkTicker == networkTicker
                }
            }

    //
    // Domain Model Conversion
    //
    private fun ProductsResponse.toDomainModel(): BlockchainCardProduct =
        BlockchainCardProduct(
            productCode = productCode,
            price = FiatValue.fromMajor(
                fiatCurrency = FiatCurrency.fromCurrencyCode(price.symbol),
                major = BigDecimal(price.value)
            ),
            brand = BlockchainCardBrand.valueOf(brand),
            type = BlockchainCardType.valueOf(type)
        )

    private fun CardsResponse.toDomainModel(): BlockchainCard =
        BlockchainCard(
            id = id,
            type = BlockchainCardType.valueOf(type),
            last4 = last4,
            expiry = expiry,
            brand = BlockchainCardBrand.valueOf(brand),
            status = BlockchainCardStatus.valueOf(status),
            createdAt = createdAt
        )
}
