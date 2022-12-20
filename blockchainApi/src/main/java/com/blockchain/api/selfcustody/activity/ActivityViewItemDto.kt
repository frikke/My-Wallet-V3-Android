package com.blockchain.api.selfcustody.activity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModuleBuilder
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable
sealed class ActivityViewItemDto {
    @Serializable
    @SerialName("STACK_VIEW")
    data class Stack(
        @SerialName("leadingImage")
        val leadingImage: ActivityIconDto?,
        @SerialName("leading")
        val leading: List<StackComponentDto>,
        @SerialName("trailing")
        val trailing: List<StackComponentDto>,
    ) : ActivityViewItemDto()

    @Serializable
    @SerialName("BUTTON")
    data class Button(
        @SerialName("text")
        val value: String,
        @SerialName("style")
        val style: String,
        @SerialName("actionType")
        val actionType: String,
        @SerialName("actionData")
        val actionData: String
    ) : ActivityViewItemDto()

    @Serializable
    @SerialName("UNKNOWN")
    object Unknown : ActivityViewItemDto()
}

fun SerializersModuleBuilder.activityViewItemSerializer() {
    polymorphic(ActivityViewItemDto::class) {
        subclass(ActivityViewItemDto.Stack::class)
        subclass(ActivityViewItemDto.Button::class)
        default { ActivityViewItemDto.Unknown.serializer() }
    }
}
