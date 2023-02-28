package com.blockchain.home.presentation.accouncement

import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.presentation.pulltorefresh.PullToRefresh

sealed interface AnnouncementsIntent : Intent<AnnouncementModelState> {
    object LoadAnnouncements : AnnouncementsIntent
    object Refresh : AnnouncementsIntent{
        override fun isValidFor(modelState: AnnouncementModelState): Boolean {
            return PullToRefresh.canRefresh(modelState.lastFreshDataTime)
        }
    }
}
