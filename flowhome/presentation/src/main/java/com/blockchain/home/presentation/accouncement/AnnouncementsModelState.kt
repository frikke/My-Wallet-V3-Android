package com.blockchain.home.presentation.accouncement

import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.componentlib.utils.ImageValue
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.data.DataResource
import com.blockchain.home.announcements.Announcement
import com.blockchain.walletmode.WalletMode

data class AnnouncementModelState(
    val walletMode: WalletMode? = null,
    val stackedAnnouncements: DataResource<List<Announcement>> = DataResource.Loading,
    val customAnnouncements: List<CustomAnnouncement> = emptyList(),
    val lastFreshDataTime: Long = 0
) : ModelState

data class CustomAnnouncement(
    val type: CustomAnnouncementType,
    val title: TextValue,
    val subtitle: TextValue,
    val icon: ImageValue
)

enum class CustomAnnouncementType {
    PHRASE_RECOVERY
}
