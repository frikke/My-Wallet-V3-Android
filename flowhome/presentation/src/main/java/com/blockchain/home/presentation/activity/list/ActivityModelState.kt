package com.blockchain.home.presentation.activity.list

import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.data.DataResource
import com.blockchain.home.presentation.SectionSize
import com.blockchain.walletmode.WalletMode

data class ActivityModelState<ACTIVITY_MODEL>(
    val activityItems: DataResource<List<ACTIVITY_MODEL>> = DataResource.Loading,
    val sectionSize: SectionSize = SectionSize.All,
    val walletMode: WalletMode,
    val filterTerm: String = "",
    val lastFreshDataTime: Long = 0
) : ModelState
