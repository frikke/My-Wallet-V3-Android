package com.blockchain.nabu.models.responses.simplebuy

import com.blockchain.api.NabuUxErrorResponse
import com.blockchain.api.paymentmethods.models.SimpleBuyConfirmationAttributes
import com.blockchain.nabu.datamanagers.OrderInput
import com.blockchain.nabu.datamanagers.OrderOutput
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SimpleBuyPairsDto(val pairs: List<SimpleBuyPairDto>)

@Serializable
data class SimpleBuyPairDto(
    val pair: String,
    val buyMin: Long,
    val buyMax: Long,
    val sellMin: Long,
    val sellMax: Long
)

@Serializable
data class SimpleBuyEligibilityDto(
    val eligible: Boolean,
    val simpleBuyTradingEligible: Boolean,
    val pendingDepositSimpleBuyTrades: Int,
    val maxPendingDepositSimpleBuyTrades: Int
)

@Serializable
data class SimpleBuyCurrency(val currency: String)

@Serializable
data class CustodialAccountResponse(
    val address: String,
    val agent: CustodialAccountAgentResponse,
    val currency: String,
    val state: String?,
    val partner: String?
) {
    companion object {
        const val PARTNER_BIND = "BIND"
        const val PARTNER_SILVERGATE = "SILVERGATE"
    }
}

@Serializable
data class CustodialAccountAgentResponse(
    val account: String? = null,
    val address: String? = null,
    val label: String? = null,
    val holderDocument: String? = null,
    val code: String? = null,
    val country: String? = null,
    val name: String? = null,
    val bankName: String? = null,
    val recipient: String? = null,
    val routingNumber: String? = null,
    val recipientAddress: String? = null,
    val accountType: String? = null,
    val swiftCode: String? = null
)

@Serializable
data class TransferFundsResponse(
    val id: String,
    val code: Long? = null // Only present in error responses
) {
    companion object {
        const val ERROR_WITHDRAWL_LOCKED = 152L
    }
}

@Serializable
data class FeesResponse(
    val fees: List<CurrencyFeeResponse>,
    val minAmounts: List<CurrencyFeeResponse>
)

@Serializable
data class CurrencyFeeResponse(
    val symbol: String,
    val minorValue: String
)

@Serializable
data class CustodialWalletOrder(
    private val quoteId: String? = null,
    private val pair: String,
    private val action: String,
    private val input: OrderInput,
    private val output: OrderOutput,
    private val paymentMethodId: String? = null,
    private val paymentType: String? = null,
    private val period: String? = null
)

