package com.blockchain.home.presentation.handhold

import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.data.DataResource
import com.blockchain.home.domain.AssetFilter
import com.blockchain.home.presentation.SectionSize
import com.blockchain.home.presentation.allassets.AssetsModelState
import com.blockchain.presentation.pulltorefresh.PullToRefresh
import com.blockchain.walletmode.WalletMode

sealed interface HandholdIntent : Intent<HandholdModelState> {
    object LoadData: HandholdIntent
}
