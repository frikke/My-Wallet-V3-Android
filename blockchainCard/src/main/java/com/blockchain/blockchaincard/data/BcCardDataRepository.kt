package com.blockchain.blockchaincard.data


import android.os.Parcelable
import com.blockchain.blockchaincard.domain.CardsResponse
import com.blockchain.blockchaincard.domain.ProductsResponse
import com.blockchain.nabu.Authenticator
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.FiatValue
import io.reactivex.rxjava3.core.Single
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

class BcCardDataRepository(
    val bcCardService: BcCardService,
    private val authenticator: Authenticator
) {

    fun getProducts(): Single<List<BlockchainDebitCardProduct>> =
        authenticator.authenticate { tokenResponse ->
            bcCardService.getProducts(
                tokenResponse.authHeader
            ).map { response ->
                response.map {
                    it.toDomainModel()
                }
            }
        }

    fun getCards(): Single<List<BlockchainDebitCard>> =
        authenticator.authenticate { tokenResponse ->
            bcCardService.getCards(
                tokenResponse.authHeader
            ).map { response ->
                response.map {
                    it.toDomainModel()
                }
            }
        }

    fun createCard(productCode: String, ssn: String): Single<BlockchainDebitCard> =
        authenticator.authenticate { tokenResponse ->
            bcCardService.createCard(
                authHeader = tokenResponse.authHeader,
                productCode = productCode,
                ssn = ssn
            ).map { card ->
                card.toDomainModel()
            }
        }

    fun deleteCard(cardId: String): Single<BlockchainDebitCard> =
        authenticator.authenticate { tokenResponse ->
            bcCardService.deleteCard(
                authHeader = tokenResponse.authHeader,
                cardId = cardId
            ).map { card ->
                card.toDomainModel()
            }
        }
}

private fun ProductsResponse.toDomainModel(): BlockchainDebitCardProduct =
    BlockchainDebitCardProduct(
        productCode = productCode,
        price = FiatValue.fromMajor(
            fiatCurrency = FiatCurrency.fromCurrencyCode(price.symbol),
            major = BigDecimal(price.value)
        ),
        brand = BcCardBrand.valueOf(brand),
        type = BcCardType.valueOf(type)
    )

private fun CardsResponse.toDomainModel(): BlockchainDebitCard =
    BlockchainDebitCard(
        cardId = cardId,
        type = BcCardType.valueOf(type),
        last4 = last4,
        expiry = expiry,
        brand = BcCardBrand.valueOf(brand),
        cardStatus = BcCardStatus.valueOf(cardStatus),
        createdAt = createdAt
    )

@Parcelize
data class BlockchainDebitCardProduct(
    val productCode: String,
    val price: FiatValue,
    val brand: BcCardBrand,
    val type: BcCardType
) : Parcelable

data class BlockchainDebitCard(
    val cardId: String,
    val type: BcCardType,
    val last4: String,
    val expiry: String,
    val brand: BcCardBrand,
    val cardStatus: BcCardStatus,
    val createdAt: String
)

enum class BcCardBrand {
    VISA,
    MASTERCARD,
    UNKNOWN
}

enum class BcCardType {
    VIRTUAL,
    PHYSICAL,
    UNKNOWN
}

enum class BcCardStatus {
    CREATED,
    ACTIVE,
    TERMINATED
}
