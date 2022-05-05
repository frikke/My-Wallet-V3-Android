package com.blockchain.nabu.models.responses.nabu

import java.util.Locale
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class KycTierStateSerializer : KSerializer<KycTierState> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("KycTierState", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: KycTierState) {
        encoder.encodeString(
            when (value) {
                KycTierState.None -> NONE
                KycTierState.Rejected -> REJECTED
                KycTierState.Pending -> PENDING
                KycTierState.UnderReview -> UNDER_REVIEW
                KycTierState.Verified -> VERIFIED
                KycTierState.Expired -> EXPIRED
            }
        )
    }

    override fun deserialize(decoder: Decoder): KycTierState {
        return when (val input = decoder.decodeString().toUpperCase(Locale.US)) {
            NONE -> KycTierState.None
            REJECTED -> KycTierState.Rejected
            PENDING -> KycTierState.Pending
            VERIFIED -> KycTierState.Verified
            UNDER_REVIEW -> KycTierState.UnderReview
            EXPIRED -> KycTierState.Expired
            else -> throw Exception("Unknown KYC Tier State: $input, unsupported data type")
        }
    }

    private companion object {
        private const val NONE = "NONE"
        private const val REJECTED = "REJECTED"
        private const val PENDING = "PENDING"
        private const val VERIFIED = "VERIFIED"
        private const val UNDER_REVIEW = "UNDER_REVIEW"
        private const val EXPIRED = "EXPIRED"
    }
}
