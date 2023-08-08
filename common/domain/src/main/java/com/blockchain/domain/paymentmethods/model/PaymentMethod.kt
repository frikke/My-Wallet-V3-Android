package com.blockchain.domain.paymentmethods.model

import com.blockchain.domain.common.model.ServerSideUxErrorInfo
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money
import java.io.Serializable
import java.util.Date
import java.util.Locale
import kotlinx.serialization.Contextual

sealed class PaymentMethod(
    val id: String,
    val type: PaymentMethodType,
    open val limits: PaymentLimits,
    val order: Int,
    open val isEligible: Boolean
) : Serializable {

    val availableBalance: Money?
        get() = (this as? Funds)?.balance

    data class GooglePay(
        override val limits: PaymentLimits,
        override val isEligible: Boolean
    ) : PaymentMethod(
        GOOGLE_PAY_PAYMENT_ID,
        PaymentMethodType.PAYMENT_CARD,
        limits,
        GOOGLE_PAY_PAYMENT_METHOD_ORDER,
        isEligible
    ) {
        private val label = "Google Pay"

        override fun detailedLabel() = label

        override fun methodName() = label

        override fun methodDetails() = label
    }

    data class UndefinedCard(
        override val limits: PaymentLimits,
        override val isEligible: Boolean,
        val cardFundSources: List<CardFundSource>?
    ) : PaymentMethod(
        UNDEFINED_CARD_PAYMENT_ID,
        PaymentMethodType.PAYMENT_CARD,
        limits,
        UNDEFINED_CARD_PAYMENT_METHOD_ORDER,
        isEligible
    ),
        UndefinedPaymentMethod {

        enum class CardFundSource {
            PREPAID,
            CREDIT,
            DEBIT,
            UNKNOWN
        }

        companion object {
            fun mapCardFundSources(sources: List<String>?): List<CardFundSource>? =
                sources?.map {
                    when (it) {
                        CardFundSource.CREDIT.name -> CardFundSource.CREDIT
                        CardFundSource.DEBIT.name -> CardFundSource.DEBIT
                        CardFundSource.PREPAID.name -> CardFundSource.PREPAID
                        else -> CardFundSource.UNKNOWN
                    }
                }
        }
    }

    data class Funds(
        val balance: Money,
        val fiatCurrency: FiatCurrency,
        override val limits: PaymentLimits,
        override val isEligible: Boolean
    ) : PaymentMethod(FUNDS_PAYMENT_ID, PaymentMethodType.FUNDS, limits, FUNDS_PAYMENT_METHOD_ORDER, isEligible)

    data class UndefinedBankAccount(
        val fiatCurrency: FiatCurrency,
        override val limits: PaymentLimits,
        override val isEligible: Boolean
    ) :
        PaymentMethod(
            UNDEFINED_BANK_ACCOUNT_ID,
            PaymentMethodType.FUNDS,
            limits,
            UNDEFINED_BANK_ACCOUNT_METHOD_ORDER,
            isEligible
        ),
        UndefinedPaymentMethod {
        val showAvailability: Boolean
            get() = currenciesRequiringAvailabilityLabel.contains(fiatCurrency)

        companion object {
            private val currenciesRequiringAvailabilityLabel = listOf(FiatCurrency.Dollars)
        }
    }

    data class UndefinedBankTransfer(
        override val limits: PaymentLimits,
        override val isEligible: Boolean
    ) :
        PaymentMethod(
            UNDEFINED_BANK_TRANSFER_PAYMENT_ID,
            PaymentMethodType.BANK_TRANSFER,
            limits,
            UNDEFINED_BANK_TRANSFER_METHOD_ORDER,
            isEligible
        ),
        UndefinedPaymentMethod

    data class Bank(
        val bankId: String,
        override val limits: PaymentLimits,
        val bankName: String,
        val accountEnding: String,
        val accountType: String,
        override val isEligible: Boolean,
        val iconUrl: String
    ) : PaymentMethod(bankId, PaymentMethodType.BANK_TRANSFER, limits, BANK_PAYMENT_METHOD_ORDER, isEligible),
        Serializable,
        RecurringBuyPaymentDetails {

        override fun detailedLabel() =
            "$bankName $accountEnding"

        override fun methodName() = bankName

        override fun methodDetails() = "$accountType $accountEnding"

        val uiAccountType: String =
            accountType.lowercase(Locale.getDefault()).replaceFirstChar {
                if (it.isLowerCase()) {
                    it.titlecase(
                        Locale.getDefault()
                    )
                } else it.toString()
            }

        override val paymentDetails: PaymentMethodType
            get() = PaymentMethodType.BANK_TRANSFER
    }

    data class Card(
        val cardId: String,
        override val limits: PaymentLimits,
        private val label: String,
        val endDigits: String,
        val partner: Partner,
        val expireDate: Date,
        val cardType: CardType,
        val status: CardStatus,
        val mobilePaymentType: MobilePaymentType? = null,
        override val isEligible: Boolean,
        @Contextual
        val cardRejectionState: CardRejectionState? = null,
        val serverSideUxErrorInfo: ServerSideUxErrorInfo? = null
    ) : PaymentMethod(cardId, PaymentMethodType.PAYMENT_CARD, limits, CARD_PAYMENT_METHOD_ORDER, isEligible),
        Serializable,
        RecurringBuyPaymentDetails {

        override fun detailedLabel() =
            "${uiLabel()} ${dottedEndDigits()}"

        override fun methodName() = label

        override fun methodDetails() = "$cardType $endDigits"

        fun uiLabel() =
            label.takeIf { it.isNotEmpty() } ?: getCardTypeLabel(cardType)

        fun dottedEndDigits() =
            "•••• $endDigits"

        private fun getCardTypeLabel(cardType: CardType) =
            when (cardType) {
                CardType.VISA -> "Visa"
                CardType.MASTERCARD -> "Mastercard"
                CardType.AMEX -> "American Express"
                CardType.DINERS_CLUB -> "Diners Club"
                CardType.MAESTRO -> "Maestro"
                CardType.JCB -> "JCB"
                else -> ""
            }

        override val paymentDetails: PaymentMethodType
            get() = PaymentMethodType.PAYMENT_CARD
    }

    fun canBeUsedForPaying(): Boolean =
        this is Card || this is Funds || this is Bank || this is GooglePay

    fun canBeAdded(): Boolean =
        this is UndefinedPaymentMethod || this is GooglePay

    open fun detailedLabel(): String = ""

    open fun methodName(): String = ""

    open fun methodDetails(): String = ""

    companion object {
        const val GOOGLE_PAY_PAYMENT_ID = "GOOGLE_PAY_PAYMENT_ID"
        const val UNDEFINED_CARD_PAYMENT_ID = "UNDEFINED_CARD_PAYMENT_ID"
        const val FUNDS_PAYMENT_ID = "FUNDS_PAYMENT_ID"
        const val UNDEFINED_BANK_ACCOUNT_ID = "UNDEFINED_BANK_ACCOUNT_ID"
        const val UNDEFINED_BANK_TRANSFER_PAYMENT_ID = "UNDEFINED_BANK_TRANSFER_PAYMENT_ID"

        private const val FUNDS_PAYMENT_METHOD_ORDER = 0
        private const val UNDEFINED_CARD_PAYMENT_METHOD_ORDER = 1
        private const val GOOGLE_PAY_PAYMENT_METHOD_ORDER = 2
        private const val CARD_PAYMENT_METHOD_ORDER = 3
        private const val BANK_PAYMENT_METHOD_ORDER = 4
        private const val UNDEFINED_BANK_TRANSFER_METHOD_ORDER = 5
        private const val UNDEFINED_BANK_ACCOUNT_METHOD_ORDER = 6
    }
}
