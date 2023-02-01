package com.blockchain.home.presentation.accouncement

import com.blockchain.commonarch.presentation.mvi_v2.Intent

sealed interface AnnouncementsIntent : Intent<AnnouncementModelState> {
    object LoadAnnouncements : AnnouncementsIntent
}
