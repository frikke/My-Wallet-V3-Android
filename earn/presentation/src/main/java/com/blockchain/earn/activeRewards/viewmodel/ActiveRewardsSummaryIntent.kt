package com.blockchain.earn.activeRewards.viewmodel

import com.blockchain.commonarch.presentation.mvi_v2.Intent
import info.blockchain.balance.Currency

sealed interface ActiveRewardsSummaryIntent : Intent<ActiveRewardsSummaryModelState> {
    class LoadData(val currency: Currency) : ActiveRewardsSummaryIntent
    class ActiveRewardsSummaryLoadError(val assetTicker: String) : ActiveRewardsSummaryIntent
}
