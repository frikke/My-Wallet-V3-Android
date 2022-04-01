package com.blockchain.blockchaincard.domain.models

import android.os.Parcelable
import com.blockchain.api.blockchainCard.data.CardsResponse
import com.blockchain.api.blockchainCard.data.ProductsResponse
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.FiatValue
import java.math.BigDecimal
import kotlinx.parcelize.Parcelize

sealed class BlockchainCardError {
    object RequestFailed : BlockchainCardError()
}

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
        id = id,
        type = BlockchainCardType.valueOf(type),
        last4 = last4,
        expiry = expiry,
        brand = BlockchainCardBrand.valueOf(brand),
        status = BlockchainCardStatus.valueOf(status),
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
    val id: String,
    val type: BlockchainCardType,
    val last4: String,
    val expiry: String,
    val brand: BlockchainCardBrand,
    val status: BlockchainCardStatus,
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
