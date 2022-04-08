package com.blockchain.blockchaincard.data

import com.blockchain.api.blockchainCard.data.CardsResponse
import com.blockchain.api.blockchainCard.data.ProductsResponse
import com.blockchain.api.services.BlockchainCardService
import com.blockchain.blockchaincard.domain.BlockchainCardRepository
import com.blockchain.blockchaincard.domain.models.BlockchainCardBrand
import com.blockchain.blockchaincard.domain.models.BlockchainCardError
import com.blockchain.blockchaincard.domain.models.BlockchainCardStatus
import com.blockchain.blockchaincard.domain.models.BlockchainCardType
import com.blockchain.blockchaincard.domain.models.BlockchainDebitCard
import com.blockchain.blockchaincard.domain.models.BlockchainDebitCardProduct
import com.blockchain.nabu.Authenticator
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.flatMap
import com.blockchain.outcome.map
import com.blockchain.outcome.mapLeft
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.FiatValue
import java.math.BigDecimal
import piuk.blockchain.androidcore.utils.extensions.awaitOutcome

internal class BlockchainCardRepositoryImpl(
    val blockchainCardService: BlockchainCardService,
    private val authenticator: Authenticator
) : BlockchainCardRepository {

    override suspend fun getProducts(): Outcome<BlockchainCardError, List<BlockchainDebitCardProduct>> =
        authenticator.getAuthHeader().awaitOutcome()
            .mapLeft { BlockchainCardError.GetAuthFailed }
            .flatMap { tokenResponse ->
                blockchainCardService.getProducts(
                    tokenResponse
                ).mapLeft { BlockchainCardError.GetProductsRequestFailed }.map { response ->
                    response.map {
                        it.toDomainModel()
                    }
                }
            }

    override suspend fun getCards(): Outcome<BlockchainCardError, List<BlockchainDebitCard>> =
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
    ): Outcome<BlockchainCardError, BlockchainDebitCard> =
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

    override suspend fun deleteCard(cardId: String): Outcome<BlockchainCardError, BlockchainDebitCard> =
        authenticator.getAuthHeader().awaitOutcome()
            .mapLeft { BlockchainCardError.GetAuthFailed }
            .flatMap { tokenResponse ->
                blockchainCardService.deleteCard(
                    authHeader = tokenResponse,
                    cardId = cardId
                ).mapLeft { BlockchainCardError.DeleteCardRequestFailed }.map { card ->
                    card.toDomainModel()
                }
            }

    private fun ProductsResponse.toDomainModel(): BlockchainDebitCardProduct =
        BlockchainDebitCardProduct(
            productCode = productCode,
            price = FiatValue.fromMajor(
                fiatCurrency = FiatCurrency.fromCurrencyCode(price.symbol),
                major = BigDecimal(price.value)
            ),
            brand = BlockchainCardBrand.valueOf(brand),
            type = BlockchainCardType.valueOf(type)
        )

    private fun CardsResponse.toDomainModel(): BlockchainDebitCard =
        BlockchainDebitCard(
            id = id,
            type = BlockchainCardType.valueOf(type),
            last4 = last4,
            expiry = expiry,
            brand = BlockchainCardBrand.valueOf(brand),
            status = BlockchainCardStatus.valueOf(status),
            createdAt = createdAt
        )
}
