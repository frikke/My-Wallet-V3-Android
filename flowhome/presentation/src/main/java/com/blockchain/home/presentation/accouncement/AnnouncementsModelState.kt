package com.blockchain.home.presentation.accouncement

import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.componentlib.utils.ImageValue
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.data.DataResource
import com.blockchain.home.announcements.Announcement
import com.blockchain.walletmode.WalletMode

data class AnnouncementModelState(
    val walletMode: WalletMode? = null,
    val remoteAnnouncements: DataResource<List<Announcement>> = DataResource.Loading,
    val hideAnnouncementsConfirmation: Boolean = false,
    val localAnnouncements: List<LocalAnnouncement> = emptyList(),
    val lastFreshDataTime: Long = 0
) : ModelState

data class LocalAnnouncement(
    val type: LocalAnnouncementType,
    val title: TextValue,
    val subtitle: TextValue,
    val icon: ImageValue
)

enum class LocalAnnouncementType {
    PHRASE_RECOVERY
}
