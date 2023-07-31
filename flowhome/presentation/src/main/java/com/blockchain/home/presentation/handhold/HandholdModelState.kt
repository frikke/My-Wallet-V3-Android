package com.blockchain.home.presentation.handhold

import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.data.DataResource
import com.blockchain.home.handhold.HandholdTasksStatus
import com.blockchain.walletmode.WalletMode

data class HandholdModelState(
    val data: DataResource<List<HandholdTasksStatus>> = DataResource.Loading,
    val walletMode: WalletMode? = null,
    val isKycRejected: Boolean = false,
    val lastFreshDataTime: Long = 0
) : ModelState
