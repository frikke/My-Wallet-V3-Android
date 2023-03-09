package com.blockchain.home.announcements

import com.blockchain.walletmode.WalletMode

data class Announcement(
    val id: String,
    val title: String,
    val description: String,
    val imageUrl: String?,
    val eligibleModes: List<WalletMode>,
    val actionUrl: String,
    val priority: Double
)

enum class ConsumeAnnouncementAction {
    DELETED, CLICKED
}