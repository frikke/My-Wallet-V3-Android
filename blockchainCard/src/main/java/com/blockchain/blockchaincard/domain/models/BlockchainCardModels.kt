package com.blockchain.blockchaincard.domain.models

import android.os.Parcelable
import com.blockchain.api.blockchainCard.api.CardsResponse
import com.blockchain.api.blockchainCard.api.ProductsResponse
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.FiatValue
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

fun ProductsResponse.toDomainModel(): BlockchainDebitCardProduct =
    BlockchainDebitCardProduct(
        productCode = productCode,
        price = FiatValue.fromMajor(
            fiatCurrency = FiatCurrency.fromCurrencyCode(price.symbol),
            major = BigDecimal(price.value)
        ),
        brand = BlockchainCardBrand.valueOf(brand),
        type = BlockchainCardType.valueOf(type)
    )

fun CardsResponse.toDomainModel(): BlockchainDebitCard =
    BlockchainDebitCard(
        cardId = cardId,
        type = BlockchainCardType.valueOf(type),
        last4 = last4,
        expiry = expiry,
        brand = BlockchainCardBrand.valueOf(brand),
        cardStatus = BlockchainCardStatus.valueOf(cardStatus),
        createdAt = createdAt
    )

@Parcelize
data class BlockchainDebitCardProduct(
    val productCode: String,
    val price: FiatValue,
    val brand: BlockchainCardBrand,
    val type: BlockchainCardType
) : Parcelable

data class BlockchainDebitCard(
    val cardId: String,
    val type: BlockchainCardType,
    val last4: String,
    val expiry: String,
    val brand: BlockchainCardBrand,
    val cardStatus: BlockchainCardStatus,
    val createdAt: String
)

enum class BlockchainCardBrand {
    VISA,
    MASTERCARD,
    UNKNOWN
}

enum class BlockchainCardType {
    VIRTUAL,
    PHYSICAL,
    UNKNOWN
}

enum class BlockchainCardStatus {
    CREATED,
    ACTIVE,
    TERMINATED
}