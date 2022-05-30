package com.blockchain.api.payments.data

import com.blockchain.api.NabuUxErrorResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateLinkBankResponse(
    @SerialName("partner")
    val partner: String,
    @SerialName("id")
    val id: String,
    @SerialName("attributes")
    val attributes: LinkBankAttrsResponse?
) {
    companion object {
        const val YODLEE_PARTNER = "YODLEE"
        const val YAPILY_PARTNER = "YAPILY"
    }
}

@Serializable
data class CreateLinkBankRequestBody(
    @SerialName("currency")
    private val currency: String
)

@Serializable
data class LinkBankAttrsResponse(
    @SerialName("token")
    val token: String?,
    @SerialName("fastlinkUrl")
    val fastlinkUrl: String?,
    @SerialName("fastlinkParams")
    val fastlinkParams: FastlinkParamsResponse?,
    @SerialName("institutions")
    val institutions: List<YapilyInstitutionResponse>?,
    @SerialName("entity")
    val entity: String?
)

@Serializable
data class YapilyInstitutionResponse(
    @SerialName("countries")
    val countries: List<YapilyCountryResponse>,
    @SerialName("fullName")
    val fullName: String,
    @SerialName("id")
    val id: String,
    @SerialName("media")
    val media: List<YapilyMediaResponse>
)

@Serializable
data class YapilyCountryResponse(
    @SerialName("countryCode2")
    val countryCode2: String,
    @SerialName("displayName")
    val displayName: String
)

@Serializable
data class YapilyMediaResponse(
    @SerialName("source")
    val source: String,
    @SerialName("type")
    val type: String
)

@Serializable
data class FastlinkParamsResponse(
    @SerialName("configName")
    val configName: String
)

@Serializable
data class LinkedBankTransferResponse(
    @SerialName("id")
    val id: String,
    @SerialName("partner")
    val partner: String,
    @SerialName("currency")
    val currency: String,
    @SerialName("state")
    val state: String,
    @SerialName("details")
    val details: LinkedBankDetailsResponse?,
    @SerialName("error")
    val error: String?,
    @SerialName("attributes")
    val attributes: LinkedBankTransferAttributesResponse?,
    @SerialName("ux")
    val ux: NabuUxErrorResponse?
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
    @SerialName("authorisationUrl")
    val authorisationUrl: String?,
    @SerialName("entity")
    val entity: String?,
    @SerialName("media")
    val media: List<BankMediaResponse>?,
    @SerialName("callbackPath")
    val callbackPath: String?
)

@Serializable
data class ProviderAccountAttrs(
    @SerialName("providerAccountId")
    val providerAccountId: String? = null,
    @SerialName("accountId")
    val accountId: String? = null,
    @SerialName("institutionId")
    val institutionId: String? = null,
    @SerialName("callback")
    val callback: String? = null
)

@Serializable
data class UpdateProviderAccountBody(
    @SerialName("attributes")
    val attributes: ProviderAccountAttrs
)

@Serializable
data class LinkedBankDetailsResponse(
    @SerialName("accountNumber")
    val accountNumber: String?,
    @SerialName("accountName")
    val accountName: String?,
    @SerialName("bankName")
    val bankName: String?,
    @SerialName("bankAccountType")
    val bankAccountType: String?,
    @SerialName("sortCode")
    val sortCode: String?,
    @SerialName("iban")
    val iban: String?,
    @SerialName("bic")
    val bic: String?
)

@Serializable
data class BankTransferPaymentBody(
    @SerialName("amountMinor")
    val amountMinor: String,
    @SerialName("currency")
    val currency: String,
    @SerialName("product")
    val product: String,
    @SerialName("attributes")
    val attributes: BankTransferPaymentAttributes?
)

@Serializable
data class BankTransferPaymentAttributes(
    @SerialName("callback")
    val callback: String?
)

@Serializable
data class BankTransferPaymentResponse(
    @SerialName("paymentId")
    val paymentId: String,
    @SerialName("bankAccountType")
    val bankAccountType: String?
)

@Serializable
data class OpenBankingTokenBody(
    @SerialName("oneTimeToken")
    val oneTimeToken: String
)

@Serializable
data class BankInfoResponse(
    @SerialName("id")
    val id: String,
    @SerialName("name")
    val name: String?,
    @SerialName("accountName")
    val accountName: String?,
    @SerialName("currency")
    val currency: String,
    @SerialName("state")
    val state: String,
    @SerialName("accountNumber")
    val accountNumber: String?,
    @SerialName("bankAccountType")
    val bankAccountType: String?,
    @SerialName("isBankAccount")
    val isBankAccount: Boolean,
    @SerialName("isBankTransferAccount")
    val isBankTransferAccount: Boolean,
    @SerialName("attributes")
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
    @SerialName("beneficiaryId")
    val beneficiaryId: String,
    @SerialName("state")
    val state: String?,
    @SerialName("amountMinor")
    val amountMinor: String,
    @SerialName("amount")
    val amount: BankTransferFiatAmount,
    @SerialName("extraAttributes")
    val extraAttributes: BankTransferChargeAttributes,
    @SerialName("ux")
    val ux: NabuUxErrorResponse?
)

@Serializable
data class BankTransferFiatAmount(
    @SerialName("symbol")
    val symbol: String,
    @SerialName("value")
    val value: String
)

@Serializable
data class BankTransferChargeAttributes(
    @SerialName("authorisationUrl")
    val authorisationUrl: String?,
    @SerialName("status")
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
    @SerialName("entity")
    val entity: String?,
    @SerialName("media")
    val media: List<BankMediaResponse>?,
    @SerialName("status")
    val status: String?,
    @SerialName("authorisationUrl")
    val authorisationUrl: String?
)

@Serializable
data class BankMediaResponse(
    @SerialName("source")
    val source: String,
    @SerialName("type")
    val type: String
) {
    companion object {
        const val ICON = "icon"
        const val LOGO = "logo"
    }
}
