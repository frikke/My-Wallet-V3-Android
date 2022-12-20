package com.blockchain.unifiedcryptowallet.domain.activity.model

import java.util.Calendar
import kotlin.math.sign

data class UnifiedActivityItem(
    val txId: String,
    val pubkey: String,
    val network: String,
    val blockExplorerUrl: String,
    val summary: ActivityDataItem,
    val status: String,
    val date: Calendar?
) : Comparable<UnifiedActivityItem> {

    override fun compareTo(other: UnifiedActivityItem): Int {
        return ((other.date?.timeInMillis ?: 0L) - (date?.timeInMillis ?: 0L)).sign
    }
}

sealed interface ActivityDetailGroups {
    data class GroupedItems(
        val title: String,
        val subtitle: String,
        val icon: ActivityIcon,
        val detailItems: List<ActivityDetailGroup>,
        val actionItems: List<ActivityDataItem>
    ) : ActivityDetailGroups
}

data class ActivityDetailGroup(
    val title: String?,
    val itemGroup: List<ActivityDataItem>
)
