package com.blockchain.home.presentation.accouncement

import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.data.DataResource
import com.blockchain.home.announcements.Announcement

data class AnnouncementsViewState(
    val remoteAnnouncements: DataResource<List<Announcement>>,
    val hideAnnouncementsConfirmation: Boolean,
    val localAnnouncements: List<LocalAnnouncement>
) : ViewState
