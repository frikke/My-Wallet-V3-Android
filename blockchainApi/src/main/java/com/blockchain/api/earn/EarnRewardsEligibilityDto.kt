package com.blockchain.api.earn

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
data class EarnRewardsEligibilityDto(
    @SerialName("eligible")
    val isEligible: Boolean = false,
    @SerialName("ineligibilityReason")
    val reason: String = DEFAULT_REASON_NONE
) {
    companion object {
        const val UNSUPPORTED_REGION = "UNSUPPORTED_REGION"
        const val INVALID_ADDRESS = "INVALID_ADDRESS"
        const val TIER_TOO_LOW = "TIER_TOO_LOW"
        const val DEFAULT_REASON_NONE = "NONE"
        private const val DEFAULT_FAILURE_REASON = "OTHER"

        fun default() = EarnRewardsEligibilityDto(
            isEligible = false,
            reason = DEFAULT_FAILURE_REASON
        )
    }
}

@Serializable(with = EarnRewardsEligibilityResponseDtoDateSerializer::class)
sealed class EarnRewardsEligibilityResponseDto {
    @Serializable
    class AssetsWithEligibility(
        val assets: Map<String, EarnRewardsEligibilityDto>
    ) : EarnRewardsEligibilityResponseDto()

    @Serializable
    class IneligibleReason(
        val eligibility: EarnRewardsEligibilityDto
    ) : EarnRewardsEligibilityResponseDto()
}

// We gotta do this because the backend returns a completely different JSON when there's no assets are eligible
@Serializer(EarnRewardsEligibilityResponseDto::class)
object EarnRewardsEligibilityResponseDtoDateSerializer : KSerializer<EarnRewardsEligibilityResponseDto> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("EarnRewardsEligibilityDto2", PrimitiveKind.STRING)

    private val assetsWithEligibilitySerializer by lazy {
        MapSerializer(String.serializer(), EarnRewardsEligibilityDto.serializer())
    }
    private val ineligibleReasonSerializer by lazy {
        EarnRewardsEligibilityDto.serializer()
    }

    override fun serialize(encoder: Encoder, value: EarnRewardsEligibilityResponseDto) {
        when (value) {
            is EarnRewardsEligibilityResponseDto.AssetsWithEligibility ->
                encoder.encodeSerializableValue(assetsWithEligibilitySerializer, value.assets)
            is EarnRewardsEligibilityResponseDto.IneligibleReason ->
                encoder.encodeSerializableValue(ineligibleReasonSerializer, value.eligibility)
        }
    }

    override fun deserialize(decoder: Decoder): EarnRewardsEligibilityResponseDto {
        val value = try {
            val decoded = decoder.decodeSerializableValue(ineligibleReasonSerializer)
            EarnRewardsEligibilityResponseDto.IneligibleReason(
                eligibility = decoded
            )
        } catch (ex: Exception) {
            EarnRewardsEligibilityResponseDto.AssetsWithEligibility(
                assets = decoder.decodeSerializableValue(assetsWithEligibilitySerializer)
            )
        }
        return value
    }
}
