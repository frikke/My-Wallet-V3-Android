package com.blockchain.api.selfcustody.activity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModuleBuilder
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable
data class ActivityTextStyleDto(
    @SerialName("typography")
    val typography: String,
    @SerialName("color")
    val color: String,
    @SerialName("strikethrough")
    val strikethrough: Boolean = false
)

@Serializable
sealed class StackComponentDto {
    @Serializable
    @SerialName("TEXT")
    data class Text(
        @SerialName("value")
        val value: String,
        @SerialName("style")
        val style: ActivityTextStyleDto
    ) : StackComponentDto()

    @Serializable
    @SerialName("BADGE")
    data class Tag(
        @SerialName("value")
        val value: String,
        @SerialName("style")
        val style: String
    ) : StackComponentDto()

    @Serializable
    @SerialName("UNKNOWN")
    object Unknown : StackComponentDto()
}

fun SerializersModuleBuilder.stackComponentSerializer() {
    polymorphic(StackComponentDto::class) {
        subclass(StackComponentDto.Text::class)
        subclass(StackComponentDto.Tag::class)
        default { StackComponentDto.Unknown.serializer() }
    }
}
