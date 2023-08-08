package com.blockchain.earn.interest.viewmodel

import com.blockchain.commonarch.presentation.mvi_v2.Intent
import info.blockchain.balance.Currency

sealed interface InterestSummaryIntent : Intent<InterestSummaryModelState> {
    class LoadData(val currency: Currency) : InterestSummaryIntent
    class InterestSummaryLoadError(val assetTicker: String) : InterestSummaryIntent
}
