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
    val orderStatus: BlockchainCardOrderStatus?,
    val createdAt: String,
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
    val type: BlockchainCardTransactionType,
    val state: BlockchainCardTransactionState,
    val originalAmount: FiatValue,
    val fundingAmount: FiatValue,
    val reversedAmount: FiatValue,
    val counterAmount: FiatValue?,
    val clearedFundingAmount: FiatValue,
    val userTransactionTime: String,
    val merchantName: String,
    val networkConversionRate: Float?,
    val declineReason: String?,
    val fee: FiatValue,
) : Parcelable

@Parcelize
data class BlockchainCardLegalDocument(
    val name: String,
    val displayName: String,
    val url: String,
    val version: String,
    val acceptedVersion: String?,
    val required: Boolean,
    var seen: Boolean = false
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
    INITIATED,
    UNACTIVATED,
    ACTIVE,
    LOCKED,
    TERMINATED
}

enum class BlockchainCardTransactionState {
    CREATED,
    PENDING,
    DECLINED,
    CANCELLED,
    COMPLETED;

    override fun toString(): String {
        return this.name.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
    }

    fun getStringResource(): Int {
        return when (this) {
            CREATED -> R.string.bc_card_transaction_created
            PENDING -> R.string.bc_card_transaction_pending
            DECLINED -> R.string.bc_card_transaction_declined
            CANCELLED -> R.string.bc_card_transaction_cancelled
            COMPLETED -> R.string.bc_card_transaction_completed
        }
    }
}

enum class BlockchainCardTransactionType {
    PAYMENT,
    PAYMENT_WITH_CASHBACK,
    REFUND,
    FUNDING,
    CHARGEBACK;

    fun getStringResource(): Int {
        return when (this) {
            PAYMENT -> R.string.bc_card_transaction_payment
            PAYMENT_WITH_CASHBACK -> R.string.bc_card_transaction_payment_with_cashback
            REFUND -> R.string.bc_card_transaction_refund
            FUNDING -> R.string.bc_card_transaction_funding
            CHARGEBACK -> R.string.bc_card_transaction_chargeback
        }
    }
}

enum class BlockchainCardOrderStatus {
    ORDERED,
    SHIPPED,
    DELIVERED
}
