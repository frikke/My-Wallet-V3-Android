package com.blockchain.api.selfcustody.activity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModuleBuilder
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable
sealed interface ActivityIconDto {
    @Serializable
    @SerialName("SMALL_TAG")
    data class SmallTag(
        val main: String,
        val tag: String
    ) : ActivityIconDto

    @Serializable
    @SerialName("OVERLAPPING_PAIR")
    data class OverlappingPair(
        val front: String,
        val back: String
    ) : ActivityIconDto

    @Serializable
    @SerialName("SINGLE_ICON")
    data class SingleIcon(
        val url: String
    ) : ActivityIconDto

    @Serializable
    @SerialName("UNKNOWN")
    object Unknown : ActivityIconDto
}

fun SerializersModuleBuilder.activityIconSerializer() {
    polymorphic(ActivityIconDto::class) {
        subclass(ActivityIconDto.SmallTag::class)
        subclass(ActivityIconDto.OverlappingPair::class)
        subclass(ActivityIconDto.SingleIcon::class)
        default { ActivityIconDto.Unknown.serializer() }
    }
}
