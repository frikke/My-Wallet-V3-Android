package com.blockchain.home.presentation.accouncement

import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.data.DataResource
import com.blockchain.home.announcements.Announcement
import com.blockchain.presentation.pulltorefresh.PullToRefresh
import com.blockchain.walletmode.WalletMode

sealed interface AnnouncementsIntent : Intent<AnnouncementModelState> {
    data class LoadAnnouncements(
        val walletMode: WalletMode
    ) : AnnouncementsIntent

    data class DeleteAnnouncement(val announcement: Announcement) : AnnouncementsIntent {
        override fun isValidFor(modelState: AnnouncementModelState): Boolean {
            return (modelState.remoteAnnouncements as? DataResource.Data)?.data?.find { it == announcement } != null
        }
    }

    data class AnnouncementClicked(val announcement: Announcement) : AnnouncementsIntent {
        override fun isValidFor(modelState: AnnouncementModelState): Boolean {
            return (modelState.remoteAnnouncements as? DataResource.Data)?.data?.find { it == announcement } != null
        }
    }

    object Refresh : AnnouncementsIntent {
        override fun isValidFor(modelState: AnnouncementModelState): Boolean {
            return PullToRefresh.canRefresh(modelState.lastFreshDataTime)
        }
    }
}
