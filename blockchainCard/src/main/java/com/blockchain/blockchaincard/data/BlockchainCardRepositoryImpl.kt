package com.blockchain.blockchaincard.data

import com.blockchain.api.services.BlockchainCardService
import com.blockchain.blockchaincard.domain.BlockchainCardRepository
import com.blockchain.blockchaincard.domain.models.BlockchainDebitCard
import com.blockchain.blockchaincard.domain.models.BlockchainDebitCardProduct
import com.blockchain.blockchaincard.domain.models.toDomainModel
import com.blockchain.nabu.Authenticator
import io.reactivex.rxjava3.core.Single

class BlockchainCardRepositoryImpl(
    val blockchainCardService: BlockchainCardService,
    private val authenticator: Authenticator
) : BlockchainCardRepository {

    override fun getProducts(): Single<List<BlockchainDebitCardProduct>> =
        authenticator.authenticate { tokenResponse ->
            blockchainCardService.getProducts(
                tokenResponse.authHeader
            ).map { response ->
                response.map {
                    it.toDomainModel()
                }
            }
        }

    override fun getCards(): Single<List<BlockchainDebitCard>> =
        authenticator.authenticate { tokenResponse ->
            blockchainCardService.getCards(
                tokenResponse.authHeader
            ).map { response ->
                response.map {
                    it.toDomainModel()
                }
            }
        }

    override fun createCard(productCode: String, ssn: String): Single<BlockchainDebitCard> =
        authenticator.authenticate { tokenResponse ->
            blockchainCardService.createCard(
                authHeader = tokenResponse.authHeader,
                productCode = productCode,
                ssn = ssn
            ).map { card ->
                card.toDomainModel()
            }
        }

    override fun deleteCard(cardId: String): Single<BlockchainDebitCard> =
        authenticator.authenticate { tokenResponse ->
            blockchainCardService.deleteCard(
                authHeader = tokenResponse.authHeader,
                cardId = cardId
            ).map { card ->
                card.toDomainModel()
            }
        }
}
