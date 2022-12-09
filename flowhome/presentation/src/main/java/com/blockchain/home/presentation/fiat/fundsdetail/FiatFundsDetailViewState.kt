package com.blockchain.home.presentation.fiat.fundsdetail

import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.data.DataResource

data class FiatFundsDetailViewState(
    val detail: DataResource<FiatFundsDetail>,
    val data: DataResource<FiatFundsDetailData>
) : ViewState

data class FiatFundsDetail(
    val name: String,
    val logo: String
)

