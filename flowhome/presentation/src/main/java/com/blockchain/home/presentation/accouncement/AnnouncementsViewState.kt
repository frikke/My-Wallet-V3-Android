package com.blockchain.home.presentation.accouncement

import com.blockchain.commonarch.presentation.mvi_v2.ViewState

data class AnnouncementsViewState(
    val announcements: List<Announcement>
) : ViewState
