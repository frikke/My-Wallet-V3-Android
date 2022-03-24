package com.blockchain.nabu.models.responses.simplebuy

import com.blockchain.api.paymentmethods.models.SimpleBuyConfirmationAttributes
import com.blockchain.nabu.datamanagers.OrderInput
import com.blockchain.nabu.datamanagers.OrderOutput
import java.util.Date
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SimpleBuyPairsResp(val pairs: List<SimpleBuyPairResp>)

@Serializable
data class SimpleBuyPairResp(
    val pair: String,
    val buyMin: Long,
    val buyMax: Long,
    val sellMin: Long,
    val sellMax: Long
)

@Serializable
data class SimpleBuyEligibility(
    val eligible: Boolean,
    val simpleBuyTradingEligible: Boolean,
    val pendingDepositSimpleBuyTrades: Int,
    val maxPendingDepositSimpleBuyTrades: Int
)

@Serializable
data class SimpleBuyCurrency(val currency: String)

@Serializable
data class SimpleBuyQuoteResponse(
    val time: @Contextual Date,
    val rate: Long,
    val rateWithoutFee: Long,
    /* the  fee value is more of a feeRate (ie it is the fee per 1 unit of crypto) to get the actual
     "fee" you'll need to multiply by amount of crypto
     */
    val fee: Long
)

@Serializable
data class BankAccountResponse(
    val address: String? = null,
    val agent: BankAgentResponse,
    val currency: String
)

@Serializable
data class BankAgentResponse(
    val account: String? = null,
    val address: String? = null,
    val code: String? = null,
    val country: String? = null,
    val name: String? = null,
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
    val paymentError: String? = null
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

        const val APPROVAL_ERROR_INVALID = "BANK_TRANSFER_PAYMENT_INVALID"
        const val APPROVAL_ERROR_FAILED = "BANK_TRANSFER_PAYMENT_FAILED"
        const val APPROVAL_ERROR_DECLINED = "BANK_TRANSFER_PAYMENT_DECLINED"
        const val APPROVAL_ERROR_REJECTED = "BANK_TRANSFER_PAYMENT_REJECTED"
        const val APPROVAL_ERROR_EXPIRED = "BANK_TRANSFER_PAYMENT_EXPIRED"
        const val APPROVAL_ERROR_EXCEEDED = "BANK_TRANSFER_PAYMENT_LIMITED_EXCEEDED"
        const val APPROVAL_ERROR_ACCOUNT_INVALID = "BANK_TRANSFER_PAYMENT_USER_ACCOUNT_INVALID"
        const val APPROVAL_ERROR_FAILED_INTERNAL = "BANK_TRANSFER_PAYMENT_FAILED_INTERNAL"
        const val APPROVAL_ERROR_INSUFFICIENT_FUNDS = "BANK_TRANSFER_PAYMENT_INSUFFICIENT_FUNDS"

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
    val amount: String
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
    val everypay: EverypayPaymentAttributesResponse? = null,
    val authorisationUrl: String? = null,
    val status: String? = null,
    val cardProvider: CardProviderPaymentAttributesResponse? = null
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

        const val DEPOSIT = "DEPOSIT"
        const val CHARGE = "CHARGE"
        const val CARD_PAYMENT_FAILED = "CARD_PAYMENT_FAILED"
        const val CARD_PAYMENT_ABANDONED = "CARD_PAYMENT_ABANDONED"
        const val CARD_PAYMENT_EXPIRED = "CARD_PAYMENT_EXPIRED"
        const val BANK_TRANSFER_PAYMENT_REJECTED = "BANK_TRANSFER_PAYMENT_REJECTED"
        const val BANK_TRANSFER_PAYMENT_EXPIRED = "BANK_TRANSFER_PAYMENT_EXPIRED"
        const val WITHDRAWAL = "WITHDRAWAL"
    }
}

@Serializable
data class TransactionAttributesResponse(
    val beneficiary: TransactionBeneficiaryResponse? = null
)

@Serializable
data class TransactionBeneficiaryResponse(
    val accountRef: String? = null
)

@Serializable
data class AmountResponse(
    val symbol: String
)

typealias BuyOrderListResponse = List<BuySellOrderResponse>
