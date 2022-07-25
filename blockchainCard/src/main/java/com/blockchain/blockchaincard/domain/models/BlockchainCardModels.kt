package com.blockchain.blockchaincard.domain.models

import android.os.Parcelable
import com.blockchain.blockchaincard.R
import com.blockchain.domain.common.model.ServerSideUxErrorInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import java.util.Locale
import kotlinx.parcelize.Parcelize

sealed class BlockchainCardError {
    object LocalCopyBlockchainCardError : BlockchainCardError()
    data class UXBlockchainCardError(val uxError: ServerSideUxErrorInfo) : BlockchainCardError()
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

@Parcelize
data class BlockchainCardAddress(
    val line1: String,
    val line2: String,
    val postCode: String,
    val city: String,
    val state: String,
    val country: String
) : Parcelable {
    fun getShortAddress(): String {
        return "$line1, $city"
    }
}

@Parcelize
data class BlockchainCardTransaction(
    val id: String,
    val cardId: String,
    val type: String,
    val state: BlockchainCardTransactionState,
    val originalAmount: FiatValue,
    val fundingAmount: FiatValue,
    val reversedAmount: FiatValue,
    val counterAmount: FiatValue?,
    val clearedFundingAmount: FiatValue,
    val userTransactionTime: String,
    val merchantName: String,
    val networkConversionRate: Int?,
    val declineReason: String?,
    val fee: FiatValue,
) : Parcelable

@Parcelize
data class BlockchainCardLegalDocuments(
    val shortFormDisclosure: BlockchainCardLegalDocument,
    val termsAndConditions: BlockchainCardLegalDocument
) : Parcelable

@Parcelize
data class BlockchainCardLegalDocument(
    val url: String,
    val version: String,
    val acceptedVersion: String?,
    val seen: Boolean
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

enum class BlockchainCardTransactionState {
    PENDING,
    CANCELLED,
    DECLINED,
    COMPLETED;

    override fun toString(): String {
        return this.name.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
    }

    fun getStringResource(): Int {
        return when (this) {
            PENDING -> R.string.bc_card_transaction_pending
            CANCELLED -> R.string.bc_card_transaction_cancelled
            DECLINED -> R.string.bc_card_transaction_declined
            COMPLETED -> R.string.bc_card_transaction_completed
        }
    }
}
