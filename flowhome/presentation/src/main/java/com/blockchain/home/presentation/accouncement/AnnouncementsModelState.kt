package com.blockchain.home.presentation.accouncement

import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.componentlib.utils.ImageValue
import com.blockchain.componentlib.utils.TextValue

data class AnnouncementModelState(
    val announcements: List<Announcement> = emptyList(),
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
