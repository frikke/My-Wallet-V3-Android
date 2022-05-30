package com.blockchain.domain.paymentmethods.model

import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money
import java.io.Serializable
import java.math.BigInteger
import java.net.URL

data class LinkBankTransfer(val id: String, val partner: BankPartner, val attributes: LinkBankAttributes) : Serializable

enum class BankPartner {
    YAPILY,
    YODLEE;

    companion object {
        const val ICON = "icon"
    }
}

interface LinkBankAttributes

data class YodleeAttributes(val fastlinkUrl: String, val token: String, val configName: String) :
    LinkBankAttributes,
    Serializable

data class YapilyAttributes(
    val entity: String,
    val institutionList: List<YapilyInstitution>
) : LinkBankAttributes, Serializable

data class YapilyInstitution(
    val operatingCountries: List<InstitutionCountry>,
    val name: String,
    val id: String,
    val iconLink: URL?
) : Serializable

data class InstitutionCountry(val countryCode: String, val displayName: String) : Serializable

data class LinkedBank(
    val id: String,
    val currency: FiatCurrency,
    val partner: BankPartner,
    val bankName: String,
    val accountName: String,
    val accountNumber: String,
    val state: LinkedBankState,
    val errorStatus: LinkedBankErrorState,
    val accountType: String,
    val authorisationUrl: String,
    val sortCode: String,
    val accountIban: String,
    val bic: String,
    val entity: String,
    val iconUrl: String,
    val callbackPath: String
) : Serializable {
    val account: String
        get() = accountNumber

    val paymentMethod: PaymentMethodType
        get() = PaymentMethodType.BANK_TRANSFER

    fun isLinkingPending() = !isLinkingInFinishedState()

    fun isLinkingInFinishedState() =
        state == LinkedBankState.ACTIVE || state == LinkedBankState.BLOCKED

    fun toPaymentMethod() =
        PaymentMethod.Bank(
            bankId = id,
            limits = PaymentLimits(BigInteger.ZERO, BigInteger.ZERO, currency),
            bankName = accountName,
            accountEnding = accountNumber,
            accountType = accountType,
            isEligible = true,
            iconUrl = iconUrl
        )
}

enum class LinkedBankErrorState {
    ACCOUNT_ALREADY_LINKED,
    NAMES_MISMATCHED,
    ACCOUNT_TYPE_UNSUPPORTED,
    NOT_INFO_FOUND,
    REJECTED,
    EXPIRED,
    FAILURE,
    INTERNAL_FAILURE,
    INVALID,
    FRAUD,
    UNKNOWN,
    NONE
}

enum class LinkedBankState {
    CREATED,
    PENDING,
    BLOCKED,
    ACTIVE,
    UNKNOWN
}

data class FiatWithdrawalFeeAndLimit(
    val minLimit: Money,
    val fee: Money
) : LegacyLimits {
    override val min: Money
        get() = minLimit
    override val max: Money?
        get() = null
}

data class CryptoWithdrawalFeeAndLimit(
    val minLimit: BigInteger,
    val fee: BigInteger
)

data class BankTransferDetails(
    val id: String,
    val amount: Money,
    val authorisationUrl: String?,
    val status: BankTransferStatus
)

enum class BankTransferStatus {
    UNKNOWN,
    PENDING,
    ERROR,
    COMPLETE
}
