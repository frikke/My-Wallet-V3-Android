package com.blockchain.home.presentation.activity.list

import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.data.DataResource
import com.blockchain.home.presentation.activity.common.ActivityComponent
import com.blockchain.walletmode.WalletMode
import java.util.Calendar

data class ActivityViewState(
    val activity: DataResource<Map<TransactionGroup, List<ActivityComponent>>>,
    val walletMode: WalletMode
) : ViewState

sealed interface TransactionGroup : Comparable<TransactionGroup> {
    override fun compareTo(other: TransactionGroup): Int {
        fun getAssignedValue(group: TransactionGroup): Long {
            return when (group) {
                Combined -> 0L
                Group.Pending -> Long.MAX_VALUE
                is Group.Date -> group.date.timeInMillis
            }
        }

        return getAssignedValue(this).compareTo(getAssignedValue(other))
    }

    object Combined : TransactionGroup

    sealed interface Group : TransactionGroup {
        data class Date(val date: Calendar) : Group {
            override fun equals(other: Any?) = date.timeInMillis == (other as? Date)?.date?.timeInMillis
            override fun hashCode() = date.timeInMillis.hashCode()
        }

        object Pending : Group
    }
}
