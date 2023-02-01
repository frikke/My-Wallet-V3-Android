package com.blockchain.home.presentation.accouncement

import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.data.DataResource

data class AnnouncementsViewState(
    val announcements: DataResource<List<Announcement>> = DataResource.Loading,
) : ViewState