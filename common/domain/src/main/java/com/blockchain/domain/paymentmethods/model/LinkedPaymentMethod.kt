package com.blockchain.domain.paymentmethods.model

import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money
import java.io.Serializable
import java.util.Date
import java.util.Locale

sealed class LinkedPaymentMethod(
    val type: PaymentMethodType,
    open val currency: FiatCurrency
) {
    data class Card(
        val cardId: String,
        val label: String,
        val endDigits: String,
        val partner: Partner,
        val expireDate: Date,
        val cardType: String,
        val status: CardStatus,
        val cardFundSources: List<String>? = null,
        val mobilePaymentType: MobilePaymentType? = null,
        override val currency: FiatCurrency
    ) : LinkedPaymentMethod(PaymentMethodType.PAYMENT_CARD, currency)

    data class Funds(
        val balance: Money,
        override val currency: FiatCurrency
    ) : LinkedPaymentMethod(PaymentMethodType.FUNDS, currency)

    data class Bank(
        val id: String,
        val name: String,
        val accountEnding: String,
        val accountType: String,
        val iconUrl: String,
        val isBankTransferAccount: Boolean,
        val state: BankState,
        override val currency: FiatCurrency
    ) : LinkedPaymentMethod(
        if (isBankTransferAccount) PaymentMethodType.BANK_TRANSFER
        else PaymentMethodType.BANK_ACCOUNT,
        currency
    ),
        Serializable {
        fun toHumanReadableAccount(): String {
            return accountType.toLowerCase(Locale.getDefault()).capitalize(Locale.getDefault())
        }
    }
}
