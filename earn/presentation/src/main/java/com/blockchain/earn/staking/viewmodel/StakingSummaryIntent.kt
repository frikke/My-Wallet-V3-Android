package com.blockchain.earn.staking.viewmodel

import com.blockchain.commonarch.presentation.mvi_v2.Intent
import info.blockchain.balance.Currency

sealed interface StakingSummaryIntent : Intent<StakingSummaryModelState> {
    class LoadData(val currency: Currency) : StakingSummaryIntent
    class StakingSummaryLoadError(val assetTicker: String) : StakingSummaryIntent
}
