package com.blockchain.blockchaincard.domain.models

import android.os.Parcelable
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import kotlinx.parcelize.Parcelize

sealed class BlockchainCardError {
    object GetAuthFailed : BlockchainCardError()
    object GetCardsRequestFailed : BlockchainCardError()
    object CreateCardRequestFailed : BlockchainCardError()
    object DeleteCardRequestFailed : BlockchainCardError()
    object GetProductsRequestFailed : BlockchainCardError()
    object GetCardWidgetTokenRequestFailed : BlockchainCardError()
    object GetCardWidgetRequestFailed : BlockchainCardError()
    object GetEligibleCardAccountsRequestFailed : BlockchainCardError()
    object GetAccountBalanceFailed : BlockchainCardError()
    object LinkCardAccountFailed : BlockchainCardError()
    object GetCardLinkedAccountFailed : BlockchainCardError()
    object LoadAllWalletsFailed : BlockchainCardError()
    object LockCardRequestFailed : BlockchainCardError()
    object UnlockCardRequestFailed : BlockchainCardError()
}

@Parcelize
data class BlockchainCardProduct(
    val productCode: String,
    val price: FiatValue,
    val brand: BlockchainCardBrand,
    val type: BlockchainCardType
) : Parcelable

@Parcelize
data class BlockchainCard(
    val id: String,
    val type: BlockchainCardType,
    val last4: String,
    val expiry: String,
    val brand: BlockchainCardBrand,
    val status: BlockchainCardStatus,
    val createdAt: String
) : Parcelable

@Parcelize
data class BlockchainCardAccount(
    val balance: CryptoValue
) : Parcelable

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
    LOCKED,
    TERMINATED
}
