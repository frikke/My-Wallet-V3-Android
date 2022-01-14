package com.blockchain.api.paymentmethods.models

import kotlinx.serialization.Serializable

@Serializable
data class CreateLinkBankResponse(
    val partner: String,
    val id: String,
    val attributes: LinkBankAttrsResponse?
) {
    companion object {
        const val YODLEE_PARTNER = "YODLEE"
        const val YAPILY_PARTNER = "YAPILY"
    }
}

@Serializable
data class CreateLinkBankRequestBody(
    private val currency: String
)

@Serializable
data class LinkBankAttrsResponse(
    val token: String?,
    val fastlinkUrl: String?,
    val fastlinkParams: FastlinkParamsResponse?,
    val institutions: List<YapilyInstitutionResponse>?,
    val entity: String?
)

@Serializable
data class YapilyInstitutionResponse(
    val countries: List<YapilyCountryResponse>,
    val fullName: String,
    val id: String,
    val media: List<YapilyMediaResponse>
)

@Serializable
data class YapilyCountryResponse(val countryCode2: String, val displayName: String)

@Serializable
data class YapilyMediaResponse(val source: String, val type: String)

@Serializable
data class FastlinkParamsResponse(
    val configName: String
)

@Serializable
data class LinkedBankTransferResponse(
    val id: String,
    val partner: String,
    val currency: String,
    val state: String,
    val details: LinkedBankDetailsResponse?,
    val error: String?,
    val attributes: LinkedBankTransferAttributesResponse?
) {
    companion object {
        const val CREATED = "CREATED"
        const val ACTIVE = "ACTIVE"
        const val PENDING = "PENDING"
        const val BLOCKED = "BLOCKED"
        const val FRAUD_REVIEW = "FRAUD_REVIEW"
        const val MANUAL_REVIEW = "MANUAL_REVIEW"

        const val ERROR_ALREADY_LINKED = "BANK_TRANSFER_ACCOUNT_ALREADY_LINKED"
        const val ERROR_ACCOUNT_INFO_NOT_FOUND = "BANK_TRANSFER_ACCOUNT_INFO_NOT_FOUND"
        const val ERROR_ACCOUNT_NOT_SUPPORTED = "BANK_TRANSFER_ACCOUNT_NOT_SUPPORTED"
        const val ERROR_NAMES_MISMATCHED = "BANK_TRANSFER_ACCOUNT_NAME_MISMATCH"
        const val ERROR_ACCOUNT_EXPIRED = "BANK_TRANSFER_ACCOUNT_EXPIRED"
        const val ERROR_ACCOUNT_REJECTED = "BANK_TRANSFER_ACCOUNT_REJECTED"
        const val ERROR_ACCOUNT_FAILURE = "BANK_TRANSFER_ACCOUNT_FAILED"
        const val ERROR_ACCOUNT_INVALID = "BANK_TRANSFER_ACCOUNT_INVALID"
        const val ERROR_ACCOUNT_FAILED_INTERNAL = "BANK_TRANSFER_ACCOUNT_FAILED_INTERNAL"
        const val ERROR_ACCOUNT_REJECTED_FRAUD = "BANK_TRANSFER_ACCOUNT_REJECTED_FRAUD"
    }
}

@Serializable
data class LinkedBankTransferAttributesResponse(
    val authorisationUrl: String?,
    val entity: String?,
    val media: List<BankMediaResponse>?,
    val callbackPath: String
)

@Serializable
data class ProviderAccountAttrs(
    val providerAccountId: String? = null,
    val accountId: String? = null,
    val institutionId: String? = null,
    val callback: String? = null
)

@Serializable
data class UpdateProviderAccountBody(
    val attributes: ProviderAccountAttrs
)

@Serializable
data class LinkedBankDetailsResponse(
    val accountNumber: String,
    val accountName: String?,
    val bankName: String?,
    val bankAccountType: String,
    val sortCode: String?,
    val iban: String?,
    val bic: String?
)

@Serializable
data class BankTransferPaymentBody(
    val amountMinor: String,
    val currency: String,
    val product: String = "SIMPLEBUY",
    val attributes: BankTransferPaymentAttributes?
)

@Serializable
data class BankTransferPaymentAttributes(
    val callback: String?
)

@Serializable
data class BankTransferPaymentResponse(
    val paymentId: String,
    val bankAccountType: String?,
    val attributes: BankTransferPaymentResponseAttributes
)

@Serializable
data class BankTransferPaymentResponseAttributes(
    val paymentId: String,
    val callbackPath: String
)

@Serializable
data class OpenBankingTokenBody(
    val oneTimeToken: String
)

@Serializable
data class BankInfoResponse(
    val id: String,
    val name: String?,
    val accountName: String?,
    val currency: String,
    val state: String,
    val accountNumber: String?,
    val bankAccountType: String?,
    val isBankAccount: Boolean,
    val isBankTransferAccount: Boolean,
    val attributes: BankInfoAttributes?
) {
    companion object {
        const val ACTIVE = "ACTIVE"
        const val PENDING = "PENDING"
        const val BLOCKED = "BLOCKED"
    }
}

@Serializable
data class BankTransferChargeResponse(
    val beneficiaryId: String,
    val state: String?,
    val amountMinor: String,
    val amount: BankTransferFiatAmount,
    val extraAttributes: BankTransferChargeAttributes
)

@Serializable
data class BankTransferFiatAmount(
    val symbol: String,
    val value: String
)

@Serializable
data class BankTransferChargeAttributes(
    val authorisationUrl: String?,
    val status: String?
) {
    companion object {
        const val CREATED = "CREATED"
        const val PRE_CHARGE_REVIEW = "PRE_CHARGE_REVIEW"
        const val AWAITING_AUTHORIZATION = "AWAITING_AUTHORIZATION"
        const val PRE_CHARGE_APPROVED = "PRE_CHARGE_APPROVED"
        const val PENDING = "PENDING"
        const val AUTHORIZED = "AUTHORIZED"
        const val CREDITED = "CREDITED"
        const val FAILED = "FAILED"
        const val FRAUD_REVIEW = "FRAUD_REVIEW"
        const val MANUAL_REVIEW = "MANUAL_REVIEW"
        const val REJECTED = "REJECTED"
        const val CLEARED = "CLEARED"
        const val COMPLETE = "COMPLETE"
    }
}

@Serializable
data class BankInfoAttributes(
    val entity: String?,
    val media: List<BankMediaResponse>?,
    val status: String?,
    val authorisationUrl: String?
)

@Serializable
data class BankMediaResponse(
    val source: String,
    val type: String
) {
    companion object {
        const val ICON = "icon"
        const val LOGO = "logo"
    }
}