@Serializable
data class BuySellOrderResponse(
    val id: String,
    val pair: String,
    val inputCurrency: String,
    val inputQuantity: String,
    val outputCurrency: String,
    val outputQuantity: String,
    val paymentMethodId: String? = null,
    val paymentType: String,
    val state: String,
    val insertedAt: String,
    val price: String? = null,
    val fee: String? = null,
    val attributes: PaymentAttributesResponse? = null,
    val expiresAt: String,
    val updatedAt: String,
    val side: String,
    val depositPaymentId: String? = null,
    val processingErrorType: String? = null,
    val recurringBuyId: String? = null,
    val failureReason: String? = null,
    val paymentError: String? = null,
    val ux: NabuUxErrorResponse? = null
) {
    companion object {
        const val PENDING_DEPOSIT = "PENDING_DEPOSIT"
        const val PENDING_EXECUTION = "PENDING_EXECUTION"
        const val PENDING_CONFIRMATION = "PENDING_CONFIRMATION"
        const val DEPOSIT_MATCHED = "DEPOSIT_MATCHED"
        const val FINISHED = "FINISHED"
        const val CANCELED = "CANCELED"
        const val FAILED = "FAILED"
        const val EXPIRED = "EXPIRED"

        // https://github.com/blockchain/service-nabu-payments/blob/master/client/src/main/kotlin/com/blockchain/nabu/payments/PaymentErrorType.kt

        // Bank transfer beneficiary errors - To be mapped it in the future
        const val BANK_TRANSFER_ACCOUNT_INFO_NOT_FOUND = "BANK_TRANSFER_ACCOUNT_INFO_NOT_FOUND"
        const val BANK_TRANSFER_ACCOUNT_ALREADY_LINKED = "BANK_TRANSFER_ACCOUNT_ALREADY_LINKED"
        const val BANK_TRANSFER_ACCOUNT_NOT_SUPPORTED = "BANK_TRANSFER_ACCOUNT_NOT_SUPPORTED"
        const val BANK_TRANSFER_ACCOUNT_FAILED = "BANK_TRANSFER_ACCOUNT_FAILED"
        const val BANK_TRANSFER_ACCOUNT_REJECTED = "BANK_TRANSFER_ACCOUNT_REJECTED"
        const val BANK_TRANSFER_ACCOUNT_EXPIRED = "BANK_TRANSFER_ACCOUNT_EXPIRED"
        const val BANK_TRANSFER_ACCOUNT_INVALID = "BANK_TRANSFER_ACCOUNT_INVALID"
        const val BANK_TRANSFER_ACCOUNT_FAILED_INTERNAL = "BANK_TRANSFER_ACCOUNT_FAILED_INTERNAL"
        const val BANK_TRANSFER_ACCOUNT_REJECTED_FRAUD = "BANK_TRANSFER_ACCOUNT_REJECTED_FRAUD"
        const val BANK_TRANSFER_ACCOUNT_MIGRATED = "BANK_TRANSFER_ACCOUNT_MIGRATED"

        // Bank transfer payment errors
        const val APPROVAL_ERROR_INVALID = "BANK_TRANSFER_PAYMENT_INVALID"
        const val APPROVAL_ERROR_FAILED = "BANK_TRANSFER_PAYMENT_FAILED"
        const val APPROVAL_ERROR_DECLINED = "BANK_TRANSFER_PAYMENT_DECLINED"
        const val APPROVAL_ERROR_REJECTED = "BANK_TRANSFER_PAYMENT_REJECTED"
        const val APPROVAL_ERROR_EXPIRED = "BANK_TRANSFER_PAYMENT_EXPIRED"
        const val APPROVAL_ERROR_EXCEEDED = "BANK_TRANSFER_PAYMENT_LIMITED_EXCEEDED"
        const val APPROVAL_ERROR_ACCOUNT_INVALID = "BANK_TRANSFER_PAYMENT_USER_ACCOUNT_INVALID"
        const val APPROVAL_ERROR_FAILED_INTERNAL = "BANK_TRANSFER_PAYMENT_FAILED_INTERNAL"
        const val APPROVAL_ERROR_INSUFFICIENT_FUNDS = "BANK_TRANSFER_PAYMENT_INSUFFICIENT_FUNDS"

        // Card create errors
        const val CARD_CREATE_DUPLICATE = "CARD_CREATE_DUPLICATE"
        const val CARD_CREATE_FAILED = "CARD_CREATE_FAILED"
        const val CARD_CREATE_ABANDONED = "CARD_CREATE_ABANDONED"
        const val CARD_CREATE_EXPIRED = "CARD_CREATE_EXPIRED"
        const val CARD_CREATE_BANK_DECLINED = "CARD_CREATE_BANK_DECLINED"
        const val CARD_CREATE_DEBIT_ONLY = "CARD_CREATE_DEBIT_ONLY"
        const val CARD_CREATE_NO_TOKEN = "CARD_CREATE_NO_TOKEN"

        // Card payment errors
        const val CARD_PAYMENT_NOT_SUPPORTED = "CARD_PAYMENT_NOT_SUPPORTED"
        const val CARD_PAYMENT_FAILED = "CARD_PAYMENT_FAILED"
        const val CARD_PAYMENT_ABANDONED = "CARD_PAYMENT_ABANDONED"
        const val CARD_PAYMENT_EXPIRED = "CARD_PAYMENT_EXPIRED"
        const val CARD_PAYMENT_INSUFFICIENT_FUNDS = "CARD_PAYMENT_INSUFFICIENT_FUNDS"
        const val CARD_PAYMENT_BANK_DECLINED = "CARD_PAYMENT_INSUFFICIENT_FUNDS"
        const val CARD_PAYMENT_DEBIT_ONLY = "CARD_PAYMENT_DEBIT_ONLY"
        const val CARD_PAYMENT_BLOCKCHAIN_DECLINED = "CARD_PAYMENT_BLOCKCHAIN_DECLINED"
        const val CARD_PAYMENT_ACQUIRER_DECLINED = "CARD_PAYMENT_ACQUIRER_DECLINED"

        // For recurring buys
        const val FAILED_INSUFFICIENT_FUNDS = "FAILED_INSUFFICIENT_FUNDS"
        const val FAILED_INTERNAL_ERROR = "FAILED_INTERNAL_ERROR"
        const val FAILED_BENEFICIARY_BLOCKED = "FAILED_BENEFICIARY_BLOCKED"
        const val FAILED_BAD_FILL = "FAILED_BAD_FILL"
        const val ISSUER_PROCESSING_ERROR = "ISSUER"
    }
}

@Serializable
data class TransferRequest(
    val address: String,
    val currency: String,
    val amount: String,
    val fee: String
)

@Serializable
class ProductTransferRequestBody(
    val currency: String,
    val amount: String,
    val origin: String,
    val destination: String
)

