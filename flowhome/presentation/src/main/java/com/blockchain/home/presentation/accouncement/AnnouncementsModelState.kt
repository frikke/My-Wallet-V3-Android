package com.blockchain.home.presentation.accouncement

import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.componentlib.utils.ImageValue
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.data.DataResource

data class AnnouncementModelState(
    val announcements: DataResource<List<Announcement>> = DataResource.Loading,
    val lastFreshDataTime: Long = 0
) : ModelState

data class Announcement(
    val type: AnnouncementType,
    val title: TextValue,
    val subtitle: TextValue,
    val icon: ImageValue
)

enum class AnnouncementType {
    PHRASE_RECOVERY
}
