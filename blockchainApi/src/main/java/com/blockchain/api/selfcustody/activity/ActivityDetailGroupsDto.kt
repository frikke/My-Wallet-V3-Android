package com.blockchain.api.selfcustody.activity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModuleBuilder
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable
sealed interface ActivityDetailGroupsDto {
    @Serializable
    @SerialName("GROUPED_ITEMS")
    data class GroupedItems(
        @SerialName("title")
        val title: String,
        @SerialName("subtitle")
        val subtitle: String,
        @SerialName("icon")
        val icon: ActivityIconDto,
        @SerialName("itemGroups")
        val items: List<DetailGroup>,
        @SerialName("floatingActions")
        val floatingActions: List<ActivityViewItemDto>
    ) : ActivityDetailGroupsDto {

        @Serializable
        data class DetailGroup(
            @SerialName("title")
            val title: String?,
            @SerialName("itemGroup")
            val itemGroup: List<ActivityViewItemDto>
        )
    }

    @Serializable
    @SerialName("UNKNOWN")
    object Unknown : ActivityDetailGroupsDto
}

fun SerializersModuleBuilder.activityDetailSerializer() {
    polymorphic(ActivityDetailGroupsDto::class) {
        subclass(ActivityDetailGroupsDto.GroupedItems::class)
        default { ActivityDetailGroupsDto.Unknown.serializer() }
    }
}
