package com.blockchain.api.earn

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

@Serializable
data class EarnRewardsEligibilityDto(
    @SerialName("eligible")
    val isEligible: Boolean,
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

@Serializable(with = EarnRewardsEligibilityResponseDtoSerializer::class)
sealed class EarnRewardsEligibilityResponseDto
@Serializable(with = AssetsWithEligibilityDeserializer::class)
class AssetsWithEligibility(
    val assets: Map<String, EarnRewardsEligibilityDto>
) : EarnRewardsEligibilityResponseDto()

@Serializable
class IneligibleReason(
    val eligibility: EarnRewardsEligibilityDto
) : EarnRewardsEligibilityResponseDto()

object EarnRewardsEligibilityResponseDtoSerializer :
    JsonContentPolymorphicSerializer<EarnRewardsEligibilityResponseDto>(EarnRewardsEligibilityResponseDto::class) {
    override fun selectDeserializer(
        element: JsonElement
    ): DeserializationStrategy<out EarnRewardsEligibilityResponseDto> {
        // We gotta do this because the backend returns a completely different JSON when there's no assets are eligible
        return when {
            element.jsonObject.containsKey("eligible") -> IneligibleReason.serializer()
            else -> AssetsWithEligibilityDeserializer
        }
    }
}

object AssetsWithEligibilityDeserializer : KSerializer<AssetsWithEligibility> {

    private val assetsWithEligibilitySerializer by lazy {
        MapSerializer(String.serializer(), EarnRewardsEligibilityDto.serializer())
    }

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("AssetsWithEligibilityDeserializer", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: AssetsWithEligibility) {
        encoder.encodeSerializableValue(assetsWithEligibilitySerializer, value.assets)
    }
    override fun deserialize(decoder: Decoder): AssetsWithEligibility {
        return AssetsWithEligibility(
            assets = decoder.decodeSerializableValue(assetsWithEligibilitySerializer)
        )
    }
}
