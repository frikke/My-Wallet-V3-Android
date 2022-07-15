package com.blockchain.nabu.models.responses.nabu

import com.blockchain.domain.paymentmethods.model.BillingAddress
import com.blockchain.serialization.JsonSerializable
import com.blockchain.serializers.AnyToStringSerializer
import kotlin.math.max
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
data class NabuUser(
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String = "",
    val emailVerified: Boolean = false,
    val dob: String? = null,
    val mobile: String? = null,
    val mobileVerified: Boolean,
    val address: Address? = null,
    val state: UserState,
    val kycState: KycState,
    private val productsUsed: ProductsUsed? = null,
    private val settings: NabuSettings? = null,
    val resubmission: ResubmissionResponse? = null,
    /**
     * ISO-8601 Timestamp w/millis, eg 2018-08-15T17:00:45.129Z
     */
    val insertedAt: String? = null,
    /**
     * ISO-8601 Timestamp w/millis, eg 2018-08-15T17:00:45.129Z
     */
    val updatedAt: String? = null,
    private val tags: Map<String, Map<String, @Serializable(with = AnyToStringSerializer::class) Any>>? = null,
    val userName: String? = null,
    val tiers: TierLevels? = null,
    val currencies: CurrenciesResponse,
    val walletGuid: String? = null
) : JsonSerializable {
    val tierInProgress
        get() =
            tiers?.let {
                if (kycState == KycState.None) {
                    max(it.selected ?: 0, it.next ?: 0)
                } else {
                    0
                }
            } ?: 0

    val tierInProgressOrCurrentTier
        get() =
            tiers?.let {
                if (kycState == KycState.Verified) {
                    it.current
                } else {
                    max(it.selected ?: 0, it.next ?: 0)
                }
            } ?: 0

    val currentTier
        get() =
            tiers?.let {
                if (kycState == KycState.Verified) {
                    it.current
                } else {
                    0
                }
            } ?: 0

    fun requireCountryCode(): String {
        return address?.countryCode ?: throw IllegalStateException("User has no country code set")
    }

    val isMarkedForResubmission: Boolean
        get() = resubmission != null

    val isMarkedForRecoveryResubmission: Boolean
        get() = resubmission?.reason == ResubmissionResponse.ACCOUNT_RECOVERED_REASON &&
            tiers?.current != 2

    val isPowerPaxTagged: Boolean
        get() = tags?.containsKey(POWER_PAX_TAG) ?: false

    val exchangeEnabled: Boolean
        get() = productsUsed?.exchange ?: settings?.MERCURY_EMAIL_VERIFIED ?: false

    companion object {
        const val POWER_PAX_TAG = "POWER_PAX"
    }
}

@Serializable
data class TierLevels(
    val current: Int? = null,
    val selected: Int? = null,
    val next: Int? = null
)

@Serializable
data class Address(
    val line1: String? = null,
    val line2: String? = null,
    val city: String? = null,
    val state: String? = null,
    val postCode: String? = null,
    @SerialName("country")
    val countryCode: String? = null
)

@Serializable
data class AddAddressRequest(
    val address: Address
) {
    companion object {

        fun fromAddressDetails(
            line1: String,
            line2: String?,
            city: String,
            state: String?,
            postCode: String,
            countryCode: String
        ): AddAddressRequest = AddAddressRequest(
            Address(
                line1,
                line2,
                city,
                state,
                postCode,
                countryCode
            )
        )

        fun fromBillingAddress(
            billingAddress: BillingAddress
        ): Address =
            Address(
                line1 = billingAddress.addressLine1,
                line2 = billingAddress.addressLine2,
                city = billingAddress.city,
                countryCode = billingAddress.countryCode,
                postCode = billingAddress.postCode,
                state = billingAddress.state
            )
    }
}

@Serializable(with = KycStateSerializer::class)
sealed class KycState {
    object None : KycState()
    object Pending : KycState()
    object UnderReview : KycState()
    object Rejected : KycState()
    object Expired : KycState()
    object Verified : KycState()
}

@Serializable(with = UserStateSerializer::class)
sealed class UserState {
    object None : UserState()
    object Created : UserState()
    object Active : UserState()
    object Blocked : UserState()
}

class KycStateSerializer : KSerializer<KycState> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("KycState", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: KycState) {
        encoder.encodeString(
            when (value) {
                KycState.None -> NONE
                KycState.Pending -> PENDING
                KycState.Rejected -> REJECTED
                KycState.UnderReview -> UNDER_REVIEW
                KycState.Expired -> EXPIRED
                KycState.Verified -> VERIFIED
            }
        )
    }

    override fun deserialize(decoder: Decoder): KycState {
        return when (val input = decoder.decodeString()) {
            NONE -> KycState.None
            PENDING -> KycState.Pending
            UNDER_REVIEW -> KycState.UnderReview
            REJECTED -> KycState.Rejected
            EXPIRED -> KycState.Expired
            VERIFIED -> KycState.Verified
            else -> throw Exception("Unknown KYC State: $input, unsupported data type")
        }
    }

    private companion object {
        private const val NONE = "NONE"
        private const val PENDING = "PENDING"
        private const val UNDER_REVIEW = "UNDER_REVIEW"
        private const val REJECTED = "REJECTED"
        private const val EXPIRED = "EXPIRED"
        private const val VERIFIED = "VERIFIED"
    }
}

class UserStateSerializer : KSerializer<UserState> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("UserState", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: UserState) {
        encoder.encodeString(
            when (value) {
                UserState.None -> NONE
                UserState.Created -> CREATED
                UserState.Active -> ACTIVE
                UserState.Blocked -> BLOCKED
            }
        )
    }

    override fun deserialize(decoder: Decoder): UserState {
        return when (val input = decoder.decodeString()) {
            NONE -> UserState.None
            CREATED -> UserState.Created
            ACTIVE -> UserState.Active
            BLOCKED -> UserState.Blocked
            else -> throw Exception("Unknown User State: $input, unsupported data type")
        }
    }

    private companion object {
        private const val NONE = "NONE"
        private const val CREATED = "CREATED"
        private const val ACTIVE = "ACTIVE"
        private const val BLOCKED = "BLOCKED"
    }
}

@Serializable
data class ProductsUsed(
    val exchange: Boolean? = false
)

@Serializable
data class NabuSettings(
    val MERCURY_EMAIL_VERIFIED: Boolean? = false
)