@Serializable
data class PaymentAttributesResponse(
    val paymentId: String? = null,
    val everypay: EverypayPaymentAttributesResponse? = null,
    val authorisationUrl: String? = null,
    val error: String? = null,
    val cardProvider: CardProviderPaymentAttributesResponse? = null,
    val needCvv: Boolean? = false,
    val cardCassy: CardProviderPaymentAttributesResponse? = null
)

@Serializable
enum class PaymentStateResponse {
    @SerialName("INITIAL")
    INITIAL,

    @SerialName("WAITING_FOR_3DS_RESPONSE")
    WAITING_FOR_3DS_RESPONSE,

    @SerialName("CONFIRMED_3DS")
    CONFIRMED_3DS,

    @SerialName("SETTLED")
    SETTLED,

    @SerialName("VOIDED")
    VOIDED,

    @SerialName("ABANDONED")
    ABANDONED,

    @SerialName("FAILED")
    FAILED
}

// cardAcquirerName and cardAcquirerAccountCode are mandatory
@Serializable
data class CardProviderPaymentAttributesResponse(
    val cardAcquirerName: String,
    val cardAcquirerAccountCode: String,
    val paymentLink: String?,
    val paymentState: PaymentStateResponse? = null,
    val clientSecret: String? = null,
    val publishableApiKey: String? = null
)

@Serializable
data class EverypayPaymentAttributesResponse(
    val paymentLink: String,
    val paymentState: PaymentStateResponse? = null
)

@Serializable
data class ConfirmOrderRequestBody(
    private val action: String = "confirm",
    private val paymentMethodId: String? = null,
    private val attributes: SimpleBuyConfirmationAttributes? = null,
    private val paymentType: String? = null
)

@Serializable
data class WithdrawRequestBody(
    private val beneficiary: String,
    private val currency: String,
    private val amount: String
)

@Serializable
data class DepositRequestBody(
    private val currency: String,
    private val depositAddress: String,
    private val txHash: String,
    private val amount: String,
    private val product: String
)

@Serializable
data class WithdrawLocksCheckRequestBody(
    private val paymentMethod: String,
    private val currency: String
)

@Serializable
data class WithdrawLocksCheckResponse(
    val rule: WithdrawLocksRuleResponse? = null
)

@Serializable
data class WithdrawLocksRuleResponse(
    val lockTime: String
)

@Serializable
data class TransactionsResponse(
    val items: List<TransactionResponse>
)

@Serializable
data class TransactionResponse(
    val id: String,
    val amount: AmountResponse,
    val amountMinor: String,
    val feeMinor: String? = null,
    val insertedAt: String,
    val type: String,
    val state: String,
    val beneficiaryId: String? = null,
    val error: String? = null,
    val extraAttributes: TransactionAttributesResponse? = null,
    val txHash: String? = null
) {
    companion object {
        const val COMPLETE = "COMPLETE"
        const val CREATED = "CREATED"
        const val PENDING = "PENDING"
        const val UNIDENTIFIED = "UNIDENTIFIED"
        const val FAILED = "FAILED"
        const val FRAUD_REVIEW = "FRAUD_REVIEW"
        const val CLEARED = "CLEARED"
        const val REJECTED = "REJECTED"
        const val MANUAL_REVIEW = "MANUAL_REVIEW"
        const val REFUNDED = "REFUNDED"
        const val PROCESSING = "PROCESSING"
        const val DEPOSIT = "DEPOSIT"
        const val INTEREST_OUTGOING = "INTEREST_OUTGOING"
        const val CHARGE = "CHARGE"
        const val CARD_PAYMENT_FAILED = "CARD_PAYMENT_FAILED"
        const val CARD_PAYMENT_ABANDONED = "CARD_PAYMENT_ABANDONED"
        const val CARD_PAYMENT_EXPIRED = "CARD_PAYMENT_EXPIRED"
        const val BANK_TRANSFER_PAYMENT_REJECTED = "BANK_TRANSFER_PAYMENT_REJECTED"
        const val BANK_TRANSFER_PAYMENT_EXPIRED = "BANK_TRANSFER_PAYMENT_EXPIRED"
        const val WITHDRAWAL = "WITHDRAWAL"
        const val DEBIT = "DEBIT"
    }
}

@Serializable
data class TransactionAttributesResponse(
    val address: String? = null,
    val confirmations: Int? = 0,
    val hash: String? = null,
    val id: String? = null,
    val txHash: String? = null,
    val transferType: String? = null,
    val beneficiary: TransactionBeneficiaryResponse? = null
)

@Serializable
data class TransactionBeneficiaryResponse(
    val accountRef: String? = null,
    val user: String? = null,
)

@Serializable
data class AmountResponse(
    val symbol: String,
    val value: String
)

typealias BuyOrderListResponse = List<BuySellOrderResponse>
